package com.poe2craft.crafting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Immutable versioned inventory, not proof that pending entries are executable rules. */
public final class CraftingRegistryLoader {
  private CraftingRegistryLoader() {}

  public static JsonNode load() {
    try (var stream =
        CraftingRegistryLoader.class.getResourceAsStream("/crafting/registry-v2.json")) {
      if (stream == null) throw new IllegalStateException("Missing crafting registry");
      return new ObjectMapper().readTree(stream);
    } catch (IOException ex) {
      throw new IllegalStateException("Invalid crafting registry", ex);
    }
  }
}
