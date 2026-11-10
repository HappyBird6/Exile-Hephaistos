package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Explicit custom selection; eligibility and natural odds come from the ordinary simulation pool.
 */
public final class CustomExaltService {
  private final CraftingOperations crafting;

  public CustomExaltService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  public record Applied(
      WorkbenchSimulator.Result result,
      AbyssCraftingState abyss,
      WorkbenchSimulator.ExaltChoice choice) {}

  private AbyssCraftingState abyss(ItemState state, AbyssCraftingState source) {
    if (source != null) {
      source.validate(crafting.abyssCatalog(state));
      if (!source.storedItem().equals(state))
        throw new IllegalArgumentException("Inconsistent Abyss state");
      return source;
    }
    return state.desecrated().isEmpty()
        ? null
        : AbyssCraftingState.from(state, crafting.abyssCatalog(state));
  }

  public List<WorkbenchSimulator.ExaltChoice> choices(
      ItemState state, AbyssCraftingState source, WorkbenchCurrency action, Set<String> omens) {
    if (action == null || action.baseAction() != CraftingAction.EXALTED)
      throw new IllegalArgumentException("Exalted currency required");
    var abyss = abyss(state, source);
    if (abyss == null) return crafting.simulator(state).exaltChoices(state, action, omens);
    var plan = crafting.abyssPlan(abyss, action, omens);
    if (!plan.available()) return List.of();
    var pool = plan.branches().getFirst().candidates();
    double total = pool.stream().mapToLong(ModifierDefinition::weight).sum();
    return pool.stream()
        .map(d -> new WorkbenchSimulator.ExaltChoice(d.id(), d.weight() / total))
        .toList();
  }

  public Applied apply(
      ItemState state,
      AbyssCraftingState source,
      WorkbenchCurrency action,
      Set<String> omens,
      String modifierId,
      RandomGenerator random) {
    var choice =
        choices(state, source, action, omens).stream()
            .filter(c -> c.modifierId().equals(modifierId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Choose an eligible Exalted modifier"));
    var abyss = abyss(state, source);
    var sampler = crafting.simulator(state);
    if (abyss == null)
      return new Applied(
          sampler.applyCustomExalt(state, action, omens, random, modifierId), null, choice);
    var next = sampler.applyCustomExalt(abyss, action, omens, random, modifierId);
    var all = omens == null ? Set.<String>of() : omens;
    var consumed =
        all.stream()
            .filter(id -> WorkbenchOmen.fromId(id).trigger().baseAction() == CraftingAction.EXALTED)
            .sorted()
            .toList();
    var added =
        next.knownItem().explicits().stream()
            .filter(m -> m.modifierId().equals(modifierId))
            .findFirst()
            .orElseThrow();
    var result =
        new WorkbenchSimulator.Result(
            sampler.ruleVersion(),
            sampler.ledgerVersion(),
            state.snapshotId(),
            next.storedItem(),
            action,
            true,
            "",
            List.of(
                new WorkbenchSimulator.Event(
                    "ADD", modifierId, added.values(), choice.probability())),
            List.of(),
            consumed,
            all.stream().filter(id -> !consumed.contains(id)).sorted().toList(),
            QualityLimitRules.describe(
                next.storedItem(), crafting.abyssCatalog(next.storedItem())));
    return new Applied(result, next, choice);
  }
}
