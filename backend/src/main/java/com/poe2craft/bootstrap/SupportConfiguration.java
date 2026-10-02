package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.*;
import com.poe2craft.crafting.domain.WorkbenchSimulator;
import com.poe2craft.crafting.infrastructure.*;
import com.poe2craft.item.ItemCatalog;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SupportConfiguration {
  @Bean
  AdditionPoolStore additionPoolStore(JdbcTemplate jdbc, ObjectMapper json) {
    return new JdbcAdditionPoolStore(jdbc, json);
  }

  @Bean
  AdditionPoolCache additionPoolCache(ItemCatalog catalog, AdditionPoolStore store) {
    var registry = CraftingRegistryLoader.load();
    var digest =
        AdditionPoolCache.hash(
            registry.get("ledgerVersion").asText()
                + registry.get("assumptionLedger")
                + registry.get("modifierWeightPolicy")
                + registry.get("mixedKnownUnknownWeightPolicy"));
    return new AdditionPoolCache(catalog, store, WorkbenchSimulator.RULE_VERSION, digest, 2048);
  }

  @Bean
  SupportRecommendations supportRecommendations(ItemCatalog catalog, AdditionPoolCache pools) {
    return new SupportRecommendations(catalog, pools);
  }
}
