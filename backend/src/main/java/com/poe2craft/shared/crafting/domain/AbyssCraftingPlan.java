package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;

/** Finite next currency step; reveal and manual choices never appear as random branches. */
public record AbyssCraftingPlan(boolean available, String reason, List<Branch> branches) {
  public AbyssCraftingPlan {
    branches = List.copyOf(branches);
  }

  public record Branch(
      String removedSlotId,
      double removalProbability,
      AbyssCraftingState remaining,
      List<ModifierDefinition> candidates,
      boolean uniformSelection) {
    public Branch {
      candidates = List.copyOf(candidates);
    }
  }

  public static AbyssCraftingPlan blocked(String reason) {
    return new AbyssCraftingPlan(false, reason, List.of());
  }
}
