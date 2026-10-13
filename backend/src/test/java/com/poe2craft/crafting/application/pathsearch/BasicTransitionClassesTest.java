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
    var classIndex = new BasicClassIndex(classes);
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
        var indexed = classes.indexedCursor(source, action);
        while (!indexed.complete()) indexed.step();
        assertThat(indexed.result().outcomes()).isEqualTo(projected.outcomes());
        assertThat(indexed.result().unresolved()).isEqualTo(projected.unresolved());
        assertThat(indexed.result().unavailable()).isEqualTo(projected.unavailable());
        int sourceId = classIndex.intern(source);
        var integerCursor = classIndex.cursor(sourceId, action);
        assertThatThrownBy(integerCursor::result).isInstanceOf(IllegalStateException.class);
        while (!integerCursor.complete()) integerCursor.step();
        var integerKernel = integerCursor.result();
        var integerLaw = new HashMap<BasicTransitionClasses.Key, Fraction>();
        integerKernel
            .outcomes()
            .forEach(
                o -> integerLaw.merge(classIndex.key(o.state()), o.probability(), Fraction::add));
        assertThat(integerLaw).isEqualTo(projected.outcomes());
        assertThat(integerKernel.unresolved()).isEqualTo(projected.unresolved());
        assertThat(integerKernel.unavailable()).isEqualTo(projected.unavailable());
        indexed
            .result()
            .witnesses()
            .forEach((key, witness) -> assertThat(classes.key(witness)).isEqualTo(key));
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
    assertThat(classIndex.size()).isEqualTo(grouped.size());
    for (int id = 0; id < classIndex.size(); id++) {
      assertThat(classes.key(classIndex.witness(id))).isEqualTo(classIndex.key(id));
      assertThat(classIndex.intern(classIndex.witness(id))).isEqualTo(id);
    }
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
    var indexed = classes.indexedCursor(variants.get(0), WorkbenchCurrency.EXALTED);
    assertThatThrownBy(indexed::result).isInstanceOf(IllegalStateException.class);
    while (!indexed.complete()) indexed.step();
    assertThat(indexed.result().replay(variants.get(0)).next().state()).isEqualTo(first);
    assertThat(indexed.result().replay(variants.get(1)).next().state()).isEqualTo(second);
    assertThatThrownBy(() -> indexed.result().replay(root))
        .isInstanceOf(IllegalArgumentException.class);
    int variantClass = classIndex.intern(variants.get(0));
    assertThat(classIndex.intern(variants.get(1))).isEqualTo(variantClass);
    assertThat(
            classIndex
                .replay(variantClass, variants.get(0), WorkbenchCurrency.EXALTED)
                .next()
                .state())
        .isEqualTo(first);
    assertThat(
            classIndex
                .replay(variantClass, variants.get(1), WorkbenchCurrency.EXALTED)
                .next()
                .state())
        .isEqualTo(second);
    assertThatThrownBy(() -> classIndex.replay(variantClass, root, WorkbenchCurrency.EXALTED))
        .isInstanceOf(IllegalArgumentException.class);
    verifyIndexedMethod(
        root,
        List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.ANNULMENT),
        classes,
        classIndex,
        graph);
    verifyIndexedMethod(
        variants.get(0),
        List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.CHAOS),
        classes,
        classIndex,
        graph);
    verifyPreparationAndTerminalTransport(root, classes, classIndex, graph);
  }

  private static void verifyPreparationAndTerminalTransport(
      BasicCurrencyState root,
      BasicTransitionClasses classes,
      BasicClassIndex index,
      Map<BasicCurrencyState, FiniteQuotientProof.Node<BasicCurrencyState>> graph) {
    int source = index.classOf(root);
    var input = ConditionalMethodLaw.root(index, Map.of(root, Fraction.ONE));
    var runner =
        new IndexedMethodRunner(
            index,
            source,
            new ExactMethodRunner.Template<>(
                "prepare-then-remove",
                List.of(s -> WorkbenchCurrency.EXALTED),
                List.of(s -> WorkbenchCurrency.ANNULMENT)),
            id -> methodExit(index.key(source), index.key(id)),
            1);
    while (!runner.atBoundary()) runner.step();
    var point = runner.observation();
    assertThat(point.cycles()).isZero();
    assertThat(point.initialUses()).isEqualTo(Map.of(WorkbenchCurrency.EXALTED, Fraction.ONE));
    var direct =
        graph.get(root).actions().stream()
            .filter(a -> a.label().equals(WorkbenchCurrency.EXALTED.name()))
            .findFirst()
            .orElseThrow()
            .outcomes();
    var saved = new HashMap<ConditionalMethodLaw, Map<BasicCurrencyState, Fraction>>();
    for (var exit : ExactMethodRunner.Exit.values())
      for (var target : point.exits().get(exit).keySet()) {
        var law = runner.condition(input, Optional.of(exit), target);
        saved.put(law, conditional(direct, index, target, law.probability()));
      }
    for (var target : point.active().keySet()) {
      var law = runner.condition(input, Optional.empty(), target);
      saved.put(law, conditional(direct, index, target, law.probability()));
    }
    runner.beginCycle();
    while (!runner.atBoundary()) runner.step();
    saved.forEach((law, expected) -> assertThat(expand(law)).isEqualTo(expected));
    assertThat(expand(runner.condition(input, Optional.empty(), source)))
        .isEqualTo(Map.of(root, Fraction.ONE));

    var terminal =
        graph.keySet().stream()
            .filter(s -> classes.key(s).observations().getFirst() == Status.MATCH)
            .findFirst()
            .orElseThrow();
    int goal = index.classOf(terminal);
    var stopped =
        new IndexedMethodRunner(
            index,
            goal,
            new ExactMethodRunner.Template<>(
                "already-complete",
                List.of(s -> WorkbenchCurrency.ANNULMENT),
                List.of(s -> WorkbenchCurrency.EXALTED)),
            id -> methodExit(index.key(goal), index.key(id)),
            1);
    var terminalInput = ConditionalMethodLaw.root(index, Map.of(terminal, Fraction.ONE));
    assertThat(stopped.atBoundary()).isTrue();
    assertThat(stopped.calculatedKernels()).isZero();
    assertThat(
            expand(
                stopped.condition(terminalInput, Optional.of(ExactMethodRunner.Exit.GOAL), goal)))
        .isEqualTo(Map.of(terminal, Fraction.ONE));
    assertThatThrownBy(
            () -> stopped.condition(input, Optional.of(ExactMethodRunner.Exit.GOAL), goal))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Different incoming law");
    var otherIndex = new BasicClassIndex(classes);
    otherIndex.intern(terminal);
    var otherJob = ConditionalMethodLaw.root(otherIndex, Map.of(terminal, Fraction.ONE));
    assertThatThrownBy(
            () -> stopped.condition(otherJob, Optional.of(ExactMethodRunner.Exit.GOAL), goal))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Different incoming law");

    var unavailable =
        new IndexedMethodRunner(
            index,
            source,
            new ExactMethodRunner.Template<>(
                "unavailable", List.of(), List.of(s -> WorkbenchCurrency.TRANSMUTATION)),
            id -> methodExit(index.key(source), index.key(id)),
            1);
    unavailable.beginCycle();
    while (!unavailable.atBoundary()) unavailable.step();
    assertThat(unavailable.observation().cycleUses()).isEmpty();
    assertThat(
            expand(unavailable.condition(input, Optional.of(ExactMethodRunner.Exit.DEAD), source)))
        .isEqualTo(Map.of(root, Fraction.ONE));
  }

  private static Optional<ExactMethodRunner.Exit> methodExit(
      BasicTransitionClasses.Key source, BasicTransitionClasses.Key target) {
    var now = target.observations();
    if (now.getFirst() == Status.MATCH) return Optional.of(ExactMethodRunner.Exit.GOAL);
    if (now.stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(ExactMethodRunner.Exit.UNRESOLVED);
    if (source.observations().equals(now)) return Optional.empty();
    for (int i = 0; i < now.size(); i++)
      if (source.observations().get(i) == Status.MATCH && now.get(i) != Status.MATCH)
        return Optional.of(ExactMethodRunner.Exit.LOSS);
    return Optional.of(ExactMethodRunner.Exit.CHECKPOINT);
  }

  private static Map<Integer, Fraction> indexedLaw(
      Map<BasicCurrencyState, Fraction> law, BasicClassIndex index) {
    var result = new HashMap<Integer, Fraction>();
    law.forEach((state, mass) -> result.merge(index.intern(state), mass, Fraction::add));
    return result;
  }

  /** Independent forward enumeration uses the closed concrete graph, never the class kernels. */
  private static void verifyIndexedMethod(
      BasicCurrencyState source,
      List<WorkbenchCurrency> actions,
      BasicTransitionClasses classes,
      BasicClassIndex index,
      Map<BasicCurrencyState, FiniteQuotientProof.Node<BasicCurrencyState>> graph) {
    var sourceKey = classes.key(source);
    var selectors =
        actions.stream()
            .<java.util.function.Function<Integer, WorkbenchCurrency>>map(a -> s -> a)
            .toList();
    var runner =
        new IndexedMethodRunner(
            index,
            index.intern(source),
            new ExactMethodRunner.Template<>("closed-graph-oracle", List.of(), selectors),
            id -> methodExit(sourceKey, index.key(id)),
            1);
    Map<BasicCurrencyState, Fraction> active = Map.of(source, Fraction.ONE);
    var exits =
        new EnumMap<ExactMethodRunner.Exit, Map<BasicCurrencyState, Fraction>>(
            ExactMethodRunner.Exit.class);
    for (var exit : ExactMethodRunner.Exit.values()) exits.put(exit, new HashMap<>());
    var uses = new HashMap<WorkbenchCurrency, Fraction>();
    long steps = 0;
    for (int cycle = 1; cycle <= 500; cycle++) {
      runner.beginCycle();
      if (cycle == 1)
        assertThatThrownBy(runner::observation).isInstanceOf(IllegalStateException.class);
      while (!runner.atBoundary()) {
        runner.step();
        assertThat(++steps).isLessThan(1000000);
        assertThat(runner.cachedKernels()).isLessThanOrEqualTo(1);
      }
      for (var action : actions) {
        var next = new HashMap<BasicCurrencyState, Fraction>();
        for (var entry : active.entrySet()) {
          var law =
              graph.get(entry.getKey()).actions().stream()
                  .filter(a -> a.label().equals(action.name()))
                  .findFirst();
          if (law.isEmpty()) {
            exits
                .get(ExactMethodRunner.Exit.DEAD)
                .merge(entry.getKey(), entry.getValue(), Fraction::add);
            continue;
          }
          uses.merge(action, entry.getValue(), Fraction::add);
          for (var outcome : law.get().outcomes().entrySet()) {
            var mass = entry.getValue().multiply(outcome.getValue());
            var terminal = methodExit(sourceKey, classes.key(outcome.getKey()));
            if (terminal.isPresent())
              exits.get(terminal.get()).merge(outcome.getKey(), mass, Fraction::add);
            else next.merge(outcome.getKey(), mass, Fraction::add);
          }
        }
        active = next;
      }
      if (Set.of(100, 300, 500).contains(cycle)) {
        var actual = runner.observation();
        for (var exit : ExactMethodRunner.Exit.values())
          assertThat(actual.exits().get(exit)).isEqualTo(indexedLaw(exits.get(exit), index));
        assertThat(actual.active()).isEqualTo(indexedLaw(active, index));
        assertThat(actual.initialUses()).isEmpty();
        assertThat(actual.cycleUses()).isEqualTo(uses);
        assertThat(actual.cycles()).isEqualTo(cycle);
        assertThat(actual.mass()).isEqualTo(Fraction.ONE);
        var input = ConditionalMethodLaw.root(index, Map.of(source, Fraction.ONE));
        for (var exit : ExactMethodRunner.Exit.values())
          for (var target : actual.exits().get(exit).keySet()) {
            var law = runner.condition(input, Optional.of(exit), target);
            var expected = conditional(exits.get(exit), index, target, law.probability());
            assertThat(expand(law)).isEqualTo(expected);
            if (cycle == 100
                && source.item().explicits().isEmpty()
                && exit == ExactMethodRunner.Exit.CHECKPOINT
                && expected.size() > 1)
              verifyConditionalChain(law, expected, classes, index, graph);
          }
        for (var target : actual.active().keySet()) {
          var law = runner.condition(input, Optional.empty(), target);
          assertThat(expand(law)).isEqualTo(conditional(active, index, target, law.probability()));
        }
        assertThatThrownBy(
                () ->
                    runner.condition(
                        input, Optional.of(ExactMethodRunner.Exit.GOAL), index.classOf(source)))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
    assertThat(runner.evictedKernels()).isPositive();
  }

  private static Map<BasicCurrencyState, Fraction> conditional(
      Map<BasicCurrencyState, Fraction> values,
      BasicClassIndex index,
      int target,
      Fraction probability) {
    var result = new HashMap<BasicCurrencyState, Fraction>();
    values.forEach(
        (state, mass) -> {
          if (index.classOf(state) == target) result.put(state, mass.divide(probability));
        });
    return result;
  }

  private static Map<BasicCurrencyState, Fraction> expand(ConditionalMethodLaw law) {
    var expansion = law.expansion();
    assertThatThrownBy(expansion::result).isInstanceOf(IllegalStateException.class);
    for (int step = 0; step < 2000000 && !expansion.complete(); step++) expansion.step();
    assertThat(expansion.complete()).isTrue();
    var result = expansion.result();
    assertThat(result.values().stream().reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    return result;
  }

  private static void verifyConditionalChain(
      ConditionalMethodLaw parent,
      Map<BasicCurrencyState, Fraction> actualInput,
      BasicTransitionClasses classes,
      BasicClassIndex index,
      Map<BasicCurrencyState, FiniteQuotientProof.Node<BasicCurrencyState>> graph) {
    var sourceKey = index.key(parent.classId());
    var next =
        new IndexedMethodRunner(
            index,
            parent.classId(),
            new ExactMethodRunner.Template<>(
                "conditional-follow-up", List.of(), List.of(s -> WorkbenchCurrency.EXALTED)),
            id -> methodExit(sourceKey, index.key(id)),
            1);
    next.beginCycle();
    while (!next.atBoundary()) next.step();
    var point = next.observation();
    assertThat(point.exits().get(ExactMethodRunner.Exit.GOAL)).isNotEmpty();
    for (var target : point.exits().get(ExactMethodRunner.Exit.GOAL).keySet()) {
      var selected = new HashMap<BasicCurrencyState, Fraction>();
      actualInput.forEach(
          (state, mass) -> {
            var action =
                graph.get(state).actions().stream()
                    .filter(a -> a.label().equals(WorkbenchCurrency.EXALTED.name()))
                    .findFirst()
                    .orElseThrow();
            action
                .outcomes()
                .forEach(
                    (outcome, probability) -> {
                      if (index.classOf(outcome) == target)
                        selected.merge(outcome, mass.multiply(probability), Fraction::add);
                    });
          });
      var chained = next.condition(parent, Optional.of(ExactMethodRunner.Exit.GOAL), target);
      var expected = conditional(selected, index, target, chained.probability());
      assertThat(expected).hasSizeGreaterThan(1);
      assertThat(expand(chained)).isEqualTo(expected);
      var singletonInput =
          ConditionalMethodLaw.root(
              index, Map.of(actualInput.keySet().iterator().next(), Fraction.ONE));
      var singleton =
          next.condition(singletonInput, Optional.of(ExactMethodRunner.Exit.GOAL), target);
      assertThat(singleton.probability()).isEqualTo(chained.probability());
      assertThat(expand(singleton)).hasSize(1).isNotEqualTo(expected);
    }
  }

  @Test
  void indexedAdditionPreservesCursorAndMaterializesOnlyGroupedWitnesses() {
    var fixture = new DeferredProductionTest();
    var root = fixture.root();
    var partition = fixture.fixture.goals.rollPartition(root.item(), fixture.goal(47));
    var classes =
        new BasicTransitionClasses(
            fixture.fixture.catalog,
            fixture.primitive,
            partition,
            fixture.fixture.goals.compileEvaluation(root.item(), fixture.goal(47)));
    var raw = fixture.primitive.primitiveCursor(root, WorkbenchCurrency.EXALTED, Set.of());
    var candidate = raw.identity().orElseThrow().branches().getFirst().candidates().getFirst();
    var range = candidate.stats().getFirst();
    var witness = raw.candidateWitness(0, 0, range.min());
    var probability = raw.candidateBandProbability(0, 0, range.min(), range.min());
    assertThat(raw.status()).isEqualTo(BasicCurrencyTransitions.Status.PARTIAL);
    var first = raw.next();
    assertThat(first.state()).isEqualTo(witness);
    assertThat(first.probability()).isEqualTo(probability);
    assertThatThrownBy(() -> raw.candidateBandProbability(0, 0, range.max(), range.min() - 1))
        .isInstanceOf(IllegalArgumentException.class);
    var grouped = fixture.primitive.primitiveCursor(root, WorkbenchCurrency.EXALTED, Set.of());
    long concreteWitnesses = 0;
    while (!grouped.complete()) {
      grouped.nextGrouped(partition::end);
      concreteWitnesses++;
    }
    var indexed = classes.indexedCursor(root, WorkbenchCurrency.EXALTED);
    while (!indexed.complete()) indexed.step();
    assertThat(indexed.materializedWitnesses()).isLessThan(concreteWitnesses);
    assertThat(indexed.result().outcomes().values().stream().reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
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
        var indexed = classes.indexedCursor(first, action);
        while (!indexed.complete()) indexed.step();
        assertThat(
                new ActionLaw(
                    indexed.result().outcomes(),
                    indexed.result().unresolved(),
                    indexed.result().unavailable()))
            .isEqualTo(
                new ActionLaw(
                    a.result().outcomes(), a.result().unresolved(), a.result().unavailable()));
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
