package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.ExactNumericDistribution.*;
import com.poe2craft.shared.crafting.domain.AdditionTransitions;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * 실제 옵션 풀에서 선택한 결과를 구체적인 수치 상태 분포로 확장한다.
 *
 * <p>기존 계약: Selection uses the existing pool, while roll outcomes retain the entire concrete item.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code MODEL}: 모형; 초기값 {@code "solar-numeric-addition-v1"} ({@code String}).
 *   <li>{@code LEDGER}: 규칙장부; 초기값 {@code "uniform-integer-roll-v1"} ({@code String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code transitions}: 전이목록 ({@code AdditionTransitions}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code NumericAdditionKernel(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그).
 *   <li>{@code supports(catalog)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code catalog}(카탈로그). 반환: {@code
 *       boolean}.
 *   <li>{@code expand(item, action, edgeBudget)}: 다음 상태 또는 분포 구간을 확장한다. 입력: {@code item}(아이템),
 *       {@code action}(행동), {@code edgeBudget}(간선·계산예산). 반환: {@code Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code complement(mass)}: complement 처리에서 {@code mass.denominator}, {@code
 *       mass.numerator}을(를) 사용한다. 입력: {@code mass}(확률질량). 반환: {@code Fraction}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Result}: {@code NumericAdditionKernel}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class NumericAdditionKernel {
  public static final String MODEL = "solar-numeric-addition-v1";
  public static final String LEDGER = "uniform-integer-roll-v1";
  private final ItemCatalog catalog;
  private final AdditionTransitions transitions;

  public NumericAdditionKernel(ItemCatalog catalog) {
    this.catalog = catalog;
    transitions = new AdditionTransitions(catalog);
  }

  public static boolean supports(ItemCatalog catalog) {
    return catalog != null
        && catalog.base().id().equals(SolarAmulet.BASE_ID)
        && catalog.modifiers().values().stream().allMatch(d -> d.stats().size() == 1);
  }

  /**
   * {@code NumericAdditionKernel}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   * </ul>
   */
  public record Result(
      boolean available, String reason, List<Outcome> outcomes, Fraction unresolved) {}

  /** An interrupted expansion returns the exact complement; it never renormalizes survivors. */
  public Result expand(ItemState item, WorkbenchCurrency action, BooleanSupplier edgeBudget) {
    var selected = transitions.transition(StateBucket.from(item), action, Set.of());
    if (!selected.available())
      return new Result(false, selected.reason(), List.of(), Fraction.ZERO);
    var outcomes = new ArrayList<Outcome>();
    var emitted = Fraction.ZERO;
    for (var candidate : selected.outcomes()) {
      var definition = catalog.find(candidate.addedModifierId()).orElseThrow();
      if (definition.stats().size() != 1)
        throw new IllegalArgumentException("Joint rolls unsupported");
      var range = definition.stats().getFirst();
      var count =
          BigInteger.valueOf(range.max())
              .subtract(BigInteger.valueOf(range.min()))
              .add(BigInteger.ONE);
      var mass =
          new Fraction(
              BigInteger.valueOf(candidate.weight()),
              BigInteger.valueOf(candidate.totalWeight()).multiply(count));
      for (long value = range.min(); ; value++) {
        if (!edgeBudget.getAsBoolean())
          return new Result(true, "BUDGET_EXHAUSTED", List.copyOf(outcomes), complement(emitted));
        var explicit = new ArrayList<>(item.explicits());
        explicit.add(new ModifierInstance(definition.id(), Map.of(range.id(), value)));
        var next =
            new ItemState(
                    item.snapshotId(),
                    item.baseItemId(),
                    item.itemLevel(),
                    candidate.state().rarity(),
                    item.implicits(),
                    explicit,
                    item.conditions(),
                    item.augmentSockets(),
                    item.catalystQuality())
                .withAugments(item.installedAugments())
                .withQualityInfused(item.qualityInfused());
        outcomes.add(new Outcome(next, mass));
        emitted = emitted.add(mass);
        if (value == range.max()) break;
      }
    }
    if (!emitted.equals(Fraction.ONE)) throw new IllegalStateException("Invalid kernel mass");
    return new Result(true, "", List.copyOf(outcomes), Fraction.ZERO);
  }

  private static Fraction complement(Fraction mass) {
    return new Fraction(mass.denominator().subtract(mass.numerator()), mass.denominator());
  }
}
