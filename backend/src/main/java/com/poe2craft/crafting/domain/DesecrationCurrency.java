package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Reveal choices remain manual. Automatic side/removal uses an explicit simulator policy, not
 * published game probabilities; explicit observed inputs remain supported.
 */
public final class DesecrationCurrency {
  private DesecrationCurrency() {}

  private static final Set<String> ARMOUR =
      Set.of("Body Armours", "Helmets", "Gloves", "Boots", "Shields", "Bucklers", "Foci");
  private static final Set<String> WEAPONS =
      Set.of(
          "Bows",
          "Crossbows",
          "Wands",
          "Staves",
          "Sceptres",
          "Quarterstaves",
          "Spears",
          "One Hand Maces",
          "Two Hand Maces",
          "One Hand Axes",
          "Two Hand Axes",
          "One Hand Swords",
          "Two Hand Swords",
          "Daggers",
          "Claws",
          "Quivers",
          "Flails",
          "Traps",
          "Talismans");
  private static final Set<String> JEWELLERY = Set.of("Amulet", "Amulets", "Rings", "Belts");

  public static boolean supportsRevealPool(String baseItemId, String pool) {
    if (pool == null) return true;
    var itemClass = BaseRegistry.itemClass(baseItemId);
    if (itemClass == null) return false;
    return "ALTERED".equals(pool)
        ? JEWELLERY.contains(itemClass)
        : (JEWELLERY.contains(itemClass) || WEAPONS.contains(itemClass));
  }

  public static AbyssCraftingState putrefy(
      ItemState item,
      String currency,
      List<AffixType> observedSides,
      Set<String> omens,
      ItemCatalog catalog) {
    if (!Set.of("Omen_of_Putrefaction").equals(omens)
        || observedSides == null
        || observedSides.isEmpty()
        || observedSides.size() > 6
        || observedSides.stream().anyMatch(s -> s == null || s == AffixType.NONE))
      throw new IllegalArgumentException("Record the observed slots for Putrefaction alone");
    if (item.explicits().stream()
        .anyMatch(
            m ->
                m.fractured()
                    || catalog
                        .find(m.modifierId())
                        .orElseThrow()
                        .text()
                        .equals("Bears the Mark of the Abyssal Lord")))
      throw new IllegalArgumentException(
          "Putrefaction with fractured or marked modifiers needs verified interaction rules");
    AbyssCraftingState.from(item, catalog);
    var empty =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            List.of(),
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality(),
            item.desecrated(),
            item.equipmentQuality(),
            item.observations());
    var seed = apply(empty, currency, observedSides.getFirst(), null, Set.of(), catalog);
    var slots = new ArrayList<AbyssCraftingState.Slot>();
    var model = seed.slots().getFirst();
    for (var side : observedSides)
      slots.add(
          new AbyssCraftingState.Slot(
              "desecrated-" + UUID.randomUUID(),
              side,
              null,
              true,
              model.minimumModifierLevel(),
              model.revealPool()));
    var corrupt =
        new ItemState(
            empty.snapshotId(),
            empty.baseItemId(),
            empty.itemLevel(),
            empty.rarity(),
            empty.implicits(),
            List.of(),
            Set.of(ItemState.Condition.CORRUPTED),
            empty.augmentSockets(),
            empty.catalystQuality(),
            List.of(),
            empty.equipmentQuality(),
            empty.observations());
    var result = new AbyssCraftingState(corrupt, slots, null);
    var cap = new ItemStateValidator(catalog).slots(empty);
    if (result.count(AffixType.PREFIX) > cap.maxPrefixes()
        || result.count(AffixType.SUFFIX) > cap.maxSuffixes())
      throw new IllegalArgumentException("Observed slots exceed item capacity");
    result.validate(catalog);
    for (var slot : slots)
      if (result.eligibleRevealIds(slot.id(), catalog).isEmpty())
        throw new IllegalArgumentException("No eligible reveal options for observed slots");
    return result;
  }

  public static AbyssCraftingState apply(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog) {
    return apply(
        item, currency, side, removedSlotId, activeOmens, catalog, RandomGenerator.getDefault());
  }

  /** Uniform eligible removal, then uniform eligible side; never samples a reveal modifier. */
  public static AbyssCraftingState apply(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog,
      RandomGenerator random) {
    Objects.requireNonNull(random);
    catalog = ManualRevealCatalog.enrich(catalog);
    if (side != null && removedSlotId != null)
      return applyEnriched(item, currency, side, removedSlotId, activeOmens, catalog);
    var before = AbyssCraftingState.from(item, catalog);
    var cap = new ItemStateValidator(catalog).slots(item);
    boolean full = before.slots().size() == cap.maxPrefixes() + cap.maxSuffixes();
    if (side != null && !full)
      return applyEnriched(item, currency, side, removedSlotId, activeOmens, catalog);
    List<String> removals = new ArrayList<>();
    if (removedSlotId != null && !removedSlotId.isBlank()) removals.add(removedSlotId);
    else if (full) before.removalCandidates(null).forEach(s -> removals.add(s.id()));
    else removals.add(null);
    var outcomes = new ArrayList<List<AbyssCraftingState>>();
    for (var removed : removals) {
      var availableSides = new ArrayList<AbyssCraftingState>();
      for (var candidateSide :
          side == null ? List.of(AffixType.PREFIX, AffixType.SUFFIX) : List.of(side)) {
        try {
          availableSides.add(
              applyEnriched(item, currency, candidateSide, removed, activeOmens, catalog));
        } catch (IllegalArgumentException unavailable) {
          // Probe complete validated outcomes: capacity, fractures, pool, level and omen
          // restrictions.
        }
      }
      if (!availableSides.isEmpty()) outcomes.add(availableSides);
    }
    if (outcomes.isEmpty()) throw new IllegalArgumentException("No eligible desecration outcome");
    var selectedRemoval = outcomes.get(random.nextInt(outcomes.size()));
    return selectedRemoval.get(random.nextInt(selectedRemoval.size()));
  }

  private static AbyssCraftingState applyEnriched(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog) {
    if (currency == null
        || !(currency.matches("(Gnawed|Preserved|Ancient)_(Rib|Jawbone|Collarbone)")
            || currency.equals("Preserved_Cranium")
            || currency.equals("Altered_Collarbone")))
      throw new IllegalArgumentException("Unsupported desecration currency");
    var omens = activeOmens == null ? Set.<String>of() : activeOmens;
    var sides =
        Map.of(
            "Omen_of_Sinistral_Necromancy", AffixType.PREFIX,
            "Omen_of_Dextral_Necromancy", AffixType.SUFFIX);
    var pools =
        Map.of(
            "Omen_of_the_Liege",
            "AMANAMU",
            "Omen_of_the_Blackblooded",
            "KURGAL",
            "Omen_of_the_Sovereign",
            "ULAMAN");
    var sideOmens = omens.stream().filter(sides::containsKey).toList();
    var poolOmens = omens.stream().filter(pools::containsKey).toList();
    if (sideOmens.size() > 1
        || poolOmens.size() > 1
        || omens.size() != sideOmens.size() + poolOmens.size())
      throw new IllegalArgumentException("Unsupported desecration omen combination");
    if (!sideOmens.isEmpty()) {
      var forced = sides.get(sideOmens.getFirst());
      if (side != null && side != forced)
        throw new IllegalArgumentException("Observed side conflicts with the Necromancy omen");
      side = forced;
    }
    if (item.rarity() != ItemState.Rarity.RARE
        || !item.conditions().isEmpty()
        || !item.desecrated().isEmpty())
      throw new IllegalArgumentException(
          "Requires an identified, unmodified-condition Rare item without desecration");
    var itemClass = BaseRegistry.itemClass(item.baseItemId());
    var classes =
        currency.endsWith("_Rib") ? ARMOUR : currency.endsWith("_Jawbone") ? WEAPONS : JEWELLERY;
    if (currency.equals("Preserved_Cranium")
        ? !BasicJewel.supportedCrafting(item.baseItemId())
        : itemClass == null || !classes.contains(itemClass))
      throw new IllegalArgumentException("Currency does not match the equipment class");
    if (!poolOmens.isEmpty()
        && (currency.equals("Altered_Collarbone")
            || (!WEAPONS.contains(itemClass) && !JEWELLERY.contains(itemClass))))
      throw new IllegalArgumentException(
          "Lich omens require an ordinary weapon or jewellery desecration");
    String pool =
        currency.equals("Altered_Collarbone")
            ? "ALTERED"
            : poolOmens.isEmpty() ? null : pools.get(poolOmens.getFirst());
    if (currency.startsWith("Gnawed_") && item.itemLevel() > 64)
      throw new IllegalArgumentException("Gnawed bones require item level 64 or lower");
    if (currency.startsWith("Ancient_") && item.itemLevel() < 40)
      throw new IllegalArgumentException(
          "No candidate can satisfy both item and minimum modifier levels");
    if (side == null || side == AffixType.NONE)
      throw new IllegalArgumentException("Observed affix side required");
    if (item.explicits().stream()
        .anyMatch(
            m ->
                catalog
                    .find(m.modifierId())
                    .map(d -> d.text().equals("Bears the Mark of the Abyssal Lord"))
                    .orElse(false)))
      throw new IllegalArgumentException(
          "Marked items require a separately verified higher-tier reveal rule");
    var before = AbyssCraftingState.from(item, catalog);
    var capacity = new ItemStateValidator(catalog).slots(item);
    boolean full = before.slots().size() == capacity.maxPrefixes() + capacity.maxSuffixes();
    if (full != (removedSlotId != null && !removedSlotId.isBlank()))
      throw new IllegalArgumentException("Record the removed modifier only when the item is full");
    var remaining = full ? before.remove(removedSlotId, null, catalog) : before;
    var currentCapacity = new ItemStateValidator(catalog).slots(remaining.knownItem());
    if (remaining.count(side)
        >= (side == AffixType.PREFIX
            ? currentCapacity.maxPrefixes()
            : currentCapacity.maxSuffixes()))
      throw new IllegalArgumentException("Observed side exceeds current affix capacity");
    var slots = new ArrayList<>(remaining.slots());
    slots.add(
        new AbyssCraftingState.Slot(
            "desecrated-" + UUID.randomUUID(),
            side,
            null,
            true,
            currency.startsWith("Ancient_") ? 40 : null,
            pool));
    var result = new AbyssCraftingState(remaining.item(), slots, null);
    result.validate(catalog);
    if (result.eligibleRevealIds(slots.getLast().id(), catalog).isEmpty())
      throw new IllegalArgumentException("No eligible reveal option for this item and currency");
    return result;
  }
}
