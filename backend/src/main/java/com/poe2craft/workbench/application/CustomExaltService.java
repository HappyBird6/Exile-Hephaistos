package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** User selection changes only modifier selection; execution and state preservation stay shared. */
public final class CustomExaltService {
  private final CraftingOperations crafting;

  public CustomExaltService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  public record Applied(
      WorkbenchSimulator.Result result,
      AbyssCraftingState abyss,
      WorkbenchSimulator.ExaltChoice choice) {}

  private CraftingItemState resolve(ItemState state, AbyssCraftingState source) {
    return CraftingItemState.resolve(state, source, crafting.abyssCatalog(state));
  }

  private AbyssCurrencyExecutor executor(ItemState state) {
    return new AbyssCurrencyExecutor(crafting.abyssCatalog(state), crafting.simulator(state));
  }

  public List<WorkbenchSimulator.ExaltChoice> choices(
      ItemState state, AbyssCraftingState source, WorkbenchCurrency action, Set<String> omens) {
    var current = resolve(state, source);
    return executor(state).exaltChoices(current.slots(), action, omens);
  }

  public Applied apply(
      ItemState state,
      AbyssCraftingState source,
      WorkbenchCurrency action,
      Set<String> omens,
      String modifierId,
      RandomGenerator random) {
    var current = resolve(state, source);
    var executor = executor(state);
    var choice =
        executor.exaltChoices(current.slots(), action, omens).stream()
            .filter(c -> c.modifierId().equals(modifierId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Choose an eligible Exalted modifier"));
    var transition = executor.applyChosenExalt(current.slots(), action, omens, modifierId, random);
    return new Applied(
        transition.receipt(),
        source != null || !state.desecrated().isEmpty() ? transition.outcome().state() : null,
        choice);
  }
}
