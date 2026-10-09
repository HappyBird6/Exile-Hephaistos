package com.poe2craft.crafting.infrastructure;

import com.poe2craft.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.item.ModifierDefinition;
import com.poe2craft.support.SealedResources;
import java.io.*;
import java.util.*;

/** Typed metadata, not an expression language; no domain resource access. */
public final class WorkbenchDefinitionsLoader {
  private WorkbenchDefinitionsLoader() {}

  public static WorkbenchDefinitions read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing Workbench definitions");
    return new WorkbenchDefinitions(
        BundledJson.mapper().readValue(stream, WorkbenchDefinitions.Document.class));
  }

  public static WorkbenchDefinitions load() {
    try (var stream = SealedResources.open("/crafting/workbench-definitions-v1.json")) {
      var definitions = read(stream);
      definitions.validateTargets(readTargets());
      return definitions;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Workbench definitions", e);
    }
  }

  static Map<String, List<ModifierDefinition>> readTargets() throws IOException {
    var ids = new HashMap<String, List<ModifierDefinition>>();
    var mapper = BundledJson.mapper();
    for (var path : SealedResources.requiredPaths()) {
      if (!path.startsWith("catalog/") || !path.endsWith("catalog.json")) continue;
      try (var catalog = SealedResources.open("/" + path)) {
        for (var modifier : mapper.readTree(catalog).path("modifiers")) {
          var target = mapper.treeToValue(modifier, ModifierDefinition.class);
          // IDs can recur in different base catalogs with different sourced ranges/weights.
          // Validate references without merging or rewriting the actual catalog definitions.
          ids.computeIfAbsent(target.id(), ignored -> new ArrayList<>()).add(target);
        }
      }
    }
    ids.replaceAll((id, references) -> List.copyOf(references));
    return Map.copyOf(ids);
  }

  private static final class Loaded {
    private static final WorkbenchDefinitions DEFINITIONS = load();
  }

  public static WorkbenchDefinitions initialize() {
    return WorkbenchDefinitions.initialize(Loaded.DEFINITIONS);
  }
}
