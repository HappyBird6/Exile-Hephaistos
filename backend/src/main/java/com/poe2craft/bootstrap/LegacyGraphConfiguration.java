package com.poe2craft.bootstrap;

import com.poe2craft.configuration.BackendConfiguration;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.path.application.BasicPathService;
import com.poe2craft.craftingsupport.path.application.GraphExplorer;
import com.poe2craft.craftingsupport.path.application.TransitionCache;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Compatibility graph and first-hit APIs; independent from persistent search-job wiring. */
@Configuration
@Import(BackendConfiguration.class)
public class LegacyGraphConfiguration {
  @Bean
  BasicPathService basicPathService(ItemCatalog catalog) {
    return new BasicPathService(catalog, RulesetManifestLoader.load().identity());
  }

  @Bean
  TransitionCache transitionCache(CraftingEngine engine, BackendSettings settings) {
    return new TransitionCache(
        engine, settings.cache().transitionEntries(), settings.cache().transitionOutcomes());
  }

  @Bean
  GraphExplorer graphExplorer(TransitionCache cache) {
    return new GraphExplorer(cache);
  }
}
