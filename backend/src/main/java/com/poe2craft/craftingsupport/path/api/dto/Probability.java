package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.shared.probability.Fraction;

/** 정확한 유리수 확률을 numerator와 denominator에 십진 문자열로 보관한다. of는 계산 결과의 분자와 분모를 변환한다. */
public record Probability(String numerator, String denominator) {
  public static Probability of(Fraction f) {
    return new Probability(f.numerator().toString(), f.denominator().toString());
  }
}
