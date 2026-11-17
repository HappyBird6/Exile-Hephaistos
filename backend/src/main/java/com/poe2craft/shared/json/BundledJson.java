package com.poe2craft.shared.json;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * 번들 설정에 공통으로 적용할 엄격한 JSON 파서 정책을 제공한다.
 *
 * <p>기존 계약: Shared strict parser policy for reviewed crafting configuration.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BundledJson()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code mapper()}: 변환기 값을 조회·구성한다. 반환: {@code ObjectMapper}.
 * </ul>
 */
public final class BundledJson {
  private BundledJson() {}

  public static ObjectMapper mapper() {
    var mapper =
        JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    for (var shape :
        java.util.List.of(
            CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean))
      mapper.coercionConfigFor(LogicalType.Textual).setCoercion(shape, CoercionAction.Fail);
    return mapper;
  }
}
