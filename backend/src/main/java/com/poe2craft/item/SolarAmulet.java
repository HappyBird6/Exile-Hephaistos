package com.poe2craft.item;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SolarAmulet {
  public static final String BASE_ID = "Metadata/Items/Amulets/FourAmulet9";
  public static final String IMPLICIT_ID = "solar-amulet:implicit:spirit";
  public static final String SPIRIT_STAT = "base_spirit_from_equipment";
  public static final int DEFAULT_ITEM_LEVEL = 82;
  public static final int DEFAULT_SPIRIT = 15;

  private SolarAmulet() {}

  /** The fixed initial Spirit roll is a preset choice, not a randomly rolled result. */
  public static ItemState initial(ItemCatalog catalog) {
    return initial(catalog, DEFAULT_ITEM_LEVEL, DEFAULT_SPIRIT);
  }

  public static ItemState initial(ItemCatalog catalog, int itemLevel, int spirit) {
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            BASE_ID,
            itemLevel,
            ItemState.Rarity.NORMAL,
            List.of(new ModifierInstance(IMPLICIT_ID, Map.of(SPIRIT_STAT, (long) spirit))),
            List.of(),
            Set.of());
    var errors = new ItemStateValidator(catalog).validate(state);
    if (!errors.isEmpty())
      throw new IllegalArgumentException("Invalid Solar Amulet preset: " + errors);
    return state;
  }
}
