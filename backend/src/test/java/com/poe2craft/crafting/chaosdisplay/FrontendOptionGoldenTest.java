package com.poe2craft.crafting.chaosdisplay;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.chaosdisplay.*;
import com.poe2craft.item.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The frontend fixture intentionally includes invalid game states. Never relax server validation.
 */
class FrontendOptionGoldenTest {
  private static final Path FIXTURES = Path.of("src/test/resources/chaosdisplay/frontend");
  private static final ObjectMapper JSON =
      new ObjectMapper().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  private static final Set<String> DISPLAY_PARITY =
      Set.of(
          "compound-newline",
          "compound-template-newline",
          "revealed-and-hidden",
          "catalyst-known-and-unreviewed",
          "unbound-source-units");

  private static JsonNode read(String name) throws Exception {
    return JSON.readTree(FIXTURES.resolve(name).toFile());
  }

  private static ChaosOptionDisplay display() throws Exception {
    byte[] bytes =
        Files.readAllBytes(Path.of("../frontend/src/shared/i18n/modifierTemplates.json"));
    return new ChaosOptionDisplay(
        DisplayTemplates.parse(
            bytes,
            read("sources.json")
                .path("files")
                .path("frontend/src/shared/i18n/modifierTemplates.json")
                .textValue(),
            JSON));
  }

  private static ItemCatalog catalog(JsonNode fixture, JsonNode state) {
    var definitions = new ArrayList<ModifierDefinition>();
    String implicit =
        state.path("implicits").isEmpty()
            ? ""
            : state.path("implicits").get(0).path("modifierId").textValue();
    fixture
        .path("definitions")
        .elements()
        .forEachRemaining(
            row -> {
              var definition = JSON.convertValue(row, ModifierDefinition.class);
              if (definition.layer() == ModifierDefinition.Layer.EXPLICIT
                  || definition.id().equals(implicit)) definitions.add(definition);
            });
    var prefix =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .toList();
    var suffix =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .toList();
    // Catalog metadata is a display-test carrier, not a new game pool or probability model.
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            state.path("snapshotId").textValue(),
            "2026-11-04",
            "https://example.test/display-fixture",
            "TEST_ONLY",
            "0".repeat(64),
            "0".repeat(64),
            prefix.size(),
            suffix.size(),
            prefix.stream().mapToLong(ModifierDefinition::weight).sum(),
            suffix.stream().mapToLong(ModifierDefinition::weight).sum()),
        new ItemCatalog.BaseItem(
            state.path("baseItemId").textValue(),
            "Display fixture",
            "https://example.test/base",
            implicit,
            1,
            1,
            3,
            3),
        definitions);
  }

  @Test
  void originalDisplaySourceHashesRemainPinnedWithoutCopyingTheTemplateAsset() throws Exception {
    var sources = read("sources.json").path("files");
    for (String file :
        List.of(
            "frontend/src/features/crafting/CraftingPage.tsx",
            "frontend/src/features/crafting/ItemCard.tsx",
            "frontend/src/features/crafting/localizedModifiers.ts",
            "frontend/src/features/crafting/catalystQuality.ts",
            "frontend/src/features/crafting/numeric-display/attributeNumbers.ts",
            "frontend/src/shared/i18n/modifierTemplates.json")) {
      byte[] bytes = Files.readAllBytes(Path.of("..").resolve(file));
      assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
          .as(file)
          .isEqualTo(sources.path(file).textValue());
    }
    byte[] inputs = Files.readAllBytes(FIXTURES.resolve("inputs.json"));
    assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(inputs)))
        .isEqualTo(
            sources
                .path("frontend/src/features/crafting/chaos-repeat/display/goldens/inputs.json")
                .textValue());
  }

  @TestFactory
  List<DynamicTest> supportedGroupsMatchAndIneligibleFixturesAreExplicitlyRejected()
      throws Exception {
    var inputs = read("inputs.json");
    var display = display();
    List<DynamicTest> tests = new ArrayList<>();
    for (var expected : read("expected.json").path("cases")) {
      String id = expected.path("inputId").textValue();
      var source = new ArrayList<JsonNode>();
      inputs
          .path("cases")
          .forEach(
              c -> {
                if (c.path("id").asText().equals(id)) source.add(c);
              });
      assertThat(source).hasSize(1);
      JsonNode state = source.getFirst().path("state");
      var locale = DisplayLocale.fromCode(expected.path("locale").textValue());
      tests.add(
          DynamicTest.dynamicTest(
              id + " / " + locale.code(),
              () -> {
                var catalog = catalog(inputs, state);
                if (DISPLAY_PARITY.contains(id)) {
                  var item = JSON.treeToValue(state, ItemState.class);
                  String before = JSON.writeValueAsString(item);
                  var actual = display.format(item, catalog, locale);
                  var groups = expected.path("result").path("groups");
                  assertThat(actual.groups()).hasSize(groups.size());
                  for (int i = 0; i < groups.size(); i++) {
                    var group = actual.groups().get(i);
                    var oracle = groups.get(i);
                    assertThat(group.id()).isEqualTo(oracle.path("modifierId").textValue());
                    assertThat(group.affixType().name())
                        .isEqualTo(oracle.path("affixType").textValue());
                    assertThat(group.fractured())
                        .isEqualTo(oracle.path("fractured").booleanValue());
                    assertThat(group.text()).isEqualTo(oracle.path("text").textValue());
                    JsonNode actualLines = JSON.valueToTree(group.lines());
                    assertThat(actualLines).isEqualTo(oracle.path("lines"));
                  }
                  assertThat(JSON.writeValueAsString(item)).isEqualTo(before);
                  assertThat(actual.supported()).isEqualTo(!id.equals("unbound-source-units"));
                } else {
                  // Deliberately fractional/out-of-range rolls, missing stats/definitions, mixed
                  // invalid
                  // observations, or two crafted Jewel effects remain invalid rather than being
                  // coerced. sanctified-catalyst also omits required implicit factors despite
                  // having an implicit modifier, so canonical validation must reject it.
                  assertThatThrownBy(
                          () ->
                              display.format(
                                  JSON.treeToValue(state, ItemState.class), catalog, locale))
                      .isInstanceOfAny(
                          IllegalArgumentException.class,
                          com.fasterxml.jackson.core.JsonProcessingException.class);
                }
              }));
    }
    assertThat(tests).hasSize(114);
    return tests;
  }
}
