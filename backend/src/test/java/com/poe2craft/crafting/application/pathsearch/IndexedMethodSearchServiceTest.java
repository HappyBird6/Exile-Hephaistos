package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

class IndexedMethodSearchServiceTest {
  @Test
  void actualLowLevelSolarPreservesConcreteEntryLawAndCancelledGeneration() throws Exception {
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
          new Create(
              2,
              "indexed-real",
              new BasicCurrencyState(low, original.start().provenance()),
              original.goal(),
              List.of());
      var started = service.createIndexed("owner", request);
      var created = started.search();
      assertThat(started.rootEntry().states()).hasSize(1);
      assertThat(started.rootEntry().states().getFirst().item()).isEqualTo(low);
      assertThat(service.createIndexed("owner", request).search()).isEqualTo(created);
      rejected("REQUEST_ID_REUSED", () -> service.create("owner", request));
      var cancelled =
          service.mutate(
              "owner",
              created.jobId(),
              new Mutation(2, "cancel", created.revision(), "CANCEL", null));
      var resumed =
          service.mutate(
              "owner",
              created.jobId(),
              new Mutation(2, "resume", cancelled.revision(), "RESUME", null));
      fixture.queue.remove().run();
      assertThat(service.get("owner", created.jobId())).isEqualTo(resumed);
      fixture.queue.remove().run();
      var current = service.get("owner", created.jobId());
      assertThat(current.status()).isEqualTo("PAUSED");
      assertThat(current.enumerationComplete()).isFalse();
      assertThat(current.strategies()).isEmpty();
      assertThat(current.calculationVersion()).isEqualTo(IndexedMethodSearchEngine.VERSION);
      var arrows = new ArrayList<Arrow>();
      var page = service.graph("owner", current.jobId(), current.revision(), null);
      while (true) {
        arrows.addAll(page.methods());
        if (page.nextCursor() == null) break;
        page = service.graph("owner", current.jobId(), current.revision(), page.nextCursor());
      }
      var method =
          arrows.stream()
              .filter(
                  a ->
                      a.sourceStateId().equals(current.rootStateId())
                          && a.initialActions().isEmpty()
                          && a.cycleActions().equals(List.of("EXALTED")))
              .findFirst()
              .orElseThrow();
      assertThat(method.points()).extracting(Point::cycles).containsExactly(100, 300, 500);
      var outcomes =
          service.indexedOutcomes("owner", current.jobId(), current.revision(), method.id(), 100);
      Fraction total = Fraction.ZERO;
      var actual = new HashMap<BasicCurrencyState, Fraction>();
      int sequence = 0;
      for (var outcome : outcomes.outcomes()) {
        assertThat(outcome.kind()).isEqualTo("CHECKPOINT");
        var bind =
            new IndexedMethodProtocol.Bind(
                2,
                "law-" + sequence++,
                current.revision(),
                "root",
                method.id(),
                100,
                outcome.kind(),
                outcome.stateId());
        var entry = service.bindEntry("owner", current.jobId(), bind);
        assertThat(service.bindEntry("owner", current.jobId(), bind).id()).isEqualTo(entry.id());
        if (sequence == 1) {
          var limited =
              service.advanceEntry(
                  "owner",
                  current.jobId(),
                  entry.id(),
                  new IndexedMethodProtocol.Advance(2, current.generation(), 4096, 1));
          assertThat(limited.status()).isEqualTo("PAUSED");
          assertThat(limited.reason()).isEqualTo("MEMORY_BUDGET");
          assertThat(limited.states()).isEmpty();
        }
        for (int step = 0; step < 1000 && !entry.status().equals("COMPLETE"); step++)
          entry =
              service.advanceEntry(
                  "owner",
                  current.jobId(),
                  entry.id(),
                  new IndexedMethodProtocol.Advance(2, current.generation(), 4096, 100000));
        assertThat(entry.status()).isEqualTo("COMPLETE");
        var mass = fraction(outcome.probability());
        total = total.add(mass);
        Fraction normalized = Fraction.ZERO;
        while (true) {
          for (var row : entry.states()) {
            normalized = normalized.add(fraction(row.probability()));
            actual.merge(
                new BasicCurrencyState(row.item(), request.start().provenance()),
                mass.multiply(fraction(row.probability())),
                Fraction::add);
          }
          if (entry.nextOffset() == null) break;
          entry = service.entry("owner", current.jobId(), entry.id(), entry.nextOffset());
        }
        assertThat(normalized).isEqualTo(Fraction.ONE);
      }
      assertThat(total).isEqualTo(Fraction.ONE);
      var primitive = BasicCurrencyTransitions.forMethodSearch(fixture.catalog, fixture.identity);
      var cursor = primitive.primitiveCursor(request.start(), WorkbenchCurrency.EXALTED, Set.of());
      var expected = new HashMap<BasicCurrencyState, Fraction>();
      while (!cursor.complete()) {
        var outcome = cursor.next();
        expected.merge(outcome.state(), outcome.probability(), Fraction::add);
      }
      assertThat(actual).isEqualTo(expected);
      assertThat(actual.size()).isGreaterThan(outcomes.outcomes().size());
      String jobId = current.jobId();
      long revision = current.revision();
      rejected(
          "CONDITIONAL_DISTRIBUTION_REQUIRED",
          () -> service.distribution("owner", jobId, revision, method.id(), 100, null));
      var paused =
          service.mutate(
              "owner", jobId, new Mutation(2, "cancel-detail", revision, "CANCEL", null));
      rejected(
          "GENERATION_CONFLICT",
          () ->
              service.advanceEntry(
                  "owner",
                  jobId,
                  "root",
                  new IndexedMethodProtocol.Advance(2, current.generation(), 1, 1)));
      rejected(
          "JOB_CANCELLED",
          () ->
              service.advanceEntry(
                  "owner",
                  jobId,
                  "root",
                  new IndexedMethodProtocol.Advance(2, paused.generation(), 1, 1)));
      rejected("JOB_EXPIRED", () -> service.entry("other", jobId, "root", 0));
      var continued =
          service.mutate(
              "owner", jobId, new Mutation(2, "resume-detail", paused.revision(), "RESUME", null));
      assertThat(
              service
                  .advanceEntry(
                      "owner",
                      jobId,
                      "root",
                      new IndexedMethodProtocol.Advance(2, continued.generation(), 1, 100000))
                  .states())
          .isEqualTo(started.rootEntry().states());
      rejected(
          "INVALID_BUDGET",
          () ->
              service.advanceEntry(
                  "owner",
                  jobId,
                  "root",
                  new IndexedMethodProtocol.Advance(2, continued.generation(), 1, 1)));
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                  new com.poe2craft.crafting.presentation.pathsearch.MethodSearchController(
                      service, fixture.json))
              .build();
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                      "/api/v2/crafting/method-searches/indexed")
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content(
                      fixture.json.writeValueAsString(
                          new Create(
                              2, "indexed-http", request.start(), request.goal(), List.of()))))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isAccepted())
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                      "$.search.calculationVersion")
                  .value(IndexedMethodSearchEngine.VERSION))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                      "$.rootEntry.status")
                  .value("COMPLETE"));
    }
  }

  @Test
  void completedIndexedEnumerationCanCancelAndResumeEntryWorkWithoutStrategy() throws Exception {
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
      var started =
          service.createIndexed(
              "owner", new Create(2, "complete-indexed", original.start(), goal, List.of()));
      fixture.queue.remove().run();
      var completed = service.get("owner", started.search().jobId());
      assertThat(completed.status()).isEqualTo("COMPLETED");
      assertThat(completed.enumerationComplete()).isTrue();
      assertThat(completed.strategies()).isEmpty();
      assertThat(completed.strategyVersion()).isEqualTo("NOT_REQUESTED");
      var cancelled =
          service.mutate(
              "owner",
              completed.jobId(),
              new Mutation(2, "cancel-complete", completed.revision(), "CANCEL", null));
      assertThat(cancelled.status()).isEqualTo("CANCELLED");
      rejected(
          "JOB_CANCELLED",
          () ->
              service.advanceEntry(
                  "owner",
                  completed.jobId(),
                  "root",
                  new IndexedMethodProtocol.Advance(2, cancelled.generation(), 1, 1)));
      var resumed =
          service.mutate(
              "owner",
              completed.jobId(),
              new Mutation(2, "resume-complete", cancelled.revision(), "RESUME", null));
      fixture.queue.remove().run();
      assertThat(service.get("owner", completed.jobId()).status()).isEqualTo("COMPLETED");
      assertThat(
              service
                  .advanceEntry(
                      "owner",
                      completed.jobId(),
                      "root",
                      new IndexedMethodProtocol.Advance(2, resumed.generation(), 1, 1))
                  .states())
          .isEqualTo(started.rootEntry().states());
    }
  }

  private static Fraction fraction(PathSearchProtocol.Probability p) {
    return new Fraction(
        new java.math.BigInteger(p.numerator()), new java.math.BigInteger(p.denominator()));
  }

  private static void rejected(
      String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(
            PathSearchProtocol.Rejected.class, ex -> assertThat(ex.code()).isEqualTo(code));
  }
}
