package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 현재 생성 가능한 옵션 풀에 맞는 조건부 티어 분포를 만든다.
 *
 * <p>기존 계약: A fresh posterior over the actual eligible pool, never a job-global tier prior.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code RollPartition}).
 *   <li>{@code fixed}: 고정 ({@code Set<TierPartition.BandKey>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticTierPartition(rolls)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code rolls}(수치굴림목록).
 *   <li>{@code members(candidates, uniform, budget)}: 구성원목록 처리에서 {@code definition.stats}, {@code
 *       definition.weight}을(를) 사용한다. 입력: {@code candidates}(후보목록), {@code uniform}(균등), {@code
 *       budget}(계산예산). 반환: {@code Map<TierPartition.BandKey, TierPartition.Member>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code AutomaticTierPartition}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Slice}: {@code AutomaticTierPartition}에서 사용하는 분할작업의 불변 값이다.
 * </ul>
 */
public final class AutomaticTierPartition {
  /**
   * {@code AutomaticTierPartition}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code region}: 영역 ({@code Object}).
   *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code families}: 옵션계열목록 ({@code Set<String>}).
   *   <li>{@code tags}: 태그목록 ({@code Set<String>}).
   *   <li>{@code fixed}: 고정 ({@code String}).
   * </ul>
   */
  private record Key(
      String stat,
      Object region,
      ModifierDefinition.AffixType side,
      Set<String> families,
      Set<String> tags,
      String fixed) {}

  /**
   * {@code AutomaticTierPartition}에서 사용하는 분할작업의 불변 값이다.
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
  private record Slice(ModifierDefinition definition, long min, long max, Fraction weight) {}

  private final RollPartition rolls;
  private final Set<TierPartition.BandKey> fixed;

  public AutomaticTierPartition(RollPartition rolls) {
    this.rolls = rolls;
    var initial = new HashSet<TierPartition.BandKey>();
    if (rolls.initial() != null)
      rolls
          .initial()
          .explicits()
          .forEach(
              m ->
                  m.values()
                      .values()
                      .forEach(v -> initial.add(new TierPartition.BandKey(m.modifierId(), v, v))));
    fixed = Set.copyOf(initial);
  }

  public Map<TierPartition.BandKey, TierPartition.Member> members(
      List<ModifierDefinition> candidates, boolean uniform, long budget) {
    var groups = new LinkedHashMap<Key, List<Slice>>();
    long visited = 0;
    for (var definition : candidates) {
      if (definition.stats().size() != 1 || (!uniform && definition.weight() == 0)) continue;
      var range = definition.stats().getFirst();
      var width =
          BigInteger.valueOf(range.max())
              .subtract(BigInteger.valueOf(range.min()))
              .add(BigInteger.ONE);
      for (long first = range.min(); ; ) {
        if (++visited > budget) return Map.of(); // No partially compiled prior is ever emitted.
        long last = rolls.end(definition, first);
        var band = new TierPartition.BandKey(definition.id(), first, last);
        var key =
            new Key(
                range.id(),
                rolls.automaticRegion(range.id(), first),
                definition.affixType(),
                definition.familyIds(),
                definition.tags(),
                fixed.contains(band) ? definition.id() + ":" + first : "");
        var weight =
            new Fraction(
                BigInteger.valueOf(uniform ? 1 : definition.weight())
                    .multiply(
                        BigInteger.valueOf(last)
                            .subtract(BigInteger.valueOf(first))
                            .add(BigInteger.ONE)),
                width);
        groups
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new Slice(definition, first, last, weight));
        if (last == range.max()) break;
        first = last + 1;
      }
    }
    var result = new HashMap<TierPartition.BandKey, TierPartition.Member>();
    for (var slices : groups.values()) {
      if (slices.size() < 2) continue;
      var total = slices.stream().map(Slice::weight).reduce(Fraction.ZERO, Fraction::add);
      var cell =
          new TierCell(
              slices.stream()
                  .map(
                      s ->
                          new TierCell.Variant(
                              s.definition().id(),
                              s.definition().stats().getFirst().id(),
                              s.min(),
                              s.max(),
                              s.weight().divide(total)))
                  .toList());
      for (var slice : slices)
        result.put(
            new TierPartition.BandKey(slice.definition().id(), slice.min(), slice.max()),
            new TierPartition.Member(cell, total, slice.weight()));
    }
    return Map.copyOf(result);
  }
}
