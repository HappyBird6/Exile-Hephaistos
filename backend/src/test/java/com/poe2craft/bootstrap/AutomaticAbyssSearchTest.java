package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.*;

@Timeout(90)
class AutomaticAbyssSearchTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();

  @Test
  void fracturedGoalDoesNotBlockFirstMutableChaosButTwoMutableAffixesStillDo() {
    var original = f.lowLifeAndMinionTwo();
    var life = original.explicits().getFirst();
    var locked = new ModifierInstance(life.modifierId(), life.values(), true);
    var root = f.item(ItemState.Rarity.RARE, 82, List.of(locked, original.explicits().get(1)));
    var goal =
        f.goal(
            root,
            Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L, "minion_skill_gem_level_+", 3L));
    var kernel = f.kernel(root, goal);
    var choices =
        new GoalDirectedPlanner(
            kernel, f.evaluation(root, goal), AutomaticCraftingAction.candidates(f.policy));
    assertThat(choices.choices(root, true))
        .anyMatch(
            c ->
                c.initial().isEmpty()
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS);
    assertThat(choices.choices(root, false))
        .noneMatch(c -> c.cycle().stream().anyMatch(a -> a.currency() == WorkbenchCurrency.CHAOS));
    var law = kernel.expand(root, f.action(WorkbenchCurrency.CHAOS), 20000);
    assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(law.outcomes()).isNotEmpty().allMatch(o -> o.item().explicits().contains(locked));
    assertThat(
            law.outcomes().stream()
                .map(AutomaticCraftingKernel.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    var extra =
        f.catalog.modifiers().values().stream()
            .filter(d -> d.stats().stream().anyMatch(s -> s.id().equals("base_maximum_mana")))
            .findFirst()
            .orElseThrow();
    var three =
        f.item(
            ItemState.Rarity.RARE,
            82,
            List.of(locked, original.explicits().get(1), modifier(extra)));
    assertThat(kernel.plan(three, f.action(WorkbenchCurrency.CHAOS)).status())
        .isNotEqualTo(AutomaticCraftingPlan.Status.READY);
    var alone = f.item(ItemState.Rarity.RARE, 82, List.of(locked));
    var aloneGoal =
        f.goal(
            alone,
            Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L, "minion_skill_gem_level_+", 3L));
    assertThat(
            new GoalDirectedPlanner(
                    f.kernel(alone, aloneGoal),
                    f.evaluation(alone, aloneGoal),
                    AutomaticCraftingAction.candidates(f.policy))
                .choices(alone))
        .anyMatch(
            c ->
                c.initial().size() == 1
                    && c.initial().getFirst().currency() == WorkbenchCurrency.EXALTED
                    && c.initial().getFirst().omens().isEmpty()
                    && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS);
  }

  @Test
  void automaticDiscoveryCreatesThreeDesecrationStrategiesWithoutAnyUserActionAndPersistsThem()
      throws Exception {
    var mana =
        f.catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.affixType() == ModifierDefinition.AffixType.PREFIX
                        && d.stats().stream().anyMatch(s -> s.id().equals("base_maximum_mana")))
            .findFirst()
            .orElseThrow();
    var root = f.item(ItemState.Rarity.RARE, 82, List.of(f.life(), modifier(mana)));
    var goal =
        f.goal(
            root,
            Map.of(
                GoalDirectedPlannerScenariosTest.LIFE,
                1L,
                "base_maximum_mana",
                1L,
                "additional_strength",
                5L));
    try (var graph = f.graph(root, goal)) {
      graph.step();
      var routes = graph.nodes().getFirst().abyss();
      assertThat(routes)
          .extracting(r -> r.strategy().kind())
          .contains(
              AbyssStrategyPlanner.Kind.COMPLETE_SIDE,
              AbyssStrategyPlanner.Kind.BLOCK_HIGH_WEIGHT,
              AbyssStrategyPlanner.Kind.SHARE_REMOVAL_RISK);
      assertThat(routes)
          .allSatisfy(
              route -> {
                assertThat(route.prepared().item().slots()).hasSize(3);
                assertThat(route.prepared().item().slots())
                    .anyMatch(s -> s.modifier() == null && s.desecrated());
                assertThat(route.transitions()).isNotEmpty();
                for (var transition : route.transitions()) {
                  assertThat(transition.removals()).isNotEmpty();
                  assertThat(
                          transition.removals().stream()
                              .map(b -> fraction(b.probability()))
                              .reduce(Fraction.ZERO, Fraction::add))
                      .isEqualTo(Fraction.ONE);
                  for (var removal : transition.removals()) {
                    if (!removal.candidates().isEmpty())
                      assertThat(
                              removal.candidates().stream()
                                  .map(c -> fraction(c.selection()))
                                  .reduce(Fraction.ZERO, Fraction::add))
                          .isEqualTo(Fraction.ONE);
                    for (var candidate : removal.candidates()) {
                      candidate.result().item().validate(f.catalog);
                      assertThat(candidate.result().ranges()).containsKey(candidate.modifierId());
                    }
                  }
                }
              });
      var blocker = routes.stream().filter(r -> r.assumed() != null).findFirst().orElseThrow();
      assertThat(blocker.assumed().condition()).isEqualTo("IF_REVEAL_CHOOSES_MODIFIER");
      assertThat(blocker.transitions().getLast().removals().getFirst().candidates())
          .noneMatch(
              c ->
                  c.modifierId()
                      .equals(blocker.strategy().modifierSelection().blockerModifierId()));
      var json = new ObjectMapper();
      var saved =
          json.readValue(
              json.writeValueAsBytes(graph.checkpoint(goal.toString())),
              AutomaticMethodGraph.Checkpoint.class);
      try (var restored =
          AutomaticMethodGraph.restore(
              saved, goal.toString(), f.policy, f.kernel(root, goal), f.evaluation(root, goal))) {
        assertThat(restored.nodes().getFirst().abyss()).isEqualTo(routes);
      }
    }
  }

  @Test
  void fracturedChaosHundredRetriesMatchFiniteSeries() {
    var original = f.lowLifeAndMinionTwo();
    var life = original.explicits().getFirst();
    var locked = new ModifierInstance(life.modifierId(), life.values(), true);
    var root = f.item(ItemState.Rarity.RARE, 82, List.of(locked, original.explicits().get(1)));
    var goal =
        f.goal(
            root,
            Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L, "minion_skill_gem_level_+", 3L));
    var kernel = f.kernel(root, goal);
    var evaluate = f.evaluation(root, goal);
    var law = kernel.expand(root, f.action(WorkbenchCurrency.CHAOS), 20000);
    var p =
        law.outcomes().stream()
            .filter(
                o ->
                    evaluate.apply(o.item()).goal()
                        == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH)
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(Fraction.ZERO, Fraction::add);
    var retry =
        law.outcomes().stream()
            .filter(
                o ->
                    evaluate.apply(o.item()).goal()
                        != com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH)
            .map(AutomaticCraftingKernel.Outcome::probability)
            .reduce(Fraction.ZERO, Fraction::add);
    var expected = Fraction.ZERO;
    var survival = Fraction.ONE;
    for (int i = 0; i < 100; i++) {
      expected = expected.add(survival.multiply(p));
      survival = survival.multiply(retry);
    }
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template(
                "locked-first", List.of(), List.of(f.action(WorkbenchCurrency.CHAOS))),
            kernel,
            state ->
                Optional.ofNullable(
                    evaluate.apply(state).goal()
                            == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH
                        ? ExactMethodRunner.Exit.GOAL
                        : null),
            CertifiedInterval.Precision.standard(),
            20000)) {
      for (int i = 0; i < 100; i++) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(64);
      }
      var actual = CertifiedInterval.zero(64);
      for (var mass : execution.observation().exits().get(ExactMethodRunner.Exit.GOAL).values())
        actual = actual.add(mass);
      assertThat(actual.contains(expected)).isTrue();
      assertThat(execution.observation().mass().contains(Fraction.ONE)).isTrue();
    }
  }

  @Test
  void existingUnrevealedStateContinuesToSixSlotsWithoutLosingFractureOrRangeConstraints() {
    var life = f.life();
    var locked = new ModifierInstance(life.modifierId(), life.values(), true);
    var root = f.item(ItemState.Rarity.RARE, 82, List.of(locked));
    var state =
        new AutomaticAbyssSearch.State(
            AbyssCraftingState.from(root, f.catalog)
                .addUnrevealed("dummy", ModifierDefinition.AffixType.PREFIX, f.catalog),
            Map.of(),
            "UNREVEALED_SLOT");
    var engine =
        new AutomaticAbyssSearch(
            f.catalog,
            f.cfg.workbenchSimulator(f.catalog, f.cfg.craftingEngine(f.catalog)),
            List.of(f.action(WorkbenchCurrency.EXALTED)));
    while (state.item().slots().size() < 6) {
      var transition = engine.expand(state, WorkbenchCurrency.EXALTED, Set.of());
      assertThat(transition.removals()).hasSize(1);
      var candidates = transition.removals().getFirst().candidates();
      assertThat(candidates).isNotEmpty();
      assertThat(
              candidates.stream()
                  .map(c -> fraction(c.selection()))
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      state = candidates.getFirst().result();
      state.item().validate(f.catalog);
      assertThat(state.item().slots()).anyMatch(slot -> locked.equals(slot.modifier()));
      assertThat(state.item().slots())
          .anyMatch(slot -> slot.id().equals("dummy") && slot.modifier() == null);
      assertThat(state.item().slots().stream().map(AbyssCraftingState.Slot::id).distinct().count())
          .isEqualTo(state.item().slots().size());
    }
    assertThat(engine.expand(state, WorkbenchCurrency.EXALTED, Set.of()).removals()).isEmpty();
    var removal = engine.expand(state, WorkbenchCurrency.ANNULMENT, Set.of());
    assertThat(removal.removals()).hasSize(5);
    assertThat(removal.removals())
        .allMatch(
            b ->
                fraction(b.probability())
                    .equals(new Fraction(BigInteger.ONE, BigInteger.valueOf(5))));
    assertThat(removal.removals())
        .allMatch(
            b ->
                b.remaining().item().slots().stream()
                    .anyMatch(slot -> locked.equals(slot.modifier())));
  }

  private static Fraction fraction(AutomaticAbyssSearch.Ratio ratio) {
    return new Fraction(
        BigInteger.valueOf(ratio.numerator()), BigInteger.valueOf(ratio.denominator()));
  }

  private static ModifierInstance modifier(ModifierDefinition d) {
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }
}
