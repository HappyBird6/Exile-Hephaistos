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

/** Optional, presentation-only delta. Older clients retain their original response contract. */
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
                : body instanceof WorkbenchController.AugmentResult result
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
