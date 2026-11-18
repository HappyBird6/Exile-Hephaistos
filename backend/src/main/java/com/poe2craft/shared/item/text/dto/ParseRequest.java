package com.poe2craft.shared.item.text.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;

/** 아이템 텍스트 파싱 요청이다. text에 게임에서 복사한 원문을 담는다. */
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
