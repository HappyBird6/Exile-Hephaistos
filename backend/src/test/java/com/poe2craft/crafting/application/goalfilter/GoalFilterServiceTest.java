package com.poe2craft.crafting.application.goalfilter;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.infrastructure.goalfilter.BundledGoalCatalogs;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalFilterServiceTest {
  @Test
  void disabledUnknownContentStillRequiresUnitAndCountMinimumCanProveImpossibleGoal() {
    var context = new Context(itemCatalog.metadata().snapshotId(), itemCatalog.base().id(), 82);
    var active =
        goal(
            index.version(),
            context.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(null, null));
    var entry = active.groups().getFirst().entries().getFirst();
    var bad =
        new GoalFilter(
            1,
            index.version(),
            active.general(),
            List.of(
                new Group(
                    "g",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        entry,
                        new Entry(
                            "disabled", "unknown", null, new Range(null, null), null, true)))));
    assertThatThrownBy(() -> service.validate(context, bad))
        .isInstanceOfSatisfying(
            GoalFilterService.InvalidGoal.class,
            e -> assertThat(e.issues()).extracting(Issue::code).contains("INVALID_STRUCTURE"));
    var impossible =
        new GoalFilter(
            1,
            index.version(),
            active.general(),
            List.of(
                new Group(
                    "count",
                    Type.COUNT,
                    false,
                    new Range(new BigDecimal("999999999999999999999"), null),
                    List.of(entry))));
    assertThat(service.validate(context, impossible).valid()).isTrue();
    assertThat(service.evaluate(SolarAmulet.initial(itemCatalog), impossible).status())
        .isEqualTo(Status.NO_MATCH);
  }

  @Test
  void checkedInProductionExampleMatchesCurrentCatalogAndActualItemEvaluation() throws Exception {
    var all = new GoalCatalogIndex(BundledGoalCatalogs.load(ReviewedCatalogTestData.workbench()));
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var stream =
        getClass().getResourceAsStream("/crafting/goalfilter/production-example.json")) {
      var example = mapper.readTree(stream);
      assertThat(example.get("synthetic").booleanValue()).isFalse();
      var context = mapper.treeToValue(example.get("context"), Context.class);
      assertThat(example.get("catalog"))
          .isEqualTo(mapper.readTree(mapper.writeValueAsString(all.catalog(context))));
      var item = mapper.treeToValue(example.get("evaluateRequest").get("item"), ItemState.class);
      var goal = mapper.treeToValue(example.get("evaluateRequest").get("goal"), GoalFilter.class);
      assertThat(example.get("evaluateResponse"))
          .isEqualTo(
              mapper.readTree(
                  mapper.writeValueAsString(new GoalFilterService(all).evaluate(item, goal))));
    }
  }

  @Test
  void unsupportedStartingStateIsNotSuccessEvenForTautologicalCount() {
    var item = item(List.of(), 82, Set.of(ItemState.Condition.UNIDENTIFIED));
    var active =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(null, null));
    var goal =
        new GoalFilter(
            1,
            index.version(),
            active.general(),
            List.of(
                new Group(
                    "count",
                    Type.COUNT,
                    false,
                    new Range(BigDecimal.ZERO, null),
                    active.groups().getFirst().entries())));
    assertThat(service.evaluate(item, goal).groups().getFirst().status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.UNSUPPORTED);
  }

  static final String COLD = "base_cold_damage_resistance_%";
  static final String ALL = "base_resist_all_elements_%";
  final ItemCatalog itemCatalog = ItemCatalogLoader.loadDefault();
  final GoalCatalogIndex index = new GoalCatalogIndex(List.of(itemCatalog));
  final GoalFilterService service = new GoalFilterService(index);

  public static GoalFilter goal(
      String version, String base, String stat, String unit, Range range) {
    return new GoalFilter(
        1,
        version,
        new General(
            base,
            new Range(BigDecimal.ONE, BigDecimal.valueOf(100)),
            List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)),
        List.of(
            new Group(
                "group",
                Type.AND,
                false,
                null,
                List.of(new Entry("row", stat, unit, range, null, false)))));
  }

  ItemState item(List<ModifierInstance> explicits, int level, Set<ItemState.Condition> conditions) {
    var initial = SolarAmulet.initial(itemCatalog);
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        level,
        ItemState.Rarity.RARE,
        initial.implicits(),
        explicits,
        conditions);
  }

  public static ModifierInstance modifier(ItemCatalog catalog, String source, long value) {
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.stats().size() == 1
                        && d.stats().getFirst().id().equals(source)
                        && d.stats().getFirst().contains(value))
            .findFirst()
            .orElseThrow();
    return new ModifierInstance(definition.id(), Map.of(source, value));
  }

  @Test
  void productionPseudoAddsTenAndTwelveOnceAndKeepsRawValues() {
    var item =
        item(
            List.of(modifier(itemCatalog, COLD, 10), modifier(itemCatalog, ALL, 12)), 82, Set.of());
    var goal =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("pseudo", "total_cold_resistance"),
            "percent",
            new Range(BigDecimal.valueOf(22), BigDecimal.valueOf(22)));
    var evaluation = service.evaluate(item, goal);
    assertThat(evaluation.status()).isEqualTo(Status.MATCH);
    assertThat(evaluation.groups().getFirst().entries().getFirst().value())
        .isEqualByComparingTo("22");
    assertThat(item.explicits()).extracting(m -> m.values().get(COLD)).contains(10L);
    var recommendation =
        service.recommend(item, goal, Set.of(), new GoalFilterService.Limits(100, 100, 100));
    assertThat(recommendation.evaluation()).isEqualTo(Status.MATCH);
    assertThat(recommendation.probability().status()).isIn("COMPLETE", "PARTIAL");
    assertThat(recommendation.probability().modelVersion()).isEqualTo(NumericAdditionKernel.MODEL);
    assertThat(recommendation.comparisons()).isNotEmpty();
    assertThat(recommendation.comparisons())
        .allSatisfy(c -> assertThat(c.successLower()).isEqualTo(1));
    assertThat(recommendation.totalSequences()).isPositive();
  }

  @Test
  void existingHigherLevelModifierIsNotDeletedByGenerationEligibility() {
    var cold = modifier(itemCatalog, COLD, 40);
    var item = item(List.of(cold), 1, Set.of());
    var goal =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(BigDecimal.valueOf(40), null));
    assertThat(new ItemStateValidator(itemCatalog).validateForGeneration(item))
        .extracting(ItemStateValidator.Violation::code)
        .contains(ItemStateValidator.Code.ITEM_LEVEL_TOO_LOW);
    assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
    assertThat(item.explicits()).containsExactly(cold);
  }

  @Test
  void unsupportedConditionsNeverTurnIntoAbsentValues() {
    var item = item(List.of(), 82, Set.of(ItemState.Condition.UNIDENTIFIED));
    var cold =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(null, null));
    var entry = cold.groups().getFirst().entries().getFirst();
    var goal =
        new GoalFilter(
            1,
            index.version(),
            cold.general(),
            List.of(new Group("g", Type.IF, false, null, List.of(entry))));
    assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.UNSUPPORTED);
    assertThat(service.evaluate(item, goal).groups().getFirst().entries().getFirst().presence())
        .isEqualTo(Presence.UNKNOWN);
  }

  @Test
  void duplicateModifierAndFamilyAreRejectedBeforeAggregation() {
    var modifier = modifier(itemCatalog, COLD, 10);
    var item = item(List.of(modifier, modifier), 82, Set.of());
    var goal =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(null, null));
    assertThatThrownBy(() -> service.evaluate(item, goal))
        .isInstanceOfSatisfying(
            GoalFilterService.InvalidGoal.class,
            e ->
                assertThat(e.issues())
                    .extracting(Issue::code)
                    .contains("DUPLICATE_MODIFIER", "CONFLICTING_MODIFIERS"));
  }

  @Test
  void unsupportedBaseHasEmptyCatalogWithoutSolarFallback() {
    var catalog =
        service.catalog(new Context(itemCatalog.metadata().snapshotId(), "missing-base", 82));
    assertThat(catalog.stats()).isEmpty();
    assertThat(catalog.context().baseItemId()).isEqualTo("missing-base");
    assertThat(catalog.issues()).extracting(Issue::code).containsExactly("UNSUPPORTED_BASE");
  }

  @Test
  void allSeventeenReviewedBasesLoadAndExposeOnlyTheirSourceStats() {
    var bases = BundledGoalCatalogs.load(ReviewedCatalogTestData.workbench());
    assertThat(bases).hasSize(17);
    var all = new GoalCatalogIndex(bases);
    for (var base : bases) {
      var catalog = all.catalog(new Context(base.metadata().snapshotId(), base.base().id(), 82));
      assertThat(catalog.issues()).isEmpty();
      assertThat(catalog.groupTypes()).hasSize(6);
      for (var stat : catalog.stats()) {
        assertThat(stat.statId()).startsWith("hephaistos:v1:");
        assertThat(stat.sourceUrls()).isNotEmpty();
        if (stat.kind() != GoalCatalog.Kind.PSEUDO)
          assertThat(
                  base.modifiers().values().stream()
                      .anyMatch(
                          m ->
                              m.layer().name().equals(stat.kind().name())
                                  && m.stats().stream()
                                      .anyMatch(s -> stat.sourceStatIds().contains(s.id()))))
              .isTrue();
      }
      System.out.println(
          "GOAL_CATALOG "
              + base.base().name()
              + " total="
              + catalog.stats().size()
              + " supported="
              + catalog.stats().stream()
                  .filter(s -> s.support().evaluation() == Capability.SUPPORTED)
                  .count());
    }
    assertThat(all.version()).isEqualTo(new GoalCatalogIndex(bases.reversed()).version());
    var sampleItem =
        item(
            List.of(modifier(itemCatalog, COLD, 10), modifier(itemCatalog, ALL, 12)), 82, Set.of());
    var sampleContext = new Context(sampleItem.snapshotId(), sampleItem.baseItemId(), 82);
    var sampleGoal =
        goal(
            all.version(),
            sampleItem.baseItemId(),
            GoalCatalogIndex.id("pseudo", "total_cold_resistance"),
            "percent",
            new Range(BigDecimal.valueOf(22), BigDecimal.valueOf(22)));
    var sample =
        Map.of(
            "version",
            1,
            "synthetic",
            false,
            "context",
            sampleContext,
            "catalog",
            all.catalog(sampleContext),
            "evaluateRequest",
            Map.of("item", sampleItem, "goal", sampleGoal),
            "evaluateResponse",
            new GoalFilterService(all).evaluate(sampleItem, sampleGoal));
    try {
      java.nio.file.Files.writeString(
          java.nio.file.Path.of("build/goal-filter-production-example.json"),
          new com.fasterxml.jackson.databind.ObjectMapper()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(sample));
    } catch (java.io.IOException error) {
      throw new java.io.UncheckedIOException(error);
    }
  }

  @Test
  void disabledUnknownStatWarnsButDisabledMalformedContentStillFails() {
    var context = new Context(itemCatalog.metadata().snapshotId(), itemCatalog.base().id(), 82);
    var active =
        goal(
            index.version(),
            context.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(null, null));
    var disabled = new Entry("disabled", "unknown", "percent", new Range(null, null), null, true);
    var g = active.groups().getFirst();
    var good =
        new GoalFilter(
            1,
            index.version(),
            active.general(),
            List.of(
                new Group(
                    g.id(), g.type(), false, null, List.of(g.entries().getFirst(), disabled))));
    assertThat(service.validate(context, good).issues())
        .extracting(Issue::code)
        .contains("UNKNOWN_STAT");
    var badRow =
        new Entry(
            "disabled",
            "unknown",
            "percent",
            new Range(BigDecimal.TEN, BigDecimal.ONE),
            null,
            true);
    var bad =
        new GoalFilter(
            1,
            index.version(),
            active.general(),
            List.of(
                new Group(g.id(), g.type(), false, null, List.of(g.entries().getFirst(), badRow))));
    assertThatThrownBy(() -> service.validate(context, bad))
        .isInstanceOf(GoalFilterService.InvalidGoal.class);
  }
}
