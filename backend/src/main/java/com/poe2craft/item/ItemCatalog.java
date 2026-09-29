package com.poe2craft.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A validated immutable data snapshot. No database, HTTP, or probability calculation. */
public final class ItemCatalog {
  private final Metadata metadata;
  private final BaseItem base;
  private final Map<String, ModifierDefinition> modifiers;

  public ItemCatalog(Metadata metadata, BaseItem base, List<ModifierDefinition> definitions) {
    this.metadata = Objects.requireNonNull(metadata);
    this.base = Objects.requireNonNull(base);
    var indexed = new LinkedHashMap<String, ModifierDefinition>();
    for (var definition : definitions) {
      if (indexed.putIfAbsent(definition.id(), definition) != null) {
        throw new IllegalArgumentException("Duplicate modifier: " + definition.id());
      }
    }
    this.modifiers = Collections.unmodifiableMap(indexed);
    var implicit = indexed.get(base.implicitModifierId());
    if (implicit == null || implicit.layer() != ModifierDefinition.Layer.IMPLICIT) {
      throw new IllegalArgumentException("Base implicit definition is missing");
    }
    for (var type :
        List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX)) {
      long count = definitions.stream().filter(d -> d.affixType() == type).count();
      long sum =
          definitions.stream()
              .filter(d -> d.affixType() == type)
              .mapToLong(ModifierDefinition::weight)
              .sum();
      if (count
              != (type == ModifierDefinition.AffixType.PREFIX
                  ? metadata.prefixCount()
                  : metadata.suffixCount())
          || sum
              != (type == ModifierDefinition.AffixType.PREFIX
                  ? metadata.prefixWeight()
                  : metadata.suffixWeight())) {
        throw new IllegalArgumentException("Snapshot counts or weight totals differ: " + type);
      }
    }
  }

  public Metadata metadata() {
    return metadata;
  }

  public BaseItem base() {
    return base;
  }

  public Map<String, ModifierDefinition> modifiers() {
    return modifiers;
  }

  public Optional<ModifierDefinition> find(String id) {
    return Optional.ofNullable(modifiers.get(id));
  }

  public record Metadata(
      String snapshotId,
      String retrievedAt,
      String sourceUrl,
      String weightPolicy,
      String rawSha256,
      String detailsSha256,
      int prefixCount,
      int suffixCount,
      long prefixWeight,
      long suffixWeight) {
    public Metadata {
      for (String value :
          List.of(snapshotId, retrievedAt, sourceUrl, weightPolicy, rawSha256, detailsSha256)) {
        if (value.isBlank())
          throw new IllegalArgumentException("Snapshot metadata must not be blank");
      }
      if (!rawSha256.matches("[0-9a-f]{64}")
          || !detailsSha256.matches("[0-9a-f]{64}")
          || prefixCount < 1
          || suffixCount < 1
          || prefixWeight < 1
          || suffixWeight < 1) {
        throw new IllegalArgumentException("Invalid snapshot metadata");
      }
    }
  }

  public record BaseItem(
      String id,
      String name,
      String sourceUrl,
      String implicitModifierId,
      int magicPrefixes,
      int magicSuffixes,
      int rarePrefixes,
      int rareSuffixes) {
    public BaseItem {
      for (String value : List.of(id, name, sourceUrl, implicitModifierId)) {
        if (value.isBlank()) throw new IllegalArgumentException("Base identity is required");
      }
      if (magicPrefixes < 0 || magicSuffixes < 0 || rarePrefixes < 0 || rareSuffixes < 0) {
        throw new IllegalArgumentException("Invalid base capacity");
      }
    }
  }
}
