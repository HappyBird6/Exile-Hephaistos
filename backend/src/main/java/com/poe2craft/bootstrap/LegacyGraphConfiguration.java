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

/**
 * 기존 그래프 탐색과 최초 성공 확률 API의 계산기를 연결한다.
 *
 * <p>기존 계약: Compatibility graph and first-hit APIs; independent from persistent search-job wiring.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code basicPathService(catalog)}: 기본·경로·서비스 처리에서 {@code RulesetManifestLoader.load}을(를)
 *       사용한다. 입력: {@code catalog}(카탈로그). 반환: {@code BasicPathService}.
 *   <li>{@code transitionCache(engine, settings)}: 전이·캐시 처리에서 {@code settings.cache}을(를) 사용한다. 입력:
 *       {@code engine}(계산엔진), {@code settings}(운영설정). 반환: {@code TransitionCache}.
 *   <li>{@code graphExplorer(cache)}: 그래프·Explorer의 입력에 따른 분기·결과를 처리한다. 입력: {@code cache}(캐시). 반환:
 *       {@code GraphExplorer}.
 * </ul>
 */
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
