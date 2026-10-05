package com.poe2craft.item;

import java.util.Map;

/** Ordinary distinct-implicit Quiver representatives; effects are display-only. */
public final class ReviewedQuivers {
  public static final Map<String, String> BASES =
      Map.ofEntries(
          Map.entry("visceral-quiver", "Metadata/Items/Quivers/FourQuiver11"),
          Map.entry("volant-quiver", "Metadata/Items/Quivers/FourQuiver10"),
          Map.entry("penetrating-quiver", "Metadata/Items/Quivers/FourQuiver9"),
          Map.entry("primed-quiver", "Metadata/Items/Quivers/FourQuiver8"),
          Map.entry("serrated-quiver", "Metadata/Items/Quivers/FourQuiver7"),
          Map.entry("toxic-quiver", "Metadata/Items/Quivers/FourQuiver6"),
          Map.entry("blunt-quiver", "Metadata/Items/Quivers/FourQuiver5"),
          Map.entry("two-point-quiver", "Metadata/Items/Quivers/FourQuiver4"),
          Map.entry("sacral-quiver", "Metadata/Items/Quivers/FourQuiver3"),
          Map.entry("fire-quiver", "Metadata/Items/Quivers/FourQuiver2"),
          Map.entry("broadhead-quiver", "Metadata/Items/Quivers/FourQuiver1"));

  private ReviewedQuivers() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
