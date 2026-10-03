package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Samples concrete elementary events; unchanged instances retain their actual values. */
public final class WorkbenchSimulator {
  public static final String RULE_VERSION = "solar-workbench-abyss-essence-v16";
  public static final String LEDGER_VERSION = "solar-uniform-assumptions-v10";
  private final ItemCatalog catalog;
  private final AdditionRules additionRules;
  private final Set<String> coupledModifierIds;
  private final Map<WorkbenchCurrency, List<String>> essenceTargetOverrides;
  private final Map<WorkbenchCurrency, List<String>> replacementTargetOverrides;

  public WorkbenchSimulator(ItemCatalog catalog, CraftingEngine engine) {
    this(catalog, engine, Set.of());
  }

  /** Explicit opt-in for source-proven multi-stat definitions; never prunes a catalog. */
  public WorkbenchSimulator(
      ItemCatalog catalog, CraftingEngine engine, Set<String> coupledModifierIds) {
    this(catalog, engine, coupledModifierIds, Map.of());
  }

  public WorkbenchSimulator(
      ItemCatalog catalog,
      CraftingEngine engine,
      Set<String> coupledModifierIds,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides) {
    this(catalog, engine, coupledModifierIds, essenceTargetOverrides, Map.of());
  }

  public WorkbenchSimulator(
      ItemCatalog catalog,
      CraftingEngine engine,
      Set<String> coupledModifierIds,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides,
      Map<WorkbenchCurrency, List<String>> replacementTargetOverrides) {
    this.catalog = Objects.requireNonNull(catalog);
    var replacements = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    replacementTargetOverrides.forEach(
        (action, ids) -> {
          if (action.replacementModifiers().isEmpty()
              || ids.isEmpty()
              || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException(
                "Replacement override requires a complete distinct target set");
          for (var id : ids) {
            var definition =
                catalog
                    .find(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown replacement target"));
            if (definition.layer() != ModifierDefinition.Layer.EXPLICIT)
              throw new IllegalArgumentException("Replacement target must be explicit");
          }
          replacements.put(action, List.copyOf(ids));
        });
    this.replacementTargetOverrides = Map.copyOf(replacements);
    var reviewed = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    essenceTargetOverrides.forEach(
        (action, ids) -> {
          if (action.essenceModifierIds().isEmpty()
              || !action.replacementModifiers().isEmpty()
              || ids.size() != 1)
            throw new IllegalArgumentException(
                "Only source-proven fixed basic essence overrides are supported");
          var id = ids.getFirst();
          var definition =
              catalog
                  .find(id)
                  .orElseThrow(() -> new IllegalArgumentException("Unknown essence target"));
          if (definition.layer() != ModifierDefinition.Layer.EXPLICIT || definition.weight() <= 0)
            throw new IllegalArgumentException(
                "Basic essence target requires a verified ordinary explicit");
          reviewed.put(action, List.copyOf(ids));
        });
    this.essenceTargetOverrides = Map.copyOf(reviewed);
    this.coupledModifierIds = Set.copyOf(coupledModifierIds);
    for (var id : this.coupledModifierIds) {
      var definition =
          catalog
              .find(id)
              .orElseThrow(() -> new IllegalArgumentException("Unknown coupled modifier"));
      if (definition.stats().size() < 2)
        throw new IllegalArgumentException("Coupled opt-in requires multiple source stats");
    }
    Objects.requireNonNull(engine);
    additionRules = new AdditionRules(catalog);
  }

  private List<String> essenceTargets(WorkbenchCurrency action) {
    return essenceTargetOverrides.getOrDefault(action, action.essenceModifierIds());
  }

  private List<String> replacementTargets(WorkbenchCurrency action) {
    return replacementTargetOverrides.getOrDefault(action, action.replacementModifiers());
  }

  public String ruleVersion() {
    if (catalog.base().id().equals(SceptreEssenceTargets.BASE_ID))
      return "sceptre-workbench-essence-v1";
    if (catalog.base().id().equals(BodyEssenceTargets.BASE_ID))
      return "body-workbench-perfect-essence-v1";
    if (catalog.base().id().equals(WandEssenceTargets.BASE_ID)) return "wand-workbench-essence-v1";
    if (catalog.base().id().equals(BowEssenceTargets.BASE_ID))
      return "bow-workbench-perfect-essence-v2";
    return coupledModifierIds.isEmpty() ? RULE_VERSION : "stocky-workbench-artificer-v26";
  }

  public String ledgerVersion() {
    if (catalog.base().id().equals(SceptreEssenceTargets.BASE_ID))
      return "sceptre-unverified-numeric-assumptions-v1";
    if (catalog.base().id().equals(BodyEssenceTargets.BASE_ID))
      return "body-unverified-numeric-assumptions-v1";
    if (catalog.base().id().equals(WandEssenceTargets.BASE_ID))
      return "wand-unverified-numeric-assumptions-v1";
    if (catalog.base().id().equals(BowEssenceTargets.BASE_ID))
      return "bow-unverified-numeric-assumptions-v1";
    return coupledModifierIds.isEmpty()
        ? LEDGER_VERSION
        : "stocky-unverified-numeric-assumptions-v11";
  }

  public Result apply(ItemState state, CraftingAction action, RandomGenerator random) {
    if (action == null) throw new IllegalArgumentException("Action required");
    return apply(state, WorkbenchCurrency.valueOf(action.name()), Set.of(), random);
  }

  public List<Availability> actions(ItemState state, Set<String> activeOmens) {
    validate(state);
    var omens = parseOmens(activeOmens);
    return Arrays.stream(WorkbenchCurrency.values())
        .filter(
            a ->
                (a != WorkbenchCurrency.ARTIFICER
                        || state.baseItemId().equals(AugmentSocketRules.STOCKY_BASE_ID))
                    && java.util.stream.Stream.concat(
                            essenceTargets(a).stream(), replacementTargets(a).stream())
                        .allMatch(id -> catalog.find(id).isPresent()))
        .map(a -> availability(state, a, omens))
        .toList();
  }

  private void validate(ItemState state) {
    if (state == null
        || !AugmentSocketRules.supportedState(state)
        || !new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported concrete item state");
  }

  private List<WorkbenchOmen> parseOmens(Set<String> ids) {
    if (ids == null) return List.of();
    if (ids.size() > WorkbenchOmen.values().length)
      throw new IllegalArgumentException("Too many active omens");
    return ids.stream().sorted().map(WorkbenchOmen::fromId).toList();
  }

  private List<WorkbenchOmen> matching(WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    return omens.stream()
        .filter(
            o ->
                o.trigger() == action
                    || (o.trigger() == WorkbenchCurrency.ESSENCE_HYSTERIA
                        && !action.replacementEssenceModifiers().isEmpty())
                    || (action.baseAction() != null
                        && o.trigger().baseAction() == action.baseAction()))
        .toList();
  }

  private Availability availability(
      ItemState state, WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    if (action == WorkbenchCurrency.ARTIFICER) {
      var reason = AugmentSocketRules.refusal(state);
      return reason.isEmpty() ? new Availability(action, true, "") : blocked(action, reason);
    }
    if (java.util.stream.Stream.concat(
            essenceTargets(action).stream(), replacementTargets(action).stream())
        .anyMatch(id -> catalog.find(id).isEmpty()))
      return blocked(action, "This material's results are not verified for this base.");
    var matches = matching(action, omens);
    if (matches.size() == 1 && matches.getFirst() == WorkbenchOmen.GREATER_EXALTATION) {
      if (action != WorkbenchCurrency.EXALTED)
        return blocked(action, "Greater Exaltation with Greater/Perfect currency is not verified.");
      if (state.rarity() != ItemState.Rarity.RARE)
        return blocked(action, "Exalted Orb requires a Rare item.");
      if (state.explicits().size() > 4)
        return blocked(
            action,
            "Greater Exaltation is supported only with at least two free explicit slots; the one-slot interaction needs verification.");
      var first = pool(state, action, null);
      if (first.isEmpty()) return blocked(action, "No verified first addition pool.");
      for (var candidate : first) {
        var next = new ArrayList<>(state.explicits());
        var values = new HashMap<String, Long>();
        for (var stat : candidate.stats()) values.put(stat.id(), stat.min());
        next.add(new ModifierInstance(candidate.id(), values));
        if (pool(copy(state, state.rarity(), state.implicits(), next), action, null).isEmpty())
          return blocked(action, "A first addition branch has no verified second addition pool.");
      }
      return new Availability(action, true, "");
    }
    if (AdditionRules.isAddition(action)) {
      var plan =
          additionRules.plan(
              StateBucket.from(state),
              action,
              omens.stream().map(WorkbenchOmen::id).collect(java.util.stream.Collectors.toSet()));
      return new Availability(action, plan.available(), plan.reason());
    }
    if (matches.size() > 1)
      return blocked(
          action,
          "Multiple omens for the same operation need combination verification. Deactivate all but one.");
    if (!matches.isEmpty() && action.minimumModifierLevel() > 0)
      return blocked(
          action,
          "Omen interaction with Greater/Perfect currency is not verified. Deactivate the omen or use ordinary currency.");
    if (state.itemLevel() < action.minimumModifierLevel())
      return blocked(action, "Item level is below the currency's minimum modifier level.");
    var omen = matches.isEmpty() ? null : matches.getFirst();
    if (!replacementTargets(action).isEmpty()) {
      if (state.rarity() != ItemState.Rarity.RARE)
        return blocked(
            action, "This material requires a Rare item with a removable explicit modifier.");
      var targets =
          replacementTargets(action).stream().map(id -> catalog.find(id).orElseThrow()).toList();
      if (targets.stream().anyMatch(target -> state.itemLevel() < target.requiredItemLevel()))
        return blocked(action, "Essence below the catalog modifier item level is unsupported.");
      var candidates = removalCandidates(state, omen);
      if (candidates.isEmpty()) return blocked(action, "No non-Fractured explicit can be removed.");
      for (var removed : candidates) {
        var rest = new ArrayList<>(state.explicits());
        rest.remove(removed);
        var afterRemoval = copy(state, state.rarity(), state.implicits(), rest);
        if (targets.stream().anyMatch(target -> !canAddFixed(afterRemoval, target)))
          return blocked(
              action,
              "An essence removal branch conflicts with a guaranteed modifier or available slots; this interaction is unsupported.");
      }
      return new Availability(action, true, "");
    }
    if (!essenceTargets(action).isEmpty()) {
      if (state.rarity() != ItemState.Rarity.MAGIC)
        return blocked(
            action, "This essence upgrades a Magic item to Rare with one guaranteed modifier.");
      var targets =
          essenceTargets(action).stream().map(id -> catalog.find(id).orElseThrow()).toList();
      if (targets.stream().anyMatch(target -> state.itemLevel() < target.requiredItemLevel()))
        return blocked(
            action,
            "Essence results below the catalog modifier's item level need verification; this low-level scope is unsupported.");
      var rare = copy(state, ItemState.Rarity.RARE, state.implicits(), state.explicits());
      if (!pool(rare, action, null).containsAll(targets))
        return blocked(
            action,
            "An essence result conflicts with existing families or available affix slots; the overlap interaction is unsupported.");
      return new Availability(action, true, "");
    }
    if (action == WorkbenchCurrency.FRACTURING) {
      if (state.rarity() != ItemState.Rarity.RARE || state.explicits().size() < 4)
        return blocked(
            action, "Fracturing requires a Rare item with at least four explicit modifiers.");
      if (state.explicits().stream().anyMatch(ModifierInstance::fractured))
        return blocked(action, "Cannot fracture an already Fractured item.");
      return new Availability(action, true, "");
    }
    if (omen == WorkbenchOmen.WHITTLING
        && state.explicits().stream().anyMatch(ModifierInstance::fractured))
      return blocked(
          action,
          "Whittling priority with Fractured modifiers needs verification. Deactivate the omen.");
    if (action == WorkbenchCurrency.ALCHEMY) {
      if (state.rarity() != ItemState.Rarity.NORMAL && state.rarity() != ItemState.Rarity.MAGIC)
        return blocked(action, "Alchemy requires a Normal or Magic item.");
      return new Availability(action, true, "");
    }
    if (action == WorkbenchCurrency.DIVINE) {
      var targets = new ArrayList<>(state.implicits());
      if (omen != WorkbenchOmen.BLESSED)
        targets.addAll(state.explicits().stream().filter(m -> !m.fractured()).toList());
      if (targets.stream()
          .noneMatch(
              m ->
                  catalog.find(m.modifierId()).orElseThrow().stats().stream()
                      .anyMatch(s -> s.max() > s.min())))
        return blocked(action, "No supported numeric range can be rerolled.");
      return new Availability(action, true, "");
    }
    var kind = action.baseAction();
    boolean rarity =
        switch (kind) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED, CHAOS -> state.rarity() == ItemState.Rarity.RARE;
          case ANNULMENT ->
              state.rarity() == ItemState.Rarity.MAGIC || state.rarity() == ItemState.Rarity.RARE;
        };
    if (!rarity) return blocked(action, "This currency cannot be used on this rarity.");
    if (kind == CraftingAction.ANNULMENT || kind == CraftingAction.CHAOS) {
      var removals = removalCandidates(state, omen);
      if (removals.isEmpty())
        return blocked(action, "No eligible explicit modifier can be removed with this omen.");
      if (kind == CraftingAction.CHAOS)
        for (var removed : removals) {
          var rest = new ArrayList<>(state.explicits());
          rest.remove(removed);
          if (pool(copy(state, state.rarity(), state.implicits(), rest), action, null).isEmpty())
            return blocked(action, "A removal branch has no verified replacement pool.");
        }
    } else if (pool(
            copy(state, upgrade(state, action), state.implicits(), state.explicits()), action, omen)
        .isEmpty())
      return blocked(
          action, "No eligible modifier or free affix slot remains for this currency and omen.");
    return new Availability(action, true, "");
  }

  private Availability blocked(WorkbenchCurrency action, String reason) {
    return new Availability(action, false, reason);
  }

  private boolean canAddFixed(ItemState state, ModifierDefinition target) {
    var existing =
        state.explicits().stream().map(m -> catalog.find(m.modifierId()).orElseThrow()).toList();
    int capacity =
        target.affixType() == ModifierDefinition.AffixType.PREFIX
            ? catalog.base().rarePrefixes()
            : catalog.base().rareSuffixes();
    return state.itemLevel() >= target.requiredItemLevel()
        && existing.stream().filter(d -> d.affixType() == target.affixType()).count() < capacity
        && existing.stream()
            .noneMatch(d -> !Collections.disjoint(d.familyIds(), target.familyIds()));
  }

  public List<ModifierDefinition> pool(
      ItemState state, WorkbenchCurrency currency, WorkbenchOmen omen) {
    return additionRules.pool(StateBucket.from(state), currency, omen);
  }

  private List<ModifierInstance> removalCandidates(ItemState state, WorkbenchOmen omen) {
    var eligible = state.explicits().stream().filter(m -> !m.fractured()).toList();
    if (omen != null && omen.affix() != null)
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().affixType() == omen.affix())
              .toList();
    if (omen == WorkbenchOmen.WHITTLING && !eligible.isEmpty()) {
      int lowest =
          eligible.stream()
              .mapToInt(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel())
              .min()
              .orElseThrow();
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel() == lowest)
              .toList();
    }
    return eligible;
  }

  private ItemState.Rarity upgrade(ItemState state, WorkbenchCurrency currency) {
    return currency.baseAction() == CraftingAction.TRANSMUTATION
        ? ItemState.Rarity.MAGIC
        : currency.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity();
  }

  public Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> activeOmens, RandomGenerator random) {
    validate(state);
    if (action == null || random == null)
      throw new IllegalArgumentException("Action and random required");
    var omens = parseOmens(activeOmens);
    var available = availability(state, action, omens);
    var allIds = omens.stream().map(WorkbenchOmen::id).toList();
    if (!available.available())
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          state,
          action,
          false,
          available.reason(),
          List.of(),
          List.of(),
          List.of(),
          allIds,
          QualityLimitRules.describe(state, catalog));
    var matched = matching(action, omens);
    if (action == WorkbenchCurrency.ARTIFICER) {
      var next =
          new ItemState(
              state.snapshotId(),
              state.baseItemId(),
              state.itemLevel(),
              state.rarity(),
              state.implicits(),
              state.explicits(),
              state.conditions(),
              1);
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "",
          List.of(),
          List.of(),
          List.of(),
          allIds,
          QualityLimitRules.describe(next, catalog));
    }
    var omen = matched.isEmpty() ? null : matched.getFirst();
    var implicits = new ArrayList<>(state.implicits());
    var explicits = new ArrayList<>(state.explicits());
    var events = new ArrayList<Event>();
    var assumptions = new ArrayList<Assumption>();
    var rarity =
        action == WorkbenchCurrency.ALCHEMY || !essenceTargets(action).isEmpty()
            ? ItemState.Rarity.RARE
            : upgrade(state, action);
    if (!replacementTargets(action).isEmpty()) {
      var candidates = removalCandidates(state, omen);
      var removed = candidates.get(random.nextInt(candidates.size()));
      explicits.remove(removed);
      events.add(new Event("REMOVE", removed.modifierId(), Map.of(), 1.0 / candidates.size()));
      assumptions.add(
          new Assumption(
              "uniform-removal-v1",
              "eligible explicit modifier instance",
              candidates.size(),
              candidates.stream().map(ModifierInstance::modifierId).toList(),
              null,
              null,
              omen == null ? action.replacementSource() : "https://poe2db.tw/us/" + omen.id(),
              "Uniform among non-Fractured explicit instances; every removal branch must accept every sourced modifier outcome. No published removal weights."));
      var targets = replacementTargets(action);
      var definition =
          catalog
              .find(targets.get(targets.size() == 1 ? 0 : random.nextInt(targets.size())))
              .orElseThrow();
      if (targets.size() > 1)
        assumptions.add(
            new Assumption(
                "uniform-essence-choice-v1",
                "fixed essence modifier outcome",
                targets.size(),
                targets,
                null,
                null,
                action.replacementSource(),
                "Uniform among the sourced modifier outcomes; no published essence choice weights. Zero ordinary spawn weight is not an essence selection weight."));
      var rolled = roll(definition, random, assumptions);
      explicits.add(rolled);
      events.add(new Event("ADD", definition.id(), rolled.values(), 1.0 / targets.size()));
    } else if (!essenceTargets(action).isEmpty()) {
      var candidates = essenceTargets(action);
      var definition =
          catalog
              .find(candidates.get(candidates.size() == 1 ? 0 : random.nextInt(candidates.size())))
              .orElseThrow();
      if (candidates.size() > 1)
        assumptions.add(
            new Assumption(
                "uniform-essence-choice-v1",
                "fixed essence modifier outcome",
                candidates.size(),
                candidates,
                null,
                null,
                action.essenceChoiceSource(),
                "Uniform among the sourced modifier outcomes; no published essence choice weights. Ordinary affix pool weights are not asserted to be essence choice weights."));
      var rolled = roll(definition, random, assumptions);
      explicits.add(rolled);
      events.add(new Event("ADD", definition.id(), rolled.values(), 1.0 / candidates.size()));
    } else if (action == WorkbenchCurrency.FRACTURING) {
      int index = random.nextInt(explicits.size());
      var chosen = explicits.get(index);
      explicits.set(index, new ModifierInstance(chosen.modifierId(), chosen.values(), true));
      events.add(
          new Event("FRACTURE", chosen.modifierId(), chosen.values(), 1.0 / explicits.size()));
      assumptions.add(
          new Assumption(
              "uniform-fracture-v1",
              "eligible explicit modifier instance",
              explicits.size(),
              explicits.stream().map(ModifierInstance::modifierId).toList(),
              null,
              null,
              "https://poe2db.tw/us/Fractured_Modifiers",
              "Uniform among supported explicit instances on this supported Rare base, including verified essence results; no published fracture selection weights."));
    } else if (action == WorkbenchCurrency.ALCHEMY) {
      for (var old : explicits) events.add(new Event("REMOVE", old.modifierId(), Map.of(), 1));
      explicits.clear();
      for (int i = 0; i < 4; i++) {
        var candidates = pool(copy(state, rarity, implicits, explicits), action, null);
        if (candidates.isEmpty())
          throw new IllegalArgumentException("Alchemy needs a complete four-modifier pool");
        long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
        long draw = random.nextLong(total);
        var chosen = candidates.getLast();
        for (var candidate : candidates) {
          draw -= candidate.weight();
          if (draw < 0) {
            chosen = candidate;
            break;
          }
        }
        var rolled = roll(chosen, random, assumptions);
        explicits.add(rolled);
        events.add(
            new Event("ADD", chosen.id(), rolled.values(), (double) chosen.weight() / total));
      }
    } else if (action == WorkbenchCurrency.DIVINE) {
      for (int i = 0; i < implicits.size(); i++) {
        var current = implicits.get(i);
        var d = catalog.find(current.modifierId()).orElseThrow();
        if (d.stats().getFirst().max() > d.stats().getFirst().min()) {
          var roll = roll(d, random, assumptions);
          implicits.set(i, roll);
          events.add(new Event("REROLL_IMPLICIT", d.id(), roll.values(), 1));
        }
      }
      if (omen != WorkbenchOmen.BLESSED)
        for (int i = 0; i < explicits.size(); i++) {
          var current = explicits.get(i);
          if (current.fractured()) continue;
          var d = catalog.find(current.modifierId()).orElseThrow();
          if (d.stats().stream().anyMatch(s -> s.max() > s.min())) {
            var roll = roll(d, random, assumptions);
            explicits.set(i, roll);
            events.add(new Event("REROLL_EXPLICIT", d.id(), roll.values(), 1));
          }
        }
    } else {
      if (action.baseAction() == CraftingAction.ANNULMENT
          || action.baseAction() == CraftingAction.CHAOS) {
        var candidates = removalCandidates(state, omen);
        var removed = candidates.get(random.nextInt(candidates.size()));
        explicits.remove(removed);
        events.add(new Event("REMOVE", removed.modifierId(), Map.of(), 1.0 / candidates.size()));
        assumptions.add(
            new Assumption(
                "uniform-removal-v1",
                "eligible explicit modifier instance",
                candidates.size(),
                candidates.stream().map(ModifierInstance::modifierId).toList(),
                null,
                null,
                omen == null
                    ? "https://poe2db.tw/us/"
                        + (action.baseAction() == CraftingAction.CHAOS
                            ? "Chaos_Orb"
                            : "Orb_of_Annulment")
                    : "https://poe2db.tw/us/" + omen.id(),
                "Uniform among eligible removal instances, after affix or lowest modifier-level restriction; no published removal weights."));
      }
      if (action.baseAction() != CraftingAction.ANNULMENT) {
        int additions = omen == WorkbenchOmen.GREATER_EXALTATION ? 2 : 1;
        for (int i = 0; i < additions; i++) {
          var intermediate = copy(state, rarity, implicits, explicits);
          var candidates =
              pool(intermediate, action, action.baseAction() == CraftingAction.CHAOS ? null : omen);
          long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
          long draw = random.nextLong(total);
          var chosen = candidates.getLast();
          for (var candidate : candidates) {
            draw -= candidate.weight();
            if (draw < 0) {
              chosen = candidate;
              break;
            }
          }
          var rolled = roll(chosen, random, assumptions);
          explicits.add(rolled);
          events.add(
              new Event("ADD", chosen.id(), rolled.values(), (double) chosen.weight() / total));
        }
      }
    }
    var result = copy(state, rarity, implicits, explicits);
    validate(result);
    var consumed = matched.stream().map(WorkbenchOmen::id).toList();
    var remaining = allIds.stream().filter(id -> !consumed.contains(id)).toList();
    return new Result(
        ruleVersion(),
        ledgerVersion(),
        state.snapshotId(),
        result,
        action,
        true,
        "",
        events,
        assumptions,
        consumed,
        remaining,
        QualityLimitRules.describe(result, catalog));
  }

  private ModifierInstance roll(
      ModifierDefinition definition, RandomGenerator random, List<Assumption> assumptions) {
    if (coupledModifierIds.contains(definition.id()))
      return CoupledStatRollModel.roll(definition, random, assumptions);
    if (definition.stats().size() != 1)
      throw new IllegalArgumentException("Joint stat roll domain needs verification");
    var range = definition.stats().getFirst();
    long n = Math.addExact(Math.subtractExact(range.max(), range.min()), 1);
    long value = Math.addExact(range.min(), random.nextLong(n));
    if (n > 1)
      assumptions.add(
          new Assumption(
              coupledModifierIds.isEmpty()
                  ? "uniform-integer-roll-v1"
                  : "assumed-source-integer-roll-v1",
              range.id(),
              n,
              List.of(),
              range.min(),
              range.max(),
              definition.sourceUrl(),
              coupledModifierIds.isEmpty()
                  ? "Each integer in this single-stat source range is a modeled candidate; no published roll weights."
                  : "UNVERIFIED numeric model: equally sampled source-unit integers between verified bounds. Interior increments, display conversion and game distribution remain unverified; this is not an established game outcome domain."));
    return new ModifierInstance(definition.id(), Map.of(range.id(), value));
  }

  private static ItemState copy(
      ItemState s,
      ItemState.Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits) {
    return new ItemState(
        s.snapshotId(),
        s.baseItemId(),
        s.itemLevel(),
        rarity,
        implicits,
        explicits,
        s.conditions(),
        s.augmentSockets());
  }

  public record Availability(WorkbenchCurrency action, boolean available, String reason) {}

  public record Event(
      String kind, String modifierId, Map<String, Long> values, double selectionProbability) {
    public Event {
      values = Map.copyOf(values);
    }
  }

  public record Assumption(
      String id,
      String candidateUnit,
      long n,
      List<String> candidates,
      Long min,
      Long max,
      String sourceUrl,
      String reason,
      Integer ratioTick) {
    public Assumption(
        String id,
        String candidateUnit,
        long n,
        List<String> candidates,
        Long min,
        Long max,
        String sourceUrl,
        String reason) {
      this(id, candidateUnit, n, candidates, min, max, sourceUrl, reason, null);
    }

    public Assumption {
      candidates = List.copyOf(candidates);
    }
  }

  public record Result(
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      ItemState state,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<Event> events,
      List<Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens,
      QualityLimitRules.Limit qualityLimit) {
    public Result {
      events = List.copyOf(events);
      assumptions = List.copyOf(assumptions);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
