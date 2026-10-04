package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;

/** User-confirmed cap loss preserves quality; catalyst reuse remains a simulator policy. */
public enum QualityCapChangePolicy {
  PRESERVE_EXISTING,
  REJECT_OVERCAP;
  public static final String VERSION = "user-confirmed-quality-cap-preserve-v2";
  public static final QualityCapChangePolicy DEFAULT = PRESERVE_EXISTING;

  public ItemState afterAcceptedOperation(ItemState state, ItemCatalog catalog) {
    return state;
  }

  public int catalystAmount(ItemState state, int cap) {
    return Math.max(cap, state.catalystQuality() == null ? 0 : state.catalystQuality().amount());
  }
}
