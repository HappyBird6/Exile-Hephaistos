package com.poe2craft.crafting.application.goalfilter;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class SolarCheckpointGoalTest {
  private static final String SPIRIT = "base_spirit_from_equipment";
  private static final String PROJECTILE = "projectile_skill_gem_level_+";
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final GoalCatalogIndex index =
      new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
  private final GoalFilterService service = new GoalFilterService(index);

  private ItemState item(ModifierInstance... modifiers) {
    var initial = SolarAmulet.initial(catalog, 82, 15);
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        82,
        ItemState.Rarity.RARE,
        initial.implicits(),
        List.of(modifiers),
        Set.of());
  }

  private ModifierInstance modifier(String stat, long value) {
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.requiredItemLevel() <= 82
                        && d.stats().size() == 1
                        && d.stats().getFirst().id().equals(stat)
                        && d.stats().getFirst().contains(value))
            .findFirst()
            .orElseThrow();
    return new ModifierInstance(definition.id(), Map.of(stat, value));
  }

  private Entry target(String stat, long minimum) {
    return new Entry(
        stat,
        GoalCatalogIndex.id("explicit", stat),
        "flat",
        new Range(BigDecimal.valueOf(minimum), null),
        null,
        false);
  }

  private GoalFilter goal(Entry... entries) {
    return new GoalFilter(
        1,
        index.version(),
        new General(catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(new Group("targets", Type.AND, false, null, List.of(entries))));
  }

  @Test
  void spirit47ExcludesImplicitEvenWhenImplicitAndExplicitSumExceeds47() {
    var goal = goal(target(SPIRIT, 47));
    assertThat(service.evaluate(item(), goal).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(modifier(SPIRIT, 46)), goal).status())
        .isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(modifier(SPIRIT, 47)), goal).status()).isEqualTo(Status.MATCH);
  }

  @Test
  void solarRootABCDiamondUsesReviewedNumericGoalsAndReevaluatesLossAndReacquisition() {
    var a = modifier(PROJECTILE, 3);
    var b = modifier(SPIRIT, 47);
    var projectile = goal(target(PROJECTILE, 3));
    var spirit = goal(target(SPIRIT, 47));
    var both = goal(target(PROJECTILE, 3), target(SPIRIT, 47));
    assertThat(service.evaluate(item(), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(a), projectile).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(b), spirit).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(a), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(b), both).status()).isEqualTo(Status.NO_MATCH);
    assertThat(service.evaluate(item(a, b), both).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item(modifier(PROJECTILE, 2), b), both).status())
        .isEqualTo(Status.NO_MATCH);
    var compiled = service.compile(item(a, b), both);
    assertThat(compiled.apply(item(b))).isEqualTo(Status.NO_MATCH);
    assertThat(compiled.apply(item(a, b))).isEqualTo(Status.MATCH);
  }
}
