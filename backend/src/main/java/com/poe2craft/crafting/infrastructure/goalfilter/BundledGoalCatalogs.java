package com.poe2craft.crafting.infrastructure.goalfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.item.ItemCatalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Reuses Workbench's publicly exposed reviewed definitions without crossing item internals. */
public final class BundledGoalCatalogs {
  private BundledGoalCatalogs() {}

  private static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry("solar", "solar-amulet"),
          Map.entry("stocky", "stocky-mitts"),
          Map.entry("bow", "crude-bow"),
          Map.entry("wand", "attuned-wand"),
          Map.entry("body", "rusted-cuirass"),
          Map.entry("sceptre", "rattling-sceptre"),
          Map.entry("belt", "rawhide-belt"),
          Map.entry("helmet", "rusted-greathelm"),
          Map.entry("ring", "iron-ring"),
          Map.entry("sapphire", "sapphire"),
          Map.entry("ruby", "ruby"),
          Map.entry("emerald", "emerald"),
          Map.entry("diamond", "diamond"),
          Map.entry("time-lost-ruby", "time-lost-ruby"),
          Map.entry("time-lost-emerald", "time-lost-emerald"),
          Map.entry("time-lost-sapphire", "time-lost-sapphire"),
          Map.entry("time-lost-diamond", "time-lost-diamond"));

  /** Called once at bootstrap. initial() is pure: no rolls, writes, currency or remote requests. */
  public static List<ItemCatalog> load(WorkbenchService workbench) {
    var catalogs = new ArrayList<ItemCatalog>();
    var json = new ObjectMapper();
    for (var entry : new TreeMap<>(BASES).entrySet()) {
      var reviewed = workbench.initial(entry.getKey(), 82);
      try (var stream =
          BundledGoalCatalogs.class.getResourceAsStream(
              "/catalog/" + entry.getValue() + "/catalog.json")) {
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
