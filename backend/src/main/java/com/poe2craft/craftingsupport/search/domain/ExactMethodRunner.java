package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 완전한 불변 상태를 사용해 유한 관측의 정확한 마르코프 전이를 계산한다.
 *
 * <p>기존 계약: Finite-observation Markov calculation. S must be the full immutable transition state.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "method-cycle-bounded-phase-kernels-v7"} ({@code String}).
 *   <li>{@code PHASE_KERNEL_WINDOW}: 시간창; 초기값 {@code 32} ({@code int}).
 *   <li>{@code template}: 템플릿 ({@code Template<S, A>}).
 *   <li>{@code transition}: 전이 ({@code Transition<S, A>}).
 *   <li>{@code classify}: 상태분류 ({@code Function<S, Optional<Exit>>}).
 *   <li>{@code compact}: 압축여부 ({@code Function<Map<S, Fraction>, Compaction<S>>}).
 *   <li>{@code normalizeTerminal}: 정규화·종료 ({@code Function<Map<S, Fraction>, Compaction<S>>}).
 *   <li>{@code terminalQueue}: 종료·대기열 ({@code ArrayDeque<PendingExit<S>>}).
 *   <li>{@code terminalNormalization}: 종료·Normalization ({@code Compaction<S>}).
 *   <li>{@code terminalResults}: 종료·결과목록 ({@code Iterator<Map.Entry<S, Fraction>>}).
 *   <li>{@code finishingBoundary}: finishing·경계 ({@code boolean}).
 *   <li>{@code compactionQueue}: compaction·대기열 ({@code ArrayDeque<Map<S, Fraction>>}).
 *   <li>{@code compaction}: compaction ({@code Compaction<S>}).
 *   <li>{@code exits}: 종료목록 ({@code Map<Exit, Map<S, Fraction>>}).
 *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<A, Fraction>}).
 *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<A, Fraction>}).
 *   <li>{@code pending}: 대기 ({@code LinkedHashMap<Execution<S>, Fraction>}).
 *   <li>{@code boundary}: 경계 ({@code Map<S, Fraction>}).
 *   <li>{@code initializing}: 초기화중 ({@code boolean}).
 *   <li>{@code atBoundary}: 기준·경계 ({@code boolean}).
 *   <li>{@code cycles}: 주기횟수 ({@code long}).
 *   <li>{@code flight}: 진행중 ({@code Flight}).
 *   <li>{@code phaseGroups}: 단계·그룹목록 ({@code Map<WorkKey, Flight>}).
 *   <li>{@code phaseReady}: 단계·준비완료 ({@code boolean}).
 *   <li>{@code groupedInputs}: 묶인·입력목록 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code groupedInputs()}: {@code groupedInputs} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code retainedPhaseKernels()}: 보유·단계·전이커널목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code ExactMethodRunner(root, template, transition, classify)}: 기본 정책을 보충해 공통 생성자로 위임한다.
 *       입력: {@code root}(시작점), {@code template}(템플릿), {@code transition}(전이), {@code
 *       classify}(상태분류).
 *   <li>{@code ExactMethodRunner(root, template, transition, classify, compact)}: 기본 정책을 보충해 공통
 *       생성자로 위임한다. 입력: {@code root}(시작점), {@code template}(템플릿), {@code transition}(전이), {@code
 *       classify}(상태분류), {@code compact}(압축여부).
 *   <li>{@code ExactMethodRunner(root, template, transition, classify, compact,
 *       normalizeTerminal)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code root}(시작점), {@code template}(템플릿),
 *       {@code transition}(전이), {@code classify}(상태분류), {@code compact}(압축여부), {@code
 *       normalizeTerminal}(정규화·종료).
 *   <li>{@code beginCycle()}: 시작·주기 처리에서 {@code boundary.forEach}, {@code pending.put}을(를) 사용한다.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code step(maxOutcomes)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code maxOutcomes}(최대값·결과목록). 사용하는 예외:
 *       {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code finishBoundary()}: 완료·경계 처리에서 {@code terminalQueue.isEmpty}, {@code
 *       boundary.size}을(를) 사용한다.
 *   <li>{@code endBoundary()}: end·경계의 입력에 따른 분기·결과를 처리한다.
 *   <li>{@code atBoundary()}: {@code atBoundary} 값을 조회한다. 반환: {@code boolean}.
 *   <li>{@code observation()}: 관측값 값을 조회·구성한다. 반환: {@code Observation<S, A>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code boundarySnapshot()}: 경계·스냅샷 값을 조회·구성한다. 반환: {@code Observation<S, A>}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code absorb(exit, state, mass)}: absorb 처리에서 {@code mass.equals}, {@code exits.get}을(를)
 *       사용한다. 입력: {@code exit}(종료), {@code state}(상태), {@code mass}(확률질량).
 *   <li>{@code stepTerminal(maxOutcomes)}: 단계·종료 처리에서 {@code terminalQueue.getFirst}, {@code
 *       pendingExit.normalization}을(를) 사용한다. 입력: {@code maxOutcomes}(최대값·결과목록).
 *   <li>{@code probability(value)}: 현재 조건의 확률을 조회한다. 입력: {@code value}(값). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Compaction}: {@code ExactMethodRunner} 내부의 Compaction 처리를 맡는다.
 *   <li>{@code Exit}: {@code ExactMethodRunner}의 종료 구분값을 정의한다.
 *   <li>{@code Outcome}: {@code ExactMethodRunner}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Kernel}: {@code ExactMethodRunner}에서 사용하는 전이커널의 불변 값이다.
 *   <li>{@code Transition}: {@code ExactMethodRunner} 내부의 전이 처리를 맡는다.
 *   <li>{@code Template}: {@code ExactMethodRunner}에서 사용하는 템플릿의 불변 값이다.
 *   <li>{@code Execution}: {@code ExactMethodRunner}에서 사용하는 실행의 불변 값이다.
 *   <li>{@code Observation}: {@code ExactMethodRunner}에서 사용하는 관측값의 불변 값이다.
 *   <li>{@code PendingExit}: {@code ExactMethodRunner}에서 사용하는 대기·종료의 불변 값이다.
 *   <li>{@code WorkKey}: {@code ExactMethodRunner} 내부의 계산작업·키 처리를 맡는다.
 *   <li>{@code Flight}: {@code ExactMethodRunner} 내부의 진행중 처리를 맡는다.
 * </ul>
 */
public final class ExactMethodRunner<S, A> {
  public static final String VERSION = "method-cycle-bounded-phase-kernels-v7";
  static final int PHASE_KERNEL_WINDOW = 32;

  /**
   * {@code ExactMethodRunner} 내부의 Compaction 처리를 맡는다.
   *
   * <p>기존 계약: A law-preserving normalization of one mutually exclusive distribution, never
   * alternatives. Selectors and classifiers must have identical semantics on every represented
   * member. The application uses constant currency selectors and compiled goal-equivalent regions.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code step(maxInputs)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code maxInputs}(최대값·입력목록).
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Map<S, Fraction>}.
   * </ul>
   */
  public interface Compaction<S> {
    boolean complete();

    void step(int maxInputs);

    Map<S, Fraction> result();
  }

  /**
   * {@code ExactMethodRunner}의 종료 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code GOAL}: 목표 구분값.
   *   <li>{@code CHECKPOINT}: 체크포인트 구분값.
   *   <li>{@code LOSS}: LOSS 구분값.
   *   <li>{@code DEAD}: 종료된 구분값.
   *   <li>{@code UNRESOLVED}: 미해결 구분값.
   *   <li>{@code REBASE}: 기준변경 구분값.
   * </ul>
   */
  public enum Exit {
    GOAL,
    CHECKPOINT,
    LOSS,
    DEAD,
    UNRESOLVED,
    REBASE
  }

  /**
   * {@code ExactMethodRunner}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code S}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Outcome(state, probability)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code state}(상태), {@code
   *       probability}(확률).
   * </ul>
   */
  public record Outcome<S>(S state, Fraction probability) {
    public Outcome {
      Objects.requireNonNull(state);
      ExactMethodRunner.probability(probability);
    }
  }

  /**
   * {@code ExactMethodRunner}에서 사용하는 전이커널의 불변 값이다.
   *
   * <p>기존 계약: Missing support is explicit mass, not a failure or an omitted display row.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome<S>>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Kernel(outcomes, unresolved, unavailable)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       outcomes}(결과목록), {@code unresolved}(미해결), {@code unavailable}(사용불가). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Kernel<S>(List<Outcome<S>> outcomes, Fraction unresolved, boolean unavailable) {
    public Kernel {
      outcomes = List.copyOf(outcomes);
      probability(unresolved);
      var total = unresolved;
      for (var outcome : outcomes) total = total.add(outcome.probability());
      if (unavailable
          ? !outcomes.isEmpty() || !unresolved.equals(Fraction.ZERO)
          : !total.equals(Fraction.ONE)) {
        throw new IllegalArgumentException("Kernel must conserve mass");
      }
    }
  }

  /**
   * {@code ExactMethodRunner} 내부의 전이 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code apply(state, action)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code state}(상태), {@code
   *       action}(행동). 반환: {@code Kernel<S>}.
   * </ul>
   */
  @FunctionalInterface
  public interface Transition<S, A> {
    Kernel<S> apply(S state, A action);
  }

  /**
   * {@code ExactMethodRunner}에서 사용하는 템플릿의 불변 값이다.
   *
   * <p>기존 계약: An action selector may branch on the exact result of the preceding primitive action.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code initial}: 초기값 ({@code List<Function<S, A>>}).
   *   <li>{@code cycle}: 주기 ({@code List<Function<S, A>>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Template(id, initial, cycle)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       initial}(초기값), {@code cycle}(주기). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Template<S, A>(
      String id, List<Function<S, A>> initial, List<Function<S, A>> cycle) {
    public Template {
      if (id == null || id.isBlank() || cycle.isEmpty())
        throw new IllegalArgumentException("Template identity and nonempty cycle required");
      initial = List.copyOf(initial);
      cycle = List.copyOf(cycle);
    }
  }

  /**
   * {@code ExactMethodRunner}에서 사용하는 실행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code S}).
   *   <li>{@code phase}: 단계 ({@code int}).
   * </ul>
   */
  public record Execution<S>(S state, int phase) {}

  /**
   * {@code ExactMethodRunner}에서 사용하는 관측값의 불변 값이다.
   *
   * <p>기존 계약: Usage is expected actual consumption, including paths absorbed before the cycle ends.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code long}).
   *   <li>{@code exits}: 종료목록 ({@code Map<Exit, Map<S, Fraction>>}).
   *   <li>{@code active}: 활성 ({@code Map<S, Fraction>}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<A, Fraction>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<A, Fraction>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code atLaterAbsorbedBoundary(boundaryCycles)}: 기준·후속·흡수된·경계 처리에서 {@code
   *       active.isEmpty}을(를) 사용한다. 입력: {@code boundaryCycles}(경계·주기횟수). 반환: {@code Observation<S,
   *       A>}. 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code mass()}: 현재 분포의 확률 질량을 구한다. 반환: {@code Fraction}.
   * </ul>
   */
  public record Observation<S, A>(
      long cycles,
      Map<Exit, Map<S, Fraction>> exits,
      Map<S, Fraction> active,
      Map<A, Fraction> initialUses,
      Map<A, Fraction> cycleUses) {
    public Observation<S, A> atLaterAbsorbedBoundary(long boundaryCycles) {
      if (!active.isEmpty() || boundaryCycles < cycles || !mass().equals(Fraction.ONE))
        throw new IllegalArgumentException("Only a later absorbed observation may be filled");
      return new Observation<>(boundaryCycles, exits, active, initialUses, cycleUses);
    }

    public Fraction mass() {
      var total = Fraction.ZERO;
      for (var distribution : exits.values())
        for (var value : distribution.values()) total = total.add(value);
      for (var value : active.values()) total = total.add(value);
      return total;
    }
  }

  private final Template<S, A> template;
  private final Transition<S, A> transition;
  private final Function<S, Optional<Exit>> classify;
  private final Function<Map<S, Fraction>, Compaction<S>> compact;
  private final Function<Map<S, Fraction>, Compaction<S>> normalizeTerminal;

  /**
   * {@code ExactMethodRunner}에서 사용하는 대기·종료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code Exit}).
   *   <li>{@code normalization}: normalization ({@code Compaction<S>}).
   * </ul>
   */
  private record PendingExit<S>(Exit kind, Compaction<S> normalization) {}

  private final ArrayDeque<PendingExit<S>> terminalQueue = new ArrayDeque<>();
  private Compaction<S> terminalNormalization;
  private Iterator<Map.Entry<S, Fraction>> terminalResults;
  private boolean finishingBoundary;
  private final ArrayDeque<Map<S, Fraction>> compactionQueue = new ArrayDeque<>();
  private Compaction<S> compaction;
  private final Map<Exit, Map<S, Fraction>> exits = new EnumMap<>(Exit.class);
  private final Map<A, Fraction> initialUses = new LinkedHashMap<>();
  private final Map<A, Fraction> cycleUses = new LinkedHashMap<>();
  private LinkedHashMap<Execution<S>, Fraction> pending = new LinkedHashMap<>();
  private final Map<S, Fraction> boundary = new LinkedHashMap<>();
  private boolean initializing;
  private boolean atBoundary;
  private long cycles;
  private Flight flight;
  private final Map<WorkKey, Flight> phaseGroups = new LinkedHashMap<>();
  private boolean phaseReady;
  private long groupedInputs;

  // The job's proven kernel cache supplies the same immutable object only for equal complete
  // transition laws. Reference equality avoids repeatedly hashing a large outcome distribution.
  /**
   * {@code ExactMethodRunner} 내부의 계산작업·키 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code phase}: 단계 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code A}).
   *   <li>{@code kernel}: 전이커널 ({@code Kernel<S>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code WorkKey(phase, action, kernel)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code phase}(단계),
   *       {@code action}(행동), {@code kernel}(전이커널).
   *   <li>{@code hashCode()}: 동등성 비교와 일치하는 해시를 반환한다. 반환: {@code int}.
   *   <li>{@code equals(other)}: 값의 동등성을 비교한다. 입력: {@code other}(비교대상). 반환: {@code boolean}.
   * </ul>
   */
  private final class WorkKey {
    final int phase;
    final A action;
    final Kernel<S> kernel;

    WorkKey(int phase, A action, Kernel<S> kernel) {
      this.phase = phase;
      this.action = action;
      this.kernel = kernel;
    }

    @Override
    public int hashCode() {
      return Objects.hash(phase, action, System.identityHashCode(kernel));
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || other.getClass() != getClass()) return false;
      @SuppressWarnings("unchecked")
      var key = (ExactMethodRunner<S, A>.WorkKey) other;
      return phase == key.phase && action.equals(key.action) && kernel == key.kernel;
    }
  }

  public long groupedInputs() {
    return groupedInputs;
  }

  int retainedPhaseKernels() {
    return phaseGroups.size() + (flight == null ? 0 : 1);
  }

  /**
   * {@code ExactMethodRunner} 내부의 진행중 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code execution}: 실행 ({@code Execution<S>}).
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
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
    Fraction mass;
    final Kernel<S> kernel;
    final int phases;
    int cursor;

    Flight(Execution<S> execution, Fraction mass, Kernel<S> kernel, int phases) {
      this.execution = execution;
      this.mass = mass;
      this.kernel = kernel;
      this.phases = phases;
    }
  }

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify) {
    this(root, template, transition, classify, null);
  }

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      Function<Map<S, Fraction>, Compaction<S>> compact) {
    this(root, template, transition, classify, compact, null);
  }

  public ExactMethodRunner(
      S root,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classify,
      Function<Map<S, Fraction>, Compaction<S>> compact,
      Function<Map<S, Fraction>, Compaction<S>> normalizeTerminal) {
    this.template = Objects.requireNonNull(template);
    this.transition = Objects.requireNonNull(transition);
    this.classify = Objects.requireNonNull(classify);
    this.compact = compact;
    this.normalizeTerminal = normalizeTerminal;
    for (var exit : Exit.values()) exits.put(exit, new LinkedHashMap<>());
    initializing = !template.initial().isEmpty();
    var terminal = classify.apply(root);
    if (terminal.isPresent()) {
      absorb(terminal.get(), root, Fraction.ONE);
      if (normalizeTerminal == null) {
        initializing = false;
        atBoundary = true;
      } else {
        initializing = true; // Resolving an already terminal root consumes no cycle or currency.
        finishBoundary();
      }
    } else if (initializing) {
      pending.put(new Execution<>(root, 0), Fraction.ONE);
    } else {
      boundary.put(root, Fraction.ONE);
      atBoundary = true;
    }
  }

  /** Start one additional cycle. Observation 100 means 100 cycles after initial preparation. */
  public void beginCycle() {
    if (!atBoundary)
      throw new IllegalStateException("Finish current cycle or initialization first");
    if (cycles == Long.MAX_VALUE) throw new IllegalStateException("Cycle counter overflow");
    boundary.forEach((state, mass) -> pending.put(new Execution<>(state, 0), mass));
    boundary.clear();
    atBoundary = false;
    if (pending.isEmpty()) finishBoundary();
  }

  /** A bounded portion of one full-state/phase expansion, retaining its outcome cursor. */
  public void step() {
    step(32);
  }

  public void step(int maxOutcomes) {
    if (maxOutcomes < 1) throw new IllegalArgumentException("Positive outcome slice required");
    if (atBoundary) throw new IllegalStateException("Begin a cycle before stepping");
    if (!terminalQueue.isEmpty()) {
      stepTerminal(maxOutcomes);
      return;
    }
    if (!compactionQueue.isEmpty()) {
      if (compaction == null) compaction = compact.apply(compactionQueue.getFirst());
      if (!compaction.complete()) compaction.step(maxOutcomes);
      if (compaction.complete()) {
        var target = compactionQueue.removeFirst();
        var result = compaction.result();
        if (result != target) {
          target.clear();
          target.putAll(result);
        }
        compaction = null;
        if (compactionQueue.isEmpty()) endBoundary();
      }
      return;
    }
    if (flight == null && !phaseReady) {
      var entry = pending.entrySet().iterator().next();
      var execution = entry.getKey();
      var mass = entry.getValue();
      var actions = initializing ? template.initial() : template.cycle();
      var action = Objects.requireNonNull(actions.get(execution.phase()).apply(execution.state()));
      // A suspended/failed kernel lookup commits neither cost nor phase progress.
      var kernel = transition.apply(execution.state(), action);
      pending.remove(execution);
      if (kernel.unavailable()) {
        absorb(Exit.DEAD, execution.state(), mass);
      } else {
        var key = new WorkKey(execution.phase(), action, kernel);
        var group = phaseGroups.get(key);
        if (group == null)
          phaseGroups.put(key, new Flight(execution, mass, kernel, actions.size()));
        else {
          group.mass = group.mass.add(mass);
          groupedInputs++;
        }
        (initializing ? initialUses : cycleUses).merge(action, mass, Fraction::add);
        // Keep source-conditioned unknown exits separate even when their known transitions share.
        absorb(Exit.UNRESOLVED, execution.state(), mass.multiply(kernel.unresolved()));
      }
      phaseReady =
          phaseGroups.size() >= PHASE_KERNEL_WINDOW
              || pending.isEmpty()
              || pending.keySet().iterator().next().phase() != execution.phase();
      if (!phaseReady) return;
      if (phaseGroups.isEmpty()) {
        phaseReady = false;
        if (pending.isEmpty()) finishBoundary();
        return;
      }
    }
    if (flight == null) {
      var group = phaseGroups.entrySet().iterator().next();
      flight = group.getValue();
      phaseGroups.remove(group.getKey());
    }
    int consumed = 0;
    while (flight.cursor < flight.kernel.outcomes().size() && consumed < maxOutcomes) {
      var outcome = flight.kernel.outcomes().get(flight.cursor);
      var terminal = classify.apply(outcome.state());
      var weight = flight.mass.multiply(outcome.probability());
      if (!weight.equals(Fraction.ZERO)) {
        // First hit precedes the next primitive, including between slices.
        if (terminal.isPresent()) absorb(terminal.get(), outcome.state(), weight);
        else if (flight.execution.phase() + 1 == flight.phases)
          boundary.merge(outcome.state(), weight, Fraction::add);
        else
          pending.merge(
              new Execution<>(outcome.state(), flight.execution.phase() + 1),
              weight,
              Fraction::add);
      }
      flight.cursor++;
      consumed++;
    }
    if (flight.cursor == flight.kernel.outcomes().size()) {
      flight = null;
      if (phaseGroups.isEmpty()) {
        phaseReady = false;
        if (pending.isEmpty()) finishBoundary();
      }
    }
  }

  private void finishBoundary() {
    if (!terminalQueue.isEmpty()) {
      finishingBoundary = true;
      return;
    }
    finishingBoundary = false;
    if (compact != null) {
      if (boundary.size() > 1) compactionQueue.add(boundary);
      if (normalizeTerminal == null)
        for (var values : exits.values()) if (values.size() > 1) compactionQueue.add(values);
    }
    if (compactionQueue.isEmpty()) endBoundary();
  }

  private void endBoundary() {
    if (initializing) initializing = false;
    else cycles++;
    atBoundary = true;
  }

  public boolean atBoundary() {
    return atBoundary;
  }

  public Observation<S, A> observation() {
    var result = boundarySnapshot();
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Method mass not conserved");
    return result;
  }

  /** Internal complete-cycle snapshot; consumers must validate mass before publishing a result. */
  public Observation<S, A> boundarySnapshot() {
    if (!atBoundary) throw new IllegalStateException("Partial cycle is not an observation");
    var saved = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    exits.forEach((exit, distribution) -> saved.put(exit, Map.copyOf(distribution)));
    var result =
        new Observation<>(
            cycles,
            Map.copyOf(saved),
            Map.copyOf(boundary),
            Map.copyOf(initialUses),
            Map.copyOf(cycleUses));
    return result;
  }

  private void absorb(Exit exit, S state, Fraction mass) {
    if (mass.equals(Fraction.ZERO)) return;
    if (normalizeTerminal == null) exits.get(exit).merge(state, mass, Fraction::add);
    else {
      var normalization = normalizeTerminal.apply(Map.of(state, mass));
      if (normalization.complete() && normalization.result().size() <= 1)
        normalization
            .result()
            .forEach((target, value) -> exits.get(exit).merge(target, value, Fraction::add));
      else terminalQueue.addLast(new PendingExit<>(exit, normalization));
    }
  }

  private void stepTerminal(int maxOutcomes) {
    var pendingExit = terminalQueue.getFirst();
    if (terminalNormalization == null) terminalNormalization = pendingExit.normalization();
    if (!terminalNormalization.complete()) {
      terminalNormalization.step(maxOutcomes);
      return;
    }
    if (terminalResults == null)
      terminalResults = terminalNormalization.result().entrySet().iterator();
    for (int n = 0; n < maxOutcomes && terminalResults.hasNext(); n++) {
      var next = terminalResults.next();
      exits.get(pendingExit.kind()).merge(next.getKey(), next.getValue(), Fraction::add);
    }
    if (!terminalResults.hasNext()) {
      terminalQueue.removeFirst();
      terminalNormalization = null;
      terminalResults = null;
      if (terminalQueue.isEmpty() && finishingBoundary) finishBoundary();
    }
  }

  private static void probability(Fraction value) {
    Objects.requireNonNull(value);
    if (value.numerator().compareTo(value.denominator()) > 0)
      throw new IllegalArgumentException("Probability exceeds one");
  }
}
