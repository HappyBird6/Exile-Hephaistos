package com.poe2craft.item.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ModifierDefinition;
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
  private static final String ROOT = "/catalog/solar-amulet/";

  private ItemCatalogLoader() {}

  /** Reviewed endgame variants retain source modifier identities, never legacy base identities. */
  public static ItemCatalog loadTopBase(String key) {
    String id;
    String name;
    ItemCatalog pool;
    switch (key) {
      case "soldier" -> {
        id = "Metadata/Items/Armours/BodyArmours/FourBodyStr3Endgame";
        name = "Soldier Cuirass";
        pool = loadBody();
      }
      case "imperial" -> {
        id = "Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame";
        name = "Imperial Greathelm";
        pool = loadHelmet();
      }
      default -> throw new IllegalArgumentException("Unreviewed endgame base");
    }
    String sourceDigest;
    try (var proof = ItemCatalogLoader.class.getResourceAsStream("/catalog/top-bases.json")) {
      var source = mapper().readTree(proof).get(key);
      if (!source.get("id").asText().equals(id) || !source.get("name").asText().equals(name))
        throw new IllegalArgumentException("Reviewed base source identity mismatch");
      sourceDigest = source.get("sourceSha256").asText();
      if (!sourceDigest.matches("[0-9a-f]{64}"))
        throw new IllegalArgumentException("Reviewed base source digest required");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed base source", e);
    }
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
                + "-20261004-"
                + sourceDigest.substring(0, 12)
                + "-"
                + poolDigest.substring(0, 12),
            "2026-10-04",
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount(),
            old.suffixCount(),
            old.prefixWeight(),
            old.suffixWeight()),
        new ItemCatalog.BaseItem(
            id, name, "https://poe2db.tw/us/" + name.replace(' ', '_'), "", 1, 1, 3, 3),
        new ArrayList<>(pool.modifiers().values()));
  }

  /** Sapphire-only ordinary candidates with explicitly modeled equal selection weights. */
  public static ItemCatalog loadSapphire() {
    try (var data = ItemCatalogLoader.class.getResourceAsStream("/catalog/sapphire/catalog.json");
        var raw = ItemCatalogLoader.class.getResourceAsStream("/catalog/sapphire/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/sapphire/details.raw.json")) {
      var catalog = load(data, raw, details);
      return new ItemCatalog(
          catalog.metadata(),
          catalog.base(),
          new ArrayList<>(catalog.modifiers().values()),
          List.of(
              "poe2db-sapphire-editor-20261004-63e81efc2080",
              "poe2db-sapphire-basic-liquid-20261004-2eaf12518879"));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Sapphire starting-item catalog", e);
    }
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
    String root = "/catalog/" + base + "/";
    try (var data = ItemCatalogLoader.class.getResourceAsStream(root + "catalog.json");
        var raw = ItemCatalogLoader.class.getResourceAsStream(root + "base.raw.json");
        var details = ItemCatalogLoader.class.getResourceAsStream(root + "details.raw.json")) {
      return load(data, raw, details);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Basic Jewel catalog", e);
    }
  }

  /** Complete ordinary Bow snapshot for Workbench dispatch only. */
  public static ItemCatalog loadBow() {
    try (var data = ItemCatalogLoader.class.getResourceAsStream("/catalog/crude-bow/catalog.json");
        var raw = ItemCatalogLoader.class.getResourceAsStream("/catalog/crude-bow/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/crude-bow/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/crude-bow/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/crude-bow/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Crude Bow catalog", e);
    }
  }

  /** Complete ordinary Wand snapshot for Workbench dispatch only. */
  public static ItemCatalog loadWand() {
    try (var data =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/attuned-wand/catalog.json");
        var raw =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/attuned-wand/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/attuned-wand/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/attuned-wand/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/attuned-wand/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Attuned Wand catalog", e);
    }
  }

  /** Complete ordinary Body Armour snapshot for Workbench dispatch only. */
  public static ItemCatalog loadBody() {
    try (var data =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rusted-cuirass/catalog.json");
        var raw =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rusted-cuirass/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-cuirass/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-cuirass/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-cuirass/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Rusted Cuirass catalog", e);
    }
  }

  /** Complete ordinary Sceptre snapshot for Workbench dispatch only. */
  public static ItemCatalog loadSceptre() {
    try (var data =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rattling-sceptre/catalog.json");
        var raw =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rattling-sceptre/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rattling-sceptre/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rattling-sceptre/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rattling-sceptre/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Rattling Sceptre catalog", e);
    }
  }

  /** Complete ordinary Belt snapshot for Workbench dispatch only. */
  public static ItemCatalog loadBelt() {
    try (var data =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rawhide-belt/catalog.json");
        var raw =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rawhide-belt/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rawhide-belt/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rawhide-belt/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rawhide-belt/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Rawhide Belt catalog", e);
    }
  }

  /** Complete ordinary Helmet snapshot for Workbench dispatch only. */
  public static ItemCatalog loadHelmet() {
    try (var data =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rusted-greathelm/catalog.json");
        var raw =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/rusted-greathelm/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-greathelm/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-greathelm/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/rusted-greathelm/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Rusted Greathelm catalog", e);
    }
  }

  /** Complete ordinary Ring snapshot for Workbench dispatch only. */
  public static ItemCatalog loadRing() {
    try (var data = ItemCatalogLoader.class.getResourceAsStream("/catalog/iron-ring/catalog.json");
        var raw = ItemCatalogLoader.class.getResourceAsStream("/catalog/iron-ring/base.raw.json");
        var details =
            ItemCatalogLoader.class.getResourceAsStream("/catalog/iron-ring/details.raw.json");
        var special =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/iron-ring/perfect-essences.catalog.json");
        var specialRaw =
            ItemCatalogLoader.class.getResourceAsStream(
                "/catalog/iron-ring/perfect-essences.raw.json")) {
      return loadWithSpecial(data, raw, details, special, specialRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Iron Ring catalog", e);
    }
  }

  public static ItemCatalog loadDefault() {
    try (var catalog = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json");
        var special = resource("perfect-infinite.catalog.json");
        var specialRaw = resource("perfect-infinite.raw.json");
        var enhancement = resource("perfect-enhancement.catalog.json");
        var enhancementRaw = resource("perfect-enhancement.raw.json");
        var breach = resource("breach-essence.catalog.json");
        var breachRaw = resource("breach-essence.raw.json");
        var runic = resource("runic-alloy.catalog.json");
        var runicRaw = resource("runic-alloy.raw.json");
        var abyss = resource("abyss-essence.catalog.json");
        var abyssRaw = resource("abyss-essence.raw.json")) {
      return addSpecial(
          addSpecial(
              addSpecial(
                  addSpecial(
                      loadWithSpecial(catalog, raw, details, special, specialRaw),
                      enhancement,
                      enhancementRaw),
                  breach,
                  breachRaw),
              runic,
              runicRaw),
          abyss,
          abyssRaw);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Solar Amulet catalog", e);
    }
  }

  /** Streams are owned by the caller. The same contract can receive a future collected snapshot. */
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
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  private static InputStream resource(String name) throws IOException {
    var input = ItemCatalogLoader.class.getResourceAsStream(ROOT + name);
    if (input == null) throw new IOException("Missing catalog resource: " + name);
    return input;
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
