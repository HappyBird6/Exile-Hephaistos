package com.poe2craft.crafting.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/** Immutable display documents; never accepted as evidence for executing a craft. */
public final class SessionSnapshots {
  public static final int MAX_BYTES = 2_000_000;

  public interface Repository {
    String create(byte[] document) throws IOException;

    byte[] read(String id) throws IOException;
  }

  public static void validate(JsonNode document) {
    int[] count = {0};
    tree(document, 0, count);
    if (!document.isObject()
        || !"exile-session".equals(document.path("format").asText())
        || !document.path("version").isInt()
        || document.path("version").intValue() != 1) throw new IllegalArgumentException();
    if (!document.path("exportedAt").isTextual()) throw new IllegalArgumentException();
    var film = document.path("film");
    if (!film.isObject()
        || !film.path("id").isTextual()
        || film.path("id").asText().isBlank()
        || !film.path("createdAt").isTextual()
        || !film.path("frames").isArray()
        || film.path("frames").isEmpty()
        || film.path("frames").size() > 2000) throw new IllegalArgumentException();
    for (var frame : film.path("frames")) {
      if (!frame.isObject()
          || !frame.path("state").isObject()
          || !(frame.path("action").isTextual() || frame.path("action").isNull()))
        throw new IllegalArgumentException();
      var state = frame.path("state");
      if (!state.path("baseItemId").isTextual()
          || !state.path("implicits").isArray()
          || !state.path("explicits").isArray()
          || !state.path("conditions").isArray()) throw new IllegalArgumentException();
    }
  }

  private static void tree(JsonNode node, int depth, int[] count) {
    if (node == null || depth > 32 || ++count[0] > 100000) throw new IllegalArgumentException();
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              entry -> {
                if (java.util.Set.of("__proto__", "constructor", "prototype")
                    .contains(entry.getKey())) throw new IllegalArgumentException();
                tree(entry.getValue(), depth + 1, count);
              });
    } else if (node.isArray()) {
      for (var child : node) tree(child, depth + 1, count);
    }
  }
}
