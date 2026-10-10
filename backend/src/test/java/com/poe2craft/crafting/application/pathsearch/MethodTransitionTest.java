package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class MethodTransitionTest {
  final PathSearchServiceTest fixture = new PathSearchServiceTest();

  PathSearchCalculation calculate(Function<ItemState, Status> predicate) throws Exception {
    try (var service = fixture.service(50000)) {
      var request = fixture.request(service);
      var c =
          new PathSearchCalculation(
              request.start(),
              predicate,
              List.of("0", "1", "2", "3", "100", "300", "500"),
              new BasicCurrencyTransitions(fixture.catalog, fixture.identity));
      while (!c.complete()) c.step();
      return c;
    }
  }

  static void conserved(Recommendation r) {
    assertThat(r.method()).isNotNull();
    for (int i = 0; i < r.points().size(); i++) {
      Fraction hit = r.method().omitted().get(i).hit().fraction();
      Fraction active = r.method().omitted().get(i).active().fraction();
      for (var exit : r.method().exits()) {
        var mass = exit.points().get(i).probability().fraction();
        if (exit.kind().equals("HIT")) hit = hit.add(mass);
        else active = active.add(mass);
      }
      var p = r.points().get(i);
      assertThat(hit).isEqualTo(p.lower().fraction());
      assertThat(active).isEqualTo(p.active().fraction());
      assertThat(hit.add(active).add(p.dead().fraction()).add(p.unresolved().fraction()))
          .isEqualTo(Fraction.ONE);
    }
  }

  @Test
  void lateSuccessSurvivesDisplayLimitAndHasStateSpecificAbsorption() throws Exception {
    BasicCurrencyTransitions.Outcome target;
    try (var service = fixture.service(50000)) {
      var cursor =
          new BasicCurrencyTransitions(fixture.catalog, fixture.identity)
              .renewalCursor(fixture.request(service).start(), List.of(WorkbenchCurrency.CHAOS));
      target = cursor.next();
      for (int i = 0; i < 40; i++) target = cursor.next();
    }
    var selected = target;
    var c =
        calculate(item -> item.equals(selected.state().item()) ? Status.MATCH : Status.NO_MATCH);
    var recommendations = c.recommendations();
    recommendations.forEach(MethodTransitionTest::conserved);
    var chaos = recommendations.getFirst();
    var hit =
        chaos.method().exits().stream()
            .filter(e -> e.kind().equals("HIT"))
            .findFirst()
            .orElseThrow();
    assertThat(hit.stateId()).isEqualTo(selected.state().canonicalKey());
    assertThat(hit.points().get(1).probability().fraction()).isEqualTo(selected.probability());
    var q = selected.probability();
    var survival = com.poe2craft.crafting.domain.pathsearch.RenewalFirstHit.complement(q);
    assertThat(hit.points().get(2).probability().fraction()).isEqualTo(q.add(survival.multiply(q)));
    assertThat(chaos.method().omitted().get(1).active().fraction()).isNotEqualTo(Fraction.ZERO);
    // A late method exit must retain its real action edge, not just its aggregate absorption mass.
    assertThat(
            c.edges.values().stream()
                .filter(e -> e.from().contains(chaos.policy().id()))
                .filter(e -> c.executions.get(e.to()).stateId().equals(hit.stateId())))
        .singleElement()
        .satisfies(e -> assertThat(e.probability().fraction()).isEqualTo(q));
  }

  @Test
  void omittedSuccessUnknownMassAndOddTwoActionObservationRemainDistinct() throws Exception {
    var start = root();
    var c =
        calculate(
            item -> {
              if (item.explicits().isEmpty() || item.explicits().equals(start.explicits()))
                return Status.NO_MATCH;
              long value = item.explicits().getFirst().values().values().iterator().next();
              return value % 3 == 0
                  ? Status.NO_MATCH
                  : value % 2 == 0 ? Status.MATCH : Status.UNKNOWN;
            });
    for (var r : c.recommendations()) {
      conserved(r);
      assertThat(r.method().omitted().getLast().hit().fraction()).isNotEqualTo(Fraction.ZERO);
      assertThat(r.points().getLast().unresolved().fraction()).isNotEqualTo(Fraction.ZERO);
      if (r.method().cycleUses() == 2) {
        var active =
            r.method().exits().stream()
                .filter(
                    e ->
                        e.kind().equals("ACTIVE")
                            && !e.points().get(3).probability().fraction().equals(Fraction.ZERO))
                .toList();
        assertThat(active).hasSize(1);
        assertThat(c.nodes.get(active.getFirst().stateId()).item().explicits()).isEmpty();
      }
    }
  }

  ItemState root() {
    try (var service = fixture.service(50000)) {
      return fixture.request(service).start().item();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void countExclusionMethodExitsRetainRealExecutionsAndRecoverToRoot() throws Exception {
    String owner = "00000000-0000-0000-0000-000000000001";
    try (var service = fixture.service(50000)) {
      var original = fixture.request(service);
      var goal =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              fixture.json.valueToTree(original.goal());
      var group = (com.fasterxml.jackson.databind.node.ObjectNode) goal.withArray("groups").get(0);
      group.put("type", "COUNT");
      group.putObject("range").put("min", 0).put("max", 0);
      ((com.fasterxml.jackson.databind.node.ObjectNode) group.withArray("entries").get(0))
          .putObject("range")
          .put("min", 6)
          .put("max", 10);
      var request =
          new Create(
              1,
              "count-exclusion",
              original.start(),
              fixture.json.treeToValue(
                  goal, com.poe2craft.crafting.domain.goalfilter.GoalFilter.class),
              List.of(),
              List.of("0", "1", "2", "3", "100", "300", "500"));
      assertThat(request.start().item().itemLevel()).isEqualTo(82);
      assertThat(request.start().item().explicits().getFirst().values()).containsValue(6L);
      var result = fixture.finish(service, service.create(owner, request));
      assertThat(result.status()).isEqualTo("COMPLETED");
      var pages = new ArrayList<GraphPage>();
      var page = result.graph();
      while (true) {
        pages.add(page);
        if (page.nextCursor() == null) break;
        page = service.graph(owner, result.jobId(), result.revision(), page.nextCursor());
      }
      var edges = pages.stream().flatMap(p -> p.edges().stream()).toList();
      for (var expansion : pages.stream().flatMap(p -> p.expansions().stream()).toList()) {
        var mass =
            edges.stream()
                .filter(e -> e.from().equals(expansion.executionId()))
                .map(e -> e.probability().fraction())
                .reduce(Fraction.ZERO, Fraction::add);
        assertThat(mass.add(expansion.unresolved().fraction())).isEqualTo(Fraction.ONE);
      }
      var chaos = result.recommendations().getFirst();
      var active =
          chaos.method().exits().stream()
              .filter(
                  e ->
                      e.kind().equals("ACTIVE")
                          && e.points().stream()
                              .anyMatch(
                                  p ->
                                      p.attempts().equals("100")
                                          && p.probability().fraction().numerator().signum() > 0))
              .toList();
      assertThat(active).hasSize(5);
      var transitions = new BasicCurrencyTransitions(fixture.catalog, fixture.identity);
      var cursor = transitions.renewalCursor(request.start(), List.of(WorkbenchCurrency.CHAOS));
      var actualMass = new HashMap<String, Fraction>();
      while (!cursor.complete()) {
        var outcome = cursor.next();
        actualMass.merge(outcome.state().canonicalKey(), outcome.probability(), Fraction::add);
      }
      for (var exit : active) {
        var execution =
            result.graph().executions().stream()
                .filter(
                    e ->
                        e.stateId().equals(exit.stateId())
                            && e.policyId().equals(chaos.policy().id())
                            && e.phase() == 0)
                .findFirst()
                .orElseThrow();
        var rootExecution =
            result.graph().executions().stream()
                .filter(
                    e ->
                        e.stateId().equals(request.start().canonicalKey())
                            && e.policyId().equals(chaos.policy().id())
                            && e.phase() == 0)
                .findFirst()
                .orElseThrow();
        assertThat(
                edges.stream()
                    .filter(
                        e -> e.from().equals(rootExecution.id()) && e.to().equals(execution.id())))
            .singleElement()
            .satisfies(
                e -> {
                  assertThat(e.action()).isEqualTo("CHAOS");
                  assertThat(e.probability().fraction()).isEqualTo(actualMass.get(exit.stateId()));
                });
        var recovery =
            fixture.finish(
                service,
                service.recover(
                    owner,
                    result.jobId(),
                    new Recover(
                        1,
                        "recover-" + execution.id(),
                        result.revision(),
                        execution.id(),
                        request.start().canonicalKey(),
                        List.of("0", "1", "2"))));
        assertThat(recovery.status()).isEqualTo("COMPLETED");
        assertThat(recovery.recovery().conditional()).isTrue();
        assertThat(recovery.recovery().includedInMain()).isFalse();
        assertThat(recovery.graph().nodes().getFirst().id()).isEqualTo(exit.stateId());
      }
      assertThat(service.get(owner, result.jobId())).isEqualTo(result);
      java.nio.file.Files.createDirectories(java.nio.file.Path.of("build/path-search"));
      fixture.json.writeValue(
          java.nio.file.Path.of("build/path-search/recovery-count-request.json").toFile(), request);
      fixture.json.writeValue(
          java.nio.file.Path.of("build/path-search/recovery-count-snapshot.json").toFile(), result);
      fixture.json.writeValue(
          java.nio.file.Path.of("build/path-search/recovery-count-pages.json").toFile(), pages);
    }
  }

  @Test
  void emptyIntermediateGoalStopsAfterOneUseAndZeroSuccessHasNoHitEdges() throws Exception {
    var early = calculate(item -> item.explicits().isEmpty() ? Status.MATCH : Status.NO_MATCH);
    for (var r : early.recommendations()) {
      conserved(r);
      if (r.method().cycleUses() == 2) {
        assertThat(r.points().get(1).lower().fraction()).isEqualTo(Fraction.ONE);
        assertThat(r.method().exits().stream().filter(e -> e.kind().equals("HIT")))
            .allSatisfy(e -> assertThat(early.nodes.get(e.stateId()).item().explicits()).isEmpty());
      } else assertThat(r.method().exits()).noneMatch(e -> e.kind().equals("HIT"));
    }
    var zero = calculate(item -> Status.NO_MATCH);
    zero.recommendations()
        .forEach(
            r -> {
              conserved(r);
              assertThat(r.method().exits()).noneMatch(e -> e.kind().equals("HIT"));
            });
  }
}
