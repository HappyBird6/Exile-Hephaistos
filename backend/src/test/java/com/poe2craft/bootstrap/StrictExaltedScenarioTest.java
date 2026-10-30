package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.*;

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
      assertThat(graph.nodes())
          .noneMatch(
              n ->
                  n.progress().goal()
                      == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH);
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
            o ->
                f.evaluation(alone, aloneGoal).apply(o.item()).goal()
                    == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH);
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
  void intermediateEssenceStateSurvivesWithoutFinishingExalted() {
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
          .noneMatch(
              m ->
                  m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
    }
  }
}
