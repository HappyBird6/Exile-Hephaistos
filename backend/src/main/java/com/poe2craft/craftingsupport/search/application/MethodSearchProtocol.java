package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/**
 * 방법 한 주기 기준의 탐색 요청·응답을 정의한다.
 *
 * <p>기존 계약: V2 observations count complete method cycles, not primitive currency uses.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code MethodSearchProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Create}: {@code MethodSearchProtocol} 내부의 생성 처리를 맡는다.
 *   <li>{@code Mutation}: {@code MethodSearchProtocol}에서 사용하는 변경의 불변 값이다.
 *   <li>{@code Bucket}: {@code MethodSearchProtocol}에서 사용하는 상태버킷의 불변 값이다.
 *   <li>{@code TierVariant}: {@code MethodSearchProtocol}에서 사용하는 티어·문맥별정의의 불변 값이다.
 *   <li>{@code OrbitView}: {@code MethodSearchProtocol}에서 사용하는 교환가능계열·조회표현의 불변 값이다.
 *   <li>{@code Exit}: {@code MethodSearchProtocol}에서 사용하는 종료의 불변 값이다.
 *   <li>{@code Point}: {@code MethodSearchProtocol}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Arrow}: {@code MethodSearchProtocol}에서 사용하는 표시간선의 불변 값이다.
 *   <li>{@code Graph}: {@code MethodSearchProtocol}에서 사용하는 그래프의 불변 값이다.
 *   <li>{@code ConditionalState}: {@code MethodSearchProtocol}에서 사용하는 조건부·상태의 불변 값이다.
 *   <li>{@code Distribution}: {@code MethodSearchProtocol}에서 사용하는 확률분포의 불변 값이다.
 *   <li>{@code Local}: {@code MethodSearchProtocol}에서 사용하는 로컬의 불변 값이다.
 *   <li>{@code Strategy}: {@code MethodSearchProtocol}에서 사용하는 전략의 불변 값이다.
 *   <li>{@code Snapshot}: {@code MethodSearchProtocol}에서 사용하는 스냅샷의 불변 값이다.
 * </ul>
 */
public final class MethodSearchProtocol {
  private MethodSearchProtocol() {}

  /**
   * {@code MethodSearchProtocol} 내부의 생성 처리를 맡는다.
   *
   * <p>기존 계약: Typed execution input with optional immutable provenance for historical JSON export.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code start}: 시작값 ({@code BasicCurrencyState}).
   *   <li>{@code goal}: 목표 ({@code GoalFilter}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code originalDocument}: 원본·문서 ({@code com.fasterxml.jackson.databind.JsonNode}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Create(version, clientRequestId, start, goal, activeOmens)}: 기본 정책을 보충해 공통 생성자로
   *       위임한다. 입력: {@code version}(버전), {@code clientRequestId}(클라이언트·요청·식별자), {@code start}(시작값),
   *       {@code goal}(목표), {@code activeOmens}(활성·징조목록).
   *   <li>{@code Create(version, clientRequestId, start, goal, activeOmens, originalDocument)}:
   *       전달받은 값으로 구성원을 초기화한다. 입력: {@code version}(버전), {@code clientRequestId}(클라이언트·요청·식별자),
   *       {@code start}(시작값), {@code goal}(목표), {@code activeOmens}(활성·징조목록), {@code
   *       originalDocument}(원본·문서).
   *   <li>{@code version()}: 현재 데이터·계산 계약의 버전을 조회한다. 반환: {@code Integer}.
   *   <li>{@code clientRequestId()}: {@code clientRequestId} 값을 조회한다. 반환: {@code String}.
   *   <li>{@code start()}: {@code start} 값을 조회한다. 반환: {@code BasicCurrencyState}.
   *   <li>{@code goal()}: {@code goal} 값을 조회한다. 반환: {@code GoalFilter}.
   *   <li>{@code activeOmens()}: {@code activeOmens} 값을 조회한다. 반환: {@code List<String>}.
   *   <li>{@code withOriginalDocument(document)}: 원본·문서을(를) 반영한 값을 구한다. 입력: {@code document}(문서).
   *       반환: {@code Create}.
   *   <li>{@code serializedDocument()}: serialized·문서 값을 조회·구성한다. 반환: {@code Object}.
   *   <li>{@code equals(other)}: 값의 동등성을 비교한다. 입력: {@code other}(비교대상). 반환: {@code boolean}.
   *   <li>{@code hashCode()}: 동등성 비교와 일치하는 해시를 반환한다. 반환: {@code int}.
   * </ul>
   */
  public static final class Create {
    private final Integer version;
    private final String clientRequestId;
    private final BasicCurrencyState start;
    private final GoalFilter goal;
    private final List<String> activeOmens;
    private final com.fasterxml.jackson.databind.JsonNode originalDocument;

    @com.fasterxml.jackson.annotation.JsonCreator
    public Create(
        @com.fasterxml.jackson.annotation.JsonProperty("version") Integer version,
        @com.fasterxml.jackson.annotation.JsonProperty("clientRequestId") String clientRequestId,
        @com.fasterxml.jackson.annotation.JsonProperty("start") BasicCurrencyState start,
        @com.fasterxml.jackson.annotation.JsonProperty("goal") GoalFilter goal,
        @com.fasterxml.jackson.annotation.JsonProperty("activeOmens") List<String> activeOmens) {
      this(version, clientRequestId, start, goal, activeOmens, null);
    }

    private Create(
        Integer version,
        String clientRequestId,
        BasicCurrencyState start,
        GoalFilter goal,
        List<String> activeOmens,
        com.fasterxml.jackson.databind.JsonNode originalDocument) {
      this.version = version;
      this.clientRequestId = clientRequestId;
      this.start = start;
      this.goal = goal;
      this.activeOmens = activeOmens;
      this.originalDocument = originalDocument == null ? null : originalDocument.deepCopy();
    }

    public Integer version() {
      return version;
    }

    public String clientRequestId() {
      return clientRequestId;
    }

    public BasicCurrencyState start() {
      return start;
    }

    public GoalFilter goal() {
      return goal;
    }

    public List<String> activeOmens() {
      return activeOmens;
    }

    public Create withOriginalDocument(com.fasterxml.jackson.databind.JsonNode document) {
      return new Create(version, clientRequestId, start, goal, activeOmens, document);
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public Object serializedDocument() {
      if (originalDocument != null) return originalDocument.deepCopy();
      var fields = new LinkedHashMap<String, Object>();
      fields.put("version", version);
      fields.put("clientRequestId", clientRequestId);
      fields.put("start", start);
      fields.put("goal", goal);
      fields.put("activeOmens", activeOmens);
      return fields;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Create value
          && Objects.equals(version, value.version)
          && Objects.equals(clientRequestId, value.clientRequestId)
          && Objects.equals(start, value.start)
          && Objects.equals(goal, value.goal)
          && Objects.equals(activeOmens, value.activeOmens);
    }

    @Override
    public int hashCode() {
      return Objects.hash(version, clientRequestId, start, goal, activeOmens);
    }
  }

  /**
   * {@code MethodSearchProtocol}에서 사용하는 변경의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code commandId}: 명령·식별자 ({@code String}).
   *   <li>{@code expectedRevision}: 기대값·리비전 ({@code Long}).
   *   <li>{@code operation}: 연산 ({@code String}).
   *   <li>{@code retainedOutcomeLimit}: 보유·결과·한도 ({@code Integer}).
   * </ul>
   */
  public record Mutation(
      Integer version,
      String commandId,
      Long expectedRevision,
      String operation,
      Integer retainedOutcomeLimit) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 상태버킷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code goal}: 목표 ({@code String}).
   *   <li>{@code entries}: 항목목록 ({@code List<String>}).
   *   <li>{@code states}: 상태목록 ({@code int}).
   *   <li>{@code representative}: 대표상태 ({@code ItemState}).
   *   <li>{@code representativeRolls}: 대표상태·수치굴림목록 ({@code Map<String, DeferredItem.Band>}).
   *   <li>{@code representativeTiers}: 대표상태·티어목록 ({@code Map<String, List<TierVariant>>}).
   *   <li>{@code representativeOrbits}: 대표상태·교환가능계열목록 ({@code Map<String, OrbitView>}).
   *   <li>{@code directComplete}: 직접·완료 ({@code boolean}).
   * </ul>
   */
  public record Bucket(
      String id,
      int depth,
      String goal,
      List<String> entries,
      int states,
      ItemState representative,
      Map<String, DeferredItem.Band> representativeRolls,
      Map<String, List<TierVariant>> representativeTiers,
      Map<String, OrbitView> representativeOrbits,
      boolean directComplete) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 티어·문맥별정의의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   * </ul>
   */
  public record TierVariant(String modifierId, String stat, long min, long max) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 교환가능계열·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code representativeIds}: 대표상태·식별자목록 ({@code List<String>}).
   *   <li>{@code assignments}: assignments ({@code List<List<List<TierVariant>>>}).
   * </ul>
   */
  public record OrbitView(
      List<String> representativeIds, List<List<List<TierVariant>>> assignments) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 종료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code bucketId}: 상태버킷·식별자 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   *   <li>{@code states}: 상태목록 ({@code int}).
   * </ul>
   */
  public record Exit(String bucketId, String kind, Probability probability, int states) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code exits}: 종료목록 ({@code List<Exit>}).
   *   <li>{@code unresolved}: 미해결 ({@code Probability}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<String, Probability>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<String, Probability>}).
   * </ul>
   */
  public record Point(
      int cycles,
      List<Exit> exits,
      Probability unresolved,
      Map<String, Probability> initialUses,
      Map<String, Probability> cycleUses) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 표시간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code sourceStateId}: 출처·상태·식별자 ({@code String}).
   *   <li>{@code sourceBucketId}: 출처·상태버킷·식별자 ({@code String}).
   *   <li>{@code sourceItem}: 출처·아이템 ({@code ItemState}).
   *   <li>{@code sourceRolls}: 출처·수치굴림목록 ({@code Map<String, DeferredItem.Band>}).
   *   <li>{@code sourceTiers}: 출처·티어목록 ({@code Map<String, List<TierVariant>>}).
   *   <li>{@code sourceOrbits}: 출처·교환가능계열목록 ({@code Map<String, OrbitView>}).
   *   <li>{@code initialActions}: 초기값·행동목록 ({@code List<String>}).
   *   <li>{@code cycleActions}: 주기·행동목록 ({@code List<String>}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   * </ul>
   */
  public record Arrow(
      String id,
      String sourceStateId,
      String sourceBucketId,
      ItemState sourceItem,
      Map<String, DeferredItem.Band> sourceRolls,
      Map<String, List<TierVariant>> sourceTiers,
      Map<String, OrbitView> sourceOrbits,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      String reason,
      List<Point> points) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 그래프의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code buckets}: buckets ({@code List<Bucket>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Arrow>}).
   *   <li>{@code methodCount}: 제작방법·개수 ({@code int}).
   *   <li>{@code nextCursor}: 다음·진행커서 ({@code String}).
   * </ul>
   */
  public record Graph(
      int version,
      String jobId,
      long revision,
      List<Bucket> buckets,
      List<Arrow> methods,
      int methodCount,
      String nextCursor) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 조건부·상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   * </ul>
   */
  public record ConditionalState(
      String stateId, ItemState item, String kind, Probability probability) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 확률분포의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code methodId}: 제작방법·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code states}: 상태목록 ({@code List<ConditionalState>}).
   *   <li>{@code unresolved}: 미해결 ({@code Probability}).
   *   <li>{@code nextCursor}: 다음·진행커서 ({@code String}).
   * </ul>
   */
  public record Distribution(
      int version,
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<ConditionalState> states,
      Probability unresolved,
      String nextCursor) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 로컬의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code methodIds}: 제작방법·식별자목록 ({@code List<String>}).
   * </ul>
   */
  public record Local(String stateId, int cycles, List<String> methodIds) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 전략의 불변 값이다.
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
   * </ul>
   */
  public record Strategy(
      String stateId,
      int cycles,
      String status,
      Probability lower,
      Probability upper,
      List<String> methodIds) {}

  /**
   * {@code MethodSearchProtocol}에서 사용하는 스냅샷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code resumable}: resumable ({@code boolean}).
   *   <li>{@code enumerationComplete}: 전체열거·완료 ({@code boolean}).
   *   <li>{@code operations}: 연산목록 ({@code long}).
   *   <li>{@code rootStateId}: 시작점·상태·식별자 ({@code String}).
   *   <li>{@code transition}: 전이 ({@code BasicCurrencyState.Provenance}).
   *   <li>{@code goalCatalogVersion}: 목표·카탈로그·버전 ({@code String}).
   *   <li>{@code templateVersion}: 템플릿·버전 ({@code String}).
   *   <li>{@code calculationVersion}: 계산·버전 ({@code String}).
   *   <li>{@code rollRepresentationVersion}: 수치굴림·Representation·버전 ({@code String}).
   *   <li>{@code rollPartitionVersion}: 수치굴림·분할·버전 ({@code String}).
   *   <li>{@code strategyVersion}: 전략·버전 ({@code String}).
   *   <li>{@code interpretation}: 해석 ({@code String}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code localRecommendations}: 로컬·추천목록 ({@code List<Local>}).
   *   <li>{@code strategies}: 전략목록 ({@code List<Strategy>}).
   *   <li>{@code graph}: 그래프 ({@code Graph}).
   * </ul>
   */
  public record Snapshot(
      int version,
      String jobId,
      String clientRequestId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean resumable,
      boolean enumerationComplete,
      long operations,
      String rootStateId,
      BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      String templateVersion,
      String calculationVersion,
      String rollRepresentationVersion,
      String rollPartitionVersion,
      String strategyVersion,
      String interpretation,
      List<WorkbenchSimulator.Assumption> assumptions,
      List<Local> localRecommendations,
      List<Strategy> strategies,
      Graph graph) {}
}
