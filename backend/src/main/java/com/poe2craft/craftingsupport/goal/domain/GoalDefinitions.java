package com.poe2craft.craftingsupport.goal.domain;

import java.util.*;

/**
 * 목표 수치의 표시 단위와 의사 수치 구성 원본을 표현한다.
 *
 * <p>기존 계약: Reviewed display units and direct pseudo sources; never grants probability support.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
 *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
 *   <li>{@code stats}: 수치목록 ({@code List<Direct>}).
 *   <li>{@code pseudos}: pseudos ({@code List<Pseudo>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalDefinitions(schemaVersion, ruleVersion, stats, pseudos)}: 전달받은 값으로 구성원을 초기화한다.
 *       입력: {@code schemaVersion}(스키마·버전), {@code ruleVersion}(규칙·버전), {@code stats}(수치목록), {@code
 *       pseudos}(pseudos). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code require(value)}: 필수 조건을 검사한다. 입력: {@code value}(값). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code unit(value)}: 단위의 입력에 따른 분기·결과를 처리한다. 입력: {@code value}(값). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code directStats()}: 직접·수치목록 값을 조회·구성한다. 반환: {@code Map<String, Direct>}.
 *   <li>{@code signature()}: 내용 기반 버전 계산에 사용할 서명을 구한다. 반환: {@code String}.
 *   <li>{@code append(result, value)}: 뒤에 값이나 계산 행을 추가한다. 입력: {@code result}(결과), {@code value}(값).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Direct}: {@code GoalDefinitions}에서 사용하는 직접의 불변 값이다.
 *   <li>{@code Pseudo}: {@code GoalDefinitions}에서 사용하는 의사수치의 불변 값이다.
 * </ul>
 */
public record GoalDefinitions(
    int schemaVersion, String ruleVersion, List<Direct> stats, List<Pseudo> pseudos) {
  /**
   * {@code GoalDefinitions}에서 사용하는 직접의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sourceStatId}: 출처·수치·식별자 ({@code String}).
   *   <li>{@code label}: 표시이름 ({@code String}).
   *   <li>{@code unit}: 단위 ({@code String}).
   *   <li>{@code sourceDivisor}: 출처·나누는수 ({@code Long}).
   *   <li>{@code evaluationOnly}: 평가·제한 ({@code Boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Direct(sourceStatId, label, unit, sourceDivisor, evaluationOnly)}: 전달받은 값으로 구성원을
   *       초기화한다. 입력: {@code sourceStatId}(출처·수치·식별자), {@code label}(표시이름), {@code unit}(단위), {@code
   *       sourceDivisor}(출처·나누는수), {@code evaluationOnly}(평가·제한).
   *   <li>{@code Direct(sourceStatId, label, unit, sourceDivisor)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
   *       {@code sourceStatId}(출처·수치·식별자), {@code label}(표시이름), {@code unit}(단위), {@code
   *       sourceDivisor}(출처·나누는수).
   *   <li>{@code Direct(sourceStatId, label, unit)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       sourceStatId}(출처·수치·식별자), {@code label}(표시이름), {@code unit}(단위).
   * </ul>
   */
  public record Direct(
      String sourceStatId, String label, String unit, Long sourceDivisor, Boolean evaluationOnly) {
    public Direct {
      if (sourceDivisor != null && sourceDivisor == 1) sourceDivisor = null;
    }

    public Direct(String sourceStatId, String label, String unit, Long sourceDivisor) {
      this(sourceStatId, label, unit, sourceDivisor, null);
    }

    public Direct(String sourceStatId, String label, String unit) {
      this(sourceStatId, label, unit, null);
    }
  }

  /**
   * {@code GoalDefinitions}에서 사용하는 의사수치의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code label}: 표시이름 ({@code String}).
   *   <li>{@code unit}: 단위 ({@code String}).
   *   <li>{@code sourceStatIds}: 출처·수치·식별자목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Pseudo(id, label, unit, sourceStatIds)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자),
   *       {@code label}(표시이름), {@code unit}(단위), {@code sourceStatIds}(출처·수치·식별자목록).
   * </ul>
   */
  public record Pseudo(String id, String label, String unit, List<String> sourceStatIds) {
    public Pseudo {
      sourceStatIds = List.copyOf(sourceStatIds);
    }
  }

  public GoalDefinitions {
    if (schemaVersion != 1 || ruleVersion == null || ruleVersion.isBlank())
      throw new IllegalArgumentException("Unsupported goal definitions version");
    stats = List.copyOf(stats);
    pseudos = List.copyOf(pseudos);
    var ids = new HashMap<String, Direct>();
    for (var stat : stats) {
      require(stat.sourceStatId());
      require(stat.label());
      unit(stat.unit());
      if (stat.sourceDivisor() != null && stat.sourceDivisor() <= 0)
        throw new IllegalArgumentException("Positive source divisor required");
      if (ids.putIfAbsent(stat.sourceStatId(), stat) != null)
        throw new IllegalArgumentException("Duplicate goal source stat: " + stat.sourceStatId());
    }
    var pseudoIds = new HashSet<String>();
    for (var pseudo : pseudos) {
      require(pseudo.id());
      require(pseudo.label());
      unit(pseudo.unit());
      if (!pseudo.id().matches("[a-z0-9_]+")
          || !pseudoIds.add(pseudo.id())
          || pseudo.sourceStatIds().isEmpty()
          || new HashSet<>(pseudo.sourceStatIds()).size() != pseudo.sourceStatIds().size())
        throw new IllegalArgumentException("Invalid pseudo definition: " + pseudo.id());
      for (var source : pseudo.sourceStatIds()) {
        var direct = ids.get(source);
        if (direct == null || !direct.unit().equals(pseudo.unit()))
          throw new IllegalArgumentException("Unknown or incompatible pseudo source: " + source);
      }
    }
  }

  private static void require(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Missing goal definition field");
  }

  private static void unit(String value) {
    if (!Set.of("flat", "percent").contains(value))
      throw new IllegalArgumentException("Unreviewed goal unit");
  }

  public Map<String, Direct> directStats() {
    var result = new TreeMap<String, Direct>();
    stats.forEach(s -> result.put(s.sourceStatId(), s));
    return Collections.unmodifiableMap(result);
  }

  /** Order-independent data identity. Length prefixes prevent delimiter collisions. */
  public String signature() {
    var result = new StringBuilder();
    stats.stream()
        .sorted(Comparator.comparing(Direct::sourceStatId))
        .forEach(
            s -> {
              append(result, s.sourceStatId());
              append(result, s.label());
              append(result, s.unit());
              if (Boolean.TRUE.equals(s.evaluationOnly())) append(result, "evaluationOnly=true");
              if (s.sourceDivisor() != null && s.sourceDivisor() != 1)
                append(result, "sourceDivisor=" + s.sourceDivisor());
            });
    pseudos.stream()
        .sorted(Comparator.comparing(Pseudo::id))
        .forEach(
            p -> {
              append(result, p.id());
              append(result, p.label());
              append(result, p.unit());
              result.append(p.sourceStatIds().size()).append(':');
              p.sourceStatIds().stream().sorted().forEach(s -> append(result, s));
            });
    return result.toString();
  }

  private static void append(StringBuilder result, String value) {
    result.append(value.length()).append(':').append(value);
  }
}
