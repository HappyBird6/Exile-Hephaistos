package com.poe2craft.workbench.repeat;

import static com.poe2craft.configuration.PublicContracts.REPEAT;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * 카오스 반복 요청의 원문 검증과 중복 실행 방지를 담당한다. MAX_BODY_BYTES는 허용 바이트 수이며 json과 states는 본문과 아이템 상태를 읽는다.
 * service와 random은 제작을 실행하고 requests는 요청별 결과를 보관한다. submit은 바이트 수와 UTF-8을 검사한 뒤 sha256으로 원문을 식별한다.
 * input, fields, text는 필드와 타입을 검증한다. cancel은 요청 식별자를 확인하고 취소를 전달한다.
 */
public final class ChaosRepeatSubmission {
  public static final int MAX_BODY_BYTES = REPEAT.maximumBodyBytes();
  private final ObjectMapper json;
  private final WorkbenchStateReader states;
  private final ChaosRepeatService service;
  private final RepeatRequestStore requests;
  private final SecureRandom random = new SecureRandom();

  public ChaosRepeatSubmission(
      CraftingOperations workbench,
      ObjectMapper mapper,
      ChaosRepeatService supplied,
      RepeatRequestStore requests) {
    this.requests = requests;
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

  public JsonNode submit(InputStream stream) throws IOException {
    // Content-Length is untrusted/optional: enforce the limit on bytes actually read, including
    // chunks.
    byte[] bytes = stream.readNBytes(MAX_BODY_BYTES + 1);
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
    return outcome;
  }

  public boolean cancel(String id) {
    if (!UUID.fromString(id).toString().equalsIgnoreCase(id)) return false;
    if (requests != null) requests.cancel(id);
    return true;
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
}
