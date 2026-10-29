package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import java.util.stream.Collectors;

/** Next-addition sensitivity, never the probability of observing an Abyss offer. */
public final class AbyssProbabilityComparison {
  private final ItemCatalog catalog;
  private final AdditionRules additions;

  public AbyssProbabilityComparison(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    additions = new AdditionRules(catalog);
  }

  public record Chance(long targetWeight, long totalWeight) {
    public Chance {
      if (targetWeight < 0 || totalWeight <= 0 || targetWeight > totalWeight)
        throw new IllegalArgumentException("Nonempty weighted pool required");
    }

    public double percent() {
      return 100.0 * targetWeight / totalWeight;
    }
  }

  public record Comparison(
      Chance baseline, Chance afterBlocking, String blockerModifierId, int blockerWeight) {}

  /** A missing comparison means the conditional scenario is not calculable, not zero chance. */
  public Optional<Comparison> compare(
      AbyssCraftingState state,
      String slotId,
      WorkbenchCurrency action,
      WorkbenchOmen omen,
      Set<String> targetIds) {
    state.validate(catalog);
    if (action.baseAction() != CraftingAction.EXALTED || targetIds.isEmpty())
      throw new IllegalArgumentException("Explicit targets and a Rare addition action required");
    var slot =
        state.slots().stream()
            .filter(s -> s.id().equals(slotId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (!slot.desecrated() || slot.modifier() != null)
      throw new IllegalArgumentException("Comparison starts from an unrevealed slot");
    var targetFamilies =
        targetIds.stream()
            .flatMap(
                id ->
                    catalog
                        .find(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"))
                        .familyIds()
                        .stream())
            .collect(Collectors.toSet());
    var baselinePool = pool(state, action, omen);
    if (baselinePool.isEmpty()) return Optional.empty();
    var baseline = chance(baselinePool, targetIds);
    var alternatives = new ArrayList<Comparison>();
    for (var blocker : catalog.modifiers().values()) {
      if (blocker.layer() != ModifierDefinition.Layer.EXPLICIT
          || blocker.affixType() != slot.side()
          || blocker.requiredItemLevel() > state.item().itemLevel()
          || blocker.weight() <= 0
          || !Collections.disjoint(blocker.familyIds(), targetFamilies)) continue;
      // Minimum values are only a valid concrete carrier for this family/slot scenario.
      // They do not model the roll or reveal distribution and never become a selected item.
      var values =
          blocker.stats().stream()
              .collect(
                  Collectors.toMap(
                      ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::min));
      var slots =
          state.slots().stream()
              .map(
                  s ->
                      s.id().equals(slotId)
                          ? new AbyssCraftingState.Slot(
                              s.id(), s.side(), new ModifierInstance(blocker.id(), values), true)
                          : s)
              .toList();
      var scenario = new AbyssCraftingState(state.item(), slots, null);
      try {
        scenario.validate(catalog);
      } catch (IllegalArgumentException unsupported) {
        continue;
      }
      var conditionalPool = pool(scenario, action, omen);
      if (conditionalPool.isEmpty()) continue;
      alternatives.add(
          new Comparison(
              baseline, chance(conditionalPool, targetIds), blocker.id(), blocker.weight()));
    }
    // Highest individual option weight, not an invented maximum reveal-success probability.
    return alternatives.stream()
        .sorted(
            Comparator.comparingInt(Comparison::blockerWeight)
                .reversed()
                .thenComparing(Comparison::blockerModifierId))
        .findFirst();
  }

  public List<ModifierDefinition> pool(
      AbyssCraftingState state, WorkbenchCurrency action, WorkbenchOmen omen) {
    state.validate(catalog);
    var item = state.knownItem();
    var bucket =
        new StateBucket(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            item.explicits().stream().map(ModifierInstance::modifierId).toList(),
            item.conditions());
    var capacity = new ItemStateValidator(catalog).slots(item);
    var plan = additions.plan(bucket, action, omen == null ? Set.of() : Set.of(omen.id()));
    if (!plan.available()) return List.of();
    return plan.candidates().stream()
        .filter(
            d ->
                state.count(d.affixType())
                    < (d.affixType() == AffixType.PREFIX
                        ? capacity.maxPrefixes()
                        : capacity.maxSuffixes()))
        .toList();
  }

  private Chance chance(List<ModifierDefinition> pool, Set<String> targets) {
    return new Chance(
        pool.stream()
            .filter(d -> targets.contains(d.id()))
            .mapToLong(ModifierDefinition::weight)
            .sum(),
        pool.stream().mapToLong(ModifierDefinition::weight).sum());
  }
}
