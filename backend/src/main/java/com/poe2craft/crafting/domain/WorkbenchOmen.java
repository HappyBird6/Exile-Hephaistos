package com.poe2craft.crafting.domain;

import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.Arrays;

/** Only individually verified effects. Combination order is deliberately unsupported. */
public enum WorkbenchOmen {
  SINISTRAL_CRYSTALLISATION(
      "Omen_of_Sinistral_Crystallisation", WorkbenchCurrency.ESSENCE_HYSTERIA, AffixType.PREFIX),
  DEXTRAL_CRYSTALLISATION(
      "Omen_of_Dextral_Crystallisation", WorkbenchCurrency.ESSENCE_HYSTERIA, AffixType.SUFFIX),
  GREATER_EXALTATION("Omen_of_Greater_Exaltation", WorkbenchCurrency.EXALTED, null),
  HOMOGENISING_EXALTATION("Omen_of_Homogenising_Exaltation", WorkbenchCurrency.EXALTED, null),
  HOMOGENISING_CORONATION("Omen_of_Homogenising_Coronation", WorkbenchCurrency.REGAL, null),
  SINISTRAL_EXALTATION("Omen_of_Sinistral_Exaltation", WorkbenchCurrency.EXALTED, AffixType.PREFIX),
  DEXTRAL_EXALTATION("Omen_of_Dextral_Exaltation", WorkbenchCurrency.EXALTED, AffixType.SUFFIX),
  SINISTRAL_ANNULMENT("Omen_of_Sinistral_Annulment", WorkbenchCurrency.ANNULMENT, AffixType.PREFIX),
  DEXTRAL_ANNULMENT("Omen_of_Dextral_Annulment", WorkbenchCurrency.ANNULMENT, AffixType.SUFFIX),
  SINISTRAL_ERASURE("Omen_of_Sinistral_Erasure", WorkbenchCurrency.CHAOS, AffixType.PREFIX),
  DEXTRAL_ERASURE("Omen_of_Dextral_Erasure", WorkbenchCurrency.CHAOS, AffixType.SUFFIX),
  WHITTLING("Omen_of_Whittling", WorkbenchCurrency.CHAOS, null),
  BLESSED("Omen_of_the_Blessed", WorkbenchCurrency.DIVINE, null);

  private final String id;
  private final WorkbenchCurrency trigger;
  private final AffixType affix;

  WorkbenchOmen(String id, WorkbenchCurrency trigger, AffixType affix) {
    this.id = id;
    this.trigger = trigger;
    this.affix = affix;
  }

  public String id() {
    return id;
  }

  public WorkbenchCurrency trigger() {
    return trigger;
  }

  public AffixType affix() {
    return affix;
  }

  public boolean homogenising() {
    return this == HOMOGENISING_EXALTATION || this == HOMOGENISING_CORONATION;
  }

  /** Only this same-trigger pair has a reviewed combined effect. */
  public static boolean verifiedDoubleAddition(java.util.List<WorkbenchOmen> matching) {
    return matching.contains(GREATER_EXALTATION)
        && (matching.size() == 1
            || (matching.size() == 2 && matching.contains(HOMOGENISING_EXALTATION)));
  }

  public static WorkbenchOmen fromId(String id) {
    return Arrays.stream(values())
        .filter(o -> o.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported active omen"));
  }
}
