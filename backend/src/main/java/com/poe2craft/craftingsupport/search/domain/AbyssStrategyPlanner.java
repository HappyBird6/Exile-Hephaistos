package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph.Progress;
import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Manual-choice routes attached to an exact automatic graph node, with no reveal distribution. */
public final class AbyssStrategyPlanner {
  public enum Kind {
    COMPLETE_SIDE,
    BLOCK_HIGH_WEIGHT,
    SHARE_REMOVAL_RISK
  }

  public record Protection(int safeBefore, int totalBefore, int safeAfter, int totalAfter) {}

  public record Strategy(
      Kind kind,
      AbyssCraftingState prepared,
      WorkbenchCurrency nextCurrency,
      Set<String> activeOmens,
      Set<String> targetModifierIds,
      AbyssProbabilityComparison.Comparison modifierSelection,
      Protection removalProtection) {
    public Strategy {
      activeOmens = Set.copyOf(activeOmens);
      targetModifierIds = Set.copyOf(targetModifierIds);
    }
  }

  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final Function<ItemState, Progress> evaluate;

  public AbyssStrategyPlanner(
      ItemCatalog catalog, WorkbenchSimulator simulator, Function<ItemState, Progress> evaluate) {
    this.catalog = catalog;
    this.simulator = simulator;
    this.evaluate = evaluate;
  }

  public List<Strategy> recommend(ItemState item, List<AutomaticCraftingAction> inventory) {
    if (item.rarity() != ItemState.Rarity.RARE
        || item.explicits().size() >= 6
        || item.catalystQuality() != null
        || BasicJewel.supportedCrafting(item.baseItemId())) return List.of();
    var source = AbyssCraftingState.from(item, catalog);
    var before = evaluate.apply(item);
    if (before.goal() == Status.MATCH
        || before.goal() == Status.UNSUPPORTED
        || before.goal() == Status.UNKNOWN) return List.of();
    var useful =
        source.removalCandidates(null).stream()
            .filter(
                s ->
                    !preserves(
                        before, evaluate.apply(source.remove(s.id(), null, catalog).knownItem())))
            .map(AbyssCraftingState.Slot::id)
            .collect(Collectors.toSet());
    // Fractured useful affixes still count for side completion, but never for removal risk.
    for (var slot : source.slots()) {
      if (slot.modifier() != null && slot.modifier().fractured()) {
        var without =
            new AbyssCraftingState(
                source.item(),
                source.slots().stream().filter(s -> !s.id().equals(slot.id())).toList(),
                null);
        if (!preserves(before, evaluate.apply(without.knownItem()))) useful.add(slot.id());
      }
    }
    var result = new ArrayList<Strategy>();
    boolean exalted =
        inventory.stream()
            .anyMatch(a -> a.currency() == WorkbenchCurrency.EXALTED && a.omens().isEmpty());
    var additions = new AbyssProbabilityComparison(catalog);
    for (var side : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      String id = "abyss-preparation-" + side.name().toLowerCase(Locale.ROOT);
      AbyssCraftingState prepared;
      try {
        prepared = source.addUnrevealed(id, side, catalog);
      } catch (IllegalArgumentException full) {
        continue;
      }
      if (exalted) {
        var pool = additions.pool(prepared, WorkbenchCurrency.EXALTED, null);
        if (source.count(side) == 2
            && source.slots().stream()
                    .filter(s -> s.side() == side && useful.contains(s.id()))
                    .count()
                == 2
            && !pool.isEmpty()) {
          result.add(
              new Strategy(
                  Kind.COMPLETE_SIDE,
                  prepared,
                  WorkbenchCurrency.EXALTED,
                  Set.of(),
                  Set.of(),
                  null,
                  null));
        }
        var targetIds = new TreeSet<String>();
        for (var candidate : pool) {
          var values =
              candidate.stats().stream()
                  .collect(
                      Collectors.toMap(
                          ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::max));
          var probe =
              prepared.append("goal-probe", new ModifierInstance(candidate.id(), values), catalog);
          var after = evaluate.apply(probe.knownItem());
          if (preserves(before, after) && matched(after) > matched(before))
            targetIds.add(candidate.id());
        }
        if (!targetIds.isEmpty()) {
          var comparison =
              additions.compare(prepared, id, WorkbenchCurrency.EXALTED, null, targetIds);
          if (comparison.isPresent()
              && comparison.get().afterBlocking().percent() > comparison.get().baseline().percent())
            result.add(
                new Strategy(
                    Kind.BLOCK_HIGH_WEIGHT,
                    prepared,
                    WorkbenchCurrency.EXALTED,
                    Set.of(),
                    targetIds,
                    comparison.get(),
                    null));
        }
      }
      if (useful.isEmpty()) continue;
      var seen = new HashSet<String>();
      for (var action : inventory) {
        if (action.currency().baseAction() != null
            || action.currency() == WorkbenchCurrency.ESSENCE_ABYSS
            || !seen.add(action.currency() + ":" + action.omenIds())) continue;
        var baseline = simulator.abyssPlan(source, action.currency(), action.omenIds());
        var next = simulator.abyssPlan(prepared, action.currency(), action.omenIds());
        if (!baseline.available() || !next.available()) continue;
        int safeBefore = safe(baseline, before), safeAfter = safe(next, before);
        if ((long) safeAfter * baseline.branches().size()
            <= (long) safeBefore * next.branches().size()) continue;
        result.add(
            new Strategy(
                Kind.SHARE_REMOVAL_RISK,
                prepared,
                action.currency(),
                action.omenIds(),
                Set.of(),
                null,
                new Protection(
                    safeBefore, baseline.branches().size(), safeAfter, next.branches().size())));
      }
    }
    return List.copyOf(result);
  }

  private int safe(AbyssCraftingPlan plan, Progress before) {
    return (int)
        plan.branches().stream()
            .filter(b -> preserves(before, evaluate.apply(b.remaining().knownItem())))
            .count();
  }

  private static long matched(Progress progress) {
    return progress.entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private static boolean preserves(Progress before, Progress after) {
    if (before.entries().size() != after.entries().size()) return false;
    for (int i = 0; i < before.entries().size(); i++)
      if (before.entries().get(i) == Status.MATCH && after.entries().get(i) != Status.MATCH)
        return false;
    return true;
  }
}
