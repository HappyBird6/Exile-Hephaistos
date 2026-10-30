package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CommonDesecrationGraphTest {
  @Test
  void commonChoicesQueueAndNodesCarryDesecrationAndOrdinarySuccessors() {
    var f = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            f.catalog,
            new WorkbenchSimulator(f.catalog, new CraftingEngine(f.catalog)),
            "amulets");
    var root = f.item(ItemState.Rarity.RARE, List.of());
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean matched = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
          return new AutomaticMethodGraph.Progress(
              matched ? Status.MATCH : Status.NO_MATCH,
              List.of(matched ? Status.MATCH : Status.NO_MATCH));
        };
    var choices =
        new GoalDirectedPlanner(kernel, evaluate, AutomaticCraftingAction.candidates(policy))
            .choices(root);
    assertThat(choices).anyMatch(c -> c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
    assertThat(choices)
        .anyMatch(
            c -> c.cycle().getFirst().operation() == AutomaticCraftingAction.Operation.DESECRATE);
    var operation = AutomaticCraftingAction.desecrate(ModifierDefinition.AffixType.PREFIX);
    var law = kernel.expand(root, operation, 100);
    assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(law.outcomes()).hasSize(1);
    assertThat(law.outcomes().getFirst().probability()).isEqualTo(Fraction.ONE);
    var prepared = law.outcomes().getFirst().item();
    assertThat(prepared.explicitSlotCount()).isEqualTo(1);
    assertThat(new ItemStateValidator(f.catalog).slots(prepared).usedPrefixes()).isEqualTo(1);
    var exalt =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var next = kernel.expand(prepared, exalt, 100);
    assertThat(next.status()).isEqualTo(AutomaticCraftingKernel.Status.POLICY_REJECTED);
    assertThat(next.outcomes()).isEmpty();
    try (var graph =
        new AutomaticMethodGraph(
            root,
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            100)) {
      for (int i = 0; i < 1500 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.nodes().getFirst().abyss()).isEmpty();
      assertThat(graph.methods())
          .anyMatch(m -> m.source() == 0 && m.template().cycle().getFirst().operation() != null);
      var preparedNodes =
          graph.nodes().stream().filter(n -> !n.item().desecrated().isEmpty()).toList();
      assertThat(preparedNodes).isNotEmpty();
      assertThat(graph.hasPendingObservation()).isTrue();
      assertThat(graph.complete()).isFalse();
      var strategy = graph.strategy(100);
      for (int i = 0; i < 32; i++) strategy.refine();
      assertThat(strategy.result(preparedNodes.getFirst().id()).status())
          .isNotEqualTo("PROVEN_ZERO");
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  preparedNodes.stream().anyMatch(n -> n.id() == m.source())
                      && m.template().initial().isEmpty()
                      && m.template().cycle().getFirst().currency() == WorkbenchCurrency.EXALTED);
    }
  }

  @Test
  void dummyRemovalExcludesFractureAndObservedSelectionKeepsItsCommonSlot() throws Exception {
    var f = new BasicCurrencyTransitionsTest();
    var locked = new ModifierInstance("a", Map.of("a", 1L), true);
    var root = f.item(ItemState.Rarity.RARE, List.of(locked));
    var policy = removalTestPolicy();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            f.catalog,
            new WorkbenchSimulator(f.catalog, new CraftingEngine(f.catalog)),
            "amulets");
    var prepared =
        kernel
            .expand(
                root, AutomaticCraftingAction.desecrate(ModifierDefinition.AffixType.SUFFIX), 100)
            .outcomes()
            .getFirst()
            .item();
    var removal =
        kernel.expand(
            prepared,
            new AutomaticCraftingAction(
                WorkbenchCurrency.ANNULMENT, Set.of(), AutomaticCurrencyPolicy.Purpose.ORDINARY),
            100);
    assertThat(removal.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(removal.outcomes()).hasSize(1);
    assertThat(removal.outcomes().getFirst().item().explicits()).containsExactly(locked);
    assertThat(removal.outcomes().getFirst().item().desecrated()).isEmpty();
    assertThat(removal.outcomes().getFirst().probability()).isEqualTo(Fraction.ONE);
    var candidate = new ModifierInstance("b", Map.of("b", 3L));
    var selected =
        AbyssCraftingState.from(prepared, f.catalog)
            .observe(
                prepared.desecrated().getFirst().id(),
                List.of(candidate, candidate, candidate),
                f.catalog)
            .choose(0, f.catalog)
            .storedItem();
    assertThat(selected.desecrated().getFirst().modifierId()).isEqualTo("b");
    assertThat(selected.explicits()).contains(locked, candidate);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    assertThat(mapper.readValue(mapper.writeValueAsBytes(selected), ItemState.class))
        .isEqualTo(selected);
    assertThat(mapper.readValue(mapper.writeValueAsBytes(prepared), ItemState.class))
        .isEqualTo(prepared);
    assertThat(
            kernel
                .plan(
                    prepared,
                    new AutomaticCraftingAction(
                        WorkbenchCurrency.CHAOS,
                        Set.of(),
                        AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION))
                .status())
        .isNotEqualTo(AutomaticCraftingPlan.Status.READY);
  }

  @Test
  void commonSixSlotStateSupportsObservedReplacementAndExactProtectedRemoval() {
    var f = new BasicCurrencyTransitionsTest();
    var definitions = new ArrayList<ModifierDefinition>();
    definitions.add(f.implicit);
    for (int i = 0; i < 8; i++)
      definitions.add(
          f.definition(
              "m" + i,
              i < 4 ? ModifierDefinition.AffixType.PREFIX : ModifierDefinition.AffixType.SUFFIX,
              1,
              1,
              i + 1));
    var catalog = f.catalog(definitions);
    var locked = new ModifierInstance("m0", Map.of("m0", 1L), true);
    var state =
        f.item(
            ItemState.Rarity.RARE,
            List.of(
                locked,
                new ModifierInstance("m1", Map.of("m1", 1L)),
                new ModifierInstance("m4", Map.of("m4", 1L)),
                new ModifierInstance("m5", Map.of("m5", 1L)),
                new ModifierInstance("m6", Map.of("m6", 1L))));
    var kernel =
        new AutomaticCraftingKernel(
            removalTestPolicy(),
            catalog,
            new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
            "amulets");
    state =
        kernel
            .expand(
                state, AutomaticCraftingAction.desecrate(ModifierDefinition.AffixType.PREFIX), 100)
            .outcomes()
            .getFirst()
            .item();
    var add =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    assertThat(state.explicitSlotCount()).isEqualTo(6);
    assertThat(state.explicits()).contains(locked);
    assertThat(state.desecrated()).hasSize(1);
    assertThat(kernel.plan(state, add).status()).isNotEqualTo(AutomaticCraftingPlan.Status.READY);
    var remove =
        new AutomaticCraftingAction(
            WorkbenchCurrency.ANNULMENT, Set.of(), AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var removals = kernel.expand(state, remove, 100);
    assertThat(removals.outcomes()).hasSize(5).allMatch(o -> o.item().explicits().contains(locked));
    assertThat(
            removals.outcomes().stream()
                .map(AutomaticCraftingKernel.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    var before = state;
    var candidate =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .filter(d -> before.explicits().stream().noneMatch(m -> m.modifierId().equals(d.id())))
            .map(d -> new ModifierInstance(d.id(), Map.of(d.id(), 1L)))
            .findFirst()
            .orElseThrow();
    var observed =
        AbyssCraftingState.from(state, catalog)
            .observe(
                state.desecrated().getFirst().id(),
                List.of(candidate, candidate, candidate),
                catalog)
            .choose(0, catalog)
            .storedItem();
    assertThat(observed.explicitSlotCount()).isEqualTo(6);
    assertThat(observed.explicits()).containsAll(state.explicits());
    assertThat(observed.desecrated().getFirst().modifierId()).isEqualTo(candidate.modifierId());
    var afterSelection = kernel.expand(observed, remove, 100);
    assertThat(afterSelection.outcomes())
        .hasSize(5)
        .allMatch(o -> o.item().explicits().contains(locked));
    assertThat(afterSelection.outcomes()).anyMatch(o -> o.item().desecrated().isEmpty());
  }

  private static AutomaticCurrencyPolicy removalTestPolicy() {
    var source = AutomaticCurrencyPolicyLoader.load().document();
    assertThat(
            source.entries().stream()
                .filter(e -> e.id().equals("ANNULMENT"))
                .findFirst()
                .orElseThrow()
                .decision())
        .isEqualTo(AutomaticCurrencyPolicy.Decision.EXCLUDED);
    var entries =
        source.entries().stream()
            .map(
                e ->
                    e.id().equals("ANNULMENT")
                        ? new AutomaticCurrencyPolicy.Entry(
                            e.number(),
                            e.id(),
                            e.nameKo(),
                            e.sourceUrl(),
                            e.effectKo(),
                            AutomaticCurrencyPolicy.Decision.USE,
                            "Isolated numeric removal test only",
                            AutomaticCurrencyPolicy.Condition.NONE)
                        : e)
            .toList();
    return new AutomaticCurrencyPolicy(
        new AutomaticCurrencyPolicy.Document(
            source.schemaVersion(),
            source.policyVersion(),
            source.sourceUrl(),
            source.sourceModifiedAt(),
            source.maximumExploredExplicits(),
            entries));
  }
}
