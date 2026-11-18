package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.application.ModifierReferenceService;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 기존 클라이언트 응답을 유지하면서 표시용 옵션 정의 차분을 제공한다.
 *
 * <p>기존 계약: Optional, presentation-only delta. Older clients retain their original response
 * contract.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code references}: 참조목록 ({@code
 *       org.springframework.beans.factory.ObjectProvider<ModifierReferenceService>}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ModifierPoolResponse(references, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       references}(참조목록), {@code json}(JSON).
 *   <li>{@code supports(method, converter)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code method}(제작방법), {@code
 *       converter}(변환기). 반환: {@code boolean}.
 *   <li>{@code beforeBodyWrite(body, method, type, converter, request, response)}: 응답 본문과 헤더에 전송
 *       계약을 적용한다. 입력: {@code body}(본문), {@code method}(제작방법), {@code type}(타입), {@code
 *       converter}(변환기), {@code request}(요청), {@code response}(응답). 반환: {@code Object}.
 * </ul>
 */
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {WorkbenchController.class, CustomExaltController.class})
public final class ModifierPoolResponse implements ResponseBodyAdvice<Object> {
  private final org.springframework.beans.factory.ObjectProvider<ModifierReferenceService>
      references;
  private final ObjectMapper json;

  public ModifierPoolResponse(
      org.springframework.beans.factory.ObjectProvider<ModifierReferenceService> references,
      ObjectMapper json) {
    this.references = references;
    this.json = json;
  }

  @Override
  public boolean supports(
      MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter method,
      MediaType type,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    String previous = request.getHeaders().getFirst("X-Modifier-Pool-Revision");
    if (previous == null || previous.length() > 64) return body;
    var state =
        body instanceof WorkbenchSimulator.Result result
            ? result.state()
            : body instanceof AbyssCurrencyExecutor.Result result
                ? result.state().storedItem()
                : body instanceof com.poe2craft.workbench.application.AugmentResult result
                    ? result.state()
                    : body instanceof com.poe2craft.shared.crafting.domain.AbyssCraftingState result
                        ? result.storedItem()
                        : body
                                instanceof
                                com.poe2craft.workbench.application.CustomExaltService.Applied
                                    result
                            ? result.result().state()
                            : null;
    if (state == null || references.getIfAvailable() == null) return body;
    ObjectNode value = json.valueToTree(body);
    value.put("rulesetIdentity", RulesetBoundary.identity());
    // Reference failures must never turn a completed random craft into a retryable error.
    try {
      value.set(
          "modifierPool",
          json.valueToTree(
              references
                  .getObject()
                  .update(
                      previous, state, request.getHeaders().getFirst("X-Modifier-Pool-Baseline"))));
    } catch (RuntimeException unavailable) {
      value.putNull("modifierPool");
    }
    return value;
  }
}
