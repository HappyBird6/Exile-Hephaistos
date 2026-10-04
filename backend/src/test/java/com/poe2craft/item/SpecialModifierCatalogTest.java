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
  @Test
  void abyssProofHasExactlyTwoAffixesWithOneSharedFamilyAndFixedMarker() throws Exception {
    var current = ItemCatalogLoader.loadDefault();
    try (var input = resource("abyss-essence.raw.json")) {
      var proofs = new ObjectMapper().readTree(input);
      assertThat(proofs).hasSize(2);
      for (var proof : proofs) {
        var row = proof.get("row");
        var affix =
            row.get("ModGenerationTypeID").asInt() == 1
                ? ModifierDefinition.AffixType.PREFIX
                : ModifierDefinition.AffixType.SUFFIX;
        var d =
            current
                .find(
                    "amulet:"
                        + affix.name().toLowerCase(java.util.Locale.ROOT)
                        + ":essence-abyssal-mark")
                .orElseThrow();
        assertThat(d.affixType()).isEqualTo(affix);
        assertThat(d.familyIds()).containsExactly("EssenceAbyss");
        assertThat(d.requiredItemLevel()).isEqualTo(row.get("Level").asInt()).isEqualTo(1);
        assertThat(d.weight()).isEqualTo(row.get("DropChance").asInt()).isZero();
        assertThat(d.stats())
            .containsExactly(
                new ModifierDefinition.StatRange("essence_abyss_guaranteed_pick", 1, 1));
        assertThat(d.text()).isEqualTo("Bears the Mark of the Abyssal Lord");
        assertThat(proof.get("detailHtml").asText())
            .contains("essence abyss guaranteed pick", "EssenceAbyss");
      }
    }
    try (var catalog = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json");
        var infinite = resource("perfect-infinite.catalog.json");
        var infiniteRaw = resource("perfect-infinite.raw.json");
        var enhancement = resource("perfect-enhancement.catalog.json");
        var enhancementRaw = resource("perfect-enhancement.raw.json");
        var breach = resource("breach-essence.catalog.json");
        var breachRaw = resource("breach-essence.raw.json");
        var runic = resource("runic-alloy.catalog.json");
        var runicRaw = resource("runic-alloy.raw.json")) {
      var previous =
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.addSpecial(
                  ItemCatalogLoader.addSpecial(
                      ItemCatalogLoader.loadWithSpecial(
                          catalog, raw, details, infinite, infiniteRaw),
                      enhancement,
                      enhancementRaw),
                  breach,
                  breachRaw),
              runic,
              runicRaw);
      assertThat(current.compatibleSnapshotIds()).contains(previous.metadata().snapshotId());
      assertThat(current.base()).isEqualTo(previous.base());
      previous.modifiers().forEach((id, d) -> assertThat(current.find(id)).contains(d));
    }
  }

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
        .containsExactly(
            "poe2db-amulets-base-2026-09-29-a4f439852790",
            "poe2db-amulets-base-2026-09-29-a4f439852790+perfect-infinite-2026-10-02-18dd1e515063",
            "solar-special-1a52789b16483bb0d55aa5cbff59dc9ca4c582dea80ac8749251494c6020327c",
            "solar-special-ffd4f62c2195319ab608a1f6a97352d0bf57189adc5612d6e80465fea792f922",
            "solar-special-d04104e874a97324adf4da5f3ec733fb5ba13a7624f0679d7abce3facde84875");
    assertThat(catalog.metadata().snapshotId())
        .startsWith("solar-special-")
        .hasSizeLessThanOrEqualTo(120);
  }

  @Test
  void enhancementProofRetainsOneCombinedStatAndEveryEarlierDefinitionExactly() throws Exception {
    var current = ItemCatalogLoader.loadDefault();
    try (var input = resource("perfect-enhancement.raw.json")) {
      var proofs = new ObjectMapper().readTree(input);
      assertThat(proofs).hasSize(1);
      var proof = proofs.get(0);
      var row = proof.get("row");
      var d = current.find("amulet:prefix:essence-global-defences").orElseThrow();
      assertThat(row.get("Code").asText()).isEqualTo("EssenceGlobalDefences1");
      assertThat(d.name()).isEqualTo("Essences");
      assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
      assertThat(row.get("ModGenerationTypeID").asInt()).isEqualTo(1);
      assertThat(d.requiredItemLevel()).isEqualTo(row.get("Level").asInt()).isEqualTo(72);
      assertThat(row.get("reqlvl").asInt()).isEqualTo(57);
      assertThat(d.weight()).isEqualTo(row.get("DropChance").asInt()).isZero();
      assertThat(d.familyIds()).containsExactly("AllDefences");
      assertThat(d.tags()).containsExactly("defences");
      assertThat(d.sourceUrl()).isEqualTo(proof.get("detailUrl").asText());
      assertThat(d.stats())
          .containsExactly(
              new ModifierDefinition.StatRange("global_armour_evasion_energy_shield_+%", 20, 30));
      assertThat(proof.get("detailHtml").asText())
          .contains(
              "<li>global armour evasion energy shield +%", "Family<td>AllDefences",
              "GenerationType<td>Prefix (1)", "Req. level<td>72 (Effective: 57)");
    }
    try (var ordinary = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json");
        var special = resource("perfect-infinite.catalog.json");
        var specialRaw = resource("perfect-infinite.raw.json")) {
      var previous = ItemCatalogLoader.loadWithSpecial(ordinary, raw, details, special, specialRaw);
      assertThat(previous.modifiers())
          .allSatisfy((id, d) -> assertThat(current.find(id)).contains(d));
      assertThat(current.base()).isEqualTo(previous.base());
      assertThat(current.compatibleSnapshotIds()).contains(previous.metadata().snapshotId());
      try (var enhancement = resource("perfect-enhancement.catalog.json")) {
        assertThatThrownBy(
                () ->
                    ItemCatalogLoader.addSpecial(
                        previous,
                        enhancement,
                        new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("checksum");
      }
    }
  }

  @Test
  void breachProofIsFixedUnscalablePrefixAndRetainsTheEntirePreviousSnapshot() throws Exception {
    var current = ItemCatalogLoader.loadDefault();
    try (var input = resource("breach-essence.raw.json")) {
      var proofs = new ObjectMapper().readTree(input);
      assertThat(proofs).hasSize(1);
      var proof = proofs.get(0);
      var d = current.find("amulet:prefix:essence-maximum-quality").orElseThrow();
      assertThat(d.name()).isEqualTo("Breachlord's");
      assertThat(d.text()).isEqualTo("+20% to Maximum Quality");
      assertThat(d.familyIds()).containsExactly("LocalMaximumQuality");
      assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
      assertThat(d.requiredItemLevel())
          .isEqualTo(proof.get("row").get("Level").asInt())
          .isEqualTo(1);
      assertThat(d.weight()).isEqualTo(proof.get("row").get("DropChance").asInt()).isZero();
      assertThat(d.stats())
          .containsExactly(new ModifierDefinition.StatRange("local_maximum_quality_+", 20, 20));
      assertThat(d.tags()).isEmpty();
      assertThat(d.sourceUrl()).isEqualTo(proof.get("detailUrl").asText());
      assertThat(proof.get("detailHtml").asText())
          .contains("Unscalable Value", "GenerationType<td>Prefix (1)", "Req. level<td>1");
    }
    try (var ordinary = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json");
        var special = resource("perfect-infinite.catalog.json");
        var specialRaw = resource("perfect-infinite.raw.json");
        var enhancement = resource("perfect-enhancement.catalog.json");
        var enhancementRaw = resource("perfect-enhancement.raw.json")) {
      var previous =
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.loadWithSpecial(ordinary, raw, details, special, specialRaw),
              enhancement,
              enhancementRaw);
      assertThat(current.compatibleSnapshotIds()).contains(previous.metadata().snapshotId());
      assertThat(previous.modifiers())
          .allSatisfy((id, d) -> assertThat(current.find(id)).contains(d));
      assertThat(current.base()).isEqualTo(previous.base());
      try (var breach = resource("breach-essence.catalog.json")) {
        assertThatThrownBy(
                () ->
                    ItemCatalogLoader.addSpecial(
                        previous,
                        breach,
                        new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("checksum");
      }
    }
  }

  @Test
  void runicProofMatchesOneSourcedWardStatAndPreservesAllExistingDefinitions() throws Exception {
    var current = ItemCatalogLoader.loadDefault();
    try (var input = resource("runic-alloy.raw.json")) {
      var proofs = new ObjectMapper().readTree(input);
      assertThat(proofs).hasSize(1);
      var proof = proofs.get(0);
      var d = current.find("amulet:prefix:alloy-maximum-runic-ward").orElseThrow();
      assertThat(d.name()).isEqualTo("Verisium");
      assertThat(d.affixType()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
      assertThat(d.familyIds()).containsExactly("RunicWardPercent");
      assertThat(d.requiredItemLevel())
          .isEqualTo(proof.get("row").get("Level").asInt())
          .isEqualTo(13);
      assertThat(proof.get("row").get("reqlvl").asInt()).isEqualTo(10);
      assertThat(d.weight()).isZero();
      assertThat(d.tags()).containsExactly("runic_ward");
      assertThat(d.stats())
          .containsExactly(new ModifierDefinition.StatRange("maximum_ward_+%", 6, 10));
      assertThat(d.sourceUrl()).isEqualTo(proof.get("detailUrl").asText());
      assertThat(proof.get("detailHtml").asText())
          .contains("Stats<td><li>maximum ward +%", "Req. level<td>13 (Effective: 10)");
    }
    try (var ordinary = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json");
        var infinite = resource("perfect-infinite.catalog.json");
        var infiniteRaw = resource("perfect-infinite.raw.json");
        var enhancement = resource("perfect-enhancement.catalog.json");
        var enhancementRaw = resource("perfect-enhancement.raw.json");
        var breach = resource("breach-essence.catalog.json");
        var breachRaw = resource("breach-essence.raw.json")) {
      var previous =
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.addSpecial(
                  ItemCatalogLoader.loadWithSpecial(ordinary, raw, details, infinite, infiniteRaw),
                  enhancement,
                  enhancementRaw),
              breach,
              breachRaw);
      assertThat(current.compatibleSnapshotIds()).contains(previous.metadata().snapshotId());
      assertThat(previous.modifiers())
          .allSatisfy((id, d) -> assertThat(current.find(id)).contains(d));
      assertThat(current.base()).isEqualTo(previous.base());
    }
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
