package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedMethodRunner;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Function;

/**
 * 최초 성공 계산식을 공유하고 등록 시에는 전이를 계산하지 않는다.
 *
 * <p>기존 계약: Compact, shared first-hit programs. Registration and requests perform no transition
 * work.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "stored-first-hit-expression-v1"} ({@code String}).
 *   <li>{@code store}: 저장소 ({@code MethodGraphStore}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code decoded}: 짧은 ID에서 원본 키로 가는 도메인별 불변 색인 ({@code LinkedHashMap<Long, Node>}).
 *   <li>{@code interned}: 공유색인된 ({@code LinkedHashMap<String, Long>}).
 *   <li>{@code MAX_CACHE_CONTENT_CHARS}: CHARS; 초기값 {@code 8192} ({@code int}).
 *   <li>{@code cacheLimit}: 캐시·한도 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code StoredMethodExpressions(store, cacheLimit)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       store}(저장소), {@code cacheLimit}(캐시·한도). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code define(id, source, initial, cycle, canonicalPolicy)}: define 처리에서 {@code
 *       cycle.isEmpty}, {@code canonicalPolicy.isBlank}을(를) 사용한다. 입력: {@code id}(식별자), {@code
 *       source}(출처), {@code initial}(초기값), {@code cycle}(주기), {@code canonicalPolicy}(정규형·정책). 반환:
 *       {@code MethodGraphStore.Method}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code sequence(actions)}: 순서 처리에서 {@code children.add}, {@code action.name}을(를) 사용한다. 입력:
 *       {@code actions}(행동목록). 반환: {@code long}.
 *   <li>{@code request(revision, method, cycles, precision)}: 계산·실행에 사용할 요청 값을 구성한다. 입력: {@code
 *       revision}(리비전), {@code method}(제작방법), {@code cycles}(주기횟수), {@code precision}(정밀도). 반환:
 *       {@code long}. 사용하는 예외: {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code cachedNodes()}: 캐시된·노드목록 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code program(method)}: program 처리에서 {@code store.method}, {@code definition.program}을(를)
 *       사용한다. 입력: {@code method}(제작방법). 반환: {@code long}.
 *   <li>{@code precisionKey(precision)}: 정밀도·키 처리에서 {@code precision.fractionalBits}, {@code
 *       precision.percentDecimals}을(를) 사용한다. 입력: {@code precision}(정밀도). 반환: {@code String}.
 *   <li>{@code evaluate(method, cycles, root, transition, policies, precision)}: 입력 조건이나 정책을 평가해
 *       결과를 구한다. 입력: {@code method}(제작방법), {@code cycles}(주기횟수), {@code root}(시작점), {@code
 *       transition}(전이), {@code policies}(정책목록), {@code precision}(정밀도). 반환: {@code Evaluation<S>}.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code actions(reference)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code reference}(참조). 반환:
 *       {@code List<WorkbenchCurrency>}.
 *   <li>{@code require(node, kind, children)}: 필수 조건을 검사한다. 입력: {@code node}(노드), {@code kind}(종류),
 *       {@code children}(하위목록). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code intern(kind, value, children, cycles)}: 동등한 기존 값을 찾아 공유하고 없으면 등록한다. 입력: {@code
 *       kind}(종류), {@code value}(값), {@code children}(하위목록), {@code cycles}(주기횟수). 반환: {@code
 *       long}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code encode(node)}: 값을 저장·전송 표현으로 부호화한다. 입력: {@code node}(노드). 반환: {@code String}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code decode(content)}: 저장·전송 표현을 내부 값으로 복원한다. 입력: {@code content}(내용). 반환: {@code Node}.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code node(id)}: 노드 처리에서 {@code decoded.get}, {@code store.operator}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code Node}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Node}: {@code StoredMethodExpressions}에서 사용하는 노드의 불변 값이다.
 *   <li>{@code Evaluation}: {@code StoredMethodExpressions} 내부의 평가 처리를 맡는다.
 * </ul>
 */
public final class StoredMethodExpressions {
  public static final String VERSION = "stored-first-hit-expression-v1";

  /**
   * {@code StoredMethodExpressions}에서 사용하는 노드의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code value}: 값 ({@code String}).
   *   <li>{@code children}: 하위목록 ({@code List<Long>}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Node(version, kind, value, children, cycles)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       version}(버전), {@code kind}(종류), {@code value}(값), {@code children}(하위목록), {@code
   *       cycles}(주기횟수).
   * </ul>
   */
  public record Node(String version, String kind, String value, List<Long> children, int cycles) {
    public Node {
      children = List.copyOf(children);
    }
  }

  private final MethodGraphStore store;
  private final ObjectMapper json = new ObjectMapper();
  private final LinkedHashMap<Long, Node> decoded = new LinkedHashMap<>(16, 0.75f, true);
  private final LinkedHashMap<String, Long> interned = new LinkedHashMap<>(16, 0.75f, true);
  private static final int MAX_CACHE_CONTENT_CHARS = 8192;
  private final int cacheLimit;

  public StoredMethodExpressions(MethodGraphStore store, int cacheLimit) {
    this.store = Objects.requireNonNull(store);
    if (cacheLimit < 0 || cacheLimit > 256)
      throw new IllegalArgumentException("Invalid node cache");
    this.cacheLimit = cacheLimit;
  }

  /**
   * Policy content must include the complete checkpoint/classifier semantics, not a display label.
   */
  public MethodGraphStore.Method define(
      String id,
      long source,
      List<WorkbenchCurrency> initial,
      List<WorkbenchCurrency> cycle,
      String canonicalPolicy) {
    if (cycle.isEmpty() || canonicalPolicy == null || canonicalPolicy.isBlank())
      throw new IllegalArgumentException("Cycle and complete policy required");
    long policy = intern("POLICY", canonicalPolicy, List.of(), 0);
    long before = sequence(initial);
    long repeat = sequence(cycle);
    long program = intern("FIRST_HIT_PROGRAM", "", List.of(policy, before, repeat), 0);
    return new MethodGraphStore.Method(
        id, source, encode(new Node(VERSION, "METHOD", "", List.of(program), 0)));
  }

  private long sequence(List<WorkbenchCurrency> actions) {
    var children = new ArrayList<Long>();
    for (var action : actions) children.add(intern("ACTION", action.name(), List.of(), 0));
    return intern("SEQUENCE", "", children, 0);
  }

  public long request(
      long revision, String method, int cycles, CertifiedInterval.Precision precision) {
    if (!List.of(100, 300, 500).contains(cycles))
      throw new IllegalArgumentException("Unknown horizon");
    long program = program(method);
    long expression = intern("OBSERVATION", precisionKey(precision), List.of(program), cycles);
    var old = store.observation(method, cycles);
    if (old.isPresent() && !Objects.equals(old.get().operator(), expression))
      throw new IllegalStateException("Observation precision or expression changed");
    return store.commit(
        revision,
        new MethodGraphStore.Batch(
            List.of(),
            List.of(),
            List.of(
                new MethodGraphStore.Observation(
                    method, cycles, MethodGraphStore.Status.REQUESTED, expression, null)),
            List.of()));
  }

  public int cachedNodes() {
    return decoded.size();
  }

  private long program(String method) {
    var definition =
        store.method(method).orElseThrow(() -> new IllegalArgumentException("Unknown method"));
    var node = decode(definition.program());
    require(node, "METHOD", 1);
    return node.children().getFirst();
  }

  private static String precisionKey(CertifiedInterval.Precision precision) {
    return precision.fractionalBits() + ":" + precision.percentDecimals();
  }

  /** The caller supplies full calculation states and the matching versioned primitive kernel. */
  public <S> Evaluation<S> evaluate(
      String method,
      int cycles,
      S root,
      Transition<S, WorkbenchCurrency> transition,
      Function<String, Function<S, Optional<Exit>>> policies,
      CertifiedInterval.Precision precision) {
    var requested =
        store
            .observation(method, cycles)
            .orElseThrow(() -> new IllegalStateException("Observation not requested"));
    if (requested.operator() == null)
      throw new IllegalStateException("Missing observation expression");
    var observation = node(requested.operator());
    require(observation, "OBSERVATION", 1);
    if (observation.cycles() != cycles
        || observation.children().getFirst() != program(method)
        || !observation.value().equals(precisionKey(precision)))
      throw new IllegalStateException("Observation expression mismatch");
    var program = node(observation.children().getFirst());
    require(program, "FIRST_HIT_PROGRAM", 3);
    var policy = node(program.children().get(0));
    require(policy, "POLICY", 0);
    var classifier = Objects.requireNonNull(policies.apply(policy.value()));
    var initial = actions(program.children().get(1));
    var cycle = actions(program.children().get(2));
    var template =
        new Template<S, WorkbenchCurrency>(
            VERSION + ":" + method,
            initial.stream().<Function<S, WorkbenchCurrency>>map(a -> s -> a).toList(),
            cycle.stream().<Function<S, WorkbenchCurrency>>map(a -> s -> a).toList());
    return new Evaluation<>(
        new CertifiedMethodRunner<>(root, template, transition, classifier, precision), cycles);
  }

  private List<WorkbenchCurrency> actions(long reference) {
    var sequence = node(reference);
    require(sequence, "SEQUENCE", sequence.children().size());
    return sequence.children().stream()
        .map(
            id -> {
              var action = node(id);
              require(action, "ACTION", 0);
              return WorkbenchCurrency.valueOf(action.value());
            })
        .toList();
  }

  private static void require(Node node, String kind, int children) {
    if (!VERSION.equals(node.version())
        || !kind.equals(node.kind())
        || node.children().size() != children
        || (!kind.equals("OBSERVATION") && node.cycles() != 0))
      throw new IllegalStateException("Incompatible method expression");
  }

  private long intern(String kind, String value, List<Long> children, int cycles) {
    String content = encode(new Node(VERSION, kind, value, children, cycles));
    var known = interned.get(content);
    if (known != null) return known;
    try {
      String digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(content.getBytes(StandardCharsets.UTF_8)));
      long id = store.internOperator(digest, content);
      if (cacheLimit > 0 && content.length() <= MAX_CACHE_CONTENT_CHARS) {
        interned.put(content, id);
        if (interned.size() > cacheLimit) interned.pollFirstEntry();
      }
      return id;
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private String encode(Node node) {
    try {
      return json.writeValueAsString(node);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Invalid expression", error);
    }
  }

  private Node decode(String content) {
    try {
      return json.readValue(content, Node.class);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Invalid stored expression", error);
    }
  }

  private Node node(long id) {
    var found = decoded.get(id);
    if (found != null) return found;
    String content = store.operator(id);
    var node = decode(content);
    if (cacheLimit > 0 && content.length() <= MAX_CACHE_CONTENT_CHARS) {
      decoded.put(id, node);
      if (decoded.size() > cacheLimit) decoded.pollFirstEntry();
    }
    return node;
  }

  /**
   * {@code StoredMethodExpressions} 내부의 평가 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code runner}: 실행기 ({@code CertifiedMethodRunner<S, WorkbenchCurrency>}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Evaluation(runner, cycles)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code runner}(실행기), {@code
   *       cycles}(주기횟수).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code CertifiedMethodRunner.Observation<S,
   *       WorkbenchCurrency>}. 사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
  public static final class Evaluation<S> {
    private final CertifiedMethodRunner<S, WorkbenchCurrency> runner;
    private final int cycles;

    private Evaluation(CertifiedMethodRunner<S, WorkbenchCurrency> runner, int cycles) {
      this.runner = runner;
      this.cycles = cycles;
    }

    public boolean complete() {
      return runner.atBoundary() && (runner.absorbed() || runner.cycles() == cycles);
    }

    public void step() {
      if (complete()) return;
      if (runner.atBoundary()) runner.beginCycle();
      else runner.step();
    }

    public long retainedStates() {
      return runner.retainedStates();
    }

    public CertifiedMethodRunner.Observation<S, WorkbenchCurrency> result() {
      if (!complete()) throw new IllegalStateException("Observation incomplete");
      var point = runner.observation();
      return point.cycles() == cycles ? point : point.atLaterAbsorbedBoundary(cycles);
    }
  }
}
