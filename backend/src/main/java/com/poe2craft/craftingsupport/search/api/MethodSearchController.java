package com.poe2craft.craftingsupport.search.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.application.CertifiedMapProtocol;
import com.poe2craft.craftingsupport.search.application.CertifiedMethodProtocol;
import com.poe2craft.craftingsupport.search.application.IndexedMethodProtocol;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol.*;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 제작 방법 탐색의 HTTP 요청을 처리한다. reader는 입력을 읽고 service는 쿠키 소유자의 검색을 실행한다. create, createIndexed,
 * createCertified는 일반·인덱스·인증 검색을 시작한다. get, graph, distribution, provenance는 상태·그래프·분포·출처를, cancel과
 * resume은 중지·재개를 처리한다. classOutcomes는 분류별 결과를 조회한다. bindEntry, entry, advanceEntry는 인덱스 항목을
 * 연결·조회·진행한다. certified, certifiedGraph, certifiedOutcomes, certifiedMap, certifiedView,
 * certifiedMapOutcomes는 인증 검색의 상태·그래프·결과·지도·보기·지도별 결과를 조회한다. controlCertified는 실행을 제어하며
 * bindCertified, certifiedEntry, advanceCertified는 인증 항목을 연결·조회·진행한다.
 */
@RestController
@RequestMapping("/api/v2/crafting/method-searches")
public final class MethodSearchController {
  private final MethodSearchService service;
  private final com.poe2craft.craftingsupport.search.application.SearchInputReader reader;

  public MethodSearchController(MethodSearchService service, ObjectMapper json) {
    this.service = service;
    this.reader = new com.poe2craft.craftingsupport.search.application.SearchInputReader(json);
  }

  @PostMapping({"", "/"})
  public ResponseEntity<Snapshot> create(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.create(
                SearchClientCookie.METHOD.owner(request, response),
                reader.read(body, Create.class)));
  }

  @PostMapping("/indexed")
  public ResponseEntity<IndexedMethodProtocol.Started> createIndexed(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.createIndexed(
                SearchClientCookie.METHOD.owner(request, response),
                reader.read(body, Create.class)));
  }

  @PostMapping("/certified")
  public ResponseEntity<CertifiedMethodProtocol.Started> createCertified(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.createCertified(
                SearchClientCookie.METHOD.owner(request, response),
                reader.read(body, CertifiedMethodProtocol.Create.class)));
  }

  @GetMapping("/certified/{id}")
  public CertifiedMethodProtocol.Snapshot certified(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.getCertified(SearchClientCookie.METHOD.owner(request, response), id);
  }

  @GetMapping("/certified/{id}/graph")
  public CertifiedMethodProtocol.Graph certifiedGraph(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(defaultValue = "0") int stateOffset,
      @RequestParam(defaultValue = "0") int methodOffset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedGraph(
        SearchClientCookie.METHOD.owner(request, response),
        id,
        revision,
        stateOffset,
        methodOffset);
  }

  @GetMapping("/certified/{id}/outcomes")
  public CertifiedMethodProtocol.Outcomes certifiedOutcomes(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam String methodId,
      @RequestParam int cycles,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedOutcomes(
        SearchClientCookie.METHOD.owner(request, response), id, revision, methodId, cycles, offset);
  }

  @GetMapping("/certified/{id}/map")
  public CertifiedMapProtocol.Graph certifiedMap(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedMap(
        SearchClientCookie.METHOD.owner(request, response), id, revision, offset);
  }

  @GetMapping("/certified/{id}/view")
  public CertifiedMapProtocol.View certifiedView(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.certifiedView(SearchClientCookie.METHOD.owner(request, response), id);
  }

  @GetMapping("/certified/{id}/map-outcomes")
  public CertifiedMapProtocol.Distribution certifiedMapOutcomes(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam String methodId,
      @RequestParam int cycles,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedMapOutcomes(
        SearchClientCookie.METHOD.owner(request, response), id, revision, methodId, cycles, offset);
  }

  @PostMapping("/certified/{id}/control")
  public CertifiedMethodProtocol.Snapshot controlCertified(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.mutateCertified(
        SearchClientCookie.METHOD.owner(request, response), id, reader.read(body, Mutation.class));
  }

  @PostMapping("/certified/{id}/entries")
  public CertifiedMethodProtocol.Entry bindCertified(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.bindCertifiedEntry(
        SearchClientCookie.METHOD.owner(request, response),
        id,
        reader.read(body, IndexedMethodProtocol.Bind.class));
  }

  @GetMapping("/certified/{id}/entries/{entryId}")
  public CertifiedMethodProtocol.Entry certifiedEntry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedEntry(
        SearchClientCookie.METHOD.owner(request, response), id, entryId, offset);
  }

  @PostMapping("/certified/{id}/entries/{entryId}/advance")
  public CertifiedMethodProtocol.Entry advanceCertified(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.advanceCertifiedEntry(
        SearchClientCookie.METHOD.owner(request, response),
        id,
        entryId,
        reader.read(body, IndexedMethodProtocol.Advance.class));
  }

  @GetMapping("/{id}/class-outcomes")
  public IndexedMethodProtocol.Outcomes classOutcomes(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam String methodId,
      @RequestParam int cycles,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.indexedOutcomes(
        SearchClientCookie.METHOD.owner(request, response), id, revision, methodId, cycles, cursor);
  }

  @PostMapping("/{id}/entries")
  public IndexedMethodProtocol.Entry bindEntry(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.bindEntry(
        SearchClientCookie.METHOD.owner(request, response),
        id,
        reader.read(body, IndexedMethodProtocol.Bind.class));
  }

  @GetMapping("/{id}/entries/{entryId}")
  public IndexedMethodProtocol.Entry entry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.entry(SearchClientCookie.METHOD.owner(request, response), id, entryId, offset);
  }

  @PostMapping("/{id}/entries/{entryId}/advance")
  public IndexedMethodProtocol.Entry advanceEntry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.advanceEntry(
        SearchClientCookie.METHOD.owner(request, response),
        id,
        entryId,
        reader.read(body, IndexedMethodProtocol.Advance.class));
  }

  @GetMapping("/provenance")
  public BasicCurrencyState.Provenance provenance(@RequestParam String baseItemId) {
    return service.provenance(baseItemId);
  }

  @GetMapping("/{id}")
  public Snapshot get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(SearchClientCookie.METHOD.owner(request, response), id);
  }

  @GetMapping("/{id}/graph")
  public Graph graph(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.graph(SearchClientCookie.METHOD.owner(request, response), id, revision, cursor);
  }

  @GetMapping("/{id}/distribution")
  public Distribution distribution(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam String methodId,
      @RequestParam int cycles,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.distribution(
        SearchClientCookie.METHOD.owner(request, response), id, revision, methodId, cycles, cursor);
  }

  @PostMapping("/{id}/cancel")
  public Snapshot cancel(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var command = reader.read(body, Mutation.class);
    if (!"CANCEL".equals(command.operation())) throw new Rejected(422, "INVALID_REQUEST");
    return service.mutate(SearchClientCookie.METHOD.owner(request, response), id, command);
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
        .body(service.mutate(SearchClientCookie.METHOD.owner(request, response), id, command));
  }
}
