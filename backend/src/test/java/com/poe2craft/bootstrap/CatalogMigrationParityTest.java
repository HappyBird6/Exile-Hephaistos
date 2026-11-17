package com.poe2craft.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * 카탈로그·Migration·동일성 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code allReviewedCatalogsRetainTheirGameplayDataAndOrdering()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code canonical(value)}: 정규형 처리에서 {@code value.isObject}, {@code
 *       mapper.createObjectNode}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code JsonNode}.
 * </ul>
 */
class CatalogMigrationParityTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void allReviewedCatalogsRetainTheirGameplayDataAndOrdering() throws Exception {
    var cfg = new CatalogConfiguration();
    var catalog = cfg.itemCatalog(cfg.workbenchDefinitions());
    var service =
        cfg.workbenchService(catalog, cfg.workbenchSimulator(catalog, cfg.craftingEngine(catalog)));
    var hashes = new TreeMap<String, String>();
    for (var reviewed : service.reviewedCatalogs()) {
      var document = mapper.createObjectNode();
      document.set("metadata", mapper.valueToTree(reviewed.metadata()));
      document.set("base", mapper.valueToTree(reviewed.base()));
      document.set("compatibility", mapper.valueToTree(reviewed.compatibleSnapshotIds()));
      document.set("modifiers", mapper.valueToTree(new ArrayList<>(reviewed.modifiers().values())));
      hashes.put(
          reviewed.base().id(),
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(mapper.writeValueAsBytes(canonical(document)))));
    }
    var expected = Path.of("src/test/resources/catalog-migration-sha256.json");
    // Captured from 278b3789 before reorganizing source data; array order is significant.
    assertEquals(mapper.readTree(expected.toFile()), mapper.valueToTree(hashes));
  }

  private JsonNode canonical(JsonNode value) {
    if (value.isObject()) {
      var result = mapper.createObjectNode();
      var names = new TreeSet<String>();
      value.fieldNames().forEachRemaining(names::add);
      for (var name : names) {
        JsonNode child = value.get(name);
        if ((name.equals("tags") || name.equals("familyIds")) && child.isArray()) {
          var sorted = new TreeSet<String>();
          child.forEach(entry -> sorted.add(entry.asText()));
          child = mapper.valueToTree(sorted);
        }
        result.set(name, canonical(child));
      }
      return result;
    }
    if (value.isArray()) {
      var result = mapper.createArrayNode();
      value.forEach(child -> result.add(canonical(child)));
      return result;
    }
    return value;
  }
}
