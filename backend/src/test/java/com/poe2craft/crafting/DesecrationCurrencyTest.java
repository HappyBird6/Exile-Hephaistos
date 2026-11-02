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

  ModifierInstance minimum(ModifierDefinition definition) {
    var values = new HashMap<String, Long>();
    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    return new ModifierInstance(definition.id(), values);
  }

  @Test
  void boneRemainsUnrevealedAcrossEligibilityLookupAndRestoreUntilExplicitSelection() {
    var catalog = ManualRevealCatalog.enrich(f.catalog);
    var original = f.rare(82, "", "amulet:prefix:hale");
    var hidden =
        DesecrationCurrency.apply(
            original, "Preserved_Collarbone", AffixType.SUFFIX, null, Set.of(), catalog);
    var slot = hidden.slots().getLast();
    var stored = hidden.storedItem();
    assertThat(hidden.offer()).isNull();
    assertThat(slot.modifier()).isNull();
    assertThat(stored.explicits()).isEqualTo(original.explicits());
    assertThat(stored.desecrated()).hasSize(1);
    assertThat(stored.desecrated().getFirst().modifierId()).isNull();
    var restored = AbyssCraftingState.from(stored, catalog);
    var eligible = restored.eligibleRevealIds(slot.id(), catalog);
    assertThat(eligible).isNotEmpty();
    assertThat(restored).isEqualTo(hidden);
    assertThat(restored.storedItem()).isEqualTo(stored);
    var selected =
        restored.selectDirect(
            slot.id(), minimum(catalog.find(eligible.getFirst()).orElseThrow()), catalog);
    assertThat(selected.slots().getLast().id()).isEqualTo(slot.id());
    assertThat(selected.slots().getLast().modifier()).isNotNull();
    assertThat(selected.offer()).isNull();
    assertThat(hidden.slots().getLast().modifier()).isNull();
    assertThat(original.desecrated()).isEmpty();
  }

  @Test
  void jewelCraniumDirectChoiceAndRestoreAllEightBases() {
    for (String key :
        List.of(
            "ruby",
            "emerald",
            "sapphire",
            "diamond",
            "time-lost-ruby",
            "time-lost-emerald",
            "time-lost-sapphire",
            "time-lost-diamond")) {
      var catalog =
          ManualRevealCatalog.enrich(
              (key.equals("sapphire")
                  ? com.poe2craft.item.infrastructure.ItemCatalogLoader.loadSapphire()
                  : com.poe2craft.item.infrastructure.ItemCatalogLoader.loadBasicJewel(key)));
      var implicits =
          catalog.base().hasImplicit()
              ? List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow()))
              : List.<ModifierInstance>of();
      var item =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              82,
              ItemState.Rarity.RARE,
              implicits,
              List.of(),
              Set.of());
      for (var side : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
        var hidden =
            DesecrationCurrency.apply(item, "Preserved_Cranium", side, null, Set.of(), catalog);
        var slot = hidden.slots().getFirst();
        var forgedPool =
            new AbyssCraftingState(
                hidden.item(),
                List.of(new AbyssCraftingState.Slot(slot.id(), side, null, true, null, "ALTERED")),
                null);
        assertThatThrownBy(() -> forgedPool.validate(catalog))
            .isInstanceOf(IllegalArgumentException.class);

        var ids = hidden.eligibleRevealIds(slot.id(), catalog);
        assertThat(ids).isNotEmpty();
        var definition =
            ids.stream()
                .map(id -> catalog.find(id).orElseThrow())
                .filter(d -> d.tags().contains("reveal_only"))
                .findFirst()
                .orElseThrow();
        assertThat(definition.weight()).isZero();
        var chosen = hidden.selectDirect(slot.id(), minimum(definition), catalog);
        assertThat(chosen.offer()).isNull();
        assertThat(chosen.storedItem().explicits()).contains(minimum(definition));
        assertThat(AbyssCraftingState.from(chosen.storedItem(), catalog)).isEqualTo(chosen);
        assertThatThrownBy(() -> chosen.selectDirect(slot.id(), minimum(definition), catalog))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(item.desecrated()).isEmpty();
      }
    }
  }

  @Test
  void alteredAndLichPoolsAreExplicitAndSurviveStorage() {
    var catalog = ManualRevealCatalog.enrich(f.catalog);
    var item = f.rare(82, "");
    var altered =
        DesecrationCurrency.apply(
            item, "Altered_Collarbone", AffixType.SUFFIX, null, Set.of(), catalog);
    var slot = altered.slots().getFirst();
    var extra =
        altered.eligibleRevealIds(slot.id(), catalog).stream()
            .map(id -> catalog.find(id).orElseThrow())
            .filter(d -> d.tags().contains("otherworldly"))
            .findFirst()
            .orElseThrow();
    var ordinary =
        DesecrationCurrency.apply(
            item, "Preserved_Collarbone", AffixType.SUFFIX, null, Set.of(), catalog);
    assertThat(ordinary.eligibleRevealIds(ordinary.slots().getFirst().id(), catalog))
        .doesNotContain(extra.id());
    var chosen = altered.selectDirect(slot.id(), minimum(extra), catalog);
    assertThat(chosen.storedItem().desecrated().getFirst().revealPool()).isEqualTo("ALTERED");
    assertThat(AbyssCraftingState.from(chosen.storedItem(), catalog)).isEqualTo(chosen);
    for (var entry :
        Map.of(
                "Omen_of_the_Liege",
                "amanamu_mod",
                "Omen_of_the_Sovereign",
                "ulaman_mod",
                "Omen_of_the_Blackblooded",
                "kurgal_mod")
            .entrySet()) {
      int available = 0;
      for (var side : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
        if (catalog.modifiers().values().stream()
            .noneMatch(d -> d.affixType() == side && d.tags().contains(entry.getValue()))) continue;
        var hidden =
            DesecrationCurrency.apply(
                item, "Preserved_Collarbone", side, null, Set.of(entry.getKey()), catalog);
        var ids = hidden.eligibleRevealIds(hidden.slots().getFirst().id(), catalog);
        assertThat(ids)
            .isNotEmpty()
            .allSatisfy(
                id -> assertThat(catalog.find(id).orElseThrow().tags()).contains(entry.getValue()));
        assertThat(AbyssCraftingState.from(hidden.storedItem(), catalog)).isEqualTo(hidden);
        var ordinaryDefinition =
            f.catalog.modifiers().values().stream()
                .filter(d -> d.affixType() == side && d.weight() > 0 && d.requiredItemLevel() <= 82)
                .findFirst()
                .orElseThrow();
        var hiddenSlot = hidden.slots().getFirst();
        var forged =
            new AbyssCraftingState(
                hidden.item(),
                List.of(
                    new AbyssCraftingState.Slot(
                        hiddenSlot.id(),
                        side,
                        minimum(ordinaryDefinition),
                        true,
                        null,
                        hiddenSlot.revealPool())),
                null);
        assertThatThrownBy(() -> forged.validate(catalog))
            .isInstanceOf(IllegalArgumentException.class);

        available++;
      }
      assertThat(available).isPositive();
    }
  }

  @Test
  void putrefactionRecordsSlotsCorruptionAndProtectionWithoutRandomness() {
    var catalog = ManualRevealCatalog.enrich(f.catalog);
    var item = f.rare(82, "", "amulet:prefix:hale");
    var sides = List.of(AffixType.PREFIX, AffixType.SUFFIX, AffixType.SUFFIX);
    var result =
        DesecrationCurrency.putrefy(
            item, "Preserved_Collarbone", sides, Set.of("Omen_of_Putrefaction"), catalog);
    assertThat(result.storedItem().conditions()).containsExactly(ItemState.Condition.CORRUPTED);
    assertThat(result.slots()).hasSize(3).allSatisfy(s -> assertThat(s.modifier()).isNull());
    assertThat(result.storedItem().explicits()).isEmpty();
    var slot = result.slots().getFirst();
    var id = result.eligibleRevealIds(slot.id(), catalog).getFirst();
    var chosen = result.selectDirect(slot.id(), minimum(catalog.find(id).orElseThrow()), catalog);
    assertThat(AbyssCraftingState.from(chosen.storedItem(), catalog)).isEqualTo(chosen);
    assertThatThrownBy(
            () ->
                DesecrationCurrency.putrefy(
                    f.rare(82, "amulet:prefix:hale", "amulet:prefix:hale"),
                    "Preserved_Collarbone",
                    sides,
                    Set.of("Omen_of_Putrefaction"),
                    catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                DesecrationCurrency.putrefy(
                    item,
                    "Preserved_Collarbone",
                    Collections.nCopies(4, AffixType.PREFIX),
                    Set.of("Omen_of_Putrefaction"),
                    catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(item.explicits()).hasSize(1);
    assertThat(item.conditions()).isEmpty();
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
    assertThat(count).isEqualTo(162 * 3 * 2);
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
        List.of("Gnawed_Collarbone", "Preserved_Rib", "Preserved_Jawbone", "unknown"))
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
