package com.poe2craft.crafting.presentation.pathsearch;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.application.pathsearch.*;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/crafting/method-searches/automatic")
public final class AutomaticSearchController {
  private final AutomaticSearchService service;
  private final ObjectMapper json;

  public AutomaticSearchController(AutomaticSearchService service, ObjectMapper json) {
    this.service = service;
    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  @PostMapping
  public ResponseEntity<AutomaticSearchService.View> create(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.create(
                owner(request, response), read(body, AutomaticSearchService.Input.class)));
  }

  @GetMapping("/{id}")
  public AutomaticSearchService.View get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(owner(request, response), id);
  }

  @GetMapping("/{id}/request")
  public AutomaticSearchService.Input original(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.originalInput(owner(request, response), id);
  }

  @PostMapping("/{id}/control")
  public AutomaticSearchService.View control(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.control(
        owner(request, response), id, read(body, AutomaticSearchService.Control.class));
  }

  private <T> T read(String input, Class<T> type) {
    try {
      if (input.length() > 262144) throw new IllegalArgumentException("Request too large");
      var node = json.readTree(input);
      safe(node);
      // Optional additions keep older clients valid; original required fields stay strict.
      if (type == AutomaticSearchService.Input.class
          && node instanceof com.fasterxml.jackson.databind.node.ObjectNode body) {
        if (!body.has("abstractQuality"))
          body.put("abstractQuality", body.path("abstractQuality40").asBoolean() ? 40 : 0);
        var general = body.path("request").path("goal").path("general");
        if (general instanceof com.fasterxml.jackson.databind.node.ObjectNode object
            && !object.has("quality")) object.putNull("quality");
      }
      return json.treeToValue(node, type);
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new PathSearchProtocol.Rejected(422, "INVALID_REQUEST");
    }
  }

  private static void safe(JsonNode node) {
    if (node == null) throw new IllegalArgumentException("Body required");
    if (node.isIntegralNumber()
        && node.bigIntegerValue().abs().compareTo(java.math.BigInteger.valueOf(9007199254740991L))
            > 0) throw new IllegalArgumentException("Unsafe integer");
    if (node.isContainerNode()) node.forEach(AutomaticSearchController::safe);
  }

  private static String owner(HttpServletRequest request, HttpServletResponse response) {
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if (cookie.getName().equals("crafting_method_client")
            && cookie.getValue().matches("[0-9a-f-]{36}")) return cookie.getValue();
    var id = UUID.randomUUID().toString();
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("crafting_method_client", id)
            .httpOnly(true)
            .sameSite("Strict")
            .secure(request.isSecure())
            .path("/api/v2/crafting/method-searches")
            .maxAge(86400)
            .build()
            .toString());
    return id;
  }
}
