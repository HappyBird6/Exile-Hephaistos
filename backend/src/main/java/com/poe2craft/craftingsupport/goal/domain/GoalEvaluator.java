package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 검증된 관측값을 목표 조건에 대입한다. 누락된 수치는 미확정으로 유지한다.
 *
 * <p>기존 계약: Pure evaluation over proven observations. Missing keys mean unknown, never absent.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code evaluate(goal, general, observations, issues)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code
 *       goal}(목표), {@code general}(일반), {@code observations}(관측값목록), {@code issues}(문제목록). 반환:
 *       {@code Evaluation}.
 *   <li>{@code pseudo(contributions, sources)}: 의사수치 처리에서 {@code sources.getOrDefault}, {@code
 *       contribution.statId}을(를) 사용한다. 입력: {@code contributions}(contributions), {@code
 *       sources}(출처목록). 반환: {@code Observation}.
 *   <li>{@code group(group, observations)}: 그룹 처리에서 {@code group.entries}, {@code
 *       entry.disabled}을(를) 사용한다. 입력: {@code group}(그룹), {@code observations}(관측값목록). 반환: {@code
 *       GroupResult}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code row(entry, observation)}: 행 처리에서 {@code observation.support}, {@code
 *       observation.presence}을(를) 사용한다. 입력: {@code entry}(항목), {@code observation}(관측값). 반환: {@code
 *       Status}.
 *   <li>{@code and(statuses)}: 및 처리에서 {@code statuses.contains}을(를) 사용한다. 입력: {@code
 *       statuses}(statuses). 반환: {@code Status}.
 *   <li>{@code uncertainty(statuses)}: uncertainty 처리에서 {@code statuses.contains}을(를) 사용한다. 입력:
 *       {@code statuses}(statuses). 반환: {@code Status}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Observation}: {@code GoalEvaluator}에서 사용하는 관측값의 불변 값이다.
 *   <li>{@code EntryResult}: {@code GoalEvaluator}에서 사용하는 항목·결과의 불변 값이다.
 *   <li>{@code GroupResult}: {@code GoalEvaluator}에서 사용하는 그룹·결과의 불변 값이다.
 *   <li>{@code Evaluation}: {@code GoalEvaluator}에서 사용하는 평가의 불변 값이다.
 * </ul>
 */
public final class GoalEvaluator {
  /**
   * {@code GoalEvaluator}에서 사용하는 관측값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code presence}: 존재여부 ({@code Presence}).
   *   <li>{@code value}: 값 ({@code BigDecimal}).
   *   <li>{@code support}: 지원범위 ({@code Capability}).
   *   <li>{@code exact}: 정확값 ({@code GoalNumber}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Observation(presence, value, support)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       presence}(존재여부), {@code value}(값), {@code support}(지원범위).
   *   <li>{@code Observation(presence, value, support, exact)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       presence}(존재여부), {@code value}(값), {@code support}(지원범위), {@code exact}(정확값). 사용하는 예외:
   *       {@code IllegalArgumentException}.
   *   <li>{@code present(value)}: present의 입력에 따른 분기·결과를 처리한다. 입력: {@code value}(값). 반환: {@code
   *       Observation}.
   *   <li>{@code presentNumber(number)}: present·숫자 처리에서 {@code number.display}을(를) 사용한다. 입력:
   *       {@code number}(숫자). 반환: {@code Observation}.
   *   <li>{@code absent()}: absent 값을 조회·구성한다. 반환: {@code Observation}.
   *   <li>{@code unknown()}: 미확정 값을 조회·구성한다. 반환: {@code Observation}.
   *   <li>{@code unsupported()}: 미지원 조건을 나타내는 결과를 만든다. 반환: {@code Observation}.
   * </ul>
   */
  public record Observation(
      Presence presence, BigDecimal value, Capability support, GoalNumber exact) {
    public Observation(Presence presence, BigDecimal value, Capability support) {
      this(presence, value, support, value == null ? null : GoalNumber.of(value));
    }

    public Observation {
      if ((presence == Presence.PRESENT) != (value != null))
        throw new IllegalArgumentException("Only present observations have a numeric value");
      if ((presence == Presence.PRESENT) != (exact != null))
        throw new IllegalArgumentException("Only present observations have an exact number");
    }

    public static Observation present(BigDecimal value) {
      return new Observation(Presence.PRESENT, value, Capability.SUPPORTED);
    }

    public static Observation presentNumber(GoalNumber number) {
      return new Observation(Presence.PRESENT, number.display(), Capability.SUPPORTED, number);
    }

    public static Observation absent() {
      return new Observation(Presence.ABSENT, null, Capability.SUPPORTED);
    }

    public static Observation unknown() {
      return new Observation(Presence.UNKNOWN, null, Capability.UNKNOWN);
    }

    public static Observation unsupported() {
      return new Observation(Presence.UNKNOWN, null, Capability.UNSUPPORTED);
    }
  }

  /**
   * {@code GoalEvaluator}에서 사용하는 항목·결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
   *   <li>{@code presence}: 존재여부 ({@code Presence}).
   *   <li>{@code value}: 값 ({@code BigDecimal}).
   *   <li>{@code exactValue}: 정확값·값 ({@code GoalNumber.Ratio}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code EntryResult(id, status, presence, value)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       id}(식별자), {@code status}(상태), {@code presence}(존재여부), {@code value}(값).
   * </ul>
   */
  public record EntryResult(
      String id,
      Status status,
      Presence presence,
      BigDecimal value,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          GoalNumber.Ratio exactValue) {
    public EntryResult(String id, Status status, Presence presence, BigDecimal value) {
      this(id, status, presence, value, null);
    }
  }

  /**
   * {@code GoalEvaluator}에서 사용하는 그룹·결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
   *   <li>{@code count}: 개수 ({@code Integer}).
   *   <li>{@code score}: score ({@code BigDecimal}).
   *   <li>{@code entries}: 항목목록 ({@code List<EntryResult>}).
   *   <li>{@code exactScore}: 정확값·Score ({@code GoalNumber.Ratio}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code GroupResult(id, status, count, score, entries)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
   *       {@code id}(식별자), {@code status}(상태), {@code count}(개수), {@code score}(score), {@code
   *       entries}(항목목록).
   * </ul>
   */
  public record GroupResult(
      String id,
      Status status,
      Integer count,
      BigDecimal score,
      List<EntryResult> entries,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          GoalNumber.Ratio exactScore) {
    public GroupResult(
        String id, Status status, Integer count, BigDecimal score, List<EntryResult> entries) {
      this(id, status, count, score, entries, null);
    }
  }

  /**
   * {@code GoalEvaluator}에서 사용하는 평가의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code catalogVersion}: 카탈로그·버전 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
   *   <li>{@code generalStatus}: 일반·상태 ({@code Status}).
   *   <li>{@code groups}: 그룹목록 ({@code List<GroupResult>}).
   *   <li>{@code issues}: 문제목록 ({@code List<Issue>}).
   * </ul>
   */
  public record Evaluation(
      int version,
      String catalogVersion,
      Status status,
      Status generalStatus,
      List<GroupResult> groups,
      List<Issue> issues) {}

  public Evaluation evaluate(
      GoalFilter goal, Status general, Map<String, Observation> observations, List<Issue> issues) {
    var results = new ArrayList<GroupResult>();
    for (var group : goal.groups()) {
      if (group.active()) results.add(group(group, observations));
    }
    var statuses = new ArrayList<Status>();
    statuses.add(general);
    results.forEach(g -> statuses.add(g.status()));
    return new Evaluation(
        1,
        goal.catalogVersion(),
        and(statuses),
        general,
        List.copyOf(results),
        List.copyOf(issues));
  }

  public static Observation pseudo(
      List<GoalCatalog.Contribution> contributions, Map<String, Observation> sources) {
    var sum = GoalNumber.ZERO;
    boolean present = false, unknown = false, unsupported = false;
    for (var contribution : contributions) {
      var source = sources.getOrDefault(contribution.statId(), Observation.unknown());
      unsupported |= source.support() == Capability.UNSUPPORTED;
      unknown |= source.presence() == Presence.UNKNOWN;
      if (source.presence() == Presence.PRESENT) {
        present = true;
        sum = sum.add(source.exact().multiply(contribution.coefficient()));
      }
    }
    if (unsupported) return Observation.unsupported();
    if (unknown) return Observation.unknown();
    return present ? Observation.presentNumber(sum) : Observation.absent();
  }

  private GroupResult group(Group group, Map<String, Observation> observations) {
    var entries = new ArrayList<EntryResult>();
    var hard = new ArrayList<Status>();
    int matched = 0, uncertain = 0, present = 0;
    var score = GoalNumber.ZERO;
    for (var entry : group.entries()) {
      if (entry.disabled()) continue;
      var value = observations.getOrDefault(entry.statId(), Observation.unknown());
      var status = row(entry, value);
      entries.add(
          new EntryResult(
              entry.id(),
              status,
              value.presence(),
              value.value(),
              value.exact() == null ? null : value.exact().ratio()));
      if (status == Status.MATCH) matched++;
      if (status == Status.UNKNOWN || status == Status.UNSUPPORTED) uncertain++;
      if (value.presence() == Presence.PRESENT) present++;
      var ifStatus = value.presence() == Presence.ABSENT ? Status.MATCH : status;
      hard.add(ifStatus);
      var weight = group.scoreWeight(entry);
      if (value.presence() == Presence.PRESENT
          && weight != null
          && (group.type() == Type.WEIGHTED_V1 || status == Status.MATCH))
        score = score.add(value.exact().multiply(weight));
    }
    var statuses = entries.stream().map(EntryResult::status).toList();
    Status result;
    switch (group.type()) {
      case AND -> result = and(statuses);
      case IF -> result = and(hard);
      case NOT ->
          result = statuses.contains(Status.MATCH) ? Status.NO_MATCH : uncertainty(statuses);
      case COUNT -> {
        var low = BigDecimal.valueOf(matched);
        var high = BigDecimal.valueOf(matched + uncertain);
        var range = group.range().forCount(entries.size());
        if ((range.min() != null && range.max() != null && range.min().compareTo(range.max()) > 0)
            || (range.max() != null && low.compareTo(range.max()) > 0)
            || (range.min() != null && high.compareTo(range.min()) < 0)) result = Status.NO_MATCH;
        else if (range.contains(low) && range.contains(high)) result = Status.MATCH;
        else result = uncertainty(statuses);
      }
      case WEIGHTED_V1 -> {
        result = and(hard);
        if (result == Status.MATCH)
          result = score.within(group.range()) ? Status.MATCH : Status.NO_MATCH;
      }
      case WEIGHTED_V2 -> {
        if (uncertain > 0) result = uncertainty(statuses);
        else result = present > 0 && score.within(group.range()) ? Status.MATCH : Status.NO_MATCH;
      }
      default -> throw new IllegalStateException("Unknown group type");
    }
    return new GroupResult(
        group.id(),
        result,
        group.type() == Type.COUNT && uncertain == 0 ? matched : null,
        group.weighted() && uncertain == 0 ? score.display() : null,
        List.copyOf(entries),
        group.weighted() && uncertain == 0 ? score.ratio() : null);
  }

  private static Status row(Entry entry, Observation observation) {
    if (observation.support() == Capability.UNSUPPORTED) return Status.UNSUPPORTED;
    if (observation.presence() == Presence.UNKNOWN) return Status.UNKNOWN;
    return observation.presence() == Presence.PRESENT && observation.exact().within(entry.range())
        ? Status.MATCH
        : Status.NO_MATCH;
  }

  public static Status and(List<Status> statuses) {
    return statuses.contains(Status.NO_MATCH) ? Status.NO_MATCH : uncertainty(statuses);
  }

  private static Status uncertainty(List<Status> statuses) {
    if (statuses.contains(Status.UNSUPPORTED)) return Status.UNSUPPORTED;
    return statuses.contains(Status.UNKNOWN) ? Status.UNKNOWN : Status.MATCH;
  }
}
