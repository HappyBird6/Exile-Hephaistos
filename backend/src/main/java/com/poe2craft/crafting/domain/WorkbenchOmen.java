package com.poe2craft.crafting.domain;

import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.Arrays;

/** Only individually verified effects. Combination order is deliberately unsupported. */
public enum WorkbenchOmen {
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

  public static WorkbenchOmen fromId(String id) {
    return Arrays.stream(values())
        .filter(o -> o.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported active omen"));
  }
}
