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

/** Reads a bundled, reviewed snapshot; never fetches data during application execution. */
public final class ItemCatalogLoader {

  private ItemCatalogLoader() {}

  /** Reviewed endgame variants retain source modifier identities, never legacy base identities. */
  public static ItemCatalog loadTopBase(String key) {
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
  private static ItemCatalog loadBundled(String pool, String... extensions) {
    String root = "/catalog/" + pool + "/";
    try (var data = SealedResources.open(root + "catalog.json")) {
      var document = mapper().readValue(data, Document.class);
      var catalog = new ItemCatalog(document.metadata(), document.base(), document.modifiers());
      for (String extension : extensions) {
        try (var special = SealedResources.open(root + extension + ".catalog.json")) {
          catalog = addSpecial(catalog, mapper().readValue(special, SpecialDocument.class));
        }
      }
      return catalog;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed catalog: " + pool, e);
    }
  }

  /** Offline snapshot import: verify captured evidence before accepting a reviewed catalog. */
  public static ItemCatalog load(InputStream catalog, InputStream raw, InputStream details)
      throws IOException {
    var mapper = mapper();
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
    var extension = mapper().readValue(special, SpecialDocument.class);
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

  private record Document(
      ItemCatalog.Metadata metadata,
      ItemCatalog.BaseItem base,
      List<ModifierDefinition> modifiers) {}

  private record SpecialMetadata(
      String snapshotId, String retrievedAt, String sourceUrl, String rawSha256) {}

  private record SpecialDocument(SpecialMetadata metadata, List<ModifierDefinition> modifiers) {}
}
