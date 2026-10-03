package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Map;

/** Source-matched Rattling Sceptre affixes. Spirit and innate skill are display-only. */
public final class SceptreEssenceTargets {
  private SceptreEssenceTargets() {}

  public static final String BASE_ID =
      "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1";
  public static final Map<WorkbenchCurrency, List<String>> VERIFIED =
      Map.ofEntries(
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_COMMAND,
              List.of("rattling-sceptre:prefix:agitative")),
          Map.entry(
              WorkbenchCurrency.ESSENCE_COMMAND, List.of("rattling-sceptre:prefix:provocative")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_COMMAND,
              List.of("rattling-sceptre:prefix:motivating")));
}
