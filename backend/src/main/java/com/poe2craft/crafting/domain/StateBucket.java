package com.poe2craft.crafting.domain;

import com.poe2craft.item.ItemState;
import com.poe2craft.item.ModifierInstance;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Exact projection for the six supported actions: explicit numeric rolls are marginalized. */
public record StateBucket(
    String snapshotId,
    String baseItemId,
    int itemLevel,
    ItemState.Rarity rarity,
    List<ModifierInstance> implicits,
    List<String> modifierIds,
    Set<ItemState.Condition> conditions) {
  public StateBucket {
    if (snapshotId == null
        || snapshotId.length() > 120
        || baseItemId == null
        || baseItemId.length() > 120
        || itemLevel < 1
        || itemLevel > 100) throw new IllegalArgumentException("Invalid bucket identity");
    Objects.requireNonNull(rarity);
    implicits = List.copyOf(implicits);
    if (implicits.size() > 1
        || modifierIds.size() > 6
        || modifierIds.stream().anyMatch(id -> id == null || id.length() > 160)) {
      throw new IllegalArgumentException("Invalid bucket modifiers");
    }
    modifierIds = modifierIds.stream().sorted().toList();
    conditions = Set.copyOf(conditions);
  }

  public static StateBucket from(ItemState item) {
    return new StateBucket(
        item.snapshotId(),
        item.baseItemId(),
        item.itemLevel(),
        item.rarity(),
        item.implicits(),
        item.explicits().stream().map(ModifierInstance::modifierId).toList(),
        item.conditions());
  }

  public StateBucket with(ItemState.Rarity nextRarity, List<String> nextModifiers) {
    return new StateBucket(
        snapshotId, baseItemId, itemLevel, nextRarity, implicits, nextModifiers, conditions);
  }

  public String id() {
    // All IDs and implicit stat keys are catalog-validated before use. Collections are canonical.
    String value =
        snapshotId
            + "\n"
            + baseItemId
            + "\n"
            + itemLevel
            + "\n"
            + rarity
            + "\n"
            + implicits
            + "\n"
            + modifierIds
            + "\n"
            + conditions.stream().sorted().toList();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
