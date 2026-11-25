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

  /** 절사의 접두·접미 동률 제거 및 등급별 교체 분포를 숫자를 전부 굴린 결과와 대조한다. */
  @Test
  void whittlingIdentityLawPreservesTiesReacquisitionAndGradedWeights() {
    String prefix = "amulet:prefix:hale";
    var start = item(ItemState.Rarity.RARE, prefix, strength);
    assertThat(catalog.find(prefix).orElseThrow().requiredItemLevel())
        .isEqualTo(catalog.find(strength).orElseThrow().requiredItemLevel());
    record Replacement(Set<String> removed, String added) {}
    for (var action :
        List.of(
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.GREATER_CHAOS,
            WorkbenchCurrency.PERFECT_CHAOS)) {
      var omens =
          action == WorkbenchCurrency.CHAOS
              ? Set.of(WorkbenchOmen.WHITTLING)
              : Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.SINISTRAL_ERASURE);
      var move = new RecipeMoves.Move(action.name(), omens, null);
      var byIdentity = WhittlingOutcomeLaw.calculate(workbench, start, move, 1_000);
      var byValue = law.successors(start, action.name(), omens, Set.of(), 20_000);
      assertThat(byIdentity.complete()).isTrue();
      assertThat(byValue.complete()).isTrue();
      var weighted = new HashMap<Replacement, Fraction>();
      for (var o : byValue.outcomes())
        weighted.merge(
            new Replacement(o.removedModifierIds(), WhittlingOutcomeLaw.addedId(start, o)),
            o.probability(),
            Fraction::add);
      var actual = new HashMap<Replacement, Fraction>();
      for (var o : byIdentity.outcomes()) {
        var added = WhittlingOutcomeLaw.addedId(start, o);
        assertThat(o.state().explicits()).hasSize(start.explicits().size());
        assertThat(
                o.state().explicits().stream().filter(m -> !m.modifierId().equals(added)).toList())
            .isEqualTo(
                start.explicits().stream()
                    .filter(m -> !o.removedModifierIds().contains(m.modifierId()))
                    .toList());
        actual.merge(
            new Replacement(o.removedModifierIds(), added), o.probability(), Fraction::add);
      }
      assertThat(actual).isEqualTo(weighted);
      var plan = workbench.recipePlan(start, action, omens);
      for (var branch : plan.branches()) {
        String removed =
            start.explicits().stream()
                .filter(m -> !branch.remaining().explicits().contains(m))
                .findFirst()
                .orElseThrow()
                .modifierId();
        long branchWeight =
            branch.candidates().stream().mapToLong(ModifierDefinition::weight).sum();
        for (var candidate : branch.candidates())
          assertThat(actual.get(new Replacement(Set.of(removed), candidate.id())))
              .isEqualTo(Fraction.of(candidate.weight(), branchWeight * plan.branches().size()));
      }
      assertThat(byIdentity.outcomes()).hasSizeLessThan(byValue.outcomes().size());
      assertThat(actual.values().stream().reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      if (action == WorkbenchCurrency.CHAOS) {
        assertThat(actual)
            .containsKeys(
                new Replacement(Set.of(prefix), prefix),
                new Replacement(Set.of(strength), strength));
        assertThat(
                actual.entrySet().stream()
                    .filter(e -> e.getKey().removed().contains(prefix))
                    .map(Map.Entry::getValue)
                    .reduce(Fraction.ZERO, Fraction::add))
            .isEqualTo(Fraction.of(1, 2));
      } else assertThat(byIdentity.removableModifierIds()).containsExactly(prefix);
      var partial = WhittlingOutcomeLaw.calculate(workbench, start, move, 2);
      assertThat(partial.complete()).isFalse();
      assertThat(partial.outcomes()).isEqualTo(byIdentity.outcomes().subList(0, 2));
      assertThat(partial.removableModifierIds()).isEqualTo(byIdentity.removableModifierIds());
    }
  }

  /** 미공개 옵션의 요구 레벨을 알 수 없는 입력은 계속 절사할 수 있다고 가정하지 않는다. */
  @Test
  void whittlingWithHiddenLevelIsUnknown() {
    var before = AbyssCraftingState.from(item(ItemState.Rarity.RARE, strength), catalog);
    var hidden =
        before.addUnrevealed("hidden", ModifierDefinition.AffixType.PREFIX, catalog).storedItem();
    var move = new RecipeMoves.Move(WorkbenchCurrency.CHAOS, WorkbenchOmen.WHITTLING);
    var result = WhittlingOutcomeLaw.calculate(workbench, hidden, move, 1_000);
    assertThat(result.complete()).isFalse();
    assertThat(result.outcomes()).isEmpty();
  }

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

  /** 결정화 징조가 지정한 방향의 교체 가능한 옵션이 없으면 해당 에센스의 결과는 0이다. 미검증 징조는 별도로 남긴다. */
  @Test
  void directionalEssenceWithoutAMutableSlotIsAProvenMiss() {
    var suffixes = item(ItemState.Rarity.RARE, strength, "amulet:suffix:of-the-starfish");
    var action = WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT;
    var prefix = Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION);
    assertThat(workbench.availability(suffixes, action, prefix).available()).isFalse();
    var blocked = law.successors(suffixes, action.name(), prefix, Set.of(defences), 20, false);
    assertThat(blocked.outcomes()).isEmpty();
    assertThat(blocked.complete()).isTrue();
    var allowed =
        law.successors(
            suffixes,
            action.name(),
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION),
            Set.of(defences),
            200,
            false);
    assertThat(allowed.complete()).isTrue();
    assertThat(allowed.outcomes()).isNotEmpty();
    // 접미를 지울 수 있어도 비어 있지 않은 접두 세 자리에 목표 접두를 추가할 수 없다.
    var fullPrefixes =
        item(ItemState.Rarity.RARE, life, "amulet:prefix:azure", "amulet:prefix:adept-s", strength);
    var noRoom =
        law.successors(
            fullPrefixes,
            action.name(),
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION),
            Set.of(defences),
            20,
            false);
    assertThat(noRoom.complete()).isTrue();
    assertThat(noRoom.outcomes()).isEmpty();
    var unknown =
        law.successors(
            suffixes,
            action.name(),
            Set.of(WorkbenchOmen.SINISTRAL_EXALTATION),
            Set.of(defences),
            20,
            false);
    assertThat(unknown.outcomes()).isEmpty();
    assertThat(unknown.complete()).isFalse();
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

  /** 뼈의 두 방향 중 접두만 성공한다. 공개할 수 있는 목표 옵션의 각 실제 수치는 작업대의 질량을 유지한다. */
  @Test
  void boneSideSelectionPrecedesGuaranteedEligibleModifier() {
    var result =
        law.successors(
            item(ItemState.Rarity.RARE), "Preserved_Collarbone", Set.of(), Set.of(life), 32, false);
    assertThat(result.complete()).isTrue();
    assertThat(result.outcomes())
        .hasSize(15)
        .allMatch(
            o ->
                o.probability().equals(Fraction.of(1, 30))
                    && o.state().desecrated().size() == 1
                    && o.state().explicits().getFirst().modifierId().equals(life));
  }

  /** 여섯 칸이 모두 차면 같은 생명력 계열을 먼저 지워야 목표 티어를 고를 수 있다. 나머지 다섯 제거도 분모에 둔다. */
  @Test
  void fullBoneRetainsAllRemovalChoicesBeforeSelectingTheTarget() {
    var five = new RecipeLightTransitionTest().fiveModifiers();
    var mods = new ArrayList<>(five.explicits());
    mods.add(minimum(catalog.find(life).orElseThrow()));
    var full = workbench.finishBasicCurrency(five, mods);
    var target = "amulet:prefix:virile";
    var law =
        new RecipeTransitions(workbench, true)
            .successors(full, "Preserved_Collarbone", Set.of(), Set.of(target), 100, false);
    assertThat(law.complete()).isTrue();
    assertThat(law.removableModifierIds()).hasSize(6);
    assertThat(law.outcomes())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.removedModifierIds()).containsExactly(life);
              assertThat(o.probability()).isEqualTo(Fraction.of(1, 6));
              assertThat(o.state().explicits()).containsAll(five.explicits());
            });
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

  /** 태양 목걸이 진화에서 정신력은 티어별 수치를 보존한다. 다른 옵션의 모든 수치를 합쳐도 같은 원래 추첨 확률이다. */
  @Test
  void numericSpiritGoalGroupsOnlyIrrelevantTransmutationRolls() {
    String spirit = "base_spirit_from_equipment";
    var start = SolarAmulet.initial(catalog, 82, 15);
    var full = law.successors(start, "TRANSMUTATION", Set.of(), Set.of(), 10000, true);
    var projected =
        new RecipeTransitions(workbench, false, Set.of(spirit))
            .successors(start, "TRANSMUTATION", Set.of(), Set.of(), 10000, true);
    assertThat(full.complete()).isTrue();
    assertThat(projected.complete()).isTrue();
    assertThat(projected.outcomes().size()).isLessThan(full.outcomes().size());
    record Choice(String tier, Long spiritRoll) {}
    var expected = new HashMap<Choice, Fraction>();
    var actual = new HashMap<Choice, Fraction>();
    var witnesses = new HashSet<ItemState>();
    for (var outcome : full.outcomes()) {
      var mod = outcome.state().explicits().getFirst();
      expected.merge(
          new Choice(mod.modifierId(), mod.values().get(spirit)),
          outcome.probability(),
          Fraction::add);
      witnesses.add(outcome.state());
    }
    for (var outcome : projected.outcomes()) {
      var mod = outcome.state().explicits().getFirst();
      assertThat(witnesses).contains(outcome.state());
      actual.merge(
          new Choice(mod.modifierId(), mod.values().get(spirit)),
          outcome.probability(),
          Fraction::add);
    }
    assertThat(actual).isEqualTo(expected);
  }

  /** 신성한 오브가 정신력과 힘을 함께 재굴림할 때도 정신력의 네 수치와 확률은 그대로다. */
  @Test
  void divineKeepsTargetNumericDistributionWhenAnotherRollIsIrrelevant() {
    String spirit = "base_spirit_from_equipment";
    var start = item(ItemState.Rarity.RARE, "amulet:prefix:countess", strength);
    var grouped =
        new RecipeTransitions(workbench, false, Set.of(spirit))
            .successors(start, "DIVINE", Set.of(), Set.of(), 4);
    assertThat(grouped.complete()).isTrue();
    assertThat(grouped.outcomes())
        .hasSize(4)
        .allSatisfy(o -> assertThat(o.probability()).isEqualTo(Fraction.of(1, 4)));
    assertThat(grouped.outcomes())
        .extracting(
            o ->
                o.state().explicits().stream()
                    .filter(m -> m.values().containsKey(spirit))
                    .findFirst()
                    .orElseThrow()
                    .values()
                    .get(spirit))
        .containsExactlyInAnyOrder(47L, 48L, 49L, 50L);
    var full = law.successors(start, "DIVINE", Set.of(), Set.of(), 1000);
    assertThat(full.complete()).isTrue();
    assertThat(full.outcomes().size()).isGreaterThan(grouped.outcomes().size());
    assertThat(full.outcomes().stream().map(RecipeTransitions.Outcome::state).toList())
        .containsAll(grouped.outcomes().stream().map(RecipeTransitions.Outcome::state).toList());
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

  /** 실제 완드의 전용 풀이 장착 전에는 닫혀 있고, 홈을 열고 장착한 뒤에만 한 옵션 카오스 반복이 성립한다. */
  @Test
  void initialChaosCanOpenASocketAndInstallTheRequiredAugmentFirst() {
    var catalog = AugmentCraftingRules.enrich(ItemCatalogLoader.loadTopBase("acrid"));
    var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var rune = "Thruds_Might";
    var targets = AugmentCraftingRules.poolSources(catalog.base().id()).get(rune);
    var target = targets.stream().sorted().findFirst().orElseThrow();
    var placeholder =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.weight() > 0
                        && d.requiredItemLevel() <= 82
                        && !targets.contains(d.id()))
            .min(Comparator.comparing(ModifierDefinition::id))
            .orElseThrow();
    var start =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            catalog.base().hasImplicit()
                ? List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow()))
                : List.of(),
            List.of(minimum(placeholder)),
            Set.of(),
            0);
    var plan = sim.forItem(start).recipePlan(start, WorkbenchCurrency.CHAOS, Set.of());
    assertThat(plan.branches())
        .singleElement()
        .satisfies(b -> assertThat(b.candidates()).noneMatch(d -> d.id().equals(target)));
    for (var frequency : List.of(RouteCards.Frequency.L, RouteCards.Frequency.M)) {
      var opt =
          new RouteCards.Option(
              1, Map.of(target, List.of(new RouteCards.Source("A", rune, frequency))));
      var card = new RouteCards.Card(List.of(opt), 0);
      java.util.function.Predicate<ItemState> done =
          item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(target));
      var search = RecipeRoutePlanner.calculate(sim, start, List.of(card), done, 20000, true, null);
      var found =
          search.routes().stream()
              .filter(
                  r ->
                      r.steps().size() == 3
                          && r.steps().stream()
                              .map(RecipeRoutePlanner.Step::action)
                              .toList()
                              .equals(List.of("ARTIFICER", rune, "CHAOS"))
                          && Integer.valueOf(1000).equals(r.steps().getLast().maxUses())
                          && r.steps().getLast().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var picked =
          found.steps().stream()
              .map(
                  s ->
                      new RecipeRoutePlanner.Step(
                          s.action(),
                          s.omens(),
                          s.acquired(),
                          s.lost(),
                          s.socketIndex(),
                          s.maxUses()))
              .toList();
      var replay =
          RecipeRoutePlanner.evaluateFixed(sim, start, card, done, 20000, true, null, picked);
      assertThat(replay.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps()).isEqualTo(found.steps());
                assertThat(r.probability()).isEqualTo(found.probability());
                var opened = replay.states().get(r.stateIndices().getFirst());
                var installed = replay.states().get(r.stateIndices().get(1));
                var acquired = replay.states().get(r.stateIndices().getLast());
                assertThat(opened.augmentSockets()).isEqualTo(1);
                assertThat(opened.installedAugments()).isEmpty();
                assertThat(installed.installedAugments())
                    .singleElement()
                    .satisfies(a -> assertThat(a.augmentId()).isEqualTo(rune));
                assertThat(installed.explicits()).isEqualTo(start.explicits());
                assertThat(acquired.installedAugments()).isEqualTo(installed.installedAugments());
                assertThat(done.test(acquired)).isTrue();
                var actualPool =
                    sim.forItem(installed)
                        .recipePlan(installed, WorkbenchCurrency.CHAOS, Set.of())
                        .branches()
                        .getFirst()
                        .candidates();
                int weight =
                    actualPool.stream()
                        .filter(d -> d.id().equals(target))
                        .findFirst()
                        .orElseThrow()
                        .weight();
                double first =
                    (double) weight
                        / actualPool.stream().mapToLong(ModifierDefinition::weight).sum();
                double byCap = -Math.expm1(1000 * Math.log1p(-first));
                assertThat(r.steps().getLast().chance().probability())
                    .isCloseTo(first, org.assertj.core.data.Offset.offset(1e-14));
                assertThat(r.steps().getLast().expectedUses())
                    .isCloseTo(byCap / first, org.assertj.core.data.Offset.offset(1e-7));
                assertThat(r.probability())
                    .isCloseTo(byCap, org.assertj.core.data.Offset.offset(1e-12));
              });
    }
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
