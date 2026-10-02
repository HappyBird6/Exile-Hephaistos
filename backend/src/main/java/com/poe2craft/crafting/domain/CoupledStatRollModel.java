package com.poe2craft.crafting.domain;

import com.poe2craft.item.ModifierDefinition;
import com.poe2craft.item.ModifierInstance;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.random.RandomGenerator;

/** User-approved conjecture, not a verified game distribution. See ISSUES.md WB-001. */
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
    assumptions.add(
        new WorkbenchSimulator.Assumption(
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
            tick));
    return new ModifierInstance(definition.id(), values);
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
