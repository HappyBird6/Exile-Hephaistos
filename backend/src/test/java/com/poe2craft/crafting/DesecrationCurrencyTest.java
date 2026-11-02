package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

class DesecrationCurrencyTest {
  final HysteriaEssenceTest f = new HysteriaEssenceTest();

  AbyssCraftingState apply(ItemState item, String currency, AffixType side, String removed) {
    return DesecrationCurrency.apply(item, currency, side, removed, Set.of(), f.catalog);
  }

  @Test
  void necromancyForcesItsSideAndRejectsConflictsWithoutChangingInput() {
    var item = f.rare(82, "", "amulet:prefix:hale");
    for (var entry :
        Map.of(
                "Omen_of_Sinistral_Necromancy",
                AffixType.PREFIX,
                "Omen_of_Dextral_Necromancy",
                AffixType.SUFFIX)
            .entrySet()) {
      var result =
          DesecrationCurrency.apply(
              item, "Ancient_Collarbone", null, null, Set.of(entry.getKey()), f.catalog);
      assertThat(result.slots().getLast().side()).isEqualTo(entry.getValue());
      assertThat(result.slots().getLast().minimumModifierLevel()).isEqualTo(40);
      assertThat(AbyssCraftingState.from(result.storedItem(), f.catalog)).isEqualTo(result);
      var opposite = entry.getValue() == AffixType.PREFIX ? AffixType.SUFFIX : AffixType.PREFIX;
      assertThatThrownBy(
              () ->
                  DesecrationCurrency.apply(
                      item,
                      "Ancient_Collarbone",
                      opposite,
                      null,
                      Set.of(entry.getKey()),
                      f.catalog))
          .isInstanceOf(IllegalArgumentException.class);
    }
    for (var omens :
        List.of(
            Set.of("Omen_of_Sinistral_Necromancy", "Omen_of_Dextral_Necromancy"),
            Set.of("Omen_of_Sinistral_Necromancy", "Omen_of_Putrefaction")))
      assertThatThrownBy(
              () ->
                  DesecrationCurrency.apply(
                      item, "Preserved_Collarbone", AffixType.PREFIX, null, omens, f.catalog))
          .isInstanceOf(IllegalArgumentException.class);
    assertThat(item.desecrated()).isEmpty();
    assertThat(item.explicits()).hasSize(1);
  }

  @Test
  void markedItemRequiresSeparateHigherTierRuleRatherThanKeepingTheMark() {
    var base = BaseRegistry.require("adherent");
    var catalog = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadTopBase(base.key());
    var marked =
        catalog.modifiers().values().stream()
            .filter(d -> d.text().equals("Bears the Mark of the Abyssal Lord"))
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    marked.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    var item =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(new ModifierInstance(marked.id(), values)),
            Set.of());
    String c = base.policy().itemClass();
    String kind =
        Set.of("Amulets", "Rings", "Belts").contains(c)
            ? "Collarbone"
            : Set.of("Body Armours", "Boots", "Gloves", "Helmets", "Shields", "Foci", "Bucklers")
                    .contains(c)
                ? "Rib"
                : "Jawbone";
    assertThatThrownBy(
            () ->
                DesecrationCurrency.apply(
                    item, "Preserved_" + kind, AffixType.PREFIX, null, Set.of(), catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("higher-tier");
  }

  @Test
  void allRegisteredTopEquipmentFamiliesAcceptMatchingBoneTiersOnBothSides() {
    int count = 0;
    for (var base : BaseRegistry.topBases().values()) {
      var catalog = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadTopBase(base.key());
      var implicits = new ArrayList<ModifierInstance>();
      if (catalog.base().hasImplicit()) {
        var d = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
        var values = new HashMap<String, Long>();
        d.stats().forEach(stat -> values.put(stat.id(), stat.min()));
        implicits.add(new ModifierInstance(d.id(), values));
      }
      var item =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              64,
              ItemState.Rarity.RARE,
              implicits,
              List.of(),
              Set.of());
      var c = base.policy().itemClass();
      String kind =
          Set.of("Amulets", "Rings", "Belts").contains(c)
              ? "Collarbone"
              : Set.of("Body Armours", "Boots", "Gloves", "Helmets", "Shields", "Foci", "Bucklers")
                      .contains(c)
                  ? "Rib"
                  : "Jawbone";
      for (String tier : List.of("Gnawed", "Preserved", "Ancient"))
        for (var side : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
          var result =
              DesecrationCurrency.apply(item, tier + "_" + kind, side, null, Set.of(), catalog);
          assertThat(result.storedItem().baseItemId()).isEqualTo(item.baseItemId());
          assertThat(result.slots()).hasSize(1);
          count++;
        }
    }
    assertThat(count).isEqualTo(117 * 3 * 2);
  }

  @Test
  void preservesFracturesObservationsAndOccupancyAcrossStorage() {
    var before =
        f.rare(82, "amulet:prefix:hale", "amulet:prefix:hale")
            .withObservations(new ItemObservations(null, List.of(), List.of()));
    var next = apply(before, "Preserved_Collarbone", AffixType.SUFFIX, null);
    assertThat(next.storedItem().explicits()).isEqualTo(before.explicits());
    assertThat(next.storedItem().observations()).isEqualTo(before.observations());
    assertThat(next.storedItem().desecrated()).hasSize(1);
    assertThat(next.count(AffixType.SUFFIX)).isEqualTo(1);
    assertThat(AbyssCraftingState.from(next.storedItem(), f.catalog)).isEqualTo(next);
    assertThatThrownBy(
            () -> apply(next.storedItem(), "Preserved_Collarbone", AffixType.SUFFIX, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validatesTierClassLevelAndObservedOutcomeWithoutMutatingInput() {
    var before = f.rare(82, "", "amulet:prefix:hale");
    for (String invalid :
        List.of(
            "Gnawed_Collarbone",
            "Preserved_Rib",
            "Preserved_Jawbone",
            "Altered_Collarbone",
            "unknown"))
      assertThatThrownBy(() -> apply(before, invalid, AffixType.SUFFIX, null))
          .isInstanceOf(IllegalArgumentException.class);
    assertThat(apply(f.rare(64, ""), "Gnawed_Collarbone", AffixType.PREFIX, null).slots())
        .hasSize(1);
    assertThatThrownBy(() -> apply(before, "Preserved_Collarbone", null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> apply(before, "Preserved_Collarbone", AffixType.SUFFIX, "explicit-0"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                DesecrationCurrency.apply(
                    before,
                    "Preserved_Collarbone",
                    AffixType.SUFFIX,
                    null,
                    Set.of("Omen_of_Putrefaction"),
                    f.catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(before.explicits()).hasSize(1);
    assertThat(before.desecrated()).isEmpty();
  }

  @Test
  void ancientConstraintSurvivesRestoreAndRejectsLowLevelOffer() {
    var next = apply(f.rare(82, ""), "Ancient_Collarbone", AffixType.PREFIX, null);
    var stored = next.storedItem();
    assertThat(stored.desecrated().getFirst().minimumModifierLevel()).isEqualTo(40);
    var restored = AbyssCraftingState.from(stored, f.catalog);
    var low = f.rare(82, "", "amulet:prefix:hale").explicits().getFirst();
    assertThatThrownBy(
            () ->
                restored.observe(
                    restored.slots().getFirst().id(), List.of(low, low, low), f.catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(restored.offer()).isNull();
    var d =
        f.catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.affixType() == AffixType.PREFIX
                        && m.requiredItemLevel() >= 40
                        && m.requiredItemLevel() <= 82)
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    d.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    var high = new ModifierInstance(d.id(), values);
    var chosen =
        restored
            .observe(restored.slots().getFirst().id(), List.of(high, high, high), f.catalog)
            .choose(0, f.catalog);
    assertThat(chosen.storedItem().desecrated().getFirst().minimumModifierLevel()).isEqualTo(40);
    assertThat(AbyssCraftingState.from(chosen.storedItem(), f.catalog)).isEqualTo(chosen);
  }

  @Test
  void fullItemRequiresObservedRemovalAndProtectsFractureAndAffixCaps() {
    var before =
        f.rare(
            82,
            "amulet:prefix:hale",
            "amulet:prefix:hale",
            "amulet:prefix:adept-s",
            "amulet:prefix:azure",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin",
            "amulet:suffix:of-the-salamander");
    String locked =
        "explicit-"
            + java.util.stream.IntStream.range(0, before.explicits().size())
                .filter(i -> before.explicits().get(i).fractured())
                .findFirst()
                .orElseThrow();
    int freeIndex =
        java.util.stream.IntStream.range(0, before.explicits().size())
            .filter(i -> before.explicits().get(i).modifierId().equals("amulet:prefix:azure"))
            .findFirst()
            .orElseThrow();
    String free = "explicit-" + freeIndex;
    assertThatThrownBy(() -> apply(before, "Preserved_Collarbone", AffixType.PREFIX, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> apply(before, "Preserved_Collarbone", AffixType.PREFIX, locked))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> apply(before, "Preserved_Collarbone", AffixType.SUFFIX, free))
        .isInstanceOf(IllegalArgumentException.class);
    var next = apply(before, "Preserved_Collarbone", AffixType.PREFIX, free);
    assertThat(next.slots()).hasSize(6);
    assertThat(next.storedItem().explicits())
        .contains(
            before.explicits().stream()
                .filter(ModifierInstance::fractured)
                .findFirst()
                .orElseThrow())
        .doesNotContain(before.explicits().get(freeIndex));
  }
}
