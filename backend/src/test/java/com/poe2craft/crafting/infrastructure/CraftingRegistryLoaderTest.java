package com.poe2craft.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CraftingRegistryLoaderTest {
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
    // The sole intentional data correction removes two adjacent duplicate base keys.
    // Reconstruct the former array before comparing all fields to the pre-refactor digest.
    var blessed =
        java.util.stream.StreamSupport.stream(registry.get("entries").spliterator(), false)
            .filter(e -> e.path("id").asText().equals("Omen_of_the_Blessed"))
            .findFirst()
            .orElseThrow();
    var members = (ArrayNode) blessed.get("supportedBases");
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
    ((ObjectNode) counts.get("serviceScope")).put("active", 171);
    assertThrows(IllegalArgumentException.class, () -> read(counts, scopes));
    var action = data("registry-v2");
    ((ObjectNode) action.get("entries").get(0)).put("action", "UNIMPLEMENTED_ACTION");
    assertThrows(IllegalArgumentException.class, () -> read(action, scopes));
  }
}
