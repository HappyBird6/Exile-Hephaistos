package com.poe2craft.crafting.presentation.pathsearch;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import com.poe2craft.crafting.application.pathsearch.PathSearchService;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/path-searches")
public final class PathSearchController {
  private final PathSearchService service;
  private final ObjectMapper json;

  public PathSearchController(PathSearchService service, ObjectMapper json) {
    this.service = service;
    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  @PostMapping({"", "/"})
  public ResponseEntity<Snapshot> create(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(service.create(owner(request, response), read(body, Create.class)));
  }

  @GetMapping("/{id}")
  public Snapshot get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(owner(request, response), id);
  }

  @GetMapping("/{id}/graph")
  public GraphPage graph(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.graph(owner(request, response), id, revision, cursor);
  }

  @PostMapping("/{id}/cancel")
  public Snapshot cancel(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var command = read(body, Mutation.class);
    if (!"CANCEL".equals(command.operation())) throw new Rejected(422, "INVALID_REQUEST");
    return service.mutate(owner(request, response), id, command);
  }

  @PostMapping("/{id}/resume")
  public ResponseEntity<Snapshot> resume(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var command = read(body, Mutation.class);
    if (!"RESUME".equals(command.operation())) throw new Rejected(422, "INVALID_REQUEST");
    return ResponseEntity.accepted().body(service.mutate(owner(request, response), id, command));
  }

  @PostMapping("/{id}/recoveries")
  public ResponseEntity<Snapshot> recover(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(service.recover(owner(request, response), id, read(body, Recover.class)));
  }

  private <T> T read(String payload, Class<T> type) {
    try {
      if (payload == null || payload.length() > 65536) throw new Rejected(422, "INVALID_REQUEST");
      var body = json.readTree(payload);
      if (body == null || !body.isObject()) throw new Rejected(422, "INVALID_REQUEST");
      if (body.has("observations")) {
        if (!body.get("observations").isArray()) throw new Rejected(422, "INVALID_OBSERVATIONS");
        for (var n : body.get("observations"))
          if (!n.isTextual()) throw new Rejected(422, "INVALID_OBSERVATIONS");
      }
      if (body.has("start")) safeIntegers(body.path("start").path("item"));
      if (body.has("expectedRevision")) safeIntegers(body.get("expectedRevision"));
      if (body.has("parentRevision")) safeIntegers(body.get("parentRevision"));
      return json.treeToValue(body, type);
    } catch (Rejected e) {
      throw e;
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
  }

  private static void safeIntegers(JsonNode node) {
    if (node.isIntegralNumber()
        && (node.bigIntegerValue().abs().compareTo(java.math.BigInteger.valueOf(9007199254740991L))
            > 0)) throw new Rejected(422, "UNSAFE_INTEGER");
    if (node.isContainerNode()) node.forEach(PathSearchController::safeIntegers);
  }

  /** Anonymous browser scope, not authentication. Does not create an application HTTP session. */
  private static String owner(HttpServletRequest request, HttpServletResponse response) {
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if (cookie.getName().equals("crafting_path_client")
            && cookie.getValue().matches("[0-9a-f-]{36}")) return cookie.getValue();
    var id = UUID.randomUUID().toString();
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("crafting_path_client", id)
            .httpOnly(true)
            .sameSite("Strict")
            .secure(request.isSecure())
            .path("/api/v1/crafting/path-searches")
            .maxAge(3600)
            .build()
            .toString());
    return id;
  }
}
