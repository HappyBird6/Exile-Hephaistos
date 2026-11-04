package com.poe2craft.crafting.application.chaosdisplay;

/** The six existing site locales; never silently substitute a locale. */
public enum DisplayLocale {
  EN("en"),
  KO("ko"),
  ZH_CN("zh-CN"),
  ZH_TW("zh-TW"),
  JA("ja"),
  ES("es");

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
