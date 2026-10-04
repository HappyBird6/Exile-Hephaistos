package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Map;

/** Individually source-matched Attuned Wand targets; innate Mana Drain is not simulated. */
public final class WandEssenceTargets {
  private WandEssenceTargets() {}

  public static final String BASE_ID = "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3";
  public static final Map<WorkbenchCurrency, List<String>> VERIFIED =
      Map.ofEntries(
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_SORCERY, List.of("attuned-wand:prefix:adept-s")),
          Map.entry(WorkbenchCurrency.ESSENCE_SORCERY, List.of("attuned-wand:prefix:professor-s")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_SORCERY, List.of("attuned-wand:prefix:incanter-s")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_SEEKING,
              List.of("attuned-wand:suffix:of-havoc:spellcriticalstrikechance2")),
          Map.entry(WorkbenchCurrency.ESSENCE_SEEKING, List.of("attuned-wand:suffix:of-disaster")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_SEEKING,
              List.of("attuned-wand:suffix:of-calamity")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_ALACRITY,
              List.of("attuned-wand:suffix:of-nimbleness")),
          Map.entry(
              WorkbenchCurrency.ESSENCE_ALACRITY, List.of("attuned-wand:suffix:of-expertise")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_ALACRITY,
              List.of("attuned-wand:suffix:of-legerdemain")));
}
