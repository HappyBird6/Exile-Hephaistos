package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.recommendation.application.AdditionPoolCache;
import com.poe2craft.craftingsupport.recommendation.application.AdditionPoolStore;
import com.poe2craft.craftingsupport.recommendation.application.SupportRecommendationService;
import com.poe2craft.craftingsupport.recommendation.infrastructure.JdbcAdditionPoolStore;
import com.poe2craft.craftingsupport.search.application.AutomaticRuntimeFactory;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.application.PathSearchService;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.craftingsupport.search.infrastructure.BundledSearchRuntimeFactory;
import com.poe2craft.craftingsupport.search.infrastructure.LocalAutomaticJobRepository;
import com.poe2craft.craftingsupport.search.infrastructure.LocalCertifiedJobRepository;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class CraftingSupportConfiguration {
  @Bean(destroyMethod = "close")
  AutomaticSearchService automaticSearchService(
      CraftingOperations workbench,
      GoalFilterService goals,
      MethodSearchService legacy,
      ObjectMapper json,
      BackendSettings settings) {
    return automaticSearchService(
        workbench, goals, legacy, json, settings.storage().methodJobDirectory(), settings);
  }

  AutomaticSearchService automaticSearchService(
      CraftingOperations workbench,
      GoalFilterService goals,
      MethodSearchService legacy,
      ObjectMapper json,
      String directory) {
    return automaticSearchService(
        workbench, goals, legacy, json, directory, BackendSettings.defaults());
  }

  private AutomaticSearchService automaticSearchService(
      CraftingOperations workbench,
      GoalFilterService goals,
      MethodSearchService legacy,
      ObjectMapper json,
      String directory,
      BackendSettings settings) {
    var policy = AutomaticCurrencyPolicyLoader.load();
    AutomaticRuntimeFactory runtimes =
        new BundledSearchRuntimeFactory(workbench, goals, legacy, json, policy);
    return new AutomaticSearchService(
        json,
        new LocalAutomaticJobRepository(java.nio.file.Path.of(directory).resolve("automatic-v1")),
        policy,
        runtimes::create,
        java.time.Clock.systemUTC(),
        runtimes::historical,
        settings.automaticSearch());
  }

  @Bean(destroyMethod = "close")
  MethodSearchService methodSearchService(
      CraftingOperations workbench,
      GoalFilterService goals,
      ObjectMapper json,
      BackendSettings settings) {
    var directory = settings.storage().methodJobDirectory();
    var service =
        new MethodSearchService(
            workbench.reviewedCatalogs(),
            goals,
            RulesetManifestLoader.load().identity(),
            json,
            settings);
    service.persistence(new LocalCertifiedJobRepository(java.nio.file.Path.of(directory), json));
    return service;
  }

  @Bean(destroyMethod = "close")
  PathSearchService pathSearchService(
      ItemCatalog catalog, GoalFilterService goals, ObjectMapper json, BackendSettings settings) {
    return new PathSearchService(
        catalog, goals, RulesetManifestLoader.load().identity(), json, settings.pathSearch());
  }

  @Bean
  GoalFilterService goalFilterService(CraftingOperations workbenchService) {
    RulesetManifestLoader.load();
    return new GoalFilterService(
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(workbenchService),
            GoalDefinitionsLoader.load(),
            RulesetManifestLoader.load().identity()));
  }

  @Bean
  AdditionPoolStore additionPoolStore(JdbcTemplate jdbc, ObjectMapper json) {
    return new JdbcAdditionPoolStore(jdbc, json);
  }

  @Bean
  AdditionPoolCache additionPoolCache(
      ItemCatalog catalog, AdditionPoolStore store, BackendSettings settings) {
    var registry = CraftingRegistryLoader.load();
    var digest =
        AdditionPoolCache.hash(
            registry.get("ledgerVersion").asText()
                + registry.get("assumptionLedger")
                + registry.get("modifierWeightPolicy")
                + registry.get("mixedKnownUnknownWeightPolicy")
                + RulesetManifestLoader.load().identity());
    return new AdditionPoolCache(
        catalog,
        store,
        WorkbenchSimulator.RULE_VERSION,
        digest,
        settings.cache().additionEntries());
  }

  @Bean
  SupportRecommendationService supportRecommendations(
      ItemCatalog catalog, AdditionPoolCache pools) {
    return new SupportRecommendationService(catalog, pools);
  }
}
