package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 품질·한도 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code solar}: 태양목걸이 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code defaultAndBreachCapsPreserveConcreteRollsAcrossCraftsAndRefusals()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code stockyDefaultDoesNotInterpretLocalWardAsQualityOrEnableQualityActions()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code forgedBreachValuesAndSpecialStatesDoNotProduceAReviewedCap()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class QualityLimitTest {
  final ItemCatalog solar = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(solar, new CraftingEngine(solar));

  @Test
  void defaultAndBreachCapsPreserveConcreteRollsAcrossCraftsAndRefusals() {
    var root = SolarAmulet.initial(solar);
    assertThat(QualityLimitRules.describe(root, solar).maximumQuality()).isEqualTo(20);
    var rare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))),
            Set.of());
    assertThat(QualityLimitRules.describe(rare, solar).maximumQuality()).isEqualTo(40);
    var divine = simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), new Random(7));
    assertThat(divine.applied()).isFalse();
    assertThat(divine.state()).isEqualTo(rare);
    assertThat(divine.qualityLimit()).isEqualTo(QualityLimitRules.describe(rare, solar));
    assertThat(divine.state().explicits()).isEqualTo(rare.explicits());
    var blocked = simulator.apply(rare, WorkbenchCurrency.REGAL, Set.of(), new Random(7));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(rare);
    assertThat(blocked.qualityLimit().maximumQuality()).isEqualTo(40);
    var annul = simulator.apply(rare, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(7));
    assertThat(annul.applied()).isTrue();
    assertThat(annul.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(rare.explicits()).hasSize(1);
  }

  @Test
  void stockyDefaultDoesNotInterpretLocalWardAsQualityOrEnableQualityActions() {
    var f = new StockyCraftingFixture();
    var rare = f.rare(82, null, "stocky-mitts:prefix:alloy-local-runic-ward");
    assertThat(QualityLimitRules.describe(rare, f.catalog).maximumQuality()).isEqualTo(20);
    var result = f.simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), new Random(7));
    assertThat(result.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(f.simulator.actions(f.root, Set.of())).hasSize(89);
    assertThat(
            f.simulator.actions(f.root, Set.of()).stream()
                .filter(a -> QualityCurrencyRules.isQuality(a.action())))
        .hasSize(2)
        .noneMatch(WorkbenchSimulator.Availability::available);
  }

  @Test
  void forgedBreachValuesAndSpecialStatesDoNotProduceAReviewedCap() {
    var root = SolarAmulet.initial(solar);
    var forged =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 40L))),
            Set.of());
    assertThatThrownBy(() -> QualityLimitRules.describe(forged, solar))
        .isInstanceOf(IllegalArgumentException.class);
    var corrupted =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            root.implicits(),
            root.explicits(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThatThrownBy(() -> QualityLimitRules.describe(corrupted, solar))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
