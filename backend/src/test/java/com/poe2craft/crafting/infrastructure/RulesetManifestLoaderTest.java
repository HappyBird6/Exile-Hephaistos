package com.poe2craft.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RulesetManifestLoaderTest {
  @Test
  void missingConsumedResourcesAndNewUnsealedDependenciesFail() throws Exception {
    var manifest = RulesetManifestLoader.load();
    var required = com.poe2craft.support.SealedResources.requiredPaths();
    assertTrue(required.contains("catalog/solar-amulet/perfect-infinite.catalog.json"));
    assertTrue(required.contains("catalog/stocky-mitts/reviewed-alloys.raw.json"));
    assertTrue(required.contains("crafting/goalfilter/definitions-v1.json"));
    for (String path : required) {
      var files = new TreeMap<>(manifest.files());
      files.remove(path);
      var incomplete =
          new RulesetManifestLoader.Manifest(
              manifest.schemaVersion(),
              manifest.rulesetVersion(),
              manifest.gameSeason(),
              manifest.gamePatch(),
              manifest.provenance(),
              manifest.engineRuleVersion(),
              manifest.ledgerVersion(),
              files);
      assertThrows(
          IllegalArgumentException.class, () -> RulesetManifestLoader.validate(incomplete), path);
    }
    var next =
        com.poe2craft.support.SealedResources.requiredPaths(
            new ByteArrayInputStream(
                "{\"new-base\":{\"pool\":\"new-reviewed-pool\"}}"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    assertTrue(next.contains("catalog/new-reviewed-pool/catalog.json"));
    try (var unsealed = getClass().getResourceAsStream("/catalog/new-reviewed-pool/catalog.json")) {
      assertNotNull(unsealed, "A present new resource must still require manifest registration");
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> RulesetManifestLoader.validateCoverage(manifest, next));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            com.poe2craft.support.SealedResources.verify(
                "catalog/new-reviewed-pool/catalog.json", "{}".getBytes(), manifest.files()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            com.poe2craft.support.SealedResources.open("/catalog/new-reviewed-pool/catalog.json"));
  }

  @Test
  void fractionalSchemaVersionsAreRejected() throws Exception {
    var mapper = new ObjectMapper();
    var bad =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.valueToTree(RulesetManifestLoader.load());
    bad.put("schemaVersion", 1.5);
    assertThrows(
        IOException.class,
        () -> RulesetManifestLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(bad))));
  }

  @Test
  void currentBundleIsSealedAndUnknownSeasonsAreExplicit() {
    var manifest = RulesetManifestLoader.load();
    assertEquals(461, manifest.files().size());
    assertEquals("UNVERIFIED", manifest.gameSeason());
    assertEquals("UNVERIFIED", manifest.gamePatch());
    assertThrows(UnsupportedOperationException.class, () -> manifest.files().clear());
    var wrong = new TreeMap<>(manifest.files());
    wrong.put(wrong.firstKey(), "0".repeat(64));
    var tampered =
        new RulesetManifestLoader.Manifest(
            manifest.schemaVersion(),
            manifest.rulesetVersion(),
            manifest.gameSeason(),
            manifest.gamePatch(),
            manifest.provenance(),
            manifest.engineRuleVersion(),
            manifest.ledgerVersion(),
            wrong);
    assertNotEquals(manifest.identity(), tampered.identity());
    assertThrows(IllegalArgumentException.class, () -> RulesetManifestLoader.validate(tampered));
    var catalog = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault();
    var definitions = com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load();
    var original = new GoalCatalogIndex(List.of(catalog), definitions);
    assertEquals(
        original.version(),
        new GoalCatalogIndex(List.of(catalog), definitions, manifest.identity()).version());
    assertNotEquals(
        original.version(),
        new GoalCatalogIndex(List.of(catalog), definitions, manifest.identity() + "-next")
            .version());
  }

  @Test
  void schemaEngineLedgerMismatchAndUnsafeResourcePathsAreRejected() throws Exception {
    var mapper = new ObjectMapper();
    for (String field : List.of("engineRuleVersion", "ledgerVersion")) {
      var bad =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              mapper.valueToTree(RulesetManifestLoader.load());
      bad.put(field, "next-unimplemented");
      assertThrows(
          IllegalArgumentException.class,
          () ->
              RulesetManifestLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(bad))));
    }
    var bad =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.valueToTree(RulesetManifestLoader.load());
    bad.put("schemaVersion", 2);
    assertThrows(
        IOException.class,
        () -> RulesetManifestLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(bad))));
  }
}
