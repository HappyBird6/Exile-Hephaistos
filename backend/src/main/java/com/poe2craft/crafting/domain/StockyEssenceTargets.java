package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Map;

/** Reviewed against PoE2DB Gloves_str and individual Data/Mods details on 2026-10-03. */
public final class StockyEssenceTargets {
  private StockyEssenceTargets() {}

  public static final Map<WorkbenchCurrency, List<String>> VERIFIED =
      Map.ofEntries(
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_BODY, List.of("stocky-mitts:prefix:sanguine")),
          Map.entry(WorkbenchCurrency.ESSENCE_BODY, List.of("stocky-mitts:prefix:robust")),
          Map.entry(WorkbenchCurrency.GREATER_ESSENCE_BODY, List.of("stocky-mitts:prefix:rotund")),
          Map.entry(WorkbenchCurrency.LESSER_ESSENCE_MIND, List.of("stocky-mitts:prefix:azure")),
          Map.entry(WorkbenchCurrency.ESSENCE_MIND, List.of("stocky-mitts:prefix:aqua")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_MIND, List.of("stocky-mitts:prefix:opalescent")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_RUIN, List.of("stocky-mitts:suffix:of-the-lost")),
          Map.entry(WorkbenchCurrency.ESSENCE_RUIN, List.of("stocky-mitts:suffix:of-banishment")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_RUIN, List.of("stocky-mitts:suffix:of-expulsion")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_INSULATION,
              List.of("stocky-mitts:suffix:of-the-salamander")),
          Map.entry(
              WorkbenchCurrency.ESSENCE_INSULATION, List.of("stocky-mitts:suffix:of-the-kiln")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_INSULATION,
              List.of("stocky-mitts:suffix:of-the-volcano")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_THAWING,
              List.of("stocky-mitts:suffix:of-the-penguin")),
          Map.entry(WorkbenchCurrency.ESSENCE_THAWING, List.of("stocky-mitts:suffix:of-the-yeti")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_THAWING,
              List.of("stocky-mitts:suffix:of-the-polar-bear")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_GROUNDING,
              List.of("stocky-mitts:suffix:of-the-squall")),
          Map.entry(
              WorkbenchCurrency.ESSENCE_GROUNDING,
              List.of("stocky-mitts:suffix:of-the-thunderhead")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_GROUNDING,
              List.of("stocky-mitts:suffix:of-the-maelstrom")),
          Map.entry(
              WorkbenchCurrency.LESSER_ESSENCE_OPULENCE, List.of("stocky-mitts:suffix:of-plunder")),
          Map.entry(WorkbenchCurrency.ESSENCE_OPULENCE, List.of("stocky-mitts:suffix:of-raiding")),
          Map.entry(
              WorkbenchCurrency.GREATER_ESSENCE_OPULENCE,
              List.of("stocky-mitts:suffix:of-archaeology")));

  public static final Map<WorkbenchCurrency, List<String>> REPLACEMENTS =
      Map.of(
          WorkbenchCurrency.ESSENCE_HYSTERIA, List.of("stocky-mitts:suffix:of-fury"),
          WorkbenchCurrency.ESSENCE_ABYSS,
              List.of(
                  "stocky-mitts:prefix:essence-abyssal-mark",
                  "stocky-mitts:suffix:essence-abyssal-mark"));
}
