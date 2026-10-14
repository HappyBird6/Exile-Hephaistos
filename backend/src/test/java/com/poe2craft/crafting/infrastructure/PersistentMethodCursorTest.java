package com.poe2craft.crafting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.application.pathsearch.MethodGraphStore.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentMethodCursorTest {
  private static final CertifiedInterval.Precision PRECISION =
      CertifiedInterval.Precision.standard();
  private static final Template<Integer, String> TEMPLATE =
      new Template<>("persisted-first-hit-v1", List.of(), List.of(s -> "add", s -> "remove"));
  private static final Function<Integer, Optional<Exit>> CLASSIFY =
      s -> s == 999 ? Optional.of(Exit.GOAL) : s == 998 ? Optional.of(Exit.LOSS) : Optional.empty();
  private static final Batch EMPTY = new Batch(List.of(), List.of(), List.of(), List.of());

  private static Fraction f(long n, long d) {
    return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
  }

  private static Identity identity() {
    return new Identity(
        "data", "rules", "roll", "templates", "goal", "dyadic64", "season", "original-full-input");
  }

  private static CertifiedMethodRunner<Integer, String> runner() {
    return new CertifiedMethodRunner<>(
        0,
        TEMPLATE,
        (state, action) -> {
          if (action.equals("remove"))
            return new Kernel<>(List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
          var outcomes = new ArrayList<Outcome<Integer>>();
          outcomes.add(new Outcome<>(999, f(1, 601)));
          outcomes.add(new Outcome<>(998, f(1, 601)));
          for (int i = 1; i <= 598; i++) outcomes.add(new Outcome<>(i, f(1, 601)));
          return new Kernel<>(outcomes, f(1, 601), false);
        },
        CLASSIFY,
        PRECISION);
  }

  @Test
  void persistedPartialPrimitiveResumesAfterReopenWithExactCostsAndExitMass(@TempDir Path dir) {
    var path = dir.resolve("cursor.sqlite");
    var original = runner();
    original.beginCycle();
    original.step(1);
    try (var store = new SqliteMethodGraphStore(path, identity(), 64)) {
      var codec =
          new PersistentMethodCursor<>(store, new ObjectMapper(), Integer.class, String.class);
      var changes =
          new Batch(
              List.of(new Source(0, 0, false, "full-state-zero")),
              List.of(new Method("m", 0, "program")),
              List.of(
                  new MethodGraphStore.Observation("m", 100, Status.PARTIAL, null, "not-ready")),
              List.of(new Checkpoint("job/generation", "7")));
      try (var writer = codec.write("m/100", original, 0, changes)) {
        writer.step(7);
        assertThat(writer.complete()).isFalse();
        assertThat(store.execution("m/100")).isEmpty();
        assertThat(store.source(0)).isEmpty();
        assertThat(store.metrics().executionRows()).isEqualTo(7);
        assertThatThrownBy(() -> original.step(1)).isInstanceOf(IllegalStateException.class);
        while (!writer.complete()) writer.step(7);
        assertThat(writer.revision()).isEqualTo(1);
      }
      assertThat(store.execution("m/100").orElseThrow().rows()).isEqualTo(603);
      assertThat(store.checkpoint("job/generation").orElseThrow().content()).isEqualTo("7");
      assertThat(store.observation("m", 100).orElseThrow().status()).isEqualTo(Status.PARTIAL);
      assertThat(store.metrics().ready()).isZero();
      // An abandoned next-generation write must not replace the published checkpoint.
      var abandoned = store.beginExecution("m/100", "uncommitted next header");
      store.appendExecution(abandoned.id(), 0, List.of("uncommitted row"));
    }
    try (var store = new SqliteMethodGraphStore(path, identity(), 64)) {
      var codec =
          new PersistentMethodCursor<>(store, new ObjectMapper(), Integer.class, String.class);
      var calls = new AtomicInteger();
      var reader =
          codec.read(
              "m/100",
              TEMPLATE,
              (state, action) -> {
                assertThat(action).isEqualTo("remove"); // The saved add action must not be rerun.
                assertThat(state).isNotIn(998, 999);
                calls.incrementAndGet();
                return new Kernel<>(List.of(new Outcome<>(0, Fraction.ONE)), Fraction.ZERO, false);
              },
              CLASSIFY,
              PRECISION,
              LinkedHashMap::new);
      reader.step(17);
      assertThat(reader.complete()).isFalse();
      assertThatThrownBy(reader::result).hasMessage("Cursor restoration incomplete");
      while (!reader.complete()) reader.step(17);
      assertThat(calls.get()).isZero();
      var restored = reader.result();
      while (!restored.atBoundary()) restored.step(1);
      while (!original.atBoundary()) original.step(1);
      var point = restored.observation();
      assertThat(point).isEqualTo(original.observation());
      assertThat(calls.get()).isEqualTo(598);
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
      assertThat(point.exits().get(Exit.GOAL).get(999).contains(f(1, 601))).isTrue();
      assertThat(point.exits().get(Exit.LOSS).get(998).contains(f(1, 601))).isTrue();
      assertThat(point.exits().get(Exit.UNRESOLVED).get(0).contains(f(1, 601))).isTrue();
      assertThat(point.active().get(0).contains(f(598, 601))).isTrue();
      assertThat(point.cycleUses().get("add")).isEqualTo(CertifiedInterval.one(64));
      assertThat(point.cycleUses().get("remove").contains(f(598, 601))).isTrue();
    }
  }

  @Test
  void staleOrCancelledWriterCannotReplaceCommittedCursorAndOldRowsCanBeReclaimed(
      @TempDir Path dir) {
    try (var store = new SqliteMethodGraphStore(dir.resolve("generation.sqlite"), identity(), 64)) {
      var codec =
          new PersistentMethodCursor<>(store, new ObjectMapper(), Integer.class, String.class);
      var runner = runner();
      try (var writer = codec.write("m/100", runner, 0, EMPTY)) {
        writer.step(256);
        assertThat(writer.complete()).isTrue();
      }
      var old = store.execution("m/100").orElseThrow();
      assertThatThrownBy(() -> store.discardExecution(old.id()))
          .hasMessage("EXECUTION_CURSOR_REFERENCED");
      runner.beginCycle();
      runner.step(1);
      try (var writer = codec.write("m/100", runner, 1, EMPTY)) {
        writer.step(2);
        assertThat(store.metrics().executionCursors()).isEqualTo(2);
        store.commit(
            1,
            new Batch(
                List.of(),
                List.of(),
                List.of(),
                List.of(new Checkpoint("generation", "cancelled"))));
        assertThatThrownBy(
                () -> {
                  while (!writer.complete()) writer.step(256);
                })
            .hasMessage("STALE_GRAPH_REVISION");
        assertThatThrownBy(() -> writer.step(1)).hasMessage("Cursor writer closed");
      }
      assertThat(store.execution("m/100").orElseThrow()).isEqualTo(old);
      assertThat(store.metrics().executionCursors()).isEqualTo(1);
      assertThat(store.revision()).isEqualTo(2);
      try (var cancelled = codec.write("m/100", runner, 2, EMPTY)) {
        cancelled.step(2);
      }
      assertThat(store.metrics().executionCursors()).isEqualTo(1);
      try (var next = codec.write("m/100", runner, 2, EMPTY)) {
        while (!next.complete()) next.step(256);
        next.discardPrevious();
        assertThat(next.revision()).isEqualTo(3);
      }
      assertThat(store.execution("m/100").orElseThrow().id()).isNotEqualTo(old.id());
      assertThat(store.metrics().executionCursors()).isEqualTo(1);
      assertThat(store.metrics().executionRows()).isEqualTo(603);
      var finished = store.execution("m/100").orElseThrow();
      store.commit(
          3,
          new Batch(List.of(), List.of(), List.of(), List.of(MethodCursorStore.finished("m/100"))));
      store.discardExecution(finished.id());
      assertThat(store.execution("m/100")).isEmpty();
      assertThat(store.metrics().executionCursors()).isZero();
      assertThat(store.metrics().executionRows()).isZero();
    }
  }

  @Test
  void unsealedAndWrongScopeReferencesAreRejectedWithoutPublishingOtherChanges(@TempDir Path dir) {
    try (var store = new SqliteMethodGraphStore(dir.resolve("references.sqlite"), identity(), 64)) {
      var draft = store.beginExecution("one", "header");
      store.appendExecution(draft.id(), 0, List.of("a", "b"));
      assertThatThrownBy(() -> store.appendExecution(draft.id(), 0, List.of("c")))
          .hasMessage("STALE_EXECUTION_CURSOR");
      assertThatThrownBy(() -> store.executionRows(draft.id(), 0, 10))
          .hasMessage("Execution cursor is not sealed");
      assertThatThrownBy(
              () ->
                  store.commit(
                      0,
                      new Batch(
                          List.of(new Source(0, 0, false, "state")),
                          List.of(),
                          List.of(),
                          List.of(new Checkpoint("execution/one", draft.id())))))
          .hasMessage("Invalid execution checkpoint reference");
      assertThat(store.source(0)).isEmpty();
      var sealed = store.sealExecution(draft.id(), 2);
      assertThatThrownBy(
              () ->
                  store.commit(
                      0,
                      new Batch(
                          List.of(),
                          List.of(),
                          List.of(),
                          List.of(new Checkpoint("execution/two", draft.id())))))
          .hasMessage("Invalid execution checkpoint reference");
      store.commit(
          0,
          new Batch(List.of(), List.of(), List.of(), List.of(MethodCursorStore.reference(sealed))));
      var first = store.executionRows(draft.id(), 0, 1);
      assertThat(first.complete()).isFalse();
      assertThat(first.rows()).containsExactly(new MethodCursorStore.ExecutionRow(0, "a"));
      var second = store.executionRows(draft.id(), 1, 1);
      assertThat(second.complete()).isTrue();
      assertThat(second.rows()).containsExactly(new MethodCursorStore.ExecutionRow(1, "b"));
    }
  }
}
