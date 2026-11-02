package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;

/** Manual observed Abyss slots. No distribution over reveal offers is assumed. */
public record AbyssCraftingState(ItemState item, List<Slot> slots, Offer offer) {
  public AbyssCraftingState {
    Objects.requireNonNull(item);
    slots = List.copyOf(slots);
    if (!item.explicits().isEmpty())
      throw new IllegalArgumentException("Explicit instances belong in slots, not twice in item");
    if (slots.size() > 6 || slots.stream().map(Slot::id).distinct().count() != slots.size())
      throw new IllegalArgumentException("Invalid explicit slots");
    if (offer != null
        && slots.stream().noneMatch(s -> s.id().equals(offer.slotId()) && s.desecrated()))
      throw new IllegalArgumentException("Offer must target a desecrated slot");
  }

  public record Slot(
      String id,
      AffixType side,
      ModifierInstance modifier,
      boolean desecrated,
      Integer minimumModifierLevel) {
    public Slot(String id, AffixType side, ModifierInstance modifier, boolean desecrated) {
      this(id, side, modifier, desecrated, null);
    }

    public Slot {
      if (minimumModifierLevel != null && (!desecrated || minimumModifierLevel != 40))
        throw new IllegalArgumentException("Unsupported desecration level constraint");
      if (id == null || id.isBlank() || id.length() > 160 || side == null || side == AffixType.NONE)
        throw new IllegalArgumentException("Slot identity and affix side are required");
      if (modifier == null && !desecrated)
        throw new IllegalArgumentException("Only desecrated slots can be unrevealed");
      if (desecrated && modifier != null && modifier.fractured())
        throw new IllegalArgumentException("Fractured desecrated interaction is unsupported");
    }
  }

  /** Exactly the three candidates observed by the user; no probability or omen cost. */
  public record Offer(String slotId, List<ModifierInstance> candidates) {
    public Offer {
      if (slotId == null || slotId.isBlank()) throw new IllegalArgumentException("Target required");
      candidates = List.copyOf(candidates);
      if (candidates.size() != 3)
        throw new IllegalArgumentException("Three observed candidates required");
    }
  }

  public static AbyssCraftingState from(ItemState state, ItemCatalog catalog) {
    var slots = new ArrayList<Slot>();
    for (int i = 0; i < state.explicits().size(); i++) {
      var m = state.explicits().get(i);
      var d =
          catalog
              .find(m.modifierId())
              .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
      var marked =
          state.desecrated().stream()
              .filter(s -> m.modifierId().equals(s.modifierId()))
              .findFirst();
      slots.add(
          new Slot(
              marked.map(ItemState.DesecratedSlot::id).orElse("explicit-" + i),
              d.affixType(),
              m,
              marked.isPresent(),
              marked.map(ItemState.DesecratedSlot::minimumModifierLevel).orElse(null)));
    }
    state.desecrated().stream()
        .filter(s -> s.modifierId() == null)
        .forEach(s -> slots.add(new Slot(s.id(), s.side(), null, true, s.minimumModifierLevel())));
    var result = new AbyssCraftingState(withExplicits(state, List.of()), slots, null);
    result.validate(catalog);
    return result;
  }

  /** Evaluation projection only: unrevealed slots contribute no invented stat values. */
  public ItemState knownItem() {
    return withExplicits(
        item, slots.stream().map(Slot::modifier).filter(Objects::nonNull).toList());
  }

  /** Lossless common state, unlike knownItem(), which is only a stat evaluation projection. */
  public ItemState storedItem() {
    return knownItem()
        .withDesecrated(
            slots.stream()
                .filter(Slot::desecrated)
                .map(
                    s ->
                        new ItemState.DesecratedSlot(
                            s.id(),
                            s.side(),
                            s.modifier() == null ? null : s.modifier().modifierId(),
                            s.minimumModifierLevel()))
                .toList());
  }

  public void validate(ItemCatalog catalog) {
    if (item.observations() != null && item.observations().needsAugmentResolution())
      throw new IllegalArgumentException(
          "Installed Augment names and counts must be resolved before crafting; observations are preserved.");
    if (!new ItemStateValidator(catalog).validate(knownItem()).isEmpty())
      throw new IllegalArgumentException("Invalid known item state");
    if (slots.stream().anyMatch(Slot::desecrated)
        && (item.rarity() != ItemState.Rarity.RARE
            || BasicJewel.supportedCrafting(item.baseItemId())))
      throw new IllegalArgumentException("Desecration requires a supported Rare equipment base");
    var capacity = new ItemStateValidator(catalog).slots(knownItem());
    if (count(AffixType.PREFIX) > capacity.maxPrefixes()
        || count(AffixType.SUFFIX) > capacity.maxSuffixes())
      throw new IllegalArgumentException("Explicit affix capacity exceeded");
    for (var slot : slots) {
      if (slot.minimumModifierLevel() != null
          && slot.modifier() != null
          && catalog.find(slot.modifier().modifierId()).orElseThrow().requiredItemLevel()
              < slot.minimumModifierLevel())
        throw new IllegalArgumentException("Candidate violates currency level limit");
      if (slot.modifier() != null
          && catalog
                  .find(slot.modifier().modifierId())
                  .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"))
                  .affixType()
              != slot.side())
        throw new IllegalArgumentException("Slot and modifier affix sides differ");
    }
    if (offer != null)
      for (var candidate : offer.candidates()) selected(offer.slotId(), candidate, catalog);
  }

  public long count(AffixType side) {
    return slots.stream().filter(s -> s.side() == side).count();
  }

  public AbyssCraftingState addUnrevealed(String id, AffixType side, ItemCatalog catalog) {
    validate(catalog);
    var next = new ArrayList<>(slots);
    next.add(new Slot(id, side, null, true));
    var result = new AbyssCraftingState(item, next, offer);
    result.validate(catalog);
    return result;
  }

  public AbyssCraftingState observe(
      String slotId, List<ModifierInstance> candidates, ItemCatalog catalog) {
    validate(catalog);
    var result = new AbyssCraftingState(item, slots, new Offer(slotId, candidates));
    result.validate(catalog);
    return result;
  }

  public AbyssCraftingState choose(int candidateIndex, ItemCatalog catalog) {
    validate(catalog);
    if (offer == null || candidateIndex < 0 || candidateIndex >= 3)
      throw new IllegalArgumentException("Observed candidate required");
    return selected(offer.slotId(), offer.candidates().get(candidateIndex), catalog);
  }

  private AbyssCraftingState selected(String id, ModifierInstance candidate, ItemCatalog catalog) {
    var target =
        slots.stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (!target.desecrated() || candidate.fractured())
      throw new IllegalArgumentException("Only the desecrated slot can be reworked");
    var definition =
        catalog
            .find(candidate.modifierId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (definition.requiredItemLevel() > item.itemLevel()
        || (target.minimumModifierLevel() != null
            && definition.requiredItemLevel() < target.minimumModifierLevel()))
      throw new IllegalArgumentException("Candidate violates item or currency level limits");
    var next =
        slots.stream()
            .map(
                s ->
                    s.id().equals(id)
                        ? new Slot(id, s.side(), candidate, true, s.minimumModifierLevel())
                        : s)
            .toList();
    var result = new AbyssCraftingState(item, next, null);
    result.validate(catalog);
    return result;
  }

  public AbyssCraftingState append(String slotId, ModifierInstance modifier, ItemCatalog catalog) {
    var definition =
        catalog
            .find(modifier.modifierId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (definition.requiredItemLevel() > item.itemLevel() || modifier.fractured())
      throw new IllegalArgumentException("Unsupported generated modifier");
    var next = new ArrayList<>(slots);
    next.add(new Slot(slotId, definition.affixType(), modifier, false));
    var result = new AbyssCraftingState(item, next, offer);
    result.validate(catalog);
    return result;
  }

  /** Side restrictions apply to the dummy exactly as to a known explicit. */
  public List<Slot> removalCandidates(AffixType side) {
    return slots.stream()
        .filter(s -> s.modifier() == null || !s.modifier().fractured())
        .filter(s -> side == null || s.side() == side)
        .toList();
  }

  public AbyssCraftingState remove(String slotId, AffixType side, ItemCatalog catalog) {
    validate(catalog);
    if (removalCandidates(side).stream().noneMatch(s -> s.id().equals(slotId)))
      throw new IllegalArgumentException("Ineligible removal slot");
    return new AbyssCraftingState(
        item,
        slots.stream().filter(s -> !s.id().equals(slotId)).toList(),
        offer != null && offer.slotId().equals(slotId) ? null : offer);
  }

  private static ItemState withExplicits(ItemState s, List<ModifierInstance> modifiers) {
    return new ItemState(
            s.snapshotId(),
            s.baseItemId(),
            s.itemLevel(),
            s.rarity(),
            s.implicits(),
            modifiers,
            s.conditions(),
            s.augmentSockets(),
            s.catalystQuality())
        .withEquipmentQuality(s.equipmentQuality())
        .withObservations(s.observations());
  }
}
