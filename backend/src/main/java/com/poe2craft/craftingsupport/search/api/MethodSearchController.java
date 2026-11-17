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
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 제작 방법 단위의 탐색과 저장된 계산 결과 조회 API를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code MethodSearchService}).
 *   <li>{@code reader}: 읽기변환기 ({@code
 *       com.poe2craft.craftingsupport.search.application.SearchInputReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code MethodSearchController(service, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       service}(서비스), {@code json}(JSON).
 *   <li>{@code create(body, request, response)}: 새 작업 또는 값을 생성한다. 입력: {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code ResponseEntity<Snapshot>}.
 *   <li>{@code createIndexed(body, request, response)}: 색인된을(를) 생성한다. 입력: {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code
 *       ResponseEntity<IndexedMethodProtocol.Started>}.
 *   <li>{@code createCertified(body, request, response)}: 확률구간을(를) 생성한다. 입력: {@code body}(본문),
 *       {@code request}(요청), {@code response}(응답). 반환: {@code
 *       ResponseEntity<CertifiedMethodProtocol.Started>}.
 *   <li>{@code certified(id, request, response)}: 확률구간 처리에서 {@code service.getCertified}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       CertifiedMethodProtocol.Snapshot}.
 *   <li>{@code certifiedGraph(id, revision, stateOffset, methodOffset, request, response)}:
 *       확률구간·그래프 처리에서 {@code service.certifiedGraph}을(를) 사용한다. 입력: {@code id}(식별자), {@code
 *       revision}(리비전), {@code stateOffset}(상태·시작위치), {@code methodOffset}(제작방법·시작위치), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code CertifiedMethodProtocol.Graph}.
 *   <li>{@code certifiedOutcomes(id, revision, methodId, cycles, offset, request, response)}:
 *       확률구간·결과목록 처리에서 {@code service.certifiedOutcomes}을(를) 사용한다. 입력: {@code id}(식별자), {@code
 *       revision}(리비전), {@code methodId}(제작방법·식별자), {@code cycles}(주기횟수), {@code offset}(시작위치),
 *       {@code request}(요청), {@code response}(응답). 반환: {@code CertifiedMethodProtocol.Outcomes}.
 *   <li>{@code certifiedMap(id, revision, offset, request, response)}: 확률구간·맵 처리에서 {@code
 *       service.certifiedMap}을(를) 사용한다. 입력: {@code id}(식별자), {@code revision}(리비전), {@code
 *       offset}(시작위치), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       CertifiedMapProtocol.Graph}.
 *   <li>{@code certifiedView(id, request, response)}: 확률구간·조회표현 처리에서 {@code
 *       service.certifiedView}을(를) 사용한다. 입력: {@code id}(식별자), {@code request}(요청), {@code
 *       response}(응답). 반환: {@code CertifiedMapProtocol.View}.
 *   <li>{@code certifiedMapOutcomes(id, revision, methodId, cycles, offset, request, response)}:
 *       확률구간·맵·결과목록 처리에서 {@code service.certifiedMapOutcomes}을(를) 사용한다. 입력: {@code id}(식별자), {@code
 *       revision}(리비전), {@code methodId}(제작방법·식별자), {@code cycles}(주기횟수), {@code offset}(시작위치),
 *       {@code request}(요청), {@code response}(응답). 반환: {@code CertifiedMapProtocol.Distribution}.
 *   <li>{@code controlCertified(id, body, request, response)}: 작업제어·확률구간 처리에서 {@code
 *       service.mutateCertified}을(를) 사용한다. 입력: {@code id}(식별자), {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code CertifiedMethodProtocol.Snapshot}.
 *   <li>{@code bindCertified(id, body, request, response)}: 연결·확률구간 처리에서 {@code
 *       service.bindCertifiedEntry}을(를) 사용한다. 입력: {@code id}(식별자), {@code body}(본문), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code CertifiedMethodProtocol.Entry}.
 *   <li>{@code certifiedEntry(id, entryId, offset, request, response)}: 확률구간·항목 처리에서 {@code
 *       service.certifiedEntry}을(를) 사용한다. 입력: {@code id}(식별자), {@code entryId}(항목·식별자), {@code
 *       offset}(시작위치), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       CertifiedMethodProtocol.Entry}.
 *   <li>{@code advanceCertified(id, entryId, body, request, response)}: 진행·확률구간 처리에서 {@code
 *       service.advanceCertifiedEntry}을(를) 사용한다. 입력: {@code id}(식별자), {@code entryId}(항목·식별자),
 *       {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       CertifiedMethodProtocol.Entry}.
 *   <li>{@code classOutcomes(id, revision, methodId, cycles, cursor, request, response)}: 동치류·결과목록
 *       처리에서 {@code service.indexedOutcomes}을(를) 사용한다. 입력: {@code id}(식별자), {@code revision}(리비전),
 *       {@code methodId}(제작방법·식별자), {@code cycles}(주기횟수), {@code cursor}(진행커서), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code IndexedMethodProtocol.Outcomes}.
 *   <li>{@code bindEntry(id, body, request, response)}: 연결·항목 처리에서 {@code service.bindEntry}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환:
 *       {@code IndexedMethodProtocol.Entry}.
 *   <li>{@code entry(id, entryId, offset, request, response)}: 항목 처리에서 {@code service.entry}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code entryId}(항목·식별자), {@code offset}(시작위치), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code IndexedMethodProtocol.Entry}.
 *   <li>{@code advanceEntry(id, entryId, body, request, response)}: 진행·항목 처리에서 {@code
 *       service.advanceEntry}을(를) 사용한다. 입력: {@code id}(식별자), {@code entryId}(항목·식별자), {@code
 *       body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       IndexedMethodProtocol.Entry}.
 *   <li>{@code provenance(baseItemId)}: 출처이력 처리에서 {@code service.provenance}을(를) 사용한다. 입력: {@code
 *       baseItemId}(베이스·아이템·식별자). 반환: {@code BasicCurrencyState.Provenance}.
 *   <li>{@code get(id, request, response)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code Snapshot}.
 *   <li>{@code graph(id, revision, cursor, request, response)}: 그래프 처리에서 {@code service.graph}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code revision}(리비전), {@code cursor}(진행커서), {@code
 *       request}(요청), {@code response}(응답). 반환: {@code Graph}.
 *   <li>{@code distribution(id, revision, methodId, cycles, cursor, request, response)}: 상태 또는 실행의
 *       확률 분포를 조회한다. 입력: {@code id}(식별자), {@code revision}(리비전), {@code methodId}(제작방법·식별자), {@code
 *       cycles}(주기횟수), {@code cursor}(진행커서), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       Distribution}.
 *   <li>{@code cancel(id, body, request, response)}: 작업 취소를 반영하고 이후 실행을 막는다. 입력: {@code id}(식별자),
 *       {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code Snapshot}. 사용하는 예외:
 *       {@code Rejected}.
 *   <li>{@code resume(id, body, request, response)}: 보존한 진행 상태에서 계산을 이어간다. 입력: {@code id}(식별자),
 *       {@code body}(본문), {@code request}(요청), {@code response}(응답). 반환: {@code
 *       ResponseEntity<Snapshot>}. 사용하는 예외: {@code Rejected}.
 *   <li>{@code read(payload, type)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code payload}(저장내용), {@code
 *       type}(타입). 반환: {@code T}.
 *   <li>{@code owner(request, response)}: 요청 또는 작업의 소유자를 판정한다. 입력: {@code request}(요청), {@code
 *       response}(응답). 반환: {@code String}.
 * </ul>
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
        .body(service.create(owner(request, response), read(body, Create.class)));
  }

  @PostMapping("/indexed")
  public ResponseEntity<IndexedMethodProtocol.Started> createIndexed(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(service.createIndexed(owner(request, response), read(body, Create.class)));
  }

  @PostMapping("/certified")
  public ResponseEntity<CertifiedMethodProtocol.Started> createCertified(
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.accepted()
        .body(
            service.createCertified(
                owner(request, response), read(body, CertifiedMethodProtocol.Create.class)));
  }

  @GetMapping("/certified/{id}")
  public CertifiedMethodProtocol.Snapshot certified(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.getCertified(owner(request, response), id);
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
        owner(request, response), id, revision, stateOffset, methodOffset);
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
        owner(request, response), id, revision, methodId, cycles, offset);
  }

  @GetMapping("/certified/{id}/map")
  public CertifiedMapProtocol.Graph certifiedMap(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedMap(owner(request, response), id, revision, offset);
  }

  @GetMapping("/certified/{id}/view")
  public CertifiedMapProtocol.View certifiedView(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.certifiedView(owner(request, response), id);
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
        owner(request, response), id, revision, methodId, cycles, offset);
  }

  @PostMapping("/certified/{id}/control")
  public CertifiedMethodProtocol.Snapshot controlCertified(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.mutateCertified(owner(request, response), id, read(body, Mutation.class));
  }

  @PostMapping("/certified/{id}/entries")
  public CertifiedMethodProtocol.Entry bindCertified(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.bindCertifiedEntry(
        owner(request, response), id, read(body, IndexedMethodProtocol.Bind.class));
  }

  @GetMapping("/certified/{id}/entries/{entryId}")
  public CertifiedMethodProtocol.Entry certifiedEntry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.certifiedEntry(owner(request, response), id, entryId, offset);
  }

  @PostMapping("/certified/{id}/entries/{entryId}/advance")
  public CertifiedMethodProtocol.Entry advanceCertified(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.advanceCertifiedEntry(
        owner(request, response), id, entryId, read(body, IndexedMethodProtocol.Advance.class));
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
        owner(request, response), id, revision, methodId, cycles, cursor);
  }

  @PostMapping("/{id}/entries")
  public IndexedMethodProtocol.Entry bindEntry(
      @PathVariable String id,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.bindEntry(
        owner(request, response), id, read(body, IndexedMethodProtocol.Bind.class));
  }

  @GetMapping("/{id}/entries/{entryId}")
  public IndexedMethodProtocol.Entry entry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestParam(defaultValue = "0") int offset,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.entry(owner(request, response), id, entryId, offset);
  }

  @PostMapping("/{id}/entries/{entryId}/advance")
  public IndexedMethodProtocol.Entry advanceEntry(
      @PathVariable String id,
      @PathVariable String entryId,
      @RequestBody String body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.advanceEntry(
        owner(request, response), id, entryId, read(body, IndexedMethodProtocol.Advance.class));
  }

  @GetMapping("/provenance")
  public BasicCurrencyState.Provenance provenance(@RequestParam String baseItemId) {
    return service.provenance(baseItemId);
  }

  @GetMapping("/{id}")
  public Snapshot get(
      @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
    return service.get(owner(request, response), id);
  }

  @GetMapping("/{id}/graph")
  public Graph graph(
      @PathVariable String id,
      @RequestParam long revision,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    return service.graph(owner(request, response), id, revision, cursor);
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
    return service.distribution(owner(request, response), id, revision, methodId, cycles, cursor);
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

  private <T> T read(String payload, Class<T> type) {
    return reader.read(payload, type);
  }

  /** Anonymous browser scope, not authentication. Does not create an application HTTP session. */
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
            .maxAge(3600)
            .build()
            .toString());
    return id;
  }
}
