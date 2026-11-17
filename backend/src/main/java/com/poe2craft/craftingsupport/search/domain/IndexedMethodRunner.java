package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 검증된 동치류에서 방법별 확률을 점진적으로 계산한다.
 *
 * <p>기존 계약: Cooperative method marginals on proven classes; observations are not incoming item
 * laws.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "cooperative-shared-class-method-marginals-v2"} ({@code
 *       String}).
 *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
 *   <li>{@code source}: 출처 ({@code int}).
 *   <li>{@code template}: 템플릿 ({@code ExactMethodRunner.Template<Integer, WorkbenchCurrency>}).
 *   <li>{@code classify}: 상태분류 ({@code Function<Integer, Optional<ExactMethodRunner.Exit>>}).
 *   <li>{@code store}: 저장소 ({@code IndexedKernelStore}).
 *   <li>{@code runner}: 실행기 ({@code ExactMethodRunner<Integer, WorkbenchCurrency>}).
 *   <li>{@code pendingKey}: 대기·키 ({@code Key}).
 *   <li>{@code pending}: 대기 ({@code IndexedKernelStore.Request}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code IndexedMethodRunner(index, source, template, classify, cacheLimit)}: 기본 정책을 보충해 공통
 *       생성자로 위임한다. 입력: {@code index}(색인), {@code source}(출처), {@code template}(템플릿), {@code
 *       classify}(상태분류), {@code cacheLimit}(캐시·한도).
 *   <li>{@code IndexedMethodRunner(store, source, template, classify)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code store}(저장소), {@code source}(출처), {@code template}(템플릿), {@code classify}(상태분류).
 *   <li>{@code kernel(source, action)}: 전이커널 처리에서 {@code pendingKey.equals}, {@code
 *       pending.complete}을(를) 사용한다. 입력: {@code source}(출처), {@code action}(행동). 반환: {@code
 *       ExactMethodRunner.Kernel<Integer>}. 사용하는 예외: {@code MissingKernel}.
 *   <li>{@code atBoundary()}: 기준·경계 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code beginCycle()}: 시작·주기 처리에서 {@code runner.beginCycle}을(를) 사용한다.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code observation()}: 관측값 값을 조회·구성한다. 반환: {@code ExactMethodRunner.Observation<Integer,
 *       WorkbenchCurrency>}.
 *   <li>{@code snapshot()}: 현재 내용을 불변 관측값으로 구성한다. 반환: {@code Snapshot}.
 *   <li>{@code condition(input, exit, target)}: 조건에 해당하는 분포를 구한다. 입력: {@code input}(입력), {@code
 *       exit}(종료), {@code target}(대상). 반환: {@code ConditionalMethodLaw}.
 *   <li>{@code cachedKernels()}: 캐시된·전이커널목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code calculatedKernels()}: calculated·전이커널목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code evictedKernels()}: evicted·전이커널목록 값을 조회·구성한다. 반환: {@code long}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code IndexedMethodRunner}에서 사용하는 키의 불변 값이다.
 *   <li>{@code MissingKernel}: {@code IndexedMethodRunner} 내부의 누락·전이커널 처리를 맡는다.
 *   <li>{@code Snapshot}: {@code IndexedMethodRunner} 내부의 스냅샷 처리를 맡는다.
 * </ul>
 */
public final class IndexedMethodRunner {
  public static final String VERSION = "cooperative-shared-class-method-marginals-v2";

  /**
   * {@code IndexedMethodRunner}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   * </ul>
   */
  private record Key(int source, WorkbenchCurrency action) {}

  /**
   * {@code IndexedMethodRunner} 내부의 누락·전이커널 처리를 맡는다.
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
   *   <li>{@code MissingKernel(key)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키).
   * </ul>
   */
  private static final class MissingKernel extends RuntimeException {
    final Key key;

    MissingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final BasicClassIndex index;
  private final int source;
  private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
  private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
  private final IndexedKernelStore store;
  private final ExactMethodRunner<Integer, WorkbenchCurrency> runner;
  private Key pendingKey;
  private IndexedKernelStore.Request pending;

  public IndexedMethodRunner(
      BasicClassIndex index,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      int cacheLimit) {
    this(new IndexedKernelStore(index, cacheLimit, Integer.MAX_VALUE), source, template, classify);
  }

  public IndexedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify) {
    this.store = Objects.requireNonNull(store);
    this.index = store.index();
    index.key(source);
    this.source = source;
    this.template = template;
    this.classify = classify;
    runner = new ExactMethodRunner<>(source, template, this::kernel, classify);
  }

  private ExactMethodRunner.Kernel<Integer> kernel(int source, WorkbenchCurrency action) {
    var key = new Key(source, action);
    if (pending != null && pendingKey.equals(key)) {
      if (!pending.complete()) throw new MissingKernel(key);
      var result = pending.result();
      pending = null;
      pendingKey = null;
      return result;
    }
    var request = store.request(source, action);
    if (request.complete()) return request.result();
    pendingKey = key;
    pending = request;
    throw new MissingKernel(key);
  }

  public boolean atBoundary() {
    return runner.atBoundary();
  }

  public void beginCycle() {
    runner.beginCycle();
  }

  public void step() {
    if (pending != null && !pending.complete()) {
      pending.step();
      return;
    }
    try {
      runner.step();
    } catch (MissingKernel missing) {
      // The handle was registered before suspension and survives shared-cache eviction.
    }
  }

  public ExactMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
    return runner.observation();
  }

  public Snapshot snapshot() {
    return new Snapshot(index, source, template, classify, runner.observation());
  }

  public ConditionalMethodLaw condition(
      ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
    return snapshot().condition(input, exit, target);
  }

  /**
   * {@code IndexedMethodRunner} 내부의 스냅샷 처리를 맡는다.
   *
   * <p>기존 계약: A completed observation can outlive its runner without losing its actual-input
   * operator.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code template}: 템플릿 ({@code ExactMethodRunner.Template<Integer, WorkbenchCurrency>}).
   *   <li>{@code classify}: 상태분류 ({@code Function<Integer, Optional<ExactMethodRunner.Exit>>}).
   *   <li>{@code point}: 지점 ({@code ExactMethodRunner.Observation<Integer, WorkbenchCurrency>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Snapshot(index, source, template, classify, point)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code index}(색인), {@code source}(출처), {@code template}(템플릿), {@code classify}(상태분류),
   *       {@code point}(지점).
   *   <li>{@code observation()}: {@code point} 값을 조회한다. 반환: {@code
   *       ExactMethodRunner.Observation<Integer, WorkbenchCurrency>}.
   *   <li>{@code atLaterAbsorbedBoundary(cycles)}: 기준·후속·흡수된·경계 처리에서 {@code
   *       point.atLaterAbsorbedBoundary}을(를) 사용한다. 입력: {@code cycles}(주기횟수). 반환: {@code Snapshot}.
   *   <li>{@code condition(input, exit, target)}: 조건에 해당하는 분포를 구한다. 입력: {@code input}(입력), {@code
   *       exit}(종료), {@code target}(대상). 반환: {@code ConditionalMethodLaw}.
   * </ul>
   */
  public static final class Snapshot {
    private final BasicClassIndex index;
    private final int source;
    private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
    private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
    private final ExactMethodRunner.Observation<Integer, WorkbenchCurrency> point;

    private Snapshot(
        BasicClassIndex index,
        int source,
        ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
        Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
        ExactMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
      this.index = index;
      this.source = source;
      this.template = template;
      this.classify = classify;
      this.point = point;
    }

    public ExactMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
      return point;
    }

    public Snapshot atLaterAbsorbedBoundary(long cycles) {
      return new Snapshot(index, source, template, classify, point.atLaterAbsorbedBoundary(cycles));
    }

    public ConditionalMethodLaw condition(
        ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
      var values = exit.map(point.exits()::get).orElse(point.active());
      var probability = values.getOrDefault(target, Fraction.ZERO);
      return ConditionalMethodLaw.output(
          input, index, source, template, classify, point.cycles(), exit, target, probability);
    }
  }

  public int cachedKernels() {
    return store.cachedKernels();
  }

  public long calculatedKernels() {
    return store.calculatedKernels();
  }

  public long evictedKernels() {
    return store.evictedKernels();
  }
}
