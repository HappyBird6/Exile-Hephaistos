package com.poe2craft.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RulesetManifestLoaderTest {
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
