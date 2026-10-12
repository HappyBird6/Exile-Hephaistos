package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReviewedMethodPoliciesTest {
  @Test
  void everyDeclaredUniformOrdinaryCatalogReachesRuleEvaluationWithoutWeightRejection()
      throws Exception {
    int count = 0;
    var json = new ObjectMapper();
    try (var directories = Files.list(Path.of("src/main/resources/catalog"))) {
      for (var directory : directories.filter(Files::isDirectory).toList()) {
        var file = directory.resolve("catalog.json");
        if (!Files.exists(file)) continue;
        var policy = json.readTree(file.toFile()).path("metadata").path("weightPolicy").asText();
        if (!Set.of(
                "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
            .contains(policy)) continue;
        ItemCatalog catalog;
        try (var data = Files.newInputStream(file);
            var raw = Files.newInputStream(directory.resolve("base.raw.json"));
            var details = Files.newInputStream(directory.resolve("details.raw.json"))) {
          catalog = ItemCatalogLoader.load(data, raw, details);
        }
        var model = BasicCurrencyTransitions.forMethodSearch(catalog, "uniform-catalog-audit");
        var implicits =
            catalog.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT)
                .map(
                    d -> {
                      var values = new LinkedHashMap<String, Long>();
                      d.stats().forEach(s -> values.put(s.id(), s.min()));
                      return new ModifierInstance(d.id(), values);
                    })
                .toList();
        var item =
            new ItemState(
                catalog.metadata().snapshotId(),
                catalog.base().id(),
                100,
                ItemState.Rarity.RARE,
                implicits,
                List.of(),
                Set.of());
        for (var action : BasicCurrencyTransitions.supportedActions()) {
          var result =
              model.expand(new BasicCurrencyState(item, model.provenance()), action, Set.of(), 1);
          assertThat(result.reason())
              .as("%s %s", directory.getFileName(), action)
              .isNotIn("SELECTION_WEIGHT_MODEL_NOT_IMPLEMENTED", "NUMERIC_BASE_NOT_IMPLEMENTED");
        }
        count++;
      }
    }
    assertThat(count).isEqualTo(30);
  }
}
