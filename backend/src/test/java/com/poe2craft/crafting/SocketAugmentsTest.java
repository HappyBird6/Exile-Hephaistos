package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SocketAugmentsTest {
  record LegacyItem(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      ItemState.Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<ItemState.Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<ItemState.DesecratedSlot> desecrated,
      Integer equipmentQuality,
      ItemObservations observations,
      ItemState.Sanctification sanctification) {}

  @Test
  void emptyAugmentsRetainLegacyRecordHashForBoundedSearch() {
    var s = empty(0);
    var legacy =
        new LegacyItem(
            s.snapshotId(),
            s.baseItemId(),
            s.itemLevel(),
            s.rarity(),
            s.implicits(),
            s.explicits(),
            s.conditions(),
            s.augmentSockets(),
            s.catalystQuality(),
            s.desecrated(),
            s.equipmentQuality(),
            s.observations(),
            s.sanctification());
    assertThat(s.hashCode()).isEqualTo(legacy.hashCode());
  }

  final StockyHysteriaTest f = new StockyHysteriaTest();

  ItemState empty(int count) {
    var s = f.root;
    return new ItemState(
        s.snapshotId(),
        s.baseItemId(),
        82,
        s.rarity(),
        s.implicits(),
        s.explicits(),
        s.conditions(),
        count);
  }

  @Test
  void installsReplacesAndPreservesThroughOrdinaryCrafting() {
    var one = SocketAugments.install(empty(2), 0, "Desert_Rune");
    var two = SocketAugments.install(one, 1, "Perfect_Glacial_Rune");
    assertThat(two.installedAugments()).hasSize(2);
    var replaced = SocketAugments.install(two, 0, "Storm_Rune");
    assertThat(replaced.installedAugments().get(1)).isEqualTo(two.installedAugments().get(1));
    var result =
        f.simulator.apply(replaced, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(4));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().installedAugments()).isEqualTo(replaced.installedAugments());
    assertThat(two.withObservations(null).withEquipmentQuality(0).installedAugments())
        .isEqualTo(two.installedAugments());
  }

  @Test
  void rejectsUnknownIdsOutOfRangeAndDuplicateSlots() {
    assertThatThrownBy(() -> SocketAugments.install(empty(3), 0, "Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(0), 0, "Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(2), 2, "Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(2), -1, "Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(2), 0, "Invented_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                empty(2)
                    .withAugments(
                        List.of(
                            new ItemState.InstalledAugment(0, "Desert_Rune"),
                            new ItemState.InstalledAugment(0, "Storm_Rune"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(empty(2).installedAugments()).isEmpty();
    assertThat(SocketAugments.catalog()).hasSize(79);
  }
}
