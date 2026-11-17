package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Artificer 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code f}: f ({@code StockyCraftingFixture}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code sockets(s, n)}: 장착홈수 처리에서 {@code s.snapshotId}, {@code s.baseItemId}을(를) 사용한다. 입력:
 *       {@code s}(s), {@code n}(n). 반환: {@code ItemState}.
 *   <li>{@code addsExactlyOneWithoutInventedMinimumLevel(level)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 입력: {@code level}(레벨).
 *   <li>{@code legacyUnknownAndSolarRefuseWithoutChangingState()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code allAvailableOrdinaryActionsAndFracturePreserveSockets()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code exceptionalAndSpecialStatesStayUnsupported()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class ArtificerTest {
  final StockyCraftingFixture f = new StockyCraftingFixture();

  ItemState sockets(ItemState s, Integer n) {
    return new ItemState(
        s.snapshotId(),
        s.baseItemId(),
        s.itemLevel(),
        s.rarity(),
        s.implicits(),
        s.explicits(),
        s.conditions(),
        n);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 4, 5, 82, 100})
  void addsExactlyOneWithoutInventedMinimumLevel(int level) {
    var s =
        new ItemState(
            f.root.snapshotId(),
            f.root.baseItemId(),
            level,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of(),
            0);
    var r =
        f.simulator.apply(
            s, WorkbenchCurrency.ARTIFICER, Set.of("Omen_of_the_Blessed"), new Random(1));
    assertThat(r.applied()).isTrue();
    assertThat(r.state()).isEqualTo(sockets(s, 1));
    assertThat(r.events()).isEmpty();
    assertThat(r.assumptions()).isEmpty();
    assertThat(r.consumedOmens()).isEmpty();
    assertThat(r.remainingOmens()).containsExactly("Omen_of_the_Blessed");
    assertThat(s.augmentSockets()).isZero();
    var again = f.simulator.apply(r.state(), WorkbenchCurrency.ARTIFICER, Set.of(), new Random(1));
    assertThat(again.applied()).isTrue();
    assertThat(again.state().augmentSockets()).isEqualTo(2);
    assertThat(
            f.simulator
                .apply(again.state(), WorkbenchCurrency.ARTIFICER, Set.of(), new Random(1))
                .applied())
        .isFalse();
  }

  @Test
  void legacyUnknownAndSolarRefuseWithoutChangingState() {
    for (var s : List.of(f.root, SolarAmulet.initial(ItemCatalogLoader.loadDefault()))) {
      var c =
          s.baseItemId().equals(f.root.baseItemId())
              ? f.simulator
              : new WorkbenchSimulator(
                  ItemCatalogLoader.loadDefault(),
                  new CraftingEngine(ItemCatalogLoader.loadDefault()));
      var r = c.apply(s, WorkbenchCurrency.ARTIFICER, Set.of(), new Random(1));
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(s);
      assertThat(r.reason()).isNotBlank();
      assertThat(c.apply(s, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1)).applied())
          .isTrue();
    }
  }

  @Test
  void allAvailableOrdinaryActionsAndFracturePreserveSockets() {
    var s =
        sockets(
            f.rare(
                82, null, "stocky-mitts:prefix:layered", "stocky-mitts:suffix:of-the-salamander"),
            1);
    for (var a : f.simulator.actions(s, Set.of()))
      if (a.available() && a.action() != WorkbenchCurrency.ARTIFICER) {
        var r = f.simulator.apply(s, a.action(), Set.of(), new Random(42));
        assertThat(r.state().augmentSockets()).as(a.action().name()).isEqualTo(1);
      }
    var magic =
        f.simulator.apply(
            sockets(f.root, 1), WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1));
    assertThat(magic.state().augmentSockets()).isEqualTo(1);
    var identified =
        f.simulator.apply(magic.state(), WorkbenchCurrency.REGAL, Set.of(), new Random(1));
    assertThat(identified.state().augmentSockets()).isEqualTo(1);
  }

  @Test
  void exceptionalAndSpecialStatesStayUnsupported() {
    for (int n : new int[] {-1, 3, 4})
      assertThatThrownBy(
              () ->
                  f.simulator.apply(
                      sockets(f.root, n), WorkbenchCurrency.ARTIFICER, Set.of(), new Random(1)))
          .isInstanceOf(IllegalArgumentException.class);
    for (var condition : ItemState.Condition.values()) {
      var s =
          new ItemState(
              f.root.snapshotId(),
              f.root.baseItemId(),
              82,
              ItemState.Rarity.NORMAL,
              List.of(),
              List.of(),
              Set.of(condition),
              0);
      assertThatThrownBy(
              () -> f.simulator.apply(s, WorkbenchCurrency.ARTIFICER, Set.of(), new Random(1)))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
