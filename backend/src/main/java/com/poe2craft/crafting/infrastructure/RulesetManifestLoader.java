package com.poe2craft.crafting.infrastructure;

import com.poe2craft.crafting.domain.WorkbenchSimulator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Reviewed bundle identity is separate from JSON schema and an unverified game season. */
public final class RulesetManifestLoader {
  private RulesetManifestLoader() {}

  public record Manifest(
      int schemaVersion,
      String rulesetVersion,
      String gameSeason,
      String gamePatch,
      String provenance,
      String engineRuleVersion,
      String ledgerVersion,
      Map<String, String> files) {
    public Manifest {
      files = Collections.unmodifiableMap(new TreeMap<>(files));
      if (schemaVersion != 1
          || rulesetVersion == null
          || rulesetVersion.isBlank()
          || gameSeason == null
          || gameSeason.isBlank()
          || gamePatch == null
          || gamePatch.isBlank()
          || provenance == null
          || provenance.isBlank()
          || files.isEmpty())
        throw new IllegalArgumentException("Invalid reviewed ruleset manifest");
      files.forEach(
          (path, hash) -> {
            if (!path.matches("(?:catalog|crafting)/[a-zA-Z0-9_./-]+\\.json")
                || path.contains("..")
                || hash == null
                || !hash.matches("[0-9a-f]{64}"))
              throw new IllegalArgumentException("Invalid ruleset resource identity");
          });
    }

    public String identity() {
      return rulesetVersion
          + "-"
          + hash(
              rulesetVersion
                  + "|"
                  + gameSeason
                  + "|"
                  + gamePatch
                  + "|"
                  + engineRuleVersion
                  + "|"
                  + ledgerVersion
                  + "|"
                  + files);
    }
  }

  public static Manifest load() {
    try (var stream =
        RulesetManifestLoader.class.getResourceAsStream("/crafting/ruleset-v1.json")) {
      var manifest = read(stream);
      validate(manifest);
      return manifest;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed ruleset manifest", e);
    }
  }

  public static void validate(Manifest manifest) throws IOException {
    for (var entry : manifest.files().entrySet()) {
      try (var data = RulesetManifestLoader.class.getResourceAsStream("/" + entry.getKey())) {
        if (data == null || !entry.getValue().equals(hash(data.readAllBytes())))
          throw new IllegalArgumentException("Ruleset resource digest differs: " + entry.getKey());
      }
    }
  }

  public static Manifest read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing reviewed ruleset manifest");
    var mapper = BundledJson.mapper();
    var manifest = mapper.readValue(stream, Manifest.class);
    if (!WorkbenchSimulator.RULE_VERSION.equals(manifest.engineRuleVersion())
        || !WorkbenchSimulator.LEDGER_VERSION.equals(manifest.ledgerVersion()))
      throw new IllegalArgumentException("Ruleset engine or ledger version differs");
    return manifest;
  }

  private static String hash(String value) {
    return hash(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String hash(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
