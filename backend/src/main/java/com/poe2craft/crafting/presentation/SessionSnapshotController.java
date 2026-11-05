package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.SessionSnapshots;
import com.poe2craft.crafting.infrastructure.LocalSessionSnapshots;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session-snapshots")
public final class SessionSnapshotController {
  private final SessionSnapshots.Repository repository;
  private final ObjectMapper json;

  public SessionSnapshotController(
      ObjectMapper json,
      @Value("${crafting.session-snapshot-directory:./data/session-snapshots-v1}")
          String directory) {
    this.json = json;
    repository = new LocalSessionSnapshots(Path.of(directory));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> create(HttpServletRequest request) throws IOException {
    byte[] bytes = request.getInputStream().readNBytes(SessionSnapshots.MAX_BYTES + 1);
    if (bytes.length > SessionSnapshots.MAX_BYTES)
      return ResponseEntity.status(413).body(ProblemDetail.forStatus(413));
    SessionSnapshots.validate(
        json.reader()
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(bytes));
    String id = repository.create(bytes);
    return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(Map.of("id", id));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> read(@PathVariable String id) throws IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(repository.read(id));
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
