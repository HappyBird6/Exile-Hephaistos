package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Initial roots and craft results carry the same identity as their transport envelope. */
@RestControllerAdvice(assignableTypes = {CraftingController.class, WorkbenchController.class})
public final class RulesetResponse implements ResponseBodyAdvice<Object> {
  private final ObjectMapper json;

  public RulesetResponse(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public boolean supports(
      MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
    return "initial".equals(method.getMethod().getName())
        || "apply".equals(method.getMethod().getName());
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter method,
      MediaType type,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    ObjectNode value = json.valueToTree(body);
    value.put("rulesetIdentity", RulesetBoundary.identity());
    return value;
  }
}
