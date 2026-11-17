package com.poe2craft.workbench.display;

/**
 * 사이트에서 지원하는 표시 언어를 구분하고 알 수 없는 언어는 거절한다.
 *
 * <p>기존 계약: Supported site locales; never silently substitute a locale.
 *
 * <p>필드·상수·열거값:
 *
 * <ul>
 *   <li>{@code EN}: EN 구분값.
 *   <li>{@code KO}: KO 구분값.
 *   <li>{@code ZH_CN}: CN 구분값.
 *   <li>{@code ZH_TW}: TW 구분값.
 *   <li>{@code JA}: JA 구분값.
 *   <li>{@code ES}: ES 구분값.
 *   <li>{@code RU}: RU 구분값.
 *   <li>{@code TH}: TH 구분값.
 *   <li>{@code code}: 코드 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code DisplayLocale(code)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code code}(코드).
 *   <li>{@code code()}: {@code code} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code fromCode(code)}: 코드을(를) 입력에서 변환한다. 입력: {@code code}(코드). 반환: {@code DisplayLocale}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public enum DisplayLocale {
  EN("en"),
  KO("ko"),
  ZH_CN("zh-CN"),
  ZH_TW("zh-TW"),
  JA("ja"),
  ES("es"),
  RU("ru"),
  TH("th");

  private final String code;

  DisplayLocale(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static DisplayLocale fromCode(String code) {
    for (var locale : values()) if (locale.code.equals(code)) return locale;
    throw new IllegalArgumentException("Unsupported display locale");
  }
}
