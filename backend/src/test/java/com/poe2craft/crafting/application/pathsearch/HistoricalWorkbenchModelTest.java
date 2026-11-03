package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class HistoricalWorkbenchModelTest {
  final WorkbenchSimulator.AutomaticIdentity current = model();

  static WorkbenchSimulator.AutomaticIdentity model() {
    var catalog = ItemCatalogLoader.loadDefault();
    return new WorkbenchSimulator(catalog, new CraftingEngine(catalog)).automaticIdentity();
  }

  WorkbenchSimulator.AutomaticIdentity old() {
    var d = current.definitions();
    return new WorkbenchSimulator.AutomaticIdentity(
        current.ruleVersion().replaceFirst("-sanctification-independent-stat-percent-v1-.*$", ""),
        current.ledgerVersion(),
        new WorkbenchDefinitions.Document(
            d.schemaVersion(),
            d.ruleVersion(),
            d.currencies().stream()
                .filter(
                    c ->
                        c.id() != WorkbenchCurrency.ARMOURERS_SCRAP
                            && c.id() != WorkbenchCurrency.BLACKSMITHS_WHETSTONE)
                .toList(),
            d.omens().stream().filter(o -> o.key() != WorkbenchOmen.SANCTIFICATION).toList()),
        current.fixedTargets(),
        current.replacementTargets(),
        current.qualityPolicy());
  }

  @Test
  void acceptsOnlyTheKnownManualAdditionForReadOnlyObservations() {
    assertThat(HistoricalWorkbenchModel.matchesReadOnly(current, current)).isTrue();
    assertThat(HistoricalWorkbenchModel.predatesManualFeatures(old())).isTrue();
    assertThat(HistoricalWorkbenchModel.matchesReadOnly(old(), current)).isTrue();
    var saved = old();
    assertThat(
            HistoricalWorkbenchModel.matchesReadOnly(
                new WorkbenchSimulator.AutomaticIdentity(
                    saved.ruleVersion(),
                    "different-ledger",
                    saved.definitions(),
                    saved.fixedTargets(),
                    saved.replacementTargets(),
                    saved.qualityPolicy()),
                current))
        .isFalse();
    assertThat(
            HistoricalWorkbenchModel.matchesReadOnly(
                new WorkbenchSimulator.AutomaticIdentity(
                    saved.ruleVersion() + "unknown",
                    saved.ledgerVersion(),
                    saved.definitions(),
                    saved.fixedTargets(),
                    saved.replacementTargets(),
                    saved.qualityPolicy()),
                current))
        .isFalse();
    var reversed = new ArrayList<>(saved.definitions().currencies());
    Collections.reverse(reversed);
    assertThat(
            HistoricalWorkbenchModel.matchesReadOnly(
                new WorkbenchSimulator.AutomaticIdentity(
                    saved.ruleVersion(),
                    saved.ledgerVersion(),
                    new WorkbenchDefinitions.Document(
                        1,
                        saved.definitions().ruleVersion(),
                        reversed,
                        saved.definitions().omens()),
                    saved.fixedTargets(),
                    saved.replacementTargets(),
                    saved.qualityPolicy()),
                current))
        .isFalse();
  }

  @Test
  void previousPlusOnePolicyRemainsStrictlyReadOnlyWithAllDefinitionsUnchanged() {
    String oldVersion =
        current
            .ruleVersion()
            .replace(
                "user-selected-equipment-quality-maximum-v2",
                "user-selected-equipment-quality-plus-one-v1");
    var previous =
        new WorkbenchSimulator.AutomaticIdentity(
            oldVersion,
            current.ledgerVersion(),
            current.definitions(),
            current.fixedTargets(),
            current.replacementTargets(),
            current.qualityPolicy());
    assertThat(HistoricalWorkbenchModel.requiresReadOnly(previous)).isTrue();
    assertThat(HistoricalWorkbenchModel.requiresReadOnly(current)).isFalse();
    assertThat(HistoricalWorkbenchModel.matchesReadOnly(previous, current)).isTrue();
    var tampered =
        new WorkbenchSimulator.AutomaticIdentity(
            oldVersion,
            "different-ledger",
            previous.definitions(),
            previous.fixedTargets(),
            previous.replacementTargets(),
            previous.qualityPolicy());
    assertThat(HistoricalWorkbenchModel.matchesReadOnly(tampered, current)).isFalse();
  }
}
