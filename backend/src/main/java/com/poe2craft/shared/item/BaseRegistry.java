package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.util.*;

/**
 * 검토된 베이스 정체성과 명시적인 장비 분류 정책을 조회한다.
 *
 * <p>기존 계약: Bundled reviewed identities and explicit class policies. Unknown bases inherit no
 * capability.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code TOP}: 상위 ({@code Map<String, Base>}).
 *   <li>{@code POLICIES}: 정책목록 ({@code Map<String, Policy>}).
 *   <li>{@code KEYS}: 키목록 ({@code Set<String>}).
 *   <li>{@code CLASSES}: 동치류목록 ({@code Map<String, String>}).
 *   <li>{@code GRANTED_SKILLS}: SKILLS ({@code Map<String, Set<String>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code read(source, rules)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code source}(출처), {@code
 *       rules}(규칙목록). 반환: {@code Snapshot}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code BaseRegistry()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code required(source, field)}: 필수 처리에서 {@code source.get}, {@code value.isTextual}을(를)
 *       사용한다. 입력: {@code source}(출처), {@code field}(필드). 반환: {@code String}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code parse(source, mapper, legacy)}: 외부 표현을 내부 값으로 해석한다. 입력: {@code source}(출처), {@code
 *       mapper}(변환기), {@code legacy}(구버전). 반환: {@code Policy}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code topBases()}: {@code TOP} 값을 조회한다. 반환: {@code Map<String, Base>}.
 *   <li>{@code registeredKeys()}: {@code KEYS} 값을 조회한다. 반환: {@code Set<String>}.
 *   <li>{@code require(key)}: 필수 조건을 검사한다. 입력: {@code key}(키). 반환: {@code Base}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code supportedBaseIds()}: 지원·베이스·식별자목록 값을 조회·구성한다. 반환: {@code Set<String>}.
 *   <li>{@code itemClass(id)}: 아이템·동치류 처리에서 {@code CLASSES.get}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code String}.
 *   <li>{@code policy(id)}: 정책 처리에서 {@code POLICIES.get}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code
 *       Policy}.
 *   <li>{@code supports(id)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code familyBases(families)}: 옵션계열·베이스목록 처리에서 {@code TOP.forEach}, {@code
 *       allowed.contains}을(를) 사용한다. 입력: {@code families}(옵션계열목록). 반환: {@code Map<String, String>}.
 *   <li>{@code qualityLimit(id)}: 품질·한도 처리에서 {@code p.qualityLimit}을(를) 사용한다. 입력: {@code id}(식별자).
 *       반환: {@code boolean}.
 *   <li>{@code rareOnly(id)}: 희귀·제한 처리에서 {@code TRUE.equals}, {@code policy.rareOnly}을(를) 사용한다. 입력:
 *       {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code ordinaryCatalyst(id)}: 일반·기폭제 처리에서 {@code p.ordinaryCatalyst}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code boolean}.
 *   <li>{@code refinedCatalyst(id)}: refined·기폭제 처리에서 {@code p.refinedCatalyst}을(를) 사용한다. 입력:
 *       {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code catalystQuality(id)}: 기폭제·품질 처리에서 {@code p.catalystQuality}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code boolean}.
 *   <li>{@code maximumQualityBreach(id)}: 최대값·품질·Breach 처리에서 {@code p.maximumQualityBreach}을(를)
 *       사용한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code recognizesGrantedSkill(id, line)}: recognizes·부여된·Skill의 입력에 따른 분기·결과를 처리한다. 입력:
 *       {@code id}(식별자), {@code line}(행). 반환: {@code boolean}.
 *   <li>{@code observedGrantedSkill(id, line)}: 관측한·부여된·Skill 처리에서 {@code Pattern.compile}, {@code
 *       m.matches}을(를) 사용한다. 입력: {@code id}(식별자), {@code line}(행). 반환: {@code
 *       ItemObservations.GrantedSkill}.
 *   <li>{@code socketExecutionMaximum(id)}: 장착홈·실행·최대값 처리에서 {@code p.socketExecutionMaximum}을(를)
 *       사용한다. 입력: {@code id}(식별자). 반환: {@code Integer}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Policy}: {@code BaseRegistry}에서 사용하는 정책의 불변 값이다.
 *   <li>{@code Base}: {@code BaseRegistry}에서 사용하는 베이스의 불변 값이다.
 *   <li>{@code Snapshot}: {@code BaseRegistry}에서 사용하는 스냅샷의 불변 값이다.
 * </ul>
 *
 * <p>초기화 블록: 선언된 공유 값과 초기 색인을 초기화한다. 초기화 실패는 타입 사용 전에 드러난다.
 */
public final class BaseRegistry {
  /**
   * {@code BaseRegistry}에서 사용하는 정책의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code itemClass}: 아이템·동치류 ({@code String}).
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code qualityLimit}: 품질·한도 ({@code boolean}).
   *   <li>{@code ordinaryCatalyst}: 일반·기폭제 ({@code boolean}).
   *   <li>{@code refinedCatalyst}: refined·기폭제 ({@code boolean}).
   *   <li>{@code catalystQuality}: 기폭제·품질 ({@code boolean}).
   *   <li>{@code maximumQualityBreach}: 최대값·품질·Breach ({@code boolean}).
   *   <li>{@code initializeImplicit}: 초기화·암시옵션 ({@code boolean}).
   *   <li>{@code socketExecutionMaximum}: 장착홈·실행·최대값 ({@code Integer}).
   *   <li>{@code divine}: 신성 ({@code boolean}).
   *   <li>{@code sourcePropertyUnscaled}: 출처·Property·Unscaled ({@code boolean}).
   *   <li>{@code snapshotDate}: 스냅샷·날짜 ({@code String}).
   *   <li>{@code snapshotRetrievedAt}: 스냅샷·수집된·기준 ({@code String}).
   *   <li>{@code legacyCatalog}: 구버전·카탈로그 ({@code String}).
   *   <li>{@code rareOnly}: 희귀·제한 ({@code Boolean}).
   *   <li>{@code newSelection}: new·선택 ({@code Boolean}).
   * </ul>
   */
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
      String legacyCatalog,
      Boolean rareOnly,
      Boolean newSelection) {}

  /**
   * {@code BaseRegistry}에서 사용하는 베이스의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   *   <li>{@code family}: 옵션계열 ({@code String}).
   *   <li>{@code pool}: 후보풀 ({@code String}).
   *   <li>{@code slug}: slug ({@code String}).
   *   <li>{@code sourceSha256}: 출처·SHA·256 ({@code String}).
   *   <li>{@code policy}: 정책 ({@code Policy}).
   * </ul>
   */
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
  private static final Map<String, String> CLASSES;
  private static final Map<String, Set<String>> GRANTED_SKILLS;

  static {
    try (var bases = SealedResources.open("/catalog/top-bases.json");
        var policies = SealedResources.open("/catalog/base-policies.json")) {
      var mapper = new ObjectMapper();
      mapper.enable(
          com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature());
      var source = mapper.readTree(bases);
      var rules = mapper.readTree(policies);
      var snapshot = read(source, rules);
      TOP = snapshot.top();
      POLICIES = snapshot.policies();
      KEYS = snapshot.keys();
      var classes = new LinkedHashMap<String, String>();
      TOP.values().forEach(b -> classes.put(b.id(), b.policy().itemClass()));
      rules
          .path("legacy")
          .forEach(
              b -> {
                String header = b.path("initialText").asText().split("\\n")[0];
                classes.put(b.path("id").asText(), header.substring("Item Class: ".length()));
              });
      CLASSES = Collections.unmodifiableMap(classes);
      var skills = new LinkedHashMap<String, Set<String>>();
      source.forEach(
          b -> {
            var lines = new HashSet<String>();
            for (String field : List.of("sourceProperties", "skillLines"))
              b.path(field)
                  .path("en")
                  .forEach(
                      line -> {
                        if (line.isTextual() && line.asText().startsWith("Grants Skill: "))
                          lines.add(line.asText());
                      });
            skills.put(b.path("id").asText(), Set.copyOf(lines));
          });
      // Bundled legacy base-proof.json evidence; no skill-level progression is inferred.
      skills.put(
          rules.path("legacy").path("wand").path("id").asText(),
          Set.of("Grants Skill: Mana Drain"));
      skills.put(
          rules.path("legacy").path("sceptre").path("id").asText(),
          Set.of("Grants Skill: Skeletal Warrior"));
      GRANTED_SKILLS = Collections.unmodifiableMap(skills);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * {@code BaseRegistry}에서 사용하는 스냅샷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code top}: 상위 ({@code Map<String, Base>}).
   *   <li>{@code policies}: 정책목록 ({@code Map<String, Policy>}).
   *   <li>{@code keys}: 키목록 ({@code Set<String>}).
   * </ul>
   */
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
    for (String field : List.of("rareOnly", "newSelection"))
      if (source.has(field) && !source.get(field).isBoolean())
        throw new IllegalArgumentException("Invalid optional base selection policy: " + field);
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
                || source.get("socketExecutionMaximum").asInt() < 1
                || source.get("socketExecutionMaximum").asInt() > 3)))
      throw new IllegalArgumentException(
          "Only reviewed socket execution maxima 1 through 3 are supported");
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

  /** Reviewed item IDs, including older bases that can still appear in saved sessions. */
  public static Set<String> supportedBaseIds() {
    return CLASSES.keySet();
  }

  public static String itemClass(String id) {
    return CLASSES.get(id);
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

  public static boolean rareOnly(String id) {
    var policy = policy(id);
    return policy != null && Boolean.TRUE.equals(policy.rareOnly());
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

  public static boolean recognizesGrantedSkill(String id, String line) {
    return observedGrantedSkill(id, line) != null;
  }

  public static ItemObservations.GrantedSkill observedGrantedSkill(String id, String line) {
    var m =
        java.util.regex.Pattern.compile("^Grants Skill: (?:Level ([0-9]+) )?(.+)$").matcher(line);
    if (!m.matches()) return null;
    String name = m.group(2);
    boolean known =
        GRANTED_SKILLS.getOrDefault(id, Set.of()).stream()
            .map(
                s ->
                    s.replaceFirst("^Grants Skill: (?:Level (?:[0-9]+|\\([0-9]+-[0-9]+\\)) )?", ""))
            .anyMatch(name::equals);
    if (!known) return null;
    try {
      return new ItemObservations.GrantedSkill(
          name, m.group(1) == null ? null : Integer.valueOf(m.group(1)));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static Integer socketExecutionMaximum(String id) {
    var p = policy(id);
    return p == null ? null : p.socketExecutionMaximum();
  }
}
