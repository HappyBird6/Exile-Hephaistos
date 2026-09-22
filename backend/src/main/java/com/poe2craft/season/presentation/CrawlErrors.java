package com.poe2craft.season.presentation;

import com.poe2craft.season.application.service.CrawlException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = CrawlController.class)
public class CrawlErrors {
  @ExceptionHandler(CrawlException.class)
  ProblemDetail domain(CrawlException e, HttpServletRequest request) {
    return problem(e.status(), e.getMessage(), request);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail malformed(Exception e, HttpServletRequest request) {
    return problem(400, "MALFORMED_REQUEST", request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e, HttpServletRequest request) {
    return problem(503, "CRAWL_STORAGE_UNAVAILABLE", request);
  }

  private ProblemDetail problem(int status, String code, HttpServletRequest request) {
    var p =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatusCode.valueOf(status), code);
    p.setInstance(URI.create(request.getRequestURI()));
    p.setProperty("code", code);
    p.setProperty("traceId", UUID.randomUUID().toString());
    return p;
  }
}
