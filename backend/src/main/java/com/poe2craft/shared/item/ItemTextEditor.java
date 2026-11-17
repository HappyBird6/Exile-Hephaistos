package com.poe2craft.shared.item;

import java.util.List;

/**
 * 편집한 수치 원문을 적용하면서 복사 텍스트에 없는 기존 상태를 보존한다.
 *
 * <p>기존 계약: Applies editable raw-roll text while retaining state which clipboard text cannot
 * encode.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemTextEditor()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code merge(before, mapped, catalog)}: merge 처리에서 {@code ManualRevealCatalog.enrich},
 *       {@code ItemTextStateValidation.validate}을(를) 사용한다. 입력: {@code before}(변경전), {@code
 *       mapped}(mapped), {@code catalog}(카탈로그). 반환: {@code SolarTextMapper.Result}.
 *   <li>{@code blocked(message)}: 차단 처리에서 {@code SolarTextMapper.Result}, {@code
 *       SolarTextMapper.Issue}을(를) 사용한다. 입력: {@code message}(메시지). 반환: {@code
 *       SolarTextMapper.Result}.
 * </ul>
 */
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
                && SocketAugmentText.effects(next, catalog).isEmpty()
                && previousObserved != null)
            && !SocketAugmentText.sameEffects(
                SocketAugmentText.effects(before, catalog),
                SocketAugmentText.effects(next, catalog)))
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
            before.installedAugments(),
            before.qualityInfused());
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
