package com.poe2craft.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CatalogLoadOrderTest {
  private ItemCatalog historical(String pool, List<String> extensions) throws IOException {
    String root = "/catalog/" + pool + "/";
    ItemCatalog catalog;
    try (var data = getClass().getResourceAsStream(root + "catalog.json");
        var raw = getClass().getResourceAsStream(root + "base.raw.json");
        var details = getClass().getResourceAsStream(root + "details.raw.json")) {
      catalog = ItemCatalogLoader.load(data, raw, details);
    }
    for (String extension : extensions) {
      try (var data = getClass().getResourceAsStream(root + extension + ".catalog.json");
          var raw = getClass().getResourceAsStream(root + extension + ".raw.json")) {
        catalog = ItemCatalogLoader.addSpecial(catalog, data, raw);
      }
    }
    return catalog;
  }

  @Test
  void extensionAndCompatibilityOrderRemainHistoricalWithoutSortingArrays() throws Exception {
    var solar =
        historical(
            "solar-amulet",
            List.of(
                "perfect-infinite",
                "perfect-enhancement",
                "breach-essence",
                "runic-alloy",
                "abyss-essence"));
    var actualSolar = ItemCatalogLoader.loadDefault();
    assertEquals(solar.metadata(), actualSolar.metadata());
    assertEquals(solar.compatibleSnapshotIds(), actualSolar.compatibleSnapshotIds());
    assertEquals(
        new ArrayList<>(solar.modifiers().values()),
        new ArrayList<>(actualSolar.modifiers().values()));
    var stocky =
        historical(
            "stocky-mitts",
            List.of(
                "abyss-essence",
                "horror-essence",
                "perfect-grounding-opulence",
                "prismatic-alloy",
                "scalar-alloys",
                "reviewed-alloys"));
    var cfg = new CraftingConfiguration();
    var initial =
        cfg.workbenchService(
                actualSolar, cfg.workbenchSimulator(actualSolar, cfg.craftingEngine(actualSolar)))
            .initial("stocky", 82);
    assertEquals(stocky.metadata(), initial.metadata());
    assertEquals(stocky.compatibleSnapshotIds(), initial.compatibleSnapshotIds());
    assertEquals(
        new ArrayList<>(stocky.modifiers().values()),
        new ArrayList<>(initial.modifiers().values()));
  }
}
