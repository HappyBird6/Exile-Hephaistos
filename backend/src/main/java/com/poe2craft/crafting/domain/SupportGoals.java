package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.*;

/** One family/tier goal; assesses the root and feasibility under finite additions only. */
public final class SupportGoals {
  private final ItemCatalog catalog;
  private final Map<String, List<ModifierDefinition>> families = new TreeMap<>();

  public SupportGoals(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    for (var d : catalog.modifiers().values()) {
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT || d.weight() == 0) continue;
      if (d.familyIds().size() != 1)
        throw new IllegalArgumentException("Support family grouping needs verification");
      families.computeIfAbsent(d.familyIds().iterator().next(), key -> new ArrayList<>()).add(d);
    }
    for (var ds : families.values()) {
      if (ds.stream().anyMatch(d -> d.affixType() != ds.getFirst().affixType()))
        throw new IllegalArgumentException("Cross-affix family projection needs verification");
      ds.sort(
          Comparator.comparingInt(ModifierDefinition::tier).thenComparing(ModifierDefinition::id));
      for (int i = 1; i < ds.size(); i++)
        if (ds.get(i - 1).requiredItemLevel() < ds.get(i).requiredItemLevel())
          throw new IllegalArgumentException("Catalog tier ranking needs verification");
    }
  }

  public List<Family> families() {
    return families.entrySet().stream()
        .map(
            e -> {
              var examples = new LinkedHashMap<List<String>, String>();
              for (var d : e.getValue())
                examples.putIfAbsent(
                    d.stats().stream().map(ModifierDefinition.StatRange::id).sorted().toList(),
                    d.text());
              return new Family(
                  e.getKey(),
                  e.getValue().getFirst().affixType(),
                  e.getValue().stream()
                      .map(d -> new Tier(d.tier(), d.requiredItemLevel(), d.text(), d.id()))
                      .toList(),
                  List.copyOf(examples.values()));
            })
        .toList();
  }

  public Assessment assess(StateBucket state, Goal goal) {
    new CraftingEngine(catalog).validate(state);
    if (goal == null || goal.required() == null || goal.candidates() == null)
      return invalid("Provide required and candidate family conditions.");
    var all = new ArrayList<>(goal.required());
    all.addAll(goal.candidates());
    if (all.size() > 30
        || goal.candidateCount() < 0
        || goal.candidateCount() > goal.candidates().size())
      return invalid(
          "Candidate N must be between zero and the number of distinct candidate families.");
    if (goal.required().isEmpty() && goal.candidateCount() == 0)
      return invalid("Choose at least one required condition or a positive candidate N.");
    var seen = new HashSet<String>();
    for (var condition : all) {
      if (condition == null || !families.containsKey(condition.family()))
        return invalid("Unknown modifier family.");
      if (!seen.add(condition.family()))
        return invalid("Each family may appear once across required and candidate conditions.");
      if (families.get(condition.family()).stream()
          .noneMatch(d -> d.tier() == condition.minimumTier()))
        return invalid("Select a tier that exists in the chosen family.");
    }
    var present = new HashMap<String, ModifierDefinition>();
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      if (d.requiredItemLevel() > state.itemLevel())
        throw new IllegalArgumentException("Modifier exceeds item level");
      present.put(d.familyIds().iterator().next(), d);
    }
    var matches = new TreeMap<String, Boolean>();
    all.forEach(
        c ->
            matches.put(
                c.family(),
                present.containsKey(c.family())
                    && present.get(c.family()).tier() <= c.minimumTier()));
    int requiredMatched =
        (int) goal.required().stream().filter(c -> matches.get(c.family())).count();
    int candidatesMatched =
        (int) goal.candidates().stream().filter(c -> matches.get(c.family())).count();
    boolean achieved =
        requiredMatched == goal.required().size() && candidatesMatched >= goal.candidateCount();
    if (achieved)
      return new Assessment(
          "ACHIEVED", true, true, true, requiredMatched, candidatesMatched, matches, List.of());
    var issues = new ArrayList<String>();
    int freeP =
        catalog.base().rarePrefixes()
            - (int)
                present.values().stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count();
    int freeS =
        catalog.base().rareSuffixes()
            - (int)
                present.values().stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                    .count();
    int needP = 0, needS = 0, possibleP = 0, possibleS = 0;
    for (var c : goal.required()) {
      if (matches.get(c.family())) continue;
      if (!canAdd(c, present, state.itemLevel()))
        issues.add(
            "Required family "
                + c.family()
                + " cannot meet T"
                + c.minimumTier()
                + " or better through additions; it is occupied below target or has no eligible tier at this item level.");
      if (families.get(c.family()).getFirst().affixType() == ModifierDefinition.AffixType.PREFIX)
        needP++;
      else needS++;
    }
    if (needP > freeP || needS > freeS)
      issues.add("Required conditions exceed the remaining prefix/suffix capacity.");
    for (var c : goal.candidates()) {
      if (matches.get(c.family()) || !canAdd(c, present, state.itemLevel())) continue;
      if (families.get(c.family()).getFirst().affixType() == ModifierDefinition.AffixType.PREFIX)
        possibleP++;
      else possibleS++;
    }
    int possible =
        candidatesMatched
            + Math.min(Math.max(0, freeP - needP), possibleP)
            + Math.min(Math.max(0, freeS - needS), possibleS);
    if (possible < goal.candidateCount())
      issues.add(
          "Candidate N cannot be reached with eligible distinct families and remaining affix capacity.");
    return new Assessment(
        issues.isEmpty() ? "READY" : "IMPOSSIBLE",
        true,
        false,
        issues.isEmpty(),
        requiredMatched,
        candidatesMatched,
        matches,
        issues);
  }

  private boolean canAdd(Condition c, Map<String, ModifierDefinition> present, int level) {
    return !present.containsKey(c.family())
        && families.get(c.family()).stream()
            .anyMatch(
                d ->
                    d.weight() > 0
                        && d.requiredItemLevel() <= level
                        && d.tier() <= c.minimumTier());
  }

  private Assessment invalid(String reason) {
    return new Assessment("INVALID_GOAL", false, false, false, 0, 0, Map.of(), List.of(reason));
  }

  public record Condition(String family, int minimumTier) {}

  public record Goal(List<Condition> required, List<Condition> candidates, int candidateCount) {}

  public record Family(
      String id,
      ModifierDefinition.AffixType affix,
      List<Tier> tiers,
      List<String> effectExamples) {}

  public record Tier(int tier, int requiredItemLevel, String exampleText, String modifierId) {}

  public record Assessment(
      String status,
      boolean valid,
      boolean achieved,
      boolean feasible,
      int requiredMatched,
      int candidatesMatched,
      Map<String, Boolean> matches,
      List<String> issues) {
    public Assessment {
      matches = Map.copyOf(matches);
      issues = List.copyOf(issues);
    }
  }
}
