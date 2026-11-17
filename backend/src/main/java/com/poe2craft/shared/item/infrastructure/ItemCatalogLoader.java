package com.poe2craft.shared.item.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 봉인된 실행 JSON을 검증하고 불변 카탈로그를 공유하는 로더다. 실행 중 외부 사이트나 raw를 읽지 않는다.
 *
 * <p>JSON은 중복 키·알 수 없는 필드·누락·null·뒤따르는 토큰을 거절하도록 mapper가 만든 공용 파서다. CATALOGS는 풀+순서 있는 확장 목록별 결과이며
 * loadBundled의 클래스 잠금으로 보호한다. TOP_CATALOGS는 BaseRegistry에 있는 베이스만, SPECIALS는 번들의 특수 옵션 문서만 저장한다.
 * MODIFIERS는 전체 값이 같은 불변 ModifierDefinition을 sharedDefinition/sharedDefinitions로 공유한다. ID가 같아도
 * 가중치·티어·출처 등이 다르면 합치지 않는다. 사용자 입력은 풀에 넣지 않는다. private 생성자는 인스턴스 생성을 막는다.
 *
 * <p>loadDefault/Stocky/Bow/Wand/Body/Sceptre/Belt/Helmet/Ring은 기존 풀과 확장 순서를 선택한다. loadBasicJewel은
 * 지원 주얼 키를 검사한다. loadTopBase는 등록을 확인한 뒤 readTopBase가 원본 정체성· snapshot 정책을 적용한 결과를 공유한다.
 * loadSapphire는 Sapphire.CATALOG의 지연 초기화를 사용하고 readSapphire가 과거 호환 snapshot을 보존한다.
 * bundledDefinitions는 시작 시 대상 검증에도 같은 캐시를 사용한다. special은 봉인된 확장을 읽고, loadBundled는 기본 풀에 확장을 순서대로
 * 합친다.
 *
 * <p>오프라인 load는 catalog/raw/details 스트림의 체크섬을 대조한다. loadWithSpecial은 특수 원자료도 검증하며 addSpecial의 스트림
 * 오버로드가 검증한 문서를 내부 오버로드로 넘긴다. 내부 addSpecial은 일반 출현 가중치를 보존하면서 특수 옵션·통계·호환 snapshot을 추가한다. digest는
 * 입력 bytes의 SHA-256이다. 이 오프라인 메서드들은 사용자 자료를 정적 캐시에 보관하지 않고 IOException을 전달한다. 번들 I/O 실패는
 * IllegalStateException, 지원하지 않는 키·정체성·체크섬은 IllegalArgumentException이다.
 *
 * <p>직렬화 타입은 Document(기본 풀), SpecialMetadata(확장 출처), SpecialDocument(확장 옵션)다.
 */
public final class ItemCatalogLoader {

  private static final ObjectMapper JSON = mapper();
  private static final java.util.Map<String, ItemCatalog> CATALOGS = new java.util.HashMap<>();
  private static final java.util.concurrent.ConcurrentMap<String, ItemCatalog> TOP_CATALOGS =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.ConcurrentMap<String, SpecialDocument> SPECIALS =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.ConcurrentMap<ModifierDefinition, ModifierDefinition>
      MODIFIERS = new java.util.concurrent.ConcurrentHashMap<>();

  private ItemCatalogLoader() {}

  /** 봉인 번들에서 읽은 불변 옵션만 전체 값이 같은 경우 공유한다. 사용자 입력은 이 풀에 넣지 않는다. */
  public static ModifierDefinition sharedDefinition(ModifierDefinition definition) {
    return MODIFIERS.computeIfAbsent(definition, value -> value);
  }

  private static List<ModifierDefinition> sharedDefinitions(List<ModifierDefinition> definitions) {
    return definitions.stream().map(ItemCatalogLoader::sharedDefinition).toList();
  }

  /** 시작 검증에서도 실제 로더의 캐시를 사용해 같은 카탈로그를 다시 역직렬화하지 않는다. */
  public static List<ModifierDefinition> bundledDefinitions(String resource) {
    if (resource.startsWith("/")) resource = resource.substring(1);
    if (!resource.startsWith("catalog/"))
      throw new IllegalArgumentException("Catalog resource required");
    if (resource.endsWith("/catalog.json"))
      return List.copyOf(loadBundled(resource.split("/")[1]).modifiers().values());
    if (resource.endsWith(".catalog.json")) return special("/" + resource).modifiers();
    throw new IllegalArgumentException("Unsupported catalog resource");
  }

  private static SpecialDocument special(String resource) {
    return SPECIALS.computeIfAbsent(
        resource,
        key -> {
          try (var input = SealedResources.open(key)) {
            var value = JSON.readValue(input, SpecialDocument.class);
            return new SpecialDocument(value.metadata(), sharedDefinitions(value.modifiers()));
          } catch (IOException e) {
            throw new IllegalStateException("Cannot load reviewed extension: " + key, e);
          }
        });
  }

  /** Reviewed endgame variants retain source modifier identities, never legacy base identities. */
  public static ItemCatalog loadTopBase(String key) {
    BaseRegistry.require(key);
    return TOP_CATALOGS.computeIfAbsent(key, ItemCatalogLoader::readTopBase);
  }

  private static ItemCatalog readTopBase(String key) {
    var reviewed = BaseRegistry.require(key);
    String id = reviewed.id(), name = reviewed.name();
    ItemCatalog pool;
    if ("BODY".equals(reviewed.policy().legacyCatalog())) pool = loadBody();
    else if ("HELMET".equals(reviewed.policy().legacyCatalog())) pool = loadHelmet();
    else {
      pool = loadBundled(reviewed.pool());
      if (!pool.base().id().equals(id) || !pool.base().name().equals(name))
        throw new IllegalArgumentException("Reviewed catalog identity mismatch");
    }
    String sourceDigest = reviewed.sourceSha256();
    var old = pool.metadata();
    String poolDigest;
    try {
      poolDigest =
          digest(
              new java.io.ByteArrayInputStream(old.snapshotId().getBytes(StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot identify reviewed modifier pool", e);
    }
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            "poe2db-"
                + key
                + ("-" + reviewed.policy().snapshotDate() + "-")
                + sourceDigest.substring(0, 12)
                + "-"
                + poolDigest.substring(0, 12),
            reviewed.policy().snapshotRetrievedAt().equals("SOURCE")
                ? old.retrievedAt()
                : reviewed.policy().snapshotRetrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount(),
            old.suffixCount(),
            old.prefixWeight(),
            old.suffixWeight()),
        new ItemCatalog.BaseItem(
            id,
            name,
            "https://poe2db.tw/us/" + name.replace(' ', '_'),
            pool.base().implicitModifierId(),
            pool.base().magicPrefixes(),
            pool.base().magicSuffixes(),
            pool.base().rarePrefixes(),
            pool.base().rareSuffixes()),
        new ArrayList<>(pool.modifiers().values()));
  }

  /** Sapphire-only ordinary candidates with explicitly modeled equal selection weights. */
  public static ItemCatalog loadSapphire() {
    return Sapphire.CATALOG;
  }

  /** Sapphire의 CATALOG를 첫 접근에 한 번 만들고 이후에는 같은 불변 카탈로그를 반환하는 홀더다. */
  private static final class Sapphire {
    private static final ItemCatalog CATALOG = readSapphire();
  }

  private static ItemCatalog readSapphire() {
    var catalog = loadBundled("sapphire");
    return new ItemCatalog(
        catalog.metadata(),
        catalog.base(),
        new ArrayList<>(catalog.modifiers().values()),
        List.of(
            "poe2db-sapphire-editor-20261004-63e81efc2080",
            "poe2db-sapphire-basic-liquid-20261004-2eaf12518879"));
  }

  public static ItemCatalog loadBasicJewel(String base) {
    if (!java.util.Set.of(
            "ruby",
            "emerald",
            "diamond",
            "time-lost-ruby",
            "time-lost-emerald",
            "time-lost-sapphire",
            "time-lost-diamond")
        .contains(base)) throw new IllegalArgumentException("Unsupported Basic Jewel catalog");
    return loadBundled(base);
  }

  public static ItemCatalog loadBow() {
    return loadBundled("crude-bow", "perfect-essences");
  }

  public static ItemCatalog loadWand() {
    return loadBundled("attuned-wand", "perfect-essences");
  }

  public static ItemCatalog loadBody() {
    return loadBundled("rusted-cuirass", "perfect-essences");
  }

  public static ItemCatalog loadSceptre() {
    return loadBundled("rattling-sceptre", "perfect-essences");
  }

  public static ItemCatalog loadBelt() {
    return loadBundled("rawhide-belt", "perfect-essences");
  }

  public static ItemCatalog loadHelmet() {
    return loadBundled("rusted-greathelm", "perfect-essences");
  }

  public static ItemCatalog loadRing() {
    return loadBundled("iron-ring", "perfect-essences");
  }

  public static ItemCatalog loadDefault() {
    return loadBundled(
        "solar-amulet",
        "perfect-infinite",
        "perfect-enhancement",
        "breach-essence",
        "runic-alloy",
        "abyss-essence");
  }

  public static ItemCatalog loadStocky() {
    return loadBundled(
        "stocky-mitts",
        "abyss-essence",
        "horror-essence",
        "perfect-grounding-opulence",
        "prismatic-alloy",
        "scalar-alloys",
        "reviewed-alloys");
  }

  // Runtime integrity comes from the ruleset seal. Raw captures are archive-only test evidence.
  private static synchronized ItemCatalog loadBundled(String pool, String... extensions) {
    String key = pool + "|" + String.join("|", extensions);
    var existing = CATALOGS.get(key);
    if (existing != null) return existing;
    String root = "/catalog/" + pool + "/";
    ItemCatalog catalog;
    try {
      if (extensions.length == 0) {
        try (var data = SealedResources.open(root + "catalog.json")) {
          var document = JSON.readValue(data, Document.class);
          catalog =
              new ItemCatalog(
                  document.metadata(), document.base(), sharedDefinitions(document.modifiers()));
        }
      } else {
        catalog = loadBundled(pool);
        for (String extension : extensions)
          catalog = addSpecial(catalog, special(root + extension + ".catalog.json"));
      }
      CATALOGS.put(key, catalog);
      return catalog;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed catalog: " + pool, e);
    }
  }

  /** Offline snapshot import: verify captured evidence before accepting a reviewed catalog. */
  public static ItemCatalog load(InputStream catalog, InputStream raw, InputStream details)
      throws IOException {
    var mapper = JSON;
    var document = mapper.readValue(catalog, Document.class);
    if (!digest(raw).equals(document.metadata().rawSha256())
        || !digest(details).equals(document.metadata().detailsSha256())) {
      throw new IllegalArgumentException("Snapshot source checksum mismatch");
    }
    return new ItemCatalog(document.metadata(), document.base(), document.modifiers());
  }

  /**
   * The ordinary snapshot remains intact; special zero-spawn results have their own source digest.
   */
  public static ItemCatalog loadWithSpecial(
      InputStream catalog,
      InputStream raw,
      InputStream details,
      InputStream special,
      InputStream specialRaw)
      throws IOException {
    return addSpecial(load(catalog, raw, details), special, specialRaw);
  }

  /** Extend only with reviewed definitions while retaining verified older frame identities. */
  public static ItemCatalog addSpecial(
      ItemCatalog ordinary, InputStream special, InputStream specialRaw) throws IOException {
    var extension = JSON.readValue(special, SpecialDocument.class);
    var source = extension.metadata();
    if (!source.rawSha256().matches("[0-9a-f]{64}")
        || !digest(specialRaw).equals(source.rawSha256()))
      throw new IllegalArgumentException("Special modifier source checksum mismatch");
    return addSpecial(ordinary, extension);
  }

  private static ItemCatalog addSpecial(ItemCatalog ordinary, SpecialDocument extension)
      throws IOException {
    var source = extension.metadata();
    if (!source.rawSha256().matches("[0-9a-f]{64}"))
      throw new IllegalArgumentException("Invalid historical source checksum");
    if (source.snapshotId().isBlank()
        || source.retrievedAt().isBlank()
        || source.sourceUrl().isBlank()
        || extension.modifiers().isEmpty()
        || extension.modifiers().stream()
            .anyMatch(d -> d.layer() != ModifierDefinition.Layer.EXPLICIT || d.weight() != 0))
      throw new IllegalArgumentException(
          "Special modifiers must be sourced explicit zero-spawn results");
    var all = new ArrayList<>(ordinary.modifiers().values());
    all.addAll(extension.modifiers());
    var old = ordinary.metadata();
    int extraPrefixes =
        (int)
            extension.modifiers().stream()
                .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                .count();
    int extraSuffixes = extension.modifiers().size() - extraPrefixes;
    String identity = old.snapshotId() + "+" + source.snapshotId();
    // Retain the already published v12 identity; later additive snapshots use a bounded digest.
    // State bucket identities are limited to 120 characters, irrespective of extension count.
    if (!ordinary.compatibleSnapshotIds().isEmpty())
      identity =
          (old.sourceUrl().contains("Gloves_str") ? "stocky-special-" : "solar-special-")
              + digest(new java.io.ByteArrayInputStream(identity.getBytes(StandardCharsets.UTF_8)));
    var combined =
        new ItemCatalog.Metadata(
            identity,
            source.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount() + extraPrefixes,
            old.suffixCount() + extraSuffixes,
            old.prefixWeight(),
            old.suffixWeight());
    var compatible = new ArrayList<>(ordinary.compatibleSnapshotIds());
    compatible.add(old.snapshotId());
    return new ItemCatalog(combined, ordinary.base(), all, compatible);
  }

  private static ObjectMapper mapper() {
    return new ObjectMapper()
        .enable(
            com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature())
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  private static String digest(InputStream input) throws IOException {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  /** 기본 JSON 문서다. metadata는 snapshot·출처·집계, base는 베이스 정체성·옵션 한도, modifiers는 옵션 정의다. */
  private record Document(
      ItemCatalog.Metadata metadata,
      ItemCatalog.BaseItem base,
      List<ModifierDefinition> modifiers) {}

  /** 특수 옵션 출처다. snapshotId는 확장 식별자, retrievedAt은 수집 시각, sourceUrl은 출처, rawSha256은 당시 원자료 해시다. */
  private record SpecialMetadata(
      String snapshotId, String retrievedAt, String sourceUrl, String rawSha256) {}

  /** 확장 JSON 문서다. metadata는 출처 검증 정보, modifiers는 일반 풀에서 출현하지 않는 명시 옵션이다. */
  private record SpecialDocument(SpecialMetadata metadata, List<ModifierDefinition> modifiers) {}
}
