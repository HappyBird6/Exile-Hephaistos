package com.poe2craft.crafting.presentation;

import static com.poe2craft.crafting.application.chaosrepeat.ChaosRepeatProtocol.*;
import static com.poe2craft.crafting.application.chaosrepeat.RepeatTestWorkbench.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.application.chaosrepeat.*;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChaosRepeatControllerTest {
  private static final String URL = "/api/v1/crafting/workbench/chaos-repeat";
  private static final ChaosRepeatController CONTROLLER =
      new ChaosRepeatController(WORKBENCH, JSON);
  private final MockMvc mvc = mvc(CONTROLLER);

  private static MockMvc mvc(ChaosRepeatController controller) {
    return MockMvcBuilders.standaloneSetup(controller)
        .addFilters(
            new RulesetBoundary(
                org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json().build()))
        .build();
  }

  private ObjectNode request(boolean abyss) {
    var body = JSON.createObjectNode();
    body.put("requestId", ID);
    body.set("state", JSON.valueToTree(abyss ? new Abyss(abyss(true)) : new Concrete(rare())));
    body.put("action", "CHAOS");
    body.putArray("activeOmens");
    body.put("pattern", ".*");
    body.put("locale", "en");
    return body;
  }

  private JsonNode send(ObjectNode body, int status) throws Exception {
    var response =
        mvc.perform(
                post(URL)
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType("application/json")
                    .content(JSON.writeValueAsBytes(body)))
            .andExpect(status().is(status))
            .andExpect(header().string(RulesetBoundary.HEADER, RulesetBoundary.identity()))
            .andReturn()
            .getResponse();
    return JSON.readTree(response.getContentAsByteArray());
  }

  @Test
  void bothStateKindsReturnFlatStateFreeWitnessAndBindExactReceivedBytesWithDefault100()
      throws Exception {
    for (boolean abyss : List.of(false, true)) {
      var body = request(abyss);
      byte[] bytes = JSON.writeValueAsBytes(body);
      var result = send(body, 200);
      assertThat(result.path("requestBinding").path("requestSha256").asText())
          .isEqualTo(ChaosRepeatController.sha256(bytes));
      assertThat(result.path("requestBinding").path("requestId").asText()).isEqualTo(ID);
      assertThat(result.path("requestBinding").path("maximumUses").intValue()).isEqualTo(100);
      assertThat(result.path("requestBinding").path("matcherVersion").asText())
          .isEqualTo(ChaosRepeatPattern.VERSION);
      assertThat(result.path("actualUses").intValue()).isEqualTo(1);
      assertThat(result.path("termination").asText()).isEqualTo("MATCH");
      assertThat(result.path("lastAppliedWitness").path("kind").asText())
          .isEqualTo(abyss ? "ABYSS" : "CONCRETE");
      assertThat(result.path("lastAppliedWitness").has("state")).isFalse();
      assertThat(result.path("requestBinding").has("state")).isFalse();
      assertThat(result.has("before")).isFalse();
      assertThat(result.path("finalState").size()).isEqualTo(2);
    }
  }

  @Test
  void normalLimitAppliesAllUsesAndOneIsTheMinimum() throws Exception {
    for (int max : List.of(1, 1000)) {
      var body = request(false);
      body.put("maximumUses", max);
      body.put("pattern", "__never_matches__");
      var result = send(body, 200);
      assertThat(result.path("actualUses").intValue()).isEqualTo(max);
      assertThat(result.path("termination").asText()).isEqualTo("LIMIT");
    }
  }

  @Test
  void numericCoercionUnknownPropertiesEnumsAndDuplicatesAreRejected() throws Exception {
    for (var max :
        List.of(
            JSON.getNodeFactory().numberNode(0),
            JSON.getNodeFactory().numberNode(1001),
            JSON.getNodeFactory().numberNode(1.0),
            JSON.getNodeFactory().textNode("1"),
            JSON.nullNode())) {
      var body = request(false);
      body.set("maximumUses", max);
      send(body, 422);
    }
    var unknown = request(false);
    unknown.put("cancel", true);
    send(unknown, 422);
    unknown = request(false);
    ((ObjectNode) unknown.get("state").get("item")).put("unexpected", 1);
    send(unknown, 422);
    var enumNumber = request(false);
    enumNumber.put("action", 1);
    send(enumNumber, 422);
    var invalidState = request(false);
    ((ObjectNode) invalidState.get("state").get("item")).put("rarity", 2);
    send(invalidState, 422);
    var duplicateOmen = request(false);
    duplicateOmen.withArray("activeOmens").add("Omen_of_Whittling").add("Omen_of_Whittling");
    send(duplicateOmen, 422);
    var missing = request(false);
    missing.remove("requestId");
    send(missing, 422);
    var boolString = request(false);
    ((ObjectNode) boolString.get("state").get("item").get("explicits").get(0))
        .put("fractured", "true");
    send(boolString, 422);
    String duplicate =
        JSON.writeValueAsString(request(false))
            .replace("\"pattern\":", "\"pattern\":\"x\",\"pattern\":");
    mvc.perform(
            post(URL)
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType("application/json")
                .content(duplicate))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void invalidCanonicalStateSanctificationAndWrongSnapshotDoNotReachApply() throws Exception {
    var snapshot = request(false);
    ((ObjectNode) snapshot.get("state").get("item")).put("snapshotId", "other");
    send(snapshot, 422);
    var sanctified = request(false);
    ((ObjectNode) sanctified.get("state").get("item")).withArray("conditions").add("SANCTIFIED");
    send(sanctified, 422);
    var values = request(false);
    var numbers =
        (ObjectNode) values.get("state").get("item").get("explicits").get(0).get("values");
    numbers.put(numbers.fieldNames().next(), Long.MAX_VALUE);
    send(values, 422);
    var wrongKind = request(false);
    ((ObjectNode) wrongKind.get("state")).put("kind", "ABYSS");
    send(wrongKind, 422);
  }

  @Test
  void rulesetBoundaryStillRequiresMatchingIdentity() throws Exception {
    mvc.perform(
            post(URL)
                .contentType("application/json")
                .content(JSON.writeValueAsBytes(request(false))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_REQUIRED"));
    mvc.perform(
            post(URL)
                .header(RulesetBoundary.HEADER, "wrong")
                .contentType("application/json")
                .content(JSON.writeValueAsBytes(request(false))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("RULESET_IDENTITY_MISMATCH"));
  }

  @Test
  void byteLimitUsesActualStreamEvenWithNoContentLengthAndUtf8MustBeValid() throws Exception {
    byte[] valid = JSON.writeValueAsBytes(request(false));
    byte[] exact = Arrays.copyOf(valid, ChaosRepeatController.MAX_BODY_BYTES);
    Arrays.fill(exact, valid.length, exact.length, (byte) ' ');
    var request =
        new MockHttpServletRequest() {
          @Override
          public int getContentLength() {
            return -1;
          }

          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(exact);
    assertThat(CONTROLLER.repeat(request).getStatusCode().value()).isEqualTo(200);
    request.setContent(Arrays.copyOf(exact, exact.length + 1));
    assertThat(CONTROLLER.repeat(request).getStatusCode().value()).isEqualTo(413);
    request.setContent(new byte[] {'{', (byte) 0xc3, (byte) 0x28, '}'});
    assertThat(CONTROLLER.repeat(request).getStatusCode().value()).isEqualTo(400);
  }

  @Test
  void grammarErrorsExposeSafeUsefulCodesWithoutPatternOrTrace() throws Exception {
    for (String pattern : List.of("", "(?i)private_marker", "(a{1000}){1000}")) {
      var body = request(false);
      body.put("pattern", pattern);
      var result = send(body, 422);
      assertThat(result.path("detail").asText())
          .doesNotContain("private_marker", "PatternSyntaxException");
      assertThat(result.has("stackTrace")).isFalse();
      assertThat(result.has("partialOutcome")).isFalse();
    }
  }

  @Test
  void partialFailureAndBusyAdmissionKeepProblemDetailsAndRulesetHeader() throws Exception {
    var fake = mock(ChaosRepeatService.class);
    var completed =
        new ChaosRepeatService(WORKBENCH, DISPLAY, RulesetBoundary.identity())
            .run(
                input(new Concrete(rare()), WorkbenchCurrency.CHAOS, 1, Set.of(), ".*", "en"),
                new Random(42));
    var partial =
        new Outcome(
            1,
            "CHAOS_REPEAT",
            completed.rulesetIdentity(),
            completed.requestBinding(),
            completed.finalState(),
            completed.actualUses(),
            completed.consumedOmens(),
            completed.remainingOmens(),
            completed.lastAppliedWitness(),
            "ERROR",
            null,
            new Failure("REPEAT_EXECUTION_FAILED", "MATCHER"));
    when(fake.run(any(), any())).thenReturn(partial);
    var controlled = mvc(new ChaosRepeatController(WORKBENCH, JSON, fake));
    controlled
        .perform(
            post(URL)
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType("application/json")
                .content(JSON.writeValueAsBytes(request(false))))
        .andExpect(status().isInternalServerError())
        .andExpect(header().string(RulesetBoundary.HEADER, RulesetBoundary.identity()))
        .andExpect(jsonPath("$.code").value("REPEAT_EXECUTION_FAILED"))
        .andExpect(jsonPath("$.partialOutcome.actualUses").value(1))
        .andExpect(jsonPath("$.partialOutcome.lastAppliedWitness.state").doesNotExist());
    when(fake.run(any(), any()))
        .thenThrow(new RepeatRejected(429, "CHAOS_REPEAT_BUSY", "Another repeat is running."));
    controlled
        .perform(
            post(URL)
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType("application/json")
                .content(JSON.writeValueAsBytes(request(false))))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("CHAOS_REPEAT_BUSY"));
  }

  @Test
  void sharedOriginalUtf8FixtureHasIdenticalJavaDigest() throws Exception {
    var bytes = Files.readAllBytes(Path.of("../contracts/chaos-repeat/request-body.json"));
    var expected =
        Files.readString(Path.of("../contracts/chaos-repeat/request-body.sha256")).trim();
    assertThat(ChaosRepeatController.sha256(bytes)).isEqualTo(expected);
    assertThat(JSON.readTree(bytes).path("pattern").asText()).contains("生命", "España", "가-힣");
    var source = JSON.readTree(bytes);
    assertThat(
            ChaosRepeatController.sha256(
                (new String(bytes, StandardCharsets.UTF_8) + "\n")
                    .getBytes(StandardCharsets.UTF_8)))
        .isNotEqualTo(expected);
    assertThat(source.path("locale").asText()).isEqualTo("ko");
  }
}
