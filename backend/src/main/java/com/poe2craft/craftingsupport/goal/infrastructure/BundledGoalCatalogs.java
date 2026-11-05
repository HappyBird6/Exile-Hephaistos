package com.poe2craft.craftingsupport.goal.infrastructure;

import com.poe2craft.shared.crafting.application.CraftingCatalogs;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.util.List;

/** Reuses Workbench's publicly exposed reviewed definitions without crossing item internals. */
public final class BundledGoalCatalogs {
  private BundledGoalCatalogs() {}

  public record Base(String key, String pool) {}

  public record Registry(int schemaVersion, List<Base> bases) {}

  public static List<Base> readBases(java.io.InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal base registry");
    var mapper = BundledJson.mapper();
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
  public static List<ItemCatalog> load(CraftingCatalogs workbench) {
    return workbench.reviewedCatalogs();
  }

  /** Frozen membership reproduces saved catalog signatures before the base-picker expansion. */
  public static List<ItemCatalog> loadHistoricalV5(CraftingCatalogs workbench) {
    try (var stream =
        SealedResources.open("/crafting/goalfilter/base-inventory-v5-readonly.json")) {
      var ids =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .readValue(
                  stream,
                  new com.fasterxml.jackson.core.type.TypeReference<java.util.Set<String>>() {});
      var catalogs = load(workbench).stream().filter(c -> ids.contains(c.base().id())).toList();
      if (catalogs.size() != ids.size())
        throw new IllegalStateException("Historical base inventory changed");
      return catalogs;
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read historical base inventory", error);
    }
  }
}
