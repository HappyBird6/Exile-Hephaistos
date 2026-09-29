package com.poe2craft.item;

import static com.poe2craft.item.ItemStateValidator.Code.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemStateTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final ItemStateValidator validator = new ItemStateValidator(catalog);
  private final ItemState initial = SolarAmulet.initial(catalog);

  @Test
  void initialSolarAmuletHasAnIndependentSpiritImplicit() {
    assertThat(initial.itemLevel()).isEqualTo(82);
    assertThat(initial.rarity()).isEqualTo(ItemState.Rarity.NORMAL);
    assertThat(initial.implicits())
        .containsExactly(
            new ModifierInstance(SolarAmulet.IMPLICIT_ID, Map.of(SolarAmulet.SPIRIT_STAT, 15L)));
    assertThat(initial.explicits()).isEmpty();
    assertThat(validator.validate(initial)).isEmpty();
    assertThat(validator.slots(initial)).isEqualTo(new ItemStateValidator.AffixSlots(0, 0, 0, 0));
    assertThat(SolarAmulet.initial(catalog, 70, 10).itemLevel()).isEqualTo(70);
    assertThatThrownBy(() -> SolarAmulet.initial(catalog, 82, 16))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyMagicAndRareAreValidAndDistinctFromNormal() {
    var magic = state(ItemState.Rarity.MAGIC, List.of());
    var rare = state(ItemState.Rarity.RARE, List.of());
    assertThat(validator.validate(magic)).isEmpty();
    assertThat(validator.validate(rare)).isEmpty();
    assertThat(Set.of(initial, magic, rare)).hasSize(3);
    assertThat(validator.slots(magic).remainingPrefixes()).isEqualTo(1);
    assertThat(validator.slots(rare).remainingSuffixes()).isEqualTo(3);
  }

  @Test
  void magicCapacityDependsOnEachAffixSide() {
    var prefixes = distinct(ModifierDefinition.AffixType.PREFIX, 2);
    var suffix = distinct(ModifierDefinition.AffixType.SUFFIX, 1).getFirst();
    assertThat(
            validator.validate(state(ItemState.Rarity.MAGIC, List.of(prefixes.getFirst(), suffix))))
        .isEmpty();
    assertThat(codes(state(ItemState.Rarity.MAGIC, prefixes))).contains(AFFIX_CAPACITY_EXCEEDED);
    assertThat(codes(state(ItemState.Rarity.NORMAL, List.of(suffix))))
        .contains(AFFIX_CAPACITY_EXCEEDED);
  }

  @Test
  void sixAffixesFillRareSlotsAndASeventhIsRejected() {
    var affixes = new ArrayList<>(distinct(ModifierDefinition.AffixType.PREFIX, 3));
    affixes.addAll(distinct(ModifierDefinition.AffixType.SUFFIX, 3));
    var rare = state(ItemState.Rarity.RARE, affixes);
    assertThat(validator.validate(rare)).isEmpty();
    assertThat(validator.slots(rare).remainingPrefixes()).isZero();
    assertThat(validator.slots(rare).remainingSuffixes()).isZero();
    affixes.add(distinct(ModifierDefinition.AffixType.PREFIX, 4).getLast());
    assertThat(codes(state(ItemState.Rarity.RARE, affixes))).contains(AFFIX_CAPACITY_EXCEEDED);
  }

  @Test
  void differentTiersOfOneFamilyConflictButMatchingTagsDoNot() {
    var brute = instance("amulet:suffix:of-the-brute");
    var wrestler = instance("amulet:suffix:of-the-wrestler");
    assertThat(codes(state(ItemState.Rarity.RARE, List.of(brute, wrestler))))
        .contains(CONFLICTING_MODIFIERS);
    assertThat(codes(state(ItemState.Rarity.RARE, List.of(brute, brute))))
        .contains(DUPLICATE_MODIFIER);
    var dexterity =
        catalog.modifiers().values().stream()
            .filter(d -> d.familyIds().contains("Dexterity"))
            .findFirst()
            .orElseThrow();
    assertThat(
            validator.validate(
                state(ItemState.Rarity.RARE, List.of(brute, instance(dexterity.id())))))
        .isEmpty();
  }

  @Test
  void spiritImplicitDoesNotBlockAnExplicitSpiritPrefix() {
    var spirit =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.familyIds().contains("BaseSpirit"))
            .findFirst()
            .orElseThrow();
    var state = state(ItemState.Rarity.MAGIC, List.of(instance(spirit.id())));
    assertThat(validator.validate(state)).isEmpty();
    assertThat(validator.slots(state).usedPrefixes()).isEqualTo(1);
    assertThat(validator.slots(state).usedSuffixes()).isZero();
  }

  @Test
  void generationLevelChecksDoNotRewriteExistingItems() {
    var highLevel =
        catalog.modifiers().values().stream()
            .filter(
                d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.requiredItemLevel() == 82)
            .findFirst()
            .orElseThrow();
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            81,
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(instance(highLevel.id())),
            Set.of());
    assertThat(validator.validate(item)).isEmpty();
    assertThat(validator.validateForGeneration(item))
        .extracting(ItemStateValidator.Violation::code)
        .containsExactly(ITEM_LEVEL_TOO_LOW);
    assertThat(item.explicits()).hasSize(1);
    var atThreshold =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            82,
            item.rarity(),
            item.implicits(),
            item.explicits(),
            item.conditions());
    assertThat(validator.validateForGeneration(atThreshold)).isEmpty();
  }

  @Test
  void valueRangesAndExactStatKeysAreCheckedWithoutRepair() {
    var badValue =
        new ModifierInstance("amulet:suffix:of-the-brute", Map.of("additional_strength", 9L));
    assertThat(codes(state(ItemState.Rarity.RARE, List.of(badValue))))
        .containsExactly(VALUE_OUT_OF_RANGE);
    assertThat(badValue.values()).containsEntry("additional_strength", 9L);
    var missing = new ModifierInstance(badValue.modifierId(), Map.of());
    var extra =
        new ModifierInstance(badValue.modifierId(), Map.of("additional_strength", 5L, "wrong", 1L));
    assertThat(codes(state(ItemState.Rarity.RARE, List.of(missing))))
        .containsExactly(STAT_SET_MISMATCH);
    assertThat(codes(state(ItemState.Rarity.RARE, List.of(extra))))
        .containsExactly(STAT_SET_MISMATCH);
  }

  @Test
  void unknownModifiersWrongLayersAndMissingImplicitsAreExplicitErrors() {
    assertThat(
            codes(state(ItemState.Rarity.RARE, List.of(new ModifierInstance("unknown", Map.of())))))
        .containsExactly(UNKNOWN_MODIFIER);
    assertThat(codes(state(ItemState.Rarity.RARE, initial.implicits())))
        .contains(WRONG_MODIFIER_LAYER);
    var noImplicit =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(codes(noImplicit)).containsExactly(INVALID_IMPLICIT);
  }

  @Test
  void unsupportedBaseSnapshotAndConditionsAreReportedInsteadOfAssumed() {
    var unknown =
        new ItemState(
            "another-snapshot",
            "another-base",
            82,
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThat(codes(unknown)).contains(SNAPSHOT_MISMATCH, UNSUPPORTED_BASE, UNSUPPORTED_STATE);
    assertThat(unknown.conditions()).containsExactly(ItemState.Condition.CORRUPTED);
    assertThat(codes(state(ItemState.Rarity.UNIQUE, List.of()))).contains(UNSUPPORTED_STATE);
  }

  @Test
  void stateEqualityIgnoresDisplayOrderButPreservesRarityLevelAndRolls() {
    var mods = new ArrayList<>(distinct(ModifierDefinition.AffixType.SUFFIX, 2));
    var a = state(ItemState.Rarity.RARE, mods);
    Collections.reverse(mods);
    var b = state(ItemState.Rarity.RARE, mods);
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    mods.clear();
    assertThat(a.explicits()).hasSize(2);
    assertThatThrownBy(() -> a.explicits().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    var values = new HashMap<>(Map.of("additional_strength", 5L));
    var modifier = new ModifierInstance("amulet:suffix:of-the-brute", values);
    values.put("additional_strength", 8L);
    assertThat(modifier.values()).containsEntry("additional_strength", 5L);
    assertThatThrownBy(() -> modifier.values().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(modifier).isNotEqualTo(new ModifierInstance(modifier.modifierId(), values));
    assertThat(SolarAmulet.initial(catalog, 81, 15)).isNotEqualTo(initial);
    assertThat(SolarAmulet.initial(catalog, 82, 10)).isNotEqualTo(initial);
  }

  @Test
  void hybridDefinitionConsumesOneSlotAndEveryFamilyIsChecked() {
    var hybrid =
        new ModifierDefinition(
            "test:hybrid",
            "Synthetic hybrid",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("SyntheticFamily", "Strength"),
            1,
            1,
            1,
            "Synthetic two-stat modifier",
            List.of(
                new ModifierDefinition.StatRange("a", 1, 2),
                new ModifierDefinition.StatRange("b", 3, 4)),
            Set.of(),
            "test:fixture");
    var definitions = new ArrayList<>(catalog.modifiers().values());
    definitions.add(hybrid);
    var old = catalog.metadata();
    var metadata =
        new ItemCatalog.Metadata(
            old.snapshotId(),
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount() + 1,
            old.suffixCount(),
            old.prefixWeight() + 1,
            old.suffixWeight());
    var synthetic = new ItemStateValidator(new ItemCatalog(metadata, catalog.base(), definitions));
    var mod = new ModifierInstance(hybrid.id(), Map.of("a", 1L, "b", 4L));
    var item = state(ItemState.Rarity.RARE, List.of(mod));
    assertThat(synthetic.validate(item)).isEmpty();
    assertThat(synthetic.slots(item).usedPrefixes()).isEqualTo(1);
    assertThat(
            synthetic.validate(
                state(ItemState.Rarity.RARE, List.of(mod, instance("amulet:suffix:of-the-brute")))))
        .extracting(ItemStateValidator.Violation::code)
        .contains(CONFLICTING_MODIFIERS);
  }

  @Test
  void invalidStructuralInputsCannotBecomeStates() {
    assertThatThrownBy(() -> SolarAmulet.initial(catalog, 0, 15))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SolarAmulet.initial(catalog, 101, 15))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ModifierInstance("", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ModifierDefinition.StatRange("stat", 2, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private ItemState state(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        82,
        rarity,
        initial.implicits(),
        mods,
        Set.of());
  }

  private List<ItemStateValidator.Code> codes(ItemState state) {
    return validator.validate(state).stream().map(ItemStateValidator.Violation::code).toList();
  }

  private ModifierInstance instance(String id) {
    var definition = catalog.find(id).orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(id, values);
  }

  private List<ModifierInstance> distinct(ModifierDefinition.AffixType type, int count) {
    var families = new HashSet<String>();
    return catalog.modifiers().values().stream()
        .filter(d -> d.affixType() == type)
        .filter(
            d -> {
              if (d.familyIds().stream().anyMatch(families::contains)) return false;
              families.addAll(d.familyIds());
              return true;
            })
        .limit(count)
        .map(d -> instance(d.id()))
        .toList();
  }
}
