package com.poe2craft.crafting.infrastructure.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalDefinitions;
import java.io.*;

/** Strict bundled configuration; domain receives only immutable typed data. */
public final class GoalDefinitionsLoader {
  private GoalDefinitionsLoader() {}

  public static GoalDefinitions load() {
    try (var stream =
        GoalDefinitionsLoader.class.getResourceAsStream(
            "/crafting/goalfilter/definitions-v1.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load goal definitions", e);
    }
  }

  public static GoalDefinitions read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal definitions");
    var mapper = com.poe2craft.crafting.infrastructure.BundledJson.mapper();
    return mapper.readValue(stream, GoalDefinitions.class);
  }
}
