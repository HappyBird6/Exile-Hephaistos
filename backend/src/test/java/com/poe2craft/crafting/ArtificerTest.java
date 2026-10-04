package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArtificerTest {
  final StockyHysteriaTest f = new StockyHysteriaTest();

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
    assertThat(again.applied()).isFalse();
    assertThat(again.state()).isEqualTo(r.state());
  }

  @Test
  void legacyUnknownAndSolarRefuseWithoutChangingState() {
    for (var s :
        List.of(
            f.root,
            SolarAmulet.initial(
                com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault()))) {
      var c =
          s.baseItemId().equals(f.root.baseItemId())
              ? f.simulator
              : new WorkbenchSimulator(
                  com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault(),
                  new CraftingEngine(
                      com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault()));
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
      if (a.available()) {
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
    for (int n : new int[] {-1, 2, 3})
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
