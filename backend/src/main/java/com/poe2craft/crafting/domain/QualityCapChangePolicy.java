package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;

/** Replaceable simulator choice; cap-removal behavior has not been verified in game. */
public enum QualityCapChangePolicy {
  CLAMP_TO_CURRENT_CAP,
  REJECT_OVERCAP;

  public static final String VERSION = "unverified-quality-cap-clamp-v1";
  public static final QualityCapChangePolicy DEFAULT = CLAMP_TO_CURRENT_CAP;

  public ItemState afterAcceptedOperation(ItemState state, ItemCatalog catalog) {
    if (state.catalystQuality() == null || this == REJECT_OVERCAP) return state;
    var untyped =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            state.itemLevel(),
            state.rarity(),
            state.implicits(),
            state.explicits(),
            state.conditions(),
            state.augmentSockets(),
            null);
    var limit = QualityLimitRules.describe(untyped, catalog);
    if (limit == null) throw new IllegalArgumentException("Quality cap unavailable");
    var quality = state.catalystQuality();
    int amount = Math.min(quality.amount(), limit.maximumQuality());
    if (amount == quality.amount()) return state;
    return new ItemState(
        state.snapshotId(),
        state.baseItemId(),
        state.itemLevel(),
        state.rarity(),
        state.implicits(),
        state.explicits(),
        state.conditions(),
        state.augmentSockets(),
        new CatalystQuality(quality.type(), amount));
  }
}
