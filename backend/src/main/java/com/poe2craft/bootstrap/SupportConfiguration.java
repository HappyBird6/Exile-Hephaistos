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
    java.util.function.BiFunction<
            com.poe2craft.crafting.application.goalfilter.GoalFilterService,
            com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.Create,
            com.poe2craft.crafting.application.pathsearch.AutomaticSearchService.Runtime>
        factory =
            (goalService, request) -> {
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
              var general = request.goal().general();
              goalService.validate(
                  new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context(
                      item.snapshotId(), item.baseItemId(), item.itemLevel()),
                  request.goal());
              var affixGoal =
                  new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
                      request.goal().version(),
                      request.goal().catalogVersion(),
                      new com.poe2craft.crafting.domain.goalfilter.GoalFilter.General(
                          general.baseItemId(), general.itemLevel(), general.rarities()),
                      request.goal().groups());
              java.util.function.Function<
                      com.poe2craft.item.ItemState,
                      com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation>
                  evaluate =
                      request.goal().groups().stream().anyMatch(g -> !g.disabled())
                          ? goalService.compileEvaluation(item, affixGoal)
                          : state ->
                              new com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation(
                                  1,
                                  affixGoal.catalogVersion(),
                                  affixGoal.general().rarities().contains(state.rarity())
                                          && affixGoal
                                              .general()
                                              .itemLevel()
                                              .contains(
                                                  java.math.BigDecimal.valueOf(state.itemLevel()))
                                      ? com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status
                                          .MATCH
                                      : com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status
                                          .NO_MATCH,
                                  com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH,
                                  java.util.List.of(),
                                  java.util.List.of());

              try {
                return new com.poe2craft.crafting.application.pathsearch.AutomaticSearchService
                    .Runtime(
                    workbench
                        .automaticKernel(item, policy)
                        .withRollPartition(
                            goalService.rollPartition(item, request.goal()),
                            json.writeValueAsString(request.goal()))
                        .withQualityGoal(general.quality() == null ? 0 : general.quality()),
                    state -> {
                      var result = evaluate.apply(state);
                      return new com.poe2craft.crafting.domain.pathsearch.AutomaticMethodGraph
                          .Progress(
                          result.status(),
                          result.groups().stream()
                              .flatMap(g -> g.entries().stream())
                              .map(e -> e.status())
                              .toList());
                    },
                    json.writeValueAsString(request.goal()),
                    goalService.minimumExplicitModifiers(item, request.goal()));
              } catch (java.io.IOException e) {
                throw new IllegalArgumentException("Cannot preserve objective", e);
              }
            };
    return new com.poe2craft.crafting.application.pathsearch.AutomaticSearchService(
        json,
        new LocalAutomaticJobRepository(java.nio.file.Path.of(directory).resolve("automatic-v1")),
        policy,
        request -> factory.apply(goals, request),
        java.time.Clock.systemUTC(),
        request -> {
          var item = request.start().item();
          var context =
              new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context(
                  item.snapshotId(), item.baseItemId(), item.itemLevel());
          var goal = request.goal();
          var goalService = goals;
          if (!goals.catalog(context).catalogVersion().equals(goal.catalogVersion())) {
            // Frozen definitions reproduce the exact historical catalog, never relabel its AST.
            var catalogs =
                com.poe2craft.crafting.infrastructure.goalfilter.BundledGoalCatalogs.load(
                    workbench);
            goalService = null;
            for (var definitions :
                java.util.List.of(
                    com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader
                        .loadHistoricalV3(),
                    com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader
                        .loadHistoricalV4())) {
              for (var identity :
                  java.util.Arrays.asList(
                      (String) null,
                      "reviewed-equipment-20261009-v2-68910c93fedbba8263972e2c97605e96271d6cf73fa6186088d3c9b76903cf1a",
                      "reviewed-equipment-20261009-v2-debb3f07052d54419a4732f49d9febf78508e23cd69a489698c5e81be80597f7",
                      "reviewed-equipment-20261009-v2-6346262eb5667dafc37a65907bb03f16469d2ce976559de50a4b6cb1a7a2d727")) {
                var index =
                    new com.poe2craft.crafting.domain.goalfilter.GoalCatalogIndex(
                        catalogs, definitions, identity);
                if (index.version().equals(goal.catalogVersion())) {
                  goalService =
                      new com.poe2craft.crafting.application.goalfilter.GoalFilterService(index);
                  break;
                }
              }
              if (goalService != null) break;
            }
            if (goalService == null)
              throw new IllegalArgumentException("Historical goal catalog changed");
          }
          var runtime =
              factory.apply(
                  goalService,
                  new com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.Create(
                      request.version(),
                      request.clientRequestId(),
                      new com.poe2craft.crafting.domain.BasicCurrencyState(
                          item, legacy.provenance(item.baseItemId())),
                      goal,
                      request.activeOmens()));
          try {
            return new com.poe2craft.crafting.application.pathsearch.AutomaticSearchService.Runtime(
                runtime.kernel(),
                runtime.evaluate(),
                json.writeValueAsString(request.goal()),
                runtime.minimumExplicitModifiers());
          } catch (java.io.IOException error) {
            throw new IllegalArgumentException("Cannot read historical objective", error);
          }
        });
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
