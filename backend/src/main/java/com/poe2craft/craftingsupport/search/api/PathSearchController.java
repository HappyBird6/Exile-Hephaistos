package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.*;
import com.poe2craft.craftingsupport.search.application.PathSearchService;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 행동 경로 탐색 작업의 생성·조회·계속·취소 API를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code PathSearchService}).
 *   <li>{@code reader}: 읽기변환기 ({@code
 *       com.poe2craft.craftingsupport.search.application.SearchInputReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PathSearchController(service, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code service}(서비스),
 *       {@code json}(JSON).
 *   <li>{@code create(body, request, response)}: 새 작업 또는 값을 생성한다. 입력: {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code ResponseEntity<Snapshot>}.
 *   <li>{@code get(id, request, response)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code Snapshot}.
 *   <li>{@code graph(id, revision, cursor, request, response)}: 그래프 처리에서 {@code service.graph}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code revision}(리비전), {@code cursor}(진행커서), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code GraphPage}.
 *   <li>{@code cancel(id, body, request, response)}: 작업 취소를 반영하고 이후 실행을 막는다. 입력: {@code id}(식별자),
 *       {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code Snapshot}. 사용하는 예외:
 *       {@code Rejected}.
 *   <li>{@code resume(id, body, request, response)}: 보존한 진행 상태에서 계산을 이어간다. 입력: {@code id}(식별자),
 *       {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       ResponseEntity<Snapshot>}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code recover(id, body, request, response)}: recover 처리에서 {@code ResponseEntity.accepted},
 *       {@code service.recover}을(를) 사용한다. 입력: {@code id}(식별자), {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code ResponseEntity<Snapshot>}.
 *   <li>{@code read(payload, type)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code payload}(저장내용), {@code
 *       type}(타입). 반환: {@code T}.
 *   <li>{@code owner(request, response)}: 요청 또는 작업의 소유자를 판정한다. 입력: {@code request}(요청), {@code
 *       response}(응답). 반환: {@code String}.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/path-searches")
public final class PathSearchController {
  private final PathSearchService service;
  private final com.poe2craft.craftingsupport.search.application.SearchInputReader reader;

  public PathSearchController(PathSearchService service, ObjectMapper json) {
    this.service = service;
    this.reader = new com.poe2craft.craftingsupport.search.application.SearchInputReader(json);
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
    return reader.read(payload, type);
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
