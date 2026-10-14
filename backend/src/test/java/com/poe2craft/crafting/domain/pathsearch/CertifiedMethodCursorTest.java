package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CertifiedMethodCursorTest {
  private static final CertifiedInterval.Precision PRECISION =
      new CertifiedInterval.Precision(64, 2);

  private static Fraction f(long n, long d) {
    return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
  }

  private static final Template<Integer, String> TEMPLATE =
      new Template<>(
          "cursor-first-hit-v1", List.of(s -> "add"), List.of(s -> "remove", s -> "add"));
  private static final Function<Integer, Optional<Exit>> CLASSIFY =
      s -> s == 9 ? Optional.of(Exit.GOAL) : s == 8 ? Optional.of(Exit.LOSS) : Optional.empty();

  private static Transition<Integer, String> kernel(AtomicInteger calls) {
    return (state, action) -> {
      calls.incrementAndGet();
      assertThat(state).isNotIn(8, 9);
      if (action.equals("add"))
        return new Kernel<>(
            List.of(
                new Outcome<>(9, f(1, 7)), new Outcome<>(8, f(1, 7)),
                new Outcome<>(1, f(2, 7)), new Outcome<>(2, f(2, 7))),
            f(1, 7),
            false);
      return new Kernel<>(List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
    };
  }

  private static CertifiedMethodRunner<Integer, String> create(AtomicInteger calls) {
    return new CertifiedMethodRunner<>(0, TEMPLATE, kernel(calls), CLASSIFY, PRECISION);
  }

  private static void step(CertifiedMethodRunner<Integer, String> runner) {
    if (runner.atBoundary()) runner.beginCycle();
    else runner.step(1);
  }

  private static void runTo(CertifiedMethodRunner<Integer, String> runner, int cycles) {
    int guard = 0;
    while (!(runner.atBoundary() && runner.cycles() == cycles) && guard++ < 100000) step(runner);
    assertThat(runner.cycles()).isEqualTo(cycles);
  }

  @Test
  void everyInitialAndCycleYieldRestoresBitIdenticalMassAndCostsWithoutReplayingActions() {
    var originalCalls = new AtomicInteger();
    var original = create(originalCalls);
    // Includes initial setup, partially consumed primitive kernels, phase changes and boundaries.
    for (int cut = 0; cut < 36; cut++) {
      var restoredCalls = new AtomicInteger();
      CertifiedMethodRunner<Integer, String> resumed;
      try (var lease = original.leaseCursor()) {
        int count = originalCalls.get();
        var restore =
            CertifiedMethodRunner.restoreCursor(
                lease.header(),
                TEMPLATE,
                kernel(restoredCalls),
                CLASSIFY,
                PRECISION,
                LinkedHashMap::new);
        var rows = lease.rows();
        while (rows.hasNext()) restore.accept(rows.next());
        resumed = restore.finish();
        assertThat(restoredCalls.get()).isZero();
        assertThat(originalCalls.get()).isEqualTo(count);
      }
      // Compare independently continued original and restored cursors, not only scalar success.
      var futureCalls = new AtomicInteger();
      CertifiedMethodRunner<Integer, String> future;
      try (var lease = original.leaseCursor()) {
        var restore =
            CertifiedMethodRunner.restoreCursor(
                lease.header(),
                TEMPLATE,
                kernel(futureCalls),
                CLASSIFY,
                PRECISION,
                LinkedHashMap::new);
        lease.rows().forEachRemaining(restore::accept);
        future = restore.finish();
      }
      runTo(resumed, 100);
      runTo(future, 100);
      assertThat(resumed.observation()).isEqualTo(future.observation());
      assertThat(restoredCalls.get()).isEqualTo(futureCalls.get());
      var expectedCalls = new AtomicInteger();
      var expected = create(expectedCalls);
      runTo(expected, 100);
      assertThat(resumed.observation()).isEqualTo(expected.observation());
      assertThat(originalCalls.get() + restoredCalls.get()).isEqualTo(expectedCalls.get());
      assertThat(resumed.observation().mass().contains(Fraction.ONE)).isTrue();
      Fraction remaining = Fraction.ONE, terminal = Fraction.ZERO, cycleUses = Fraction.ZERO;
      for (int cycle = 0; cycle <= 100; cycle++) {
        if (cycle > 0) cycleUses = cycleUses.add(remaining);
        terminal = terminal.add(remaining.multiply(f(1, 7)));
        remaining = remaining.multiply(f(4, 7));
      }
      var point = resumed.observation();
      assertThat(point.exits().get(Exit.GOAL).get(9).contains(terminal)).isTrue();
      assertThat(point.exits().get(Exit.LOSS).get(8).contains(terminal)).isTrue();
      assertThat(point.exits().get(Exit.UNRESOLVED).get(0).contains(terminal)).isTrue();
      assertThat(point.cycleUses().get("add").contains(cycleUses)).isTrue();
      assertThat(point.cycleUses().get("remove").contains(cycleUses)).isTrue();
      step(original);
    }
  }

  @Test
  void restoredInitialSuccessAndLossRemainDisjointAndDoNotConsumeAnotherCurrency() {
    var calls = new AtomicInteger();
    var runner =
        new CertifiedMethodRunner<Integer, String>(
            0,
            TEMPLATE,
            (s, a) ->
                new Kernel<>(
                    List.of(new Outcome<>(9, f(1, 2)), new Outcome<>(8, f(1, 2))),
                    Fraction.ZERO,
                    false),
            CLASSIFY,
            PRECISION);
    runner.step(1); // Half of the initial action has been consumed, with its cost already charged.
    try (var lease = runner.leaseCursor()) {
      var restore =
          CertifiedMethodRunner.restoreCursor(
              lease.header(), TEMPLATE, kernel(calls), CLASSIFY, PRECISION, LinkedHashMap::new);
      lease.rows().forEachRemaining(restore::accept);
      var resumed = restore.finish();
      resumed.step(1);
      assertThat(resumed.absorbed()).isTrue();
      assertThat(calls.get()).isZero();
      var point = resumed.observation().atLaterAbsorbedBoundary(500);
      assertThat(point.exits().get(Exit.GOAL).get(9).contains(f(1, 2))).isTrue();
      assertThat(point.exits().get(Exit.LOSS).get(8).contains(f(1, 2))).isTrue();
      assertThat(point.initialUses().get("add")).isEqualTo(CertifiedInterval.one(64));
      assertThat(point.cycleUses()).isEmpty();
    }
  }

  @Test
  void leasesBlockMutationAndRejectTruncatedDuplicatedOrIncompatibleCursors() {
    var runner = create(new AtomicInteger());
    var lease = runner.leaseCursor();
    assertThatThrownBy(() -> runner.step(1)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(runner::leaseCursor).isInstanceOf(IllegalStateException.class);
    var rows = lease.rows();
    var first = rows.next();
    var restore =
        CertifiedMethodRunner.restoreCursor(
            lease.header(),
            TEMPLATE,
            kernel(new AtomicInteger()),
            CLASSIFY,
            PRECISION,
            LinkedHashMap::new);
    assertThatThrownBy(restore::finish).isInstanceOf(IllegalArgumentException.class);
    restore.accept(first);
    assertThatThrownBy(() -> restore.accept(first)).isInstanceOf(IllegalArgumentException.class);
    var wrong =
        new Template<Integer, String>(
            "different-definition", List.of(s -> "add"), List.of(s -> "remove", s -> "add"));
    assertThatThrownBy(
            () ->
                CertifiedMethodRunner.restoreCursor(
                    lease.header(),
                    wrong,
                    kernel(new AtomicInteger()),
                    CLASSIFY,
                    PRECISION,
                    LinkedHashMap::new))
        .hasMessage("Incompatible method cursor");
    assertThatThrownBy(
            () ->
                CertifiedMethodRunner.restoreCursor(
                    lease.header(),
                    TEMPLATE,
                    kernel(new AtomicInteger()),
                    CLASSIFY,
                    new CertifiedInterval.Precision(128, 2),
                    LinkedHashMap::new))
        .hasMessage("Incompatible method cursor");
    lease.close();
    assertThatThrownBy(rows::hasNext).hasMessage("Cursor lease closed");
    runner.step(1);
    try (var partial = runner.leaseCursor()) {
      var incomplete =
          CertifiedMethodRunner.restoreCursor(
              partial.header(),
              TEMPLATE,
              kernel(new AtomicInteger()),
              CLASSIFY,
              PRECISION,
              LinkedHashMap::new);
      partial
          .rows()
          .forEachRemaining(
              row -> {
                if (row.kind() != CertifiedMethodRunner.CursorKind.KERNEL) incomplete.accept(row);
              });
      assertThatThrownBy(incomplete::finish).hasMessage("Invalid flight cursor");
    }
  }
}
