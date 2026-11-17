package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.domain.AbyssStrategyPlanner;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 자동 제작 탐색의 생성·조회·계속·취소 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code AutomaticSearchService}).
 *   <li>{@code reader}: 읽기변환기 ({@code
 *       com.poe2craft.craftingsupport.search.application.AutomaticSearchInputReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticSearchController(service, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       service}(서비스), {@code json}(JSON).
 *   <li>{@code create(body, request, response)}: 새 작업 또는 값을 생성한다. 입력: {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code
 *       ResponseEntity<AutomaticSearchService.View>}.
 *   <li>{@code get(id, request, response)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code AutomaticSearchService.View}.
 *   <li>{@code abyssStrategies(id, node, request, response)}: 훼손·전략목록 처리에서 {@code
 *       service.abyssStrategies}을(를) 사용한다. 입력: {@code id}(식별자), {@code node}(노드), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code
 *       java.util.List<AbyssStrategyPlanner.Strategy>}.
 *   <li>{@code original(id, request, response)}: 원본 처리에서 {@code service.originalInput}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       AutomaticSearchService.Input}.
 *   <li>{@code control(id, body, request, response)}: 작업제어 처리에서 {@code service.control}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환:
 *       {@code AutomaticSearchService.View}.
 *   <li>{@code read(input, type)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code input}(입력), {@code type}(타입).
 *       반환: {@code T}.
 *   <li>{@code owner(request, response)}: 요청 또는 작업의 소유자를 판정한다. 입력: {@code request}(요청), {@code
 *       response}(응답). 반환: {@code String}.
 * </ul>
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
