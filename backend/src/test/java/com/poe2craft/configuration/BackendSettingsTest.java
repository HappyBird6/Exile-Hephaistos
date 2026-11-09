package com.poe2craft.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

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
