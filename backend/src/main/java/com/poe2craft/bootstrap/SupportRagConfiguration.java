package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.rag.application.RagComparisonService;
import com.poe2craft.craftingsupport.rag.application.StrategyKnowledge;
import com.poe2craft.craftingsupport.rag.infrastructure.*;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.infrastructure.*;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class SupportRagConfiguration {
  @Bean
  RagComparisonService ragComparisonService(
      CraftingOperations workbench,
      GoalFilterService goals,
      MethodSearchService legacy,
      ObjectMapper json,
      BackendSettings settings,
      StringRedisTemplate redis,
      @Value("${OPENAI_API_KEY:}") String apiKey) {
    var policy = AutomaticCurrencyPolicyLoader.load();
    var limits = settings.supportRag();
    var clock = Clock.systemUTC();
    StrategyKnowledge knowledge;
    try {
      knowledge = BundledStrategyKnowledge.load(json);
    } catch (IllegalArgumentException | IllegalStateException invalidCorpus) {
      org.slf4j.LoggerFactory.getLogger(SupportRagConfiguration.class)
          .warn("RAG test unavailable: bundled strategy documents failed validation");
      knowledge = null;
    }
    return new RagComparisonService(
        new BundledSearchRuntimeFactory(workbench, goals, legacy, json, policy),
        policy,
        knowledge,
        new OpenAiStrategyModel(json, limits, apiKey, new RedisRagCallBudget(redis, limits, clock)),
        limits,
        json,
        clock);
  }
}
