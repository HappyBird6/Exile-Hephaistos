package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Read-only candidate projection. Execution predicates remain in the crafting domain. */
@Service
public final class ModifierReferenceService {
  private final CraftingOperations crafting;

  public ModifierReferenceService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  public record Entry(
      String modifierId,
      String kind,
      boolean eligible,
      List<String> reasons,
      List<String> actions) {}

  public record Reference(List<Entry> entries) {}

  public Reference describe(ItemState input, boolean dynamic) {
    var catalog = crafting.abyssCatalog(input);
    var abyss = AbyssCraftingState.from(input, catalog);
    abyss.validate(catalog);
    var simulator = crafting.simulator(input);
    // A normal item has no explicit modifiers; show the first addition's pool.
    var state =
        input.rarity() == ItemState.Rarity.NORMAL
            ? new ItemState(
                input.snapshotId(),
                input.baseItemId(),
                input.itemLevel(),
                ItemState.Rarity.MAGIC,
                input.implicits(),
                input.explicits(),
                input.conditions(),
                input.augmentSockets(),
                input.catalystQuality(),
                input.desecrated(),
                input.equipmentQuality(),
                input.observations(),
                input.sanctification(),
                input.installedAugments())
            : input;
    var natural =
        (input.desecrated().stream().anyMatch(s -> s.modifierId() == null)
                ? new AbyssProbabilityComparison(catalog)
                    .pool(abyss, WorkbenchCurrency.EXALTED, null)
                : simulator.pool(state, WorkbenchCurrency.EXALTED, null))
            .stream().map(ModifierDefinition::id).collect(java.util.stream.Collectors.toSet());
    var hidden =
        abyss.slots().stream().filter(s -> s.desecrated() && s.modifier() == null).toList();
    var reveal = new HashSet<String>();
    for (var slot : hidden) reveal.addAll(abyss.eligibleRevealIds(slot.id(), catalog));
    var targets = new TreeMap<String, List<String>>();
    simulator
        .referenceEssenceTargets()
        .forEach(
            (action, ids) ->
                ids.forEach(id -> targets.computeIfAbsent(id, k -> new ArrayList<>()).add(action)));
    var entries = new ArrayList<Entry>();
    var resolver = new ModifierPoolResolver(catalog);
    for (var d : catalog.modifiers().values()) {
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT) continue;
      if ((d.weight() > 0 || d.tags().contains("augment_pool"))
          && !d.tags().contains("reveal_only")) {
        var reasons = dynamic ? exclusions(state, abyss, resolver, d, false) : List.<String>of();
        if (dynamic && !natural.contains(d.id()) && reasons.isEmpty())
          reasons = List.of("STATE_RESTRICTION");
        entries.add(
            new Entry(
                d.id(), "ordinary", !dynamic || natural.contains(d.id()), reasons, List.of()));
      }
      if (d.tags().contains("reveal_only") || d.tags().contains("unveiled_mod")) {
        var reasons =
            dynamic ? exclusions(state, abyss, resolver, d, !hidden.isEmpty()) : List.<String>of();
        if (dynamic && !hidden.isEmpty() && !reveal.contains(d.id()) && reasons.isEmpty())
          reasons = List.of("REVEAL_POOL");
        entries.add(new Entry(d.id(), "desecrated", reasons.isEmpty(), reasons, List.of()));
      }
      if (targets.containsKey(d.id())) {
        var reasons = dynamic ? exclusions(state, abyss, resolver, d, false) : List.<String>of();
        entries.add(
            new Entry(
                d.id(), "essence", reasons.isEmpty(), reasons, List.copyOf(targets.get(d.id()))));
      }
    }
    return new Reference(List.copyOf(entries));
  }

  private List<String> exclusions(
      ItemState state,
      AbyssCraftingState abyss,
      ModifierPoolResolver resolver,
      ModifierDefinition d,
      boolean replacingHidden) {
    int reservedPrefixes =
        (int)
            abyss.slots().stream()
                .filter(
                    slot ->
                        slot.modifier() == null
                            && slot.side() == ModifierDefinition.AffixType.PREFIX)
                .count();
    int reservedSuffixes =
        (int)
            abyss.slots().stream()
                .filter(
                    slot ->
                        slot.modifier() == null
                            && slot.side() == ModifierDefinition.AffixType.SUFFIX)
                .count();
    return resolver.exclusions(
        StateBucket.forModifierEligibility(state),
        d,
        reservedPrefixes,
        reservedSuffixes,
        replacingHidden);
  }
}
