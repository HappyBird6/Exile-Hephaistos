package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.CertifiedMethodProtocol.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedLawStore;
import com.poe2craft.craftingsupport.search.domain.CertifiedMutableLawStore;
import com.poe2craft.craftingsupport.search.domain.CertifiedOutcomeStrategy;
import com.poe2craft.craftingsupport.search.domain.ConditionalMethodLaw;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.probability.Fraction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * 하나의 작업자로 구간 확률 탐색 작업과 체크포인트 복구를 관리한다.
 *
 * <p>기존 계약: Certified jobs share one worker; optional local checkpoints survive process restarts.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CHECKPOINT_VERSION}: 버전; 초기값 {@code "certified-job-checkpoint-v2-control"} ({@code
 *       String}).
 *   <li>{@code repository}: 조회·저장을 위임하는 저장소 ({@code CertifiedJobRepository}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code unavailable}: 사용불가 ({@code Map<String, DiskInput>}).
 *   <li>{@code jobs}: 작업목록 ({@code Map<String, Job>}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code executor}: 작업실행기 ({@code Executor}).
 *   <li>{@code runBudget}: 실행·계산예산 ({@code int}).
 *   <li>{@code factory}: 생성기 ({@code BiFunction<MethodSearchProtocol.Create,
 *       CertifiedInterval.Precision, CertifiedMethodSearchEngine>}).
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings.CertifiedSearch}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ownerKey(owner)}: 소유자·키 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code owner}(소유자). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code persistence(repository, json)}: persistence 처리에서 {@code jobs.isEmpty}, {@code
 *       json.copy}을(를) 사용한다. 입력: {@code repository}(저장소), {@code json}(JSON). 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code encode(value)}: 값을 저장·전송 표현으로 부호화한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code decode(value, type)}: 저장·전송 표현을 내부 값으로 복원한다. 입력: {@code value}(값), {@code type}(타입).
 *       반환: {@code T}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code persist(job)}: persist 처리에서 {@code engine.operations}, {@code
 *       engine.graphRevision}을(를) 사용한다. 입력: {@code job}(작업).
 *   <li>{@code identity(engine, input)}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 입력: {@code engine}(계산엔진),
 *       {@code input}(입력). 반환: {@code MethodGraphStore.Identity}.
 *   <li>{@code identity(engine, input, originalInput)}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 입력: {@code
 *       engine}(계산엔진), {@code input}(입력), {@code originalInput}(원본·입력). 반환: {@code
 *       MethodGraphStore.Identity}.
 *   <li>{@code CertifiedSearchJobs(clock, executor, runBudget, factory)}: 기본 정책을 보충해 공통 생성자로 위임한다.
 *       입력: {@code clock}(현재 시각 공급자), {@code executor}(작업실행기), {@code runBudget}(실행·계산예산), {@code
 *       factory}(생성기).
 *   <li>{@code CertifiedSearchJobs(clock, executor, runBudget, factory, settings)}: 전달받은 값으로 구성원을
 *       초기화한다. 입력: {@code clock}(현재 시각 공급자), {@code executor}(작업실행기), {@code runBudget}(실행·계산예산),
 *       {@code factory}(생성기), {@code settings}(운영설정).
 *   <li>{@code create(owner, request)}: 새 작업 또는 값을 생성한다. 입력: {@code owner}(소유자), {@code
 *       request}(요청). 반환: {@code Started}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code get(owner, id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code owner}(소유자), {@code id}(식별자). 반환:
 *       {@code Snapshot}.
 *   <li>{@code hasRequest(owner, requestId)}: 요청을(를) 보유 여부를 판정한다. 입력: {@code owner}(소유자), {@code
 *       requestId}(요청·식별자). 반환: {@code boolean}.
 *   <li>{@code job(owner, id)}: 작업 처리에서 {@code unavailable.get}, {@code rejected.owner}을(를) 사용한다.
 *       입력: {@code owner}(소유자), {@code id}(식별자). 반환: {@code Job}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code mutate(owner, id, command)}: mutate 처리에서 {@code Integer.valueOf}, {@code
 *       command.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code command}(명령).
 *       반환: {@code Snapshot}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code graph(owner, id, revision, stateOffset, methodOffset)}: 그래프 처리에서 {@code
 *       job.acquireRequest}, {@code states.size}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자),
 *       {@code revision}(리비전), {@code stateOffset}(상태·시작위치), {@code methodOffset}(제작방법·시작위치). 반환:
 *       {@code Graph}.
 *   <li>{@code outcomes(owner, id, revision, methodId, cycles, offset)}: 가능한 결과와 확률을 조회한다. 입력:
 *       {@code owner}(소유자), {@code id}(식별자), {@code revision}(리비전), {@code methodId}(제작방법·식별자),
 *       {@code cycles}(주기횟수), {@code offset}(시작위치). 반환: {@code Outcomes}.
 *   <li>{@code map(owner, id, revision, offset)}: 맵 처리에서 {@code job.acquireRequest}, {@code
 *       CertifiedMapProtocol.graph}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code
 *       revision}(리비전), {@code offset}(시작위치). 반환: {@code CertifiedMapProtocol.Graph}.
 *   <li>{@code view(owner, id)}: 내부 계산 상태를 읽기 전용 조회 값으로 구성한다. 입력: {@code owner}(소유자), {@code
 *       id}(식별자). 반환: {@code CertifiedMapProtocol.View}.
 *   <li>{@code mapOutcomes(owner, id, revision, methodId, cycles, offset)}: 맵·결과목록 처리에서 {@code
 *       job.acquireRequest}, {@code CertifiedMapProtocol.distribution}을(를) 사용한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자), {@code revision}(리비전), {@code methodId}(제작방법·식별자), {@code
 *       cycles}(주기횟수), {@code offset}(시작위치). 반환: {@code CertifiedMapProtocol.Distribution}.
 *   <li>{@code outcomes(job, revision, methodId, cycles, offset)}: 가능한 결과와 확률을 조회한다. 입력: {@code
 *       job}(작업), {@code revision}(리비전), {@code methodId}(제작방법·식별자), {@code cycles}(주기횟수), {@code
 *       offset}(시작위치). 반환: {@code Outcomes}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code appendOutcomes(result, kind, law, offset, decimals)}: append·결과목록 처리에서 {@code
 *       result.size}, {@code law.size}을(를) 사용한다. 입력: {@code result}(결과), {@code kind}(종류), {@code
 *       law}(분포), {@code offset}(시작위치), {@code decimals}(소수자릿수). 반환: {@code int}.
 *   <li>{@code bind(owner, id, request)}: 연결 처리에서 {@code Integer.valueOf}, {@code
 *       request.version}을(를) 사용한다. 입력: {@code owner}(소유자), {@code id}(식별자), {@code request}(요청).
 *       반환: {@code Entry}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code entry(owner, id, entryId, offset)}: 항목 처리에서 {@code job.acquireRequest}을(를) 사용한다. 입력:
 *       {@code owner}(소유자), {@code id}(식별자), {@code entryId}(항목·식별자), {@code offset}(시작위치). 반환:
 *       {@code Entry}.
 *   <li>{@code advance(owner, id, entryId, request)}: 허용된 계산량만큼 작업을 진행한다. 입력: {@code owner}(소유자),
 *       {@code id}(식별자), {@code entryId}(항목·식별자), {@code request}(요청). 반환: {@code Entry}. 사용하는 예외:
 *       {@code Rejected}.
 *   <li>{@code entryRetained(job)}: 항목·보유 처리에서 {@code entries.values}, {@code
 *       expansion.retainedStates}을(를) 사용한다. 입력: {@code job}(작업). 반환: {@code long}.
 *   <li>{@code entryView(job, work, offset)}: 항목·조회표현 처리에서 {@code expansion.complete}, {@code
 *       expansion.result}을(를) 사용한다. 입력: {@code job}(작업), {@code work}(계산작업), {@code offset}(시작위치).
 *       반환: {@code Entry}.
 *   <li>{@code entryWork(job, id)}: 항목·계산작업 처리에서 {@code entries.get}을(를) 사용한다. 입력: {@code job}(작업),
 *       {@code id}(식별자). 반환: {@code EntryWork}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code checkOffset(offset, size)}: 시작위치을(를) 검사한다. 입력: {@code offset}(시작위치), {@code
 *       size}(크기). 사용하는 예외: {@code Rejected}.
 *   <li>{@code method(revision, id)}: 제작방법 처리에서 {@code methods.stream}, {@code m.id}을(를) 사용한다. 입력:
 *       {@code revision}(리비전), {@code id}(식별자). 반환: {@code CertifiedMethodSearchEngine.MethodView}.
 *   <li>{@code revision(job, revision)}: 리비전 처리에서 {@code revisions.get}을(를) 사용한다. 입력: {@code
 *       job}(작업), {@code revision}(리비전). 반환: {@code Revision}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code submit(job)}: submit 처리에서 {@code executor.execute}을(를) 사용한다. 입력: {@code job}(작업).
 *   <li>{@code run(job, generation)}: 작업의 실행 루프를 수행한다. 입력: {@code job}(작업), {@code
 *       generation}(세대번호).
 *   <li>{@code publish(job)}: 새 결과나 체크포인트를 외부 조회에 공개한다. 입력: {@code job}(작업).
 *   <li>{@code dispose(job)}: dispose 처리에서 {@code job.acquire}, {@code revisions.clear}을(를) 사용한다.
 *       입력: {@code job}(작업).
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code DiskInput}: {@code CertifiedSearchJobs}에서 사용하는 디스크·입력의 불변 값이다.
 *   <li>{@code DiskEntry}: {@code CertifiedSearchJobs}에서 사용하는 디스크·항목의 불변 값이다.
 *   <li>{@code DiskJob}: {@code CertifiedSearchJobs}에서 사용하는 디스크·작업의 불변 값이다.
 *   <li>{@code ControlCheckpoint}: {@code CertifiedSearchJobs}에서 사용하는 작업제어·체크포인트의 불변 값이다.
 *   <li>{@code Revision}: {@code CertifiedSearchJobs}에서 사용하는 리비전의 불변 값이다.
 *   <li>{@code Command}: {@code CertifiedSearchJobs}에서 사용하는 명령의 불변 값이다.
 *   <li>{@code Guard}: {@code CertifiedSearchJobs} 내부의 Guard 처리를 맡는다.
 *   <li>{@code EntryWork}: {@code CertifiedSearchJobs} 내부의 항목·계산작업 처리를 맡는다.
 *   <li>{@code Job}: {@code CertifiedSearchJobs} 내부의 작업 처리를 맡는다.
 * </ul>
 */
final class CertifiedSearchJobs {
  static final String CHECKPOINT_VERSION = "certified-job-checkpoint-v2-control";
  private CertifiedJobRepository repository;
  private ObjectMapper json;
  private final Map<String, DiskInput> unavailable = new LinkedHashMap<>();

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 디스크·입력의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code Create}).
   * </ul>
   */
  private record DiskInput(String owner, Create request) {}

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 디스크·항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code parent}: 상위값 ({@code String}).
   *   <li>{@code binding}: 결합 ({@code IndexedMethodProtocol.Bind}).
   *   <li>{@code expansion}: 확장 ({@code ConditionalMethodLaw.ExpansionCheckpoint}).
   * </ul>
   */
  private record DiskEntry(
      String id,
      String parent,
      IndexedMethodProtocol.Bind binding,
      ConditionalMethodLaw.ExpansionCheckpoint expansion) {}

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 디스크·작업의 불변 값이다.
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
   *   <li>{@code limit}: 한도 ({@code int}).
   *   <li>{@code entryLimit}: 항목·한도 ({@code int}).
   *   <li>{@code strategySteps}: 전략·단계목록 ({@code long}).
   *   <li>{@code strategyBlocked}: 전략·차단 ({@code String}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Command>}).
   *   <li>{@code entries}: 항목목록 ({@code List<DiskEntry>}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code long}).
   * </ul>
   */
  private record DiskJob(
      String version,
      String id,
      long revision,
      long generation,
      String status,
      String reason,
      int limit,
      int entryLimit,
      long strategySteps,
      String strategyBlocked,
      Map<String, Command> commands,
      List<DiskEntry> entries,
      long expires) {}

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 작업제어·체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code execution}: 실행 ({@code String}).
   *   <li>{@code job}: 작업 ({@code DiskJob}).
   * </ul>
   */
  private record ControlCheckpoint(String execution, DiskJob job) {}

  private static String ownerKey(String owner) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(owner.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  void persistence(CertifiedJobRepository repository, ObjectMapper json) {
    synchronized (jobs) {
      if (this.repository != null || !jobs.isEmpty())
        throw new IllegalStateException("Persistence must be configured before requests");
      this.repository = repository;
      this.json =
          json.copy()
              .enable(
                  com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      for (var descriptor : repository.list()) {
        MethodGraphStore opened = null;
        CertifiedMethodSearchEngine restoring = null;
        try {
          var input = decode(descriptor.identity().originalInput(), DiskInput.class);
          var store = opened = repository.open(descriptor);
          long recoveryRevision = store.revision();
          var saved = store.execution("job");
          if (saved.isEmpty()) {
            store.close();
            continue;
          }
          var control =
              store
                  .checkpoint("job/control")
                  .map(c -> decode(c.content(), ControlCheckpoint.class));
          if (control.isPresent() && !control.get().execution().equals(saved.get().id()))
            throw new IllegalStateException("Job control references another execution");
          var header =
              control
                  .map(ControlCheckpoint::job)
                  .orElseGet(() -> decode(saved.get().header(), DiskJob.class));
          long lease =
              store
                  .checkpoint("job/lease")
                  .map(c -> Long.parseLong(c.content()))
                  .orElse(header.expires());
          if (clock.instant().toEpochMilli() > Math.max(header.expires(), lease)) {
            store.close();
            continue;
          }
          if (!CHECKPOINT_VERSION.equals(header.version())
              || !descriptor.id().equals(header.id())) {
            throw new IllegalStateException("Incompatible job checkpoint");
          }
          var engine =
              restoring = factory.apply(input.request().request(), input.request().precision());
          if (!descriptor
              .identity()
              .equals(identity(engine, input, descriptor.identity().originalInput()))) {
            throw new IllegalStateException(
                "Job calculation versions changed; explicit recalculation required");
          }
          var job = new Job(header.id(), input.owner(), input.request(), engine);
          job.store = store;
          job.storeRevision = recoveryRevision;
          try (var restore = engine.checkpointRestore(this.json)) {
            long offset = 0;
            while (true) {
              var page = store.executionRows(saved.get().id(), offset, 128);
              for (var row : page.rows())
                restore.accept(
                    decode(row.content(), CertifiedMethodSearchEngine.CheckpointLine.class));
              offset += page.rows().size();
              if (page.complete()) break;
            }
            restore.finish();
            job.revision = header.revision();
            job.generation = header.generation() + 1;
            job.limit = header.limit();
            engine.increaseMemoryBudget(job.limit);
            job.checkpointOperations = engine.operations();
            job.checkpointGraphRevision = engine.graphRevision();
            job.checkpointExecution = saved.get().id();
            job.entryLimit = header.entryLimit();
            job.commands.putAll(header.commands());
            job.strategyBlocked = header.strategyBlocked();
            if (header.strategySteps() > 0) {
              job.strategy = engine.strategyWork();
              for (long i = 0; i < header.strategySteps(); i++) job.strategy.step();
              job.strategySteps = header.strategySteps();
            }
            for (var savedEntry : header.entries()) {
              if (savedEntry.id().equals("root")) {
                job.entries.put(
                    "root", new EntryWork("root", null, engine.rootLaw(), savedEntry.expansion()));
                continue;
              }
              var binding = savedEntry.binding();
              var parent = entryWork(job, savedEntry.parent());
              var kind =
                  binding.kind().equals("ACTIVE")
                      ? Optional.<ExactMethodRunner.Exit>empty()
                      : Optional.of(ExactMethodRunner.Exit.valueOf(binding.kind()));
              var law =
                  engine
                      .observation(binding.methodId(), binding.cycles())
                      .condition(parent.law, kind, engine.classId(binding.targetStateId()));
              job.entries.put(
                  savedEntry.id(),
                  new EntryWork(savedEntry.id(), savedEntry.parent(), law, savedEntry.expansion()));
              job.bindings.put(binding.requestId(), binding);
              job.entryIds.put(binding.requestId(), savedEntry.id());
            }
            job.status =
                Set.of("CANCELLED", "COMPLETED", "FAILED").contains(header.status())
                    ? header.status()
                    : "PAUSED";
            job.reason = job.status.equals("PAUSED") ? "PROCESS_RESTART" : header.reason();
            jobs.put(job.id, job);
            publish(job);
          }
        } catch (RuntimeException | Error failure) {
          jobs.remove(descriptor.id());
          if (restoring != null) restoring.close();
          if (opened != null) opened.close();
          if (failure instanceof Error error) throw error;
          try {
            unavailable.put(
                descriptor.id(), decode(descriptor.identity().originalInput(), DiskInput.class));
          } catch (RuntimeException ignored) {
            /* Retain the original file for explicit recovery. */
          }
          java.util.logging.Logger.getLogger(CertifiedSearchJobs.class.getName())
              .warning(
                  "Job checkpoint could not be restored; original file retained: "
                      + descriptor.id());
        }
      }
    }
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid job checkpoint", e);
    }
  }

  private <T> T decode(String value, Class<T> type) {
    try {
      return json.readValue(value, type);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid job checkpoint", e);
    }
  }

  private void persist(Job job) {
    if (job.store == null) return;
    boolean changed =
        job.checkpointOperations != job.engine.operations()
            || job.checkpointGraphRevision != job.engine.graphRevision();
    if (changed) job.engine.settleCheckpoint();
    var entries = new ArrayList<DiskEntry>();
    for (var entry : job.entries.values()) {
      IndexedMethodProtocol.Bind binding = null;
      for (var link : job.entryIds.entrySet())
        if (link.getValue().equals(entry.id)) binding = job.bindings.get(link.getKey());
      entries.add(new DiskEntry(entry.id, entry.parent, binding, entry.expansion.checkpoint()));
    }
    var header =
        new DiskJob(
            CHECKPOINT_VERSION,
            job.id,
            job.revision,
            job.generation,
            job.status,
            job.reason,
            job.limit,
            job.entryLimit,
            job.strategySteps,
            job.strategyBlocked,
            Map.copyOf(job.commands),
            entries,
            job.expires.toEpochMilli());
    if (!changed) {
      job.storeRevision =
          job.store.commit(
              job.storeRevision,
              new MethodGraphStore.Batch(
                  List.of(),
                  List.of(),
                  List.of(),
                  List.of(
                      new MethodGraphStore.Checkpoint(
                          "job/control",
                          encode(new ControlCheckpoint(job.checkpointExecution, header))))));
      job.durableLeaseUntil = job.expires.toEpochMilli();
      return;
    }
    var previous = job.store.execution("job");
    long revision = job.storeRevision;
    var sealed =
        job.store.captureExecution(
            "job",
            encode(header),
            sink -> job.engine.checkpoint(json, line -> sink.accept(encode(line))));
    try {
      job.storeRevision =
          job.store.commit(
              revision,
              new MethodGraphStore.Batch(
                  List.of(),
                  List.of(),
                  List.of(),
                  List.of(
                      MethodCursorStore.reference(sealed),
                      new MethodGraphStore.Checkpoint(
                          "job/control", encode(new ControlCheckpoint(sealed.id(), header))))));
      job.durableLeaseUntil = job.expires.toEpochMilli();
      job.checkpointOperations = job.engine.operations();
      job.checkpointGraphRevision = job.engine.graphRevision();
      job.checkpointExecution = sealed.id();
      previous.ifPresent(
          old -> {
            try {
              job.store.discardExecution(old.id());
            } catch (RuntimeException cleanup) {
              java.util.logging.Logger.getLogger(CertifiedSearchJobs.class.getName())
                  .warning("Unreferenced job checkpoint retained for later cleanup: " + old.id());
            }
          });
    } catch (RuntimeException | Error failure) {
      try {
        job.store.discardExecution(sealed.id());
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  private MethodGraphStore.Identity identity(CertifiedMethodSearchEngine engine, DiskInput input) {
    return identity(engine, input, encode(input));
  }

  private MethodGraphStore.Identity identity(
      CertifiedMethodSearchEngine engine, DiskInput input, String originalInput) {
    return new MethodGraphStore.Identity(
        CHECKPOINT_VERSION,
        engine.calculationVersion(),
        ConditionalMethodLaw.VERSION,
        engine.templatesVersion(),
        input.request().request().goal().catalogVersion(),
        CertifiedInterval.VERSION,
        "request-provenance",
        originalInput);
  }

  private final Map<String, Job> jobs = new LinkedHashMap<>();
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final BiFunction<
          MethodSearchProtocol.Create, CertifiedInterval.Precision, CertifiedMethodSearchEngine>
      factory;

  private final BackendSettings.CertifiedSearch settings;

  CertifiedSearchJobs(
      Clock clock,
      Executor executor,
      int runBudget,
      BiFunction<
              MethodSearchProtocol.Create, CertifiedInterval.Precision, CertifiedMethodSearchEngine>
          factory) {
    this(clock, executor, runBudget, factory, BackendSettings.defaults().certifiedSearch());
  }

  CertifiedSearchJobs(
      Clock clock,
      Executor executor,
      int runBudget,
      BiFunction<
              MethodSearchProtocol.Create, CertifiedInterval.Precision, CertifiedMethodSearchEngine>
          factory,
      BackendSettings.CertifiedSearch settings) {
    this.settings = java.util.Objects.requireNonNull(settings);
    this.clock = clock;
    this.executor = executor;
    this.runBudget = runBudget;
    this.factory = factory;
  }

  Started create(String owner, Create request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.request() == null
        || request.precision() == null) throw new Rejected(422, "INVALID_REQUEST");
    synchronized (jobs) {
      if (unavailable.values().stream()
          .anyMatch(
              input ->
                  input.owner().equals(ownerKey(owner))
                      && input
                          .request()
                          .request()
                          .clientRequestId()
                          .equals(request.request().clientRequestId())))
        throw new Rejected(409, "CHECKPOINT_RECALCULATION_REQUIRED");
      jobs.values()
          .removeIf(
              j -> {
                if (!clock.instant().isAfter(j.expires)) return false;
                dispose(j);
                return true;
              });
      for (var existing : jobs.values()) {
        if (existing.owner.equals(ownerKey(owner))
            && Objects.equals(
                existing.request.request().clientRequestId(),
                request.request().clientRequestId())) {
          if (!existing.request.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
          try (var ignored = existing.acquireRequest()) {
            return new Started(
                existing.snapshot, entryView(existing, existing.entries.get("root"), 0));
          }
        }
      }
      if (jobs.size() >= settings.maxJobs()) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var job = new Job(owner, request, factory.apply(request.request(), request.precision()));
      if (repository != null) {
        job.store =
            repository.create(
                new CertifiedJobRepository.Descriptor(
                    job.id, identity(job.engine, new DiskInput(job.owner, request))));
      }
      jobs.put(job.id, job);
      try (var ignored = job.acquireRequest()) {
        publish(job);
        submit(job);
        return new Started(job.snapshot, entryView(job, job.entries.get("root"), 0));
      }
    }
  }

  Snapshot get(String owner, String id) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      return job.snapshot;
    }
  }

  boolean hasRequest(String owner, String requestId) {
    synchronized (jobs) {
      return jobs.values().stream()
          .anyMatch(
              j ->
                  !clock.instant().isAfter(j.expires)
                      && j.owner.equals(ownerKey(owner))
                      && j.request.request().clientRequestId().equals(requestId));
    }
  }

  private Job job(String owner, String id) {
    synchronized (jobs) {
      var rejected = unavailable.get(id);
      if (rejected != null && rejected.owner().equals(ownerKey(owner)))
        throw new Rejected(409, "CHECKPOINT_RECALCULATION_REQUIRED");
      var job = jobs.get(id);
      if (job == null || !job.owner.equals(ownerKey(owner)))
        throw new Rejected(404, "SEARCH_NOT_FOUND");
      if (clock.instant().isAfter(job.expires)) throw new Rejected(410, "SEARCH_EXPIRED");
      return job;
    }
  }

  Snapshot mutate(String owner, String id, MethodSearchProtocol.Mutation command) {
    if (command == null
        || !Integer.valueOf(2).equals(command.version())
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.expectedRevision() == null
        || !Set.of("CANCEL", "RESUME").contains(Objects.toString(command.operation(), "")))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var old = job.commands.get(command.commandId());
      if (old != null) {
        if (!old.command().equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.snapshot();
      }
      if (command.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (command.operation().equals("RESUME")) {
        if (!Set.of("PAUSED", "CANCELLED").contains(job.status))
          throw new Rejected(409, "SEARCH_NOT_PAUSED");
        if (command.retainedOutcomeLimit() != null) {
          int limit = command.retainedOutcomeLimit();
          if (limit < job.limit || limit > 1000000)
            throw new Rejected(422, "INVALID_MEMORY_BUDGET");
          job.engine.increaseMemoryBudget(limit);
          job.limit = limit;
        }
        job.generation++;
        job.status = "QUEUED";
        job.reason = null;
        publish(job);
        submit(job);
      } else {
        job.generation++;
        job.status = "CANCELLED";
        job.reason = null;
        publish(job);
      }
      job.commands.put(command.commandId(), new Command(command, job.snapshot));
      while (job.commands.size() > 64) job.commands.remove(job.commands.keySet().iterator().next());
      persist(job);
      return job.snapshot;
    }
  }

  Graph graph(String owner, String id, long revision, int stateOffset, int methodOffset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var saved = revision(job, revision);
      checkOffset(stateOffset, saved.states.size());
      checkOffset(methodOffset, saved.methods.size());
      int stateEnd = Math.min(stateOffset + 48, saved.states.size());
      int methodEnd = Math.min(methodOffset + 12, saved.methods.size());
      var states =
          saved.states.subList(stateOffset, stateEnd).stream()
              .map(
                  s ->
                      new State(
                          s.id(),
                          s.depth(),
                          s.progress().goal().name(),
                          s.progress().entries().stream().map(Enum::name).toList(),
                          s.complete(),
                          s.representative().representative().item()))
              .toList();
      var methods =
          saved.methods.subList(methodOffset, methodEnd).stream()
              .map(m -> CertifiedMethodProtocol.method(m, job.engine.precision()))
              .toList();
      return new Graph(
          id,
          revision,
          states,
          methods,
          CertifiedMethodProtocol.locals(
              saved.states.subList(stateOffset, stateEnd), saved.methods, job.engine.precision()),
          stateEnd < saved.states.size() ? stateEnd : null,
          methodEnd < saved.methods.size() ? methodEnd : null);
    }
  }

  Outcomes outcomes(
      String owner, String id, long revision, String methodId, int cycles, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      return outcomes(job, revision, methodId, cycles, offset);
    }
  }

  CertifiedMapProtocol.Graph map(String owner, String id, long revision, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var saved = revision(job, revision);
      return CertifiedMapProtocol.graph(
          id, revision, saved.states(), saved.methods(), offset, job.engine.precision());
    }
  }

  CertifiedMapProtocol.View view(String owner, String id) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var current = job.snapshot;
      var saved = revision(job, current.revision());
      return new CertifiedMapProtocol.View(
          new CertifiedMapProtocol.Header(
              CertifiedMapProtocol.VERSION,
              id,
              job.request.request().clientRequestId(),
              current.revision(),
              current.generation(),
              current.status(),
              current.reason(),
              Set.of("PAUSED", "CANCELLED").contains(current.status())
                  && job.strategyPause() == null,
              current.enumerationComplete(),
              current.rootStateId(),
              current.strategyStatus(),
              current.localRecommendations(),
              current.strategies().stream()
                  .map(
                      s ->
                          new MethodSearchProtocol.Strategy(
                              s.stateId(),
                              s.cycles(),
                              s.lower().equals(s.upper()) ? "EXACT" : "BOUNDED",
                              s.lower(),
                              s.upper(),
                              s.methodIds()))
                  .toList()),
          CertifiedMapProtocol.graph(
              id, current.revision(), saved.states(), saved.methods(), 0, job.engine.precision()));
    }
  }

  CertifiedMapProtocol.Distribution mapOutcomes(
      String owner, String id, long revision, String methodId, int cycles, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      return CertifiedMapProtocol.distribution(
          outcomes(job, revision, methodId, cycles, offset), revision(job, revision).states());
    }
  }

  private Outcomes outcomes(Job job, long revision, String methodId, int cycles, int offset) {
    var method = method(revision(job, revision), methodId);
    var point = method.points().get(cycles);
    if (point == null) throw new Rejected(409, "OBSERVATION_NOT_READY");
    int size = point.active().size() + point.exits().values().stream().mapToInt(Map::size).sum();
    checkOffset(offset, size);
    var result = new ArrayList<Outcome>();
    int remainingOffset = offset;
    for (var kind : ExactMethodRunner.Exit.values())
      remainingOffset =
          appendOutcomes(
              result,
              kind.name(),
              point.exits().get(kind),
              remainingOffset,
              job.engine.precision().percentDecimals());
    appendOutcomes(
        result,
        "ACTIVE",
        point.active(),
        remainingOffset,
        job.engine.precision().percentDecimals());
    return new Outcomes(
        job.id,
        revision,
        methodId,
        cycles,
        List.copyOf(result),
        offset + result.size() < size ? offset + result.size() : null);
  }

  private static int appendOutcomes(
      List<Outcome> result,
      String kind,
      Map<String, CertifiedInterval> law,
      int offset,
      int decimals) {
    if (result.size() == 48) return 0;
    if (offset >= law.size()) return offset - law.size();
    for (var entry : CertifiedLawStore.page(law, offset, 48 - result.size()))
      result.add(new Outcome(kind, entry.getKey(), Bounds.probability(entry.getValue(), decimals)));
    return 0;
  }

  Entry bind(String owner, String id, IndexedMethodProtocol.Bind request) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.revision() == null
        || request.requestId() == null
        || request.requestId().isBlank()
        || request.requestId().length() > 256
        || request.cycles() == null
        || !MethodSearchCalculation.OBSERVATIONS.contains(request.cycles())
        || request.kind() == null
        || request.kind().equals("UNRESOLVED")) throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var previous = job.bindings.get(request.requestId());
      if (previous != null) {
        if (!previous.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
        return entryView(job, job.entries.get(job.entryIds.get(request.requestId())), 0);
      }
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "SEARCH_CANCELLED");
      if (job.entries.size() >= 65) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var method = method(revision(job, request.revision()), request.methodId());
      if (!method.points().containsKey(request.cycles()))
        throw new Rejected(409, "OBSERVATION_NOT_READY");
      var parent = entryWork(job, request.parentEntryId());
      if (!method.source().equals("class:" + parent.law.classId()))
        throw new Rejected(422, "ENTRY_SOURCE_MISMATCH");
      try {
        var kind =
            request.kind().equals("ACTIVE")
                ? Optional.<ExactMethodRunner.Exit>empty()
                : Optional.of(ExactMethodRunner.Exit.valueOf(request.kind()));
        var law =
            job.engine
                .observation(request.methodId(), request.cycles())
                .condition(parent.law, kind, job.engine.classId(request.targetStateId()));
        var entry = new EntryWork(UUID.randomUUID().toString(), parent.id, law);
        job.entries.put(entry.id, entry);
        job.bindings.put(request.requestId(), request);
        job.entryIds.put(request.requestId(), entry.id);
        persist(job);
        return entryView(job, entry, 0);
      } catch (IllegalArgumentException ex) {
        throw new Rejected(422, "INVALID_CONDITIONAL_EXIT");
      }
    }
  }

  Entry entry(String owner, String id, String entryId, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      return entryView(job, entryWork(job, entryId), offset);
    }
  }

  Entry advance(String owner, String id, String entryId, IndexedMethodProtocol.Advance request) {
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
    try (var ignored = job.acquireRequest()) {
      if (request.expectedGeneration() != job.generation)
        throw new Rejected(409, "GENERATION_CONFLICT");
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "SEARCH_CANCELLED");
      if (request.retainedStateLimit() < job.entryLimit)
        throw new Rejected(422, "INVALID_MEMORY_BUDGET");
      job.entryLimit = request.retainedStateLimit();
      var work = entryWork(job, entryId);
      long until = System.nanoTime() + 20000000L;
      for (int n = 0;
          n < request.maxSteps() && !work.expansion.complete() && System.nanoTime() < until;
          n++) {
        if (entryRetained(job) > job.entryLimit) break;
        work.expansion.step();
      }
      persist(job);
      return entryView(job, work, 0);
    }
  }

  private static long entryRetained(Job job) {
    return job.entries.values().stream().mapToLong(e -> e.expansion.retainedStates()).sum();
  }

  private Entry entryView(Job job, EntryWork work, int offset) {
    boolean complete = work.expansion.complete();
    var values =
        complete
            ? work.expansion.result().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().canonicalKey()))
                .toList()
            : List.<Map.Entry<BasicCurrencyState, Fraction>>of();
    checkOffset(offset, values.size());
    int end = Math.min(offset + 48, values.size());
    var states =
        values.subList(offset, end).stream()
            .map(
                e ->
                    new MethodSearchProtocol.ConditionalState(
                        e.getKey().canonicalKey(),
                        e.getKey().item(),
                        "ENTRY",
                        Probability.of(e.getValue())))
            .toList();
    var probability =
        complete
            ? CertifiedInterval.enclose(
                work.expansion.probability(), job.engine.precision().fractionalBits())
            : work.law
                .enclosure()
                .orElseGet(
                    () ->
                        CertifiedInterval.enclose(
                            work.law.probability(), job.engine.precision().fractionalBits()));
    boolean cancelled = job.status.equals("CANCELLED");
    return new Entry(
        work.id,
        "class:" + work.law.classId(),
        work.parent,
        cancelled ? "CANCELLED" : complete ? "COMPLETE" : "PARTIAL",
        cancelled
            ? "SEARCH_CANCELLED"
            : !complete && entryRetained(job) > job.entryLimit ? "MEMORY_BUDGET" : null,
        Bounds.probability(probability, job.engine.precision().percentDecimals()),
        job.generation,
        states,
        end < values.size() ? end : null);
  }

  private static EntryWork entryWork(Job job, String id) {
    var entry = job.entries.get(id);
    if (entry == null) throw new Rejected(404, "ENTRY_NOT_FOUND");
    return entry;
  }

  private static void checkOffset(int offset, int size) {
    if (offset < 0 || offset > size) throw new Rejected(422, "INVALID_OFFSET");
  }

  private static CertifiedMethodSearchEngine.MethodView method(Revision revision, String id) {
    return revision.methods.stream()
        .filter(m -> m.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new Rejected(404, "METHOD_NOT_FOUND"));
  }

  private static Revision revision(Job job, long revision) {
    var found = job.revisions.get(revision);
    if (found == null) throw new Rejected(410, "REVISION_EXPIRED");
    return found;
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
    int used = 0;
    while (used < runBudget) {
      try (var ignored = job.acquire()) {
        if (job.generation != generation || !Set.of("QUEUED", "RUNNING").contains(job.status))
          return;
        if (clock.instant().isAfter(job.expires)) {
          job.status = "PAUSED";
          job.reason = "SEARCH_EXPIRED";
          publish(job);
          return;
        }
        job.status = "RUNNING";
        job.expires = clock.instant().plus(settings.ttl());
        try {
          long until = System.nanoTime() + 20000000L;
          for (int n = 0;
              n < 128
                  && used < runBudget
                  && System.nanoTime() < until
                  && !job.finished()
                  && job.strategyPause() == null
                  && !job.engine.memoryLimited();
              n++, used++) {
            if (!job.engine.complete()) job.engine.step();
            else if (job.strategy == null) {
              if (job.engine.strategyFitsPreparationBudget())
                job.strategy = job.engine.strategyWork();
              else job.strategyBlocked = "STRATEGY_PREPARATION_BUDGET";
            } else {
              job.strategy.step();
              job.strategySteps++;
            }
          }
          if (job.finished()) {
            job.status = "COMPLETED";
            job.reason = null;
            publish(job);
            return;
          }
          if (job.strategyPause() != null || job.engine.memoryLimited() || used >= runBudget) {
            job.status = "PAUSED";
            job.reason =
                job.strategyPause() != null
                    ? job.strategyPause()
                    : job.engine.storageLimited()
                        ? "STORAGE_BUDGET"
                        : job.engine.memoryLimited() ? "MEMORY_BUDGET" : "COMPUTE_BUDGET";
            publish(job);
            return;
          }
          if (used % 512 == 0) publish(job);
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

  private void publish(Job job) {
    if (job.store != null) job.engine.settleCheckpoint();
    if (job.snapshot != null
        && job.publishedGraphRevision == job.engine.graphRevision()
        && job.publishedStrategyRevision == (job.strategy == null ? -1 : job.strategy.revision())
        && job.snapshot.generation() == job.generation
        && job.snapshot.status().equals(job.status)
        && Objects.equals(job.snapshot.reason(), job.reason)) return;
    job.revision++;
    boolean changed = job.publishedGraphRevision != job.engine.graphRevision();
    var previous = job.revisions.isEmpty() ? null : job.revisions.get(job.revision - 1);
    var states = changed || previous == null ? job.engine.states() : previous.states();
    var methods = changed || previous == null ? job.engine.methods() : previous.methods();
    job.publishedGraphRevision = job.engine.graphRevision();
    job.publishedStrategyRevision = job.strategy == null ? -1 : job.strategy.revision();
    var local =
        !changed && job.snapshot != null
            ? job.snapshot.localRecommendations()
            : CertifiedMethodProtocol.locals(
                states.stream().filter(s -> s.id().equals(job.engine.rootId())).toList(),
                methods.stream().filter(m -> m.source().equals(job.engine.rootId())).toList(),
                job.engine.precision());
    job.revisions.put(job.revision, new Revision(states, methods));
    while (job.revisions.size() > 4) job.revisions.remove(job.revisions.keySet().iterator().next());
    job.snapshot =
        new Snapshot(
            VERSION,
            job.id,
            job.revision,
            job.generation,
            job.status,
            job.reason,
            job.engine.complete(),
            job.engine.operations(),
            job.engine.rootId(),
            job.engine.precision(),
            CertifiedInterval.VERSION,
            job.engine.calculationVersion(),
            job.engine.templatesVersion(),
            ConditionalMethodLaw.VERSION,
            job.request.request().start().provenance(),
            job.request.request().goal().catalogVersion(),
            job.engine.assumptions(),
            job.strategyBlocked != null
                ? "PAUSED"
                : job.strategy == null ? "NOT_REQUESTED" : job.strategy.status(),
            CertifiedOutcomeStrategy.VERSION,
            states.size(),
            methods.size(),
            new Storage(
                CertifiedLawStore.VERSION,
                job.engine.retainedEntries(),
                job.engine.storage().storedRows(),
                job.engine.storage().packedBytes(),
                32L * 1024 * 1024,
                job.engine.freezeTransientBytes(),
                job.engine.kernelStorage().packedBytes(),
                job.engine.kernelStorage().packedByteLimit(),
                job.engine.storage().materializedRows(),
                job.engine.storage().uniquePages(),
                CertifiedMutableLawStore.VERSION,
                job.engine.absorptionStorage().rows(),
                job.engine.absorptionStorage().packedBytes(),
                job.engine.absorptionStorage().decodedRows(),
                job.engine.storage().residentPackedBytes(),
                job.engine.storage().diskBytes()),
            local,
            job.strategy == null
                ? List.of()
                : job.strategy.rows().stream()
                    .map(
                        row -> {
                          var result = row.result();
                          return new Strategy(
                              row.stateId(),
                              row.cycles(),
                              result.status(),
                              Probability.of(result.value().lower()),
                              Probability.of(result.value().upper()),
                              result.methodIds(),
                              result.reachableOpen(),
                              result.reachableUnresolved(),
                              result.transitionArithmetic(),
                              result.fixedPointPending());
                        })
                    .toList());
    persist(job);
  }

  private void dispose(Job job) {
    try (var ignored = job.acquire()) {
      job.generation++;
      job.status = "EXPIRED";
      job.disposed = true;
      job.revisions.clear();
      job.commands.clear();
      job.entries.clear();
      job.bindings.clear();
      job.entryIds.clear();
      job.engine.close();
      if (job.store != null) job.store.close();
      job.strategy = null;
    }
  }

  void close() {
    synchronized (jobs) {
      jobs.values().forEach(this::dispose);
      jobs.clear();
    }
  }

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 리비전의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code states}: 상태목록 ({@code List<MethodSearchEngine.StateView>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<CertifiedMethodSearchEngine.MethodView>}).
   * </ul>
   */
  private record Revision(
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods) {}

  /**
   * {@code CertifiedSearchJobs}에서 사용하는 명령의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code command}: 명령 ({@code MethodSearchProtocol.Mutation}).
   *   <li>{@code snapshot}: 스냅샷 ({@code Snapshot}).
   * </ul>
   */
  private record Command(MethodSearchProtocol.Mutation command, Snapshot snapshot) {}

  /**
   * {@code CertifiedSearchJobs} 내부의 Guard 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  private interface Guard extends AutoCloseable {
    @Override
    void close();
  }

  /**
   * {@code CertifiedSearchJobs} 내부의 항목·계산작업 처리를 맡는다.
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
   *   <li>{@code EntryWork(id, parent, law, saved)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자),
   *       {@code parent}(상위값), {@code law}(분포), {@code saved}(저장된).
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

    EntryWork(
        String id,
        String parent,
        ConditionalMethodLaw law,
        ConditionalMethodLaw.ExpansionCheckpoint saved) {
      this.id = id;
      this.parent = parent;
      this.law = law;
      this.expansion = law.restoreExpansion(saved);
    }
  }

  /**
   * {@code CertifiedSearchJobs} 내부의 작업 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code lock}: 잠금 ({@code java.util.concurrent.locks.ReentrantLock}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code store}: 저장소 ({@code MethodGraphStore}).
   *   <li>{@code strategySteps}: 전략·단계목록 ({@code long}).
   *   <li>{@code storeRevision}: 저장소·리비전 ({@code long}).
   *   <li>{@code durableLeaseUntil}: durable·사용권·Until ({@code long}).
   *   <li>{@code checkpointOperations}: 체크포인트·연산목록; 초기값 {@code -1} ({@code long}).
   *   <li>{@code checkpointGraphRevision}: 체크포인트·그래프·리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code checkpointExecution}: 체크포인트·실행 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code Create}).
   *   <li>{@code engine}: 계산 실행기 ({@code CertifiedMethodSearchEngine}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code Instant}).
   *   <li>{@code revisions}: 리비전목록 ({@code Map<Long, Revision>}).
   *   <li>{@code commands}: 명령목록 ({@code Map<String, Command>}).
   *   <li>{@code entries}: 항목목록 ({@code Map<String, EntryWork>}).
   *   <li>{@code bindings}: 결합목록 ({@code Map<String, IndexedMethodProtocol.Bind>}).
   *   <li>{@code entryIds}: 항목·식별자목록 ({@code Map<String, String>}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code publishedGraphRevision}: 공개된·그래프·리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code publishedStrategyRevision}: 공개된·전략·리비전; 초기값 {@code -1} ({@code long}).
   *   <li>{@code strategy}: 전략 ({@code CertifiedStrategyWork}).
   *   <li>{@code strategyBlocked}: 전략·차단 ({@code String}).
   *   <li>{@code disposed}: 해제여부 ({@code boolean}).
   *   <li>{@code limit}: 한도; 초기값 {@code 100000} ({@code int}).
   *   <li>{@code entryLimit}: 항목·한도; 초기값 {@code 1} ({@code int}).
   *   <li>{@code status}: 현재 처리 상태; 초기값 {@code "QUEUED"} ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code snapshot}: 스냅샷 ({@code Snapshot}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code acquire()}: acquire 값을 조회·구성한다. 반환: {@code Guard}.
   *   <li>{@code acquireRequest()}: acquire·요청 값을 조회·구성한다. 반환: {@code Guard}. 사용하는 예외: {@code
   *       Rejected}.
   *   <li>{@code finished()}: 종료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code strategyPause()}: 전략·Pause 값을 조회·구성한다. 반환: {@code String}.
   *   <li>{@code Job(owner, request, engine)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code owner}(소유자),
   *       {@code request}(요청), {@code engine}(계산엔진).
   *   <li>{@code Job(id, owner, request, engine)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       owner}(소유자), {@code request}(요청), {@code engine}(계산엔진).
   * </ul>
   */
  private final class Job {
    final java.util.concurrent.locks.ReentrantLock lock =
        new java.util.concurrent.locks.ReentrantLock(true);

    Guard acquire() {
      lock.lock();
      return lock::unlock;
    }

    Guard acquireRequest() {
      var guard = acquire();
      if (disposed || clock.instant().isAfter(expires)) {
        guard.close();
        throw new Rejected(410, "SEARCH_EXPIRED");
      }
      expires = clock.instant().plus(settings.ttl());
      if (store != null && expires.toEpochMilli() > durableLeaseUntil) {
        try {
          storeRevision =
              store.commit(
                  storeRevision,
                  new MethodGraphStore.Batch(
                      List.of(),
                      List.of(),
                      List.of(),
                      List.of(
                          new MethodGraphStore.Checkpoint(
                              "job/lease", Long.toString(expires.toEpochMilli())))));
          durableLeaseUntil = expires.toEpochMilli();
        } catch (RuntimeException | Error failure) {
          guard.close();
          throw failure;
        }
      }
      return guard;
    }

    final String id, owner;
    MethodGraphStore store;
    long strategySteps;
    long storeRevision;
    long durableLeaseUntil;
    long checkpointOperations = -1, checkpointGraphRevision = -1;
    String checkpointExecution;
    final Create request;
    final CertifiedMethodSearchEngine engine;
    volatile Instant expires = clock.instant().plus(settings.ttl());
    final Map<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new LinkedHashMap<>();
    final Map<String, EntryWork> entries = new LinkedHashMap<>();
    final Map<String, IndexedMethodProtocol.Bind> bindings = new LinkedHashMap<>();
    final Map<String, String> entryIds = new LinkedHashMap<>();
    long revision = -1, generation;
    long publishedGraphRevision = -1;
    long publishedStrategyRevision = -1;
    CertifiedStrategyWork strategy;
    String strategyBlocked;

    boolean finished() {
      return engine.complete() && strategy != null && strategy.complete();
    }

    String strategyPause() {
      return strategyBlocked != null
          ? strategyBlocked
          : strategy == null ? null : strategy.pauseReason();
    }

    boolean disposed;
    int limit = 100000, entryLimit = 1;
    String status = "QUEUED", reason;
    volatile Snapshot snapshot;

    Job(String owner, Create request, CertifiedMethodSearchEngine engine) {
      this(UUID.randomUUID().toString(), ownerKey(owner), request, engine);
    }

    Job(String id, String owner, Create request, CertifiedMethodSearchEngine engine) {
      this.id = id;
      this.owner = owner;
      this.request = request;
      this.engine = engine;
      entries.put("root", new EntryWork("root", null, engine.rootLaw()));
    }
  }
}
