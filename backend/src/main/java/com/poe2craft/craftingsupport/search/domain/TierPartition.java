package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 지원 화폐의 생성 경계에 따라 작업의 티어 구간을 나눈다.
 *
 * <p>기존 계약: Job-fixed disjoint tier cells, split at every supported currency eligibility boundary.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "shared-pool-eligibility-tier-partition-v3"} ({@code
 *       String}).
 *   <li>{@code members}: 구성원목록 ({@code Map<BandKey, Member>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TierPartition(members)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code members}(구성원목록).
 *   <li>{@code empty()}: 빈 초기값 또는 비어 있는지 여부를 구한다. 반환: {@code TierPartition}.
 *   <li>{@code member(modifier, min, max)}: 구성원 처리에서 {@code Optional.ofNullable}, {@code
 *       members.get}을(를) 사용한다. 입력: {@code modifier}(옵션), {@code min}(최소값), {@code max}(최대값). 반환:
 *       {@code Optional<Member>}.
 *   <li>{@code compile(catalog, root, rolls, eligibility)}: 입력 조건을 재사용할 평가·실행 표현으로 바꾼다. 입력: {@code
 *       catalog}(카탈로그), {@code root}(시작점), {@code rolls}(수치굴림목록), {@code eligibility}(생성적격). 반환:
 *       {@code TierPartition}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code BandKey}: {@code TierPartition}에서 사용하는 구간·키의 불변 값이다.
 *   <li>{@code Member}: {@code TierPartition}에서 사용하는 구성원의 불변 값이다.
 *   <li>{@code Key}: {@code TierPartition}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Weighted}: {@code TierPartition}에서 사용하는 가중의 불변 값이다.
 * </ul>
 */
public final class TierPartition {
  public static final String VERSION = "shared-pool-eligibility-tier-partition-v3";

  /**
   * {@code TierPartition}에서 사용하는 구간·키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifier}: 옵션 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   * </ul>
   */
  public record BandKey(String modifier, long min, long max) {}

  /**
   * {@code TierPartition}에서 사용하는 구성원의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code cell}: 셀 ({@code TierCell}).
   *   <li>{@code cellWeight}: 셀·가중치 ({@code Fraction}).
   *   <li>{@code intervalWeight}: 구간·가중치 ({@code Fraction}).
   * </ul>
   */
  public record Member(TierCell cell, Fraction cellWeight, Fraction intervalWeight) {}

  /**
   * {@code TierPartition}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code goalRegion}: 목표·영역 ({@code Object}).
   *   <li>{@code affix}: 접두접미옵션 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code families}: 옵션계열목록 ({@code Set<String>}).
   *   <li>{@code tags}: 태그목록 ({@code Set<String>}).
   *   <li>{@code currencyEligibility}: 화폐·생성적격 ({@code int}).
   *   <li>{@code initialSingleton}: 초기값·Singleton ({@code String}).
   * </ul>
   */
  private record Key(
      Object goalRegion,
      ModifierDefinition.AffixType affix,
      Set<String> families,
      Set<String> tags,
      int currencyEligibility,
      String initialSingleton) {}

  /**
   * {@code TierPartition}에서 사용하는 가중의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code definition}: 정의 ({@code ModifierDefinition}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   *   <li>{@code weight}: 후보 선택에 사용하는 가중치 ({@code Fraction}).
   * </ul>
   */
  private record Weighted(ModifierDefinition definition, long min, long max, Fraction weight) {}

  private final Map<BandKey, Member> members;

  private TierPartition(Map<BandKey, Member> members) {
    this.members = Map.copyOf(members);
  }

  static TierPartition empty() {
    return new TierPartition(Map.of());
  }

  public Optional<Member> member(String modifier, long min, long max) {
    return Optional.ofNullable(members.get(new BandKey(modifier, min, max)));
  }

  static TierPartition compile(
      ItemCatalog catalog,
      ItemState root,
      RollPartition rolls,
      BasicCandidateEligibility eligibility) {
    var groups = new LinkedHashMap<Key, List<Weighted>>();
    var fixed = new HashSet<BandKey>();
    root.explicits()
        .forEach(
            m -> m.values().values().forEach(v -> fixed.add(new BandKey(m.modifierId(), v, v))));
    boolean uniform =
        Set.of(
                "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
            .contains(catalog.metadata().weightPolicy());
    for (var definition : catalog.modifiers().values()) {
      if (definition.layer() != ModifierDefinition.Layer.EXPLICIT
          || definition.weight() <= 0
          || definition.requiredItemLevel() > root.itemLevel()
          || definition.stats().size() != 1) continue;
      var range = definition.stats().getFirst();
      for (long first = range.min(); ; ) {
        long last = rolls.end(definition, first);
        var band = new BandKey(definition.id(), first, last);
        var key =
            new Key(
                rolls.tierRegion(range.id(), first),
                definition.affixType(),
                definition.familyIds(),
                definition.tags(),
                eligibility.category(definition.id()),
                fixed.contains(band) ? definition.id() + ":" + first : "");
        var width =
            BigInteger.valueOf(last).subtract(BigInteger.valueOf(first)).add(BigInteger.ONE);
        var totalWidth =
            BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE);
        var weight =
            new Fraction(
                BigInteger.valueOf(uniform ? 1 : definition.weight()).multiply(width), totalWidth);
        groups
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new Weighted(definition, first, last, weight));
        if (last == range.max()) break;
        first = last + 1;
      }
    }
    var members = new HashMap<BandKey, Member>();
    for (var group : groups.values()) {
      if (group.size() < 2)
        continue; // Keep the established roll-only representation for one interval.
      var total = group.stream().map(Weighted::weight).reduce(Fraction.ZERO, Fraction::add);
      var cell =
          new TierCell(
              group.stream()
                  .map(
                      v ->
                          new TierCell.Variant(
                              v.definition().id(),
                              v.definition().stats().getFirst().id(),
                              v.min(),
                              v.max(),
                              new Fraction(
                                  v.weight().numerator().multiply(total.denominator()),
                                  v.weight().denominator().multiply(total.numerator()))))
                  .toList());
      for (var v : group)
        members.put(
            new BandKey(v.definition().id(), v.min(), v.max()),
            new Member(cell, total, v.weight()));
    }
    return new TierPartition(members);
  }
}
