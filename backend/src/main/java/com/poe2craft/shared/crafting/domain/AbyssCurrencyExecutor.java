package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.random.RandomGenerator;

/** Slot-aware adapter around the existing concrete and Abyss samplers. */
public final class AbyssCurrencyExecutor {
  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;

  public AbyssCurrencyExecutor(ItemCatalog catalog, WorkbenchSimulator simulator) {
    this.catalog = ManualRevealCatalog.enrich(Objects.requireNonNull(catalog));
    this.simulator = Objects.requireNonNull(simulator);
  }

  public record Result(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Result {
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }

  public Result apply(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      RandomGenerator random) {
    Objects.requireNonNull(state);
    Objects.requireNonNull(action);
    Objects.requireNonNull(random);
    var omens = activeOmens == null ? Set.<String>of() : Set.copyOf(activeOmens);
    state.validate(catalog);
    if (!state.item().conditions().isEmpty())
      return blocked(state, action, omens, "ITEM_CONDITION");
    boolean hidden = state.slots().stream().anyMatch(s -> s.modifier() == null);
    boolean valuesOnly =
        action == WorkbenchCurrency.DIVINE
            || action == WorkbenchCurrency.ARMOURERS_SCRAP
            || action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE
            || action == WorkbenchCurrency.ARTIFICER
            || action.catalystType() != null;
    try {
      // The concrete sampler knows existing item values, quality, socket and omen rules.
      // Hidden slots are opaque and can only bypass it for operations that cannot change slots.
      if (!hidden || valuesOnly) {
        var result = simulator.apply(state.knownItem(), action, omens, random);
        if (!result.applied()) return blocked(state, action, omens, result.reason());
        var removedIds =
            result.events().stream()
                .filter(e -> e.kind().equals("REMOVE"))
                .map(WorkbenchSimulator.Event::modifierId)
                .toList();
        var slots = new ArrayList<AbyssCraftingState.Slot>();
        if (hidden) state.slots().stream().filter(s -> s.modifier() == null).forEach(slots::add);
        int index = 0;
        for (var modifier : result.state().explicits()) {
          var previous =
              state.slots().stream()
                  .filter(
                      s ->
                          s.modifier() != null
                              && !removedIds.contains(s.modifier().modifierId())
                              && s.modifier().modifierId().equals(modifier.modifierId()))
                  .findFirst();
          if (previous.isPresent()) {
            var s = previous.get();
            slots.add(
                new AbyssCraftingState.Slot(
                    s.id(),
                    s.side(),
                    modifier,
                    s.desecrated(),
                    s.minimumModifierLevel(),
                    s.revealPool()));
          } else {
            String id;
            do {
              id = "currency-" + index++;
            } while (containsId(state, id));
            slots.add(
                new AbyssCraftingState.Slot(
                    id,
                    catalog.find(modifier.modifierId()).orElseThrow().affixType(),
                    modifier,
                    false));
          }
        }
        var offer = state.offer();
        if (offer != null && slots.stream().noneMatch(s -> s.id().equals(state.offer().slotId())))
          offer = null;
        var next =
            new AbyssCraftingState(
                AbyssCraftingState.withExplicits(result.state(), List.of()), slots, offer);
        next.validate(catalog);
        return new Result(
            next, action, true, result.reason(), result.consumedOmens(), result.remainingOmens());
      }
      // Known slot-changing rules already count hidden slots, protect fractures and enforce
      // families.
      var matching = new TreeSet<String>();
      for (var id : omens) {
        var omen = WorkbenchOmen.fromId(id);
        if (omen.trigger() == action
            || (action.baseAction() != null && omen.trigger().baseAction() == action.baseAction())
            || (omen.trigger() == WorkbenchCurrency.ESSENCE_HYSTERIA
                && !action.isLiquid()
                && !action.replacementEssenceModifiers().isEmpty())) matching.add(id);
      }
      if (action.baseAction() == CraftingAction.CHAOS) {
        var effects = matching.stream().map(WorkbenchOmen::fromId).toList();
        if (effects.contains(WorkbenchOmen.WHITTLING))
          return blocked(state, action, omens, "UNREVEALED_MODIFIER_LEVEL_UNKNOWN");
        if (effects.size() > 1
            || effects.stream()
                .anyMatch(o -> action.minimumModifierLevel() > 0 && !o.supportsTieredCurrency()))
          return blocked(state, action, omens, "ABYSS_OMEN_COMBINATION_UNSUPPORTED");
        if (state.offer() != null)
          return blocked(state, action, omens, "RESOLVE_OFFER_BEFORE_CURRENCY");
        var side = effects.isEmpty() ? null : effects.getFirst().affix();
        var locked = BasicJewel.chaosLockedSides(state.storedItem(), catalog);
        var removals =
            state.removalCandidates(side).stream()
                .filter(slot -> !locked.contains(slot.side()))
                .toList();
        if (removals.isEmpty()) return blocked(state, action, omens, "NO_ELIGIBLE_REMOVAL");
        var addition =
            action == WorkbenchCurrency.GREATER_CHAOS
                ? WorkbenchCurrency.GREATER_EXALTED
                : action == WorkbenchCurrency.PERFECT_CHAOS
                    ? WorkbenchCurrency.PERFECT_EXALTED
                    : WorkbenchCurrency.EXALTED;
        var pools = new ArrayList<List<ModifierDefinition>>();
        for (var removal : removals) {
          var remaining = state.remove(removal.id(), side, catalog);
          var pool = new AbyssProbabilityComparison(catalog).pool(remaining, addition, null);
          if (pool.isEmpty()) return blocked(state, action, omens, "NO_ELIGIBLE_ADDITION");
          pools.add(pool);
        }
        int selected = random.nextInt(removals.size());
        var removal = removals.get(selected);
        var remaining = state.remove(removal.id(), side, catalog);
        var pool = pools.get(selected);
        long draw = random.nextLong(pool.stream().mapToLong(ModifierDefinition::weight).sum());
        var chosen = pool.getLast();
        for (var candidate : pool) {
          draw -= candidate.weight();
          if (draw < 0) {
            chosen = candidate;
            break;
          }
        }
        // A valid concrete carrier delegates numeric and coupled rolls to the existing sampler.
        // It never supplies values for any unrevealed slot.
        var values = new HashMap<String, Long>();
        chosen.stats().forEach(s -> values.put(s.id(), s.min()));
        var carrier =
            AbyssCraftingState.withExplicits(
                state.item(), List.of(new ModifierInstance(chosen.id(), values)));
        var instance = carrier.explicits().getFirst();
        if (chosen.stats().stream().anyMatch(s -> s.min() != s.max())) {
          var rolled = simulator.apply(carrier, WorkbenchCurrency.DIVINE, Set.of(), random);
          if (!rolled.applied()) return blocked(state, action, omens, "NUMERIC_ROLL_UNAVAILABLE");
          instance = rolled.state().explicits().getFirst();
        }
        var next = remaining.append(removal.id(), instance, catalog);
        return new Result(
            next,
            action,
            true,
            "Simulator policy: uniform eligible removal, then published conditional modifier weights; reveal odds are unknown.",
            List.copyOf(matching),
            omens.stream().filter(id -> !matching.contains(id)).sorted().toList());
      }
      // Catalyst quality is retained metadata under the concrete sampler's current policy;
      // the legacy Abyss sampler's quality guard must not become a blanket craft prohibition.
      var i = state.item();
      var sampling =
          i.catalystQuality() == null
              ? state
              : new AbyssCraftingState(
                  new ItemState(
                          i.snapshotId(),
                          i.baseItemId(),
                          i.itemLevel(),
                          i.rarity(),
                          i.implicits(),
                          i.explicits(),
                          i.conditions(),
                          i.augmentSockets(),
                          null,
                          i.desecrated(),
                          i.equipmentQuality(),
                          i.observations())
                      .withAugments(i.installedAugments()),
                  state.slots(),
                  state.offer());
      var plan = simulator.abyssPlan(sampling, action, matching);
      if (!plan.available()) return blocked(state, action, omens, plan.reason());
      var sampled = simulator.applyAbyss(sampling, action, matching, random);
      var next = new AbyssCraftingState(state.item(), sampled.slots(), sampled.offer());
      next.validate(catalog);
      return new Result(
          next,
          action,
          true,
          "",
          List.copyOf(matching),
          omens.stream().filter(id -> !matching.contains(id)).sorted().toList());
    } catch (IllegalArgumentException unsupported) {
      // The original immutable state and every omen survive a rejected transition.
      return blocked(state, action, omens, "ABYSS_CURRENCY_UNAVAILABLE");
    }
  }

  private static boolean containsId(AbyssCraftingState state, String id) {
    return state.slots().stream().anyMatch(s -> s.id().equals(id));
  }

  private static Result blocked(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens, String reason) {
    return new Result(state, action, false, reason, List.of(), omens.stream().sorted().toList());
  }
}
