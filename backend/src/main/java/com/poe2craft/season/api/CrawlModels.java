package com.poe2craft.season.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CrawlModels {
  private CrawlModels() {}

  public record Target(
      UUID id,
      String name,
      String url,
      @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) boolean enabled) {}

  public record Update(
      @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) long version,
      List<Target> targets) {}

  public record Limits(int maxTargets) {}

  public record Settings(
      long version, List<Target> targets, boolean runnerEnabled, Limits limits) {}

  public record Source(String url, int status, String sha256, long bytes, Instant fetchedAt) {}

  public record Run(
      UUID id,
      String status,
      Instant createdAt,
      Instant startedAt,
      Instant finishedAt,
      List<Target> targets,
      String errorCode,
      Integer sourceCount,
      List<Source> sources) {}
}
