package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedMethodRunnerTest {
  static Fraction f(long a, long b) {
    return new Fraction(BigInteger.valueOf(a), BigInteger.valueOf(b));
  }

  @Test
  void integerOutwardRoundingAndFiveHundredFirstHitCyclesContainExactOracle() {
    var random = new Random(137);
    for (int bits : List.of(16, 32, 64, 128, 256)) {
      for (int i = 0; i < 2000; i++) {
        var x = f(random.nextInt(1000), 1000 + random.nextInt(1000));
        var y = f(random.nextInt(1000), 1000 + random.nextInt(1000));
        var ix = CertifiedInterval.enclose(x, bits);
        var iy = CertifiedInterval.enclose(y, bits);
        assertThat(ix.contains(x)).isTrue();
        assertThat(ix.add(iy).contains(x.add(y))).isTrue();
        assertThat(ix.multiply(iy).contains(x.multiply(y))).isTrue();
      }
      var runner = runner(bits, List.of(), List.of("add", "remove"));
      Fraction remaining = Fraction.ONE, add = Fraction.ZERO, remove = Fraction.ZERO;
      for (int cycle = 1; cycle <= 500; cycle++) {
        add = add.add(remaining);
        remaining = remaining.multiply(f(5, 7));
        remove = remove.add(remaining);
        runner.beginCycle();
        while (!runner.atBoundary()) runner.step(1);
        var point = runner.observation();
        var goal =
            new Fraction(
                remaining.denominator().subtract(remaining.numerator()), remaining.denominator());
        assertThat(point.cycles()).isEqualTo(cycle);
        assertThat(point.exits().get(Exit.GOAL).get(2).contains(goal)).isTrue();
        assertThat(point.active().get(0).contains(remaining)).isTrue();
        assertThat(point.mass().contains(Fraction.ONE)).isTrue();
        assertThat(point.cycleUses().get("add").contains(add)).isTrue();
        assertThat(point.cycleUses().get("remove").contains(remove)).isTrue();
      }
    }
  }

  private static CertifiedMethodRunner<Integer, String> runner(
      int bits, List<String> initial, List<String> cycle) {
    return new CertifiedMethodRunner<>(
        0,
        new Template<>(
            "first-hit",
            initial.stream()
                .<java.util.function.Function<Integer, String>>map(a -> s -> a)
                .toList(),
            cycle.stream().<java.util.function.Function<Integer, String>>map(a -> s -> a).toList()),
        (s, a) -> {
          if (a.equals("add")) {
            assertThat(s).isZero();
            return new Kernel<>(
                List.of(new Outcome<>(2, f(2, 7)), new Outcome<>(1, f(5, 7))),
                Fraction.ZERO,
                false);
          }
          assertThat(s).isEqualTo(1); // No removal is allowed after goal first hit.
          return new Kernel<>(List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
        },
        s -> s == 2 ? Optional.of(Exit.GOAL) : Optional.empty(),
        new CertifiedInterval.Precision(bits, 2));
  }

  @Test
  void preparationUnknownAndUnavailableRemainSeparateFromRoundingError() {
    var prepared = runner(64, List.of("add"), List.of("remove", "add"));
    while (!prepared.atBoundary()) prepared.step();
    var initial = prepared.observation();
    assertThat(initial.cycles()).isZero();
    assertThat(initial.initialUses().get("add").contains(Fraction.ONE)).isTrue();
    assertThat(initial.exits().get(Exit.GOAL).get(2).contains(f(2, 7))).isTrue();
    prepared.beginCycle();
    while (!prepared.atBoundary()) prepared.step();
    assertThat(prepared.observation().cycleUses().get("remove").contains(f(5, 7))).isTrue();
    assertThat(prepared.observation().cycleUses().get("add").contains(f(5, 7))).isTrue();
    assertThat(prepared.observation().exits().get(Exit.GOAL).get(2).contains(f(24, 49))).isTrue();
    assertThat(initial.cycles()).isZero();
    var unknown =
        new CertifiedMethodRunner<Integer, String>(
            0,
            new Template<>("unknown", List.of(), List.of(s -> "act")),
            (s, a) -> new Kernel<>(List.of(new Outcome<>(0, f(2, 3))), f(1, 3), false),
            s -> Optional.empty(),
            CertifiedInterval.Precision.standard());
    unknown.beginCycle();
    while (!unknown.atBoundary()) unknown.step();
    assertThat(unknown.observation().exits().get(Exit.GOAL)).isEmpty();
    assertThat(unknown.observation().exits().get(Exit.UNRESOLVED).get(0).contains(f(1, 3)))
        .isTrue();
    var dead =
        new CertifiedMethodRunner<Integer, String>(
            0,
            new Template<>("dead", List.of(), List.of(s -> "act")),
            (s, a) -> new Kernel<>(List.of(), Fraction.ZERO, true),
            s -> Optional.empty(),
            CertifiedInterval.Precision.standard());
    dead.beginCycle();
    while (!dead.atBoundary()) dead.step();
    assertThat(dead.observation().cycleUses()).isEmpty();
    assertThat(dead.observation().exits().get(Exit.DEAD).get(0).contains(Fraction.ONE)).isTrue();
    assertThat(dead.observation().atLaterAbsorbedBoundary(500).cycleUses()).isEmpty();
  }

  @Test
  void displayAndRankingRequireProvenSeparationAndClosedEnumeration() {
    assertThat(CertifiedInterval.enclose(f(101, 20000), 64).confirmedPercent(2)).isEmpty();
    assertThat(CertifiedInterval.enclose(f(1, 3), 64).confirmedPercent(2)).contains("33.33");
    for (int bits : List.of(16, 64)) {
      var zero = CertifiedInterval.zero(bits);
      var choices =
          List.of(
              new CertifiedRecommendations.Choice(
                  "a", CertifiedInterval.enclose(f(1, 3), bits), zero),
              new CertifiedRecommendations.Choice(
                  "b", CertifiedInterval.enclose(f(33333334, 100000000), bits), zero));
      assertThat(CertifiedRecommendations.local(choices, false).status())
          .isEqualTo("PENDING_ENUMERATION");
      var result = CertifiedRecommendations.local(choices, true);
      assertThat(result.status()).isEqualTo(bits == 16 ? "PENDING_ARITHMETIC" : "CERTIFIED");
      assertThat(result.methodIds()).isEqualTo(bits == 16 ? List.of() : List.of("b"));
      assertThat(
              CertifiedRecommendations.local(
                      List.of(
                          choices.getFirst(),
                          new CertifiedRecommendations.Choice(
                              "unknown", zero, CertifiedInterval.one(bits))),
                      true)
                  .status())
          .isEqualTo("PENDING_MODEL");
    }
    assertThatThrownBy(() -> new CertifiedInterval.Precision(15, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CertifiedInterval.one(64).add(CertifiedInterval.one(32)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
