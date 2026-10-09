package com.poe2craft.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/** Bundled reviewed identities and explicit class policies. Unknown bases inherit no capability. */
public final class BaseRegistry {
  public record Policy(
      String itemClass,
      String ruleVersion,
      String ledgerVersion,
      boolean qualityLimit,
      boolean ordinaryCatalyst,
      boolean refinedCatalyst,
      boolean catalystQuality,
      boolean maximumQualityBreach,
      boolean initializeImplicit,
      Integer socketExecutionMaximum,
      boolean divine,
      boolean sourcePropertyUnscaled,
      String snapshotDate,
      String snapshotRetrievedAt,
      String legacyCatalog) {}

  public record Base(
      String key,
      String id,
      String name,
      String family,
      String pool,
      String slug,
      String sourceSha256,
      Policy policy) {}

  private static final Map<String, Base> TOP;
  private static final Map<String, Policy> POLICIES;
  private static final Set<String> KEYS;

  static {
    try (var bases = com.poe2craft.support.SealedResources.open("/catalog/top-bases.json");
        var policies = com.poe2craft.support.SealedResources.open("/catalog/base-policies.json")) {
      var mapper = new ObjectMapper();
      mapper.enable(
          com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature());
      var source = mapper.readTree(bases);
      var rules = mapper.readTree(policies);
      var snapshot = read(source, rules);
      TOP = snapshot.top();
      POLICIES = snapshot.policies();
      KEYS = snapshot.keys();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  record Snapshot(Map<String, Base> top, Map<String, Policy> policies, Set<String> keys) {}

  static Snapshot read(JsonNode source, JsonNode rules) {
    var mapper = new ObjectMapper();
    if (source == null
        || !source.isObject()
        || source.isEmpty()
        || rules == null
        || !rules.isObject()
        || !rules.path("schemaVersion").isInt()
        || rules.path("schemaVersion").asInt() != 1)
      throw new IllegalArgumentException("Unknown base policy schema");
    for (String section : List.of("families", "baseOverrides", "legacy"))
      if (!rules.path(section).isObject())
        throw new IllegalArgumentException("Missing base policy section: " + section);
    rules
        .path("baseOverrides")
        .fieldNames()
        .forEachRemaining(
            key -> {
              if (!source.has(key))
                throw new IllegalArgumentException("Unknown base override: " + key);
            });
    var keys = new LinkedHashSet<String>();
    var top = new LinkedHashMap<String, Base>();
    var byId = new LinkedHashMap<String, Policy>();
    source
        .fields()
        .forEachRemaining(
            entry -> {
              var b = entry.getValue();
              String key = entry.getKey(), family = required(b, "family");
              if (!b.isObject() || !key.equals(required(b, "key")))
                throw new IllegalArgumentException("Base key differs from registry key: " + key);
              var familyPolicy = rules.path("families").get(family);
              if (familyPolicy == null)
                throw new IllegalArgumentException("Unreviewed base family");
              var merged = familyPolicy.deepCopy();
              var override = rules.path("baseOverrides").get(key);
              if (override != null)
                ((com.fasterxml.jackson.databind.node.ObjectNode) merged)
                    .setAll((com.fasterxml.jackson.databind.node.ObjectNode) override);
              for (String field :
                  List.of(
                      "itemClass",
                      "ruleVersion",
                      "ledgerVersion",
                      "snapshotDate",
                      "snapshotRetrievedAt")) required(merged, field);
              for (String field : List.of("initializeImplicit", "sourcePropertyUnscaled"))
                if (!merged.path(field).isBoolean())
                  throw new IllegalArgumentException("Missing class policy: " + field);
              var policy = parse(merged, mapper, false);
              var base =
                  new Base(
                      key,
                      required(b, "id"),
                      required(b, "name"),
                      family,
                      required(b, "pool"),
                      required(b, "slug"),
                      required(b, "sourceSha256"),
                      policy);
              if (!base.sourceSha256().matches("[0-9a-f]{64}")
                  || !key.matches("[a-z0-9-]+")
                  || !base.pool().matches("[a-z0-9-]+"))
                throw new IllegalArgumentException("Invalid reviewed base identity");
              if (byId.putIfAbsent(base.id(), policy) != null)
                throw new IllegalArgumentException("Duplicate reviewed base ID");
              top.put(key, base);
              keys.add(key);
            });
    rules
        .path("legacy")
        .fields()
        .forEachRemaining(
            entry -> {
              if (!entry.getKey().matches("[a-z0-9-]+"))
                throw new IllegalArgumentException("Invalid legacy base key");
              if (top.containsKey(entry.getKey())
                  || byId.putIfAbsent(
                          required(entry.getValue(), "id"), parse(entry.getValue(), mapper, true))
                      != null) throw new IllegalArgumentException("Duplicate legacy base identity");
              keys.add(entry.getKey());
            });
    return new Snapshot(
        Collections.unmodifiableMap(top),
        Collections.unmodifiableMap(byId),
        Collections.unmodifiableSet(keys));
  }

  private BaseRegistry() {}

  private static String required(JsonNode source, String field) {
    var value = source.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank())
      throw new IllegalArgumentException("Missing base registry field: " + field);
    return value.asText();
  }

  private static Policy parse(JsonNode source, ObjectMapper mapper, boolean legacy) {
    if (source == null || !source.isObject())
      throw new IllegalArgumentException("Invalid base policy object");
    var allowed = new HashSet<String>();
    if (legacy) allowed.addAll(List.of("id", "slug", "initialText"));
    for (var component : Policy.class.getRecordComponents()) allowed.add(component.getName());
    source
        .fieldNames()
        .forEachRemaining(
            field -> {
              if (!allowed.contains(field))
                throw new IllegalArgumentException("Unknown base policy field: " + field);
            });
    // Legacy entries intentionally lack class/version/snapshot fields; their execution remains
    // explicit.
    for (String field :
        List.of(
            "qualityLimit",
            "ordinaryCatalyst",
            "refinedCatalyst",
            "catalystQuality",
            "maximumQualityBreach",
            "divine"))
      if (!source.path(field).isBoolean())
        throw new IllegalArgumentException("Missing base capability: " + field);
    if (!source.has("socketExecutionMaximum")
        || (!source.get("socketExecutionMaximum").isNull()
            && (!source.get("socketExecutionMaximum").isInt()
                || source.get("socketExecutionMaximum").asInt() != 1)))
      throw new IllegalArgumentException(
          "Only the reviewed one-socket execution policy is supported");
    try {
      return mapper
          .readerFor(Policy.class)
          .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .readValue(source);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid base policy", e);
    }
  }

  public static Map<String, Base> topBases() {
    return TOP;
  }

  public static Set<String> registeredKeys() {
    return KEYS;
  }

  public static Base require(String key) {
    var base = TOP.get(key);
    if (base == null) throw new IllegalArgumentException("Unreviewed endgame base");
    return base;
  }

  public static Policy policy(String id) {
    return POLICIES.get(id);
  }

  public static boolean supports(String id) {
    return TOP.values().stream().anyMatch(b -> b.id().equals(id));
  }

  public static Map<String, String> familyBases(String... families) {
    var allowed = Set.of(families);
    var result = new LinkedHashMap<String, String>();
    TOP.forEach(
        (key, base) -> {
          if (allowed.contains(base.family()) && base.policy().legacyCatalog() == null)
            result.put(key, base.id());
        });
    return Collections.unmodifiableMap(result);
  }

  public static boolean qualityLimit(String id) {
    var p = policy(id);
    return p != null && p.qualityLimit();
  }

  public static boolean ordinaryCatalyst(String id) {
    var p = policy(id);
    return p != null && p.ordinaryCatalyst();
  }

  public static boolean refinedCatalyst(String id) {
    var p = policy(id);
    return p != null && p.refinedCatalyst();
  }

  public static boolean catalystQuality(String id) {
    var p = policy(id);
    return p != null && p.catalystQuality();
  }

  public static boolean maximumQualityBreach(String id) {
    var p = policy(id);
    return p != null && p.maximumQualityBreach();
  }

  public static Integer socketExecutionMaximum(String id) {
    var p = policy(id);
    return p == null ? null : p.socketExecutionMaximum();
  }
}
