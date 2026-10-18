package com.poe2craft.crafting.domain;

import java.util.*;

/** Execution identity is separate from ItemState; an omen here is consumed by this action. */
public record AutomaticCraftingAction(
    WorkbenchCurrency currency, Set<WorkbenchOmen> omens, AutomaticCurrencyPolicy.Purpose purpose) {
  public AutomaticCraftingAction {
    Objects.requireNonNull(currency);
    Objects.requireNonNull(purpose);
    omens = Collections.unmodifiableSet(new TreeSet<>(omens));
  }

  /** Every approved currency is considered, even when its execution kernel is unsupported. */
  public static List<AutomaticCraftingAction> candidates(AutomaticCurrencyPolicy policy) {
    var result = new ArrayList<AutomaticCraftingAction>();
    for (var entry : policy.candidates()) {
      if (entry.id().contains(":")) continue;
      var currency = WorkbenchCurrency.valueOf(entry.id());
      var temporary = currency == WorkbenchCurrency.REGAL || currency == WorkbenchCurrency.EXALTED;
      var purpose =
          temporary
              ? AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
              : AutomaticCurrencyPolicy.Purpose.ORDINARY;
      result.add(new AutomaticCraftingAction(currency, Set.of(), purpose));
      if (temporary || currency == WorkbenchCurrency.CHAOS) {
        result.add(
            new AutomaticCraftingAction(
                currency, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION));
      }
      if (currency == WorkbenchCurrency.CHAOS) {
        result.add(
            new AutomaticCraftingAction(
                currency, Set.of(), AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION));
        for (var omens :
            List.of(
                Set.of(WorkbenchOmen.WHITTLING),
                Set.of(WorkbenchOmen.SINISTRAL_ERASURE),
                Set.of(WorkbenchOmen.DEXTRAL_ERASURE),
                Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.SINISTRAL_ERASURE),
                Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.DEXTRAL_ERASURE))) {
          result.add(
              new AutomaticCraftingAction(
                  currency, omens, AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION));
        }
      }
      if (currency == WorkbenchCurrency.ESSENCE_BREACH)
        result.add(
            new AutomaticCraftingAction(
                currency, Set.of(), AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT));
      var variants =
          currency == WorkbenchCurrency.REGAL
              ? List.of(WorkbenchOmen.SINISTRAL_CORONATION, WorkbenchOmen.DEXTRAL_CORONATION)
              : currency == WorkbenchCurrency.EXALTED
                  ? List.of(WorkbenchOmen.SINISTRAL_EXALTATION, WorkbenchOmen.DEXTRAL_EXALTATION)
                  : !currency.replacementEssenceModifiers().isEmpty()
                      ? List.of(
                          WorkbenchOmen.SINISTRAL_CRYSTALLISATION,
                          WorkbenchOmen.DEXTRAL_CRYSTALLISATION)
                      : List.<WorkbenchOmen>of();
      for (var omen : variants) {
        result.add(new AutomaticCraftingAction(currency, Set.of(omen), purpose));
        if (temporary)
          result.add(
              new AutomaticCraftingAction(
                  currency, Set.of(omen), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION));
      }
    }
    return List.copyOf(result);
  }

  public Set<String> omenIds() {
    return omens.stream()
        .map(WorkbenchOmen::id)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
