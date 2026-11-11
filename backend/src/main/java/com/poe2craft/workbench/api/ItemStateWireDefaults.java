package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.stream.Collectors;

/** Optional boolean state flags have one wire default: omitted=false, explicit null is invalid. */
final class ItemStateWireDefaults {
  static final Set<String> ITEM_FLAGS = booleanFlags(ItemState.class);
  static final Set<String> MODIFIER_FLAGS = booleanFlags(ModifierInstance.class);

  private ItemStateWireDefaults() {}

  private static Set<String> booleanFlags(Class<?> record) {
    return Arrays.stream(record.getRecordComponents())
        .filter(c -> c.getType() == boolean.class)
        .map(java.lang.reflect.RecordComponent::getName)
        .collect(Collectors.toUnmodifiableSet());
  }

  static JsonNode normalized(JsonNode input) {
    var copy = input.deepCopy();
    visit(copy);
    return copy;
  }

  private static void visit(JsonNode node) {
    if (node instanceof ObjectNode object) {
      Set<String> flags =
          object.path("baseItemId").isTextual() && object.path("explicits").isArray()
              ? ITEM_FLAGS
              : object.path("modifierId").isTextual() && object.path("values").isObject()
                  ? MODIFIER_FLAGS
                  : Set.of();
      for (String flag : flags) {
        if (!object.has(flag)) object.put(flag, false);
        else if (!object.get(flag).isBoolean())
          throw new IllegalArgumentException("Boolean item flag required");
      }
    }
    if (node.isContainerNode()) node.elements().forEachRemaining(ItemStateWireDefaults::visit);
  }
}
