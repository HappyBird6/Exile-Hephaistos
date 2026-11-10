package com.poe2craft.shared.item;

import java.util.List;

/** Applies editable raw-roll text while retaining state which clipboard text cannot encode. */
public final class ItemTextEditor {
  private ItemTextEditor() {}

  public static SolarTextMapper.Result merge(
      ItemState before, SolarTextMapper.Result mapped, ItemCatalog catalog) {
    catalog = ManualRevealCatalog.enrich(catalog);
    var originalIssues = ItemTextStateValidation.validate(before, catalog);
    if (!originalIssues.isEmpty()) return blocked("The original item state is invalid.");
    if (before.conditions().contains(ItemState.Condition.SANCTIFIED))
      return blocked("Sanctified items cannot be modified.");
    if (!mapped.mapped()) return mapped;
    var next = mapped.state();
    if (!before.baseItemId().equals(next.baseItemId())
        || !before.snapshotId().equals(next.snapshotId()))
      return blocked("Changing the base requires importing a new item.");
    // Editing must retain original slot identity, reveal constraints and scaling factors.
    if ((!before.desecrated().isEmpty() || before.sanctification() != null)
        && (!before.implicits().equals(next.implicits())
            || !before.explicits().equals(next.explicits())
            || before.rarity() != next.rarity()))
      return blocked(
          "Modifier edits cannot reconstruct desecrated slots or sanctification factors.");
    var observed = next.observations();
    var previousObserved = before.observations();
    Integer expectedSockets =
        before.augmentSockets() != null
            ? before.augmentSockets()
            : previousObserved == null ? null : previousObserved.socketCount();
    Integer nextSockets =
        next.augmentSockets() != null
            ? next.augmentSockets()
            : observed == null ? null : observed.socketCount();
    boolean sameSkills =
        observed == null
            || observed.grantedSkills().isEmpty()
            || (previousObserved != null
                && observed.grantedSkills().equals(previousObserved.grantedSkills()))
            || observed.grantedSkills().stream()
                .allMatch(
                    skill ->
                        skill.level() == null
                            && BaseRegistry.recognizesGrantedSkill(
                                before.baseItemId(), "Grants Skill: " + skill.name()));
    if (!java.util.Objects.equals(expectedSockets, nextSockets)
        || (!(before.installedAugments().isEmpty()
                && SocketAugmentText.effects(next).isEmpty()
                && previousObserved != null)
            && !SocketAugmentText.sameEffects(
                SocketAugmentText.effects(before), SocketAugmentText.effects(next)))
        || !sameSkills)
      return blocked(
          "Socket and Rune lines are protected; use the socket controls to change installed augments.");
    if ((next.catalystQuality() != null
            && !java.util.Objects.equals(next.catalystQuality(), before.catalystQuality()))
        || (!next.conditions().isEmpty() && !next.conditions().equals(before.conditions())))
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
        ItemTextStateValidation.validate(merged, catalog).stream()
            .map(message -> new SolarTextMapper.Issue(0, message))
            .toList();
    return new SolarTextMapper.Result(issues.isEmpty(), issues.isEmpty() ? merged : null, issues);
  }

  private static SolarTextMapper.Result blocked(String message) {
    return new SolarTextMapper.Result(false, null, List.of(new SolarTextMapper.Issue(0, message)));
  }
}
