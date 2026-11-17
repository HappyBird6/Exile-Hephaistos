package com.poe2craft.shared.item;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 아이템에 실제 붙은 옵션과 출처 단위의 수치·고정 여부를 보존한다.
 *
 * <p>기존 계약: Values use the source stat units, not numbers extracted from formatted display text.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
 *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code Map<String, Long>}).
 *   <li>{@code fractured}: 분열고정 ({@code boolean}).
 *   <li>{@code crafted}: 제작옵션 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ModifierInstance(modifierId, values)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       modifierId}(옵션·식별자), {@code values}(값목록).
 *   <li>{@code ModifierInstance(modifierId, values, fractured)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code modifierId}(옵션·식별자), {@code values}(값목록), {@code fractured}(분열고정).
 *   <li>{@code asCrafted()}: as·제작옵션 값을 조회·구성한다. 반환: {@code ModifierInstance}.
 *   <li>{@code ModifierInstance(modifierId, values, fractured, crafted)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code modifierId}(옵션·식별자), {@code values}(값목록), {@code fractured}(분열고정), {@code
 *       crafted}(제작옵션). 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public record ModifierInstance(
    String modifierId, Map<String, Long> values, boolean fractured, boolean crafted) {
  public ModifierInstance(String modifierId, Map<String, Long> values) {
    this(modifierId, values, false, false);
  }

  public ModifierInstance(String modifierId, Map<String, Long> values, boolean fractured) {
    this(modifierId, values, fractured, false);
  }

  public ModifierInstance asCrafted() {
    return new ModifierInstance(modifierId, values, fractured, true);
  }

  public ModifierInstance {
    if (modifierId == null || modifierId.isBlank()) {
      throw new IllegalArgumentException("Modifier ID is required");
    }
    values.forEach(
        (key, value) -> {
          if (key == null || key.isBlank() || value == null) {
            throw new IllegalArgumentException("Stat IDs and values are required");
          }
        });
    values = Collections.unmodifiableMap(new TreeMap<>(values));
  }
}
