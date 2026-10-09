package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BaseRegistryParityTest {
  static final ObjectMapper M = new ObjectMapper();

  static JsonNode canonical(JsonNode n) {
    if (n.isObject()) {
      var result = M.createObjectNode();
      var keys = new TreeSet<String>();
      n.fieldNames().forEachRemaining(keys::add);
      for (var k : keys) result.set(k, canonical(n.get(k)));
      return result;
    }
    if (n.isArray()) {
      var values = new ArrayList<JsonNode>();
      n.forEach(v -> values.add(canonical(v)));
      if (values.stream().allMatch(JsonNode::isTextual))
        values.sort(Comparator.comparing(JsonNode::asText));
      var result = M.createArrayNode();
      values.forEach(result::add);
      return result;
    }
    return n;
  }

  static String digest(Object value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(M.writeValueAsBytes(canonical(M.valueToTree(value)))));
  }

  @Test
  void allExistingInitialsActionsAndSeededTracesRemainExact() throws Exception {
    var cfg = new CraftingConfiguration();
    var catalog = cfg.itemCatalog();
    var engine = cfg.craftingEngine(catalog);
    var service = cfg.workbenchService(catalog, cfg.workbenchSimulator(catalog, engine));
    var result = new TreeMap<String, Object>();
    var expected =
        M.readTree(getClass().getResourceAsStream("/base-registry-runtime-baseline.json"));
    org.junit.jupiter.api.Assertions.assertEquals(125, expected.size());
    for (var it = expected.fieldNames(); it.hasNext(); ) {
      var key = it.next();
      var record = new TreeMap<String, Object>();
      for (int level : new int[] {1, 20, 82}) {
        var initial = service.initial(key, level);
        record.put("initial-" + level, digest(initial));
        var s = initial.state();
        var state =
            new ItemState(
                s.snapshotId(),
                s.baseItemId(),
                s.itemLevel(),
                s.rarity(),
                s.implicits(),
                s.modifierIds().stream()
                    .map(
                        id ->
                            new ModifierInstance(
                                id,
                                initial.modifiers().get(id).stats().stream()
                                    .collect(
                                        java.util.stream.Collectors.toMap(
                                            ModifierDefinition.StatRange::id, stat -> stat.max()))))
                    .toList(),
                s.conditions(),
                initial.augmentSockets());
        record.put("actions-" + level, digest(service.actions(state, Set.of())));
        var trace = new ArrayList<Object>();
        var random = new Random(760105L);
        for (var action :
            List.of(
                WorkbenchCurrency.TRANSMUTATION,
                WorkbenchCurrency.AUGMENTATION,
                WorkbenchCurrency.REGAL,
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.DIVINE,
                WorkbenchCurrency.CHAOS,
                WorkbenchCurrency.ANNULMENT)) {
          var applied = service.apply(state, action, Set.of(), random);
          trace.add(applied);
          state = applied.state();
        }
        record.put("seeded-" + level, digest(trace));
      }
      result.put(key, record);
      org.junit.jupiter.api.Assertions.assertEquals(
          expected.get(key), M.valueToTree(record), key + ": pre-refactor behavior changed");
    }
    org.junit.jupiter.api.Assertions.assertEquals(expected, M.valueToTree(result));
  }
}
