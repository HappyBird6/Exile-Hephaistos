package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.domain.BasicClassIndex;
import com.poe2craft.craftingsupport.search.domain.BasicTransitionClasses;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 동치류를 정확히 식별하고 자주 사용하는 증명 상태만 메모리에 유지한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code collisionsAndEvictionPreserveFullClassIdentityAndKernel(directory)}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 입력: {@code directory}(디렉터리). 사용하는 예외: {@code Exception}.
 * </ul>
 */
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
