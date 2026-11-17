package com.poe2craft.workbench.repeat;

import static com.poe2craft.configuration.PublicContracts.REPEAT;
import static com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.workbench.display.DisplayLocale;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.random.RandomGenerator;

/**
 * 시간·횟수·취소를 확인하며 반복 실행을 조정하고 이미 끝난 사용 결과를 보존한다.
 *
 * <p>기존 계약: Bounded sequential batches; completed uses survive cancellation or time limits.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code workbench}: 작업대 ({@code CraftingOperations}).
 *   <li>{@code display}: 표시 ({@code ChaosDisplayResources}).
 *   <li>{@code rulesetIdentity}: 현재 실행 규칙 묶음의 식별자 ({@code String}).
 *   <li>{@code concreteApply}: 구체값·적용 ({@code ChaosRepeatRunner.Apply}).
 *   <li>{@code abyssApply}: 훼손·적용 ({@code AbyssApply}).
 *   <li>{@code admission}: admission ({@code Semaphore}).
 *   <li>{@code budgetNanos}: 계산예산·나노초 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosRepeatService(workbench, display, identity)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code workbench}(작업대), {@code display}(표시), {@code identity}(정체성).
 *   <li>{@code ChaosRepeatService(workbench, display, identity, settings)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code workbench}(작업대), {@code display}(표시), {@code identity}(정체성), {@code
 *       settings}(운영설정).
 *   <li>{@code ChaosRepeatService(workbench, display, identity, concreteApply, abyssApply)}: 기본 정책을
 *       보충해 공통 생성자로 위임한다. 입력: {@code workbench}(작업대), {@code display}(표시), {@code identity}(정체성),
 *       {@code concreteApply}(구체값·적용), {@code abyssApply}(훼손·적용).
 *   <li>{@code ChaosRepeatService(workbench, display, identity, concreteApply, abyssApply,
 *       settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code workbench}(작업대), {@code display}(표시), {@code
 *       identity}(정체성), {@code concreteApply}(구체값·적용), {@code abyssApply}(훼손·적용), {@code
 *       settings}(운영설정).
 *   <li>{@code run(input, random)}: 작업의 실행 루프를 수행한다. 입력: {@code input}(입력), {@code random}(난수생성기).
 *       반환: {@code Outcome}.
 *   <li>{@code run(input, random, cancelled)}: 작업의 실행 루프를 수행한다. 입력: {@code input}(입력), {@code
 *       random}(난수생성기), {@code cancelled}(취소여부). 반환: {@code Outcome}. 사용하는 예외: {@code
 *       RepeatRejected}.
 *   <li>{@code matches(state, catalog, locale, pattern)}: 일치결과 처리에서 {@code display.format}, {@code
 *       pattern.find}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그), {@code locale}(언어코드),
 *       {@code pattern}(패턴). 반환: {@code boolean}.
 *   <li>{@code abyss(input, binding, catalog, locale, pattern, random, stop)}: 훼손 처리에서 {@code
 *       input.state}, {@code input.activeOmens}을(를) 사용한다. 입력: {@code input}(입력), {@code
 *       binding}(결합), {@code catalog}(카탈로그), {@code locale}(언어코드), {@code pattern}(패턴), {@code
 *       random}(난수생성기), {@code stop}(stop). 반환: {@code Outcome}.
 *   <li>{@code outcome(binding, state, count, consumed, remaining, witness, termination, rejection,
 *       failure)}: 결과의 입력에 따른 분기·결과를 처리한다. 입력: {@code binding}(결합), {@code state}(상태), {@code
 *       count}(개수), {@code consumed}(소비된), {@code remaining}(남은), {@code witness}(증명상태), {@code
 *       termination}(종료), {@code rejection}(거절), {@code failure}(실패). 반환: {@code Outcome}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code AbyssApply}: {@code ChaosRepeatService} 내부의 훼손·적용 처리를 맡는다.
 * </ul>
 */
public final class ChaosRepeatService {
  private final CraftingOperations workbench;
  private final ChaosDisplayResources display;
  private final String rulesetIdentity;
  private final ChaosRepeatRunner.Apply concreteApply;
  private final AbyssApply abyssApply;
  private final Semaphore admission;
  private final long budgetNanos;

  /**
   * {@code ChaosRepeatService} 내부의 훼손·적용 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code apply(state, action, omens, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code
   *       state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환: {@code
   *       AbyssCurrencyExecutor.Result}.
   * </ul>
   */
  @FunctionalInterface
  interface AbyssApply {
    AbyssCurrencyExecutor.Result apply(
        AbyssCraftingState state,
        WorkbenchCurrency action,
        Set<String> omens,
        RandomGenerator random);
  }

  public ChaosRepeatService(
      CraftingOperations workbench, ChaosDisplayResources display, String identity) {
    this(workbench, display, identity, BackendSettings.defaults().workbench());
  }

  public ChaosRepeatService(
      CraftingOperations workbench,
      ChaosDisplayResources display,
      String identity,
      BackendSettings.Workbench settings) {
    this(workbench, display, identity, workbench::apply, workbench::applyAbyssCurrency, settings);
  }

  ChaosRepeatService(
      CraftingOperations workbench,
      ChaosDisplayResources display,
      String identity,
      ChaosRepeatRunner.Apply concreteApply,
      AbyssApply abyssApply) {
    this(
        workbench,
        display,
        identity,
        concreteApply,
        abyssApply,
        BackendSettings.defaults().workbench());
  }

  ChaosRepeatService(
      CraftingOperations workbench,
      ChaosDisplayResources display,
      String identity,
      ChaosRepeatRunner.Apply concreteApply,
      AbyssApply abyssApply,
      BackendSettings.Workbench settings) {
    this.workbench = Objects.requireNonNull(workbench);
    this.display = Objects.requireNonNull(display);
    this.rulesetIdentity = Objects.requireNonNull(identity);
    this.concreteApply = Objects.requireNonNull(concreteApply);
    this.abyssApply = Objects.requireNonNull(abyssApply);
    this.budgetNanos = settings.repeatTimeBudget().toNanos();
    this.admission = new Semaphore(settings.repeatConcurrency());
  }

  public Outcome run(Input input, RandomGenerator random) {
    return run(input, random, () -> false);
  }

  public Outcome run(
      Input input, RandomGenerator random, java.util.function.BooleanSupplier cancelled) {
    long deadline = System.nanoTime() + budgetNanos;
    java.util.function.BooleanSupplier stop =
        () ->
            Thread.currentThread().isInterrupted()
                || System.nanoTime() >= deadline
                || cancelled.getAsBoolean();
    if (!admission.tryAcquire())
      throw new RepeatRejected(
          429,
          "CHAOS_REPEAT_BUSY",
          "Another Chaos repeat is running. No uses were performed for this request.");
    try {
      if (input == null
          || random == null
          || input.state() == null
          || input.action() == null
          || input.maximumUses() < REPEAT.minimumUses()
          || input.maximumUses() > REPEAT.maximumUses()
          || !Set.of(
                  WorkbenchCurrency.CHAOS,
                  WorkbenchCurrency.GREATER_CHAOS,
                  WorkbenchCurrency.PERFECT_CHAOS)
              .contains(input.action())) throw RepeatRejected.input();
      var locale = DisplayLocale.fromCode(input.locale());
      input.activeOmens().forEach(WorkbenchOmen::fromId);
      var initial = input.state().displayItem();
      if (!initial.conditions().isEmpty() || initial.rarity() != ItemState.Rarity.RARE)
        throw RepeatRejected.input();
      var catalog = workbench.abyssCatalog(initial);
      if (input.state() instanceof Abyss abyss) abyss.abyss().validate(catalog);
      else {
        if (!initial.desecrated().isEmpty()
            || !new ItemStateValidator(catalog).validate(initial).isEmpty())
          throw RepeatRejected.input();
        workbench.actions(initial, input.activeOmens());
      }
      display.format(initial, catalog, locale); // capability validation, never an initial match
      var pattern = ChaosRepeatPattern.compile(input.pattern());
      var binding =
          new Binding(
              input.requestId(),
              input.requestSha256(),
              input.action(),
              input.locale(),
              input.maximumUses(),
              ChaosRepeatPattern.VERSION);
      if (input.state() instanceof Concrete concrete) {
        var result =
            new ChaosRepeatRunner(concreteApply)
                .run(
                    concrete.item(),
                    input.action(),
                    input.activeOmens(),
                    input.maximumUses(),
                    random,
                    state -> matches(state, catalog, locale, pattern),
                    stop);
        return outcome(
            binding,
            new Concrete(result.state()),
            result.completedUses(),
            result.consumedOmens(),
            result.remainingOmens(),
            ConcreteWitness.from(result.lastAppliedResult()),
            result.termination() == ChaosRepeatRunner.Termination.STOP_REQUESTED
                ? "ERROR"
                : result.termination().name(),
            result.lastResult() != null && !result.lastResult().applied()
                ? new Rejection("CURRENCY_INAPPLICABLE", result.lastResult().reason())
                : null,
            result.termination() == ChaosRepeatRunner.Termination.STOP_REQUESTED
                ? new Failure("REPEAT_EXECUTION_FAILED", "STOP_SIGNAL")
                : result.failureStage() == null
                    ? null
                    : new Failure("REPEAT_EXECUTION_FAILED", result.failureStage().name()));
      }
      return abyss(input, binding, catalog, locale, pattern, random, stop);
    } catch (RepeatRejected rejected) {
      throw rejected;
    } catch (IllegalArgumentException invalid) {
      throw RepeatRejected.input();
    } finally {
      admission.release();
    }
  }

  private boolean matches(
      ItemState state, ItemCatalog catalog, DisplayLocale locale, ChaosRepeatPattern pattern) {
    return display.format(state, catalog, locale).groups().stream()
        .anyMatch(group -> pattern.find(group.text()));
  }

  private Outcome abyss(
      Input input,
      Binding binding,
      ItemCatalog catalog,
      DisplayLocale locale,
      ChaosRepeatPattern pattern,
      RandomGenerator random,
      java.util.function.BooleanSupplier stop) {
    var state = ((Abyss) input.state()).abyss();
    var remaining = input.activeOmens();
    var consumed = new HashMap<String, Integer>();
    int count = 0;
    AbyssWitness witness = null;
    String stage = "APPLY";
    try {
      while (count < input.maximumUses()) {
        stage = "STOP_SIGNAL";
        if (stop.getAsBoolean())
          return outcome(
              binding,
              new Abyss(state),
              count,
              consumed,
              remaining,
              witness,
              "ERROR",
              null,
              new Failure("REPEAT_EXECUTION_FAILED", stage));
        stage = "APPLY";
        var step = abyssApply.apply(state, input.action(), remaining, random);
        if (!step.applied())
          return outcome(
              binding,
              new Abyss(state),
              count,
              consumed,
              remaining,
              witness,
              "INAPPLICABLE",
              new Rejection("CURRENCY_INAPPLICABLE", step.reason()),
              null);
        state = step.state();
        count++;
        witness = AbyssWitness.from(step);
        remaining = Set.copyOf(step.remainingOmens());
        step.consumedOmens().forEach(id -> consumed.merge(id, 1, Integer::sum));
        stage = "MATCHER";
        if (matches(state.storedItem(), catalog, locale, pattern))
          return outcome(
              binding, new Abyss(state), count, consumed, remaining, witness, "MATCH", null, null);
      }
      return outcome(
          binding, new Abyss(state), count, consumed, remaining, witness, "LIMIT", null, null);
    } catch (RuntimeException failure) {
      return outcome(
          binding,
          new Abyss(state),
          count,
          consumed,
          remaining,
          witness,
          "ERROR",
          null,
          new Failure("REPEAT_EXECUTION_FAILED", stage));
    }
  }

  private Outcome outcome(
      Binding binding,
      State state,
      int count,
      Map<String, Integer> consumed,
      Set<String> remaining,
      Witness witness,
      String termination,
      Rejection rejection,
      Failure failure) {
    return new Outcome(
        1,
        "CHAOS_REPEAT",
        rulesetIdentity,
        binding,
        state,
        count,
        consumed,
        remaining,
        witness,
        termination,
        rejection,
        failure);
  }
}
