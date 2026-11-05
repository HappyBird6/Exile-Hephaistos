package com.poe2craft.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;

/** Typed operational settings. Edit backend-settings.yaml; game rules remain in sealed catalogs. */
@Validated
@ConfigurationProperties("backend")
public record BackendSettings(
    @Valid @NotNull PathSearch pathSearch,
    @Valid @NotNull MethodSearch methodSearch,
    @Valid @NotNull CertifiedSearch certifiedSearch,
    @Valid @NotNull AutomaticSearch automaticSearch,
    @Valid @NotNull Cache cache,
    @Valid @NotNull Storage storage,
    @Valid @NotNull Workbench workbench) {

  public record PathSearch(
      @Min(1) int maxJobs,
      @Min(1) int maxRevisions,
      @Min(1) int pageEdges,
      @Min(1) int maxCommands,
      @Min(1) int runBudget,
      @NotNull Duration ttl) {
    public PathSearch {
      positive(ttl, "path-search.ttl");
    }
  }

  public record MethodSearch(
      @Min(1) int maxJobs,
      @Min(1) int maxRevisions,
      @Min(1) int pageSize,
      @Min(1) int maxCommands,
      @Min(1) int runBudget,
      @Min(1) int certifiedRunBudget,
      @Min(1) int kernelOutcomeLimit,
      @NotNull Duration timeSlice,
      @NotNull Duration ttl) {
    public MethodSearch {
      positive(timeSlice, "method-search.time-slice");
      positive(ttl, "method-search.ttl");
    }
  }

  public record CertifiedSearch(@Min(1) int maxJobs, @NotNull Duration ttl) {
    public CertifiedSearch {
      positive(ttl, "certified-search.ttl");
    }
  }

  public record AutomaticSearch(
      @Min(1) int maxActive, @NotNull Duration ttl, @NotNull Duration timeSlice) {
    public AutomaticSearch {
      positive(ttl, "automatic-search.ttl");
      positive(timeSlice, "automatic-search.time-slice");
    }
  }

  public record Cache(
      @Min(1) int transitionEntries, @Min(1) int transitionOutcomes, @Min(1) int additionEntries) {}

  public record Storage(@NotBlank String methodJobDirectory) {}

  public record Workbench(@Min(1) int repeatConcurrency) {}

  private static void positive(Duration value, String name) {
    if (value == null || value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException(name + " must be a positive duration");
    }
  }

  /**
   * Direct construction in unit tests uses the same shipped defaults as Spring, never a second
   * list.
   */
  public static BackendSettings defaults() {
    return Defaults.VALUE;
  }

  private static final class Defaults {
    private static final BackendSettings VALUE = load();

    private static BackendSettings load() {
      try {
        var environment = new StandardEnvironment();
        // Defaults must not change with the shell running a unit test.
        environment
            .getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment
            .getPropertySources()
            .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        for (var source :
            new YamlPropertySourceLoader()
                .load("backend-defaults", new ClassPathResource("backend-settings.yaml"))) {
          environment.getPropertySources().addLast(source);
        }
        return new Binder(
                ConfigurationPropertySources.get(environment),
                new org.springframework.boot.context.properties.bind
                    .PropertySourcesPlaceholdersResolver(environment))
            .bind("backend", Bindable.of(BackendSettings.class))
            .orElseThrow(() -> new IllegalStateException("Backend defaults are missing"));
      } catch (IOException error) {
        throw new IllegalStateException("Cannot load backend-settings.yaml", error);
      }
    }
  }
}
