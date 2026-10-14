package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedSearchJobsTest {
  @Test
  void expiryEvictionAndShutdownInvalidateQueuedWorkWithoutRunningClosedStores() throws Exception {
    var fixture = new PathSearchServiceTest();
    var now =
        new java.util.concurrent.atomic.AtomicReference<>(
            java.time.Instant.parse("2026-10-14T00:00:00Z"));
    var expireAfterReads = new java.util.concurrent.atomic.AtomicInteger();
    Clock clock =
        new Clock() {
          @Override
          public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(now.get(), zone);
          }

          @Override
          public java.time.Instant instant() {
            if (expireAfterReads.get() > 0 && expireAfterReads.decrementAndGet() == 0)
              now.set(now.get().plusSeconds(21 * 60));
            return now.get();
          }
        };
    try (var old = fixture.service(1);
        var service =
            new MethodSearchService(
                fixture.catalog,
                fixture.goals,
                fixture.identity,
                fixture.json,
                clock,
                fixture.executor,
                64)) {
      var original = fixture.request(old);
      var request =
          new CertifiedMethodProtocol.Create(
              1,
              new MethodSearchProtocol.Create(
                  2, "expiry", original.start(), original.goal(), List.of()),
              CertifiedInterval.Precision.standard());
      var first = service.createCertified("owner", request);
      var staleWorker = fixture.queue.remove();
      expireAfterReads.set(2); // Lookup succeeds; expiry occurs before the job lock check.
      rejected(
          "SEARCH_EXPIRED",
          () ->
              service.mutateCertified(
                  "owner",
                  first.search().jobId(),
                  new MethodSearchProtocol.Mutation(
                      2, "late-cancel", first.search().revision(), "CANCEL", null)));
      rejected("SEARCH_EXPIRED", () -> service.getCertified("owner", first.search().jobId()));
      var replacement = service.createCertified("owner", request);
      assertThat(replacement.search().jobId()).isNotEqualTo(first.search().jobId());
      assertThatCode(staleWorker::run).doesNotThrowAnyException();
      rejected("SEARCH_NOT_FOUND", () -> service.getCertified("owner", first.search().jobId()));
      var queuedAtShutdown = fixture.queue.remove();
      service.close();
      assertThatCode(queuedAtShutdown::run).doesNotThrowAnyException();
      rejected(
          "SEARCH_NOT_FOUND", () -> service.getCertified("owner", replacement.search().jobId()));
    }
  }

  @Test
  void realCatalogBoundsKeepExactConditionalLawAndGenerationControls() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var old = fixture.service(1);
        var service =
            new MethodSearchService(
                fixture.catalog,
                fixture.goals,
                fixture.identity,
                fixture.json,
                Clock.systemUTC(),
                fixture.executor,
                12000)) {
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
      var request =
          new CertifiedMethodProtocol.Create(
              1,
              new MethodSearchProtocol.Create(
                  2,
                  "certified-real",
                  new BasicCurrencyState(low, original.start().provenance()),
                  original.goal(),
                  List.of()),
              new CertifiedInterval.Precision(32, 2));
      var started = service.createCertified("owner", request);
      var created = started.search();
      assertThat(started.rootEntry().states()).hasSize(1);
      assertThat(service.createCertified("owner", request).search()).isEqualTo(created);
      rejected("REQUEST_ID_REUSED", () -> service.createIndexed("owner", request.request()));
      rejected("REQUEST_ID_REUSED", () -> service.create("owner", request.request()));
      rejected(
          "REQUEST_ID_REUSED",
          () ->
              service.createCertified(
                  "owner",
                  new CertifiedMethodProtocol.Create(
                      1, request.request(), new CertifiedInterval.Precision(64, 2))));
      var cancel =
          new MethodSearchProtocol.Mutation(2, "cancel", created.revision(), "CANCEL", null);
      var cancelled = service.mutateCertified("owner", created.jobId(), cancel);
      assertThat(service.mutateCertified("owner", created.jobId(), cancel)).isEqualTo(cancelled);
      var resumed =
          service.mutateCertified(
              "owner",
              created.jobId(),
              new MethodSearchProtocol.Mutation(2, "resume", cancelled.revision(), "RESUME", null));
      fixture.queue.remove().run();
      assertThat(service.getCertified("owner", created.jobId())).isEqualTo(resumed);
      fixture.queue.remove().run();
      var current = service.getCertified("owner", created.jobId());
      assertThat(current.status()).isEqualTo("PAUSED");
      assertThat(current.enumerationComplete()).isFalse();
      assertThat(current.strategyStatus()).isEqualTo("NOT_REQUESTED");
      assertThat(current.arithmeticVersion()).isEqualTo(CertifiedInterval.VERSION);
      assertThat(current.transition()).isEqualTo(original.start().provenance());
      var methods = new ArrayList<CertifiedMethodProtocol.Method>();
      int offset = 0;
      while (true) {
        var page = service.certifiedGraph("owner", current.jobId(), current.revision(), 0, offset);
        methods.addAll(page.methods());
        if (page.nextMethodOffset() == null) break;
        offset = page.nextMethodOffset();
      }
      var method =
          methods.stream()
              .filter(
                  m ->
                      m.sourceStateId().equals(current.rootStateId())
                          && m.initialActions().isEmpty()
                          && m.cycleActions().equals(List.of("EXALTED")))
              .findFirst()
              .orElseThrow();
      assertThat(method.points())
          .extracting(CertifiedMethodProtocol.Point::cycles)
          .containsExactly(100, 300, 500);
      var outcomes =
          service.certifiedOutcomes(
              "owner", current.jobId(), current.revision(), method.id(), 100, 0);
      assertThat(outcomes.nextOffset()).isNull();
      var raw = new HashMap<BasicCurrencyState, Fraction>();
      var primitive = BasicCurrencyTransitions.forMethodSearch(fixture.catalog, fixture.identity);
      var cursor =
          primitive.primitiveCursor(request.request().start(), WorkbenchCurrency.EXALTED, Set.of());
      while (!cursor.complete()) {
        var outcome = cursor.next();
        raw.merge(outcome.state(), outcome.probability(), Fraction::add);
      }
      var reconstructed = new HashMap<BasicCurrencyState, Fraction>();
      int sequence = 0;
      for (var outcome : outcomes.outcomes()) {
        var bind =
            new IndexedMethodProtocol.Bind(
                2,
                "entry-" + sequence++,
                current.revision(),
                "root",
                method.id(),
                100,
                outcome.kind(),
                outcome.stateId());
        var entry = service.bindCertifiedEntry("owner", current.jobId(), bind);
        assertThat(service.bindCertifiedEntry("owner", current.jobId(), bind).id())
            .isEqualTo(entry.id());
        if (sequence == 1) {
          var limited =
              service.advanceCertifiedEntry(
                  "owner",
                  current.jobId(),
                  entry.id(),
                  new IndexedMethodProtocol.Advance(2, current.generation(), 4096, 1));
          assertThat(limited.reason()).isEqualTo("MEMORY_BUDGET");
          assertThat(limited.states()).isEmpty();
        }
        for (int step = 0; step < 1000 && !entry.status().equals("COMPLETE"); step++)
          entry =
              service.advanceCertifiedEntry(
                  "owner",
                  current.jobId(),
                  entry.id(),
                  new IndexedMethodProtocol.Advance(2, current.generation(), 4096, 100000));
        assertThat(entry.status()).isEqualTo("COMPLETE");
        var law = new HashMap<BasicCurrencyState, Fraction>();
        while (true) {
          entry
              .states()
              .forEach(
                  row ->
                      law.put(
                          new BasicCurrencyState(row.item(), original.start().provenance()),
                          row.probability().fraction()));
          if (entry.nextOffset() == null) break;
          entry = service.certifiedEntry("owner", current.jobId(), entry.id(), entry.nextOffset());
        }
        var mass = law.keySet().stream().map(raw::get).reduce(Fraction.ZERO, Fraction::add);
        contains(outcome.probability(), mass);
        contains(entry.conditionalProbability(), mass);
        assertThat(law.values().stream().reduce(Fraction.ZERO, Fraction::add))
            .isEqualTo(Fraction.ONE);
        law.forEach(
            (state, probability) ->
                reconstructed.merge(state, probability.multiply(mass), Fraction::add));
      }
      assertThat(reconstructed).isEqualTo(raw);
      rejected("SEARCH_NOT_FOUND", () -> service.getCertified("other", current.jobId()));
      rejected(
          "GENERATION_CONFLICT",
          () ->
              service.advanceCertifiedEntry(
                  "owner",
                  current.jobId(),
                  "root",
                  new IndexedMethodProtocol.Advance(2, 0L, 1, 100000)));
      rejected(
          "REVISION_EXPIRED",
          () -> service.certifiedGraph("owner", current.jobId(), created.revision(), 0, 0));
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                  new com.poe2craft.crafting.presentation.pathsearch.MethodSearchController(
                      service, fixture.json))
              .build();
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                      "/api/v2/crafting/method-searches/certified")
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content(
                      fixture.json.writeValueAsString(
                          new CertifiedMethodProtocol.Create(
                              1,
                              new MethodSearchProtocol.Create(
                                  2,
                                  "http",
                                  request.request().start(),
                                  request.request().goal(),
                                  List.of()),
                              request.precision()))))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isAccepted());
    }
  }

  @Test
  void wireNeverConvertsAnUnseparatedRoundedIntervalToOneValue() {
    var tie =
        CertifiedInterval.enclose(
            new Fraction(java.math.BigInteger.valueOf(101), java.math.BigInteger.valueOf(20000)),
            64);
    var bounds = CertifiedMethodProtocol.Bounds.probability(tie, 2);
    assertThat(bounds.confirmedPercent()).isNull();
    assertThat(bounds.exact()).isFalse();
    contains(
        bounds,
        new Fraction(java.math.BigInteger.valueOf(101), java.math.BigInteger.valueOf(20000)));
    var quantity =
        CertifiedMethodProtocol.Bounds.quantity(
            CertifiedInterval.one(64).add(CertifiedInterval.one(64)));
    assertThat(quantity.lower().fraction()).isEqualTo(Fraction.of(2, 1));
    assertThat(quantity.confirmedPercent()).isNull();
  }

  @Test
  void completedEnumerationStillHonorsEntryCancellationAndDoesNotClaimStrategy() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var old = fixture.service(1);
        var service =
            new MethodSearchService(
                fixture.catalog,
                fixture.goals,
                fixture.identity,
                fixture.json,
                Clock.systemUTC(),
                fixture.executor,
                64)) {
      var original = fixture.request(old);
      var tree = fixture.json.valueToTree(original.goal());
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              tree.path("groups").get(0).path("entries").get(0).path("range"))
          .put("min", 1);
      var goal =
          fixture.json.treeToValue(tree, com.poe2craft.crafting.domain.goalfilter.GoalFilter.class);
      var request =
          new CertifiedMethodProtocol.Create(
              1,
              new MethodSearchProtocol.Create(2, "complete", original.start(), goal, List.of()),
              CertifiedInterval.Precision.standard());
      var started = service.createCertified("owner", request);
      fixture.queue.remove().run();
      var completed = service.getCertified("owner", started.search().jobId());
      assertThat(completed.status()).isEqualTo("COMPLETED");
      assertThat(completed.enumerationComplete()).isTrue();
      assertThat(completed.strategyStatus()).isEqualTo("NOT_REQUESTED");
      var cancelled =
          service.mutateCertified(
              "owner",
              completed.jobId(),
              new MethodSearchProtocol.Mutation(
                  2, "cancel-complete", completed.revision(), "CANCEL", null));
      rejected(
          "SEARCH_CANCELLED",
          () ->
              service.advanceCertifiedEntry(
                  "owner",
                  completed.jobId(),
                  "root",
                  new IndexedMethodProtocol.Advance(2, cancelled.generation(), 1, 1)));
      var resumed =
          service.mutateCertified(
              "owner",
              completed.jobId(),
              new MethodSearchProtocol.Mutation(
                  2, "resume-complete", cancelled.revision(), "RESUME", null));
      fixture.queue.remove().run();
      assertThat(service.getCertified("owner", completed.jobId()).status()).isEqualTo("COMPLETED");
      assertThat(
              service
                  .advanceCertifiedEntry(
                      "owner",
                      completed.jobId(),
                      "root",
                      new IndexedMethodProtocol.Advance(2, resumed.generation(), 1, 1))
                  .states())
          .isEqualTo(started.rootEntry().states());
      service.createIndexed(
          "owner",
          new MethodSearchProtocol.Create(2, "reverse", original.start(), goal, List.of()));
      rejected(
          "REQUEST_ID_REUSED",
          () ->
              service.createCertified(
                  "owner",
                  new CertifiedMethodProtocol.Create(
                      1,
                      new MethodSearchProtocol.Create(
                          2, "reverse", original.start(), goal, List.of()),
                      request.precision())));
    }
  }

  private static void contains(CertifiedMethodProtocol.Bounds bounds, Fraction value) {
    var lo = bounds.lower().fraction();
    var hi = bounds.upper().fraction();
    assertThat(lo.numerator().multiply(value.denominator()))
        .isLessThanOrEqualTo(value.numerator().multiply(lo.denominator()));
    assertThat(hi.numerator().multiply(value.denominator()))
        .isGreaterThanOrEqualTo(value.numerator().multiply(hi.denominator()));
  }

  private static void rejected(String reason, Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            PathSearchProtocol.Rejected.class, ex -> assertThat(ex.code()).isEqualTo(reason));
  }
}
