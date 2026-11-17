package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 지연 수치 셀의 계산과 정확한 분포 페이지를 탐색 서비스에 연결한다.
 *
 * <p>기존 계약: Adapter preserves the existing deferred-cell calculation and its exact distribution
 * pages.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code calculation}: 계산 ({@code MethodSearchCalculation}).
 *   <li>{@code rootId}: 시작점·식별자 ({@code String}).
 *   <li>{@code ids}: 식별자목록 ({@code Map<DeferredItem, String>}).
 *   <li>{@code identities}: 식별자목록 ({@code Map<String, DeferredItem>}).
 *   <li>{@code points}: 지점목록 ({@code IdentityHashMap<Observation<DeferredItem, WorkbenchCurrency>,
 *       Observation<String, WorkbenchCurrency>>}).
 *   <li>{@code snapshotOperation}: 스냅샷·연산 ({@code long}).
 *   <li>{@code states}: 상태목록 ({@code List<StateView>}).
 *   <li>{@code methods}: 제작방법목록 ({@code List<MethodView>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code DeferredMethodSearchEngine(calculation)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       calculation}(계산).
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code memoryLimited()}: 메모리·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code increaseMemoryBudget(limit)}: 메모리·계산예산을(를) 한도를 늘린다. 입력: {@code limit}(한도).
 *   <li>{@code operations()}: 연산목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code rootId()}: {@code rootId} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code calculationVersion()}: 계산·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code stateVersion()}: 상태·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code templatesVersion()}: 템플릿목록·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code states()}: 상태목록 값을 조회·구성한다. 반환: {@code List<StateView>}.
 *   <li>{@code methods()}: 제작방법목록 값을 조회·구성한다. 반환: {@code List<MethodView>}.
 *   <li>{@code id(item)}: 식별자 처리에서 {@code ids.computeIfAbsent}, {@code value.canonicalKey}을(를)
 *       사용한다. 입력: {@code item}(아이템). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code law(input)}: 분포 처리에서 {@code input.forEach}, {@code result.merge}을(를) 사용한다. 입력:
 *       {@code input}(입력). 반환: {@code Map<String, Fraction>}.
 *   <li>{@code point(input)}: 지점 처리에서 {@code points.computeIfAbsent}, {@code value.exits}을(를) 사용한다.
 *       입력: {@code input}(입력). 반환: {@code Observation<String, WorkbenchCurrency>}.
 *   <li>{@code refresh()}: 보유 자료의 갱신을 시도한다. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 */
final class DeferredMethodSearchEngine implements MethodSearchEngine {
  private final MethodSearchCalculation calculation;
  private final String rootId;
  private final Map<DeferredItem, String> ids = new HashMap<>();
  private final Map<String, DeferredItem> identities = new HashMap<>();
  private final IdentityHashMap<
          Observation<DeferredItem, WorkbenchCurrency>, Observation<String, WorkbenchCurrency>>
      points = new IdentityHashMap<>();
  private long snapshotOperation = Long.MIN_VALUE;
  private List<StateView> states = List.of();
  private List<MethodView> methods = List.of();

  DeferredMethodSearchEngine(MethodSearchCalculation calculation) {
    this.calculation = calculation;
    rootId = calculation.states().getFirst().id();
  }

  public void step() {
    calculation.step();
  }

  public boolean complete() {
    return calculation.complete();
  }

  public boolean memoryLimited() {
    return calculation.memoryLimited();
  }

  public void increaseMemoryBudget(int limit) {
    calculation.increaseMemoryBudget(limit);
  }

  public long operations() {
    return calculation.operations();
  }

  public String rootId() {
    return rootId;
  }

  public String calculationVersion() {
    return MethodSearchCalculation.VERSION;
  }

  public String stateVersion() {
    return DeferredItem.VERSION;
  }

  public String templatesVersion() {
    return MethodSearchCalculation.TEMPLATES;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return calculation.assumptions();
  }

  public List<StateView> states() {
    refresh();
    return states;
  }

  public List<MethodView> methods() {
    refresh();
    return methods;
  }

  private String id(DeferredItem item) {
    return ids.computeIfAbsent(
        item,
        value -> {
          var key = value.canonicalKey();
          var previous = identities.putIfAbsent(key, value);
          if (previous != null && !previous.equals(value))
            throw new IllegalStateException("State digest collision");
          return key;
        });
  }

  private Map<String, Fraction> law(Map<DeferredItem, Fraction> input) {
    var result = new LinkedHashMap<String, Fraction>();
    input.forEach((state, mass) -> result.merge(id(state), mass, Fraction::add));
    return Collections.unmodifiableMap(result);
  }

  private Observation<String, WorkbenchCurrency> point(
      Observation<DeferredItem, WorkbenchCurrency> input) {
    return points.computeIfAbsent(
        input,
        value -> {
          var exits = new LinkedHashMap<Exit, Map<String, Fraction>>();
          value.exits().forEach((exit, distribution) -> exits.put(exit, law(distribution)));
          return new Observation<>(
              value.cycles(),
              Collections.unmodifiableMap(exits),
              law(value.active()),
              value.initialUses(),
              value.cycleUses());
        });
  }

  private void refresh() {
    if (snapshotOperation == calculation.operations()) return;
    states =
        calculation.states().stream()
            .map(
                state -> {
                  if (!id(state.state()).equals(state.id()))
                    throw new IllegalStateException("Inconsistent state identity");
                  return StateView.cell(state);
                })
            .toList();
    methods =
        calculation.methods().stream()
            .map(
                method -> {
                  var observations =
                      new LinkedHashMap<Integer, Observation<String, WorkbenchCurrency>>();
                  method
                      .points()
                      .forEach((cycles, value) -> observations.put(cycles, point(value)));
                  return new MethodView(
                      method.id(),
                      method.source(),
                      method.initialActions(),
                      method.actions(),
                      method.status(),
                      method.reason(),
                      Collections.unmodifiableMap(observations));
                })
            .toList();
    snapshotOperation = calculation.operations();
  }
}
