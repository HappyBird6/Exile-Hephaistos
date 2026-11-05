package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;

/** Shared reviewed glove catalog and state setup for rule and HTTP tests. */
public class StockyCraftingFixture {
  public final ItemCatalog catalog = stocky();

  public static ItemCatalog stocky() {
    try (var data =
            StockyCraftingFixture.class.getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw =
            StockyCraftingFixture.class.getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/details.raw.json");
        var special =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/abyss-essence.catalog.json");
        var specialRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/horror-essence.raw.json");
        var perfect =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json");
        var perfectRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/perfect-grounding-opulence.raw.json");
        var prismatic =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/prismatic-alloy.catalog.json");
        var scalar =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/scalar-alloys.catalog.json");
        var scalarRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/scalar-alloys.raw.json");
        var reviewed =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/reviewed-alloys.catalog.json");
        var reviewedRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/reviewed-alloys.raw.json");
        var prismaticRaw =
            StockyCraftingFixture.class.getResourceAsStream(
                "/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      return ItemCatalogLoader.addSpecial(
          ItemCatalogLoader.addSpecial(
              ItemCatalogLoader.addSpecial(
                  ItemCatalogLoader.addSpecial(
                      ItemCatalogLoader.addSpecial(
                          ItemCatalogLoader.loadWithSpecial(
                              data, raw, details, special, specialRaw),
                          horror,
                          horrorRaw),
                      perfect,
                      perfectRaw),
                  prismatic,
                  prismaticRaw),
              scalar,
              scalarRaw),
          reviewed,
          reviewedRaw);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          StockyEssenceTargets.VERIFIED,
          StockyEssenceTargets.REPLACEMENTS);
  public final ItemState root =
      new ItemState(
          catalog.metadata().snapshotId(),
          CatalogCraftingService.STOCKY_BASE_ID,
          82,
          ItemState.Rarity.NORMAL,
          List.of(),
          List.of(),
          Set.of());
  public final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_HYSTERIA;
  public final String target = "stocky-mitts:suffix:of-fury";

  public ItemState rare(int level, String locked, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.RARE,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()), id.equals(locked));
                })
            .toList(),
        root.conditions());
  }
}
