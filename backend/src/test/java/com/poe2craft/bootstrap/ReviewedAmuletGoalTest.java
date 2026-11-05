package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.ReviewedAmulets;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;

class ReviewedAmuletGoalTest {
  @TestFactory
  Stream<DynamicTest> everyRegisteredAmuletEvaluatesEveryCatalogStat() {
    return Stream.concat(Stream.of("solar"), ReviewedAmulets.BASES.keySet().stream().sorted())
        .map(
            key ->
                DynamicTest.dynamicTest(
                    key,
                    () -> {
                      var cfg = new CatalogConfiguration();
                      var base = cfg.itemCatalog(cfg.workbenchDefinitions());
                      var wb =
                          cfg.workbenchService(
                              base, cfg.workbenchSimulator(base, cfg.craftingEngine(base)));
                      var service =
                          new GoalFilterService(
                              new GoalCatalogIndex(
                                  BundledGoalCatalogs.load(wb), GoalDefinitionsLoader.load()));
                      var initial = wb.initial(key, 82);
                      var root = initial.state();
                      var catalog =
                          service.catalog(new Context(root.snapshotId(), root.baseItemId(), 82));
                      assertThat(catalog.stats()).isNotEmpty();
                      var essenceStats =
                          Set.of(
                              "global_armour_evasion_energy_shield_+%",
                              "strength_+%", "dexterity_+%", "intelligence_+%");
                      var low =
                          service.catalog(new Context(root.snapshotId(), root.baseItemId(), 1));
                      for (var raw : essenceStats) {
                        var selected =
                            catalog.stats().stream()
                                .filter(
                                    s ->
                                        s.kind() == GoalCatalog.Kind.EXPLICIT
                                            && s.sourceStatIds().equals(List.of(raw)))
                                .findFirst()
                                .orElseThrow();
                        assertThat(selected.eligible())
                            .as(key + ": selectable essence " + raw)
                            .isTrue();
                        assertThat(selected.unit()).isEqualTo("percent");
                        assertThat(
                                low.stats().stream()
                                    .filter(s -> s.statId().equals(selected.statId()))
                                    .findFirst()
                                    .orElseThrow()
                                    .eligible())
                            .isFalse();
                      }
                      for (var stat : catalog.stats()) {
                        // New source-only implicits remain visible but grant no inferred goal math.
                        if (stat.kind() == GoalCatalog.Kind.IMPLICIT
                            && !GoalDefinitionsLoader.load()
                                .directStats()
                                .containsKey(stat.sourceStatIds().getFirst())) {
                          assertThat(stat.support().evaluation()).isEqualTo(Capability.UNSUPPORTED);
                          assertThat(stat.support().probability())
                              .isEqualTo(Capability.UNSUPPORTED);
                          continue;
                        }
                        assertThat(stat.support().evaluation())
                            .as(key + ":" + stat.statId())
                            .isEqualTo(Capability.SUPPORTED);
                        if (stat.kind() == GoalCatalog.Kind.PSEUDO) continue;
                        var definitionEntry =
                            GoalDefinitionsLoader.load()
                                .directStats()
                                .get(stat.sourceStatIds().getFirst());
                        if (Boolean.TRUE.equals(definitionEntry.evaluationOnly()))
                          assertThat(stat.support().probability())
                              .isEqualTo(Capability.UNSUPPORTED);
                        var raw = stat.sourceStatIds().getFirst();
                        var definition =
                            initial.modifiers().values().stream()
                                .filter(
                                    d ->
                                        d.layer().name().equals(stat.kind().name())
                                            && d.stats().stream().anyMatch(s -> s.id().equals(raw)))
                                .findFirst()
                                .orElseThrow();
                        for (boolean maximum : List.of(false, true)) {
                          var values = new TreeMap<String, Long>();
                          definition
                              .stats()
                              .forEach(s -> values.put(s.id(), maximum ? s.max() : s.min()));
                          var modifier = new ModifierInstance(definition.id(), values);
                          var item =
                              new ItemState(
                                  root.snapshotId(),
                                  root.baseItemId(),
                                  82,
                                  ItemState.Rarity.RARE,
                                  stat.kind() == GoalCatalog.Kind.IMPLICIT
                                      ? List.of(modifier)
                                      : root.implicits(),
                                  stat.kind() == GoalCatalog.Kind.EXPLICIT
                                      ? List.of(modifier)
                                      : List.of(),
                                  Set.of());
                          var value =
                              BigDecimal.valueOf(values.get(raw))
                                  .divide(
                                      BigDecimal.valueOf(
                                          stat.sourceDivisor() == null ? 1 : stat.sourceDivisor()),
                                      java.math.MathContext.DECIMAL128);
                          assertThat(
                                  service
                                      .evaluate(
                                          item,
                                          goal(
                                              catalog,
                                              stat,
                                              new Range(
                                                  value.subtract(BigDecimal.ONE),
                                                  value.add(BigDecimal.ONE))))
                                      .status())
                              .as(key + ":" + raw)
                              .isEqualTo(Status.MATCH);
                          assertThat(
                                  service
                                      .evaluate(
                                          item,
                                          goal(
                                              catalog,
                                              stat,
                                              new Range(value.add(BigDecimal.ONE), null)))
                                      .status())
                              .as(key + ":" + raw + " boundary")
                              .isEqualTo(Status.NO_MATCH);
                          if (stat.kind() == GoalCatalog.Kind.EXPLICIT) {
                            var empty =
                                new ItemState(
                                    root.snapshotId(),
                                    root.baseItemId(),
                                    82,
                                    ItemState.Rarity.RARE,
                                    root.implicits(),
                                    List.of(),
                                    Set.of());
                            assertThat(
                                    service
                                        .evaluate(
                                            empty,
                                            goal(catalog, stat, new Range(BigDecimal.ZERO, null)))
                                        .status())
                                .as(raw + " absent is not a matching zero")
                                .isEqualTo(Status.NO_MATCH);
                          }
                        }
                      }
                    }));
  }

  private GoalFilter goal(GoalCatalog catalog, GoalCatalog.Stat stat, Range range) {
    return new GoalFilter(
        1,
        catalog.catalogVersion(),
        new General(
            catalog.context().baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(
            new Group(
                "g",
                Type.AND,
                false,
                null,
                List.of(new Entry("e", stat.statId(), stat.unit(), range, null, false)))));
  }
}
