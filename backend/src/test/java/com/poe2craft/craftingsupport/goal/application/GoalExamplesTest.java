package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalDefinitions;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
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
    var selected =
        service.preview(context(catalog, 6), goal, workbench, null, "amulet:prefix:hale");
    assertThat(selected.cards())
        .singleElement()
        .satisfies(
            card -> {
              assertThat(card.optionTiers())
                  .containsEntry("amulet:prefix:hale", List.of("amulet:prefix:hale"))
                  .containsEntry("amulet:prefix:cobalt", List.of("amulet:prefix:cobalt"))
                  .doesNotContainKeys("amulet:prefix:healthy", "amulet:prefix:beryl");
              assertThat(service.evaluate(card.item(), goal).status()).isEqualTo(Status.MATCH);
            });
  }

  /** 각 티어 경계에서 떨어진 단 하나의 정수 쌍으로만 가중합을 달성하는 실제 카탈로그 사례다. */
  @Test
  void findsInteriorRollsForANonUnitWeightedSum() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var context = context(catalog, 82);
    var sources = new HashMap<>(simulator.modifierSources(82));
    sources
        .keySet()
        .retainAll(
            Set.of(
                catalog.base().implicitModifierId(), "amulet:prefix:hale", "amulet:prefix:cobalt"));
    var life = "base_maximum_life";
    var mana = "base_maximum_mana";
    var rows =
        List.of(
            new Entry(
                "life",
                GoalCatalogIndex.id("explicit", life),
                "flat",
                new Range(null, null),
                BigDecimal.valueOf(4),
                false),
            new Entry(
                "mana",
                GoalCatalogIndex.id("explicit", mana),
                "flat",
                new Range(null, null),
                BigDecimal.valueOf(7),
                false));
    var sum = BigDecimal.valueOf(206);
    var goal =
        goal(
            index,
            catalog,
            List.of(new Group("sum", Type.WEIGHTED_V2, false, new Range(sum, sum), rows)));
    var states = service.examples(context, goal, simulator, sources);
    assertThat(states)
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.explicits())
                  .containsExactlyInAnyOrder(
                      new ModifierInstance("amulet:prefix:hale", Map.of(life, 13L)),
                      new ModifierInstance("amulet:prefix:cobalt", Map.of(mana, 22L)));
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
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

  /** 품질 단독 목표는 기폭제 종류를 열어 두고, 주입 및 상한 옵션 보유·제거가 가능한 희귀도를 빠짐없이 제공한다. */
  @Test
  void qualityOnlyKeepsMinimumWithoutInventingACatalyst() {
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
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    assertThat(service.preview(context(catalog, 82), goal, workbench).cards())
        .singleElement()
        .satisfies(
            card -> {
              assertThat(card.minimumQuality()).isEqualTo(15);
              assertThat(card.item().catalystQuality()).isNull();
            });
    var higher =
        new GoalFilter(
            1,
            index.version(),
            new General(
                catalog.base().id(),
                new Range(null, null),
                List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.RARE),
                30),
            List.of());
    var cards = service.preview(context(catalog, 82), higher, workbench).cards();
    assertThat(cards)
        .allSatisfy(
            card -> {
              assertThat(card.minimumQuality()).isEqualTo(30);
              assertThat(card.item().catalystQuality()).isNull();
            });
    assertThat(cards)
        .extracting(
            c ->
                List.of(
                    c.item().rarity(),
                    c.item().explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(QualityLimitRules.BREACH_ID))))
        .containsExactlyInAnyOrder(
            List.of(ItemState.Rarity.NORMAL, false),
            List.of(ItemState.Rarity.RARE, false),
            List.of(ItemState.Rarity.RARE, true));
  }

  /** 같은 냉기 원본을 보존하고 기폭제별 20·40% 최종 수치, 티어 고정, 하한과 상한을 대조한다. */
  @Test
  void catalystScenariosUseOneEligibleTypeAndTheActualCap() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var cold = "base_cold_damage_resistance_%";
    var tier = "amulet:suffix:of-the-walrus";
    var raw = new ModifierInstance(tier, Map.of(cold, 30L), true);
    var general =
        new General(catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE), 15);
    for (int target : List.of(30, 36, 42, 43)) {
      var goal =
          new GoalFilter(
              1,
              index.version(),
              general,
              List.of(
                  new Group(
                      "cold",
                      Type.AND,
                      false,
                      null,
                      List.of(rangeEntry("explicit", cold, "percent", target, target)))));
      var cards = service.preview(context(catalog, 82), goal, workbench, raw, tier).cards();
      var types =
          cards.stream()
              .map(c -> c.item().catalystQuality().type())
              .collect(java.util.stream.Collectors.toSet());
      if (target == 43) assertThat(cards).isEmpty();
      else if (target == 30) {
        var expected = EnumSet.allOf(CatalystQuality.Type.class);
        expected.remove(CatalystQuality.Type.TUL);
        assertThat(types).containsExactlyInAnyOrderElementsOf(expected);
      } else {
        assertThat(types).containsExactly(CatalystQuality.Type.TUL);
        assertThat(cards)
            .allSatisfy(
                c ->
                    assertThat(c.item().catalystQuality().amount())
                        .isEqualTo(target == 36 ? 20 : 40));
      }
      for (var card : cards) {
        assertThat(card.minimumQuality()).isEqualTo(15);
        assertThat(card.projectedValues().get(tier).get(cold))
            .isEqualByComparingTo(BigDecimal.valueOf(target));
        assertThat(card.optionTiers().get(tier)).containsExactly(tier);
        assertThat(card.tierVariants())
            .allSatisfy(
                item -> {
                  assertThat(item.explicits()).contains(raw);
                  assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
                });
      }
    }
    var exact36 =
        new GoalFilter(
            1,
            index.version(),
            general,
            List.of(
                new Group(
                    "cold",
                    Type.AND,
                    false,
                    null,
                    List.of(rangeEntry("explicit", cold, "percent", 36, 36)))));
    assertThat(service.preview(context(catalog, 82), exact36, workbench, null, tier).cards())
        .anySatisfy(
            c -> {
              assertThat(c.item().catalystQuality())
                  .isEqualTo(new CatalystQuality(CatalystQuality.Type.TUL, 20));
              assertThat(c.optionTiers().get(tier)).containsExactly(tier);
            });
  }

  /** 하나의 분열 저항에 목표 수치를 적용해, 같은 원본 티어를 올바른 기폭제만 충족하는지 확인한다. */
  @ParameterizedTest
  @MethodSource("resistanceQualities")
  void projectedResistanceCardsUseTheMatchingCatalysts(
      String stat,
      String tier,
      long raw,
      int amount,
      int target,
      Set<CatalystQuality.Type> expected) {
    var catalog =
        tier.startsWith("iron-ring:")
            ? ItemCatalogLoader.loadRing()
            : ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var fracture = new ModifierInstance(tier, Map.of(stat, raw), true);
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                catalog.base().id(),
                new Range(null, null),
                List.of(ItemState.Rarity.RARE),
                Math.min(amount, 40)),
            List.of(
                new Group(
                    "res",
                    Type.AND,
                    false,
                    null,
                    List.of(rangeEntry("explicit", stat, "percent", target, target)))));
    var cards = service.preview(context(catalog, 82), goal, workbench, fracture, tier).cards();
    assertThat(cards.stream().map(c -> c.item().catalystQuality().type()).distinct())
        .containsExactlyInAnyOrderElementsOf(expected);
    assertThat(cards)
        .allSatisfy(
            c -> {
              assertThat(c.optionTiers().get(tier)).containsExactly(tier);
              assertThat(c.projectedValues().get(tier).get(stat))
                  .isEqualByComparingTo(BigDecimal.valueOf(target));
              assertThat(c.tierVariants())
                  .allSatisfy(
                      item -> {
                        assertThat(item.explicits()).contains(fracture);
                        assertThat(item.catalystQuality().amount()).isEqualTo(amount);
                        assertThat(item.qualityInfused()).isTrue();
                        assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
                      });
            });
  }

  /** 네 단일 저항과 세 원소 기폭제가 적용되는 모든 원소 저항의 실제 카탈로그 굴림이다. */
  private static Stream<Arguments> resistanceQualities() {
    return Stream.of(
        Arguments.of(
            "base_cold_damage_resistance_%",
            "amulet:suffix:of-the-walrus", 30L, 50, 45, Set.of(CatalystQuality.Type.TUL)),
        Arguments.of(
            "base_fire_damage_resistance_%",
            "amulet:suffix:of-the-furnace", 30L, 50, 45, Set.of(CatalystQuality.Type.XOPH)),
        Arguments.of(
            "base_lightning_damage_resistance_%",
            "amulet:suffix:of-the-tempest", 30L, 50, 45, Set.of(CatalystQuality.Type.ESH)),
        Arguments.of(
            "base_chaos_damage_resistance_%",
            "amulet:suffix:of-exile", 20L, 50, 30, Set.of(CatalystQuality.Type.CHAYULA)),
        Arguments.of(
            "base_resist_all_elements_%",
            "iron-ring:suffix:of-the-rainbow",
            16L,
            30,
            21,
            Set.of(CatalystQuality.Type.XOPH, CatalystQuality.Type.TUL, CatalystQuality.Type.ESH)));
  }

  /** 작업대에서 기폭제·주입·소멸을 실제 적용한 상태를 동일한 목표의 완성 카드에서 복원한다. */
  @Test
  void cardsIncludeRetainedAndInfusedQualityAfterRemovingTheCap() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var base = SolarAmulet.initial(catalog);
    String cold = "base_cold_damage_resistance_%", tier = "amulet:suffix:of-the-walrus";
    var fracture = new ModifierInstance(tier, Map.of(cold, 30L), true);
    var cap =
        new ModifierInstance(QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))
            .asCrafted();
    var start =
        new ItemState(
            base.snapshotId(),
            base.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            base.implicits(),
            List.of(fracture, cap),
            Set.of());
    var random = new Random(1);
    var typed = simulator.apply(start, WorkbenchCurrency.CATALYST_TUL, Set.of(), random);
    assertThat(typed.applied()).isTrue();
    var infused =
        simulator.apply(typed.state(), WorkbenchCurrency.VAAL_CATALYSING_INFUSER, Set.of(), random);
    assertThat(infused.applied()).isTrue();
    for (var before : List.of(typed.state(), infused.state())) {
      var removed = simulator.apply(before, WorkbenchCurrency.ANNULMENT, Set.of(), random);
      assertThat(removed.applied()).isTrue();
      var item = removed.state();
      assertThat(item.explicits()).containsExactly(fracture);
      assertThat(item.catalystQuality()).isEqualTo(before.catalystQuality());
      int target = before.qualityInfused() ? 45 : 42;
      var goal =
          new GoalFilter(
              1,
              index.version(),
              new General(
                  base.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE), 40),
              List.of(
                  new Group(
                      "cold",
                      Type.AND,
                      false,
                      null,
                      List.of(rangeEntry("explicit", cold, "percent", target, target))),
                  new Group(
                      "cap",
                      Type.NOT,
                      false,
                      null,
                      List.of(
                          rangeEntry("explicit", QualityLimitRules.STAT_ID, "percent", 20, 20)))));
      assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
      assertThat(service.preview(context(catalog, 82), goal, workbench, fracture).cards())
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.item().explicits()).containsExactly(fracture);
                assertThat(c.item().qualityInfused()).isEqualTo(item.qualityInfused());
                assertThat(c.item().catalystQuality()).isEqualTo(item.catalystQuality());
                assertThat(c.projectedValues().get(tier).get(cold))
                    .isEqualByComparingTo(BigDecimal.valueOf(target));
              });
    }
  }

  /** 개수 필터는 한 아이템에 하나의 기폭제만 적용한다. 두 저항에 서로 다른 기폭제의 최댓값을 합치지 않는다. */
  @Test
  void countCannotCombineTwoDifferentCatalystsOnOneItem() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var sources = new LinkedHashMap<>(simulator.modifierSources(82));
    sources
        .keySet()
        .retainAll(
            Set.of(
                catalog.base().implicitModifierId(),
                "amulet:suffix:of-the-walrus",
                "amulet:suffix:of-the-furnace"));
    var rows =
        List.of(
            rangeEntry("explicit", "base_cold_damage_resistance_%", "percent", 36, 36),
            rangeEntry("explicit", "base_fire_damage_resistance_%", "percent", 36, 36));
    for (int minimum : List.of(1, 2)) {
      var goal =
          new GoalFilter(
              1,
              index.version(),
              new General(
                  catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE), 20),
              List.of(
                  new Group(
                      "either",
                      Type.COUNT,
                      false,
                      new Range(BigDecimal.valueOf(minimum), null),
                      rows)));
      var items = service.examples(context(catalog, 82), goal, simulator, sources);
      if (minimum == 2) assertThat(items).isEmpty();
      else
        assertThat(items.stream().map(i -> i.catalystQuality().type()).distinct())
            .containsExactlyInAnyOrder(CatalystQuality.Type.TUL, CatalystQuality.Type.XOPH);
    }
  }

  /** 두 저항이 각각 30인 최상 굴림에서 한 기폭제는 하나만 45로 높인다. Count와 두 가중합의 경계를 확인한다. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      value = Type.class,
      names = {"COUNT", "WEIGHTED_V1", "WEIGHTED_V2"})
  void compoundGoalsCannotCombineDifferentCatalysts(Type type) {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var sources = new LinkedHashMap<>(simulator.modifierSources(82));
    sources
        .keySet()
        .retainAll(
            Set.of(
                catalog.base().implicitModifierId(),
                QualityLimitRules.BREACH_ID,
                "amulet:suffix:of-the-walrus",
                "amulet:suffix:of-the-furnace"));
    boolean count = type == Type.COUNT;
    var rows =
        List.of("base_cold_damage_resistance_%", "base_fire_damage_resistance_%").stream()
            .map(
                stat ->
                    new Entry(
                        stat,
                        GoalCatalogIndex.id("explicit", stat),
                        "percent",
                        count ? new Range(BigDecimal.valueOf(45), null) : new Range(null, null),
                        count ? null : BigDecimal.ONE,
                        false))
            .toList();
    int best = count ? 1 : 75;
    for (int minimum : List.of(best, best + 1)) {
      var goal =
          new GoalFilter(
              1,
              index.version(),
              new General(
                  catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE), 40),
              List.of(
                  new Group(
                      "res", type, false, new Range(BigDecimal.valueOf(minimum), null), rows)));
      var items = service.examples(context(catalog, 82), goal, simulator, sources);
      if (minimum > best) assertThat(items).isEmpty();
      else {
        assertThat(items.stream().map(i -> i.catalystQuality().type()).distinct())
            .containsExactlyInAnyOrder(CatalystQuality.Type.TUL, CatalystQuality.Type.XOPH);
        assertThat(items)
            .allSatisfy(
                i -> {
                  assertThat(i.catalystQuality().amount()).isEqualTo(50);
                  assertThat(i.qualityInfused()).isTrue();
                  assertThat(service.evaluate(i, goal).status()).isEqualTo(Status.MATCH);
                });
      }
    }
  }

  /** 정제 기폭제와 주얼 접미 효과가 같은 굴림에 적용돼야 도달하는 가중합을 실제 아이템으로 검증한다. */
  @Test
  void refinedQualityAndJewelEffectReachWeightedScoreTogether() {
    var catalog = ItemCatalogLoader.loadSapphire();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var stat = "display_cast_speed_percent";
    var index =
        new GoalCatalogIndex(
            List.of(catalog),
            new GoalDefinitions(
                1,
                "cast-speed",
                List.of(new GoalDefinitions.Direct(stat, "Cast Speed", "percent")),
                List.of()));
    var service = new GoalFilterService(index);
    String suffix = "sapphire:suffix:of-enchanting",
        effect = "sapphire:crafted:CraftedJewelSuffixEffect";
    var sources = new LinkedHashMap<>(simulator.modifierSources(82));
    sources.keySet().retainAll(Set.of(suffix, effect));
    var score = BigDecimal.TEN;
    var row =
        new Entry(
            "speed",
            GoalCatalogIndex.id("explicit", stat),
            "percent",
            new Range(null, null),
            BigDecimal.TWO,
            false);
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                catalog.base().id(), new Range(null, null), List.of(ItemState.Rarity.RARE), 20),
            List.of(
                new Group(
                    "score", Type.WEIGHTED_V2, false, new Range(score, score), List.of(row))));
    var items = service.examples(context(catalog, 82), goal, simulator, sources);
    assertThat(items).allSatisfy(item -> assertThat(item.qualityInfused()).isFalse());
    assertThat(items)
        .anySatisfy(
            item -> {
              assertThat(item.catalystQuality().type()).isEqualTo(CatalystQuality.Type.SIBILANT);
              assertThat(item.explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder(suffix, effect);
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
