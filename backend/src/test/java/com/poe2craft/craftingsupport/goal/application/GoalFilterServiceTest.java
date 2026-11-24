package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.domain.NumericAdditionKernel;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 아이템 수치 관측과 목표 조건 검증·평가 및 유한 추가 탐색을 조정한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code COLD}: COLD; 초기값 {@code "base_cold_damage_resistance_%"} ({@code String}).
 *   <li>{@code ALL}: ALL; 초기값 {@code "base_resist_all_elements_%"} ({@code String}).
 *   <li>{@code itemCatalog}: 아이템·카탈로그 ({@code ItemCatalog}).
 *   <li>{@code index}: 색인 ({@code GoalCatalogIndex}).
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code GoalFilterService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code compiledDependencyClosurePreservesEveryAstKindAcrossLossAndMerge()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code disabledUnknownContentStillRequiresUnitAndCountMinimumCanProveImpossibleGoal()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code checkedInProductionExampleRetainsItsHistoricalCatalogAndEvaluation()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code unsupportedStartingStateIsNotSuccessEvenForTautologicalCount()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code goal(version, base, stat, unit, range)}: 목표 처리에서 {@code BigDecimal.valueOf}을(를)
 *       사용한다. 입력: {@code version}(버전), {@code base}(베이스), {@code stat}(수치), {@code unit}(단위),
 *       {@code range}(범위). 반환: {@code GoalFilter}.
 *   <li>{@code item(explicits, level, conditions)}: 아이템 처리에서 {@code SolarAmulet.initial}, {@code
 *       initial.snapshotId}을(를) 사용한다. 입력: {@code explicits}(명시옵션목록), {@code level}(레벨), {@code
 *       conditions}(상태조건). 반환: {@code ItemState}.
 *   <li>{@code modifier(catalog, source, value)}: 옵션 처리에서 {@code catalog.modifiers}, {@code
 *       d.layer}을(를) 사용한다. 입력: {@code catalog}(카탈로그), {@code source}(출처), {@code value}(값). 반환:
 *       {@code ModifierInstance}.
 *   <li>{@code retiredTotalsArePreservedWhenReadingHistoricalValues()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code existingHigherLevelModifierIsNotDeletedByGenerationEligibility()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unsupportedConditionsNeverTurnIntoAbsentValues()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code duplicateModifierAndFamilyAreRejectedBeforeAggregation()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code unsupportedBaseHasEmptyCatalogWithoutSolarFallback()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code allSeventeenReviewedBasesLoadAndExposeOnlyTheirSourceStats()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code java.io.UncheckedIOException}.
 *   <li>{@code disabledUnknownStatWarnsButDisabledMalformedContentStillFails()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class GoalFilterServiceTest {
  @Test
  void compiledDependencyClosurePreservesEveryAstKindAcrossLossAndMerge() {
    var empty = item(List.of(), 82, Set.of());
    var sources =
        List.of(
            empty,
            item(List.of(modifier(itemCatalog, COLD, 10)), 82, Set.of()),
            item(
                List.of(modifier(itemCatalog, COLD, 10), modifier(itemCatalog, ALL, 12)),
                82,
                Set.of()),
            item(List.of(modifier(itemCatalog, ALL, 12)), 82, Set.of()));
    for (var type : Type.values()) {
      boolean weighted = type == Type.WEIGHTED_V1 || type == Type.WEIGHTED_V2;
      var entries =
          List.of(
              new Entry(
                  "all-elements",
                  GoalCatalogIndex.id("explicit", ALL),
                  "percent",
                  new Range(BigDecimal.TEN, null),
                  weighted ? BigDecimal.valueOf(2) : null,
                  false),
              new Entry(
                  "direct",
                  GoalCatalogIndex.id("explicit", COLD),
                  "percent",
                  new Range(BigDecimal.valueOf(8), BigDecimal.valueOf(15)),
                  weighted ? BigDecimal.ONE : null,
                  false));
      var goal =
          new GoalFilter(
              1,
              index.version(),
              new General(
                  empty.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
              List.of(
                  new Group(
                      "target",
                      type,
                      false,
                      weighted || type == Type.COUNT ? new Range(BigDecimal.ONE, null) : null,
                      entries),
                  new Group(
                      "preserved-disabled",
                      Type.AND,
                      true,
                      null,
                      List.of(
                          new Entry(
                              "unknown",
                              "retained-unknown-stat",
                              "flat",
                              new Range(null, null),
                              null,
                              true)))));
      var compiled = service.compileEvaluation(empty, goal);
      for (var source : sources) {
        var expected = service.evaluate(source, goal);
        assertThat(compiled.apply(source).status()).isEqualTo(expected.status());
        assertThat(compiled.apply(source).groups()).isEqualTo(expected.groups());
      }
      assertThat(goal.groups().get(1).entries().getFirst().statId())
          .isEqualTo("retained-unknown-stat");
    }
  }

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
  void checkedInProductionExampleRetainsItsHistoricalCatalogAndEvaluation() throws Exception {
    var all =
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(ReviewedCatalogTestData.workbench()),
            GoalDefinitionsLoader.load());
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var stream =
        getClass().getResourceAsStream("/crafting/goalfilter/production-example.json")) {
      var example = mapper.readTree(stream);
      assertThat(example.get("synthetic").booleanValue()).isFalse();
      var context = mapper.treeToValue(example.get("context"), Context.class);
      var actualCatalog = all.catalog(context);
      var expectedCatalog =
          (com.fasterxml.jackson.databind.node.ObjectNode) example.get("catalog").deepCopy();
      // Modifier families/tags are sets; JSON array iteration order is not part of their contract.
      var expectedModifiers =
          mapper.convertValue(
              expectedCatalog.remove("sourceModifiers"),
              new com.fasterxml.jackson.core.type.TypeReference<
                  Map<String, ModifierDefinition>>() {});
      assertThat(expectedModifiers).isEqualTo(actualCatalog.sourceModifiers());
      var actualJson =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              mapper.readTree(mapper.writeValueAsString(actualCatalog));
      actualJson.remove("sourceModifiers");
      assertThat(expectedCatalog).isEqualTo(actualJson);
      var item = mapper.treeToValue(example.get("evaluateRequest").get("item"), ItemState.class);
      var goal = mapper.treeToValue(example.get("evaluateRequest").get("goal"), GoalFilter.class);
      assertThat(example.get("evaluateResponse"))
          .isEqualTo(
              mapper.readTree(
                  mapper.writeValueAsString(
                      new GoalFilterService(all).forHistoricalDisplay().evaluate(item, goal))));
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
  final GoalCatalogIndex index =
      new GoalCatalogIndex(List.of(itemCatalog), GoalDefinitionsLoader.load());
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
  void retiredTotalsArePreservedWhenReadingHistoricalValues() {
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
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    assertThat(service.catalog(context).stats())
        .noneMatch(s -> s.kind() == GoalCatalog.Kind.PSEUDO);
    assertThatThrownBy(() -> service.evaluate(item, goal))
        .isInstanceOfSatisfying(
            GoalFilterService.InvalidGoal.class,
            e -> assertThat(e.issues()).extracting(Issue::code).contains("RETIRED_TOTAL_GOAL"));
    var evaluation = service.forHistoricalDisplay().evaluate(item, goal);
    assertThat(evaluation.status()).isEqualTo(Status.MATCH);
    assertThat(evaluation.groups().getFirst().entries().getFirst().value())
        .isEqualByComparingTo("22");
    assertThat(item.explicits()).extracting(m -> m.values().get(COLD)).contains(10L);
    assertThat(service.forHistoricalDisplay().compile(item, goal).apply(item))
        .isEqualTo(Status.MATCH);
    var direct =
        goal(
            index.version(),
            item.baseItemId(),
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(BigDecimal.TEN, BigDecimal.TEN));
    var recommendation =
        service.recommend(item, direct, Set.of(), new GoalFilterService.Limits(100, 100, 100));
    assertThat(recommendation.evaluation()).isEqualTo(Status.MATCH);
    assertThat(recommendation.probability().status()).isIn("COMPLETE", "PARTIAL");
    assertThat(recommendation.probability().modelVersion()).isEqualTo(NumericAdditionKernel.MODEL);
    assertThat(recommendation.comparisons())
        .isNotEmpty()
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
    var all = new GoalCatalogIndex(bases, GoalDefinitionsLoader.load());
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
    assertThat(all.version())
        .isEqualTo(new GoalCatalogIndex(bases.reversed(), GoalDefinitionsLoader.load()).version());
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
            new GoalFilterService(all).forHistoricalDisplay().evaluate(sampleItem, sampleGoal));
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
