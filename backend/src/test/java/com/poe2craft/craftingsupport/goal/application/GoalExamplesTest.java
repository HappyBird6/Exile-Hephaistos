package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalDefinitions;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/** 현행 장갑과 목걸이 카탈로그에서 목표 범위, 부분 합산, 획득·레벨 자격 및 원래 목표 판정과의 일치를 확인한다. */
class GoalExamplesTest {
  /** 여섯 가지 수치 중 하나만 필요한 경우에도 다섯 번째 이후 카드를 반환하고, 모든 적격 생명력 티어는 한 카드에 담는다. */
  @Test
  void groupsAllEligibleLifeTiersAndReturnsMoreThanFourDifferentCards() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var entries =
        List.of(
            rangeEntry("explicit", "base_maximum_life", "flat", 35, 1000),
            rangeEntry("explicit", "base_maximum_mana", "flat", 1, 1000),
            rangeEntry("explicit", "base_spirit_from_equipment", "flat", 1, 1000),
            rangeEntry("explicit", "base_cold_damage_resistance_%", "percent", 1, 100),
            rangeEntry("explicit", "base_fire_damage_resistance_%", "percent", 1, 100),
            rangeEntry("explicit", "base_lightning_damage_resistance_%", "percent", 1, 100));
    var goal =
        goal(
            index,
            catalog,
            List.of(new Group("any", Type.COUNT, false, new Range(BigDecimal.ONE, null), entries)));
    var result = service.preview(context(catalog, 82), goal, workbench);
    var lifeIds =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.familyIds().equals(Set.of("IncreasedLife"))
                        && d.weight() > 0
                        && d.requiredItemLevel() <= 82
                        && d.stats().getFirst().max() >= 35)
            .map(ModifierDefinition::id)
            .toList();
    assertThat(lifeIds).hasSizeGreaterThan(4);
    assertThat(result.cards().stream().filter(card -> card.item().explicits().size() == 1))
        .hasSizeGreaterThan(4);
    var sources = simulator.modifierSources(82);
    assertThat(
            result.cards().stream()
                .filter(
                    card ->
                        card.item().explicits().size() == 1
                            && lifeIds.contains(card.item().explicits().getFirst().modifierId())))
        .singleElement()
        .satisfies(
            card -> {
              assertThat(card.tierVariants().getFirst()).isEqualTo(card.item());
              assertThat(
                      card.tierVariants().stream()
                          .map(item -> item.explicits().getFirst().modifierId()))
                  .containsExactlyInAnyOrderElementsOf(lifeIds);
              for (var item : card.tierVariants()) {
                var id = item.explicits().getFirst().modifierId();
                assertThat(card.sources().get(id))
                    .isEqualTo(
                        sources.get(id).stream()
                            .filter(source -> source.supportsRarity(item.rarity()))
                            .toList());
                assertThat(result.modifiers().get(id)).isEqualTo(catalog.modifiers().get(id));
                assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
              }
            });
  }

  /** 두 티어 목록의 곱 중 합산 수치가 가능한 두 쌍만 실제 완성 상태로 한 카드에 보관한다. */
  @Test
  void preservesOnlyVerifiedPairsOfLifeAndManaTiers() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var entries =
        List.of(
            rangeEntry("explicit", "base_maximum_life", "flat", 1, 100),
            rangeEntry("explicit", "base_maximum_mana", "flat", 1, 100));
    var score = BigDecimal.valueOf(34);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group("both", Type.AND, false, null, entries),
                new Group(
                    "sum",
                    Type.WEIGHTED_V2,
                    false,
                    new Range(score, score),
                    entries.stream()
                        .map(
                            e ->
                                new Entry(
                                    "score-" + e.id(),
                                    e.statId(),
                                    e.unit(),
                                    e.range(),
                                    e.weight(),
                                    false))
                        .toList())));
    var result = service.preview(context(catalog, 6), goal, workbench);
    assertThat(result.cards())
        .singleElement()
        .satisfies(
            card -> {
              assertThat(
                      card.tierVariants().stream()
                          .map(
                              item ->
                                  item.explicits().stream()
                                      .map(ModifierInstance::modifierId)
                                      .toList()))
                  .containsExactlyInAnyOrder(
                      List.of("amulet:prefix:beryl", "amulet:prefix:healthy"),
                      List.of("amulet:prefix:cobalt", "amulet:prefix:hale"));
              assertThat(card.tierVariants())
                  .allSatisfy(
                      item ->
                          assertThat(service.evaluate(item, goal).status())
                              .isEqualTo(Status.MATCH));
            });
  }

  /** 같은 베이스에 T1 생명력 옵션이 두 개 있으면 T2와 한 카드에 담고 각 정의·수치의 정체성을 보존한다. */
  @Test
  void groupsBothRankOneLifeDefinitionsWithTheOtherEligibleRanks() {
    var catalog = ItemCatalogLoader.loadTopBase("ancestral");
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group(
                    "life",
                    Type.AND,
                    false,
                    null,
                    List.of(rangeEntry("explicit", "base_maximum_life", "flat", 100, 999)))));
    var eligible =
        List.of(
            "stocky-mitts:prefix:athlete-s",
            "rusted-greathelm:prefix:fecund",
            "stocky-mitts:prefix:virile");
    var result = service.preview(context(catalog, 82), goal, workbench);
    assertThat(
            result.cards().stream()
                .filter(
                    card ->
                        card.item().explicits().size() == 1
                            && eligible.contains(card.item().explicits().getFirst().modifierId())))
        .singleElement()
        .satisfies(
            card -> {
              assertThat(
                      card.tierVariants().stream()
                          .map(item -> item.explicits().getFirst().modifierId()))
                  .containsExactlyInAnyOrderElementsOf(eligible);
              assertThat(eligible.stream().map(id -> result.modifiers().get(id).tier()))
                  .containsExactly(1, 1, 2);
            });
  }

  /** 표시용 수치를 테스트 평가기에 등록해도 활·철퇴·육척봉 피해를 서로 같은 옵션의 티어로 합치지 않는다. */
  @Test
  void keepsDifferentJewelEffectsWithIdenticalDisplayRangesApart() {
    var catalog = ItemCatalogLoader.loadBasicJewel("diamond");
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var display = "display_source_value";
    var index =
        new GoalCatalogIndex(
            List.of(catalog),
            new GoalDefinitions(
                1,
                "display-test",
                List.of(new GoalDefinitions.Direct(display, "Test display value", "percent")),
                List.of()));
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group(
                    "display",
                    Type.AND,
                    false,
                    null,
                    List.of(rangeEntry("explicit", display, "percent", 10, 10)))));
    var result = service.preview(context(catalog, 82), goal, workbench);
    var weapons =
        Set.of("diamond:prefix:beating", "diamond:prefix:monk-s", "diamond:prefix:perforating");
    var cards =
        result.cards().stream()
            .filter(
                card ->
                    card.item().explicits().size() == 1
                        && weapons.contains(card.item().explicits().getFirst().modifierId()))
            .toList();
    assertThat(cards)
        .hasSize(3)
        .allSatisfy(card -> assertThat(card.tierVariants()).containsExactly(card.item()));
  }

  /** 일반 획득과 에센스로 얻는 Rotund는 수치 90을 실제로 굴린 상태로 반환하고 미달·레벨 부족 예시는 제외한다. */
  @Test
  void exactLifeRequiresTheRolledValueAndEligibleTier() {
    var catalog = ItemCatalogLoader.loadStocky();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var life = rangeEntry("explicit", "base_maximum_life", "flat", 90, 90);
    var secondLife =
        new Entry("secondLife", life.statId(), life.unit(), new Range(null, null), null, false);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group("empty", Type.AND, false, null, List.of()),
                new Group(
                    "g",
                    Type.COUNT,
                    false,
                    new Range(BigDecimal.valueOf(2), BigDecimal.valueOf(5)),
                    List.of(life, secondLife)),
                new Group(
                    "inactive",
                    Type.WEIGHTED_V2,
                    false,
                    new Range(null, null),
                    List.of(
                        new Entry(
                            "disabled", life.statId(), life.unit(), life.range(), null, true)))));
    var context = context(catalog, 46);
    var sources = new HashMap<>(simulator.modifierSources(46));
    sources.keySet().removeIf(id -> !id.equals("stocky-mitts:prefix:rotund"));
    var examples = service.examples(context, goal, simulator, sources);
    assertThat(examples)
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.explicits())
                  .containsExactly(
                      new ModifierInstance(
                          "stocky-mitts:prefix:rotund", Map.of("base_maximum_life", 90L)));
              var evaluation = service.evaluate(item, goal);
              assertThat(evaluation.status()).isEqualTo(Status.MATCH);
              assertThat(evaluation.groups())
                  .singleElement()
                  .satisfies(
                      group -> {
                        assertThat(group.id()).isEqualTo("g");
                        assertThat(group.count()).isEqualTo(2);
                      });
            });
    assertThat(service.examples(context(catalog, 45), goal, simulator, sources)).isEmpty();
    assertThat(service.examples(context, goal, simulator, Map.of())).isEmpty();
  }

  /** 알려진 직접 수치와 의사 합산 목표를 동시에 충족한 다른 옵션 조합을 찾고 모든 카드에 기존 판정기를 재적용한다. */
  @Test
  void findsDistinctCardsForCombinedDirectAndPseudoGoals() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group(
                    "g",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        rangeEntry("explicit", "base_cold_damage_resistance_%", "percent", 10, 10),
                        rangeEntry("pseudo", "total_cold_resistance", "percent", 21, 29)))));
    var sources = simulator.modifierSources(82);
    var examples = service.examples(context(catalog, 82), goal, simulator, sources);
    assertThat(examples)
        .hasSizeGreaterThan(1)
        .allSatisfy(
            item -> {
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
              assertThat(new ItemStateValidator(catalog).validateForGeneration(item)).isEmpty();
              assertThat(item.explicits()).hasSize(2);
              item.explicits().forEach(m -> assertThat(sources).containsKey(m.modifierId()));
            });
  }

  /** 정확한 접두·접미 목표 6개를 모두 검사해 각 슬롯의 가족·레벨·수치가 호환되는 완성 상태를 확인한다. */
  @Test
  void fillsSixSlotsWithoutViolatingFamilyOrSideLimits() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var entries =
        List.of(
            rangeEntry("explicit", "base_maximum_life", "flat", 90, 90),
            rangeEntry("explicit", "base_maximum_mana", "flat", 75, 75),
            rangeEntry("explicit", "base_spirit_from_equipment", "flat", 35, 35),
            rangeEntry("explicit", "base_cold_damage_resistance_%", "percent", 11, 11),
            rangeEntry("explicit", "base_fire_damage_resistance_%", "percent", 21, 21),
            rangeEntry("explicit", "base_lightning_damage_resistance_%", "percent", 31, 31));
    var goal = goal(index, catalog, List.of(new Group("all", Type.AND, false, null, entries)));
    var examples =
        service.examples(context(catalog, 82), goal, simulator, simulator.modifierSources(82));
    assertThat(examples)
        .isNotEmpty()
        .allSatisfy(
            item -> {
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
              var slots = new ItemStateValidator(catalog).slots(item);
              assertThat(slots.usedPrefixes()).isEqualTo(3);
              assertThat(slots.usedSuffixes()).isEqualTo(3);
            });
  }

  /** 품질 단독 목표는 촉매를 적용할 수 있는 베이스에 한해 구체적인 품질 상태로 판정한다. */
  @Test
  void qualityOnlyUsesConcreteCatalystQuality() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE), 15),
            List.of());
    assertThat(
            service.examples(context(catalog, 82), goal, simulator, simulator.modifierSources(82)))
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.catalystQuality().amount()).isEqualTo(15);
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
            });
  }

  /** 증강물의 치명타 수치 19가 필요한 단일·중복 가중합의 점수와 행 범위를 구성한다. */
  private static Stream<Arguments> criticalStrikeScores() {
    var any = new Range(null, null);
    var first = criticalStrike("first", any, null);
    var second = criticalStrike("second", any, null);
    return Stream.of(
        Arguments.of(Type.WEIGHTED_V2, 19, List.of(first)),
        Arguments.of(Type.WEIGHTED_V2, 38, List.of(criticalStrike("double", any, 2))),
        Arguments.of(Type.WEIGHTED_V1, 38, List.of(first, second)),
        Arguments.of(Type.WEIGHTED_V2, 38, List.of(first, second)),
        Arguments.of(
            Type.WEIGHTED_V2,
            76,
            List.of(
                criticalStrike("positive", any, 3),
                criticalStrike("negative", any, -1),
                criticalStrike("included", new Range(BigDecimal.TEN, null), 2),
                criticalStrike("excluded", new Range(null, BigDecimal.ONE), 4),
                criticalStrike("zero", any, 0))));
  }

  /** 같은 치명타 수치에 독립적인 ID·허용 범위·가중치를 적용한 행을 만든다. */
  private static Entry criticalStrike(String id, Range range, Integer weight) {
    return new Entry(
        id,
        GoalCatalogIndex.id("explicit", "critical_strike_chance_+%"),
        "percent",
        range,
        weight == null ? null : BigDecimal.valueOf(weight),
        false);
  }

  /** 빈 가중치와 명시한 가중치 모두, 증강물을 보존하고 옵션 수치와 가중합의 정확한 경계를 맞춘다. */
  @ParameterizedTest
  @MethodSource("criticalStrikeScores")
  void augmentOnlyWeightedGoalKeepsTheRequiredSocketAndExactScore(
      Type type, int score, List<Entry> entries) {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadStocky());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var target = BigDecimal.valueOf(score);
    var goal =
        goal(
            index,
            catalog,
            List.of(new Group("score", type, false, new Range(target, target), entries)));
    var sources = new HashMap<>(simulator.modifierSources(82));
    sources
        .keySet()
        .removeIf(id -> !id.equals("augment:marksman:MarksmanInfluenceCriticalHitChance1"));
    assertThat(service.examples(context(catalog, 82), goal, simulator, sources))
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.installedAugments()).hasSize(1);
              assertThat(item.augmentSockets()).isEqualTo(1);
              assertThat(item.explicits().getFirst().values().get("critical_strike_chance_+%"))
                  .isEqualTo(19L);
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
            });
  }

  /** 테스트 베이스와 항목 그룹으로 실제 희귀 아이템 목표를 구성한다. */
  private static GoalFilter goal(GoalCatalogIndex index, ItemCatalog catalog, List<Group> groups) {
    return new GoalFilter(
        1,
        index.version(),
        new General(catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        groups);
  }

  /** 원천 수치 ID와 해당 단위의 허용 범위를 갖는 필터 행을 만든다. */
  private static Entry rangeEntry(String layer, String raw, String unit, int min, int max) {
    return new Entry(
        raw,
        GoalCatalogIndex.id(layer, raw),
        unit,
        new Range(BigDecimal.valueOf(min), BigDecimal.valueOf(max)),
        null,
        false);
  }

  /** 스냅샷과 레벨로 예시 요청 문맥을 구성한다. */
  private static Context context(ItemCatalog catalog, int level) {
    return new Context(catalog.metadata().snapshotId(), catalog.base().id(), level);
  }
}
