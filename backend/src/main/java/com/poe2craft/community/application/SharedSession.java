package com.poe2craft.community.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Public snapshots exclude private clipboard provenance and local library bookkeeping. */
public final class SharedSession {
  private SharedSession() {}

  public static JsonNode validate(JsonNode input, int maxBytes, int maxFrames) {
    if (input == null
        || !input.isObject()
        || input.toString().getBytes(StandardCharsets.UTF_8).length > maxBytes) bad();
    if (!input.path("id").isTextual()
        || input.path("id").asText().length() > 160
        || !input.path("createdAt").isTextual()) bad();
    try {
      Instant.parse(input.path("createdAt").asText());
    } catch (RuntimeException e) {
      bad();
    }
    var frames = input.path("frames");
    if (!frames.isArray() || frames.isEmpty() || frames.size() > maxFrames) bad();
    String base = null;
    for (var frame : frames) {
      var state = frame.path("state");
      String current = state.path("baseItemId").asText();
      if (!state.isObject()
          || current.isBlank()
          || current.length() > 256
          || !Set.of("NORMAL", "MAGIC", "RARE", "UNIQUE").contains(state.path("rarity").asText())
          || !state.path("itemLevel").isIntegralNumber()
          || state.path("itemLevel").asInt() < 1
          || state.path("itemLevel").asInt() > 100
          || !state.path("implicits").isArray()
          || !state.path("explicits").isArray()
          || !state.path("conditions").isArray()) bad();
      if (base != null && !base.equals(current)) bad();
      base = current;
      if (!frame.has("action")
          || !(frame.path("action").isNull() || frame.path("action").isTextual())) bad();
    }
    ObjectNode out = ((ObjectNode) input).deepCopy();
    out.retain("id", "createdAt", "rulesetIdentity", "compatibilityId", "frames");
    return out;
  }

  public static void bad() {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SHARED_SESSION");
  }
}
