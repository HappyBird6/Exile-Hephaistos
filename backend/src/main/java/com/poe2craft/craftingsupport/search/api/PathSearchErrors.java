package com.poe2craft.craftingsupport.search.api;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService.InvalidGoal;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 탐색 작업의 입력·소유권·진행 오류를 공개 응답으로 변환한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code rejected(error)}: 거절된 처리에서 {@code error.status}, {@code error.code}을(를) 사용한다. 입력:
 *       {@code error}(오류). 반환: {@code ResponseEntity<Problem>}.
 *   <li>{@code goal(error)}: 목표 처리에서 {@code error.staleCatalog}, {@code error.issues}을(를) 사용한다. 입력:
 *       {@code error}(오류). 반환: {@code ResponseEntity<Problem>}.
 *   <li>{@code invalid()}: 잘못된 입력을 나타내는 오류 결과를 만든다. 반환: {@code ResponseEntity<Problem>}.
 *   <li>{@code failed()}: failed 값을 조회·구성한다. 반환: {@code ResponseEntity<Problem>}.
 *   <li>{@code problem(status, code)}: 오류상세 처리에서 {@code ResponseEntity.status}, {@code
 *       HttpStatus.valueOf}을(를) 사용한다. 입력: {@code status}(상태), {@code code}(코드). 반환: {@code
 *       ResponseEntity<Problem>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Problem}: {@code PathSearchErrors}에서 사용하는 오류상세의 불변 값이다.
 * </ul>
 */
@RestControllerAdvice(
    assignableTypes = {
      PathSearchController.class,
      MethodSearchController.class,
      AutomaticSearchController.class
    })
public final class PathSearchErrors {
  /**
   * {@code PathSearchErrors}에서 사용하는 오류상세의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code type}: 타입 ({@code String}).
   *   <li>{@code title}: 제목 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code int}).
   *   <li>{@code code}: 코드 ({@code String}).
   * </ul>
   */
  public record Problem(String type, String title, int status, String code) {}

  @ExceptionHandler(Rejected.class)
  public ResponseEntity<Problem> rejected(Rejected error) {
    var result = problem(error.status(), error.code());
    if (error.retryAfterSeconds() == null) return result;
    return ResponseEntity.status(error.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(error.retryAfterSeconds()))
        .body(result.getBody());
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
