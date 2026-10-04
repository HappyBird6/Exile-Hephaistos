package com.poe2craft.crafting.domain;

import com.poe2craft.item.BasicJewel;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ItemState;
import com.poe2craft.item.ItemStateValidator;
import com.poe2craft.item.ModifierDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure exhaustive one-action distribution under the published-weight model. */
public final class CraftingEngine {
  public static final String RULE_VERSION = "solar-base-six-v1";
  private final ItemCatalog catalog;
  private final ModifierPoolResolver resolver;

  public CraftingEngine(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    resolver = new ModifierPoolResolver(catalog);
  }

  public void validate(StateBucket state) {
    if (state == null) throw new IllegalArgumentException("State is required");
    // Reuse concrete validation for the real implicit; no invented explicit rolls.
    var shell =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            state.itemLevel(),
            state.rarity(),
            state.implicits(),
            List.of(),
            state.conditions());
    if (!new ItemStateValidator(catalog).validate(shell).isEmpty()) {
      throw new IllegalArgumentException("Unsupported item state or snapshot");
    }
    var families = new HashSet<String>();
    int p = 0, s = 0;
    for (String id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown modifier"));
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT
          || d.familyIds().stream().anyMatch(families::contains)) {
        throw new IllegalArgumentException("Invalid modifier layer or conflicting families");
      }
      families.addAll(d.familyIds());
      if (d.affixType() == ModifierDefinition.AffixType.PREFIX) p++;
      else s++;
    }
    int maxP =
        state.rarity() == ItemState.Rarity.NORMAL
            ? 0
            : state.rarity() == ItemState.Rarity.MAGIC
                ? catalog.base().magicPrefixes()
                : catalog.base().rarePrefixes();
    int maxS =
        state.rarity() == ItemState.Rarity.NORMAL
            ? 0
            : state.rarity() == ItemState.Rarity.MAGIC
                ? catalog.base().magicSuffixes()
                : catalog.base().rareSuffixes();
    if (BasicJewel.supported(state.baseItemId()) && state.rarity() == ItemState.Rarity.RARE) {
      maxP += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      maxS += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
      if (!BasicJewel.existingCapacity(p, s, maxP, maxS))
        throw new IllegalArgumentException("Affix capacity exceeded");
      return;
    }
    if (p > maxP || s > maxS) throw new IllegalArgumentException("Affix capacity exceeded");
  }

  public List<Availability> actions(StateBucket state) {
    validate(state);
    return java.util.Arrays.stream(CraftingAction.values())
        .map(a -> availability(state, a))
        .toList();
  }

  private Availability availability(StateBucket state, CraftingAction action) {
    boolean rarity =
        switch (action) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED, CHAOS -> state.rarity() == ItemState.Rarity.RARE;
          case ANNULMENT ->
              state.rarity() == ItemState.Rarity.MAGIC || state.rarity() == ItemState.Rarity.RARE;
        };
    if (!rarity)
      return new Availability(action, false, "This currency cannot be used on this rarity.");
    if (action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS) {
      if (state.modifierIds().isEmpty())
        return new Availability(action, false, "There is no explicit modifier to remove.");
      if (action == CraftingAction.CHAOS) {
        for (int i = 0; i < state.modifierIds().size(); i++) {
          if (resolver.resolve(remove(state, i)).totalWeight() == 0) {
            return new Availability(
                action, false, "A removal branch has no supported replacement modifier.");
          }
        }
      }
    } else if (resolver.resolve(upgrade(state, action)).totalWeight() == 0) {
      return new Availability(action, false, "No eligible modifier or free affix slot remains.");
    }
    return new Availability(action, true, "");
  }

  public TransitionResult transition(StateBucket state, CraftingAction action) {
    validate(state);
    Objects.requireNonNull(action, "action");
    var available = availability(state, action);
    if (!available.available())
      return new TransitionResult(state.id(), action, false, available.reason(), List.of());
    Map<StateBucket, Double> results = new LinkedHashMap<>();
    if (action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS) {
      double removalChance = 1.0 / state.modifierIds().size();
      for (int i = 0; i < state.modifierIds().size(); i++) {
        var removed = remove(state, i);
        if (action == CraftingAction.ANNULMENT) results.merge(removed, removalChance, Double::sum);
        else add(removed, removalChance, results);
      }
    } else add(upgrade(state, action), 1.0, results);
    var outcomes =
        results.entrySet().stream()
            .map(e -> new Outcome(e.getKey().id(), e.getKey(), e.getValue()))
            .sorted(
                Comparator.comparingDouble(Outcome::probability)
                    .reversed()
                    .thenComparing(Outcome::id))
            .toList();
    return new TransitionResult(state.id(), action, true, "", outcomes);
  }

  private void add(StateBucket state, double branchChance, Map<StateBucket, Double> results) {
    var pool = resolver.resolve(state);
    for (var candidate : pool.candidates()) {
      var ids = new ArrayList<>(state.modifierIds());
      ids.add(candidate.id());
      results.merge(
          state.with(state.rarity(), ids),
          branchChance * candidate.weight() / pool.totalWeight(),
          Double::sum);
    }
  }

  private StateBucket upgrade(StateBucket state, CraftingAction action) {
    return state.with(
        action == CraftingAction.TRANSMUTATION
            ? ItemState.Rarity.MAGIC
            : action == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity(),
        state.modifierIds());
  }

  private StateBucket remove(StateBucket state, int index) {
    var ids = new ArrayList<>(state.modifierIds());
    ids.remove(index);
    return state.with(state.rarity(), ids);
  }

  public record Availability(CraftingAction action, boolean available, String reason) {}

  public record Outcome(String id, StateBucket state, double probability) {}

  public record TransitionResult(
      String fromId,
      CraftingAction action,
      boolean available,
      String reason,
      List<Outcome> outcomes) {
    public TransitionResult {
      outcomes = List.copyOf(outcomes);
    }
  }
}
