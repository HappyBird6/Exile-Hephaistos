package com.poe2craft.shared.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CraftingRegistryLoaderTest {
  @Test
  void levelReferencesExpandWithoutChangingApiAndRejectConflictingDefinitions() throws Exception {
    var scopes = data("supported-base-sets-v1");
    var registry = data("registry-v2");
    var output = read(registry, scopes);
    assertFalse(output.toString().contains("minimumModifierLevelRef"));
    for (var entry : output.get("entries")) {
      if (entry.has("minimumModifierLevel")) {
        assertEquals(
            WorkbenchCurrency.valueOf(entry.get("action").asText()).minimumModifierLevel(),
            entry.get("minimumModifierLevel").intValue());
      }
    }
    var wrong = data("registry-v2");
    ((ObjectNode) wrong.get("entries").get(0)).put("minimumModifierLevelRef", "GREATER_EXALTED");
    assertThrows(IllegalArgumentException.class, () -> read(wrong, scopes));
    var conflict = data("registry-v2");
    ((ObjectNode) conflict.get("entries").get(0)).put("minimumModifierLevel", 0);
    assertThrows(IllegalArgumentException.class, () -> read(conflict, scopes));
  }

  @Test
  void entryTyposAndNonIntegralSummaryCountsAreRejected() throws Exception {
    var scopes = data("supported-base-sets-v1");
    var typo = data("registry-v2");
    var entry = (ObjectNode) typo.get("entries").get(0);
    entry.set("supportedBaseSett", entry.remove("supportedBaseSet"));
    assertThrows(IllegalArgumentException.class, () -> read(typo, scopes));
    for (String field : List.of("registered", "active", "deferred")) {
      for (String type : List.of("text", "float", "null")) {
        var bad = data("registry-v2");
        var summary = (ObjectNode) bad.get("serviceScope");
        int original = summary.get(field).intValue();
        if (type.equals("text")) summary.put(field, Integer.toString(original));
        else if (type.equals("float")) summary.put(field, original + 0.5);
        else summary.putNull(field);
        assertThrows(IllegalArgumentException.class, () -> read(bad, scopes));
      }
    }
    var fractional = data("supported-base-sets-v1");
    fractional.put("schemaVersion", 1.5);
    assertThrows(IOException.class, () -> read(data("registry-v2"), fractional));
  }

  private static final ObjectMapper M = new ObjectMapper();

  private ObjectNode data(String name) throws IOException {
    return (ObjectNode) M.readTree(getClass().getResourceAsStream("/crafting/" + name + ".json"));
  }

  private JsonNode read(JsonNode data, JsonNode scopes) throws IOException {
    return CraftingRegistryLoader.read(
        new ByteArrayInputStream(M.writeValueAsBytes(data)),
        new ByteArrayInputStream(M.writeValueAsBytes(scopes)));
  }

  private static JsonNode canonical(JsonNode node) {
    if (node.isObject()) {
      var result = M.createObjectNode();
      var keys = new TreeSet<String>();
      node.fieldNames().forEachRemaining(keys::add);
      keys.forEach(k -> result.set(k, canonical(node.get(k))));
      return result;
    }
    if (node.isArray()) {
      var result = M.createArrayNode();
      node.forEach(v -> result.add(canonical(v)));
      return result;
    }
    return node;
  }

  @Test
  void publicRegistryRetainsEveryValueAndArrayOrder() throws Exception {
    var registry = CraftingRegistryLoader.load();
    assertEquals("UNVERIFIED", registry.path("ruleset").path("gameSeason").asText());
    assertEquals(
        RulesetManifestLoader.load().identity(),
        registry.path("ruleset").path("identity").asText());
    ((ObjectNode) registry).remove("ruleset");
    // Equipment completion advances the engine descriptors; verify them, then restore the frozen
    // baseline.
    assertEquals("solar-workbench-abyss-essence-v20", registry.path("ruleVersion").asText());
    assertEquals("solar-uniform-assumptions-v13", registry.path("ledgerVersion").asText());
    ((ObjectNode) registry).put("ruleVersion", "solar-workbench-abyss-essence-v16");
    ((ObjectNode) registry).put("ledgerVersion", "solar-uniform-assumptions-v10");
    ((ObjectNode) registry.path("workbenchBases").path("solar"))
        .put("ruleVersion", "solar-workbench-abyss-essence-v16");
    ((ObjectNode) registry.path("workbenchBases").path("solar"))
        .put("ledgerVersion", "solar-uniform-assumptions-v10");

    // These three public metadata records intentionally change for the new manual feature.
    // Restore only their frozen fragments before checking every other value/order below.
    var originalFragments =
        M.readTree(
            getClass().getResourceAsStream("/crafting-registry-v25-original-fragments.json"));
    var entries = (ArrayNode) registry.path("entries");
    for (int index = 0; index < entries.size(); index++) {
      var original = originalFragments.path("entries").get(entries.get(index).path("id").asText());
      if (original != null) entries.set(index, original.deepCopy());
    }
    ((ObjectNode) registry).set("serviceScope", originalFragments.path("serviceScope").deepCopy());
    // The sole intentional data correction removes two adjacent duplicate base keys.
    // Reconstruct the former array before comparing all fields to the pre-refactor digest.
    var blessed =
        java.util.stream.StreamSupport.stream(registry.get("entries").spliterator(), false)
            .filter(e -> e.path("id").asText().equals("Omen_of_the_Blessed"))
            .findFirst()
            .orElseThrow();
    var members = (ArrayNode) blessed.get("supportedBases");
    assertEquals(119, members.size());
    // Newly reviewed bases are additive. Compare the frozen original membership's
    // entire registry document, including every old value and array order.
    var frozenIds = new HashSet<String>();
    M.readTree(
            getClass().getResourceAsStream("/crafting/goalfilter/base-inventory-v5-readonly.json"))
        .forEach(id -> frozenIds.add(id.asText()));
    var frozenKeys = new HashSet<String>();
    for (String resource : List.of("/catalog/top-bases.json", "/catalog/base-policies.json")) {
      var source = M.readTree(getClass().getResourceAsStream(resource));
      if (resource.endsWith("base-policies.json")) source = source.path("legacy");
      source
          .fields()
          .forEachRemaining(
              entry -> {
                if (frozenIds.contains(entry.getValue().path("id").asText()))
                  frozenKeys.add(entry.getKey());
              });
    }
    var workbenchBases = (ObjectNode) registry.path("workbenchBases");
    var addedKeys = new ArrayList<String>();
    workbenchBases
        .fieldNames()
        .forEachRemaining(
            key -> {
              if (!frozenKeys.contains(key)) addedKeys.add(key);
            });
    workbenchBases.remove(addedKeys);
    for (var entry : registry.path("entries")) {
      if (!entry.path("supportedBases").isArray()) continue;
      var originalMembers = M.createArrayNode();
      entry
          .path("supportedBases")
          .forEach(
              key -> {
                if (frozenKeys.contains(key.asText())) originalMembers.add(key);
              });
      ((ObjectNode) entry).set("supportedBases", originalMembers);
    }
    members = (ArrayNode) blessed.get("supportedBases");
    assertEquals(74, members.size());
    assertEquals("elegant-crossbow", members.get(58).asText());
    assertEquals("flexed-crossbow", members.get(59).asText());
    var legacy = M.createArrayNode();
    for (var member : members) {
      legacy.add(member);
      if (Set.of("elegant-crossbow", "flexed-crossbow").contains(member.asText()))
        legacy.add(member);
    }
    ((ObjectNode) blessed).set("supportedBases", legacy);
    String digest =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(M.writeValueAsBytes(canonical(registry))));
    // The frozen baseline digest excludes only the 26 retired verificationEvidence arrays.
    // It was derived from the committed original registry after reproducing its former digest;
    // game data, metadata other than those report paths, and array ordering remain covered.
    String expected =
        new String(
                getClass()
                    .getResourceAsStream("/crafting-registry-public-sha256.txt")
                    .readAllBytes(),
                StandardCharsets.UTF_8)
            .trim();
    assertEquals(expected, digest);
    assertEquals(220, registry.get("entries").size());
    assertFalse(registry.toString().contains("supportedBaseSet"));
  }

  @Test
  void publicRegistryDoesNotExposeRetiredCollectionReports() {
    for (var entry : CraftingRegistryLoader.load().path("entries")) {
      assertFalse(entry.has("verificationEvidence"), entry.path("id").asText());
    }
  }

  @Test
  void newManualPolicyMetadataLabelsTheUserSelectedQualityIncrement() {
    var registry = CraftingRegistryLoader.load();
    var byId = new HashMap<String, JsonNode>();
    registry.path("entries").forEach(entry -> byId.put(entry.path("id").asText(), entry));
    var sanctification = byId.get("Omen_of_Sanctification");
    assertEquals("ACTIVE", sanctification.path("serviceScope").asText());
    assertEquals("SANCTIFICATION", sanctification.path("action").asText());
    assertEquals("IMPLEMENTED", sanctification.path("effectStatus").asText());
    assertEquals(
        byId.get("Divine_Orb").path("supportedBases"), sanctification.path("supportedBases"));
    assertTrue(sanctification.path("reason").asText().contains("simulator policy"));
    for (String id : List.of("Armourers_Scrap", "Blacksmiths_Whetstone")) {
      var currency = byId.get(id);
      assertEquals("ACTIVE", currency.path("serviceScope").asText());
      assertEquals("IMPLEMENTED", currency.path("effectStatus").asText());
      assertEquals(
          "user-selected-equipment-quality-maximum-v2", currency.path("qualityPolicy").asText());
      assertTrue(currency.path("sourceSha256").asText().matches("[0-9a-f]{64}"));
      assertTrue(currency.path("reason").asText().contains("User-selected simulator policy"));
    }
  }

  @Test
  void invalidIdScopeReferenceVersionAndCountsFailBeforeApiExposure() throws Exception {
    var registry = data("registry-v2");
    var scopes = data("supported-base-sets-v1");
    ((ArrayNode) registry.get("entries")).add(registry.get("entries").get(0).deepCopy());
    assertThrows(IllegalArgumentException.class, () -> read(registry, scopes));
    for (String field : List.of("ruleVersion", "ledgerVersion", "version")) {
      var bad = data("registry-v2");
      bad.put(field, "unknown");
      assertThrows(IllegalArgumentException.class, () -> read(bad, scopes));
    }
    var missing = data("registry-v2");
    ((ObjectNode) missing.get("entries").get(0)).put("supportedBaseSet", "unknown");
    assertThrows(IllegalArgumentException.class, () -> read(missing, scopes));
    var invalidScopes = data("supported-base-sets-v1");
    ((ArrayNode) invalidScopes.get("sets").elements().next()).add("unknown-base");
    assertThrows(IllegalArgumentException.class, () -> read(data("registry-v2"), invalidScopes));
    var counts = data("registry-v2");
    ((ObjectNode) counts.get("serviceScope"))
        .put("active", counts.path("serviceScope").path("active").intValue() + 1);
    assertThrows(IllegalArgumentException.class, () -> read(counts, scopes));
    var action = data("registry-v2");
    ((ObjectNode) action.get("entries").get(0)).put("action", "UNIMPLEMENTED_ACTION");
    assertThrows(IllegalArgumentException.class, () -> read(action, scopes));
  }
}
