package com.poe2craft.item;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Values use the source stat units, not numbers extracted from formatted display text. */
public record ModifierInstance(String modifierId, Map<String, Long> values, boolean fractured) {
  public ModifierInstance(String modifierId, Map<String, Long> values) {
    this(modifierId, values, false);
  }

  public ModifierInstance {
    if (modifierId == null || modifierId.isBlank()) {
      throw new IllegalArgumentException("Modifier ID is required");
    }
    values.forEach(
        (key, value) -> {
          if (key == null || key.isBlank() || value == null) {
            throw new IllegalArgumentException("Stat IDs and values are required");
          }
        });
    values = Collections.unmodifiableMap(new TreeMap<>(values));
  }
}
