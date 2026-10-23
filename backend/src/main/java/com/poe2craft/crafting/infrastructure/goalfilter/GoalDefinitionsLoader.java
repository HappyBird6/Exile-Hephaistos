package com.poe2craft.crafting.infrastructure.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalDefinitions;
import java.io.*;

/** Strict bundled configuration; domain receives only immutable typed data. */
public final class GoalDefinitionsLoader {
  private GoalDefinitionsLoader() {}

  public static GoalDefinitions load() {
    try (var stream =
        com.poe2craft.support.SealedResources.open("/crafting/goalfilter/definitions-v1.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load goal definitions", e);
    }
  }

  public static GoalDefinitions read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal definitions");
    var mapper = com.poe2craft.crafting.infrastructure.BundledJson.mapper();
    var data = mapper.readTree(stream);
    if (data.has("stats") && data.get("stats").isArray())
      for (var stat : data.get("stats"))
        if (stat.isObject() && !stat.has("sourceDivisor"))
          ((com.fasterxml.jackson.databind.node.ObjectNode) stat).put("sourceDivisor", 1);
    return mapper.treeToValue(data, GoalDefinitions.class);
  }

  /** Frozen additive-era definitions are used exclusively to read old observations. */
  public static GoalDefinitions loadHistoricalV3() {
    try (var stream =
        com.poe2craft.support.SealedResources.open(
            "/crafting/goalfilter/definitions-v3-readonly.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load historical goal definitions", e);
    }
  }

  /** Frozen direct-ring definitions predate reviewed rate conversions. */
  public static GoalDefinitions loadHistoricalV4() {
    try (var stream =
        com.poe2craft.support.SealedResources.open(
            "/crafting/goalfilter/definitions-v4-readonly.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load historical goal definitions", e);
    }
  }
}
