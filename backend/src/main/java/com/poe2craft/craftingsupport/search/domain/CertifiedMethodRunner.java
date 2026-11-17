package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 결정적 구간 연산으로 유한 시간의 방법 전이와 최초 성공을 계산한다.
 *
 * <p>기존 계약: Finite-horizon Markov transport with deterministic enclosing arithmetic and primitive
 * first hit.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "certified-method-markov-v1"} ({@code String}).
 *   <li>{@code template}: 템플릿 ({@code Template<S, A>}).
 *   <li>{@code transition}: 전이 ({@code Transition<S, A>}).
 *   <li>{@code classify}: 상태분류 ({@code Function<S, Optional<Exit>>}).
 *   <li>{@code bits}: 비트수 ({@code int}).
 *   <li>{@code exits}: 종료목록 ({@code Map<Exit, Map<S, CertifiedInterval>>}).
 *   <li>{@code pending}: 대기 ({@code Map<Execution<S>, CertifiedInterval>}).
 *   <li>{@code boundary}: 경계 ({@code Map<S, CertifiedInterval>}).
 *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<A, CertifiedInterval>}).
 *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<A, CertifiedInterval>}).
 *   <li>{@code absorbedMass}: 흡수된·확률질량 ({@code CertifiedInterval}).
 *   <li>{@code boundaryMass}: 경계·확률질량 ({@code CertifiedInterval}).
 *   <li>{@code initializing}: 초기화중 ({@code boolean}).
 *   <li>{@code atBoundary}: 기준·경계 ({@code boolean}).
 *   <li>{@code cycles}: 주기횟수 ({@code long}).
 *   <li>{@code absorbedEntries}: 흡수된·항목목록 ({@code long}).
 *   <li>{@code terminalFailureSentinel}: 종료·실패·Sentinel ({@code S}).
 *   <li>{@code flight}: 진행중 ({@code Flight}).
 *   <li>{@code observationLeased}: 관측값·Leased ({@code boolean}).
 *   <li>{@code CURSOR_VERSION}: 버전 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code compactTerminalFailures(sentinel)}: 압축여부·종료·Failures의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       sentinel}(sentinel). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code CertifiedMethodRunner(root, template, transition, classify, precision)}: 기본 정책을 보충해
 *       공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code template}(템플릿), {@code transition}(전이), {@code
 *       classify}(상태분류), {@code precision}(정밀도).
 *   <li>{@code CertifiedMethodRunner(root, template, transition, classify, precision,
 *       absorptionMaps)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code template}(템플릿),
 *       {@code transition}(전이), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionMaps}(흡수·맵목록).
 *   <li>{@code CertifiedMethodRunner(root, template, transition, classify, precision,
 *       absorptionMaps, workingMaps)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code root}(시작점), {@code
 *       template}(템플릿), {@code transition}(전이), {@code classify}(상태분류), {@code precision}(정밀도),
 *       {@code absorptionMaps}(흡수·맵목록), {@code workingMaps}(계산중·맵목록).
 *   <li>{@code CertifiedMethodRunner(template, transition, classify, precision, absorptionMaps,
 *       workingMaps)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code template}(템플릿), {@code transition}(전이),
 *       {@code classify}(상태분류), {@code precision}(정밀도), {@code absorptionMaps}(흡수·맵목록), {@code
 *       workingMaps}(계산중·맵목록).
 *   <li>{@code leaseCursor()}: 사용권·진행커서 값을 조회·구성한다. 반환: {@code CursorLease}.
 *   <li>{@code restoreCursor(header, template, transition, classify, precision, absorptionMaps)}:
 *       진행커서을(를) 복원한다. 입력: {@code header}(헤더), {@code template}(템플릿), {@code transition}(전이),
 *       {@code classify}(상태분류), {@code precision}(정밀도), {@code absorptionMaps}(흡수·맵목록). 반환: {@code
 *       CursorRestore<S, A>}.
 *   <li>{@code restoreCursor(header, template, transition, classify, precision, absorptionMaps,
 *       workingMaps)}: 진행커서을(를) 복원한다. 입력: {@code header}(헤더), {@code template}(템플릿), {@code
 *       transition}(전이), {@code classify}(상태분류), {@code precision}(정밀도), {@code
 *       absorptionMaps}(흡수·맵목록), {@code workingMaps}(계산중·맵목록). 반환: {@code CursorRestore<S, A>}.
 *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code resident(map)}: 메모리상주 처리에서 {@code law.residentEntries}, {@code map.size}을(를) 사용한다.
 *       입력: {@code map}(맵). 반환: {@code long}.
 *   <li>{@code closeWorkingMaps()}: close·계산중·맵목록 처리에서 {@code law.close}을(를) 사용한다.
 *   <li>{@code retention()}: 보유량 값을 조회·구성한다. 반환: {@code Retention}.
 *   <li>{@code atBoundary()}: {@code atBoundary} 값을 조회한다. 반환: {@code boolean}.
 *   <li>{@code cycles()}: {@code cycles} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code absorbed()}: 흡수된 값을 조회·구성한다. 반환: {@code boolean}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code beginCycle()}: 시작·주기 처리에서 {@code boundary.forEach}, {@code pending.put}을(를) 사용한다.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code probabilitySum(a, b)}: 확률·합계 처리에서 {@code a.add}을(를) 사용한다. 입력: {@code a}(a), {@code
 *       b}(b). 반환: {@code CertifiedInterval}.
 *   <li>{@code absorb(kind, state, mass)}: absorb 처리에서 {@code mass.zero}, {@code exits.get}을(를)
 *       사용한다. 입력: {@code kind}(종류), {@code state}(상태), {@code mass}(확률질량).
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code step(allowance)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code allowance}(allowance). 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code finish()}: 진행 중인 계산 또는 누적 처리를 마무리한다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code observation()}: 관측값 값을 조회·구성한다. 반환: {@code Observation<S, A>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code provisional(horizon, pendingState)}: provisional 처리에서 {@code exits.forEach}, {@code
 *       saved.put}을(를) 사용한다. 입력: {@code horizon}(계산기간), {@code pendingState}(대기·상태). 반환: {@code
 *       Observation<S, A>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code leaseObservation()}: 사용권·관측값 값을 조회·구성한다. 반환: {@code ObservationLease}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Observation}: {@code CertifiedMethodRunner}에서 사용하는 관측값의 불변 값이다.
 *   <li>{@code WorkingMaps}: {@code CertifiedMethodRunner}에서 사용하는 계산중·맵목록의 불변 값이다.
 *   <li>{@code Flight}: {@code CertifiedMethodRunner} 내부의 진행중 처리를 맡는다.
 *   <li>{@code CursorKind}: {@code CertifiedMethodRunner}의 진행커서·종류 구분값을 정의한다.
 *   <li>{@code CursorRow}: {@code CertifiedMethodRunner}에서 사용하는 진행커서·행의 불변 값이다.
 *   <li>{@code FlightCursor}: {@code CertifiedMethodRunner}에서 사용하는 진행중·진행커서의 불변 값이다.
 *   <li>{@code CursorHeader}: {@code CertifiedMethodRunner}에서 사용하는 진행커서·헤더의 불변 값이다.
 *   <li>{@code CursorLease}: {@code CertifiedMethodRunner} 내부의 진행커서·사용권 처리를 맡는다.
 *   <li>{@code CursorRestore}: {@code CertifiedMethodRunner} 내부의 진행커서·복원 처리를 맡는다.
 *   <li>{@code Retention}: {@code CertifiedMethodRunner}에서 사용하는 보유량의 불변 값이다.
 *   <li>{@code ObservationLease}: {@code CertifiedMethodRunner} 내부의 관측값·사용권 처리를 맡는다.
 * </ul>
 */
public final class CertifiedMethodRunner<S, A> {
  public static final String VERSION = "certified-method-markov-v1";

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 관측값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code long}).
   *   <li>{@code exits}: 종료목록 ({@code Map<Exit, Map<S, CertifiedInterval>>}).
   *   <li>{@code active}: 활성 ({@code Map<S, CertifiedInterval>}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<A, CertifiedInterval>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<A, CertifiedInterval>}).
   *   <li>{@code mass}: 확률질량 ({@code CertifiedInterval}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code atLaterAbsorbedBoundary(later)}: 기준·후속·흡수된·경계 처리에서 {@code active.isEmpty}, {@code
   *       mass.contains}을(를) 사용한다. 입력: {@code later}(후속). 반환: {@code Observation<S, A>}. 사용하는 예외:
   *       {@code IllegalArgumentException}.
   * </ul>
   */
  public record Observation<S, A>(
      long cycles,
      Map<Exit, Map<S, CertifiedInterval>> exits,
      Map<S, CertifiedInterval> active,
      Map<A, CertifiedInterval> initialUses,
      Map<A, CertifiedInterval> cycleUses,
      CertifiedInterval mass) {
    public Observation<S, A> atLaterAbsorbedBoundary(long later) {
      if (!active.isEmpty() || later < cycles || !mass.contains(Fraction.ONE))
        throw new IllegalArgumentException("Complete absorbed observation required");
      return new Observation<>(later, exits, active, initialUses, cycleUses, mass);
    }
  }

  private final Template<S, A> template;
  private final Transition<S, A> transition;
  private final Function<S, Optional<Exit>> classify;
  private final int bits;
  private final Map<Exit, Map<S, CertifiedInterval>> exits = new EnumMap<>(Exit.class);
  private final Map<Execution<S>, CertifiedInterval> pending;
  private final Map<S, CertifiedInterval> boundary;

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 계산중·맵목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code pending}: 대기 ({@code Supplier<? extends Map<Execution<S>, CertifiedInterval>>}).
   *   <li>{@code boundary}: 경계 ({@code Supplier<? extends Map<S, CertifiedInterval>>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code memory()}: 메모리 값을 조회·구성한다. 반환: {@code WorkingMaps<S>}.
   * </ul>
   */
  public record WorkingMaps<S>(
      Supplier<? extends Map<Execution<S>, CertifiedInterval>> pending,
      Supplier<? extends Map<S, CertifiedInterval>> boundary) {
    public static <S> WorkingMaps<S> memory() {
      return new WorkingMaps<>(LinkedHashMap::new, LinkedHashMap::new);
    }
  }

  private final Map<A, CertifiedInterval> initialUses = new LinkedHashMap<>(),
      cycleUses = new LinkedHashMap<>();
  private CertifiedInterval absorbedMass, boundaryMass;
  private boolean initializing, atBoundary;
  private long cycles;
  private long absorbedEntries;
  private S terminalFailureSentinel;

  /**
   * Only absorbing failure maps are compacted: no future action can inspect these items. Active,
   * checkpoint and goal states retain their complete identities.
   */
  public void compactTerminalFailures(S sentinel) {
    if (observationLeased) throw new IllegalStateException("Observation is being frozen");
    terminalFailureSentinel = Objects.requireNonNull(sentinel);
  }

  private Flight flight;
  private boolean observationLeased;

  /**
   * {@code CertifiedMethodRunner} 내부의 진행중 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code execution}: 실행 ({@code Execution<S>}).
   *   <li>{@code mass}: 확률질량 ({@code CertifiedInterval}).
   *   <li>{@code kernel}: 전이커널 ({@code Kernel<S>}).
   *   <li>{@code phases}: 단계목록 ({@code int}).
   *   <li>{@code cursor}: 진행커서 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Flight(execution, mass, kernel, phases)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       execution}(실행), {@code mass}(확률질량), {@code kernel}(전이커널), {@code phases}(단계목록).
   * </ul>
   */
  private final class Flight {
    final Execution<S> execution;
    final CertifiedInterval mass;
    final Kernel<S> kernel;
    final int phases;
    int cursor;

    Flight(Execution<S> execution, CertifiedInterval mass, Kernel<S> kernel, int phases) {
      this.execution = execution;
      this.mass = mass;
      this.kernel = kernel;
      this.phases = phases;
    }
  }

  public CertifiedMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision) {
    this(root, template, transition, classify, precision, LinkedHashMap::new);
  }

  public CertifiedMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    this(root, template, transition, classify, precision, absorptionMaps, WorkingMaps.memory());
  }

  public CertifiedMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps,
      WorkingMaps<S> workingMaps) {
    this(template, transition, classify, precision, absorptionMaps, workingMaps);
    var first = classify.apply(root);
    if (first.isPresent()) {
      absorb(first.orElseThrow(), root, CertifiedInterval.one(bits));
      atBoundary = true;
    } else if (!template.initial().isEmpty()) {
      initializing = true;
      pending.put(new Execution<>(root, 0), CertifiedInterval.one(bits));
    } else {
      boundary.put(root, CertifiedInterval.one(bits));
      boundaryMass = CertifiedInterval.one(bits);
      atBoundary = true;
    }
  }

  private CertifiedMethodRunner(
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps,
      WorkingMaps<S> workingMaps) {
    this.template = template;
    this.transition = transition;
    this.classify = classify;
    bits = precision.fractionalBits();
    pending = workingMaps.pending().get();
    boundary = workingMaps.boundary().get();
    absorbedMass = CertifiedInterval.zero(bits);
    boundaryMass = CertifiedInterval.zero(bits);
    for (var kind : Exit.values()) exits.put(kind, absorptionMaps.get());
  }

  public static final String CURSOR_VERSION = VERSION + ":streaming-cursor-v1";

  /**
   * {@code CertifiedMethodRunner}의 진행커서·종류 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code PENDING}: 대기 구분값.
   *   <li>{@code BOUNDARY}: 경계 구분값.
   *   <li>{@code EXIT}: 종료 구분값.
   *   <li>{@code INITIAL_USE}: 사용 구분값.
   *   <li>{@code CYCLE_USE}: 사용 구분값.
   *   <li>{@code KERNEL}: 전이커널 구분값.
   * </ul>
   */
  public enum CursorKind {
    PENDING,
    BOUNDARY,
    EXIT,
    INITIAL_USE,
    CYCLE_USE,
    KERNEL
  }

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 진행커서·행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code CursorKind}).
   *   <li>{@code exit}: 종료 ({@code Exit}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code S}).
   *   <li>{@code phase}: 단계 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code A}).
   *   <li>{@code mass}: 확률질량 ({@code CertifiedInterval}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   */
  public record CursorRow<S, A>(
      CursorKind kind,
      Exit exit,
      S state,
      int phase,
      A action,
      CertifiedInterval mass,
      Fraction probability) {}

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 진행중·진행커서의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code execution}: 실행 ({@code Execution<S>}).
   *   <li>{@code mass}: 확률질량 ({@code CertifiedInterval}).
   *   <li>{@code cursor}: 진행커서 ({@code int}).
   *   <li>{@code outcomeCount}: 결과·개수 ({@code int}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code phases}: 단계목록 ({@code int}).
   * </ul>
   */
  public record FlightCursor<S>(
      Execution<S> execution,
      CertifiedInterval mass,
      int cursor,
      int outcomeCount,
      Fraction unresolved,
      int phases) {}

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 진행커서·헤더의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code templateId}: 템플릿·식별자 ({@code String}).
   *   <li>{@code bits}: 비트수 ({@code int}).
   *   <li>{@code cycles}: 주기횟수 ({@code long}).
   *   <li>{@code initialPhases}: 초기값·단계목록 ({@code int}).
   *   <li>{@code cyclePhases}: 주기·단계목록 ({@code int}).
   *   <li>{@code initializing}: 초기화중 ({@code boolean}).
   *   <li>{@code atBoundary}: 기준·경계 ({@code boolean}).
   *   <li>{@code absorbedMass}: 흡수된·확률질량 ({@code CertifiedInterval}).
   *   <li>{@code boundaryMass}: 경계·확률질량 ({@code CertifiedInterval}).
   *   <li>{@code absorbedEntries}: 흡수된·항목목록 ({@code long}).
   *   <li>{@code flight}: 진행중 ({@code FlightCursor<S>}).
   * </ul>
   */
  public record CursorHeader<S>(
      String version,
      String templateId,
      int bits,
      long cycles,
      int initialPhases,
      int cyclePhases,
      boolean initializing,
      boolean atBoundary,
      CertifiedInterval absorbedMass,
      CertifiedInterval boundaryMass,
      long absorbedEntries,
      FlightCursor<S> flight) {}

  /**
   * {@code CertifiedMethodRunner} 내부의 진행커서·사용권 처리를 맡는다.
   *
   * <p>기존 계약: Streaming stable views; acquiring a cursor does not clone the retained state maps.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code closed}: 닫힘 ({@code boolean}).
   *   <li>{@code header}: 헤더 ({@code CursorHeader<S>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code CursorLease()}: 전달받은 값으로 구성원을 초기화한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code header()}: 저장 문서의 소유권·만료·초기 조건을 읽는다. 반환: {@code CursorHeader<S>}.
   *   <li>{@code rows()}: 행목록 값을 조회·구성한다. 반환: {@code Iterator<CursorRow<S, A>>}. 사용하는 예외: {@code
   *       NoSuchElementException}.
   *   <li>{@code ensureOpen()}: ensure·열린의 입력에 따른 분기·결과를 처리한다. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public final class CursorLease implements AutoCloseable {
    private boolean closed;
    private final CursorHeader<S> header;

    private CursorLease() {
      if (observationLeased) throw new IllegalStateException("Runner is already leased");
      observationLeased = true;
      header =
          new CursorHeader<>(
              CURSOR_VERSION,
              template.id(),
              bits,
              cycles,
              template.initial().size(),
              template.cycle().size(),
              initializing,
              atBoundary,
              absorbedMass,
              boundaryMass,
              absorbedEntries,
              flight == null
                  ? null
                  : new FlightCursor<>(
                      flight.execution,
                      flight.mass,
                      flight.cursor,
                      flight.kernel.outcomes().size(),
                      flight.kernel.unresolved(),
                      flight.phases));
    }

    public CursorHeader<S> header() {
      ensureOpen();
      return header;
    }

    public Iterator<CursorRow<S, A>> rows() {
      ensureOpen();
      var parts = new ArrayList<Iterator<CursorRow<S, A>>>();
      parts.add(
          pending.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.PENDING,
                          null,
                          e.getKey().state(),
                          e.getKey().phase(),
                          null,
                          e.getValue(),
                          null))
              .iterator());
      parts.add(
          boundary.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.BOUNDARY, null, e.getKey(), 0, null, e.getValue(), null))
              .iterator());
      exits.forEach(
          (kind, law) ->
              parts.add(
                  law.entrySet().stream()
                      .map(
                          e ->
                              new CursorRow<S, A>(
                                  CursorKind.EXIT, kind, e.getKey(), 0, null, e.getValue(), null))
                      .iterator()));
      parts.add(
          initialUses.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.INITIAL_USE, null, null, 0, e.getKey(), e.getValue(), null))
              .iterator());
      parts.add(
          cycleUses.entrySet().stream()
              .map(
                  e ->
                      new CursorRow<S, A>(
                          CursorKind.CYCLE_USE, null, null, 0, e.getKey(), e.getValue(), null))
              .iterator());
      if (flight != null)
        parts.add(
            flight.kernel.outcomes().stream()
                .map(
                    o ->
                        new CursorRow<S, A>(
                            CursorKind.KERNEL, null, o.state(), 0, null, null, o.probability()))
                .iterator());
      return new Iterator<>() {
        int part;

        public boolean hasNext() {
          ensureOpen();
          while (part < parts.size() && !parts.get(part).hasNext()) part++;
          return part < parts.size();
        }

        public CursorRow<S, A> next() {
          if (!hasNext()) throw new NoSuchElementException();
          return parts.get(part).next();
        }
      };
    }

    private void ensureOpen() {
      if (closed) throw new IllegalStateException("Cursor lease closed");
    }

    public void close() {
      if (!closed) {
        closed = true;
        observationLeased = false;
      }
    }
  }

  public CursorLease leaseCursor() {
    return new CursorLease();
  }

  /** Restores rows in their saved order, without executing or charging another primitive action. */
  public static <S, A> CursorRestore<S, A> restoreCursor(
      CursorHeader<S> header,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    return restoreCursor(
        header, template, transition, classify, precision, absorptionMaps, WorkingMaps.memory());
  }

  public static <S, A> CursorRestore<S, A> restoreCursor(
      CursorHeader<S> header,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps,
      WorkingMaps<S> workingMaps) {
    return new CursorRestore<>(
        header, template, transition, classify, precision, absorptionMaps, workingMaps);
  }

  /**
   * {@code CertifiedMethodRunner} 내부의 진행커서·복원 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code header}: 헤더 ({@code CursorHeader<S>}).
   *   <li>{@code runner}: 실행기 ({@code CertifiedMethodRunner<S, A>}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome<S>>}).
   *   <li>{@code finished}: 종료 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code CursorRestore(header, template, transition, classify, precision, maps,
   *       workingMaps)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code header}(헤더), {@code template}(템플릿), {@code
   *       transition}(전이), {@code classify}(상태분류), {@code precision}(정밀도), {@code maps}(맵목록),
   *       {@code workingMaps}(계산중·맵목록). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code probability(mass)}: 현재 조건의 확률을 조회한다. 입력: {@code mass}(확률질량). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code put(target, key, mass)}: 키에 해당하는 값을 기록한다. 입력: {@code target}(대상), {@code key}(키),
   *       {@code mass}(확률질량). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code accept(row)}: 수락 처리에서 {@code row.kind}, {@code row.mass}을(를) 사용한다. 입력: {@code
   *       row}(행). 사용하는 예외: {@code IllegalStateException}, {@code IllegalArgumentException}.
   *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   *   <li>{@code sum(values)}: 항목들의 합을 계산한다. 입력: {@code values}(값목록). 반환: {@code
   *       CertifiedInterval}.
   *   <li>{@code finish()}: 진행 중인 계산 또는 누적 처리를 마무리한다. 반환: {@code CertifiedMethodRunner<S, A>}. 사용하는
   *       예외: {@code IllegalStateException}, {@code IllegalArgumentException}.
   * </ul>
   */
  public static final class CursorRestore<S, A> implements AutoCloseable {
    private final CursorHeader<S> header;
    private final CertifiedMethodRunner<S, A> runner;
    private final List<Outcome<S>> outcomes = new ArrayList<>();
    private boolean finished;

    private CursorRestore(
        CursorHeader<S> header,
        Template<S, A> template,
        Transition<S, A> transition,
        Function<S, Optional<Exit>> classify,
        CertifiedInterval.Precision precision,
        Supplier<? extends Map<S, CertifiedInterval>> maps,
        WorkingMaps<S> workingMaps) {
      if (!CURSOR_VERSION.equals(header.version())
          || !template.id().equals(header.templateId())
          || precision.fractionalBits() != header.bits()
          || header.cycles() < 0
          || template.initial().size() != header.initialPhases()
          || template.cycle().size() != header.cyclePhases()
          || header.initializing()
              && (header.cycles() != 0 || header.initialPhases() == 0 || header.atBoundary()))
        throw new IllegalArgumentException("Incompatible method cursor");
      this.header = header;
      runner =
          new CertifiedMethodRunner<>(template, transition, classify, precision, maps, workingMaps);
      runner.cycles = header.cycles();
      runner.initializing = header.initializing();
      runner.atBoundary = header.atBoundary();
      probability(header.absorbedMass());
      probability(header.boundaryMass());
    }

    private void probability(CertifiedInterval mass) {
      if (mass == null
          || mass.fractionalBits() != header.bits()
          || mass.upperUnits().compareTo(java.math.BigInteger.ONE.shiftLeft(header.bits())) > 0)
        throw new IllegalArgumentException("Invalid cursor probability");
    }

    private <K> void put(Map<K, CertifiedInterval> target, K key, CertifiedInterval mass) {
      if (key == null
          || mass == null
          || mass.fractionalBits() != header.bits()
          || target.putIfAbsent(key, mass) != null)
        throw new IllegalArgumentException("Invalid or duplicate cursor row");
    }

    public void accept(CursorRow<S, A> row) {
      if (finished) throw new IllegalStateException("Cursor restoration finished");
      switch (row.kind()) {
        case PENDING -> {
          probability(row.mass());
          int phases = header.initializing() ? header.initialPhases() : header.cyclePhases();
          if (row.state() == null || row.phase() < 0 || row.phase() >= phases)
            throw new IllegalArgumentException("Invalid pending phase");
          put(runner.pending, new Execution<>(row.state(), row.phase()), row.mass());
        }
        case BOUNDARY -> {
          probability(row.mass());
          put(runner.boundary, row.state(), row.mass());
        }
        case EXIT -> {
          probability(row.mass());
          if (row.exit() == null) throw new IllegalArgumentException("Missing exit kind");
          put(runner.exits.get(row.exit()), row.state(), row.mass());
          runner.absorbedEntries++;
        }
        case INITIAL_USE -> put(runner.initialUses, row.action(), row.mass());
        case CYCLE_USE -> put(runner.cycleUses, row.action(), row.mass());
        case KERNEL -> {
          if (header.flight() == null || outcomes.size() >= header.flight().outcomeCount())
            throw new IllegalArgumentException("Unexpected kernel row");
          outcomes.add(new Outcome<>(row.state(), row.probability()));
        }
      }
    }

    public long retainedStates() {
      return runner.retainedStates() + outcomes.size();
    }

    @Override
    public void close() {
      if (finished) return;
      finished = true;
      runner.pending.clear();
      runner.boundary.clear();
      runner.closeWorkingMaps();
      outcomes.clear();
      for (var law : runner.exits.values()) {
        if (law instanceof CertifiedMutableLawStore.Law backed) backed.close();
        else law.clear();
      }
    }

    private CertifiedInterval sum(Collection<CertifiedInterval> values) {
      var total = CertifiedInterval.zero(header.bits());
      for (var value : values) total = total.add(value).probability();
      return total;
    }

    public CertifiedMethodRunner<S, A> finish() {
      if (finished) throw new IllegalStateException("Cursor restoration finished");
      var absorbed = CertifiedInterval.zero(header.bits());
      for (var law : runner.exits.values())
        absorbed = absorbed.add(sum(law.values())).probability();
      var boundary = sum(runner.boundary.values());
      if (!absorbed.equals(header.absorbedMass())
          || !boundary.equals(header.boundaryMass())
          || runner.absorbedEntries != header.absorbedEntries())
        throw new IllegalArgumentException("Cursor accumulator mismatch");
      runner.absorbedMass = absorbed;
      runner.boundaryMass = boundary;
      var total = absorbed.add(boundary).add(sum(runner.pending.values())).probability();
      if (header.flight() != null) {
        var saved = header.flight();
        int phases = header.initializing() ? header.initialPhases() : header.cyclePhases();
        probability(saved.mass());
        if (saved.execution() == null
            || saved.execution().state() == null
            || saved.execution().phase() < 0
            || saved.execution().phase() >= phases
            || saved.phases() != phases
            || saved.cursor() < 0
            || saved.cursor() >= saved.outcomeCount()
            || outcomes.size() != saved.outcomeCount())
          throw new IllegalArgumentException("Invalid flight cursor");
        var kernel = new Kernel<>(outcomes, saved.unresolved(), false);
        runner.flight = runner.new Flight(saved.execution(), saved.mass(), kernel, phases);
        runner.flight.cursor = saved.cursor();
        for (int i = saved.cursor(); i < outcomes.size(); i++)
          total =
              total
                  .add(
                      saved
                          .mass()
                          .multiply(
                              CertifiedInterval.enclose(
                                  outcomes.get(i).probability(), header.bits()))
                          .probability())
                  .probability();
      }
      if (!total.contains(Fraction.ONE)
          || header.atBoundary() && (!runner.pending.isEmpty() || runner.flight != null)
          || !header.atBoundary() && runner.pending.isEmpty() && runner.flight == null)
        throw new IllegalArgumentException("Invalid cursor mass or phase");
      outcomes.clear();
      finished = true;
      return runner;
    }
  }

  public long retainedStates() {
    long residentExits = 0;
    for (var law : exits.values())
      if (!(law instanceof CertifiedMutableLawStore.Law)) residentExits += law.size();
    return resident(pending) + resident(boundary) + residentExits;
  }

  private static long resident(Map<?, CertifiedInterval> map) {
    return map instanceof CertifiedWorkingLaw<?> law ? law.residentEntries() : map.size();
  }

  public void closeWorkingMaps() {
    if (pending instanceof CertifiedWorkingLaw<?> law) law.close();
    if (boundary instanceof CertifiedWorkingLaw<?> law) law.close();
  }

  /**
   * {@code CertifiedMethodRunner}에서 사용하는 보유량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code pending}: 대기 ({@code long}).
   *   <li>{@code boundary}: 경계 ({@code long}).
   *   <li>{@code exits}: 종료목록 ({@code long}).
   *   <li>{@code flightOutcomes}: 진행중·결과목록 ({@code long}).
   * </ul>
   */
  public record Retention(long pending, long boundary, long exits, long flightOutcomes) {}

  public Retention retention() {
    return new Retention(
        pending.size(),
        boundary.size(),
        absorbedEntries,
        flight == null ? 0 : flight.kernel.outcomes().size());
  }

  public boolean atBoundary() {
    return atBoundary;
  }

  public long cycles() {
    return cycles;
  }

  public boolean absorbed() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle");
    return boundary.isEmpty();
  }

  public void beginCycle() {
    if (observationLeased) throw new IllegalStateException("Observation is being frozen");
    if (!atBoundary || cycles == Long.MAX_VALUE)
      throw new IllegalStateException("Cannot begin cycle");
    boundary.forEach((state, mass) -> pending.put(new Execution<>(state, 0), mass));
    boundary.clear();
    boundaryMass = CertifiedInterval.zero(bits);
    atBoundary = false;
    if (pending.isEmpty()) finish();
  }

  private static CertifiedInterval probabilitySum(CertifiedInterval a, CertifiedInterval b) {
    return a.add(b).probability();
  }

  private void absorb(Exit kind, S state, CertifiedInterval mass) {
    if (mass.zero()) return;
    if (terminalFailureSentinel != null && (kind == Exit.LOSS || kind == Exit.DEAD))
      state = terminalFailureSentinel;
    var law = exits.get(kind);
    int before = law.size();
    law.merge(state, mass, CertifiedMethodRunner::probabilitySum);
    absorbedEntries += law.size() - before;
    absorbedMass = absorbedMass.add(mass).probability();
  }

  public void step() {
    step(32);
  }

  public void step(int allowance) {
    if (observationLeased) throw new IllegalStateException("Observation is being frozen");
    if (allowance < 1 || atBoundary)
      throw new IllegalStateException("Active phase and positive allowance required");
    if (flight == null) {
      var entry =
          pending instanceof CertifiedWorkingLaw<Execution<S>> law
              ? law.firstEntry()
              : pending.entrySet().iterator().next();
      var execution = entry.getKey();
      var mass = entry.getValue();
      var actions = initializing ? template.initial() : template.cycle();
      var action = Objects.requireNonNull(actions.get(execution.phase()).apply(execution.state()));
      var kernel = transition.apply(execution.state(), action);
      // A suspended lookup changes neither cost nor the execution cursor.
      pending.remove(execution);
      if (kernel.unavailable()) {
        absorb(Exit.DEAD, execution.state(), mass);
        if (pending.isEmpty()) finish();
        return;
      }
      (initializing ? initialUses : cycleUses).merge(action, mass, CertifiedInterval::add);
      absorb(
          Exit.UNRESOLVED,
          execution.state(),
          mass.multiply(CertifiedInterval.enclose(kernel.unresolved(), bits)).probability());
      flight = new Flight(execution, mass, kernel, actions.size());
    }
    for (int n = 0; n < allowance && flight.cursor < flight.kernel.outcomes().size(); n++) {
      var outcome = flight.kernel.outcomes().get(flight.cursor++);
      var mass =
          flight
              .mass
              .multiply(CertifiedInterval.enclose(outcome.probability(), bits))
              .probability();
      if (mass.zero()) continue;
      var terminal = classify.apply(outcome.state());
      if (terminal.isPresent()) absorb(terminal.orElseThrow(), outcome.state(), mass);
      else if (flight.execution.phase() + 1 == flight.phases) {
        boundary.merge(outcome.state(), mass, CertifiedMethodRunner::probabilitySum);
        boundaryMass = boundaryMass.add(mass).probability();
      } else
        pending.merge(
            new Execution<>(outcome.state(), flight.execution.phase() + 1),
            mass,
            CertifiedMethodRunner::probabilitySum);
    }
    if (flight.cursor == flight.kernel.outcomes().size()) {
      flight = null;
      if (pending.isEmpty()) finish();
    }
  }

  private void finish() {
    if (!absorbedMass.add(boundaryMass).contains(Fraction.ONE))
      throw new IllegalStateException("Mass outside arithmetic enclosure");
    if (initializing) initializing = false;
    else cycles++;
    atBoundary = true;
  }

  public Observation<S, A> observation() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle");
    var saved = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
    exits.forEach((kind, law) -> saved.put(kind, Map.copyOf(law)));
    return new Observation<>(
        cycles,
        Map.copyOf(saved),
        Map.copyOf(boundary),
        Map.copyOf(initialUses),
        Map.copyOf(cycleUses),
        absorbedMass.add(boundaryMass));
  }

  /**
   * Conservative first-hit observation while a cycle is still running. Unprocessed mass is unknown.
   */
  public Observation<S, A> provisional(long horizon, S pendingState) {
    if (horizon < cycles) throw new IllegalArgumentException("Cannot project an earlier horizon");
    var saved = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
    exits.forEach((kind, law) -> saved.put(kind, new LinkedHashMap<>(law)));
    var remaining = absorbedMass.probability().complement();
    if (!remaining.zero())
      saved.get(Exit.UNRESOLVED).merge(pendingState, remaining, CertifiedInterval::add);
    return new Observation<>(
        horizon,
        saved,
        Map.of(),
        Map.copyOf(initialUses),
        Map.copyOf(cycleUses),
        absorbedMass.add(remaining));
  }

  /**
   * {@code CertifiedMethodRunner} 내부의 관측값·사용권 처리를 맡는다.
   *
   * <p>기존 계약: Internal immutable view lease: advancement is blocked until the reader releases it.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code point}: 지점 ({@code Observation<S, A>}).
   *   <li>{@code closed}: 닫힘 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code ObservationLease()}: 전달받은 값으로 구성원을 초기화한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code point()}: 지점 값을 조회·구성한다. 반환: {@code Observation<S, A>}. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  final class ObservationLease implements AutoCloseable {
    private final Observation<S, A> point;
    private boolean closed;

    private ObservationLease() {
      if (!atBoundary || observationLeased)
        throw new IllegalStateException("Boundary lease unavailable");
      observationLeased = true;
      var view = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
      exits.forEach((kind, law) -> view.put(kind, Collections.unmodifiableMap(law)));
      point =
          new Observation<>(
              cycles,
              Map.copyOf(view),
              Collections.unmodifiableMap(boundary),
              Map.copyOf(initialUses),
              Map.copyOf(cycleUses),
              absorbedMass.add(boundaryMass));
    }

    Observation<S, A> point() {
      if (closed) throw new IllegalStateException("Observation lease closed");
      return point;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        observationLeased = false;
      }
    }
  }

  ObservationLease leaseObservation() {
    return new ObservationLease();
  }
}
