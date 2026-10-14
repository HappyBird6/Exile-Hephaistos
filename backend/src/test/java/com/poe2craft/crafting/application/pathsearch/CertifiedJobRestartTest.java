package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.LocalCertifiedJobRepository;
import com.poe2craft.item.ItemState;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertifiedJobRestartTest {
  private CertifiedMethodSearchEngine engine(
      PathSearchServiceTest fixture, CertifiedMethodProtocol.Create request) {
    var start = request.request().start();
    var goal = request.request().goal();
    var evaluator = fixture.goals.compileEvaluation(start.item(), goal);
    var primitive = BasicCurrencyTransitions.forMethodSearch(fixture.catalog, fixture.identity);
    return new CertifiedMethodSearchEngine(
        start,
        new BasicTransitionClasses(
            fixture.catalog, primitive, fixture.goals.rollPartition(start.item(), goal), evaluator),
        evaluator,
        1000000,
        request.precision());
  }

  private CertifiedMethodSearchEngine.MethodView continuation(CertifiedMethodSearchEngine engine) {
    for (int step = 0; step < 500000; step++) {
      if (step % 128 != 0) {
        engine.step();
        continue;
      }
      var method =
          engine.methods().stream()
              .filter(
                  m ->
                      m.source().equals(engine.rootId())
                          && m.initialActions().isEmpty()
                          && m.actions()
                              .equals(
                                  List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.ANNULMENT)))
              .findFirst();
      if (method.isPresent() && method.get().points().containsKey(500)) return method.get();
      if (engine.memoryLimited())
        throw new AssertionError("Continuation exceeded memory allowance");
      engine.step();
    }
    throw new AssertionError("Continuation did not reach requested horizon");
  }

  @Test
  void continuedRunnerMatchesUninterruptedExitMassAndCurrencyUses() throws Exception {
    var fixture = new PathSearchServiceTest();
    var request = request(fixture);
    var original = engine(fixture, request);
    var restored = engine(fixture, request);
    try {
      for (int i = 0; i < 2000; i++) original.step();
      original.settleCheckpoint();
      var rows = new ArrayList<String>();
      original.checkpoint(
          fixture.json,
          row -> {
            try {
              rows.add(fixture.json.writeValueAsString(row));
            } catch (Exception error) {
              throw new IllegalStateException(error);
            }
          });
      try (var read = restored.checkpointRestore(fixture.json)) {
        for (var row : rows)
          read.accept(
              fixture.json.readValue(row, CertifiedMethodSearchEngine.CheckpointLine.class));
        read.finish();
      }
      var expected = continuation(original);
      var actual = continuation(restored);
      assertThat(actual.branches()).isEqualTo(expected.branches());
      for (int cycles : List.of(100, 300, 500)) {
        assertThat(actual.points().get(cycles).initialUses())
            .isEqualTo(expected.points().get(cycles).initialUses());
        assertThat(actual.points().get(cycles).cycleUses())
            .isEqualTo(expected.points().get(cycles).cycleUses());
        assertThat(actual.points().get(cycles).mass())
            .isEqualTo(expected.points().get(cycles).mass());
      }
    } finally {
      original.close();
      restored.close();
    }
  }

  public static void main(String[] args) throws Exception {
    com.poe2craft.crafting.infrastructure.WorkbenchDefinitionsLoader.initialize();
    var test = new CertifiedJobRestartTest();
    var fixture = new PathSearchServiceTest();
    try (var service = test.service(fixture, Path.of(args[0]))) {
      var snapshot = service.getCertified("owner", args[1]);
      var entry =
          service.advanceCertifiedEntry(
              "owner",
              args[1],
              args[2],
              new IndexedMethodProtocol.Advance(2, snapshot.generation(), 3, 100000));
      java.nio.file.Files.writeString(
          Path.of(args[3]),
          fixture.json.writeValueAsString(
              Map.of(
                  "methods",
                  test.methods(service, snapshot),
                  "entry",
                  entry,
                  "operations",
                  snapshot.operations())));
    }
  }

  private void child(Path dir, String id, String entry) throws Exception {
    var paths = new LinkedHashSet<String>();
    for (ClassLoader loader = getClass().getClassLoader();
        loader != null;
        loader = loader.getParent())
      if (loader instanceof java.net.URLClassLoader urls)
        for (var url : urls.getURLs()) paths.add(Path.of(url.toURI()).toString());
    if (paths.isEmpty()) paths.add(System.getProperty("java.class.path"));
    var process =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                String.join(java.io.File.pathSeparator, paths),
                getClass().getName(),
                dir.toString(),
                id,
                entry,
                dir.resolve("child-result.json").toString())
            .redirectErrorStream(true)
            .redirectOutput(dir.resolve("child.log").toFile())
            .start();
    try {
      assertThat(process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .withFailMessage(java.nio.file.Files.readString(dir.resolve("child.log")))
          .isZero();
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
  }

  private MethodSearchService service(PathSearchServiceTest fixture, Path dir) {
    var service =
        new MethodSearchService(
            fixture.catalog,
            fixture.goals,
            fixture.identity,
            fixture.json,
            Clock.systemUTC(),
            fixture.executor,
            12000);
    service.persistence(new LocalCertifiedJobRepository(dir, fixture.json));
    return service;
  }

  private CertifiedMethodProtocol.Create request(PathSearchServiceTest fixture) throws Exception {
    try (var old = fixture.service(1)) {
      var original = fixture.request(old);
      var item = original.start().item();
      var low =
          new ItemState(
              item.snapshotId(),
              item.baseItemId(),
              1,
              item.rarity(),
              item.implicits(),
              item.explicits(),
              item.conditions(),
              item.augmentSockets(),
              item.catalystQuality());
      return new CertifiedMethodProtocol.Create(
          1,
          new MethodSearchProtocol.Create(
              2,
              "restart-real",
              new BasicCurrencyState(low, original.start().provenance()),
              original.goal(),
              List.of()),
          new CertifiedInterval.Precision(32, 2));
    }
  }

  private List<CertifiedMethodProtocol.Method> methods(
      MethodSearchService service, CertifiedMethodProtocol.Snapshot snapshot) {
    var result = new ArrayList<CertifiedMethodProtocol.Method>();
    int offset = 0;
    while (true) {
      var page = service.certifiedGraph("owner", snapshot.jobId(), snapshot.revision(), 0, offset);
      result.addAll(page.methods());
      if (page.nextMethodOffset() == null) return result;
      offset = page.nextMethodOffset();
    }
  }

  @Test
  void jobReopensGraphAndPartialConditionalTransportWithNewGeneration(@TempDir Path dir)
      throws Exception {
    var fixture = new PathSearchServiceTest();
    var request = request(fixture);
    CertifiedMethodProtocol.Snapshot before;
    List<CertifiedMethodProtocol.Method> points;
    String entryId;
    CertifiedMethodProtocol.Entry expected;
    try (var original = service(fixture, dir)) {
      var created = original.createCertified("owner", request).search();
      fixture.queue.remove().run();
      before = original.getCertified("owner", created.jobId());
      assertThat(before.status()).isEqualTo("PAUSED");
      points = methods(original, before);
      var method =
          points.stream()
              .filter(
                  m ->
                      m.sourceStateId().equals(before.rootStateId())
                          && m.initialActions().isEmpty()
                          && m.cycleActions().equals(List.of("EXALTED")))
              .findFirst()
              .orElseThrow();
      var outcome =
          original
              .certifiedOutcomes("owner", before.jobId(), before.revision(), method.id(), 100, 0)
              .outcomes()
              .getFirst();
      var binding =
          new IndexedMethodProtocol.Bind(
              2,
              "persisted-entry",
              before.revision(),
              "root",
              method.id(),
              100,
              outcome.kind(),
              outcome.stateId());
      var entry = original.bindCertifiedEntry("owner", before.jobId(), binding);
      entryId = entry.id();
      entry =
          original.advanceCertifiedEntry(
              "owner",
              before.jobId(),
              entryId,
              new IndexedMethodProtocol.Advance(2, before.generation(), 3, 100000));
      assertThat(entry.status()).isEqualTo("PARTIAL");
      child(dir, before.jobId(), entryId);
      var childResult =
          fixture.json.readTree(java.nio.file.Files.readString(dir.resolve("child-result.json")));
      assertThat(childResult.get("methods")).isEqualTo(fixture.json.valueToTree(points));
      assertThat(childResult.get("operations").asLong()).isEqualTo(before.operations());
      // A second process/service claims the committed revision. The old generation cannot write it.
      try (var reopened = service(new PathSearchServiceTest(), dir)) {
        var restored = reopened.getCertified("owner", before.jobId());
        assertThat(restored.operations()).isEqualTo(before.operations());
        assertThat(restored.generation()).isGreaterThan(before.generation());
        assertThat(methods(reopened, restored)).isEqualTo(points);
        assertThat(reopened.bindCertifiedEntry("owner", before.jobId(), binding).id())
            .isEqualTo(entryId);
        assertThatThrownBy(
                () ->
                    reopened.advanceCertifiedEntry(
                        "owner",
                        before.jobId(),
                        entryId,
                        new IndexedMethodProtocol.Advance(2, before.generation(), 1, 100000)))
            .isInstanceOf(PathSearchProtocol.Rejected.class);
        var value = reopened.certifiedEntry("owner", before.jobId(), entryId, 0);
        for (int i = 0; i < 1000 && !value.status().equals("COMPLETE"); i++)
          value =
              reopened.advanceCertifiedEntry(
                  "owner",
                  before.jobId(),
                  entryId,
                  new IndexedMethodProtocol.Advance(2, restored.generation(), 4096, 100000));
        assertThat(value.status()).isEqualTo("COMPLETE");
        assertThat(value.nextOffset()).isNull();
        var primitive =
            BasicCurrencyTransitions.forMethodSearch(fixture.catalog, fixture.identity)
                .primitiveCursor(request.request().start(), WorkbenchCurrency.EXALTED, Set.of());
        var raw =
            new HashMap<
                String,
                com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction>();
        while (!primitive.complete()) {
          var rolled = primitive.next();
          raw.merge(
              rolled.state().canonicalKey(),
              rolled.probability(),
              com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction::add);
        }
        var mass = com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction.ZERO;
        for (var state : value.states())
          mass = mass.add(Objects.requireNonNull(raw.get(state.stateId())));
        for (var state : value.states())
          assertThat(state.probability().fraction())
              .isEqualTo(raw.get(state.stateId()).divide(mass));
        var lower = value.conditionalProbability().lower().fraction();
        var upper = value.conditionalProbability().upper().fraction();
        assertThat(lower.numerator().multiply(mass.denominator()))
            .isLessThanOrEqualTo(mass.numerator().multiply(lower.denominator()));
        assertThat(upper.numerator().multiply(mass.denominator()))
            .isGreaterThanOrEqualTo(mass.numerator().multiply(upper.denominator()));
        expected = value;
        assertThatThrownBy(
                () ->
                    original.advanceCertifiedEntry(
                        "owner",
                        before.jobId(),
                        entryId,
                        new IndexedMethodProtocol.Advance(2, before.generation(), 1, 100000)))
            .isInstanceOf(IllegalStateException.class);
      }
    }
    try (var reopened = service(new PathSearchServiceTest(), dir)) {
      var value = reopened.certifiedEntry("owner", before.jobId(), entryId, 0);
      assertThat(value.states()).isEqualTo(expected.states());
      assertThat(value.conditionalProbability()).isEqualTo(expected.conditionalProbability());
      var snapshot = reopened.getCertified("owner", before.jobId());
      var cancel =
          new MethodSearchProtocol.Mutation(
              2, "durable-cancel", snapshot.revision(), "CANCEL", null);
      var cancelled = reopened.mutateCertified("owner", before.jobId(), cancel);
      assertThat(reopened.mutateCertified("owner", before.jobId(), cancel)).isEqualTo(cancelled);
    }
    try (var reopened = service(new PathSearchServiceTest(), dir)) {
      assertThat(reopened.getCertified("owner", before.jobId()).status()).isEqualTo("CANCELLED");
    }
    var descriptor = dir.resolve(before.jobId() + ".job.json");
    var changed = fixture.json.readTree(java.nio.file.Files.readString(descriptor));
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.get("identity"))
        .put("rules", "different-rules");
    var retained = fixture.json.writeValueAsString(changed);
    java.nio.file.Files.writeString(descriptor, retained);
    var malformed = dir.resolve(UUID.randomUUID() + ".job.json");
    java.nio.file.Files.writeString(malformed, "{");
    try (var reopened = service(new PathSearchServiceTest(), dir)) {
      assertThatThrownBy(() -> reopened.getCertified("owner", before.jobId()))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              error -> assertThat(error.code()).isEqualTo("CHECKPOINT_RECALCULATION_REQUIRED"));
      assertThatThrownBy(() -> reopened.createCertified("owner", request))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
    assertThat(java.nio.file.Files.readString(descriptor)).isEqualTo(retained);
    assertThat(java.nio.file.Files.readString(malformed)).isEqualTo("{");
  }
}
