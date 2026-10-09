package com.poe2craft.crafting.infrastructure.goalfilter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalDefinitionsLoaderTest {
  private final ObjectMapper mapper = new ObjectMapper();

  private ObjectNode data() throws IOException {
    return (ObjectNode)
        mapper.readTree(getClass().getResourceAsStream("/crafting/goalfilter/definitions-v1.json"));
  }

  private GoalDefinitions read(JsonNode data) throws IOException {
    return GoalDefinitionsLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(data)));
  }

  @Test
  void reviewedDataHasExactCountsAndRejectsInvalidDefinitions() throws Exception {
    var bundled = GoalDefinitionsLoader.load();
    assertEquals(14, bundled.stats().size());
    assertEquals(6, bundled.pseudos().size());
    var data = data();
    ((ArrayNode) data.get("stats")).add(data.get("stats").get(0).deepCopy());
    assertThrows(IOException.class, () -> read(data));
    for (String unit : List.of("source", "milliseconds", "")) {
      var invalid = data();
      ((ObjectNode) invalid.get("stats").get(0)).put("unit", unit);
      assertThrows(IOException.class, () -> read(invalid));
    }
    var reference = data();
    ((ArrayNode) reference.get("pseudos").get(0).get("sourceStatIds")).add("unknown");
    assertThrows(IOException.class, () -> read(reference));
    var version = data();
    version.put("schemaVersion", 2);
    assertThrows(IOException.class, () -> read(version));
    assertThrows(UnsupportedOperationException.class, () -> bundled.stats().clear());
    assertThrows(
        IOException.class,
        () ->
            GoalDefinitionsLoader.read(
                new ByteArrayInputStream(
                    "{\"schemaVersion\":1,\"schemaVersion\":2}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }

  @Test
  void addingReviewedStatAndPseudoNeedsOnlyDataAndNeverGrantsNumericSupport() throws Exception {
    var data = data();
    var added =
        mapper
            .createObjectNode()
            .put("sourceStatId", "test_reviewed_flat")
            .put("label", "Reviewed Test Stat")
            .put("unit", "flat");
    ((ArrayNode) data.get("stats")).add(added);
    var pseudo =
        mapper
            .createObjectNode()
            .put("id", "total_test")
            .put("label", "Total Test")
            .put("unit", "flat");
    pseudo.putArray("sourceStatIds").add("test_reviewed_flat").add("base_maximum_life");
    ((ArrayNode) data.get("pseudos")).add(pseudo);
    var definitions = read(data);
    assertEquals(15, definitions.directStats().size());
    assertEquals(7, definitions.pseudos().size());
    var original = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadBow();
    var modifiers = new ArrayList<>(original.modifiers().values());
    var old = modifiers.getFirst();
    modifiers.set(
        0,
        new com.poe2craft.item.ModifierDefinition(
            old.id(),
            old.name(),
            old.layer(),
            old.affixType(),
            old.familyIds(),
            old.requiredItemLevel(),
            old.weight(),
            old.tier(),
            old.text(),
            List.of(
                new com.poe2craft.item.ModifierDefinition.StatRange("test_reviewed_flat", 1, 10)),
            old.tags(),
            old.sourceUrl()));
    var item = new com.poe2craft.item.ItemCatalog(original.metadata(), original.base(), modifiers);
    var context = new GoalFilter.Context(item.metadata().snapshotId(), item.base().id(), 82);
    var index = new GoalCatalogIndex(List.of(item), definitions);
    var result =
        index.catalog(context).stats().stream()
            .filter(s -> s.statId().equals(GoalCatalogIndex.id("pseudo", "total_test")))
            .findFirst()
            .orElseThrow();
    assertEquals("flat", result.unit());
    // Bow has no base_maximum_life source: only the newly registered direct stat contributes.
    assertEquals(1, result.contributions().size());
    assertEquals(
        GoalCatalogIndex.id("explicit", "test_reviewed_flat"),
        result.contributions().getFirst().statId());
    var direct =
        index.catalog(context).stats().stream()
            .filter(s -> s.statId().equals(GoalCatalogIndex.id("explicit", "test_reviewed_flat")))
            .findFirst()
            .orElseThrow();
    assertEquals("Reviewed Test Stat", direct.label());
    assertEquals(GoalFilter.Capability.SUPPORTED, direct.support().evaluation());
    assertEquals(GoalFilter.Capability.UNSUPPORTED, direct.support().probability());
    assertNotEquals(
        new GoalCatalogIndex(List.of(item), GoalDefinitionsLoader.load()).version(),
        index.version());
  }

  @Test
  void baseScopeIsExplicitAndRejectsDuplicatesPathsUnknownFieldsAndVersions() throws Exception {
    var data =
        (ObjectNode)
            mapper.readTree(getClass().getResourceAsStream("/crafting/goalfilter/bases-v1.json"));
    assertEquals(
        17,
        BundledGoalCatalogs.readBases(new ByteArrayInputStream(mapper.writeValueAsBytes(data)))
            .size());
    ((ArrayNode) data.get("bases")).add(data.get("bases").get(0).deepCopy());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BundledGoalCatalogs.readBases(
                new ByteArrayInputStream(mapper.writeValueAsBytes(data))));
    assertThrows(IllegalArgumentException.class, () -> BundledGoalCatalogs.readBases(null));
  }
}
