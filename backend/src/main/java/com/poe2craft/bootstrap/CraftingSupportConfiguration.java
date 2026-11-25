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
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 목표 평가·추천·경로 탐색 서비스의 의존성과 작업 실행기를 구성한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code automaticSearchService(workbench, goals, legacy, json, settings)}: 자동·탐색·서비스 처리에서
 *       {@code settings.storage}을(를) 사용한다. 입력: {@code workbench}(작업대), {@code goals}(목표목록), {@code
 *       legacy}(구버전), {@code json}(JSON), {@code settings}(운영설정). 반환: {@code
 *       AutomaticSearchService}.
 *   <li>{@code automaticSearchService(workbench, goals, legacy, json, directory)}: 자동·탐색·서비스 처리에서
 *       {@code BackendSettings.defaults}을(를) 사용한다. 입력: {@code workbench}(작업대), {@code goals}(목표목록),
 *       {@code legacy}(구버전), {@code json}(JSON), {@code directory}(디렉터리). 반환: {@code
 *       AutomaticSearchService}.
 *   <li>{@code automaticSearchService(workbench, goals, legacy, json, directory, settings)}:
 *       자동·탐색·서비스 처리에서 {@code AutomaticCurrencyPolicyLoader.load}, {@code Path.of}을(를) 사용한다. 입력:
 *       {@code workbench}(작업대), {@code goals}(목표목록), {@code legacy}(구버전), {@code json}(JSON),
 *       {@code directory}(디렉터리), {@code settings}(운영설정). 반환: {@code AutomaticSearchService}.
 *   <li>{@code methodSearchService(workbench, goals, json, settings)}: 제작방법·탐색·서비스 처리에서 {@code
 *       settings.storage}, {@code workbench.reviewedCatalogs}을(를) 사용한다. 입력: {@code workbench}(작업대),
 *       {@code goals}(목표목록), {@code json}(JSON), {@code settings}(운영설정). 반환: {@code
 *       MethodSearchService}.
 *   <li>{@code pathSearchService(catalog, goals, json, settings)}: 경로·탐색·서비스 처리에서 {@code
 *       RulesetManifestLoader.load}, {@code settings.pathSearch}을(를) 사용한다. 입력: {@code
 *       catalog}(카탈로그), {@code goals}(목표목록), {@code json}(JSON), {@code settings}(운영설정). 반환: {@code
 *       PathSearchService}.
 *   <li>{@code goalFilterService(workbenchService)}: 목표·필터·서비스 처리에서 {@code
 *       RulesetManifestLoader.load}, {@code BundledGoalCatalogs.load}을(를) 사용한다. 입력: {@code
 *       workbenchService}(작업대·서비스). 반환: {@code GoalFilterService}.
 *   <li>{@code additionPoolStore(jdbc, json)}: 추가·후보풀·저장소의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       jdbc}(JDBC), {@code json}(JSON). 반환: {@code AdditionPoolStore}.
 *   <li>{@code additionPoolCache(catalog, store, settings)}: 추가·후보풀·캐시 처리에서 {@code
 *       CraftingRegistryLoader.load}, {@code AdditionPoolCache.hash}을(를) 사용한다. 입력: {@code
 *       catalog}(카탈로그), {@code store}(저장소), {@code settings}(운영설정). 반환: {@code AdditionPoolCache}.
 *   <li>{@code supportRecommendations(catalog, pools)}: 지원범위·추천목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       catalog}(카탈로그), {@code pools}(후보풀목록). 반환: {@code SupportRecommendationService}.
 * </ul>
 */
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

  /** 테스트 탭의 입력 검증과 규칙 계산을 작업대·목표 필터·테스트 탐색 설정에 연결한다. */
  @Bean
  TestRouteService testRouteService(
      CraftingOperations workbench, GoalFilterService goals, BackendSettings settings) {
    return new TestRouteService(workbench, goals, settings.testRoutes());
  }

  @Bean
  GoalFilterService goalFilterService(CraftingOperations workbenchService) {
    RulesetManifestLoader.load();
    return new GoalFilterService(
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(workbenchService).stream()
                .map(ManualRevealCatalog::enrichOtherworldly)
                .toList(),
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
