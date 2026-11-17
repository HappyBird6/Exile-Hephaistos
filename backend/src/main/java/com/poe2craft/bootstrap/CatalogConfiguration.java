package com.poe2craft.bootstrap;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.ReviewedEssencesLoader;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemObservations;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarTextMapper;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfiguration {
  // Keep absent observation fields out of legacy protocol consumers; domain values stay
  // Jackson-free.
  abstract static class ItemStateJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    abstract java.util.List<ItemState.InstalledAugment> installedAugments();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract ItemState.Sanctification sanctification();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    abstract boolean qualityInfused();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Integer equipmentQuality();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract ItemObservations observations();
  }

  abstract static class ObservationJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Double augmentEffectScale();
  }

  abstract static class ModifierJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    abstract boolean crafted();
  }

  abstract static class DesecrationJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Integer minimumModifierLevel();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract String revealPool();
  }

  @Bean
  public org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
      itemStateJson() {
    return builder ->
        builder
            .mixIn(ItemState.class, ItemStateJson.class)
            .mixIn(ItemObservations.class, ObservationJson.class)
            .mixIn(com.poe2craft.shared.item.ModifierInstance.class, ModifierJson.class)
            .mixIn(ItemState.DesecratedSlot.class, DesecrationJson.class)
            .mixIn(AbyssCraftingState.Slot.class, DesecrationJson.class);
  }

  @Bean
  WorkbenchDefinitions workbenchDefinitions() {
    return WorkbenchDefinitionsLoader.initialize();
  }

  @Bean
  ItemCatalog itemCatalog(WorkbenchDefinitions definitions) {
    return EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
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
  CatalogCraftingService workbenchService(ItemCatalog catalog, WorkbenchSimulator simulator) {
    var service =
        new CatalogCraftingService(
            catalog,
            simulator,
            ItemCatalogLoader.loadStocky(),
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
    var manifest = ReviewedEssencesLoader.load();
    for (var entry : manifest.entrySet()) {
      service.registerReviewedBase(
          entry.getKey(),
          ItemCatalogLoader.loadTopBase(entry.getKey()),
          entry.getValue().fixed(),
          entry.getValue().replacements());
    }
    return service;
  }

  @Bean
  SolarTextMapper solarTextMapper(CatalogCraftingService workbench) {
    return new SolarTextMapper(workbench.reviewedCatalogs());
  }
}
