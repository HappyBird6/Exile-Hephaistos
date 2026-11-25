package com.poe2craft.shared.crafting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 제작 재료 등록 목록을 한 번 검증하고 호출자에게 안전한 JSON 사본을 준다.
 *
 * <p>기존 계약: Immutable versioned inventory, not proof that pending entries are executable rules.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code ENTRY_FIELDS}: 필드목록 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CraftingRegistryLoader()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code JsonNode}.
 *   <li>{@code loadBundle()}: 번들 자원을 읽고 검증한 결과를 구성한다. 반환: {@code JsonNode}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code read(stream, scopes)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code stream}(스트림), {@code
 *       scopes}(scopes). 반환: {@code JsonNode}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IOException}.
 *   <li>{@code validateBases(members)}: 베이스목록을(를) 검증한다. 입력: {@code members}(구성원목록). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code count(source, field)}: 개수 처리에서 {@code source.path}, {@code
 *       value.isIntegralNumber}을(를) 사용한다. 입력: {@code source}(출처), {@code field}(필드). 반환: {@code
 *       int}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code text(source, field)}: 텍스트 처리에서 {@code source.get}, {@code value.isTextual}을(를) 사용한다.
 *       입력: {@code source}(출처), {@code field}(필드). 반환: {@code String}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Loaded}: 최초 접근 시 검증한 번들 값을 JVM 클래스 초기화로 한 번만 공개한다.
 *   <li>{@code BaseSets}: {@code CraftingRegistryLoader}에서 사용하는 베이스·Sets의 불변 값이다.
 * </ul>
 */
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

  /**
   * 최초 접근 시 검증한 번들 값을 JVM 클래스 초기화로 한 번만 공개한다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code VALUE}: 한 번 검증한 공유 값 ({@code JsonNode}).
   * </ul>
   */
  private static final class Loaded {
    private static final JsonNode VALUE = loadBundle();

    /** 검증된 등록 목록을 한 번 순회해 기보의 카오스 재료 ID를 공유한다. */
    private static final Set<String> CHAOS_MATERIALS = chaosMaterials(VALUE);
  }

  /** 등록·구현된 화폐 중 작업대의 기본 행동이 카오스인 공개 식별자를 선택한다. */
  private static Set<String> chaosMaterials(JsonNode registry) {
    var materials = new HashSet<String>();
    for (var entry : registry.path("entries")) {
      if (entry.path("category").asText().equals("CURRENCY")
          && entry.path("registrationStatus").asText().equals("REGISTERED")
          && entry.path("effectStatus").asText().equals("IMPLEMENTED")
          && entry.hasNonNull("action")
          && WorkbenchCurrency.valueOf(entry.path("action").asText()).baseAction()
              == CraftingAction.CHAOS) materials.add(entry.path("id").asText());
    }
    return Set.copyOf(materials);
  }

  /** 게시된 기보의 재료 ID가 현재 등록된 카오스 등급 중 하나인지 확인한다. */
  public static boolean isChaosMaterial(String materialId) {
    return Loaded.CHAOS_MATERIALS.contains(materialId);
  }

  /** 등록된 장신구용 기폭제 재료 ID를 실제 작업대 행동으로 찾는다. 정제형은 주얼 전용이다. */
  public static Optional<WorkbenchCurrency> ordinaryCatalyst(String materialId) {
    for (var entry : Loaded.VALUE.path("entries")) {
      if (entry.path("id").asText().equals(materialId)
          && entry.path("category").asText().equals("CATALYST")
          && entry.path("serviceScope").asText().equals("ACTIVE")
          && entry.hasNonNull("action")) {
        var action = WorkbenchCurrency.valueOf(entry.get("action").asText());
        if (action.catalystType() != null && !action.refinedCatalyst()) return Optional.of(action);
      }
    }
    return Optional.empty();
  }

  public static JsonNode load() {
    return Loaded.VALUE.deepCopy();
  }

  private static JsonNode loadBundle() {
    WorkbenchDefinitionsLoader.initialize();
    try (var stream = SealedResources.open("/crafting/registry-v2.json");
        var scopes = SealedResources.open("/crafting/supported-base-sets-v1.json")) {
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

  /**
   * {@code CraftingRegistryLoader}에서 사용하는 베이스·Sets의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code sets}: sets ({@code Map<String, List<String>>}).
   * </ul>
   */
  public record BaseSets(int schemaVersion, Map<String, List<String>> sets) {}

  /** Expands explicit reviewed sets into the unchanged public JSON contract. */
  public static JsonNode read(InputStream stream, InputStream scopes) throws IOException {
    if (stream == null || scopes == null)
      throw new IllegalArgumentException("Missing crafting registry data");
    var mapper = BundledJson.mapper();
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
        var omen = WorkbenchOmen.fromId(id);
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
