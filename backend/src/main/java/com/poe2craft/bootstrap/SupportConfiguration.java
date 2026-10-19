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
  @Bean(destroyMethod = "close")
  com.poe2craft.crafting.application.pathsearch.AutomaticSearchService automaticSearchService(
      WorkbenchService workbench,
      com.poe2craft.crafting.application.goalfilter.GoalFilterService goals,
      com.poe2craft.crafting.application.pathsearch.MethodSearchService legacy,
      ObjectMapper json,
      @org.springframework.beans.factory.annotation.Value(
              "${crafting.method-job-directory:./data/method-jobs}")
          String directory) {
    var policy = AutomaticCurrencyPolicyLoader.load();
    return new com.poe2craft.crafting.application.pathsearch.AutomaticSearchService(
        json,
        new LocalAutomaticJobRepository(java.nio.file.Path.of(directory).resolve("automatic-v1")),
        policy,
        request -> {
          if (request == null
              || !Integer.valueOf(2).equals(request.version())
              || request.start() == null
              || request.goal() == null
              || request.activeOmens() == null
              || !request.activeOmens().isEmpty())
            throw new com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected(
                422, "INVALID_REQUEST");
          var item = request.start().item();
          if (!legacy.provenance(item.baseItemId()).equals(request.start().provenance()))
            throw new com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected(
                422, "RULESET_IDENTITY_MISMATCH");
          var catalog =
              workbench.reviewedCatalogs().stream()
                  .filter(c -> c.base().id().equals(item.baseItemId()))
                  .findFirst()
                  .orElseThrow();
          if (!new com.poe2craft.item.ItemStateValidator(catalog).validate(item).isEmpty()
              || item.explicits().size() > 4)
            throw new com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected(
                422, "INVALID_ITEM");
          var evaluate = goals.compileEvaluation(item, request.goal());

          try {
            return new com.poe2craft.crafting.application.pathsearch.AutomaticSearchService.Runtime(
                workbench
                    .automaticKernel(item, policy)
                    .withRollPartition(
                        goals.rollPartition(item, request.goal()),
                        json.writeValueAsString(request.goal())),
                state -> {
                  var result = evaluate.apply(state);
                  return new com.poe2craft.crafting.domain.pathsearch.AutomaticMethodGraph.Progress(
                      result.status(),
                      result.groups().stream()
                          .flatMap(g -> g.entries().stream())
                          .map(e -> e.status())
                          .toList());
                },
                json.writeValueAsString(request.goal()),
                goals.minimumExplicitModifiers(item, request.goal()));
          } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Cannot preserve objective", e);
          }
        },
        java.time.Clock.systemUTC());
  }

  @Bean(destroyMethod = "close")
  com.poe2craft.crafting.application.pathsearch.MethodSearchService methodSearchService(
      WorkbenchService workbench,
      com.poe2craft.crafting.application.goalfilter.GoalFilterService goals,
      ObjectMapper json,
      @org.springframework.beans.factory.annotation.Value(
              "${crafting.method-job-directory:./data/method-jobs}")
          String directory) {
    var service =
        new com.poe2craft.crafting.application.pathsearch.MethodSearchService(
            workbench.reviewedCatalogs(), goals, RulesetManifestLoader.load().identity(), json);
    service.persistence(new LocalCertifiedJobRepository(java.nio.file.Path.of(directory), json));
    return service;
  }

  @Bean(destroyMethod = "close")
  com.poe2craft.crafting.application.pathsearch.PathSearchService pathSearchService(
      ItemCatalog catalog,
      com.poe2craft.crafting.application.goalfilter.GoalFilterService goals,
      ObjectMapper json) {
    return new com.poe2craft.crafting.application.pathsearch.PathSearchService(
        catalog, goals, RulesetManifestLoader.load().identity(), json);
  }

  @Bean
  com.poe2craft.crafting.application.goalfilter.GoalFilterService goalFilterService(
      WorkbenchService workbenchService) {
    RulesetManifestLoader.load();
    return new com.poe2craft.crafting.application.goalfilter.GoalFilterService(
        new com.poe2craft.crafting.domain.goalfilter.GoalCatalogIndex(
            com.poe2craft.crafting.infrastructure.goalfilter.BundledGoalCatalogs.load(
                workbenchService),
            com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load(),
            RulesetManifestLoader.load().identity()));
  }

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
                + registry.get("mixedKnownUnknownWeightPolicy")
                + RulesetManifestLoader.load().identity());
    return new AdditionPoolCache(catalog, store, WorkbenchSimulator.RULE_VERSION, digest, 2048);
  }

  @Bean
  SupportRecommendations supportRecommendations(ItemCatalog catalog, AdditionPoolCache pools) {
    return new SupportRecommendations(catalog, pools);
  }
}
