package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.infrastructure.goalfilter.*;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalCatalogParityTest {
  @Test
  void everyStartingCatalogSourceIsDiscoverableWithoutRarityOrFamilyPruning() throws Exception {
    var cfg = new CraftingConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    var workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    var catalogs = BundledGoalCatalogs.load(workbench);
    assertThat(catalogs).hasSize(134);
    var index = new GoalCatalogIndex(catalogs, GoalDefinitionsLoader.load());
    for (var source : catalogs) {
      for (int level : new int[] {1, 82}) {
        var goalCatalog =
            index.catalog(new Context(source.metadata().snapshotId(), source.base().id(), level));
        assertThat(goalCatalog.issues()).isEmpty();
        assertThat(goalCatalog.sourceModifiers()).isEqualTo(source.modifiers());
        var ids = goalCatalog.stats().stream().map(GoalCatalog.Stat::statId).toList();
        for (var modifier : source.modifiers().values())
          for (var stat : modifier.stats())
            assertThat(ids).contains(GoalCatalogIndex.id(modifier.layer().name(), stat.id()));
      }
    }
    var service = new GoalFilterService(index);
    var json = new ObjectMapper();
    for (String key : List.of("solar", "stocky", "amber")) {
      var initial = workbench.initial(key, 82);
      var state = initial.state();
      var context = new Context(state.snapshotId(), state.baseItemId(), 82);
      var catalog = service.catalog(context);
      var definition =
          initial.modifiers().values().stream()
              .filter(
                  d ->
                      d.layer() == ModifierDefinition.Layer.EXPLICIT
                          && d.stats().size() == 1
                          && d.stats().getFirst().id().equals("base_maximum_life")
                          && d.weight() > 0
                          && d.requiredItemLevel() <= 82)
              .findFirst()
              .orElseThrow();
      long value = definition.stats().getFirst().max();
      var item =
          new ItemState(
              state.snapshotId(),
              state.baseItemId(),
              82,
              ItemState.Rarity.RARE,
              state.implicits(),
              List.of(new ModifierInstance(definition.id(), Map.of("base_maximum_life", value))),
              Set.of());
      var stat =
          catalog.stats().stream()
              .filter(s -> s.statId().equals(GoalCatalogIndex.id("explicit", "base_maximum_life")))
              .findFirst()
              .orElseThrow();
      var goal =
          new GoalFilter(
              1,
              catalog.catalogVersion(),
              new General(
                  state.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
              List.of(
                  new Group(
                      "g",
                      Type.AND,
                      false,
                      null,
                      List.of(
                          new Entry(
                              "e",
                              stat.statId(),
                              stat.unit(),
                              new Range(BigDecimal.valueOf(value), BigDecimal.valueOf(value)),
                              null,
                              false)))));
      var roundTrip = json.readValue(json.writeValueAsBytes(goal), GoalFilter.class);
      assertThat(service.evaluate(item, roundTrip).status()).isEqualTo(Status.MATCH);
      var unsupported =
          catalog.stats().stream()
              .filter(
                  s ->
                      s.kind() == GoalCatalog.Kind.EXPLICIT
                          && s.eligible()
                          && s.support().evaluation() == Capability.UNSUPPORTED)
              .findFirst()
              .orElseThrow();
      var unreviewed =
          new GoalFilter(
              1,
              catalog.catalogVersion(),
              goal.general(),
              List.of(
                  new Group(
                      "g",
                      Type.AND,
                      false,
                      null,
                      List.of(
                          new Entry(
                              "e",
                              unsupported.statId(),
                              unsupported.unit(),
                              new Range(null, null),
                              null,
                              false)))));
      assertThat(service.evaluate(item, unreviewed).status()).isEqualTo(Status.UNSUPPORTED);
    }
  }
}
