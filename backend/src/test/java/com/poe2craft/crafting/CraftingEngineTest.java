package com.poe2craft.crafting;

import static com.poe2craft.crafting.domain.CraftingAction.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.GraphExplorer;
import com.poe2craft.crafting.application.TransitionCache;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class CraftingEngineTest {
  private final ItemCatalog catalog = fixture();
  private final CraftingEngine engine = new CraftingEngine(catalog);
  private final StateBucket root = StateBucket.from(SolarAmulet.initial(catalog));

  static ItemCatalog fixture() {
    var real = ItemCatalogLoader.loadDefault();
    var defs = new ArrayList<ModifierDefinition>();
    defs.add(real.find(SolarAmulet.IMPLICIT_ID).orElseThrow());
    defs.add(mod("a1", "A", true, 1, 2));
    defs.add(mod("a2", "A", true, 20, 3));
    defs.add(mod("b", "B", true, 1, 5));
    defs.add(mod("c", "C", true, 1, 7));
    defs.add(mod("d", "D", true, 1, 11));
    defs.add(mod("x", "X", false, 1, 13));
    defs.add(mod("y", "Y", false, 1, 17));
    defs.add(mod("z", "Z", false, 1, 19));
    defs.add(mod("w", "W", false, 1, 23));
    var old = real.metadata();
    var metadata =
        new ItemCatalog.Metadata(
            "synthetic-v1",
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            5,
            4,
            28,
            72);
    return new ItemCatalog(metadata, real.base(), defs);
  }

  private static ModifierDefinition mod(
      String id, String family, boolean prefix, int level, int weight) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        prefix ? ModifierDefinition.AffixType.PREFIX : ModifierDefinition.AffixType.SUFFIX,
        Set.of(family),
        level,
        weight,
        1,
        id,
        List.of(new ModifierDefinition.StatRange("value", 0, 1)),
        Set.of("same-tag"),
        "test:fixture");
  }

  private StateBucket state(ItemState.Rarity rarity, String... ids) {
    return root.with(rarity, List.of(ids));
  }

  private static double sum(CraftingEngine.TransitionResult result) {
    return result.outcomes().stream().mapToDouble(CraftingEngine.Outcome::probability).sum();
  }

  private static Map<StateBucket, Double> distribution(CraftingEngine.TransitionResult result) {
    var map = new HashMap<StateBucket, Double>();
    result.outcomes().forEach(o -> map.put(o.state(), o.probability()));
    return map;
  }

  @Test
  void weightedPoolDoesNotForcePrefixAndSuffixToHalf() {
    var result = engine.transition(root, TRANSMUTATION);
    assertThat(result.available()).isTrue();
    assertThat(sum(result)).isCloseTo(1, within(1e-12));
    assertThat(result.outcomes()).hasSize(9);
    assertThat(distribution(result).get(state(ItemState.Rarity.MAGIC, "a2")))
        .isCloseTo(.03, within(1e-12));
    double prefix =
        result.outcomes().stream()
            .filter(
                o ->
                    catalog.find(o.state().modifierIds().getFirst()).orElseThrow().affixType()
                        == ModifierDefinition.AffixType.PREFIX)
            .mapToDouble(CraftingEngine.Outcome::probability)
            .sum();
    assertThat(prefix).isCloseTo(.28, within(1e-12));
    var low =
        new StateBucket(
            root.snapshotId(),
            root.baseItemId(),
            19,
            root.rarity(),
            root.implicits(),
            List.of(),
            Set.of());
    var lowResult = engine.transition(low, TRANSMUTATION);
    assertThat(lowResult.outcomes()).hasSize(8);
    assertThat(lowResult.outcomes()).noneMatch(o -> o.state().modifierIds().contains("a2"));
    assertThat(
            lowResult.outcomes().stream()
                .filter(o -> o.state().modifierIds().contains("a1"))
                .findFirst()
                .orElseThrow()
                .probability())
        .isCloseTo(2.0 / 97, within(1e-12));
  }

  @Test
  void augmentationAndRegalUseTheCorrectPostUpgradeCapacity() {
    var magic = state(ItemState.Rarity.MAGIC, "a1");
    var augmented = engine.transition(magic, AUGMENTATION);
    assertThat(augmented.outcomes()).hasSize(4);
    assertThat(distribution(augmented).get(state(ItemState.Rarity.MAGIC, "a1", "x")))
        .isCloseTo(13.0 / 72, within(1e-12));
    assertThat(
            engine.transition(state(ItemState.Rarity.MAGIC, "a1", "x"), AUGMENTATION).available())
        .isFalse();
    var regal = engine.transition(state(ItemState.Rarity.MAGIC, "a1", "x"), REGAL);
    assertThat(regal.available()).isTrue();
    assertThat(sum(regal)).isCloseTo(1, within(1e-12));
    assertThat(regal.outcomes())
        .allMatch(
            o ->
                o.state().rarity() == ItemState.Rarity.RARE && o.state().modifierIds().size() == 3);
    assertThat(regal.outcomes()).noneMatch(o -> o.state().modifierIds().contains("a2"));
  }

  @Test
  void exaltedRespectsFullSidesAndFamilyConflicts() {
    var item = state(ItemState.Rarity.RARE, "a1", "b", "c", "x", "y");
    var result = engine.transition(item, EXALTED);
    assertThat(result.outcomes()).hasSize(2);
    assertThat(
            distribution(result).get(state(ItemState.Rarity.RARE, "a1", "b", "c", "x", "y", "z")))
        .isCloseTo(19.0 / 42, within(1e-12));
    assertThat(engine.transition(result.outcomes().getFirst().state(), EXALTED).available())
        .isFalse();
    assertThatThrownBy(() -> engine.transition(state(ItemState.Rarity.RARE, "a1", "a2"), EXALTED))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void annulmentIsUniformOverInstancesAndPreservesRarity() {
    var result = engine.transition(state(ItemState.Rarity.RARE, "a1", "w"), ANNULMENT);
    assertThat(result.outcomes()).hasSize(2).allMatch(o -> o.probability() == .5);
    var empty =
        engine
            .transition(state(ItemState.Rarity.MAGIC, "a1"), ANNULMENT)
            .outcomes()
            .getFirst()
            .state();
    assertThat(empty.rarity()).isEqualTo(ItemState.Rarity.MAGIC);
    assertThat(empty.modifierIds()).isEmpty();
    assertThat(engine.transition(empty, ANNULMENT).available()).isFalse();
    assertThat(engine.transition(state(ItemState.Rarity.RARE), CHAOS).available()).isFalse();
  }

  @Test
  void chaosReopensFamiliesRecalculatesDenominatorsAndMergesSelfLoops() {
    var before = state(ItemState.Rarity.RARE, "a1", "x");
    var result = engine.transition(before, CHAOS);
    assertThat(sum(result)).isCloseTo(1, within(1e-12));
    assertThat(distribution(result).get(before))
        .isCloseTo(.5 * 2 / 87 + .5 * 13 / 95, within(1e-12));
    assertThat(distribution(result).get(state(ItemState.Rarity.RARE, "a2", "x")))
        .isCloseTo(.5 * 3 / 87, within(1e-12));
    assertThat(result.outcomes().stream().map(CraftingEngine.Outcome::id).distinct().count())
        .isEqualTo(result.outcomes().size());
  }

  @Test
  void numericRollProjectionIsExactForTheSupportedActions() {
    var one =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(new ModifierInstance("a1", Map.of("value", 0L))),
            Set.of());
    var two =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(new ModifierInstance("a1", Map.of("value", 1L))),
            Set.of());
    assertThat(one).isNotEqualTo(two);
    assertThat(StateBucket.from(one)).isEqualTo(StateBucket.from(two));
    for (var action : CraftingAction.values())
      assertThat(engine.transition(StateBucket.from(one), action))
          .isEqualTo(engine.transition(StateBucket.from(two), action));
  }

  @Test
  void graphMatchesIndependentUnmergedThreeStepEnumeration() {
    var cache = new TransitionCache(engine, 256, 25000);
    var result =
        new GraphExplorer(cache)
            .explore(root, List.of(TRANSMUTATION, AUGMENTATION, REGAL), 2000, 10000, () -> false);
    Map<List<String>, Double> oracle = new HashMap<>();
    // Enumerate every ordered path independently. No engine, resolver, graph or intermediate
    // merging.
    var defs =
        catalog.modifiers().values().stream()
            .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
            .toList();
    for (var first : defs) {
      var seconds = defs.stream().filter(d -> d.affixType() != first.affixType()).toList();
      double totalSecond = seconds.stream().mapToInt(ModifierDefinition::weight).sum();
      for (var second : seconds) {
        var thirds =
            defs.stream()
                .filter(
                    d ->
                        Collections.disjoint(d.familyIds(), first.familyIds())
                            && Collections.disjoint(d.familyIds(), second.familyIds()))
                .toList();
        double totalThird = thirds.stream().mapToInt(ModifierDefinition::weight).sum();
        for (var third : thirds) {
          var key = List.of(first.id(), second.id(), third.id()).stream().sorted().toList();
          double p =
              first.weight() / 100.0 * second.weight() / totalSecond * third.weight() / totalThird;
          oracle.merge(key, p, Double::sum);
        }
      }
    }
    assertThat(result.complete()).isTrue();
    assertThat(result.completedProbability()).isCloseTo(1, within(1e-12));
    assertThat(result.terminals()).hasSize(oracle.size());
    for (var end : result.terminals())
      assertThat(end.probability())
          .isCloseTo(oracle.get(result.nodes().get(end.id()).modifierIds()), within(1e-12));
    assertThat(result.edges().size()).isGreaterThan(result.nodes().size());
  }

  @Test
  void graphCyclesAndBlockedActionsDoNotLoseMass() {
    var explorer = new GraphExplorer(new TransitionCache(engine, 256, 25000));
    var loop =
        explorer.explore(
            state(ItemState.Rarity.RARE),
            List.of(EXALTED, ANNULMENT, EXALTED, ANNULMENT),
            2000,
            10000,
            () -> false);
    assertThat(loop.nodes()).hasSize(10);
    assertThat(loop.terminals()).hasSize(1);
    assertThat(loop.terminals().getFirst().probability()).isCloseTo(1, within(1e-12));
    var blocked = explorer.explore(root, List.of(EXALTED), 2000, 10000, () -> false);
    assertThat(blocked.blockedProbability()).isEqualTo(1);
    assertThat(blocked.completedProbability()).isZero();
  }

  @Test
  void truncatedAndCancelledExplorationsKeepTheirUnexploredProbability() {
    var explorer = new GraphExplorer(new TransitionCache(engine, 256, 25000));
    var limited =
        explorer.explore(root, List.of(TRANSMUTATION, AUGMENTATION, REGAL), 20, 40, () -> false);
    assertThat(limited.complete()).isFalse();
    assertThat(limited.nodes().size()).isLessThanOrEqualTo(20);
    assertThat(limited.edges().size()).isLessThanOrEqualTo(40);
    assertThat(
            limited.completedProbability()
                + limited.blockedProbability()
                + limited.unexploredProbability())
        .isCloseTo(1, within(1e-12));
    assertThat(limited.terminals().stream().mapToDouble(GraphExplorer.Terminal::probability).sum())
        .isCloseTo(1, within(1e-12));
    var cancelled = explorer.explore(root, List.of(TRANSMUTATION), 2000, 10000, () -> true);
    assertThat(cancelled.unexploredProbability()).isEqualTo(1);
    assertThat(cancelled.edges()).isEmpty();
  }

  @Test
  void boundedCacheSeparatesStatesAndActionsAndReusesCanonicalBuckets() {
    var cache = new TransitionCache(engine, 2, 100);
    var a = state(ItemState.Rarity.RARE, "a1", "x");
    var b = state(ItemState.Rarity.RARE, "x", "a1");
    assertThat(cache.get(a, ANNULMENT)).isSameAs(cache.get(b, ANNULMENT));
    cache.get(a, EXALTED);
    cache.get(root, TRANSMUTATION);
    assertThat(cache.stats().hits()).isEqualTo(1);
    assertThat(cache.stats().entries()).isEqualTo(2);
    assertThat(cache.stats().retainedOutcomes()).isLessThanOrEqualTo(100);
    var tiny = new TransitionCache(engine, 5, 1);
    tiny.get(root, TRANSMUTATION);
    assertThat(tiny.stats().entries()).isZero();
    var stale =
        new StateBucket(
            "other", root.baseItemId(), 82, root.rarity(), root.implicits(), List.of(), Set.of());
    assertThatThrownBy(() -> cache.get(stale, TRANSMUTATION))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void realCatalogPerformanceAndProbabilityInvariants() {
    var real = ItemCatalogLoader.loadDefault();
    var realEngine = new CraftingEngine(real);
    var start = StateBucket.from(SolarAmulet.initial(real));
    var rare = start.with(ItemState.Rarity.RARE, List.of());
    for (int i = 0; i < 6; i++)
      rare = realEngine.transition(rare, EXALTED).outcomes().getFirst().state();
    var cold = new ArrayList<Long>();
    var warm = new ArrayList<Long>();
    var cache = new TransitionCache(realEngine, 256, 25000);
    for (int i = 0; i < 25; i++) {
      long t = System.nanoTime();
      var result = realEngine.transition(rare, CHAOS);
      long duration = System.nanoTime() - t;
      assertThat(sum(result)).isCloseTo(1, within(1e-10));
      result.outcomes().forEach(o -> realEngine.validate(o.state()));
      if (i >= 5) cold.add(duration);
      cache.get(rare, CHAOS);
      t = System.nanoTime();
      cache.get(rare, CHAOS);
      if (i >= 5) warm.add(System.nanoTime() - t);
    }
    Collections.sort(cold);
    Collections.sort(warm);
    double coldP95 = cold.get(18) / 1e6, warmP95 = warm.get(18) / 1e6;
    System.out.printf(
        "Solar full-rare Chaos, 20 measured runs: uncached p95=%.3fms, cached p95=%.3fms%n",
        coldP95, warmP95);
    assertThat(coldP95).isLessThan(300);
    assertThat(warmP95).isLessThan(300);
    var graph =
        new GraphExplorer(cache)
            .explore(start, List.of(TRANSMUTATION, AUGMENTATION, REGAL), 2000, 10000, () -> false);
    assertThat(graph.nodes().size()).isLessThanOrEqualTo(2000);
    assertThat(
            graph.completedProbability()
                + graph.blockedProbability()
                + graph.unexploredProbability())
        .isCloseTo(1, within(1e-9));
  }
}
