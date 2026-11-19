package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.shared.item.ItemState.Rarity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 저장·전송 가능한 목표 조건 트리와 버전을 표현한다.
 *
 * <p>기존 계약: Versioned wire AST. Disabled content is preserved and validated, never normalized.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
 *   <li>{@code catalogVersion}: 카탈로그·버전 ({@code String}).
 *   <li>{@code general}: 일반 ({@code General}).
 *   <li>{@code groups}: 그룹목록 ({@code List<Group>}).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Context}: {@code GoalFilter}에서 사용하는 문맥의 불변 값이다.
 *   <li>{@code General}: {@code GoalFilter}에서 사용하는 일반의 불변 값이다.
 *   <li>{@code Range}: {@code GoalFilter}에서 사용하는 범위의 불변 값이다.
 *   <li>{@code Group}: {@code GoalFilter}에서 사용하는 그룹의 불변 값이다.
 *   <li>{@code Entry}: {@code GoalFilter}에서 사용하는 항목의 불변 값이다.
 *   <li>{@code Type}: {@code GoalFilter}의 타입 구분값을 정의한다.
 *   <li>{@code Status}: {@code GoalFilter}의 상태 구분값을 정의한다.
 *   <li>{@code Capability}: {@code GoalFilter}의 Capability 구분값을 정의한다.
 *   <li>{@code Presence}: {@code GoalFilter}의 존재여부 구분값을 정의한다.
 *   <li>{@code Issue}: {@code GoalFilter}에서 사용하는 문제의 불변 값이다.
 *   <li>{@code Severity}: {@code GoalFilter}의 Severity 구분값을 정의한다.
 * </ul>
 */
public record GoalFilter(
    Integer version, String catalogVersion, General general, List<Group> groups) {
  /**
   * {@code GoalFilter}에서 사용하는 문맥의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
   *   <li>{@code baseItemId}: 원본 베이스 식별자 ({@code String}).
   *   <li>{@code itemLevel}: 옵션 생성 제한에 사용하는 아이템 레벨 ({@code Integer}).
   * </ul>
   */
  public record Context(String snapshotId, String baseItemId, Integer itemLevel) {}

  /**
   * {@code GoalFilter}에서 사용하는 일반의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code baseItemId}: 원본 베이스 식별자 ({@code String}).
   *   <li>{@code itemLevel}: 옵션 생성 제한에 사용하는 아이템 레벨 ({@code Range}).
   *   <li>{@code rarities}: 희귀도목록 ({@code List<Rarity>}).
   *   <li>{@code quality}: 품질 ({@code Integer}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code General(baseItemId, itemLevel, rarities)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code rarities}(희귀도목록).
   * </ul>
   */
  public record General(
      String baseItemId,
      Range itemLevel,
      List<Rarity> rarities,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          Integer quality) {
    public General(String baseItemId, Range itemLevel, List<Rarity> rarities) {
      this(baseItemId, itemLevel, rarities, null);
    }
  }

  /**
   * {@code GoalFilter}에서 사용하는 범위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code BigDecimal}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code BigDecimal}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code contains(value)}: 해당 항목의 포함 여부를 확인한다. 입력: {@code value}(값). 반환: {@code boolean}.
   * </ul>
   */
  public record Range(BigDecimal min, BigDecimal max) {
    public boolean contains(BigDecimal value) {
      return (min == null || value.compareTo(min) >= 0)
          && (max == null || value.compareTo(max) <= 0);
    }

    /** 개수 필터의 입력은 보존하고 활성 행 개수에서 요구하는 정수 경계를 계산한다. */
    public Range forCount(int rows) {
      if ((min == null || (min.signum() >= 0 && min.scale() <= 0))
          && (max == null || (max.signum() > 0 && max.scale() <= 0))) return this;
      var rowCount = BigDecimal.valueOf(rows);
      var lower = roundedCount(min);
      // 거래소에서 3행 중 1행이 일치할 때 최소 -2는 통과, -1은 탈락한다. 음수 최소는 1개 이상을 요구한다.
      if (lower != null && lower.signum() < 0)
        lower = lower.compareTo(rowCount.negate()) <= 0 ? BigDecimal.ONE : rowCount.add(lower);
      var upper = roundedCount(max);
      // 최대 0은 제한 없음, -1은 일치 행이 없어야 한다. 3행에서 -2는 최대 1개, -3은 최대 0개다.
      if (upper != null) {
        if (upper.signum() == 0) upper = null;
        else if (upper.equals(BigDecimal.ONE.negate())) upper = BigDecimal.ZERO;
        else if (upper.signum() < 0)
          upper = upper.compareTo(rowCount.negate()) <= 0 ? BigDecimal.ZERO : rowCount.add(upper);
      }
      return new Range(lower, upper);
    }

    /** 가능한 개수보다 큰 지수를 거대한 정수로 펼치지 않으면서 검색 경계를 반올림한다. */
    private static BigDecimal roundedCount(BigDecimal value) {
      if (value == null || value.scale() <= 0) return value;
      if (value.abs().compareTo(BigDecimal.valueOf(5, 1)) < 0) return BigDecimal.ZERO;
      return value.setScale(0, RoundingMode.HALF_UP);
    }
  }

  /**
   * {@code GoalFilter}에서 사용하는 그룹의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code type}: 타입 ({@code Type}).
   *   <li>{@code disabled}: 비활성 ({@code Boolean}).
   *   <li>{@code range}: 범위 ({@code Range}).
   *   <li>{@code entries}: 항목목록 ({@code List<Entry>}).
   * </ul>
   */
  public record Group(String id, Type type, Boolean disabled, Range range, List<Entry> entries) {
    /** 활성 행이 있는 그룹만 목표 판정에 참여한다. 빈 편집 그룹과 비활성 행만 있는 그룹은 건너뛴다. */
    public boolean active() {
      if (!Boolean.FALSE.equals(disabled) || entries == null) return false;
      for (var entry : entries)
        if (entry != null && Boolean.FALSE.equals(entry.disabled())) return true;
      return false;
    }

    /** 이 그룹이 행별 가중치로 점수를 계산하는지 반환한다. */
    public boolean weighted() {
      return type == Type.WEIGHTED_V1 || type == Type.WEIGHTED_V2;
    }

    /** 가중합의 빈 칸은 계산할 때만 1로 읽는다. 다른 그룹에는 점수 가중치가 없다. */
    public BigDecimal scoreWeight(Entry entry) {
      if (!weighted()) return null;
      return entry.weight() == null ? BigDecimal.ONE : entry.weight();
    }
  }

  /**
   * {@code GoalFilter}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code statId}: 수치·식별자 ({@code String}).
   *   <li>{@code unit}: 단위 ({@code String}).
   *   <li>{@code range}: 범위 ({@code Range}).
   *   <li>{@code weight}: 입력한 점수 가중치. 빈 칸은 {@code null}로 보존한다.
   *   <li>{@code disabled}: 비활성 ({@code Boolean}).
   * </ul>
   */
  public record Entry(
      String id, String statId, String unit, Range range, BigDecimal weight, Boolean disabled) {}

  /**
   * {@code GoalFilter}의 타입 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code AND}: 및 구분값.
   *   <li>{@code NOT}: NOT 구분값.
   *   <li>{@code IF}: IF 구분값.
   *   <li>{@code COUNT}: 개수 구분값.
   *   <li>{@code WEIGHTED_V1}: 1 구분값.
   *   <li>{@code WEIGHTED_V2}: 2 구분값.
   * </ul>
   */
  public enum Type {
    AND,
    NOT,
    IF,
    COUNT,
    WEIGHTED_V1,
    WEIGHTED_V2
  }

  /**
   * {@code GoalFilter}의 상태 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code MATCH}: 일치 구분값.
   *   <li>{@code NO_MATCH}: 일치 구분값.
   *   <li>{@code UNKNOWN}: 미확정 구분값.
   *   <li>{@code UNSUPPORTED}: 미지원 구분값.
   * </ul>
   */
  public enum Status {
    MATCH,
    NO_MATCH,
    UNKNOWN,
    UNSUPPORTED
  }

  /**
   * {@code GoalFilter}의 Capability 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code SUPPORTED}: 지원 구분값.
   *   <li>{@code UNKNOWN}: 미확정 구분값.
   *   <li>{@code UNSUPPORTED}: 미지원 구분값.
   * </ul>
   */
  public enum Capability {
    SUPPORTED,
    UNKNOWN,
    UNSUPPORTED
  }

  /**
   * {@code GoalFilter}의 존재여부 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code PRESENT}: PRESENT 구분값.
   *   <li>{@code ABSENT}: ABSENT 구분값.
   *   <li>{@code UNKNOWN}: 미확정 구분값.
   * </ul>
   */
  public enum Presence {
    PRESENT,
    ABSENT,
    UNKNOWN
  }

  /**
   * {@code GoalFilter}에서 사용하는 문제의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code String}).
   *   <li>{@code path}: 경로 ({@code String}).
   *   <li>{@code message}: 메시지 ({@code String}).
   *   <li>{@code severity}: severity ({@code Severity}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code error(code, path, message)}: 오류의 입력에 따른 분기·결과를 처리한다. 입력: {@code code}(코드), {@code
   *       path}(경로), {@code message}(메시지). 반환: {@code Issue}.
   *   <li>{@code warning(code, path, message)}: 경고의 입력에 따른 분기·결과를 처리한다. 입력: {@code code}(코드),
   *       {@code path}(경로), {@code message}(메시지). 반환: {@code Issue}.
   * </ul>
   */
  public record Issue(String code, String path, String message, Severity severity) {
    public static Issue error(String code, String path, String message) {
      return new Issue(code, path, message, Severity.ERROR);
    }

    public static Issue warning(String code, String path, String message) {
      return new Issue(code, path, message, Severity.WARNING);
    }
  }

  /**
   * {@code GoalFilter}의 Severity 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code ERROR}: 오류 구분값.
   *   <li>{@code WARNING}: 경고 구분값.
   * </ul>
   */
  public enum Severity {
    ERROR,
    WARNING
  }
}
