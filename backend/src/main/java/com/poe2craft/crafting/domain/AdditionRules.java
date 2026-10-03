package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.*;

/** Shared eligibility and minimum-level pool for sampling and exact finite additions. */
public final class AdditionRules {
  private final ItemCatalog catalog;
  private final ModifierPoolResolver resolver;
  private final CraftingEngine validator;

  public AdditionRules(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    resolver = new ModifierPoolResolver(catalog);
    validator = new CraftingEngine(catalog);
  }

  public static boolean isAddition(WorkbenchCurrency action) {
    return action != null
        && action.baseAction() != null
        && action.baseAction() != CraftingAction.CHAOS
        && action.baseAction() != CraftingAction.ANNULMENT;
  }

  public Plan plan(StateBucket state, WorkbenchCurrency action, Set<String> activeOmens) {
    validator.validate(state);
    Objects.requireNonNull(action, "action");
    var ids = activeOmens == null ? List.<String>of() : activeOmens.stream().sorted().toList();
    var omens = ids.stream().map(WorkbenchOmen::fromId).toList();
    if (!isAddition(action))
      return blocked(state, ids, "This action is outside the finite addition model.");
    var matching =
        omens.stream().filter(o -> o.trigger().baseAction() == action.baseAction()).toList();
    if (matching.size() > 1)
      return blocked(
          state,
          ids,
          "Multiple omens for the same operation need combination verification. Deactivate all but one.");
    if (matching.contains(WorkbenchOmen.GREATER_EXALTATION))
      return blocked(state, ids, "Two-modifier additions are outside the finite addition model.");
    if (!matching.isEmpty() && action.minimumModifierLevel() > 0)
      return blocked(
          state,
          ids,
          "Omen interaction with Greater/Perfect currency is not verified. Deactivate the omen or use ordinary currency.");
    if (state.itemLevel() < action.minimumModifierLevel())
      return blocked(state, ids, "Item level is below the currency's minimum modifier level.");
    boolean rarity =
        switch (action.baseAction()) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED -> state.rarity() == ItemState.Rarity.RARE;
          default -> false;
        };
    if (!rarity) return blocked(state, ids, "This currency cannot be used on this rarity.");
    var next = state.with(upgradedRarity(state, action), state.modifierIds());
    var candidates = pool(next, action, matching.isEmpty() ? null : matching.getFirst());
    if (candidates.isEmpty())
      return blocked(
          state,
          ids,
          "No eligible modifier or free affix slot remains for this currency and omen.");
    var consumed = matching.stream().map(WorkbenchOmen::id).toList();
    return new Plan(
        true,
        "",
        next,
        candidates,
        consumed,
        ids.stream().filter(id -> !consumed.contains(id)).toList());
  }

  private Plan blocked(StateBucket state, List<String> ids, String reason) {
    return new Plan(false, reason, state, List.of(), List.of(), ids);
  }

  public static ItemState.Rarity upgradedRarity(StateBucket state, WorkbenchCurrency action) {
    return action.baseAction() == CraftingAction.TRANSMUTATION
        ? ItemState.Rarity.MAGIC
        : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity();
  }

  /** PoE2DB GenGroup = generation type + first family; this snapshot is single-family. */
  public List<ModifierDefinition> pool(
      StateBucket state, WorkbenchCurrency currency, WorkbenchOmen omen) {
    var eligible = resolver.resolve(state).candidates();
    if (omen != null && omen.affix() != null)
      eligible = eligible.stream().filter(d -> d.affixType() == omen.affix()).toList();
    if (omen != null && omen.homogenising()) eligible = matchingTags(eligible, existingTags(state));
    if (currency.minimumModifierLevel() == 0) return eligible;
    var highest = new HashMap<String, Integer>();
    for (var d : eligible) highest.merge(group(d), d.requiredItemLevel(), Math::max);
    return eligible.stream()
        .filter(
            d ->
                d.requiredItemLevel() >= currency.minimumModifierLevel()
                    || d.requiredItemLevel() == highest.get(group(d)))
        .toList();
  }

  private String group(ModifierDefinition d) {
    if (d.familyIds().size() != 1)
      throw new IllegalArgumentException("Modifier-type grouping needs verification");
    return d.affixType() + ":" + d.familyIds().iterator().next();
  }

  /** Modifier tags, not spawn tags or families; the caller freezes this set before a multi-add. */
  public Set<String> existingTags(StateBucket state) {
    var tags = new HashSet<String>();
    java.util.stream.Stream.concat(
            state.modifierIds().stream(),
            state.implicits().stream().map(com.poe2craft.item.ModifierInstance::modifierId))
        .forEach(id -> tags.addAll(catalog.find(id).orElseThrow().tags()));
    return Set.copyOf(tags);
  }

  public static List<ModifierDefinition> matchingTags(
      List<ModifierDefinition> eligible, Set<String> originalTags) {
    return originalTags.isEmpty()
        ? eligible
        : eligible.stream().filter(d -> !Collections.disjoint(d.tags(), originalTags)).toList();
  }

  public record Plan(
      boolean available,
      String reason,
      StateBucket upgraded,
      List<ModifierDefinition> candidates,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Plan {
      candidates = List.copyOf(candidates);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
