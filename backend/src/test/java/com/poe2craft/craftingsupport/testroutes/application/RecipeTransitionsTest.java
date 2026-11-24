package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 태양의 목걸이에서 후보 선별 전 확률, 보호 징조, 미관측 공개와 승급 전이를 대조한다. */
class RecipeTransitionsTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator workbench = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final RecipeTransitions law = new RecipeTransitions(workbench);
  final String life = "amulet:prefix:rotund";
  final String strength = "amulet:suffix:of-the-brute";
  final String defences = "amulet:prefix:essence-global-defences";

  /** 명시 옵션 ID마다 카탈로그의 최소 수치를 부여한다. */
  ItemState item(ItemState.Rarity rarity, String... ids) {
    var root = SolarAmulet.initial(catalog, 82, 12);
    var explicit =
        Arrays.stream(ids)
            .map(
                id -> {
                  var d = catalog.find(id).orElseThrow();
                  var values = new HashMap<String, Long>();
                  d.stats().forEach(stat -> values.put(stat.id(), stat.min()));
                  return new ModifierInstance(id, values);
                })
            .toList();
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        rarity,
        root.implicits(),
        explicit,
        Set.of());
  }

  /** 직접 적중만 요청해도 분모는 작업대의 전체 엑잘 풀이다. */
  @Test
  void filteredAdditionRetainsTheOriginalPoolDenominatorAndValues() {
    var empty = item(ItemState.Rarity.RARE);
    var result = law.successors(empty, "EXALTED", Set.of(), Set.of(life), 100, false);
    assertThat(result.complete()).isTrue();
    assertThat(result.outcomes()).hasSize(15);
    var actual =
        result.outcomes().stream()
            .map(RecipeTransitions.Outcome::probability)
            .reduce(Fraction.ZERO, Fraction::add);
    var eligible = workbench.pool(empty, WorkbenchCurrency.EXALTED, null);
    assertThat(actual)
        .isEqualTo(
            Fraction.of(
                catalog.find(life).orElseThrow().weight(),
                eligible.stream().mapToLong(ModifierDefinition::weight).sum()));
    assertThat(result.outcomes())
        .allMatch(o -> o.state().explicits().getFirst().modifierId().equals(life));
  }

  /** 수치 열거가 중단되어도 적법한 제거 후보 전체를 검사한다. */
  @Test
  void allPossibleLossesRemainVisibleAtTheRollBudget() {
    var start = item(ItemState.Rarity.RARE, life, strength);
    var unsafe =
        law.successors(start, "PERFECT_ESSENCE_ENHANCEMENT", Set.of(), Set.of(defences), 1, false);
    assertThat(unsafe.removableModifierIds()).containsExactlyInAnyOrder(life, strength);
    assertThat(unsafe.complete()).isFalse();
    var safe =
        law.successors(
            start,
            "PERFECT_ESSENCE_ENHANCEMENT",
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION),
            Set.of(defences),
            50,
            false);
    assertThat(safe.removableModifierIds()).containsExactly(strength);
    assertThat(safe.outcomes())
        .isNotEmpty()
        .allMatch(o -> o.state().explicits().stream().anyMatch(m -> m.modifierId().equals(life)));
    assertThat(
            safe.outcomes().stream()
                .map(RecipeTransitions.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
  }

  /** 승급 에센스는 신규 일반 명시 옵션을 남긴다. */
  @Test
  void upgradeAddsOrdinaryTarget() {
    var result =
        law.successors(
            item(ItemState.Rarity.MAGIC, strength),
            "GREATER_ESSENCE_BODY",
            Set.of(),
            Set.of(life),
            100,
            false);
    assertThat(result.complete()).isTrue();
    assertThat(result.outcomes())
        .hasSize(15)
        .allMatch(
            o ->
                o.state().rarity() == ItemState.Rarity.RARE
                    && o.state().explicits().stream().noneMatch(ModifierInstance::crafted));
  }

  /** 미관측 훼손 후보는 적격 상태의 조건부 실행만 성립한다. */
  @Test
  void unobservedRevealsAreConditional() {
    var result =
        law.successors(
            item(ItemState.Rarity.RARE), "Preserved_Collarbone", Set.of(), Set.of(life), 32, false);
    assertThat(result.complete()).isFalse();
    assertThat(result.outcomes())
        .isNotEmpty()
        .allMatch(
            o ->
                o.probability() == null
                    && o.state().desecrated().size() == 1
                    && o.state().explicits().getFirst().modifierId().equals(life));
  }

  /** 일반 Divine은 비분열 생명력과 힘 양쪽을 다시 굴린다. */
  @Test
  void divineKeepsTheJointStateDistribution() {
    var start = item(ItemState.Rarity.RARE, life, strength);
    var result = law.successors(start, "DIVINE", Set.of(), Set.of(life, strength), 100);
    assertThat(result.complete()).isTrue();
    assertThat(result.outcomes())
        .hasSize(60)
        .allMatch(o -> o.probability().equals(Fraction.of(1, 60)));
    assertThat(result.outcomes())
        .extracting(RecipeTransitions.Outcome::state)
        .doesNotHaveDuplicates();
  }

  /** 합금에서 징조를 생략해 제거되거나 보존되는 두 경로는 같은 제작 확률의 일부이다. */
  @Test
  void alloyReplacementPlacesACraftedAffix() {
    var target = "amulet:prefix:alloy-maximum-runic-ward";
    var result =
        law.successors(
            item(ItemState.Rarity.RARE, life, strength),
            "RUNIC_ALLOY",
            Set.of(),
            Set.of(target),
            50,
            false);
    assertThat(result.complete()).isTrue();
    assertThat(result.outcomes())
        .hasSize(10)
        .allMatch(
            o ->
                o.probability().equals(Fraction.of(1, 10))
                    && o.state().explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(target) && m.crafted()));
  }

  /** 수치가 없는 목표에서는 티어 내부 결과가 같은 상태로 투영되며 전체 확률을 보존한다. */
  @Test
  void presenceOnlyProjectionAggregatesWithoutChangingTheTierProbability() {
    var empty = item(ItemState.Rarity.RARE);
    var projected =
        new RecipeTransitions(workbench, true)
            .successors(empty, "EXALTED", Set.of(), Set.of(life), 1, false);
    var full = law.successors(empty, "EXALTED", Set.of(), Set.of(life), 25, false);
    assertThat(projected.complete()).isTrue();
    assertThat(projected.outcomes())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.state()).isEqualTo(full.outcomes().getFirst().state());
              assertThat(o.probability())
                  .isEqualTo(
                      full.outcomes().stream()
                          .map(RecipeTransitions.Outcome::probability)
                          .reduce(Fraction.ZERO, Fraction::add));
            });
  }

  /** 장착 전후의 같은 엑잘에서 증강물 전용 풀의 실제 자격이 달라진다. */
  @Test
  void socketPreparationUnlocksTheActualAugmentPool() {
    var wand = AugmentCraftingRules.enrich(ItemCatalogLoader.loadTopBase("acrid"));
    var sim = new WorkbenchSimulator(wand, new CraftingEngine(wand));
    var transitions = new RecipeTransitions(sim);
    var start =
        new ItemState(
            wand.metadata().snapshotId(),
            wand.base().id(),
            82,
            ItemState.Rarity.RARE,
            wand.base().hasImplicit()
                ? List.of(minimum(wand.find(wand.base().implicitModifierId()).orElseThrow()))
                : List.of(),
            List.of(),
            Set.of(),
            0);
    var targets = AugmentCraftingRules.poolSources(wand.base().id()).get("Thruds_Might");
    var before = transitions.successors(start, "EXALTED", Set.of(), targets, 20, false);
    assertThat(before.outcomes()).isEmpty();
    var opened = transitions.successors(start, "ARTIFICER", Set.of(), targets, 20);
    assertThat(opened.outcomes()).hasSize(1);
    var socket = opened.outcomes().getFirst().state();
    var ordinaryRune = transitions.successors(socket, "Perfect_Desert_Rune", Set.of(), targets, 20);
    assertThat(ordinaryRune.outcomes()).hasSize(1);
    var occupied = ordinaryRune.outcomes().getFirst().state();
    assertThat(transitions.successors(occupied, "Thruds_Might", Set.of(), targets, 20).outcomes())
        .isEmpty();
    assertThat(transitions.successors(occupied, "Thruds_Might@1", Set.of(), targets, 20).outcomes())
        .isEmpty();
    var target = targets.stream().sorted().findFirst().orElseThrow();
    var source = new RouteCards.Source("A", "Thruds_Might", RouteCards.Frequency.M);
    var card =
        new RouteCards.Card(List.of(new RouteCards.Option(1, Map.of(target, List.of(source)))), 0);
    var route =
        RecipeRoutePlanner.calculate(
            sim,
            occupied,
            List.of(card),
            state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(target)),
            10000,
            true,
            null);
    assertThat(route.routes())
        .anySatisfy(
            r -> {
              assertThat(r.steps().getFirst().action()).isEqualTo("Thruds_Might");
              assertThat(r.steps().getFirst().socketIndex()).isZero();
              assertThat(r.steps().getFirst().acquired()).isEmpty();
              assertThat(r.order()).containsExactly(1);
              assertThat(r.probability()).isPositive();
            });
    var installed = transitions.successors(occupied, "Thruds_Might@0", Set.of(), targets, 20);
    assertThat(installed.outcomes()).hasSize(1);
    assertThat(
            transitions
                .successors(
                    installed.outcomes().getFirst().state(),
                    "EXALTED",
                    Set.of(),
                    targets,
                    20,
                    false)
                .outcomes())
        .isNotEmpty();
  }

  /** 감정액은 해당 주얼의 교체 후 적격 결과만 분모에 포함한다. */
  @Test
  void jewelLiquidUsesEligibleReplacementLaw() {
    var jewel = ItemCatalogLoader.loadBasicJewel("time-lost-ruby");
    var sim = new WorkbenchSimulator(jewel, new CraftingEngine(jewel));
    var start =
        new ItemState(
            jewel.metadata().snapshotId(),
            jewel.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(minimum(jewel.find(jewel.base().implicitModifierId()).orElseThrow())),
            List.of(
                minimum(
                    jewel.modifiers().values().stream()
                        .filter(
                            m ->
                                m.affixType() == ModifierDefinition.AffixType.PREFIX
                                    && m.weight() > 0)
                        .findFirst()
                        .orElseThrow())),
            Set.of());
    var currency =
        sim.baseMaterialActions().stream()
            .filter(WorkbenchCurrency::isLiquid)
            .findFirst()
            .orElseThrow();
    var result =
        new RecipeTransitions(sim).successors(start, currency.name(), Set.of(), Set.of(), 250);
    assertThat(result.outcomes())
        .isNotEmpty()
        .allMatch(o -> o.state().explicits().getFirst().crafted());
    assertThat(result.complete()).isTrue();
    assertThat(
            result.outcomes().stream()
                .map(RecipeTransitions.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
  }

  /** 옵션 정의의 검증된 최소 수치 인스턴스를 생성한다. */
  private static ModifierInstance minimum(ModifierDefinition definition) {
    var values = new TreeMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(definition.id(), values);
  }
}
