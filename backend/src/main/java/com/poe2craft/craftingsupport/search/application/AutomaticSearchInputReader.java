package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.databind.*;

/** Strict automatic-search input with shared item-state wire defaults. */
public final class AutomaticSearchInputReader {
  private final ObjectMapper json;

  public AutomaticSearchInputReader(ObjectMapper json) {
    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  public <T> T read(String input, Class<T> type) {
    try {
      if (input.length() > 262144) throw new IllegalArgumentException("Request too large");
      var node = json.readTree(input);
      safe(node);
      node = com.poe2craft.shared.item.ItemStateWireDefaults.normalized(node);
      // Optional additions keep older clients valid; original required fields stay strict.
      if (type == AutomaticSearchService.Input.class
          && node instanceof com.fasterxml.jackson.databind.node.ObjectNode body) {
        if (!body.has("rareOutcomePolicy"))
          body.set(
              "rareOutcomePolicy",
              json.valueToTree(
                  com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy.disabled()));
        if (body.get("rareOutcomePolicy").isNull())
          throw new IllegalArgumentException("Policy required");
        if (!body.has("abstractQuality"))
          body.put("abstractQuality", body.path("abstractQuality40").asBoolean() ? 40 : 0);
      }
      var general = node.path("request").path("goal").path("general");
      if (general instanceof com.fasterxml.jackson.databind.node.ObjectNode object
          && !object.has("quality")) object.putNull("quality");
      for (var item :
          java.util.List.of(
              node.path("start").path("item"), node.path("request").path("start").path("item")))
        if (item instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
          if (!object.has("desecrated")) object.putArray("desecrated");
          if (object.hasNonNull("installedAugments")
              && (!object.path("installedAugments").isArray()
                  || !object.path("installedAugments").isEmpty()))
            throw new IllegalArgumentException("Installed Augments require the manual workbench");
          if (!object.has("installedAugments")) object.putArray("installedAugments");
          // Ordinary quality belongs to the manual workbench until automatic copies retain it.
          if (object.hasNonNull("equipmentQuality"))
            throw new IllegalArgumentException("Equipment quality requires the manual workbench");
          if (!object.has("equipmentQuality")) object.putNull("equipmentQuality");
          if (object.hasNonNull("observations"))
            throw new IllegalArgumentException(
                "Clipboard observations require the manual workbench");
          if (!object.has("observations")) object.putNull("observations");
          if (object.hasNonNull("sanctification"))
            throw new IllegalArgumentException("Sanctification requires the manual workbench");
          if (!object.has("sanctification")) object.putNull("sanctification");
        }
      return json.treeToValue(node, type);
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new PathSearchProtocol.Rejected(422, "INVALID_REQUEST");
    }
  }

  private static void safe(JsonNode node) {
    if (node == null) throw new IllegalArgumentException("Body required");
    if (node.isIntegralNumber()
        && node.bigIntegerValue().abs().compareTo(java.math.BigInteger.valueOf(9007199254740991L))
            > 0) throw new IllegalArgumentException("Unsafe integer");
    if (node.isContainerNode()) node.forEach(AutomaticSearchInputReader::safe);
  }
}
