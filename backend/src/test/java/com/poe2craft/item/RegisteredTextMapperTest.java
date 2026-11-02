package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.item.testparser.ItemTextService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisteredTextMapperTest {
  private final ItemTextService parser = new ItemTextService();

  @Test
  void legacySkillObservationsUseBundledProofAndRetainUnknownLevel() {
    assertThat(
            BaseRegistry.observedGrantedSkill(
                "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3",
                "Grants Skill: Level 12 Mana Drain"))
        .isEqualTo(new ItemObservations.GrantedSkill("Mana Drain", 12));
    assertThat(
            BaseRegistry.observedGrantedSkill(
                "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1",
                "Grants Skill: Skeletal Warrior"))
        .isEqualTo(new ItemObservations.GrantedSkill("Skeletal Warrior", null));
  }

  @Test
  void shrineSkillResolvesIdentityAndRetainsExplicitLevelWithoutInventingIt() {
    var mapper =
        new SolarTextMapper(
            List.of(
                ItemCatalogLoader.loadTopBase("shrine-fire"),
                ItemCatalogLoader.loadTopBase("shrine-ice"),
                ItemCatalogLoader.loadTopBase("shrine-lightning")));
    for (var pair :
        java.util.Map.of(
                "Fire", "shrine-fire", "Ice", "shrine-ice", "Lightning", "shrine-lightning")
            .entrySet()) {
      String source =
          "Item Class: Sceptres\nRarity: Normal\nShrine Sceptre\n--------\nGrants Skill: Level 17 Purity of "
              + pair.getKey()
              + "\nItem Level: 82";
      var mapped = mapper.map(parser.parseText(source));
      assertThat(mapped.issues()).isEmpty();
      assertThat(mapped.state().baseItemId())
          .isEqualTo(BaseRegistry.topBases().get(pair.getValue()).id());
      assertThat(mapped.state().observations().grantedSkills())
          .containsExactly(new ItemObservations.GrantedSkill("Purity of " + pair.getKey(), 17));
      assertThat(
              mapper
                  .map(parser.parseText(source.replace("Level 17 ", "")))
                  .state()
                  .observations()
                  .grantedSkills()
                  .getFirst()
                  .level())
          .isNull();
      assertThat(mapper.map(parser.parseText(source.replace("Level 17", "Level 0"))).state())
          .isNull();
    }
  }

  @Test
  void socketObservationsDoNotPretendToBeArtificerCapacityAndRuneEffectsRemainSeparate() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    String source =
        "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nSockets: S S S S\nItem Level: 82\n--------\n20% increased Armour (rune)";
    var mapped = new SolarTextMapper(catalog).map(parser.parseText(source));
    assertThat(mapped.issues()).isEmpty();
    assertThat(mapped.state().augmentSockets()).isNull();
    assertThat(mapped.state().observations().socketCount()).isEqualTo(4);
    assertThat(mapped.state().observations().augmentEffects())
        .containsExactly("20% increased Armour (rune)");
    assertThat(mapped.state().explicits()).isEmpty();
  }

  @Test
  void everyRegisteredTopBaseWithoutImplicitAcceptsExactNormalIdentity() {
    int checked = 0;
    for (var base : BaseRegistry.topBases().values()) {
      var catalog = ItemCatalogLoader.loadTopBase(base.key());
      if (catalog.base().hasImplicit()) continue;
      String source =
          "Item Class: "
              + base.policy().itemClass()
              + "\nRarity: Normal\n"
              + base.name()
              + "\n--------\nItem Level: 82";
      var result = new SolarTextMapper(catalog).map(parser.parseText(source));
      assertThat(result.issues()).as(base.key()).isEmpty();
      assertThat(result.state().baseItemId()).isEqualTo(base.id());
      assertThat(result.state().snapshotId()).isEqualTo(catalog.metadata().snapshotId());
      assertThat(result.state().explicits()).isEmpty();
      checked++;
    }
    assertThat(checked).isGreaterThan(20);
  }

  @Test
  void derivedPropertiesRemainInOriginalDocumentAndDoNotBecomeRawModifiers() {
    String source =
        "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nArmour: 684 (augmented)\nQuality: +20%\nItem Level: 82";
    var parsed = parser.parseText(source);
    var result = new SolarTextMapper(ItemCatalogLoader.loadTopBase("soldier")).map(parsed);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits()).isEmpty();
    assertThat(parsed.text().originalText()).isEqualTo(source);
    assertThat(
            new SolarTextMapper(ItemCatalogLoader.loadTopBase("soldier"))
                .map(parser.parseText(source.replace("684 (augmented)", "unknown")))
                .state())
        .isNull();
  }

  @Test
  void onlyExactCatalogGrantedSkillLinesAreAccepted() {
    String source =
        "Item Class: Sceptres\nRarity: Normal\nOmen Sceptre\n--------\nSpirit: 100\nGrants Skill: Malice\nItem Level: 82";
    var mapper = new SolarTextMapper(ItemCatalogLoader.loadTopBase("omen"));
    assertThat(mapper.map(parser.parseText(source)).issues()).isEmpty();
    assertThat(mapper.map(parser.parseText(source.replace("Malice", "Unknown"))).state()).isNull();
  }

  @Test
  void ambiguousNameNeverChoosesAnElementOrFirstCatalog() {
    var fire = ItemCatalogLoader.loadTopBase("shrine-fire");
    var ice = ItemCatalogLoader.loadTopBase("shrine-ice");
    var mapper = new SolarTextMapper(List.of(fire, ice));
    var result =
        mapper.map(
            parser.parseText(
                "Item Class: Sceptres\nRarity: Normal\nShrine Sceptre\n--------\nItem Level: 82"));
    assertThat(result.mapped()).isFalse();
    assertThat(result.state()).isNull();
  }

  @Test
  void mismatchAndUnknownPropertiesDoNotProduceLossyState() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    var mapper = new SolarTextMapper(catalog);
    var source =
        "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nItem Level: 82";
    assertThat(mapper.map(parser.parseText(source.replace("Body Armours", "Amulets"))).mapped())
        .isFalse();
    var withQuality = source + "\nQuality: +20%";
    var parsed = parser.parseText(withQuality);
    assertThat(mapper.map(parsed).state().equipmentQuality()).isEqualTo(20);
    assertThat(parsed.text().originalText()).isEqualTo(withQuality);
    for (var suffix : List.of("\nDesecrated Prefix", "\nDesecrated Suffix", "\nCorrupted")) {
      var special = parser.parseText(source + suffix);
      assertThat(mapper.map(special).state()).isNull();
      assertThat(special.text().originalText()).isEqualTo(source + suffix);
    }
  }

  @Test
  void compoundRangesBindByVerifiedEndpointsRatherThanAlphabeticStatOrder() {
    var catalog = ItemCatalogLoader.loadRing();
    var definition = catalog.find("iron-ring:prefix:burnished").orElseThrow();
    String source =
        "Item Class: Rings\nRarity: Rare\nTest Ring\nIron Ring\n--------\nItem Level: 82"
            + "\n--------\nAdds 1 to 4 Physical Damage to Attacks (implicit)\n--------\n"
            + "{ Prefix Modifier \""
            + definition.name()
            + "\" (Tier: "
            + definition.tier()
            + ") }\n"
            + "Adds 3(2-3) to 5(4-6) Physical Damage to Attacks (fractured)";
    var result = new SolarTextMapper(catalog).map(parser.parseText(source));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits())
        .containsExactly(
            new ModifierInstance(
                definition.id(),
                java.util.Map.of(
                    "attack_minimum_added_physical_damage",
                    3L,
                    "attack_maximum_added_physical_damage",
                    5L),
                true));
    assertThat(result.state().implicits()).hasSize(1);
    assertThat(
            new SolarTextMapper(catalog)
                .map(
                    parser.parseText(
                        source.replace("{ Prefix Modifier", "{ Desecrated Prefix Modifier")))
                .mapped())
        .isFalse();
    assertThat(
            new SolarTextMapper(catalog)
                .map(parser.parseText(source.replace("5(4-6)", "5(3-6)")))
                .mapped())
        .isFalse();
  }

  @Test
  void multilineAdvancedModifierRemainsOneAffixAndOrphanMetadataStillBlocks() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    var definition = catalog.find("rusted-cuirass:prefix:oyster-s").orElseThrow();
    String source =
        "Item Class: Body Armours\nRarity: Rare\nTest Armour\nSoldier Cuirass\n--------\nItem Level: 82\n--------\n"
            + "{ Prefix Modifier \""
            + definition.name()
            + "\" (Tier: "
            + definition.tier()
            + ") }\n"
            + "10% increased Armour\n+8 to maximum Life";
    var parsed = parser.parseText(source);
    var result = new SolarTextMapper(catalog).map(parsed);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits())
        .containsExactly(
            new ModifierInstance(
                definition.id(),
                java.util.Map.of(
                    "base_maximum_life", 8L, "local_physical_damage_reduction_rating_+%", 10L)));
    assertThat(parsed.text().originalText()).isEqualTo(source);
    assertThat(
            new SolarTextMapper(catalog)
                .map(parser.parseText(source + "\n{ Prefix Modifier \"Unknown\" (Tier: 1) }"))
                .mapped())
        .isFalse();
  }

  @Test
  void qualityAndUnrevealedSlotsSurviveMappingAndAbyssRoundTrip() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    var source =
        "Item Class: Body Armours\nRarity: Rare\nTest Armour\nSoldier Cuirass\n--------\nQuality: +20% (augmented)\nItem Level: 82\n--------\nDesecrated Prefix\nDesecrated Suffix";
    var parsed = parser.parseText(source);
    var result = new SolarTextMapper(catalog).map(parsed);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().equipmentQuality()).isEqualTo(20);
    assertThat(result.state().desecrated()).hasSize(2).allMatch(s -> s.modifierId() == null);
    var abyss = com.poe2craft.crafting.domain.AbyssCraftingState.from(result.state(), catalog);
    assertThat(abyss.storedItem()).isEqualTo(result.state());
    assertThat(parsed.text().originalText()).isEqualTo(source);
    var tooMany = source + "\nDesecrated Prefix\nDesecrated Prefix\nDesecrated Prefix";
    assertThat(new SolarTextMapper(catalog).map(parser.parseText(tooMany)).mapped()).isFalse();
  }

  @Test
  void revealedDesecratedHybridRetainsOneMarkedSlot() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    var d = catalog.find("rusted-cuirass:prefix:oyster-s").orElseThrow();
    String source =
        "Item Class: Body Armours\nRarity: Rare\nTest Armour\nSoldier Cuirass\n--------\nItem Level: 82\n--------\n"
            + "{ Desecrated Prefix Modifier \""
            + d.name()
            + "\" (Tier: "
            + d.tier()
            + ") }\n"
            + "10% increased Armour\n+8 to maximum Life";
    var result = new SolarTextMapper(catalog).map(parser.parseText(source));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().desecrated()).hasSize(1);
    assertThat(result.state().desecrated().getFirst().modifierId()).isEqualTo(d.id());
    assertThat(
            com.poe2craft.crafting.domain.AbyssCraftingState.from(result.state(), catalog)
                .storedItem())
        .isEqualTo(result.state());
  }
}
