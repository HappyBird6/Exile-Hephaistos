package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import java.util.List;

/**
 * 탐색 API의 JSON 타입·안전 정수·선택 상태 기본값을 공통 처리한다. 원본 문자열은 보관하지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SearchInputReader(source)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code source}(출처).
 *   <li>{@code read(input, type)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code input}(입력), {@code type}(타입).
 *       반환: {@code T}.
 *   <li>{@code automatic(input, type)}: 자동 탐색의 크기·선택 정책 기본값을 적용해 입력을 읽는다. 입력: {@code input}(입력),
 *       {@code type}(타입). 반환: {@code T}.
 *   <li>{@code read(input, type, automatic)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code input}(입력), {@code
 *       type}(타입), {@code automatic}(자동). 반환: {@code T}. 사용하는 예외: {@code Rejected}, {@code
 *       IllegalArgumentException}.
 *   <li>{@code safeIntegers(node, code)}: 입력 수치가 클라이언트와 서버의 안전 정수 범위인지 확인한다. 입력: {@code node}(노드),
 *       {@code code}(코드). 사용하는 예외: {@code Rejected}.
 * </ul>
 */
public final class SearchInputReader {
  private final ObjectMapper json;

  public SearchInputReader(ObjectMapper source) {
    json =
        source
            .copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  public <T> T read(String input, Class<T> type) {
    return read(input, type, false);
  }

  public <T> T automatic(String input, Class<T> type) {
    return read(input, type, true);
  }

  private <T> T read(String input, Class<T> type, boolean automatic) {
    try {
      if (input == null || input.length() > (automatic ? 262144 : 65536))
        throw new Rejected(422, "INVALID_REQUEST");
      var node = json.readTree(input);
      if (!(node instanceof ObjectNode body)) throw new Rejected(422, "INVALID_REQUEST");
      if (!automatic && body.has("observations")) {
        if (!body.path("observations").isArray()) throw new Rejected(422, "INVALID_OBSERVATIONS");
        for (var value : body.path("observations"))
          if (!value.isTextual()) throw new Rejected(422, "INVALID_OBSERVATIONS");
      }
      if (automatic) safeIntegers(body, "INVALID_REQUEST");
      else {
        safeIntegers(body.path("start").path("item"), "UNSAFE_INTEGER");
        safeIntegers(body.path("request").path("start").path("item"), "UNSAFE_INTEGER");
        for (var field :
            List.of("expectedRevision", "expectedGeneration", "revision", "parentRevision"))
          safeIntegers(body.path(field), "UNSAFE_INTEGER");
      }
      ItemStateWireDefaults.normalizeOwned(body);
      if (type == AutomaticSearchService.Input.class) {
        if (!body.has("rareOutcomePolicy"))
          body.set("rareOutcomePolicy", json.valueToTree(RareOutcomePolicy.disabled()));
        if (body.path("rareOutcomePolicy").isNull()) throw new Rejected(422, "INVALID_REQUEST");
        if (!body.has("abstractQuality"))
          body.put("abstractQuality", body.path("abstractQuality40").asBoolean() ? 40 : 0);
      }
      for (var general :
          List.of(
              body.path("goal").path("general"), body.path("request").path("goal").path("general")))
        if (general instanceof ObjectNode object && !object.has("quality"))
          object.putNull("quality");
      for (var item :
          List.of(
              body.path("start").path("item"), body.path("request").path("start").path("item"))) {
        if (!(item instanceof ObjectNode object)) continue;
        if (!object.has("desecrated")) object.putArray("desecrated");
        if (object.hasNonNull("installedAugments")
            && (!object.path("installedAugments").isArray()
                || !object.path("installedAugments").isEmpty()))
          throw new IllegalArgumentException("Installed Augments require the manual workbench");
        if (!object.has("installedAugments")) object.putArray("installedAugments");
        for (var field : List.of("equipmentQuality", "observations", "sanctification")) {
          if (object.hasNonNull(field))
            throw new IllegalArgumentException(
                "Item property requires the manual workbench: " + field);
          if (!object.has(field)) object.putNull(field);
        }
      }
      return json.treeToValue(body, type);
    } catch (Rejected error) {
      throw error;
    } catch (java.io.IOException | IllegalArgumentException error) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
  }

  private static void safeIntegers(JsonNode node, String code) {
    if (node.isIntegralNumber()
        && node.bigIntegerValue().abs().compareTo(java.math.BigInteger.valueOf(9007199254740991L))
            > 0) throw new Rejected(422, code);
    if (node.isContainerNode()) node.forEach(child -> safeIntegers(child, code));
  }
}
