package com.poe2craft.workbench.display;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
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
 * Frontend·Option·Golden 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: The frontend fixture intentionally includes invalid game states. Never relax server
 * validation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code FIXTURES}: FIXTURES ({@code Path}).
 *   <li>{@code JSON}: 공통 JSON 변환기 ({@code ObjectMapper}).
 *   <li>{@code DISPLAY_PARITY}: 동일성 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code read(name)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code name}(이름). 반환: {@code JsonNode}. 사용하는
 *       예외: {@code Exception}.
 *   <li>{@code display()}: 표시 값을 조회·구성한다. 반환: {@code ChaosOptionDisplay}. 사용하는 예외: {@code
 *       Exception}.
 *   <li>{@code catalog(fixture, state)}: 카탈로그 처리에서 {@code state.path}, {@code fixture.path}을(를)
 *       사용한다. 입력: {@code fixture}(검증사례), {@code state}(상태). 반환: {@code ItemCatalog}.
 *   <li>{@code historicalInputProvenanceIsIntactWhileCurrentTemplatesUseTheActiveManifest()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code supportedGroupsMatchAndIneligibleFixturesAreExplicitlyRejected()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 반환: {@code List<DynamicTest>}. 사용하는 예외: {@code Exception}.
 * </ul>
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
          "catalyst-source-unit-scaling",
          "unbound-source-units");

  private static JsonNode read(String name) throws Exception {
    return JSON.readTree(FIXTURES.resolve(name).toFile());
  }

  private static ChaosOptionDisplay display() throws Exception {
    byte[] bytes =
        Files.readAllBytes(Path.of("../frontend/src/shared/i18n/modifierTemplates.json"));
    return new ChaosOptionDisplay(
        DisplayTemplates.parse(
            bytes, com.poe2craft.shared.item.ModifierTextResources.TEMPLATE_SHA256, JSON));
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
  void historicalInputProvenanceIsIntactWhileCurrentTemplatesUseTheActiveManifest()
      throws Exception {
    // sources.json records historical provenance, not a freeze on production source files.
    // Keep the original fixture immutable; current template integrity is enforced above
    // against the active manifest, while every golden below still checks rendered output.
    var sources = read("sources.json").path("files");
    for (var hashes = sources.elements(); hashes.hasNext(); )
      assertThat(hashes.next().asText()).matches("[a-f0-9]{64}");
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
                if (id.equals("sanctified-catalyst")) {
                  var item = JSON.treeToValue(state, ItemState.class);
                  var actual = display.format(item, catalog, locale);
                  // Valid under current implicit rules, but never eligible for Chaos matching.
                  assertThat(actual.supported()).isFalse();
                  assertThat(actual.groups()).isEmpty();
                  assertThat(actual.issues())
                      .containsExactly(
                          new ChaosOptionDisplay.Issue(
                              ChaosOptionDisplay.Code.SANCTIFIED_UNSUPPORTED, "", true));
                } else if (DISPLAY_PARITY.contains(id)) {
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
                  // coerced. Implicits no longer require sanctification factors.
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
    assertThat(tests).hasSize(19 * DisplayLocale.values().length);
    return tests;
  }
}
