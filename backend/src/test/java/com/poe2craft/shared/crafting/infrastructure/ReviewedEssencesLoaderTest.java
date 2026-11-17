package com.poe2craft.shared.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import org.junit.jupiter.api.Test;

/**
 * Essences·Loader·검증의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code data()}: 데이터 값을 조회·구성한다. 반환: {@code ObjectNode}. 사용하는 예외: {@code IOException}.
 *   <li>{@code read(data)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code data}(데이터). 반환: {@code Object}. 사용하는
 *       예외: {@code IOException}.
 *   <li>{@code allTargetIdsAndTheirOrderRemainExact()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는
 *       예외: {@code Exception}.
 *   <li>{@code wrongShapesCoercedIdsUnknownFieldsAndMissingBasesAreRejected()}: 시나리오의 잘못된 입력·상태가
 *       {@code IOException}, {@code IllegalArgumentException}로 거절되는지 검증한다. 사용하는 예외: {@code
 *       Exception}.
 * </ul>
 */
class ReviewedEssencesLoaderTest {
  private final ObjectMapper mapper = new ObjectMapper();

  private ObjectNode data() throws IOException {
    return (ObjectNode)
        mapper.readTree(getClass().getResourceAsStream("/catalog/top-base-essences.json"));
  }

  private Object read(JsonNode data) throws IOException {
    return ReviewedEssencesLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(data)));
  }

  @Test
  void allTargetIdsAndTheirOrderRemainExact() throws Exception {
    assertEquals(data(), mapper.valueToTree(ReviewedEssencesLoader.load()));
  }

  @Test
  void wrongShapesCoercedIdsUnknownFieldsAndMissingBasesAreRejected() throws Exception {
    for (JsonNode invalid :
        java.util.List.of(mapper.createArrayNode(), mapper.getNodeFactory().textNode("bad")))
      assertThrows(IOException.class, () -> read(invalid));
    var wrong = data();
    ((ObjectNode) wrong.elements().next()).set("fixed", mapper.createArrayNode());
    assertThrows(IOException.class, () -> read(wrong));
    var scalar = data();
    var targets = (ObjectNode) scalar.elements().next().get("fixed");
    targets.set("ESSENCE_HASTE", mapper.createArrayNode().add(42));
    assertThrows(IOException.class, () -> read(scalar));
    var missing = data();
    missing.remove(missing.fieldNames().next());
    assertThrows(IllegalArgumentException.class, () -> read(missing));
    var typo = data();
    ((ObjectNode) typo.elements().next()).put("fixxed", true);
    assertThrows(IOException.class, () -> read(typo));
    assertThrows(
        IOException.class,
        () ->
            ReviewedEssencesLoader.read(
                new ByteArrayInputStream(
                    "{\"same\":{},\"same\":{}}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }
}
