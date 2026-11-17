package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.MethodSearchService.*;

import com.poe2craft.craftingsupport.search.application.CertifiedMethodProtocol.Bounds;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/**
 * 계산 상태의 정체성을 유지하면서 결과 표시용 그룹을 정의한다.
 *
 * <p>기존 계약: Display grouping only: source class identities and alternative methods remain
 * independent.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "certified-bucket-map-v1"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedMapProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code graph(jobId, revision, states, methods, offset, precision)}: 그래프 처리에서 {@code
 *       methods.size}, {@code PathSearchProtocol.Rejected}을(를) 사용한다. 입력: {@code jobId}(작업·식별자),
 *       {@code revision}(리비전), {@code states}(상태목록), {@code methods}(제작방법목록), {@code offset}(시작위치),
 *       {@code precision}(정밀도). 반환: {@code Graph}. 사용하는 예외: {@code PathSearchProtocol.Rejected}.
 *   <li>{@code arrow(method, source, precision)}: 표시간선 처리에서 {@code CertifiedMethodProtocol.method},
 *       {@code base.points}을(를) 사용한다. 입력: {@code method}(제작방법), {@code source}(출처), {@code
 *       precision}(정밀도). 반환: {@code Arrow}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code distribution(outcomes, states)}: 상태 또는 실행의 확률 분포를 조회한다. 입력: {@code outcomes}(결과목록),
 *       {@code states}(상태목록). 반환: {@code Distribution}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Exit}: {@code CertifiedMapProtocol}에서 사용하는 종료의 불변 값이다.
 *   <li>{@code Point}: {@code CertifiedMapProtocol}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Arrow}: {@code CertifiedMapProtocol}에서 사용하는 표시간선의 불변 값이다.
 *   <li>{@code Graph}: {@code CertifiedMapProtocol}에서 사용하는 그래프의 불변 값이다.
 *   <li>{@code Header}: {@code CertifiedMapProtocol}에서 사용하는 헤더의 불변 값이다.
 *   <li>{@code View}: {@code CertifiedMapProtocol}에서 사용하는 조회표현의 불변 값이다.
 *   <li>{@code ClassItem}: {@code CertifiedMapProtocol}에서 사용하는 동치류·아이템의 불변 값이다.
 *   <li>{@code Distribution}: {@code CertifiedMapProtocol}에서 사용하는 확률분포의 불변 값이다.
 *   <li>{@code Group}: {@code CertifiedMapProtocol} 내부의 그룹 처리를 맡는다.
 * </ul>
 */
public final class CertifiedMapProtocol {
  public static final String VERSION = "certified-bucket-map-v1";

  private CertifiedMapProtocol() {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 종료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code bucketId}: 상태버킷·식별자 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Bounds}).
   * </ul>
   */
  public record Exit(String bucketId, String kind, Bounds probability) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code exits}: 종료목록 ({@code List<Exit>}).
   *   <li>{@code unresolved}: 미해결 ({@code Bounds}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<String, Bounds>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<String, Bounds>}).
   * </ul>
   */
  public record Point(
      int cycles,
      List<Exit> exits,
      Bounds unresolved,
      Map<String, Bounds> initialUses,
      Map<String, Bounds> cycleUses) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 표시간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code sourceStateId}: 출처·상태·식별자 ({@code String}).
   *   <li>{@code sourceBucketId}: 출처·상태버킷·식별자 ({@code String}).
   *   <li>{@code sourceItem}: 출처·아이템 ({@code ItemState}).
   *   <li>{@code sourceRolls}: 출처·수치굴림목록 ({@code Map<String, DeferredItem.Band>}).
   *   <li>{@code sourceTiers}: 출처·티어목록 ({@code Map<String,
   *       List<MethodSearchProtocol.TierVariant>>}).
   *   <li>{@code sourceOrbits}: 출처·교환가능계열목록 ({@code Map<String, MethodSearchProtocol.OrbitView>}).
   *   <li>{@code initialActions}: 초기값·행동목록 ({@code List<String>}).
   *   <li>{@code cycleActions}: 주기·행동목록 ({@code List<String>}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   * </ul>
   */
  public record Arrow(
      String id,
      String sourceStateId,
      String sourceBucketId,
      ItemState sourceItem,
      Map<String, DeferredItem.Band> sourceRolls,
      Map<String, List<MethodSearchProtocol.TierVariant>> sourceTiers,
      Map<String, MethodSearchProtocol.OrbitView> sourceOrbits,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      List<Point> points) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 그래프의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code buckets}: buckets ({@code List<MethodSearchProtocol.Bucket>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Arrow>}).
   *   <li>{@code localRecommendations}: 로컬·추천목록 ({@code List<CertifiedMethodProtocol.Local>}).
   *   <li>{@code methodCount}: 제작방법·개수 ({@code int}).
   *   <li>{@code nextOffset}: 다음·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Graph(
      String version,
      String jobId,
      long revision,
      List<MethodSearchProtocol.Bucket> buckets,
      List<Arrow> methods,
      List<CertifiedMethodProtocol.Local> localRecommendations,
      int methodCount,
      Integer nextOffset) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 헤더의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code clientRequestId}: 클라이언트·요청·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code resumable}: resumable ({@code boolean}).
   *   <li>{@code enumerationComplete}: 전체열거·완료 ({@code boolean}).
   *   <li>{@code rootStateId}: 시작점·상태·식별자 ({@code String}).
   *   <li>{@code strategyStatus}: 전략·상태 ({@code String}).
   *   <li>{@code localRecommendations}: 로컬·추천목록 ({@code List<CertifiedMethodProtocol.Local>}).
   *   <li>{@code strategies}: 전략목록 ({@code List<MethodSearchProtocol.Strategy>}).
   * </ul>
   */
  public record Header(
      String version,
      String jobId,
      String clientRequestId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean resumable,
      boolean enumerationComplete,
      String rootStateId,
      String strategyStatus,
      List<CertifiedMethodProtocol.Local> localRecommendations,
      List<MethodSearchProtocol.Strategy> strategies) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code search}: 탐색 ({@code Header}).
   *   <li>{@code graph}: 그래프 ({@code Graph}).
   * </ul>
   */
  public record View(Header search, Graph graph) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 동치류·아이템의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code Map<String, DeferredItem.Band>}).
   *   <li>{@code tiers}: 티어목록 ({@code Map<String, List<MethodSearchProtocol.TierVariant>>}).
   *   <li>{@code orbits}: 교환가능계열목록 ({@code Map<String, MethodSearchProtocol.OrbitView>}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Bounds}).
   * </ul>
   */
  public record ClassItem(
      String stateId,
      ItemState item,
      Map<String, DeferredItem.Band> rolls,
      Map<String, List<MethodSearchProtocol.TierVariant>> tiers,
      Map<String, MethodSearchProtocol.OrbitView> orbits,
      String kind,
      Bounds probability) {}

  /**
   * {@code CertifiedMapProtocol}에서 사용하는 확률분포의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code methodId}: 제작방법·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code states}: 상태목록 ({@code List<ClassItem>}).
   *   <li>{@code nextOffset}: 다음·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Distribution(
      String version,
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<ClassItem> states,
      Integer nextOffset) {}

  /**
   * {@code CertifiedMapProtocol} 내부의 그룹 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code first}: 최초 ({@code MethodSearchEngine.StateView}).
   *   <li>{@code size}: 크기 ({@code int}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code complete}: 완료; 초기값 {@code true} ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code add(state)}: 값 또는 항목을 더한다. 입력: {@code state}(상태).
   *   <li>{@code view(id)}: 내부 계산 상태를 읽기 전용 조회 값으로 구성한다. 입력: {@code id}(식별자). 반환: {@code
   *       MethodSearchProtocol.Bucket}.
   * </ul>
   */
  private static final class Group {
    MethodSearchEngine.StateView first;
    int size, depth = Integer.MAX_VALUE;
    boolean complete = true;

    void add(MethodSearchEngine.StateView state) {
      if (first == null || state.depth() < depth) first = state;
      depth = Math.min(depth, state.depth());
      size++;
      complete &= state.complete();
    }

    MethodSearchProtocol.Bucket view(String id) {
      var item = first.representative();
      return new MethodSearchProtocol.Bucket(
          id,
          depth,
          first.progress().goal().name(),
          first.progress().entries().stream().map(Enum::name).toList(),
          size,
          item.item(),
          item.rolls(),
          tierView(item),
          orbitView(item),
          complete);
    }
  }

  static Graph graph(
      String jobId,
      long revision,
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods,
      int offset,
      CertifiedInterval.Precision precision) {
    if (offset < 0 || offset > methods.size())
      throw new PathSearchProtocol.Rejected(422, "INVALID_OFFSET");
    int end = Math.min(offset + 12, methods.size());
    var selected = methods.subList(offset, end);
    var sourceIds = new HashSet<String>();
    selected.forEach(m -> sourceIds.add(m.source()));
    var sources = new LinkedHashMap<String, MethodSearchEngine.StateView>();
    var groups = new LinkedHashMap<String, Group>();
    for (var state : states) {
      groups.computeIfAbsent(bucket(state.progress()), ignored -> new Group()).add(state);
      if (sourceIds.contains(state.id())) sources.put(state.id(), state);
    }
    var arrows =
        selected.stream()
            .map(method -> arrow(method, sources.get(method.source()), precision))
            .toList();
    return new Graph(
        VERSION,
        jobId,
        revision,
        groups.entrySet().stream().map(e -> e.getValue().view(e.getKey())).toList(),
        arrows,
        CertifiedMethodProtocol.locals(List.copyOf(sources.values()), methods, precision),
        methods.size(),
        end < methods.size() ? end : null);
  }

  private static Arrow arrow(
      CertifiedMethodSearchEngine.MethodView method,
      MethodSearchEngine.StateView source,
      CertifiedInterval.Precision precision) {
    if (source == null) throw new IllegalStateException("Missing source class");
    var base = CertifiedMethodProtocol.method(method, precision);
    var points = new ArrayList<Point>();
    for (var point : base.points()) {
      var exits = new ArrayList<Exit>();
      method
          .branches()
          .get(point.cycles())
          .forEach(
              (kind, targets) -> {
                if (kind.equals("UNRESOLVED")) return;
                targets.forEach(
                    (target, mass) ->
                        exits.add(
                            new Exit(
                                bucket(target),
                                kind,
                                Bounds.probability(mass, precision.percentDecimals()))));
              });
      exits.sort(Comparator.comparing(Exit::kind).thenComparing(Exit::bucketId));
      points.add(
          new Point(
              point.cycles(),
              List.copyOf(exits),
              point.exits().get("UNRESOLVED"),
              point.initialUses(),
              point.cycleUses()));
    }
    var item = source.representative();
    return new Arrow(
        method.id(),
        method.source(),
        bucket(source.progress()),
        item.item(),
        item.rolls(),
        tierView(item),
        orbitView(item),
        base.initialActions(),
        base.cycleActions(),
        base.status(),
        List.copyOf(points));
  }

  static Distribution distribution(
      CertifiedMethodProtocol.Outcomes outcomes, List<MethodSearchEngine.StateView> states) {
    var needed = new HashSet<String>();
    outcomes.outcomes().forEach(o -> needed.add(o.stateId()));
    var selected = new HashMap<String, DeferredItem>();
    for (var state : states)
      if (needed.contains(state.id())) selected.put(state.id(), state.representative());
    var items =
        outcomes.outcomes().stream()
            .map(
                outcome -> {
                  var item = selected.get(outcome.stateId());
                  if (item == null) throw new IllegalStateException("Missing outcome class");
                  return new ClassItem(
                      outcome.stateId(),
                      item.item(),
                      item.rolls(),
                      tierView(item),
                      orbitView(item),
                      outcome.kind(),
                      outcome.probability());
                })
            .toList();
    return new Distribution(
        VERSION,
        outcomes.jobId(),
        outcomes.revision(),
        outcomes.methodId(),
        outcomes.cycles(),
        items,
        outcomes.nextOffset());
  }
}
