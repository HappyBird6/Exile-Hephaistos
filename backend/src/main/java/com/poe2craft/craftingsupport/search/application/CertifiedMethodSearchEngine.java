package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Evaluation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.BasicClassIndex;
import com.poe2craft.craftingsupport.search.domain.BasicTransitionClasses;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedLawStore;
import com.poe2craft.craftingsupport.search.domain.CertifiedMethodRunner;
import com.poe2craft.craftingsupport.search.domain.CertifiedMutableLawStore;
import com.poe2craft.craftingsupport.search.domain.CertifiedOutcomeStrategy;
import com.poe2craft.craftingsupport.search.domain.ConditionalMethodLaw;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.craftingsupport.search.domain.IndexedCertifiedMethodRunner;
import com.poe2craft.craftingsupport.search.domain.IndexedKernelStore;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 조건부 아이템 분포를 유지하며 검증 가능한 확률 구간으로 방법 탐색을 계산한다.
 *
 * <p>기존 계약: Certified class marginals. Incoming item laws remain exact retained conditional
 * operators.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "certified-method-map-v11-backed-pages"} ({@code String}).
 *   <li>{@code laws}: 분포목록 ({@code CertifiedLawStore}).
 *   <li>{@code absorptionStore}: 흡수·저장소 ({@code CertifiedMutableLawStore}).
 *   <li>{@code workingLaws}: 계산중·분포목록 ({@code TemporaryWorkingLaws}).
 *   <li>{@code workingMaps}: 계산중·맵목록 ({@code CertifiedMethodRunner.WorkingMaps<Integer>}).
 *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
 *   <li>{@code classStorage}: 동치류·저장소 ({@code TemporaryClassIndex}).
 *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
 *   <li>{@code progressCache}: 진행상황·캐시 ({@code Map<Integer, MethodSearchCalculation.Progress>}).
 *   <li>{@code entryOffsets}: 항목·Offsets ({@code List<Integer>}).
 *   <li>{@code liveEntries}: 현재유효·항목목록 ({@code long}).
 *   <li>{@code savedEntries}: 저장된·항목목록 ({@code long}).
 *   <li>{@code preparationEntries}: 준비·항목목록 ({@code long}).
 *   <li>{@code freezeOwner}: 불변복사·소유자 ({@code Method}).
 *   <li>{@code preparations}: preparations ({@code Map<Preparation, Prepared>}).
 *   <li>{@code lawViews}: 분포·Views ({@code Map<Map<Integer, CertifiedInterval>, Map<String,
 *       CertifiedInterval>>}).
 *   <li>{@code progressLaws}: 진행상황·분포목록 ({@code Map<Map<Integer, CertifiedInterval>,
 *       Map<MethodSearchCalculation.Progress, CertifiedInterval>>}).
 *   <li>{@code progressLawEntries}: 진행상황·분포·항목목록 ({@code long}).
 *   <li>{@code targetSummaryEntries}: 대상·요약·항목목록 ({@code long}).
 *   <li>{@code kernels}: 전이커널목록 ({@code IndexedKernelStore}).
 *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, Evaluation>}).
 *   <li>{@code states}: 상태목록 ({@code Map<Integer, State>}).
 *   <li>{@code methods}: 제작방법목록 ({@code Map<String, Method>}).
 *   <li>{@code ready}: 준비완료 ({@code ArrayDeque<Method>}).
 *   <li>{@code shallowestReadyDepth}: shallowest·준비완료·깊이 ({@code int}).
 *   <li>{@code adjacency}: 인접목록 ({@code Map<Integer, Set<Integer>>}).
 *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
 *   <li>{@code rootLaw}: 시작점·분포 ({@code ConditionalMethodLaw}).
 *   <li>{@code root}: 시작점 ({@code int}).
 *   <li>{@code budget}: 계산예산 ({@code int}).
 *   <li>{@code operations}: 연산목록 ({@code long}).
 *   <li>{@code diagnostics}: 진단값 ({@code boolean}).
 *   <li>{@code diagnosticSteps}: diagnostic·단계목록 ({@code Map<String, Long>}).
 *   <li>{@code graphRevision}: 그래프·리비전 ({@code long}).
 *   <li>{@code undiscovered}: undiscovered ({@code int}).
 *   <li>{@code previewedPatterns}: previewed·Patterns ({@code Set<PreviewPattern>}).
 *   <li>{@code frontier}: 미탐색경계 ({@code PriorityQueue<Expansion>}).
 *   <li>{@code previews}: previews ({@code PriorityQueue<Expansion>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code enableDiagnostics()}: enable·진단값 처리에서 {@code kernels.enableDiagnostics}을(를) 사용한다.
 *   <li>{@code workload()}: 작업량 값을 조회·구성한다. 반환: {@code Workload}.
 *   <li>{@code settleCheckpoint()}: settle·체크포인트 처리에서 {@code methods.values}, {@code
 *       runner.retainedStates}을(를) 사용한다.
 *   <li>{@code checkpoint(json, sink)}: 현재 진행 상태의 복구 지점을 기록한다. 입력: {@code json}(JSON), {@code
 *       sink}(sink). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code checkpointRestore(json)}: 체크포인트·복원의 입력에 따른 분기·결과를 처리한다. 입력: {@code json}(JSON). 반환:
 *       {@code CheckpointRestore}.
 *   <li>{@code matched(progress)}: 일치한 처리에서 {@code progress.entries}을(를) 사용한다. 입력: {@code
 *       progress}(진행상황). 반환: {@code int}.
 *   <li>{@code previewSlots(expansion)}: 미리보기·슬롯목록 처리에서 {@code index.witness}, {@code
 *       expansion.state}을(를) 사용한다. 입력: {@code expansion}(확장). 반환: {@code int}.
 *   <li>{@code CertifiedMethodSearchEngine(start, classes, evaluate, budget, precision)}: 기본 정책을
 *       보충해 공통 생성자로 위임한다. 입력: {@code start}(시작값), {@code classes}(동치류목록), {@code evaluate}(평가),
 *       {@code budget}(계산예산), {@code precision}(정밀도).
 *   <li>{@code CertifiedMethodSearchEngine(start, classes, evaluate, budget, precision, backing)}:
 *       전달받은 값으로 구성원을 초기화한다. 입력: {@code start}(시작값), {@code classes}(동치류목록), {@code evaluate}(평가),
 *       {@code budget}(계산예산), {@code precision}(정밀도), {@code backing}(backing).
 *   <li>{@code rootLaw()}: {@code rootLaw} 값을 조회한다. 반환: {@code ConditionalMethodLaw}.
 *   <li>{@code observation(method, cycles)}: 관측값 처리에서 {@code methods.get}, {@code saved.get}을(를)
 *       사용한다. 입력: {@code method}(제작방법), {@code cycles}(주기횟수). 반환: {@code
 *       IndexedCertifiedMethodRunner.Snapshot}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 *   <li>{@code classId(id)}: 동치류·식별자 처리에서 {@code states.values}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code int}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code id(key)}: 식별자의 입력에 따른 분기·결과를 처리한다. 입력: {@code key}(키). 반환: {@code String}.
 *   <li>{@code rootId()}: 시작점·식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code rootDirectComplete()}: 시작점·직접·완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code calculationVersion()}: {@code VERSION} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code stateVersion()}: 상태·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code templatesVersion()}: 템플릿목록·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code operations()}: {@code operations} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code graphRevision()}: {@code graphRevision} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code retainedEntries()}: 보유·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code workingEntries()}: 계산중·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code storage()}: 저장소 값을 조회·구성한다. 반환: {@code CertifiedLawStore.Metrics}.
 *   <li>{@code absorptionStorage()}: 흡수·저장소 값을 조회·구성한다. 반환: {@code
 *       CertifiedMutableLawStore.Metrics}.
 *   <li>{@code kernelStorage()}: 전이커널·저장소 값을 조회·구성한다. 반환: {@code IndexedKernelStore.CacheMetrics}.
 *   <li>{@code freezeTransientBytes()}: 불변복사·임시·바이트수 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code storageLimited()}: 저장소·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code classCount()}: 동치류·개수 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code retention()}: 보유량 값을 조회·구성한다. 반환: {@code Retention}.
 *   <li>{@code precision()}: {@code precision} 값을 조회한다. 반환: {@code CertifiedInterval.Precision}.
 *   <li>{@code memoryLimited()}: 메모리·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code increaseMemoryBudget(limit)}: 메모리·계산예산을(를) 한도를 늘린다. 입력: {@code limit}(한도). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code state(key, depth)}: 현재 상태 표현을 조회한다. 입력: {@code key}(키), {@code depth}(깊이). 반환:
 *       {@code State}.
 *   <li>{@code link(source, target)}: 연결 처리에서 {@code adjacency.computeIfAbsent}, {@code
 *       frontier.add}을(를) 사용한다. 입력: {@code source}(출처), {@code target}(대상).
 *   <li>{@code previewCandidate(state)}: 계산 중 후보의 미리보기 값을 만든다. 입력: {@code state}(상태).
 *   <li>{@code preview(source)}: 현재 진행 상태의 미리보기 값을 만든다. 입력: {@code source}(출처).
 *   <li>{@code discover(source)}: discover 처리에서 {@code progress.goal}, {@code
 *       BasicCurrencyTransitions.supportedActions}을(를) 사용한다. 입력: {@code source}(출처).
 *   <li>{@code add(source, initial, actions)}: 값 또는 항목을 더한다. 입력: {@code source}(출처), {@code
 *       initial}(초기값), {@code actions}(행동목록).
 *   <li>{@code progress(key)}: 진행상황 처리에서 {@code progressCache.computeIfAbsent}, {@code
 *       index.key}을(를) 사용한다. 입력: {@code key}(키). 반환: {@code MethodSearchCalculation.Progress}.
 *   <li>{@code classify(source, key, structural)}: 완전한 전이 특성을 기준으로 상태의 동치류를 판정한다. 입력: {@code
 *       source}(출처), {@code key}(키), {@code structural}(구조적). 반환: {@code Optional<Exit>}.
 *   <li>{@code states()}: 상태목록 값을 조회·구성한다. 반환: {@code List<MethodSearchEngine.StateView>}.
 *   <li>{@code methods()}: 제작방법목록 값을 조회·구성한다. 반환: {@code List<MethodView>}.
 *   <li>{@code strategyFitsPreparationBudget()}: 전략·Fits·준비·계산예산 값을 조회·구성한다. 반환: {@code boolean}.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code strategyWork()}: 전략·계산작업 값을 조회·구성한다. 반환: {@code CertifiedStrategyWork}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code strategy(cycles)}: 전략 처리에서 {@code OBSERVATIONS.contains}, {@code states.values}을(를)
 *       사용한다. 입력: {@code cycles}(주기횟수). 반환: {@code CertifiedOutcomeStrategy<String>}. 사용하는 예외:
 *       {@code IllegalStateException}, {@code IllegalArgumentException}.
 *   <li>{@code ids(input)}: 식별자목록 처리에서 {@code lawViews.computeIfAbsent}, {@code law.size}을(를) 사용한다.
 *       입력: {@code input}(입력). 반환: {@code Map<String, CertifiedInterval>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Preparation}: {@code CertifiedMethodSearchEngine}에서 사용하는 준비의 불변 값이다.
 *   <li>{@code Prepared}: {@code CertifiedMethodSearchEngine}에서 사용하는 준비된의 불변 값이다.
 *   <li>{@code DepthWork}: {@code CertifiedMethodSearchEngine}에서 사용하는 깊이·계산작업의 불변 값이다.
 *   <li>{@code Workload}: {@code CertifiedMethodSearchEngine}에서 사용하는 작업량의 불변 값이다.
 *   <li>{@code CheckpointLine}: {@code CertifiedMethodSearchEngine}에서 사용하는 체크포인트·행의 불변 값이다.
 *   <li>{@code ClassCheckpoint}: {@code CertifiedMethodSearchEngine}에서 사용하는 동치류·체크포인트의 불변 값이다.
 *   <li>{@code StateCheckpoint}: {@code CertifiedMethodSearchEngine}에서 사용하는 상태·체크포인트의 불변 값이다.
 *   <li>{@code MethodCheckpoint}: {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·체크포인트의 불변 값이다.
 *   <li>{@code PointCheckpoint}: {@code CertifiedMethodSearchEngine}에서 사용하는 지점·체크포인트의 불변 값이다.
 *   <li>{@code QueueCheckpoint}: {@code CertifiedMethodSearchEngine}에서 사용하는 대기열·체크포인트의 불변 값이다.
 *   <li>{@code CheckpointRestore}: {@code CertifiedMethodSearchEngine} 내부의 체크포인트·복원 처리를 맡는다.
 *   <li>{@code Expansion}: {@code CertifiedMethodSearchEngine}에서 사용하는 확장의 불변 값이다.
 *   <li>{@code PreviewPattern}: {@code CertifiedMethodSearchEngine}에서 사용하는 미리보기·패턴의 불변 값이다.
 *   <li>{@code Retention}: {@code CertifiedMethodSearchEngine}에서 사용하는 보유량의 불변 값이다.
 *   <li>{@code MethodRetention}: {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·보유량의 불변 값이다.
 *   <li>{@code MethodView}: {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·조회표현의 불변 값이다.
 *   <li>{@code IdentifierLaw}: {@code CertifiedMethodSearchEngine} 내부의 Identifier·분포 처리를 맡는다.
 *   <li>{@code State}: {@code CertifiedMethodSearchEngine} 내부의 상태 처리를 맡는다.
 *   <li>{@code Method}: {@code CertifiedMethodSearchEngine} 내부의 제작방법 처리를 맡는다.
 * </ul>
 */
final class CertifiedMethodSearchEngine {
  static final String VERSION = "certified-method-map-v11-backed-pages";
  private final CertifiedLawStore laws;
  private final CertifiedMutableLawStore absorptionStore;
  private final TemporaryWorkingLaws workingLaws;
  private final CertifiedMethodRunner.WorkingMaps<Integer> workingMaps;
  private final BasicClassIndex index;
  private final TemporaryClassIndex classStorage;
  private final CertifiedInterval.Precision precision;
  private final Map<Integer, MethodSearchCalculation.Progress> progressCache =
      new LinkedHashMap<>(16, .75f, true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<Integer, MethodSearchCalculation.Progress> eldest) {
          return size() > 512;
        }
      };
  private final List<Integer> entryOffsets = new ArrayList<>();
  private long liveEntries, savedEntries;
  private long preparationEntries;
  private Method freezeOwner;

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 준비의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code MethodSearchCalculation.Progress}).
   *   <li>{@code cycle}: 주기 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code law}: 분포 ({@code Map<Integer, Fraction>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   * </ul>
   */
  private record Preparation(
      int source,
      MethodSearchCalculation.Progress progress,
      List<WorkbenchCurrency> cycle,
      Map<Integer, Fraction> law,
      Fraction unresolved,
      boolean unavailable) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 준비된의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code Method}).
   *   <li>{@code law}: 분포 ({@code IndexedCertifiedMethodRunner.InitialLaw}).
   * </ul>
   */
  private record Prepared(Method owner, IndexedCertifiedMethodRunner.InitialLaw law) {}

  private final Map<Preparation, Prepared> preparations = new HashMap<>();
  private final Map<Map<Integer, CertifiedInterval>, Map<String, CertifiedInterval>> lawViews =
      new IdentityHashMap<>();
  private final Map<
          Map<Integer, CertifiedInterval>, Map<MethodSearchCalculation.Progress, CertifiedInterval>>
      progressLaws = new IdentityHashMap<>();
  private long progressLawEntries, targetSummaryEntries;
  private final IndexedKernelStore kernels;
  private final Function<ItemState, Evaluation> evaluate;
  private final Map<Integer, State> states = new LinkedHashMap<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private int shallowestReadyDepth = Integer.MAX_VALUE;
  private final Map<Integer, Set<Integer>> adjacency = new HashMap<>();
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
  private final ConditionalMethodLaw rootLaw;
  private final int root;
  private int budget;
  private long operations;
  private boolean diagnostics;
  private final Map<String, Long> diagnosticSteps = new LinkedHashMap<>();

  void enableDiagnostics() {
    diagnostics = true;
    kernels.enableDiagnostics();
  }

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 깊이·계산작업의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code states}: 상태목록 ({@code long}).
   *   <li>{@code goals}: 목표목록 ({@code long}).
   *   <li>{@code expanded}: 확장된 ({@code long}).
   *   <li>{@code complete}: 완료 ({@code long}).
   *   <li>{@code undiscoveredActions}: undiscovered·행동목록 ({@code long}).
   *   <li>{@code methods}: 제작방법목록 ({@code long}).
   *   <li>{@code completedMethods}: 완료·제작방법목록 ({@code long}).
   * </ul>
   */
  record DepthWork(
      int depth,
      long states,
      long goals,
      long expanded,
      long complete,
      long undiscoveredActions,
      long methods,
      long completedMethods) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 작업량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code operations}: 연산목록 ({@code long}).
   *   <li>{@code methods}: 제작방법목록 ({@code long}).
   *   <li>{@code completedMethods}: 완료·제작방법목록 ({@code long}).
   *   <li>{@code unfinishedMethods}: unfinished·제작방법목록 ({@code long}).
   *   <li>{@code unresolvedMethods}: 미해결·제작방법목록 ({@code long}).
   *   <li>{@code delegates}: delegates ({@code long}).
   *   <li>{@code queued}: 대기중 ({@code long}).
   *   <li>{@code uniqueQueued}: 고유·대기중 ({@code long}).
   *   <li>{@code steps}: 단계목록 ({@code Map<String, Long>}).
   *   <li>{@code depths}: depths ({@code List<DepthWork>}).
   *   <li>{@code kernels}: 전이커널목록 ({@code IndexedKernelStore.Workload}).
   * </ul>
   */
  record Workload(
      long operations,
      long methods,
      long completedMethods,
      long unfinishedMethods,
      long unresolvedMethods,
      long delegates,
      long queued,
      long uniqueQueued,
      Map<String, Long> steps,
      List<DepthWork> depths,
      IndexedKernelStore.Workload kernels) {}

  /** On-demand accounting, separate from the probability graph and production UI. */
  Workload workload() {
    var levels = new TreeMap<Integer, long[]>();
    for (var state : states.values()) {
      var counts = levels.computeIfAbsent(state.depth, ignored -> new long[7]);
      counts[0]++;
      if (state.progress.goal() == Status.MATCH) counts[1]++;
      if (state.expanded) counts[2]++;
      if (state.expanded && state.methods.stream().allMatch(m -> m.done)) counts[3]++;
      if (!state.expanded)
        counts[4] += BasicCurrencyTransitions.supportedActions().size() - state.discovery;
      counts[5] += state.methods.size();
      counts[6] += state.methods.stream().filter(m -> m.done).count();
    }
    long done = methods.values().stream().filter(m -> m.done).count();
    return new Workload(
        operations,
        methods.size(),
        done,
        methods.size() - done,
        methods.values().stream().filter(m -> m.unresolved).count(),
        methods.values().stream().filter(m -> m.delegate != null).count(),
        ready.size(),
        ready.stream().map(m -> m.id).distinct().count(),
        Map.copyOf(diagnosticSteps),
        levels.entrySet().stream()
            .map(
                e -> {
                  var n = e.getValue();
                  return new DepthWork(e.getKey(), n[0], n[1], n[2], n[3], n[4], n[5], n[6]);
                })
            .toList(),
        kernels.workload());
  }

  private long graphRevision;

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 체크포인트·행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code value}: 값 ({@code JsonNode}).
   * </ul>
   */
  record CheckpointLine(String kind, JsonNode value) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 동치류·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code int}).
   *   <li>{@code witness}: 증명상태 ({@code BasicCurrencyState}).
   *   <li>{@code key}: 키 ({@code JsonNode}).
   * </ul>
   */
  record ClassCheckpoint(int id, BasicCurrencyState witness, JsonNode key) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 상태·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code int}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code discovery}: 발견 ({@code int}).
   *   <li>{@code previewDiscovery}: 미리보기·발견 ({@code int}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   * </ul>
   */
  record StateCheckpoint(
      int key, int depth, int discovery, int previewDiscovery, boolean expanded) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code initial}: 초기값 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code done}: 완료여부 ({@code boolean}).
   *   <li>{@code executionDone}: 실행·완료여부 ({@code boolean}).
   *   <li>{@code delegate}: 위임대상 ({@code String}).
   * </ul>
   */
  record MethodCheckpoint(
      String id,
      int source,
      List<WorkbenchCurrency> initial,
      List<WorkbenchCurrency> actions,
      boolean done,
      boolean executionDone,
      String delegate) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 지점·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code method}: 제작방법 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code point}: 지점 ({@code CertifiedMethodRunner.Observation<Integer,
   *       WorkbenchCurrency>}).
   * </ul>
   */
  record PointCheckpoint(
      String method,
      int cycles,
      CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 대기열·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code operations}: 연산목록 ({@code long}).
   *   <li>{@code graphRevision}: 그래프·리비전 ({@code long}).
   *   <li>{@code budget}: 계산예산 ({@code int}).
   *   <li>{@code ready}: 준비완료 ({@code List<String>}).
   *   <li>{@code frontier}: 미탐색경계 ({@code List<int[]>}).
   *   <li>{@code previews}: previews ({@code List<int[]>}).
   *   <li>{@code patterns}: patterns ({@code List<PreviewPattern>}).
   *   <li>{@code adjacency}: 인접목록 ({@code Map<Integer, Set<Integer>>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   * </ul>
   */
  record QueueCheckpoint(
      long operations,
      long graphRevision,
      int budget,
      List<String> ready,
      List<int[]> frontier,
      List<int[]> previews,
      List<PreviewPattern> patterns,
      Map<Integer, Set<Integer>> adjacency,
      List<WorkbenchSimulator.Assumption> assumptions) {}

  /** Finish publication bookkeeping only; this never advances a primitive action. */
  void settleCheckpoint() {
    long before =
        methods.values().stream()
            .filter(m -> m.runner != null)
            .mapToLong(m -> m.runner.retainedStates())
            .sum();
    if (freezeOwner != null) {
      var owner = freezeOwner;
      while (owner.freezing != null) owner.step();
    }
    for (var method : methods.values()) {
      while (!method.publications.isEmpty()) method.step();
    }
    ready.removeIf(m -> m.done);
    liveEntries +=
        methods.values().stream()
                .filter(m -> m.runner != null)
                .mapToLong(m -> m.runner.retainedStates())
                .sum()
            - before;
    shallowestReadyDepth =
        ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
  }

  void checkpoint(ObjectMapper json, Consumer<CheckpointLine> sink) {
    if (freezeOwner != null || methods.values().stream().anyMatch(m -> !m.publications.isEmpty()))
      throw new IllegalStateException("Checkpoint publication is not settled");
    for (int id = 0; id < index.size(); id++)
      sink.accept(
          new CheckpointLine(
              "CLASS",
              json.valueToTree(
                  new ClassCheckpoint(id, index.witness(id), json.valueToTree(index.key(id))))));
    for (var state : states.values())
      sink.accept(
          new CheckpointLine(
              "STATE",
              json.valueToTree(
                  new StateCheckpoint(
                      state.key,
                      state.depth,
                      state.discovery,
                      state.previewDiscovery,
                      state.expanded))));
    for (var method : methods.values()) {
      sink.accept(
          new CheckpointLine(
              "METHOD",
              json.valueToTree(
                  new MethodCheckpoint(
                      method.id,
                      method.source.key,
                      method.initial,
                      method.actions,
                      method.done,
                      method.executionDone,
                      method.delegate == null ? null : method.delegate.id))));
      for (var point : method.saved.entrySet())
        sink.accept(
            new CheckpointLine(
                "POINT",
                json.valueToTree(
                    new PointCheckpoint(
                        method.id, point.getKey(), point.getValue().observation()))));
      if (method.runner != null)
        try (var lease = method.runner.leaseCursor()) {
          sink.accept(new CheckpointLine("RUNNER", json.valueToTree(lease.header())));
          lease
              .rows()
              .forEachRemaining(
                  row -> sink.accept(new CheckpointLine("ROW", json.valueToTree(row))));
          sink.accept(new CheckpointLine("END_RUNNER", json.valueToTree(method.id)));
        }
    }
    sink.accept(
        new CheckpointLine(
            "QUEUES",
            json.valueToTree(
                new QueueCheckpoint(
                    operations,
                    graphRevision,
                    budget,
                    ready.stream().map(m -> m.id).toList(),
                    frontier.stream().map(e -> new int[] {e.state.key, e.depth}).toList(),
                    previews.stream().map(e -> new int[] {e.state.key, e.depth}).toList(),
                    List.copyOf(previewedPatterns),
                    adjacency,
                    assumptions()))));
  }

  CheckpointRestore checkpointRestore(ObjectMapper json) {
    return new CheckpointRestore(json);
  }

  /**
   * {@code CertifiedMethodSearchEngine} 내부의 체크포인트·복원 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
   *   <li>{@code delegates}: delegates ({@code Map<String, String>}).
   *   <li>{@code current}: 현재 ({@code Method}).
   *   <li>{@code runner}: 실행기 ({@code IndexedCertifiedMethodRunner.Restoration}).
   *   <li>{@code queues}: queues ({@code QueueCheckpoint}).
   *   <li>{@code finished}: 종료 ({@code boolean}).
   *   <li>{@code classes}: 동치류목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code CheckpointRestore(json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON). 사용하는 예외:
   *       {@code IllegalStateException}.
   *   <li>{@code accept(line)}: 수락 처리에서 {@code line.kind}, {@code json.convertValue}을(를) 사용한다. 입력:
   *       {@code line}(행). 사용하는 예외: {@code IllegalStateException}, {@code
   *       IllegalArgumentException}.
   *   <li>{@code finish()}: 진행 중인 계산 또는 누적 처리를 마무리한다. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  final class CheckpointRestore implements AutoCloseable {
    final ObjectMapper json;
    final Map<String, String> delegates = new LinkedHashMap<>();
    Method current;
    IndexedCertifiedMethodRunner.Restoration runner;
    QueueCheckpoint queues;
    boolean finished;
    int classes;

    CheckpointRestore(ObjectMapper json) {
      if (operations != 0 || index.size() != 1)
        throw new IllegalStateException("Fresh engine required");
      this.json = json;
      states.clear();
      frontier.clear();
      previews.clear();
      undiscovered = 0;
    }

    void accept(CheckpointLine line) {
      if (finished) throw new IllegalStateException("Checkpoint restore finished");
      switch (line.kind()) {
        case "CLASS" -> {
          var row = json.convertValue(line.value(), ClassCheckpoint.class);
          if (row.id() != classes++
              || index.intern(row.witness()) != row.id()
              || !json.valueToTree(index.key(row.id()))
                  .equals(
                      (a, b) ->
                          a.isNumber() && b.isNumber()
                              ? a.decimalValue().compareTo(b.decimalValue())
                              : a.equals(b) ? 0 : 1,
                      row.key()))
            throw new IllegalArgumentException("Transition class identity changed");
        }
        case "STATE" -> {
          var row = json.convertValue(line.value(), StateCheckpoint.class);
          int actions = BasicCurrencyTransitions.supportedActions().size();
          if (row.depth() < 0
              || row.discovery() < 0
              || row.discovery() > actions
              || row.previewDiscovery() < 0
              || row.previewDiscovery() > actions)
            throw new IllegalArgumentException("Invalid discovery position");
          var state = new State(row.key(), row.depth(), progress(row.key()));
          state.discovery = row.discovery();
          state.previewDiscovery = row.previewDiscovery();
          state.expanded = row.expanded();
          if (states.putIfAbsent(row.key(), state) != null)
            throw new IllegalArgumentException("Duplicate state");
          if (!state.expanded) undiscovered++;
        }
        case "METHOD" -> {
          if (runner != null) throw new IllegalArgumentException("Unfinished runner");
          var row = json.convertValue(line.value(), MethodCheckpoint.class);
          if (row.actions().isEmpty()
              || !row.id().equals(id(row.source()) + ":" + row.initial() + ">" + row.actions()))
            throw new IllegalArgumentException("Method definition identity changed");
          current =
              new Method(
                  Objects.requireNonNull(states.get(row.source())),
                  row.id(),
                  row.initial(),
                  row.actions());
          current.done = row.done();
          current.executionDone = row.executionDone();
          current.prepared = true;
          if (methods.putIfAbsent(row.id(), current) != null)
            throw new IllegalArgumentException("Duplicate method");
          current.source.methods.add(current);
          if (row.delegate() != null) delegates.put(row.id(), row.delegate());
        }
        case "POINT" -> {
          var row = json.convertValue(line.value(), PointCheckpoint.class);
          if (current == null || !current.id.equals(row.method()))
            throw new IllegalArgumentException("Point owner mismatch");
          if (!MethodSearchCalculation.OBSERVATIONS.contains(row.cycles())
              || current.saved.containsKey(row.cycles())
              || row.point().cycles() != row.cycles())
            throw new IllegalArgumentException("Invalid observation horizon");
          var method = current;
          var point =
              IndexedCertifiedMethodRunner.restoredSnapshot(
                      index,
                      method.source.key,
                      method.template(),
                      target ->
                          classify(
                              method.source,
                              target,
                              method.initial.isEmpty() && method.actions.size() == 1),
                      precision,
                      row.point())
                  .freeze(laws);
          // Derive only display summaries. Discovery queues and adjacency are restored separately.
          var parts = new ArrayList<>(point.observation().exits().values());
          parts.add(point.observation().active());
          for (var law : parts) {
            var totals = new LinkedHashMap<MethodSearchCalculation.Progress, CertifiedInterval>();
            law.forEach(
                (target, mass) ->
                    totals.merge(progress(target), mass, (a, b) -> a.add(b).probability()));
            progressLaws.put(law, Map.copyOf(totals));
            progressLawEntries += totals.size() + 1;
            current.linkedLaws.add(law);
          }
          current.publish(row.cycles(), point);
        }
        case "RUNNER" -> {
          if (current == null || runner != null)
            throw new IllegalArgumentException("Invalid runner owner");
          CertifiedMethodRunner.CursorHeader<Integer> header =
              json.convertValue(
                  line.value(),
                  json.getTypeFactory()
                      .constructParametricType(
                          CertifiedMethodRunner.CursorHeader.class, Integer.class));
          var method = current;
          runner =
              IndexedCertifiedMethodRunner.restoreCursor(
                  header,
                  kernels,
                  method.source.key,
                  method.template(),
                  target ->
                      classify(
                          method.source,
                          target,
                          method.initial.isEmpty() && method.actions.size() == 1),
                  precision,
                  absorptionStore,
                  workingMaps);
        }
        case "ROW" -> {
          if (runner == null) throw new IllegalArgumentException("Missing runner header");
          runner.accept(
              json.convertValue(
                  line.value(),
                  json.getTypeFactory()
                      .constructParametricType(
                          CertifiedMethodRunner.CursorRow.class,
                          Integer.class,
                          WorkbenchCurrency.class)));
        }
        case "END_RUNNER" -> {
          if (runner == null || !current.id.equals(line.value().asText()))
            throw new IllegalArgumentException("Runner owner mismatch");
          current.runner = runner.finish();
          runner = null;
          liveEntries += current.runner.retainedStates();
        }
        case "QUEUES" -> {
          if (queues != null) throw new IllegalArgumentException("Duplicate queues");
          queues = json.convertValue(line.value(), QueueCheckpoint.class);
        }
        default -> throw new IllegalArgumentException("Unknown engine checkpoint row");
      }
    }

    void finish() {
      if (finished || runner != null || queues == null || !states.containsKey(root))
        throw new IllegalArgumentException("Incomplete engine checkpoint");
      for (var entry : delegates.entrySet()) {
        var method = methods.get(entry.getKey());
        var owner = Objects.requireNonNull(methods.get(entry.getValue()));
        var first = kernels.request(owner.source.key, owner.initial.getFirst());
        while (!first.complete()) first.step();
        var second = kernels.request(method.source.key, method.initial.getFirst());
        while (!second.complete()) second.step();
        method.delegate = owner;
        method.equivalence =
            new IndexedCertifiedMethodRunner.InitialLaw(first)
                .equivalentTo(new IndexedCertifiedMethodRunner.InitialLaw(second));
      }
      ready.clear();
      for (var id : queues.ready()) ready.addLast(Objects.requireNonNull(methods.get(id)));
      frontier.clear();
      for (var e : queues.frontier())
        frontier.add(new Expansion(Objects.requireNonNull(states.get(e[0])), e[1]));
      previews.clear();
      for (var e : queues.previews())
        previews.add(new Expansion(Objects.requireNonNull(states.get(e[0])), e[1]));
      previewedPatterns.addAll(queues.patterns());
      adjacency.putAll(queues.adjacency());
      assumptions.addAll(queues.assumptions());
      operations = queues.operations();
      graphRevision = queues.graphRevision();
      budget = queues.budget();
      shallowestReadyDepth =
          ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
      finished = true;
    }

    public void close() {
      if (runner != null) runner.close();
    }
  }

  private int undiscovered;

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 확장의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code State}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   * </ul>
   */
  private record Expansion(State state, int depth) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 미리보기·패턴의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code MethodSearchCalculation.Progress}).
   * </ul>
   */
  private record PreviewPattern(int depth, MethodSearchCalculation.Progress progress) {}

  private final Set<PreviewPattern> previewedPatterns = new HashSet<>();

  private final PriorityQueue<Expansion> frontier =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth).thenComparingInt(e -> e.state().key));
  private final PriorityQueue<Expansion> previews =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth)
              .thenComparingInt(e -> -matched(e.state().progress))
              .thenComparingInt(this::previewSlots)
              .thenComparingInt(e -> e.state().key));

  private static int matched(MethodSearchCalculation.Progress progress) {
    return (int) progress.entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private int previewSlots(Expansion expansion) {
    return index.witness(expansion.state().key).item().explicits().size();
  }

  CertifiedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget,
      CertifiedInterval.Precision precision) {
    this(
        start,
        classes,
        evaluate,
        budget,
        precision,
        new TemporaryLawPages(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), 8L * 1024 * 1024));
  }

  CertifiedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget,
      CertifiedInterval.Precision precision,
      CertifiedLawStore.PackedPages backing) {
    classStorage =
        new TemporaryClassIndex(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), classes, 256);
    this.index = new BasicClassIndex(classes, classStorage);
    this.precision = precision;
    laws = new CertifiedLawStore(precision.fractionalBits(), backing);
    absorptionStore = new CertifiedMutableLawStore(precision.fractionalBits(), 64);
    workingLaws =
        new TemporaryWorkingLaws(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir")),
            precision.fractionalBits(),
            128);
    workingMaps =
        new CertifiedMethodRunner.WorkingMaps<>(workingLaws::pending, workingLaws::boundary);
    int offset = 2;
    for (var group : evaluate.apply(start.item()).groups()) {
      offset++;
      for (var entry : group.entries()) entryOffsets.add(offset++);
    }
    this.evaluate = evaluate;
    this.budget = budget;
    kernels = new IndexedKernelStore(index, 2000, budget);
    root = index.intern(start);
    rootLaw = ConditionalMethodLaw.root(index, Map.of(start, Fraction.ONE));
    state(root, 0);
  }

  ConditionalMethodLaw rootLaw() {
    return rootLaw;
  }

  IndexedCertifiedMethodRunner.Snapshot observation(String method, int cycles) {
    var found = methods.get(method);
    if (found == null) throw new IllegalArgumentException("Unknown method");
    var point = found.saved.get(cycles);
    if (point == null) throw new IllegalStateException("Observation not ready");
    return point;
  }

  int classId(String id) {
    for (var state : states.values()) if (id(state.key).equals(id)) return state.key;
    throw new IllegalArgumentException("Unknown class");
  }

  private String id(int key) {
    return "class:" + key;
  }

  public String rootId() {
    return id(root);
  }

  boolean rootDirectComplete() {
    var state = states.get(root);
    return state.expanded && state.methods.stream().allMatch(m -> m.done);
  }

  public String calculationVersion() {
    return VERSION;
  }

  public String stateVersion() {
    return BasicClassIndex.VERSION;
  }

  public String templatesVersion() {
    return MethodSearchCalculation.TEMPLATES;
  }

  public long operations() {
    return operations;
  }

  long graphRevision() {
    return graphRevision;
  }

  public boolean complete() {
    return ready.isEmpty() && undiscovered == 0;
  }

  public long retainedEntries() {
    return workingEntries() + kernels.cachedEntries();
  }

  private long workingEntries() {
    return index.residentEntries()
        + progressCache.size()
        + states.size()
        + methods.size()
        + index.profileRetainedRows()
        + liveEntries
        + laws.pageCount()
        + laws.descriptors()
        + laws.materializedRows()
        + progressLawEntries
        + targetSummaryEntries
        + previews.size()
        + previewedPatterns.size()
        + absorptionStore.residentEntries()
        + workingLaws.residentEntries()
        + preparationEntries;
  }

  CertifiedLawStore.Metrics storage() {
    return laws.metrics();
  }

  CertifiedMutableLawStore.Metrics absorptionStorage() {
    return absorptionStore.metrics();
  }

  IndexedKernelStore.CacheMetrics kernelStorage() {
    return kernels.cacheMetrics();
  }

  long freezeTransientBytes() {
    return freezeOwner == null || freezeOwner.freezing == null
        ? 0
        : freezeOwner.freezing.transientBytes();
  }

  boolean storageLimited() {
    return laws.residentPackedBytes() + absorptionStore.packedBytes() > 32L * 1024 * 1024;
  }

  void close() {
    if (freezeOwner != null && freezeOwner.freezing != null) freezeOwner.freezing.close();
    freezeOwner = null;
    ready.clear();
    preparations.clear();
    methods.clear();
    states.clear();
    frontier.clear();
    previews.clear();
    previewedPatterns.clear();
    lawViews.clear();
    progressLaws.clear();
    adjacency.clear();
    progressCache.clear();
    index.close();
    laws.close();
    absorptionStore.close();
    workingLaws.close();
    kernels.close();
  }

  public int classCount() {
    return index.size();
  }

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 보유량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code classes}: 동치류목록 ({@code int}).
   *   <li>{@code states}: 상태목록 ({@code int}).
   *   <li>{@code progressKeys}: 진행상황·키목록 ({@code int}).
   *   <li>{@code adjacencyEdges}: 인접목록·간선목록 ({@code long}).
   *   <li>{@code queuedMethods}: 대기중·제작방법목록 ({@code int}).
   *   <li>{@code activeRunners}: 활성·Runners ({@code int}).
   *   <li>{@code pending}: 대기 ({@code long}).
   *   <li>{@code boundary}: 경계 ({@code long}).
   *   <li>{@code exits}: 종료목록 ({@code long}).
   *   <li>{@code flightOutcomes}: 진행중·결과목록 ({@code long}).
   *   <li>{@code savedAccounting}: 저장된·Accounting ({@code long}).
   *   <li>{@code uniqueSavedEntries}: 고유·저장된·항목목록 ({@code long}).
   *   <li>{@code cachedKernels}: 캐시된·전이커널목록 ({@code int}).
   *   <li>{@code cachedOutcomes}: 캐시된·결과목록 ({@code int}).
   *   <li>{@code pendingKernels}: 대기·전이커널목록 ({@code int}).
   *   <li>{@code calculatedKernels}: calculated·전이커널목록 ({@code long}).
   *   <li>{@code sharedRequests}: 공유·요청목록 ({@code long}).
   *   <li>{@code kernelStorage}: 전이커널·저장소 ({@code IndexedKernelStore.CacheMetrics}).
   *   <li>{@code preparationGroups}: 준비·그룹목록 ({@code int}).
   *   <li>{@code preparationEntries}: 준비·항목목록 ({@code long}).
   *   <li>{@code sharedMethods}: 공유·제작방법목록 ({@code long}).
   *   <li>{@code freezeTransientBytes}: 불변복사·임시·바이트수 ({@code long}).
   *   <li>{@code lawStorage}: 분포·저장소 ({@code CertifiedLawStore.Metrics}).
   *   <li>{@code absorptionStorage}: 흡수·저장소 ({@code CertifiedMutableLawStore.Metrics}).
   *   <li>{@code workingStorage}: 계산중·저장소 ({@code TemporaryWorkingLaws.Metrics}).
   *   <li>{@code classStorage}: 동치류·저장소 ({@code TemporaryClassIndex.Metrics}).
   *   <li>{@code additionProfiles}: 추가·특성목록 ({@code BasicTransitionClasses.ProfileMetrics}).
   *   <li>{@code largestMethods}: largest·제작방법목록 ({@code List<MethodRetention>}).
   * </ul>
   */
  record Retention(
      int classes,
      int states,
      int progressKeys,
      long adjacencyEdges,
      int queuedMethods,
      int activeRunners,
      long pending,
      long boundary,
      long exits,
      long flightOutcomes,
      long savedAccounting,
      long uniqueSavedEntries,
      int cachedKernels,
      int cachedOutcomes,
      int pendingKernels,
      long calculatedKernels,
      long sharedRequests,
      IndexedKernelStore.CacheMetrics kernelStorage,
      int preparationGroups,
      long preparationEntries,
      long sharedMethods,
      long freezeTransientBytes,
      CertifiedLawStore.Metrics lawStorage,
      CertifiedMutableLawStore.Metrics absorptionStorage,
      TemporaryWorkingLaws.Metrics workingStorage,
      TemporaryClassIndex.Metrics classStorage,
      BasicTransitionClasses.ProfileMetrics additionProfiles,
      List<MethodRetention> largestMethods) {}

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·보유량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code long}).
   *   <li>{@code pending}: 대기 ({@code long}).
   *   <li>{@code boundary}: 경계 ({@code long}).
   *   <li>{@code exits}: 종료목록 ({@code long}).
   * </ul>
   */
  record MethodRetention(String id, long cycles, long pending, long boundary, long exits) {}

  Retention retention() {
    long pending = 0, boundary = 0, exits = 0, flights = 0;
    int active = 0;
    var largest = new ArrayList<MethodRetention>();
    Set<Map<?, ?>> unique = Collections.newSetFromMap(new IdentityHashMap<>());
    for (var method : methods.values()) {
      if (method.runner != null) {
        active++;
        var retained = method.runner.retention();
        pending += retained.pending();
        boundary += retained.boundary();
        exits += retained.exits();
        flights += retained.flightOutcomes();
        largest.add(
            new MethodRetention(
                method.id,
                method.runner.cycles(),
                retained.pending(),
                retained.boundary(),
                retained.exits()));
      }
      for (var point : method.saved.values()) {
        unique.add(point.observation().active());
        unique.addAll(point.observation().exits().values());
      }
    }
    largest.sort(
        Comparator.comparingLong((MethodRetention m) -> m.pending() + m.boundary() + m.exits())
            .reversed());
    return new Retention(
        index.size(),
        states.size(),
        progressCache.size(),
        adjacency.values().stream().mapToLong(Set::size).sum(),
        ready.size(),
        active,
        pending,
        boundary,
        exits,
        flights,
        savedEntries,
        unique.stream().mapToLong(Map::size).sum(),
        kernels.cachedKernels(),
        kernels.cachedOutcomes(),
        kernels.pendingKernels(),
        kernels.calculatedKernels(),
        kernels.sharedRequests(),
        kernels.cacheMetrics(),
        preparations.size(),
        preparationEntries,
        methods.values().stream().filter(m -> m.delegate != null).count(),
        freezeTransientBytes(),
        laws.metrics(),
        absorptionStore.metrics(),
        workingLaws.metrics(),
        classStorage.metrics(),
        index.profileMetrics(),
        List.copyOf(largest.subList(0, Math.min(12, largest.size()))));
  }

  public CertifiedInterval.Precision precision() {
    return precision;
  }

  public boolean memoryLimited() {
    kernels.trimToEntries(
        (int) Math.max(0, Math.min(Integer.MAX_VALUE, budget - workingEntries())));
    return retainedEntries() > budget || storageLimited();
  }

  public void increaseMemoryBudget(int limit) {
    if (limit < budget) throw new IllegalArgumentException("Cannot shrink budget");
    budget = limit;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    var all = new LinkedHashSet<>(assumptions);
    all.addAll(kernels.assumptions());
    return List.copyOf(all);
  }

  public void step() {
    if (complete() || memoryLimited()) return;
    operations++;
    while (!frontier.isEmpty()
        && (frontier.peek().state().expanded
            || frontier.peek().depth() != frontier.peek().state().depth)) frontier.remove();
    while (!previews.isEmpty()
        && (previews.peek().state().expanded
            || previews.peek().state().previewDiscovery
                == BasicCurrencyTransitions.supportedActions().size()
            || (previews.peek().state().previewDiscovery == 0
                && previewedPatterns.contains(
                    new PreviewPattern(previews.peek().depth(), previews.peek().state().progress)))
            || previews.peek().depth() != previews.peek().state().depth)) previews.remove();
    if (operations % 4 == 0
        && ready.size() < 64
        && !previews.isEmpty()
        && (ready.isEmpty() || previews.peek().depth() <= shallowestReadyDepth + 1)) {
      preview(previews.peek().state());
    } else if (!frontier.isEmpty()
        && (ready.isEmpty()
            || (operations % 4 == 0
                && ready.size() < 64
                && frontier.peek().depth() <= shallowestReadyDepth))) {
      discover(frontier.peek().state());
    } else if (!ready.isEmpty()) {
      var method = ready.removeFirst();
      if (diagnostics) diagnosticSteps.merge(method.phase(), 1L, Long::sum);
      long before = method.runner == null ? 0 : method.runner.retainedStates();
      method.step();
      if (method.done) graphRevision++;
      liveEntries += (method.runner == null ? 0 : method.runner.retainedStates()) - before;
      if (!method.done) ready.addLast(method);
      else if (method.source.depth == shallowestReadyDepth)
        shallowestReadyDepth =
            ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
    }
  }

  private State state(int key, int depth) {
    var found = states.get(key);
    if (found != null) {
      if (depth < found.depth) graphRevision++;
      found.depth = Math.min(found.depth, depth);
      return found;
    }
    var progress = progress(key);
    var added = new State(key, depth, progress);
    states.put(key, added);
    graphRevision++;
    if (progress.goal() == Status.MATCH) added.expanded = true;
    else {
      undiscovered++;
      frontier.add(new Expansion(added, depth));
    }
    return added;
  }

  private void link(State source, int target) {
    var linked = state(target, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source.key, unused -> new LinkedHashSet<>()).add(target);
    if (linked.depth <= source.depth + 1) return;
    linked.depth = source.depth + 1;
    graphRevision++;
    if (!linked.expanded) frontier.add(new Expansion(linked, linked.depth));
    previewCandidate(linked);
    var queue = new ArrayDeque<Integer>();
    queue.add(target);
    while (!queue.isEmpty()) {
      var from = states.get(queue.removeFirst());
      for (int next : adjacency.getOrDefault(from.key, Set.of())) {
        var to = states.get(next);
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          graphRevision++;
          if (!to.expanded) frontier.add(new Expansion(to, to.depth));
          previewCandidate(to);
          queue.addLast(next);
        }
      }
    }
  }

  private void previewCandidate(State state) {
    if (state.methods.stream().anyMatch(m -> !m.done))
      shallowestReadyDepth = Math.min(shallowestReadyDepth, state.depth);
    if (!state.expanded
        && matched(state.progress) > 0
        && !previewedPatterns.contains(new PreviewPattern(state.depth, state.progress)))
      previews.add(new Expansion(state, state.depth));
  }

  /** One-layer lookahead exposes checkpoint continuations without declaring full discovery. */
  private void preview(State source) {
    if (source.previewDiscovery == 0)
      previewedPatterns.add(new PreviewPattern(source.depth, source.progress));
    var action = BasicCurrencyTransitions.supportedActions().get(source.previewDiscovery++);
    // Replacement can retain an active law for many cycles; it remains in normal discovery.
    if (action.baseAction() == CraftingAction.CHAOS) return;
    var cursor = index.replay(source.key, index.witness(source.key), action);
    assumptions.addAll(cursor.assumptions());
    if (cursor.status() != BasicCurrencyTransitions.Status.UNAVAILABLE)
      add(source, List.of(), List.of(action));
  }

  private void discover(State source) {
    if (source.progress.goal() == Status.MATCH) {
      source.expanded = true;
      graphRevision++;
      undiscovered--;
      return;
    }
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discovery == actions.size()) {
      source.expanded = true;
      graphRevision++;
      undiscovered--;
      return;
    }
    var action = actions.get(source.discovery++);
    var cursor = index.replay(source.key, index.witness(source.key), action);
    assumptions.addAll(cursor.assumptions());
    if (cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE) return;
    add(source, List.of(), List.of(action));
    if (cursor.status() == BasicCurrencyTransitions.Status.UNSUPPORTED) return;
    for (var next : actions)
      if (cursor.mayApplyAfter(next)) {
        if (next != action) add(source, List.of(), List.of(action, next));
        add(source, List.of(action), List.of(next));
      }
  }

  private void add(State source, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
    String key = id(source.key) + ":" + initial + ">" + actions;
    if (methods.containsKey(key)) return;
    var method = new Method(source, key, initial, actions);
    methods.put(key, method);
    graphRevision++;
    source.methods.add(method);
    ready.addLast(method);
    shallowestReadyDepth = Math.min(shallowestReadyDepth, source.depth);
  }

  private MethodSearchCalculation.Progress progress(int key) {
    return progressCache.computeIfAbsent(
        key,
        value -> {
          var observations = index.key(value).observations();
          return new MethodSearchCalculation.Progress(
              observations.getFirst(), entryOffsets.stream().map(observations::get).toList());
        });
  }

  /**
   * {@code CertifiedMethodSearchEngine}에서 사용하는 제작방법·조회표현의 불변 값이다.
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
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, CertifiedMethodRunner.Observation<String,
   *       WorkbenchCurrency>>}).
   *   <li>{@code checkpoints}: 체크포인트목록 ({@code Map<Integer, Map<MethodSearchCalculation.Progress,
   *       CertifiedInterval>>}).
   *   <li>{@code branches}: 분기목록 ({@code Map<Integer, Map<String,
   *       Map<MethodSearchCalculation.Progress, CertifiedInterval>>>}).
   * </ul>
   */
  record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, CertifiedMethodRunner.Observation<String, WorkbenchCurrency>> points,
      Map<Integer, Map<MethodSearchCalculation.Progress, CertifiedInterval>> checkpoints,
      Map<Integer, Map<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>>
          branches) {}

  private Optional<Exit> classify(State source, int key, boolean structural) {
    var now = progress(key);
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    if (!now.entries().equals(source.progress.entries())) {
      for (int i = 0; i < now.entries().size(); i++)
        if (source.progress.entries().get(i) == Status.MATCH
            && now.entries().get(i) != Status.MATCH) return Optional.of(Exit.LOSS);
      return Optional.of(Exit.CHECKPOINT);
    }
    var before = index.witness(source.key).item();
    var after = index.witness(key).item();
    if (structural
        && (before.rarity() != after.rarity()
            || before.explicits().size() != after.explicits().size()))
      return Optional.of(Exit.CHECKPOINT);
    return Optional.empty();
  }

  public List<MethodSearchEngine.StateView> states() {
    return states.values().stream()
        .map(
            s ->
                new MethodSearchEngine.StateView(
                    id(s.key),
                    new DeferredItem(index.witness(s.key)),
                    s.depth,
                    s.progress,
                    s.expanded,
                    s.expanded && s.methods.stream().allMatch(m -> m.done),
                    Optional.empty()))
        .toList();
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    id(m.source.key),
                    m.initial,
                    m.actions,
                    m.done ? m.unresolved ? "UNSUPPORTED" : "COMPLETE" : "PARTIAL",
                    null,
                    Map.copyOf(m.points),
                    Map.copyOf(m.checkpoints),
                    Map.copyOf(m.branches)))
        .toList();
  }

  /** Finite completed graph adapter. Large-job cooperative strategy scheduling is separate. */
  boolean strategyFitsPreparationBudget() {
    if (!complete()) return false;
    if (states.size() > 128 || methods.size() > 1024) return false;
    for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
      long rows = 0;
      for (var method : methods.values()) {
        var point = method.points.get(cycles);
        if (point == null) throw new IllegalStateException("Completed method lacks observation");
        rows += point.active().size();
        for (var law : point.exits().values()) rows += law.size();
        if (rows > 8192) return false;
      }
    }
    return true;
  }

  CertifiedStrategyWork strategyWork() {
    if (!strategyFitsPreparationBudget())
      throw new IllegalStateException("Strategy preparation budget exceeded");
    return new CertifiedStrategyWork(
        states.keySet().stream().map(this::id).toList(),
        precision.percentDecimals(),
        this::strategy);
  }

  /** Finite completed graph adapter; callers must guard synchronous preparation. */
  CertifiedOutcomeStrategy<String> strategy(int cycles) {
    if (!complete()) throw new IllegalStateException("Enumeration incomplete");
    if (!MethodSearchCalculation.OBSERVATIONS.contains(cycles))
      throw new IllegalArgumentException("Unknown horizon");
    var graph = new LinkedHashMap<String, CertifiedOutcomeStrategy.State<String>>();
    for (var state : states.values()) {
      var choices = new ArrayList<CertifiedOutcomeStrategy.Choice<String>>();
      for (var method : state.methods) {
        var point = method.points.get(cycles);
        if (point == null) throw new IllegalStateException("Completed method lacks observation");
        var outcomes = new LinkedHashMap<>(point.active());
        var unresolved = CertifiedInterval.zero(precision.fractionalBits());
        for (var exit : point.exits().entrySet()) {
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values())
              unresolved = unresolved.add(mass).probability();
          else
            exit.getValue()
                .forEach(
                    (target, mass) ->
                        outcomes.merge(target, mass, (a, b) -> a.add(b).probability()));
        }
        choices.add(new CertifiedOutcomeStrategy.Choice<>(method.id, outcomes, unresolved));
      }
      graph.put(
          id(state.key),
          new CertifiedOutcomeStrategy.State<>(
              state.progress.goal() == Status.MATCH, false, choices));
    }
    return new CertifiedOutcomeStrategy<>(graph, precision.fractionalBits());
  }

  private Map<String, CertifiedInterval> ids(Map<Integer, CertifiedInterval> input) {
    return lawViews.computeIfAbsent(
        input,
        law -> {
          savedEntries += law.size();
          return new IdentifierLaw(law);
        });
  }

  /**
   * {@code CertifiedMethodSearchEngine} 내부의 Identifier·분포 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code law}: 분포 ({@code Map<Integer, CertifiedInterval>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code IdentifierLaw(law)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code law}(분포).
   *   <li>{@code total()}: 합계 값을 조회·구성한다. 반환: {@code CertifiedInterval}.
   *   <li>{@code page(offset, limit)}: 페이지 처리에서 {@code CertifiedLawStore.page}, {@code
   *       e.getKey}을(를) 사용한다. 입력: {@code offset}(시작위치), {@code limit}(한도). 반환: {@code
   *       List<Map.Entry<String, CertifiedInterval>>}.
   *   <li>{@code values()}: 값목록 값을 조회·구성한다. 반환: {@code Collection<CertifiedInterval>}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code int}.
   *   <li>{@code get(key)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code key}(키). 반환: {@code
   *       CertifiedInterval}.
   *   <li>{@code entrySet()}: 항목·집합 값을 조회·구성한다. 반환: {@code Set<Map.Entry<String,
   *       CertifiedInterval>>}.
   * </ul>
   */
  private final class IdentifierLaw extends AbstractMap<String, CertifiedInterval>
      implements CertifiedLawStore.PagedLaw<String> {
    private final Map<Integer, CertifiedInterval> law;

    IdentifierLaw(Map<Integer, CertifiedInterval> law) {
      this.law = law;
    }

    @Override
    public CertifiedInterval total() {
      return CertifiedLawStore.total(law, precision.fractionalBits());
    }

    @Override
    public List<Map.Entry<String, CertifiedInterval>> page(int offset, int limit) {
      return CertifiedLawStore.page(law, offset, limit).stream()
          .map(e -> Map.entry(id(e.getKey()), e.getValue()))
          .toList();
    }

    @Override
    public Collection<CertifiedInterval> values() {
      return law.values();
    }

    @Override
    public int size() {
      return law.size();
    }

    @Override
    public CertifiedInterval get(Object key) {
      if (!(key instanceof String text) || !text.startsWith("class:")) return null;
      try {
        int parsed = Integer.parseInt(text.substring(6));
        return parsed >= 0 && text.equals("class:" + parsed) ? law.get(parsed) : null;
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    @Override
    public Set<Map.Entry<String, CertifiedInterval>> entrySet() {
      return new AbstractSet<>() {
        @Override
        public int size() {
          return law.size();
        }

        @Override
        public Iterator<Map.Entry<String, CertifiedInterval>> iterator() {
          var source = law.entrySet().iterator();
          return new Iterator<>() {
            @Override
            public boolean hasNext() {
              return source.hasNext();
            }

            @Override
            public Map.Entry<String, CertifiedInterval> next() {
              var next = source.next();
              return Map.entry(id(next.getKey()), next.getValue());
            }
          };
        }
      };
    }
  }

  /**
   * {@code CertifiedMethodSearchEngine} 내부의 상태 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code MethodSearchCalculation.Progress}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Method>}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code discovery}: 발견 ({@code int}).
   *   <li>{@code previewDiscovery}: 미리보기·발견 ({@code int}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code State(key, depth, progress)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키), {@code
   *       depth}(깊이), {@code progress}(진행상황).
   * </ul>
   */
  private static final class State {
    final int key;
    final MethodSearchCalculation.Progress progress;
    final List<Method> methods = new ArrayList<>();
    int depth, discovery, previewDiscovery;
    boolean expanded;

    State(int key, int depth, MethodSearchCalculation.Progress progress) {
      this.key = key;
      this.depth = depth;
      this.progress = progress;
    }
  }

  /**
   * {@code CertifiedMethodSearchEngine} 내부의 제작방법 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code State}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code initial}: 초기값 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code saved}: 저장된 ({@code Map<Integer, IndexedCertifiedMethodRunner.Snapshot>}).
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, CertifiedMethodRunner.Observation<String,
   *       WorkbenchCurrency>>}).
   *   <li>{@code checkpoints}: 체크포인트목록 ({@code Map<Integer, Map<MethodSearchCalculation.Progress,
   *       CertifiedInterval>>}).
   *   <li>{@code branches}: 분기목록 ({@code Map<Integer, Map<String,
   *       Map<MethodSearchCalculation.Progress, CertifiedInterval>>>}).
   *   <li>{@code runner}: 실행기 ({@code IndexedCertifiedMethodRunner}).
   *   <li>{@code freezing}: freezing ({@code IndexedCertifiedMethodRunner.FrozenSnapshot}).
   *   <li>{@code publications}: publications ({@code ArrayDeque<Publication>}).
   *   <li>{@code executionDone}: 실행·완료여부 ({@code boolean}).
   *   <li>{@code preparationRequest}: 준비·요청 ({@code IndexedKernelStore.Request}).
   *   <li>{@code delegate}: 위임대상 ({@code Method}).
   *   <li>{@code equivalence}: equivalence ({@code
   *       IndexedCertifiedMethodRunner.InitialEquivalence}).
   *   <li>{@code prepared}: 준비된 ({@code boolean}).
   *   <li>{@code linkedLaws}: linked·분포목록 ({@code Set<Map<Integer, CertifiedInterval>>}).
   *   <li>{@code done}: 완료여부 ({@code boolean}).
   *   <li>{@code unresolved}: 미해결 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code template()}: 템플릿 값을 조회·구성한다. 반환: {@code Template<Integer, WorkbenchCurrency>}.
   *   <li>{@code Method(source, id, initial, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       source}(출처), {@code id}(식별자), {@code initial}(초기값), {@code actions}(행동목록).
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code phase()}: 단계 값을 조회·구성한다. 반환: {@code String}.
   *   <li>{@code save(cycles, snapshot)}: 현재 값을 저장 경계에 기록한다. 입력: {@code cycles}(주기횟수), {@code
   *       snapshot}(스냅샷).
   *   <li>{@code publish(cycles, snapshot)}: 새 결과나 체크포인트를 외부 조회에 공개한다. 입력: {@code cycles}(주기횟수),
   *       {@code snapshot}(스냅샷).
   * </ul>
   *
   * <p>중첩 타입:
   *
   * <ul>
   *   <li>{@code Publication}: {@code Method} 내부의 Publication 처리를 맡는다.
   * </ul>
   */
  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial, actions;
    final Map<Integer, IndexedCertifiedMethodRunner.Snapshot> saved = new LinkedHashMap<>();
    final Map<Integer, CertifiedMethodRunner.Observation<String, WorkbenchCurrency>> points =
        new LinkedHashMap<>();
    final Map<Integer, Map<MethodSearchCalculation.Progress, CertifiedInterval>> checkpoints =
        new LinkedHashMap<>();
    final Map<Integer, Map<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>>
        branches = new LinkedHashMap<>();
    IndexedCertifiedMethodRunner runner;
    IndexedCertifiedMethodRunner.FrozenSnapshot freezing;
    final ArrayDeque<Publication> publications = new ArrayDeque<>();
    boolean executionDone;
    IndexedKernelStore.Request preparationRequest;
    Method delegate;
    IndexedCertifiedMethodRunner.InitialEquivalence equivalence;
    boolean prepared;
    final Set<Map<Integer, CertifiedInterval>> linkedLaws =
        Collections.newSetFromMap(new IdentityHashMap<>());
    boolean done, unresolved;

    Template<Integer, WorkbenchCurrency> template() {
      return new Template<>(
          templatesVersion() + id,
          initial.stream().<Function<Integer, WorkbenchCurrency>>map(a -> s -> a).toList(),
          actions.stream().<Function<Integer, WorkbenchCurrency>>map(a -> s -> a).toList());
    }

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = initial;
      this.actions = actions;
    }

    void step() {
      if (!publications.isEmpty()) {
        if (publications.peekFirst().step()) publications.removeFirst();
        if (publications.isEmpty() && executionDone) done = true;
        return;
      }
      if (executionDone) {
        done = true;
        return;
      }
      if (!prepared && !initial.isEmpty()) {
        if (preparationRequest == null)
          preparationRequest = kernels.request(source.key, initial.getFirst());
        if (!preparationRequest.complete()) {
          preparationRequest.step();
          return;
        }
        var law = new IndexedCertifiedMethodRunner.InitialLaw(preparationRequest);
        preparationRequest = null;
        var key =
            new Preparation(
                source.progress.goal() == Status.NO_MATCH
                        && source.progress.entries().stream()
                            .allMatch(s -> s == Status.MATCH || s == Status.NO_MATCH)
                    ? -1
                    : source.key,
                source.progress,
                actions,
                law.outcomes(),
                law.unresolved(),
                law.unavailable());
        var shared = preparations.get(key);
        if (shared == null) {
          preparations.put(key, new Prepared(this, law));
          preparationEntries += law.outcomes().size();
        } else {
          delegate = shared.owner();
          equivalence = shared.law().equivalentTo(law);
        }
        prepared = true;
      }
      if (delegate != null) {
        for (int cycles : MethodSearchCalculation.OBSERVATIONS)
          if (!saved.containsKey(cycles) && delegate.saved.containsKey(cycles))
            save(cycles, delegate.saved.get(cycles).withEquivalentInitial(equivalence));
        executionDone = delegate.done;
        return;
      }
      if (runner == null)
        runner =
            new IndexedCertifiedMethodRunner(
                kernels,
                source.key,
                new Template<>(
                    templatesVersion() + id,
                    initial.stream()
                        .<Function<Integer, WorkbenchCurrency>>map(a -> s -> a)
                        .toList(),
                    actions.stream()
                        .<Function<Integer, WorkbenchCurrency>>map(a -> s -> a)
                        .toList()),
                target -> classify(source, target, initial.isEmpty() && actions.size() == 1),
                precision,
                absorptionStore,
                workingMaps);
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      if (!runner.absorbed()
          && !MethodSearchCalculation.OBSERVATIONS.contains(Math.toIntExact(runner.cycles()))) {
        runner.beginCycle();
        return;
      }
      if (freezing == null) {
        if (freezeOwner != null && freezeOwner != this) return;
        freezeOwner = this;
        freezing = runner.freezeSnapshot(laws);
        return;
      }
      if (!freezing.complete()) {
        freezing.step();
        return;
      }
      var snapshot = freezing.result();
      freezing.close();
      freezing = null;
      freezeOwner = null;
      var point = snapshot.observation();
      if (point.active().isEmpty()) {
        for (int cycles : MethodSearchCalculation.OBSERVATIONS)
          if (!saved.containsKey(cycles)) save(cycles, snapshot.atLaterAbsorbedBoundary(cycles));
        executionDone = true;
        runner.close();
        runner = null;
        return;
      }
      int cycles = Math.toIntExact(point.cycles());
      if (MethodSearchCalculation.OBSERVATIONS.contains(cycles)) save(cycles, snapshot);
      if (cycles == 500) {
        executionDone = true;
        runner.close();
        runner = null;
      } else runner.beginCycle();
    }

    String phase() {
      if (!publications.isEmpty()) return "PUBLICATION";
      if (executionDone) return "FINISH";
      if (!prepared && !initial.isEmpty()) return "PREPARATION";
      if (delegate != null) return "DELEGATE";
      if (runner == null) return "START";
      if (!runner.atBoundary()) return "TRANSITION";
      if (!runner.absorbed()
          && !MethodSearchCalculation.OBSERVATIONS.contains(Math.toIntExact(runner.cycles())))
        return "NEXT_CYCLE";
      if (freezing == null)
        return freezeOwner != null && freezeOwner != this ? "FREEZE_WAIT" : "FREEZE_START";
      return freezing.complete() ? "FREEZE_FINISH" : "FREEZE";
    }

    void save(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
      publications.addLast(new Publication(cycles, snapshot));
    }

    /**
     * {@code Method} 내부의 Publication 처리를 맡는다.
     *
     * <p>필드·상수:
     *
     * <ul>
     *   <li>{@code cycles}: 주기횟수 ({@code int}).
     *   <li>{@code snapshot}: 스냅샷 ({@code IndexedCertifiedMethodRunner.Snapshot}).
     *   <li>{@code lawsToLink}: 분포목록·대상·연결 ({@code Iterator<Map<Integer, CertifiedInterval>>}).
     *   <li>{@code targets}: 대상목록 ({@code Iterator<Map.Entry<Integer, CertifiedInterval>>}).
     *   <li>{@code currentLaw}: 현재·분포 ({@code Map<Integer, CertifiedInterval>}).
     *   <li>{@code totals}: totals ({@code Map<MethodSearchCalculation.Progress,
     *       CertifiedInterval>}).
     *   <li>{@code linkLaw}: 연결·분포 ({@code boolean}).
     * </ul>
     *
     * <p>생성자·메서드:
     *
     * <ul>
     *   <li>{@code Publication(cycles, snapshot)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code cycles}(주기횟수),
     *       {@code snapshot}(스냅샷).
     *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 반환: {@code boolean}.
     * </ul>
     */
    private final class Publication {
      final int cycles;
      final IndexedCertifiedMethodRunner.Snapshot snapshot;
      final Iterator<Map<Integer, CertifiedInterval>> lawsToLink;
      Iterator<Map.Entry<Integer, CertifiedInterval>> targets = Collections.emptyIterator();
      Map<Integer, CertifiedInterval> currentLaw;
      Map<MethodSearchCalculation.Progress, CertifiedInterval> totals;
      boolean linkLaw;

      Publication(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
        this.cycles = cycles;
        this.snapshot = snapshot;
        var laws = new ArrayList<>(snapshot.observation().exits().values());
        laws.add(snapshot.observation().active());
        lawsToLink = laws.iterator();
      }

      boolean step() {
        int used = 0;
        while (used < 256) {
          if (targets.hasNext()) {
            var target = targets.next();
            if (linkLaw) link(source, target.getKey());
            if (totals != null)
              totals.merge(
                  progress(target.getKey()), target.getValue(), (a, b) -> a.add(b).probability());
            used++;
            continue;
          }
          if (currentLaw != null && totals != null) {
            if (!progressLaws.containsKey(currentLaw)) {
              progressLaws.put(currentLaw, Map.copyOf(totals));
              progressLawEntries += totals.size() + 1;
            }
            totals = null;
          }
          if (!lawsToLink.hasNext()) {
            publish(cycles, snapshot);
            return true;
          }
          currentLaw = lawsToLink.next();
          linkLaw = linkedLaws.add(currentLaw);
          totals = progressLaws.containsKey(currentLaw) ? null : new LinkedHashMap<>();
          if (linkLaw || totals != null) targets = currentLaw.entrySet().iterator();
        }
        return false;
      }
    }

    void publish(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
      graphRevision++;
      saved.put(cycles, snapshot);
      var point = snapshot.observation();
      var targets = new LinkedHashMap<MethodSearchCalculation.Progress, CertifiedInterval>();
      for (var kind : List.of(Exit.GOAL, Exit.CHECKPOINT, Exit.LOSS))
        progressLaws
            .get(point.exits().get(kind))
            .forEach(
                (target, mass) -> targets.merge(target, mass, (a, b) -> a.add(b).probability()));
      checkpoints.put(cycles, Map.copyOf(targets));
      targetSummaryEntries += targets.size() + 1;
      var exitsByProgress =
          new LinkedHashMap<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>();
      point.exits().forEach((kind, law) -> exitsByProgress.put(kind.name(), progressLaws.get(law)));
      exitsByProgress.put("ACTIVE", progressLaws.get(point.active()));
      branches.put(cycles, Map.copyOf(exitsByProgress));
      targetSummaryEntries += exitsByProgress.size() + 1;
      var exits = new EnumMap<Exit, Map<String, CertifiedInterval>>(Exit.class);
      point
          .exits()
          .forEach(
              (kind, law) -> {
                exits.put(kind, ids(law));
              });
      unresolved |= !point.exits().get(Exit.UNRESOLVED).isEmpty();
      points.put(
          cycles,
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              Collections.unmodifiableMap(exits),
              ids(point.active()),
              point.initialUses(),
              point.cycleUses(),
              point.mass()));
    }
  }
}
