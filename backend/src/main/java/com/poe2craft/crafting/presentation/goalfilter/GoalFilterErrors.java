package com.poe2craft.crafting.presentation.goalfilter;

import com.poe2craft.crafting.application.goalfilter.GoalFilterService.InvalidGoal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = GoalFilterController.class)
public final class GoalFilterErrors {
  @ExceptionHandler(InvalidGoal.class)
  public ProblemDetail invalid(InvalidGoal error) {
    var result =
        problem(
            error.staleCatalog() ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY,
            error.staleCatalog() ? "CATALOG_VERSION_MISMATCH" : "INVALID_GOAL_FILTER",
            "Review the goal filter validation issues.");
    result.setProperty("issues", error.issues());
    return result;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    GoalFilterController.MalformedPayload.class
  })
  public ProblemDetail malformed() {
    return problem(
        HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The goal filter request could not be read.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail invalidItem() {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "INVALID_ITEM",
        "Provide a valid ItemState and goal filter.");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail unavailable() {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "GOAL_FILTER_UNAVAILABLE",
        "The goal filter service is unavailable.");
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    var result = ProblemDetail.forStatusAndDetail(status, detail);
    result.setTitle(status.getReasonPhrase());
    result.setProperty("code", code);
    return result;
  }
}
