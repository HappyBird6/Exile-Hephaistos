package com.poe2craft.crafting.domain;

import java.util.*;

/** User-approved exploration policy. This never grants game eligibility or probability support. */
public final class AutomaticCurrencyPolicy {
  public static final String VERSION = "approved-187-2026-10-30-strict-exalted-v8";
  public static final int MAXIMUM_EXPLORED_EXPLICITS = 6;
  // The frozen 187-entry policy already excludes these as supplementary resources.
  // Their new manual enum names must not expand the approved automatic search inventory.
  private static final Map<String, String> MANUAL_CURRENCY_POLICY_IDS =
      Map.of(
          WorkbenchCurrency.ARMOURERS_SCRAP.name(), "EXTRA:Armourers_Scrap",
          WorkbenchCurrency.BLACKSMITHS_WHETSTONE.name(), "EXTRA:Blacksmiths_Whetstone");

  public enum Decision {
    USE,
    CONDITIONAL,
    EXCLUDED,
    DEFERRED
  }

  public enum Condition {
    NONE,
    GOAL_ACQUISITION,
    FIRST_AFFIX_ACQUISITION,
    RARITY_UPGRADE,
    TEMPORARY_REMOVAL,
    HYSTERIA_PARTS,
    BREACH_QUALITY,
    PRISMATIC_PARTS,
    SWIFT_PARTS,
    SOVEREIGN_PARTS,
    EXPANSIVE_PARTS,
    CYCLONIC_PARTS,
    MYSTIC_PARTS,
    TEMPORARY_REGAL_PREFIX,
    TEMPORARY_REGAL_SUFFIX,
    TEMPORARY_EXALTED_PREFIX,
    TEMPORARY_EXALTED_SUFFIX
  }

  public enum Purpose {
    ORDINARY,
    RARITY_UPGRADE,
    GOAL_ACQUISITION,
    TARGET_PREPARATION,
    TEMPORARY_AFFIX_FOR_REMOVAL,
    QUALITY_CHECKPOINT,
    QUALITY_BASE_CHECKPOINT
  }

  public enum Verdict {
    PERMITTED,
    EXCLUDED,
    DEFERRED,
    CONDITION_NOT_MET,
    UNREVIEWED
  }

  public record Entry(
      int number,
      String id,
      String nameKo,
      String sourceUrl,
      String effectKo,
      Decision decision,
      String restrictionKo,
      Condition condition) {}

  public record Document(
      int schemaVersion,
      String policyVersion,
      String sourceUrl,
      String sourceModifiedAt,
      int maximumExploredExplicits,
      List<Entry> entries) {
    public Document {
      entries = List.copyOf(entries);
    }
  }

  /** Family uses the reviewed base registry vocabulary. Omens also require the paired currency. */
  public record Context(String family, Purpose purpose, WorkbenchCurrency currency) {
    public Context {
      Objects.requireNonNull(family);
      Objects.requireNonNull(purpose);
    }
  }

  private final Document document;
  private final Map<String, Entry> entries;

  public AutomaticCurrencyPolicy(Document document) {
    if (document.schemaVersion() != 1
        || !VERSION.equals(document.policyVersion())
        || document.maximumExploredExplicits() != MAXIMUM_EXPLORED_EXPLICITS
        || document.entries().size() != 187)
      throw new IllegalArgumentException("Unrecognized automatic currency policy contract");
    var indexed = new LinkedHashMap<String, Entry>();
    for (int i = 0; i < document.entries().size(); i++) {
      var entry = document.entries().get(i);
      if (entry.number() != i + 1
          || entry.id() == null
          || entry.id().isBlank()
          || indexed.putIfAbsent(entry.id(), entry) != null
          || entry.decision() == null
          || entry.condition() == null
          || entry.sourceUrl() == null
          || !entry.sourceUrl().startsWith("https://poe2db.tw/kr/")
          || (entry.decision() == Decision.CONDITIONAL) != (entry.condition() != Condition.NONE))
        throw new IllegalArgumentException("Invalid policy entry or fixed numbering");
      if (entry.number() <= 160) WorkbenchCurrency.valueOf(entry.id());
      else if (entry.number() <= 178) {
        if (!entry.id().startsWith("OMEN:")) throw new IllegalArgumentException("Omen ID required");
        WorkbenchOmen.valueOf(entry.id().substring(5));
      } else if (!entry.id().startsWith("EXTRA:"))
        throw new IllegalArgumentException("Supplementary ID required");
    }
    if (indexed.keySet().stream().filter(id -> !id.contains(":")).count()
                + MANUAL_CURRENCY_POLICY_IDS.size()
            != WorkbenchCurrency.values().length
        || Arrays.stream(WorkbenchCurrency.values())
            .anyMatch(
                currency ->
                    !indexed.containsKey(
                        MANUAL_CURRENCY_POLICY_IDS.getOrDefault(currency.name(), currency.name())))
        || MANUAL_CURRENCY_POLICY_IDS.values().stream()
            .anyMatch(id -> indexed.get(id).decision() != Decision.EXCLUDED))
      throw new IllegalArgumentException("Policy must cover every defined currency");
    this.document = document;
    this.entries = Collections.unmodifiableMap(indexed);
  }

  public Document document() {
    return document;
  }

  public List<Entry> candidates() {
    return document.entries().stream()
        .filter(e -> e.decision() == Decision.USE || e.decision() == Decision.CONDITIONAL)
        .toList();
  }

  public Verdict evaluate(String id, Context context) {
    Objects.requireNonNull(context);
    var entry = entries.get(MANUAL_CURRENCY_POLICY_IDS.getOrDefault(id, id));
    if (entry == null) return Verdict.UNREVIEWED;
    if (entry.decision() == Decision.EXCLUDED) return Verdict.EXCLUDED;
    if (entry.decision() == Decision.DEFERRED) return Verdict.DEFERRED;
    boolean temporary = context.purpose() == Purpose.TEMPORARY_AFFIX_FOR_REMOVAL;
    boolean permitted =
        switch (entry.condition()) {
          case NONE, BREACH_QUALITY -> true;
          case RARITY_UPGRADE ->
              context.purpose() == Purpose.RARITY_UPGRADE
                  && context.currency() == WorkbenchCurrency.TRANSMUTATION
                  && context.family().equals("amulets");
          case FIRST_AFFIX_ACQUISITION -> context.purpose() == Purpose.GOAL_ACQUISITION;
          case GOAL_ACQUISITION ->
              context.purpose() == Purpose.GOAL_ACQUISITION
                  || context.purpose() == Purpose.TARGET_PREPARATION;
          case TEMPORARY_REMOVAL -> temporary || context.purpose() == Purpose.GOAL_ACQUISITION;
          case TEMPORARY_REGAL_PREFIX, TEMPORARY_REGAL_SUFFIX ->
              (temporary || context.purpose() == Purpose.GOAL_ACQUISITION)
                  && context.currency() == WorkbenchCurrency.REGAL;
          case TEMPORARY_EXALTED_PREFIX, TEMPORARY_EXALTED_SUFFIX ->
              (temporary || context.purpose() == Purpose.GOAL_ACQUISITION)
                  && context.currency() == WorkbenchCurrency.EXALTED;
          case HYSTERIA_PARTS -> Set.of("gloves", "boots", "quivers").contains(context.family());
          case PRISMATIC_PARTS -> context.family().equals("gloves");
          case SWIFT_PARTS ->
              Set.of("gloves", "rings", "shields", "bucklers", "foci").contains(context.family());
          case SOVEREIGN_PARTS -> Set.of("rings", "amulets", "belts").contains(context.family());
          case EXPANSIVE_PARTS -> context.family().equals("boots");
          case CYCLONIC_PARTS ->
              Set.of("boots", "gloves", "helmet", "helmets").contains(context.family());
          case MYSTIC_PARTS ->
              Set.of("helmet", "helmets", "gloves", "boots", "quivers").contains(context.family());
        };
    return permitted ? Verdict.PERMITTED : Verdict.CONDITION_NOT_MET;
  }

  /** Crossing this boundary is unexplored mass, including successful outcomes beyond the bound. */
  public boolean withinExplorationBoundary(int explicitCount) {
    if (explicitCount < 0) throw new IllegalArgumentException("Negative explicit count");
    return explicitCount <= document.maximumExploredExplicits();
  }
}
