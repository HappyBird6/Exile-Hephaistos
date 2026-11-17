package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 옵션 선택만 사용자에게 맡기고 제작 실행과 상태 보존은 공통 규칙을 사용한다.
 *
 * <p>기존 계약: User selection changes only modifier selection; execution and state preservation stay
 * shared.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CustomExaltService(crafting)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code crafting}(제작).
 *   <li>{@code resolve(state, source)}: 해결 처리에서 {@code CraftingItemState.resolve}, {@code
 *       crafting.abyssCatalog}을(를) 사용한다. 입력: {@code state}(상태), {@code source}(출처). 반환: {@code
 *       CraftingItemState}.
 *   <li>{@code executor(state)}: 작업실행기 처리에서 {@code crafting.abyssCatalog}, {@code
 *       crafting.simulator}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code AbyssCurrencyExecutor}.
 *   <li>{@code choices(state, source, action, omens)}: 선택목록 처리에서 {@code current.slots}을(를) 사용한다.
 *       입력: {@code state}(상태), {@code source}(출처), {@code action}(행동), {@code omens}(징조목록). 반환:
 *       {@code List<WorkbenchSimulator.ExaltChoice>}.
 *   <li>{@code apply(state, source, action, omens, modifierId, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다.
 *       입력: {@code state}(상태), {@code source}(출처), {@code action}(행동), {@code omens}(징조목록), {@code
 *       modifierId}(옵션·식별자), {@code random}(난수생성기). 반환: {@code Applied}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Applied}: {@code CustomExaltService}에서 사용하는 적용여부의 불변 값이다.
 * </ul>
 */
public final class CustomExaltService {
  private final CraftingOperations crafting;

  public CustomExaltService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  /**
   * {@code CustomExaltService}에서 사용하는 적용여부의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code result}: 결과 ({@code WorkbenchSimulator.Result}).
   *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code AbyssCraftingState}).
   *   <li>{@code choice}: 선택 ({@code WorkbenchSimulator.ExaltChoice}).
   * </ul>
   */
  public record Applied(
      WorkbenchSimulator.Result result,
      AbyssCraftingState abyss,
      WorkbenchSimulator.ExaltChoice choice) {}

  private CraftingItemState resolve(ItemState state, AbyssCraftingState source) {
    return CraftingItemState.resolve(state, source, crafting.abyssCatalog(state));
  }

  private AbyssCurrencyExecutor executor(ItemState state) {
    return new AbyssCurrencyExecutor(crafting.abyssCatalog(state), crafting.simulator(state));
  }

  public List<WorkbenchSimulator.ExaltChoice> choices(
      ItemState state, AbyssCraftingState source, WorkbenchCurrency action, Set<String> omens) {
    var current = resolve(state, source);
    return executor(state).exaltChoices(current.slots(), action, omens);
  }

  public Applied apply(
      ItemState state,
      AbyssCraftingState source,
      WorkbenchCurrency action,
      Set<String> omens,
      String modifierId,
      RandomGenerator random) {
    var current = resolve(state, source);
    var executor = executor(state);
    var choice =
        executor.exaltChoices(current.slots(), action, omens).stream()
            .filter(c -> c.modifierId().equals(modifierId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Choose an eligible Exalted modifier"));
    var transition = executor.applyChosenExalt(current.slots(), action, omens, modifierId, random);
    return new Applied(
        transition.receipt(),
        source != null || !state.desecrated().isEmpty() ? transition.outcome().state() : null,
        choice);
  }
}
