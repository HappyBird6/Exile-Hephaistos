package com.poe2craft.crafting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.MethodGraphStore.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMethodGraphStoreTest {
  public static class CrashWriter {
    public static void main(String[] args) {
      var store = new SqliteMethodGraphStore(Path.of(args[0]), identity("crash-input"), 64);
      store.commit(
          0,
          new Batch(
              List.of(new Source(0, 0, false, "full-state")),
              List.of(new Method("m", 0, "full-program")),
              List.of(new Observation("m", 100, Status.PARTIAL, null, "unresolved:1")),
              List.of(new Checkpoint("resume", "next-candidate:17"))));
      Runtime.getRuntime().halt(0);
    }
  }

  private static Identity identity(String input) {
    return new Identity(
        "data-1", "rules-1", "roll-1", "templates-1", "goal-1", "dyadic-64", "season-1", input);
  }

  @Test
  void committedCheckpointSurvivesProcessExitWithoutClose(@TempDir Path dir) throws Exception {
    var locations = new LinkedHashSet<String>();
    for (Class<?> type :
        List.of(
            CrashWriter.class,
            SqliteMethodGraphStore.class,
            Class.forName("org.sqlite.JDBC"),
            com.fasterxml.jackson.databind.ObjectMapper.class,
            com.fasterxml.jackson.core.JsonFactory.class,
            com.fasterxml.jackson.annotation.JsonInclude.class,
            org.slf4j.LoggerFactory.class))
      locations.add(
          Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
    var path = dir.resolve("crash.sqlite");
    var process =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                String.join(java.io.File.pathSeparator, locations),
                CrashWriter.class.getName(),
                path.toString())
            .redirectErrorStream(true)
            .redirectOutput(dir.resolve("child.log").toFile())
            .start();
    try {
      assertThat(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .withFailMessage(java.nio.file.Files.readString(dir.resolve("child.log")))
          .isZero();
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
    try (var store = new SqliteMethodGraphStore(path, identity("crash-input"), 64)) {
      assertThat(store.revision()).isEqualTo(1);
      assertThat(store.checkpoint("resume").orElseThrow().content()).isEqualTo("next-candidate:17");
      assertThat(store.observation("m", 100).orElseThrow().status()).isEqualTo(Status.PARTIAL);
      assertThat(store.metrics().ready()).isZero();
      assertThat(store.observation("m", 300)).isEmpty();
    }
  }

  @Test
  void durableCheckpointSeparatesUnrequestedPartialAndReadyObservations(@TempDir Path dir) {
    var path = dir.resolve("graph.sqlite");
    var identity = identity("{\"AND\":[\"unknown-original-stat\",\"explicit-spirit>=47\"]}");
    long revision;
    try (var store = new SqliteMethodGraphStore(path, identity, 64)) {
      var method =
          new Method("root:exalt-chaos", 0, "initial:EXALTED;cycle:CHAOS;first-hit:goal-1");
      revision =
          store.commit(
              0,
              new Batch(
                  List.of(new Source(0, 0, false, "full-item-with-rolls")),
                  List.of(method),
                  List.of(new Observation(method.id(), 100, Status.REQUESTED, null, null)),
                  List.of(new Checkpoint("discovery", "{\"source\":0,\"nextAction\":1}"))));
      assertThat(store.observation(method.id(), 300)).isEmpty();
      assertThat(store.observation(method.id(), 500)).isEmpty();
      long operator =
          store.internOperator("shared-expression", "exact conditional operator with provenance");
      revision =
          store.commit(
              revision,
              new Batch(
                  List.of(),
                  List.of(),
                  List.of(
                      new Observation(
                          method.id(), 100, Status.PARTIAL, operator, "unresolved:[0,1]")),
                  List.of(
                      new Checkpoint("evaluation", "{\"cycle\":7,\"firstHitAbsorbed\":true}"))));
      assertThat(store.metrics().ready()).isZero();
      revision =
          store.commit(
              revision,
              new Batch(
                  List.of(new Source(0, 0, true, "full-item-with-rolls")),
                  List.of(),
                  List.of(
                      new Observation(
                          method.id(),
                          100,
                          Status.READY,
                          operator,
                          "goal:[1/3,1/3];loss:[2/3,2/3]")),
                  List.of(new Checkpoint("evaluation", "{\"cycle\":100,\"complete\":true}"))));
      assertThat(store.metrics().ready()).isEqualTo(1);
    }
    try (var reopened = new SqliteMethodGraphStore(path, identity, 64)) {
      assertThat(reopened.revision()).isEqualTo(revision);
      assertThat(reopened.source(0).orElseThrow().discovered()).isTrue();
      assertThat(reopened.checkpoint("evaluation").orElseThrow().content()).contains("100");
      assertThat(reopened.observation("root:exalt-chaos", 100).orElseThrow().status())
          .isEqualTo(Status.READY);
      assertThat(reopened.observation("root:exalt-chaos", 300)).isEmpty();
      assertThat(
              reopened.operator(
                  reopened.observation("root:exalt-chaos", 100).orElseThrow().operator()))
          .isEqualTo("exact conditional operator with provenance");
    }
    assertThatThrownBy(() -> new SqliteMethodGraphStore(path, identity("different full input"), 64))
        .hasRootCauseMessage("INCOMPATIBLE_GRAPH_STORE");
    var changed =
        new Identity(
            identity.data(),
            identity.rules(),
            "roll-2",
            identity.templates(),
            identity.goals(),
            identity.arithmetic(),
            identity.season(),
            identity.originalInput());
    assertThatThrownBy(() -> new SqliteMethodGraphStore(path, changed, 64))
        .hasRootCauseMessage("INCOMPATIBLE_GRAPH_STORE");
  }

  @Test
  void collisionsAndFailedWritesCannotMergeSourcesOrAdvanceCheckpoints(@TempDir Path dir) {
    try (var store =
        new SqliteMethodGraphStore(dir.resolve("graph.sqlite"), identity("input"), 64)) {
      long one = store.internOperator("same-hash", "source-ilvl:80;delete-candidates:2");
      long two = store.internOperator("same-hash", "source-ilvl:81;delete-candidates:3");
      assertThat(two).isNotEqualTo(one);
      assertThat(store.internOperator("same-hash", "source-ilvl:80;delete-candidates:2"))
          .isEqualTo(one);
      long revision =
          store.commit(
              0,
              new Batch(
                  List.of(new Source(0, 2, false, "state-0")),
                  List.of(new Method("m", 0, "program-1")),
                  List.of(new Observation("m", 100, Status.READY, one, "mass:1")),
                  List.of(new Checkpoint("cursor", "old"))));
      assertThatThrownBy(
              () ->
                  store.commit(
                      revision,
                      new Batch(
                          List.of(new Source(1, 1, false, "state-1")),
                          List.of(new Method("m", 0, "different-program")),
                          List.of(),
                          List.of(new Checkpoint("cursor", "new")))))
          .hasMessage("METHOD_IDENTITY_CONFLICT");
      assertThat(store.source(1)).isEmpty();
      assertThat(store.revision()).isEqualTo(revision);
      assertThat(store.checkpoint("cursor").orElseThrow().content()).isEqualTo("old");
      assertThatThrownBy(
              () ->
                  store.commit(
                      revision,
                      new Batch(
                          List.of(new Source(0, 0, true, "different-state")),
                          List.of(),
                          List.of(),
                          List.of())))
          .hasMessage("SOURCE_IDENTITY_CONFLICT");
      assertThatThrownBy(
              () ->
                  store.commit(
                      revision,
                      new Batch(
                          List.of(),
                          List.of(),
                          List.of(new Observation("m", 100, Status.PARTIAL, one, "different mass")),
                          List.of())))
          .hasMessage("TERMINAL_OBSERVATION_CONFLICT");
      assertThat(store.observation("m", 100).orElseThrow().summary()).isEqualTo("mass:1");
      assertThatThrownBy(
              () -> store.commit(0, new Batch(List.of(), List.of(), List.of(), List.of())))
          .hasMessage("STALE_GRAPH_REVISION");
    }
  }

  @Test
  void boundedDepthPagesKeepAllAlternativesAndRejectStaleDepthOrdering(@TempDir Path dir) {
    try (var store =
        new SqliteMethodGraphStore(dir.resolve("large.sqlite"), identity("input"), 64)) {
      long revision = 0;
      for (int base = 0; base < 2048; base += 32) {
        var sources = new ArrayList<Source>();
        var methods = new ArrayList<Method>();
        for (int id = base; id < base + 32; id++) {
          sources.add(new Source(id, id % 5, true, "exact-state-" + id));
          for (int choice = 0; choice < 8; choice++)
            methods.add(new Method(id + ":" + choice, id, "alternative-" + choice));
        }
        revision = store.commit(revision, new Batch(sources, methods, List.of(), List.of()));
      }
      var seen = new HashSet<String>();
      Cursor cursor = null;
      int depth = -1;
      do {
        var page = store.methods(revision, cursor, 37);
        assertThat(page.methods()).hasSizeLessThanOrEqualTo(37);
        for (var row : page.methods()) {
          assertThat(row.depth()).isGreaterThanOrEqualTo(depth);
          depth = row.depth();
          assertThat(seen.add(row.method().id())).isTrue();
        }
        cursor = page.next();
      } while (cursor != null);
      assertThat(seen).hasSize(16384);
      assertThat(store.metrics().methods()).isEqualTo(16384);
      assertThat(store.metrics().cacheKiB()).isEqualTo(64);
      assertThat(store.metrics().diskBytes()).isPositive();
      long old = revision;
      revision =
          store.commit(
              revision,
              new Batch(
                  List.of(new Source(4, 0, true, "exact-state-4")),
                  List.of(),
                  List.of(),
                  List.of()));
      assertThatThrownBy(() -> store.methods(old, null, 37)).hasMessage("STALE_GRAPH_REVISION");
      assertThat(store.source(4).orElseThrow().depth()).isZero();
      assertThat(store.methods(revision, null, 16).methods())
          .filteredOn(row -> row.method().source() == 4)
          .hasSize(8)
          .allMatch(row -> row.depth() == 0);
    }
  }
}
