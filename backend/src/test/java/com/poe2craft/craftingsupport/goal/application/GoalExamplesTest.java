package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 현행 장갑과 목걸이 카탈로그에서 목표 범위, 부분 합산, 획득·레벨 자격 및 원래 목표 판정과의 일치를 확인한다. */
class GoalExamplesTest {
  /** 일반 획득과 에센스로 얻는 Rotund는 수치 90을 실제로 굴린 상태로 반환하고 미달·레벨 부족 예시는 제외한다. */
  @Test
  void exactLifeRequiresTheRolledValueAndEligibleTier() {
    var catalog = ItemCatalogLoader.loadStocky();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var life = rangeEntry("explicit", "base_maximum_life", "flat", 90, 90);
    var goal = goal(index, catalog, List.of(new Group("g", Type.AND, false, null, List.of(life))));
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
              assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
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

  /** 증강물 전용 풀의 수치는 해당 증강물을 장착한 소켓을 보존하고, 가중합의 수치 경계까지 맞춘다. */
  @Test
  void augmentOnlyWeightedGoalKeepsTheRequiredSocketAndExactScore() {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadStocky());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var entry =
        new Entry(
            "crit",
            GoalCatalogIndex.id("explicit", "critical_strike_chance_+%"),
            "percent",
            new Range(null, null),
            BigDecimal.valueOf(2),
            false);
    var goal =
        goal(
            index,
            catalog,
            List.of(
                new Group(
                    "score",
                    Type.WEIGHTED_V2,
                    false,
                    new Range(BigDecimal.valueOf(38), BigDecimal.valueOf(38)),
                    List.of(entry))));
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
