package com.poe2craft.workbench.display;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChaosOptionDisplayTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Path SOURCE = Path.of("../frontend/src/shared/i18n/modifierTemplates.json");

  private static byte[] bytes() throws Exception {
    return Files.readAllBytes(SOURCE);
  }

  private static String hash(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static DisplayTemplates templates() throws Exception {
    var bytes = bytes();
    return DisplayTemplates.parse(bytes, hash(bytes), JSON);
  }

  private static ModifierDefinition definition(String id) throws Exception {
    var root = JSON.readTree(bytes());
    var binding = root.path("definitions").path(id);
    var stats = new ArrayList<ModifierDefinition.StatRange>();
    for (var stat : binding.path("stats"))
      stats.add(
          new ModifierDefinition.StatRange(
              stat.path("id").textValue(),
              stat.path("min").longValue(),
              stat.path("max").longValue()));
    return new ModifierDefinition(
        id,
        root.path("templates")
            .path("en")
            .path(binding.path("template").textValue())
            .path("name")
            .textValue(),
        ModifierDefinition.Layer.EXPLICIT,
        ModifierDefinition.AffixType.SUFFIX,
        Set.of(id),
        1,
        1,
        1,
        binding.path("englishText").textValue(),
        stats,
        Set.of(),
        "https://example.test/source");
  }

  private static ModifierDefinition simple(
      String id,
      ModifierDefinition.AffixType side,
      String text,
      List<ModifierDefinition.StatRange> stats,
      Set<String> tags) {
    return new ModifierDefinition(
        id,
        "Name must not match",
        ModifierDefinition.Layer.EXPLICIT,
        side,
        Set.of(id),
        1,
        1,
        1,
        text,
        stats,
        tags,
        "https://example.test/source");
  }

  private static ItemCatalog catalog(List<ModifierDefinition> definitions, String base) {
    var rows = new ArrayList<>(definitions);
    for (var side :
        List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX))
      if (rows.stream().noneMatch(d -> d.affixType() == side))
        rows.add(
            simple(
                "unused:" + side,
                side,
                "unused",
                List.of(new ModifierDefinition.StatRange("unused", 1, 1)),
                Set.of()));
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            "display-test",
            "2026-11-04",
            "https://example.test/source",
            "TEST_ONLY",
            "0".repeat(64),
            "0".repeat(64),
            (int)
                rows.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count(),
            (int)
                rows.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                    .count(),
            rows.stream()
                .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                .mapToLong(ModifierDefinition::weight)
                .sum(),
            rows.stream()
                .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                .mapToLong(ModifierDefinition::weight)
                .sum()),
        new ItemCatalog.BaseItem(
            base, "Ignored item name", "https://example.test/base", "", 1, 1, 3, 3),
        rows);
  }

  private static ItemState state(ItemCatalog catalog, List<ModifierInstance> explicits) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        82,
        ItemState.Rarity.RARE,
        List.of(),
        explicits,
        Set.of());
  }

  @Test
  void realDivisorUsesCurrentLocaleAndDoesNotMutateCanonicalValues() throws Exception {
    var definition = definition("amulet:suffix:of-convalescence");
    var catalog = catalog(List.of(definition), "display:test");
    var item =
        state(
            catalog,
            List.of(
                new ModifierInstance(
                    definition.id(), Map.of("base_life_regeneration_rate_per_minute", 791L))));
    String before = JSON.writeValueAsString(item);
    var display = new ChaosOptionDisplay(templates());
    assertThat(display.format(item, catalog, DisplayLocale.EN).groups().getFirst().text())
        .isEqualTo("13.2 Life Regeneration per second");
    assertThat(display.format(item, catalog, DisplayLocale.ES).groups().getFirst().text())
        .contains("13,2");
    for (var locale : DisplayLocale.values()) {
      var result = display.format(item, catalog, locale);
      assertThat(result.supported()).isTrue();
      assertThat(result.groups()).hasSize(1);
      assertThat(result.issues()).isEmpty();
    }
    assertThat(JSON.writeValueAsString(item)).isEqualTo(before);
  }

  @Test
  void keepsModifierAndLfBoundariesAndNormalFracturedPrefixSuffixOrder() throws Exception {
    var suffix =
        simple(
            "a:suffix",
            ModifierDefinition.AffixType.SUFFIX,
            "suffix",
            List.of(new ModifierDefinition.StatRange("s", 1, 1)),
            Set.of());
    var prefix =
        simple(
            "b:prefix",
            ModifierDefinition.AffixType.PREFIX,
            "line1\nline2",
            List.of(new ModifierDefinition.StatRange("p", 1, 1)),
            Set.of("crafted"));
    var fractured =
        simple(
            "c:fracture",
            ModifierDefinition.AffixType.SUFFIX,
            "fracture",
            List.of(new ModifierDefinition.StatRange("f", 1, 1)),
            Set.of());
    var catalog = catalog(List.of(suffix, prefix, fractured), "display:test");
    var item =
        state(
                catalog,
                List.of(
                    new ModifierInstance(suffix.id(), Map.of("s", 1L)),
                    new ModifierInstance(prefix.id(), Map.of("p", 1L)),
                    new ModifierInstance(fractured.id(), Map.of("f", 1L), true)))
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "visible", ModifierDefinition.AffixType.PREFIX, prefix.id()),
                    new ItemState.DesecratedSlot(
                        "hidden", ModifierDefinition.AffixType.PREFIX, null)));
    var result = new ChaosOptionDisplay(templates()).format(item, catalog, DisplayLocale.EN);
    assertThat(result.groups())
        .extracting(ChaosOptionDisplay.Group::id)
        .containsExactly(fractured.id(), prefix.id(), suffix.id());
    assertThat(result.groups().get(1).lines()).containsExactly("line1", "line2");
    assertThat(result.groups().getFirst().fractured()).isTrue();
    assertThat(result.groups())
        .allSatisfy(
            g ->
                assertThat(g.text())
                    .doesNotContain("Ignored item name", "Name must not match", "hidden", "P1"));
  }

  @Test
  void preservesRawFallbackButBlocksUsingItsNameAndStatIdsAsOptionSentences() throws Exception {
    var definition =
        simple(
            "raw:compound",
            ModifierDefinition.AffixType.PREFIX,
            "unknown compound",
            List.of(
                new ModifierDefinition.StatRange("internal_stat_a", 1, 2),
                new ModifierDefinition.StatRange("internal_stat_b", 3, 4)),
            Set.of());
    var catalog = catalog(List.of(definition), "display:test");
    var item =
        state(
            catalog,
            List.of(
                new ModifierInstance(
                    definition.id(), Map.of("internal_stat_a", 1L, "internal_stat_b", 3L))));
    var result = new ChaosOptionDisplay(templates()).format(item, catalog, DisplayLocale.KO);
    assertThat(result.supported()).isFalse();
    assertThat(result.groups().getFirst().text())
        .contains("Name must not match", "internal stat a", "source units");
    assertThat(result.issues())
        .anyMatch(i -> i.code() == ChaosOptionDisplay.Code.SOURCE_UNITS_FALLBACK && i.blocking());
  }

  @Test
  void rejectsUnvalidatedStateAndReportsSanctifiedOutsideChaosScope() throws Exception {
    var definition =
        simple(
            "single",
            ModifierDefinition.AffixType.PREFIX,
            "(1—2) effect",
            List.of(new ModifierDefinition.StatRange("v", 1, 2)),
            Set.of());
    var catalog = catalog(List.of(definition), "display:test");
    var display = new ChaosOptionDisplay(templates());
    assertThatThrownBy(
            () ->
                display.format(
                    state(catalog, List.of(new ModifierInstance("missing", Map.of("v", 1L)))),
                    catalog,
                    DisplayLocale.EN))
        .isInstanceOf(IllegalArgumentException.class);
    var item = state(catalog, List.of(new ModifierInstance(definition.id(), Map.of("v", 1L))));
    var sanctified =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            item.explicits(),
            Set.of(ItemState.Condition.SANCTIFIED));
    var result = display.format(sanctified, catalog, DisplayLocale.EN);
    assertThat(result.supported()).isFalse();
    assertThat(result.groups()).isEmpty();
    assertThat(result.issues())
        .extracting(ChaosOptionDisplay.Issue::code)
        .containsExactly(ChaosOptionDisplay.Code.SANCTIFIED_UNSUPPORTED);
  }

  @Test
  void signedSourceDivisorPreservesReducedEffectSentence() throws Exception {
    var definition = definition("rusted-cuirass:suffix:of-sealing");
    var catalog = catalog(List.of(definition), "display:test");
    var item =
        state(
            catalog,
            List.of(new ModifierInstance(definition.id(), Map.of("self_bleed_duration_+%", -38L))));
    var display = new ChaosOptionDisplay(templates());
    assertThat(display.format(item, catalog, DisplayLocale.EN).groups().getFirst().text())
        .isEqualTo("38% reduced Duration of Bleeding on You");
    for (var locale : DisplayLocale.values()) {
      var result = display.format(item, catalog, locale);
      assertThat(result.supported()).isTrue();
      assertThat(result.groups().getFirst().text()).contains("38");
    }
    assertThat(item.explicits().getFirst().values()).containsEntry("self_bleed_duration_+%", -38L);
  }

  @Test
  void excludesActualImplicitAndKeepsCanonicalQualityProjectionUnchanged() throws Exception {
    var catalog = ItemCatalogLoader.loadDefault();
    var root = SolarAmulet.initial(catalog);
    var definition = catalog.find("amulet:prefix:athlete-s").orElseThrow();
    var item =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(new ModifierInstance(definition.id(), Map.of("base_maximum_life", 121L))),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.FLESH, 20));
    String before = JSON.writeValueAsString(item);
    var result = new ChaosOptionDisplay(templates()).format(item, catalog, DisplayLocale.EN);
    assertThat(result.groups()).hasSize(1);
    assertThat(result.groups().getFirst().text()).isEqualTo("+145 to maximum Life");
    assertThat(result.groups())
        .extracting(ChaosOptionDisplay.Group::id)
        .doesNotContain(root.implicits().getFirst().modifierId());
    assertThat(JSON.writeValueAsString(item)).isEqualTo(before);
  }

  @Test
  void validJewelUsesExistingPrefixEffectAndTypedQualityWithoutChangingRolls() throws Exception {
    var catalog = ItemCatalogLoader.loadSapphire();
    var item =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(
                new ModifierInstance(
                    "sapphire:prefix:acrimonious", Map.of("display_source_value", 11L)),
                new ModifierInstance(
                    "sapphire:suffix:of-enchanting", Map.of("display_cast_speed_percent", 3L)),
                new ModifierInstance(
                    "sapphire:crafted:CraftedJewelPrefixEffect",
                    Map.of("display_source_value", 40L))),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.SIBILANT, 20));
    String before = JSON.writeValueAsString(item);
    var result = new ChaosOptionDisplay(templates()).format(item, catalog, DisplayLocale.EN);
    assertThat(result.supported()).isTrue();
    assertThat(result.groups())
        .extracting(ChaosOptionDisplay.Group::text)
        .containsExactly(
            "15% increased Magnitude of Ailments you inflict",
            "40% increased Effect of Prefixes", "4% increased Cast Speed");
    assertThat(result.issues())
        .anyMatch(i -> i.code() == ChaosOptionDisplay.Code.JEWEL_EFFECT_AND_QUALITY_PROVISIONAL);
    assertThat(JSON.writeValueAsString(item)).isEqualTo(before);
  }

  @Test
  void verifiesTemplateBytesAndNeverRepairsMissingTranslationsOrBindingMismatch() throws Exception {
    var source = bytes();
    assertThatThrownBy(() -> DisplayTemplates.parse(source, "0".repeat(64), JSON))
        .isInstanceOf(IllegalArgumentException.class);
    JsonNode changed = JSON.readTree(source);
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.path("templates")).remove("ko");
    var invalid = JSON.writeValueAsBytes(changed);
    assertThatThrownBy(() -> DisplayTemplates.parse(invalid, hash(invalid), JSON))
        .isInstanceOf(IllegalArgumentException.class);
    var definition = definition("amulet:suffix:of-convalescence");
    var changedDefinition =
        simple(
            definition.id(),
            definition.affixType(),
            "Changed (786—1080)",
            definition.stats(),
            Set.of());
    var catalog = catalog(List.of(changedDefinition), "display:test");
    var result =
        new ChaosOptionDisplay(templates())
            .format(
                state(
                    catalog,
                    List.of(
                        new ModifierInstance(
                            definition.id(),
                            Map.of("base_life_regeneration_rate_per_minute", 791L)))),
                catalog,
                DisplayLocale.KO);
    assertThat(result.groups().getFirst().text()).isEqualTo("Changed 791");
    assertThat(result.issues()).anyMatch(i -> i.code() == ChaosOptionDisplay.Code.ENGLISH_FALLBACK);
  }
}
