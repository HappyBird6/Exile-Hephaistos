package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeferredProductionTest {
  final PathSearchServiceTest fixture = new PathSearchServiceTest();
  final BasicCurrencyTransitions primitive =
      new BasicCurrencyTransitions(fixture.catalog, fixture.identity);

  BasicCurrencyState root() {
    var initial = SolarAmulet.initial(fixture.catalog, 82, 15);
    return new BasicCurrencyState(
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(),
            Set.of()),
        primitive.provenance());
  }

  GoalFilter goal(long spirit) {
    return new GoalFilter(
        1,
        fixture.index.version(),
        new General(
            root().item().baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(
            new Group(
                "goal",
                Type.AND,
                false,
                null,
                List.of(
                    new Entry(
                        "a",
                        GoalCatalogIndex.id("explicit", "projectile_skill_gem_level_+"),
                        "flat",
                        new Range(BigDecimal.valueOf(3), null),
                        null,
                        false),
                    new Entry(
                        "b",
                        GoalCatalogIndex.id("explicit", "base_spirit_from_equipment"),
                        "flat",
                        new Range(BigDecimal.valueOf(spirit), null),
                        null,
                        false)))));
  }

  @Test
  void productionDistributionPagesRestoreEveryConcreteRollOnceAndKeepRevisionScope() {
    try (var service =
        new MethodSearchService(
            fixture.catalog,
            fixture.goals,
            fixture.identity,
            fixture.json,
            java.time.Clock.systemUTC(),
            fixture.executor,
            12000)) {
      var created =
          service.create(
              "owner",
              new MethodSearchProtocol.Create(2, "deferred-pages", root(), goal(48), List.of()));
      fixture.queue.remove().run();
      var snapshot = service.get("owner", created.jobId());
      assertThat(snapshot.status()).isEqualTo("PAUSED");
      String graphCursor = null;
      var rootMethods = new ArrayList<String>();
      MethodSearchProtocol.Arrow selected = null;
      do {
        var page = service.graph("owner", snapshot.jobId(), snapshot.revision(), graphCursor);
        page.methods().stream()
            .filter(m -> m.sourceStateId().equals(snapshot.rootStateId()))
            .forEach(
                m ->
                    rootMethods.add(
                        m.initialActions()
                            + ">"
                            + m.cycleActions()
                            + ":"
                            + m.status()
                            + ":"
                            + m.points().stream()
                                .map(MethodSearchProtocol.Point::cycles)
                                .toList()));
        selected =
            page.methods().stream()
                .filter(
                    m ->
                        m.sourceStateId().equals(root().canonicalKey())
                            && m.initialActions().isEmpty()
                            && m.cycleActions().equals(List.of("EXALTED"))
                            && m.points().stream().anyMatch(p -> p.cycles() == 100))
                .findFirst()
                .orElse(selected);
        graphCursor = page.nextCursor();
      } while (selected == null && graphCursor != null);
      assertThat(selected)
          .as(
              "root=%s expected=%s; methods=%s",
              snapshot.rootStateId(), root().canonicalKey(), rootMethods)
          .isNotNull();
      var expected = eager(new DeferredItem(root()), WorkbenchCurrency.EXALTED);
      var firstGraph = service.graph("owner", snapshot.jobId(), snapshot.revision(), null);
      var repeatedGraph = service.graph("owner", snapshot.jobId(), snapshot.revision(), null);
      assertThat(repeatedGraph).isEqualTo(firstGraph);
      for (int i = 0; i < firstGraph.methods().size(); i++)
        assertThat(repeatedGraph.methods().get(i)).isSameAs(firstGraph.methods().get(i));
      var actual = new LinkedHashMap<BasicCurrencyState, Fraction>();
      String cursor = null, firstToken = null;
      int pages = 0;
      do {
        var page =
            service.distribution(
                "owner", snapshot.jobId(), snapshot.revision(), selected.id(), 100, cursor);
        assertThat(page)
            .isEqualTo(
                service.distribution(
                    "owner", snapshot.jobId(), snapshot.revision(), selected.id(), 100, cursor));
        assertThat(page.states()).hasSizeLessThanOrEqualTo(48);
        for (var row : page.states()) {
          var state = new BasicCurrencyState(row.item(), primitive.provenance());
          assertThat(actual).doesNotContainKey(state);
          assertThat(row.stateId()).isEqualTo(state.canonicalKey());
          actual.put(
              state,
              new Fraction(
                  new BigInteger(row.probability().numerator()),
                  new BigInteger(row.probability().denominator())));
        }
        cursor = page.nextCursor();
        if (pages++ == 0) firstToken = cursor;
      } while (cursor != null);
      assertThat(actual).isEqualTo(expected);
      assertThat(pages).isGreaterThan(1);
      var token = firstToken;
      var methodId = selected.id();
      assertThatThrownBy(
              () ->
                  service.distribution(
                      "owner", snapshot.jobId(), snapshot.revision(), methodId, 300, token))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      service.mutate(
          "owner",
          snapshot.jobId(),
          new MethodSearchProtocol.Mutation(2, "stop", snapshot.revision(), "CANCEL", null));
      assertThat(service.graph("owner", snapshot.jobId(), snapshot.revision(), null))
          .isEqualTo(firstGraph);
      assertThat(
              service
                  .distribution(
                      "owner", snapshot.jobId(), snapshot.revision(), methodId, 100, token)
                  .states())
          .isNotEmpty();
    }
  }

  @Test
  void realSolarCellsRestoreTheFullWeightedLawAndSplitGoalThresholds() {
    var partition = fixture.goals.rollPartition(root().item(), goal(48));
    var deferred = new DeferredTransitions(primitive, partition);
    var cursor =
        deferred.primitiveCursor(new DeferredItem(root()), WorkbenchCurrency.EXALTED, Set.of());
    var cells = new LinkedHashMap<DeferredItem, Fraction>();
    while (!cursor.complete()) {
      var next = cursor.next();
      cells.merge(next.state(), next.probability(), Fraction::add);
    }
    var full = eager(new DeferredItem(root()), WorkbenchCurrency.EXALTED);
    assertThat(restore(cells)).isEqualTo(full);
    assertThat(cells.size()).isLessThan(full.size());
    assertThat(cells.keySet()).anyMatch(c -> !c.tiers().isEmpty());
    var evaluate = fixture.goals.compileEvaluation(root().item(), goal(48));
    for (var cell : cells.keySet()) {
      var expected = statuses(evaluate.apply(cell.item()));
      for (var offset = BigInteger.ZERO;
          offset.compareTo(cell.cardinality()) < 0;
          offset = offset.add(BigInteger.ONE))
        assertThat(statuses(evaluate.apply(cell.concrete(offset).item()))).isEqualTo(expected);
    }
    var spirit =
        cells.keySet().stream()
            .filter(c -> c.rolls().containsKey("amulet:prefix:countess"))
            .findFirst()
            .orElseThrow();
    assertThat(spirit.rolls().get("amulet:prefix:countess").min()).isEqualTo(48);
    assertThat(spirit.rolls().get("amulet:prefix:countess").max()).isEqualTo(50);
    var weightedTier =
        cells.keySet().stream()
            .filter(c -> !c.tiers().isEmpty())
            .min(Comparator.comparing(DeferredItem::cardinality))
            .orElseThrow();
    var jointFamily =
        cells.keySet().stream()
            .filter(c -> !c.orbits().isEmpty())
            .flatMap(c -> deferredLaw(deferred, c, WorkbenchCurrency.EXALTED).keySet().stream())
            .filter(
                c ->
                    c.orbits().size() == 1
                        && c.orbits().values().iterator().next().counts().size() == 2
                        && c.orbits().values().iterator().next().counts().values().stream()
                                .mapToInt(Integer::intValue)
                                .sum()
                            == 2)
            .min(Comparator.comparing(DeferredItem::cardinality))
            .orElseThrow();
    assertThat(jointFamily.cardinality()).isLessThanOrEqualTo(BigInteger.valueOf(100));
    // Loss, fresh replacement and retaining the joint source law must agree for every supported
    // action.
    for (var source : List.of(spirit, weightedTier, jointFamily)) {
      for (var action : BasicCurrencyTransitions.supportedActions()) {
        var next = deferred.primitiveCursor(source, action, Set.of());
        var reference = primitive.primitiveCursor(source.representative(), action, Set.of());
        assertThat(next.blocked()).isEqualTo(reference.blocked());
        if (next.blocked()) continue;
        var law = new LinkedHashMap<DeferredItem, Fraction>();
        while (!next.complete()) {
          var outcome = next.next();
          law.merge(outcome.state(), outcome.probability(), Fraction::add);
        }
        assertThat(restore(law)).as("action %s", action).isEqualTo(eager(source, action));
      }
    }
  }

  @Test
  void observedAnonymousFamilyIsExcludedFromARecompiledPartition() {
    var partition = fixture.goals.rollPartition(root().item(), goal(47));
    var anonymous =
        deferredLaw(
                new DeferredTransitions(primitive, partition),
                new DeferredItem(root()),
                WorkbenchCurrency.EXALTED)
            .keySet()
            .stream()
            .filter(c -> !c.orbits().isEmpty())
            .findFirst()
            .orElseThrow();
    var fixed = new DeferredItem(anonymous.concrete(BigInteger.ZERO));
    assertThatThrownBy(
            () ->
                new DeferredTransitions(primitive, partition)
                    .primitiveCursor(fixed, WorkbenchCurrency.CHAOS, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    var initialFamilies =
        fixture
            .catalog
            .modifiers()
            .get(fixed.item().explicits().getFirst().modifierId())
            .familyIds();
    var recomputed =
        new DeferredTransitions(primitive, fixture.goals.rollPartition(fixed.item(), goal(47)));
    var law = deferredLaw(recomputed, fixed, WorkbenchCurrency.CHAOS);
    assertThat(restore(law)).isEqualTo(eager(fixed, WorkbenchCurrency.CHAOS));
    for (var cell : law.keySet())
      for (var orbit : cell.orbits().values())
        for (var family : orbit.profile().families())
          assertThat(Collections.disjoint(initialFamilies, family.ids())).isTrue();
  }

  @Test
  void initialIrrelevantFixedValueNeverOverlapsARegeneratedWeightedTierCell() {
    var original = root().item();
    var item =
        new ItemState(
            original.snapshotId(),
            original.baseItemId(),
            original.itemLevel(),
            original.rarity(),
            original.implicits(),
            List.of(
                new ModifierInstance("amulet:prefix:stalwart", Map.of("base_maximum_life", 50L))),
            original.conditions());
    var initial = new DeferredItem(new BasicCurrencyState(item, primitive.provenance()));
    var transitions =
        new DeferredTransitions(primitive, fixture.goals.rollPartition(item, goal(47)));
    var law = deferredLaw(transitions, initial, WorkbenchCurrency.CHAOS);
    assertThat(law).containsKey(initial);
    assertThat(restore(law)).isEqualTo(eager(initial, WorkbenchCurrency.CHAOS));
    var seen = new HashSet<BasicCurrencyState>();
    for (var cell : law.keySet()) {
      for (var i = BigInteger.ZERO; i.compareTo(cell.cardinality()) < 0; i = i.add(BigInteger.ONE))
        assertThat(seen.add(cell.concrete(i))).as("disjoint cell %s", cell.canonicalKey()).isTrue();
    }
  }

  @Test
  void initialFixedRollDoesNotOverlapRegeneratedUniformCells() {
    var item = root().item();
    var fixed =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            List.of(
                new ModifierInstance(
                    "amulet:prefix:countess", Map.of("base_spirit_from_equipment", 49L))),
            item.conditions());
    var partition = fixture.goals.rollPartition(fixed, goal(47));
    var definition = fixture.catalog.modifiers().get("amulet:prefix:countess");
    assertThat(partition.end(definition, 47L)).isEqualTo(48);
    assertThat(partition.end(definition, 49L)).isEqualTo(49);
    assertThat(partition.end(definition, 50L)).isEqualTo(50);
  }

  @Test
  void existingLifeCanBePreservedOrLostAndReacquiredWithExactConditionalLaws() {
    var item = root().item();
    var life =
        new BasicCurrencyState(
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(
                    new ModifierInstance(
                        "amulet:prefix:stalwart", Map.of("base_maximum_life", 50L))),
                item.conditions()),
            primitive.provenance());
    var known =
        fixture.index.knownStats(
            new Context(item.snapshotId(), item.baseItemId(), item.itemLevel()));
    var lifeId = GoalCatalogIndex.id("explicit", "base_maximum_life");
    var chaosId = GoalCatalogIndex.id("explicit", "base_chaos_damage_resistance_%");
    var goal =
        new GoalFilter(
            1,
            fixture.index.version(),
            new General(item.baseItemId(), new Range(null, null), List.of(item.rarity())),
            List.of(
                new Group(
                    "goal",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        new Entry(
                            "life",
                            lifeId,
                            known.get(lifeId).unit(),
                            new Range(BigDecimal.valueOf(50), null),
                            null,
                            false),
                        new Entry(
                            "chaos",
                            chaosId,
                            known.get(chaosId).unit(),
                            new Range(BigDecimal.ONE, null),
                            null,
                            false)))));
    var transitions =
        new DeferredTransitions(primitive, fixture.goals.rollPartition(life.item(), goal));
    var evaluate = fixture.goals.compileEvaluation(life.item(), goal);
    var source = new DeferredItem(life);
    var direct = deferredLaw(transitions, source, WorkbenchCurrency.EXALTED);
    assertThat(restore(direct)).isEqualTo(eager(source, WorkbenchCurrency.EXALTED));
    assertThat(direct.keySet()).anyMatch(s -> evaluate.apply(s.item()).status() == Status.MATCH);
    var loss = deferredLaw(transitions, source, WorkbenchCurrency.CHAOS);
    assertThat(restore(loss)).isEqualTo(eager(source, WorkbenchCurrency.CHAOS));
    var chaosOnly =
        loss.keySet().stream()
            .filter(
                s ->
                    s.item().explicits().stream()
                        .anyMatch(m -> m.values().containsKey("base_chaos_damage_resistance_%")))
            .findFirst()
            .orElseThrow();
    assertThat(evaluate.apply(chaosOnly.item()).status()).isEqualTo(Status.NO_MATCH);
    var recovery = deferredLaw(transitions, chaosOnly, WorkbenchCurrency.EXALTED);
    assertThat(restore(recovery)).isEqualTo(eager(chaosOnly, WorkbenchCurrency.EXALTED));
    assertThat(recovery.keySet()).anyMatch(s -> evaluate.apply(s.item()).status() == Status.MATCH);
  }

  private static Map<DeferredItem, Fraction> deferredLaw(
      DeferredTransitions transitions, DeferredItem source, WorkbenchCurrency action) {
    var cursor = transitions.primitiveCursor(source, action, Set.of());
    var result = new LinkedHashMap<DeferredItem, Fraction>();
    while (!cursor.complete()) {
      var next = cursor.next();
      result.merge(next.state(), next.probability(), Fraction::add);
    }
    return result;
  }

  @Test
  void productCellsHaveBigIntegerRandomAccessWithoutMaterialization() {
    var item = root().item();
    for (long minimum : List.of(0L, Long.MIN_VALUE)) {
      var state =
          new BasicCurrencyState(
              new ItemState(
                  item.snapshotId(),
                  item.baseItemId(),
                  item.itemLevel(),
                  item.rarity(),
                  item.implicits(),
                  List.of(
                      new ModifierInstance("x", Map.of("x", minimum)),
                      new ModifierInstance("y", Map.of("y", 0L))),
                  item.conditions()),
              primitive.provenance());
      var cell =
          new DeferredItem(
              state,
              Map.of(
                  "x",
                  new DeferredItem.Band("x", minimum, Long.MAX_VALUE),
                  "y",
                  new DeferredItem.Band("y", 0, Long.MAX_VALUE)));
      assertThat(cell.cardinality()).isEqualTo(BigInteger.ONE.shiftLeft(minimum == 0 ? 126 : 127));
      assertThat(cell.concrete(cell.cardinality().subtract(BigInteger.ONE)).item().explicits())
          .allSatisfy(m -> assertThat(m.values().values()).containsOnly(Long.MAX_VALUE));
    }
  }

  private Map<BasicCurrencyState, Fraction> eager(DeferredItem source, WorkbenchCurrency action) {
    var result = new LinkedHashMap<BasicCurrencyState, Fraction>();
    for (var i = BigInteger.ZERO;
        i.compareTo(source.cardinality()) < 0;
        i = i.add(BigInteger.ONE)) {
      var cursor = primitive.primitiveCursor(source.concrete(i), action, Set.of());
      while (!cursor.complete()) {
        var next = cursor.next();
        var p = next.probability().multiply(source.concreteProbability(i));
        result.merge(next.state(), p, Fraction::add);
      }
    }
    return result;
  }

  private static Map<BasicCurrencyState, Fraction> restore(Map<DeferredItem, Fraction> cells) {
    var result = new LinkedHashMap<BasicCurrencyState, Fraction>();
    cells.forEach(
        (cell, mass) -> {
          for (var i = BigInteger.ZERO;
              i.compareTo(cell.cardinality()) < 0;
              i = i.add(BigInteger.ONE))
            result.merge(
                cell.concrete(i), mass.multiply(cell.concreteProbability(i)), Fraction::add);
        });
    return result;
  }

  private static Object statuses(GoalEvaluator.Evaluation evaluation) {
    return List.of(
        evaluation.status(),
        evaluation.groups().stream()
            .map(g -> g.entries().stream().map(e -> e.status()).toList())
            .toList());
  }
}
