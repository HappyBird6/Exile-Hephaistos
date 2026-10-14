package com.poe2craft.crafting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.application.pathsearch.MethodGraphStore.*;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoredMethodExpressionsTest {
  @Test
  void repeatedDefinitionsReuseBoundedContentCacheWithoutDatabaseLookups(@TempDir Path dir) {
    try (var store = new SqliteMethodGraphStore(dir.resolve("cache.sqlite"), identity(), 64)) {
      var expressions = new StoredMethodExpressions(store, 16);
      var first =
          expressions.define(
              "one", 0, List.of(), List.of(WorkbenchCurrency.EXALTED), "full-policy");
      var before = store.metrics();
      for (int i = 0; i < 100; i++)
        assertThat(
                expressions
                    .define(
                        "method-" + i,
                        i,
                        List.of(),
                        List.of(WorkbenchCurrency.EXALTED),
                        "full-policy")
                    .program())
            .isEqualTo(first.program());
      var after = store.metrics();
      assertThat(after.operators()).isEqualTo(before.operators());
      assertThat(after.writes()).isEqualTo(before.writes());
      assertThat(after.reads() - before.reads())
          .isEqualTo(5); // Only the five metrics COUNT queries.
    }
  }

  private static final CertifiedInterval.Precision PRECISION =
      new CertifiedInterval.Precision(64, 2);

  private static Identity identity() {
    return new Identity(
        "data", "rules", "rolls", "templates", "goals", "dyadic-64", "season", "full-input");
  }

  private static Fraction f(long n, long d) {
    return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
  }

  @Test
  void requestsShareProgramsWithoutExecutingOrExpandingUnrequestedHorizons(@TempDir Path dir) {
    var path = dir.resolve("lazy.sqlite");
    try (var store = new SqliteMethodGraphStore(path, identity(), 64)) {
      var expressions = new StoredMethodExpressions(store, 2);
      var cycle = List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.ANNULMENT);
      var one = expressions.define("one", 0, List.of(), cycle, "goal:2;source-progress:empty");
      var two = expressions.define("two", 1, List.of(), cycle, "goal:2;source-progress:empty");
      var different =
          expressions.define("different", 0, List.of(), cycle, "goal:2;source-progress:life");
      assertThat(two.program()).isEqualTo(one.program());
      assertThat(different.program()).isNotEqualTo(one.program());
      long programs = store.metrics().operators();
      long revision =
          store.commit(
              0,
              new Batch(
                  List.of(
                      new Source(0, 0, true, "ilvl80;eligible-delete:2"),
                      new Source(1, 1, true, "ilvl81;eligible-delete:3")),
                  List.of(one, two, different),
                  List.of(),
                  List.of()));
      assertThat(store.metrics().observations()).isZero();
      revision = expressions.request(revision, "one", 100, PRECISION);
      revision = expressions.request(revision, "two", 100, PRECISION);
      assertThat(store.metrics().operators()).isEqualTo(programs + 1);
      assertThat(store.observation("one", 100).orElseThrow().operator())
          .isEqualTo(store.observation("two", 100).orElseThrow().operator());
      assertThat(store.observation("one", 300)).isEmpty();
      assertThat(store.observation("one", 500)).isEmpty();
      revision = expressions.request(revision, "one", 500, PRECISION);
      assertThat(store.metrics().operators()).isEqualTo(programs + 2);
      assertThat(store.metrics().ready()).isZero();
      long pinned = revision;
      assertThatThrownBy(
              () ->
                  expressions.request(pinned, "one", 100, new CertifiedInterval.Precision(128, 2)))
          .hasMessage("Observation precision or expression changed");
      assertThat(store.revision()).isEqualTo(revision);
    }
    try (var store = new SqliteMethodGraphStore(path, identity(), 64)) {
      assertThat(store.observation("one", 100).orElseThrow().status()).isEqualTo(Status.REQUESTED);
      assertThat(store.method("different").orElseThrow().source()).isZero();
      assertThat(store.observation("one", 300)).isEmpty();
    }
  }

  @Test
  void lazyEvaluationMatchesIndependentFirstHitAndConsumptionOracle(@TempDir Path dir) {
    try (var store = new SqliteMethodGraphStore(dir.resolve("oracle.sqlite"), identity(), 64)) {
      var expressions = new StoredMethodExpressions(store, 2);
      var method =
          expressions.define(
              "m",
              0,
              List.of(WorkbenchCurrency.EXALTED),
              List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.EXALTED),
              "goal=2;no-checkpoints");
      long revision =
          store.commit(
              0,
              new Batch(
                  List.of(new Source(0, 0, true, "state0")),
                  List.of(method),
                  List.of(),
                  List.of()));
      for (int cycles : List.of(100, 300, 500)) {
        revision = expressions.request(revision, "m", cycles, PRECISION);
        var calls = new AtomicInteger();
        var evaluation =
            expressions.evaluate(
                "m",
                cycles,
                0,
                (state, action) -> {
                  calls.incrementAndGet();
                  assertThat(state).isNotEqualTo(2); // Never delete after a primitive first hit.
                  if (action == WorkbenchCurrency.EXALTED) {
                    assertThat(state).isZero();
                    return new Kernel<>(
                        List.of(new Outcome<>(2, f(2, 7)), new Outcome<>(1, f(5, 7))),
                        Fraction.ZERO,
                        false);
                  }
                  assertThat(state).isEqualTo(1);
                  return new Kernel<>(
                      List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
                },
                policy -> {
                  assertThat(policy).isEqualTo("goal=2;no-checkpoints");
                  return state -> state == 2 ? Optional.of(Exit.GOAL) : Optional.empty();
                },
                PRECISION);
        assertThat(calls.get()).isZero();
        assertThatThrownBy(evaluation::result).hasMessage("Observation incomplete");
        int steps = 0;
        while (!evaluation.complete() && steps++ < 10000) evaluation.step();
        assertThat(evaluation.complete()).isTrue();
        var point = evaluation.result();
        Fraction remaining = f(5, 7), uses = Fraction.ZERO;
        for (int i = 0; i < cycles; i++) {
          uses = uses.add(remaining);
          remaining = remaining.multiply(f(5, 7));
        }
        var goal =
            new Fraction(
                remaining.denominator().subtract(remaining.numerator()), remaining.denominator());
        assertThat(point.cycles()).isEqualTo(cycles);
        assertThat(point.mass().contains(Fraction.ONE)).isTrue();
        assertThat(point.exits().get(Exit.GOAL).get(2).contains(goal)).isTrue();
        assertThat(point.active().get(1).contains(remaining)).isTrue();
        assertThat(point.initialUses().get(WorkbenchCurrency.EXALTED).contains(Fraction.ONE))
            .isTrue();
        assertThat(point.cycleUses().get(WorkbenchCurrency.ANNULMENT).contains(uses)).isTrue();
        assertThat(point.cycleUses().get(WorkbenchCurrency.EXALTED).contains(uses)).isTrue();
        assertThat(expressions.cachedNodes()).isLessThanOrEqualTo(2);
        // Evaluation cannot publish a READY result before its caller commits a validated summary.
        assertThat(store.metrics().ready()).isZero();
      }
    }
  }

  @Test
  void initialFirstHitSkipsAllCyclesAndUnsupportedMassIsRetained(@TempDir Path dir) {
    try (var store = new SqliteMethodGraphStore(dir.resolve("exits.sqlite"), identity(), 64)) {
      var expressions = new StoredMethodExpressions(store, 0);
      var method =
          expressions.define(
              "m",
              0,
              List.of(WorkbenchCurrency.EXALTED),
              List.of(WorkbenchCurrency.ANNULMENT),
              "goal=2");
      long revision =
          store.commit(
              0,
              new Batch(
                  List.of(new Source(0, 0, true, "state0")),
                  List.of(method),
                  List.of(),
                  List.of()));
      expressions.request(revision, "m", 100, PRECISION);
      var evaluation =
          expressions.evaluate(
              "m",
              100,
              0,
              (state, action) -> {
                assertThat(action).isEqualTo(WorkbenchCurrency.EXALTED);
                return new Kernel<>(List.of(new Outcome<>(2, f(2, 3))), f(1, 3), false);
              },
              policy -> state -> state == 2 ? Optional.of(Exit.GOAL) : Optional.empty(),
              PRECISION);
      for (int i = 0; i < 20 && !evaluation.complete(); i++) evaluation.step();
      assertThat(evaluation.complete()).isTrue();
      var result = evaluation.result();
      assertThat(result.cycles()).isEqualTo(100);
      assertThat(result.cycleUses()).isEmpty();
      assertThat(result.mass().contains(Fraction.ONE)).isTrue();
      assertThat(result.exits().get(Exit.UNRESOLVED).values()).hasSize(1);
      assertThat(result.exits().get(Exit.UNRESOLVED).values().iterator().next().contains(f(1, 3)))
          .isTrue();
      assertThat(expressions.cachedNodes()).isZero();
    }
  }
}
