package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 화폐·징조 조합을 방법 실행으로 묶고 개별 행동의 최초 성공 의미를 유지한다.
 *
 * <p>기존 계약: Method transport for approved currency/omen executions, with primitive first-hit
 * semantics.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "automatic-method-execution-v6-joint-lineage"} ({@code
 *       String}).
 *   <li>{@code runner}: 실행기 ({@code CertifiedMethodRunner<State, AutomaticCraftingAction>}).
 *   <li>{@code initializingRoot}: 초기화중·시작점; 초기값 {@code true} ({@code boolean}).
 *   <li>{@code root}: 시작점 ({@code ItemState}).
 *   <li>{@code initialTiers}: 초기값·티어목록 ({@code Map<String, TierCell>}).
 *   <li>{@code symbolicSource}: symbolic·출처 ({@code boolean}).
 *   <li>{@code initialQuality40}: 초기값·품질·40 ({@code boolean}).
 *   <li>{@code initialAbstractQuality}: 초기값·추상화된·품질 ({@code int}).
 *   <li>{@code template}: 템플릿 ({@code Template}).
 *   <li>{@code preparedRetryAnchor}: 준비된·재시도·Anchor ({@code ItemState}).
 *   <li>{@code preparedRepresentatives}: 준비된·대표상태목록 ({@code Map<ItemState, ItemState>}).
 *   <li>{@code preparedTransitionState}: 준비된·전이·상태 ({@code State}).
 *   <li>{@code preparedTransitionAction}: 준비된·전이·행동 ({@code AutomaticCraftingAction}).
 *   <li>{@code preparedTransition}: 준비된·전이 ({@code Kernel<State>}).
 *   <li>{@code kernel}: 전이커널 ({@code AutomaticCraftingKernel}).
 *   <li>{@code classify}: 상태분류 ({@code java.util.function.BiFunction<ItemState, Integer,
 *       Optional<Exit>>}).
 *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
 *   <li>{@code primitiveOutcomeBudget}: 개별행동·결과·계산예산 ({@code long}).
 *   <li>{@code primitiveLimited}: 개별행동·제한된 ({@code boolean}).
 *   <li>{@code primitiveCursor}: 개별행동·진행커서 ({@code AutomaticCraftingKernel.PrimitiveCursor}).
 *   <li>{@code primitiveState}: 개별행동·상태 ({@code State}).
 *   <li>{@code primitiveAction}: 개별행동·행동 ({@code AutomaticCraftingAction}).
 *   <li>{@code preparationSuccess}: 준비·성공 ({@code PreparationSuccess}).
 *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
 *   <li>{@code unresolvedReasons}: 미해결·이유목록 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code
 *       template}(템플릿), {@code kernel}(전이커널), {@code classify}(상태분류), {@code precision}(정밀도),
 *       {@code primitiveOutcomeBudget}(개별행동·결과·계산예산).
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget, preparationSuccess)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       root}(시작점), {@code template}(템플릿), {@code kernel}(전이커널), {@code classify}(상태분류), {@code
 *       precision}(정밀도), {@code primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code
 *       preparationSuccess}(준비·성공).
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget, preparationSuccess, initialQuality40)}: 기본 정책을 보충해 공통 생성자로 위임한다.
 *       입력: {@code root}(시작점), {@code template}(템플릿), {@code kernel}(전이커널), {@code classify}(상태분류),
 *       {@code precision}(정밀도), {@code primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code
 *       preparationSuccess}(준비·성공), {@code initialQuality40}(초기값·품질·40).
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget, preparationSuccess, initialQuality40, initialAbstractQuality)}: 기본
 *       정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code template}(템플릿), {@code kernel}(전이커널),
 *       {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code preparationSuccess}(준비·성공), {@code
 *       initialQuality40}(초기값·품질·40), {@code initialAbstractQuality}(초기값·추상화된·품질).
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget, preparationSuccess, initialQuality40, initialAbstractQuality,
 *       initialTiers)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code template}(템플릿),
 *       {@code kernel}(전이커널), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code preparationSuccess}(준비·성공), {@code
 *       initialQuality40}(초기값·품질·40), {@code initialAbstractQuality}(초기값·추상화된·품질), {@code
 *       initialTiers}(초기값·티어목록).
 *   <li>{@code AutomaticMethodExecution(root, template, kernel, classify, precision,
 *       primitiveOutcomeBudget, preparationSuccess, initialQuality40, initialAbstractQuality,
 *       initialTiers, symbolicSource)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code root}(시작점), {@code
 *       template}(템플릿), {@code kernel}(전이커널), {@code classify}(상태분류), {@code precision}(정밀도),
 *       {@code primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code preparationSuccess}(준비·성공), {@code
 *       initialQuality40}(초기값·품질·40), {@code initialAbstractQuality}(초기값·추상화된·품질), {@code
 *       initialTiers}(초기값·티어목록), {@code symbolicSource}(symbolic·출처). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code compiledTemplate()}: compiled·템플릿 값을 조회·구성한다. 반환: {@code
 *       ExactMethodRunner.Template<State, AutomaticCraftingAction>}.
 *   <li>{@code transition(state, action)}: 전이 처리에서 {@code state.equals}, {@code action.equals}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code Kernel<State>}. 사용하는 예외: {@code
 *       PrimitiveBudgetReached}, {@code PrimitiveYield}.
 *   <li>{@code retained(source, outcome)}: 보유 처리에서 {@code source.retainedFromRoot}, {@code
 *       retained.retainAll}을(를) 사용한다. 입력: {@code source}(출처), {@code outcome}(결과). 반환: {@code
 *       Set<String>}.
 *   <li>{@code retryPlan(item)}: 재시도·계획 처리에서 {@code template.initial}, {@code
 *       AutomaticCraftingKernel.mutableAffixCount}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code
 *       AutomaticCraftingPlan}.
 *   <li>{@code preparedRetryCandidate()}: 준비된·재시도·후보 값을 조회·구성한다. 반환: {@code ItemState}.
 *   <li>{@code preparedRepresentative(item)}: 준비된·대표상태 처리에서 {@code classify.apply}, {@code
 *       preparedRepresentatives.computeIfAbsent}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code
 *       ItemState}.
 *   <li>{@code classifyState(state)}: 상태분류·상태 처리에서 {@code classify.apply}, {@code state.item}을(를)
 *       사용한다. 입력: {@code state}(상태). 반환: {@code Optional<Exit>}.
 *   <li>{@code completedQuality(state)}: 완료·품질 처리에서 {@code kernel.workedQuality}, {@code
 *       state.item}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code boolean}.
 *   <li>{@code checkpoint(objectiveIdentity)}: 현재 진행 상태의 복구 지점을 기록한다. 입력: {@code
 *       objectiveIdentity}(목적·정체성). 반환: {@code Checkpoint}.
 *   <li>{@code restore(saved, kernel, objectiveIdentity, classify)}: 저장된 상태로 실행 또는 조회 상태를 복원한다. 입력:
 *       {@code saved}(저장된), {@code kernel}(전이커널), {@code objectiveIdentity}(목적·정체성), {@code
 *       classify}(상태분류). 반환: {@code AutomaticMethodExecution}.
 *   <li>{@code restore(saved, kernel, objectiveIdentity, classify)}: 저장된 상태로 실행 또는 조회 상태를 복원한다. 입력:
 *       {@code saved}(저장된), {@code kernel}(전이커널), {@code objectiveIdentity}(목적·정체성), {@code
 *       classify}(상태분류). 반환: {@code AutomaticMethodExecution}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code atBoundary()}: 기준·경계 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code absorbed()}: 흡수된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code cycles()}: 주기횟수 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code retryRepresentatives()}: 재시도·대표상태목록 값을 조회·구성한다. 반환: {@code Set<ItemState>}.
 *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code hasPrimitiveInFlight()}: 개별행동·입력·진행중을(를) 보유 여부를 판정한다. 반환: {@code boolean}.
 *   <li>{@code beginCycle()}: 시작·주기 처리에서 {@code runner.beginCycle}을(를) 사용한다.
 *   <li>{@code step(outcomes)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code outcomes}(결과목록).
 *   <li>{@code resourceLimited()}: {@code primitiveLimited} 값을 조회한다. 반환: {@code boolean}.
 *   <li>{@code increasePrimitiveBudget(budget)}: 개별행동·계산예산을(를) 한도를 늘린다. 입력: {@code budget}(계산예산).
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code observation()}: 관측값 값을 조회·구성한다. 반환: {@code CertifiedMethodRunner.Observation<State,
 *       AutomaticCraftingAction>}.
 *   <li>{@code provisional(horizon)}: provisional 처리에서 {@code runner.atBoundary}, {@code
 *       runner.absorbed}을(를) 사용한다. 입력: {@code horizon}(계산기간). 반환: {@code
 *       CertifiedMethodRunner.Observation<State, AutomaticCraftingAction>}.
 *   <li>{@code usageBounds(spent, actions, repetitions)}: 사용량·경계값 처리에서 {@code actions.forEach},
 *       {@code counts.merge}을(를) 사용한다. 입력: {@code spent}(spent), {@code actions}(행동목록), {@code
 *       repetitions}(repetitions). 반환: {@code Map<AutomaticCraftingAction, CertifiedInterval>}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code unresolvedReasons()}: 미해결·이유목록 값을 조회·구성한다. 반환: {@code Set<String>}.
 *   <li>{@code materialUses(uses)}: 제작재료·사용량 처리에서 {@code uses.forEach}, {@code action.purpose}을(를)
 *       사용한다. 입력: {@code uses}(사용량). 반환: {@code Map<String, CertifiedInterval>}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code PreparationSuccess}: {@code AutomaticMethodExecution}의 준비·성공 구분값을 정의한다.
 *   <li>{@code State}: {@code AutomaticMethodExecution}에서 사용하는 상태의 불변 값이다.
 *   <li>{@code Template}: {@code AutomaticMethodExecution}에서 사용하는 템플릿의 불변 값이다.
 *   <li>{@code PrimitiveBudgetReached}: {@code AutomaticMethodExecution} 내부의 개별행동·계산예산·Reached 처리를
 *       맡는다.
 *   <li>{@code PrimitiveYield}: {@code AutomaticMethodExecution} 내부의 개별행동·Yield 처리를 맡는다.
 *   <li>{@code Checkpoint}: {@code AutomaticMethodExecution}에서 사용하는 체크포인트의 불변 값이다.
 * </ul>
 */
public final class AutomaticMethodExecution implements AutoCloseable {
  public static final String VERSION = "automatic-method-execution-v6-joint-lineage";

  /**
   * {@code AutomaticMethodExecution}의 준비·성공 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code UNDECIDED}: UNDECIDED 구분값.
   *   <li>{@code STOP_ON_FIRST_HIT}: 목표도달 구분값.
   *   <li>{@code REQUIRE_REPLACEMENT}: 준비 단계의 우연한 적중을 생략하고 교체한다.
   *   <li>{@code REPEAT_UNTIL_FIRST_AFFIX}: 제작지원 세션에서 준비 후 첫 목표 옵션까지 손실 없는 카오스를 반복한다.
   * </ul>
   */
  public enum PreparationSuccess {
    UNDECIDED,
    STOP_ON_FIRST_HIT,
    REQUIRE_REPLACEMENT,
    REPEAT_UNTIL_FIRST_AFFIX
  }

  /**
   * {@code AutomaticMethodExecution}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code temporaryPreparation}: 임시·준비 ({@code boolean}).
   *   <li>{@code quality40}: 품질·40 ({@code boolean}).
   *   <li>{@code abstractQuality}: 추상화된·품질 ({@code int}).
   *   <li>{@code tiers}: 티어목록 ({@code Map<String, TierCell>}).
   *   <li>{@code intermediate}: 재료별 상태를 복원하기 위한 실제 중간 아이템 목록이다. 동일한 확률 상태에는 하나의 예시만 보존한다.
   *   <li>{@code withFinalStepChance(chance)}는 동일한 결과에 마지막 화폐의 조건부 확률을 기록한 복사본을 반환한다.
   *   <li>조건부 확률을 전달하지 않는 기존 생성자는 해당 값을 비워 둔다.
   *   <li>{@code finalStepChance}: 두 화폐를 연속 사용할 때 중간 상태에서 마지막 화폐가 해당 결과를 만들 조건부 확률이다.
   *   <li>{@code expectedChaos}: 첫 적중까지 카오스를 쓰는 결과의 조건부 평균 소모량이다.
   *   <li>{@code retainedFromRoot}: 보유·원본·시작점 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code State(item, temporaryPreparation, quality40, abstractQuality, tiers,
   *       retainedFromRoot)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code item}(아이템), {@code
   *       temporaryPreparation}(임시·준비), {@code quality40}(품질·40), {@code abstractQuality}(추상화된·품질),
   *       {@code tiers}(티어목록), {@code retainedFromRoot}(보유·원본·시작점). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code State(item, temporaryPreparation, quality40, abstractQuality, tiers)}: 기본 정책을 보충해
   *       공통 생성자로 위임한다. 입력: {@code item}(아이템), {@code temporaryPreparation}(임시·준비), {@code
   *       quality40}(품질·40), {@code abstractQuality}(추상화된·품질), {@code tiers}(티어목록).
   *   <li>{@code State(item, temporaryPreparation, quality40, abstractQuality)}: 기본 정책을 보충해 공통 생성자로
   *       위임한다. 입력: {@code item}(아이템), {@code temporaryPreparation}(임시·준비), {@code
   *       quality40}(품질·40), {@code abstractQuality}(추상화된·품질).
   *   <li>{@code State(item, temporaryPreparation, quality40)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       item}(아이템), {@code temporaryPreparation}(임시·준비), {@code quality40}(품질·40).
   *   <li>{@code State(item, temporaryPreparation)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       item}(아이템), {@code temporaryPreparation}(임시·준비).
   * </ul>
   */
  public record State(
      ItemState item,
      boolean temporaryPreparation,
      boolean quality40,
      int abstractQuality,
      Map<String, TierCell> tiers,
      Set<String> retainedFromRoot,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
          List<ItemState> intermediate,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Fraction expectedChaos,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Fraction finalStepChance) {
    public State(
        ItemState item,
        boolean temporaryPreparation,
        boolean quality40,
        int abstractQuality,
        Map<String, TierCell> tiers,
        Set<String> retainedFromRoot,
        List<ItemState> intermediate,
        Fraction expectedChaos) {
      this(
          item,
          temporaryPreparation,
          quality40,
          abstractQuality,
          tiers,
          retainedFromRoot,
          intermediate,
          expectedChaos,
          null);
    }

    State withFinalStepChance(Fraction chance) {
      return new State(
          item,
          temporaryPreparation,
          quality40,
          abstractQuality,
          tiers,
          retainedFromRoot,
          intermediate,
          expectedChaos,
          chance);
    }

    public State(
        ItemState item,
        boolean temporaryPreparation,
        boolean quality40,
        int abstractQuality,
        Map<String, TierCell> tiers,
        Set<String> retainedFromRoot) {
      this(
          item,
          temporaryPreparation,
          quality40,
          abstractQuality,
          tiers,
          retainedFromRoot,
          List.of(),
          null);
    }

    // 같은 확률 상태에는 실제로 관측한 중간 경로 하나만 보존한다.
    @Override
    public boolean equals(Object other) {
      return this == other
          || other instanceof State s
              && temporaryPreparation == s.temporaryPreparation
              && quality40 == s.quality40
              && abstractQuality == s.abstractQuality
              && item.equals(s.item)
              && tiers.equals(s.tiers)
              && retainedFromRoot.equals(s.retainedFromRoot)
              && Objects.equals(expectedChaos, s.expectedChaos)
              && Objects.equals(finalStepChance, s.finalStepChance);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          item,
          temporaryPreparation,
          quality40,
          abstractQuality,
          tiers,
          retainedFromRoot,
          expectedChaos,
          finalStepChance);
    }

    public State {
      intermediate = intermediate == null ? List.of() : List.copyOf(intermediate);
      tiers = tiers == null ? Map.of() : Map.copyOf(tiers);
      retainedFromRoot = retainedFromRoot == null ? Set.of() : Set.copyOf(retainedFromRoot);
      if (abstractQuality < 0 || abstractQuality > 40)
        throw new IllegalArgumentException("Invalid quality work amount");
      if (quality40) abstractQuality = 40;
    }

    public State(
        ItemState item,
        boolean temporaryPreparation,
        boolean quality40,
        int abstractQuality,
        Map<String, TierCell> tiers) {
      this(item, temporaryPreparation, quality40, abstractQuality, tiers, Set.of());
    }

    public State(
        ItemState item, boolean temporaryPreparation, boolean quality40, int abstractQuality) {
      this(item, temporaryPreparation, quality40, abstractQuality, Map.of());
    }

    public State(ItemState item, boolean temporaryPreparation, boolean quality40) {
      this(item, temporaryPreparation, quality40, quality40 ? 40 : 0);
    }

    public State(ItemState item, boolean temporaryPreparation) {
      this(item, temporaryPreparation, false);
    }
  }

  /**
   * {@code AutomaticMethodExecution}에서 사용하는 템플릿의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code initial}: 초기값 ({@code List<AutomaticCraftingAction>}).
   *   <li>{@code cycle}: 주기 ({@code List<AutomaticCraftingAction>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code boundedChaos()}: 한도내·카오스 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code Template(id, initial, cycle)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       initial}(초기값), {@code cycle}(주기). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Template(
      String id, List<AutomaticCraftingAction> initial, List<AutomaticCraftingAction> cycle) {
    public boolean boundedChaos() {
      return cycle.size() == 1
          && cycle.getFirst().currency() == WorkbenchCurrency.CHAOS
          && cycle.getFirst().omens().isEmpty()
          && cycle.getFirst().purpose() == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION;
    }

    /** 일반 장비에서 첫 카오스의 단일 교체 슬롯을 만드는 고정 준비 순서다. */
    boolean preparesNormalChaos() {
      return boundedChaos()
          && initial.size() == 3
          && initial.get(0).currency() == WorkbenchCurrency.TRANSMUTATION
          && initial.get(1).currency() == WorkbenchCurrency.REGAL
          && initial.get(2).currency() == WorkbenchCurrency.ANNULMENT
          && initial.stream().allMatch(a -> a.omens().isEmpty());
    }

    public Template {
      Objects.requireNonNull(id);
      initial = List.copyOf(initial);
      cycle = List.copyOf(cycle);
      if (id.isBlank() || cycle.isEmpty())
        throw new IllegalArgumentException("Method cycle required");
      var sequence = new ArrayList<>(initial);
      sequence.addAll(cycle);
      for (int i = 0; i < sequence.size(); i++) {
        var action = sequence.get(i);
        if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION) {
          if (action.currency() != WorkbenchCurrency.CHAOS
              || !action.omens().isEmpty()
              || i + 1 == sequence.size()
              || sequence.get(i + 1).currency() != WorkbenchCurrency.CHAOS
              || !sequence.get(i + 1).omens().contains(WorkbenchOmen.WHITTLING))
            throw new IllegalArgumentException(
                "Tier preparation requires a planned Whittling acquisition");
        }
        if (action.purpose() != AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL)
          continue;
        if (i + 1 == sequence.size()
            || (sequence.get(i + 1).currency().replacementModifiers().isEmpty()
                && sequence.get(i + 1).currency() != WorkbenchCurrency.CHAOS))
          throw new IllegalArgumentException(
              "Temporary addition requires a following replacement step");
      }
    }
  }

  private CertifiedMethodRunner<State, AutomaticCraftingAction> runner;
  // The runner synchronously classifies its root during construction. A post-preparation
  // return to that item is a failure; the untouched initial root is not.
  private boolean initializingRoot = true;
  private final ItemState root;
  private final Map<String, TierCell> initialTiers;
  private final boolean symbolicSource;
  private final boolean initialQuality40;
  private final int initialAbstractQuality;
  private final Template template;
  private final ItemState preparedRetryAnchor;
  // This memo only avoids repeated plan comparisons; evicting it never changes probability mass.
  private final Map<ItemState, ItemState> preparedRepresentatives =
      new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemState, ItemState> eldest) {
          return size() > 64;
        }
      };
  private State preparedTransitionState;
  private AutomaticCraftingAction preparedTransitionAction;
  private Kernel<State> preparedTransition;
  private final AutomaticCraftingKernel kernel;
  private final java.util.function.BiFunction<ItemState, Integer, Optional<Exit>> classify;
  private final CertifiedInterval.Precision precision;
  private long primitiveOutcomeBudget;
  private boolean primitiveLimited;

  /**
   * {@code AutomaticMethodExecution} 내부의 개별행동·계산예산·Reached 처리를 맡는다.
   *
   * <p>직접 선언한 필드·생성자·메서드는 없다.
   */
  private static final class PrimitiveBudgetReached extends RuntimeException {}

  /**
   * {@code AutomaticMethodExecution} 내부의 개별행동·Yield 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code PrimitiveYield()}: 전달받은 값으로 구성원을 초기화한다.
   * </ul>
   */
  private static final class PrimitiveYield extends RuntimeException {
    PrimitiveYield() {
      super(null, null, false, false);
    }
  }

  private AutomaticCraftingKernel.PrimitiveCursor primitiveCursor;
  private State primitiveState;
  private AutomaticCraftingAction primitiveAction;

  private final PreparationSuccess preparationSuccess;
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
  private final Set<String> unresolvedReasons = new LinkedHashSet<>();

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        PreparationSuccess.UNDECIDED);
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        preparationSuccess,
        false);
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      boolean initialQuality40) {
    this(
        root,
        template,
        kernel,
        (item, quality) -> classify.apply(item),
        precision,
        primitiveOutcomeBudget,
        preparationSuccess,
        initialQuality40,
        initialQuality40 ? 40 : 0);
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      java.util.function.BiFunction<ItemState, Integer, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      boolean initialQuality40,
      int initialAbstractQuality) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        preparationSuccess,
        initialQuality40,
        initialAbstractQuality,
        Map.of());
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      java.util.function.BiFunction<ItemState, Integer, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      boolean initialQuality40,
      int initialAbstractQuality,
      Map<String, TierCell> initialTiers) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        preparationSuccess,
        initialQuality40,
        initialAbstractQuality,
        initialTiers,
        !initialTiers.isEmpty());
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      java.util.function.BiFunction<ItemState, Integer, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      boolean initialQuality40,
      int initialAbstractQuality,
      Map<String, TierCell> initialTiers,
      boolean symbolicSource) {
    this.symbolicSource = symbolicSource;
    this.initialTiers = Map.copyOf(initialTiers);
    if (primitiveOutcomeBudget < 1)
      throw new IllegalArgumentException("Positive primitive budget required");
    this.root = root;
    this.initialQuality40 = initialQuality40;
    this.initialAbstractQuality = initialAbstractQuality;
    this.template = template;
    this.kernel = kernel;
    this.classify = classify;
    this.precision = precision;
    this.primitiveOutcomeBudget = primitiveOutcomeBudget;
    this.preparationSuccess = Objects.requireNonNull(preparationSuccess);
    preparedRetryAnchor = preparedRetryCandidate();
    runner =
        new CertifiedMethodRunner<>(
            new State(
                root,
                false,
                initialQuality40,
                initialAbstractQuality,
                initialTiers,
                symbolicSource
                    ? root.explicits().stream()
                        .map(ModifierInstance::modifierId)
                        .collect(java.util.stream.Collectors.toSet())
                    : Set.of()),
            compiledTemplate(),
            this::transition,
            this::classifyState,
            precision);
    initializingRoot = false;
    if (preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT)
      runner.compactTerminalFailures(
          new State(
              root,
              false,
              initialQuality40,
              initialAbstractQuality,
              initialTiers,
              symbolicSource
                  ? root.explicits().stream()
                      .map(ModifierInstance::modifierId)
                      .collect(java.util.stream.Collectors.toSet())
                  : Set.of()));
  }

  private ExactMethodRunner.Template<State, AutomaticCraftingAction> compiledTemplate() {
    var initial =
        template.initial().stream()
            .<Function<State, AutomaticCraftingAction>>map(a -> s -> a)
            .toList();
    var cycle =
        template.cycle().stream()
            .<Function<State, AutomaticCraftingAction>>map(a -> s -> a)
            .toList();
    return new ExactMethodRunner.Template<>(template.id(), initial, cycle);
  }

  private Kernel<State> transition(State state, AutomaticCraftingAction action) {
    if (state.equals(preparedTransitionState) && action.equals(preparedTransitionAction))
      return preparedTransition;
    if (template.initial().stream()
            .anyMatch(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)
        && action.currency() == WorkbenchCurrency.CHAOS
        && action.omens().contains(WorkbenchOmen.WHITTLING)) {
      var plan = kernel.plan(state.item(), action);
      // Stop before charging the follower if preparation did not protect any reviewed target.
      if (plan.status() == AutomaticCraftingPlan.Status.READY
          && !plan.branches().isEmpty()
          && plan.branches().stream()
              .allMatch(
                  b ->
                      classify.apply(b.remaining(), state.abstractQuality()).orElse(null)
                          == Exit.LOSS)) return new Kernel<>(List.of(), Fraction.ZERO, true);
    }
    if (primitiveCursor == null
        || !state.equals(primitiveState)
        || !action.equals(primitiveAction)) {
      primitiveCursor =
          kernel.deferredCursor(state.item(), state.tiers(), action, primitiveOutcomeBudget);
      primitiveState = state;
      primitiveAction = action;
    }
    primitiveCursor.advance(64);
    if (primitiveCursor.limited()) throw new PrimitiveBudgetReached();
    if (!primitiveCursor.finished()) throw new PrimitiveYield();
    var law = primitiveCursor.result();
    primitiveCursor = null;
    primitiveState = null;
    primitiveAction = null;
    // The runner has not removed this pending state or charged material yet.
    if (law.reason().equals("OUTCOME_BUDGET")) throw new PrimitiveBudgetReached();
    assumptions.addAll(law.assumptions());
    if (law.status() == AutomaticCraftingKernel.Status.UNAVAILABLE)
      return new Kernel<>(List.of(), Fraction.ZERO, true);
    if (!law.reason().isEmpty()) unresolvedReasons.add(law.reason());
    var outcomes = new LinkedHashMap<State, Fraction>();
    var terminalRepresentatives = new EnumMap<Exit, State>(Exit.class);
    var unresolved = law.unresolved().add(law.boundary());
    var trace = state.intermediate();
    if (!template.initial().isEmpty()
        && !state.item().equals(root)
        && (!action.equals(template.cycle().getFirst()) || state.temporaryPreparation())) {
      var path = new ArrayList<>(trace);
      path.add(state.item());
      trace = List.copyOf(path);
    }
    // 이 세 준비 작업의 옵션은 모두 더미다. 최종 한 슬롯은 첫 카오스에서 완전히
    // 교체되므로 실제 작업대 분포에서 대표 이력을 고르고 동일한 후속 분포를 한 번만 계산한다.
    if (root.rarity() == ItemState.Rarity.NORMAL
        && root.explicits().isEmpty()
        && root.catalystQuality() == null
        && template.preparesNormalChaos()
        && template.initial().contains(action)
        && unresolved.equals(Fraction.ZERO)
        && !law.outcomes().isEmpty()) {
      var example =
          law.outcomes().stream()
              .filter(o -> classify.apply(o.item(), state.abstractQuality()).isEmpty())
              .findFirst()
              .orElse(law.outcomes().getFirst());
      var next =
          new State(
              example.item(),
              true,
              state.quality40(),
              state.abstractQuality(),
              example.tiers(),
              retained(state, example),
              trace,
              null);
      return new Kernel<>(List.of(new Outcome<>(next, Fraction.ONE)), Fraction.ZERO, false);
    }
    var hit = losslessChaosHit(state, action, law);
    var expectation = hit == null ? null : Fraction.ONE.divide(hit);
    for (var outcome : law.outcomes()) {
      if (hit != null && classify.apply(outcome.item(), state.abstractQuality()).isEmpty())
        continue;
      if (!completedQuality(state)
          && AutomaticCraftingKernel.hasQualityCap(state.item())
          && !AutomaticCraftingKernel.hasQualityCap(outcome.item())) {
        unresolved = unresolved.add(outcome.probability());
        unresolvedReasons.add("QUALITY_CAP_REMOVAL_BEFORE_40");
      } else {
        // One non-fractured affix is completely removed by each Chaos attempt. The next
        // eligible pool is therefore identical, independent of the failed modifier/tier.
        // Do not apply this quotient to multi-affix, quality-changing, or prepared cycles.
        boolean stationaryRetry =
            action.currency() == WorkbenchCurrency.CHAOS
                && action.omens().stream().allMatch(o -> o == WorkbenchOmen.WHITTLING)
                && action.purpose() == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
                && template.initial().isEmpty()
                && template.cycle().size() == 1
                && AutomaticCraftingKernel.mutableAffixCount(root) == 1
                && AutomaticCraftingKernel.mutableAffixCount(state.item()) == 1
                && AutomaticCraftingKernel.mutableAffixCount(outcome.item()) == 1
                && AutomaticCraftingKernel.sameFractures(root, state.item())
                && AutomaticCraftingKernel.sameFractures(root, outcome.item())
                && root.catalystQuality() == null
                && outcome.item().catalystQuality() == null
                && (!AutomaticCraftingKernel.hasQualityCap(root) || initialQuality40)
                && (!AutomaticCraftingKernel.hasQualityCap(outcome.item()) || state.quality40())
                && classify.apply(outcome.item(), state.abstractQuality()).isEmpty();
        var next =
            new State(
                stationaryRetry ? root : preparedRepresentative(outcome.item()),
                action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
                    || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION
                    || (template.initial().contains(action)
                        && (template.boundedChaos()
                            || action.purpose() == AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE
                            || action.currency() == WorkbenchCurrency.ANNULMENT)),
                state.quality40()
                    || action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT,
                action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT
                    ? 40
                    : action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT
                        ? Math.max(20, state.abstractQuality())
                        : state.abstractQuality(),
                stationaryRetry || !preparedRepresentative(outcome.item()).equals(outcome.item())
                    ? initialTiers
                    : outcome.tiers(),
                retained(state, outcome),
                trace,
                expectation);
        if (preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT
            && preparedRetryAnchor != null) {
          var terminal = classifyState(next).orElse(null);
          if (terminal == Exit.LOSS || terminal == Exit.DEAD) {
            var representative = terminalRepresentatives.putIfAbsent(terminal, next);
            if (representative != null) next = representative;
          }
        }
        // 일반 장비에서 제왕까지 마친 성공 상태는 후속 전이가 없다. 같은 목표 구간의
        // 결합 수치만 대표값으로 묶으며 각 실제 결과의 확률은 그대로 더한다.
        if (action.currency() == WorkbenchCurrency.REGAL
            && root.rarity() == ItemState.Rarity.NORMAL
            && root.explicits().isEmpty()
            && classifyState(next).orElse(null) == Exit.GOAL) {
          var example = kernel.terminalRollExample(next.item());
          if (example != next.item()
              && classify.apply(example, next.abstractQuality()).orElse(null) == Exit.GOAL)
            next =
                new State(
                    example,
                    next.temporaryPreparation(),
                    next.quality40(),
                    next.abstractQuality(),
                    next.tiers(),
                    next.retainedFromRoot(),
                    next.intermediate(),
                    next.expectedChaos());
        }
        outcomes.merge(
            next,
            hit == null ? outcome.probability() : outcome.probability().divide(hit),
            Fraction::add);
      }
    }
    // 두 작업이 연결된 성공 경로는 준비 상태별 마지막 전이 확률을 보존한다.
    // 먼저 같은 결과의 티어·수치 확률을 합산한 후 기록해야 대표 수치 하나의 확률이 되지 않는다.
    boolean split =
        template.initial().size() == 1
            && template.cycle().size() == 1
            && state.temporaryPreparation()
            && action.equals(template.cycle().getFirst())
            && !template.boundedChaos()
            && !action.equals(template.initial().getFirst());
    var transitions = new ArrayList<Outcome<State>>(outcomes.size());
    outcomes.forEach(
        (next, probability) -> {
          var exit = split ? classifyState(next).orElse(null) : null;
          var observed =
              exit == Exit.GOAL || exit == Exit.CHECKPOINT
                  ? next.withFinalStepChance(probability)
                  : next;
          transitions.add(new Outcome<>(observed, probability));
        });
    var result = new Kernel<State>(transitions, unresolved, false);
    // A proven renewal state's complete transition law is identical on every cycle.
    // Retain only this one folded law; never cache an OUTCOME_BUDGET result.
    if (state.item().equals(preparedRetryAnchor) && action.equals(template.cycle().getFirst())) {
      preparedTransitionState = state;
      preparedTransitionAction = action;
      preparedTransition = result;
    }
    return result;
  }

  /** 단일 변경 옵션을 모두 제거하는 동일 풀에서 실패 시 손실 없이 재시도할 수 있는지 확인해 1회 적중 확률을 구한다. */
  private Fraction losslessChaosHit(
      State state, AutomaticCraftingAction action, AutomaticCraftingKernel.Result law) {
    if (preparationSuccess != PreparationSuccess.REPEAT_UNTIL_FIRST_AFFIX
        || !template.boundedChaos()
        || action.currency() != WorkbenchCurrency.CHAOS
        || !law.unresolved().equals(Fraction.ZERO)
        || !law.boundary().equals(Fraction.ZERO)
        || !state.item().desecrated().isEmpty()
        || state.item().catalystQuality() != null
        || AutomaticCraftingKernel.mutableAffixCount(state.item()) != 1) return null;
    Fraction hit = Fraction.ZERO;
    for (var outcome : law.outcomes()) {
      var exit = classify.apply(outcome.item(), state.abstractQuality()).orElse(null);
      if (exit == Exit.GOAL || exit == Exit.CHECKPOINT) hit = hit.add(outcome.probability());
      else if (exit != null
          || AutomaticCraftingKernel.mutableAffixCount(outcome.item()) != 1
          || !AutomaticCraftingKernel.sameFractures(state.item(), outcome.item())
          || outcome.item().catalystQuality() != null
          || AutomaticCraftingKernel.hasQualityCap(outcome.item())) return null;
    }
    return hit.equals(Fraction.ZERO) ? null : hit;
  }

  private Set<String> retained(State source, AutomaticCraftingKernel.Outcome outcome) {
    var retained = new HashSet<>(source.retainedFromRoot());
    retained.retainAll(outcome.retained());
    return Set.copyOf(retained);
  }

  private AutomaticCraftingPlan retryPlan(ItemState item) {
    if (template.initial().size() != 1
        || AutomaticCraftingKernel.mutableAffixCount(root) != 0
        || template.cycle().size() != 1
        || template.cycle().getFirst().currency() != WorkbenchCurrency.CHAOS
        || AutomaticCraftingKernel.mutableAffixCount(item) != 1
        || !AutomaticCraftingKernel.sameFractures(root, item)
        || item.catalystQuality() != null
        || AutomaticCraftingKernel.hasQualityCap(item)) return null;
    var plan = kernel.plan(item, template.cycle().getFirst());
    return plan.status() == AutomaticCraftingPlan.Status.READY
            && plan.branches().size() == 1
            && AutomaticCraftingKernel.mutableAffixCount(plan.branches().getFirst().remaining())
                == 0
            && AutomaticCraftingKernel.sameFractures(root, plan.branches().getFirst().remaining())
        ? plan
        : null;
  }

  private ItemState preparedRetryCandidate() {
    if (template.initial().size() != 1 || AutomaticCraftingKernel.mutableAffixCount(root) != 0)
      return null;
    return kernel.representatives(kernel.plan(root, template.initial().getFirst())).stream()
        .filter(
            item ->
                classify.apply(item, initialAbstractQuality).isEmpty() && retryPlan(item) != null)
        .findFirst()
        .orElse(null);
  }

  private ItemState preparedRepresentative(ItemState item) {
    if (preparedRetryAnchor == null || !classify.apply(item, initialAbstractQuality).isEmpty())
      return item;
    return preparedRepresentatives.computeIfAbsent(
        item,
        value -> {
          var plan = retryPlan(value);
          return plan != null && plan.equals(retryPlan(preparedRetryAnchor))
              ? preparedRetryAnchor
              : value;
        });
  }

  private Optional<Exit> classifyState(State state) {
    var exit = classify.apply(state.item(), state.abstractQuality());
    if (state.temporaryPreparation()
        && (preparationSuccess == PreparationSuccess.REPEAT_UNTIL_FIRST_AFFIX
            || preparationSuccess == PreparationSuccess.REQUIRE_REPLACEMENT)
        && (exit.orElse(null) == Exit.CHECKPOINT || exit.orElse(null) == Exit.GOAL))
      return Optional.empty();
    if (state.abstractQuality() > initialAbstractQuality && exit.isEmpty())
      return Optional.of(Exit.CHECKPOINT);
    // Retry an eligible unsuccessful Chaos from its actual next state. Losing an already
    // matched milestone remains LOSS; only a failure with no milestone change continues.
    if (!initializingRoot
        && template.cycle().size() == 1
        && template.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS
        && template.cycle().getFirst().omens().isEmpty()
        && exit.isEmpty()) return Optional.empty();
    if (!completedQuality(state)
        && AutomaticCraftingKernel.hasQualityCap(state.item())
        && state.temporaryPreparation()) return Optional.empty();
    if (!completedQuality(state) && AutomaticCraftingKernel.hasQualityCap(state.item()))
      return state.item().equals(root) ? Optional.empty() : Optional.of(Exit.CHECKPOINT);
    if (state.quality40() && !initialQuality40 && exit.isEmpty())
      return Optional.of(Exit.CHECKPOINT);
    if (state.temporaryPreparation() && exit.orElse(null) == Exit.GOAL) {
      if (preparationSuccess == PreparationSuccess.UNDECIDED) {
        unresolvedReasons.add("PREPARATION_FIRST_HIT_POLICY_UNDECIDED");
        return Optional.of(Exit.UNRESOLVED);
      }
      if (preparationSuccess == PreparationSuccess.REQUIRE_REPLACEMENT) return Optional.empty();
    }
    // An eligible one-affix Chaos retry continues without another preparation charge.
    // Only equal complete removal plans are quotiented; distinct pools remain distinct.
    if (exit.isEmpty() && preparedRetryAnchor != null && retryPlan(state.item()) != null)
      return Optional.empty();
    // A prepared state with an unavailable follower cannot consume that follower.
    // Absorb it now so thousands of failed numeric rolls do not become pending jobs.
    if (exit.isEmpty()
        && preparedRetryAnchor != null
        && state.temporaryPreparation()
        && kernel.plan(state.item(), template.cycle().getFirst()).status()
            == AutomaticCraftingPlan.Status.UNAVAILABLE) return Optional.of(Exit.DEAD);
    // Preparation is not a completed method checkpoint.
    if (state.temporaryPreparation() && exit.orElse(null) == Exit.CHECKPOINT)
      return Optional.empty();
    if (!initializingRoot
        && preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT
        && !state.temporaryPreparation()
        && exit.isEmpty()
        && (!state.item().equals(root)
            || template.initial().stream()
                .anyMatch(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)))
      return Optional.of(Exit.LOSS);
    return exit;
  }

  private boolean completedQuality(State state) {
    return kernel.workedQuality(state.item(), state.abstractQuality()) >= 40;
  }

  /**
   * {@code AutomaticMethodExecution}에서 사용하는 체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code objectiveIdentity}: 목적·정체성 ({@code String}).
   *   <li>{@code identity}: 정체성 ({@code AutomaticCraftingKernel.Identity}).
   *   <li>{@code root}: 시작점 ({@code ItemState}).
   *   <li>{@code initialQuality40}: 초기값·품질·40 ({@code boolean}).
   *   <li>{@code initialAbstractQuality}: 초기값·추상화된·품질 ({@code int}).
   *   <li>{@code initialTiers}: 초기값·티어목록 ({@code Map<String, TierCell>}).
   *   <li>{@code symbolicSource}: symbolic·출처 ({@code boolean}).
   *   <li>{@code template}: 템플릿 ({@code Template}).
   *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
   *   <li>{@code primitiveOutcomeBudget}: 개별행동·결과·계산예산 ({@code long}).
   *   <li>{@code preparationSuccess}: 준비·성공 ({@code PreparationSuccess}).
   *   <li>{@code header}: 헤더 ({@code CertifiedMethodRunner.CursorHeader<State>}).
   *   <li>{@code rows}: 행목록 ({@code List<CertifiedMethodRunner.CursorRow<State,
   *       AutomaticCraftingAction>>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code unresolvedReasons}: 미해결·이유목록 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Checkpoint(version, objectiveIdentity, identity, root, initialQuality40,
   *       initialAbstractQuality, initialTiers, symbolicSource, template, precision,
   *       primitiveOutcomeBudget, preparationSuccess, header, rows, assumptions,
   *       unresolvedReasons)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code version}(버전), {@code
   *       objectiveIdentity}(목적·정체성), {@code identity}(정체성), {@code root}(시작점), {@code
   *       initialQuality40}(초기값·품질·40), {@code initialAbstractQuality}(초기값·추상화된·품질), {@code
   *       initialTiers}(초기값·티어목록), {@code symbolicSource}(symbolic·출처), {@code template}(템플릿),
   *       {@code precision}(정밀도), {@code primitiveOutcomeBudget}(개별행동·결과·계산예산), {@code
   *       preparationSuccess}(준비·성공), {@code header}(헤더), {@code rows}(행목록), {@code
   *       assumptions}(계산가정), {@code unresolvedReasons}(미해결·이유목록). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code withIdentity(shared)}: 정체성을(를) 반영한 값을 구한다. 입력: {@code shared}(공유). 반환: {@code
   *       Checkpoint}.
   * </ul>
   */
  public record Checkpoint(
      String version,
      String objectiveIdentity,
      AutomaticCraftingKernel.Identity identity,
      ItemState root,
      boolean initialQuality40,
      int initialAbstractQuality,
      Map<String, TierCell> initialTiers,
      boolean symbolicSource,
      Template template,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      CertifiedMethodRunner.CursorHeader<State> header,
      List<CertifiedMethodRunner.CursorRow<State, AutomaticCraftingAction>> rows,
      List<WorkbenchSimulator.Assumption> assumptions,
      Set<String> unresolvedReasons) {
    public Checkpoint {
      if (objectiveIdentity == null || objectiveIdentity.isBlank())
        throw new IllegalArgumentException("Canonical objective identity required");
      rows = List.copyOf(rows);
      assumptions = List.copyOf(assumptions);
      unresolvedReasons = Set.copyOf(unresolvedReasons);
    }

    public Checkpoint withIdentity(AutomaticCraftingKernel.Identity shared) {
      return new Checkpoint(
          version,
          objectiveIdentity,
          shared,
          root,
          initialQuality40,
          initialAbstractQuality,
          initialTiers,
          symbolicSource,
          template,
          precision,
          primitiveOutcomeBudget,
          preparationSuccess,
          header,
          rows,
          assumptions,
          unresolvedReasons);
    }
  }

  public Checkpoint checkpoint(String objectiveIdentity) {
    try (var lease = runner.leaseCursor()) {
      var rows = new ArrayList<CertifiedMethodRunner.CursorRow<State, AutomaticCraftingAction>>();
      lease.rows().forEachRemaining(rows::add);
      return new Checkpoint(
          VERSION,
          objectiveIdentity,
          kernel.identity(),
          root,
          initialQuality40,
          initialAbstractQuality,
          initialTiers,
          symbolicSource,
          template,
          precision,
          primitiveOutcomeBudget,
          preparationSuccess,
          lease.header(),
          rows,
          assumptions(),
          unresolvedReasons());
    }
  }

  public static AutomaticMethodExecution restore(
      Checkpoint saved,
      AutomaticCraftingKernel kernel,
      String objectiveIdentity,
      Function<ItemState, Optional<Exit>> classify) {
    return restore(saved, kernel, objectiveIdentity, (item, quality) -> classify.apply(item));
  }

  public static AutomaticMethodExecution restore(
      Checkpoint saved,
      AutomaticCraftingKernel kernel,
      String objectiveIdentity,
      java.util.function.BiFunction<ItemState, Integer, Optional<Exit>> classify) {
    if (!VERSION.equals(saved.version())
        || !kernel.identity().equals(saved.identity())
        || !saved.objectiveIdentity().equals(objectiveIdentity))
      throw new IllegalArgumentException(
          "Method execution requires explicit recalculation for changed models");
    var execution =
        new AutomaticMethodExecution(
            saved.root(),
            saved.template(),
            kernel,
            classify,
            saved.precision(),
            saved.primitiveOutcomeBudget(),
            saved.preparationSuccess(),
            saved.initialQuality40(),
            saved.initialAbstractQuality(),
            saved.initialTiers() == null ? Map.of() : saved.initialTiers(),
            saved.symbolicSource());
    execution.runner.closeWorkingMaps();
    try (var restore =
        CertifiedMethodRunner.restoreCursor(
            saved.header(),
            execution.compiledTemplate(),
            execution::transition,
            execution::classifyState,
            saved.precision(),
            LinkedHashMap::new)) {
      saved.rows().forEach(restore::accept);
      execution.runner = restore.finish();
      if (saved.preparationSuccess() == PreparationSuccess.STOP_ON_FIRST_HIT)
        execution.runner.compactTerminalFailures(
            new State(
                saved.root(),
                false,
                saved.initialQuality40(),
                saved.initialAbstractQuality(),
                saved.initialTiers() == null ? Map.of() : saved.initialTiers()));
    }
    execution.assumptions.addAll(saved.assumptions());
    execution.unresolvedReasons.addAll(saved.unresolvedReasons());
    return execution;
  }

  public boolean atBoundary() {
    return runner.atBoundary();
  }

  public boolean absorbed() {
    return runner.absorbed();
  }

  public long cycles() {
    return runner.cycles();
  }

  public Set<ItemState> retryRepresentatives() {
    return preparedRetryAnchor == null ? Set.of() : Set.of(preparedRetryAnchor);
  }

  public long retainedStates() {
    return (primitiveCursor == null ? 0 : primitiveCursor.retainedStates())
        + runner.retainedStates()
        + runner.retention().flightOutcomes()
        + preparedRepresentatives.size()
        + (preparedTransition == null ? 0 : preparedTransition.outcomes().size());
  }

  public boolean hasPrimitiveInFlight() {
    return primitiveCursor != null || runner.retention().flightOutcomes() > 0;
  }

  public void beginCycle() {
    runner.beginCycle();
  }

  public void step(int outcomes) {
    if (primitiveLimited) return;
    try {
      runner.step(outcomes);
    } catch (PrimitiveYield ex) {
      // Keep the runner pending; no material has been charged yet.
    } catch (PrimitiveBudgetReached ex) {
      primitiveLimited = true;
    }
  }

  public boolean resourceLimited() {
    return primitiveLimited;
  }

  public void increasePrimitiveBudget(long budget) {
    if (budget < primitiveOutcomeBudget)
      throw new IllegalArgumentException("Resource budget cannot shrink");
    if (budget > primitiveOutcomeBudget) {
      primitiveCursor = null;
      primitiveState = null;
      primitiveAction = null;
    }
    primitiveOutcomeBudget = budget;
    primitiveLimited = false;
  }

  public CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> observation() {
    return withExpectedUses(runner.observation());
  }

  /** 첫 적중까지 반복한 경우, 한 번으로 축약한 전이에 누락된 기대 소모량을 복원한다. */
  private CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> withExpectedUses(
      CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> point) {
    if (!template.boundedChaos()) return point;
    var extra = CertifiedInterval.zero(precision.fractionalBits());
    for (var law : point.exits().values())
      for (var row : law.entrySet()) {
        var mean = row.getKey().expectedChaos();
        if (mean == null) continue;
        var afterFirst =
            new Fraction(mean.numerator().subtract(mean.denominator()), mean.denominator());
        extra =
            extra.add(
                row.getValue()
                    .multiply(CertifiedInterval.enclose(afterFirst, precision.fractionalBits())));
      }
    if (extra.zero()) return point;
    var uses = new LinkedHashMap<>(point.cycleUses());
    uses.merge(template.cycle().getFirst(), extra, CertifiedInterval::add);
    return new CertifiedMethodRunner.Observation<>(
        point.cycles(), point.exits(), point.active(), point.initialUses(), uses, point.mass());
  }

  public CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> provisional(
      int horizon) {
    if (runner.atBoundary() && runner.absorbed())
      return observation().atLaterAbsorbedBoundary(horizon);
    if (runner.atBoundary() && runner.cycles() == horizon) return observation();
    var point =
        runner.provisional(
            horizon,
            new State(
                root,
                false,
                initialQuality40,
                initialAbstractQuality,
                initialTiers,
                symbolicSource
                    ? root.explicits().stream()
                        .map(ModifierInstance::modifierId)
                        .collect(java.util.stream.Collectors.toSet())
                    : Set.of()));
    return new CertifiedMethodRunner.Observation<>(
        horizon,
        point.exits(),
        point.active(),
        usageBounds(point.initialUses(), template.initial(), 1),
        usageBounds(point.cycleUses(), template.cycle(), horizon),
        point.mass());
  }

  private Map<AutomaticCraftingAction, CertifiedInterval> usageBounds(
      Map<AutomaticCraftingAction, CertifiedInterval> spent,
      List<AutomaticCraftingAction> actions,
      int repetitions) {
    var counts = new LinkedHashMap<AutomaticCraftingAction, Long>();
    actions.forEach(a -> counts.merge(a, (long) repetitions, Long::sum));
    var result = new LinkedHashMap<AutomaticCraftingAction, CertifiedInterval>();
    counts.forEach(
        (action, count) ->
            result.put(
                action,
                new CertifiedInterval(
                    precision.fractionalBits(),
                    spent
                        .getOrDefault(action, CertifiedInterval.zero(precision.fractionalBits()))
                        .lowerUnits(),
                    java.math.BigInteger.valueOf(count).shiftLeft(precision.fractionalBits()))));
    return result;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  public Set<String> unresolvedReasons() {
    return Set.copyOf(unresolvedReasons);
  }

  /**
   * Expand execution costs into actual currencies and consumed omens; never count two as one use.
   */
  public static Map<String, CertifiedInterval> materialUses(
      Map<AutomaticCraftingAction, CertifiedInterval> uses) {
    var result = new LinkedHashMap<String, CertifiedInterval>();
    uses.forEach(
        (action, probability) -> {
          if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT) {
            result.merge("WORK:QUALITY_20", probability, CertifiedInterval::add);
            return;
          }
          if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
            result.merge("WORK:QUALITY_40", probability, CertifiedInterval::add);
            return;
          }
          if (action.currency() != null)
            result.merge(action.currency().name(), probability, CertifiedInterval::add);
          action
              .omens()
              .forEach(
                  omen -> result.merge("OMEN:" + omen.name(), probability, CertifiedInterval::add));
        });
    return Collections.unmodifiableMap(result);
  }

  @Override
  public void close() {
    primitiveCursor = null;
    primitiveState = null;
    primitiveAction = null;
    runner.closeWorkingMaps();
  }
}
