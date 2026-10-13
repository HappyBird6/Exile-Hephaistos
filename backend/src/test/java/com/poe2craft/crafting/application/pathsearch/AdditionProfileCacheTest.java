package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import org.junit.jupiter.api.Test;

class AdditionProfileCacheTest {
  @Test
  void completedProfileReusesOnlyCandidateLawAndRebuildsEachActualItemWitness() {
    var f = new DeferredProductionTest();
    var root = f.root();
    var evaluate = f.fixture.goals.compileEvaluation(root.item(), f.goal(47));
    var partition = f.fixture.goals.rollPartition(root.item(), f.goal(47));
    var cached = new BasicTransitionClasses(f.fixture.catalog, f.primitive, partition, evaluate, 2);
    var uncached =
        new BasicTransitionClasses(f.fixture.catalog, f.primitive, partition, evaluate, 0);
    var partial = cached.indexedCursor(root, WorkbenchCurrency.EXALTED);
    partial.step();
    assertThat(cached.profileMetrics().profiles()).isZero();
    var first = cached.indexedCursor(root, WorkbenchCurrency.EXALTED);
    while (!first.complete()) first.step();
    assertThat(cached.profileMetrics().profiles()).isPositive();
    for (int pass = 0; pass < 2; pass++) {
      var actual = cached.indexedCursor(root, WorkbenchCurrency.EXALTED);
      var expected = uncached.indexedCursor(root, WorkbenchCurrency.EXALTED);
      while (!actual.complete()) actual.step();
      while (!expected.complete()) expected.step();
      assertThat(actual.result().outcomes()).isEqualTo(expected.result().outcomes());
      assertThat(actual.result().witnesses()).isEqualTo(expected.result().witnesses());
    }
    assertThat(cached.profileMetrics().hits()).isGreaterThanOrEqualTo(2);
    for (var child : first.result().witnesses().values()) {
      for (var action : java.util.List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.CHAOS)) {
        var actual = cached.indexedCursor(child, action);
        var expected = uncached.indexedCursor(child, action);
        while (!actual.complete()) actual.step();
        while (!expected.complete()) expected.step();
        assertThat(actual.result().outcomes()).isEqualTo(expected.result().outcomes());
        assertThat(actual.result().witnesses()).isEqualTo(expected.result().witnesses());
        if (action == WorkbenchCurrency.EXALTED) {
          var grandchild = actual.result().witnesses().values().iterator().next();
          var multi = cached.indexedCursor(grandchild, WorkbenchCurrency.CHAOS);
          var raw = uncached.indexedCursor(grandchild, WorkbenchCurrency.CHAOS);
          while (!multi.complete()) multi.step();
          while (!raw.complete()) raw.step();
          assertThat(multi.result().outcomes()).isEqualTo(raw.result().outcomes());
        }
      }
    }
    assertThat(cached.profileMetrics().profiles()).isLessThanOrEqualTo(2);
    assertThat(cached.profileMetrics().retainedRows()).isLessThanOrEqualTo(20000);
    assertThat(uncached.profileMetrics().profiles()).isZero();
    while (!partial.complete()) partial.step();
    assertThat(partial.result().outcomes()).isEqualTo(first.result().outcomes());
  }
}
