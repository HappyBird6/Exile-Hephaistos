package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.PathSearchProtocol.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.SolarAmulet;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 작업별 잠금으로 조회·진행·취소를 동기화하고 작업 수와 수명을 제한한다.
 *
 * <p>기존 계약: Bounded, process-local jobs. A monitor is the publication/cancel linearization
 * boundary.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code PREDICATE}: 판정조건; 초기값 {@code "goal-filter-full-item-v1"} ({@code String}).
 *   <li>{@code SEARCH}: 탐색; 초기값 {@code "solar-streaming-renewal-v1"} ({@code String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code goals}: 목표목록 ({@code GoalFilterService}).
 *   <li>{@code transitions}: 전이목록 ({@code BasicCurrencyTransitions}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code executor}: 작업실행기 ({@code Executor}).
 *   <li>{@code runBudget}: 실행·계산예산 ({@code int}).
 *   <li>{@code jobs}: 작업목록 ({@code Map<String, Job>}).
 *   <li>{@code requestIds}: 요청·식별자목록 ({@code Map<String, String>}).
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings.PathSearch}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PathSearchService(catalog, goals, identity, json)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code catalog}(카탈로그), {@code goals}(목표목록), {@code identity}(정체성), {@code json}(JSON).
 *   <li>{@code PathSearchService(catalog, goals, identity, json, settings)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code catalog}(카탈로그), {@code goals}(목표목록), {@code identity}(정체성), {@code
 *       json}(JSON), {@code settings}(운영설정).
 *   <li>{@code PathSearchService(catalog, goals, identity, json, clock, executor, runBudget)}: 기본
 *       정책을 보충해 공통 생성자로 위임한다. 입력: {@code catalog}(카탈로그), {@code goals}(목표목록), {@code
 *       identity}(정체성), {@code json}(JSON), {@code clock}(현재 시각 공급자), {@code executor}(작업실행기),
 *       {@code runBudget}(실행·계산예산).
 *   <li>{@code PathSearchService(catalog, goals, identity, json, clock, executor, runBudget,
 *       settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그), {@code goals}(목표목록), {@code
 *       identity}(정체성), {@code json}(JSON), {@code clock}(현재 시각 공급자), {@code executor}(작업실행기),
 *       {@code runBudget}(실행·계산예산), {@code settings}(운영설정). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code provenance()}: 출처이력 값을 조회·구성한다. 반환: {@code BasicCurrencyState.Provenance}.
 *   <li>{@code create(owner, request)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code
 *       request}(요청). 반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code create(owner, request, predicate, recovery, fingerprint)}: 새 작업 또는 값을 생성한다. 입력:
 *       {@code owner}(소유자), {@code request}(요청), {@code predicate}(판정조건), {@code recovery}(복구),
 *       {@code fingerprint}(지문). 반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code get(owner, id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code owner}(소유자), {@code id}(식별자). 반환:
 *       {@code Snapshot}.
 *   <li>{@code mutate(owner, id, request)}: mutate 처리에서 {@code Integer.valueOf}, {@code
 *       request.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code request}(요청).
 *       반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code graph(owner, id, revision, cursor)}: 그래프 처리에서 {@code revisions.get}, {@code
 *       cursors.get}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전),
 *       {@code cursor}(진행커서). 반환: {@code GraphPage}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code recover(owner, id, request)}: recover 처리에서 {@code Integer.valueOf}, {@code
 *       request.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code request}(요청).
 *       반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code submit(job)}: submit 처리에서 {@code executor.execute}을(를) 사용한다. 입력: {@code job}(작업).
 *   <li>{@code run(job, generation)}: 작업의 실행 루프를 수행한다. 입력: {@code job}(작업), {@code
 *       generation}(세대번호).
 *   <li>{@code live(job, generation)}: 현재유효 처리에서 {@code Thread.currentThread}, {@code
 *       clock.instant}을(를) 사용한다. 입력: {@code job}(작업), {@code generation}(세대번호). 반환: {@code
 *       boolean}.
 *   <li>{@code publish(job)}: 새 결과나 체크포인트를 외부 조회에 공개한다. 입력: {@code job}(작업).
 *   <li>{@code validate(r)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code r}(r). 사용하는 예외: {@code Rejected}.
 *   <li>{@code observations(values)}: 관측값목록 처리에서 {@code values.isEmpty}, {@code values.size}을(를)
 *       사용한다. 입력: {@code values}(값목록). 반환: {@code List<String>}. 사용하는 예외: {@code Rejected}, {@code
 *       NumberFormatException}.
 *   <li>{@code blocked(request)}: 차단 처리에서 {@code request.start}, {@code BASE_ID.equals}을(를) 사용한다.
 *       입력: {@code request}(요청). 반환: {@code String}.
 *   <li>{@code fingerprint(r)}: 지문 처리에서 {@code r.version}, {@code r.start}을(를) 사용한다. 입력: {@code
 *       r}(r). 반환: {@code String}.
 *   <li>{@code encode(value)}: 값을 저장·전송 표현으로 부호화한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는
 *       예외: {@code Rejected}.
 *   <li>{@code digest(value)}: 다이제스트 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code job(owner, id)}: 작업 처리에서 {@code jobs.get}, {@code owner.equals}을(를) 사용한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자). 반환: {@code Job}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code expire()}: expire 처리에서 {@code jobs.values}, {@code clock.instant}을(를) 사용한다.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Job}: {@code PathSearchService} 내부의 작업 처리를 맡는다.
 *   <li>{@code Command}: {@code PathSearchService}에서 사용하는 명령의 불변 값이다.
 *   <li>{@code Revision}: {@code PathSearchService} 내부의 리비전 처리를 맡는다.
 * </ul>
 */
public final class PathSearchService implements AutoCloseable {
  public static final String PREDICATE = "goal-filter-full-item-v1",
      SEARCH = "solar-streaming-renewal-v1";
  private final ItemCatalog catalog;
  private final GoalFilterService goals;
  private final BasicCurrencyTransitions transitions;
  private final ObjectMapper json;
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final Map<String, Job> jobs = new LinkedHashMap<>();
  private final Map<String, String> requestIds = new HashMap<>();
  private final BackendSettings.PathSearch settings;

  public PathSearchService(
      ItemCatalog catalog, GoalFilterService goals, String identity, ObjectMapper json) {
    this(catalog, goals, identity, json, BackendSettings.defaults().pathSearch());
  }

  public PathSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      BackendSettings.PathSearch settings) {
    this(
        catalog,
        goals,
        identity,
        json,
        Clock.systemUTC(),
        SearchExecutors.singleWorker("crafting-path-search", settings.maxJobs()),
        settings.runBudget(),
        settings);
  }

  public PathSearchService(
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
        BackendSettings.defaults().pathSearch());
  }

  public PathSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget,
      BackendSettings.PathSearch settings) {
    if (runBudget < 1) throw new IllegalArgumentException("Positive work budget required");
    this.catalog = catalog;
    this.goals = goals;
    this.transitions = new BasicCurrencyTransitions(catalog, identity);
    this.json = json.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    this.clock = clock;
    this.executor = executor;
    this.runBudget = runBudget;
    this.settings = Objects.requireNonNull(settings);
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public Snapshot create(String owner, Create request) {
    try {
      request = json.readValue(encode(request), Create.class);
    } catch (java.io.IOException e) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
    validate(request);
    var predicate = goals.compile(request.start().item(), request.goal());
    return create(owner, request, predicate, null, fingerprint(request));
  }

  private Snapshot create(
      String owner,
      Create request,
      Function<ItemState, GoalFilter.Status> predicate,
      Recovery recovery,
      String fingerprint) {
    synchronized (jobs) {
      expire();
      String key = owner + ":" + request.clientRequestId();
      var old = requestIds.get(key);
      if (old != null) {
        var existing = jobs.get(old);
        if (!existing.fingerprint.equals(fingerprint)) throw new Rejected(409, "REQUEST_ID_REUSED");
        synchronized (existing) {
          return existing.snapshot;
        }
      }
      if (jobs.size() >= settings.maxJobs()) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var job = new Job(owner, request, fingerprint, predicate, recovery);
      jobs.put(job.id, job);
      requestIds.put(key, job.id);
      synchronized (job) {
        publish(job);
        var accepted = job.snapshot;
        if (job.blocked == null) submit(job);
        return accepted;
      }
    }
  }

  public Snapshot get(String owner, String id) {
    var job = job(owner, id);
    synchronized (job) {
      return job.snapshot;
    }
  }

  public Snapshot mutate(String owner, String id, Mutation request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.commandId() == null
        || request.commandId().isBlank()
        || request.expectedRevision() == null
        || request.expectedRevision() < 0
        || !Set.of("CANCEL", "RESUME").contains(request.operation()))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    synchronized (job) {
      var old = job.commands.get(request.commandId());
      if (old != null) {
        if (!old.request.equals(request)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.response;
      }
      if (job.commands.size() >= settings.maxCommands())
        throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      if (request.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (request.operation().equals("CANCEL")) {
        if (Set.of("QUEUED", "RUNNING", "PAUSED").contains(job.status)) {
          job.generation++;
          job.status = "CANCELLED";
          publish(job);
        }
      } else {
        if (!job.snapshot.resumable()) throw new Rejected(409, "JOB_NOT_RESUMABLE");
        job.generation++;
        job.status = "QUEUED";
        publish(job);
        submit(job);
      }
      var response = job.snapshot;
      job.commands.put(request.commandId(), new Command(request, response));
      return response;
    }
  }

  public GraphPage graph(String owner, String id, long revision, String cursor) {
    var job = job(owner, id);
    synchronized (job) {
      var saved = job.revisions.get(revision);
      if (saved == null) throw new Rejected(410, "REVISION_EXPIRED");
      int offset = 0;
      if (cursor != null) {
        var position = saved.cursors.get(cursor);
        if (position == null) throw new Rejected(422, "INVALID_CURSOR");
        offset = position;
      }
      return saved.pages.get(offset);
    }
  }

  public Snapshot recover(String owner, String id, Recover request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.parentRevision() == null
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()) throw new Rejected(422, "INVALID_REQUEST");
    observations(request.observations());
    var parent = job(owner, id);
    Create frozen;
    Recovery metadata;
    ItemState checkpoint;
    synchronized (parent) {
      var revision = parent.revisions.get(request.parentRevision());
      if (revision == null) throw new Rejected(410, "REVISION_EXPIRED");
      var failure = revision.executions.get(request.failureExecutionId());
      var target = revision.nodes.get(request.checkpointStateId());
      if (failure == null
          || target == null
          || !revision.nodes.get(failure.stateId()).goalStatus().equals("NO_MATCH"))
        throw new Rejected(422, "INVALID_RECOVERY_REFERENCE");
      // Reverse walk only actual retained edges of this policy. Sharing a job/state is
      // insufficient.
      var seen = new HashSet<String>();
      var todo = new ArrayDeque<String>();
      todo.add(failure.id());
      boolean ancestor = false;
      while (!todo.isEmpty()) {
        var execution = todo.removeFirst();
        if (!seen.add(execution)) continue;
        if (revision.executions.get(execution).stateId().equals(target.id())) {
          ancestor = true;
          break;
        }
        for (var edge : revision.edges)
          if (edge.to().equals(execution)
              && revision.executions.get(edge.from()).policyId().equals(failure.policyId()))
            todo.add(edge.from());
      }
      if (!ancestor) throw new Rejected(422, "INVALID_RECOVERY_CHECKPOINT");
      checkpoint = target.item();
      var state =
          new BasicCurrencyState(revision.nodes.get(failure.stateId()).item(), provenance());
      frozen =
          new Create(
              1,
              request.clientRequestId(),
              state,
              parent.request.goal(),
              List.of(),
              request.observations());
      metadata = new Recovery(id, request.parentRevision(), failure.id(), target.id(), true, false);
    }
    validate(frozen);
    var targetState = checkpoint;
    return create(
        owner,
        frozen,
        item -> item.equals(targetState) ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
        metadata,
        digest(fingerprint(frozen) + encode(metadata)));
  }

  private void submit(Job job) {
    long generation = job.generation;
    try {
      executor.execute(() -> run(job, generation));
    } catch (RejectedExecutionException e) {
      job.status = "PAUSED";
      job.reason = "SEARCH_CAPACITY_REACHED";
      publish(job);
    }
  }

  private void run(Job job, long generation) {
    synchronized (job) {
      if (!live(job, generation)) return;
      job.status = "RUNNING";
      publish(job);
    }
    try {
      for (int work = 0; work < runBudget; work++) {
        synchronized (job) {
          if (!live(job, generation)) return;
          job.calculation.step();
          if (job.calculation.complete()) {
            job.status = job.calculation.outputLimited() ? "PAUSED" : "COMPLETED";
            job.reason = job.calculation.outputLimited() ? "FRACTION_OUTPUT_LIMIT" : null;
            publish(job);
            return;
          }
          if ((work + 1) % 1000 == 0) publish(job);
        }
      }
      synchronized (job) {
        if (live(job, generation)) {
          job.status = "PAUSED";
          job.reason = "WORK_BUDGET_EXHAUSTED";
          publish(job);
        }
      }
    } catch (RuntimeException e) {
      synchronized (job) {
        if (live(job, generation)) {
          job.status = "FAILED";
          job.reason = "SEARCH_FAILED";
          publish(job);
        }
      }
    }
  }

  private boolean live(Job job, long generation) {
    return !Thread.currentThread().isInterrupted()
        && job.generation == generation
        && clock.instant().isBefore(job.expires)
        && Set.of("QUEUED", "RUNNING").contains(job.status);
  }

  private void publish(Job job) {
    job.revision++;
    var c = job.calculation;
    var recommendations = job.blocked == null ? c.recommendations() : List.<Recommendation>of();
    var saved = new Revision(job.id, job.revision, c, settings.pageEdges());
    job.revisions.put(job.revision, saved);
    while (job.revisions.size() > settings.maxRevisions())
      job.revisions.remove(job.revisions.keySet().iterator().next());
    var capabilities =
        new Capabilities(
            job.evaluation,
            job.blocked == null ? "SUPPORTED" : "UNSUPPORTED",
            job.blocked,
            PathSearchCalculation.ACTIONS.stream()
                .flatMap(List::stream)
                .map(Enum::name)
                .distinct()
                .toList(),
            List.of("AND", "COUNT"),
            true,
            true);
    job.snapshot =
        new Snapshot(
            1,
            job.id,
            job.request.clientRequestId(),
            job.fingerprint,
            job.revision,
            job.status,
            job.expires.toString(),
            capabilities,
            new Provenance(
                provenance(),
                job.request.goal().catalogVersion(),
                PREDICATE,
                SEARCH,
                PathSearchCalculation.CANDIDATES,
                BasicCurrencyTransitions.INTERPRETATION),
            new Scope(
                "FINITE_POLICY_CANDIDATES", job.blocked == null ? 6 : 0, 6, job.blocked == null),
            recommendations,
            job.blocked == null
                ? PathSearchCalculation.rankings(recommendations, c.observations)
                : List.of(),
            saved.pages.getFirst(),
            job.recovery,
            job.blocked == null
                && !c.complete()
                && Set.of("PAUSED", "CANCELLED").contains(job.status),
            job.blocked == null ? job.reason : job.blocked);
  }

  private void validate(Create r) {
    if (r == null
        || !Integer.valueOf(1).equals(r.version())
        || r.clientRequestId() == null
        || r.clientRequestId().isBlank()
        || r.clientRequestId().length() > 256
        || r.start() == null
        || r.goal() == null
        || r.activeOmens() == null
        || !r.activeOmens().isEmpty()) throw new Rejected(422, "INVALID_REQUEST");
    observations(r.observations());
    if (!provenance().equals(r.start().provenance()))
      throw new Rejected(422, "RULESET_IDENTITY_MISMATCH");
    if (r.start().item().baseItemId().equals(catalog.base().id())
        && new ItemStateValidator(catalog)
            .validate(r.start().item()).stream()
                .anyMatch(v -> v.code() != ItemStateValidator.Code.UNSUPPORTED_STATE))
      throw new Rejected(422, "INVALID_ITEM");
    goals.evaluate(r.start().item(), r.goal());
  }

  private static List<String> observations(List<String> values) {
    if (values == null
        || values.isEmpty()
        || values.size() > 32
        || new HashSet<>(values).size() != values.size())
      throw new Rejected(422, "INVALID_OBSERVATIONS");
    try {
      for (String value : values)
        if (value == null || !value.matches("0|[1-9][0-9]*")) throw new NumberFormatException();
      return values.stream().map(Long::parseLong).sorted().map(Object::toString).toList();
    } catch (NumberFormatException e) {
      throw new Rejected(422, "INVALID_OBSERVATIONS");
    }
  }

  private String blocked(Create request) {
    var item = request.start().item();
    if (!SolarAmulet.BASE_ID.equals(item.baseItemId())
        || item.rarity() != ItemState.Rarity.RARE
        || item.explicits().size() != 1
        || item.explicits().getFirst().fractured()
        || item.catalystQuality() != null
        || !item.conditions().isEmpty()) return "START_STATE_NOT_SUPPORTED";
    if (request.goal().groups().stream()
        .anyMatch(
            g ->
                !g.disabled()
                    && g.type() != GoalFilter.Type.AND
                    && g.type() != GoalFilter.Type.COUNT)) return "GOAL_GROUP_NOT_SUPPORTED";
    var evaluated = goals.evaluate(item, request.goal()).status();
    return evaluated == GoalFilter.Status.UNKNOWN || evaluated == GoalFilter.Status.UNSUPPORTED
        ? "GOAL_EVALUATION_NOT_SUPPORTED"
        : null;
  }

  private String fingerprint(Create r) {
    return digest(
        encode(new Create(r.version(), "", r.start(), r.goal(), r.activeOmens(), r.observations()))
            + PREDICATE
            + SEARCH
            + PathSearchCalculation.CANDIDATES);
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (java.io.IOException e) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
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
      synchronized (job) {
        job.generation++;
        jobs.remove(job.id);
        requestIds.remove(job.owner + ":" + job.request.clientRequestId());
      }
    }
  }

  @Override
  public void close() {
    if (executor instanceof ExecutorService service) service.shutdownNow();
  }

  /**
   * {@code PathSearchService} 내부의 작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code fingerprint}: 지문 ({@code String}).
   *   <li>{@code blocked}: 차단 ({@code String}).
   *   <li>{@code evaluation}: 평가 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code Create}).
   *   <li>{@code recovery}: 복구 ({@code Recovery}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code Instant}).
   *   <li>{@code calculation}: 계산 ({@code PathSearchCalculation}).
   *   <li>{@code revisions}: 리비전목록 ({@code LinkedHashMap<Long, Revision>}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Command>}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code snapshot}: 스냅샷 ({@code Snapshot}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Job(owner, request, fingerprint, predicate, recovery)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code owner}(소유자), {@code request}(요청), {@code fingerprint}(지문), {@code
   *       predicate}(판정조건), {@code recovery}(복구).
   * </ul>
   */
  private final class Job {
    final String id = UUID.randomUUID().toString(), owner, fingerprint, blocked, evaluation;
    final Create request;
    final Recovery recovery;
    final Instant expires = clock.instant().plus(settings.ttl());
    final PathSearchCalculation calculation;
    final LinkedHashMap<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new HashMap<>();
    long generation, revision = -1;
    String status, reason;
    Snapshot snapshot;

    Job(
        String owner,
        Create request,
        String fingerprint,
        Function<ItemState, GoalFilter.Status> predicate,
        Recovery recovery) {
      this.owner = owner;
      this.request = request;
      this.fingerprint = fingerprint;
      this.recovery = recovery;
      blocked = blocked(request);
      var rootStatus = predicate.apply(request.start().item());
      evaluation =
          rootStatus == GoalFilter.Status.UNKNOWN
              ? "UNKNOWN"
              : rootStatus == GoalFilter.Status.UNSUPPORTED ? "UNSUPPORTED" : "SUPPORTED";
      status = blocked == null ? "QUEUED" : "UNSUPPORTED";
      calculation =
          new PathSearchCalculation(
              request.start(), predicate, observations(request.observations()), transitions);
    }
  }

  /**
   * {@code PathSearchService}에서 사용하는 명령의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code Mutation}).
   *   <li>{@code response}: 응답 ({@code Snapshot}).
   * </ul>
   */
  private record Command(Mutation request, Snapshot response) {}

  /**
   * {@code PathSearchService} 내부의 리비전 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code nodes}: 노드목록 ({@code Map<String, Node>}).
   *   <li>{@code executions}: 실행목록 ({@code Map<String, Execution>}).
   *   <li>{@code edges}: 간선목록 ({@code List<Edge>}).
   *   <li>{@code pages}: 페이지목록 ({@code List<GraphPage>}).
   *   <li>{@code cursors}: cursors ({@code Map<String, Integer>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Revision(job, revision, c, pageEdges)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code job}(작업),
   *       {@code revision}(리비전), {@code c}(c), {@code pageEdges}(페이지·간선목록).
   * </ul>
   */
  private static final class Revision {
    final Map<String, Node> nodes;
    final Map<String, Execution> executions;
    final List<Edge> edges;
    final List<GraphPage> pages = new ArrayList<>();
    final Map<String, Integer> cursors = new HashMap<>();

    Revision(String job, long revision, PathSearchCalculation c, int pageEdges) {
      nodes = Map.copyOf(c.nodes);
      executions = Map.copyOf(c.executions);
      edges = List.copyOf(c.edges.values());
      int count = Math.max(1, (edges.size() + pageEdges - 1) / pageEdges);
      var tokens = new ArrayList<String>();
      for (int i = 0; i < count; i++) {
        String token = UUID.randomUUID().toString();
        tokens.add(token);
        cursors.put(token, i);
      }
      for (int i = 0; i < count; i++)
        pages.add(
            new GraphPage(
                1,
                job,
                revision,
                i == 0 ? List.copyOf(c.nodes.values()) : List.of(),
                i == 0 ? List.copyOf(c.executions.values()) : List.of(),
                edges.subList(i * pageEdges, Math.min(edges.size(), (i + 1) * pageEdges)),
                i == count - 1 ? List.copyOf(c.expansions.values()) : List.of(),
                i + 1 < count ? tokens.get(i + 1) : null));
    }
  }
}
