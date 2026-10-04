package com.poe2craft.item;

import com.poe2craft.item.ModifierDefinition.AffixType;
import com.poe2craft.item.ModifierDefinition.Layer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validation does not repair input or remove unsupported modifiers. */
public final class ItemStateValidator {
  private final ItemCatalog catalog;

  public ItemStateValidator(ItemCatalog catalog) {
    this.catalog = java.util.Objects.requireNonNull(catalog);
  }

  public List<Violation> validate(ItemState state) {
    var errors = new ArrayList<Violation>();
    if (state.baseItemId().equals(SapphireJewel.BASE_ID)
        && (state.rarity() != ItemState.Rarity.MAGIC && state.rarity() != ItemState.Rarity.RARE
            || state.augmentSockets() != null
            || state.explicits().size() > 1
            || state.explicits().stream().anyMatch(ModifierInstance::fractured)))
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE,
              "",
              "Sapphire editing supports only Magic/Rare and at most one reviewed existing suffix"));
    if (state.catalystQuality() != null) {
      int cap = state.baseItemId().equals(SolarAmulet.BASE_ID) ? 40 : 20;
      if (!CatalystQuality.supportedBase(state.baseItemId())
          || state.catalystQuality().amount() > cap)
        errors.add(
            new Violation(
                Code.UNSUPPORTED_STATE,
                "",
                "Unsupported catalyst quality or amount above reviewed reachable maximum"));
    }
    if (!state.snapshotId().equals(catalog.metadata().snapshotId())) {
      errors.add(new Violation(Code.SNAPSHOT_MISMATCH, "", "State and catalog snapshots differ"));
    }
    if (!state.baseItemId().equals(catalog.base().id())) {
      errors.add(
          new Violation(Code.UNSUPPORTED_BASE, "", "Only this snapshot's base is supported"));
    }
    if (state.rarity() == ItemState.Rarity.UNIQUE || !state.conditions().isEmpty()) {
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE, "", "Unique or special item conditions are not supported"));
    }
    if (catalog.base().hasImplicit()
        ? state.implicits().size() != 1
            || !state
                .implicits()
                .getFirst()
                .modifierId()
                .equals(catalog.base().implicitModifierId())
        : !state.implicits().isEmpty()) {
      errors.add(new Violation(Code.INVALID_IMPLICIT, "", "Implicits must match the catalog base"));
    }
    validateModifiers(state.implicits(), Layer.IMPLICIT, errors);
    validateModifiers(state.explicits(), Layer.EXPLICIT, errors);
    if (state.implicits().stream().anyMatch(ModifierInstance::fractured)
        || state.explicits().stream().filter(ModifierInstance::fractured).count() > 1
        || (state.explicits().stream().anyMatch(ModifierInstance::fractured)
            && state.rarity() != ItemState.Rarity.RARE)) {
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE,
              "",
              "Only one fractured explicit on a Rare item is supported"));
    }
    if (state.rarity() != ItemState.Rarity.UNIQUE
        && state.snapshotId().equals(catalog.metadata().snapshotId())
        && state.baseItemId().equals(catalog.base().id())) {
      var slots = slots(state);
      if (slots.usedPrefixes() > slots.maxPrefixes()
          || slots.usedSuffixes() > slots.maxSuffixes()) {
        errors.add(
            new Violation(Code.AFFIX_CAPACITY_EXCEEDED, "", "Explicit affix capacity exceeded"));
      }
    }
    return List.copyOf(errors);
  }

  private void validateModifiers(
      List<ModifierInstance> instances, Layer layer, List<Violation> errors) {
    var ids = new HashSet<String>();
    var families = new HashSet<String>();
    for (var instance : instances) {
      String id = instance.modifierId();
      if (!ids.add(id))
        errors.add(new Violation(Code.DUPLICATE_MODIFIER, id, "Modifier appears twice"));
      var definition = catalog.find(id).orElse(null);
      if (definition == null) {
        errors.add(
            new Violation(Code.UNKNOWN_MODIFIER, id, "Modifier is not in the Base snapshot"));
        continue;
      }
      if (definition.layer() != layer) {
        errors.add(
            new Violation(Code.WRONG_MODIFIER_LAYER, id, "Implicit and explicit layers differ"));
      }
      if (layer == Layer.EXPLICIT) {
        if (definition.familyIds().stream().anyMatch(families::contains)) {
          errors.add(
              new Violation(Code.CONFLICTING_MODIFIERS, id, "Explicit modifier families overlap"));
        }
        families.addAll(definition.familyIds());
      }
      Set<String> expected = new HashSet<>();
      for (var stat : definition.stats()) {
        expected.add(stat.id());
        Long value = instance.values().get(stat.id());
        if (value != null && !stat.contains(value)) {
          errors.add(new Violation(Code.VALUE_OUT_OF_RANGE, id, "Stat out of range: " + stat.id()));
        }
      }
      if (!instance.values().keySet().equals(expected)) {
        errors.add(new Violation(Code.STAT_SET_MISMATCH, id, "Missing or additional stat values"));
      }
    }
  }

  /** Separate from existing-item validation: only newly generated affixes obey this check. */
  public List<Violation> validateForGeneration(ItemState state) {
    var errors = new ArrayList<>(validate(state));
    for (var modifier : state.explicits()) {
      catalog
          .find(modifier.modifierId())
          .ifPresent(
              definition -> {
                if (definition.requiredItemLevel() > state.itemLevel()) {
                  errors.add(
                      new Violation(
                          Code.ITEM_LEVEL_TOO_LOW,
                          definition.id(),
                          "New modifier requires a higher item level"));
                }
              });
    }
    return List.copyOf(errors);
  }

  /** Counts instances, not display lines. Only call for known, supported base states. */
  public AffixSlots slots(ItemState state) {
    if (!state.snapshotId().equals(catalog.metadata().snapshotId())
        || !state.baseItemId().equals(catalog.base().id())
        || state.rarity() == ItemState.Rarity.UNIQUE) {
      throw new IllegalArgumentException("Cannot resolve slots for this state");
    }
    int prefixes = 0, suffixes = 0;
    for (var instance : state.explicits()) {
      var definition = catalog.find(instance.modifierId()).orElse(null);
      if (definition != null && definition.layer() == Layer.EXPLICIT) {
        if (definition.affixType() == AffixType.PREFIX) prefixes++;
        if (definition.affixType() == AffixType.SUFFIX) suffixes++;
      }
    }
    int maxPrefixes =
        switch (state.rarity()) {
          case NORMAL -> 0;
          case MAGIC -> catalog.base().magicPrefixes();
          case RARE -> catalog.base().rarePrefixes();
          default -> throw new IllegalArgumentException("Unsupported rarity");
        };
    int maxSuffixes =
        switch (state.rarity()) {
          case NORMAL -> 0;
          case MAGIC -> catalog.base().magicSuffixes();
          case RARE -> catalog.base().rareSuffixes();
          default -> throw new IllegalArgumentException("Unsupported rarity");
        };
    return new AffixSlots(prefixes, suffixes, maxPrefixes, maxSuffixes);
  }

  public record AffixSlots(int usedPrefixes, int usedSuffixes, int maxPrefixes, int maxSuffixes) {
    public int remainingPrefixes() {
      return Math.max(0, maxPrefixes - usedPrefixes);
    }

    public int remainingSuffixes() {
      return Math.max(0, maxSuffixes - usedSuffixes);
    }
  }

  public record Violation(Code code, String modifierId, String message) {}

  public enum Code {
    SNAPSHOT_MISMATCH,
    UNSUPPORTED_BASE,
    UNSUPPORTED_STATE,
    INVALID_IMPLICIT,
    UNKNOWN_MODIFIER,
    DUPLICATE_MODIFIER,
    WRONG_MODIFIER_LAYER,
    CONFLICTING_MODIFIERS,
    STAT_SET_MISMATCH,
    VALUE_OUT_OF_RANGE,
    AFFIX_CAPACITY_EXCEEDED,
    ITEM_LEVEL_TOO_LOW
  }
}
