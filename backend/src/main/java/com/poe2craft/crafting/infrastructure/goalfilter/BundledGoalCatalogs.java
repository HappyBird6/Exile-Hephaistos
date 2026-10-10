package com.poe2craft.crafting.infrastructure.goalfilter;

import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.item.ItemCatalog;
import java.io.IOException;
import java.util.List;

/** Reuses Workbench's publicly exposed reviewed definitions without crossing item internals. */
public final class BundledGoalCatalogs {
  private BundledGoalCatalogs() {}

  public record Base(String key, String pool) {}

  public record Registry(int schemaVersion, List<Base> bases) {}

  public static List<Base> readBases(java.io.InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal base registry");
    var mapper = com.poe2craft.crafting.infrastructure.BundledJson.mapper();
    var registry = mapper.readValue(stream, Registry.class);
    if (registry.schemaVersion() != 1 || registry.bases() == null || registry.bases().isEmpty())
      throw new IllegalArgumentException("Invalid goal base registry version or entries");
    var keys = new java.util.HashSet<String>();
    var pools = new java.util.HashSet<String>();
    for (var base : registry.bases())
      if (base == null
          || base.key() == null
          || base.pool() == null
          || !base.key().matches("[a-z0-9-]+")
          || !base.pool().matches("[a-z0-9-]+")
          || !keys.add(base.key())
          || !pools.add(base.pool()))
        throw new IllegalArgumentException("Invalid or duplicate goal base identity");
    return registry.bases().stream().sorted(java.util.Comparator.comparing(Base::key)).toList();
  }

  /** Shares the complete starting-item inventory; registration requires no second goal list. */
  public static List<ItemCatalog> load(WorkbenchService workbench) {
    return workbench.reviewedCatalogs();
  }
}
