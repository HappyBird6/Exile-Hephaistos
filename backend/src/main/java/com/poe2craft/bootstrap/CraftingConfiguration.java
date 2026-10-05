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
      try (var source = getClass().getResourceAsStream("/catalog/top-base-essences.json")) {
        var manifest = new com.fasterxml.jackson.databind.ObjectMapper().readTree(source);
        for (var key :
            java.util.stream.Stream.concat(
                    com.poe2craft.item.ReviewedGloves.BASES.keySet().stream(),
                    java.util.stream.Stream.concat(
                        com.poe2craft.item.ReviewedHelmets.BASES.keySet().stream(),
                        java.util.stream.Stream.concat(
                            com.poe2craft.item.ReviewedBodies.BASES.keySet().stream(),
                            java.util.stream.Stream.concat(
                                com.poe2craft.item.ReviewedBoots.BASES.keySet().stream(),
                                java.util.stream.Stream.concat(
                                    com.poe2craft.item.ReviewedBows.BASES.keySet().stream(),
                                    java.util.stream.Stream.concat(
                                        com.poe2craft.item.ReviewedRings.BASES.keySet().stream(),
                                        java.util.stream.Stream.concat(
                                            com.poe2craft.item.ReviewedAmulets.BASES
                                                .keySet()
                                                .stream(),
                                            java.util.stream.Stream.concat(
                                                com.poe2craft.item.ReviewedSceptres.BASES
                                                    .keySet()
                                                    .stream(),
                                                java.util.stream.Stream.concat(
                                                    com.poe2craft.item.ReviewedWands.BASES
                                                        .keySet()
                                                        .stream(),
                                                    java.util.stream.Stream.concat(
                                                        com.poe2craft.item.ReviewedBelts.BASES
                                                            .keySet()
                                                            .stream(),
                                                        java.util.stream.Stream.concat(
                                                            com
                                                                .poe2craft
                                                                .item
                                                                .ReviewedCrossbows
                                                                .BASES
                                                                .keySet()
                                                                .stream(),
                                                            java.util.stream.Stream.concat(
                                                                com
                                                                    .poe2craft
                                                                    .item
                                                                    .ReviewedOffhands
                                                                    .BASES
                                                                    .keySet()
                                                                    .stream(),
                                                                com
                                                                    .poe2craft
                                                                    .item
                                                                    .ReviewedQuivers
                                                                    .BASES
                                                                    .keySet()
                                                                    .stream()))))))))))))
                .toList()) {
          service.registerReviewedArmour(
              key,
              ItemCatalogLoader.loadTopBase(key),
              reviewedEssences(manifest.get(key).get("fixed")),
              reviewedEssences(manifest.get(key).get("replacements")));
        }
      }
      return service;
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot load Stocky Mitts catalog", e);
    }
  }

  private static java.util.Map<
          com.poe2craft.crafting.domain.WorkbenchCurrency, java.util.List<String>>
      reviewedEssences(com.fasterxml.jackson.databind.JsonNode source) {
    var result =
        new java.util.EnumMap<
            com.poe2craft.crafting.domain.WorkbenchCurrency, java.util.List<String>>(
            com.poe2craft.crafting.domain.WorkbenchCurrency.class);
    source
        .fields()
        .forEachRemaining(
            entry -> {
              var ids = new java.util.ArrayList<String>();
              entry.getValue().forEach(id -> ids.add(id.asText()));
              result.put(
                  com.poe2craft.crafting.domain.WorkbenchCurrency.valueOf(entry.getKey()),
                  java.util.List.copyOf(ids));
            });
    return result;
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
