package com.poe2craft.shared.economy.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 한 리그의 한 수집 시점에 대한 추정 가격이다. 제작 확률 규칙과 분리한다.
 *
 * <p>기존 계약: Prices are estimates from one league and one collection, separate from crafting rules.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code league}: league ({@code String}).
 *   <li>{@code updatedAt}: 갱신시각·기준 ({@code Instant}).
 *   <li>{@code exaltedPerDivine}: 엑잘티드·단위별·신성 ({@code BigDecimal}).
 *   <li>{@code prices}: 시세목록 ({@code Map<String, Quote>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PriceSnapshot(league, updatedAt, exaltedPerDivine, prices)}: 전달받은 값으로 구성원을 초기화한다.
 *       입력: {@code league}(league), {@code updatedAt}(갱신시각·기준), {@code
 *       exaltedPerDivine}(엑잘티드·단위별·신성), {@code prices}(시세목록). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Quote}: {@code PriceSnapshot}에서 사용하는 Quote의 불변 값이다.
 * </ul>
 */
public record PriceSnapshot(
    String league, Instant updatedAt, BigDecimal exaltedPerDivine, Map<String, Quote> prices) {
  /**
   * {@code PriceSnapshot}에서 사용하는 Quote의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   *   <li>{@code divine}: 신성 ({@code BigDecimal}).
   *   <li>{@code exalted}: 엑잘티드 ({@code BigDecimal}).
   * </ul>
   */
  public record Quote(String name, BigDecimal divine, BigDecimal exalted) {}

  public PriceSnapshot {
    if (league == null
        || league.isBlank()
        || updatedAt == null
        || exaltedPerDivine == null
        || exaltedPerDivine.signum() <= 0
        || prices == null
        || prices.isEmpty()) throw new IllegalArgumentException("Invalid price snapshot");
    prices = Map.copyOf(prices);
  }
}
