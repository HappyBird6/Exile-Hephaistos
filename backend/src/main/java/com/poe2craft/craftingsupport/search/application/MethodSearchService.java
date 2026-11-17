package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.MethodSearchProtocol.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;
import com.poe2craft.craftingsupport.search.domain.BasicTransitionClasses;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ConditionalMethodLaw;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.DeferredMixtures;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.craftingsupport.search.domain.OutcomeStrategy;
import com.poe2craft.craftingsupport.search.domain.RollPartition;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.probability.Fraction;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 세대 번호로 오래된 결과 공개를 막으며 프로세스 내부 탐색 작업을 관리한다.
 *
 * <p>기존 계약: Process-local v2 jobs. A generation token guards each publication and queued
 * continuation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings}).
 *   <li>{@code goals}: 목표목록 ({@code GoalFilterService}).
 *   <li>{@code transitions}: 전이목록 ({@code BasicCurrencyTransitions}).
 *   <li>{@code catalogs}: 공유하는 검증된 베이스 카탈로그 목록 ({@code Map<String, ItemCatalog>}).
 *   <li>{@code models}: 모형목록 ({@code Map<String, BasicCurrencyTransitions>}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code executor}: 작업실행기 ({@code Executor}).
 *   <li>{@code runBudget}: 실행·계산예산 ({@code int}).
 *   <li>{@code jobs}: 작업목록 ({@code Map<String, Job>}).
 *   <li>{@code certified}: 확률구간 ({@code CertifiedSearchJobs}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code persistence(repository)}: persistence 처리에서 {@code certified.persistence}을(를) 사용한다.
 *       입력: {@code repository}(저장소).
 *   <li>{@code MethodSearchService(catalogs, goals, identity, json)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code catalogs}(카탈로그목록), {@code goals}(목표목록), {@code identity}(정체성), {@code json}(JSON).
 *   <li>{@code MethodSearchService(catalogs, goals, identity, json, clock, executor, runBudget)}:
 *       기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code catalogs}(카탈로그목록), {@code goals}(목표목록), {@code
 *       identity}(정체성), {@code json}(JSON), {@code clock}(현재 시각 공급자), {@code executor}(작업실행기),
 *       {@code runBudget}(실행·계산예산).
 *   <li>{@code register(catalogs, identity)}: 등록여부 처리에서 {@code catalogs.put}, {@code
 *       entry.base}을(를) 사용한다. 입력: {@code catalogs}(카탈로그목록), {@code identity}(정체성).
 *   <li>{@code MethodSearchService(catalog, goals, identity, json)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code catalog}(카탈로그), {@code goals}(목표목록), {@code identity}(정체성), {@code json}(JSON).
 *   <li>{@code MethodSearchService(catalog, goals, identity, json, settings)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code catalog}(카탈로그), {@code goals}(목표목록), {@code identity}(정체성), {@code
 *       json}(JSON), {@code settings}(운영설정).
 *   <li>{@code MethodSearchService(catalogs, goals, identity, json, settings)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code catalogs}(카탈로그목록), {@code goals}(목표목록), {@code identity}(정체성), {@code
 *       json}(JSON), {@code settings}(운영설정).
 *   <li>{@code MethodSearchService(catalog, goals, identity, json, clock, executor, runBudget)}: 기본
 *       정책을 보충해 공통 생성자로 위임한다. 입력: {@code catalog}(카탈로그), {@code goals}(목표목록), {@code
 *       identity}(정체성), {@code json}(JSON), {@code clock}(현재 시각 공급자), {@code executor}(작업실행기),
 *       {@code runBudget}(실행·계산예산).
 *   <li>{@code MethodSearchService(catalog, goals, identity, json, clock, executor, runBudget,
 *       certifiedRunBudget, settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그), {@code
 *       goals}(목표목록), {@code identity}(정체성), {@code json}(JSON), {@code clock}(현재 시각 공급자), {@code
 *       executor}(작업실행기), {@code runBudget}(실행·계산예산), {@code certifiedRunBudget}(확률구간·실행·계산예산),
 *       {@code settings}(운영설정). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code certifiedEngine(request, precision)}: 확률구간·계산엔진 처리에서 {@code Integer.valueOf}, {@code
 *       request.version}을(를) 사용한다. 입력: {@code request}(요청), {@code precision}(정밀도). 반환: {@code
 *       CertifiedMethodSearchEngine}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code createCertified(owner, input)}: 확률구간을(를) 생성한다. 입력: {@code owner}(소유자), {@code
 *       input}(입력). 반환: {@code CertifiedMethodProtocol.Started}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code getCertified(owner, id)}: 확률구간을(를) 조회한다. 입력: {@code owner}(소유자), {@code id}(식별자).
 *       반환: {@code CertifiedMethodProtocol.Snapshot}.
 *   <li>{@code mutateCertified(owner, id, command)}: mutate·확률구간 처리에서 {@code certified.mutate}을(를)
 *       사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code command}(명령). 반환: {@code
 *       CertifiedMethodProtocol.Snapshot}.
 *   <li>{@code certifiedGraph(owner, id, revision, stateOffset, methodOffset)}: 확률구간·그래프 처리에서
 *       {@code certified.graph}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       revision}(리비전), {@code stateOffset}(상태·시작위치), {@code methodOffset}(제작방법·시작위치). 반환: {@code
 *       CertifiedMethodProtocol.Graph}.
 *   <li>{@code certifiedOutcomes(owner, id, revision, method, cycles, offset)}: 확률구간·결과목록 처리에서
 *       {@code certified.outcomes}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       revision}(리비전), {@code method}(제작방법), {@code cycles}(주기횟수), {@code offset}(시작위치). 반환:
 *       {@code CertifiedMethodProtocol.Outcomes}.
 *   <li>{@code certifiedMap(owner, id, revision, offset)}: 확률구간·맵 처리에서 {@code certified.map}을(를)
 *       사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전), {@code offset}(시작위치).
 *       반환: {@code CertifiedMapProtocol.Graph}.
 *   <li>{@code certifiedView(owner, id)}: 확률구간·조회표현 처리에서 {@code certified.view}을(를) 사용한다. 입력:
 *       {@code owner}(소유자), {@code id}(식별자). 반환: {@code CertifiedMapProtocol.View}.
 *   <li>{@code certifiedMapOutcomes(owner, id, revision, method, cycles, offset)}: 확률구간·맵·결과목록 처리에서
 *       {@code certified.mapOutcomes}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       revision}(리비전), {@code method}(제작방법), {@code cycles}(주기횟수), {@code offset}(시작위치). 반환:
 *       {@code CertifiedMapProtocol.Distribution}.
 *   <li>{@code bindCertifiedEntry(owner, id, request)}: 연결·확률구간·항목 처리에서 {@code certified.bind}을(를)
 *       사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code request}(요청). 반환: {@code
 *       CertifiedMethodProtocol.Entry}.
 *   <li>{@code certifiedEntry(owner, id, entryId, offset)}: 확률구간·항목 처리에서 {@code
 *       certified.entry}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       entryId}(항목·식별자), {@code offset}(시작위치). 반환: {@code CertifiedMethodProtocol.Entry}.
 *   <li>{@code advanceCertifiedEntry(owner, id, entryId, request)}: 진행·확률구간·항목 처리에서 {@code
 *       certified.advance}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       entryId}(항목·식별자), {@code request}(요청). 반환: {@code CertifiedMethodProtocol.Entry}.
 *   <li>{@code provenance()}: 출처이력 값을 조회·구성한다. 반환: {@code BasicCurrencyState.Provenance}.
 *   <li>{@code provenance(baseItemId)}: 출처이력 처리에서 {@code models.get}, {@code model.provenance}을(를)
 *       사용한다. 입력: {@code baseItemId}(베이스·아이템·식별자). 반환: {@code BasicCurrencyState.Provenance}. 사용하는
 *       예외: {@code Rejected}.
 *   <li>{@code create(owner, input)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code input}(입력).
 *       반환: {@code Snapshot}.
 *   <li>{@code createIndexed(owner, input)}: 색인된을(를) 생성한다. 입력: {@code owner}(소유자), {@code
 *       input}(입력). 반환: {@code IndexedMethodProtocol.Started}.
 *   <li>{@code create(owner, input, indexed)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code
 *       input}(입력), {@code indexed}(색인된). 반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code get(owner, id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code owner}(소유자), {@code id}(식별자). 반환:
 *       {@code Snapshot}.
 *   <li>{@code mutate(owner, id, command)}: mutate 처리에서 {@code Integer.valueOf}, {@code
 *       command.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code command}(명령).
 *       반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code graph(owner, id, revision, cursor)}: 그래프 처리에서 {@code job.acquire}, {@code
 *       saved.graph}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전),
 *       {@code cursor}(진행커서). 반환: {@code Graph}.
 *   <li>{@code distribution(owner, id, revision, methodId, cycles, cursor)}: 상태 또는 실행의 확률 분포를 조회한다.
 *       입력: {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전), {@code methodId}(제작방법·식별자),
 *       {@code cycles}(주기횟수), {@code cursor}(진행커서). 반환: {@code Distribution}. 사용하는 예외: {@code
 *       Rejected}.
 *   <li>{@code indexedOutcomes(owner, id, revision, methodId, cycles)}: 색인된·결과목록의 입력에 따른 분기·결과를
 *       처리한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전), {@code
 *       methodId}(제작방법·식별자), {@code cycles}(주기횟수). 반환: {@code IndexedMethodProtocol.Outcomes}.
 *   <li>{@code indexedOutcomes(owner, id, revision, methodId, cycles, cursor)}: 색인된·결과목록 처리에서
 *       {@code job.acquire}, {@code point.active}을(를) 사용한다. 입력: {@code owner}(소유자), {@code
 *       id}(식별자), {@code revision}(리비전), {@code methodId}(제작방법·식별자), {@code cycles}(주기횟수), {@code
 *       cursor}(진행커서). 반환: {@code IndexedMethodProtocol.Outcomes}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code indexed(job)}: 색인된의 입력에 따른 분기·결과를 처리한다. 입력: {@code job}(작업). 반환: {@code
 *       IndexedMethodSearchEngine}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code indexedPoint(revision, methodId, cycles)}: 색인된·지점 처리에서 {@code methods.stream},
 *       {@code m.id}을(를) 사용한다. 입력: {@code revision}(리비전), {@code methodId}(제작방법·식별자), {@code
 *       cycles}(주기횟수). 반환: {@code ExactMethodRunner.Observation<String, WorkbenchCurrency>}. 사용하는
 *       예외: {@code Rejected}.
 *   <li>{@code bindEntry(owner, id, request)}: 연결·항목 처리에서 {@code Integer.valueOf}, {@code
 *       request.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code request}(요청).
 *       반환: {@code IndexedMethodProtocol.Entry}. 사용하는 예외: {@code Rejected}, {@code
 *       IllegalArgumentException}.
 *   <li>{@code entry(owner, id, entryId, offset)}: 항목 처리에서 {@code job.acquire}을(를) 사용한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자), {@code entryId}(항목·식별자), {@code offset}(시작위치). 반환: {@code
 *       IndexedMethodProtocol.Entry}.
 *   <li>{@code advanceEntry(owner, id, entryId, request)}: 진행·항목 처리에서 {@code Integer.valueOf},
 *       {@code request.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       entryId}(항목·식별자), {@code request}(요청). 반환: {@code IndexedMethodProtocol.Entry}. 사용하는 예외:
 *       {@code Rejected}.
 *   <li>{@code retainedEntryStates(job)}: 보유·항목·상태목록 처리에서 {@code entries.values}, {@code
 *       expansion.retainedStates}을(를) 사용한다. 입력: {@code job}(작업). 반환: {@code long}.
 *   <li>{@code entryWork(job, id)}: 항목·계산작업 처리에서 {@code entries.get}을(를) 사용한다. 입력: {@code job}(작업),
 *       {@code id}(식별자). 반환: {@code EntryWork}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code entryView(job, work, offset)}: 항목·조회표현 처리에서 {@code expansion.complete}, {@code
 *       expansion.result}을(를) 사용한다. 입력: {@code job}(작업), {@code work}(계산작업), {@code offset}(시작위치).
 *       반환: {@code IndexedMethodProtocol.Entry}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code position(saved, cursor, scope)}: 진행위치 처리에서 {@code offset.compareTo}, {@code
 *       BigInteger.valueOf}을(를) 사용한다. 입력: {@code saved}(저장된), {@code cursor}(진행커서), {@code
 *       scope}(범위). 반환: {@code int}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code bigPosition(saved, cursor, scope)}: big·진행위치 처리에서 {@code cursors.get}, {@code
 *       scope.equals}을(를) 사용한다. 입력: {@code saved}(저장된), {@code cursor}(진행커서), {@code scope}(범위).
 *       반환: {@code java.math.BigInteger}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code revision(job, revision)}: 리비전 처리에서 {@code revisions.get}을(를) 사용한다. 입력: {@code
 *       job}(작업), {@code revision}(리비전). 반환: {@code Revision}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code submit(job)}: submit 처리에서 {@code executor.execute}을(를) 사용한다. 입력: {@code job}(작업).
 *   <li>{@code run(job, generation)}: 작업의 실행 루프를 수행한다. 입력: {@code job}(작업), {@code
 *       generation}(세대번호).
 *   <li>{@code localRecommendations(states, bySource)}: 로컬·추천목록 처리에서 {@code source.expanded},
 *       {@code bySource.getOrDefault}을(를) 사용한다. 입력: {@code states}(상태목록), {@code bySource}(기준·출처).
 *       반환: {@code List<Local>}.
 *   <li>{@code publish(job)}: 새 결과나 체크포인트를 외부 조회에 공개한다. 입력: {@code job}(작업).
 *   <li>{@code job(owner, id)}: 작업 처리에서 {@code jobs.get}, {@code owner.equals}을(를) 사용한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자). 반환: {@code Job}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code expire()}: expire 처리에서 {@code jobs.values}, {@code clock.instant}을(를) 사용한다.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code p(f)}: p 처리에서 {@code Probability.of}을(를) 사용한다. 입력: {@code f}(f). 반환: {@code
 *       Probability}.
 *   <li>{@code tierView(state)}: 티어·조회표현 처리에서 {@code state.tiers}, {@code result.put}을(를) 사용한다. 입력:
 *       {@code state}(상태). 반환: {@code Map<String, List<TierVariant>>}.
 *   <li>{@code orbitView(state)}: 교환가능계열·조회표현 처리에서 {@code state.orbits}, {@code result.put}을(를)
 *       사용한다. 입력: {@code state}(상태). 반환: {@code Map<String, OrbitView>}.
 *   <li>{@code compare(a, b)}: 비교 처리에서 {@code a.numerator}, {@code b.denominator}을(를) 사용한다. 입력:
 *       {@code a}(a), {@code b}(b). 반환: {@code int}.
 *   <li>{@code bucket(p)}: 상태버킷 처리에서 {@code p.goal}, {@code p.entries}을(를) 사용한다. 입력: {@code p}(p).
 *       반환: {@code String}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code EntryWork}: {@code MethodSearchService} 내부의 항목·계산작업 처리를 맡는다.
 *   <li>{@code DistributionCell}: {@code MethodSearchService}에서 사용하는 확률분포·셀의 불변 값이다.
 *   <li>{@code Job}: {@code MethodSearchService} 내부의 작업 처리를 맡는다.
 *   <li>{@code LockGuard}: {@code MethodSearchService} 내부의 잠금·Guard 처리를 맡는다.
 *   <li>{@code Command}: {@code MethodSearchService}에서 사용하는 명령의 불변 값이다.
 *   <li>{@code CachedArrow}: {@code MethodSearchService}에서 사용하는 캐시된·표시간선의 불변 값이다.
 *   <li>{@code Position}: {@code MethodSearchService}에서 사용하는 진행위치의 불변 값이다.
 *   <li>{@code Revision}: {@code MethodSearchService} 내부의 리비전 처리를 맡는다.
 * </ul>
 */
public final class MethodSearchService implements AutoCloseable {
  private final BackendSettings settings;
  private final GoalFilterService goals;
  private final BasicCurrencyTransitions transitions;
  private final Map<String, ItemCatalog> catalogs = new LinkedHashMap<>();
  private final Map<String, BasicCurrencyTransitions> models = new LinkedHashMap<>();
  private final ObjectMapper json;
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final Map<String, Job> jobs = new LinkedHashMap<>();
  private final CertifiedSearchJobs certified;

  public void persistence(CertifiedJobRepository repository) {
    certified.persistence(repository, json);
  }

  public MethodSearchService(
      List<ItemCatalog> catalogs, GoalFilterService goals, String identity, ObjectMapper json) {
    this(
        catalogs.stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow(),
        goals,
        identity,
        json);
    register(catalogs, identity);
  }

  public MethodSearchService(
      List<ItemCatalog> catalogs,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget) {
    this(catalogs.getFirst(), goals, identity, json, clock, executor, runBudget);
    register(catalogs, identity);
  }

  private void register(List<ItemCatalog> catalogs, String identity) {
    for (var entry : catalogs) {
      this.catalogs.put(entry.base().id(), entry);
      models.computeIfAbsent(
          entry.base().id(), ignored -> BasicCurrencyTransitions.forMethodSearch(entry, identity));
    }
  }

  public MethodSearchService(
      ItemCatalog catalog, GoalFilterService goals, String identity, ObjectMapper json) {
    this(catalog, goals, identity, json, BackendSettings.defaults());
  }

  public MethodSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      BackendSettings settings) {
    this(
        catalog,
        goals,
        identity,
        json,
        Clock.systemUTC(),
        SearchExecutors.singleWorker("crafting-method-search", settings.methodSearch().maxJobs()),
        settings.methodSearch().runBudget(),
        settings.methodSearch().certifiedRunBudget(),
        settings);
  }

  public MethodSearchService(
      List<ItemCatalog> catalogs,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      BackendSettings settings) {
    this(
        catalogs.stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow(),
        goals,
        identity,
        json,
        settings);
    register(catalogs, identity);
  }

  public MethodSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget) {
    this(
        catalog,
        goals,
        identity,
        json,
        clock,
        executor,
        runBudget,
        runBudget,
        BackendSettings.defaults());
  }

  private MethodSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget,
      int certifiedRunBudget,
      BackendSettings settings) {
    this.settings = Objects.requireNonNull(settings);
    this.goals = goals;
    this.transitions = BasicCurrencyTransitions.forMethodSearch(catalog, identity);
    catalogs.put(catalog.base().id(), catalog);
    models.put(catalog.base().id(), transitions);
    this.json = json.copy();
    this.clock = clock;
    this.executor = executor;
    if (runBudget < 1) throw new IllegalArgumentException("Positive run budget required");
    this.runBudget = runBudget;
    certified =
        new CertifiedSearchJobs(
            clock, executor, certifiedRunBudget, this::certifiedEngine, settings.certifiedSearch());
  }

  private CertifiedMethodSearchEngine certifiedEngine(
      Create request, CertifiedInterval.Precision precision) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.clientRequestId().length() > 256
        || request.start() == null
        || request.goal() == null
        || request.activeOmens() == null) throw new Rejected(422, "INVALID_REQUEST");
    var catalog = catalogs.get(request.start().item().baseItemId());
    if (catalog == null) throw new Rejected(422, "UNSUPPORTED_BASE");
    var model = models.get(catalog.base().id());
    if (!model.provenance().equals(request.start().provenance()))
      throw new Rejected(422, "RULESET_IDENTITY_MISMATCH");
    if (!new ItemStateValidator(catalog).validate(request.start().item()).isEmpty())
      throw new Rejected(422, "INVALID_ITEM");
    if (!request.activeOmens().isEmpty()) throw new Rejected(422, "INDEXED_OMENS_UNSUPPORTED");
    var evaluator = goals.compileEvaluation(request.start().item(), request.goal());
    var classes =
        new BasicTransitionClasses(
            catalog, model, goals.rollPartition(request.start().item(), request.goal()), evaluator);
    return new CertifiedMethodSearchEngine(
        request.start(),
        classes,
        evaluator,
        settings.methodSearch().kernelOutcomeLimit(),
        precision);
  }

  public CertifiedMethodProtocol.Started createCertified(
      String owner, CertifiedMethodProtocol.Create input) {
    try {
      var request =
          json.readValue(json.writeValueAsBytes(input), CertifiedMethodProtocol.Create.class);
      synchronized (jobs) {
        expire();
        if (request != null
            && request.request() != null
            && jobs.values().stream()
                .anyMatch(
                    j ->
                        j.owner.equals(owner)
                            && j.request
                                .clientRequestId()
                                .equals(request.request().clientRequestId())))
          throw new Rejected(409, "REQUEST_ID_REUSED");
        return certified.create(owner, request);
      }
    } catch (Rejected ex) {
      throw ex;
    } catch (java.io.IOException | IllegalArgumentException ex) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
  }

  public CertifiedMethodProtocol.Snapshot getCertified(String owner, String id) {
    return certified.get(owner, id);
  }

  public CertifiedMethodProtocol.Snapshot mutateCertified(
      String owner, String id, Mutation command) {
    return certified.mutate(owner, id, command);
  }

  public CertifiedMethodProtocol.Graph certifiedGraph(
      String owner, String id, long revision, int stateOffset, int methodOffset) {
    return certified.graph(owner, id, revision, stateOffset, methodOffset);
  }

  public CertifiedMethodProtocol.Outcomes certifiedOutcomes(
      String owner, String id, long revision, String method, int cycles, int offset) {
    return certified.outcomes(owner, id, revision, method, cycles, offset);
  }

  public CertifiedMapProtocol.Graph certifiedMap(
      String owner, String id, long revision, int offset) {
    return certified.map(owner, id, revision, offset);
  }

  public CertifiedMapProtocol.View certifiedView(String owner, String id) {
    return certified.view(owner, id);
  }

  public CertifiedMapProtocol.Distribution certifiedMapOutcomes(
      String owner, String id, long revision, String method, int cycles, int offset) {
    return certified.mapOutcomes(owner, id, revision, method, cycles, offset);
  }

  public CertifiedMethodProtocol.Entry bindCertifiedEntry(
      String owner, String id, IndexedMethodProtocol.Bind request) {
    return certified.bind(owner, id, request);
  }

  public CertifiedMethodProtocol.Entry certifiedEntry(
      String owner, String id, String entryId, int offset) {
    return certified.entry(owner, id, entryId, offset);
  }

  public CertifiedMethodProtocol.Entry advanceCertifiedEntry(
      String owner, String id, String entryId, IndexedMethodProtocol.Advance request) {
    return certified.advance(owner, id, entryId, request);
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public BasicCurrencyState.Provenance provenance(String baseItemId) {
    var model = models.get(baseItemId);
    if (model == null) throw new Rejected(422, "UNSUPPORTED_BASE");
    return model.provenance();
  }

  public Snapshot create(String owner, Create input) {
    return create(owner, input, false);
  }

  public IndexedMethodProtocol.Started createIndexed(String owner, Create input) {
    var snapshot = create(owner, input, true);
    var job = job(owner, snapshot.jobId());
    try (var ignored = job.acquire()) {
      return new IndexedMethodProtocol.Started(
          job.snapshot, entryView(job, job.entries.get("root"), 0));
    }
  }

  private Snapshot create(String owner, Create input, boolean indexed) {
    Create request;
    try {
      request = json.readValue(json.writeValueAsBytes(input), Create.class);
    } catch (java.io.IOException ex) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.clientRequestId().length() > 256
        || request.start() == null
        || request.goal() == null
        || request.activeOmens() == null) throw new Rejected(422, "INVALID_REQUEST");
    var catalog = catalogs.get(request.start().item().baseItemId());
    if (catalog == null) throw new Rejected(422, "UNSUPPORTED_BASE");
    var transitions = models.get(catalog.base().id());
    if (!transitions.provenance().equals(request.start().provenance()))
      throw new Rejected(422, "RULESET_IDENTITY_MISMATCH");
    if (!request.start().item().baseItemId().equals(catalog.base().id()))
      throw new Rejected(422, "UNSUPPORTED_BASE");
    if (!new ItemStateValidator(catalog).validate(request.start().item()).isEmpty())
      throw new Rejected(422, "INVALID_ITEM");
    request.activeOmens().forEach(WorkbenchOmen::fromId);
    var evaluator = goals.compileEvaluation(request.start().item(), request.goal());
    synchronized (jobs) {
      expire();
      if (certified.hasRequest(owner, request.clientRequestId()))
        throw new Rejected(409, "REQUEST_ID_REUSED");
      for (var job : jobs.values())
        if (job.owner.equals(owner)
            && job.request.clientRequestId().equals(request.clientRequestId())) {
          if (!job.request.equals(request)
              || indexed != (job.calculation instanceof IndexedMethodSearchEngine))
            throw new Rejected(409, "REQUEST_ID_REUSED");
          try (var ignored = job.acquire()) {
            return job.snapshot;
          }
        }
      if (jobs.size() >= settings.methodSearch().maxJobs())
        throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var partition = goals.rollPartition(request.start().item(), request.goal());
      if (indexed && !request.activeOmens().isEmpty())
        throw new Rejected(422, "INDEXED_OMENS_UNSUPPORTED");
      var classes = new BasicTransitionClasses(catalog, transitions, partition, evaluator);
      var mixtures = new DeferredMixtures(classes);
      var job =
          new Job(
              owner,
              request,
              indexed
                  ? new IndexedMethodSearchEngine(
                      request.start(),
                      classes,
                      evaluator,
                      settings.methodSearch().kernelOutcomeLimit())
                  : new DeferredMethodSearchEngine(
                      new MethodSearchCalculation(
                          request.start(),
                          evaluator,
                          transitions,
                          Set.copyOf(request.activeOmens()),
                          settings.methodSearch().kernelOutcomeLimit(),
                          partition,
                          mixtures)));
      jobs.put(job.id, job);
      try (var ignored = job.acquire()) {
        publish(job);
        var accepted = job.snapshot;
        submit(job);
        return accepted;
      }
    }
  }

  public Snapshot get(String owner, String id) {
    return job(owner, id).snapshot;
  }

  public Snapshot mutate(String owner, String id, Mutation command) {
    if (command == null
        || !Integer.valueOf(2).equals(command.version())
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.expectedRevision() == null
        || command.operation() == null
        || !Set.of("CANCEL", "RESUME").contains(command.operation()))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var old = job.commands.get(command.commandId());
      if (old != null) {
        if (!old.command.equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.response;
      }
      if (job.commands.size() >= settings.methodSearch().maxCommands())
        throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      if (command.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (command.operation().equals("CANCEL")) {
        if (Set.of("QUEUED", "RUNNING", "PAUSED").contains(job.status)
            || (job.calculation instanceof IndexedMethodSearchEngine
                && job.status.equals("COMPLETED"))) {
          job.generation++;
          job.status = "CANCELLED";
          publish(job);
        }
      } else {
        if (!job.snapshot.resumable()) throw new Rejected(409, "JOB_NOT_RESUMABLE");
        if (command.retainedOutcomeLimit() != null) {
          if (command.retainedOutcomeLimit() < 100000 || command.retainedOutcomeLimit() > 1000000)
            throw new Rejected(422, "INVALID_BUDGET");
          try {
            job.calculation.increaseMemoryBudget(command.retainedOutcomeLimit());
          } catch (IllegalArgumentException ex) {
            // A previously enlarged job cannot shrink its retained-outcome allowance.
            throw new Rejected(422, "INVALID_BUDGET");
          }
        }
        job.generation++;
        job.status = "QUEUED";
        job.reason = null;
        publish(job);
        submit(job);
      }
      job.commands.put(command.commandId(), new Command(command, job.snapshot));
      return job.snapshot;
    }
  }

  public Graph graph(String owner, String id, long revision, String cursor) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var saved = revision(job, revision);
      return saved.graph(position(saved, cursor, "graph"));
    }
  }

  public Distribution distribution(
      String owner, String id, long revision, String methodId, int cycles, String cursor) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var saved = revision(job, revision);
      var method =
          saved.methods.stream()
              .filter(m -> m.id().equals(methodId))
              .findFirst()
              .orElseThrow(() -> new Rejected(422, "INVALID_METHOD"));
      var point = method.points().get(cycles);
      if (point == null) throw new Rejected(409, "OBSERVATION_NOT_READY");
      String scope = "distribution:" + methodId + ":" + cycles;
      var offset = bigPosition(saved, cursor, scope);
      var rows = new ArrayList<ConditionalState>();
      var cells = new ArrayList<DistributionCell>();
      var unresolved = Fraction.ZERO;
      for (var exit : point.exits().entrySet()) {
        if (exit.getKey() == ExactMethodRunner.Exit.UNRESOLVED)
          for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
        else
          exit.getValue()
              .forEach(
                  (s, mass) ->
                      cells.add(new DistributionCell(saved.cell(s), exit.getKey().name(), mass)));
      }
      point
          .active()
          .forEach((s, mass) -> cells.add(new DistributionCell(saved.cell(s), "ACTIVE", mass)));
      cells.sort(
          Comparator.comparing((DistributionCell cell) -> cell.state().canonicalKey())
              .thenComparing(DistributionCell::kind));
      var total =
          cells.stream()
              .map(cell -> cell.state().cardinality())
              .reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
      if (offset.compareTo(total) > 0) throw new Rejected(422, "INVALID_CURSOR");
      var skip = offset;
      for (var cell : cells) {
        var size = cell.state().cardinality();
        if (skip.compareTo(size) >= 0) {
          skip = skip.subtract(size);
          continue;
        }
        while (skip.compareTo(size) < 0 && rows.size() < 48) {
          var concrete = cell.state().concrete(skip);
          var mass = cell.mass().multiply(cell.state().concreteProbability(skip));
          rows.add(
              new ConditionalState(concrete.canonicalKey(), concrete.item(), cell.kind(), p(mass)));
          skip = skip.add(java.math.BigInteger.ONE);
        }
        if (rows.size() == 48) break;
        skip = java.math.BigInteger.ZERO;
      }
      var end = offset.add(java.math.BigInteger.valueOf(rows.size()));
      return new Distribution(
          2,
          id,
          revision,
          methodId,
          cycles,
          List.copyOf(rows),
          p(unresolved),
          end.compareTo(total) < 0 ? saved.cursor(scope, end) : null);
    }
  }

  public IndexedMethodProtocol.Outcomes indexedOutcomes(
      String owner, String id, long revision, String methodId, int cycles) {
    return indexedOutcomes(owner, id, revision, methodId, cycles, null);
  }

  public IndexedMethodProtocol.Outcomes indexedOutcomes(
      String owner, String id, long revision, String methodId, int cycles, String cursor) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      indexed(job);
      var saved = revision(job, revision);
      var point = indexedPoint(saved, methodId, cycles);
      String scope = "class-outcomes:" + methodId + ":" + cycles;
      int offset = position(saved, cursor, scope);
      int total = point.active().size() + point.exits().values().stream().mapToInt(Map::size).sum();
      if (offset > total) throw new Rejected(422, "INVALID_CURSOR");
      var rows = new ArrayList<IndexedMethodProtocol.Outcome>();
      int at = 0;
      for (var exit : point.exits().entrySet())
        for (var entry : exit.getValue().entrySet()) {
          if (at++ >= offset && rows.size() < 48)
            rows.add(
                new IndexedMethodProtocol.Outcome(
                    exit.getKey().name(), entry.getKey(), p(entry.getValue())));
        }
      for (var entry : point.active().entrySet()) {
        if (at++ >= offset && rows.size() < 48)
          rows.add(
              new IndexedMethodProtocol.Outcome("ACTIVE", entry.getKey(), p(entry.getValue())));
      }
      int end = offset + rows.size();
      return new IndexedMethodProtocol.Outcomes(
          id,
          revision,
          methodId,
          cycles,
          List.copyOf(rows),
          end < total ? saved.cursor(scope, end) : null);
    }
  }

  private static IndexedMethodSearchEngine indexed(Job job) {
    if (!(job.calculation instanceof IndexedMethodSearchEngine engine))
      throw new Rejected(409, "NOT_INDEXED_SEARCH");
    return engine;
  }

  private ExactMethodRunner.Observation<String, WorkbenchCurrency> indexedPoint(
      Revision revision, String methodId, int cycles) {
    var method =
        revision.methods.stream()
            .filter(m -> m.id().equals(methodId))
            .findFirst()
            .orElseThrow(() -> new Rejected(422, "INVALID_METHOD"));
    var point = method.points().get(cycles);
    if (point == null) throw new Rejected(409, "OBSERVATION_NOT_READY");
    return point;
  }

  public IndexedMethodProtocol.Entry bindEntry(
      String owner, String id, IndexedMethodProtocol.Bind request) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.requestId() == null
        || request.requestId().isBlank()
        || request.requestId().length() > 256
        || request.revision() == null
        || request.parentEntryId() == null
        || request.methodId() == null
        || request.cycles() == null
        || request.kind() == null
        || request.targetStateId() == null) throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var engine = indexed(job);
      var old = job.bindings.get(request.requestId());
      String entryId = job.entryIds.get(request.requestId());
      if (old != null) {
        if (!old.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
        return entryView(job, job.entries.get(entryId), 0);
      }
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "JOB_CANCELLED");
      if (job.entries.size() >= 65) throw new Rejected(503, "ENTRY_CAPACITY_REACHED");
      indexedPoint(revision(job, request.revision()), request.methodId(), request.cycles());
      var parent = job.entries.get(request.parentEntryId());
      if (parent == null) throw new Rejected(422, "INVALID_ENTRY");
      try {
        var kind =
            request.kind().equals("ACTIVE")
                ? Optional.<ExactMethodRunner.Exit>empty()
                : Optional.of(ExactMethodRunner.Exit.valueOf(request.kind()));
        if (kind.filter(k -> k == ExactMethodRunner.Exit.UNRESOLVED).isPresent())
          throw new IllegalArgumentException("Unresolved has no known law");
        var law =
            engine
                .observation(request.methodId(), request.cycles())
                .condition(parent.law, kind, engine.classId(request.targetStateId()));
        entryId = UUID.randomUUID().toString();
        var work = new EntryWork(entryId, request.parentEntryId(), law);
        job.entries.put(entryId, work);
        job.bindings.put(request.requestId(), request);
        job.entryIds.put(request.requestId(), entryId);
        return entryView(job, work, 0);
      } catch (IllegalArgumentException ex) {
        throw new Rejected(422, "INVALID_CONDITIONAL_ENTRY");
      }
    }
  }

  public IndexedMethodProtocol.Entry entry(String owner, String id, String entryId, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      indexed(job);
      return entryView(job, entryWork(job, entryId), offset);
    }
  }

  public IndexedMethodProtocol.Entry advanceEntry(
      String owner, String id, String entryId, IndexedMethodProtocol.Advance request) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.expectedGeneration() == null
        || request.maxSteps() == null
        || request.maxSteps() < 1
        || request.maxSteps() > 4096
        || request.retainedStateLimit() == null
        || request.retainedStateLimit() < 1
        || request.retainedStateLimit() > 1000000) throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      indexed(job);
      if (request.expectedGeneration() != job.generation)
        throw new Rejected(409, "GENERATION_CONFLICT");
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "JOB_CANCELLED");
      var work = entryWork(job, entryId);
      if (request.retainedStateLimit() < job.entryLimit) throw new Rejected(422, "INVALID_BUDGET");
      job.entryLimit = request.retainedStateLimit();
      long until = System.nanoTime() + settings.methodSearch().timeSlice().toNanos();
      for (int n = 0;
          n < request.maxSteps()
              && !work.expansion.complete()
              && retainedEntryStates(job) <= job.entryLimit
              && System.nanoTime() < until;
          n++) work.expansion.step();
      return entryView(job, work, 0);
    }
  }

  private static long retainedEntryStates(Job job) {
    return job.entries.values().stream().mapToLong(e -> e.expansion.retainedStates()).sum();
  }

  private static EntryWork entryWork(Job job, String id) {
    var work = job.entries.get(id);
    if (work == null) throw new Rejected(422, "INVALID_ENTRY");
    return work;
  }

  private IndexedMethodProtocol.Entry entryView(Job job, EntryWork work, int offset) {
    if (offset < 0) throw new Rejected(422, "INVALID_CURSOR");
    boolean complete = work.expansion.complete();
    var rows = new ArrayList<ConditionalState>();
    Integer next = null;
    if (complete) {
      var law = work.expansion.result();
      if (offset > law.size()) throw new Rejected(422, "INVALID_CURSOR");
      int at = 0;
      for (var item : law.entrySet()) {
        if (at++ < offset) continue;
        if (rows.size() == 48) {
          next = offset + rows.size();
          break;
        }
        rows.add(
            new ConditionalState(
                item.getKey().canonicalKey(),
                item.getKey().item(),
                "CONDITIONAL",
                p(item.getValue())));
      }
    } else if (offset != 0) throw new Rejected(409, "ENTRY_NOT_READY");
    String reason = !complete && retainedEntryStates(job) > job.entryLimit ? "MEMORY_BUDGET" : null;
    return new IndexedMethodProtocol.Entry(
        work.id,
        "class:" + work.law.classId(),
        work.parent,
        complete
            ? "COMPLETE"
            : job.status.equals("CANCELLED") ? "CANCELLED" : reason == null ? "PENDING" : "PAUSED",
        reason,
        p(work.law.probability()),
        job.generation,
        List.copyOf(rows),
        next);
  }

  /**
   * {@code MethodSearchService} 내부의 항목·계산작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code parent}: 상위값 ({@code String}).
   *   <li>{@code law}: 분포 ({@code ConditionalMethodLaw}).
   *   <li>{@code expansion}: 확장 ({@code ConditionalMethodLaw.Expansion}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code EntryWork(id, parent, law)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       parent}(상위값), {@code law}(분포).
   * </ul>
   */
  private static final class EntryWork {
    final String id, parent;
    final ConditionalMethodLaw law;
    final ConditionalMethodLaw.Expansion expansion;

    EntryWork(String id, String parent, ConditionalMethodLaw law) {
      this.id = id;
      this.parent = parent;
      this.law = law;
      expansion = law.expansion();
    }
  }

  private int position(Revision saved, String cursor, String scope) {
    var offset = bigPosition(saved, cursor, scope);
    if (offset.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
      throw new Rejected(422, "INVALID_CURSOR");
    return offset.intValueExact();
  }

  private java.math.BigInteger bigPosition(Revision saved, String cursor, String scope) {
    if (cursor == null) return java.math.BigInteger.ZERO;
    var position = saved.cursors.get(cursor);
    if (position == null || !position.scope.equals(scope))
      throw new Rejected(422, "INVALID_CURSOR");
    return position.offset;
  }

  /**
   * {@code MethodSearchService}에서 사용하는 확률분포·셀의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code DeferredItem}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
   * </ul>
   */
  private record DistributionCell(DeferredItem state, String kind, Fraction mass) {}

  private Revision revision(Job job, long revision) {
    var saved = job.revisions.get(revision);
    if (saved == null) throw new Rejected(410, "REVISION_EXPIRED");
    return saved;
  }

  private void submit(Job job) {
    long generation = job.generation;
    try {
      executor.execute(() -> run(job, generation));
    } catch (RejectedExecutionException ex) {
      job.status = "PAUSED";
      job.reason = "WORKER_CAPACITY";
      publish(job);
    }
  }

  private void run(Job job, long generation) {
    int used = 0, lastPublishedWork = 0;
    while (used < runBudget) {
      try (var ignored = job.acquire()) {
        if (generation != job.generation || !Set.of("QUEUED", "RUNNING").contains(job.status))
          return;
        job.status = "RUNNING";
        try {
          // Each slice publishes a coherent revision; queued controls acquire the fair lock between
          // slices.
          long until = System.nanoTime() + settings.methodSearch().timeSlice().toNanos();
          int steps = 0;
          while (steps < 128
              && used < runBudget
              && System.nanoTime() < until
              && !job.calculation.complete()
              && !job.calculation.memoryLimited()) {
            job.calculation.step();
            used++;
            steps++;
          }
          if (job.calculation.complete()) {
            if (job.calculation instanceof IndexedMethodSearchEngine) {
              job.status = "COMPLETED";
              publish(job);
              return;
            }
            boolean exact = true;
            for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
              var solver = job.solvers.computeIfAbsent(cycles, n -> job.calculation.strategy(n));
              solver.refine();
              solver.refineExactPolicy();
              solver.refineAlmostSure();
              used++;
              exact &= solver.bounds().values().stream().allMatch(OutcomeStrategy.Bounds::exact);
            }
            if (exact) {
              job.status = "COMPLETED";
              publish(job);
              return;
            }
          }
          if (job.calculation.memoryLimited()) {
            job.status = "PAUSED";
            job.reason = "MEMORY_BUDGET";
            publish(job);
            return;
          }
          if (used >= runBudget) {
            job.status = "PAUSED";
            job.reason = "COMPUTE_BUDGET";
            publish(job);
            return;
          }
          if (used - lastPublishedWork >= 512) {
            lastPublishedWork = used;
            publish(job);
          }
        } catch (RuntimeException ex) {
          job.status = "FAILED";
          job.reason = "CALCULATION_FAILED";
          publish(job);
          return;
        }
      }
      Thread.yield();
    }
  }

  static List<Local> localRecommendations(
      List<MethodSearchEngine.StateView> states,
      Map<String, List<MethodSearchEngine.MethodView>> bySource) {
    var locals = new ArrayList<Local>();
    for (var source : states)
      if (source.expanded()) {
        var methods = bySource.getOrDefault(source.id(), List.of());
        for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
          Fraction best = null;
          var ids = new ArrayList<String>();
          for (var method : methods) {
            var observation = method.points().get(cycles);
            if (observation == null) continue;
            var value =
                observation.exits().get(ExactMethodRunner.Exit.GOAL).values().stream()
                    .reduce(Fraction.ZERO, Fraction::add);
            if (best == null || compare(value, best) > 0) {
              best = value;
              ids.clear();
              ids.add(method.id());
            } else if (compare(value, best) == 0) ids.add(method.id());
          }
          if (best != null) {
            var selected = best;
            boolean certified =
                methods.stream()
                    .allMatch(
                        method -> {
                          var observation = method.points().get(cycles);
                          if (observation == null) return false;
                          var goal =
                              observation.exits().get(ExactMethodRunner.Exit.GOAL).values().stream()
                                  .reduce(Fraction.ZERO, Fraction::add);
                          var unknown =
                              observation
                                  .exits()
                                  .get(ExactMethodRunner.Exit.UNRESOLVED)
                                  .values()
                                  .stream()
                                  .reduce(Fraction.ZERO, Fraction::add);
                          return compare(selected, goal.add(unknown)) >= 0;
                        });
            if (certified) locals.add(new Local(source.id(), cycles, List.copyOf(ids)));
          }
        }
      }
    return List.copyOf(locals);
  }

  private void publish(Job job) {
    job.revision++;
    var saved = new Revision(job);
    job.revisions.put(job.revision, saved);
    while (job.revisions.size() > settings.methodSearch().maxRevisions())
      job.revisions.remove(job.revisions.keySet().iterator().next());
    var locals = localRecommendations(saved.states, saved.bySource);
    var strategies = new ArrayList<Strategy>();
    job.solvers.forEach(
        (cycles, solver) -> {
          solver
              .bounds()
              .forEach(
                  (state, value) -> {
                    var ids =
                        solver.certifiedAlternatives(state).stream()
                            .map(OutcomeStrategy.Recommendation::methodId)
                            .toList();
                    strategies.add(
                        new Strategy(
                            state,
                            cycles,
                            value.exact() ? "EXACT" : "BOUNDS",
                            p(value.lower()),
                            p(value.upper()),
                            ids));
                  });
        });
    job.snapshot =
        new Snapshot(
            2,
            job.id,
            job.request.clientRequestId(),
            job.revision,
            job.generation,
            job.status,
            job.reason,
            Set.of("PAUSED", "CANCELLED").contains(job.status),
            job.calculation.complete(),
            job.calculation.operations(),
            job.calculation.rootId(),
            job.request.start().provenance(),
            job.request.goal().catalogVersion(),
            job.calculation.templatesVersion(),
            job.calculation.calculationVersion(),
            job.calculation.stateVersion(),
            RollPartition.VERSION,
            job.calculation instanceof IndexedMethodSearchEngine
                ? "NOT_REQUESTED"
                : OutcomeStrategy.VERSION,
            BasicCurrencyTransitions.INTERPRETATION,
            job.calculation.assumptions(),
            List.copyOf(locals),
            List.copyOf(strategies),
            saved.graph(0));
  }

  private Job job(String owner, String id) {
    synchronized (jobs) {
      expire();
      var job = jobs.get(id);
      if (job == null || !job.owner.equals(owner)) throw new Rejected(410, "JOB_EXPIRED");
      return job;
    }
  }

  private void expire() {
    var expired = jobs.values().stream().filter(j -> !clock.instant().isBefore(j.expires)).toList();
    for (var job : expired) {
      try (var ignored = job.acquire()) {
        job.generation++;
        jobs.remove(job.id);
      }
    }
  }

  @Override
  public void close() {
    if (executor instanceof ExecutorService service) service.shutdownNow();
    certified.close();
  }

  private static Probability p(Fraction f) {
    return Probability.of(f);
  }

  static Map<String, List<TierVariant>> tierView(DeferredItem state) {
    var result = new LinkedHashMap<String, List<TierVariant>>();
    state
        .tiers()
        .forEach(
            (key, cell) ->
                result.put(
                    key,
                    cell.variants().stream()
                        .map(v -> new TierVariant(v.modifierId(), v.stat(), v.min(), v.max()))
                        .toList()));
    return Map.copyOf(result);
  }

  static Map<String, OrbitView> orbitView(DeferredItem state) {
    var result = new TreeMap<String, OrbitView>();
    state
        .orbits()
        .forEach(
            (id, cell) ->
                result.put(
                    id,
                    new OrbitView(
                        cell.representatives().stream().map(ModifierInstance::modifierId).toList(),
                        cell.assignments().stream()
                            .map(
                                a ->
                                    a.families().stream()
                                        .map(
                                            tier ->
                                                tier.variants().stream()
                                                    .map(
                                                        v ->
                                                            new TierVariant(
                                                                v.modifierId(),
                                                                v.stat(),
                                                                v.min(),
                                                                v.max()))
                                                    .toList())
                                        .toList())
                            .toList())));
    return Map.copyOf(result);
  }

  private static int compare(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  static String bucket(MethodSearchCalculation.Progress p) {
    return p.goal()
        + ":"
        + p.entries().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");
  }

  /**
   * {@code MethodSearchService} 내부의 작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code lock}: 잠금 ({@code java.util.concurrent.locks.ReentrantLock}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code Create}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code Instant}).
   *   <li>{@code calculation}: 계산 ({@code MethodSearchEngine}).
   *   <li>{@code revisions}: 리비전목록 ({@code Map<Long, Revision>}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Command>}).
   *   <li>{@code solvers}: solvers ({@code Map<Integer, OutcomeStrategy<String>>}).
   *   <li>{@code arrows}: arrows ({@code Map<String, CachedArrow>}).
   *   <li>{@code entries}: 항목목록 ({@code Map<String, EntryWork>}).
   *   <li>{@code bindings}: 결합목록 ({@code Map<String, IndexedMethodProtocol.Bind>}).
   *   <li>{@code entryIds}: 항목·식별자목록 ({@code Map<String, String>}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code entryLimit}: 항목·한도; 초기값 {@code 1} ({@code int}).
   *   <li>{@code status}: 현재 처리 상태; 초기값 {@code "QUEUED"} ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code snapshot}: 스냅샷 ({@code Snapshot}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code acquire()}: acquire 값을 조회·구성한다. 반환: {@code LockGuard}.
   *   <li>{@code Job(owner, request, calculation)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code owner}(소유자),
   *       {@code request}(요청), {@code calculation}(계산).
   * </ul>
   */
  private final class Job {
    final java.util.concurrent.locks.ReentrantLock lock =
        new java.util.concurrent.locks.ReentrantLock(true);

    LockGuard acquire() {
      lock.lock();
      return lock::unlock;
    }

    final String id = UUID.randomUUID().toString(), owner;
    final Create request;
    final Instant expires = clock.instant().plus(settings.methodSearch().ttl());
    final MethodSearchEngine calculation;
    final Map<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new LinkedHashMap<>();
    final Map<Integer, OutcomeStrategy<String>> solvers = new LinkedHashMap<>();
    final Map<String, CachedArrow> arrows = new HashMap<>();
    final Map<String, EntryWork> entries = new LinkedHashMap<>();
    final Map<String, IndexedMethodProtocol.Bind> bindings = new LinkedHashMap<>();
    final Map<String, String> entryIds = new LinkedHashMap<>();
    long revision = -1, generation;
    int entryLimit = 1;
    String status = "QUEUED", reason;
    volatile Snapshot snapshot;

    Job(String owner, Create request, MethodSearchEngine calculation) {
      this.owner = owner;
      this.request = request;
      this.calculation = calculation;
      if (calculation instanceof IndexedMethodSearchEngine indexed)
        entries.put("root", new EntryWork("root", null, indexed.rootLaw()));
    }
  }

  /**
   * {@code MethodSearchService} 내부의 잠금·Guard 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  private interface LockGuard extends AutoCloseable {
    @Override
    void close();
  }

  /**
   * {@code MethodSearchService}에서 사용하는 명령의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code command}: 명령 ({@code Mutation}).
   *   <li>{@code response}: 응답 ({@code Snapshot}).
   * </ul>
   */
  private record Command(Mutation command, Snapshot response) {}

  /**
   * {@code MethodSearchService}에서 사용하는 캐시된·표시간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code method}: 제작방법 ({@code MethodSearchEngine.MethodView}).
   *   <li>{@code arrow}: 표시간선 ({@code Arrow}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code matches(next)}: 일치결과 처리에서 {@code method.status}, {@code next.status}을(를) 사용한다. 입력:
   *       {@code next}(다음). 반환: {@code boolean}.
   * </ul>
   */
  private record CachedArrow(MethodSearchEngine.MethodView method, Arrow arrow) {
    boolean matches(MethodSearchEngine.MethodView next) {
      return method.status().equals(next.status())
          && Objects.equals(method.reason(), next.reason())
          && method.points().size() == next.points().size()
          && method.points().entrySet().stream()
              .allMatch(e -> next.points().get(e.getKey()) == e.getValue());
    }
  }

  /**
   * {@code MethodSearchService}에서 사용하는 진행위치의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code scope}: 범위 ({@code String}).
   *   <li>{@code offset}: 시작위치 ({@code java.math.BigInteger}).
   * </ul>
   */
  private record Position(String scope, java.math.BigInteger offset) {}

  /**
   * {@code MethodSearchService} 내부의 리비전 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code arrows}: arrows ({@code Map<String, CachedArrow>}).
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code number}: 숫자 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code List<MethodSearchEngine.StateView>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<MethodSearchEngine.MethodView>}).
   *   <li>{@code byId}: 기준·식별자 ({@code Map<String, MethodSearchEngine.StateView>}).
   *   <li>{@code bySource}: 기준·출처 ({@code Map<String, List<MethodSearchEngine.MethodView>>}).
   *   <li>{@code buckets}: buckets ({@code List<Bucket>}).
   *   <li>{@code cursors}: cursors ({@code Map<String, Position>}).
   *   <li>{@code tokens}: tokens ({@code Map<Position, String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Revision(job)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code job}(작업).
   *   <li>{@code cursor(scope, offset)}: 진행커서 처리에서 {@code BigInteger.valueOf}을(를) 사용한다. 입력: {@code
   *       scope}(범위), {@code offset}(시작위치). 반환: {@code String}.
   *   <li>{@code cursor(scope, offset)}: 진행커서 처리에서 {@code tokens.computeIfAbsent}, {@code
   *       UUID.randomUUID}을(를) 사용한다. 입력: {@code scope}(범위), {@code offset}(시작위치). 반환: {@code
   *       String}.
   *   <li>{@code graph(offset)}: 그래프 처리에서 {@code methods.size}, {@code settings.methodSearch}을(를)
   *       사용한다. 입력: {@code offset}(시작위치). 반환: {@code Graph}. 사용하는 예외: {@code Rejected}.
   *   <li>{@code arrow(method)}: 표시간선 처리에서 {@code arrows.get}, {@code method.id}을(를) 사용한다. 입력:
   *       {@code method}(제작방법). 반환: {@code Arrow}.
   *   <li>{@code group(groups, kind, state, mass)}: 그룹 처리에서 {@code byId.get}, {@code
   *       groups.computeIfAbsent}을(를) 사용한다. 입력: {@code groups}(그룹목록), {@code kind}(종류), {@code
   *       state}(상태), {@code mass}(확률질량). 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code cell(id)}: 셀 처리에서 {@code byId.get}, {@code state.distributionCell}을(를) 사용한다. 입력:
   *       {@code id}(식별자). 반환: {@code DeferredItem}. 사용하는 예외: {@code IllegalStateException}.
   * </ul>
   */
  private final class Revision {
    final Map<String, CachedArrow> arrows;
    final String jobId;
    final long number;
    final List<MethodSearchEngine.StateView> states;
    final List<MethodSearchEngine.MethodView> methods;
    final Map<String, MethodSearchEngine.StateView> byId = new HashMap<>();
    final Map<String, List<MethodSearchEngine.MethodView>> bySource = new HashMap<>();
    final List<Bucket> buckets;
    final Map<String, Position> cursors = new HashMap<>();
    final Map<Position, String> tokens = new HashMap<>();

    Revision(Job job) {
      arrows = job.arrows;
      jobId = job.id;
      number = job.revision;
      states = job.calculation.states();
      var groups = new LinkedHashMap<String, List<MethodSearchEngine.StateView>>();
      states.forEach(
          s -> {
            byId.put(s.id(), s);
            groups.computeIfAbsent(bucket(s.progress()), unused -> new ArrayList<>()).add(s);
          });
      methods =
          job.calculation.methods().stream()
              .sorted(
                  Comparator.comparingInt(
                          (MethodSearchEngine.MethodView m) -> byId.get(m.source()).depth())
                      .thenComparing(m -> m.points().isEmpty())
                      .thenComparing(MethodSearchEngine.MethodView::id))
              .toList();
      methods.forEach(
          method ->
              bySource.computeIfAbsent(method.source(), unused -> new ArrayList<>()).add(method));
      buckets =
          groups.entrySet().stream()
              .map(
                  e -> {
                    var first = e.getValue().getFirst();
                    return new Bucket(
                        e.getKey(),
                        e.getValue().stream()
                            .mapToInt(MethodSearchEngine.StateView::depth)
                            .min()
                            .orElseThrow(),
                        first.progress().goal().name(),
                        first.progress().entries().stream().map(Enum::name).toList(),
                        e.getValue().size(),
                        first.representative().item(),
                        first.representative().rolls(),
                        tierView(first.representative()),
                        orbitView(first.representative()),
                        e.getValue().stream().allMatch(MethodSearchEngine.StateView::complete));
                  })
              .toList();
    }

    String cursor(String scope, int offset) {
      return cursor(scope, java.math.BigInteger.valueOf(offset));
    }

    String cursor(String scope, java.math.BigInteger offset) {
      var position = new Position(scope, offset);
      return tokens.computeIfAbsent(
          position,
          key -> {
            var token = UUID.randomUUID().toString();
            cursors.put(token, key);
            return token;
          });
    }

    Graph graph(int offset) {
      if (offset > methods.size()) throw new Rejected(422, "INVALID_CURSOR");
      int end = Math.min(methods.size(), offset + settings.methodSearch().pageSize());
      return new Graph(
          2,
          jobId,
          number,
          buckets,
          methods.subList(offset, end).stream().map(this::arrow).toList(),
          methods.size(),
          end < methods.size() ? cursor("graph", end) : null);
    }

    Arrow arrow(MethodSearchEngine.MethodView method) {
      var cached = arrows.get(method.id());
      if (cached != null && cached.matches(method)) return cached.arrow();
      var source = byId.get(method.source());
      var points = new ArrayList<Point>();
      for (var entry : new TreeMap<>(method.points()).entrySet()) {
        var groups = new LinkedHashMap<String, Map<String, Fraction>>();
        var observation = entry.getValue();
        var unresolved = Fraction.ZERO;
        for (var exit : observation.exits().entrySet()) {
          if (exit.getKey() == ExactMethodRunner.Exit.UNRESOLVED) {
            for (var value : exit.getValue().values()) unresolved = unresolved.add(value);
          } else
            exit.getValue()
                .forEach((state, mass) -> group(groups, exit.getKey().name(), state, mass));
        }
        observation.active().forEach((state, mass) -> group(groups, "ACTIVE", state, mass));
        var exits =
            groups.entrySet().stream()
                .map(
                    e -> {
                      int split = e.getKey().indexOf('|');
                      return new Exit(
                          e.getKey().substring(split + 1),
                          e.getKey().substring(0, split),
                          p(e.getValue().values().stream().reduce(Fraction.ZERO, Fraction::add)),
                          e.getValue().size());
                    })
                .toList();
        var initial = new LinkedHashMap<String, Probability>();
        var cycle = new LinkedHashMap<String, Probability>();
        observation.initialUses().forEach((a, mass) -> initial.put(a.name(), p(mass)));
        observation.cycleUses().forEach((a, mass) -> cycle.put(a.name(), p(mass)));
        points.add(
            new Point(
                entry.getKey(), exits, p(unresolved), Map.copyOf(initial), Map.copyOf(cycle)));
      }
      var result =
          new Arrow(
              method.id(),
              method.source(),
              bucket(source.progress()),
              source.representative().item(),
              source.representative().rolls(),
              tierView(source.representative()),
              orbitView(source.representative()),
              method.initialActions().stream().map(Enum::name).toList(),
              method.actions().stream().map(Enum::name).toList(),
              method.status(),
              method.reason(),
              List.copyOf(points));
      arrows.put(method.id(), new CachedArrow(method, result));
      return result;
    }

    void group(
        Map<String, Map<String, Fraction>> groups, String kind, String state, Fraction mass) {
      var target = byId.get(state);
      if (target == null) throw new IllegalStateException("Missing exact exit state");
      groups
          .computeIfAbsent(kind + "|" + bucket(target.progress()), unused -> new LinkedHashMap<>())
          .merge(state, mass, Fraction::add);
    }

    DeferredItem cell(String id) {
      var state = byId.get(id);
      if (state == null) throw new IllegalStateException("Missing distribution state");
      return state
          .distributionCell()
          .orElseThrow(() -> new Rejected(409, "CONDITIONAL_DISTRIBUTION_REQUIRED"));
    }
  }
}
