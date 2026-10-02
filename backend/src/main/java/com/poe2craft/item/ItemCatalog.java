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
  private final List<String> compatibleSnapshotIds;

  public ItemCatalog(Metadata metadata, BaseItem base, List<ModifierDefinition> definitions) {
    this(metadata, base, definitions, List.of());
  }

  public ItemCatalog(
      Metadata metadata,
      BaseItem base,
      List<ModifierDefinition> definitions,
      List<String> compatibleSnapshotIds) {
    this.metadata = Objects.requireNonNull(metadata);
    this.base = Objects.requireNonNull(base);
    this.compatibleSnapshotIds = List.copyOf(compatibleSnapshotIds);
    if (this.compatibleSnapshotIds.stream()
        .anyMatch(id -> id.isBlank() || id.equals(metadata.snapshotId())))
      throw new IllegalArgumentException("Invalid compatible snapshot identity");
    var indexed = new LinkedHashMap<String, ModifierDefinition>();
    for (var definition : definitions) {
      if (indexed.putIfAbsent(definition.id(), definition) != null) {
        throw new IllegalArgumentException("Duplicate modifier: " + definition.id());
      }
    }
    this.modifiers = Collections.unmodifiableMap(indexed);
    var implicits =
        definitions.stream().filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT).toList();
    if (base.hasImplicit()
        ? implicits.size() != 1 || !implicits.getFirst().id().equals(base.implicitModifierId())
        : !implicits.isEmpty()) {
      throw new IllegalArgumentException("Implicit definitions must match the base exactly");
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

  /** Only snapshots whose existing base and every existing definition were preserved exactly. */
  public List<String> compatibleSnapshotIds() {
    return compatibleSnapshotIds;
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
      for (String value : List.of(id, name, sourceUrl)) {
        if (value.isBlank()) throw new IllegalArgumentException("Base identity is required");
      }
      Objects.requireNonNull(implicitModifierId, "implicitModifierId");
      if (!implicitModifierId.isEmpty() && implicitModifierId.isBlank()) {
        throw new IllegalArgumentException("Implicit identity must be empty or nonblank");
      }
      if (magicPrefixes < 0 || magicSuffixes < 0 || rarePrefixes < 0 || rareSuffixes < 0) {
        throw new IllegalArgumentException("Invalid base capacity");
      }
    }

    /** Empty identity denotes a sourced base with no implicit; base properties are separate. */
    public boolean hasImplicit() {
      return !implicitModifierId.isEmpty();
    }
  }
}
