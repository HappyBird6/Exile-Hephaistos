package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 기본 전이를 공유하며 방법 계산별 정밀도로 확률 구간을 계산한다.
 *
 * <p>기존 계약: Shared exact primitive kernels, with arithmetic precision scoped to one cooperative
 * method.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code store}: 저장소 ({@code IndexedKernelStore}).
 *   <li>{@code source}: 출처 ({@code int}).
 *   <li>{@code template}: 템플릿 ({@code ExactMethodRunner.Template<Integer, WorkbenchCurrency>}).
 *   <li>{@code classify}: 상태분류 ({@code Function<Integer, Optional<ExactMethodRunner.Exit>>}).
 *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
 *   <li>{@code runner}: 실행기 ({@code CertifiedMethodRunner<Integer, WorkbenchCurrency>}).
 *   <li>{@code pending}: 대기 ({@code IndexedKernelStore.Request}).
 *   <li>{@code pendingKey}: 대기·키 ({@code Key}).
 *   <li>{@code absorptionLaws}: 흡수·분포목록 ({@code List<CertifiedMutableLawStore.Law>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code IndexedCertifiedMethodRunner(store, source, template, classify, precision)}: 기본 정책을
 *       보충해 공통 생성자로 위임한다. 입력: {@code store}(저장소), {@code source}(출처), {@code template}(템플릿), {@code
 *       classify}(상태분류), {@code precision}(정밀도).
 *   <li>{@code IndexedCertifiedMethodRunner(store, source, template, classify, precision,
 *       absorptionStore)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code store}(저장소), {@code source}(출처),
 *       {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionStore}(흡수·저장소).
 *   <li>{@code IndexedCertifiedMethodRunner(store, source, template, classify, precision,
 *       absorptionStore, workingMaps)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code store}(저장소), {@code
 *       source}(출처), {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionStore}(흡수·저장소), {@code workingMaps}(계산중·맵목록).
 *   <li>{@code IndexedCertifiedMethodRunner(store, source, template, classify, precision,
 *       absorptionStore, workingMaps, initialize)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code store}(저장소),
 *       {@code source}(출처), {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도),
 *       {@code absorptionStore}(흡수·저장소), {@code workingMaps}(계산중·맵목록), {@code initialize}(초기화).
 *   <li>{@code leaseCursor()}: 사용권·진행커서 값을 조회·구성한다. 반환: {@code CertifiedMethodRunner<Integer,
 *       WorkbenchCurrency>.CursorLease}.
 *   <li>{@code restoreCursor(header, store, source, template, classify, precision,
 *       absorptionStore)}: 진행커서을(를) 복원한다. 입력: {@code header}(헤더), {@code store}(저장소), {@code
 *       source}(출처), {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionStore}(흡수·저장소). 반환: {@code Restoration}.
 *   <li>{@code restoreCursor(header, store, source, template, classify, precision, absorptionStore,
 *       workingMaps)}: 진행커서을(를) 복원한다. 입력: {@code header}(헤더), {@code store}(저장소), {@code
 *       source}(출처), {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionStore}(흡수·저장소), {@code workingMaps}(계산중·맵목록). 반환: {@code Restoration}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code kernel(source, action)}: 전이커널 처리에서 {@code pendingKey.equals}, {@code
 *       pending.complete}을(를) 사용한다. 입력: {@code source}(출처), {@code action}(행동). 반환: {@code
 *       ExactMethodRunner.Kernel<Integer>}. 사용하는 예외: {@code Missing}.
 *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code retention()}: 보유량 값을 조회·구성한다. 반환: {@code CertifiedMethodRunner.Retention}.
 *   <li>{@code atBoundary()}: 기준·경계 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code absorbed()}: 흡수된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code cycles()}: 주기횟수 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code beginCycle()}: 시작·주기 처리에서 {@code runner.beginCycle}을(를) 사용한다.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code observation()}: 관측값 값을 조회·구성한다. 반환: {@code
 *       CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency>}.
 *   <li>{@code snapshot()}: 현재 내용을 불변 관측값으로 구성한다. 반환: {@code Snapshot}.
 *   <li>{@code restoredSnapshot(index, source, template, classify, precision, point)}: restored·스냅샷
 *       처리에서 {@code index.key}, {@code CertifiedInterval.zero}을(를) 사용한다. 입력: {@code index}(색인),
 *       {@code source}(출처), {@code template}(템플릿), {@code classify}(상태분류), {@code precision}(정밀도),
 *       {@code point}(지점). 반환: {@code Snapshot}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code freezeSnapshot(store)}: 불변복사·스냅샷의 입력에 따른 분기·결과를 처리한다. 입력: {@code store}(저장소). 반환:
 *       {@code FrozenSnapshot}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Missing}: {@code IndexedCertifiedMethodRunner} 내부의 누락 처리를 맡는다.
 *   <li>{@code Key}: {@code IndexedCertifiedMethodRunner}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Restoration}: {@code IndexedCertifiedMethodRunner} 내부의 복원 처리를 맡는다.
 *   <li>{@code FrozenSnapshot}: {@code IndexedCertifiedMethodRunner} 내부의 Frozen·스냅샷 처리를 맡는다.
 *   <li>{@code InitialLaw}: {@code IndexedCertifiedMethodRunner} 내부의 초기값·분포 처리를 맡는다.
 *   <li>{@code InitialEquivalence}: {@code IndexedCertifiedMethodRunner} 내부의 초기값·Equivalence 처리를
 *       맡는다.
 *   <li>{@code Snapshot}: {@code IndexedCertifiedMethodRunner} 내부의 스냅샷 처리를 맡는다.
 * </ul>
 */
public final class IndexedCertifiedMethodRunner implements AutoCloseable {
  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 누락 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Missing()}: 전달받은 값으로 구성원을 초기화한다.
   * </ul>
   */
  private static final class Missing extends RuntimeException {
    Missing() {
      super(null, null, false, false);
    }
  }

  /**
   * {@code IndexedCertifiedMethodRunner}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   * </ul>
   */
  private record Key(int source, WorkbenchCurrency action) {}

  private final IndexedKernelStore store;
  private final int source;
  private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
  private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
  private final CertifiedInterval.Precision precision;
  private CertifiedMethodRunner<Integer, WorkbenchCurrency> runner;
  private IndexedKernelStore.Request pending;
  private Key pendingKey;
  private final List<CertifiedMutableLawStore.Law> absorptionLaws = new ArrayList<>();

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision) {
    this(store, source, template, classify, precision, null);
  }

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore) {
    this(
        store,
        source,
        template,
        classify,
        precision,
        absorptionStore,
        CertifiedMethodRunner.WorkingMaps.memory());
  }

  public IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps) {
    this(store, source, template, classify, precision, absorptionStore, workingMaps, true);
  }

  private IndexedCertifiedMethodRunner(
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps,
      boolean initialize) {
    this.store = Objects.requireNonNull(store);
    this.source = source;
    this.template = template;
    this.classify = classify;
    this.precision = precision;
    store.index().key(source);
    if (!initialize) return;
    runner =
        absorptionStore == null
            ? new CertifiedMethodRunner<>(
                source,
                template,
                this::kernel,
                classify,
                precision,
                LinkedHashMap::new,
                workingMaps)
            : new CertifiedMethodRunner<>(
                source,
                template,
                this::kernel,
                classify,
                precision,
                () -> {
                  var law = absorptionStore.create();
                  absorptionLaws.add(law);
                  return law;
                },
                workingMaps);
  }

  public CertifiedMethodRunner<Integer, WorkbenchCurrency>.CursorLease leaseCursor() {
    return runner.leaseCursor();
  }

  /**
   * The caller restores the same class index and versioned method semantics before reading rows.
   */
  public static Restoration restoreCursor(
      CertifiedMethodRunner.CursorHeader<Integer> header,
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore) {
    return restoreCursor(
        header,
        store,
        source,
        template,
        classify,
        precision,
        absorptionStore,
        CertifiedMethodRunner.WorkingMaps.memory());
  }

  public static Restoration restoreCursor(
      CertifiedMethodRunner.CursorHeader<Integer> header,
      IndexedKernelStore store,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMutableLawStore absorptionStore,
      CertifiedMethodRunner.WorkingMaps<Integer> workingMaps) {
    var target =
        new IndexedCertifiedMethodRunner(
            store, source, template, classify, precision, absorptionStore, workingMaps, false);
    return new Restoration(
        target,
        CertifiedMethodRunner.restoreCursor(
            header,
            template,
            target::kernel,
            classify,
            precision,
            () -> {
              if (absorptionStore == null) return new LinkedHashMap<Integer, CertifiedInterval>();
              var law = absorptionStore.create();
              target.absorptionLaws.add(law);
              return law;
            },
            workingMaps));
  }

  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 복원 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code target}: 대상 ({@code IndexedCertifiedMethodRunner}).
   *   <li>{@code restoration}: 복원 ({@code CertifiedMethodRunner.CursorRestore<Integer,
   *       WorkbenchCurrency>}).
   *   <li>{@code finished}: 종료 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Restoration(target, restoration)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code target}(대상),
   *       {@code restoration}(복원).
   *   <li>{@code accept(row)}: 수락 처리에서 {@code restoration.accept}을(를) 사용한다. 입력: {@code row}(행).
   *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code finish()}: 진행 중인 계산 또는 누적 처리를 마무리한다. 반환: {@code IndexedCertifiedMethodRunner}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public static final class Restoration implements AutoCloseable {
    private final IndexedCertifiedMethodRunner target;
    private final CertifiedMethodRunner.CursorRestore<Integer, WorkbenchCurrency> restoration;
    private boolean finished;

    private Restoration(
        IndexedCertifiedMethodRunner target,
        CertifiedMethodRunner.CursorRestore<Integer, WorkbenchCurrency> restoration) {
      this.target = target;
      this.restoration = restoration;
    }

    public void accept(CertifiedMethodRunner.CursorRow<Integer, WorkbenchCurrency> row) {
      restoration.accept(row);
    }

    public long retainedStates() {
      return restoration.retainedStates();
    }

    public IndexedCertifiedMethodRunner finish() {
      target.runner = restoration.finish();
      finished = true;
      return target;
    }

    @Override
    public void close() {
      if (finished) return;
      restoration.close();
      target.absorptionLaws.clear();
    }
  }

  @Override
  public void close() {
    if (runner != null) runner.closeWorkingMaps();
    absorptionLaws.forEach(CertifiedMutableLawStore.Law::close);
    absorptionLaws.clear();
    pending = null;
    pendingKey = null;
  }

  private ExactMethodRunner.Kernel<Integer> kernel(int source, WorkbenchCurrency action) {
    var key = new Key(source, action);
    if (pending != null && pendingKey.equals(key)) {
      if (!pending.complete()) throw new Missing();
      var result = pending.result();
      pending = null;
      pendingKey = null;
      return result;
    }
    var request = store.request(source, action);
    if (request.complete()) return request.result();
    pending = request;
    pendingKey = key;
    throw new Missing();
  }

  public long retainedStates() {
    return runner.retainedStates();
  }

  public CertifiedMethodRunner.Retention retention() {
    return runner.retention();
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
    } catch (Missing ignored) {
      /* Suspended before changing phase or cost. */
    }
  }

  public CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
    return runner.observation();
  }

  public Snapshot snapshot() {
    return new Snapshot(store.index(), source, template, classify, precision, runner.observation());
  }

  public static Snapshot restoredSnapshot(
      BasicClassIndex index,
      int source,
      ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
      CertifiedInterval.Precision precision,
      CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
    index.key(source);
    var total = CertifiedInterval.zero(precision.fractionalBits());
    if (point.cycles() < 0 || point.exits().size() != ExactMethodRunner.Exit.values().length)
      throw new IllegalArgumentException("Invalid saved observation");
    var parts = new ArrayList<>(point.exits().values());
    for (var law : parts)
      for (var entry : law.entrySet()) {
        index.key(entry.getKey());
        if (entry.getValue().fractionalBits() != precision.fractionalBits())
          throw new IllegalArgumentException("Observation precision mismatch");
        total = total.add(entry.getValue()).probability();
      }
    var active = CertifiedInterval.zero(precision.fractionalBits());
    for (var entry : point.active().entrySet()) {
      index.key(entry.getKey());
      if (entry.getValue().fractionalBits() != precision.fractionalBits())
        throw new IllegalArgumentException("Observation precision mismatch");
      active = active.add(entry.getValue()).probability();
    }
    total = total.add(active);
    if (!total.equals(point.mass()) || !total.contains(Fraction.ONE))
      throw new IllegalArgumentException("Saved observation mass mismatch");
    return new Snapshot(index, source, template, classify, precision, point);
  }

  public FrozenSnapshot freezeSnapshot(CertifiedLawStore store) {
    return new FrozenSnapshot(store);
  }

  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 Frozen·스냅샷 처리를 맡는다.
   *
   * <p>기존 계약: Cooperative storage of a leased boundary; no duplicate row maps are captured.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code store}: 저장소 ({@code CertifiedLawStore}).
   *   <li>{@code lease}: 사용권 ({@code CertifiedMethodRunner<Integer,
   *       WorkbenchCurrency>.ObservationLease}).
   *   <li>{@code exits}: 종료목록 ({@code Map<ExactMethodRunner.Exit, Map<Integer,
   *       CertifiedInterval>>}).
   *   <li>{@code current}: 현재 ({@code CertifiedLawStore.Freeze}).
   *   <li>{@code part}: part ({@code int}).
   *   <li>{@code result}: 결과 ({@code Snapshot}).
   *   <li>{@code closed}: 닫힘 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code FrozenSnapshot(store)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code store}(저장소).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code transientBytes()}: 임시·바이트수 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code storeIndex()}: 저장소·색인 값을 조회·구성한다. 반환: {@code BasicClassIndex}.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Snapshot}. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public final class FrozenSnapshot implements AutoCloseable {
    private final CertifiedLawStore store;
    private CertifiedMethodRunner<Integer, WorkbenchCurrency>.ObservationLease lease;
    private final Map<ExactMethodRunner.Exit, Map<Integer, CertifiedInterval>> exits =
        new EnumMap<>(ExactMethodRunner.Exit.class);
    private CertifiedLawStore.Freeze current;
    private int part;
    private Snapshot result;
    private boolean closed;

    private FrozenSnapshot(CertifiedLawStore store) {
      this.store = store;
      lease = runner.leaseObservation();
    }

    public boolean complete() {
      return result != null;
    }

    public long transientBytes() {
      return current == null ? 0 : current.transientBytes();
    }

    public void step() {
      if (closed) throw new IllegalStateException("Snapshot freeze closed");
      if (complete()) return;
      var point = lease.point();
      var kinds = ExactMethodRunner.Exit.values();
      if (current == null) {
        current =
            store.prepare(part < kinds.length ? point.exits().get(kinds[part]) : point.active());
        return;
      }
      if (!current.complete()) {
        current.step();
        return;
      }
      if (part < kinds.length) {
        exits.put(kinds[part++], current.result());
        current = null;
        return;
      }
      result =
          new Snapshot(
              storeIndex(),
              source,
              template,
              classify,
              precision,
              new CertifiedMethodRunner.Observation<>(
                  point.cycles(),
                  Map.copyOf(exits),
                  current.result(),
                  point.initialUses(),
                  point.cycleUses(),
                  point.mass()));
      lease.close();
      lease = null;
      current = null;
      exits.clear();
    }

    private BasicClassIndex storeIndex() {
      return IndexedCertifiedMethodRunner.this.store.index();
    }

    public Snapshot result() {
      if (!complete()) throw new IllegalStateException("Partial snapshot freeze");
      return result;
    }

    @Override
    public void close() {
      closed = true;
      if (lease != null) lease.close();
      lease = null;
      current = null;
      exits.clear();
    }
  }

  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 초기값·분포 처리를 맡는다.
   *
   * <p>기존 계약: Certified from a completed shared primitive request, never from an asserted hash.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<Integer, Fraction>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code InitialLaw(request)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code request}(요청).
   *   <li>{@code outcomes()}: 가능한 결과와 확률을 조회한다. 반환: {@code Map<Integer, Fraction>}.
   *   <li>{@code unresolved()}: {@code unresolved} 값을 조회한다. 반환: {@code Fraction}.
   *   <li>{@code unavailable()}: {@code unavailable} 값을 조회한다. 반환: {@code boolean}.
   *   <li>{@code equivalentTo(other)}: equivalent·대상 처리에서 {@code unresolved.equals}, {@code
   *       outcomes.equals}을(를) 사용한다. 입력: {@code other}(비교대상). 반환: {@code InitialEquivalence}. 사용하는
   *       예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public static final class InitialLaw {
    private final BasicClassIndex index;
    private final int source;
    private final WorkbenchCurrency action;
    private final Map<Integer, Fraction> outcomes;
    private final Fraction unresolved;
    private final boolean unavailable;

    public InitialLaw(IndexedKernelStore.Request request) {
      var kernel = request.result();
      index = request.store().index();
      source = request.source();
      action = request.action();
      var law = new HashMap<Integer, Fraction>();
      kernel.outcomes().forEach(o -> law.merge(o.state(), o.probability(), Fraction::add));
      outcomes = Map.copyOf(law);
      unresolved = kernel.unresolved();
      unavailable = kernel.unavailable();
    }

    public Map<Integer, Fraction> outcomes() {
      return outcomes;
    }

    public Fraction unresolved() {
      return unresolved;
    }

    public boolean unavailable() {
      return unavailable;
    }

    public InitialEquivalence equivalentTo(InitialLaw other) {
      if (index != other.index
          || unavailable != other.unavailable
          || !unresolved.equals(other.unresolved)
          || !outcomes.equals(other.outcomes))
        throw new IllegalArgumentException("Initial class laws differ");
      return new InitialEquivalence(index, source, other.source, action, other.action);
    }
  }

  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 초기값·Equivalence 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code targetSource}: 대상·출처 ({@code int}).
   *   <li>{@code from}: 원본 ({@code WorkbenchCurrency}).
   *   <li>{@code to}: 대상 ({@code WorkbenchCurrency}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code InitialEquivalence(index, source, targetSource, from, to)}: 전달받은 값으로 구성원을 초기화한다.
   *       입력: {@code index}(색인), {@code source}(출처), {@code targetSource}(대상·출처), {@code from}(원본),
   *       {@code to}(대상).
   * </ul>
   */
  public static final class InitialEquivalence {
    private final BasicClassIndex index;
    private final int source, targetSource;
    private final WorkbenchCurrency from, to;

    private InitialEquivalence(
        BasicClassIndex index,
        int source,
        int targetSource,
        WorkbenchCurrency from,
        WorkbenchCurrency to) {
      this.index = index;
      this.source = source;
      this.targetSource = targetSource;
      this.from = from;
      this.to = to;
    }
  }

  /**
   * {@code IndexedCertifiedMethodRunner} 내부의 스냅샷 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code template}: 템플릿 ({@code ExactMethodRunner.Template<Integer, WorkbenchCurrency>}).
   *   <li>{@code classify}: 상태분류 ({@code Function<Integer, Optional<ExactMethodRunner.Exit>>}).
   *   <li>{@code precision}: 정밀도 ({@code CertifiedInterval.Precision}).
   *   <li>{@code point}: 지점 ({@code CertifiedMethodRunner.Observation<Integer,
   *       WorkbenchCurrency>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Snapshot(index, source, template, classify, precision, point)}: 전달받은 값으로 구성원을
   *       초기화한다. 입력: {@code index}(색인), {@code source}(출처), {@code template}(템플릿), {@code
   *       classify}(상태분류), {@code precision}(정밀도), {@code point}(지점).
   *   <li>{@code observation()}: {@code point} 값을 조회한다. 반환: {@code
   *       CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency>}.
   *   <li>{@code freeze(store)}: 변경 중인 값을 불변 조회·저장 표현으로 만든다. 입력: {@code store}(저장소). 반환: {@code
   *       Snapshot}.
   *   <li>{@code atLaterAbsorbedBoundary(cycles)}: 기준·후속·흡수된·경계 처리에서 {@code
   *       point.atLaterAbsorbedBoundary}을(를) 사용한다. 입력: {@code cycles}(주기횟수). 반환: {@code Snapshot}.
   *   <li>{@code withEquivalentInitial(proof)}: Equivalent·초기값을(를) 반영한 값을 구한다. 입력: {@code
   *       proof}(증명). 반환: {@code Snapshot}. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code condition(input, exit, target)}: 조건에 해당하는 분포를 구한다. 입력: {@code input}(입력), {@code
   *       exit}(종료), {@code target}(대상). 반환: {@code ConditionalMethodLaw}.
   * </ul>
   */
  public static final class Snapshot {
    private final BasicClassIndex index;
    private final int source;
    private final ExactMethodRunner.Template<Integer, WorkbenchCurrency> template;
    private final Function<Integer, Optional<ExactMethodRunner.Exit>> classify;
    private final CertifiedInterval.Precision precision;
    private final CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point;

    private Snapshot(
        BasicClassIndex index,
        int source,
        ExactMethodRunner.Template<Integer, WorkbenchCurrency> template,
        Function<Integer, Optional<ExactMethodRunner.Exit>> classify,
        CertifiedInterval.Precision precision,
        CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {
      this.index = index;
      this.source = source;
      this.template = template;
      this.classify = classify;
      this.precision = precision;
      this.point = point;
    }

    public CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> observation() {
      return point;
    }

    public Snapshot freeze(CertifiedLawStore store) {
      var exits =
          new EnumMap<ExactMethodRunner.Exit, Map<Integer, CertifiedInterval>>(
              ExactMethodRunner.Exit.class);
      point.exits().forEach((kind, law) -> exits.put(kind, store.freeze(law)));
      return new Snapshot(
          index,
          source,
          template,
          classify,
          precision,
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              Map.copyOf(exits),
              store.freeze(point.active()),
              point.initialUses(),
              point.cycleUses(),
              point.mass()));
    }

    public Snapshot atLaterAbsorbedBoundary(long cycles) {
      return new Snapshot(
          index, source, template, classify, precision, point.atLaterAbsorbedBoundary(cycles));
    }

    /**
     * Change only an equivalent one-action preparation; retain the real action for exact replay.
     */
    public Snapshot withEquivalentInitial(InitialEquivalence proof) {
      if (proof.index != index
          || proof.source != source
          || template.initial().size() != 1
          || template.initial().getFirst().apply(source) != proof.from)
        throw new IllegalArgumentException("Equivalence does not match this method");
      if (source != proof.targetSource
          && (classify.apply(source).isPresent() || classify.apply(proof.targetSource).isPresent()))
        throw new IllegalArgumentException("Cannot change an already terminal origin");
      var uses = new HashMap<>(point.initialUses());
      var count = uses.remove(proof.from);
      if (count != null) uses.merge(proof.to, count, CertifiedInterval::add);
      var rebound =
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              point.exits(),
              point.active(),
              Map.copyOf(uses),
              point.cycleUses(),
              point.mass());
      var reboundTemplate =
          new ExactMethodRunner.Template<Integer, WorkbenchCurrency>(
              template.id() + ":equivalent-initial:" + proof.to,
              List.of(s -> proof.to),
              template.cycle());
      return new Snapshot(index, proof.targetSource, reboundTemplate, classify, precision, rebound);
    }

    public ConditionalMethodLaw condition(
        ConditionalMethodLaw input, Optional<ExactMethodRunner.Exit> exit, int target) {
      var values = exit.map(point.exits()::get).orElse(point.active());
      return ConditionalMethodLaw.boundedOutput(
          input,
          index,
          source,
          template,
          classify,
          point.cycles(),
          exit,
          target,
          values.getOrDefault(target, CertifiedInterval.zero(precision.fractionalBits())));
    }
  }
}
