package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedLawStore;
import com.poe2craft.craftingsupport.search.domain.CertifiedRecommendations;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 정확값을 감싸는 확률 구간과 작업 상태의 전송 값을 정의한다.
 *
 * <p>기존 계약: Additive wire values: an arithmetic enclosure is never a statistical confidence
 * interval.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "certified-method-wire-v7-strategy-job"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedMethodProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code method(method, precision)}: 제작방법 처리에서 {@code method.points}, {@code
 *       Entry.comparingByKey}을(를) 사용한다. 입력: {@code method}(제작방법), {@code precision}(정밀도). 반환:
 *       {@code Method}.
 *   <li>{@code locals(states, methods, precision)}: locals 처리에서 {@code methods.forEach}, {@code
 *       bySource.computeIfAbsent}을(를) 사용한다. 입력: {@code states}(상태목록), {@code methods}(제작방법목록),
 *       {@code precision}(정밀도). 반환: {@code List<Local>}.
 *   <li>{@code unresolved(status)}: 미해결의 입력에 따른 분기·결과를 처리한다. 입력: {@code status}(상태). 반환: {@code
 *       boolean}.
 *   <li>{@code target(progress)}: 대상 처리에서 {@code progress.goal}, {@code progress.entries}을(를) 사용한다.
 *       입력: {@code progress}(진행상황). 반환: {@code Target}.
 *   <li>{@code sum(values, precision)}: 항목들의 합을 계산한다. 입력: {@code values}(값목록), {@code
 *       precision}(정밀도). 반환: {@code CertifiedInterval}.
 *   <li>{@code quantities(values)}: quantities 처리에서 {@code values.forEach}, {@code result.put}을(를)
 *       사용한다. 입력: {@code values}(값목록). 반환: {@code Map<String, Bounds>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Create}: {@code CertifiedMethodProtocol}에서 사용하는 생성의 불변 값이다.
 *   <li>{@code State}: {@code CertifiedMethodProtocol}에서 사용하는 상태의 불변 값이다.
 *   <li>{@code Snapshot}: {@code CertifiedMethodProtocol}에서 사용하는 스냅샷의 불변 값이다.
 *   <li>{@code Strategy}: {@code CertifiedMethodProtocol}에서 사용하는 전략의 불변 값이다.
 *   <li>{@code Storage}: {@code CertifiedMethodProtocol}에서 사용하는 저장소의 불변 값이다.
 *   <li>{@code Graph}: {@code CertifiedMethodProtocol}에서 사용하는 그래프의 불변 값이다.
 *   <li>{@code Outcomes}: {@code CertifiedMethodProtocol}에서 사용하는 결과목록의 불변 값이다.
 *   <li>{@code Entry}: {@code CertifiedMethodProtocol}에서 사용하는 항목의 불변 값이다.
 *   <li>{@code Started}: {@code CertifiedMethodProtocol}에서 사용하는 시작시각의 불변 값이다.
 *   <li>{@code Bounds}: {@code CertifiedMethodProtocol}에서 사용하는 경계값의 불변 값이다.
 *   <li>{@code Outcome}: {@code CertifiedMethodProtocol}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Point}: {@code CertifiedMethodProtocol}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Checkpoint}: {@code CertifiedMethodProtocol}에서 사용하는 체크포인트의 불변 값이다.
 *   <li>{@code Method}: {@code CertifiedMethodProtocol}에서 사용하는 제작방법의 불변 값이다.
 *   <li>{@code Target}: {@code CertifiedMethodProtocol}에서 사용하는 대상의 불변 값이다.
 *   <li>{@code Local}: {@code CertifiedMethodProtocol}에서 사용하는 로컬의 불변 값이다.
 * </ul>
 */
public final class CertifiedMethodProtocol {
  public static final String VERSION = "certified-method-wire-v7-strategy-job";

  private CertifiedMethodProtocol() {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 생성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code MethodSearchProtocol.Create}).
   *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
   * </ul>
   */
  public record Create(
      Integer version,
      MethodSearchProtocol.Create request,
      CertifiedInterval.Precision precision) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code goal}: 목표 ({@code String}).
   *   <li>{@code entries}: 항목목록 ({@code List<String>}).
   *   <li>{@code directComplete}: 직접·완료 ({@code boolean}).
   *   <li>{@code representative}: 대표상태 ({@code ItemState}).
   * </ul>
   */
  public record State(
      String id,
      int depth,
      String goal,
      List<String> entries,
      boolean directComplete,
      ItemState representative) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 스냅샷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code enumerationComplete}: 전체열거·완료 ({@code boolean}).
   *   <li>{@code operations}: 연산목록 ({@code long}).
   *   <li>{@code rootStateId}: 시작점·상태·식별자 ({@code String}).
   *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
   *   <li>{@code arithmeticVersion}: 산술·버전 ({@code String}).
   *   <li>{@code calculationVersion}: 계산·버전 ({@code String}).
   *   <li>{@code templateVersion}: 템플릿·버전 ({@code String}).
   *   <li>{@code conditionalVersion}: 조건부·버전 ({@code String}).
   *   <li>{@code transition}: 전이 ({@code BasicCurrencyState.Provenance}).
   *   <li>{@code goalCatalogVersion}: 목표·카탈로그·버전 ({@code String}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code strategyStatus}: 전략·상태 ({@code String}).
   *   <li>{@code strategyVersion}: 전략·버전 ({@code String}).
   *   <li>{@code stateCount}: 상태·개수 ({@code int}).
   *   <li>{@code methodCount}: 제작방법·개수 ({@code int}).
   *   <li>{@code storage}: 저장소 ({@code Storage}).
   *   <li>{@code localRecommendations}: 로컬·추천목록 ({@code List<Local>}).
   *   <li>{@code strategies}: 전략목록 ({@code List<Strategy>}).
   * </ul>
   */
  public record Snapshot(
      String version,
      String jobId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean enumerationComplete,
      long operations,
      String rootStateId,
      CertifiedInterval.Precision precision,
      String arithmeticVersion,
      String calculationVersion,
      String templateVersion,
      String conditionalVersion,
      BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      List<WorkbenchSimulator.Assumption> assumptions,
      String strategyStatus,
      String strategyVersion,
      int stateCount,
      int methodCount,
      Storage storage,
      List<Local> localRecommendations,
      List<Strategy> strategies) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 전략의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code lower}: 하한 ({@code Probability}).
   *   <li>{@code upper}: 상한 ({@code Probability}).
   *   <li>{@code methodIds}: 제작방법·식별자목록 ({@code List<String>}).
   *   <li>{@code reachableOpen}: 도달가능·열린 ({@code boolean}).
   *   <li>{@code reachableUnresolved}: 도달가능·미해결 ({@code boolean}).
   *   <li>{@code transitionArithmetic}: 전이·산술 ({@code boolean}).
   *   <li>{@code fixedPointPending}: 고정·지점·대기 ({@code boolean}).
   * </ul>
   */
  public record Strategy(
      String stateId,
      int cycles,
      String status,
      Probability lower,
      Probability upper,
      List<String> methodIds,
      boolean reachableOpen,
      boolean reachableUnresolved,
      boolean transitionArithmetic,
      boolean fixedPointPending) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 저장소의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code residentEntries}: 메모리상주·항목목록 ({@code long}).
   *   <li>{@code storedClassRows}: 저장된·동치류·행목록 ({@code long}).
   *   <li>{@code packedBytes}: 압축된·바이트수 ({@code long}).
   *   <li>{@code residentPackedByteLimit}: 메모리상주·압축된·Byte·한도 ({@code long}).
   *   <li>{@code transientFreezeBytes}: 임시·불변복사·바이트수 ({@code long}).
   *   <li>{@code kernelPackedBytes}: 전이커널·압축된·바이트수 ({@code long}).
   *   <li>{@code kernelPackedByteLimit}: 전이커널·압축된·Byte·한도 ({@code long}).
   *   <li>{@code materializedRows}: materialized·행목록 ({@code int}).
   *   <li>{@code uniquePages}: 고유·페이지목록 ({@code int}).
   *   <li>{@code workingVersion}: 계산중·버전 ({@code String}).
   *   <li>{@code workingClassRows}: 계산중·동치류·행목록 ({@code long}).
   *   <li>{@code workingPackedBytes}: 계산중·압축된·바이트수 ({@code long}).
   *   <li>{@code workingMaterializedRows}: 계산중·Materialized·행목록 ({@code int}).
   *   <li>{@code residentPackedBytes}: 메모리상주·압축된·바이트수 ({@code long}).
   *   <li>{@code diskBytes}: 디스크·바이트수 ({@code long}).
   * </ul>
   */
  public record Storage(
      String version,
      long residentEntries,
      long storedClassRows,
      long packedBytes,
      long residentPackedByteLimit,
      long transientFreezeBytes,
      long kernelPackedBytes,
      long kernelPackedByteLimit,
      int materializedRows,
      int uniquePages,
      String workingVersion,
      long workingClassRows,
      long workingPackedBytes,
      int workingMaterializedRows,
      long residentPackedBytes,
      long diskBytes) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 그래프의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code List<State>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Method>}).
   *   <li>{@code localRecommendations}: 로컬·추천목록 ({@code List<Local>}).
   *   <li>{@code nextStateOffset}: 다음·상태·시작위치 ({@code Integer}).
   *   <li>{@code nextMethodOffset}: 다음·제작방법·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Graph(
      String jobId,
      long revision,
      List<State> states,
      List<Method> methods,
      List<Local> localRecommendations,
      Integer nextStateOffset,
      Integer nextMethodOffset) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 결과목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code methodId}: 제작방법·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code nextOffset}: 다음·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Outcomes(
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<Outcome> outcomes,
      Integer nextOffset) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code parentEntryId}: 상위값·항목·식별자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code conditionalProbability}: 조건부·확률 ({@code Bounds}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code List<MethodSearchProtocol.ConditionalState>}).
   *   <li>{@code nextOffset}: 다음·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Entry(
      String id,
      String stateId,
      String parentEntryId,
      String status,
      String reason,
      Bounds conditionalProbability,
      long generation,
      List<MethodSearchProtocol.ConditionalState> states,
      Integer nextOffset) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 시작시각의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code search}: 탐색 ({@code Snapshot}).
   *   <li>{@code rootEntry}: 시작점·항목 ({@code Entry}).
   * </ul>
   */
  public record Started(Snapshot search, Entry rootEntry) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 경계값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code lower}: 하한 ({@code Probability}).
   *   <li>{@code upper}: 상한 ({@code Probability}).
   *   <li>{@code arithmeticWidth}: 산술·Width ({@code Probability}).
   *   <li>{@code exact}: 정확값 ({@code boolean}).
   *   <li>{@code confirmedPercent}: confirmed·백분율 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code probability(value, decimals)}: 현재 조건의 확률을 조회한다. 입력: {@code value}(값), {@code
   *       decimals}(소수자릿수). 반환: {@code Bounds}.
   *   <li>{@code quantity(value)}: quantity의 입력에 따른 분기·결과를 처리한다. 입력: {@code value}(값). 반환: {@code
   *       Bounds}.
   *   <li>{@code of(value, confirmed)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code value}(값), {@code
   *       confirmed}(confirmed). 반환: {@code Bounds}.
   * </ul>
   */
  public record Bounds(
      Probability lower,
      Probability upper,
      Probability arithmeticWidth,
      boolean exact,
      String confirmedPercent) {
    public static Bounds probability(CertifiedInterval value, int decimals) {
      var probability = value.probability();
      return of(probability, probability.confirmedPercent(decimals).orElse(null));
    }

    public static Bounds quantity(CertifiedInterval value) {
      return of(value, null);
    }

    private static Bounds of(CertifiedInterval value, String confirmed) {
      var width =
          new Fraction(
              value.upperUnits().subtract(value.lowerUnits()),
              java.math.BigInteger.ONE.shiftLeft(value.fractionalBits()));
      return new Bounds(
          Probability.of(value.lower()),
          Probability.of(value.upper()),
          Probability.of(width),
          value.exact(),
          confirmed);
    }
  }

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Bounds}).
   * </ul>
   */
  public record Outcome(String kind, String stateId, Bounds probability) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code exits}: 종료목록 ({@code Map<String, Bounds>}).
   *   <li>{@code active}: 활성 ({@code Bounds}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<String, Bounds>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<String, Bounds>}).
   *   <li>{@code checkpoints}: 체크포인트목록 ({@code List<Checkpoint>}).
   * </ul>
   */
  public record Point(
      int cycles,
      Map<String, Bounds> exits,
      Bounds active,
      Map<String, Bounds> initialUses,
      Map<String, Bounds> cycleUses,
      List<Checkpoint> checkpoints) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code target}: 대상 ({@code Target}).
   *   <li>{@code probability}: 확률 ({@code Bounds}).
   * </ul>
   */
  public record Checkpoint(Target target, Bounds probability) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 제작방법의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code sourceStateId}: 출처·상태·식별자 ({@code String}).
   *   <li>{@code initialActions}: 초기값·행동목록 ({@code List<String>}).
   *   <li>{@code cycleActions}: 주기·행동목록 ({@code List<String>}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   * </ul>
   */
  public record Method(
      String id,
      String sourceStateId,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      List<Point> points) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 대상의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code goal}: 목표 ({@code String}).
   *   <li>{@code entries}: 항목목록 ({@code List<String>}).
   * </ul>
   */
  public record Target(String goal, List<String> entries) {}

  /**
   * {@code CertifiedMethodProtocol}에서 사용하는 로컬의 불변 값이다.
   *
   * <p>기존 계약: A null target ranks the whole goal; a target ranks one observed checkpoint pattern.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code target}: 대상 ({@code Target}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code methodIds}: 제작방법·식별자목록 ({@code List<String>}).
   * </ul>
   */
  public record Local(
      String stateId, int cycles, Target target, String status, List<String> methodIds) {}

  static Method method(
      CertifiedMethodSearchEngine.MethodView method, CertifiedInterval.Precision precision) {
    var points = new ArrayList<Point>();
    method.points().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              var point = entry.getValue();
              var exits = new LinkedHashMap<String, Bounds>();
              point
                  .exits()
                  .forEach(
                      (kind, law) ->
                          exits.put(
                              kind.name(),
                              Bounds.probability(
                                  sum(law, precision), precision.percentDecimals())));
              points.add(
                  new Point(
                      entry.getKey(),
                      Map.copyOf(exits),
                      Bounds.probability(
                          sum(point.active(), precision), precision.percentDecimals()),
                      quantities(point.initialUses()),
                      quantities(point.cycleUses()),
                      method
                          .checkpoints()
                          .getOrDefault(entry.getKey(), Map.of())
                          .entrySet()
                          .stream()
                          .sorted(Comparator.comparing(e -> e.getKey().toString()))
                          .map(
                              e ->
                                  new Checkpoint(
                                      target(e.getKey()),
                                      Bounds.probability(
                                          e.getValue(), precision.percentDecimals())))
                          .toList()));
            });
    return new Method(
        method.id(),
        method.source(),
        method.initialActions().stream().map(Enum::name).toList(),
        method.actions().stream().map(Enum::name).toList(),
        method.status(),
        List.copyOf(points));
  }

  static List<Local> locals(
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods,
      CertifiedInterval.Precision precision) {
    var bySource = new HashMap<String, List<CertifiedMethodSearchEngine.MethodView>>();
    methods.forEach(m -> bySource.computeIfAbsent(m.source(), ignored -> new ArrayList<>()).add(m));
    var result = new ArrayList<Local>();
    for (var state : states) {
      var direct = bySource.getOrDefault(state.id(), List.of());
      for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
        boolean complete = state.expanded();
        var choices = new ArrayList<CertifiedRecommendations.Choice>();
        for (var method : direct) {
          var point = method.points().get(cycles);
          if (point == null) {
            complete = false;
            continue;
          }
          choices.add(
              new CertifiedRecommendations.Choice(
                  method.id(),
                  sum(point.exits().get(ExactMethodRunner.Exit.GOAL), precision),
                  sum(point.exits().get(ExactMethodRunner.Exit.UNRESOLVED), precision)));
        }
        var ranked = CertifiedRecommendations.local(choices, complete);
        result.add(new Local(state.id(), cycles, null, ranked.status(), ranked.methodIds()));
        var targets = new LinkedHashSet<MethodSearchCalculation.Progress>();
        for (var method : direct)
          targets.addAll(method.checkpoints().getOrDefault(cycles, Map.of()).keySet());
        targets.remove(state.progress());
        targets.removeIf(
            target ->
                unresolved(target.goal())
                    || target.entries().stream().anyMatch(CertifiedMethodProtocol::unresolved));
        for (var target : targets) {
          var targetChoices = new ArrayList<CertifiedRecommendations.Choice>();
          for (var method : direct) {
            var point = method.points().get(cycles);
            if (point == null) continue;
            targetChoices.add(
                new CertifiedRecommendations.Choice(
                    method.id(),
                    method
                        .checkpoints()
                        .getOrDefault(cycles, Map.of())
                        .getOrDefault(target, CertifiedInterval.zero(precision.fractionalBits())),
                    sum(point.exits().get(ExactMethodRunner.Exit.UNRESOLVED), precision)));
          }
          var targetRanked = CertifiedRecommendations.local(targetChoices, complete);
          result.add(
              new Local(
                  state.id(),
                  cycles,
                  target(target),
                  targetRanked.status(),
                  targetRanked.methodIds()));
        }
      }
    }
    return List.copyOf(result);
  }

  private static boolean unresolved(GoalFilter.Status status) {
    return status == GoalFilter.Status.UNKNOWN || status == GoalFilter.Status.UNSUPPORTED;
  }

  private static Target target(MethodSearchCalculation.Progress progress) {
    return new Target(progress.goal().name(), progress.entries().stream().map(Enum::name).toList());
  }

  private static CertifiedInterval sum(
      Map<?, CertifiedInterval> values, CertifiedInterval.Precision precision) {
    return CertifiedLawStore.total(values, precision.fractionalBits());
  }

  private static Map<String, Bounds> quantities(Map<WorkbenchCurrency, CertifiedInterval> values) {
    var result = new LinkedHashMap<String, Bounds>();
    values.forEach((action, value) -> result.put(action.name(), Bounds.quantity(value)));
    return Map.copyOf(result);
  }
}
