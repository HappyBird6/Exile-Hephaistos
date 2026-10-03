package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class QualityLimitTest {
  final ItemCatalog solar = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(solar, new CraftingEngine(solar));

  @Test
  void defaultAndBreachCapsPreserveConcreteRollsAcrossCraftsAndRefusals() {
    var root = SolarAmulet.initial(solar);
    assertThat(QualityLimitRules.describe(root, solar).maximumQuality()).isEqualTo(20);
    var rare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))),
            Set.of());
    assertThat(QualityLimitRules.describe(rare, solar).maximumQuality()).isEqualTo(40);
    var divine = simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), new Random(7));
    assertThat(divine.applied()).isTrue();
    assertThat(divine.qualityLimit()).isEqualTo(QualityLimitRules.describe(rare, solar));
    assertThat(divine.state().explicits()).isEqualTo(rare.explicits());
    var blocked = simulator.apply(rare, WorkbenchCurrency.REGAL, Set.of(), new Random(7));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(rare);
    assertThat(blocked.qualityLimit().maximumQuality()).isEqualTo(40);
    var annul = simulator.apply(rare, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(7));
    assertThat(annul.applied()).isTrue();
    assertThat(annul.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(rare.explicits()).hasSize(1);
  }

  @Test
  void stockyDefaultDoesNotInterpretLocalWardAsQualityOrEnableQualityActions() {
    var f = new StockyHysteriaTest();
    var rare = f.rare(82, null, "stocky-mitts:prefix:alloy-local-runic-ward");
    assertThat(QualityLimitRules.describe(rare, f.catalog).maximumQuality()).isEqualTo(20);
    var result = f.simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), new Random(7));
    assertThat(result.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(f.simulator.actions(f.root, Set.of())).hasSize(57);
  }

  @Test
  void forgedBreachValuesAndSpecialStatesDoNotProduceAReviewedCap() {
    var root = SolarAmulet.initial(solar);
    var forged =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 40L))),
            Set.of());
    assertThatThrownBy(() -> QualityLimitRules.describe(forged, solar))
        .isInstanceOf(IllegalArgumentException.class);
    var corrupted =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            root.implicits(),
            root.explicits(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThatThrownBy(() -> QualityLimitRules.describe(corrupted, solar))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
