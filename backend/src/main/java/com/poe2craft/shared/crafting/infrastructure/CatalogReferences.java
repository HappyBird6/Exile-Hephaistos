package com.poe2craft.shared.crafting.infrastructure;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.*;
import org.springframework.stereotype.Service;

/** Content-addressed immutable projections. Retention is bounded by the bundled base registry. */
@Service
public final class CatalogReferences {
  private final CraftingOperations crafting;
  private final ObjectMapper json;
  private final Map<String, String> aliases = new HashMap<>();

  private record Projection(String base, boolean reveal) {}

  private final Map<String, Projection> content = new HashMap<>();
  private boolean indexed;

  public CatalogReferences(CraftingOperations crafting, ObjectMapper json) {
    this.crafting = crafting;
    this.json = json;
  }

  public synchronized String reference(String base, boolean reveal) {
    if (!BaseRegistry.registeredKeys().contains(base))
      throw new IllegalArgumentException("Unknown base");
    return aliases.computeIfAbsent(
        base + ":" + reveal,
        ignored -> {
          JsonNode definitions = json.valueToTree(crafting.initial(base, 82, reveal).modifiers());
          try {
            String hash =
                HexFormat.of()
                    .formatHex(
                        java.security.MessageDigest.getInstance("SHA-256")
                            .digest(json.writeValueAsBytes(canonical(definitions, ""))));
            content.putIfAbsent(hash, new Projection(base, reveal));
            return hash;
          } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
          }
        });
  }

  public synchronized JsonNode find(String hash) {
    if (!hash.matches("[a-f0-9]{64}")) return null;
    // A different replica may receive a content URL before receiving its initial request.
    // Scan the finite registry at most once; arbitrary hashes never grow either map.
    if (!content.containsKey(hash) && !indexed) {
      for (String base : BaseRegistry.registeredKeys()) {
        reference(base, false);
        reference(base, true);
      }
      indexed = true;
    }
    var projection = content.get(hash);
    return projection == null
        ? null
        : json.createObjectNode()
            .put("rulesetIdentity", RulesetBoundary.identity())
            .set(
                "modifiers",
                json.valueToTree(
                    crafting.initial(projection.base(), 82, projection.reveal()).modifiers()));
  }

  private JsonNode canonical(JsonNode node, String field) {
    if (node.isObject()) {
      var result = json.createObjectNode();
      var names = new TreeSet<String>();
      node.fieldNames().forEachRemaining(names::add);
      names.forEach(name -> result.set(name, canonical(node.get(name), name)));
      return result;
    }
    if (node.isArray()) {
      var children = new ArrayList<JsonNode>();
      node.forEach(v -> children.add(canonical(v, "")));
      if (field.equals("tags")) children.sort(Comparator.comparing(JsonNode::asText));
      var result = json.createArrayNode();
      children.forEach(result::add);
      return result;
    }
    return node;
  }
}
