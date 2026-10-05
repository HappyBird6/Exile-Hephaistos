package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReviewedSceptresTest {
  @Test
  void hallowedRetainsCompleteVerifiedClassPoolWithoutTurningSkillIntoAffix() {
    var c = ItemCatalogLoader.loadTopBase("hallowed");
    var old = ItemCatalogLoader.loadSceptre();
    assertThat(c.base().id()).isEqualTo(ReviewedSceptres.BASES.get("hallowed"));
    assertThat(c.metadata().snapshotId()).isNotEqualTo(old.metadata().snapshotId());
    assertThat(c.modifiers()).isEqualTo(old.modifiers());
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(150);
    assertThat(c.base().hasImplicit()).isFalse();
    assertThat(c.modifiers().values())
        .allMatch(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT);
    var root =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(QualityLimitRules.describe(root, c).maximumQuality()).isEqualTo(20);
  }

  @Test
  void reviewedCommandAndPerfectCommandApplyAndWrongClassEssencesRejectAtomically() {
    var c = ItemCatalogLoader.loadTopBase("hallowed");
    var coupled =
        c.modifiers().values().stream()
            .filter(d -> d.stats().size() > 1)
            .map(ModifierDefinition::id)
            .collect(java.util.stream.Collectors.toSet());
    var simulator =
        new WorkbenchSimulator(
            c,
            new CraftingEngine(c),
            coupled,
            SceptreEssenceTargets.VERIFIED,
            Map.of(
                WorkbenchCurrency.PERFECT_ESSENCE_COMMAND,
                List.of("rattling-sceptre:suffix:essence-aura-magnitude")));
    var normal =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    for (var action : SceptreEssenceTargets.VERIFIED.keySet()) {
      var magic =
          new ItemState(
              normal.snapshotId(),
              normal.baseItemId(),
              82,
              ItemState.Rarity.MAGIC,
              List.of(),
              List.of(),
              Set.of());
      var r = simulator.apply(magic, action, Set.of(), new Random(17));
      assertThat(r.applied()).isTrue();
      assertThat(r.state().explicits())
          .anyMatch(m -> SceptreEssenceTargets.VERIFIED.get(action).contains(m.modifierId()));
    }
    var rolled =
        simulator.apply(normal, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(17)).state();
    var rare =
        new ItemState(
            rolled.snapshotId(),
            rolled.baseItemId(),
            rolled.itemLevel(),
            ItemState.Rarity.RARE,
            rolled.implicits(),
            rolled.explicits().subList(0, 1),
            rolled.conditions());
    var perfect =
        simulator.apply(rare, WorkbenchCurrency.PERFECT_ESSENCE_COMMAND, Set.of(), new Random(17));
    assertThat(perfect.applied()).isTrue();
    assertThat(perfect.state().explicits())
        .anyMatch(m -> m.modifierId().equals("rattling-sceptre:suffix:essence-aura-magnitude"));
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_ESSENCE_SORCERY,
            WorkbenchCurrency.PERFECT_ESSENCE_BODY,
            WorkbenchCurrency.ARTIFICER)) {
      var r = simulator.apply(rare, action, Set.of(), new Random(17));
      assertThat(r.applied()).isFalse();
      assertThat(r.state()).isEqualTo(rare);
    }
  }
}
