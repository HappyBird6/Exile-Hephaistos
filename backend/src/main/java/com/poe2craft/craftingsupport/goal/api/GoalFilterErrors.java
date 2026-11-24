package com.poe2craft.craftingsupport.goal.api;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService.InvalidGoal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 목표 필터 오류를 안정적인 공개 오류 코드로 변환한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code invalid(error)}: 잘못된 입력을 나타내는 오류 결과를 만든다. 입력: {@code error}(오류). 반환: {@code
 *       ProblemDetail}.
 *   <li>{@code malformed()}: 잘못된형식 값을 조회·구성한다. 반환: {@code ProblemDetail}.
 *   <li>{@code invalidItem()}: 유효하지않은·아이템 값을 조회·구성한다. 반환: {@code ProblemDetail}.
 *   <li>{@code unavailable()}: 사용불가 값을 조회·구성한다. 반환: {@code ProblemDetail}.
 *   <li>{@code problem(status, code, detail)}: 오류상세 처리에서 {@code ProblemDetail.forStatusAndDetail},
 *       {@code result.setTitle}을(를) 사용한다. 입력: {@code status}(상태), {@code code}(코드), {@code
 *       detail}(상세). 반환: {@code ProblemDetail}.
 * </ul>
 */
@RestControllerAdvice(assignableTypes = {GoalFilterController.class, TestRouteController.class})
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
    com.poe2craft.craftingsupport.goal.api.MalformedPayload.class
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
