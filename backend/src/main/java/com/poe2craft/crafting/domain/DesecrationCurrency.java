package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;

/**
 * Conditional manual outcome. Side/removal are observed inputs, never a fabricated distribution.
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

  public static AbyssCraftingState apply(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog) {
    if (currency == null
        || !currency.matches("(Gnawed|Preserved|Ancient)_(Rib|Jawbone|Collarbone)"))
      throw new IllegalArgumentException("Unsupported desecration currency");
    if (activeOmens != null && !activeOmens.isEmpty())
      throw new IllegalArgumentException("Deactivate omens before recording a bone-only outcome");
    if (item.rarity() != ItemState.Rarity.RARE
        || !item.conditions().isEmpty()
        || !item.desecrated().isEmpty())
      throw new IllegalArgumentException(
          "Requires an identified, unmodified-condition Rare item without desecration");
    var itemClass = BaseRegistry.itemClass(item.baseItemId());
    var classes =
        currency.endsWith("_Rib") ? ARMOUR : currency.endsWith("_Jawbone") ? WEAPONS : JEWELLERY;
    if (itemClass == null || !classes.contains(itemClass))
      throw new IllegalArgumentException("Currency does not match the equipment class");
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
    var slots = new ArrayList<>(remaining.slots());
    slots.add(
        new AbyssCraftingState.Slot(
            "desecrated-" + UUID.randomUUID(),
            side,
            null,
            true,
            currency.startsWith("Ancient_") ? 40 : null));
    var result = new AbyssCraftingState(remaining.item(), slots, null);
    result.validate(catalog);
    return result;
  }
}
