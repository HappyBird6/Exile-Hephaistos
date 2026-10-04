package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class CatalystRegistryTest {
  @Test
  void verifiedCatalystActionsHaveExactBaseRestrictionsAndNeverRemainPending() {
    var registry = CraftingRegistryLoader.load();
    var catalogs = new LinkedHashMap<String, ItemCatalog>();
    catalogs.put("solar", ItemCatalogLoader.loadDefault());
    catalogs.put("ring", ItemCatalogLoader.loadRing());
    for (var base :
        List.of(
            "ruby",
            "emerald",
            "sapphire",
            "diamond",
            "time-lost-ruby",
            "time-lost-emerald",
            "time-lost-sapphire",
            "time-lost-diamond"))
      catalogs.put(
          base,
          base.equals("sapphire")
              ? ItemCatalogLoader.loadSapphire()
              : ItemCatalogLoader.loadBasicJewel(base));
    int count = 0;
    for (var entry : registry.path("entries")) {
      if (!entry.path("category").asText().equals("CATALYST")) continue;
      count++;
      var action = WorkbenchCurrency.valueOf(entry.path("action").asText());
      var expected =
          action.refinedCatalyst()
              ? Set.of("ruby", "emerald", "sapphire", "diamond")
              : Set.of("solar", "ring");
      var declared = new HashSet<String>();
      entry.path("supportedBases").forEach(b -> declared.add(b.asText()));
      assertThat(declared).isEqualTo(expected);
      assertThat(entry.path("effectStatus").asText()).isEqualTo("IMPLEMENTED");
      assertThat(entry.path("registrationHistory").path("effectStatus").asText())
          .isEqualTo("PENDING_RULE_VERIFICATION");
      for (var base : catalogs.entrySet()) {
        var c = base.getValue();
        var implicits =
            c.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT)
                .map(
                    d -> {
                      var values = new TreeMap<String, Long>();
                      d.stats().forEach(s -> values.put(s.id(), s.min()));
                      return new ModifierInstance(d.id(), values);
                    })
                .toList();
        var before =
            new ItemState(
                c.metadata().snapshotId(),
                c.base().id(),
                82,
                ItemState.Rarity.RARE,
                implicits,
                List.of(),
                Set.of());
        var result =
            new WorkbenchSimulator(c, new CraftingEngine(c))
                .apply(before, action, Set.of(), new Random(1));
        assertThat(result.applied())
            .as(entry.path("id").asText() + " on " + base.getKey())
            .isEqualTo(expected.contains(base.getKey()));
        if (!result.applied()) assertThat(result.state()).isEqualTo(before);
      }
    }
    assertThat(count).isEqualTo(26);
    long active = 0, implemented = 0, pending = 0, deferred = 0;
    for (var e : registry.path("entries")) {
      if (e.path("serviceScope").asText().equals("ACTIVE")) {
        active++;
        if (e.path("effectStatus").asText().equals("IMPLEMENTED")) implemented++;
        else pending++;
      } else deferred++;
    }
    assertThat(registry.path("serviceScope").path("active").asLong()).isEqualTo(active);
    assertThat(registry.path("serviceScope").path("implementedActive").asLong())
        .isEqualTo(implemented);
    assertThat(registry.path("serviceScope").path("pending").asLong()).isEqualTo(pending);
    assertThat(registry.path("serviceScope").path("deferred").asLong()).isEqualTo(deferred);
  }
}
