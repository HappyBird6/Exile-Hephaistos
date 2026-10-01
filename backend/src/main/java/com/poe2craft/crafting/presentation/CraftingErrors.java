package com.poe2craft.crafting.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(
    assignableTypes = {
      CraftingController.class,
      WorkbenchController.class,
      SupportController.class,
      SupportRecommendationController.class
    })
public final class CraftingErrors {
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail invalid() {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "INVALID_CRAFTING_REQUEST",
        "Provide a supported Solar Amulet state, snapshot and valid exploration limits.");
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail malformed() {
    return problem(
        HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The crafting request could not be read.");
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    var result = ProblemDetail.forStatusAndDetail(status, detail);
    result.setTitle(status.getReasonPhrase());
    result.setProperty("code", code);
    return result;
  }
}
