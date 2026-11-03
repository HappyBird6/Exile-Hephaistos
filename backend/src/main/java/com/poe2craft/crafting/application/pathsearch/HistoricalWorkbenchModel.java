package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import java.util.*;

/** Narrow compatibility for immutable observations, never execution or cursor restoration. */
final class HistoricalWorkbenchModel {
  private static final Set<WorkbenchCurrency> MANUAL_CURRENCIES =
      Set.of(WorkbenchCurrency.ARMOURERS_SCRAP, WorkbenchCurrency.BLACKSMITHS_WHETSTONE);
  private static final Set<String> MANUAL_SUFFIXES = Set.of(
      "-sanctification-independent-stat-percent-v1-equipment-quality-deferred-v1",
      "-sanctification-independent-stat-percent-v1-equipment-quality-user-step1-v1");

  static boolean predatesManualFeatures(WorkbenchSimulator.AutomaticIdentity model) {
    return model != null && model.definitions() != null
        && model.definitions().currencies().stream().noneMatch(c -> MANUAL_CURRENCIES.contains(c.id()))
        && model.definitions().omens().stream().noneMatch(o -> o.key() == WorkbenchOmen.SANCTIFICATION);
  }

  static boolean matchesReadOnly(WorkbenchSimulator.AutomaticIdentity saved,
      WorkbenchSimulator.AutomaticIdentity current) {
    if (saved == null || current == null) return false;
    if (saved.equals(current)) return true;
    if (!predatesManualFeatures(saved) || current.definitions() == null) return false;
    boolean knownSuffix = MANUAL_SUFFIXES.stream().anyMatch(suffix ->
        current.ruleVersion().equals(saved.ruleVersion() + suffix));
    if (!knownSuffix) return false;
    var definitions = current.definitions();
    var currencies = definitions.currencies().stream()
        .filter(c -> !MANUAL_CURRENCIES.contains(c.id())).toList();
    var omens = definitions.omens().stream()
        .filter(o -> o.key() != WorkbenchOmen.SANCTIFICATION).toList();
    if (definitions.currencies().size() - currencies.size() != 2
        || definitions.omens().size() - omens.size() != 1) return false;
    var historicalDefinitions = new WorkbenchDefinitions.Document(
        definitions.schemaVersion(), definitions.ruleVersion(), currencies, omens);
    // Every old definition, ordering, override, ledger and numeric policy must still agree.
    return saved.equals(new WorkbenchSimulator.AutomaticIdentity(saved.ruleVersion(),
        current.ledgerVersion(), historicalDefinitions, current.fixedTargets(),
        current.replacementTargets(), current.qualityPolicy()));
  }
}
