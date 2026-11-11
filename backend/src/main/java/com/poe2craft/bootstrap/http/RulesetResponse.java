package com.poe2craft.bootstrap.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.api.CraftingCatalogController;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.api.WorkbenchController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Initial roots and craft results carry the same identity as their transport envelope. */
@RestControllerAdvice(
    assignableTypes = {CraftingCatalogController.class, WorkbenchController.class})
public final class RulesetResponse implements ResponseBodyAdvice<Object> {
  private final ObjectMapper json;
  private final org.springframework.beans.factory.ObjectProvider<
          com.poe2craft.shared.crafting.infrastructure.CatalogReferences>
      references;

  public RulesetResponse(
      ObjectMapper json,
      org.springframework.beans.factory.ObjectProvider<
              com.poe2craft.shared.crafting.infrastructure.CatalogReferences>
          references) {
    this.references = references;
    this.json = json;
  }

  @Override
  public boolean supports(
      MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
    return "initial".equals(method.getMethod().getName())
        || "apply".equals(method.getMethod().getName())
        || "installAugment".equals(method.getMethod().getName());
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter method,
      MediaType type,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    var params =
        org.springframework.web.util.UriComponentsBuilder.fromUri(request.getURI())
            .build()
            .getQueryParams();
    boolean initial = "initial".equals(method.getMethod().getName());
    boolean shared = "shared".equals(params.getFirst("catalog"));
    boolean separate = initial && (shared || "separate".equals(params.getFirst("catalog")));
    Object envelope = body;
    if (separate) {
      // Avoid serializing thousands of immutable definitions only to remove them afterwards.
      var fields = new java.util.LinkedHashMap<String, Object>();
      try {
        for (var field : body.getClass().getRecordComponents())
          if (!field.getName().equals("modifiers"))
            fields.put(field.getName(), field.getAccessor().invoke(body));
      } catch (ReflectiveOperationException invalid) {
        throw new IllegalStateException("Invalid initial envelope", invalid);
      }
      envelope = fields;
    }
    ObjectNode value = json.valueToTree(envelope);
    value.put("rulesetIdentity", RulesetBoundary.identity());
    if (initial) {
      response.getHeaders().setCacheControl("no-cache");
      if (separate) {
        var base = params.getFirst("base");
        if (shared && references.getIfAvailable() != null) {
          value.put(
              "catalogUrl",
              "/api/v1/crafting/catalogs/"
                  + RulesetBoundary.identity()
                  + "/shared/"
                  + references
                      .getObject()
                      .reference(
                          base == null ? "solar" : base,
                          "true".equals(params.getFirst("manualReveal"))));
          return value;
        }
        value.put(
            "catalogUrl",
            "/api/v1/crafting/catalogs/"
                + RulesetBoundary.identity()
                + "/"
                + (base == null ? "solar" : base)
                + "?manualReveal="
                + "true".equals(params.getFirst("manualReveal")));
      }
    }
    return value;
  }
}
