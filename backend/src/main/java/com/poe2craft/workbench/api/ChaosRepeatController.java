package com.poe2craft.workbench.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.repeat.ChaosDisplayResources;
import com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import com.poe2craft.workbench.repeat.ChaosRepeatService;
import com.poe2craft.workbench.repeat.RepeatRejected;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * A separate batch transport; existing single-apply validation and response advice stay unchanged.
 */
@RestController
public final class ChaosRepeatController {
  public static final int MAX_BODY_BYTES = 65536;
  private final ObjectMapper json;
  private final WorkbenchStateReader states;
  private final ChaosRepeatService service;
  private com.poe2craft.workbench.repeat.RepeatRequestStore requests;
  private final SecureRandom random = new SecureRandom();

  public ChaosRepeatController(CraftingOperations workbench, ObjectMapper mapper) {
    this(workbench, mapper, (ChaosRepeatService) null);
  }

  @Autowired
  public ChaosRepeatController(
      CraftingOperations workbench,
      ObjectMapper mapper,
      BackendSettings settings,
      com.poe2craft.workbench.repeat.RepeatRequestStore requests) {
    this(
        workbench,
        mapper,
        new ChaosRepeatService(
            workbench,
            new ChaosDisplayResources(mapper),
            RulesetBoundary.identity(),
            settings.workbench()));
    this.requests = requests;
  }

  ChaosRepeatController(
      CraftingOperations workbench, ObjectMapper mapper, ChaosRepeatService supplied) {
    json =
        mapper
            .copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    states = new WorkbenchStateReader(workbench, json);
    service =
        supplied != null
            ? supplied
            : new ChaosRepeatService(
                workbench, new ChaosDisplayResources(json), RulesetBoundary.identity());
  }

  @PostMapping(
      value = "/api/v1/crafting/workbench/chaos-repeat",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> repeat(HttpServletRequest request) {
    try {
      // Content-Length is untrusted/optional: enforce the limit on bytes actually read, including
      // chunks.
      byte[] bytes = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
      if (bytes.length > MAX_BODY_BYTES)
        throw new RepeatRejected(
            413,
            "CHAOS_REPEAT_BODY_TOO_LARGE",
            "Chaos repeat requests allow at most 65536 UTF-8 bytes.");
      StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes));
      var body = json.readTree(bytes);
      var input = input(body, sha256(bytes));
      JsonNode outcome =
          requests == null
              ? json.valueToTree(service.run(input, random))
              : requests.execute(
                  input.requestId(),
                  input.requestSha256(),
                  stop -> json.valueToTree(service.run(input, random, stop)));
      if (outcome.has("rejectedStatus"))
        throw new RepeatRejected(
            outcome.path("rejectedStatus").asInt(),
            outcome.path("rejectedCode").asText(),
            outcome.path("rejectedDetail").asText());
      if (!outcome.path("failure").isNull()) {
        var problem =
            problem(
                500,
                "REPEAT_EXECUTION_FAILED",
                "Chaos repeat stopped after an internal failure. The verified completed portion is"
                    + " included.");
        problem.setProperty("partialOutcome", outcome);
        return ResponseEntity.status(500)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
      }
      return ResponseEntity.ok(outcome);
    } catch (RepeatRejected rejected) {
      return ResponseEntity.status(rejected.status())
          .header("Retry-After", "1")
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem(rejected.status(), rejected.code(), rejected.getMessage()));
    } catch (IOException malformed) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem(400, "MALFORMED_REQUEST", "Provide one valid UTF-8 JSON request."));
    } catch (IllegalArgumentException invalid) {
      var rejected = RepeatRejected.input();
      return ResponseEntity.unprocessableEntity()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem(422, rejected.code(), rejected.getMessage()));
    } catch (RuntimeException unavailable) {
      return ResponseEntity.internalServerError()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem(500, "REPEAT_EXECUTION_FAILED", "Chaos repeat could not be completed."));
    }
  }

  @PostMapping("/api/v1/crafting/workbench/chaos-repeat/{id}/cancel")
  public ResponseEntity<Void> cancel(@PathVariable String id) {
    if (!UUID.fromString(id).toString().equalsIgnoreCase(id))
      return ResponseEntity.badRequest().build();
    if (requests != null) requests.cancel(id);
    return ResponseEntity.noContent().build();
  }

  private Input input(JsonNode body, String digest) {
    fields(
        body,
        Set.of("requestId", "state", "action", "activeOmens", "pattern", "locale", "maximumUses"));
    String requestId = text(body.get("requestId"));
    if (requestId.length() != 36
        || !UUID.fromString(requestId).toString().equalsIgnoreCase(requestId))
      throw RepeatRejected.input();
    String kind = text(body.path("state").get("kind"));
    State state;
    if (kind.equals("CONCRETE")) {
      fields(body.get("state"), Set.of("kind", "item"));
      state = new Concrete(states.concreteState(body.path("state").get("item")));
    } else if (kind.equals("ABYSS")) {
      fields(body.get("state"), Set.of("kind", "abyss"));
      state = new Abyss(states.abyssState(body.path("state").get("abyss")));
    } else throw RepeatRejected.input();
    var omensNode = body.get("activeOmens");
    if (omensNode == null || !omensNode.isArray()) throw RepeatRejected.input();
    var omens = new HashSet<String>();
    for (var omen : omensNode) if (!omens.add(text(omen))) throw RepeatRejected.input();
    var maximum = body.get("maximumUses");
    int limit = 1000;
    if (maximum != null) {
      if (!maximum.isIntegralNumber() || !maximum.canConvertToInt()) throw RepeatRejected.input();
      limit = maximum.intValue();
    }
    return new Input(
        requestId,
        digest,
        state,
        WorkbenchCurrency.valueOf(text(body.get("action"))),
        omens,
        text(body.get("pattern")),
        text(body.get("locale")),
        limit);
  }

  private static void fields(JsonNode node, Set<String> allowed) {
    if (node == null || !node.isObject()) throw RepeatRejected.input();
    node.fieldNames()
        .forEachRemaining(
            name -> {
              if (!allowed.contains(name)) throw RepeatRejected.input();
            });
  }

  private static String text(JsonNode node) {
    if (node == null || !node.isTextual()) throw RepeatRejected.input();
    return node.textValue();
  }

  public static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("SHA-256 unavailable");
    }
  }

  private static ProblemDetail problem(int status, String code, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    problem.setTitle(HttpStatus.valueOf(status).getReasonPhrase());
    problem.setProperty("code", code);
    return problem;
  }
}
