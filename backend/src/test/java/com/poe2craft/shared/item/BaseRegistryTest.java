package com.poe2craft.shared.item;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/**
 * 검토된 베이스 정체성과 명시적인 장비 분류 정책을 조회한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code M}: 공통 JSON 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code source()}: 출처 값을 조회·구성한다. 반환: {@code ObjectNode}. 사용하는 예외: {@code Exception}.
 *   <li>{@code rules()}: 규칙목록 값을 조회·구성한다. 반환: {@code ObjectNode}. 사용하는 예외: {@code Exception}.
 *   <li>{@code sameClassRegistrationNeedsDataOnlyAndUnknownClassFailsClosed()}: 시나리오의 잘못된 입력·상태가
 *       {@code IllegalArgumentException}로 거절되는지 검증한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code duplicateIdentityMissingCapabilityAndUnsupportedSocketPolicyAreRejected()}: 시나리오의
 *       잘못된 입력·상태가 {@code IllegalArgumentException}, {@code UnsupportedOperationException}로 거절되는지
 *       검증한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code orphanOverridesKeyMismatchAndPolicyTyposAreRejected()}: 시나리오의 잘못된 입력·상태가 {@code
 *       IllegalArgumentException}로 거절되는지 검증한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code allFormerReviewedMapsRemainExactWithoutIndependentIdLists()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class BaseRegistryTest {
  private static final ObjectMapper M = new ObjectMapper();

  private ObjectNode source() throws Exception {
    return (ObjectNode) M.readTree(getClass().getResourceAsStream("/catalog/top-bases.json"));
  }

  private ObjectNode rules() throws Exception {
    return (ObjectNode) M.readTree(getClass().getResourceAsStream("/catalog/base-policies.json"));
  }

  @Test
  void sameClassRegistrationNeedsDataOnlyAndUnknownClassFailsClosed() throws Exception {
    var source = source();
    var rules = rules();
    var added = source.get("grand-spear").deepCopy();
    ((ObjectNode) added)
        .put("key", "sourced-spear")
        .put("id", "Metadata/Items/Test/SourcedSpear")
        .put("name", "Sourced Spear")
        .put("slug", "Sourced_Spear")
        .put("pool", "sourced-spear");
    source.set("sourced-spear", added);
    var snapshot = BaseRegistry.read(source, rules);
    assertEquals(163, snapshot.top().size());
    assertEquals(
        BaseRegistry.require("grand-spear").policy(), snapshot.top().get("sourced-spear").policy());
    assertEquals("sourced-spear", snapshot.top().get("sourced-spear").pool());
    ((ObjectNode) added).put("family", "source-reviewed-new-class");
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, rules));
    ((ObjectNode) rules.get("families"))
        .set("source-reviewed-new-class", rules.get("families").get("spears").deepCopy());
    assertEquals(
        "Spears", BaseRegistry.read(source, rules).top().get("sourced-spear").policy().itemClass());
  }

  @Test
  void duplicateIdentityMissingCapabilityAndUnsupportedSocketPolicyAreRejected() throws Exception {
    var source = source();
    var rules = rules();
    source.set("duplicate", source.get("grand-spear").deepCopy());
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, rules));
    source.remove("duplicate");
    ((ObjectNode) rules.get("families").get("spears")).remove("ordinaryCatalyst");
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, rules));
    var completeRules = rules();
    ((ObjectNode) completeRules.get("families").get("spears")).put("socketExecutionMaximum", 4);
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, completeRules));
    assertThrows(UnsupportedOperationException.class, () -> BaseRegistry.topBases().clear());
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.require("unknown"));
    assertNull(BaseRegistry.policy("Metadata/Items/Unknown"));
    assertFalse(BaseRegistry.catalystQuality("Metadata/Items/Unknown"));
  }

  @Test
  void orphanOverridesKeyMismatchAndPolicyTyposAreRejected() throws Exception {
    var source = source();
    var rules = rules();
    ((ObjectNode) rules.get("baseOverrides")).set("unknown", M.createObjectNode());
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, rules));
    var complete = rules();
    ((ObjectNode) complete.get("families").get("spears")).put("ordinaryCatlyst", false);
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, complete));
    ((ObjectNode) source.get("grand-spear")).put("key", "typo");
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, rules()));
  }

  @Test
  void allFormerReviewedMapsRemainExactWithoutIndependentIdLists() throws Exception {
    var old = M.readTree(getClass().getResourceAsStream("/base-registry-reviewed-baseline.json"));
    for (var entries = old.fields(); entries.hasNext(); ) {
      var entry = entries.next();
      var type = Class.forName("com.poe2craft.shared.item." + entry.getKey().replace(".java", ""));
      var current = M.valueToTree(type.getField("BASES").get(null));
      entry
          .getValue()
          .fields()
          .forEachRemaining(
              previous ->
                  assertEquals(
                      previous.getValue(),
                      current.get(previous.getKey()),
                      entry.getKey() + ":" + previous.getKey()));
    }
    assertEquals(162, BaseRegistry.topBases().size());
    assertEquals(2, BaseRegistry.socketExecutionMaximum(BaseRegistry.require("grand-spear").id()));
    assertEquals(
        2, BaseRegistry.socketExecutionMaximum("Metadata/Items/Armours/Gloves/FourGlovesStr1"));
    assertFalse(BaseRegistry.ordinaryCatalyst(BaseRegistry.require("grand-spear").id()));
    assertTrue(BaseRegistry.ordinaryCatalyst(BaseRegistry.require("kinetic").id()));
    assertFalse(BaseRegistry.qualityLimit(BaseRegistry.require("linen-belt").id()));
  }
}
