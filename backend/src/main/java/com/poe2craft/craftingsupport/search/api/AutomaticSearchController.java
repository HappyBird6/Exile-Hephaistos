package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.domain.AbyssStrategyPlanner;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 자동 탐색 HTTP 요청을 처리한다. reader는 생성·제어 요청을 읽고 service는 쿠키 소유자의 탐색을 실행한다. create는 시작, get은 진행 상태,
 * abyssStrategies는 훼손 후보, original은 원래 입력을 조회하며 control은 실행을 제어한다.
 */
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
                SearchClientCookie.AUTOMATIC.owner(request, response),
                reader.read(body, AutomaticSearchService.Input.class)));
  }

  @GetMapping("/{id}")
  public AutomaticSearchService.View get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(SearchClientCookie.AUTOMATIC.owner(request, response), id);
  }

  @GetMapping("/{id}/nodes/{node}/abyss")
  public java.util.List<AbyssStrategyPlanner.Strategy> abyssStrategies(
      @PathVariable String id,
      @PathVariable int node,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.abyssStrategies(SearchClientCookie.AUTOMATIC.owner(request, response), id, node);
  }

  @GetMapping("/{id}/request")
  public AutomaticSearchService.Input original(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.originalInput(SearchClientCookie.AUTOMATIC.owner(request, response), id);
  }

  @PostMapping("/{id}/control")
  public AutomaticSearchService.View control(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.control(
        SearchClientCookie.AUTOMATIC.owner(request, response),
        id,
        reader.read(body, AutomaticSearchService.Control.class));
  }
}
