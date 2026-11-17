package com.poe2craft.workbench.api;

import static com.poe2craft.configuration.PublicContracts.REPEAT;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.application.WorkbenchStateReader;
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
 * 원문 바이트에 묶인 중복 실행 방지 계약으로 카오스 반복 요청을 처리한다.
 *
 * <p>기존 계약: A separate batch transport; existing single-apply validation and response advice stay
 * unchanged.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code MAX_BODY_BYTES}: 바이트수 ({@code int}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code states}: 상태목록 ({@code WorkbenchStateReader}).
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code ChaosRepeatService}).
 *   <li>{@code requests}: 요청목록 ({@code com.poe2craft.workbench.repeat.RepeatRequestStore}).
 *   <li>{@code random}: 호출 순서대로 소비하는 난수 공급자 ({@code SecureRandom}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosRepeatController(workbench, mapper)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       workbench}(작업대), {@code mapper}(변환기).
 *   <li>{@code ChaosRepeatController(workbench, mapper, settings, requests)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code workbench}(작업대), {@code mapper}(변환기), {@code settings}(운영설정), {@code
 *       requests}(요청목록).
 *   <li>{@code ChaosRepeatController(workbench, mapper, supplied)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       workbench}(작업대), {@code mapper}(변환기), {@code supplied}(supplied).
 *   <li>{@code repeat(request)}: 반복 처리에서 {@code request.getInputStream}, {@code
 *       UTF_8.newDecoder}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code ResponseEntity<?>}. 사용하는
 *       예외: {@code RepeatRejected}.
 *   <li>{@code cancel(id)}: 작업 취소를 반영하고 이후 실행을 막는다. 입력: {@code id}(식별자). 반환: {@code
 *       ResponseEntity<Void>}.
 *   <li>{@code input(body, digest)}: 입력 처리에서 {@code body.get}, {@code requestId.length}을(를) 사용한다.
 *       입력: {@code body}(본문), {@code digest}(다이제스트). 반환: {@code Input}.
 *   <li>{@code fields(node, allowed)}: 필드목록 처리에서 {@code node.isObject}, {@code
 *       RepeatRejected.input}을(를) 사용한다. 입력: {@code node}(노드), {@code allowed}(허용된).
 *   <li>{@code text(node)}: 텍스트 처리에서 {@code node.isTextual}, {@code RepeatRejected.input}을(를) 사용한다.
 *       입력: {@code node}(노드). 반환: {@code String}.
 *   <li>{@code sha256(bytes)}: SHA·256 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code bytes}(바이트수). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code problem(status, code, detail)}: 오류상세 처리에서 {@code ProblemDetail.forStatusAndDetail},
 *       {@code HttpStatusCode.valueOf}을(를) 사용한다. 입력: {@code status}(상태), {@code code}(코드), {@code
 *       detail}(상세). 반환: {@code ProblemDetail}.
 * </ul>
 */
@RestController
public final class ChaosRepeatController {
  public static final int MAX_BODY_BYTES = REPEAT.maximumBodyBytes();
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
            "Chaos repeat requests allow at most " + MAX_BODY_BYTES + " UTF-8 bytes.");
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
    int limit = REPEAT.defaultUses();
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
