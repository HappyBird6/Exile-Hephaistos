package com.poe2craft.crafting.domain;

import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.Arrays;

/** Bounded effects and explicitly reviewed composition rules; other combinations remain blocked. */
public enum WorkbenchOmen {
  SINISTRAL_ALCHEMY,
  DEXTRAL_ALCHEMY,
  SINISTRAL_CORONATION,
  DEXTRAL_CORONATION,
  GREATER_ANNULMENT,
  SINISTRAL_CRYSTALLISATION,
  DEXTRAL_CRYSTALLISATION,
  GREATER_EXALTATION,
  HOMOGENISING_EXALTATION,
  HOMOGENISING_CORONATION,
  SINISTRAL_EXALTATION,
  DEXTRAL_EXALTATION,
  SINISTRAL_ANNULMENT,
  DEXTRAL_ANNULMENT,
  SINISTRAL_ERASURE,
  DEXTRAL_ERASURE,
  WHITTLING,
  BLESSED;

  private WorkbenchDefinitions.Omen definition() {
    return WorkbenchDefinitions.active().omen(this);
  }

  public String id() {
    return definition().id();
  }

  public WorkbenchCurrency trigger() {
    return definition().trigger();
  }

  public AffixType affix() {
    return definition().affix();
  }

  public boolean homogenising() {
    return this == HOMOGENISING_EXALTATION || this == HOMOGENISING_CORONATION;
  }

  /** Reviewed single-effect composition with the currency's existing added-level pool. */
  public boolean supportsTieredCurrency() {
    return definition().supportsTieredCurrency();
  }

  /** User-specified order and its symmetric side: side pool, lowest level, uniform ties. */
  public static boolean sideWhittling(java.util.List<WorkbenchOmen> matching) {
    return matching.size() == 2
        && (matching.contains(SINISTRAL_ERASURE) || matching.contains(DEXTRAL_ERASURE))
        && matching.contains(WHITTLING);
  }

  /** Audited prefix/count example and symmetric suffix rule; not arbitrary combinations. */
  public static boolean sideDoubleRemoval(java.util.List<WorkbenchOmen> matching) {
    return matching.size() == 2
        && (matching.contains(SINISTRAL_ANNULMENT) || matching.contains(DEXTRAL_ANNULMENT))
        && matching.contains(GREATER_ANNULMENT);
  }

  /** Only this same-trigger pair has a reviewed combined effect. */
  public static boolean verifiedDoubleAddition(java.util.List<WorkbenchOmen> matching) {
    return matching.contains(GREATER_EXALTATION)
        && (matching.size() == 1
            || (matching.size() == 2 && matching.contains(HOMOGENISING_EXALTATION)));
  }

  public static WorkbenchOmen fromId(String id) {
    return Arrays.stream(values())
        .filter(o -> o.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported active omen"));
  }
}
