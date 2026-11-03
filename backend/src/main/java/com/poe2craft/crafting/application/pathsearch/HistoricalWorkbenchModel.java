package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import java.util.*;

/** Narrow compatibility for immutable observations, never execution or cursor restoration. */
final class HistoricalWorkbenchModel {
  private static final Set<WorkbenchCurrency> MANUAL_CURRENCIES =
      Set.of(WorkbenchCurrency.ARMOURERS_SCRAP, WorkbenchCurrency.BLACKSMITHS_WHETSTONE);
  private static final Set<String> MANUAL_SUFFIXES =
      Set.of(
          "-sanctification-independent-stat-percent-v1-equipment-quality-deferred-v1",
          "-sanctification-independent-stat-percent-v1-user-selected-equipment-quality-plus-one-v1",
          "-sanctification-independent-stat-percent-v1-user-selected-equipment-quality-maximum-v2");
  private static final String CURRENT_MANUAL_SUFFIX =
      "-sanctification-independent-stat-percent-v1-user-selected-equipment-quality-maximum-v2";

  static boolean requiresReadOnly(WorkbenchSimulator.AutomaticIdentity model) {
    return predatesManualFeatures(model)
        || (model != null
            && MANUAL_SUFFIXES.stream()
                .filter(suffix -> !suffix.equals(CURRENT_MANUAL_SUFFIX))
                .anyMatch(suffix -> model.ruleVersion().endsWith(suffix)));
  }

  static boolean predatesManualFeatures(WorkbenchSimulator.AutomaticIdentity model) {
    return model != null
        && model.definitions() != null
        && model.definitions().currencies().stream()
            .noneMatch(c -> MANUAL_CURRENCIES.contains(c.id()))
        && model.definitions().omens().stream()
            .noneMatch(o -> o.key() == WorkbenchOmen.SANCTIFICATION);
  }

  static boolean matchesReadOnly(
      WorkbenchSimulator.AutomaticIdentity saved, WorkbenchSimulator.AutomaticIdentity current) {
    if (saved == null || current == null) return false;
    if (saved.equals(current)) return true;
    // Only a known manual policy changed; the frozen automatic policy excludes both quality
    // currencies. Require every definition, override, ledger and numerical policy to match.
    if (current.ruleVersion().endsWith(CURRENT_MANUAL_SUFFIX)
        && MANUAL_SUFFIXES.stream()
            .filter(suffix -> !suffix.equals(CURRENT_MANUAL_SUFFIX))
            .anyMatch(
                suffix ->
                    saved.ruleVersion().endsWith(suffix)
                        && current
                            .ruleVersion()
                            .equals(
                                saved
                                        .ruleVersion()
                                        .substring(
                                            0, saved.ruleVersion().length() - suffix.length())
                                    + CURRENT_MANUAL_SUFFIX))) {
      return saved.equals(
          new WorkbenchSimulator.AutomaticIdentity(
              saved.ruleVersion(),
              current.ledgerVersion(),
              current.definitions(),
              current.fixedTargets(),
              current.replacementTargets(),
              current.qualityPolicy()));
    }
    if (!predatesManualFeatures(saved) || current.definitions() == null) return false;
    boolean knownSuffix =
        MANUAL_SUFFIXES.stream()
            .anyMatch(suffix -> current.ruleVersion().equals(saved.ruleVersion() + suffix));
    if (!knownSuffix) return false;
    var definitions = current.definitions();
    var currencies =
        definitions.currencies().stream().filter(c -> !MANUAL_CURRENCIES.contains(c.id())).toList();
    var omens =
        definitions.omens().stream().filter(o -> o.key() != WorkbenchOmen.SANCTIFICATION).toList();
    if (definitions.currencies().size() - currencies.size() != 2
        || definitions.omens().size() - omens.size() != 1) return false;
    var historicalDefinitions =
        new WorkbenchDefinitions.Document(
            definitions.schemaVersion(), definitions.ruleVersion(), currencies, omens);
    // Every old definition, ordering, override, ledger and numeric policy must still agree.
    return saved.equals(
        new WorkbenchSimulator.AutomaticIdentity(
            saved.ruleVersion(),
            current.ledgerVersion(),
            historicalDefinitions,
            current.fixedTargets(),
            current.replacementTargets(),
            current.qualityPolicy()));
  }
}
