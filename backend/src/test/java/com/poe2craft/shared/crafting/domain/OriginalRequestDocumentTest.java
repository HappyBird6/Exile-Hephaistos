package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 원본·요청·문서 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
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
 *   <li>{@code historicalDocumentsRetainTheirExactJsonShapeAndFieldOrder()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code retainedSourceIsImmutableAndNotPartOfExecutionIdentity()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code currentRequestShapeAndUnknownFieldRejectionRemainUnchanged()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class OriginalRequestDocumentTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  @Test
  void historicalDocumentsRetainTheirExactJsonShapeAndFieldOrder() throws Exception {
    for (var path :
        List.of(
            "/legacy-v8/complete.json",
            "/legacy-v10/quality30-disk.json",
            "/legacy-v10/chaos-disk.json")) {
      var source = json.readTree(getClass().getResourceAsStream(path)).path("request");
      var typed = json.treeToValue(source, MethodSearchProtocol.Create.class);
      var restored = typed.withOriginalDocument(source);
      assertThat(restored).isEqualTo(typed);
      assertThat(json.writeValueAsString(restored)).isEqualTo(json.writeValueAsString(source));
    }
  }

  @Test
  void retainedSourceIsImmutableAndNotPartOfExecutionIdentity() throws Exception {
    var request = new MethodSearchProtocol.Create(2, "original", null, null, List.of());
    var source = (ObjectNode) json.valueToTree(request);
    var retained = request.withOriginalDocument(source);
    source.put("clientRequestId", "changed");
    assertThat(retained).isEqualTo(request).hasSameHashCodeAs(request);
    var exported = (ObjectNode) retained.serializedDocument();
    exported.put("clientRequestId", "changed-again");
    assertThat(json.valueToTree(retained).path("clientRequestId").asText()).isEqualTo("original");
  }

  @Test
  void currentRequestShapeAndUnknownFieldRejectionRemainUnchanged() throws Exception {
    var request = new MethodSearchProtocol.Create(2, "current", null, null, List.of());
    var document = json.valueToTree(request);
    assertThat(document.size()).isEqualTo(5);
    assertThat(document.has("start")).isTrue();
    assertThat(document.get("start").isNull()).isTrue();
    assertThat(json.treeToValue(document, MethodSearchProtocol.Create.class)).isEqualTo(request);
    ((ObjectNode) document).put("originalDocument", "untrusted");
    assertThatThrownBy(() -> json.treeToValue(document, MethodSearchProtocol.Create.class))
        .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
  }
}
