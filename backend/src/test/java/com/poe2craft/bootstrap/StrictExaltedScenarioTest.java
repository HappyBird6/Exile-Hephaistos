package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodExecution;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.craftingsupport.search.domain.GoalDirectedPlanner;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * 엄격파서·엑잘티드·Scenario 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>무징조 에센스의 3개 제거 후보와 최소 수치가 없는 정신력의 5개 티어도 실제 작업대 분포로 검증한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code f}: f ({@code GoalDirectedPlannerScenariosTest}).
 *   <li>{@code SPIRIT}: 정신력; 초기값 {@code "base_spirit_from_equipment"} ({@code String}).
 *   <li>{@code PROJECTILE}: PROJECTILE; 초기값 {@code "projectile_skill_gem_level_+"} ({@code
 *       String}).
 *   <li>{@code DEFENCE}: 방어값; 초기값 {@code "global_armour_evasion_energy_shield_+%"} ({@code
 *       String}).
 *   <li>{@code CHAOS}: 카오스; 초기값 {@code "base_chaos_damage_resistance_%"} ({@code String}).
 *   <li>{@code fracture}: fracture ({@code ModifierInstance}).
 *   <li>{@code projectile}: projectile ({@code ModifierInstance}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code root()}: 시작점 값을 조회·구성한다. 반환: {@code ItemState}.
 *   <li>{@code exalt(purpose, omens)}: 엑잘티드의 입력에 따른 분기·결과를 처리한다. 입력: {@code purpose}(목적), {@code
 *       omens}(징조목록). 반환: {@code AutomaticCraftingAction}.
 *   <li>{@code occupiedMutableSlotRejectsFinishingExaltBeforeAnyProbabilityExpansion()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unreachableNumericGoalStillKeepsStandaloneRarityProgressAndRestoresIt()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code essenceConsumesUnknownSlotWithoutInventingChaosResistance()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code onlyFirstMutableSlotOrWitnessedSideDummyIsAllowed()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code intermediateEssenceStateContinuesWithFinishingExalted()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
@Timeout(90)
class StrictExaltedScenarioTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();
  static final String SPIRIT = "base_spirit_from_equipment",
      PROJECTILE = "projectile_skill_gem_level_+",
      DEFENCE = "global_armour_evasion_energy_shield_+%",
      CHAOS = "base_chaos_damage_resistance_%";
  final ModifierInstance fracture =
      new ModifierInstance("amulet:prefix:countess", Map.of(SPIRIT, 47L), true);
  final ModifierInstance projectile =
      new ModifierInstance("amulet:suffix:of-the-sharpshooter", Map.of(PROJECTILE, 3L));

  ItemState root() {
    return f.item(ItemState.Rarity.RARE, 82, List.of(fracture, projectile));
  }

  AutomaticCraftingAction exalt(AutomaticCurrencyPolicy.Purpose purpose, WorkbenchOmen... omens) {
    return new AutomaticCraftingAction(WorkbenchCurrency.EXALTED, Set.of(omens), purpose);
  }

  @Test
  void occupiedMutableSlotRejectsFinishingExaltBeforeAnyProbabilityExpansion() {
    var source = f.item(ItemState.Rarity.RARE, 82, List.of(f.life()));
    var goal = f.goal(source, Map.of(CHAOS, 1L));
    var kernel = f.kernel(source, goal);
    var action = exalt(AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    for (var result :
        List.of(kernel.expand(source, action, 1), kernel.expand(source, action, 5000))) {
      assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.POLICY_REJECTED);
      assertThat(result.reason()).isEqualTo("EXALTED_REQUIRES_FIRST_AFFIX_OR_PLANNED_REMOVAL");
      assertThat(result.outcomes()).isEmpty();
      assertThat(result.unresolved()).isEqualTo(Fraction.ONE);
    }
    assertThat(
            new GoalDirectedPlanner(
                    kernel,
                    f.evaluation(source, goal),
                    AutomaticCraftingAction.candidates(GoalDirectedPlannerScenariosTest.policy))
                .choices(source))
        .noneMatch(
            c ->
                c.initial().isEmpty()
                    && c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.EXALTED));
  }

  @Test
  void unreachableNumericGoalStillKeepsStandaloneRarityProgressAndRestoresIt() {
    var source = f.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = f.goal(source, Map.of(CHAOS, 999L));
    try (var graph = f.graph(source, goal)) {
      f.exhaust(graph);
      assertThat(graph.nodes()).anyMatch(n -> n.item().rarity() == ItemState.Rarity.MAGIC);
      assertThat(graph.nodes()).noneMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE);
      try (var restored =
          AutomaticMethodGraph.restore(
              graph.checkpoint(goal.toString()),
              goal.toString(),
              f.policy,
              f.kernel(source, goal),
              f.evaluation(source, goal))) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
      }
    }
  }

  @Test
  void essenceConsumesUnknownSlotWithoutInventingChaosResistance() {
    var root =
        root()
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "unrevealed", ModifierDefinition.AffixType.PREFIX, null)));
    var goal = f.goal(root, Map.of(SPIRIT, 47L, PROJECTILE, 3L, DEFENCE, 20L, CHAOS, 1L));
    var kernel = f.kernel(root, goal);
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var plan = kernel.plan(root, action);
    assertThat(plan.branches()).hasSize(1);
    assertThat(plan.branches().getFirst().remaining().explicits())
        .containsExactlyInAnyOrder(fracture, projectile);
    assertThat(plan.branches().getFirst().remaining().desecrated()).isEmpty();
    assertThat(plan.branches().getFirst().candidates())
        .allMatch(d -> d.id().equals("amulet:prefix:essence-global-defences"));
    var result = kernel.expand(root, action, 5000);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(result.outcomes())
        .isNotEmpty()
        .allSatisfy(
            o -> {
              assertThat(o.item().explicits()).contains(fracture, projectile).hasSize(3);
              assertThat(o.item().explicits()).noneMatch(m -> m.values().containsKey(CHAOS));
              assertThat(o.item().desecrated()).isEmpty();
            });
    assertThat(
            result.outcomes().stream()
                .map(AutomaticCraftingKernel.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
  }

  @Test
  void plainEnhancementMustRemoveTheDesecratedSuffixToKeepBothGoals() {
    var spirit = new ModifierInstance("amulet:prefix:baronness", Map.of(SPIRIT, 34L));
    var root =
        f.item(ItemState.Rarity.RARE, 82, List.of(spirit, projectile))
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "unrevealed", ModifierDefinition.AffixType.SUFFIX, null)));
    var requirements = new HashMap<String, Long>();
    requirements.put(SPIRIT, null);
    requirements.put(PROJECTILE, 3L);
    requirements.put(DEFENCE, null);
    var goal = f.goal(root, requirements);
    var kernel = f.kernel(root, goal);
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var result = kernel.expand(root, action, 5000);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    var evaluate = f.evaluation(root, goal);
    var success = Fraction.ZERO;
    var loss = Fraction.ZERO;
    for (var outcome : result.outcomes()) {
      if (evaluate.apply(outcome.item()).goal() == GoalFilter.Status.MATCH) {
        assertThat(outcome.item().explicits()).contains(spirit, projectile);
        assertThat(outcome.item().desecrated()).isEmpty();
        success = success.add(outcome.probability());
      } else loss = loss.add(outcome.probability());
    }
    assertThat(success).isEqualTo(Fraction.of(1, 3));
    assertThat(loss).isEqualTo(Fraction.of(2, 3));
  }

  @Test
  void compoundExaltThenEssencePreservesTheLastCurrencyProbability() {
    var spirit = new ModifierInstance("amulet:prefix:baronness", Map.of(SPIRIT, 34L));
    var source =
        f.item(ItemState.Rarity.RARE, 82, List.of(spirit))
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "unrevealed", ModifierDefinition.AffixType.SUFFIX, null)));
    var requirements = new HashMap<String, Long>();
    requirements.put(SPIRIT, null);
    requirements.put(PROJECTILE, 3L);
    requirements.put(DEFENCE, null);
    var goal = f.goal(source, requirements);
    var evaluate = f.evaluation(source, goal);
    var first =
        exalt(
            AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL,
            WorkbenchOmen.DEXTRAL_EXALTATION);
    var last =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var kernel = f.kernel(source, goal);
    var intermediate = kernel.expand(source, first, 20000);
    var projectileChance =
        intermediate.outcomes().stream()
            .filter(o -> o.item().explicits().contains(projectile))
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(Fraction.ZERO, Fraction::add);
    assertThat(projectileChance).isNotEqualTo(Fraction.ZERO);
    try (var run =
        new AutomaticMethodExecution(
            source,
            new AutomaticMethodExecution.Template("exalt-essence", List.of(first), List.of(last)),
            kernel,
            item ->
                evaluate.apply(item).goal() == GoalFilter.Status.MATCH
                    ? Optional.of(ExactMethodRunner.Exit.GOAL)
                    : Optional.empty(),
            CertifiedInterval.Precision.standard(),
            20000,
            AutomaticMethodExecution.PreparationSuccess.REQUIRE_REPLACEMENT)) {
      while (!run.atBoundary()) run.step(64);
      run.beginCycle();
      while (!run.atBoundary()) run.step(64);
      var goals = run.observation().exits().get(ExactMethodRunner.Exit.GOAL);
      assertThat(goals).isNotEmpty();
      assertThat(goals.keySet())
          .allMatch(state -> Fraction.of(1, 3).equals(state.finalStepChance()));
      var total =
          goals.values().stream().reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(total.contains(projectileChance.multiply(Fraction.of(1, 3)))).isTrue();
    }
  }

  @Test
  void spiritWithoutMinimumIncludesEveryEligibleTierInChaosProbability() {
    var root = f.item(ItemState.Rarity.RARE, 82, List.of(f.life()));
    var goal = f.goal(root, Collections.singletonMap(SPIRIT, null));
    var kernel = f.kernel(root, goal);
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var candidates = kernel.plan(root, action).branches().getFirst().candidates();
    var weights =
        candidates.stream()
            .filter(m -> m.stats().stream().anyMatch(s -> s.id().equals(SPIRIT)))
            .map(ModifierDefinition::weight)
            .toList();
    assertThat(weights).containsExactlyInAnyOrder(500, 500, 500, 500, 400);
    var total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
    var evaluate = f.evaluation(root, goal);
    assertThat(evaluate.apply(root).goal()).isNotEqualTo(GoalFilter.Status.MATCH);
    var result = kernel.expand(root, action, 20000);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    var success =
        result.outcomes().stream()
            .filter(o -> evaluate.apply(o.item()).goal() == GoalFilter.Status.MATCH)
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(Fraction.ZERO, Fraction::add);
    assertThat(success).isEqualTo(Fraction.of(2400, total));
  }

  @Test
  void onlyFirstMutableSlotOrWitnessedSideDummyIsAllowed() {
    var root = root();
    var goal = f.goal(root, Map.of(SPIRIT, 47L, PROJECTILE, 3L, DEFENCE, 20L, CHAOS, 1L));
    var kernel = f.kernel(root, goal);
    assertThat(kernel.plan(root, exalt(AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION)).status())
        .isEqualTo(AutomaticCraftingPlan.Status.UNAVAILABLE);
    var choices =
        new GoalDirectedPlanner(
                kernel, f.evaluation(root, goal), AutomaticCraftingAction.candidates(f.policy))
            .choices(root);
    assertThat(choices)
        .noneMatch(
            c ->
                c.initial().isEmpty()
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
    for (var side : List.of(WorkbenchOmen.SINISTRAL_EXALTATION, WorkbenchOmen.DEXTRAL_EXALTATION))
      assertThat(choices)
          .anyMatch(
              c ->
                  c.initial().size() == 1
                      && c.initial().getFirst().currency() == WorkbenchCurrency.EXALTED
                      && c.initial().getFirst().omens().equals(Set.of(side))
                      && c.cycle().getFirst().currency()
                          == WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT);
    assertThat(choices)
        .noneMatch(
            c ->
                c.initial().stream()
                    .anyMatch(
                        a -> a.currency() == WorkbenchCurrency.EXALTED && a.omens().isEmpty()));
    var alone = f.item(ItemState.Rarity.RARE, 82, List.of(fracture));
    var aloneGoal = f.goal(alone, Map.of(SPIRIT, 47L, PROJECTILE, 3L));
    var first = f.kernel(alone, aloneGoal);
    var law = first.expand(alone, exalt(AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION), 20000);
    assertThat(law.outcomes()).isNotEmpty().allMatch(o -> o.item().explicits().contains(fracture));
    assertThat(law.outcomes())
        .anyMatch(
            o -> f.evaluation(alone, aloneGoal).apply(o.item()).goal() == GoalFilter.Status.MATCH);
    assertThat(
            new GoalDirectedPlanner(
                    first,
                    f.evaluation(alone, aloneGoal),
                    AutomaticCraftingAction.candidates(f.policy))
                .choices(alone))
        .anyMatch(
            c ->
                c.initial().size() == 1
                    && c.initial().getFirst().currency() == WorkbenchCurrency.EXALTED
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS);
  }

  @Test
  void intermediateEssenceStateContinuesWithFinishingExalted() {
    var root =
        root()
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "unrevealed", ModifierDefinition.AffixType.PREFIX, null)));
    var goal = f.goal(root, Map.of(SPIRIT, 47L, PROJECTILE, 3L, DEFENCE, 20L, CHAOS, 1L));
    try (var graph = f.graph(root, goal)) {
      for (int i = 0; i < 800 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.source() == 0
                      && m.template().cycle().getFirst().currency()
                          == WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT);
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.item().explicits().stream().anyMatch(m -> m.values().containsKey(DEFENCE))
                      && n.item().explicits().stream()
                          .noneMatch(m -> m.values().containsKey(CHAOS)));
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.source() != 0
                      && m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.LAST_RESORT_EXALTED
                      && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
    }
  }
}
