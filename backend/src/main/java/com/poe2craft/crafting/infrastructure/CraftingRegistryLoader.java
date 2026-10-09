package com.poe2craft.crafting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.WorkbenchSimulator;
import com.poe2craft.item.BaseRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Immutable versioned inventory, not proof that pending entries are executable rules. */
public final class CraftingRegistryLoader {
  private static final Set<String> ENTRY_FIELDS =
      Set.of(
          "id",
          "name",
          "category",
          "sourceUrl",
          "sourcePage",
          "sourceRetrievedAt",
          "sourceSha256",
          "registrationStatus",
          "effectStatus",
          "solarStatus",
          "action",
          "reason",
          "ruleSource",
          "ruleVerifiedAt",
          "availabilityStatus",
          "minimumModifierLevel",
          "minimumModifierLevelRef",
          "minimumLevelException",
          "serviceScope",
          "supportedBaseSet",
          "supportedBases",
          "implementationScope",
          "interactionScope",
          "stockyStatus",
          "bowStatus",
          "wandEvidence",
          "bodyEvidence",
          "ringEvidence",
          "ruleSourceSha256",
          "modifierSource",
          "minimumSupportedItemLevel",
          "serviceScopeReason",
          "beltEvidence",
          "helmetEvidence",
          "sceptreEvidence",
          "availabilitySource",
          "registrationHistory",
          "verificationEvidence",
          "qualityPolicy");

  private CraftingRegistryLoader() {}

  public static JsonNode load() {
    WorkbenchDefinitionsLoader.initialize();
    try (var stream = com.poe2craft.support.SealedResources.open("/crafting/registry-v2.json");
        var scopes =
            com.poe2craft.support.SealedResources.open("/crafting/supported-base-sets-v1.json")) {
      var registry = (ObjectNode) read(stream, scopes);
      var ruleset = RulesetManifestLoader.load();
      registry
          .putObject("ruleset")
          .put("schemaVersion", ruleset.schemaVersion())
          .put("rulesetVersion", ruleset.rulesetVersion())
          .put("identity", ruleset.identity())
          .put("gameSeason", ruleset.gameSeason())
          .put("gamePatch", ruleset.gamePatch())
          .put("provenance", ruleset.provenance());
      return registry;
    } catch (IOException ex) {
      throw new IllegalStateException("Invalid crafting registry", ex);
    }
  }

  public record BaseSets(int schemaVersion, Map<String, List<String>> sets) {}

  /** Expands explicit reviewed sets into the unchanged public JSON contract. */
  public static JsonNode read(InputStream stream, InputStream scopes) throws IOException {
    if (stream == null || scopes == null)
      throw new IllegalArgumentException("Missing crafting registry data");
    var mapper = com.poe2craft.crafting.infrastructure.BundledJson.mapper();
    var sets = mapper.readValue(scopes, BaseSets.class);
    if (sets.schemaVersion() != 1 || sets.sets() == null || sets.sets().isEmpty())
      throw new IllegalArgumentException("Invalid supported base sets schema");
    sets.sets()
        .forEach(
            (key, members) -> {
              if (!key.matches("[a-z0-9-]+"))
                throw new IllegalArgumentException("Invalid base set ID");
              validateBases(members);
            });
    var registry = mapper.readTree(stream);
    if (!registry.isObject()
        || !registry.path("entries").isArray()
        || !registry.path("workbenchBases").isObject()
        || !registry.path("version").asText().equals("equipment-crafting-registry-v2")
        || !registry.path("ruleVersion").asText().equals(WorkbenchSimulator.RULE_VERSION)
        || !registry.path("ledgerVersion").asText().equals(WorkbenchSimulator.LEDGER_VERSION))
      throw new IllegalArgumentException("Invalid crafting registry shape or version");
    var ids = new HashSet<String>();
    var usedSets = new HashSet<String>();
    int active = 0, deferred = 0;
    for (var entry : registry.get("entries")) {
      if (!entry.isObject()) throw new IllegalArgumentException("Invalid crafting registry entry");
      entry
          .fieldNames()
          .forEachRemaining(
              field -> {
                if (!ENTRY_FIELDS.contains(field))
                  throw new IllegalArgumentException(
                      "Unknown crafting registry entry field: " + field);
              });
      String id = text(entry, "id");
      if (!ids.add(id)) throw new IllegalArgumentException("Duplicate crafting registry ID: " + id);
      text(entry, "name");
      text(entry, "category");
      text(entry, "effectStatus");
      String scope = text(entry, "serviceScope");
      if (scope.equals("ACTIVE")) active++;
      else if (scope.equals("DEFERRED")) deferred++;
      else throw new IllegalArgumentException("Unknown crafting service scope: " + id);
      if ((scope.equals("ACTIVE") || entry.hasNonNull("sourceSha256"))
          && !entry.path("sourceSha256").asText().matches("[0-9a-f]{64}"))
        throw new IllegalArgumentException("Invalid crafting source digest: " + id);
      if (entry.has("minimumModifierLevelRef")) {
        String reference = text(entry, "minimumModifierLevelRef");
        if (entry.has("minimumModifierLevel") || !reference.equals(text(entry, "action")))
          throw new IllegalArgumentException("Conflicting currency level definition: " + id);
        var action = WorkbenchCurrency.valueOf(reference);
        ((ObjectNode) entry).remove("minimumModifierLevelRef");
        ((ObjectNode) entry).put("minimumModifierLevel", action.minimumModifierLevel());
      } else if (entry.has("minimumModifierLevel")) {
        var action = WorkbenchCurrency.valueOf(text(entry, "action"));
        if (count(entry, "minimumModifierLevel") != action.minimumModifierLevel())
          throw new IllegalArgumentException("Currency level differs from its definition: " + id);
      }
      if (entry.has("supportedBaseSet")) {
        String set = text(entry, "supportedBaseSet");
        var members = sets.sets().get(set);
        if (members == null || entry.has("supportedBases"))
          throw new IllegalArgumentException("Unknown or conflicting crafting base set: " + id);
        usedSets.add(set);
        ((ObjectNode) entry).remove("supportedBaseSet");
        ((ObjectNode) entry).set("supportedBases", mapper.valueToTree(members));
      }
      if (entry.has("supportedBases")) {
        if (!entry.get("supportedBases").isArray())
          throw new IllegalArgumentException("Invalid supported bases: " + id);
        var members = new ArrayList<String>();
        for (var member : entry.get("supportedBases")) {
          if (!member.isTextual())
            throw new IllegalArgumentException("Invalid supported base key: " + id);
          members.add(member.asText());
        }
        validateBases(members);
      }
      if (entry.path("category").asText().equals("OMEN") && scope.equals("ACTIVE")) {
        var omen = com.poe2craft.crafting.domain.WorkbenchOmen.fromId(id);
        if (entry.hasNonNull("action") && !omen.name().equals(text(entry, "action")))
          throw new IllegalArgumentException("Crafting omen identity differs: " + id);
      } else if (entry.hasNonNull("action")) WorkbenchCurrency.valueOf(text(entry, "action"));
    }
    if (!usedSets.equals(sets.sets().keySet()))
      throw new IllegalArgumentException("Unused supported base set");
    var summary = registry.path("serviceScope");
    if (count(summary, "registered") != ids.size()
        || count(summary, "active") != active
        || count(summary, "deferred") != deferred)
      throw new IllegalArgumentException("Crafting inventory counts differ");
    registry
        .path("workbenchBases")
        .fieldNames()
        .forEachRemaining(
            key -> {
              if (!BaseRegistry.registeredKeys().contains(key))
                throw new IllegalArgumentException("Unknown workbench base: " + key);
            });
    return registry;
  }

  private static void validateBases(List<String> members) {
    if (members == null
        || members.isEmpty()
        || new HashSet<>(members).size() != members.size()
        || !BaseRegistry.registeredKeys().containsAll(members))
      throw new IllegalArgumentException("Unknown, empty or duplicate supported base keys");
  }

  private static int count(JsonNode source, String field) {
    var value = source.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0)
      throw new IllegalArgumentException("Invalid crafting registry count: " + field);
    return value.intValue();
  }

  private static String text(JsonNode source, String field) {
    var value = source.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank())
      throw new IllegalArgumentException("Missing crafting registry field: " + field);
    return value.asText();
  }
}
