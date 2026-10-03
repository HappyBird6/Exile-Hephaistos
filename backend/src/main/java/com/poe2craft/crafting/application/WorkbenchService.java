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
  private final ItemCatalog wand;
  private final ItemCatalog body;
  private final WorkbenchSimulator bodySimulator;
  private final WorkbenchSimulator wandSimulator;
  private final ItemCatalog bow;
  private final WorkbenchSimulator bowSimulator;

  public WorkbenchService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky) {
    this(solar, solarSimulator, stocky, null);
  }

  public WorkbenchService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky, ItemCatalog bow) {
    this(solar, solarSimulator, stocky, bow, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand) {
    this(solar, solarSimulator, stocky, bow, wand, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body) {
    this.body = body;
    this.bodySimulator =
        body == null
            ? null
            : new WorkbenchSimulator(
                body,
                new CraftingEngine(body),
                body.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of(),
                BodyEssenceTargets.REPLACEMENTS);
    this.wand = wand;
    this.wandSimulator =
        wand == null
            ? null
            : new WorkbenchSimulator(
                wand,
                new CraftingEngine(wand),
                wand.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                WandEssenceTargets.VERIFIED);
    this.bow = bow;
    this.bowSimulator =
        bow == null
            ? null
            : new WorkbenchSimulator(
                bow,
                new CraftingEngine(bow),
                bow.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                BowEssenceTargets.VERIFIED);
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
            stocky,
            new CraftingEngine(stocky),
            coupled,
            StockyEssenceTargets.VERIFIED,
            StockyEssenceTargets.REPLACEMENTS);
  }

  public Initial initial(String base, int level) {
    var catalog = catalog(base);
    var state =
        base.equals("solar")
            ? SolarAmulet.initial(catalog, level, 15)
            : new ItemState(
                catalog.metadata().snapshotId(),
                catalog.base().id(),
                level,
                ItemState.Rarity.NORMAL,
                List.of(),
                List.of(),
                Set.of());
    var bucket = StateBucket.from(state);
    var engine = new CraftingEngine(catalog);
    engine.validate(bucket);
    return new Initial(
        simulator(state).ruleVersion(),
        catalog.metadata(),
        bucket.id(),
        bucket,
        catalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds(),
        QualityLimitRules.describe(state, catalog),
        base.equals("stocky") ? 0 : null);
  }

  private ItemCatalog catalog(String base) {
    return switch (base) {
      case "solar" -> solar;
      case "stocky" -> stocky;
      case "body" -> {
        if (body == null) throw new IllegalArgumentException("Body Armour catalog unavailable");
        yield body;
      }
      case "wand" -> {
        if (wand == null) throw new IllegalArgumentException("Wand catalog unavailable");
        yield wand;
      }
      case "bow" -> {
        if (bow == null) throw new IllegalArgumentException("Bow catalog unavailable");
        yield bow;
      }
      default -> throw new IllegalArgumentException("Unsupported Workbench base");
    };
  }

  private WorkbenchSimulator simulator(ItemState state) {
    if (state == null) throw new IllegalArgumentException("State required");
    if (state.baseItemId().equals(SolarAmulet.BASE_ID)) return solarSimulator;
    if (state.baseItemId().equals(STOCKY_BASE_ID)) return stockySimulator;
    if (state.baseItemId().equals(BodyEssenceTargets.BASE_ID) && bodySimulator != null)
      return bodySimulator;
    if (state.baseItemId().equals(WandEssenceTargets.BASE_ID) && wandSimulator != null)
      return wandSimulator;
    if (state.baseItemId().equals(BowEssenceTargets.BASE_ID) && bowSimulator != null)
      return bowSimulator;
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
      List<String> compatibleSnapshotIds,
      QualityLimitRules.Limit qualityLimit,
      Integer augmentSockets) {}
}
