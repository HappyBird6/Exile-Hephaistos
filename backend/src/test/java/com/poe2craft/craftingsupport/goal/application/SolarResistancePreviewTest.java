package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 레벨 82 태양의 목걸이에서 필수 세 옵션·저항 개수·냉기 가중합을 함께 설정한 미리보기를 검증한다. */
class SolarResistancePreviewTest {
  /** 사진에서 선택한 명시 옵션의 원천 수치 ID다. */
  private static final String SPIRIT = "base_spirit_from_equipment",
      PROJECTILE = "projectile_skill_gem_level_+",
      DEFENCES = "global_armour_evasion_energy_shield_+%",
      COLD = "base_cold_damage_resistance_%",
      FIRE = "base_fire_damage_resistance_%",
      LIGHTNING = "base_lightning_damage_resistance_%",
      CHAOS = "base_chaos_damage_resistance_%";

  /** 실제 카탈로그에서 독립적으로 조립한 세 조합과 가능한 티어가 미리보기에서도 빠짐없이 반환되는지 확인한다. */
  @Test
  void findsEachLegalSecondResistanceAndEveryEligibleTier() {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var goal = goal(index, catalog);
    var validator = new ItemStateValidator(catalog);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var base = SolarAmulet.initial(catalog, 82, 15);
    var allSources = simulator.modifierSources(base.itemLevel());
    var others = List.of(FIRE, LIGHTNING, CHAOS);
    var expected = new HashMap<String, Set<String>>();

    for (var other : others) {
      var stats = List.of(SPIRIT, PROJECTILE, DEFENCES, COLD, other);
      var baseline = new LinkedHashMap<String, ModifierInstance>();
      var candidates = new HashMap<String, List<ModifierDefinition>>();
      for (var stat : stats) {
        var tiers =
            catalog.modifiers().values().stream()
                .filter(
                    d ->
                        d.layer() == ModifierDefinition.Layer.EXPLICIT
                            && d.requiredItemLevel() <= base.itemLevel()
                            && d.stats().size() == 1
                            && d.stats().getFirst().id().equals(stat)
                            && (!stat.equals(COLD) || d.stats().getFirst().max() >= 30)
                            && (stat.equals(DEFENCES)
                                ? d.id().equals("amulet:prefix:essence-global-defences")
                                : d.weight() > 0))
                .toList();
        assertThat(tiers).as(stat).isNotEmpty();
        candidates.put(stat, tiers);
        baseline.put(stat, maxRoll(tiers.getFirst()));
      }
      var ids = new HashSet<String>();
      for (var stat : stats) {
        for (var tier : candidates.get(stat)) {
          var changed = new LinkedHashMap<>(baseline);
          changed.put(stat, maxRoll(tier));
          var witness = rare(base, changed.values());
          assertThat(validator.validateForGeneration(witness)).isEmpty();
          assertThat(service.evaluate(witness, goal).status()).isEqualTo(Status.MATCH);
          ids.add(tier.id());
        }
      }
      expected.put(other, ids);
      // T4 냉기 저항의 29와 30은 모두 합법적인 굴림이지만 가중합 경계에서 결과가 달라진다.
      for (long value : List.of(29L, 30L)) {
        baseline.put(
            COLD, new ModifierInstance("amulet:suffix:of-the-walrus", Map.of(COLD, value)));
        var state = rare(base, baseline.values());
        assertThat(validator.validateForGeneration(state)).isEmpty();
        assertThat(service.evaluate(state, goal).status())
            .isEqualTo(value == 30 ? Status.MATCH : Status.NO_MATCH);
      }
    }
    var result =
        service.preview(
            new Context(base.snapshotId(), base.baseItemId(), base.itemLevel()), goal, workbench);
    var actual = new HashMap<String, Set<String>>();
    for (var card : result.cards()) {
      var cardResists = new HashSet<String>();
      var tiersByStat = new HashMap<String, Set<String>>();
      for (var item : card.tierVariants()) {
        assertThat(validator.validateForGeneration(item)).isEmpty();
        assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
        var second =
            others.stream()
                .filter(
                    stat -> item.explicits().stream().anyMatch(m -> m.values().containsKey(stat)))
                .toList();
        assertThat(second).hasSize(1);
        cardResists.add(second.getFirst());
        var ids = actual.computeIfAbsent(second.getFirst(), ignored -> new HashSet<>());
        for (var modifier : item.explicits()) {
          var id = modifier.modifierId();
          ids.add(id);
          for (var stat : modifier.values().keySet())
            tiersByStat.computeIfAbsent(stat, ignored -> new HashSet<>()).add(id);
          assertThat(card.sources().get(id))
              .containsExactlyInAnyOrderElementsOf(
                  allSources.get(id).stream()
                      .filter(source -> source.supportsRarity(item.rarity()))
                      .toList());
        }
      }
      assertThat(cardResists).hasSize(1);
      for (var modifier : card.item().explicits()) {
        var stat = modifier.values().keySet().iterator().next();
        assertThat(card.optionTiers().get(modifier.modifierId()))
            .containsExactlyInAnyOrderElementsOf(tiersByStat.get(stat));
      }
    }
    assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(others);
    for (var other : others)
      assertThat(actual.get(other)).as(other).containsAll(expected.get(other));
    assertThat(allSources.get("amulet:prefix:essence-global-defences"))
        .contains(
            new ModifierAcquisition(
                ModifierAcquisition.Kind.ESSENCE, "PERFECT_ESSENCE_ENHANCEMENT"));
  }

  /** 투사체 접미를 요구하지 않으면 Count 최소 2는 냉기를 포함한 쌍과 세쌍을 모두 허용하고 최대 2는 쌍만 허용한다. */
  @Test
  void includesThreeResistancesUnlessCountHasAMaximumOfTwo() {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var base = SolarAmulet.initial(catalog, 82, 15);
    var context = new Context(base.snapshotId(), base.baseItemId(), base.itemLevel());
    var original = goal(index, catalog);
    var count = original.groups().get(1);
    var weighted = original.groups().get(2);
    var resistances = Set.of(COLD, FIRE, LIGHTNING, CHAOS);
    var pairs = Set.of(Set.of(COLD, FIRE), Set.of(COLD, LIGHTNING), Set.of(COLD, CHAOS));
    var triples =
        Set.of(
            Set.of(COLD, FIRE, LIGHTNING),
            Set.of(COLD, FIRE, CHAOS),
            Set.of(COLD, LIGHTNING, CHAOS));
    for (var range :
        List.of(new Range(BigDecimal.TWO, null), new Range(BigDecimal.TWO, BigDecimal.TWO))) {
      var filter =
          new GoalFilter(
              1,
              index.version(),
              original.general(),
              List.of(new Group(count.id(), Type.COUNT, false, range, count.entries()), weighted));
      var expected = new HashSet<>(pairs);
      if (range.max() == null) expected.addAll(triples);
      var shapes = new HashSet<Set<String>>();
      for (var card : service.preview(context, filter, workbench).cards()) {
        var item = card.item();
        assertThat(service.evaluate(item, filter).status()).isEqualTo(Status.MATCH);
        var shape = new HashSet<String>();
        for (var modifier : item.explicits())
          for (var sourceStat : modifier.values().keySet())
            if (resistances.contains(sourceStat)) shape.add(sourceStat);
        shapes.add(shape);
      }
      assertThat(shapes).as("Count %s", range).containsExactlyInAnyOrderElementsOf(expected);
    }
  }

  /** 가중합이 0이어야 할 때 빈 옵션과 범위를 벗어난 티어를 두 가중합 방식이 반대로 처리하는지 검증한다. */
  @Test
  void zeroWeightedScoreDistinguishesAnAbsentRowFromAnOutOfRangeRoll() {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var service = new GoalFilterService(index);
    var workbench = Mockito.mock(CraftingOperations.class);
    Mockito.when(workbench.simulatorForBase(catalog.base().id())).thenReturn(simulator);
    var context = new Context(catalog.metadata().snapshotId(), catalog.base().id(), 14);
    var zero = new Range(BigDecimal.ZERO, BigDecimal.ZERO);
    // 이 레벨의 냉기 저항은 T7 11–15, T8 6–10이므로 두 티어 모두 행의 최소값 21에 미달한다.
    var cold =
        new Entry(
            "cold",
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(BigDecimal.valueOf(21), null),
            BigDecimal.ONE,
            false);
    for (var type : List.of(Type.WEIGHTED_V1, Type.WEIGHTED_V2)) {
      var filter =
          new GoalFilter(
              1,
              index.version(),
              goal(index, catalog).general(),
              List.of(new Group("zero-score", type, false, zero, List.of(cold))));
      var cards = service.preview(context, filter, workbench).cards();
      assertThat(cards).hasSize(1);
      var card = cards.getFirst();
      if (type == Type.WEIGHTED_V1) assertThat(card.item().explicits()).isEmpty();
      else {
        assertThat(card.item().explicits()).hasSize(1);
        assertThat(card.optionTiers().get(card.item().explicits().getFirst().modifierId()))
            .containsExactlyInAnyOrder("amulet:suffix:of-the-seal", "amulet:suffix:of-the-penguin");
      }
    }
  }

  /** 카탈로그의 직접 옵션마다 최댓값을 채우고 전용 에센스 옵션은 제작 상태로 기록한다. */
  private static ModifierInstance maxRoll(ModifierDefinition definition) {
    var stat = definition.stats().getFirst();
    var modifier = new ModifierInstance(definition.id(), Map.of(stat.id(), stat.max()));
    return definition.weight() == 0 ? modifier.asCrafted() : modifier;
  }

  /** 베이스 고정 옵션·레벨을 보존하고 지정한 명시 옵션으로 희귀 상태를 구성한다. */
  private static ItemState rare(ItemState base, Collection<ModifierInstance> explicits) {
    return new ItemState(
        base.snapshotId(),
        base.baseItemId(),
        base.itemLevel(),
        ItemState.Rarity.RARE,
        base.implicits(),
        List.copyOf(explicits),
        Set.of());
  }

  /** And 세 행, Count 최소 2, Weighted Sum 가중치 1·최소 30의 거래소 목표를 만든다. */
  private static GoalFilter goal(GoalCatalogIndex index, ItemCatalog catalog) {
    var any = new Range(null, null);
    return new GoalFilter(
        1,
        index.version(),
        new General(catalog.base().id(), any, List.of(ItemState.Rarity.RARE)),
        List.of(
            new Group(
                "required",
                Type.AND,
                false,
                null,
                List.of(entry(SPIRIT), entry(PROJECTILE), entry(DEFENCES))),
            new Group(
                "resistances",
                Type.COUNT,
                false,
                new Range(BigDecimal.TWO, null),
                List.of(entry(COLD), entry(FIRE), entry(LIGHTNING), entry(CHAOS))),
            new Group(
                "cold-score",
                Type.WEIGHTED_V1,
                false,
                new Range(BigDecimal.valueOf(30), null),
                List.of(
                    new Entry(
                        "weighted-cold",
                        GoalCatalogIndex.id("explicit", COLD),
                        "percent",
                        any,
                        BigDecimal.ONE,
                        false)))));
  }

  /** 옵션 종류에 대응하는 단위와 빈 행 범위를 갖는 명시 수치 필터를 만든다. */
  private static Entry entry(String stat) {
    return new Entry(
        stat,
        GoalCatalogIndex.id("explicit", stat),
        stat.endsWith("%") ? "percent" : "flat",
        new Range(null, null),
        null,
        false);
  }
}
