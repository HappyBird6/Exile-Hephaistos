package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class BasicTransitionClassesTest {
  @Test
  void highestCandidateFallbackBelowTheCurrencyCutoffSplitsUnequalFamilies() {
    var fixture = new MethodSearchCalculationTest();
    var definitions = new ArrayList<>(fixture.catalog.modifiers().values());
    var template = fixture.catalog.modifiers().get("amulet:suffix:of-the-seal");
    for (var family : List.of("x", "y"))
      for (int tier = 0; tier < 2; tier++) {
        int weight = family.equals("x") == (tier == 1) ? 3 : 1;
        definitions.add(
            new ModifierDefinition(
                family + tier,
                family,
                template.layer(),
                template.affixType(),
                Set.of(family),
                tier == 0 ? 1 : 10,
                weight,
                tier + 1,
                template.text(),
                List.of(new ModifierDefinition.StatRange("irrelevant", 1, 1)),
                Set.of(),
                template.sourceUrl()));
      }
    var m = fixture.catalog.metadata();
    var catalog =
        new ItemCatalog(
            new ItemCatalog.Metadata(
                m.snapshotId(),
                m.retrievedAt(),
                m.sourceUrl(),
                m.weightPolicy(),
                m.rawSha256(),
                m.detailsSha256(),
                1,
                5,
                1,
                9),
            fixture.catalog.base(),
            definitions);
    var primitive = new BasicCurrencyTransitions(catalog, "method-search-fixture");
    var root = new BasicCurrencyState(fixture.root().item(), primitive.provenance());
    // An empty conjunction is used only as a domain-level observation that ignores all stats.
    var goal =
        new GoalFilter(
            1,
            "fixture",
            new General(
                root.item().baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
            List.of());
    var partition = RollPartition.compile(catalog, Map.of(), goal, root.item());
    var deferred = new DeferredTransitions(primitive, partition);
    var cursor =
        deferred.primitiveCursor(
            new DeferredItem(root), WorkbenchCurrency.GREATER_EXALTED, Set.of());
    var actual = new HashMap<BasicCurrencyState, Fraction>();
    while (!cursor.complete()) {
      var output = cursor.next();
      for (var i = java.math.BigInteger.ZERO;
          i.compareTo(output.state().cardinality()) < 0;
          i = i.add(java.math.BigInteger.ONE))
        actual.merge(
            output.state().concrete(i),
            output.probability().multiply(output.state().concreteProbability(i)),
            Fraction::add);
    }
    var expected = new HashMap<BasicCurrencyState, Fraction>();
    primitive
        .expand(root, WorkbenchCurrency.GREATER_EXALTED, Set.of(), 1000)
        .outcomes()
        .forEach(o -> expected.merge(o.state(), o.probability(), Fraction::add));
    assertThat(actual).isEqualTo(expected);
    assertThat(
            actual.entrySet().stream()
                .filter(e -> e.getKey().item().explicits().getFirst().modifierId().equals("x1"))
                .map(Map.Entry::getValue))
        .containsExactly(Fraction.of(1, 2));
    assertThat(
            actual.entrySet().stream()
                .filter(e -> e.getKey().item().explicits().getFirst().modifierId().equals("y1"))
                .map(Map.Entry::getValue))
        .containsExactly(Fraction.of(1, 6));
    assertThat(actual.keySet())
        .noneMatch(s -> Set.of("x0", "y0").contains(s.item().explicits().getFirst().modifierId()));
  }

  record ActionLaw(
      Map<BasicTransitionClasses.Key, Fraction> outcomes,
      Fraction unresolved,
      boolean unavailable) {}

  @Test
  void completeConcreteGraphProvesClassesWithoutReplacingEntryLaws() throws Exception {
    var fixture = new MethodSearchCalculationTest();
    var definitions = new ArrayList<>(fixture.catalog.modifiers().values());
    var template = fixture.catalog.modifiers().get("amulet:suffix:of-the-seal");
    for (var family : List.of("anonymous-x", "anonymous-y"))
      definitions.add(
          new ModifierDefinition(
              family,
              family,
              template.layer(),
              template.affixType(),
              Set.of(family),
              1,
              1,
              1,
              template.text(),
              List.of(new ModifierDefinition.StatRange("irrelevant", 1, 1)),
              Set.of(),
              template.sourceUrl()));
    var m = fixture.catalog.metadata();
    var catalog =
        new ItemCatalog(
            new ItemCatalog.Metadata(
                m.snapshotId(),
                m.retrievedAt(),
                m.sourceUrl(),
                m.weightPolicy(),
                m.rawSha256(),
                m.detailsSha256(),
                1,
                3,
                1,
                3),
            fixture.catalog.base(),
            definitions);
    var primitive = new BasicCurrencyTransitions(catalog, "method-search-fixture");
    var root = new BasicCurrencyState(fixture.root().item(), primitive.provenance());
    var index =
        new GoalCatalogIndex(
            List.of(catalog), fixture.fixture.definitions(), "method-search-fixture");
    var known =
        index.knownStats(new Context(root.item().snapshotId(), root.item().baseItemId(), 82));
    var bId = GoalCatalogIndex.id("explicit", template.stats().getFirst().id());
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new General(
                root.item().baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
            List.of(
                new Group(
                    "goal",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        new Entry(
                            "a",
                            GoalCatalogIndex.id("explicit", fixture.fixture.stat),
                            "flat",
                            new Range(BigDecimal.ONE, null),
                            null,
                            false),
                        new Entry(
                            "b",
                            bId,
                            known.get(bId).unit(),
                            new Range(BigDecimal.valueOf(6), null),
                            null,
                            false)))));
    var partition = RollPartition.compile(catalog, known, goal, root.item());
    var evaluate = new GoalFilterService(index).compileEvaluation(root.item(), goal);
    var classes = new BasicTransitionClasses(catalog, primitive, partition, evaluate);
    var graph =
        new LinkedHashMap<BasicCurrencyState, FiniteQuotientProof.Node<BasicCurrencyState>>();
    var laws = new HashMap<List<Object>, ActionLaw>();
    var queue = new ArrayDeque<BasicCurrencyState>();
    queue.add(root);
    while (!queue.isEmpty()) {
      var source = queue.remove();
      if (graph.containsKey(source)) continue;
      var actions = new ArrayList<FiniteQuotientProof.Action<BasicCurrencyState>>();
      for (var action : BasicCurrencyTransitions.supportedActions()) {
        var result = primitive.expand(source, action, Set.of(), 1000);
        var cursor = classes.cursor(source, action);
        while (!cursor.complete()) cursor.step();
        var projected = cursor.result();
        var law =
            new ActionLaw(projected.outcomes(), projected.unresolved(), projected.unavailable());
        var previous = laws.putIfAbsent(List.of(classes.key(source), action), law);
        if (previous != null)
          assertThat(law).as("equivalent full states, %s", action).isEqualTo(previous);
        assertThat(projected.unavailable())
            .isEqualTo(result.status() == BasicCurrencyTransitions.Status.UNAVAILABLE);
        if (projected.unavailable()) continue;
        assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
        var raw = new LinkedHashMap<BasicCurrencyState, Fraction>();
        var expected = new LinkedHashMap<BasicTransitionClasses.Key, Fraction>();
        for (var outcome : result.outcomes()) {
          raw.merge(outcome.state(), outcome.probability(), Fraction::add);
          expected.merge(classes.key(outcome.state()), outcome.probability(), Fraction::add);
          if (!graph.containsKey(outcome.state())) queue.add(outcome.state());
        }
        assertThat(projected.outcomes()).isEqualTo(expected);
        actions.add(
            new FiniteQuotientProof.Action<>(
                action.name(), raw, Map.of(action.name(), Fraction.ONE)));
      }
      graph.put(
          source,
          new FiniteQuotientProof.Node<>(classes.key(source).observations(), true, actions));
      assertThat(graph.size()).isLessThanOrEqualTo(24);
    }
    assertThat(graph).hasSize(24);
    var proof = new FiniteQuotientProof<>(graph);
    var grouped = new HashMap<BasicTransitionClasses.Key, Set<Integer>>();
    graph
        .keySet()
        .forEach(
            s ->
                grouped
                    .computeIfAbsent(classes.key(s), ignored -> new HashSet<>())
                    .add(proof.blocks().get(s)));
    assertThat(grouped).hasSizeLessThan(graph.size());
    assertThat(grouped.values()).allSatisfy(blocks -> assertThat(blocks).hasSize(1));
    var variants =
        graph.keySet().stream()
            .filter(
                s ->
                    s.item().explicits().size() == 1
                        && s.item()
                            .explicits()
                            .getFirst()
                            .modifierId()
                            .equals("amulet:prefix:adept-s"))
            .toList();
    assertThat(variants).hasSize(2);
    assertThat(classes.key(variants.get(0))).isEqualTo(classes.key(variants.get(1)));
    var cursor = classes.cursor(variants.get(0), WorkbenchCurrency.EXALTED);
    assertThatThrownBy(cursor::result).isInstanceOf(IllegalStateException.class);
    while (!cursor.complete()) cursor.step();
    var kernel = cursor.result();
    var first = kernel.replay(variants.get(0)).next().state();
    var second = kernel.replay(variants.get(1)).next().state();
    assertThat(first).isNotEqualTo(second);
    assertThat(first.item().explicits()).contains(variants.get(0).item().explicits().getFirst());
    assertThat(second.item().explicits()).contains(variants.get(1).item().explicits().getFirst());
    assertThatThrownBy(() -> kernel.replay(root)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void realGoalThresholdAndRemovalIdentityRemainDistinct() {
    var fixture = new DeferredProductionTest();
    var root = fixture.root();
    var partition = fixture.fixture.goals.rollPartition(root.item(), fixture.goal(47));
    var classes =
        new BasicTransitionClasses(
            fixture.fixture.catalog,
            fixture.primitive,
            partition,
            fixture.fixture.goals.compileEvaluation(root.item(), fixture.goal(47)));
    var cursor = fixture.primitive.primitiveCursor(root, WorkbenchCurrency.EXALTED, Set.of());
    var groups = new HashMap<BasicTransitionClasses.Key, List<BasicCurrencyState>>();
    while (!cursor.complete()) {
      var outcome = cursor.nextGrouped(partition::end).outcome();
      groups
          .computeIfAbsent(classes.key(outcome.state()), ignored -> new ArrayList<>())
          .add(outcome.state());
    }
    var checked = 0;
    for (var group : groups.values()) {
      if (group.size() < 2) continue;
      var first = group.getFirst();
      var last = group.getLast();
      for (var action : BasicCurrencyTransitions.supportedActions()) {
        var a = classes.cursor(first, action);
        var b = classes.cursor(last, action);
        while (!a.complete()) a.step();
        while (!b.complete()) b.step();
        assertThat(
                new ActionLaw(
                    a.result().outcomes(), a.result().unresolved(), a.result().unavailable()))
            .isEqualTo(
                new ActionLaw(
                    b.result().outcomes(), b.result().unresolved(), b.result().unavailable()));
      }
      checked++;
    }
    assertThat(checked).isGreaterThan(10);
    var spirit =
        groups.keySet().stream()
            .filter(
                k ->
                    k.modifiers()
                        .getFirst()
                        .region()
                        .toString()
                        .contains("base_spirit_from_equipment"))
            .toList();
    assertThat(spirit).hasSizeGreaterThanOrEqualTo(2);
    var example = groups.values().iterator().next().getFirst();
    var item = example.item();
    var modifier = item.explicits().getFirst();
    var fractured =
        new BasicCurrencyState(
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(new ModifierInstance(modifier.modifierId(), modifier.values(), true)),
                item.conditions()),
            example.provenance());
    assertThat(classes.key(fractured)).isNotEqualTo(classes.key(example));
  }
}
