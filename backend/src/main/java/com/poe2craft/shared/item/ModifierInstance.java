package com.poe2craft.shared.item;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Values use the source stat units, not numbers extracted from formatted display text. */
public record ModifierInstance(
    String modifierId, Map<String, Long> values, boolean fractured, boolean crafted) {
  public ModifierInstance(String modifierId, Map<String, Long> values) {
    this(modifierId, values, false, false);
  }

  public ModifierInstance(String modifierId, Map<String, Long> values, boolean fractured) {
    this(modifierId, values, fractured, false);
  }

  public ModifierInstance asCrafted() {
    return new ModifierInstance(modifierId, values, fractured, true);
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
