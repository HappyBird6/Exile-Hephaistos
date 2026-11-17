package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 카탈로그·검증·데이터의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>기존 계약: Explicit inventory of checked-in bases; never assumes unpushed registries or fetches a
 * URL.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ReviewedCatalogTestData()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code List<ItemCatalog>}.
 *   <li>{@code stocky()}: StockyMitts 값을 조회·구성한다. 반환: {@code ItemCatalog}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code workbench()}: 작업대 값을 조회·구성한다. 반환: {@code CatalogCraftingService}.
 * </ul>
 */
public final class ReviewedCatalogTestData {
  private ReviewedCatalogTestData() {}

  public static List<ItemCatalog> load() {
    var catalogs = new ArrayList<ItemCatalog>();
    catalogs.add(ItemCatalogLoader.loadDefault());
    catalogs.add(stocky());
    catalogs.add(ItemCatalogLoader.loadBow());
    catalogs.add(ItemCatalogLoader.loadWand());
    catalogs.add(ItemCatalogLoader.loadBody());
    catalogs.add(ItemCatalogLoader.loadSceptre());
    catalogs.add(ItemCatalogLoader.loadBelt());
    catalogs.add(ItemCatalogLoader.loadHelmet());
    catalogs.add(ItemCatalogLoader.loadRing());
    catalogs.add(ItemCatalogLoader.loadSapphire());
    for (var base :
        List.of(
            "ruby",
            "emerald",
            "diamond",
            "time-lost-ruby",
            "time-lost-emerald",
            "time-lost-sapphire",
            "time-lost-diamond")) catalogs.add(ItemCatalogLoader.loadBasicJewel(base));
    return List.copyOf(catalogs);
  }

  private static ItemCatalog stocky() {
    String root = "/catalog/stocky-mitts/";
    try (var data = ReviewedCatalogTestData.class.getResourceAsStream(root + "catalog.json");
        var raw = ReviewedCatalogTestData.class.getResourceAsStream(root + "base.raw.json");
        var details =
            ReviewedCatalogTestData.class.getResourceAsStream(root + "details.raw.json")) {
      var catalog = ItemCatalogLoader.load(data, raw, details);
      for (var extension :
          List.of(
              "abyss-essence",
              "horror-essence",
              "perfect-grounding-opulence",
              "prismatic-alloy",
              "scalar-alloys",
              "reviewed-alloys")) {
        try (var special =
                ReviewedCatalogTestData.class.getResourceAsStream(
                    root + extension + ".catalog.json");
            var specialRaw =
                ReviewedCatalogTestData.class.getResourceAsStream(root + extension + ".raw.json")) {
          catalog = ItemCatalogLoader.addSpecial(catalog, special, specialRaw);
        }
      }
      return catalog;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed Stocky Mitts snapshot", e);
    }
  }

  public static CatalogCraftingService workbench() {
    var c = load();
    return new CatalogCraftingService(
        c.get(0),
        new WorkbenchSimulator(c.get(0), new CraftingEngine(c.get(0))),
        c.get(1),
        c.get(2),
        c.get(3),
        c.get(4),
        c.get(5),
        c.get(6),
        c.get(7),
        c.get(8),
        c.get(9),
        c.get(10),
        c.get(11),
        c.get(12),
        c.get(13),
        c.get(14),
        c.get(15),
        c.get(16));
  }
}
