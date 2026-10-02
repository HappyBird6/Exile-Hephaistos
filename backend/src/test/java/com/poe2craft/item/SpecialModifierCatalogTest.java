package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.ModifierPoolResolver;
import com.poe2craft.crafting.domain.StateBucket;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecialModifierCatalogTest {
  InputStream resource(String name) {
    return getClass().getResourceAsStream("/catalog/solar-amulet/" + name);
  }

  @Test
  void exactSpecialRowsAndDetailStatsMatchWithoutJoiningTheOrdinarySpawnPool() throws Exception {
    var catalog = ItemCatalogLoader.loadDefault();
    try (var input = resource("perfect-infinite.raw.json")) {
      var rows = new ObjectMapper().readTree(input);
      assertThat(rows).hasSize(3);
      for (var proof : rows) {
        var row = proof.get("row");
        var d =
            catalog.modifiers().values().stream()
                .filter(m -> m.sourceUrl().equals(proof.get("detailUrl").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(d.name()).isEqualTo("of the Essence");
        assertThat(d.requiredItemLevel()).isEqualTo(row.get("Level").asInt()).isEqualTo(72);
        assertThat(d.weight()).isEqualTo(row.get("DropChance").asInt()).isZero();
        assertThat(d.familyIds()).containsExactly(row.get("ModFamilyList").get(0).asText());
        assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
        assertThat(row.get("ModGenerationTypeID").asInt()).isEqualTo(2);
        var matcher =
            java.util.regex.Pattern.compile(
                    "<li>(.*?) <span class=\"badge bg-primary\">(\\d+) <span class=\"ndash\">[^<]*</span> (\\d+)</span>")
                .matcher(proof.get("detailHtml").asText());
        assertThat(matcher.find()).isTrue();
        assertThat(d.stats())
            .containsExactly(
                new ModifierDefinition.StatRange(
                    matcher.group(1).replace(' ', '_'),
                    Long.parseLong(matcher.group(2)),
                    Long.parseLong(matcher.group(3))));
        assertThat(d.tier()).isEqualTo(1);
      }
    }
    var root = SolarAmulet.initial(catalog);
    var rare =
        new StateBucket(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(),
            root.conditions());
    var pool = new ModifierPoolResolver(catalog).resolve(rare);
    assertThat(pool.candidates()).hasSize(209).allMatch(d -> d.weight() > 0);
    assertThat(pool.totalWeight()).isEqualTo(168856);
    assertThat(catalog.compatibleSnapshotIds())
        .containsExactly("poe2db-amulets-base-2026-09-29-a4f439852790");
    assertThat(catalog.metadata().snapshotId()).contains("+perfect-infinite-2026-10-02-");
  }

  @Test
  void rejectsChangedSpecialSourceOrAnInventedPositiveSpawnWeight() throws Exception {
    String json;
    try (var input = resource("perfect-infinite.catalog.json")) {
      json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    for (boolean changedSource : List.of(true, false)) {
      try (var ordinary = resource("catalog.json");
          var raw = resource("base.raw.json");
          var details = resource("details.raw.json");
          var source = resource("perfect-infinite.raw.json")) {
        var special =
            new ByteArrayInputStream(
                (changedSource ? json : json.replaceFirst("\"weight\": 0", "\"weight\": 1000"))
                    .getBytes(StandardCharsets.UTF_8));
        var input =
            changedSource
                ? new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))
                : source;
        assertThatThrownBy(
                () -> ItemCatalogLoader.loadWithSpecial(ordinary, raw, details, special, input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(changedSource ? "checksum" : "zero-spawn");
      }
    }
  }
}
