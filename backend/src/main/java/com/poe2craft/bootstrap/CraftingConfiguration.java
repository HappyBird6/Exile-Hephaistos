package com.poe2craft.bootstrap;

import com.poe2craft.crafting.application.GraphExplorer;
import com.poe2craft.crafting.application.TransitionCache;
import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.CraftingEngine;
import com.poe2craft.crafting.domain.WorkbenchSimulator;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.SolarTextMapper;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CraftingConfiguration {
  @Bean
  ItemCatalog itemCatalog() {
    return ItemCatalogLoader.loadDefault();
  }

  @Bean
  CraftingEngine craftingEngine(ItemCatalog catalog) {
    return new CraftingEngine(catalog);
  }

  @Bean
  WorkbenchSimulator workbenchSimulator(ItemCatalog catalog, CraftingEngine engine) {
    return new WorkbenchSimulator(catalog, engine);
  }

  @Bean
  WorkbenchService workbenchService(ItemCatalog catalog, WorkbenchSimulator simulator) {
    try (var data = getClass().getResourceAsStream("/catalog/stocky-mitts/catalog.json");
        var raw = getClass().getResourceAsStream("/catalog/stocky-mitts/base.raw.json");
        var details = getClass().getResourceAsStream("/catalog/stocky-mitts/details.raw.json");
        var special =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.catalog.json");
        var specialRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/horror-essence.raw.json");
        var perfect =
            getClass()
                .getResourceAsStream(
                    "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json");
        var perfectRaw =
            getClass()
                .getResourceAsStream("/catalog/stocky-mitts/perfect-grounding-opulence.raw.json");
        var prismatic =
            getClass().getResourceAsStream("/catalog/stocky-mitts/prismatic-alloy.catalog.json");
        var scalar =
            getClass().getResourceAsStream("/catalog/stocky-mitts/scalar-alloys.catalog.json");
        var scalarRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/scalar-alloys.raw.json");
        var reviewed =
            getClass().getResourceAsStream("/catalog/stocky-mitts/reviewed-alloys.catalog.json");
        var reviewedRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/reviewed-alloys.raw.json");
        var prismaticRaw =
            getClass().getResourceAsStream("/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      return new WorkbenchService(
          catalog,
          simulator,
          ItemCatalogLoader.addSpecial(
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
              reviewedRaw));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot load Stocky Mitts catalog", e);
    }
  }

  @Bean
  SolarTextMapper solarTextMapper(ItemCatalog catalog) {
    return new SolarTextMapper(catalog);
  }

  @Bean
  TransitionCache transitionCache(CraftingEngine engine) {
    return new TransitionCache(engine, 256, 25000);
  }

  @Bean
  GraphExplorer graphExplorer(TransitionCache cache) {
    return new GraphExplorer(cache);
  }
}
