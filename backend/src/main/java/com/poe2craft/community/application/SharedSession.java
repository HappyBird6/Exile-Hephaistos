package com.poe2craft.community.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 개인 보관함 정보와 비공개 복사 원문을 제외한 공개 세션 값이다.
 *
 * <p>기존 계약: Public snapshots exclude private clipboard provenance and local library bookkeeping.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SharedSession()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code validate(input, maxBytes, maxFrames, chaosMaterial)}: 입력의 구조와 크기, 카탈로그에 등록된 카오스 기보를
 *       검사해 공개할 세션 사본을 반환한다.
 *   <li>{@code bad()}: bad의 입력에 따른 분기·결과를 처리한다. 사용하는 예외: {@code ResponseStatusException}.
 * </ul>
 */
public final class SharedSession {
  private SharedSession() {}

  public static JsonNode validate(
      JsonNode input, int maxBytes, int maxFrames, Predicate<String> chaosMaterial) {
    if (input == null
        || !input.isObject()
        || input.toString().getBytes(StandardCharsets.UTF_8).length > maxBytes) bad();
    if (!input.path("id").isTextual()
        || input.path("id").asText().length() > 160
        || !input.path("createdAt").isTextual()) bad();
    try {
      Instant.parse(input.path("createdAt").asText());
    } catch (RuntimeException e) {
      bad();
    }
    var frames = input.path("frames");
    if (!frames.isArray() || frames.isEmpty() || frames.size() > maxFrames) bad();
    ObjectNode out = ((ObjectNode) input).deepCopy();
    out.retain("id", "createdAt", "rulesetIdentity", "compatibilityId", "frames");
    String base = null;
    var publishedFrames = out.path("frames");
    for (int index = 0; index < publishedFrames.size(); index++) {
      var frame = publishedFrames.get(index);
      var state = frame.path("state");
      String current = state.path("baseItemId").asText();
      if (!state.isObject()
          || current.isBlank()
          || current.length() > 256
          || !Set.of("NORMAL", "MAGIC", "RARE", "UNIQUE").contains(state.path("rarity").asText())
          || !state.path("itemLevel").isIntegralNumber()
          || state.path("itemLevel").asInt() < 1
          || state.path("itemLevel").asInt() > 100
          || !state.path("implicits").isArray()
          || !state.path("explicits").isArray()
          || !state.path("conditions").isArray()) bad();
      if (base != null && !base.equals(current)) bad();
      base = current;
      if (!frame.has("action")
          || !(frame.path("action").isNull() || frame.path("action").isTextual())) bad();
      if (frame.has("projectedChance")) {
        var original = frame.get("projectedChance");
        publishChance(((ObjectNode) frame).putObject("projectedChance"), original);
      }
      if (frame.has("projectedOmenOutcomes"))
        publishOmenOutcomes((ObjectNode) frame, index, chaosMaterial);
    }
    if (input.has("projectedRouteChance"))
      publishChance(out.putObject("projectedRouteChance"), input.get("projectedRouteChance"));
    return out;
  }

  /** 절사의 배타적 세 결과가 같은 카오스 한 단계에서 발생하며 합계가 1일 수 있는지 보관함과 동일하게 검사한다. */
  private static void publishOmenOutcomes(
      ObjectNode frame, int index, Predicate<String> chaosMaterial) {
    var crafts = frame.path("projectedCurrencies");
    if (index == 0
        || !frame.path("action").asText().equals("SESSION_PLAN")
        || frame.hasNonNull("manual")
        || frame.hasNonNull("evidence")
        || !crafts.isArray()
        || crafts.size() != 1
        || !isWhittlingChaos(crafts.get(0), chaosMaterial)) bad();
    var original = frame.get("projectedOmenOutcomes");
    var published = frame.putObject("projectedOmenOutcomes");
    double lowerSum = 0, upperSum = 0;
    for (String outcome : List.of("success", "recoverable", "failure")) {
      var chance = published.putObject(outcome);
      publishChance(chance, original.path(outcome));
      var exact = chance.get("probability");
      var range = chance.get("probabilityBounds");
      if (exact.isNull() && range == null) bad();
      lowerSum += exact.isNull() ? range.get("lower").doubleValue() : exact.doubleValue();
      upperSum += exact.isNull() ? range.get("upper").doubleValue() : exact.doubleValue();
    }
    // 프런트와 같은 순서와 허용 오차로 IEEE 754로 전송된 세 값의 합을 비교한다.
    double tolerance = 2 * Math.ulp(1.0);
    if (lowerSum > 1 + tolerance || upperSum < 1 - tolerance) bad();
  }

  /** 기보의 재료 ID와 징조 순서를 읽어 일반 절사와 한쪽 말소를 곁들인 절사만 허용한다. */
  private static boolean isWhittlingChaos(JsonNode craft, Predicate<String> chaosMaterial) {
    var omens = craft.path("omenIds");
    if (!chaosMaterial.test(craft.path("currencyId").asText())
        || !omens.isArray()
        || omens.isEmpty()
        || omens.size() > 2
        || !validChaosCounts(craft)) return false;
    var matching = new ArrayList<WorkbenchOmen>(omens.size());
    try {
      for (var omen : omens) matching.add(WorkbenchOmen.fromId(omen.asText()));
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return matching.equals(List.of(WorkbenchOmen.WHITTLING))
        || WorkbenchOmen.sideWhittling(matching);
  }

  /** 횟수 필드가 붙은 기보는 보관함의 유한한 카오스 반복 및 기대 사용량 범위를 그대로 검사한다. */
  private static boolean validChaosCounts(JsonNode craft) {
    var max = craft.get("maxUses");
    if (max != null
        && (!max.isNumber()
            || max.asInt() != max.doubleValue()
            || max.asInt() < PublicContracts.REPEAT.minimumUses()
            || max.asInt() > PublicContracts.REPEAT.maximumUses())) return false;
    var perUse = craft.get("omensPerUse");
    if (perUse != null && (max == null || !perUse.isBoolean() || !perUse.booleanValue()))
      return false;
    var expected = craft.get("expectedUses");
    if (expected == null || expected.isNull()) return true;
    double count = expected.doubleValue();
    return expected.isNumber()
        && Double.isFinite(count)
        && count > 0
        && (count >= 1 || (max != null && max.asInt() > 1))
        && (max == null || count <= max.asInt());
  }

  /** 게시판 세션에는 경로와 절사 결과 각각의 확률·경계·훼손 가정만 복사한다. */
  private static void publishChance(ObjectNode published, JsonNode chance) {
    if (chance == null || !chance.isObject()) bad();
    var exact = chance.path("probability");
    if (!exact.isNull() && !unitProbability(exact)) bad();
    published.set("probability", exact);
    if (chance.has("probabilityBounds")) {
      var bounds = chance.get("probabilityBounds");
      var lower = bounds.path("lower");
      var upper = bounds.path("upper");
      if (!unitProbability(lower)
          || !unitProbability(upper)
          || lower.decimalValue().compareTo(upper.decimalValue()) > 0
          || (!exact.isNull()
              && (exact.decimalValue().compareTo(lower.decimalValue()) < 0
                  || exact.decimalValue().compareTo(upper.decimalValue()) > 0))) bad();
      var range = published.putObject("probabilityBounds");
      range.set("lower", lower);
      range.set("upper", upper);
    }
    if (chance.path("assumesEligibleReveal").isBoolean()
        && chance.path("assumesEligibleReveal").booleanValue())
      published.put("assumesEligibleReveal", true);
  }

  /** 공개하는 경로의 확률과 구간 끝점은 유한한 0~1 사이 수치만 허용한다. */
  private static boolean unitProbability(JsonNode value) {
    return value.isNumber()
        && Double.isFinite(value.doubleValue())
        && value.decimalValue().signum() >= 0
        && value.decimalValue().compareTo(java.math.BigDecimal.ONE) <= 0;
  }

  public static void bad() {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SHARED_SESSION");
  }
}
