package com.poe2craft.craftingsupport.path.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.poe2craft.craftingsupport.path.application.BasicPathService;
import java.io.IOException;

/** 기본 경로 계산 요청을 검사한다. reader는 엄격한 Jackson 설정을 공유하며 read는 관측값을 정수로 정규화한다. */
final class BasicPathRequestReader {
  private final ObjectReader reader;

  BasicPathRequestReader(ObjectMapper mapper) {

    reader =
        mapper
            .copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .readerFor(BasicPathService.Request.class)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  BasicPathService.Request read(JsonNode body) {
    if (body == null || !body.isObject())
      throw new IllegalArgumentException("Explicit path request required");
    try {
      var normalized = body.deepCopy();
      var observations = normalized.get("observations");
      if (observations != null && observations.isArray()) {
        var array = (com.fasterxml.jackson.databind.node.ArrayNode) observations;
        for (int i = 0; i < array.size(); i++) {
          var value = array.get(i);
          if (value.isTextual()) {
            if (!value.textValue().matches("0|[1-9][0-9]*"))
              throw new IllegalArgumentException("Exact nonnegative decimal observations required");
            array.set(
                i,
                com.fasterxml.jackson.databind.node.LongNode.valueOf(
                    Long.parseLong(value.textValue())));
          } else if (!value.isIntegralNumber() || !value.canConvertToLong())
            throw new IllegalArgumentException("Exact integer observations required");
        }
      }
      return reader.readValue(normalized);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid or unsupported path request fields");
    }
  }
}
