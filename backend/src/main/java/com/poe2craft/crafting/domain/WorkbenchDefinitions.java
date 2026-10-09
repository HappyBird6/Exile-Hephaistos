package com.poe2craft.crafting.domain;

import com.poe2craft.item.ModifierDefinition;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;

/**
 * One immutable, explicitly initialized active bundle; enum identities and algorithms stay in code.
 */
public final class WorkbenchDefinitions {
  public record Currency(
      WorkbenchCurrency id,
      CraftingAction baseAction,
      int minimumModifierLevel,
      String fixedModifierId,
      List<String> essenceModifierIds,
      String essenceChoiceSource,
      List<String> replacementModifiers,
      String replacementEssenceSource,
      String replacementSource) {
    public Currency {
      essenceModifierIds = List.copyOf(essenceModifierIds);
      replacementModifiers = List.copyOf(replacementModifiers);
    }
  }

  public record Omen(
      WorkbenchOmen key,
      String id,
      WorkbenchCurrency trigger,
      AffixType affix,
      boolean supportsTieredCurrency) {}

  public record Document(
      int schemaVersion, String ruleVersion, List<Currency> currencies, List<Omen> omens) {
    public Document {
      currencies = List.copyOf(currencies);
      omens = List.copyOf(omens);
    }
  }

  private final Document document;
  private final Map<WorkbenchCurrency, Currency> currencies;
  private final Map<WorkbenchOmen, Omen> omens;
  private static volatile WorkbenchDefinitions active;

  public WorkbenchDefinitions(Document document) {
    this.document = Objects.requireNonNull(document);
    if (document.schemaVersion() != 1
        || !"workbench-static-definitions-v1".equals(document.ruleVersion()))
      throw new IllegalArgumentException("Unsupported Workbench definitions version");
    var currencyMap = new EnumMap<WorkbenchCurrency, Currency>(WorkbenchCurrency.class);
    for (var c : document.currencies()) {
      if (c.id() == null || currencyMap.putIfAbsent(c.id(), c) != null)
        throw new IllegalArgumentException("Missing or duplicate currency definition");
      if (c.minimumModifierLevel() < 0 || (c.baseAction() == null && c.minimumModifierLevel() != 0))
        throw new IllegalArgumentException("Invalid currency modifier level");
      String ordinary = c.id().name().replaceFirst("^(GREATER_|PERFECT_)", "");
      CraftingAction expected =
          Arrays.stream(CraftingAction.values())
              .filter(a -> a.name().equals(ordinary))
              .findFirst()
              .orElse(null);
      if (c.baseAction() != expected)
        throw new IllegalArgumentException(
            "Currency operation is outside its implemented boundary");
      ids(c.essenceModifierIds());
      ids(c.replacementModifiers());
      if (c.fixedModifierId() != null
          && !c.essenceModifierIds().equals(List.of(c.fixedModifierId())))
        throw new IllegalArgumentException("Fixed essence target differs");
      if ((!c.essenceModifierIds().isEmpty() && !c.id().name().contains("ESSENCE"))
          || (!c.essenceModifierIds().isEmpty() && !c.replacementModifiers().isEmpty())
          || (c.essenceModifierIds().size() > 1
              && (c.fixedModifierId() != null || c.essenceChoiceSource() == null))
          || (c.essenceModifierIds().size() == 1 && c.fixedModifierId() == null)
          || (c.essenceModifierIds().size() <= 1 && c.essenceChoiceSource() != null))
        throw new IllegalArgumentException("Invalid essence target mode");
      boolean replacementKind =
          c.id().isLiquid() || c.id().isAlloy() || c.id().name().contains("ESSENCE");
      boolean essenceReplacement =
          !c.id().isLiquid() && !c.id().isAlloy() && !c.replacementModifiers().isEmpty();
      if ((!c.replacementModifiers().isEmpty()
              && (!replacementKind || c.replacementSource() == null))
          || ((c.replacementEssenceSource() != null) != essenceReplacement)
          || (c.replacementEssenceSource() != null
              && (c.id().isLiquid() || c.id().isAlloy() || c.replacementModifiers().isEmpty())))
        throw new IllegalArgumentException(
            "Replacement definition is outside its implemented boundary");
      url(c.essenceChoiceSource());
      url(c.replacementEssenceSource());
      url(c.replacementSource());
    }
    if (!document.currencies().stream()
        .map(Currency::id)
        .toList()
        .equals(List.of(WorkbenchCurrency.values())))
      throw new IllegalArgumentException(
          "Currency definitions must cover enum identities in API order");
    var omenMap = new EnumMap<WorkbenchOmen, Omen>(WorkbenchOmen.class);
    var omenIds = new HashSet<String>();
    var tiered =
        Set.of(
            WorkbenchOmen.SINISTRAL_CORONATION,
            WorkbenchOmen.DEXTRAL_CORONATION,
            WorkbenchOmen.SINISTRAL_EXALTATION,
            WorkbenchOmen.DEXTRAL_EXALTATION,
            WorkbenchOmen.SINISTRAL_ERASURE,
            WorkbenchOmen.DEXTRAL_ERASURE,
            WorkbenchOmen.WHITTLING,
            WorkbenchOmen.HOMOGENISING_EXALTATION,
            WorkbenchOmen.HOMOGENISING_CORONATION);
    for (var o : document.omens()) {
      if (o.key() == null
          || o.id() == null
          || !o.id().matches("Omen_of_[A-Za-z_]+")
          || !omenIds.add(o.id())
          || omenMap.putIfAbsent(o.key(), o) != null
          || !currencyMap.containsKey(o.trigger()))
        throw new IllegalArgumentException("Missing, duplicate or unknown omen identity/trigger");
      AffixType expected =
          o.key().name().startsWith("SINISTRAL_")
              ? AffixType.PREFIX
              : o.key().name().startsWith("DEXTRAL_") ? AffixType.SUFFIX : null;
      if (o.affix() != expected || (o.supportsTieredCurrency() && !tiered.contains(o.key())))
        throw new IllegalArgumentException("Omen applicability is outside its reviewed boundary");
      if (!Set.of(
              WorkbenchCurrency.ALCHEMY,
              WorkbenchCurrency.REGAL,
              WorkbenchCurrency.ANNULMENT,
              WorkbenchCurrency.EXALTED,
              WorkbenchCurrency.CHAOS,
              WorkbenchCurrency.DIVINE,
              WorkbenchCurrency.ESSENCE_HYSTERIA)
          .contains(o.trigger()))
        throw new IllegalArgumentException("Unsupported omen trigger operation");
    }
    if (!document.omens().stream().map(Omen::key).toList().equals(List.of(WorkbenchOmen.values())))
      throw new IllegalArgumentException(
          "Omen definitions must cover enum identities in API order");
    currencies = Collections.unmodifiableMap(currencyMap);
    omens = Collections.unmodifiableMap(omenMap);
  }

  private static void ids(List<String> ids) {
    if (new HashSet<>(ids).size() != ids.size() || ids.stream().anyMatch(i -> i.isBlank()))
      throw new IllegalArgumentException("Blank or duplicate modifier target");
  }

  private static void url(String value) {
    if (value != null && !value.matches("https://poe2db\\.tw/us/[A-Za-z0-9_]+"))
      throw new IllegalArgumentException("Invalid reviewed currency source URL");
  }

  public Document document() {
    return document;
  }

  public Currency currency(WorkbenchCurrency id) {
    return currencies.get(Objects.requireNonNull(id));
  }

  public Omen omen(WorkbenchOmen id) {
    return omens.get(Objects.requireNonNull(id));
  }

  public void validateTargets(Map<String, List<ModifierDefinition>> known) {
    for (var c : document.currencies()) {
      if (!known.keySet().containsAll(c.essenceModifierIds())
          || !known.keySet().containsAll(c.replacementModifiers()))
        throw new IllegalArgumentException("Unknown currency modifier target: " + c.id());
      for (var id : c.essenceModifierIds()) {
        if (known.get(id).stream()
            .noneMatch(
                target ->
                    target.layer() == ModifierDefinition.Layer.EXPLICIT && target.weight() > 0))
          throw new IllegalArgumentException("Fixed essence requires an ordinary explicit target");
      }
      for (var id : c.replacementModifiers()) {
        if (known.get(id).stream()
            .noneMatch(target -> target.layer() == ModifierDefinition.Layer.EXPLICIT))
          throw new IllegalArgumentException("Replacement requires an explicit target");
      }
    }
  }

  public static synchronized WorkbenchDefinitions initialize(WorkbenchDefinitions definitions) {
    Objects.requireNonNull(definitions);
    if (active != null && !active.document.equals(definitions.document))
      throw new IllegalStateException("A different Workbench ruleset is already initialized");
    if (active == null) active = definitions;
    return active;
  }

  public static WorkbenchDefinitions active() {
    var definitions = active;
    if (definitions == null)
      throw new IllegalStateException("Workbench definitions have not been initialized");
    return definitions;
  }
}
