package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.item.testparser.ItemTextService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisteredTextMapperTest {
  private final ItemTextService parser = new ItemTextService();

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
