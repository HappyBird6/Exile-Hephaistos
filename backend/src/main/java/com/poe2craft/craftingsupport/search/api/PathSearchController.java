package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.*;
import com.poe2craft.craftingsupport.search.application.PathSearchService;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 경로 탐색 요청을 reader로 읽고 익명 검색 쿠키의 소유자와 함께 service에 전달한다. create는 검색 시작, get과 graph는 진행 상태와 그래프 조회,
 * cancel과 resume은 중지·재개, recover는 복구 경로 계산을 처리한다.
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
        .body(
            service.create(
                SearchClientCookie.PATH.owner(request, response), reader.read(body, Create.class)));
  }

  @GetMapping("/{id}")
  public Snapshot get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(SearchClientCookie.PATH.owner(request, response), id);
  }

  @GetMapping("/{id}/graph")
  public GraphPage graph(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.graph(SearchClientCookie.PATH.owner(request, response), id, revision, cursor);
  }

  @PostMapping("/{id}/cancel")
  public Snapshot cancel(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var command = reader.read(body, Mutation.class);
    if (!"CANCEL".equals(command.operation())) throw new Rejected(422, "INVALID_REQUEST");
    return service.mutate(SearchClientCookie.PATH.owner(request, response), id, command);
  }

  @PostMapping("/{id}/resume")
  public ResponseEntity<Snapshot> resume(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var command = reader.read(body, Mutation.class);
    if (!"RESUME".equals(command.operation())) throw new Rejected(422, "INVALID_REQUEST");
    return ResponseEntity.accepted()
        .body(service.mutate(SearchClientCookie.PATH.owner(request, response), id, command));
  }

  @PostMapping("/{id}/recoveries")
  public ResponseEntity<Snapshot> recover(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.recover(
                SearchClientCookie.PATH.owner(request, response),
                id,
                reader.read(body, Recover.class)));
  }
}
