package com.poe2craft.shared.item;

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
    ItemObservations observations,
    Sanctification sanctification,
    List<InstalledAugment> installedAugments) {
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
      Integer equipmentQuality,
      ItemObservations observations,
      Sanctification sanctification) {
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
        observations,
        sanctification,
        List.of());
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
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality,
      ItemObservations observations) {
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
        observations,
        null);
  }

  /** Original integer rolls remain untouched; these exact percentages are display projections. */
  public record Sanctification(
      String policy,
      java.util.Map<String, java.util.Map<String, Integer>> implicits,
      java.util.Map<String, java.util.Map<String, Integer>> explicits) {
    public Sanctification {
      if (!"independent-stat-percent-v1".equals(policy))
        throw new IllegalArgumentException("Unknown sanctification policy");
      implicits = immutableFactors(implicits);
      explicits = immutableFactors(explicits);
    }

    private static java.util.Map<String, java.util.Map<String, Integer>> immutableFactors(
        java.util.Map<String, java.util.Map<String, Integer>> factors) {
      var copy = new java.util.TreeMap<String, java.util.Map<String, Integer>>();
      factors.forEach(
          (id, values) -> {
            if (id == null
                || id.isBlank()
                || values.isEmpty()
                || values.entrySet().stream()
                    .anyMatch(
                        e ->
                            e.getKey() == null
                                || e.getKey().isBlank()
                                || e.getValue() == null
                                || e.getValue() < 78
                                || e.getValue() > 122))
              throw new IllegalArgumentException("Invalid sanctification factors");
            copy.put(id, java.util.Map.copyOf(values));
          });
      return java.util.Collections.unmodifiableMap(copy);
    }
  }

  public ItemState withSanctification(Sanctification value) {
    var flags = new java.util.HashSet<>(conditions);
    if (value != null) flags.add(Condition.SANCTIFIED);
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        flags,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        value,
        installedAugments);
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
        value,
        sanctification,
        installedAugments);
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
        observations,
        sanctification,
        installedAugments);
  }

  /** Slot identity survives observation; a null modifier ID is an unrevealed explicit slot. */
  public record DesecratedSlot(
      String id,
      ModifierDefinition.AffixType side,
      String modifierId,
      Integer minimumModifierLevel,
      String revealPool) {
    public DesecratedSlot(
        String id,
        ModifierDefinition.AffixType side,
        String modifierId,
        Integer minimumModifierLevel) {
      this(id, side, modifierId, minimumModifierLevel, null);
    }

    public DesecratedSlot(String id, ModifierDefinition.AffixType side, String modifierId) {
      this(id, side, modifierId, null);
    }

    public DesecratedSlot {
      if (revealPool != null
          && !Set.of("ALTERED", "AMANAMU", "KURGAL", "ULAMAN").contains(revealPool))
        throw new IllegalArgumentException("Unknown reveal pool");
      if (minimumModifierLevel != null && minimumModifierLevel != 40)
        throw new IllegalArgumentException("Unsupported desecration level constraint");
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
        observations,
        sanctification,
        installedAugments);
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

  public record InstalledAugment(int slotIndex, String augmentId) {}

  /** Preserve legacy empty-item hashing used by bounded search iteration and saved state keys. */
  @Override
  public int hashCode() {
    int hash = Objects.hashCode(snapshotId);
    hash = 31 * hash + Objects.hashCode(baseItemId);
    hash = 31 * hash + itemLevel;
    hash = 31 * hash + Objects.hashCode(rarity);
    hash = 31 * hash + Objects.hashCode(implicits);
    hash = 31 * hash + Objects.hashCode(explicits);
    hash = 31 * hash + Objects.hashCode(conditions);
    hash = 31 * hash + Objects.hashCode(augmentSockets);
    hash = 31 * hash + Objects.hashCode(catalystQuality);
    hash = 31 * hash + Objects.hashCode(desecrated);
    hash = 31 * hash + Objects.hashCode(equipmentQuality);
    hash = 31 * hash + Objects.hashCode(observations);
    hash = 31 * hash + Objects.hashCode(sanctification);
    return installedAugments.isEmpty() ? hash : 31 * hash + installedAugments.hashCode();
  }

  public ItemState withAugments(List<InstalledAugment> value) {
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
        observations,
        sanctification,
        value);
  }

  public ItemState {
    installedAugments = installedAugments == null ? List.of() : List.copyOf(installedAugments);
    if (snapshotId == null || snapshotId.isBlank() || baseItemId == null || baseItemId.isBlank()) {
      throw new IllegalArgumentException("Snapshot and base item IDs are required");
    }
    if (itemLevel < 1 || itemLevel > 100) {
      throw new IllegalArgumentException("Item level must be between 1 and 100");
    }
    Objects.requireNonNull(rarity, "rarity");
    SocketAugments.validate(baseItemId, augmentSockets, installedAugments);
    if (equipmentQuality != null && (equipmentQuality < 0 || equipmentQuality > 100))
      throw new IllegalArgumentException("Invalid observed equipment quality");
    implicits = canonical(implicits);
    explicits = canonical(explicits);
    conditions = Set.copyOf(conditions);
    if (sanctification != null
        && (!conditions.contains(Condition.SANCTIFIED) || rarity != Rarity.RARE))
      throw new IllegalArgumentException("Sanctification requires a Sanctified Rare item");
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
