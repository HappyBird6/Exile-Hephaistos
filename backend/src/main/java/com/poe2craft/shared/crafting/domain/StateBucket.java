package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 지원 행동의 정확한 상태 구분을 유지하면서 명시 옵션 수치 굴림은 주변화한다.
 *
 * <p>기존 계약: Exact projection for the six supported actions: explicit numeric rolls are
 * marginalized.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
 *   <li>{@code baseItemId}: 원본 베이스 식별자 ({@code String}).
 *   <li>{@code itemLevel}: 옵션 생성 제한에 사용하는 아이템 레벨 ({@code int}).
 *   <li>{@code rarity}: 현재 아이템 희귀도 ({@code ItemState.Rarity}).
 *   <li>{@code implicits}: 아이템의 암시 옵션 목록 ({@code List<ModifierInstance>}).
 *   <li>{@code modifierIds}: 옵션·식별자목록 ({@code List<String>}).
 *   <li>{@code conditions}: 타락 등 아이템 상태 조건 ({@code Set<ItemState.Condition>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code StateBucket(snapshotId, baseItemId, itemLevel, rarity, implicits, modifierIds,
 *       conditions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code
 *       baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code
 *       implicits}(암시옵션목록), {@code modifierIds}(옵션·식별자목록), {@code conditions}(상태조건). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code from(item)}: 다른 표현의 입력을 현재 표현으로 바꾼다. 입력: {@code item}(아이템). 반환: {@code StateBucket}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code forModifierEligibility(item)}: 대상·옵션·생성적격 처리에서 {@code item.snapshotId}, {@code
 *       item.baseItemId}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code StateBucket}.
 *   <li>{@code with(nextRarity, nextModifiers)}: 지정한 요소를 반영한 값을 구한다. 입력: {@code
 *       nextRarity}(다음·희귀도), {@code nextModifiers}(다음·옵션목록). 반환: {@code StateBucket}.
 *   <li>{@code id()}: 식별자 값을 조회·구성한다. 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 */
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
        || modifierIds.size() > 7
        || modifierIds.stream().anyMatch(id -> id == null || id.length() > 160)) {
      throw new IllegalArgumentException("Invalid bucket modifiers");
    }
    modifierIds = modifierIds.stream().sorted().toList();
    conditions = Set.copyOf(conditions);
  }

  public static StateBucket from(ItemState item) {
    if (item.catalystQuality() != null)
      throw new IllegalArgumentException(
          "Catalyst quality cannot be projected into an affix-only bucket");
    return forModifierEligibility(item);
  }

  /** Candidate eligibility ignores numeric quality; this projection must not replace the item. */
  public static StateBucket forModifierEligibility(ItemState item) {
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
