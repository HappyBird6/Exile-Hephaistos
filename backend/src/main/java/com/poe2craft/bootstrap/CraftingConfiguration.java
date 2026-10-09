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
    try (var data =
            com.poe2craft.support.SealedResources.open("/catalog/stocky-mitts/catalog.json");
        var raw =
            com.poe2craft.support.SealedResources.open("/catalog/stocky-mitts/base.raw.json");
        var details =
            com.poe2craft.support.SealedResources.open("/catalog/stocky-mitts/details.raw.json");
        var special =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/abyss-essence.catalog.json");
        var specialRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/abyss-essence.raw.json");
        var horror =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/horror-essence.catalog.json");
        var horrorRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/horror-essence.raw.json");
        var perfect =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/perfect-grounding-opulence.catalog.json");
        var perfectRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/perfect-grounding-opulence.raw.json");
        var prismatic =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/prismatic-alloy.catalog.json");
        var scalar =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/scalar-alloys.catalog.json");
        var scalarRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/scalar-alloys.raw.json");
        var reviewed =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/reviewed-alloys.catalog.json");
        var reviewedRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/reviewed-alloys.raw.json");
        var prismaticRaw =
            com.poe2craft.support.SealedResources.open(
                "/catalog/stocky-mitts/prismatic-alloy.raw.json")) {
      var service =
          new WorkbenchService(
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
                  reviewedRaw),
              ItemCatalogLoader.loadBow(),
              ItemCatalogLoader.loadWand(),
              ItemCatalogLoader.loadBody(),
              ItemCatalogLoader.loadSceptre(),
              ItemCatalogLoader.loadBelt(),
              ItemCatalogLoader.loadHelmet(),
              ItemCatalogLoader.loadRing(),
              ItemCatalogLoader.loadSapphire(),
              ItemCatalogLoader.loadBasicJewel("ruby"),
              ItemCatalogLoader.loadBasicJewel("emerald"),
              ItemCatalogLoader.loadBasicJewel("diamond"),
              ItemCatalogLoader.loadBasicJewel("time-lost-ruby"),
              ItemCatalogLoader.loadBasicJewel("time-lost-emerald"),
              ItemCatalogLoader.loadBasicJewel("time-lost-sapphire"),
              ItemCatalogLoader.loadBasicJewel("time-lost-diamond"),
              ItemCatalogLoader.loadTopBase("soldier"),
              ItemCatalogLoader.loadTopBase("imperial"));
      var manifest = com.poe2craft.crafting.infrastructure.ReviewedEssencesLoader.load();
      for (var entry : manifest.entrySet()) {
        service.registerReviewedBase(
            entry.getKey(),
            ItemCatalogLoader.loadTopBase(entry.getKey()),
            entry.getValue().fixed(),
            entry.getValue().replacements());
      }
      return service;
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
