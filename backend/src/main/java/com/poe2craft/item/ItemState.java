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
    CatalystQuality catalystQuality,
    List<DesecratedSlot> desecrated,
    Integer equipmentQuality,
    ItemObservations observations) {
  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        null);
  }

  public ItemState withObservations(ItemObservations value) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        value);
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        null);
  }

  public ItemState withEquipmentQuality(Integer quality) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        quality,
        observations);
  }

  /** Slot identity survives observation; a null modifier ID is an unrevealed explicit slot. */
  public record DesecratedSlot(String id, ModifierDefinition.AffixType side, String modifierId) {
    public DesecratedSlot {
      if (id == null || id.isBlank() || side == null || side == ModifierDefinition.AffixType.NONE)
        throw new IllegalArgumentException("Desecrated slot identity and side required");
    }
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        List.of());
  }

  public ItemState withDesecrated(List<DesecratedSlot> slots) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        slots,
        equipmentQuality,
        observations);
  }

  public int explicitSlotCount() {
    return explicits.size() + (int) desecrated.stream().filter(s -> s.modifierId() == null).count();
  }

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
    if (equipmentQuality != null && (equipmentQuality < 0 || equipmentQuality > 100))
      throw new IllegalArgumentException("Invalid observed equipment quality");
    implicits = canonical(implicits);
    explicits = canonical(explicits);
    conditions = Set.copyOf(conditions);
    desecrated = desecrated == null ? List.of() : List.copyOf(desecrated);
    if (desecrated.stream().map(DesecratedSlot::id).distinct().count() != desecrated.size())
      throw new IllegalArgumentException("Duplicate desecrated slot");
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
