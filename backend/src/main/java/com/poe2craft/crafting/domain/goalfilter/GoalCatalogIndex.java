package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalCatalog.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ModifierDefinition;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Catalog membership proves base applicability; level eligibility only applies to new generation.
 */
public final class GoalCatalogIndex {
  public static final String RULE_VERSION =
      "goal-catalog-source-units-v1/solar-numeric-addition-v1/uniform-integer-roll-v1";
  private final Map<String, ItemCatalog> bases;
  private final String version;
  private static final Map<String, String> PERCENT =
      Map.ofEntries(
          Map.entry("base_cold_damage_resistance_%", "Cold Resistance"),
          Map.entry("base_fire_damage_resistance_%", "Fire Resistance"),
          Map.entry("base_lightning_damage_resistance_%", "Lightning Resistance"),
          Map.entry("base_chaos_damage_resistance_%", "Chaos Resistance"),
          Map.entry("base_resist_all_elements_%", "All Elemental Resistances"));
  private static final Map<String, String> FLAT =
      Map.ofEntries(
          Map.entry("base_maximum_life", "Maximum Life"),
          Map.entry("base_maximum_mana", "Maximum Mana"),
          Map.entry("base_maximum_energy_shield", "Maximum Energy Shield"),
          Map.entry("base_spirit_from_equipment", "Spirit"),
          Map.entry("additional_strength", "Strength"),
          Map.entry("additional_dexterity", "Dexterity"),
          Map.entry("additional_intelligence", "Intelligence"),
          Map.entry("additional_all_attributes", "All Attributes"),
          Map.entry("accuracy_rating", "Accuracy Rating"));

  public GoalCatalogIndex(List<ItemCatalog> catalogs) {
    var indexed = new TreeMap<String, ItemCatalog>();
    for (var catalog : catalogs) {
      if (indexed.put(catalog.base().id(), catalog) != null)
        throw new IllegalArgumentException("Duplicate base catalog");
    }
    bases = Collections.unmodifiableMap(indexed);
    var signature = new StringBuilder(RULE_VERSION);
    indexed
        .values()
        .forEach(
            c -> {
              signature.append(c.metadata()).append(c.base());
              c.modifiers().values().stream()
                  .sorted(Comparator.comparing(ModifierDefinition::id))
                  .forEach(
                      d -> {
                        signature
                            .append(d.id())
                            .append(d.name())
                            .append(d.layer())
                            .append(d.affixType())
                            .append(d.familyIds().stream().sorted().toList())
                            .append(d.requiredItemLevel())
                            .append(d.weight())
                            .append(d.tier())
                            .append(d.text())
                            .append(d.stats())
                            .append(d.tags().stream().sorted().toList())
                            .append(d.sourceUrl());
                      });
            });
    try {
      version =
          "hephaistos-goal-v1-"
              + HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(signature.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String version() {
    return version;
  }

  public Optional<ItemCatalog> itemCatalog(Context context) {
    var catalog = bases.get(context.baseItemId());
    return catalog != null && catalog.metadata().snapshotId().equals(context.snapshotId())
        ? Optional.of(catalog)
        : Optional.empty();
  }

  public GoalCatalog catalog(Context context) {
    var itemCatalog = itemCatalog(context).orElse(null);
    var issues = new ArrayList<Issue>();
    if (itemCatalog == null)
      issues.add(
          Issue.warning(
              bases.containsKey(context.baseItemId()) ? "UNSUPPORTED_SNAPSHOT" : "UNSUPPORTED_BASE",
              "/context",
              "No reviewed catalog exists for this context."));
    boolean numeric = NumericAdditionKernel.supports(itemCatalog);
    var groups =
        Arrays.stream(Type.values())
            .map(
                t ->
                    new GroupSupport(
                        t,
                        Capability.SUPPORTED,
                        numeric ? Capability.SUPPORTED : Capability.UNSUPPORTED,
                        numeric ? null : "NUMERIC_BASE_NOT_IMPLEMENTED"))
            .toList();
    return new GoalCatalog(
        1,
        version,
        context,
        groups,
        itemCatalog == null ? List.of() : stats(itemCatalog, context.itemLevel()),
        List.copyOf(issues));
  }

  /** Retains known IDs across base changes so validation can diagnose ineligibility. */
  public Map<String, Stat> knownStats(Context context) {
    var known = new TreeMap<String, Stat>();
    for (var base : bases.values())
      for (var stat : stats(base, context.itemLevel()))
        known.putIfAbsent(
            stat.statId(),
            new Stat(
                stat.statId(),
                stat.label(),
                stat.unit(),
                stat.kind(),
                stat.support(),
                false,
                "STAT_NOT_ON_SELECTED_BASE",
                stat.sourceStatIds(),
                stat.contributions(),
                stat.sourceUrls()));
    catalog(context).stats().forEach(s -> known.put(s.statId(), s));
    return Collections.unmodifiableMap(known);
  }

  public static String id(String layer, String source) {
    return "hephaistos:v1:" + layer.toLowerCase(Locale.ROOT) + ":" + source;
  }

  public static Optional<Mapping> mapping(Stat stat) {
    if (stat.kind() == Kind.PSEUDO || stat.support().evaluation() != Capability.SUPPORTED)
      return Optional.empty();
    return Optional.of(new Mapping(stat.sourceStatIds().getFirst(), stat.kind().name(), 1, 1));
  }

  private List<Stat> stats(ItemCatalog catalog, int level) {
    var definitions = new TreeMap<String, List<ModifierDefinition>>();
    for (var modifier : catalog.modifiers().values())
      for (var stat : modifier.stats())
        definitions
            .computeIfAbsent(id(modifier.layer().name(), stat.id()), key -> new ArrayList<>())
            .add(modifier);
    var result = new TreeMap<String, Stat>();
    definitions.forEach(
        (key, modifiers) -> {
          var first = modifiers.getFirst();
          var raw =
              first.stats().stream()
                  .map(ModifierDefinition.StatRange::id)
                  .filter(s -> id(first.layer().name(), s).equals(key))
                  .findFirst()
                  .orElseThrow();
          boolean supported = PERCENT.containsKey(raw) || FLAT.containsKey(raw);
          boolean eligible =
              modifiers.stream()
                  .anyMatch(
                      d ->
                          d.layer() == ModifierDefinition.Layer.IMPLICIT
                              || (d.requiredItemLevel() <= level && d.weight() > 0));
          result.put(
              key,
              new Stat(
                  key,
                  PERCENT.getOrDefault(raw, FLAT.getOrDefault(raw, raw)),
                  PERCENT.containsKey(raw) ? "percent" : FLAT.containsKey(raw) ? "flat" : "source",
                  Kind.valueOf(first.layer().name()),
                  new Support(
                      supported ? Capability.SUPPORTED : Capability.UNSUPPORTED,
                      Capability.UNSUPPORTED,
                      supported
                          ? "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED"
                          : "UNIT_OR_EFFECT_NOT_REVIEWED"),
                  eligible,
                  eligible ? null : "NO_ELIGIBLE_GENERATION_MODIFIER",
                  List.of(raw),
                  List.of(),
                  modifiers.stream()
                      .map(ModifierDefinition::sourceUrl)
                      .distinct()
                      .sorted()
                      .toList()));
        });
    addPseudo(
        result,
        "total_cold_resistance",
        "Total Cold Resistance",
        "percent",
        List.of("base_cold_damage_resistance_%", "base_resist_all_elements_%"));
    addPseudo(
        result,
        "total_fire_resistance",
        "Total Fire Resistance",
        "percent",
        List.of("base_fire_damage_resistance_%", "base_resist_all_elements_%"));
    addPseudo(
        result,
        "total_lightning_resistance",
        "Total Lightning Resistance",
        "percent",
        List.of("base_lightning_damage_resistance_%", "base_resist_all_elements_%"));
    addPseudo(
        result,
        "total_strength",
        "Total Strength",
        "flat",
        List.of("additional_strength", "additional_all_attributes"));
    addPseudo(
        result,
        "total_dexterity",
        "Total Dexterity",
        "flat",
        List.of("additional_dexterity", "additional_all_attributes"));
    addPseudo(
        result,
        "total_intelligence",
        "Total Intelligence",
        "flat",
        List.of("additional_intelligence", "additional_all_attributes"));
    if (NumericAdditionKernel.supports(catalog)) {
      result.replaceAll(
          (key, stat) ->
              stat.support().evaluation() == Capability.SUPPORTED
                  ? new Stat(
                      stat.statId(),
                      stat.label(),
                      stat.unit(),
                      stat.kind(),
                      new Support(Capability.SUPPORTED, Capability.SUPPORTED, null),
                      stat.eligible(),
                      stat.eligibilityReason(),
                      stat.sourceStatIds(),
                      stat.contributions(),
                      stat.sourceUrls())
                  : stat);
    }
    return List.copyOf(result.values());
  }

  private static void addPseudo(
      Map<String, Stat> result, String name, String label, String unit, List<String> raw) {
    // Only direct layer-scoped sources are allowed; pseudo-to-pseudo edges cannot double count.
    var sources =
        result.values().stream()
            .filter(s -> s.kind() != Kind.PSEUDO && raw.contains(s.sourceStatIds().getFirst()))
            .toList();
    if (sources.isEmpty()) return;
    boolean eligible = sources.stream().anyMatch(Stat::eligible);
    var contributions =
        sources.stream().map(s -> new Contribution(s.statId(), BigDecimal.ONE)).toList();
    result.put(
        id("pseudo", name),
        new Stat(
            id("pseudo", name),
            label,
            unit,
            Kind.PSEUDO,
            new Support(
                Capability.SUPPORTED,
                Capability.UNSUPPORTED,
                "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED"),
            eligible,
            eligible ? null : "NO_ELIGIBLE_GENERATION_MODIFIER",
            sources.stream().map(Stat::statId).toList(),
            contributions,
            sources.stream().flatMap(s -> s.sourceUrls().stream()).distinct().sorted().toList()));
  }
}
