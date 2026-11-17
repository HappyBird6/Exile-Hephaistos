package com.poe2craft.shared.item;

import java.util.Collection;
import java.util.Set;

/**
 * 기본 주얼의 정체성과 영구 제작 옵션 규칙을 처리한다.
 *
 * <p>기존 계약: Reviewed Basic Jewel identities and persistent Crafted mechanics; no passive-tree
 * simulation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASES}: 베이스목록 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicJewel()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supported(base)}: 지원 처리에서 {@code BASES.contains}을(를) 사용한다. 입력: {@code base}(베이스).
 *       반환: {@code boolean}.
 *   <li>{@code timeLost(base)}: 시간·Lost의 입력에 따른 분기·결과를 처리한다. 입력: {@code base}(베이스). 반환: {@code
 *       boolean}.
 *   <li>{@code supportedCrafting(base)}: 지원·제작의 입력에 따른 분기·결과를 처리한다. 입력: {@code base}(베이스). 반환:
 *       {@code boolean}.
 *   <li>{@code extra(ids, side)}: extra 처리에서 {@code ids.stream}, {@code id.endsWith}을(를) 사용한다. 입력:
 *       {@code ids}(식별자목록), {@code side}(접두접미). 반환: {@code int}.
 *   <li>{@code existingCapacity(p, s, maxP, maxS)}: 기존·용량의 입력에 따른 분기·결과를 처리한다. 입력: {@code p}(p),
 *       {@code s}(s), {@code maxP}(최대값·P), {@code maxS}(최대값·S). 반환: {@code boolean}.
 *   <li>{@code insertionCapacity(p, s, maxP, maxS)}: insertion·용량의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       p}(p), {@code s}(s), {@code maxP}(최대값·P), {@code maxS}(최대값·S). 반환: {@code int}.
 *   <li>{@code chaosLockedSides(state, catalog)}: 카오스·잠긴·Sides 처리에서 {@code state.baseItemId},
 *       {@code state.rarity}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그). 반환: {@code
 *       Set<ModifierDefinition.AffixType>}.
 *   <li>{@code effect(state, side)}: 효과 처리에서 {@code state.explicits}, {@code m.modifierId}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code side}(접두접미). 반환: {@code long}.
 * </ul>
 */
public final class BasicJewel {
  private static final Set<String> BASES =
      Set.of(
          "Metadata/Items/Jewels/JewelStr",
          "Metadata/Items/Jewels/JewelDex",
          SapphireJewel.BASE_ID,
          "Metadata/Items/Jewels/JewelDiamond");

  private BasicJewel() {}

  public static boolean supported(String base) {
    return BASES.contains(base);
  }

  public static boolean timeLost(String base) {
    return Set.of(
            "Metadata/Items/Jewels/JewelRadiusStr",
            "Metadata/Items/Jewels/JewelRadiusDex",
            "Metadata/Items/Jewels/JewelRadiusInt",
            "Metadata/Items/Jewels/JewelRadiusDiamond")
        .contains(base);
  }

  public static boolean supportedCrafting(String base) {
    return supported(base) || timeLost(base);
  }

  public static int extra(Collection<String> ids, ModifierDefinition.AffixType side) {
    String code =
        side == ModifierDefinition.AffixType.PREFIX
            ? ":crafted:CraftedJewelAdditionalPrefixAllowed"
            : ":crafted:CraftedJewelAdditionalSuffixAllowed";
    return ids.stream().anyMatch(id -> id.endsWith(code)) ? 1 : 0;
  }

  /** Cap loss preserves reachable existing overflow, while insertion still uses current caps. */
  public static boolean existingCapacity(int p, int s, int maxP, int maxS) {
    return p + s <= 5 && (maxP + maxS > 4 ? p <= maxP && s <= maxS : p <= 3 && s <= 3);
  }

  /** Existing overflow occupies its own slots; it never increases the other side's cap. */
  public static int insertionCapacity(int p, int s, int maxP, int maxS) {
    return Math.min(5, maxP + maxS + Math.max(0, p - maxP) + Math.max(0, s - maxS));
  }

  /** User-specified Chaos rule: an existing over-cap side is immutable during replacement. */
  public static Set<ModifierDefinition.AffixType> chaosLockedSides(
      ItemState state, ItemCatalog catalog) {
    if (!supportedCrafting(state.baseItemId()) || state.rarity() != ItemState.Rarity.RARE)
      return Set.of();
    var slots = new ItemStateValidator(catalog).slots(state);
    var locked = java.util.EnumSet.noneOf(ModifierDefinition.AffixType.class);
    if (slots.usedPrefixes() > slots.maxPrefixes()) locked.add(ModifierDefinition.AffixType.PREFIX);
    if (slots.usedSuffixes() > slots.maxSuffixes()) locked.add(ModifierDefinition.AffixType.SUFFIX);
    return Set.copyOf(locked);
  }

  public static long effect(ItemState state, ModifierDefinition.AffixType side) {
    String code =
        side == ModifierDefinition.AffixType.PREFIX
            ? ":crafted:CraftedJewelPrefixEffect"
            : ":crafted:CraftedJewelSuffixEffect";
    return state.explicits().stream()
        .filter(m -> m.modifierId().endsWith(code))
        .mapToLong(m -> m.values().get("display_source_value"))
        .findFirst()
        .orElse(0);
  }
}
