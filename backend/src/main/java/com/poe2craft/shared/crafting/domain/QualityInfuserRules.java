package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;

/** Simulator-specific infusion: once per item, no corruption, bounded by the active quality cap. */
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
