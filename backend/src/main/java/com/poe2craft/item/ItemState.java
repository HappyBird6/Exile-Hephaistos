package com.poe2craft.item;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable crafting state. Probabilities, actions and path history belong outside this value. */
public record ItemState(
    String snapshotId,
    String baseItemId,
    int itemLevel,
    Rarity rarity,
    List<ModifierInstance> implicits,
    List<ModifierInstance> explicits,
    Set<Condition> conditions,
    Integer augmentSockets,
    CatalystQuality catalystQuality) {
  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        null);
  }

  /** Legacy states have unknown socket counts; never infer zero during migration. */
  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions) {
    this(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits, conditions, null);
  }

  public ItemState {
    if (snapshotId == null || snapshotId.isBlank() || baseItemId == null || baseItemId.isBlank()) {
      throw new IllegalArgumentException("Snapshot and base item IDs are required");
    }
    if (itemLevel < 1 || itemLevel > 100) {
      throw new IllegalArgumentException("Item level must be between 1 and 100");
    }
    Objects.requireNonNull(rarity, "rarity");
    implicits = canonical(implicits);
    explicits = canonical(explicits);
    conditions = Set.copyOf(conditions);
  }

  private static List<ModifierInstance> canonical(List<ModifierInstance> modifiers) {
    return modifiers.stream()
        .sorted(
            Comparator.comparing(ModifierInstance::modifierId)
                .thenComparing(m -> m.values().toString()))
        .toList();
  }

  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE
  }

  /** Preserved on input, but not supported by the initial base-affix model. */
  public enum Condition {
    CORRUPTED,
    MIRRORED,
    UNIDENTIFIED,
    SANCTIFIED
  }
}
