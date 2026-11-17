package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.*;
import java.util.*;

/** One target-first replacement law for concrete items and unresolved desecrated slots. */
public final class EssenceReplacementRules {
  private EssenceReplacementRules() {}

  public record Outcome(ModifierDefinition target, List<AbyssCraftingState.Slot> removals) {
    public Outcome {
      removals = List.copyOf(removals);
    }
  }

  public record Plan(List<Outcome> outcomes, String reason) {
    public Plan {
      outcomes = List.copyOf(outcomes);
    }

    public boolean available() {
      return reason.isEmpty();
    }
  }

  public static Plan plan(
      AbyssCraftingState state,
      ItemCatalog catalog,
      List<ModifierDefinition> targets,
      WorkbenchOmen omen) {
    var limits = new ItemStateValidator(catalog).slots(state.knownItem());
    var removable = state.removalCandidates(omen == null ? null : omen.affix());
    var outcomes = new ArrayList<Outcome>();
    for (var target : targets) {
      int capacity =
          target.affixType() == ModifierDefinition.AffixType.PREFIX
              ? limits.maxPrefixes()
              : limits.maxSuffixes();
      boolean full = state.count(target.affixType()) >= capacity;
      var removals =
          removable.stream().filter(slot -> !full || slot.side() == target.affixType()).toList();
      if (removals.isEmpty())
        return new Plan(List.of(), "No removable modifier remains on the required affix side.");
      var values = new HashMap<String, Long>();
      target.stats().forEach(s -> values.put(s.id(), s.min()));
      // Do not silently condition away family conflicts or invent a retry distribution.
      for (var removed : removals) {
        try {
          state
              .remove(removed.id(), null, catalog)
              .append(removed.id(), new ModifierInstance(target.id(), values).asCrafted(), catalog);
        } catch (IllegalArgumentException conflict) {
          return new Plan(
              List.of(),
              "An essence removal branch conflicts with a guaranteed modifier family or item constraints.");
        }
      }
      outcomes.add(new Outcome(target, removals));
    }
    return new Plan(outcomes, outcomes.isEmpty() ? "No verified essence outcome." : "");
  }
}
