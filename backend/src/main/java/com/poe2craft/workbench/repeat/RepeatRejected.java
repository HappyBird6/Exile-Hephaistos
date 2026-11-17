package com.poe2craft.workbench.repeat;

/**
 * 내부 예외나 아이템 원문을 포함하지 않는 고정 공개 오류다.
 *
 * <p>기존 계약: Safe, fixed public errors. Never construct these details from an engine exception or
 * item text.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code status}: 현재 처리 상태 ({@code int}).
 *   <li>{@code code}: 코드 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RepeatRejected(status, code, detail)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code status}(상태),
 *       {@code code}(코드), {@code detail}(상세).
 *   <li>{@code status()}: 현재 진행 상태를 조회한다. 반환: {@code int}.
 *   <li>{@code code()}: {@code code} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code input()}: 입력 값을 조회·구성한다. 반환: {@code RepeatRejected}.
 * </ul>
 */
public final class RepeatRejected extends IllegalArgumentException {
  private final int status;
  private final String code;

  public RepeatRejected(int status, String code, String detail) {
    super(detail);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static RepeatRejected input() {
    return new RepeatRejected(
        422,
        "INVALID_CHAOS_REPEAT_REQUEST",
        "The Chaos repeat request contains an invalid item state or unsupported parameters.");
  }
}
