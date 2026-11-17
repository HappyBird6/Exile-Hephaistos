package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 서로 겹치지 않는 옵션 티어·수치 구간과 정확한 조건부 가중치다.
 *
 * <p>기존 계약: Disjoint modifier-tier/roll intervals with their exact conditional weights.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code variants}: variants ({@code List<Variant>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TierCell(variants)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code variants}(variants). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code representative(fractured)}: 대표상태 처리에서 {@code variants.getFirst}, {@code
 *       first.item}을(를) 사용한다. 입력: {@code fractured}(분열고정). 반환: {@code ModifierInstance}.
 *   <li>{@code cardinality()}: cardinality 값을 조회·구성한다. 반환: {@code BigInteger}.
 *   <li>{@code concrete(offset, fractured)}: 구체값 처리에서 {@code offset.signum}, {@code
 *       offset.compareTo}을(를) 사용한다. 입력: {@code offset}(시작위치), {@code fractured}(분열고정). 반환: {@code
 *       Concrete}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Variant}: {@code TierCell}에서 사용하는 문맥별정의의 불변 값이다.
 *   <li>{@code Concrete}: {@code TierCell}에서 사용하는 구체값의 불변 값이다.
 * </ul>
 */
public record TierCell(List<Variant> variants) {
  /**
   * {@code TierCell}에서 사용하는 문맥별정의의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Variant(modifierId, stat, min, max, probability)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       modifierId}(옵션·식별자), {@code stat}(수치), {@code min}(최소값), {@code max}(최대값), {@code
   *       probability}(확률). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code size()}: 보유 항목의 개수를 조회한다. 반환: {@code BigInteger}.
   *   <li>{@code item(value, fractured)}: 아이템의 입력에 따른 분기·결과를 처리한다. 입력: {@code value}(값), {@code
   *       fractured}(분열고정). 반환: {@code ModifierInstance}.
   * </ul>
   */
  public record Variant(String modifierId, String stat, long min, long max, Fraction probability) {
    public Variant {
      if (modifierId == null || stat == null || min > max || probability.numerator().signum() <= 0)
        throw new IllegalArgumentException("Positive, nonempty tier interval required");
    }

    public BigInteger size() {
      return BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
    }

    ModifierInstance item(long value, boolean fractured) {
      return new ModifierInstance(modifierId, Map.of(stat, value), fractured);
    }
  }

  public TierCell {
    variants =
        variants.stream()
            .sorted(Comparator.comparing(Variant::modifierId).thenComparingLong(Variant::min))
            .toList();
    if (variants.isEmpty()) throw new IllegalArgumentException("Empty tier cell");
    var total = Fraction.ZERO;
    Variant previous = null;
    for (var v : variants) {
      if (previous != null
          && previous.modifierId().equals(v.modifierId())
          && (!previous.stat().equals(v.stat()) || previous.max() >= v.min()))
        throw new IllegalArgumentException("Overlapping tier intervals");
      total = total.add(v.probability());
      previous = v;
    }
    if (!total.equals(Fraction.ONE))
      throw new IllegalArgumentException("Tier law must conserve mass");
  }

  public ModifierInstance representative(boolean fractured) {
    var first = variants.getFirst();
    return first.item(first.min(), fractured);
  }

  public BigInteger cardinality() {
    return variants.stream().map(Variant::size).reduce(BigInteger.ZERO, BigInteger::add);
  }

  /**
   * {@code TierCell}에서 사용하는 구체값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifier}: 옵션 ({@code ModifierInstance}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   */
  public record Concrete(ModifierInstance modifier, Fraction probability) {}

  public Concrete concrete(BigInteger offset, boolean fractured) {
    if (offset.signum() < 0) throw new IllegalArgumentException("Negative tier offset");
    for (var variant : variants) {
      if (offset.compareTo(variant.size()) < 0) {
        var value = BigInteger.valueOf(variant.min()).add(offset).longValueExact();
        var p = variant.probability();
        return new Concrete(
            variant.item(value, fractured),
            new Fraction(p.numerator(), p.denominator().multiply(variant.size())));
      }
      offset = offset.subtract(variant.size());
    }
    throw new IllegalArgumentException("Tier offset out of range");
  }
}
