package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Evaluation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.DeferredMixtures;
import com.poe2craft.craftingsupport.search.domain.DeferredTransitions;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.craftingsupport.search.domain.OutcomeStrategy;
import com.poe2craft.craftingsupport.search.domain.RenewalMethodObservation;
import com.poe2craft.craftingsupport.search.domain.RollPartition;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 작업 하나의 방법 깊이 탐색을 분할 실행하고 재개한다.
 *
 * <p>기존 계약: Job-local, resumable method-depth exploration. UI buckets never key calculation states.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "bounded-phase-kernel-method-search-v16"} ({@code String}).
 *   <li>{@code OPEN_METHOD_WINDOW}: 시간창; 초기값 {@code 512} ({@code int}).
 *   <li>{@code TEMPLATES}: 템플릿목록; 초기값 {@code "applicable-single-and-two-action-cycles-v3"} ({@code
 *       String}).
 *   <li>{@code OBSERVATIONS}: 관측값목록 ({@code List<Integer>}).
 *   <li>{@code transitions}: 전이목록 ({@code DeferredTransitions}).
 *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, Progress>}).
 *   <li>{@code compact}: 압축여부 ({@code Function<Map<DeferredItem, Fraction>,
 *       ExactMethodRunner.Compaction<DeferredItem>>}).
 *   <li>{@code mixtures}: mixtures ({@code DeferredMixtures}).
 *   <li>{@code states}: 상태목록 ({@code LinkedHashMap<DeferredItem, State>}).
 *   <li>{@code identities}: 식별자목록 ({@code Map<String, DeferredItem>}).
 *   <li>{@code methods}: 제작방법목록 ({@code LinkedHashMap<String, Method>}).
 *   <li>{@code runnable}: runnable ({@code ArrayDeque<Ready>}).
 *   <li>{@code preferredMethods}: preferred·제작방법목록 ({@code PriorityQueue<Ready>}).
 *   <li>{@code runnableCount}: runnable·개수 ({@code int}).
 *   <li>{@code methodRound}: 제작방법·반올림 ({@code int}).
 *   <li>{@code readySequence}: 준비완료·순서 ({@code long}).
 *   <li>{@code waiting}: waiting ({@code Map<Key, List<Method>>}).
 *   <li>{@code fairKernels}: fair·전이커널목록 ({@code ArrayDeque<Key>}).
 *   <li>{@code frontier}: 미탐색경계 ({@code PriorityQueue<Expansion>}).
 *   <li>{@code adjacency}: 인접목록 ({@code Map<State, Set<State>>}).
 *   <li>{@code kernels}: 전이커널목록 ({@code Map<Key, Kernel<DeferredItem>>}).
 *   <li>{@code completedKernels}: 완료·전이커널목록 ({@code LinkedHashMap<Key, Integer>}).
 *   <li>{@code derivedKernels}: derived·전이커널목록 ({@code Set<Key>}).
 *   <li>{@code dependencyPins}: 의존값·Pins ({@code Map<Key, Integer>}).
 *   <li>{@code kernelDeliveries}: 전이커널·Deliveries ({@code Map<Key, Set<Method>>}).
 *   <li>{@code executingMethod}: executing·제작방법 ({@code Method}).
 *   <li>{@code derivedKernelEvictions}: derived·전이커널·캐시제거수 ({@code long}).
 *   <li>{@code primitiveKernelEvictions}: 개별행동·전이커널·캐시제거수 ({@code long}).
 *   <li>{@code kernelAliases}: 전이커널·Aliases ({@code Map<Key, Key>}).
 *   <li>{@code aliasesByKernel}: aliases·기준·전이커널 ({@code Map<Key, Set<Key>>}).
 *   <li>{@code kernelDepth}: 전이커널·깊이 ({@code Map<Key, Integer>}).
 *   <li>{@code kernelMethodLength}: 전이커널·제작방법·길이 ({@code Map<Key, Integer>}).
 *   <li>{@code kernelIdentities}: 전이커널·식별자목록 ({@code Map<DeferredTransitions.Identity, Key>}).
 *   <li>{@code identityByKernel}: 정체성·기준·전이커널 ({@code Map<Key, DeferredTransitions.Identity>}).
 *   <li>{@code sharedKernelLookups}: 공유·전이커널·Lookups ({@code long}).
 *   <li>{@code pendingKernels}: 대기·전이커널목록 ({@code LinkedHashMap<Key, KernelWork>}).
 *   <li>{@code progress}: 진행상황 ({@code Map<ItemState, Progress>}).
 *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
 *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
 *   <li>{@code retainedOutcomeLimit}: 보유·결과·한도 ({@code int}).
 *   <li>{@code openMethodWindow}: 열린·제작방법·시간창 ({@code int}).
 *   <li>{@code openMethods}: 열린·제작방법목록 ({@code int}).
 *   <li>{@code undiscoveredStates}: undiscovered·상태목록 ({@code int}).
 *   <li>{@code operations}: 연산목록 ({@code long}).
 *   <li>{@code kernelRound}: 전이커널·반올림 ({@code int}).
 *   <li>{@code retainedOutcomes}: 보유·결과목록 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code MethodSearchCalculation(root, evaluator, transitions, activeOmens,
 *       retainedOutcomeLimit)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code
 *       evaluator}(평가기), {@code transitions}(전이목록), {@code activeOmens}(활성·징조목록), {@code
 *       retainedOutcomeLimit}(보유·결과·한도).
 *   <li>{@code MethodSearchCalculation(root, evaluator, transitions, activeOmens,
 *       retainedOutcomeLimit, partition)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code
 *       evaluator}(평가기), {@code transitions}(전이목록), {@code activeOmens}(활성·징조목록), {@code
 *       retainedOutcomeLimit}(보유·결과·한도), {@code partition}(분할).
 *   <li>{@code MethodSearchCalculation(root, evaluator, transitions, activeOmens,
 *       retainedOutcomeLimit, partition, mixtures)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       root}(시작점), {@code evaluator}(평가기), {@code transitions}(전이목록), {@code
 *       activeOmens}(활성·징조목록), {@code retainedOutcomeLimit}(보유·결과·한도), {@code partition}(분할),
 *       {@code mixtures}(mixtures).
 *   <li>{@code MethodSearchCalculation(root, evaluate, transitions, activeOmens,
 *       retainedOutcomeLimit, direct)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code
 *       evaluate}(평가), {@code transitions}(전이목록), {@code activeOmens}(활성·징조목록), {@code
 *       retainedOutcomeLimit}(보유·결과·한도), {@code direct}(직접).
 *   <li>{@code MethodSearchCalculation(root, evaluate, transitions, activeOmens,
 *       retainedOutcomeLimit, direct, openMethodWindow)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       root}(시작점), {@code evaluate}(평가), {@code transitions}(전이목록), {@code activeOmens}(활성·징조목록),
 *       {@code retainedOutcomeLimit}(보유·결과·한도), {@code direct}(직접), {@code
 *       openMethodWindow}(열린·제작방법·시간창).
 *   <li>{@code MethodSearchCalculation(root, evaluate, transitions, activeOmens,
 *       retainedOutcomeLimit, direct, openMethodWindow, partition)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code root}(시작점), {@code evaluate}(평가), {@code transitions}(전이목록), {@code
 *       activeOmens}(활성·징조목록), {@code retainedOutcomeLimit}(보유·결과·한도), {@code direct}(직접), {@code
 *       openMethodWindow}(열린·제작방법·시간창), {@code partition}(분할). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code operations()}: {@code operations} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code mixtureMetrics()}: 혼합·측정값 값을 조회·구성한다. 반환: {@code DeferredMixtures.Metrics}.
 *   <li>{@code memoryLimited()}: 메모리·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code increaseMemoryBudget(limit)}: 메모리·계산예산을(를) 한도를 늘린다. 입력: {@code limit}(한도). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code trimKernels()}: trim·전이커널목록 처리에서 {@code derivedKernels.size}, {@code
 *       completedKernels.entrySet}을(를) 사용한다.
 *   <li>{@code alias(request, canonical)}: alias 처리에서 {@code kernelAliases.put}, {@code
 *       aliasesByKernel.computeIfAbsent}을(를) 사용한다. 입력: {@code request}(요청), {@code canonical}(정규형).
 *   <li>{@code finishKernel(key, value, ownedOutcomes, derived)}: 완료·전이커널 처리에서 {@code kernels.put},
 *       {@code completedKernels.put}을(를) 사용한다. 입력: {@code key}(키), {@code value}(값), {@code
 *       ownedOutcomes}(owned·결과목록), {@code derived}(derived).
 *   <li>{@code registerKernel(request, depth, methodLength)}: 등록여부·전이커널 처리에서 {@code
 *       kernelAliases.get}, {@code kernelDepth.merge}을(를) 사용한다. 입력: {@code request}(요청), {@code
 *       depth}(깊이), {@code methodLength}(제작방법·길이). 반환: {@code Key}.
 *   <li>{@code derivedKernelEvictions()}: {@code derivedKernelEvictions} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code primitiveKernelEvictions()}: {@code primitiveKernelEvictions} 값을 조회한다. 반환: {@code
 *       long}.
 *   <li>{@code cacheMetrics()}: 캐시·측정값 값을 조회·구성한다. 반환: {@code CacheMetrics}.
 *   <li>{@code matches(state)}: 일치결과의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태). 반환: {@code int}.
 *   <li>{@code wake(key)}: wake 처리에서 {@code waiting.remove}, {@code ready.forEach}을(를) 사용한다. 입력:
 *       {@code key}(키).
 *   <li>{@code queue(method)}: 대기열 처리에서 {@code runnable.addLast}, {@code preferredMethods.add}을(를)
 *       사용한다. 입력: {@code method}(제작방법). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code state(value, depth)}: 현재 상태 표현을 조회한다. 입력: {@code value}(값), {@code depth}(깊이). 반환:
 *       {@code State}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code enqueue(state)}: enqueue 처리에서 {@code frontier.add}을(를) 사용한다. 입력: {@code state}(상태).
 *   <li>{@code link(source, value)}: 연결 처리에서 {@code states.get}, {@code
 *       adjacency.computeIfAbsent}을(를) 사용한다. 입력: {@code source}(출처), {@code value}(값).
 *   <li>{@code progress(state)}: 진행상황 처리에서 {@code progress.computeIfAbsent}, {@code state.item}을(를)
 *       사용한다. 입력: {@code state}(상태). 반환: {@code Progress}.
 *   <li>{@code finished(state)}: 종료 처리에서 {@code methods.stream}, {@code methods.get}을(를) 사용한다. 입력:
 *       {@code state}(상태). 반환: {@code boolean}.
 *   <li>{@code discover(source)}: discover 처리에서 {@code BasicCurrencyTransitions.supportedActions},
 *       {@code actions.size}을(를) 사용한다. 입력: {@code source}(출처).
 *   <li>{@code add(source, actions)}: 값 또는 항목을 더한다. 입력: {@code source}(출처), {@code actions}(행동목록).
 *   <li>{@code add(source, initial, actions)}: 값 또는 항목을 더한다. 입력: {@code source}(출처), {@code
 *       initial}(초기값), {@code actions}(행동목록).
 *   <li>{@code classify(source, outcome, structuralCheckpoint)}: 완전한 전이 특성을 기준으로 상태의 동치류를 판정한다. 입력:
 *       {@code source}(출처), {@code outcome}(결과), {@code structuralCheckpoint}(구조적·체크포인트). 반환:
 *       {@code Optional<Exit>}.
 *   <li>{@code kernel(state, action)}: 전이커널 처리에서 {@code kernelAliases.getOrDefault}, {@code
 *       kernels.get}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code
 *       Kernel<DeferredItem>}. 사용하는 예외: {@code PendingKernel}.
 *   <li>{@code states()}: 상태목록 값을 조회·구성한다. 반환: {@code List<StateView>}.
 *   <li>{@code methods()}: 제작방법목록 값을 조회·구성한다. 반환: {@code List<MethodView>}.
 *   <li>{@code strategy(cycles)}: 전략 처리에서 {@code states.values}, {@code methods.get}을(를) 사용한다. 입력:
 *       {@code cycles}(주기횟수). 반환: {@code OutcomeStrategy<DeferredItem>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Progress}: {@code MethodSearchCalculation}에서 사용하는 진행상황의 불변 값이다.
 *   <li>{@code StateView}: {@code MethodSearchCalculation}에서 사용하는 상태·조회표현의 불변 값이다.
 *   <li>{@code MethodView}: {@code MethodSearchCalculation}에서 사용하는 제작방법·조회표현의 불변 값이다.
 *   <li>{@code Key}: {@code MethodSearchCalculation}에서 사용하는 키의 불변 값이다.
 *   <li>{@code PendingKernel}: {@code MethodSearchCalculation} 내부의 대기·전이커널 처리를 맡는다.
 *   <li>{@code Ready}: {@code MethodSearchCalculation}에서 사용하는 준비완료의 불변 값이다.
 *   <li>{@code Expansion}: {@code MethodSearchCalculation}에서 사용하는 확장의 불변 값이다.
 *   <li>{@code CacheMetrics}: {@code MethodSearchCalculation}에서 사용하는 캐시·측정값의 불변 값이다.
 *   <li>{@code State}: {@code MethodSearchCalculation} 내부의 상태 처리를 맡는다.
 *   <li>{@code Method}: {@code MethodSearchCalculation} 내부의 제작방법 처리를 맡는다.
 *   <li>{@code KernelWork}: {@code MethodSearchCalculation} 내부의 전이커널·계산작업 처리를 맡는다.
 *   <li>{@code MixtureKernelWork}: {@code MethodSearchCalculation} 내부의 혼합·전이커널·계산작업 처리를 맡는다.
 * </ul>
 */
public final class MethodSearchCalculation {
  public static final String VERSION = "bounded-phase-kernel-method-search-v16";
  static final int OPEN_METHOD_WINDOW = 512;
  public static final String TEMPLATES = "applicable-single-and-two-action-cycles-v3";
  public static final List<Integer> OBSERVATIONS = List.of(100, 300, 500);

  /**
   * {@code MethodSearchCalculation}에서 사용하는 진행상황의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code goal}: 목표 ({@code Status}).
   *   <li>{@code entries}: 항목목록 ({@code List<Status>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Progress(goal, entries)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code goal}(목표), {@code
   *       entries}(항목목록).
   * </ul>
   */
  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  /**
   * {@code MethodSearchCalculation}에서 사용하는 상태·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code DeferredItem}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code Progress}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   * </ul>
   */
  public record StateView(
      String id,
      DeferredItem state,
      int depth,
      Progress progress,
      boolean expanded,
      boolean complete) {}

  /**
   * {@code MethodSearchCalculation}에서 사용하는 제작방법·조회표현의 불변 값이다.
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
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, Observation<DeferredItem,
   *       WorkbenchCurrency>>}).
   * </ul>
   */
  public record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, Observation<DeferredItem, WorkbenchCurrency>> points) {}

  /**
   * {@code MethodSearchCalculation}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code DeferredItem}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   * </ul>
   */
  private record Key(DeferredItem state, WorkbenchCurrency action) {}

  /**
   * {@code MethodSearchCalculation} 내부의 대기·전이커널 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code Key}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code PendingKernel(key)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키).
   * </ul>
   */
  private static final class PendingKernel extends RuntimeException {
    final Key key;

    PendingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final DeferredTransitions transitions;
  private final Function<ItemState, Progress> evaluate;
  private Function<Map<DeferredItem, Fraction>, ExactMethodRunner.Compaction<DeferredItem>> compact;
  private DeferredMixtures mixtures;
  private final LinkedHashMap<DeferredItem, State> states = new LinkedHashMap<>();
  private final Map<String, DeferredItem> identities = new HashMap<>();
  private final LinkedHashMap<String, Method> methods = new LinkedHashMap<>();

  /**
   * {@code MethodSearchCalculation}에서 사용하는 준비완료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code method}: 제작방법 ({@code Method}).
   *   <li>{@code ticket}: ticket ({@code long}).
   * </ul>
   */
  private record Ready(Method method, long ticket) {}

  private final ArrayDeque<Ready> runnable = new ArrayDeque<>();
  private final PriorityQueue<Ready> preferredMethods =
      new PriorityQueue<>(
          Comparator.comparingInt((Ready ready) -> ready.method().source.depth)
              .thenComparingInt(ready -> -ready.method().source.matchingEntries)
              .thenComparingInt(
                  ready -> ready.method().initial.size() + ready.method().actions.size())
              .thenComparingLong(Ready::ticket));
  private int runnableCount;
  private int methodRound;
  private long readySequence;
  private final Map<Key, List<Method>> waiting = new HashMap<>();
  private final ArrayDeque<Key> fairKernels = new ArrayDeque<>();

  /**
   * {@code MethodSearchCalculation}에서 사용하는 확장의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code State}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code matches}: 일치결과 ({@code int}).
   * </ul>
   */
  private record Expansion(State state, int depth, int matches) {}

  private final PriorityQueue<Expansion> frontier =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth)
              .thenComparing(Comparator.comparingInt(Expansion::matches).reversed())
              .thenComparing(e -> e.state().id));
  private final Map<State, Set<State>> adjacency = new HashMap<>();
  private final Map<Key, Kernel<DeferredItem>> kernels = new HashMap<>();
  private final LinkedHashMap<Key, Integer> completedKernels = new LinkedHashMap<>(16, .75f, true);
  private final Set<Key> derivedKernels = new HashSet<>();
  private final Map<Key, Integer> dependencyPins = new HashMap<>();
  private final Map<Key, Set<Method>> kernelDeliveries = new HashMap<>();
  private Method executingMethod;
  private long derivedKernelEvictions;
  private long primitiveKernelEvictions;
  private final Map<Key, Key> kernelAliases = new HashMap<>();
  private final Map<Key, Set<Key>> aliasesByKernel = new HashMap<>();
  private final Map<Key, Integer> kernelDepth = new HashMap<>();
  private final Map<Key, Integer> kernelMethodLength = new HashMap<>();
  private final Map<DeferredTransitions.Identity, Key> kernelIdentities = new HashMap<>();
  private final Map<Key, DeferredTransitions.Identity> identityByKernel = new HashMap<>();
  private long sharedKernelLookups;
  private final LinkedHashMap<Key, KernelWork> pendingKernels = new LinkedHashMap<>();
  private final Map<ItemState, Progress> progress = new HashMap<>();
  private final Set<String> activeOmens;
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  private int retainedOutcomeLimit;
  private final int openMethodWindow;
  private int openMethods;
  private int undiscoveredStates;
  private long operations;
  private int kernelRound;
  private int retainedOutcomes;

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit) {
    this(root, evaluator, transitions, activeOmens, retainedOutcomeLimit, RollPartition.exact());
  }

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      RollPartition partition) {
    this(
        root,
        item -> {
          var result = evaluator.apply(item);
          return new Progress(
              result.status(),
              result.groups().stream()
                  .flatMap(group -> group.entries().stream())
                  .map(entry -> entry.status())
                  .toList());
        },
        transitions,
        activeOmens,
        retainedOutcomeLimit,
        true,
        OPEN_METHOD_WINDOW,
        partition);
  }

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      RollPartition partition,
      DeferredMixtures mixtures) {
    this(root, evaluator, transitions, activeOmens, retainedOutcomeLimit, partition);
    if (activeOmens.isEmpty()) {
      this.mixtures = mixtures;
      compact = mixtures::start;
    }
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct) {
    this(
        root, evaluate, transitions, activeOmens, retainedOutcomeLimit, direct, OPEN_METHOD_WINDOW);
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct,
      int openMethodWindow) {
    this(
        root,
        evaluate,
        transitions,
        activeOmens,
        retainedOutcomeLimit,
        direct,
        openMethodWindow,
        RollPartition.exact());
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct,
      int openMethodWindow,
      RollPartition partition) {
    this.evaluate = evaluate;
    this.transitions = new DeferredTransitions(transitions, partition);
    this.activeOmens = Set.copyOf(activeOmens);
    if (retainedOutcomeLimit < 1)
      throw new IllegalArgumentException("Positive memory budget required");
    this.retainedOutcomeLimit = retainedOutcomeLimit;
    if (openMethodWindow < 1) throw new IllegalArgumentException("Positive work window required");
    this.openMethodWindow = openMethodWindow;
    state(new DeferredItem(root), 0);
  }

  public long operations() {
    return operations;
  }

  public DeferredMixtures.Metrics mixtureMetrics() {
    return mixtures == null ? new DeferredMixtures.Metrics(0, 0, 0, 0) : mixtures.metrics();
  }

  public boolean memoryLimited() {
    return retainedOutcomes >= retainedOutcomeLimit;
  }

  public boolean complete() {
    return openMethods == 0
        && undiscoveredStates == 0
        && pendingKernels.isEmpty()
        && runnableCount == 0
        && waiting.isEmpty();
  }

  public void increaseMemoryBudget(int limit) {
    if (limit < retainedOutcomeLimit) throw new IllegalArgumentException("Budget cannot shrink");
    retainedOutcomeLimit = limit;
  }

  /** A primitive cursor emits at most one outcome per kernel turn; methods alternate fairly. */
  public void step() {
    if (memoryLimited() || complete()) return;
    operations++;
    while (!frontier.isEmpty()
        && (frontier.peek().state().expanded
            || frontier.peek().depth() != frontier.peek().state().depth)) frontier.remove();
    // Backpressure postpones frontier discovery; it never discards states/actions or marks
    // them complete. At most one action's finite template batch can overshoot this window.
    if (!frontier.isEmpty()
        && openMethods < openMethodWindow
        && (methods.isEmpty() || openMethods == 0 || operations % 3 == 1)) {
      discover(frontier.peek().state());
      return;
    }
    // Alternating queues prevents one expensive kernel from starving cheap method observations.
    if (!pendingKernels.isEmpty() && (runnableCount == 0 || operations % 2 == 0)) {
      if (Math.floorMod(kernelRound++, 4) == 3) {
        Key key;
        do {
          key = fairKernels.removeFirst();
        } while (!pendingKernels.containsKey(key));
        pendingKernels.get(key).step();
        if (pendingKernels.containsKey(key)) fairKernels.addLast(key);
      } else {
        var selected =
            pendingKernels.values().stream()
                .filter(KernelWork::ready)
                .min(
                    Comparator.comparingInt((KernelWork work) -> kernelDepth.get(work.key))
                        .thenComparingInt(work -> kernelMethodLength.get(work.key))
                        .thenComparing(KernelWork::remainingWork)
                        .thenComparingInt(work -> -matches(work.key.state())))
                .orElseThrow();
        selected.step();
      }
      return;
    }
    if (runnableCount == 0) return;
    boolean fair = Math.floorMod(methodRound++, 4) == 3;
    Ready ready;
    do {
      ready = fair ? runnable.removeFirst() : preferredMethods.remove();
    } while (!ready.method().queued || ready.ticket() != ready.method().ticket);
    var selected = ready.method();
    selected.queued = false;
    runnableCount--;
    try {
      executingMethod = selected;
      selected.step();
      if (!selected.done) queue(selected);
    } catch (PendingKernel pending) {
      var key =
          registerKernel(
              pending.key,
              selected.source.depth,
              selected.initial.size() + selected.actions.size());
      if (kernels.containsKey(key)) {
        kernelDeliveries.computeIfAbsent(key, unused -> new HashSet<>()).add(selected);
        completedKernels.get(key);
        queue(selected);
      } else waiting.computeIfAbsent(key, unused -> new ArrayList<>()).add(selected);
    } finally {
      executingMethod = null;
      trimKernels();
    }
  }

  private void trimKernels() {
    int target = retainedOutcomeLimit - retainedOutcomeLimit / 4;
    if (derivedKernels.size() <= openMethodWindow && retainedOutcomes < target) return;
    var entries = completedKernels.entrySet().iterator();
    while ((derivedKernels.size() > openMethodWindow || retainedOutcomes >= target)
        && entries.hasNext()) {
      var entry = entries.next();
      var key = entry.getKey();
      if (kernelDeliveries.containsKey(key) || dependencyPins.containsKey(key)) continue;
      if (retainedOutcomes < target && !derivedKernels.contains(key)) continue;
      retainedOutcomes -= entry.getValue();
      entries.remove();
      kernels.remove(key);
      var aliases = aliasesByKernel.remove(key);
      if (aliases != null) aliases.forEach(kernelAliases::remove);
      var identity = identityByKernel.remove(key);
      if (identity != null) kernelIdentities.remove(identity);
      kernelDepth.remove(key);
      kernelMethodLength.remove(key);
      if (derivedKernels.remove(key)) derivedKernelEvictions++;
      else primitiveKernelEvictions++;
    }
  }

  private void alias(Key request, Key canonical) {
    kernelAliases.put(request, canonical);
    aliasesByKernel.computeIfAbsent(canonical, ignored -> new HashSet<>()).add(request);
  }

  private void finishKernel(
      Key key, Kernel<DeferredItem> value, int ownedOutcomes, boolean derived) {
    kernels.put(key, value);
    completedKernels.put(key, ownedOutcomes);
    if (derived) derivedKernels.add(key);
    var consumers = waiting.get(key);
    if (consumers != null && !consumers.isEmpty())
      kernelDeliveries.put(key, new HashSet<>(consumers));
    pendingKernels.remove(key);
    fairKernels.remove(key);
    wake(key);
    trimKernels();
  }

  private Key registerKernel(Key request, int depth, int methodLength) {
    var known = kernelAliases.get(request);
    if (known != null) {
      kernelDepth.merge(known, depth, Math::min);
      kernelMethodLength.merge(known, methodLength, Math::min);
      return known;
    }
    if (mixtures != null) {
      var basis = mixtures.expand(Map.of(request.state(), Fraction.ONE));
      if (!basis.equals(Map.of(request.state(), Fraction.ONE))) {
        var dependencies = new LinkedHashMap<Key, Fraction>();
        basis.forEach(
            (state, mass) ->
                dependencies.merge(
                    registerKernel(new Key(state, request.action()), depth, methodLength),
                    mass,
                    Fraction::add));
        dependencies.keySet().forEach(key -> dependencyPins.merge(key, 1, Integer::sum));
        alias(request, request);
        kernelDepth.put(request, depth);
        kernelMethodLength.put(request, methodLength);
        fairKernels.addLast(request);
        pendingKernels.put(request, new MixtureKernelWork(request, dependencies));
        return request;
      }
    }
    var cursor = transitions.primitiveCursor(request.state(), request.action(), activeOmens);
    assumptions.addAll(cursor.assumptions());
    var identity = cursor.identity();
    var canonical = identity.map(kernelIdentities::get).orElse(null);
    if (canonical != null) {
      kernelDepth.merge(canonical, depth, Math::min);
      kernelMethodLength.merge(canonical, methodLength, Math::min);
      alias(request, canonical);
      sharedKernelLookups++;
      return canonical;
    }
    alias(request, request);
    kernelDepth.put(request, depth);
    kernelMethodLength.put(request, methodLength);
    identity.ifPresent(
        value -> {
          kernelIdentities.put(value, request);
          identityByKernel.put(request, value);
        });
    fairKernels.addLast(request);
    pendingKernels.put(request, new KernelWork(request, cursor));
    return request;
  }

  /**
   * {@code MethodSearchCalculation}에서 사용하는 캐시·측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sourceActions}: 출처·행동목록 ({@code int}).
   *   <li>{@code distinctKernels}: 중복없는·전이커널목록 ({@code int}).
   *   <li>{@code sharedLookups}: 공유·Lookups ({@code long}).
   *   <li>{@code outcomes}: 결과목록 ({@code int}).
   *   <li>{@code methodInstances}: 제작방법·Instances ({@code int}).
   *   <li>{@code liveRunners}: 현재유효·Runners ({@code long}).
   *   <li>{@code completedMethods}: 완료·제작방법목록 ({@code int}).
   *   <li>{@code groupedExecutionInputs}: 묶인·실행·입력목록 ({@code long}).
   * </ul>
   */
  public record CacheMetrics(
      int sourceActions,
      int distinctKernels,
      long sharedLookups,
      int outcomes,
      int methodInstances,
      long liveRunners,
      int completedMethods,
      long groupedExecutionInputs) {}

  public long derivedKernelEvictions() {
    return derivedKernelEvictions;
  }

  public long primitiveKernelEvictions() {
    return primitiveKernelEvictions;
  }

  public CacheMetrics cacheMetrics() {
    return new CacheMetrics(
        kernelAliases.size(),
        kernels.size() + pendingKernels.size(),
        sharedKernelLookups,
        retainedOutcomes,
        methods.size(),
        methods.values().stream().filter(m -> m.runner != null).count(),
        methods.size() - openMethods,
        methods.values().stream()
            .mapToLong(m -> m.foldedInputs + (m.runner == null ? 0 : m.runner.groupedInputs()))
            .sum());
  }

  private int matches(DeferredItem state) {
    return (int) progress(state).entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private void wake(Key key) {
    var ready = waiting.remove(key);
    if (ready != null) ready.forEach(this::queue);
  }

  private void queue(Method method) {
    if (method.queued || method.done)
      throw new IllegalStateException("Method queued twice or after completion");
    method.queued = true;
    method.ticket = ++readySequence;
    var ready = new Ready(method, method.ticket);
    runnable.addLast(ready);
    preferredMethods.add(ready);
    runnableCount++;
  }

  private State state(DeferredItem value, int depth) {
    var old = states.get(value);
    if (old != null) {
      if (depth < old.depth) {
        old.depth = depth;
        enqueue(old);
      }
      return old;
    }
    var id = value.canonicalKey();
    var collision = identities.putIfAbsent(id, value);
    if (collision != null && !collision.equals(value))
      throw new IllegalStateException("State digest collision");
    var result = new State(value, id, depth);
    states.put(value, result);
    enqueue(result);
    undiscoveredStates++;
    return result;
  }

  private void enqueue(State state) {
    frontier.add(new Expansion(state, state.depth, matches(state.value)));
  }

  private void link(State source, DeferredItem value) {
    var target = states.get(value);
    if (target == null) target = state(value, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source, unused -> new LinkedHashSet<>()).add(target);
    var queue = new ArrayDeque<State>();
    if (target.depth > source.depth + 1) {
      target.depth = source.depth + 1;
      enqueue(target);
      queue.add(target);
    }
    while (!queue.isEmpty()) {
      var from = queue.removeFirst();
      for (var to : adjacency.getOrDefault(from, Set.of()))
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          enqueue(to);
          queue.add(to);
        }
    }
  }

  private Progress progress(DeferredItem state) {
    return progress.computeIfAbsent(state.item(), evaluate);
  }

  private boolean finished(State state) {
    return state.methods.stream().allMatch(id -> methods.get(id).done);
  }

  private void discover(State source) {
    if (progress(source.value).goal() == Status.MATCH) {
      source.expanded = true;
      undiscoveredStates--;
      return;
    }
    // Applicability comes from the shared rule engine, not a fixed six-policy catalog.
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discoveryIndex < actions.size()) {
      var first = actions.get(source.discoveryIndex++);
      var cursor = transitions.primitiveCursor(source.value, first, activeOmens);
      if (cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE) return;
      add(source, List.of(first));
      if (cursor.status() == BasicCurrencyTransitions.Status.UNSUPPORTED) return;
      for (var second : BasicCurrencyTransitions.supportedActions()) {
        if (!cursor.mayApplyAfter(second)) continue;
        if (second != first) add(source, List.of(first, second));
        add(source, List.of(first), List.of(second));
      }
      return;
    }
    source.expanded = true;
    undiscoveredStates--;
  }

  private void add(State source, List<WorkbenchCurrency> actions) {
    add(source, List.of(), actions);
  }

  private void add(State source, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
    var id =
        source.id
            + ":"
            + initial.stream().map(Enum::name).reduce((a, b) -> a + "/" + b).orElse("")
            + ">"
            + actions.stream().map(Enum::name).reduce((a, b) -> a + "/" + b).orElseThrow();
    if (methods.containsKey(id)) return;
    var method = new Method(source, id, initial, actions);
    methods.put(id, method);
    source.methods.add(id);
    queue(method);
    openMethods++;
  }

  private Optional<Exit> classify(
      State source, DeferredItem outcome, boolean structuralCheckpoint) {
    var now = progress(outcome);
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    var before = progress(source.value).entries();
    if (!before.equals(now.entries())) {
      for (int i = 0; i < before.size(); i++)
        if (before.get(i) == Status.MATCH && now.entries().get(i) != Status.MATCH)
          return Optional.of(Exit.LOSS);
      return Optional.of(Exit.CHECKPOINT);
    }
    if (structuralCheckpoint
        && (source.value.item().rarity() != outcome.item().rarity()
            || source.value.item().explicits().size() != outcome.item().explicits().size()))
      return Optional.of(Exit.CHECKPOINT);
    return Optional.empty();
  }

  private Kernel<DeferredItem> kernel(DeferredItem state, WorkbenchCurrency action) {
    var key = new Key(state, action);
    var canonical = kernelAliases.getOrDefault(key, key);
    var result = kernels.get(canonical);
    if (result == null) throw new PendingKernel(key);
    if (completedKernels.containsKey(canonical)) {
      completedKernels.get(canonical);
      var consumers = kernelDeliveries.get(canonical);
      if (consumers != null && consumers.remove(executingMethod) && consumers.isEmpty())
        kernelDeliveries.remove(canonical);
    }
    return result;
  }

  public List<StateView> states() {
    return states.values().stream()
        .map(
            s ->
                new StateView(
                    s.id,
                    s.value,
                    s.depth,
                    progress(s.value),
                    s.expanded,
                    s.expanded && finished(s)))
        .toList();
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    m.source.id,
                    m.initial,
                    m.actions,
                    m.done ? m.unresolved ? "UNSUPPORTED" : "COMPLETE" : "PARTIAL",
                    m.reason,
                    Map.copyOf(m.points)))
        .toList();
  }

  public OutcomeStrategy<DeferredItem> strategy(int cycles) {
    var graph = new LinkedHashMap<DeferredItem, OutcomeStrategy.State<DeferredItem>>();
    for (var s : states.values()) {
      var choices = new ArrayList<OutcomeStrategy.Choice<DeferredItem>>();
      boolean closed = s.expanded;
      for (var id : s.methods) {
        var method = methods.get(id);
        var point = method.points.get(cycles);
        if (point == null) {
          closed = false;
          continue;
        }
        var outcomes = new LinkedHashMap<DeferredItem, Fraction>(point.active());
        var unresolved = Fraction.ZERO;
        for (var exit : point.exits().entrySet())
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
          else
            exit.getValue().forEach((target, mass) -> outcomes.merge(target, mass, Fraction::add));
        choices.add(new OutcomeStrategy.Choice<>(id, outcomes, unresolved));
      }
      graph.put(
          s.value,
          new OutcomeStrategy.State<>(progress(s.value).goal() == Status.MATCH, closed, choices));
    }
    return new OutcomeStrategy<>(graph);
  }

  /**
   * {@code MethodSearchCalculation} 내부의 상태 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code value}: 값 ({@code DeferredItem}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<String>}).
   *   <li>{@code matchingEntries}: 일치·항목목록 ({@code int}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code discoveryIndex}: 발견·색인 ({@code int}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code State(value, id, depth)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code value}(값), {@code
   *       id}(식별자), {@code depth}(깊이).
   * </ul>
   */
  private final class State {
    final DeferredItem value;
    final String id;
    final List<String> methods = new ArrayList<>();
    final int matchingEntries;
    int depth;
    int discoveryIndex;
    boolean expanded;

    State(DeferredItem value, String id, int depth) {
      this.value = value;
      this.id = id;
      this.depth = depth;
      this.matchingEntries = matches(value);
    }
  }

  /**
   * {@code MethodSearchCalculation} 내부의 제작방법 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code State}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code initial}: 초기값 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code runner}: 실행기 ({@code ExactMethodRunner<DeferredItem, WorkbenchCurrency>}).
   *   <li>{@code previousBoundary}: 이전·경계 ({@code Observation<DeferredItem, WorkbenchCurrency>}).
   *   <li>{@code pendingBoundary}: 대기·경계 ({@code Observation<DeferredItem, WorkbenchCurrency>}).
   *   <li>{@code activeExpansion}: 활성·확장 ({@code ExactMethodRunner.Compaction<DeferredItem>}).
   *   <li>{@code foldedInputs}: folded·입력목록 ({@code long}).
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, Observation<DeferredItem,
   *       WorkbenchCurrency>>}).
   *   <li>{@code done}: 완료여부 ({@code boolean}).
   *   <li>{@code queued}: 대기중 ({@code boolean}).
   *   <li>{@code ticket}: ticket ({@code long}).
   *   <li>{@code unresolved}: 미해결 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Method(source, id, initial, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       source}(출처), {@code id}(식별자), {@code initial}(초기값), {@code actions}(행동목록).
   *   <li>{@code initialize()}: 초기화 처리에서 {@code actions.stream}, {@code initial.stream}을(를) 사용한다.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code save(cycles, point)}: 현재 값을 저장 경계에 기록한다. 입력: {@code cycles}(주기횟수), {@code
   *       point}(지점). 사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial;
    final List<WorkbenchCurrency> actions;
    ExactMethodRunner<DeferredItem, WorkbenchCurrency> runner;
    Observation<DeferredItem, WorkbenchCurrency> previousBoundary;
    Observation<DeferredItem, WorkbenchCurrency> pendingBoundary;
    ExactMethodRunner.Compaction<DeferredItem> activeExpansion;
    long foldedInputs;
    final Map<Integer, Observation<DeferredItem, WorkbenchCurrency>> points = new LinkedHashMap<>();
    boolean done;
    boolean queued;
    long ticket;
    boolean unresolved;
    String reason;

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = List.copyOf(initial);
      this.actions = List.copyOf(actions);
    }

    void initialize() {
      List<Function<DeferredItem, WorkbenchCurrency>> selectors =
          actions.stream()
              .<Function<DeferredItem, WorkbenchCurrency>>map(action -> s -> action)
              .toList();
      runner =
          new ExactMethodRunner<>(
              source.value,
              new Template<>(
                  TEMPLATES + ":" + id,
                  initial.stream()
                      .<Function<DeferredItem, WorkbenchCurrency>>map(action -> s -> action)
                      .toList(),
                  selectors),
              MethodSearchCalculation.this::kernel,
              outcome -> classify(source, outcome, initial.isEmpty() && actions.size() == 1),
              compact,
              mixtures == null ? null : mixtures::startExpansion);
    }

    void step() {
      if (runner == null) initialize();
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      if (pendingBoundary == null) {
        pendingBoundary = runner.boundarySnapshot();
        activeExpansion =
            mixtures == null ? null : mixtures.startExpansion(pendingBoundary.active());
      }
      if (activeExpansion != null && !activeExpansion.complete()) {
        activeExpansion.step(32);
        return;
      }
      var point =
          new Observation<DeferredItem, WorkbenchCurrency>(
              pendingBoundary.cycles(),
              pendingBoundary.exits(),
              activeExpansion == null ? pendingBoundary.active() : activeExpansion.result(),
              pendingBoundary.initialUses(),
              pendingBoundary.cycleUses());
      pendingBoundary = null;
      activeExpansion = null;
      int cycles = Math.toIntExact(point.cycles());
      if (point.active().isEmpty()) {
        for (int n : OBSERVATIONS)
          if (!points.containsKey(n)) save(n, point.atLaterAbsorbedBoundary(n));
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      if (cycles == 1
          && point.initialUses().isEmpty()
          && point.active().keySet().equals(Set.of(source.value))) {
        // Every nonabsorbed result has the exact source state and phase. The unchanged kernel
        // therefore proves renewal, including all losses and unresolved exits, not just successes.
        for (int n : OBSERVATIONS) save(n, RenewalMethodObservation.repeat(point, source.value, n));
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      if (previousBoundary != null) {
        var invariant = RenewalMethodObservation.repeatInvariant(previousBoundary, point, cycles);
        if (invariant.isPresent()) {
          for (int n : OBSERVATIONS)
            if (!points.containsKey(n))
              save(
                  n,
                  RenewalMethodObservation.repeatInvariant(previousBoundary, point, n)
                      .orElseThrow());
          done = true;
          foldedInputs += runner.groupedInputs();
          runner = null;
          previousBoundary = null;
          openMethods--;
          return;
        }
      }
      if (OBSERVATIONS.contains(cycles) && !points.containsKey(cycles)) {
        save(cycles, point);
      }
      if (cycles == OBSERVATIONS.getLast()) {
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      previousBoundary = point;
      runner.beginCycle();
    }

    void save(int cycles, Observation<DeferredItem, WorkbenchCurrency> point) {
      if (!point.mass().equals(Fraction.ONE))
        throw new IllegalStateException("Method result lost mass");
      if (points.putIfAbsent(cycles, point) != null)
        throw new IllegalStateException("An earlier observation must not be overwritten");
      point.active().keySet().forEach(s -> link(source, s));
      point
          .exits()
          .forEach(
              (kind, values) -> {
                if (kind != Exit.UNRESOLVED) values.keySet().forEach(s -> link(source, s));
              });
      unresolved |= !point.exits().get(Exit.UNRESOLVED).isEmpty();
      if (unresolved) reason = "KERNEL_OR_GOAL_UNRESOLVED";
    }
  }

  /**
   * {@code MethodSearchCalculation} 내부의 전이커널·계산작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code Key}).
   *   <li>{@code cursor}: 진행커서 ({@code DeferredTransitions.Cursor}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<DeferredItem, Fraction>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code KernelWork(key, cursor)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키), {@code
   *       cursor}(진행커서).
   *   <li>{@code ready()}: {@code true} 값을 조회한다. 반환: {@code boolean}.
   *   <li>{@code remainingWork()}: 남은·계산작업 값을 조회·구성한다. 반환: {@code java.math.BigInteger}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   * </ul>
   */
  private class KernelWork {
    final Key key;
    final DeferredTransitions.Cursor cursor;
    final Map<DeferredItem, Fraction> outcomes = new LinkedHashMap<>();

    KernelWork(Key key, DeferredTransitions.Cursor cursor) {
      this.key = key;
      this.cursor = cursor;
    }

    boolean ready() {
      return true;
    }

    java.math.BigInteger remainingWork() {
      return cursor.remainingOutcomes();
    }

    void step() {
      if (cursor.blocked()) {
        boolean unavailable = cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE;
        finishKernel(
            key,
            new Kernel<>(List.of(), unavailable ? Fraction.ZERO : Fraction.ONE, unavailable),
            0,
            false);
        return;
      }
      var next = cursor.next();
      if (!outcomes.containsKey(next.state())) retainedOutcomes++;
      outcomes.merge(next.state(), next.probability(), Fraction::add);
      if (cursor.complete()) {
        finishKernel(
            key,
            new Kernel<>(
                outcomes.entrySet().stream()
                    .map(e -> new Outcome<>(e.getKey(), e.getValue()))
                    .toList(),
                Fraction.ZERO,
                false),
            outcomes.size(),
            false);
      }
      trimKernels();
    }
  }

  /**
   * {@code MethodSearchCalculation} 내부의 혼합·전이커널·계산작업 처리를 맡는다.
   *
   * <p>기존 계약: Linear transport of one conditional mixture through already verified finite-cell
   * kernels.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code dependencies}: 의존목록 ({@code Map<Key, Fraction>}).
   *   <li>{@code inputs}: 입력목록 ({@code Iterator<Map.Entry<Key, Fraction>>}).
   *   <li>{@code input}: 입력 ({@code Map.Entry<Key, Fraction>}).
   *   <li>{@code current}: 현재 ({@code Kernel<DeferredItem>}).
   *   <li>{@code position}: 진행위치 ({@code int}).
   *   <li>{@code unavailable}: 사용불가 ({@code Boolean}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code normalizer}: normalizer ({@code ExactMethodRunner.Compaction<DeferredItem>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code MixtureKernelWork(key, dependencies)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키),
   *       {@code dependencies}(의존목록).
   *   <li>{@code ready()}: 준비완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code remainingWork()}: 남은·계산작업 값을 조회·구성한다. 반환: {@code java.math.BigInteger}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code releaseDependencies()}: 해제·의존목록 처리에서 {@code dependencies.keySet}, {@code
   *       dependencyPins.compute}을(를) 사용한다. 사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
  private final class MixtureKernelWork extends KernelWork {
    final Map<Key, Fraction> dependencies;
    Iterator<Map.Entry<Key, Fraction>> inputs;
    Map.Entry<Key, Fraction> input;
    Kernel<DeferredItem> current;
    int position;
    Boolean unavailable;
    Fraction unresolved = Fraction.ZERO;
    ExactMethodRunner.Compaction<DeferredItem> normalizer;

    MixtureKernelWork(Key key, Map<Key, Fraction> dependencies) {
      super(key, null);
      this.dependencies = Map.copyOf(dependencies);
    }

    @Override
    boolean ready() {
      return dependencies.keySet().stream().allMatch(kernels::containsKey);
    }

    @Override
    java.math.BigInteger remainingWork() {
      return java.math.BigInteger.valueOf(dependencies.size());
    }

    @Override
    void step() {
      if (!ready()) return;
      if (dependencies.size() == 1) {
        var value = kernels.get(dependencies.keySet().iterator().next());
        // Count shared outcome references conservatively so eviction never hides retained storage.
        retainedOutcomes += value.outcomes().size();
        releaseDependencies();
        finishKernel(key, value, value.outcomes().size(), true);
        sharedKernelLookups++;
        return;
      }
      if (inputs == null) inputs = dependencies.entrySet().iterator();
      if (current == null && inputs.hasNext()) {
        input = inputs.next();
        current = kernels.get(input.getKey());
        completedKernels.get(input.getKey());
        if (unavailable != null && unavailable != current.unavailable())
          throw new IllegalStateException("Mixture crossed an applicability boundary");
        unavailable = current.unavailable();
        unresolved = unresolved.add(input.getValue().multiply(current.unresolved()));
        position = 0;
      }
      if (current != null) {
        for (int n = 0; n < 32 && position < current.outcomes().size(); n++, position++) {
          var outcome = current.outcomes().get(position);
          if (!outcomes.containsKey(outcome.state())) retainedOutcomes++;
          outcomes.merge(
              outcome.state(), input.getValue().multiply(outcome.probability()), Fraction::add);
        }
        if (position == current.outcomes().size()) current = null;
        trimKernels();
        return;
      }
      if (normalizer == null) normalizer = mixtures.start(outcomes);
      if (!normalizer.complete()) normalizer.step(32);
      if (!normalizer.complete()) return;
      var values = normalizer.result();
      retainedOutcomes += values.size() - outcomes.size();
      releaseDependencies();
      finishKernel(
          key,
          new Kernel<>(
              values.entrySet().stream().map(e -> new Outcome<>(e.getKey(), e.getValue())).toList(),
              unresolved,
              Boolean.TRUE.equals(unavailable)),
          values.size(),
          true);
    }

    private void releaseDependencies() {
      dependencies
          .keySet()
          .forEach(
              key ->
                  dependencyPins.compute(
                      key,
                      (ignored, count) -> {
                        if (count == null || count < 1)
                          throw new IllegalStateException("Missing kernel dependency pin");
                        return count == 1 ? null : count - 1;
                      }));
    }
  }
}
