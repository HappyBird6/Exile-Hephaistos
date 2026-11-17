package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;

/**
 * 현재 상한과 타락 여부를 확인해 아이템당 한 번의 품질 주입을 처리한다.
 *
 * <p>기존 계약: Simulator-specific infusion: once per item, no corruption, bounded by the active
 * quality cap.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "quality-infuser-once-plus-ten-v1"} ({@code String}).
 *   <li>{@code INCREASE}: INCREASE; 초기값 {@code 10} ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code QualityInfuserRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supports(action)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code action}(행동). 반환: {@code boolean}.
 *   <li>{@code refusal(state, action, catalog)}: refusal 처리에서 {@code state.conditions}, {@code
 *       state.qualityInfused}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       catalog}(카탈로그). 반환: {@code String}.
 *   <li>{@code current(state, action)}: 현재 버전의 정의나 실행 값을 조회한다. 입력: {@code state}(상태), {@code
 *       action}(행동). 반환: {@code int}.
 *   <li>{@code apply(state, action, catalog)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code catalog}(카탈로그). 반환: {@code ItemState}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 */
public final class QualityInfuserRules {
  public static final String VERSION = "quality-infuser-once-plus-ten-v1";
  public static final int INCREASE = 10;

  private QualityInfuserRules() {}

  public static boolean supports(WorkbenchCurrency action) {
    return action == WorkbenchCurrency.VAAL_ARMOURERS_INFUSER
        || action == WorkbenchCurrency.VAAL_BLACKSMITHS_INFUSER
        || action == WorkbenchCurrency.VAAL_ARCANISTS_INFUSER
        || action == WorkbenchCurrency.VAAL_CATALYSING_INFUSER;
  }

  public static String refusal(ItemState state, WorkbenchCurrency action, ItemCatalog catalog) {
    if (!supports(action)) return "Unsupported quality infuser.";
    if (!state.conditions().isEmpty()) return "This item cannot be modified.";
    if (state.qualityInfused()) return "A quality infuser has already been used on this item.";
    String base = state.baseItemId();
    boolean caster =
        base.contains("/Wands/") || base.contains("/Staves/") || base.contains("/Sceptres/");
    boolean jewellery = action == WorkbenchCurrency.VAAL_CATALYSING_INFUSER;
    boolean matches =
        switch (action) {
          case VAAL_ARMOURERS_INFUSER -> base.startsWith("Metadata/Items/Armours/");
          case VAAL_BLACKSMITHS_INFUSER -> base.startsWith("Metadata/Items/Weapons/") && !caster;
          case VAAL_ARCANISTS_INFUSER -> caster;
          case VAAL_CATALYSING_INFUSER ->
              base.startsWith("Metadata/Items/Rings/")
                  || base.startsWith("Metadata/Items/Amulets/");
          default -> false;
        };
    if (!matches) return "This infuser does not support this item class.";
    if (jewellery && state.catalystQuality() == null)
      return "Apply a catalyst type before using this infuser.";
    if (!jewellery && state.equipmentQuality() == null)
      return "The item's current quality is unknown.";
    var cap = QualityLimitRules.describe(state, catalog);
    if (cap == null) return "This item has no supported quality limit.";
    if (current(state, action) >= cap.maximumQuality() + INCREASE)
      return "This item has reached the infuser quality limit.";
    return "";
  }

  public static int current(ItemState state, WorkbenchCurrency action) {
    return action == WorkbenchCurrency.VAAL_CATALYSING_INFUSER
        ? state.catalystQuality().amount()
        : state.equipmentQuality();
  }

  public static ItemState apply(ItemState state, WorkbenchCurrency action, ItemCatalog catalog) {
    String reason = refusal(state, action, catalog);
    if (!reason.isEmpty()) throw new IllegalArgumentException(reason);
    int amount =
        Math.min(
            current(state, action) + INCREASE,
            QualityLimitRules.describe(state, catalog).maximumQuality() + INCREASE);
    if (action != WorkbenchCurrency.VAAL_CATALYSING_INFUSER)
      return state.withEquipmentQuality(amount).withQualityInfused(true);
    return new ItemState(
        state.snapshotId(),
        state.baseItemId(),
        state.itemLevel(),
        state.rarity(),
        state.implicits(),
        state.explicits(),
        state.conditions(),
        state.augmentSockets(),
        new CatalystQuality(state.catalystQuality().type(), amount),
        state.desecrated(),
        state.equipmentQuality(),
        state.observations(),
        state.sanctification(),
        state.installedAugments(),
        true);
  }
}
