package com.poe2craft.workbench.display;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Port of attributeNumbers.ts, deliberately independent of host Java locale/CLDR data. */
public final class DisplayNumbers {
  private DisplayNumbers() {}

  public static String number(double value, DisplayLocale locale) {
    if (!Double.isFinite(value))
      throw new IllegalArgumentException("Finite display value required");
    if (Math.abs(value) < 0.05) value = 0;
    return decimal(BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP), locale);
  }

  private static String decimal(BigDecimal value, DisplayLocale locale) {
    String plain = value.abs().stripTrailingZeros().toPlainString();
    String[] parts = plain.split("\\.", -1);
    return (value.signum() < 0 ? "-" : "")
        + integer(parts[0], locale)
        + (parts.length > 1 ? decimalSeparator(locale) + parts[1] : "");
  }

  static String integer(String digits, DisplayLocale locale) {
    // Intl es groups from five digits; ru uses a non-breaking space from four digits.
    if (digits.length() < (locale == DisplayLocale.ES ? 5 : 4)) return digits;
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) result.append(groupSeparator(locale));
      result.append(digits.charAt(i));
    }
    return result.toString();
  }

  public static String text(String text, DisplayLocale source, DisplayLocale output) {
    String decimal = decimalSeparator(source);
    String group = groupSeparator(source);
    Pattern pattern =
        Pattern.compile(
            "(?<![\\p{L}\\p{N}_])([+-]?)([0-9]+(?:"
                + Pattern.quote(group)
                + "[0-9]{3})*)"
                + Pattern.quote(decimal)
                + "([0-9]+)(?![0-9])");
    Matcher matcher = pattern.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String fraction = matcher.group(3);
      BigInteger tenths =
          new BigInteger(matcher.group(2).replace(group, ""))
              .multiply(BigInteger.TEN)
              .add(BigInteger.valueOf(fraction.charAt(0) - '0'))
              .add(BigInteger.valueOf(fraction.length() > 1 && fraction.charAt(1) >= '5' ? 1 : 0));
      BigInteger[] parts = tenths.divideAndRemainder(BigInteger.TEN);
      String sign = matcher.group(1);
      if (sign.equals("-") && tenths.signum() == 0) sign = "";
      String replacement =
          sign
              + integer(parts[0].toString(), output)
              + (parts[1].signum() == 0 ? "" : decimalSeparator(output) + parts[1]);
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    return matcher.appendTail(result).toString();
  }

  private static String decimalSeparator(DisplayLocale locale) {
    return locale == DisplayLocale.ES || locale == DisplayLocale.RU ? "," : ".";
  }

  private static String groupSeparator(DisplayLocale locale) {
    return switch (locale) {
      case ES -> ".";
      case RU -> "\u00a0";
      default -> ",";
    };
  }

  static String sourceNumber(double value) {
    if (!Double.isFinite(value))
      throw new IllegalArgumentException("Finite display value required");
    if (value == 0) return "0";
    BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
    double absolute = Math.abs(value);
    if (absolute >= 1e-6 && absolute < 1e21) return decimal.toPlainString();
    return decimal.toString().replace("E", "e");
  }
}
