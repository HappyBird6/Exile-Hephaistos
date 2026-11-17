package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 태양목걸이·체크포인트·목표 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code SPIRIT}: 정신력; 초기값 {@code "base_spirit_from_equipment"} ({@code String}).
 *   <li>{@code PROJECTILE}: PROJECTILE; 초기값 {@code "projectile_skill_gem_level_+"} ({@code
 *       String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code index}: 색인 ({@code GoalCatalogIndex}).
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code GoalFilterService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code item(modifiers)}: 아이템 처리에서 {@code SolarAmulet.initial}, {@code
 *       initial.snapshotId}을(를) 사용한다. 입력: {@code modifiers}(옵션목록). 반환: {@code ItemState}.
 *   <li>{@code modifier(stat, value)}: 옵션 처리에서 {@code catalog.modifiers}, {@code d.layer}을(를) 사용한다.
 *       입력: {@code stat}(수치), {@code value}(값). 반환: {@code ModifierInstance}.
 *   <li>{@code target(stat, minimum)}: 대상 처리에서 {@code GoalCatalogIndex.id}, {@code
 *       BigDecimal.valueOf}을(를) 사용한다. 입력: {@code stat}(수치), {@code minimum}(최소값). 반환: {@code
 *       Entry}.
 *   <li>{@code goal(entries)}: 목표 처리에서 {@code index.version}, {@code catalog.base}을(를) 사용한다. 입력:
 *       {@code entries}(항목목록). 반환: {@code GoalFilter}.
 *   <li>{@code spirit47ExcludesImplicitEvenWhenImplicitAndExplicitSumExceeds47()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code actualSolarEligiblePoolsReachBothDiamondArmsAndUseExistingLife()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code solarRootABCDiamondUsesReviewedNumericGoalsAndReevaluatesLossAndReacquisition()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class SolarCheckpointGoalTest {
  private static final String SPIRIT = "base_spirit_from_equipment";
  private static final String PROJECTILE = "projectile_skill_gem_level_+";
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final GoalCatalogIndex index =
      new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
  private final GoalFilterService service = new GoalFilterService(index);

  private ItemState item(ModifierInstance... modifiers) {
    var initial = SolarAmulet.initial(catalog, 82, 15);
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        82,
        ItemState.Rarity.RARE,
        initial.implicits(),
        List.of(modifiers),
        Set.of());
  }

  private ModifierInstance modifier(String stat, long value) {
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.requiredItemLevel() <= 82
                        && d.stats().size() == 1
                        && d.stats().getFirst().id().equals(stat)
                        && d.stats().getFirst().contains(value))
            .findFirst()
            .orElseThrow();
    return new ModifierInstance(definition.id(), Map.of(stat, value));
  }

  private Entry target(String stat, long minimum) {
    return new Entry(
        stat,
        GoalCatalogIndex.id("explicit", stat),
        "flat",
        new Range(BigDecimal.valueOf(minimum), null),
        null,
        false);
  }

  private GoalFilter goal(Entry... entries) {
    return new GoalFilter(
        1,
        index.version(),
        new General(catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(new Group("targets", Type.AND, false, null, List.of(entries))));
  }

  @Test
  void spirit47ExcludesImplicitEvenWhenImplicitAndExplicitSumExceeds47() {
    var goal = goal(target(SPIRIT, 47));
    assertThat(service.evaluate(item(), goal).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(modifier(SPIRIT, 46)), goal).status())
        .isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(modifier(SPIRIT, 47)), goal).status()).isEqualTo(Status.MATCH);
  }

  @Test
  void actualSolarEligiblePoolsReachBothDiamondArmsAndUseExistingLife() {
    var transitions = new BasicCurrencyTransitions(catalog, "solar-production-probe");
    var a = modifier(PROJECTILE, 3);
    var b = modifier(SPIRIT, 47);
    var both = goal(target(PROJECTILE, 3), target(SPIRIT, 47));
    var match = service.compileEvaluation(item(), both);
    for (var start : List.of(item(), item(a), item(b))) {
      var result =
          transitions.expand(
              new BasicCurrencyState(start, transitions.provenance()),
              WorkbenchCurrency.EXALTED,
              Set.of(),
              200000);
      assertThat(result.status()).isEqualTo(BasicCurrencyTransitions.Status.COMPLETE);
      assertThat(
              result.outcomes().stream()
                  .map(BasicCurrencyTransitions.Outcome::probability)
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      if (start.explicits().isEmpty()) {
        assertThat(result.outcomes())
            .anyMatch(
                o ->
                    match.apply(o.state().item()).groups().getFirst().entries().getFirst().status()
                        == Status.MATCH);
        assertThat(result.outcomes())
            .anyMatch(
                o ->
                    match.apply(o.state().item()).groups().getFirst().entries().getLast().status()
                        == Status.MATCH);
      } else
        assertThat(result.outcomes())
            .anyMatch(o -> match.apply(o.state().item()).status() == Status.MATCH);
    }
    var life = modifier("base_maximum_life", 50);
    var goal =
        goal(
            target("base_maximum_life", 50),
            new Entry(
                "chaos",
                GoalCatalogIndex.id("explicit", "base_chaos_damage_resistance_%"),
                "percent",
                new Range(BigDecimal.ONE, null),
                null,
                false));
    var evaluator = service.compileEvaluation(item(life), goal);
    assertThat(evaluator.apply(item(life)).groups().getFirst().entries().getFirst().status())
        .isEqualTo(Status.MATCH);
    var result =
        transitions.expand(
            new BasicCurrencyState(item(life), transitions.provenance()),
            WorkbenchCurrency.EXALTED,
            Set.of(),
            200000);
    assertThat(result.status()).isEqualTo(BasicCurrencyTransitions.Status.COMPLETE);
    assertThat(result.outcomes())
        .anyMatch(o -> evaluator.apply(o.state().item()).status() == Status.MATCH);
    assertThat(evaluator.apply(item()).groups().getFirst().entries().getFirst().status())
        .isEqualTo(Status.NO_MATCH);
  }

  @Test
  void solarRootABCDiamondUsesReviewedNumericGoalsAndReevaluatesLossAndReacquisition() {
    var a = modifier(PROJECTILE, 3);
    var b = modifier(SPIRIT, 47);
    var projectile = goal(target(PROJECTILE, 3));
    var spirit = goal(target(SPIRIT, 47));
    var both = goal(target(PROJECTILE, 3), target(SPIRIT, 47));
    assertThat(service.evaluate(item(), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(a), projectile).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(b), spirit).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(a), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(b), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(a, b), both).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(modifier(PROJECTILE, 2), b), both).status())
        .isEqualTo(Status.NO_MATCH);
    var compiled = service.compile(item(a, b), both);
    assertThat(compiled.apply(item(b))).isEqualTo(Status.NO_MATCH);
    assertThat(compiled.apply(item(a, b))).isEqualTo(Status.MATCH);
  }
}
