package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarTextMapper;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.*;
import org.junit.jupiter.api.Test;

class BaseSelectionPolicyTest {
  private ItemState root(ItemCatalog catalog, ItemState.Rarity rarity) {
    var implicit = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
    var values = new LinkedHashMap<String, Long>();
    implicit.stats().forEach(stat -> values.put(stat.id(), stat.max()));
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        82,
        rarity,
        List.of(new ModifierInstance(implicit.id(), values)),
        List.of(),
        Set.of());
  }

  @Test
  void rareOnlyBasesInitializeRareAndRejectNormalOrMagicAtBothConcreteAndBucketBoundaries() {
    var config = new CatalogConfiguration();
    var solar = config.itemCatalog(config.workbenchDefinitions());
    var workbench =
        config.workbenchService(
            solar, config.workbenchSimulator(solar, config.craftingEngine(solar)));
    for (String key :
        List.of(
            "dusk-amulet",
            "gloam-amulet",
            "penumbra-amulet",
            "tenebrous-amulet",
            "corona-amulet",
            "sinew-belt",
            "invoking-belt",
            "forking-belt")) {
      var catalog = ItemCatalogLoader.loadTopBase(key);
      assertThat(workbench.initial(key, 82).state().rarity())
          .as(key)
          .isEqualTo(ItemState.Rarity.RARE);
      assertThat(new ItemStateValidator(catalog).validate(root(catalog, ItemState.Rarity.RARE)))
          .as(key)
          .isEmpty();
      for (var rarity : List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC)) {
        var invalid = root(catalog, rarity);
        assertThat(new ItemStateValidator(catalog).validate(invalid))
            .anyMatch(issue -> issue.message().contains("requires Rare"));
        assertThatThrownBy(() -> new CraftingEngine(catalog).validate(StateBucket.from(invalid)))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
    var corona = ItemCatalogLoader.loadTopBase("corona-amulet");
    String text =
        "Item Class: Amulets\nRarity: Normal\nCorona Amulet\n--------\nItem Level: 82\n--------\nThis item gains bonuses from Socketed Items as though it was a Helmet (implicit)";
    var result = new SolarTextMapper(corona).map(new ItemTextService().parseText(text));
    assertThat(result.mapped()).isFalse();
    assertThat(result.issues()).anyMatch(issue -> issue.message().contains("requires Rare"));
  }

  @Test
  void asymmetricLimitsComeFromSourceAndSameNameArmourIsMappedOnlyByValidatedImplicit() {
    for (var pair :
        Map.of(
                "dusk-amulet",
                List.of(4, 2),
                "gloam-amulet",
                List.of(2, 4),
                "penumbra-amulet",
                List.of(5, 1),
                "tenebrous-amulet",
                List.of(1, 5))
            .entrySet()) {
      var catalog = ItemCatalogLoader.loadTopBase(pair.getKey());
      var slots = new ItemStateValidator(catalog).slots(root(catalog, ItemState.Rarity.RARE));
      assertThat(slots.maxPrefixes()).isEqualTo(pair.getValue().getFirst());
      assertThat(slots.maxSuffixes()).isEqualTo(pair.getValue().getLast());
    }
    var catalogs =
        List.of("shrouded-mail-fire", "shrouded-mail-cold", "shrouded-mail-lightning").stream()
            .map(ItemCatalogLoader::loadTopBase)
            .toList();
    var mapper = new SolarTextMapper(catalogs);
    for (String element : List.of("Fire", "Cold", "Lightning")) {
      String text =
          "Item Class: Body Armours\nRarity: Normal\nShrouded Mail\n--------\nItem Level: 82\n--------\n+23% to "
              + element
              + " Resistance (implicit)";
      var result = mapper.map(new ItemTextService().parseText(text));
      assertThat(result.issues()).as(element).isEmpty();
      assertThat(result.state().baseItemId())
          .isEqualTo(
              BaseRegistry.require("shrouded-mail-" + element.toLowerCase(Locale.ROOT)).id());
    }
  }

  @Test
  void
      userExcludedRingIdentitiesRemainRegisteredForExistingRecordsAndTemporarySkillsUseSourceCandidates() {
    for (String key : List.of("kinetic", "mnemonic", "vitalic", "two-stone-fire-cold")) {
      var base = BaseRegistry.require(key);
      assertThat(base.policy().newSelection()).isFalse();
      assertThat(ItemCatalogLoader.loadTopBase(key).base().id()).isEqualTo(base.id());
    }
    assertThat(
            BaseRegistry.recognizesGrantedSkill(
                BaseRegistry.require("lament-amulet").id(), "Grants Skill: Level 11 Arctic Armour"))
        .isTrue();
    assertThat(
            BaseRegistry.recognizesGrantedSkill(
                BaseRegistry.require("portent-amulet").id(), "Grants Skill: Level 11 Wolf Pack"))
        .isTrue();
    assertThat(
            BaseRegistry.recognizesGrantedSkill(
                BaseRegistry.require("absent-amulet").id(),
                "Grants Skill: Level 12 Cast on Elemental Ailment"))
        .isTrue();
  }
}
