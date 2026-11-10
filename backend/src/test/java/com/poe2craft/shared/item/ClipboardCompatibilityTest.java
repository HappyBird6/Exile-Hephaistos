package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.*;
import org.junit.jupiter.api.Test;

class ClipboardCompatibilityTest {
  private final ItemTextService parser = new ItemTextService();

  private static String header(String itemClass, String rarity, String base) {
    return "Item Class: "
        + itemClass
        + "\nRarity: "
        + rarity
        + "\n"
        + (rarity.equals("Rare") ? "Test Item\n" : "")
        + base
        + "\n--------\nItem Level: 100\n--------\n";
  }

  @Test
  void gameLeechDecimalsRecoverPermyriadUnitsAndRejectChangedBounds() {
    var f = new StockyCraftingFixture();
    String source =
        header("Gloves", "Rare", "Stocky Mitts")
            + "{ Suffix Modifier \"of the Arid\" (Tier: 3) }\nLeech 6.3(6-6.9)% of Physical Attack Damage as Mana";
    var result = new SolarTextMapper(f.catalog).map(parser.parseText(source));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_mana_leech_from_physical_attack_damage_permyriad", 630L);
    assertThat(
            new SolarTextMapper(f.catalog)
                .map(parser.parseText(source.replace("(6-6.9)", "(0-100)")))
                .mapped())
        .isFalse();
  }

  @Test
  void perMinuteFractionsRoundTripWithoutRoundingToTheCardDisplay() {
    var catalog = ItemCatalogLoader.loadDefault();
    String source =
        header("Amulets", "Rare", "Solar Amulet")
            + "+15 to Spirit (implicit)\n--------\n{ Suffix Modifier \"of Convalescence\" (Tier: 4) }\n13.183333333333334 Life Regeneration per second";
    var definition =
        catalog.modifiers().values().stream()
            .filter(d -> d.id().equals("amulet:suffix:of-convalescence"))
            .findFirst()
            .orElseThrow();
    source = source.replace("(Tier: 4)", "(Tier: " + definition.tier() + ")");
    var result = new SolarTextMapper(catalog).map(parser.parseText(source));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_life_regeneration_rate_per_minute", 791L);
  }

  @Test
  void compoundImplicitLinesStayOneCatalogModifier() {
    var catalog = ItemCatalogLoader.loadTopBase("absent-amulet");
    var result =
        new SolarTextMapper(catalog)
            .map(
                parser.parseText(
                    header("Amulets", "Rare", "Absent Amulet")
                        + "-1 Prefix Modifier allowed (implicit)\n-1 Suffix Modifier allowed (implicit)"));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().implicits()).hasSize(1);
    assertThat(result.state().implicits().getFirst().values()).hasSize(2);
  }

  @Test
  void hiddenFixedWeaponStatsComeFromTheRegisteredBaseInsteadOfPrivateClipboardLines() {
    var catalog = ItemCatalogLoader.loadTopBase("bolting-quarterstaff");
    var result =
        new SolarTextMapper(catalog)
            .map(parser.parseText(header("Quarterstaves", "Normal", "Bolting Quarterstaff")));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().implicits())
        .singleElement()
        .satisfies(
            modifier ->
                assertThat(modifier.values())
                    .containsEntry(
                        "local_weapon_implicit_hidden_added_maximum_lightning_damage", 100L));
  }

  @Test
  void uniquelyIdentifiedRuneEffectsRestoreInstalledIdentityWithCanonicalSlotOrder() {
    var f = new StockyCraftingFixture();
    var result =
        new SolarTextMapper(f.catalog)
            .map(
                parser.parseText(
                    header("Gloves", "Normal", "Stocky Mitts")
                        + "Sockets: S S\n--------\n+22% to Fire Resistance (rune)\n+22% to Cold Resistance (rune)"));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().augmentSockets()).isEqualTo(2);
    assertThat(result.state().installedAugments())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactlyInAnyOrder("Perfect_Desert_Rune", "Perfect_Glacial_Rune");
  }

  @Test
  void equivalentAccuracyRunesRemainUnidentifiedAndDoNotBlockOrdinaryCrafting() {
    var catalog = ItemCatalogLoader.loadBow();
    var mapped =
        new SolarTextMapper(catalog)
            .map(
                parser.parseText(
                    header("Bows", "Normal", "Crude Bow")
                        + "Sockets: S\n--------\n+150 to Accuracy Rating (rune)"));
    assertThat(mapped.issues()).isEmpty();
    assertThat(mapped.state().installedAugments()).isEmpty();
    assertThat(mapped.state().observations().augmentEffects())
        .containsExactly("+150 to Accuracy Rating (rune)");
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    assertThat(
            simulator.actions(mapped.state(), Set.of()).stream()
                .filter(a -> a.action() == WorkbenchCurrency.ALCHEMY)
                .findFirst()
                .orElseThrow()
                .available())
        .isTrue();
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.weight() > 0
                        && d.stats().size() == 1)
            .findFirst()
            .orElseThrow();
    var original = mapped.state();
    var rare =
        new ItemState(
                original.snapshotId(),
                original.baseItemId(),
                100,
                ItemState.Rarity.RARE,
                original.implicits(),
                List.of(
                    new ModifierInstance(
                        definition.id(),
                        Map.of(
                            definition.stats().getFirst().id(),
                            definition.stats().getFirst().min()))),
                Set.of(),
                original.augmentSockets(),
                null)
            .withObservations(original.observations());
    var crafted = simulator.apply(rare, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(4));
    assertThat(crafted.applied()).isTrue();
    assertThat(crafted.state().observations()).isEqualTo(mapped.state().observations());
  }

  @Test
  void ambiguousRuneTotalsAreNotMisreadAsAUniqueHigherTier() {
    var result =
        SocketAugmentText.resolve(
            ItemCatalogLoader.loadBow().base().id(), 3, List.of("Adds 9 to 15 Cold Damage (rune)"));
    // Actual game clipboard ambiguity:
    // https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/issues/1675
    assertThat(result.installed()).isEmpty();
    assertThat(result.unidentified()).containsExactly("Adds 9 to 15 Cold Damage (rune)");
  }

  @Test
  void ordinaryGameLeechWithoutAdvancedMetadataRestoresItsExactRollAndFracture() {
    var fixture = new StockyCraftingFixture();
    var result =
        new SolarTextMapper(fixture.catalog)
            .map(
                parser.parseText(
                    header("Gloves", "Rare", "Stocky Mitts")
                        + "Leech 6.61% of Physical Attack Damage as Mana (fractured)"));
    // Actual wording: PathOfBuilding-PoE2/spec/System/TestItemParse_spec.lua.
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits())
        .singleElement()
        .satisfies(
            modifier -> {
              assertThat(modifier.values())
                  .containsEntry("base_mana_leech_from_physical_attack_damage_permyriad", 661L);
              assertThat(modifier.fractured()).isTrue();
            });
  }

  @Test
  void advancedCatalystMetadataIdentifiesRawRollsButOrdinaryScaledValuesAreNotGuessed() {
    var catalog = ItemCatalogLoader.loadDefault();
    String text =
        header("Amulets", "Rare", "Solar Amulet")
            + "Quality (Life Modifiers): +20% (augmented)\n--------\n+15 to Spirit (implicit)\n--------\n{ Prefix Modifier \"Healthy\" (Tier: 8) — Life — 20% Increased }\n+22(20-29) to maximum Life";
    var result = new SolarTextMapper(catalog).map(parser.parseText(text));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().catalystQuality())
        .isEqualTo(new CatalystQuality(CatalystQuality.Type.FLESH, 20));
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_maximum_life", 22L);
    assertThat(
            new SolarTextMapper(catalog)
                .map(parser.parseText(text.replace(" — Life — 20% Increased", "")))
                .mapped())
        .isFalse();
  }

  @Test
  void editingAffixesKeepsUnidentifiedRuneEffectsAndDoesNotTreatThemAsAnInstalledIdentity() {
    var catalog = ItemCatalogLoader.loadBow();
    String text =
        header("Bows", "Normal", "Crude Bow")
            + "Sockets: S\n--------\n+150 to Accuracy Rating (rune)";
    var mapper = new SolarTextMapper(catalog);
    var before = mapper.map(parser.parseText(text)).state();
    var edited =
        ItemTextEditor.merge(
            before,
            mapper.map(parser.parseText(text.replace("Item Level: 100", "Item Level: 99"))),
            catalog);
    assertThat(edited.issues()).isEmpty();
    assertThat(edited.state().itemLevel()).isEqualTo(99);
    assertThat(edited.state().observations()).isEqualTo(before.observations());
    assertThat(edited.state().installedAugments()).isEmpty();
    var changed =
        ItemTextEditor.merge(
            before, mapper.map(parser.parseText(text.replace("150", "300"))), catalog);
    assertThat(changed.mapped()).isFalse();
  }
}
