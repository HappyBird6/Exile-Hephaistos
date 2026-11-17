package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import java.util.*;

/**
 * 옵션 ID 선택의 유한 확률 분포를 계산하고 수치 굴림은 주변화한다.
 *
 * <p>기존 계약: Exhaustive modifier-ID distribution. Numeric roll outcomes are marginalized, never
 * sampled.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code rules}: 규칙목록 ({@code AdditionRules}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AdditionTransitions(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그).
 *   <li>{@code transition(state, action, activeOmens)}: 전이 처리에서 {@code rules.plan}, {@code
 *       plan.available}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       activeOmens}(활성·징조목록). 반환: {@code Result}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Outcome}: {@code AdditionTransitions}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Result}: {@code AdditionTransitions}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class AdditionTransitions {
  private final AdditionRules rules;

  public AdditionTransitions(ItemCatalog catalog) {
    rules = new AdditionRules(catalog);
  }

  public Result transition(StateBucket state, WorkbenchCurrency action, Set<String> activeOmens) {
    var plan = rules.plan(state, action, activeOmens);
    if (!plan.available())
      return new Result(
          WorkbenchSimulator.RULE_VERSION,
          WorkbenchSimulator.LEDGER_VERSION,
          state,
          action,
          false,
          plan.reason(),
          List.of(),
          List.of(),
          plan.remainingOmens());
    long total = 0;
    for (var candidate : plan.candidates()) total = Math.addExact(total, candidate.weight());
    final long denominator = total;
    var outcomes =
        plan.candidates().stream()
            .map(
                candidate -> {
                  var ids = new ArrayList<>(state.modifierIds());
                  ids.add(candidate.id());
                  return new Outcome(
                      plan.upgraded().with(plan.upgraded().rarity(), ids),
                      candidate.id(),
                      candidate.weight(),
                      denominator);
                })
            .toList();
    return new Result(
        WorkbenchSimulator.RULE_VERSION,
        WorkbenchSimulator.LEDGER_VERSION,
        state,
        action,
        true,
        "",
        outcomes,
        plan.consumedOmens(),
        plan.remainingOmens());
  }

  /**
   * {@code AdditionTransitions}에서 사용하는 결과의 불변 값이다.
   *
   * <p>기존 계약: Integer numerator/denominator preserve the exact per-edge published-weight ratio.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code addedModifierId}: added·옵션·식별자 ({@code String}).
   *   <li>{@code weight}: 후보 선택에 사용하는 가중치 ({@code long}).
   *   <li>{@code totalWeight}: 합계·가중치 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code probability()}: 현재 조건의 확률을 조회한다. 반환: {@code double}.
   * </ul>
   */
  public record Outcome(StateBucket state, String addedModifierId, long weight, long totalWeight) {
    public double probability() {
      return (double) weight / totalWeight;
    }
  }

  /**
   * {@code AdditionTransitions}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code source}: 출처 ({@code StateBucket}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Result(ruleVersion, ledgerVersion, source, action, available, reason, outcomes,
   *       consumedOmens, remainingOmens)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code ruleVersion}(규칙·버전),
   *       {@code ledgerVersion}(규칙장부·버전), {@code source}(출처), {@code action}(행동), {@code
   *       available}(사용가능), {@code reason}(이유), {@code outcomes}(결과목록), {@code
   *       consumedOmens}(소비된·징조목록), {@code remainingOmens}(남은·징조목록).
   * </ul>
   */
  public record Result(
      String ruleVersion,
      String ledgerVersion,
      StateBucket source,
      WorkbenchCurrency action,
      boolean available,
      String reason,
      List<Outcome> outcomes,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Result {
      outcomes = List.copyOf(outcomes);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
