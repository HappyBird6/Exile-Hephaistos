package com.poe2craft.shared.item.text;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 아이템 텍스트 분석 오류를 원문 노출 없는 공개 응답으로 바꾼다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code tooLarge(request)}: too·Large의 입력에 따른 분기·결과를 처리한다. 입력: {@code request}(요청). 반환:
 *       {@code ProblemDetail}.
 *   <li>{@code invalid(request)}: 잘못된 입력을 나타내는 오류 결과를 만든다. 입력: {@code request}(요청). 반환: {@code
 *       ProblemDetail}.
 *   <li>{@code malformed(request)}: 잘못된형식의 입력에 따른 분기·결과를 처리한다. 입력: {@code request}(요청). 반환: {@code
 *       ProblemDetail}.
 *   <li>{@code problem(status, code, detail, request)}: 오류상세 처리에서 {@code
 *       ProblemDetail.forStatusAndDetail}, {@code problem.setTitle}을(를) 사용한다. 입력: {@code
 *       status}(상태), {@code code}(코드), {@code detail}(상세), {@code request}(요청). 반환: {@code
 *       ProblemDetail}.
 * </ul>
 */
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
