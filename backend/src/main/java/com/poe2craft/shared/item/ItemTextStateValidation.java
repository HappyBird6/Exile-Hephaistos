package com.poe2craft.shared.item;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Clipboard mapping validates observed data and reserved/revealed slots without losing either. */
final class ItemTextStateValidation {
  private ItemTextStateValidation() {}

  static List<String> validate(ItemState state, ItemCatalog catalog) {
    var projection =
        !state.desecrated().isEmpty()
                && state.conditions().equals(Set.of(ItemState.Condition.CORRUPTED))
            ? state.withConditions(Set.of())
            : state;
    var issues =
        new ArrayList<>(
            new ItemStateValidator(catalog)
                .validate(projection).stream().map(ItemStateValidator.Violation::message).toList());
    if (issues.isEmpty() && !state.desecrated().isEmpty()) {
      try {
        // Unresolved augment observations remain readable and are retained on the mapped item.
        // Their validity is checked above; the slot engine only needs the concrete slot state.
        AbyssCraftingState.from(state.withObservations(null), catalog);
      } catch (IllegalArgumentException invalid) {
        issues.add("Invalid desecrated slot, reveal pool or item state.");
      }
    }
    return List.copyOf(issues);
  }
}
