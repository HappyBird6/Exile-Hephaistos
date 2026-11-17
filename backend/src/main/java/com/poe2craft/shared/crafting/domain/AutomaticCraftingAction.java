package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;

/**
 * 아이템과 별개로 실행할 화폐 및 소비할 징조의 정체성을 표현한다.
 *
 * <p>기존 계약: Execution identity is separate from ItemState; an omen here is consumed by this action.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code currency}: 화폐 ({@code WorkbenchCurrency}).
 *   <li>{@code omens}: 징조목록 ({@code Set<WorkbenchOmen>}).
 *   <li>{@code purpose}: 목적 ({@code AutomaticCurrencyPolicy.Purpose}).
 *   <li>{@code operation}: 연산 ({@code Operation}).
 *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
 *   <li>{@code comparison}: 비교 ({@code AbyssProbabilityComparison.Comparison}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticCraftingAction(currency, omens, purpose)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code currency}(화폐), {@code omens}(징조목록), {@code purpose}(목적).
 *   <li>{@code desecrate(side)}: 훼손의 입력에 따른 분기·결과를 처리한다. 입력: {@code side}(접두접미). 반환: {@code
 *       AutomaticCraftingAction}.
 *   <li>{@code AutomaticCraftingAction(currency, omens, purpose, operation, side, comparison)}:
 *       전달받은 값으로 구성원을 초기화한다. 입력: {@code currency}(화폐), {@code omens}(징조목록), {@code purpose}(목적),
 *       {@code operation}(연산), {@code side}(접두접미), {@code comparison}(비교). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code candidates(policy)}: 후보목록 처리에서 {@code policy.candidates}, {@code entry.id}을(를) 사용한다.
 *       입력: {@code policy}(정책). 반환: {@code List<AutomaticCraftingAction>}.
 *   <li>{@code omenIds()}: 징조·식별자목록 값을 조회·구성한다. 반환: {@code Set<String>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Operation}: {@code AutomaticCraftingAction}의 연산 구분값을 정의한다.
 * </ul>
 */
public record AutomaticCraftingAction(
    WorkbenchCurrency currency,
    Set<WorkbenchOmen> omens,
    AutomaticCurrencyPolicy.Purpose purpose,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        Operation operation,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        ModifierDefinition.AffixType side,
    @com.fasterxml.jackson.annotation.JsonInclude(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        AbyssProbabilityComparison.Comparison comparison) {
  /**
   * {@code AutomaticCraftingAction}의 연산 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code DESECRATE}: 훼손 구분값.
   * </ul>
   */
  public enum Operation {
    DESECRATE
  }

  public AutomaticCraftingAction(
      WorkbenchCurrency currency,
      Set<WorkbenchOmen> omens,
      AutomaticCurrencyPolicy.Purpose purpose) {
    this(currency, omens, purpose, null, null, null);
  }

  public static AutomaticCraftingAction desecrate(ModifierDefinition.AffixType side) {
    return new AutomaticCraftingAction(
        null, Set.of(), AutomaticCurrencyPolicy.Purpose.ORDINARY, Operation.DESECRATE, side, null);
  }

  public AutomaticCraftingAction {
    if (operation == null) Objects.requireNonNull(currency);
    else if (currency != null
        || side == null
        || side == ModifierDefinition.AffixType.NONE
        || !omens.isEmpty()) throw new IllegalArgumentException("Invalid non-currency operation");
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
          currency == WorkbenchCurrency.TRANSMUTATION
              ? AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE
              : temporary
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
    return result.stream()
        .filter(
            a ->
                a.omens().stream()
                    .allMatch(
                        o ->
                            policy.evaluate(
                                    "OMEN:" + o.name(),
                                    new AutomaticCurrencyPolicy.Context(
                                        "amulets", a.purpose(), a.currency()))
                                == AutomaticCurrencyPolicy.Verdict.PERMITTED))
        .toList();
  }

  public Set<String> omenIds() {
    return omens.stream()
        .map(WorkbenchOmen::id)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
