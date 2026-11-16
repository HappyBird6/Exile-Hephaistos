package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.domain.AbyssStrategyPlanner;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/crafting/method-searches/automatic")
public final class AutomaticSearchController {
  private final AutomaticSearchService service;
  private final com.poe2craft.craftingsupport.search.application.AutomaticSearchInputReader reader;

  public AutomaticSearchController(AutomaticSearchService service, ObjectMapper json) {
    this.service = service;
    this.reader =
        new com.poe2craft.craftingsupport.search.application.AutomaticSearchInputReader(json);
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

  @GetMapping("/{id}/nodes/{node}/abyss")
  public java.util.List<AbyssStrategyPlanner.Strategy> abyssStrategies(
      @PathVariable String id,
      @PathVariable int node,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.abyssStrategies(owner(request, response), id, node);
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
    return reader.read(input, type);
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
