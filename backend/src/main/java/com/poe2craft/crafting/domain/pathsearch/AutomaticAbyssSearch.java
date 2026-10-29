package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import java.util.stream.Collectors;

/** Factored conditional state expansion. No law over reveal offers is supplied or implied. */
public final class AutomaticAbyssSearch {
  public record Ratio(long numerator, long denominator) {
    public Ratio {
      if (numerator < 0 || denominator <= 0 || numerator > denominator)
        throw new IllegalArgumentException("Invalid probability mass");
    }
  }

  /** Ranges describe a family of states, not a rolled value or a reveal probability. */
  public record Range(String stat, long min, long max) {}

  public record State(AbyssCraftingState item, Map<String, List<Range>> ranges, String condition) {}

  public record Candidate(String modifierId, Ratio selection, State result, List<Next> next) {}

  public record Removal(
      String slotId, Ratio probability, State remaining, List<Candidate> candidates) {}

  public record Next(
      WorkbenchCurrency currency, Set<String> omens, int removalBranches, int candidateCount) {}

  public record Transition(
      State source, WorkbenchCurrency currency, Set<String> omens, List<Removal> removals) {}

  public record Route(
      AbyssStrategyPlanner.Strategy strategy,
      State prepared,
      State assumed,
      List<Transition> transitions,
      String status) {}

  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final List<AutomaticCraftingAction> inventory;

  public AutomaticAbyssSearch(
      ItemCatalog catalog, WorkbenchSimulator simulator, List<AutomaticCraftingAction> inventory) {
    this.catalog = catalog;
    this.simulator = simulator;
    this.inventory = List.copyOf(inventory);
  }

  public List<Route> expand(List<AbyssStrategyPlanner.Strategy> strategies) {
    var result = new ArrayList<Route>();
    for (var strategy : strategies) {
      var prepared = new State(strategy.prepared(), Map.of(), "UNREVEALED_SLOT");
      var transitions = new ArrayList<Transition>();
      transitions.add(expand(prepared, strategy.nextCurrency(), strategy.activeOmens()));
      State assumed = null;
      if (strategy.modifierSelection() != null) {
        var blocker = catalog.find(strategy.modifierSelection().blockerModifierId()).orElseThrow();
        var slot =
            prepared.item().slots().stream()
                .filter(s -> s.desecrated() && s.modifier() == null)
                .findFirst()
                .orElseThrow();
        var modifier = carrier(blocker);
        var selected =
            prepared
                .item()
                .observe(slot.id(), List.of(modifier, modifier, modifier), catalog)
                .choose(0, catalog);
        assumed =
            new State(
                selected, Map.of(blocker.id(), ranges(blocker)), "IF_REVEAL_CHOOSES_MODIFIER");
        transitions.add(expand(assumed, strategy.nextCurrency(), strategy.activeOmens()));
      }
      result.add(
          new Route(strategy, prepared, assumed, List.copyOf(transitions), "CONDITIONAL_FRONTIER"));
    }
    return List.copyOf(result);
  }

  public Transition expand(State source, WorkbenchCurrency currency, Set<String> omens) {
    source.item().validate(catalog);
    var plan = simulator.abyssPlan(source.item(), currency, omens);
    var removals = new ArrayList<Removal>();
    if (!plan.available()) return new Transition(source, currency, omens, List.of());
    for (var branch : plan.branches()) {
      var remainingRanges = retainedRanges(source.ranges(), branch.remaining());
      var remaining = new State(branch.remaining(), remainingRanges, source.condition());
      long denominator =
          branch.uniformSelection()
              ? branch.candidates().size()
              : branch.candidates().stream().mapToLong(ModifierDefinition::weight).sum();
      var candidates = new ArrayList<Candidate>();
      for (var definition : branch.candidates()) {
        long numerator = branch.uniformSelection() ? 1 : definition.weight();
        if (numerator <= 0) continue;
        var appended =
            branch.remaining().append(nextSlotId(branch.remaining()), carrier(definition), catalog);
        var nextRanges = new LinkedHashMap<>(remainingRanges);
        nextRanges.put(definition.id(), ranges(definition));
        var state = new State(appended, Map.copyOf(nextRanges), source.condition());
        candidates.add(
            new Candidate(definition.id(), new Ratio(numerator, denominator), state, next(state)));
      }
      removals.add(
          new Removal(
              branch.removedSlotId(),
              new Ratio(1, plan.branches().size()),
              remaining,
              List.copyOf(candidates)));
    }
    return new Transition(source, currency, omens, List.copyOf(removals));
  }

  private static String nextSlotId(AbyssCraftingState state) {
    var ids = state.slots().stream().map(AbyssCraftingState.Slot::id).collect(Collectors.toSet());
    int index = 0;
    while (ids.contains("automatic-added-" + index)) index++;
    return "automatic-added-" + index;
  }

  public static long retainedRecords(List<Route> routes) {
    if (routes == null) return 0;
    return routes.stream()
        .mapToLong(
            r ->
                2
                    + (r.assumed() == null ? 0 : 1)
                    + r.transitions().stream()
                        .mapToLong(
                            t ->
                                1
                                    + t.removals().stream()
                                        .mapToLong(
                                            b ->
                                                1
                                                    + b.candidates().stream()
                                                        .mapToLong(c -> 1 + c.next().size())
                                                        .sum())
                                        .sum())
                        .sum())
        .sum();
  }

  private List<Next> next(State state) {
    var result = new ArrayList<Next>();
    var seen = new HashSet<String>();
    for (var action : inventory) {
      if (!seen.add(action.currency() + ":" + action.omenIds())) continue;
      // Range carriers must never decide a value-sensitive removal rule.
      if (!state.ranges().isEmpty() && action.omens().contains(WorkbenchOmen.WHITTLING)) continue;
      var plan = simulator.abyssPlan(state.item(), action.currency(), action.omenIds());
      if (plan.available())
        result.add(
            new Next(
                action.currency(),
                action.omenIds(),
                plan.branches().size(),
                plan.branches().stream().mapToInt(b -> b.candidates().size()).sum()));
    }
    return List.copyOf(result);
  }

  private static Map<String, List<Range>> retainedRanges(
      Map<String, List<Range>> source, AbyssCraftingState state) {
    var ids =
        state.slots().stream()
            .map(AbyssCraftingState.Slot::modifier)
            .filter(Objects::nonNull)
            .map(ModifierInstance::modifierId)
            .collect(Collectors.toSet());
    return source.entrySet().stream()
        .filter(e -> ids.contains(e.getKey()))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static List<Range> ranges(ModifierDefinition definition) {
    return definition.stats().stream().map(s -> new Range(s.id(), s.min(), s.max())).toList();
  }

  private static ModifierInstance carrier(ModifierDefinition definition) {
    return new ModifierInstance(
        definition.id(),
        definition.stats().stream()
            .collect(
                Collectors.toMap(
                    ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::min)));
  }
}
