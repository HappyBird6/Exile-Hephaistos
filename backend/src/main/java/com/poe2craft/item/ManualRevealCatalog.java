package com.poe2craft.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Additive manual-only definitions. Published eligibility markers are never sampling weights. */
public final class ManualRevealCatalog {
  private ManualRevealCatalog() {}

  private static final Map<String, List<ModifierDefinition>> BASES = load();

  private static Map<String, List<ModifierDefinition>> load() {
    try (var in = com.poe2craft.support.SealedResources.open("/catalog/manual-reveal.json")) {
      var mapper = new ObjectMapper();
      var data = mapper.readTree(in);
      var definitions = new HashMap<String, ModifierDefinition>();
      for (var node : data.path("definitions")) {
        var d = mapper.treeToValue(node, ModifierDefinition.class);
        if (d.weight() != 0 || !d.tags().contains("reveal_only"))
          throw new IllegalArgumentException("Invalid manual reveal definition");
        definitions.put(d.id(), d);
      }
      var result = new HashMap<String, List<ModifierDefinition>>();
      data.path("bases")
          .fields()
          .forEachRemaining(
              entry -> {
                var list = new ArrayList<ModifierDefinition>();
                entry
                    .getValue()
                    .forEach(id -> list.add(Objects.requireNonNull(definitions.get(id.asText()))));
                result.put(entry.getKey(), List.copyOf(list));
              });
      return Map.copyOf(result);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read manual reveal definitions", e);
    }
  }

  public static ItemCatalog enrich(ItemCatalog catalog) {
    var extra =
        BASES.getOrDefault(catalog.base().id(), List.of()).stream()
            .filter(d -> catalog.find(d.id()).isEmpty())
            .toList();
    if (extra.isEmpty()) return catalog;
    var definitions = new ArrayList<>(catalog.modifiers().values());
    definitions.addAll(extra);
    var old = catalog.metadata();
    int prefixes =
        (int)
            extra.stream()
                .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                .count();
    // Ordinary snapshot identity and weights stay intact. The sealed ruleset includes this additive
    // resource.
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            old.snapshotId(),
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount() + prefixes,
            old.suffixCount() + extra.size() - prefixes,
            old.prefixWeight(),
            old.suffixWeight()),
        catalog.base(),
        definitions,
        catalog.compatibleSnapshotIds());
  }
}
