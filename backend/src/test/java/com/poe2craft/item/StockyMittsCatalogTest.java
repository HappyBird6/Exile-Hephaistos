package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class StockyMittsCatalogTest {
  @Test
  void completePublishedPoolLoadsWithoutPruningOrChangingTheDefault() throws Exception {
    var catalog = load();
    assertThat(catalog.modifiers()).hasSize(182);
    assertThat(catalog.base().hasImplicit()).isFalse();
    assertThat(catalog.metadata().prefixCount()).isEqualTo(83);
    assertThat(catalog.metadata().suffixCount()).isEqualTo(99);
    assertThat(catalog.metadata().prefixWeight()).isEqualTo(63700);
    assertThat(catalog.metadata().suffixWeight()).isEqualTo(84500);
    assertThat(catalog.modifiers().values().stream().filter(d -> d.stats().size() > 1).count())
        .isEqualTo(42);
    assertThat(ItemCatalogLoader.loadDefault().modifiers()).hasSize(218);
    try (var raw = resource("base.raw.json")) {
      var rows = new ObjectMapper().readTree(raw);
      for (var row : rows) {
        var matches =
            catalog.modifiers().values().stream()
                .filter(d -> d.name().equals(row.get("Name").asText()))
                .filter(d -> d.requiredItemLevel() == row.get("Level").asInt())
                .toList();
        assertThat(matches).hasSize(1);
        var definition = matches.getFirst();
        assertThat(definition.weight()).isEqualTo(row.get("DropChance").asInt());
        assertThat(definition.familyIds())
            .containsExactly(row.get("ModFamilyList").get(0).asText());
        assertThat(definition.affixType())
            .isEqualTo(
                row.get("ModGenerationTypeID").asInt() == 1
                    ? ModifierDefinition.AffixType.PREFIX
                    : ModifierDefinition.AffixType.SUFFIX);
      }
    }
  }

  @Test
  void recoveredDetailsPreserveExactSignedSourceBounds() throws Exception {
    var catalog = load();
    assertThat(catalog.find("stocky-mitts:prefix:encased").orElseThrow().stats())
        .containsExactly(
            new ModifierDefinition.StatRange(
                "local_base_physical_damage_reduction_rating", 160, 190));
    String[] names = {"worthy", "apt", "talented", "skilled", "proficient"};
    for (int index = 0; index < names.length; index++) {
      long value = -15 - index * 5;
      assertThat(catalog.find("stocky-mitts:suffix:of-the-" + names[index]).orElseThrow().stats())
          .containsExactly(
              new ModifierDefinition.StatRange("local_attribute_requirements_+%", value, value));
    }
  }

  @Test
  void sourceChangesFailChecksumValidation() throws Exception {
    try (var data = resource("catalog.json");
        var details = resource("details.raw.json")) {
      assertThatThrownBy(
              () -> ItemCatalogLoader.load(data, new ByteArrayInputStream(new byte[0]), details))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("checksum");
    }
    try (var data = resource("catalog.json");
        var raw = resource("base.raw.json")) {
      assertThatThrownBy(
              () -> ItemCatalogLoader.load(data, raw, new ByteArrayInputStream(new byte[0])))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("checksum");
    }
  }

  @Test
  void abyssAddsOnlyTwoZeroSpawnDefinitionsAndRetainsExactOrdinarySnapshot() throws Exception {
    var ordinary = load();
    try (var special = resource("abyss-essence.catalog.json");
        var raw = resource("abyss-essence.raw.json")) {
      var extended = ItemCatalogLoader.addSpecial(ordinary, special, raw);
      assertThat(extended.modifiers()).hasSize(184);
      assertThat(extended.compatibleSnapshotIds())
          .containsExactly(ordinary.metadata().snapshotId());
      assertThat(extended.metadata().snapshotId()).hasSizeLessThanOrEqualTo(120);
      assertThat(extended.metadata().prefixCount()).isEqualTo(84);
      assertThat(extended.metadata().suffixCount()).isEqualTo(100);
      assertThat(extended.metadata().prefixWeight()).isEqualTo(63700);
      assertThat(extended.metadata().suffixWeight()).isEqualTo(84500);
      assertThat(extended.base()).isEqualTo(ordinary.base());
      ordinary
          .modifiers()
          .forEach((id, definition) -> assertThat(extended.find(id)).contains(definition));
      assertThat(extended.modifiers().values().stream().filter(d -> d.weight() > 0).count())
          .isEqualTo(182);
      assertThat(extended.modifiers().values().stream().filter(d -> d.weight() == 0).toList())
          .hasSize(2)
          .allSatisfy(
              d -> {
                assertThat(d.familyIds()).containsExactly("EssenceAbyss");
                assertThat(d.requiredItemLevel()).isEqualTo(1);
                assertThat(d.stats())
                    .containsExactly(
                        new ModifierDefinition.StatRange("essence_abyss_guaranteed_pick", 1, 1));
              });
    }
    try (var special = resource("abyss-essence.catalog.json")) {
      assertThatThrownBy(
              () ->
                  ItemCatalogLoader.addSpecial(
                      ordinary, special, new ByteArrayInputStream(new byte[0])))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("checksum");
    }
  }

  private static ItemCatalog load() throws Exception {
    try (var data = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json")) {
      return ItemCatalogLoader.load(data, raw, details);
    }
  }

  private static InputStream resource(String name) {
    return StockyMittsCatalogTest.class.getResourceAsStream("/catalog/stocky-mitts/" + name);
  }
}
