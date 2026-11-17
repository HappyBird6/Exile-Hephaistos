package com.poe2craft.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

/**
 * 환경별 운영 한도와 저장·로그·요청 설정을 타입으로 표현한다. 게임 규칙은 포함하지 않는다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code runner}: 실행기 ({@code ApplicationContextRunner}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code springAndDirectConstructionUseTheSameShippedDefaults()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code overridesBindToTypedFeatureSettings()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code oldJobDirectoryPropertyStillResolvesAndNewSettingCanOverrideIt()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code zeroCapacityAndBudgetPreventStartup()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code nonPositiveDurationsPreventStartup()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class BackendSettingsTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(BackendConfiguration.class)
          .withPropertyValues("backend.prices.proxy-url=")
          .withInitializer(
              context -> {
                try {
                  for (var source :
                      new YamlPropertySourceLoader()
                          .load(
                              "backend-defaults", new ClassPathResource("backend-settings.yaml"))) {
                    context.getEnvironment().getPropertySources().addLast(source);
                  }
                } catch (IOException error) {
                  throw new UncheckedIOException(error);
                }
              });

  @Test
  void springAndDirectConstructionUseTheSameShippedDefaults() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(BackendSettings.class)).isEqualTo(BackendSettings.defaults());
        });
  }

  @Test
  void overridesBindToTypedFeatureSettings() {
    runner
        .withPropertyValues(
            "backend.path-search.max-jobs=3",
            "backend.path-search.ttl=7m",
            "backend.workbench.repeat-concurrency=2")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var settings = context.getBean(BackendSettings.class);
              assertThat(settings.pathSearch().maxJobs()).isEqualTo(3);
              assertThat(settings.pathSearch().ttl()).isEqualTo(Duration.ofMinutes(7));
              assertThat(settings.workbench().repeatConcurrency()).isEqualTo(2);
            });
  }

  @Test
  void oldJobDirectoryPropertyStillResolvesAndNewSettingCanOverrideIt() {
    runner
        .withPropertyValues("crafting.method-job-directory=./custom-jobs")
        .run(
            context ->
                assertThat(context.getBean(BackendSettings.class).storage().methodJobDirectory())
                    .isEqualTo("./custom-jobs"));
    runner
        .withPropertyValues(
            "crafting.method-job-directory=./custom-jobs",
            "backend.storage.method-job-directory=./new-jobs")
        .run(
            context ->
                assertThat(context.getBean(BackendSettings.class).storage().methodJobDirectory())
                    .isEqualTo("./new-jobs"));
  }

  @Test
  void zeroCapacityAndBudgetPreventStartup() {
    for (String property :
        new String[] {
          "backend.path-search.max-jobs=0",
          "backend.method-search.run-budget=0",
          "backend.workbench.repeat-concurrency=0"
        }) {
      runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }
  }

  @Test
  void nonPositiveDurationsPreventStartup() {
    for (String property :
        new String[] {
          "backend.path-search.ttl=0s",
          "backend.method-search.time-slice=-1ms",
          "backend.automatic-search.ttl=-1h"
        }) {
      runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }
  }
}
