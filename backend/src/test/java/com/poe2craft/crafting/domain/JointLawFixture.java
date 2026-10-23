package com.poe2craft.crafting.domain;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;

/** Independently enumerable weighted-goal catalog; no production inventory is changed. */
public final class JointLawFixture {
  public final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  public final ItemCatalog catalog;
  public final ItemState root;
  public final GoalFilter goal;
  public final AutomaticCraftingKernel kernel;

  public JointLawFixture() {
    var tiny = new BasicCurrencyTransitionsTest();
    var definitions = new ArrayList<ModifierDefinition>();
    definitions.add(tiny.implicit);
    for (var stat : List.of("a", "b", "c"))
      for (int value : stat.equals("c") ? List.of(1) : List.of(1, 4)) {
        definitions.add(
            new ModifierDefinition(
                stat + value,
                stat + value,
                ModifierDefinition.Layer.EXPLICIT,
                stat.equals("b")
                    ? ModifierDefinition.AffixType.SUFFIX
                    : ModifierDefinition.AffixType.PREFIX,
                Set.of(stat),
                1,
                1,
                value,
                stat,
                List.of(new ModifierDefinition.StatRange(stat, value, value)),
                Set.of(),
                "https://example.test/" + stat + value));
      }
    catalog = tiny.catalog(definitions);
    root = tiny.item(ItemState.Rarity.NORMAL, List.of());
    var entries =
        List.of("a", "b").stream()
            .map(stat -> new GoalFilter.Entry(stat, stat, "flat", null, BigDecimal.ONE, false))
            .toList();
    goal =
        new GoalFilter(
            1,
            "joint-test",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group(
                    "sum",
                    GoalFilter.Type.WEIGHTED_V1,
                    false,
                    new GoalFilter.Range(BigDecimal.valueOf(5), null),
                    entries),
                new GoalFilter.Group(
                    "third",
                    GoalFilter.Type.AND,
                    false,
                    null,
                    List.of(
                        new GoalFilter.Entry(
                            "c",
                            "c",
                            "flat",
                            new GoalFilter.Range(BigDecimal.ONE, null),
                            null,
                            false)))));
    var known = new HashMap<String, GoalCatalog.Stat>();
    for (var stat : List.of("a", "b", "c"))
      known.put(
          stat,
          new GoalCatalog.Stat(
              stat,
              stat,
              "flat",
              GoalCatalog.Kind.EXPLICIT,
              new GoalCatalog.Support(
                  GoalFilter.Capability.SUPPORTED, GoalFilter.Capability.SUPPORTED, null),
              true,
              null,
              List.of(stat),
              List.of(),
              List.of()));
    kernel =
        new AutomaticCraftingKernel(
                policy,
                catalog,
                new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
                "amulets")
            .withRollPartition(RollPartition.compile(catalog, known, goal, root), goal.toString());
  }

  public long value(ItemState item, String stat) {
    return item.explicits().stream().mapToLong(m -> m.values().getOrDefault(stat, 0L)).sum();
  }

  public AutomaticMethodGraph.Progress evaluate(ItemState item) {
    boolean sum = value(item, "a") + value(item, "b") >= 5, third = value(item, "c") >= 1;
    return new AutomaticMethodGraph.Progress(
        sum && third && item.rarity() == ItemState.Rarity.RARE
            ? GoalFilter.Status.MATCH
            : GoalFilter.Status.NO_MATCH,
        List.of(
            sum ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
            third ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH));
  }

  public AutomaticMethodGraph graph() {
    return new AutomaticMethodGraph(
        root,
        policy,
        kernel,
        this::evaluate,
        CertifiedInterval.Precision.standard(),
        64,
        128,
        20000,
        5000);
  }

  public void exhaust(AutomaticMethodGraph graph) {
    for (int steps = 0; !graph.finished() && !graph.resourceLimited() && steps < 100000; steps++) {
      graph.step();
      if (steps % 100 == 0) graph.publishProgress();
    }
    graph.publishProgress();
  }
}
