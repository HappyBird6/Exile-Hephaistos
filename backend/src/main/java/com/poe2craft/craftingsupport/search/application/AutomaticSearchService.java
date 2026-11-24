package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.search.domain.AbyssStrategyPlanner;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.AutomaticPolicyCandidates;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 자동 탐색 작업의 소유권·수명·진행·취소·저장을 조정한다.
 *
 * <p>기존 계약: Request-driven bounded slices share one lock; no detached work survives cancellation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "automatic-method-job-v1"} ({@code String}).
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings.AutomaticSearch}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code codec}: 저장·전송 표현의 변환기 ({@code AutomaticJobCodec}).
 *   <li>{@code repository}: 조회·저장을 위임하는 저장소 ({@code AutomaticJobRepository}).
 *   <li>{@code policy}: 정책 ({@code AutomaticCurrencyPolicy}).
 *   <li>{@code factory}: 생성기 ({@code Function<MethodSearchProtocol.Create, Runtime>}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code historicalFactory}: 과거·생성기 ({@code Function<MethodSearchProtocol.Create, Runtime>}).
 *   <li>{@code jobs}: 작업목록 ({@code LinkedHashMap<String, Job>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticSearchService(json, repository, policy, factory, clock)}: 기본 정책을 보충해 공통
 *       생성자로 위임한다. 입력: {@code json}(JSON), {@code repository}(저장소), {@code policy}(정책), {@code
 *       factory}(생성기), {@code clock}(현재 시각 공급자).
 *   <li>{@code AutomaticSearchService(json, repository, policy, factory, clock,
 *       historicalFactory)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code json}(JSON), {@code
 *       repository}(저장소), {@code policy}(정책), {@code factory}(생성기), {@code clock}(현재 시각 공급자),
 *       {@code historicalFactory}(과거·생성기).
 *   <li>{@code AutomaticSearchService(json, repository, policy, factory, clock, historicalFactory,
 *       settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON), {@code repository}(저장소), {@code
 *       policy}(정책), {@code factory}(생성기), {@code clock}(현재 시각 공급자), {@code
 *       historicalFactory}(과거·생성기), {@code settings}(운영설정).
 *   <li>{@code ownerKey(owner)}: 소유자·키 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code owner}(소유자). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code admissionHeader(id)}: 작업 실행을 허용할지 검사할 최소 저장 정보를 읽는다. 입력: {@code id}(식별자). 반환: {@code
 *       AutomaticJobCodec.StoredHeader}.
 *   <li>{@code read(id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code id}(식별자). 반환: {@code Disk}.
 *   <li>{@code cache(job)}: 재사용할 결과를 캐시에 보관한다. 입력: {@code job}(작업).
 *   <li>{@code restore(disk)}: 저장된 상태로 실행 또는 조회 상태를 복원한다. 입력: {@code disk}(디스크). 반환: {@code Job}.
 *       사용하는 예외: {@code Rejected}.
 *   <li>{@code load(owner, id)}: 검증된 자료를 로딩한다. 입력: {@code owner}(소유자), {@code id}(식별자). 반환: {@code
 *       Job}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code disk(job)}: 디스크 처리에서 {@code graph.checkpoint}, {@code runtime.objective}을(를) 사용한다.
 *       입력: {@code job}(작업). 반환: {@code Disk}.
 *   <li>{@code save(job)}: 현재 값을 저장 경계에 기록한다. 입력: {@code job}(작업). 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code create(owner, request)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code
 *       request}(요청). 반환: {@code View}.
 *   <li>{@code create(owner, input)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code input}(입력).
 *       반환: {@code View}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code requireActiveSlot()}: 활성·슬롯을(를) 필수 조건을 확인한다. 사용하는 예외: {@code Rejected}.
 *   <li>{@code get(owner, id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code owner}(소유자), {@code id}(식별자). 반환:
 *       {@code View}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code original(owner, id)}: 원본 처리에서 {@code saved.owner}, {@code json.readTree}을(를) 사용한다.
 *       입력: {@code owner}(소유자), {@code id}(식별자). 반환: {@code MethodSearchProtocol.Create}. 사용하는 예외:
 *       {@code Rejected}.
 *   <li>{@code originalInput(owner, id)}: 저장 문서에 보존된 원래 요청을 조회한다. 입력: {@code owner}(소유자), {@code
 *       id}(식별자). 반환: {@code Input}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code control(owner, id, command)}: 작업제어 처리에서 {@code command.commandId}, {@code
 *       command.operation}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code command}(명령).
 *       반환: {@code View}. 사용하는 예외: {@code Rejected}, {@code IllegalStateException}.
 *   <li>{@code abyssStrategies(owner, id, nodeId)}: 훼손·전략목록 처리에서 {@code graph.abyssStrategies}을(를)
 *       사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code nodeId}(노드·식별자). 반환: {@code
 *       List<AbyssStrategyPlanner.Strategy>}.
 *   <li>{@code diagnostics(owner, id)}: 진단값 처리에서 {@code graph.metrics}을(를) 사용한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자). 반환: {@code Diagnostics}.
 *   <li>{@code view(job)}: 내부 계산 상태를 읽기 전용 조회 값으로 구성한다. 입력: {@code job}(작업). 반환: {@code View}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code arrows(nodes, methods)}: 그래프 간선을 공개 응답으로 변환한다. 입력: {@code nodes}(노드목록), {@code
 *       methods}(제작방법목록). 반환: {@code List<Arrow>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Input}: {@code AutomaticSearchService}에서 사용하는 입력의 불변 값이다.
 *   <li>{@code Runtime}: {@code AutomaticSearchService}에서 사용하는 실행환경의 불변 값이다.
 *   <li>{@code Control}: {@code AutomaticSearchService}에서 사용하는 작업제어의 불변 값이다.
 *   <li>{@code JointCase}: {@code AutomaticSearchService}에서 사용하는 결합·Case의 불변 값이다.
 *   <li>{@code ExitView}: {@code AutomaticSearchService}에서 사용하는 종료·조회표현의 불변 값이다.
 *   <li>{@code PointView}: {@code AutomaticSearchService}에서 사용하는 지점·조회표현의 불변 값이다.
 *   <li>{@code Arrow}: {@code AutomaticSearchService}에서 사용하는 표시간선의 불변 값이다.
 *   <li>{@code Recommendation}: {@code AutomaticSearchService}에서 사용하는 추천의 불변 값이다.
 *   <li>{@code ExpectedUse}: {@code AutomaticSearchService}에서 사용하는 기대값·사용의 불변 값이다.
 *   <li>{@code PolicyCandidate}: {@code AutomaticSearchService}에서 사용하는 정책·후보의 불변 값이다.
 *   <li>{@code View}: {@code AutomaticSearchService}에서 사용하는 조회표현의 불변 값이다.
 *   <li>{@code Disk}: {@code AutomaticSearchService}에서 사용하는 디스크의 불변 값이다.
 *   <li>{@code Job}: {@code AutomaticSearchService} 내부의 작업 처리를 맡는다.
 *   <li>{@code Diagnostics}: {@code AutomaticSearchService}에서 사용하는 진단값의 불변 값이다.
 * </ul>
 */
public final class AutomaticSearchService implements AutoCloseable {
  public static final String VERSION = "automatic-method-job-v1";

  /**
   * {@code AutomaticSearchService}에서 사용하는 입력의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code MethodSearchProtocol.Create}).
   *   <li>{@code abstractQuality40}: 추상화된·품질·40 ({@code boolean}).
   *   <li>{@code abstractQuality}: 추상화된·품질 ({@code int}).
   *   <li>{@code rareOutcomePolicy}: 희귀·결과·정책 ({@code RareOutcomePolicy}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Input(request, abstractQuality40, abstractQuality, rareOutcomePolicy)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code request}(요청), {@code abstractQuality40}(추상화된·품질·40), {@code
   *       abstractQuality}(추상화된·품질), {@code rareOutcomePolicy}(희귀·결과·정책).
   *   <li>{@code Input(request, abstractQuality40, abstractQuality)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
   *       {@code request}(요청), {@code abstractQuality40}(추상화된·품질·40), {@code
   *       abstractQuality}(추상화된·품질).
   *   <li>{@code Input(request, abstractQuality40)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       request}(요청), {@code abstractQuality40}(추상화된·품질·40).
   * </ul>
   */
  public record Input(
      MethodSearchProtocol.Create request,
      boolean abstractQuality40,
      int abstractQuality,
      RareOutcomePolicy rareOutcomePolicy) {
    public Input {
      rareOutcomePolicy =
          rareOutcomePolicy == null ? RareOutcomePolicy.disabled() : rareOutcomePolicy;
    }

    public Input(
        MethodSearchProtocol.Create request, boolean abstractQuality40, int abstractQuality) {
      this(request, abstractQuality40, abstractQuality, RareOutcomePolicy.disabled());
    }

    public Input(MethodSearchProtocol.Create request, boolean abstractQuality40) {
      this(request, abstractQuality40, abstractQuality40 ? 40 : 0);
    }
  }

  /**
   * {@code AutomaticSearchService}에서 사용하는 실행환경의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kernel}: 전이커널 ({@code AutomaticCraftingKernel}).
   *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, AutomaticMethodGraph.Progress>}).
   *   <li>{@code objective}: 목적 ({@code String}).
   *   <li>{@code minimumExplicitModifiers}: 최소값·명시옵션·옵션목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Runtime(kernel, evaluate, objective)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       kernel}(전이커널), {@code evaluate}(평가), {@code objective}(목적).
   * </ul>
   */
  public record Runtime(
      AutomaticCraftingKernel kernel,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      String objective,
      int minimumExplicitModifiers) {
    public Runtime(
        AutomaticCraftingKernel kernel,
        Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
        String objective) {
      this(kernel, evaluate, objective, 0);
    }
  }

  /**
   * {@code AutomaticSearchService}에서 사용하는 작업제어의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code commandId}: 명령·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code operation}: 연산 ({@code String}).
   * </ul>
   */
  public record Control(String commandId, long revision, long generation, String operation) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 결합·Case의 불변 값이다.
   *
   * <p>기존 계약: Mutually exclusive joint components. Local factors include fresh draws and known
   * constants; projected source variables retain their complete joint law, never products of
   * marginals.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code local}: 로컬 ({@code List<ModifierInstance>}).
   *   <li>{@code tiers}: 티어목록 ({@code Map<String, AutomaticMethodGraph.TierLaw>}).
   *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code Map<String,
   *       AutomaticCraftingKernel.RollBand>}).
   *   <li>{@code retainedFromSource}: 보유·원본·출처 ({@code Set<String>}).
   *   <li>{@code intermediate}: 복합 제작 중 각 선행 화폐를 적용한 직후의 실현 가능한 아이템 이력이다.
   *   <li>{@code finalStepChance}: 준비 이후 마지막 단일 화폐의 조건부 결과 확률이다.
   *   <li>{@code expectedChaos}: 이 경로에서 목표가 붙을 때까지 반복하는 카오스의 조건부 기대 소모량이다.
   *   <li>{@code preparationOnly}: 준비 작업에서 이미 목표 상태로 종료되어 후속 화폐를 사용하지 않았는지 표시한다.
   *   <li>{@code probability}: 확률 ({@code CertifiedMethodProtocol.Bounds}).
   * </ul>
   */
  public record JointCase(
      List<ModifierInstance> local,
      Map<String, AutomaticMethodGraph.TierLaw> tiers,
      Map<String, AutomaticCraftingKernel.RollBand> rolls,
      Set<String> retainedFromSource,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
          boolean preparationOnly,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
          List<ItemState> intermediate,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          PathSearchProtocol.Probability expectedChaos,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          PathSearchProtocol.Probability finalStepChance,
      CertifiedMethodProtocol.Bounds probability) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 종료·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code target}: 대상 ({@code Integer}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code conditional}: 조건부 ({@code List<JointCase>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code ExitView(target, kind, probability, conditional)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       target}(대상), {@code kind}(종류), {@code probability}(확률), {@code conditional}(조건부).
   *   <li>{@code ExitView(target, kind, probability)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       target}(대상), {@code kind}(종류), {@code probability}(확률).
   * </ul>
   */
  public record ExitView(
      Integer target,
      String kind,
      CertifiedMethodProtocol.Bounds probability,
      List<JointCase> conditional) {
    public ExitView {
      conditional = conditional == null ? List.of() : List.copyOf(conditional);
    }

    public ExitView(Integer target, String kind, CertifiedMethodProtocol.Bounds probability) {
      this(target, kind, probability, List.of());
    }
  }

  /**
   * {@code AutomaticSearchService}에서 사용하는 지점·조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<ExitView>}).
   *   <li>{@code initialUses}: 초기값·사용량 ({@code Map<String, CertifiedMethodProtocol.Bounds>}).
   *   <li>{@code cycleUses}: 주기·사용량 ({@code Map<String, CertifiedMethodProtocol.Bounds>}).
   * </ul>
   */
  public record PointView(
      int cycles,
      boolean complete,
      List<ExitView> outcomes,
      Map<String, CertifiedMethodProtocol.Bounds> initialUses,
      Map<String, CertifiedMethodProtocol.Bounds> cycleUses) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 표시간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code source}: 출처 ({@code int}).
   *   <li>{@code initial}: 초기값 ({@code List<AutomaticCraftingAction>}).
   *   <li>{@code cycle}: 주기 ({@code List<AutomaticCraftingAction>}).
   *   <li>{@code executionMode}: 실행·모드 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<PointView>}).
   *   <li>{@code reasons}: 이유목록 ({@code Set<String>}).
   *   <li>{@code finished}: 종료 ({@code boolean}).
   * </ul>
   */
  public record Arrow(
      String id,
      int source,
      List<AutomaticCraftingAction> initial,
      List<AutomaticCraftingAction> cycle,
      String executionMode,
      List<PointView> points,
      Set<String> reasons,
      boolean finished) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 추천의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code node}: 노드 ({@code int}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code local}: 로컬 ({@code List<String>}).
   *   <li>{@code strategy}: 전략 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code strategyStatus}: 전략·상태 ({@code String}).
   *   <li>{@code strategyMethods}: 전략·제작방법목록 ({@code List<String>}).
   * </ul>
   */
  public record Recommendation(
      int node,
      int cycles,
      List<String> local,
      CertifiedMethodProtocol.Bounds strategy,
      String strategyStatus,
      List<String> strategyMethods) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 기대값·사용의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code lower}: 하한 ({@code PathSearchProtocol.Probability}).
   *   <li>{@code upper}: 상한 ({@code PathSearchProtocol.Probability}).
   * </ul>
   */
  public record ExpectedUse(
      PathSearchProtocol.Probability lower, PathSearchProtocol.Probability upper) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 정책·후보의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code decisions}: 결정목록 ({@code List<AutomaticPolicyCandidates.Decision>}).
   *   <li>{@code success}: 성공 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code rebase}: 기준변경 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code failure}: 실패 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code unresolved}: 미해결 ({@code CertifiedMethodProtocol.Bounds}).
   *   <li>{@code expectedUses}: 기대값·사용량 ({@code Map<String, ExpectedUse>}).
   *   <li>{@code usageStatus}: 사용량·상태 ({@code String}).
   *   <li>{@code rankStatus}: 순위·상태 ({@code String}).
   *   <li>{@code distinguishingDecisions}: distinguishing·결정목록 ({@code
   *       List<AutomaticPolicyCandidates.Decision>}).
   * </ul>
   */
  public record PolicyCandidate(
      String id,
      int cycles,
      List<AutomaticPolicyCandidates.Decision> decisions,
      CertifiedMethodProtocol.Bounds success,
      CertifiedMethodProtocol.Bounds rebase,
      CertifiedMethodProtocol.Bounds failure,
      CertifiedMethodProtocol.Bounds unresolved,
      Map<String, ExpectedUse> expectedUses,
      String usageStatus,
      String rankStatus,
      List<AutomaticPolicyCandidates.Decision> distinguishingDecisions) {}

  /**
   * {@code AutomaticSearchService}에서 사용하는 조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code canResume}: can·재개 ({@code boolean}).
   *   <li>{@code operations}: 연산목록 ({@code long}).
   *   <li>{@code retainedStates}: 보유·상태목록 ({@code long}).
   *   <li>{@code resources}: 자원목록 ({@code AutomaticMethodGraph.ResourceUsage}).
   *   <li>{@code nodes}: 노드목록 ({@code List<AutomaticMethodGraph.NodeView>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Arrow>}).
   *   <li>{@code recommendations}: 추천목록 ({@code List<Recommendation>}).
   *   <li>{@code rollModels}: 수치굴림·모형목록 ({@code List<String>}).
   *   <li>{@code candidates}: 후보목록 ({@code List<PolicyCandidate>}).
   *   <li>{@code rareOutcomePolicy}: 희귀·결과·정책 ({@code RareOutcomePolicy}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code View(version, id, revision, generation, status, reason, complete, canResume,
   *       operations, retainedStates, resources, nodes, methods, recommendations, rollModels,
   *       candidates, rareOutcomePolicy)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code version}(버전), {@code
   *       id}(식별자), {@code revision}(리비전), {@code generation}(세대번호), {@code status}(상태), {@code
   *       reason}(이유), {@code complete}(완료), {@code canResume}(can·재개), {@code operations}(연산목록),
   *       {@code retainedStates}(보유·상태목록), {@code resources}(자원목록), {@code nodes}(노드목록), {@code
   *       methods}(제작방법목록), {@code recommendations}(추천목록), {@code rollModels}(수치굴림·모형목록), {@code
   *       candidates}(후보목록), {@code rareOutcomePolicy}(희귀·결과·정책).
   *   <li>{@code View(version, id, revision, generation, status, reason, complete, canResume,
   *       operations, retainedStates, resources, nodes, methods, recommendations, rollModels)}: 기본
   *       정책을 보충해 공통 생성자로 위임한다. 입력: {@code version}(버전), {@code id}(식별자), {@code revision}(리비전),
   *       {@code generation}(세대번호), {@code status}(상태), {@code reason}(이유), {@code complete}(완료),
   *       {@code canResume}(can·재개), {@code operations}(연산목록), {@code retainedStates}(보유·상태목록),
   *       {@code resources}(자원목록), {@code nodes}(노드목록), {@code methods}(제작방법목록), {@code
   *       recommendations}(추천목록), {@code rollModels}(수치굴림·모형목록).
   *   <li>{@code View(version, id, revision, generation, status, reason, complete, canResume,
   *       operations, retainedStates, resources, nodes, methods, recommendations)}: 기본 정책을 보충해 공통
   *       생성자로 위임한다. 입력: {@code version}(버전), {@code id}(식별자), {@code revision}(리비전), {@code
   *       generation}(세대번호), {@code status}(상태), {@code reason}(이유), {@code complete}(완료), {@code
   *       canResume}(can·재개), {@code operations}(연산목록), {@code retainedStates}(보유·상태목록), {@code
   *       resources}(자원목록), {@code nodes}(노드목록), {@code methods}(제작방법목록), {@code
   *       recommendations}(추천목록).
   * </ul>
   */
  public record View(
      String version,
      String id,
      long revision,
      long generation,
      String status,
      String reason,
      boolean complete,
      boolean canResume,
      long operations,
      long retainedStates,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          AutomaticMethodGraph.ResourceUsage resources,
      List<AutomaticMethodGraph.NodeView> nodes,
      List<Arrow> methods,
      List<Recommendation> recommendations,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
          List<String> rollModels,
      List<PolicyCandidate> candidates,
      RareOutcomePolicy rareOutcomePolicy) {
    public View {
      rollModels = rollModels == null ? List.of() : List.copyOf(rollModels);
      candidates = candidates == null ? List.of() : List.copyOf(candidates);
      rareOutcomePolicy =
          rareOutcomePolicy == null ? RareOutcomePolicy.disabled() : rareOutcomePolicy;
    }

    public View(
        String version,
        String id,
        long revision,
        long generation,
        String status,
        String reason,
        boolean complete,
        boolean canResume,
        long operations,
        long retainedStates,
        AutomaticMethodGraph.ResourceUsage resources,
        List<AutomaticMethodGraph.NodeView> nodes,
        List<Arrow> methods,
        List<Recommendation> recommendations,
        List<String> rollModels) {
      this(
          version,
          id,
          revision,
          generation,
          status,
          reason,
          complete,
          canResume,
          operations,
          retainedStates,
          resources,
          nodes,
          methods,
          recommendations,
          rollModels,
          List.of(),
          RareOutcomePolicy.disabled());
    }

    public View(
        String version,
        String id,
        long revision,
        long generation,
        String status,
        String reason,
        boolean complete,
        boolean canResume,
        long operations,
        long retainedStates,
        AutomaticMethodGraph.ResourceUsage resources,
        List<AutomaticMethodGraph.NodeView> nodes,
        List<Arrow> methods,
        List<Recommendation> recommendations) {
      this(
          version,
          id,
          revision,
          generation,
          status,
          reason,
          complete,
          canResume,
          operations,
          retainedStates,
          resources,
          nodes,
          methods,
          recommendations,
          List.of());
    }
  }

  /**
   * {@code AutomaticSearchService}에서 사용하는 디스크의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code MethodSearchProtocol.Create}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code long}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Control>}).
   *   <li>{@code graph}: 그래프 ({@code AutomaticMethodGraph.Checkpoint}).
   * </ul>
   */
  public record Disk(
      String version,
      String id,
      String owner,
      MethodSearchProtocol.Create request,
      long revision,
      long generation,
      String status,
      String reason,
      long expires,
      Map<String, Control> commands,
      AutomaticMethodGraph.Checkpoint graph) {}

  private final BackendSettings.AutomaticSearch settings;
  private final ObjectMapper json;
  private final AutomaticJobCodec codec;
  private final AutomaticJobRepository repository;
  private final AutomaticCurrencyPolicy policy;
  private final Function<MethodSearchProtocol.Create, Runtime> factory;
  private final Clock clock;
  private final Function<MethodSearchProtocol.Create, Runtime> historicalFactory;
  private final LinkedHashMap<String, Job> jobs = new LinkedHashMap<>(4, .75f, true);

  /**
   * {@code AutomaticSearchService} 내부의 작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태; 초기값 {@code "READY"} ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유; 초기값 {@code ""} ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code MethodSearchProtocol.Create}).
   *   <li>{@code runtime}: 실행환경 ({@code Runtime}).
   *   <li>{@code graph}: 그래프 ({@code AutomaticMethodGraph}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전; 초기값 {@code 1} ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호; 초기값 {@code 1} ({@code long}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code long}).
   *   <li>{@code responseNanos}: 응답·나노초 ({@code long}).
   *   <li>{@code responses}: responses ({@code long}).
   *   <li>{@code candidates}: 후보목록 ({@code List<PolicyCandidate>}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Control>}).
   * </ul>
   */
  private static final class Job {
    String id, owner, status = "READY", reason = "";
    MethodSearchProtocol.Create request;
    Runtime runtime;
    AutomaticMethodGraph graph;
    long revision = 1, generation = 1, expires;
    long responseNanos, responses;
    List<PolicyCandidate> candidates = List.of();
    Map<String, Control> commands = new LinkedHashMap<>();
  }

  public AutomaticSearchService(
      ObjectMapper json,
      AutomaticJobRepository repository,
      AutomaticCurrencyPolicy policy,
      Function<MethodSearchProtocol.Create, Runtime> factory,
      Clock clock) {
    this(json, repository, policy, factory, clock, factory);
  }

  public AutomaticSearchService(
      ObjectMapper json,
      AutomaticJobRepository repository,
      AutomaticCurrencyPolicy policy,
      Function<MethodSearchProtocol.Create, Runtime> factory,
      Clock clock,
      Function<MethodSearchProtocol.Create, Runtime> historicalFactory) {
    this(
        json,
        repository,
        policy,
        factory,
        clock,
        historicalFactory,
        BackendSettings.defaults().automaticSearch());
  }

  public AutomaticSearchService(
      ObjectMapper json,
      AutomaticJobRepository repository,
      AutomaticCurrencyPolicy policy,
      Function<MethodSearchProtocol.Create, Runtime> factory,
      Clock clock,
      Function<MethodSearchProtocol.Create, Runtime> historicalFactory,
      BackendSettings.AutomaticSearch settings) {
    this.settings = java.util.Objects.requireNonNull(settings);
    this.historicalFactory = historicalFactory;
    this.json =
        json.copy()
            .disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    this.repository = repository;
    this.codec = new AutomaticJobCodec(this.json, repository);
    this.policy = policy;
    this.factory = factory;
    this.clock = clock;
  }

  private static String ownerKey(String owner) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(owner.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  // Admission and idempotency must not deserialize another job's historical graph.
  // Its original bytes remain available for explicit read-only recovery/recalculation.

  // An unreadable archive is not an executable job. Keep it for explicit recovery,
  // but do not let its compatibility error block unrelated creation or admission.
  private AutomaticJobCodec.StoredHeader admissionHeader(String id) {
    try {
      return codec.header(id);
    } catch (Rejected error) {
      if (error.status() == 409 && error.code().equals("RECALCULATION_REQUIRED")) return null;
      throw error;
    }
  }

  private Disk read(String id) {
    return codec.read(id);
  }

  private void cache(Job job) {
    jobs.put(job.id, job);
    while (jobs.size() > 2) {
      var id = jobs.keySet().iterator().next();
      jobs.remove(id).graph.close();
    }
  }

  private Job restore(Disk disk) {
    if (!VERSION.equals(disk.version()) || disk.request().goal().usesRetiredTotal())
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    var job = new Job();
    job.id = disk.id();
    job.owner = disk.owner();
    job.request = disk.request();
    job.revision = disk.revision();
    job.generation = disk.generation();
    job.status = disk.status();
    job.reason = disk.reason();
    job.expires = disk.expires();
    job.commands.putAll(disk.commands());
    try {
      job.runtime = factory.apply(job.request);
      job.graph =
          AutomaticMethodGraph.restore(
              disk.graph(),
              job.runtime.objective(),
              policy,
              job.runtime.kernel(),
              job.runtime.evaluate());
    } catch (IllegalArgumentException e) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
    try {
      job.candidates = AutomaticResultProjection.evaluate(job.graph);
      return job;
    } catch (RuntimeException error) {
      job.graph.close();
      throw error;
    }
  }

  private Job load(String owner, String id) {
    if (!id.matches("[0-9a-f-]{36}") || !repository.ids().contains(id))
      throw new Rejected(404, "JOB_NOT_FOUND");
    var job = jobs.get(id);
    if (job == null) {
      var saved = read(id);
      if (!saved.owner().equals(ownerKey(owner))) throw new Rejected(404, "JOB_NOT_FOUND");
      job = restore(saved);
      cache(job);
    }
    if (!job.owner.equals(ownerKey(owner)) || job.expires < clock.millis())
      throw new Rejected(404, "JOB_NOT_FOUND");
    return job;
  }

  private Disk disk(Job job) {
    return new Disk(
        VERSION,
        job.id,
        job.owner,
        job.request,
        job.revision,
        job.generation,
        job.status,
        job.reason,
        job.expires,
        Map.copyOf(job.commands),
        job.graph.checkpoint(job.runtime.objective()));
  }

  private void save(Job job) {
    try {
      repository.save(job.id, json.writeValueAsBytes(disk(job)));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot serialize automatic job", e);
    }
  }

  public synchronized View create(String owner, MethodSearchProtocol.Create request) {
    return create(owner, new Input(request, false));
  }

  public synchronized View create(String owner, Input input) {
    if (input == null
        || input.abstractQuality() < 0
        || input.abstractQuality() > 40
        || input.abstractQuality40() != (input.abstractQuality() == 40))
      throw new Rejected(422, "INVALID_REQUEST");
    var request = input.request();
    if (request == null
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.clientRequestId().length() > 256) throw new Rejected(422, "INVALID_REQUEST");
    for (var id : repository.ids()) {
      var saved = admissionHeader(id);
      if (saved == null) continue;
      if (saved.expires() < clock.millis()) {
        // Expiry releases admission and working memory, never the user's stored result.
        // Existing byte/document ceilings still reject new work when storage is full.
        var old = jobs.remove(id);
        if (old != null) old.graph.close();
        continue;
      }
      if (saved.owner().equals(ownerKey(owner))
          && saved.request().clientRequestId().equals(request.clientRequestId())) {
        if (!saved.request().equals(request)
            || !saved.rareOutcomePolicy().equals(json.valueToTree(input.rareOutcomePolicy()))
            || (saved.quality40() != input.abstractQuality40()
                || saved.abstractQuality() != input.abstractQuality()))
          throw new Rejected(409, "REQUEST_ID_REUSED");
        return get(owner, id);
      }
    }
    requireActiveSlot();
    var job = new Job();
    job.id = UUID.randomUUID().toString();
    job.owner = ownerKey(owner);
    job.request = request;
    job.runtime = factory.apply(request);
    if (job.runtime.minimumExplicitModifiers() > AutomaticCurrencyPolicy.MAXIMUM_EXPLORED_EXPLICITS)
      throw new Rejected(422, "GOAL_EXCEEDS_SIX_EXPLICIT_SCOPE");
    job.expires = clock.millis() + settings.ttl().toMillis();
    job.graph =
        new AutomaticMethodGraph(
            request.start().item(),
            policy,
            job.runtime.kernel(),
            job.runtime.evaluate(),
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000,
            input.abstractQuality40(),
            input.abstractQuality());
    try {
      job.graph.configureRareOutcomes(input.rareOutcomePolicy());
      job.candidates = AutomaticResultProjection.evaluate(job.graph);
      save(job);
    } catch (AutomaticJobRepository.BudgetExceeded error) {
      job.graph.close();
      throw new Rejected(429, "JOB_STORAGE_CAPACITY");
    } catch (RuntimeException error) {
      job.graph.close();
      throw error;
    }
    cache(job);
    return view(job);
  }

  /** READY owns admission; paused/cancelled work must reacquire it on RESUME. */
  private void requireActiveSlot() {
    long active =
        repository.ids().stream()
            .map(this::admissionHeader)
            .filter(java.util.Objects::nonNull)
            .filter(d -> d.expires() >= clock.millis() && d.status().equals("READY"))
            .count();
    if (active >= settings.maxActive()) throw new Rejected(429, "JOB_CAPACITY");
  }

  public synchronized View get(String owner, String id) {
    if (!id.matches("[0-9a-f-]{36}") || !repository.ids().contains(id))
      throw new Rejected(404, "JOB_NOT_FOUND");
    var saved = read(id);
    if (!saved.owner().equals(ownerKey(owner)) || saved.expires() < clock.millis())
      throw new Rejected(404, "JOB_NOT_FOUND");
    if ((AutomaticMethodGraph.VERSION.equals(saved.graph().version())
            && saved.request().goal().usesRetiredTotal())
        || "goal-directed-milestone-graph-v14-amulet-evaluation".equals(saved.graph().version())
        || "goal-directed-milestone-graph-v13-initial-chaos-stage".equals(saved.graph().version())
        || "goal-directed-milestone-graph-v12-joint-attribute-laws".equals(saved.graph().version())
        || "goal-directed-milestone-graph-v11-tier-laws".equals(saved.graph().version())
        || "goal-directed-milestone-graph-v10-quality-goal".equals(saved.graph().version())
        || "goal-directed-milestone-graph-v9-terminal-classes".equals(saved.graph().version())
        || ((AutomaticMethodGraph.VERSION.equals(saved.graph().version())
                || "goal-directed-milestone-graph-v17-strict-exalted"
                    .equals(saved.graph().version()))
            && ("approved-187-2026-11-18-first-chaos-v11"
                    .equals(saved.graph().identity().policyVersion())
                || "approved-187-2026-11-18-exalted-fallback-v10"
                    .equals(saved.graph().identity().policyVersion())
                || "approved-187-2026-11-18-session-crafting-v9"
                    .equals(saved.graph().identity().policyVersion())
                || "approved-187-2026-10-30-strict-exalted-v8"
                    .equals(saved.graph().identity().policyVersion())
                || "approved-187-2026-10-21-rarity-v4"
                    .equals(saved.graph().identity().policyVersion())
                || "approved-187-2026-10-21-goal-v3"
                    .equals(saved.graph().identity().policyVersion())
                || "automatic-scalar-kernel-v5-goal-acquisition"
                    .equals(saved.graph().identity().kernelVersion())
                || "automatic-finite-kernel-v6-declared-coupled-rolls"
                    .equals(saved.graph().identity().kernelVersion())
                || HistoricalWorkbenchModel.requiresReadOnly(saved.graph().identity().model()))))
      try {
        return SavedAutomaticResultReader.read(saved, historicalFactory.apply(saved.request()));
      } catch (RuntimeException error) {
        throw new Rejected(409, "RECALCULATION_REQUIRED");
      }
    if (LegacyAutomaticResultReader.VERSION.equals(saved.graph().version())) {
      try {
        return LegacyAutomaticResultReader.read(saved, historicalFactory.apply(saved.request()));
      } catch (RuntimeException error) {
        throw new Rejected(409, "RECALCULATION_REQUIRED");
      }
    }
    return view(load(owner, id));
  }

  public synchronized MethodSearchProtocol.Create original(String owner, String id) {
    var saved = read(id);
    if (!saved.owner().equals(ownerKey(owner))) throw new Rejected(404, "JOB_NOT_FOUND");
    // 원래 요청의 생략 필드를 유지한다. 큰 그래프를 다시 읽거나 새 스키마로 재직렬화하지 않는다.
    return saved.request().withOriginalDocument(codec.originalRequest(id));
  }

  public synchronized Input originalInput(String owner, String id) {
    var saved = read(id);
    if (!saved.owner().equals(ownerKey(owner))) throw new Rejected(404, "JOB_NOT_FOUND");
    return new Input(
        saved.request().withOriginalDocument(codec.originalRequest(id)),
        saved.graph().nodes().getFirst().quality40(),
        saved.graph().nodes().getFirst().abstractQuality(),
        saved.graph().rareOutcomePolicy());
  }

  public synchronized View control(String owner, String id, Control command) {
    var job = load(owner, id);
    if (command == null
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.operation() == null
        || !Set.of("ADVANCE", "RESUME", "CANCEL").contains(command.operation()))
      throw new Rejected(422, "INVALID_COMMAND");
    var previous = job.commands.get(command.commandId());
    if (previous != null) {
      if (!previous.equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
      return view(job);
    }
    if (command.generation() != job.generation || command.revision() != job.revision)
      throw new Rejected(409, "STALE_REVISION");
    var before = read(id);
    try {
      switch (command.operation()) {
        case "CANCEL" -> {
          job.status = "CANCELLED";
          job.reason = "";
          job.generation++;
        }
        case "RESUME" -> {
          // A fast calculation can finish immediately before a queued CANCEL.
          // Restore its actual terminal result without increasing any budget.
          if (job.graph.finished()) {
            job.status = job.graph.complete() ? "COMPLETE" : "PARTIAL";
            job.reason =
                job.graph.complete()
                    ? (job.graph.hasGoal()
                        ? "SUPPORTED_PLANS_COMPLETE"
                        : "NO_PLAN_IN_SUPPORTED_SCOPE")
                    : "UNRESOLVED_MODEL_OR_POLICY";
            job.generation++;
            break;
          }
          if (!job.status.equals("READY")) requireActiveSlot();
          var saved = job.graph.checkpoint(job.runtime.objective());
          if (job.status.equals("PAUSED")
              && (job.reason.equals("STORAGE_BUDGET") || !job.graph.canResume()))
            throw new Rejected(409, "MAXIMUM_RESOURCE_BUDGET");
          job.graph.increaseLimits(
              Math.min(128, saved.states() * 2),
              Math.min(256, saved.methods() * 2),
              Math.min(50000, saved.retained() * 2),
              Math.min(10000, saved.primitive() * 2));
          job.generation++;
          job.status = "READY";
          job.reason = "";
        }
        case "ADVANCE" -> {
          if (!job.status.equals("READY")) break;
          long until = System.nanoTime() + settings.timeSlice().toNanos();
          for (int i = 0;
              i < 2000
                  && System.nanoTime() < until
                  && !job.graph.finished()
                  && !job.graph.resourceLimited();
              i++) job.graph.step();
          job.graph.publishProgress();
          if (job.graph.resourceLimited()) {
            job.status = "PAUSED";
            job.reason = "RESOURCE_BUDGET";
          } else if (job.graph.finished()) {
            job.status = job.graph.complete() ? "COMPLETE" : "PARTIAL";
            job.reason =
                job.graph.complete()
                    ? (job.graph.hasGoal()
                        ? "SUPPORTED_PLANS_COMPLETE"
                        : "NO_PLAN_IN_SUPPORTED_SCOPE")
                    : "UNRESOLVED_MODEL_OR_POLICY";
          }
        }
        default -> throw new IllegalStateException();
      }
      if (!command.operation().equals("CANCEL"))
        job.candidates = AutomaticResultProjection.evaluate(job.graph);
      job.revision++;
      job.commands.put(command.commandId(), command);
      if (job.commands.size() > 256) job.commands.remove(job.commands.keySet().iterator().next());
      save(job);
    } catch (AutomaticJobRepository.BudgetExceeded error) {
      job.graph.close();
      jobs.remove(id);
      var paused = restore(before);
      paused.status = "PAUSED";
      paused.reason = "STORAGE_BUDGET";
      paused.revision++;
      paused.commands.put(command.commandId(), command);
      save(paused);
      cache(paused);
      return view(paused);
    } catch (RuntimeException error) {
      job.graph.close();
      jobs.remove(id);
      cache(restore(before));
      throw error;
    }
    return view(job);
  }

  public synchronized List<AbyssStrategyPlanner.Strategy> abyssStrategies(
      String owner, String id, int nodeId) {
    return load(owner, id).graph.abyssStrategies(nodeId);
  }

  /**
   * {@code AutomaticSearchService}에서 사용하는 진단값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code graph}: 그래프 ({@code AutomaticMethodGraph.Metrics}).
   *   <li>{@code responseNanos}: 응답·나노초 ({@code long}).
   *   <li>{@code responses}: responses ({@code long}).
   * </ul>
   */
  public record Diagnostics(
      AutomaticMethodGraph.Metrics graph, long responseNanos, long responses) {}

  public synchronized Diagnostics diagnostics(String owner, String id) {
    var job = load(owner, id);
    return new Diagnostics(job.graph.metrics(), job.responseNanos, job.responses);
  }

  private View view(Job job) {
    long started = System.nanoTime();
    try {
      return AutomaticResultProjection.render(
          job.id, job.revision, job.generation, job.status, job.reason, job.graph, job.candidates);
    } finally {
      job.responseNanos += System.nanoTime() - started;
      job.responses++;
    }
  }

  @Override
  public synchronized void close() {
    jobs.values().forEach(j -> j.graph.close());
    jobs.clear();
  }

  static List<Arrow> arrows(
      List<AutomaticMethodGraph.NodeView> nodes, List<AutomaticMethodGraph.MethodView> methods) {
    return AutomaticResultProjection.arrows(nodes, methods);
  }
}
