package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.crafting.domain.StockyCraftingFixture;
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

  final StockyCraftingFixture f = new StockyCraftingFixture();

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
    var one = SocketAugments.install(empty(2), 0, "Perfect_Desert_Rune");
    var two = SocketAugments.install(one, 1, "Perfect_Glacial_Rune");
    assertThat(two.installedAugments()).hasSize(2);
    var replaced = SocketAugments.install(two, 0, "Perfect_Storm_Rune");
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
    assertThatThrownBy(() -> SocketAugments.install(empty(3), 0, "Perfect_Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(0), 0, "Perfect_Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(2), 2, "Perfect_Desert_Rune"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SocketAugments.install(empty(2), -1, "Perfect_Desert_Rune"))
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
    assertThat(SocketAugments.catalog()).hasSize(289);
  }

  @Test
  void blocksNewLowerTiersButRetainsLegacyStatesAndOtherReviewedCompositions() {
    var entries = SocketAugments.catalog();
    int selectable = 0;
    for (var entry : entries) {
      var id = entry.path("id").asText();
      if (entry.path("selectionEnabled").asBoolean()) selectable++;
      if (!SocketAugments.supports(f.root.baseItemId(), id)) continue;
      if (entry.path("selectionEnabled").asBoolean()) {
        assertThat(SocketAugments.install(empty(2), 0, id).installedAugments())
            .containsExactly(new ItemState.InstalledAugment(0, id));
      } else {
        assertThatThrownBy(() -> SocketAugments.install(empty(2), 0, id))
            .isInstanceOf(IllegalArgumentException.class);
        var old = empty(2).withAugments(List.of(new ItemState.InstalledAugment(0, id)));
        var result =
            f.simulator.apply(old, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(4));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().installedAugments()).isEqualTo(old.installedAugments());
        assertThat(SocketAugments.install(old, 1, "Perfect_Desert_Rune").installedAugments().get(0))
            .isEqualTo(old.installedAugments().get(0));
      }
    }
    assertThat(selectable).isEqualTo(244);
    assertThat(SocketAugments.catalog().findValuesAsText("id"))
        .contains("Lesser_Tempered_Rune", "Tempered_Rune", "Greater_Tempered_Rune")
        .doesNotContain("Perfect_Tempered_Rune");
  }

  @Test
  void ordinaryIdolsAndEyesEnforcePerIdentityAndSharedLimitsButAllowReplacement() {
    var idol = SocketAugments.install(empty(2), 0, "Snake_Idol");
    assertThat(SocketAugments.install(idol, 1, "Snake_Idol").installedAugments()).hasSize(2);
    var limited = SocketAugments.install(empty(2), 0, "Idol_of_Sirrius");
    assertThatThrownBy(() -> SocketAugments.install(limited, 1, "Idol_of_Sirrius"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(SocketAugments.install(limited, 0, "Idol_of_Sirrius")).isEqualTo(limited);
    var eye = SocketAugments.install(empty(2), 0, "Tecrods_Gaze");
    assertThatThrownBy(() -> SocketAugments.install(eye, 1, "Kurgals_Gaze"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(SocketAugments.install(eye, 0, "Kurgals_Gaze").installedAugments())
        .containsExactly(new ItemState.InstalledAugment(0, "Kurgals_Gaze"));
    assertThatThrownBy(
            () ->
                empty(2)
                    .withAugments(
                        List.of(
                            new ItemState.InstalledAugment(0, "Tecrods_Gaze"),
                            new ItemState.InstalledAugment(1, "Kurgals_Gaze"))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void retainsLegacyVizomaDuplicatesButRejectsNewDuplicatesAndDeferredSpecialOperations() {
    var weapon =
        new ItemState(
            f.root.snapshotId(),
            "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand2",
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of(),
            2);
    var legacy =
        weapon.withAugments(
            List.of(
                new ItemState.InstalledAugment(0, "Soul_Core_of_Vizoma"),
                new ItemState.InstalledAugment(1, "Soul_Core_of_Vizoma")));
    assertThat(legacy.withEquipmentQuality(10).installedAugments()).hasSize(2);
    var single = SocketAugments.install(weapon, 0, "Soul_Core_of_Vizoma");
    assertThatThrownBy(() -> SocketAugments.install(single, 1, "Soul_Core_of_Vizoma"))
        .isInstanceOf(IllegalArgumentException.class);
    for (var id : List.of("Cadigans_Epiphany", "Masterwork_Rune", "Owl_Idol")) {
      assertThatThrownBy(() -> SocketAugments.install(empty(2), 0, id))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void preservesAllNineOriginalLimitedSoulCoreStatesWithoutAllowingNewDuplicates() {
    var bow =
        new ItemState(
            f.root.snapshotId(),
            "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow8Endgame",
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of(),
            2);
    int checked = 0;
    for (var entry : SocketAugments.catalog()) {
      if (!entry.path("legacyLimitExempt").asBoolean(false)) continue;
      var id = entry.path("id").asText();
      var old =
          bow.withAugments(
              List.of(
                  new ItemState.InstalledAugment(0, id), new ItemState.InstalledAugment(1, id)));
      assertThat(old.withEquipmentQuality(10).installedAugments()).hasSize(2);
      var single = SocketAugments.install(bow, 0, id);
      assertThatThrownBy(() -> SocketAugments.install(single, 1, id))
          .isInstanceOf(IllegalArgumentException.class);
      checked++;
    }
    assertThat(checked).isEqualTo(9);
  }
}
