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
    // Legacy first-24 action edges can have no hit: method exits must not be derived from them.
    assertThat(
            c.edges.values().stream()
                .filter(e -> e.from().contains(chaos.policy().id()))
                .noneMatch(e -> c.executions.get(e.to()).stateId().equals(hit.stateId())))
        .isTrue();
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
