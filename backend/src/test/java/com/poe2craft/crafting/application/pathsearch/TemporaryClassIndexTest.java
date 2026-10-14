package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemporaryClassIndexTest {
  @Test
  void collisionsAndEvictionPreserveFullClassIdentityAndKernel(@TempDir Path directory)
      throws Exception {
    var fixture = new DeferredProductionTest();
    var root = fixture.root();
    var goal = fixture.goal(47);
    var evaluate = fixture.fixture.goals.compileEvaluation(root.item(), goal);
    var classes =
        new BasicTransitionClasses(
            fixture.fixture.catalog,
            fixture.primitive,
            fixture.fixture.goals.rollPartition(root.item(), goal),
            evaluate);
    var storage = new TemporaryClassIndex(directory, classes, 2, key -> 0);
    try (var memory = new BasicClassIndex(classes);
        var disk = new BasicClassIndex(classes, storage)) {
      assertThat(disk.intern(root)).isEqualTo(memory.intern(root));
      var unique = new LinkedHashMap<BasicTransitionClasses.Key, BasicCurrencyState>();
      var primitive = fixture.primitive.primitiveCursor(root, WorkbenchCurrency.EXALTED, Set.of());
      while (!primitive.complete() && unique.size() < 20) {
        var state = primitive.next().state();
        unique.putIfAbsent(classes.key(state), state);
      }
      assertThat(unique.size()).isGreaterThan(2);
      for (var state : unique.values())
        assertThat(disk.intern(state)).isEqualTo(memory.intern(state));
      for (var state : unique.values()) {
        int id = disk.classOf(state);
        assertThat(id).isEqualTo(memory.classOf(state));
        assertThat(disk.key(id)).isEqualTo(memory.key(id));
        assertThat(disk.witness(id)).isEqualTo(memory.witness(id));
        assertThat(disk.intern(state)).isEqualTo(id);
      }
      var actual = disk.cursor(0, WorkbenchCurrency.EXALTED);
      var expected = memory.cursor(0, WorkbenchCurrency.EXALTED);
      while (!actual.complete()) actual.step();
      while (!expected.complete()) expected.step();
      assertThat(actual.result()).isEqualTo(expected.result());
      assertThat(disk.size()).isEqualTo(memory.size());
      assertThat(storage.metrics().cachedRows()).isLessThanOrEqualTo(2);
      assertThat(storage.metrics().diskBytes()).isPositive();
      assertThatThrownBy(() -> storage.add(classes.key(root), unique.values().iterator().next()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    try (var files = Files.list(directory)) {
      assertThat(files.toList()).isEmpty();
    }
  }
}
