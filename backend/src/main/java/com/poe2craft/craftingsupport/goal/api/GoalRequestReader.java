package com.poe2craft.craftingsupport.goal.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import java.util.List;

/** 목표 필터 API의 JSON을 검사한다. json은 엄격한 역직렬화 설정을 보관한다. decode는 요청 타입에 맞춰 변환하고 잘못된 아이템과 잘못된 본문을 구별한다. */
final class GoalRequestReader {
  private final ObjectMapper json;

  GoalRequestReader(ObjectMapper json) {

    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  <T> T decode(JsonNode body, Class<T> type) {
    if (body == null || !body.isObject()) throw new MalformedPayload();
    try {
      return json.treeToValue(body, type);
    } catch (ValueInstantiationException error) {
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error(
                  "INVALID_ITEM", "/item", "ItemState properties are invalid.")));
    } catch (JsonProcessingException error) {
      throw new MalformedPayload();
    }
  }
}
