package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class ItemCatalogTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();

  @Test
  void allBaseRowsMatchTheReviewedSourceWithoutInventingWeights() throws Exception {
    JsonNode raw;
    try (var input = resource("base.raw.json")) {
      raw = new ObjectMapper().readTree(input);
    }
    assertThat(raw).hasSize(209);
    assertThat(catalog.modifiers()).hasSize(216);
    assertThat(catalog.metadata().prefixCount()).isEqualTo(84);
    assertThat(catalog.metadata().suffixCount()).isEqualTo(131);
    assertThat(catalog.metadata().prefixWeight()).isEqualTo(72200);
    assertThat(catalog.metadata().suffixWeight()).isEqualTo(96656);
    var matched = new HashSet<String>();
    for (var row : raw) {
      var definition =
          catalog.modifiers().values().stream()
              .filter(d -> d.sourceUrl().equals(row.get("hover").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(matched.add(definition.id())).isTrue();
      assertThat(definition.layer()).isEqualTo(ModifierDefinition.Layer.EXPLICIT);
      assertThat(definition.name()).isEqualTo(row.get("Name").asText());
      assertThat(definition.requiredItemLevel()).isEqualTo(row.get("Level").asInt());
      assertThat(definition.weight()).isEqualTo(row.get("DropChance").asInt());
      assertThat(definition.affixType())
          .isEqualTo(
              row.get("ModGenerationTypeID").asInt() == 1
                  ? ModifierDefinition.AffixType.PREFIX
                  : ModifierDefinition.AffixType.SUFFIX);
      assertThat(definition.familyIds())
          .containsExactlyInAnyOrderElementsOf(strings(row.get("ModFamilyList")));
      assertThat(definition.tags())
          .containsExactlyInAnyOrderElementsOf(strings(row.get("fossil_no")));
      var levels =
          StreamSupport.stream(raw.spliterator(), false)
              .filter(
                  r ->
                      r.get("ModGenerationTypeID").equals(row.get("ModGenerationTypeID"))
                          && r.get("ModFamilyList").get(0).equals(row.get("ModFamilyList").get(0)))
              .map(r -> r.get("Level").asInt())
              .distinct()
              .sorted(java.util.Comparator.reverseOrder())
              .toList();
      assertThat(definition.tier()).isEqualTo(levels.indexOf(definition.requiredItemLevel()) + 1);
    }
    // PoE2DB currently publishes 1 for these rows. Never replace it with a guessed conventional
    // weight.
    assertThat(catalog.modifiers().get("amulet:suffix:of-prestidigitation").weight()).isEqualTo(1);
  }

  @Test
  void everyStatRangeMatchesItsSavedDetailAndKnownTierExamples() throws Exception {
    JsonNode details;
    try (var input = resource("details.raw.json")) {
      details = new ObjectMapper().readTree(input);
    }
    for (var row : details) {
      var definition = catalog.find(row.get("id").asText()).orElseThrow();
      var matcher =
          java.util.regex.Pattern.compile(
                  "<li>(.*?) <span class=\"badge bg-primary\">(-?\\d+) <span class=\"ndash\">—</span> (-?\\d+)</span>")
              .matcher(row.get("statsHtml").asText());
      var expected = new ArrayList<ModifierDefinition.StatRange>();
      while (matcher.find()) {
        expected.add(
            new ModifierDefinition.StatRange(
                matcher.group(1).replace(' ', '_'),
                Long.parseLong(matcher.group(2)),
                Long.parseLong(matcher.group(3))));
      }
      assertThat(definition.stats()).as(definition.id()).containsExactlyElementsOf(expected);
    }
    var brute = catalog.find("amulet:suffix:of-the-brute").orElseThrow();
    assertThat(brute.requiredItemLevel()).isEqualTo(1);
    assertThat(brute.weight()).isEqualTo(1000);
    assertThat(brute.stats())
        .containsExactly(new ModifierDefinition.StatRange("additional_strength", 5, 8));
    var implicit = catalog.find(SolarAmulet.IMPLICIT_ID).orElseThrow();
    assertThat(implicit.stats())
        .containsExactly(new ModifierDefinition.StatRange(SolarAmulet.SPIRIT_STAT, 10, 15));
  }

  @Test
  void changedSourcesAndMalformedCatalogsAreRejected() throws Exception {
    try (var data = resource("catalog.json");
        var details = resource("details.raw.json")) {
      assertThatThrownBy(() -> ItemCatalogLoader.load(data, bytes("[]"), details))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("checksum");
    }
    String json;
    try (var input = resource("catalog.json")) {
      json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    String missingWeight = json.replaceFirst("\"weight\": \\d+,", "");
    try (var raw = resource("base.raw.json");
        var details = resource("details.raw.json")) {
      assertThatThrownBy(() -> ItemCatalogLoader.load(bytes(missingWeight), raw, details))
          .isInstanceOf(java.io.IOException.class);
    }
    var definitions = new ArrayList<>(catalog.modifiers().values());
    definitions.add(definitions.getFirst());
    assertThatThrownBy(() -> new ItemCatalog(catalog.metadata(), catalog.base(), definitions))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate");
    definitions.removeLast();
    definitions.removeFirst();
    assertThatThrownBy(() -> new ItemCatalog(catalog.metadata(), catalog.base(), definitions))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("totals");
  }

  private static List<String> strings(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).toList();
  }

  private static InputStream resource(String name) {
    return ItemCatalogTest.class.getResourceAsStream("/catalog/solar-amulet/" + name);
  }

  private static InputStream bytes(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }
}
