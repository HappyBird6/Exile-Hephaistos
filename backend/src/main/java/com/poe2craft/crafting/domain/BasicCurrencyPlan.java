package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.List;

/** No-omen one-removal/one-addition plan shared by sampling and exhaustive enumeration. */
record BasicCurrencyPlan(boolean available, String reason, List<Branch> branches) {
  static final List<WorkbenchCurrency> ACTIONS =
      List.of(
          WorkbenchCurrency.TRANSMUTATION,
          WorkbenchCurrency.GREATER_TRANSMUTATION,
          WorkbenchCurrency.PERFECT_TRANSMUTATION,
          WorkbenchCurrency.AUGMENTATION,
          WorkbenchCurrency.GREATER_AUGMENTATION,
          WorkbenchCurrency.PERFECT_AUGMENTATION,
          WorkbenchCurrency.REGAL,
          WorkbenchCurrency.GREATER_REGAL,
          WorkbenchCurrency.PERFECT_REGAL,
          WorkbenchCurrency.EXALTED,
          WorkbenchCurrency.GREATER_EXALTED,
          WorkbenchCurrency.PERFECT_EXALTED,
          WorkbenchCurrency.ANNULMENT,
          WorkbenchCurrency.CHAOS,
          WorkbenchCurrency.GREATER_CHAOS,
          WorkbenchCurrency.PERFECT_CHAOS);

  BasicCurrencyPlan {
    branches = List.copyOf(branches);
  }

  record Branch(
      ModifierInstance removed, ItemState remaining, List<ModifierDefinition> candidates) {
    Branch {
      candidates = List.copyOf(candidates);
    }
  }

  List<ModifierInstance> removals() {
    return branches.stream().map(Branch::removed).filter(java.util.Objects::nonNull).toList();
  }

  List<ModifierDefinition> candidatesFor(ItemState remaining) {
    return branches.stream()
        .filter(b -> b.remaining().equals(remaining))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("State is outside the basic currency plan"))
        .candidates();
  }
}
