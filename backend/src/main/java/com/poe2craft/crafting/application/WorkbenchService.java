package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Workbench-only catalog dispatch. Support and Explorer retain their Solar catalog. */
public final class WorkbenchService {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";
  private final ItemCatalog solar;
  private final ItemCatalog stocky;
  private final WorkbenchSimulator solarSimulator;
  private final WorkbenchSimulator stockySimulator;

  public WorkbenchService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky) {
    this.solar = solar;
    this.solarSimulator = solarSimulator;
    this.stocky = stocky;
    var coupled =
        stocky.modifiers().values().stream()
            .filter(d -> d.stats().size() > 1)
            .map(ModifierDefinition::id)
            .collect(java.util.stream.Collectors.toSet());
    stockySimulator =
        new WorkbenchSimulator(
            stocky, new CraftingEngine(stocky), coupled, StockyEssenceTargets.VERIFIED);
  }

  public Initial initial(String base, int level) {
    var catalog = catalog(base);
    var state =
        base.equals("solar")
            ? SolarAmulet.initial(catalog, level, 15)
            : new ItemState(
                catalog.metadata().snapshotId(),
                STOCKY_BASE_ID,
                level,
                ItemState.Rarity.NORMAL,
                List.of(),
                List.of(),
                Set.of());
    var bucket = StateBucket.from(state);
    var engine = new CraftingEngine(catalog);
    engine.validate(bucket);
    return new Initial(
        (base.equals("solar") ? solarSimulator : stockySimulator).ruleVersion(),
        catalog.metadata(),
        bucket.id(),
        bucket,
        catalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds());
  }

  private ItemCatalog catalog(String base) {
    return switch (base) {
      case "solar" -> solar;
      case "stocky" -> stocky;
      default -> throw new IllegalArgumentException("Unsupported Workbench base");
    };
  }

  private WorkbenchSimulator simulator(ItemState state) {
    if (state == null) throw new IllegalArgumentException("State required");
    if (state.baseItemId().equals(SolarAmulet.BASE_ID)) return solarSimulator;
    if (state.baseItemId().equals(STOCKY_BASE_ID)) return stockySimulator;
    throw new IllegalArgumentException("Unsupported Workbench base");
  }

  public WorkbenchSimulator.Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> omens, RandomGenerator random) {
    return simulator(state).apply(state, action, omens, random);
  }

  public List<WorkbenchSimulator.Availability> actions(ItemState state, Set<String> omens) {
    return simulator(state).actions(state, omens);
  }

  public record Initial(
      String ruleVersion,
      ItemCatalog.Metadata metadata,
      String id,
      StateBucket state,
      Map<String, ModifierDefinition> modifiers,
      List<CraftingEngine.Availability> actions,
      List<String> compatibleSnapshotIds) {}
}
