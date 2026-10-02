package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Samples concrete elementary events; unchanged instances retain their actual values. */
public final class WorkbenchSimulator {
  public static final String RULE_VERSION = "solar-workbench-affix-v2";
  public static final String LEDGER_VERSION = "solar-uniform-assumptions-v1";
  private final ItemCatalog catalog;
  private final AdditionRules additionRules;

  public WorkbenchSimulator(ItemCatalog catalog, CraftingEngine engine) {
    this.catalog = Objects.requireNonNull(catalog);
    Objects.requireNonNull(engine);
    additionRules = new AdditionRules(catalog);
  }

  public Result apply(ItemState state, CraftingAction action, RandomGenerator random) {
    if (action == null) throw new IllegalArgumentException("Action required");
    return apply(state, WorkbenchCurrency.valueOf(action.name()), Set.of(), random);
  }

  public List<Availability> actions(ItemState state, Set<String> activeOmens) {
    validate(state);
    var omens = parseOmens(activeOmens);
    return Arrays.stream(WorkbenchCurrency.values())
        .map(a -> availability(state, a, omens))
        .toList();
  }

  private void validate(ItemState state) {
    if (state == null || !new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported concrete item state");
  }

  private List<WorkbenchOmen> parseOmens(Set<String> ids) {
    if (ids == null) return List.of();
    if (ids.size() > WorkbenchOmen.values().length)
      throw new IllegalArgumentException("Too many active omens");
    return ids.stream().sorted().map(WorkbenchOmen::fromId).toList();
  }

  private List<WorkbenchOmen> matching(WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    return omens.stream()
        .filter(
            o ->
                o.trigger() == action
                    || (action.baseAction() != null
                        && o.trigger().baseAction() == action.baseAction()))
        .toList();
  }

  private Availability availability(
      ItemState state, WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    if (AdditionRules.isAddition(action)) {
      var plan =
          additionRules.plan(
              StateBucket.from(state),
              action,
              omens.stream().map(WorkbenchOmen::id).collect(java.util.stream.Collectors.toSet()));
      return new Availability(action, plan.available(), plan.reason());
    }
    var matches = matching(action, omens);
    if (matches.size() > 1)
      return blocked(
          action,
          "Multiple omens for the same operation need combination verification. Deactivate all but one.");
    if (!matches.isEmpty() && action.minimumModifierLevel() > 0)
      return blocked(
          action,
          "Omen interaction with Greater/Perfect currency is not verified. Deactivate the omen or use ordinary currency.");
    if (state.itemLevel() < action.minimumModifierLevel())
      return blocked(action, "Item level is below the currency's minimum modifier level.");
    var omen = matches.isEmpty() ? null : matches.getFirst();
    if (action == WorkbenchCurrency.DIVINE) {
      var targets = new ArrayList<>(state.implicits());
      if (omen != WorkbenchOmen.BLESSED) targets.addAll(state.explicits());
      if (targets.stream()
          .noneMatch(
              m ->
                  catalog.find(m.modifierId()).orElseThrow().stats().stream()
                      .anyMatch(s -> s.max() > s.min())))
        return blocked(action, "No supported numeric range can be rerolled.");
      return new Availability(action, true, "");
    }
    var kind = action.baseAction();
    boolean rarity =
        switch (kind) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED, CHAOS -> state.rarity() == ItemState.Rarity.RARE;
          case ANNULMENT ->
              state.rarity() == ItemState.Rarity.MAGIC || state.rarity() == ItemState.Rarity.RARE;
        };
    if (!rarity) return blocked(action, "This currency cannot be used on this rarity.");
    if (kind == CraftingAction.ANNULMENT || kind == CraftingAction.CHAOS) {
      var removals = removalCandidates(state, omen);
      if (removals.isEmpty())
        return blocked(action, "No eligible explicit modifier can be removed with this omen.");
      if (kind == CraftingAction.CHAOS)
        for (var removed : removals) {
          var rest = new ArrayList<>(state.explicits());
          rest.remove(removed);
          if (pool(copy(state, state.rarity(), state.implicits(), rest), action, null).isEmpty())
            return blocked(action, "A removal branch has no verified replacement pool.");
        }
    } else if (pool(
            copy(state, upgrade(state, action), state.implicits(), state.explicits()), action, omen)
        .isEmpty())
      return blocked(
          action, "No eligible modifier or free affix slot remains for this currency and omen.");
    return new Availability(action, true, "");
  }

  private Availability blocked(WorkbenchCurrency action, String reason) {
    return new Availability(action, false, reason);
  }

  public List<ModifierDefinition> pool(
      ItemState state, WorkbenchCurrency currency, WorkbenchOmen omen) {
    return additionRules.pool(StateBucket.from(state), currency, omen);
  }

  private List<ModifierInstance> removalCandidates(ItemState state, WorkbenchOmen omen) {
    var eligible = state.explicits();
    if (omen != null && omen.affix() != null)
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().affixType() == omen.affix())
              .toList();
    if (omen == WorkbenchOmen.WHITTLING && !eligible.isEmpty()) {
      int lowest =
          eligible.stream()
              .mapToInt(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel())
              .min()
              .orElseThrow();
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel() == lowest)
              .toList();
    }
    return eligible;
  }

  private ItemState.Rarity upgrade(ItemState state, WorkbenchCurrency currency) {
    return currency.baseAction() == CraftingAction.TRANSMUTATION
        ? ItemState.Rarity.MAGIC
        : currency.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity();
  }

  public Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> activeOmens, RandomGenerator random) {
    validate(state);
    if (action == null || random == null)
      throw new IllegalArgumentException("Action and random required");
    var omens = parseOmens(activeOmens);
    var available = availability(state, action, omens);
    var allIds = omens.stream().map(WorkbenchOmen::id).toList();
    if (!available.available())
      return new Result(
          RULE_VERSION,
          LEDGER_VERSION,
          state.snapshotId(),
          state,
          action,
          false,
          available.reason(),
          List.of(),
          List.of(),
          List.of(),
          allIds);
    var matched = matching(action, omens);
    var omen = matched.isEmpty() ? null : matched.getFirst();
    var implicits = new ArrayList<>(state.implicits());
    var explicits = new ArrayList<>(state.explicits());
    var events = new ArrayList<Event>();
    var assumptions = new ArrayList<Assumption>();
    var rarity = upgrade(state, action);
    if (action == WorkbenchCurrency.DIVINE) {
      for (int i = 0; i < implicits.size(); i++) {
        var current = implicits.get(i);
        var d = catalog.find(current.modifierId()).orElseThrow();
        if (d.stats().getFirst().max() > d.stats().getFirst().min()) {
          var roll = roll(d, random, assumptions);
          implicits.set(i, roll);
          events.add(new Event("REROLL_IMPLICIT", d.id(), roll.values(), 1));
        }
      }
      if (omen != WorkbenchOmen.BLESSED)
        for (int i = 0; i < explicits.size(); i++) {
          var current = explicits.get(i);
          var d = catalog.find(current.modifierId()).orElseThrow();
          if (d.stats().getFirst().max() > d.stats().getFirst().min()) {
            var roll = roll(d, random, assumptions);
            explicits.set(i, roll);
            events.add(new Event("REROLL_EXPLICIT", d.id(), roll.values(), 1));
          }
        }
    } else {
      if (action.baseAction() == CraftingAction.ANNULMENT
          || action.baseAction() == CraftingAction.CHAOS) {
        var candidates = removalCandidates(state, omen);
        var removed = candidates.get(random.nextInt(candidates.size()));
        explicits.remove(removed);
        events.add(new Event("REMOVE", removed.modifierId(), Map.of(), 1.0 / candidates.size()));
        assumptions.add(
            new Assumption(
                "uniform-removal-v1",
                "eligible explicit modifier instance",
                candidates.size(),
                candidates.stream().map(ModifierInstance::modifierId).toList(),
                null,
                null,
                omen == null
                    ? "https://poe2db.tw/us/"
                        + (action.baseAction() == CraftingAction.CHAOS
                            ? "Chaos_Orb"
                            : "Orb_of_Annulment")
                    : "https://poe2db.tw/us/" + omen.id(),
                "Uniform among eligible removal instances, after affix or lowest modifier-level restriction; no published removal weights."));
      }
      if (action.baseAction() != CraftingAction.ANNULMENT) {
        var intermediate = copy(state, rarity, implicits, explicits);
        var candidates =
            pool(intermediate, action, action.baseAction() == CraftingAction.CHAOS ? null : omen);
        long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
        long draw = random.nextLong(total);
        var chosen = candidates.getLast();
        for (var candidate : candidates) {
          draw -= candidate.weight();
          if (draw < 0) {
            chosen = candidate;
            break;
          }
        }
        var rolled = roll(chosen, random, assumptions);
        explicits.add(rolled);
        events.add(
            new Event("ADD", chosen.id(), rolled.values(), (double) chosen.weight() / total));
      }
    }
    var result = copy(state, rarity, implicits, explicits);
    validate(result);
    var consumed = matched.stream().map(WorkbenchOmen::id).toList();
    var remaining = allIds.stream().filter(id -> !consumed.contains(id)).toList();
    return new Result(
        RULE_VERSION,
        LEDGER_VERSION,
        state.snapshotId(),
        result,
        action,
        true,
        "",
        events,
        assumptions,
        consumed,
        remaining);
  }

  private ModifierInstance roll(
      ModifierDefinition definition, RandomGenerator random, List<Assumption> assumptions) {
    if (definition.stats().size() != 1)
      throw new IllegalArgumentException("Joint stat roll domain needs verification");
    var range = definition.stats().getFirst();
    long n = Math.addExact(Math.subtractExact(range.max(), range.min()), 1);
    long value = Math.addExact(range.min(), random.nextLong(n));
    if (n > 1)
      assumptions.add(
          new Assumption(
              "uniform-integer-roll-v1",
              range.id(),
              n,
              List.of(),
              range.min(),
              range.max(),
              definition.sourceUrl(),
              "Each integer in this single-stat source range is a modeled candidate; no published roll weights."));
    return new ModifierInstance(definition.id(), Map.of(range.id(), value));
  }

  private static ItemState copy(
      ItemState s,
      ItemState.Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits) {
    return new ItemState(
        s.snapshotId(),
        s.baseItemId(),
        s.itemLevel(),
        rarity,
        implicits,
        explicits,
        s.conditions());
  }

  public record Availability(WorkbenchCurrency action, boolean available, String reason) {}

  public record Event(
      String kind, String modifierId, Map<String, Long> values, double selectionProbability) {
    public Event {
      values = Map.copyOf(values);
    }
  }

  public record Assumption(
      String id,
      String candidateUnit,
      long n,
      List<String> candidates,
      Long min,
      Long max,
      String sourceUrl,
      String reason) {
    public Assumption {
      candidates = List.copyOf(candidates);
    }
  }

  public record Result(
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      ItemState state,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<Event> events,
      List<Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Result {
      events = List.copyOf(events);
      assumptions = List.copyOf(assumptions);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
