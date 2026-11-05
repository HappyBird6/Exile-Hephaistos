package com.poe2craft.workbench.history.api;

import com.poe2craft.workbench.history.application.SessionSnapshotService;
import com.poe2craft.workbench.history.application.SessionSnapshotValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session-snapshots")
public final class SessionSnapshotController {
  private final SessionSnapshotService snapshots;

  public SessionSnapshotController(SessionSnapshotService snapshots) {
    this.snapshots = snapshots;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> create(HttpServletRequest request) throws IOException {
    byte[] bytes = request.getInputStream().readNBytes(SessionSnapshotValidator.MAX_BYTES + 1);
    if (bytes.length > SessionSnapshotValidator.MAX_BYTES)
      return ResponseEntity.status(413).body(ProblemDetail.forStatus(413));
    String id = snapshots.create(bytes);
    return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(Map.of("id", id));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> read(@PathVariable String id) throws IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(snapshots.read(id));
  }

  @RestControllerAdvice(assignableTypes = SessionSnapshotController.class)
  public static final class Errors {
    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<ProblemDetail> missing() {
      return problem(404);
    }

    @ExceptionHandler({
      IllegalArgumentException.class,
      com.fasterxml.jackson.core.JsonProcessingException.class
    })
    public ResponseEntity<ProblemDetail> invalid() {
      return problem(422);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unavailable() {
      return problem(503);
    }

    private ResponseEntity<ProblemDetail> problem(int status) {
      return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
    }
  }
}
