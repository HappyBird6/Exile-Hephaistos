package com.poe2craft.crafting.infrastructure.goalfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.item.ItemCatalog;
import java.io.IOException;
import java.util.ArrayList;
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

  private static List<Base> bases() {
    try (var stream =
        BundledGoalCatalogs.class.getResourceAsStream("/crafting/goalfilter/bases-v1.json")) {
      return readBases(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read goal base registry", e);
    }
  }

  /** Called once at bootstrap. initial() is pure: no rolls, writes, currency or remote requests. */
  public static List<ItemCatalog> load(WorkbenchService workbench) {
    var catalogs = new ArrayList<ItemCatalog>();
    var json = new ObjectMapper();
    for (var entry : bases()) {
      var reviewed = workbench.initial(entry.key(), 82);
      try (var stream =
          BundledGoalCatalogs.class.getResourceAsStream(
              "/catalog/" + entry.pool() + "/catalog.json")) {
        if (stream == null) throw new IllegalStateException("Missing reviewed base properties");
        var base = json.treeToValue(json.readTree(stream).get("base"), ItemCatalog.BaseItem.class);
        if (!base.id().equals(reviewed.state().baseItemId()))
          throw new IllegalStateException("Reviewed base identity differs");
        catalogs.add(
            new ItemCatalog(
                reviewed.metadata(),
                base,
                new ArrayList<>(reviewed.modifiers().values()),
                reviewed.compatibleSnapshotIds()));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read reviewed base properties", e);
      }
    }
    return List.copyOf(catalogs);
  }
}
