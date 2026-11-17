package com.poe2craft.shared.item.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.poe2craft.shared.item.text.ItemModels.Item;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 아이템 복사 텍스트를 표시용 구조로 분석하는 HTTP API다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code ItemTextService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemTextController(service)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code service}(서비스).
 *   <li>{@code parse(request)}: 외부 표현을 내부 값으로 해석한다. 입력: {@code request}(요청). 반환: {@code Item}. 사용하는
 *       예외: {@code ItemTextTooLargeException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code ParseRequest}: {@code ItemTextController}에서 사용하는 Parse·요청의 불변 값이다.
 *   <li>{@code ItemTextTooLargeException}: {@code ItemTextController} 내부의
 *       아이템·텍스트·Too·Large·Exception 처리를 맡는다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/items")
public final class ItemTextController {
  private final ItemTextService service;

  public ItemTextController(ItemTextService service) {
    this.service = service;
  }

  @PostMapping("/parse")
  public Item parse(@RequestBody ParseRequest request) {
    if (request.text() != null
        && request.text().getBytes(StandardCharsets.UTF_8).length
            > ItemTextService.MAX_TEXT_BYTES) {
      throw new ItemTextTooLargeException();
    }
    return service.parseText(request.text());
  }

  /**
   * {@code ItemTextController}에서 사용하는 Parse·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code fromJson(body)}: JSON을(를) 입력에서 변환한다. 입력: {@code body}(본문). 반환: {@code
   *       ParseRequest}. 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record ParseRequest(String text) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ParseRequest fromJson(Map<String, Object> body) {
      Object text = body.get("text");
      if (text != null && !(text instanceof String)) {
        throw new IllegalArgumentException("text must be a string");
      }
      return new ParseRequest((String) text);
    }
  }

  /**
   * {@code ItemTextController} 내부의 아이템·텍스트·Too·Large·Exception 처리를 맡는다.
   *
   * <p>직접 선언한 필드·생성자·메서드는 없다.
   */
  static final class ItemTextTooLargeException extends RuntimeException {}
}
