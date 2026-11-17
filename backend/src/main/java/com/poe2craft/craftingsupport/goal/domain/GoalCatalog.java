package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.shared.item.ModifierDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 목표 편집기에 노출하는 수치·단위·출처 정보를 담는다.
 *
 * <p>기존 계약: Display units and source scales are explicit; production IDs are local, never GGG IDs.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
 *   <li>{@code catalogVersion}: 카탈로그·버전 ({@code String}).
 *   <li>{@code context}: 문맥 ({@code Context}).
 *   <li>{@code groupTypes}: 그룹·Types ({@code List<GroupSupport>}).
 *   <li>{@code stats}: 수치목록 ({@code List<Stat>}).
 *   <li>{@code issues}: 문제목록 ({@code List<Issue>}).
 *   <li>{@code sourceModifiers}: 출처·옵션목록 ({@code Map<String, ModifierDefinition>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalCatalog(version, catalogVersion, context, groupTypes, stats, issues)}: 기본 정책을
 *       보충해 공통 생성자로 위임한다. 입력: {@code version}(버전), {@code catalogVersion}(카탈로그·버전), {@code
 *       context}(문맥), {@code groupTypes}(그룹·Types), {@code stats}(수치목록), {@code issues}(문제목록).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Support}: {@code GoalCatalog}에서 사용하는 지원범위의 불변 값이다.
 *   <li>{@code GroupSupport}: {@code GoalCatalog}에서 사용하는 그룹·지원범위의 불변 값이다.
 *   <li>{@code Contribution}: {@code GoalCatalog}에서 사용하는 Contribution의 불변 값이다.
 *   <li>{@code Stat}: {@code GoalCatalog}에서 사용하는 수치의 불변 값이다.
 *   <li>{@code Kind}: {@code GoalCatalog}의 종류 구분값을 정의한다.
 *   <li>{@code Mapping}: {@code GoalCatalog}에서 사용하는 매핑의 불변 값이다.
 * </ul>
 */
public record GoalCatalog(
    int version,
    String catalogVersion,
    Context context,
    List<GroupSupport> groupTypes,
    List<Stat> stats,
    List<Issue> issues,
    Map<String, ModifierDefinition> sourceModifiers) {
  public GoalCatalog(
      int version,
      String catalogVersion,
      Context context,
      List<GroupSupport> groupTypes,
      List<Stat> stats,
      List<Issue> issues) {
    this(version, catalogVersion, context, groupTypes, stats, issues, Map.of());
  }

  /**
   * {@code GoalCatalog}에서 사용하는 지원범위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code evaluation}: 평가 ({@code Capability}).
   *   <li>{@code probability}: 확률 ({@code Capability}).
   *   <li>{@code reasonCode}: 이유·코드 ({@code String}).
   * </ul>
   */
  public record Support(Capability evaluation, Capability probability, String reasonCode) {}

  /**
   * {@code GoalCatalog}에서 사용하는 그룹·지원범위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code type}: 타입 ({@code Type}).
   *   <li>{@code evaluation}: 평가 ({@code Capability}).
   *   <li>{@code probability}: 확률 ({@code Capability}).
   *   <li>{@code reasonCode}: 이유·코드 ({@code String}).
   * </ul>
   */
  public record GroupSupport(
      Type type, Capability evaluation, Capability probability, String reasonCode) {}

  /**
   * {@code GoalCatalog}에서 사용하는 Contribution의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code statId}: 수치·식별자 ({@code String}).
   *   <li>{@code coefficient}: 계수 ({@code BigDecimal}).
   * </ul>
   */
  public record Contribution(String statId, BigDecimal coefficient) {}

  /**
   * {@code GoalCatalog}에서 사용하는 수치의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code statId}: 수치·식별자 ({@code String}).
   *   <li>{@code label}: 표시이름 ({@code String}).
   *   <li>{@code unit}: 단위 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code Kind}).
   *   <li>{@code support}: 지원범위 ({@code Support}).
   *   <li>{@code eligible}: 적격 ({@code boolean}).
   *   <li>{@code eligibilityReason}: 생성적격·이유 ({@code String}).
   *   <li>{@code sourceStatIds}: 출처·수치·식별자목록 ({@code List<String>}).
   *   <li>{@code contributions}: contributions ({@code List<Contribution>}).
   *   <li>{@code sourceUrls}: 출처·Urls ({@code List<String>}).
   *   <li>{@code sourceDivisor}: 출처·나누는수 ({@code Long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Stat(statId, label, unit, kind, support, eligible, eligibilityReason,
   *       sourceStatIds, contributions, sourceUrls)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       statId}(수치·식별자), {@code label}(표시이름), {@code unit}(단위), {@code kind}(종류), {@code
   *       support}(지원범위), {@code eligible}(적격), {@code eligibilityReason}(생성적격·이유), {@code
   *       sourceStatIds}(출처·수치·식별자목록), {@code contributions}(contributions), {@code
   *       sourceUrls}(출처·Urls).
   * </ul>
   */
  public record Stat(
      String statId,
      String label,
      String unit,
      Kind kind,
      Support support,
      boolean eligible,
      String eligibilityReason,
      List<String> sourceStatIds,
      List<Contribution> contributions,
      List<String> sourceUrls,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Long sourceDivisor) {
    public Stat(
        String statId,
        String label,
        String unit,
        Kind kind,
        Support support,
        boolean eligible,
        String eligibilityReason,
        List<String> sourceStatIds,
        List<Contribution> contributions,
        List<String> sourceUrls) {
      this(
          statId,
          label,
          unit,
          kind,
          support,
          eligible,
          eligibilityReason,
          sourceStatIds,
          contributions,
          sourceUrls,
          null);
    }
  }

  /**
   * {@code GoalCatalog}의 종류 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code EXPLICIT}: 명시옵션 구분값.
   *   <li>{@code IMPLICIT}: 암시옵션 구분값.
   *   <li>{@code PSEUDO}: 의사수치 구분값.
   * </ul>
   */
  public enum Kind {
    EXPLICIT,
    IMPLICIT,
    PSEUDO
  }

  /**
   * {@code GoalCatalog}에서 사용하는 매핑의 불변 값이다.
   *
   * <p>기존 계약: Exact rational conversion from one raw stat, scoped to its modifier layer.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sourceStatId}: 출처·수치·식별자 ({@code String}).
   *   <li>{@code layer}: 옵션계층 ({@code String}).
   *   <li>{@code numerator}: 분자 ({@code long}).
   *   <li>{@code denominator}: 분모 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Mapping(sourceStatId, layer, numerator, denominator)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code sourceStatId}(출처·수치·식별자), {@code layer}(옵션계층), {@code numerator}(분자), {@code
   *       denominator}(분모). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Mapping(String sourceStatId, String layer, long numerator, long denominator) {
    public Mapping {
      if (denominator <= 0)
        throw new IllegalArgumentException("Positive scale denominator required");
    }
  }
}
