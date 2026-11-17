package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * 사용자가 승인한 복합 수치 굴림 가정을 계산한다. 실제 게임에서 증명된 분포는 아니다.
 *
 * <p>기존 계약: User-approved conjecture, not a verified game distribution. See ISSUES.md WB-001.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code ID}: 식별자; 초기값 {@code "user-coupled-ratio-half-up-v1"} ({@code String}).
 *   <li>{@code RESOLUTION}: RESOLUTION; 초기값 {@code 10000} ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CoupledStatRollModel()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code roll(definition, random, assumptions)}: 정의된 수치 범위와 분포 가정에 따라 난수로 결과를 정한다. 입력: {@code
 *       definition}(정의), {@code random}(난수생성기), {@code assumptions}(계산가정). 반환: {@code
 *       ModifierInstance}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code assumption(definition, tick)}: assumption 처리에서 {@code
 *       WorkbenchSimulator.Assumption}, {@code definition.id}을(를) 사용한다. 입력: {@code definition}(정의),
 *       {@code tick}(tick). 반환: {@code WorkbenchSimulator.Assumption}.
 *   <li>{@code cells(definition)}: 셀목록 처리에서 {@code definition.stats}, {@code values.put}을(를) 사용한다.
 *       입력: {@code definition}(정의). 반환: {@code List<Cell>}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code value(stat, tick)}: 값 처리에서 {@code BigInteger.valueOf}, {@code stat.min}을(를) 사용한다.
 *       입력: {@code stat}(수치), {@code tick}(tick). 반환: {@code long}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Cell}: {@code CoupledStatRollModel}에서 사용하는 셀의 불변 값이다.
 * </ul>
 */
public final class CoupledStatRollModel {
  public static final String ID = "user-coupled-ratio-half-up-v1";
  public static final int RESOLUTION = 10_000;

  private CoupledStatRollModel() {}

  public static ModifierInstance roll(
      ModifierDefinition definition,
      RandomGenerator random,
      List<WorkbenchSimulator.Assumption> assumptions) {
    if (definition.stats().size() < 2)
      throw new IllegalArgumentException("Coupled model requires multiple verified source stats");
    int tick = random.nextInt(RESOLUTION + 1);
    var values = new LinkedHashMap<String, Long>();
    for (var stat : definition.stats()) values.put(stat.id(), value(stat, tick));
    assumptions.add(assumption(definition, tick));
    return new ModifierInstance(definition.id(), values);
  }

  public static WorkbenchSimulator.Assumption assumption(
      ModifierDefinition definition, Integer tick) {
    return new WorkbenchSimulator.Assumption(
        ID,
        "assumed shared ratio ticks (not distinct item outcomes)",
        RESOLUTION + 1L,
        List.of(definition.id()),
        0L,
        (long) RESOLUTION,
        definition.sourceUrl(),
        "Unverified user conjecture approved 2026-10-02; ISSUES.md WB-001. "
            + "One shared ratio tick/10000 for all stats; 10001 equally sampled model ticks, "
            + "not a game-verified permitted outcome set or uniform rounded outcomes. "
            + "Interpolate each source min/max; integer HALF_UP rounding, ties away from zero. "
            + "Different ticks may produce the same tuple; modifier selection weights are separate.",
        tick);
  }

  /**
   * {@code CoupledStatRollModel}에서 사용하는 셀의 불변 값이다.
   *
   * <p>기존 계약: Each joint tuple keeps the count of source ticks producing it, never uniform tuples.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifier}: 옵션 ({@code ModifierInstance}).
   *   <li>{@code ticks}: ticks ({@code int}).
   * </ul>
   */
  public record Cell(ModifierInstance modifier, int ticks) {}

  public static List<Cell> cells(ModifierDefinition definition) {
    if (definition.stats().size() < 2)
      throw new IllegalArgumentException("Coupled model requires multiple source stats");
    var result = new ArrayList<Cell>();
    for (int first = 0; first <= RESOLUTION; ) {
      var values = new LinkedHashMap<String, Long>();
      int last = RESOLUTION;
      for (var stat : definition.stats()) {
        long number = value(stat, first);
        values.put(stat.id(), number);
        int low = first, high = RESOLUTION;
        // HALF_UP interpolation is monotone, including negative ties and fixed stats.
        while (low < high) {
          int mid = low + (high - low + 1) / 2;
          if (value(stat, mid) == number) low = mid;
          else high = mid - 1;
        }
        last = Math.min(last, low);
      }
      result.add(
          new Cell(new ModifierInstance(definition.id(), Map.copyOf(values)), last - first + 1));
      first = last + 1;
    }
    return List.copyOf(result);
  }

  public static long value(ModifierDefinition.StatRange stat, int tick) {
    if (tick < 0 || tick > RESOLUTION)
      throw new IllegalArgumentException("Ratio tick outside model domain");
    var numerator =
        BigInteger.valueOf(stat.min())
            .multiply(BigInteger.valueOf(RESOLUTION - tick))
            .add(BigInteger.valueOf(stat.max()).multiply(BigInteger.valueOf(tick)));
    return new BigDecimal(numerator)
        .divide(BigDecimal.valueOf(RESOLUTION), 0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
