package com.poe2craft.craftingsupport.search.application;

import java.util.*;

/**
 * 탐색 그래프·계산 상태·분포의 영속 저장 경계를 정의한다.
 *
 * <p>기존 계약: Durable calculation catalog; absence or pending work is never a zero-probability
 * result.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "method-graph-store-v1"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code revision()}: 리비전 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code commit(expectedRevision, batch)}: 완전히 기록된 상태를 공개 가능한 체크포인트로 확정한다. 입력: {@code
 *       expectedRevision}(기대값·리비전), {@code batch}(일괄실행). 반환: {@code long}.
 *   <li>{@code source(id)}: 출처의 실행 계약을 정의한다. 입력: {@code id}(식별자). 반환: {@code Optional<Source>}.
 *   <li>{@code method(id)}: 제작방법의 실행 계약을 정의한다. 입력: {@code id}(식별자). 반환: {@code Optional<Method>}.
 *   <li>{@code methods(expectedRevision, after, limit)}: 제작방법목록의 실행 계약을 정의한다. 입력: {@code
 *       expectedRevision}(기대값·리비전), {@code after}(변경후), {@code limit}(한도). 반환: {@code Page}.
 *   <li>{@code observation(method, cycles)}: 관측값의 실행 계약을 정의한다. 입력: {@code method}(제작방법), {@code
 *       cycles}(주기횟수). 반환: {@code Optional<Observation>}.
 *   <li>{@code checkpoint(name)}: 현재 진행 상태의 복구 지점을 기록한다. 입력: {@code name}(이름). 반환: {@code
 *       Optional<Checkpoint>}.
 *   <li>{@code internOperator(digest, canonicalContent)}: 공유색인·연산자의 실행 계약을 정의한다. 입력: {@code
 *       digest}(다이제스트), {@code canonicalContent}(정규형·내용). 반환: {@code long}.
 *   <li>{@code operator(id)}: 연산자의 실행 계약을 정의한다. 입력: {@code id}(식별자). 반환: {@code String}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Identity}: {@code MethodGraphStore}에서 사용하는 정체성의 불변 값이다.
 *   <li>{@code Source}: {@code MethodGraphStore}에서 사용하는 출처의 불변 값이다.
 *   <li>{@code Method}: {@code MethodGraphStore}에서 사용하는 제작방법의 불변 값이다.
 *   <li>{@code Status}: {@code MethodGraphStore}의 상태 구분값을 정의한다.
 *   <li>{@code Observation}: {@code MethodGraphStore}에서 사용하는 관측값의 불변 값이다.
 *   <li>{@code Checkpoint}: {@code MethodGraphStore}에서 사용하는 체크포인트의 불변 값이다.
 *   <li>{@code Batch}: {@code MethodGraphStore}에서 사용하는 일괄실행의 불변 값이다.
 *   <li>{@code MethodRow}: {@code MethodGraphStore}에서 사용하는 제작방법·행의 불변 값이다.
 *   <li>{@code Cursor}: {@code MethodGraphStore}에서 사용하는 진행커서의 불변 값이다.
 *   <li>{@code Page}: {@code MethodGraphStore}에서 사용하는 페이지의 불변 값이다.
 *   <li>{@code Metrics}: {@code MethodGraphStore}에서 사용하는 측정값의 불변 값이다.
 * </ul>
 */
public interface MethodGraphStore extends MethodCursorStore, AutoCloseable {
  String VERSION = "method-graph-store-v1";

  /**
   * {@code MethodGraphStore}에서 사용하는 정체성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code data}: 데이터 ({@code String}).
   *   <li>{@code rules}: 규칙목록 ({@code String}).
   *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code String}).
   *   <li>{@code templates}: 템플릿목록 ({@code String}).
   *   <li>{@code goals}: 목표목록 ({@code String}).
   *   <li>{@code arithmetic}: 산술 ({@code String}).
   *   <li>{@code season}: season ({@code String}).
   *   <li>{@code originalInput}: 원본·입력 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Identity(data, rules, rolls, templates, goals, arithmetic, season,
   *       originalInput)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code data}(데이터), {@code rules}(규칙목록), {@code
   *       rolls}(수치굴림목록), {@code templates}(템플릿목록), {@code goals}(목표목록), {@code arithmetic}(산술),
   *       {@code season}(season), {@code originalInput}(원본·입력). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  record Identity(
      String data,
      String rules,
      String rolls,
      String templates,
      String goals,
      String arithmetic,
      String season,
      String originalInput) {
    public Identity {
      for (var value :
          List.of(data, rules, rolls, templates, goals, arithmetic, season, originalInput))
        if (value.isBlank()) throw new IllegalArgumentException("Complete store identity required");
    }
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 출처의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code long}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code discovered}: 발견된 ({@code boolean}).
   *   <li>{@code content}: 내용 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Source(id, depth, discovered, content)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자),
   *       {@code depth}(깊이), {@code discovered}(발견된), {@code content}(내용). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  record Source(long id, int depth, boolean discovered, String content) {
    public Source {
      if (id < 0 || depth < 0 || content == null || content.isBlank())
        throw new IllegalArgumentException("Invalid source");
    }
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 제작방법의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code source}: 출처 ({@code long}).
   *   <li>{@code program}: program ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Method(id, source, program)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       source}(출처), {@code program}(program). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  record Method(String id, long source, String program) {
    public Method {
      if (id == null || id.isBlank() || source < 0 || program == null || program.isBlank())
        throw new IllegalArgumentException("Invalid method");
    }
  }

  /**
   * {@code MethodGraphStore}의 상태 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code REQUESTED}: REQUESTED 구분값.
   *   <li>{@code PARTIAL}: 부분 구분값.
   *   <li>{@code READY}: 준비완료 구분값.
   *   <li>{@code UNSUPPORTED}: 미지원 구분값.
   * </ul>
   */
  enum Status {
    REQUESTED,
    PARTIAL,
    READY,
    UNSUPPORTED
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 관측값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code method}: 제작방법 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
   *   <li>{@code operator}: 연산자 ({@code Long}).
   *   <li>{@code summary}: 요약 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Observation(method, cycles, status, operator, summary)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code method}(제작방법), {@code cycles}(주기횟수), {@code status}(상태), {@code operator}(연산자),
   *       {@code summary}(요약). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  record Observation(String method, int cycles, Status status, Long operator, String summary) {
    public Observation {
      if (method == null
          || method.isBlank()
          || !List.of(100, 300, 500).contains(cycles)
          || status == null) throw new IllegalArgumentException("Invalid observation");
      if ((status == Status.READY || status == Status.UNSUPPORTED)
          && (operator == null || summary == null))
        throw new IllegalArgumentException(
            "A terminal observation retains its operator and summary");
    }
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 체크포인트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   *   <li>{@code content}: 내용 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Checkpoint(name, content)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code name}(이름), {@code
   *       content}(내용). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  record Checkpoint(String name, String content) {
    public Checkpoint {
      if (name == null || name.isBlank() || content == null)
        throw new IllegalArgumentException("Invalid checkpoint");
    }
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 일괄실행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sources}: 출처목록 ({@code List<Source>}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Method>}).
   *   <li>{@code observations}: 관측값목록 ({@code List<Observation>}).
   *   <li>{@code checkpoints}: 체크포인트목록 ({@code List<Checkpoint>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Batch(sources, methods, observations, checkpoints)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code sources}(출처목록), {@code methods}(제작방법목록), {@code observations}(관측값목록), {@code
   *       checkpoints}(체크포인트목록).
   * </ul>
   */
  record Batch(
      List<Source> sources,
      List<Method> methods,
      List<Observation> observations,
      List<Checkpoint> checkpoints) {
    public Batch {
      sources = List.copyOf(sources);
      methods = List.copyOf(methods);
      observations = List.copyOf(observations);
      checkpoints = List.copyOf(checkpoints);
    }
  }

  /**
   * {@code MethodGraphStore}에서 사용하는 제작방법·행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ordinal}: ordinal ({@code long}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code method}: 제작방법 ({@code Method}).
   * </ul>
   */
  record MethodRow(long ordinal, int depth, Method method) {}

  /**
   * {@code MethodGraphStore}에서 사용하는 진행커서의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code ordinal}: ordinal ({@code long}).
   * </ul>
   */
  record Cursor(int depth, long ordinal) {}

  /**
   * {@code MethodGraphStore}에서 사용하는 페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<MethodRow>}).
   *   <li>{@code next}: 다음 ({@code Cursor}).
   * </ul>
   */
  record Page(long revision, List<MethodRow> methods, Cursor next) {}

  /**
   * {@code MethodGraphStore}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sources}: 출처목록 ({@code long}).
   *   <li>{@code methods}: 제작방법목록 ({@code long}).
   *   <li>{@code operators}: operators ({@code long}).
   *   <li>{@code observations}: 관측값목록 ({@code long}).
   *   <li>{@code ready}: 준비완료 ({@code long}).
   *   <li>{@code diskBytes}: 디스크·바이트수 ({@code long}).
   *   <li>{@code reads}: 읽기횟수 ({@code long}).
   *   <li>{@code writes}: 쓰기횟수 ({@code long}).
   *   <li>{@code cacheKiB}: 캐시·Ki·B ({@code int}).
   *   <li>{@code executionCursors}: 실행·Cursors ({@code long}).
   *   <li>{@code executionRows}: 실행·행목록 ({@code long}).
   * </ul>
   */
  record Metrics(
      long sources,
      long methods,
      long operators,
      long observations,
      long ready,
      long diskBytes,
      long reads,
      long writes,
      int cacheKiB,
      long executionCursors,
      long executionRows) {}

  long revision();

  long commit(long expectedRevision, Batch batch);

  Optional<Source> source(long id);

  Optional<Method> method(String id);

  Page methods(long expectedRevision, Cursor after, int limit);

  Optional<Observation> observation(String method, int cycles);

  Optional<Checkpoint> checkpoint(String name);

  /** Digest equality only selects candidates; actual canonical content must also match. */
  long internOperator(String digest, String canonicalContent);

  String operator(long id);

  Metrics metrics();

  @Override
  void close();
}
