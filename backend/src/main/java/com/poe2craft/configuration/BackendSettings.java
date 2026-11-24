package com.poe2craft.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;

/**
 * 환경별 운영 한도와 저장·로그·요청 설정을 타입으로 표현한다. 게임 규칙은 포함하지 않는다.
 *
 * <p>기존 계약: Typed operational settings. Edit backend-settings.yaml; game rules remain in sealed
 * catalogs.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code pathSearch}: 경로·탐색 ({@code PathSearch}).
 *   <li>{@code methodSearch}: 제작방법·탐색 ({@code MethodSearch}).
 *   <li>{@code certifiedSearch}: 확률구간·탐색 ({@code CertifiedSearch}).
 *   <li>{@code automaticSearch}: 자동·탐색 ({@code AutomaticSearch}).
 *   <li>{@code testRoutes}: 제작 지원 테스트의 일반·증강물 풀 등장 빈도 분류 기준.
 *   <li>{@code cache}: 캐시 ({@code Cache}).
 *   <li>{@code storage}: 저장소 ({@code Storage}).
 *   <li>{@code workbench}: 작업대 ({@code Workbench}).
 *   <li>{@code prices}: 시세목록 ({@code Prices}).
 *   <li>{@code requestProtection}: 요청·보호정책 ({@code RequestProtection}).
 *   <li>{@code adminLogs}: admin·로그목록 ({@code AdminLogs}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code positive(value, name)}: 양수 조건을 확인한다. 입력: {@code value}(값), {@code name}(이름). 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code defaults()}: 기본값목록 값을 조회·구성한다. 반환: {@code BackendSettings}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code AdminLogs}: {@code BackendSettings}에서 사용하는 Admin·로그목록의 불변 값이다.
 *   <li>{@code RequestProtection}: {@code BackendSettings}에서 사용하는 요청·보호정책의 불변 값이다.
 *   <li>{@code PathSearch}: {@code BackendSettings}에서 사용하는 경로·탐색의 불변 값이다.
 *   <li>{@code MethodSearch}: {@code BackendSettings}에서 사용하는 제작방법·탐색의 불변 값이다.
 *   <li>{@code CertifiedSearch}: {@code BackendSettings}에서 사용하는 확률구간·탐색의 불변 값이다.
 *   <li>{@code AutomaticSearch}: {@code BackendSettings}에서 사용하는 자동·탐색의 불변 값이다.
 *   <li>{@code TestRoutes}: 제작 지원 테스트의 등장 빈도 분류와 계산 예산이다.
 *   <li>{@code Cache}: {@code BackendSettings}에서 사용하는 캐시의 불변 값이다.
 *   <li>{@code Storage}: {@code BackendSettings}에서 사용하는 저장소의 불변 값이다.
 *   <li>{@code Workbench}: {@code BackendSettings}에서 사용하는 작업대의 불변 값이다.
 *   <li>{@code Prices}: {@code BackendSettings}에서 사용하는 시세목록의 불변 값이다.
 *   <li>{@code Defaults}: {@code BackendSettings} 내부의 기본값목록 처리를 맡는다.
 * </ul>
 */
@Validated
@ConfigurationProperties("backend")
public record BackendSettings(
    @Valid @NotNull PathSearch pathSearch,
    @Valid @NotNull MethodSearch methodSearch,
    @Valid @NotNull CertifiedSearch certifiedSearch,
    @Valid @NotNull AutomaticSearch automaticSearch,
    @Valid @NotNull TestRoutes testRoutes,
    @Valid @NotNull Cache cache,
    @Valid @NotNull Storage storage,
    @Valid @NotNull Workbench workbench,
    @Valid @NotNull Prices prices,
    @Valid @NotNull RequestProtection requestProtection,
    @Valid @NotNull AdminLogs adminLogs) {

  /** lowBelow 미만은 L, highFrom 이상은 H로 분류하며 outcomeBudget은 한 요청이 검증할 제작 결과 수다. */
  public record TestRoutes(double lowBelow, double highFrom, @Min(1) int outcomeBudget) {
    /** 두 경계가 유효한 확률인지 시작 시 검사한다. */
    public TestRoutes {
      if (!(0 < lowBelow && lowBelow < highFrom && highFrom <= 1)) {
        throw new IllegalArgumentException("Invalid test route frequency thresholds");
      }
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 Admin·로그목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code previousDays}: 이전·일 ({@code int}).
   *   <li>{@code queueCapacity}: 대기열·용량 ({@code int}).
   *   <li>{@code batchSize}: 일괄실행·크기 ({@code int}).
   *   <li>{@code flushMillis}: 기록반영·밀리초 ({@code int}).
   *   <li>{@code archiveMillis}: 아카이브·밀리초 ({@code int}).
   *   <li>{@code archiveWorkMillis}: 아카이브·계산작업·밀리초 ({@code int}).
   *   <li>{@code archiveDirectory}: 아카이브·디렉터리 ({@code String}).
   *   <li>{@code timeZone}: 시간·시간대 ({@code String}).
   *   <li>{@code pageSize}: 페이지·크기 ({@code int}).
   *   <li>{@code archiveDays}: 아카이브·일 ({@code int}).
   *   <li>{@code archiveMaxBytes}: 아카이브·최대값·바이트수 ({@code long}).
   *   <li>{@code dailyRecords}: daily·기록목록 ({@code int}).
   *   <li>{@code statisticsCacheEntries}: 통계·캐시·항목목록 ({@code int}).
   *   <li>{@code statisticsCacheMillis}: 통계·캐시·밀리초 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code AdminLogs(previousDays, queueCapacity, batchSize, flushMillis, archiveMillis,
   *       archiveWorkMillis, archiveDirectory, timeZone, pageSize, archiveDays, archiveMaxBytes,
   *       dailyRecords, statisticsCacheEntries, statisticsCacheMillis)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code previousDays}(이전·일), {@code queueCapacity}(대기열·용량), {@code batchSize}(일괄실행·크기),
   *       {@code flushMillis}(기록반영·밀리초), {@code archiveMillis}(아카이브·밀리초), {@code
   *       archiveWorkMillis}(아카이브·계산작업·밀리초), {@code archiveDirectory}(아카이브·디렉터리), {@code
   *       timeZone}(시간·시간대), {@code pageSize}(페이지·크기), {@code archiveDays}(아카이브·일), {@code
   *       archiveMaxBytes}(아카이브·최대값·바이트수), {@code dailyRecords}(daily·기록목록), {@code
   *       statisticsCacheEntries}(통계·캐시·항목목록), {@code statisticsCacheMillis}(통계·캐시·밀리초).
   *   <li>{@code AdminLogs(previousDays, queueCapacity, batchSize, flushMillis, archiveMillis,
   *       archiveWorkMillis, archiveDirectory, timeZone, pageSize, archiveDays, archiveMaxBytes,
   *       dailyRecords)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code previousDays}(이전·일), {@code
   *       queueCapacity}(대기열·용량), {@code batchSize}(일괄실행·크기), {@code flushMillis}(기록반영·밀리초), {@code
   *       archiveMillis}(아카이브·밀리초), {@code archiveWorkMillis}(아카이브·계산작업·밀리초), {@code
   *       archiveDirectory}(아카이브·디렉터리), {@code timeZone}(시간·시간대), {@code pageSize}(페이지·크기), {@code
   *       archiveDays}(아카이브·일), {@code archiveMaxBytes}(아카이브·최대값·바이트수), {@code
   *       dailyRecords}(daily·기록목록).
   * </ul>
   */
  public record AdminLogs(
      @Min(1) @Max(31) int previousDays,
      @Min(100) int queueCapacity,
      @Min(1) @Max(5000) int batchSize,
      @Min(100) int flushMillis,
      @Min(1000) int archiveMillis,
      @Min(100) @Max(60000) int archiveWorkMillis,
      @NotBlank String archiveDirectory,
      @NotBlank String timeZone,
      @Min(1) @Max(200) int pageSize,
      @Min(1) int archiveDays,
      @Min(1048576) long archiveMaxBytes,
      @Min(1) int dailyRecords,
      @Min(1) int statisticsCacheEntries,
      @Min(100) int statisticsCacheMillis) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public AdminLogs {}

    public AdminLogs(
        int previousDays,
        int queueCapacity,
        int batchSize,
        int flushMillis,
        int archiveMillis,
        int archiveWorkMillis,
        String archiveDirectory,
        String timeZone,
        int pageSize,
        int archiveDays,
        long archiveMaxBytes,
        int dailyRecords) {
      this(
          previousDays,
          queueCapacity,
          batchSize,
          flushMillis,
          archiveMillis,
          archiveWorkMillis,
          archiveDirectory,
          timeZone,
          pageSize,
          archiveDays,
          archiveMaxBytes,
          dailyRecords,
          defaults().adminLogs().statisticsCacheEntries(),
          defaults().adminLogs().statisticsCacheMillis());
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 요청·보호정책의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code requestsPerSecond}: 요청목록·단위별·두번째 ({@code int}).
   *   <li>{@code burst}: burst ({@code int}).
   *   <li>{@code maxInFlight}: 최대값·입력·진행중 ({@code int}).
   *   <li>{@code maxBodyBytes}: 최대값·본문·바이트수 ({@code int}).
   * </ul>
   */
  public record RequestProtection(
      @Min(1) int requestsPerSecond,
      @Min(1) int burst,
      @Min(1) int maxInFlight,
      @Min(1024) @Max(1048576) int maxBodyBytes) {}

  /**
   * {@code BackendSettings}에서 사용하는 경로·탐색의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxJobs}: 최대값·작업목록 ({@code int}).
   *   <li>{@code maxRevisions}: 최대값·리비전목록 ({@code int}).
   *   <li>{@code pageEdges}: 페이지·간선목록 ({@code int}).
   *   <li>{@code maxCommands}: 최대값·명령목록 ({@code int}).
   *   <li>{@code runBudget}: 실행·계산예산 ({@code int}).
   *   <li>{@code ttl}: 만료시간 ({@code Duration}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code PathSearch(maxJobs, maxRevisions, pageEdges, maxCommands, runBudget, ttl)}: 전달받은
   *       값으로 구성원을 초기화한다. 입력: {@code maxJobs}(최대값·작업목록), {@code maxRevisions}(최대값·리비전목록), {@code
   *       pageEdges}(페이지·간선목록), {@code maxCommands}(최대값·명령목록), {@code runBudget}(실행·계산예산), {@code
   *       ttl}(만료시간).
   * </ul>
   */
  public record PathSearch(
      @Min(1) int maxJobs,
      @Min(1) int maxRevisions,
      @Min(1) int pageEdges,
      @Min(1) int maxCommands,
      @Min(1) int runBudget,
      @NotNull Duration ttl) {
    public PathSearch {
      positive(ttl, "path-search.ttl");
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 제작방법·탐색의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxJobs}: 최대값·작업목록 ({@code int}).
   *   <li>{@code maxRevisions}: 최대값·리비전목록 ({@code int}).
   *   <li>{@code pageSize}: 페이지·크기 ({@code int}).
   *   <li>{@code maxCommands}: 최대값·명령목록 ({@code int}).
   *   <li>{@code runBudget}: 실행·계산예산 ({@code int}).
   *   <li>{@code certifiedRunBudget}: 확률구간·실행·계산예산 ({@code int}).
   *   <li>{@code kernelOutcomeLimit}: 전이커널·결과·한도 ({@code int}).
   *   <li>{@code timeSlice}: 시간·분할작업 ({@code Duration}).
   *   <li>{@code ttl}: 만료시간 ({@code Duration}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code MethodSearch(maxJobs, maxRevisions, pageSize, maxCommands, runBudget,
   *       certifiedRunBudget, kernelOutcomeLimit, timeSlice, ttl)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       maxJobs}(최대값·작업목록), {@code maxRevisions}(최대값·리비전목록), {@code pageSize}(페이지·크기), {@code
   *       maxCommands}(최대값·명령목록), {@code runBudget}(실행·계산예산), {@code
   *       certifiedRunBudget}(확률구간·실행·계산예산), {@code kernelOutcomeLimit}(전이커널·결과·한도), {@code
   *       timeSlice}(시간·분할작업), {@code ttl}(만료시간).
   * </ul>
   */
  public record MethodSearch(
      @Min(1) int maxJobs,
      @Min(1) int maxRevisions,
      @Min(1) int pageSize,
      @Min(1) int maxCommands,
      @Min(1) int runBudget,
      @Min(1) int certifiedRunBudget,
      @Min(1) int kernelOutcomeLimit,
      @NotNull Duration timeSlice,
      @NotNull Duration ttl) {
    public MethodSearch {
      positive(timeSlice, "method-search.time-slice");
      positive(ttl, "method-search.ttl");
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 확률구간·탐색의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxJobs}: 최대값·작업목록 ({@code int}).
   *   <li>{@code ttl}: 만료시간 ({@code Duration}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code CertifiedSearch(maxJobs, ttl)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       maxJobs}(최대값·작업목록), {@code ttl}(만료시간).
   * </ul>
   */
  public record CertifiedSearch(@Min(1) int maxJobs, @NotNull Duration ttl) {
    public CertifiedSearch {
      positive(ttl, "certified-search.ttl");
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 자동·탐색의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxActive}: 최대값·활성 ({@code int}).
   *   <li>{@code ttl}: 만료시간 ({@code Duration}).
   *   <li>{@code timeSlice}: 시간·분할작업 ({@code Duration}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code AutomaticSearch(maxActive, ttl, timeSlice)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       maxActive}(최대값·활성), {@code ttl}(만료시간), {@code timeSlice}(시간·분할작업).
   * </ul>
   */
  public record AutomaticSearch(
      @Min(1) int maxActive, @NotNull Duration ttl, @NotNull Duration timeSlice) {
    public AutomaticSearch {
      positive(ttl, "automatic-search.ttl");
      positive(timeSlice, "automatic-search.time-slice");
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 캐시의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code transitionEntries}: 전이·항목목록 ({@code int}).
   *   <li>{@code transitionOutcomes}: 전이·결과목록 ({@code int}).
   *   <li>{@code additionEntries}: 추가·항목목록 ({@code int}).
   * </ul>
   */
  public record Cache(
      @Min(1) int transitionEntries, @Min(1) int transitionOutcomes, @Min(1) int additionEntries) {}

  /**
   * {@code BackendSettings}에서 사용하는 저장소의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code methodJobDirectory}: 제작방법·작업·디렉터리 ({@code String}).
   * </ul>
   */
  public record Storage(@NotBlank String methodJobDirectory) {}

  /**
   * {@code BackendSettings}에서 사용하는 작업대의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code repeatConcurrency}: 반복·동시작업수 ({@code int}).
   *   <li>{@code repeatTimeBudget}: 반복·시간·계산예산 ({@code Duration}).
   *   <li>{@code repeatReceiptTtl}: 반복·Receipt·만료시간 ({@code Duration}).
   *   <li>{@code requestConcurrency}: 요청·동시작업수 ({@code int}).
   *   <li>{@code referenceCatalogEntries}: 참조·카탈로그·항목목록 ({@code int}).
   *   <li>{@code referenceStateEntries}: 참조·상태·항목목록 ({@code int}).
   *   <li>{@code supportEnabled}: 지원범위·사용여부 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Workbench(repeatConcurrency, repeatTimeBudget, repeatReceiptTtl,
   *       requestConcurrency, referenceCatalogEntries, referenceStateEntries, supportEnabled)}:
   *       전달받은 값으로 구성원을 초기화한다. 입력: {@code repeatConcurrency}(반복·동시작업수), {@code
   *       repeatTimeBudget}(반복·시간·계산예산), {@code repeatReceiptTtl}(반복·Receipt·만료시간), {@code
   *       requestConcurrency}(요청·동시작업수), {@code referenceCatalogEntries}(참조·카탈로그·항목목록), {@code
   *       referenceStateEntries}(참조·상태·항목목록), {@code supportEnabled}(지원범위·사용여부). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code Workbench(repeatConcurrency)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       repeatConcurrency}(반복·동시작업수).
   * </ul>
   */
  public record Workbench(
      @Min(1) int repeatConcurrency,
      @NotNull Duration repeatTimeBudget,
      @NotNull Duration repeatReceiptTtl,
      @Min(1) int requestConcurrency,
      @Min(1) int referenceCatalogEntries,
      @Min(1) int referenceStateEntries,
      boolean supportEnabled) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public Workbench {
      positive(repeatTimeBudget, "workbench.repeat-time-budget");
      positive(repeatReceiptTtl, "workbench.repeat-receipt-ttl");
      if (repeatReceiptTtl.compareTo(repeatTimeBudget.multipliedBy(2)) < 0)
        throw new IllegalArgumentException("Receipt retention must exceed execution budget");
    }

    public Workbench(int repeatConcurrency) {
      this(
          repeatConcurrency,
          defaults().workbench().repeatTimeBudget(),
          defaults().workbench().repeatReceiptTtl(),
          defaults().workbench().requestConcurrency(),
          defaults().workbench().referenceCatalogEntries(),
          defaults().workbench().referenceStateEntries(),
          defaults().workbench().supportEnabled());
    }
  }

  /**
   * {@code BackendSettings}에서 사용하는 시세목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code proxyUrl}: proxy·출처주소 ({@code String}).
   *   <li>{@code userAgent}: 사용자·에이전트 ({@code String}).
   *   <li>{@code cacheKey}: 캐시·키 ({@code String}).
   *   <li>{@code refreshInterval}: 갱신·구간 ({@code Duration}).
   *   <li>{@code checkInterval}: check·구간 ({@code Duration}).
   *   <li>{@code minimumRefreshInterval}: 최소값·갱신·구간 ({@code Duration}).
   *   <li>{@code refreshLease}: 갱신·사용권 ({@code Duration}).
   *   <li>{@code connectTimeout}: connect·제한시간 ({@code Duration}).
   *   <li>{@code requestTimeout}: 요청·제한시간 ({@code Duration}).
   *   <li>{@code initialDelay}: 초기값·Delay ({@code Duration}).
   *   <li>{@code categories}: 분류목록 ({@code java.util.List<&#64;NotBlank String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Prices(sourceUrl, proxyUrl, userAgent, cacheKey, refreshInterval, checkInterval,
   *       minimumRefreshInterval, refreshLease, connectTimeout, requestTimeout, initialDelay,
   *       categories)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code sourceUrl}(출처·출처주소), {@code
   *       proxyUrl}(proxy·출처주소), {@code userAgent}(사용자·에이전트), {@code cacheKey}(캐시·키), {@code
   *       refreshInterval}(갱신·구간), {@code checkInterval}(check·구간), {@code
   *       minimumRefreshInterval}(최소값·갱신·구간), {@code refreshLease}(갱신·사용권), {@code
   *       connectTimeout}(connect·제한시간), {@code requestTimeout}(요청·제한시간), {@code
   *       initialDelay}(초기값·Delay), {@code categories}(분류목록). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Prices(
      @NotBlank String sourceUrl,
      @NotNull String proxyUrl,
      @NotBlank String userAgent,
      @NotBlank String cacheKey,
      @NotNull Duration refreshInterval,
      @NotNull Duration checkInterval,
      @NotNull Duration minimumRefreshInterval,
      @NotNull Duration refreshLease,
      @NotNull Duration connectTimeout,
      @NotNull Duration requestTimeout,
      @NotNull Duration initialDelay,
      @NotEmpty java.util.List<@NotBlank String> categories) {
    public Prices {
      positive(refreshInterval, "prices.refresh-interval");
      positive(checkInterval, "prices.check-interval");
      positive(minimumRefreshInterval, "prices.minimum-refresh-interval");
      positive(refreshLease, "prices.refresh-lease");
      positive(connectTimeout, "prices.connect-timeout");
      positive(requestTimeout, "prices.request-timeout");
      positive(initialDelay, "prices.initial-delay");
      categories = java.util.List.copyOf(categories);
      if (!categories.contains("Currency") || minimumRefreshInterval.compareTo(refreshInterval) > 0)
        throw new IllegalArgumentException("Invalid currency price schedule");
    }
  }

  private static void positive(Duration value, String name) {
    if (value == null || value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException(name + " must be a positive duration");
    }
  }

  /**
   * Direct construction in unit tests uses the same shipped defaults as Spring, never a second
   * list.
   */
  public static BackendSettings defaults() {
    return Defaults.VALUE;
  }

  /**
   * {@code BackendSettings} 내부의 기본값목록 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code VALUE}: 한 번 검증한 공유 값 ({@code BackendSettings}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code BackendSettings}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   */
  private static final class Defaults {
    private static final BackendSettings VALUE = load();

    private static BackendSettings load() {
      try {
        var environment = new StandardEnvironment();
        // Defaults must not change with the shell running a unit test.
        environment
            .getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment
            .getPropertySources()
            .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        for (var source :
            new YamlPropertySourceLoader()
                .load("backend-defaults", new ClassPathResource("backend-settings.yaml"))) {
          environment.getPropertySources().addLast(source);
        }
        return new Binder(
                ConfigurationPropertySources.get(environment),
                new org.springframework.boot.context.properties.bind
                    .PropertySourcesPlaceholdersResolver(environment))
            .bind("backend", Bindable.of(BackendSettings.class))
            .orElseThrow(() -> new IllegalStateException("Backend defaults are missing"));
      } catch (IOException error) {
        throw new IllegalStateException("Cannot load backend-settings.yaml", error);
      }
    }
  }
}
