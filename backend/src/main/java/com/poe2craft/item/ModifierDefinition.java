package com.poe2craft.item;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ModifierDefinition(
    String id,
    String name,
    Layer layer,
    AffixType affixType,
    Set<String> familyIds,
    int requiredItemLevel,
    int weight,
    int tier,
    String text,
    List<StatRange> stats,
    Set<String> tags,
    String sourceUrl) {
  public ModifierDefinition {
    if (id == null
        || id.isBlank()
        || name == null
        || name.isBlank()
        || text == null
        || text.isBlank()
        || sourceUrl == null
        || sourceUrl.isBlank()) {
      throw new IllegalArgumentException("Modifier identity, text and source are required");
    }
    Objects.requireNonNull(layer, "layer");
    Objects.requireNonNull(affixType, "affixType");
    familyIds = Set.copyOf(familyIds);
    tags = Set.copyOf(tags);
    stats = List.copyOf(stats);
    if (familyIds.isEmpty()
        || familyIds.stream().anyMatch(String::isBlank)
        || stats.isEmpty()
        || stats.stream().map(StatRange::id).distinct().count() != stats.size()
        || requiredItemLevel < 1
        || requiredItemLevel > 100
        || weight < 0) {
      throw new IllegalArgumentException("Invalid modifier definition: " + id);
    }
    if (layer == Layer.IMPLICIT
        ? affixType != AffixType.NONE || tier != 0 || weight != 0
        : affixType == AffixType.NONE || tier < 1) {
      throw new IllegalArgumentException("Invalid modifier layer/tier/affix: " + id);
    }
  }

  public enum Layer {
    IMPLICIT,
    EXPLICIT
  }

  public enum AffixType {
    NONE,
    PREFIX,
    SUFFIX
  }

  public record StatRange(String id, long min, long max) {
    public StatRange {
      if (id == null || id.isBlank() || min > max) {
        throw new IllegalArgumentException("Invalid stat range");
      }
    }

    public boolean contains(long value) {
      return value >= min && value <= max;
    }
  }
}
