package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.craftingsupport.search.domain.OutcomeStrategy;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 내부 확률 표현과 별개로 불변 그래프 조회 계약을 제공한다.
 *
 * <p>기존 계약: Immutable graph views use state identities, independently of the probability
 * representation.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code memoryLimited()}: 메모리·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code increaseMemoryBudget(limit)}: 메모리·계산예산을(를) 한도를 늘린다. 입력: {@code limit}(한도).
 *   <li>{@code operations()}: 연산목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code rootId()}: 시작점·식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code calculationVersion()}: 계산·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code stateVersion()}: 상태·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code templatesVersion()}: 템플릿목록·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code states()}: 상태목록 값을 조회·구성한다. 반환: {@code List<StateView>}.
 *   <li>{@code methods()}: 제작방법목록 값을 조회·구성한다. 반환: {@code List<MethodView>}.
 *   <li>{@code strategy(cycles)}: 전략 처리에서 {@code bySource.computeIfAbsent}, {@code m.source}을(를)
 *       사용한다. 입력: {@code cycles}(주기횟수). 반환: {@code OutcomeStrategy<String>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code StateView}: {@code MethodSearchEngine}에서 사용하는 상태·조회표현의 불변 값이다.
 *   <li>{@code MethodView}: {@code MethodSearchEngine}에서 사용하는 제작방법·조회표현의 불변 값이다.
 * </ul>
 */
interface MethodSearchEngine {
  /**
   * {@code MethodSearchEngine}에서 사용하는 상태·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code representative}: 대표상태 ({@code DeferredItem}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code MethodSearchCalculation.Progress}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code distributionCell}: 확률분포·셀 ({@code Optional<DeferredItem>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code cell(state)}: 셀 처리에서 {@code state.id}, {@code state.state}을(를) 사용한다. 입력: {@code
   *       state}(상태). 반환: {@code StateView}.
   * </ul>
   */
  record StateView(
      String id,
      DeferredItem representative,
      int depth,
      MethodSearchCalculation.Progress progress,
      boolean expanded,
      boolean complete,
      Optional<DeferredItem> distributionCell) {
    static StateView cell(MethodSearchCalculation.StateView state) {
      return new StateView(
          state.id(),
          state.state(),
          state.depth(),
          state.progress(),
          state.expanded(),
          state.complete(),
          Optional.of(state.state()));
    }
  }

  /**
   * {@code MethodSearchEngine}에서 사용하는 제작방법·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code source}: 출처 ({@code String}).
   *   <li>{@code initialActions}: 초기값·행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, Observation<String, WorkbenchCurrency>>}).
   * </ul>
   */
  record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, Observation<String, WorkbenchCurrency>> points) {}

  void step();

  boolean complete();

  boolean memoryLimited();

  void increaseMemoryBudget(int limit);

  long operations();

  String rootId();

  String calculationVersion();

  String stateVersion();

  String templatesVersion();

  List<WorkbenchSimulator.Assumption> assumptions();

  List<StateView> states();

  List<MethodView> methods();

  default OutcomeStrategy<String> strategy(int cycles) {
    var states = states();
    var bySource = new HashMap<String, List<MethodView>>();
    methods()
        .forEach(m -> bySource.computeIfAbsent(m.source(), ignored -> new ArrayList<>()).add(m));
    var graph = new LinkedHashMap<String, OutcomeStrategy.State<String>>();
    for (var state : states) {
      var choices = new ArrayList<OutcomeStrategy.Choice<String>>();
      boolean closed = state.expanded();
      for (var method : bySource.getOrDefault(state.id(), List.of())) {
        var point = method.points().get(cycles);
        if (point == null) {
          closed = false;
          continue;
        }
        var outcomes = new LinkedHashMap<>(point.active());
        var unresolved = Fraction.ZERO;
        for (var exit : point.exits().entrySet())
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
          else
            exit.getValue().forEach((target, mass) -> outcomes.merge(target, mass, Fraction::add));
        choices.add(new OutcomeStrategy.Choice<>(method.id(), outcomes, unresolved));
      }
      graph.put(
          state.id(),
          new OutcomeStrategy.State<>(state.progress().goal() == Status.MATCH, closed, choices));
    }
    return new OutcomeStrategy<>(graph);
  }
}
