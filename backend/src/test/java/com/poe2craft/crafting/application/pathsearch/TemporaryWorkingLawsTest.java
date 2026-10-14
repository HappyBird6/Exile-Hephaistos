package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemporaryWorkingLawsTest {
  @Test
  void diskRowsPreserveOrderUpdatesRemovalAndBoundedCache(@TempDir Path directory)
      throws Exception {
    var expected = new LinkedHashMap<Integer, CertifiedInterval>();
    try (var store = new TemporaryWorkingLaws(directory, 64, 7)) {
      var actual = store.boundary();
      var random = new Random(741);
      for (int n = 0; n < 4000; n++) {
        int key = random.nextInt(900);
        if (n % 4 == 0) assertThat(actual.remove(key)).isEqualTo(expected.remove(key));
        else {
          var value =
              CertifiedInterval.enclose(
                  new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(4000)), 64);
          assertThat(actual.put(key, value)).isEqualTo(expected.put(key, value));
        }
        assertThat(store.metrics().cachedRows()).isLessThanOrEqualTo(7);
      }
      assertThat(new ArrayList<>(actual.entrySet()))
          .isEqualTo(new ArrayList<>(expected.entrySet()));
      assertThat(actual.firstEntry()).isEqualTo(expected.firstEntry());
      assertThat(store.metrics().rows()).isEqualTo(expected.size());
      assertThat(store.residentEntries()).isLessThanOrEqualTo(8);
      assertThat(store.metrics().diskBytes()).isPositive();
      actual.clear();
      assertThat(actual).isEmpty();
      actual.put(42, CertifiedInterval.one(64));
      assertThat(actual.firstEntry().getKey()).isEqualTo(42);
      actual.close();
      assertThat(store.metrics().rows()).isZero();
    }
    try (var files = Files.list(directory)) {
      assertThat(files.toList()).isEmpty();
    }
  }

  @Test
  void diskWorkingLawsMatchIndependentFirstHitOracleAndMemoryRunner(@TempDir Path directory) {
    var precision = new CertifiedInterval.Precision(64, 2);
    var template =
        new Template<Integer, String>("ordered", List.of(), List.of(s -> "add", s -> "remove"));
    Transition<Integer, String> transition =
        (state, action) -> {
          if (action.equals("add"))
            return new Kernel<>(
                List.of(new Outcome<>(2, f(2, 7)), new Outcome<>(1, f(5, 7))),
                Fraction.ZERO,
                false);
          assertThat(state).isEqualTo(1);
          return new Kernel<>(List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
        };
    java.util.function.Function<Integer, Optional<Exit>> classify =
        s -> s == 2 ? Optional.of(Exit.GOAL) : Optional.empty();
    var memory = new CertifiedMethodRunner<>(0, template, transition, classify, precision);
    try (var store = new TemporaryWorkingLaws(directory, 64, 0)) {
      var disk =
          new CertifiedMethodRunner<>(
              0,
              template,
              transition,
              classify,
              precision,
              LinkedHashMap::new,
              new CertifiedMethodRunner.WorkingMaps<>(store::pending, store::boundary));
      Fraction remaining = Fraction.ONE, add = Fraction.ZERO, remove = Fraction.ZERO;
      for (int cycle = 1; cycle <= 500; cycle++) {
        add = add.add(remaining);
        remaining = remaining.multiply(f(5, 7));
        remove = remove.add(remaining);
        memory.beginCycle();
        disk.beginCycle();
        while (!memory.atBoundary()) {
          memory.step(1);
          disk.step(1);
        }
        assertThat(disk.atBoundary()).isTrue();
        if (List.of(100, 300, 500).contains(cycle)) {
          var point = disk.observation();
          assertThat(point).isEqualTo(memory.observation());
          assertThat(point.active().get(0).contains(remaining)).isTrue();
          assertThat(
                  point
                      .exits()
                      .get(Exit.GOAL)
                      .get(2)
                      .contains(
                          new Fraction(
                              remaining.denominator().subtract(remaining.numerator()),
                              remaining.denominator())))
              .isTrue();
          assertThat(point.cycleUses().get("add").contains(add)).isTrue();
          assertThat(point.cycleUses().get("remove").contains(remove)).isTrue();
        }
      }
      disk.closeWorkingMaps();
      assertThat(store.metrics().laws()).isZero();
    }
  }

  private static Fraction f(long n, long d) {
    return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
  }
}
