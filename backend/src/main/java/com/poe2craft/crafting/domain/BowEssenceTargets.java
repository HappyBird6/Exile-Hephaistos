package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Map;

/** Current PoE2DB Bows essence Code, source level, family and effect verified individually. */
public final class BowEssenceTargets {
  private BowEssenceTargets() {}

  public static final String BASE_ID = "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1";
  public static final Map<WorkbenchCurrency, List<String>> VERIFIED =
      Map.ofEntries(
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_ABRASION, List.of("crude-bow:prefix:burnished")),
          Map.entry(WorkbenchCurrency.ESSENCE_ABRASION, List.of("crude-bow:prefix:gleaming")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_ABRASION, List.of("crude-bow:prefix:razor-sharp")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_FLAMES, List.of("crude-bow:prefix:smouldering")),
          Map.entry(WorkbenchCurrency.ESSENCE_FLAMES, List.of("crude-bow:prefix:flaming")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_FLAMES, List.of("crude-bow:prefix:incinerating")),
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_ICE, List.of("crude-bow:prefix:chilled")),
          Map.entry(WorkbenchCurrency.ESSENCE_ICE, List.of("crude-bow:prefix:freezing")),
          Map.entry(WorkbenchCurrency.GREATER_ESSENCE_ICE, List.of("crude-bow:prefix:glaciated")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_ELECTRICITY, List.of("crude-bow:prefix:buzzing")),
          Map.entry(WorkbenchCurrency.ESSENCE_ELECTRICITY, List.of("crude-bow:prefix:sparking")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_ELECTRICITY, List.of("crude-bow:prefix:shocking")),
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_BATTLE, List.of("crude-bow:prefix:focused")),
          Map.entry(WorkbenchCurrency.ESSENCE_BATTLE, List.of("crude-bow:prefix:consistent")),
          Map.entry(WorkbenchCurrency.GREATER_ESSENCE_BATTLE, List.of("crude-bow:prefix:hunter-s")),
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_HASTE, List.of("crude-bow:suffix:of-ease")),
          Map.entry(WorkbenchCurrency.ESSENCE_HASTE, List.of("crude-bow:suffix:of-mastery")),
          Map.entry(WorkbenchCurrency.GREATER_ESSENCE_HASTE, List.of("crude-bow:suffix:of-renown")),
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_SEEKING, List.of("crude-bow:suffix:of-havoc")),
          Map.entry(WorkbenchCurrency.ESSENCE_SEEKING, List.of("crude-bow:suffix:of-disaster")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_SEEKING, List.of("crude-bow:suffix:of-calamity")));
}
