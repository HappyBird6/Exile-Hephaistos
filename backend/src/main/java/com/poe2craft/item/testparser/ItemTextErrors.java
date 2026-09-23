package com.poe2craft.item.testparser;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ItemTextController.class)
public final class ItemTextErrors {
  @ExceptionHandler(ItemTextController.ItemTextTooLargeException.class)
  public ProblemDetail tooLarge(HttpServletRequest request) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "ITEM_TEXT_TOO_LARGE",
        "Item text exceeds the 16 KiB UTF-8 limit.",
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail invalid(HttpServletRequest request) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "INVALID_ITEM_TEXT",
        "Provide English item text with rarity and an item name.",
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail malformed(HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "Provide a JSON object containing a text string.",
        request);
  }

  private static ProblemDetail problem(
      HttpStatus status, String code, String detail, HttpServletRequest request) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    problem.setProperty("traceId", UUID.randomUUID().toString());
    return problem;
  }
}
