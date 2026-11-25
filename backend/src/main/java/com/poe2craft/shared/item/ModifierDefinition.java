package com.poe2craft.shared.item;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 옵션 정체성·계열·티어·가중치·출처 수치 범위를 담는 불변 정의다.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code id}: 이 값의 식별자 ({@code String}).
 *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
 *   <li>{@code layer}: 옵션계층 ({@code Layer}).
 *   <li>{@code affixType}: 접두접미옵션·타입 ({@code AffixType}).
 *   <li>{@code familyIds}: 중복 부여를 제한하는 옵션 계열 목록 ({@code Set<String>}).
 *   <li>{@code requiredItemLevel}: 필수·아이템·레벨 ({@code int}).
 *   <li>{@code weight}: 후보 선택에 사용하는 가중치 ({@code int}).
 *   <li>{@code tier}: 정의된 옵션 티어 ({@code int}).
 *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
 *   <li>{@code stats}: 수치목록 ({@code List<StatRange>}).
 *   <li>{@code tags}: 태그목록 ({@code Set<String>}).
 *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ModifierDefinition(id, name, layer, affixType, familyIds, requiredItemLevel, weight,
 *       tier, text, stats, tags, sourceUrl)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
 *       name}(이름), {@code layer}(옵션계층), {@code affixType}(접두접미옵션·타입), {@code
 *       familyIds}(옵션계열·식별자목록), {@code requiredItemLevel}(필수·아이템·레벨), {@code weight}(가중치), {@code
 *       tier}(티어), {@code text}(텍스트), {@code stats}(수치목록), {@code tags}(태그목록), {@code
 *       sourceUrl}(출처·출처주소). 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Layer}: {@code ModifierDefinition}의 옵션계층 구분값을 정의한다.
 *   <li>{@code AffixType}: {@code ModifierDefinition}의 접두접미옵션·타입 구분값을 정의한다.
 *   <li>{@code StatRange}: {@code ModifierDefinition}에서 사용하는 수치·범위의 불변 값이다.
 * </ul>
 */
public record ModifierDefinition(
    String id,
    String name,
    Layer layer,
    AffixType affixType,
    Set<String> familyIds,
    int requiredItemLevel,
    int weight,
    int tier,
    String text,
    List<StatRange> stats,
    Set<String> tags,
    String sourceUrl) {
  public ModifierDefinition {
    if (id == null
        || id.isBlank()
        || name == null
        || name.isBlank()
        || text == null
        || text.isBlank()
        || sourceUrl == null
        || sourceUrl.isBlank()) {
      throw new IllegalArgumentException("Modifier identity, text and source are required");
    }
    Objects.requireNonNull(layer, "layer");
    Objects.requireNonNull(affixType, "affixType");
    familyIds = Set.copyOf(familyIds);
    tags = Set.copyOf(tags);
    stats = List.copyOf(stats);
    if (familyIds.isEmpty()
        || familyIds.stream().anyMatch(String::isBlank)
        || stats.isEmpty()
        || stats.stream().map(StatRange::id).distinct().count() != stats.size()
        || requiredItemLevel < 1
        || requiredItemLevel > 100
        || weight < 0) {
      throw new IllegalArgumentException("Invalid modifier definition: " + id);
    }
    if (layer == Layer.IMPLICIT
        ? affixType != AffixType.NONE || tier != 0 || weight != 0
        : affixType == AffixType.NONE
            || (tier < 1
                && !(tier == 0
                    && (tags.contains("reveal_only") || tags.contains("augment_pool"))))) {
      throw new IllegalArgumentException("Invalid modifier layer/tier/affix: " + id);
    }
  }

  /** 별세계의 수동 공개 수치 키는 여러 옵션이 공유하므로 목표에는 개별 옵션 ID를 쓴다. */
  public String goalSource(StatRange range) {
    return tags.contains("otherworldly") ? id : range.id();
  }

  /**
   * {@code ModifierDefinition}의 옵션계층 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code IMPLICIT}: 암시옵션 구분값.
   *   <li>{@code EXPLICIT}: 명시옵션 구분값.
   * </ul>
   */
  public enum Layer {
    IMPLICIT,
    EXPLICIT
  }

  /**
   * {@code ModifierDefinition}의 접두접미옵션·타입 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code NONE}: NONE 구분값.
   *   <li>{@code PREFIX}: 접두 구분값.
   *   <li>{@code SUFFIX}: 접미 구분값.
   * </ul>
   */
  public enum AffixType {
    NONE,
    PREFIX,
    SUFFIX
  }

  /**
   * {@code ModifierDefinition}에서 사용하는 수치·범위의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code StatRange(id, min, max)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       min}(최소값), {@code max}(최대값). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code contains(value)}: 해당 항목의 포함 여부를 확인한다. 입력: {@code value}(값). 반환: {@code boolean}.
   * </ul>
   */
  public record StatRange(String id, long min, long max) {
    public StatRange {
      if (id == null || id.isBlank() || min > max) {
        throw new IllegalArgumentException("Invalid stat range");
      }
    }

    public boolean contains(long value) {
      return value >= min && value <= max;
    }
  }
}
