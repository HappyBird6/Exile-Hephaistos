package com.poe2craft.item.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ModifierDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Reads a bundled, reviewed snapshot; never fetches data during application execution. */
public final class ItemCatalogLoader {
  private static final String ROOT = "/catalog/solar-amulet/";

  private ItemCatalogLoader() {}

  public static ItemCatalog loadDefault() {
    try (var catalog = resource("catalog.json");
        var raw = resource("base.raw.json");
        var details = resource("details.raw.json")) {
      return load(catalog, raw, details);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Solar Amulet catalog", e);
    }
  }

  /** Streams are owned by the caller. The same contract can receive a future collected snapshot. */
  public static ItemCatalog load(InputStream catalog, InputStream raw, InputStream details)
      throws IOException {
    var mapper =
        new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    var document = mapper.readValue(catalog, Document.class);
    if (!digest(raw).equals(document.metadata().rawSha256())
        || !digest(details).equals(document.metadata().detailsSha256())) {
      throw new IllegalArgumentException("Snapshot source checksum mismatch");
    }
    return new ItemCatalog(document.metadata(), document.base(), document.modifiers());
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
}
