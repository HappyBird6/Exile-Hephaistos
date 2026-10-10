package com.poe2craft.crafting.presentation.pathsearch;

import com.poe2craft.crafting.application.goalfilter.GoalFilterService.InvalidGoal;
import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = PathSearchController.class)
public final class PathSearchErrors {
  public record Problem(String type, String title, int status, String code) {}

  @ExceptionHandler(Rejected.class)
  public ResponseEntity<Problem> rejected(Rejected error) {
    return problem(error.status(), error.code());
  }

  @ExceptionHandler(InvalidGoal.class)
  public ResponseEntity<Problem> goal(InvalidGoal error) {
    String code =
        error.staleCatalog()
            ? "CATALOG_VERSION_MISMATCH"
            : error.issues().stream().anyMatch(i -> i.code().equals("UNKNOWN_STAT"))
                ? "UNKNOWN_STAT"
                : "INVALID_GOAL_FILTER";
    return problem(error.staleCatalog() ? 409 : 422, code);
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public ResponseEntity<Problem> invalid() {
    return problem(422, "INVALID_REQUEST");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Problem> failed() {
    return problem(503, "SEARCH_UNAVAILABLE");
  }

  private static ResponseEntity<Problem> problem(int status, String code) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            new Problem("about:blank", HttpStatus.valueOf(status).getReasonPhrase(), status, code));
  }
}
