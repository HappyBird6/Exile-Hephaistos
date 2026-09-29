package com.poe2craft.bootstrap;

import com.poe2craft.crafting.application.GraphExplorer;
import com.poe2craft.crafting.application.TransitionCache;
import com.poe2craft.crafting.domain.CraftingEngine;
import com.poe2craft.item.ItemCatalog;
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
  TransitionCache transitionCache(CraftingEngine engine) {
    return new TransitionCache(engine, 256, 25000);
  }

  @Bean
  GraphExplorer graphExplorer(TransitionCache cache) {
    return new GraphExplorer(cache);
  }
}
