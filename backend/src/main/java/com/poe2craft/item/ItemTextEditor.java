package com.poe2craft.item;

import java.util.List;

/** Applies editable raw-roll text while retaining state which clipboard text cannot encode. */
public final class ItemTextEditor {
  private ItemTextEditor() {}

  public static SolarTextMapper.Result merge(
      ItemState before, SolarTextMapper.Result mapped, ItemCatalog catalog) {
    var originalIssues = new ItemStateValidator(catalog).validate(before);
    if (!originalIssues.isEmpty()) return blocked("The original item state is invalid.");
    if (!mapped.mapped()) return mapped;
    var next = mapped.state();
    if (!before.baseItemId().equals(next.baseItemId())
        || !before.snapshotId().equals(next.snapshotId()))
      return blocked("Changing the base requires importing a new item.");
    // Slot identity, reveal pools and scaling factors have no clipboard representation.
    if ((!before.desecrated().isEmpty() || before.sanctification() != null)
        && (!before.implicits().equals(next.implicits())
            || !before.explicits().equals(next.explicits())
            || before.rarity() != next.rarity()))
      return blocked(
          "Modifier edits cannot reconstruct desecrated slots or sanctification factors.");
    var expectedRunes = new java.util.ArrayList<String>();
    for (var installed : before.installedAugments()) {
      for (var entry : SocketAugments.catalog()) {
        if (entry.path("id").asText().equals(installed.augmentId()))
          expectedRunes.add(
              "Rune: Slot "
                  + (installed.slotIndex() + 1)
                  + ": "
                  + entry.path("names").path("en").asText());
      }
    }
    var observed = next.observations();
    if ((observed == null
            && ((before.augmentSockets() != null && before.augmentSockets() > 0)
                || !expectedRunes.isEmpty()))
        || (observed != null
            && (!java.util.Objects.equals(observed.socketCount(), before.augmentSockets())
                || !observed.augmentEffects().equals(expectedRunes)
                || !observed.grantedSkills().isEmpty())))
      return blocked(
          "Socket and Rune lines are protected; use the socket controls to change installed augments.");
    if (next.catalystQuality() != null || !next.conditions().isEmpty())
      return blocked(
          "Edit raw rolls only; catalyst quality and special conditions are retained separately.");
    var merged =
        new ItemState(
            before.snapshotId(),
            before.baseItemId(),
            next.itemLevel(),
            next.rarity(),
            next.implicits(),
            next.explicits(),
            before.conditions(),
            before.augmentSockets(),
            before.catalystQuality(),
            before.desecrated(),
            next.equipmentQuality(),
            before.observations(),
            before.sanctification(),
            before.installedAugments());
    var issues =
        new ItemStateValidator(catalog)
            .validate(merged).stream().map(v -> new SolarTextMapper.Issue(0, v.message())).toList();
    return new SolarTextMapper.Result(issues.isEmpty(), issues.isEmpty() ? merged : null, issues);
  }

  private static SolarTextMapper.Result blocked(String message) {
    return new SolarTextMapper.Result(false, null, List.of(new SolarTextMapper.Issue(0, message)));
  }
}
