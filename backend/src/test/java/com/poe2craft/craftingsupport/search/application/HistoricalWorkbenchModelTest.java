package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 과거 작업대 결과를 읽기 전용으로 해석한다. 실행 재개에는 사용하지 않는다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code current}: 현재 ({@code WorkbenchSimulator.AutomaticIdentity}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code model()}: 모형 값을 조회·구성한다. 반환: {@code WorkbenchSimulator.AutomaticIdentity}.
 *   <li>{@code old()}: 이전 값을 조회·구성한다. 반환: {@code WorkbenchSimulator.AutomaticIdentity}.
 *   <li>{@code acceptsOnlyTheKnownManualAdditionForReadOnlyObservations()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code previousPlusOnePolicyRemainsStrictlyReadOnlyWithAllDefinitionsUnchanged()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class HistoricalWorkbenchModelTest {
  final WorkbenchSimulator.AutomaticIdentity current = model();

  static WorkbenchSimulator.AutomaticIdentity model() {
    var catalog = ItemCatalogLoader.loadDefault();
    var model = new WorkbenchSimulator(catalog, new CraftingEngine(catalog)).automaticIdentity();
    // 이 테스트가 다루는 마지막 품질 정책 판본 이후 추가된 제작 규칙과 분리한다.
    var version =
        "test-workbench-sanctification-independent-stat-percent-v1-"
            + "user-selected-equipment-quality-maximum-v2";
    return new WorkbenchSimulator.AutomaticIdentity(
        version,
        model.ledgerVersion(),
        model.definitions(),
        model.fixedTargets(),
        model.replacementTargets(),
        model.qualityPolicy());
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
