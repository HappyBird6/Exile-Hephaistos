package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 자동 탐색의 요청 크기와 선택 정책 기본값을 공통 파서에 전달하는 호환 진입점이다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code reader}: 읽기변환기 ({@code SearchInputReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticSearchInputReader(json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON).
 *   <li>{@code read(input, type)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code input}(입력), {@code type}(타입).
 *       반환: {@code T}.
 * </ul>
 */
public final class AutomaticSearchInputReader {
  private final SearchInputReader reader;

  public AutomaticSearchInputReader(ObjectMapper json) {
    reader = new SearchInputReader(json);
  }

  public <T> T read(String input, Class<T> type) {
    return reader.automatic(input, type);
  }
}
