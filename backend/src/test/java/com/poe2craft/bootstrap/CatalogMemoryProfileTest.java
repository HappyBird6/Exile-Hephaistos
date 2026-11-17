package com.poe2craft.bootstrap;

import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 카탈로그 등록 후 동일 값의 옵션 객체가 공유되는지 측정한다. 인스턴스 필드는 없다. profilesRegisteredDefinitions는 실제
 * CatalogConfiguration으로 모든 베이스를 등록하고 identities(객체 정체성 집합)와 values(값 집합), references(전체 참조 수)를
 * 비교한다. 공유 카탈로그 재사용과 수정 불가 여부, 외부 증강물 JSON 변경으로 내부 정의가 오염되지 않는지도 검증한다. 힙 전체 사용량을 측정하는 테스트는 아니다.
 */
class CatalogMemoryProfileTest {
  @Test
  void profilesRegisteredDefinitions() {
    var config = new CatalogConfiguration();
    var catalog = config.itemCatalog(config.workbenchDefinitions());
    var service =
        config.workbenchService(
            catalog, config.workbenchSimulator(catalog, config.craftingEngine(catalog)));
    var identities = Collections.newSetFromMap(new IdentityHashMap<ModifierDefinition, Boolean>());
    var values = new HashSet<ModifierDefinition>();
    int references = 0;
    for (var base : service.reviewedCatalogs()) {
      references += base.modifiers().size();
      identities.addAll(base.modifiers().values());
      values.addAll(base.modifiers().values());
    }
    org.junit.jupiter.api.Assertions.assertEquals(
        values.size(),
        identities.size(),
        "Equal bundled modifier values must share an immutable instance");
    org.junit.jupiter.api.Assertions.assertSame(
        ItemCatalogLoader.loadDefault(), ItemCatalogLoader.loadDefault());
    org.junit.jupiter.api.Assertions.assertSame(
        ItemCatalogLoader.loadTopBase("soldier"), ItemCatalogLoader.loadTopBase("soldier"));
    org.junit.jupiter.api.Assertions.assertThrows(
        UnsupportedOperationException.class, () -> catalog.modifiers().clear());
    var exposed = com.poe2craft.shared.item.SocketAugments.catalog();
    String augmentId = exposed.get(0).path("id").asText();
    String originalKind = com.poe2craft.shared.item.SocketAugments.kind(augmentId);
    ((com.fasterxml.jackson.databind.node.ObjectNode) exposed.get(0))
        .put("kind", "mutated-response");
    org.junit.jupiter.api.Assertions.assertEquals(
        originalKind, com.poe2craft.shared.item.SocketAugments.kind(augmentId));
    System.out.printf(
        "CATALOG_MEMORY bases=%d references=%d objects=%d values=%d reusedLoad=%s%n",
        service.reviewedCatalogs().size(),
        references,
        identities.size(),
        values.size(),
        ItemCatalogLoader.loadDefault() == ItemCatalogLoader.loadDefault());
  }
}
