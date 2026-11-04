package com.poe2craft.crafting.application.chaosrepeat;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChaosRepeatPatternTest {
  @Test
  void substringCaseFoldingAndAnchorsRetainNormalUnicodeSemantics() {
    assertThat(ChaosRepeatPattern.compile("ESPAÑA").find("Bonificación España 12,5%")).isTrue();
    assertThat(ChaosRepeatPattern.compile("[가-힣]+|[一-龥]+").find("생명력 50")).isTrue();
    assertThat(ChaosRepeatPattern.compile("[가-힣]+|[一-龥]+").find("生命 +50")).isTrue();
    assertThat(ChaosRepeatPattern.compile("é").find("É")).isTrue();
    assertThat(ChaosRepeatPattern.compile("é").find("e")).isFalse();
    assertThat(ChaosRepeatPattern.compile("ß").find("ss")).isFalse();
    assertThat(ChaosRepeatPattern.compile("^Life$").find("+50 Life")).isFalse();
    assertThat(ChaosRepeatPattern.compile("^Life$").find("LIFE")).isTrue();
    assertThat(ChaosRepeatPattern.compile("Life$").find("Life\nMana")).isFalse();
    assertThat(ChaosRepeatPattern.compile("Life.*Mana").find("Life\nMana")).isFalse();
    assertThat(ChaosRepeatPattern.compile("Life[\\s\\S]*Mana").find("Life\nMana")).isTrue();
    assertThat(ChaosRepeatPattern.compile(" Life ").find("Life")).isFalse();
    assertThat(ChaosRepeatPattern.compile("\\x{1F600}").find("😀")).isTrue();
    assertThat(ChaosRepeatPattern.compile("[😀-🙏]").find("😃")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "(?i)a",
        "(?-i)a",
        "(?s:a)",
        "(?=a)",
        "(?!a)",
        "(?<=a)",
        "(a)\\1",
        "a++",
        "a*+",
        "a{2}+",
        "(?<name>a)",
        "\\p{L}",
        "[a&&b]",
        "[z-a]",
        "(",
        "[",
        "a{1001}",
        "a{3,2}",
        "a{999999}",
        "a\\"
      })
  void unsupportedOrInvalidSyntaxIsRejectedWithoutEchoingInput(String source) {
    assertThatThrownBy(() -> ChaosRepeatPattern.compile(source))
        .isInstanceOf(RepeatRejected.class)
        .satisfies(error -> assertThat(((RepeatRejected) error).status()).isEqualTo(422));
  }

  @Test
  void lengthDepthAndExpansionAreBoundedBeforeCompilation() {
    for (String source :
        List.of(
            "",
            "a".repeat(513),
            "(".repeat(17) + "a" + ")".repeat(17),
            "(a{1000}){1000}",
            "((ab){1000}){1000}"))
      assertThatThrownBy(() -> ChaosRepeatPattern.compile(source))
          .isInstanceOf(RepeatRejected.class);
    assertThat(ChaosRepeatPattern.compile("a".repeat(512)).find("a".repeat(512))).isTrue();
    assertThat(ChaosRepeatPattern.compile("(".repeat(16) + "a" + ")".repeat(16)).find("a"))
        .isTrue();
    assertThat(ChaosRepeatPattern.compile("a{1000}").find("a".repeat(1000))).isTrue();
    assertThatThrownBy(() -> ChaosRepeatPattern.compile(String.valueOf('\ud800')))
        .isInstanceOf(RepeatRejected.class);
  }

  @Test
  void nestedRegularQuantifiersDoNotUseBacktracking() {
    // A failure case for backtracking engines; bounded input and a generous regression deadline.
    org.junit.jupiter.api.Assertions.assertTimeout(
        Duration.ofSeconds(10),
        () ->
            assertThat(ChaosRepeatPattern.compile("^(a+)+$").find("a".repeat(100_000) + "!"))
                .isFalse());
  }
}
