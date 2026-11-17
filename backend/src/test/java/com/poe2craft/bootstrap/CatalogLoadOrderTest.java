package com.poe2craft.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 카탈로그·Load·순서 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code historical(pool, extensions)}: 과거 정의의 읽기 전용 실행 환경을 조회한다. 입력: {@code pool}(후보풀),
 *       {@code extensions}(extensions). 반환: {@code ItemCatalog}. 사용하는 예외: {@code IOException}.
 *   <li>{@code extensionAndCompatibilityOrderRemainHistoricalWithoutSortingArrays()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
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
    var initial = ItemCatalogLoader.loadStocky();
    assertEquals(stocky.metadata(), initial.metadata());
    assertEquals(stocky.compatibleSnapshotIds(), initial.compatibleSnapshotIds());
    assertEquals(
        new ArrayList<>(stocky.modifiers().values()),
        new ArrayList<>(initial.modifiers().values()));
  }
}
