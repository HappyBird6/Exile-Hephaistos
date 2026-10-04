package com.poe2craft.item;

import java.util.Set;

/** Already present quality, never a catalyst use or a substitute for original modifier rolls. */
public record CatalystQuality(Type type, int amount) {
  public CatalystQuality {
    java.util.Objects.requireNonNull(type, "quality type");
    if (amount < 0 || amount > 100) throw new IllegalArgumentException("Invalid quality amount");
  }

  public enum Type {
    FLESH("life"),
    NEURAL("mana"),
    CARAPACE("defences", "armour", "evasion", "energyshield"),
    UUL_NETOL("physical"),
    XOPH("fire"),
    TUL("cold"),
    ESH("lightning"),
    CHAYULA("chaos"),
    REAVER("attack"),
    SIBILANT("caster"),
    SKITTERING("speed"),
    ADAPTIVE("attribute"),
    NECROTIC("minion");

    private final Set<String> tags;

    Type(String... tags) {
      this.tags = Set.of(tags);
    }

    public boolean matches(Set<String> modifierTags) {
      return modifierTags.stream().anyMatch(tags::contains);
    }
  }

  public static boolean supportedBase(String base) {
    return base.equals(SolarAmulet.BASE_ID)
        || base.equals("Metadata/Items/Rings/FourRing1")
        || ReviewedRings.supports(base)
        || BasicJewel.supported(base);
  }
}
