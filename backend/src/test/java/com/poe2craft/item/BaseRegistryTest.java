package com.poe2craft.item;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

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
        .put("id", "Metadata/Items/Test/SourcedSpear")
        .put("name", "Sourced Spear")
        .put("slug", "Sourced_Spear")
        .put("pool", "sourced-spear");
    source.set("sourced-spear", added);
    var snapshot = BaseRegistry.read(source, rules);
    assertEquals(109, snapshot.top().size());
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
    ((ObjectNode) completeRules.get("families").get("spears")).put("socketExecutionMaximum", 2);
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.read(source, completeRules));
    assertThrows(UnsupportedOperationException.class, () -> BaseRegistry.topBases().clear());
    assertThrows(IllegalArgumentException.class, () -> BaseRegistry.require("unknown"));
    assertNull(BaseRegistry.policy("Metadata/Items/Unknown"));
    assertFalse(BaseRegistry.catalystQuality("Metadata/Items/Unknown"));
  }

  @Test
  void allFormerReviewedMapsRemainExactWithoutIndependentIdLists() throws Exception {
    var old = M.readTree(getClass().getResourceAsStream("/base-registry-reviewed-baseline.json"));
    for (var entries = old.fields(); entries.hasNext(); ) {
      var entry = entries.next();
      var type = Class.forName("com.poe2craft.item." + entry.getKey().replace(".java", ""));
      assertEquals(
          entry.getValue(), M.valueToTree(type.getField("BASES").get(null)), entry.getKey());
    }
    assertEquals(108, BaseRegistry.topBases().size());
    assertNull(BaseRegistry.socketExecutionMaximum(BaseRegistry.require("grand-spear").id()));
    assertEquals(
        1, BaseRegistry.socketExecutionMaximum("Metadata/Items/Armours/Gloves/FourGlovesStr1"));
    assertFalse(BaseRegistry.ordinaryCatalyst(BaseRegistry.require("grand-spear").id()));
    assertTrue(BaseRegistry.ordinaryCatalyst(BaseRegistry.require("kinetic").id()));
    assertFalse(BaseRegistry.qualityLimit(BaseRegistry.require("linen-belt").id()));
  }
}
