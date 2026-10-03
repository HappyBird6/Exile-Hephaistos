package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;

/** Source-verified maximum only; neither applied quality nor scaled item stats are modeled. */
public final class QualityLimitRules {
  public static final String VERSION = "quality-limit-v1";
  public static final String BREACH_ID = "amulet:prefix:essence-maximum-quality";
  public static final String STAT_ID = "local_maximum_quality_+";

  private QualityLimitRules() {}

  public static Limit describe(ItemState state, ItemCatalog catalog) {
    if (state == null || !new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported state for quality limit");
    if (!state.baseItemId().equals(SolarAmulet.BASE_ID)
        && !state.baseItemId().equals("Metadata/Items/Armours/Gloves/FourGlovesStr1"))
      return null; // Other catalogs do not inherit a reviewed cap.
    int maximum = 20;
    for (var instance : state.explicits()) {
      if (!instance.values().containsKey(STAT_ID)) continue;
      var definition = catalog.find(instance.modifierId()).orElseThrow();
      if (!state.baseItemId().equals(SolarAmulet.BASE_ID)
          || !instance.modifierId().equals(BREACH_ID)
          || !definition.familyIds().equals(java.util.Set.of("LocalMaximumQuality"))
          || definition.affixType() != ModifierDefinition.AffixType.PREFIX
          || definition.stats().size() != 1
          || definition.stats().getFirst().min() != 20
          || definition.stats().getFirst().max() != 20
          || instance.values().get(STAT_ID) != 20L)
        throw new IllegalArgumentException("Unreviewed maximum-quality modifier");
      maximum = 40;
    }
    return new Limit(VERSION, maximum);
  }

  public record Limit(String ruleVersion, int maximumQuality) {}
}
