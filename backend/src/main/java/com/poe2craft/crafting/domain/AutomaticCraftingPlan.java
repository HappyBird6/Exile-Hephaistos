package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.*;

/** Shared eligible branches, retaining every removal and sourced fixed outcome. */
public record AutomaticCraftingPlan(
    Status status,
    String reason,
    List<Branch> branches,
    boolean uniformCandidates,
    String sourceUrl) {
  public enum Status {
    READY,
    UNAVAILABLE,
    UNSUPPORTED
  }

  public record Branch(ItemState remaining, List<ModifierDefinition> candidates) {
    public Branch {
      candidates = List.copyOf(candidates);
    }
  }

  public AutomaticCraftingPlan {
    branches = List.copyOf(branches);
  }
}
