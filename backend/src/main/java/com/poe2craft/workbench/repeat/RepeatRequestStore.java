package com.poe2craft.workbench.repeat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * 요청 ID를 정확한 원문 바이트와 규칙에 묶는 중복 실행 방지·취소 계약이다.
 *
 * <p>기존 계약: Deduplication and cancellation boundary. A request ID binds to exact bytes and one
 * ruleset.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code execute(id, digest, operation)}: execute의 실행 계약을 정의한다. 입력: {@code id}(식별자), {@code
 *       digest}(다이제스트), {@code operation}(연산). 반환: {@code JsonNode}.
 *   <li>{@code cancel(id)}: 작업 취소를 반영하고 이후 실행을 막는다. 입력: {@code id}(식별자).
 * </ul>
 */
public interface RepeatRequestStore {
  JsonNode execute(String id, String digest, Function<BooleanSupplier, JsonNode> operation);

  void cancel(String id);
}
