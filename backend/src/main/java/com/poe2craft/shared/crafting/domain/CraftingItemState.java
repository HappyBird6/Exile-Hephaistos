package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import java.util.Objects;

/** One current item with two views: concrete values and preserved explicit-slot identities. */
public record CraftingItemState(ItemState item, AbyssCraftingState slots) {
  public static CraftingItemState resolve(
      ItemState item, AbyssCraftingState slots, ItemCatalog catalog) {
    Objects.requireNonNull(item);
    var resolved = slots == null ? AbyssCraftingState.from(item, catalog) : slots;
    resolved.validate(catalog);
    if (!resolved.storedItem().equals(item))
      throw new IllegalArgumentException(
          "Concrete item and explicit slots describe different states");
    return new CraftingItemState(item, resolved);
  }
}
