package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;

/**
 * 내부 커서를 노출하지 않는 경로 탐색 요청·응답 계약이다.
 *
 * <p>기존 계약: Fixed v1 wire records. Internal continuation is deliberately absent.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PathSearchProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Create}: {@code PathSearchProtocol}에서 사용하는 생성의 불변 값이다.
 *   <li>{@code Mutation}: {@code PathSearchProtocol}에서 사용하는 변경의 불변 값이다.
 *   <li>{@code Recover}: {@code PathSearchProtocol}에서 사용하는 Recover의 불변 값이다.
 *   <li>{@code Probability}: {@code PathSearchProtocol}에서 사용하는 확률의 불변 값이다.
 *   <li>{@code Capabilities}: {@code PathSearchProtocol}에서 사용하는 지원기능의 불변 값이다.
 *   <li>{@code Provenance}: {@code PathSearchProtocol}에서 사용하는 출처이력의 불변 값이다.
 *   <li>{@code Point}: {@code PathSearchProtocol}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Policy}: {@code PathSearchProtocol}에서 사용하는 정책의 불변 값이다.
 *   <li>{@code Eventual}: {@code PathSearchProtocol}에서 사용하는 Eventual의 불변 값이다.
 *   <li>{@code ExitPoint}: {@code PathSearchProtocol}에서 사용하는 종료·지점의 불변 값이다.
 *   <li>{@code MethodExit}: {@code PathSearchProtocol}에서 사용하는 제작방법·종료의 불변 값이다.
 *   <li>{@code OmittedPoint}: {@code PathSearchProtocol}에서 사용하는 Omitted·지점의 불변 값이다.
 *   <li>{@code MethodTransition}: {@code PathSearchProtocol}에서 사용하는 제작방법·전이의 불변 값이다.
 *   <li>{@code Recommendation}: {@code PathSearchProtocol}에서 사용하는 추천의 불변 값이다.
 *   <li>{@code Rank}: {@code PathSearchProtocol}에서 사용하는 순위의 불변 값이다.
 *   <li>{@code Ranking}: {@code PathSearchProtocol}에서 사용하는 순위의 불변 값이다.
 *   <li>{@code Node}: {@code PathSearchProtocol}에서 사용하는 노드의 불변 값이다.
 *   <li>{@code Execution}: {@code PathSearchProtocol}에서 사용하는 실행의 불변 값이다.
 *   <li>{@code Edge}: {@code PathSearchProtocol}에서 사용하는 간선의 불변 값이다.
 *   <li>{@code Expansion}: {@code PathSearchProtocol}에서 사용하는 확장의 불변 값이다.
 *   <li>{@code GraphPage}: {@code PathSearchProtocol}에서 사용하는 그래프·페이지의 불변 값이다.
 *   <li>{@code Recovery}: {@code PathSearchProtocol}에서 사용하는 복구의 불변 값이다.
 *   <li>{@code Scope}: {@code PathSearchProtocol}에서 사용하는 범위의 불변 값이다.
 *   <li>{@code Snapshot}: {@code PathSearchProtocol}에서 사용하는 스냅샷의 불변 값이다.
 *   <li>{@code Rejected}: {@code PathSearchProtocol} 내부의 거절된 처리를 맡는다.
 * </ul>
 */
public final class PathSearchProtocol {
  private PathSearchProtocol() {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 생성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code start}: 시작값 ({@code BasicCurrencyState}).
   *   <li>{@code goal}: 목표 ({@code GoalFilter}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code observations}: 관측값목록 ({@code List<String>}).
   * </ul>
   */
  public record Create(
      Integer version,
      String clientRequestId,
      BasicCurrencyState start,
      GoalFilter goal,
      List<String> activeOmens,
      List<String> observations) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 변경의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code operation}: 연산 ({@code String}).
   *   <li>{@code commandId}: 명령·식별자 ({@code String}).
   *   <li>{@code expectedRevision}: 기대값·리비전 ({@code Long}).
   * </ul>
   */
  public record Mutation(
      Integer version, String operation, String commandId, Long expectedRevision) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 Recover의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code parentRevision}: 상위값·리비전 ({@code Long}).
   *   <li>{@code failureExecutionId}: 실패·실행·식별자 ({@code String}).
   *   <li>{@code checkpointStateId}: 체크포인트·상태·식별자 ({@code String}).
   *   <li>{@code observations}: 관측값목록 ({@code List<String>}).
   * </ul>
   */
  public record Recover(
      Integer version,
      String clientRequestId,
      Long parentRevision,
      String failureExecutionId,
      String checkpointStateId,
      List<String> observations) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 확률의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code String}).
   *   <li>{@code denominator}: 분모 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code of(value)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code value}(값). 반환: {@code Probability}.
   *   <li>{@code fraction()}: 유리수 값을 조회·구성한다. 반환: {@code Fraction}.
   * </ul>
   */
  public record Probability(String numerator, String denominator) {
    public static Probability of(Fraction value) {
      return new Probability(value.numerator().toString(), value.denominator().toString());
    }

    public Fraction fraction() {
      return new Fraction(
          new java.math.BigInteger(numerator), new java.math.BigInteger(denominator));
    }
  }

  /**
   * {@code PathSearchProtocol}에서 사용하는 지원기능의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code evaluation}: 평가 ({@code String}).
   *   <li>{@code search}: 탐색 ({@code String}).
   *   <li>{@code reasonCode}: 이유·코드 ({@code String}).
   *   <li>{@code actions}: 행동목록 ({@code List<String>}).
   *   <li>{@code goalGroupTypes}: 목표·그룹·Types ({@code List<String>}).
   *   <li>{@code conditionalRecovery}: 조건부·복구 ({@code boolean}).
   *   <li>{@code resume}: 재개 ({@code boolean}).
   * </ul>
   */
  public record Capabilities(
      String evaluation,
      String search,
      String reasonCode,
      List<String> actions,
      List<String> goalGroupTypes,
      boolean conditionalRecovery,
      boolean resume) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 출처이력의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code transition}: 전이 ({@code BasicCurrencyState.Provenance}).
   *   <li>{@code goalCatalogVersion}: 목표·카탈로그·버전 ({@code String}).
   *   <li>{@code predicateVersion}: 판정조건·버전 ({@code String}).
   *   <li>{@code searchVersion}: 탐색·버전 ({@code String}).
   *   <li>{@code candidateSetVersion}: 후보·집합·버전 ({@code String}).
   *   <li>{@code interpretation}: 해석 ({@code String}).
   * </ul>
   */
  public record Provenance(
      BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      String predicateVersion,
      String searchVersion,
      String candidateSetVersion,
      String interpretation) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code String}).
   *   <li>{@code lower}: 하한 ({@code Probability}).
   *   <li>{@code upper}: 상한 ({@code Probability}).
   *   <li>{@code active}: 활성 ({@code Probability}).
   *   <li>{@code dead}: 종료된 ({@code Probability}).
   *   <li>{@code unresolved}: 미해결 ({@code Probability}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   * </ul>
   */
  public record Point(
      String attempts,
      Probability lower,
      Probability upper,
      Probability active,
      Probability dead,
      Probability unresolved,
      String status) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 정책의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code actions}: 행동목록 ({@code List<String>}).
   *   <li>{@code mode}: 모드 ({@code String}).
   * </ul>
   */
  public record Policy(String id, List<String> actions, String mode) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 Eventual의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   *   <li>{@code proofVersion}: 증명·버전 ({@code String}).
   * </ul>
   */
  public record Eventual(String status, Probability probability, String proofVersion) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 종료·지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   * </ul>
   */
  public record ExitPoint(String attempts, Probability probability) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 제작방법·종료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<ExitPoint>}).
   * </ul>
   */
  public record MethodExit(String stateId, String kind, List<ExitPoint> points) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 Omitted·지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code String}).
   *   <li>{@code hit}: 목표도달 ({@code Probability}).
   *   <li>{@code active}: 활성 ({@code Probability}).
   * </ul>
   */
  public record OmittedPoint(String attempts, Probability hit, Probability active) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 제작방법·전이의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code fromStateId}: 원본·상태·식별자 ({@code String}).
   *   <li>{@code stopCondition}: stop·조건 ({@code String}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code int}).
   *   <li>{@code proofVersion}: 증명·버전 ({@code String}).
   *   <li>{@code exits}: 종료목록 ({@code List<MethodExit>}).
   *   <li>{@code omitted}: omitted ({@code List<OmittedPoint>}).
   * </ul>
   */
  public record MethodTransition(
      String fromStateId,
      String stopCondition,
      int cycleUses,
      String proofVersion,
      List<MethodExit> exits,
      List<OmittedPoint> omitted) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 추천의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code policy}: 정책 ({@code Policy}).
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   *   <li>{@code eventual}: eventual ({@code Eventual}).
   *   <li>{@code method}: 제작방법 ({@code MethodTransition}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Recommendation(policy, points, eventual)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       policy}(정책), {@code points}(지점목록), {@code eventual}(eventual).
   * </ul>
   */
  public record Recommendation(
      Policy policy,
      List<Point> points,
      Eventual eventual,
      @JsonInclude(JsonInclude.Include.NON_NULL) MethodTransition method) {
    public Recommendation(Policy policy, List<Point> points, Eventual eventual) {
      this(policy, points, eventual, null);
    }
  }

  /**
   * {@code PathSearchProtocol}에서 사용하는 순위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code policyId}: 정책·식별자 ({@code String}).
   *   <li>{@code rank}: 순위 ({@code int}).
   * </ul>
   */
  public record Rank(String policyId, int rank) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 순위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code entries}: 항목목록 ({@code List<Rank>}).
   * </ul>
   */
  public record Ranking(String attempts, String status, List<Rank> entries) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 노드의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code goalStatus}: 목표·상태 ({@code String}).
   * </ul>
   */
  public record Node(String id, ItemState item, String goalStatus) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 실행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code policyId}: 정책·식별자 ({@code String}).
   *   <li>{@code phase}: 단계 ({@code int}).
   * </ul>
   */
  public record Execution(String id, String stateId, String policyId, int phase) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code from}: 원본 ({@code String}).
   *   <li>{@code to}: 대상 ({@code String}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   *   <li>{@code kind}: 종류 ({@code String}).
   * </ul>
   */
  public record Edge(
      String id, String from, String to, String action, Probability probability, String kind) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 확장의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code executionId}: 실행·식별자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code unresolved}: 미해결 ({@code Probability}).
   * </ul>
   */
  public record Expansion(String executionId, String status, Probability unresolved) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 그래프·페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code nodes}: 노드목록 ({@code List<Node>}).
   *   <li>{@code executions}: 실행목록 ({@code List<Execution>}).
   *   <li>{@code edges}: 간선목록 ({@code List<Edge>}).
   *   <li>{@code expansions}: expansions ({@code List<Expansion>}).
   *   <li>{@code nextCursor}: 다음·진행커서 ({@code String}).
   * </ul>
   */
  public record GraphPage(
      int version,
      String jobId,
      long revision,
      List<Node> nodes,
      List<Execution> executions,
      List<Edge> edges,
      List<Expansion> expansions,
      String nextCursor) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 복구의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code parentJobId}: 상위값·작업·식별자 ({@code String}).
   *   <li>{@code parentRevision}: 상위값·리비전 ({@code long}).
   *   <li>{@code failureExecutionId}: 실패·실행·식별자 ({@code String}).
   *   <li>{@code checkpointStateId}: 체크포인트·상태·식별자 ({@code String}).
   *   <li>{@code conditional}: 조건부 ({@code boolean}).
   *   <li>{@code includedInMain}: included·입력·Main ({@code boolean}).
   * </ul>
   */
  public record Recovery(
      String parentJobId,
      long parentRevision,
      String failureExecutionId,
      String checkpointStateId,
      boolean conditional,
      boolean includedInMain) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 범위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code generated}: 생성된 ({@code int}).
   *   <li>{@code total}: 합계 ({@code Integer}).
   *   <li>{@code enumerationComplete}: 전체열거·완료 ({@code boolean}).
   * </ul>
   */
  public record Scope(String kind, int generated, Integer total, boolean enumerationComplete) {}

  /**
   * {@code PathSearchProtocol}에서 사용하는 스냅샷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code requestFingerprint}: 요청·지문 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code expiresAt}: 만료시각·기준 ({@code String}).
   *   <li>{@code capabilities}: 지원기능 ({@code Capabilities}).
   *   <li>{@code provenance}: 출처이력 ({@code Provenance}).
   *   <li>{@code candidateScope}: 후보·범위 ({@code Scope}).
   *   <li>{@code recommendations}: 추천목록 ({@code List<Recommendation>}).
   *   <li>{@code rankings}: rankings ({@code List<Ranking>}).
   *   <li>{@code graph}: 그래프 ({@code GraphPage}).
   *   <li>{@code recovery}: 복구 ({@code Recovery}).
   *   <li>{@code resumable}: resumable ({@code boolean}).
   *   <li>{@code reasonCode}: 이유·코드 ({@code String}).
   * </ul>
   */
  public record Snapshot(
      int version,
      String jobId,
      String clientRequestId,
      String requestFingerprint,
      long revision,
      String status,
      String expiresAt,
      Capabilities capabilities,
      Provenance provenance,
      Scope candidateScope,
      List<Recommendation> recommendations,
      List<Ranking> rankings,
      GraphPage graph,
      Recovery recovery,
      boolean resumable,
      String reasonCode) {}

  /**
   * {@code PathSearchProtocol} 내부의 거절된 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code int}).
   *   <li>{@code code}: 코드 ({@code String}).
   *   <li>{@code retryAfterSeconds}: 재시도·변경후·초 ({@code Long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Rejected(status, code)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code status}(상태), {@code
   *       code}(코드).
   *   <li>{@code Rejected(status, code, retryAfterSeconds)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       status}(상태), {@code code}(코드), {@code retryAfterSeconds}(재시도·변경후·초).
   *   <li>{@code retryAfterSeconds()}: {@code retryAfterSeconds} 값을 조회한다. 반환: {@code Long}.
   *   <li>{@code status()}: 현재 진행 상태를 조회한다. 반환: {@code int}.
   *   <li>{@code code()}: {@code code} 값을 조회한다. 반환: {@code String}.
   * </ul>
   */
  public static final class Rejected extends IllegalArgumentException {
    private final int status;
    private final String code;
    private final Long retryAfterSeconds;

    public Rejected(int status, String code) {
      this(status, code, null);
    }

    public Rejected(int status, String code, Long retryAfterSeconds) {
      super(code);
      this.status = status;
      this.code = code;
      this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long retryAfterSeconds() {
      return retryAfterSeconds;
    }

    public int status() {
      return status;
    }

    public String code() {
      return code;
    }
  }
}
