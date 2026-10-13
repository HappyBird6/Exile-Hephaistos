package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedMethodProductionTest {
  @Test
  void realSolarRenewalKeepsExactTruthInsideEveryRequestedHorizon() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var service = fixture.service(1)) {
      var request = fixture.request(service);
      var original = request.start().item();
      var item =
          new ItemState(
              original.snapshotId(),
              original.baseItemId(),
              1,
              original.rarity(),
              original.implicits(),
              List.of(),
              Set.of());
      var root = new BasicCurrencyState(item, request.start().provenance());
      var tree = fixture.json.valueToTree(request.goal());
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              tree.path("groups").get(0).path("entries").get(0).path("range"))
          .put("min", 7);
      var goal = fixture.json.treeToValue(tree, GoalFilter.class);
      var evaluate = fixture.goals.compileEvaluation(item, goal);
      var primitive = BasicCurrencyTransitions.forMethodSearch(fixture.catalog, fixture.identity);
      var raw = primitive.primitiveCursor(root, WorkbenchCurrency.EXALTED, Set.of());
      Fraction hit = Fraction.ZERO;
      while (!raw.complete()) {
        var outcome = raw.next();
        if (evaluate.apply(outcome.state().item()).status() == GoalFilter.Status.MATCH)
          hit = hit.add(outcome.probability());
      }
      assertThat(hit.numerator().signum()).isPositive();
      assertThat(hit.numerator()).isLessThan(hit.denominator());
      var miss = new Fraction(hit.denominator().subtract(hit.numerator()), hit.denominator());
      for (int bits : List.of(16, 64)) {
        var index =
            new BasicClassIndex(
                new BasicTransitionClasses(
                    fixture.catalog, primitive, fixture.goals.rollPartition(item, goal), evaluate));
        int source = index.intern(root);
        var runner =
            new IndexedCertifiedMethodRunner(
                new IndexedKernelStore(index, 64, 10000),
                source,
                new Template<>(
                    "real-exalt-annul",
                    List.of(),
                    List.of(s -> WorkbenchCurrency.EXALTED, s -> WorkbenchCurrency.ANNULMENT)),
                state ->
                    index.key(state).observations().getFirst() == GoalFilter.Status.MATCH
                        ? Optional.of(Exit.GOAL)
                        : Optional.empty(),
                new CertifiedInterval.Precision(bits, 2));
        Fraction remaining = Fraction.ONE, exalt = Fraction.ZERO, annul = Fraction.ZERO;
        for (int cycles = 1; cycles <= 500; cycles++) {
          exalt = exalt.add(remaining);
          remaining = remaining.multiply(miss);
          annul = annul.add(remaining);
          runner.beginCycle();
          while (!runner.atBoundary()) runner.step();
          if (!List.of(100, 300, 500).contains(cycles)) continue;
          var point = runner.observation();
          var success =
              point.exits().get(Exit.GOAL).values().stream()
                  .reduce(CertifiedInterval.zero(bits), CertifiedInterval::add)
                  .probability();
          assertThat(
                  success.contains(
                      new Fraction(
                          remaining.denominator().subtract(remaining.numerator()),
                          remaining.denominator())))
              .isTrue();
          assertThat(point.active().keySet()).containsExactly(source);
          assertThat(point.active().get(source).contains(remaining)).isTrue();
          assertThat(point.cycleUses().get(WorkbenchCurrency.EXALTED).contains(exalt)).isTrue();
          assertThat(point.cycleUses().get(WorkbenchCurrency.ANNULMENT).contains(annul)).isTrue();
          assertThat(point.mass().contains(Fraction.ONE)).isTrue();
          assertThat(point.exits().get(Exit.UNRESOLVED)).isEmpty();
        }
      }
    }
  }
}
