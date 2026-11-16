package com.poe2craft.craftingsupport.rag.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.rag.application.*;
import com.poe2craft.craftingsupport.search.application.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Public test route; paid work requires the explicit feature switch and server-side call budget.
 */
@RestController
@RequestMapping("/api/support-test/rag")
public final class RagComparisonController {
  private final RagComparisonService service;
  private final AutomaticSearchInputReader reader;

  public RagComparisonController(RagComparisonService service, ObjectMapper json) {
    this.service = service;
    this.reader = new AutomaticSearchInputReader(json);
  }

  @GetMapping
  public ResponseEntity<RagComparisonService.Availability> status() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.availability());
  }

  @PostMapping("/compare")
  public ResponseEntity<RagComparisonService.Report> compare(@RequestBody String body) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.compare(reader.read(body, RagComparisonService.Input.class)));
  }

  @ExceptionHandler(RagFailure.class)
  public ResponseEntity<ProblemDetail> failed(RagFailure error) {
    return problem(error.status(), error.getMessage());
  }

  @ExceptionHandler(PathSearchProtocol.Rejected.class)
  public ResponseEntity<ProblemDetail> rejected() {
    return problem(422, "RAG_INVALID_INPUT");
  }

  @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
  public ResponseEntity<ProblemDetail> invalid() {
    return problem(422, "RAG_INVALID_INPUT");
  }

  private ResponseEntity<ProblemDetail> problem(int status, String code) {
    var body = ProblemDetail.forStatus(status);
    body.setProperty("code", code);
    return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
  }
}
