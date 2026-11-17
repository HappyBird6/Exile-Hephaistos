package com.poe2craft.workbench.repeat;

import static com.poe2craft.configuration.PublicContracts.REPEAT;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

/**
 * 선형 시간 정규식 엔진에 전달하기 전에 제한된 패턴 문법을 검사한다.
 *
 * <p>기존 계약: Bounded regular-language grammar checked before the linear-time engine compiles a
 * pattern.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "chaos-repeat-re2j-1.8-find-ci-v1"} ({@code String}).
 *   <li>{@code BUDGET}: 계산예산 ({@code int}).
 *   <li>{@code pattern}: 패턴 ({@code Pattern}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosRepeatPattern(pattern)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code pattern}(패턴).
 *   <li>{@code compile(source)}: 입력 조건을 재사용할 평가·실행 표현으로 바꾼다. 입력: {@code source}(출처). 반환: {@code
 *       ChaosRepeatPattern}.
 *   <li>{@code find(text)}: 조건에 맞는 항목을 찾는다. 입력: {@code text}(텍스트). 반환: {@code boolean}.
 *   <li>{@code rejected(code, detail)}: 거절된의 입력에 따른 분기·결과를 처리한다. 입력: {@code code}(코드), {@code
 *       detail}(상세). 반환: {@code RepeatRejected}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Grammar}: {@code ChaosRepeatPattern} 내부의 Grammar 처리를 맡는다.
 * </ul>
 */
public final class ChaosRepeatPattern {
  public static final String VERSION = "chaos-repeat-re2j-1.8-find-ci-v1";
  private static final int BUDGET = REPEAT.patternProgram();
  private final Pattern pattern;

  private ChaosRepeatPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public static ChaosRepeatPattern compile(String source) {
    if (source == null || source.isEmpty() || source.length() > REPEAT.patternLength())
      throw rejected(
          "PATTERN_LENGTH",
          "Use a nonempty pattern of at most "
              + REPEAT.patternLength()
              + " UTF-16 code units. Input is not trimmed.");
    var grammar = new Grammar(source);
    grammar.alternation(0);
    if (grammar.index != source.length()) throw grammar.invalid();
    try {
      var compiled = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
      if (compiled.programSize() > BUDGET)
        throw rejected(
            "PATTERN_COMPLEXITY",
            "The compiled pattern exceeds the " + BUDGET + "-instruction budget.");
      return new ChaosRepeatPattern(compiled);
    } catch (PatternSyntaxException invalid) {
      throw grammar.invalid();
    }
  }

  public boolean find(String text) {
    return pattern.matcher(text).find();
  }

  private static RepeatRejected rejected(String code, String detail) {
    return new RepeatRejected(422, code, detail);
  }

  /**
   * {@code ChaosRepeatPattern} 내부의 Grammar 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code String}).
   *   <li>{@code index}: 색인 ({@code int}).
   *   <li>{@code nodes}: 노드목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Grammar(source)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code source}(출처).
   *   <li>{@code alternation(depth)}: alternation의 입력에 따른 분기·결과를 처리한다. 입력: {@code depth}(깊이). 반환:
   *       {@code int}.
   *   <li>{@code sequence(depth)}: 순서 처리에서 {@code source.length}을(를) 사용한다. 입력: {@code depth}(깊이).
   *       반환: {@code int}.
   *   <li>{@code atom(depth)}: atom 처리에서 {@code source.charAt}, {@code REPEAT.patternDepth}을(를)
   *       사용한다. 입력: {@code depth}(깊이). 반환: {@code int}.
   *   <li>{@code escape()}: escape 처리에서 {@code source.length}, {@code source.charAt}을(를) 사용한다.
   *   <li>{@code scalar(c)}: scalar 처리에서 {@code Character.isHighSurrogate}, {@code
   *       source.length}을(를) 사용한다. 입력: {@code c}(c).
   *   <li>{@code number()}: 숫자 값을 조회·구성한다. 반환: {@code int}.
   *   <li>{@code node()}: 노드 처리에서 {@code REPEAT.patternNodes}을(를) 사용한다.
   *   <li>{@code bounded(cost)}: 한도내의 입력에 따른 분기·결과를 처리한다. 입력: {@code cost}(비용). 반환: {@code int}.
   *   <li>{@code peek()}: peek 값을 조회·구성한다. 반환: {@code char}.
   *   <li>{@code take(c)}: take 처리에서 {@code source.length}을(를) 사용한다. 입력: {@code c}(c). 반환: {@code
   *       boolean}.
   *   <li>{@code invalid()}: 잘못된 입력을 나타내는 오류 결과를 만든다. 반환: {@code RepeatRejected}.
   * </ul>
   */
  private static final class Grammar {
    private final String source;
    private int index;
    private int nodes;

    private Grammar(String source) {
      this.source = source;
    }

    private int alternation(int depth) {
      int cost = sequence(depth);
      while (take('|')) {
        node();
        cost = bounded((long) cost + sequence(depth) + 1);
      }
      return cost;
    }

    private int sequence(int depth) {
      int cost = 0;
      while (index < source.length() && peek() != ')' && peek() != '|') {
        node();
        int atom = atom(depth);
        if (index < source.length()) {
          char q = peek();
          if (q == '*' || q == '+' || q == '?' || q == '{') {
            node();
            index++;
            if (q == '{') {
              int min = number();
              int max = min;
              if (take(',')) max = index < source.length() && peek() == '}' ? -1 : number();
              if (!take('}') || min > 1000 || max > 1000 || (max >= 0 && min > max))
                throw invalid();
              atom = bounded((long) atom * (max < 0 ? min + 1 : max) + 2);
            } else atom = bounded((long) atom + 2);
            take('?'); // RE2 reluctant quantifiers retain the same bounded program cost.
            if (index < source.length() && "*+?{".indexOf(peek()) >= 0) throw invalid();
          }
        }
        cost = bounded((long) cost + atom);
      }
      return cost;
    }

    private int atom(int depth) {
      char c = source.charAt(index++);
      if (c == '(') {
        if (depth >= REPEAT.patternDepth())
          throw rejected(
              "PATTERN_DEPTH",
              "Patterns allow at most " + REPEAT.patternDepth() + " nested groups.");
        if (take('?') && !take(':')) throw invalid(); // no inline flags, lookaround or named groups
        int cost = alternation(depth + 1);
        if (!take(')')) throw invalid();
        return bounded((long) cost + 2);
      }
      if (c == '[') {
        take('^');
        boolean first = true;
        while (index < source.length()) {
          char next = source.charAt(index++);
          if (next == ']' && !first) return 1;
          if (next == '\\') escape();
          else {
            if (next == '[' && index < source.length() && peek() == ':') throw invalid();
            if (next == '&' && index < source.length() && peek() == '&') throw invalid();
            scalar(next);
          }
          first = false;
        }
        throw invalid();
      }
      if (c == '\\') escape();
      else {
        if ("*+?{}".indexOf(c) >= 0) throw invalid();
        scalar(c);
      }
      return 1;
    }

    private void escape() {
      if (index == source.length()) throw invalid();
      char c = source.charAt(index++);
      if ("dDsSwWnrtfvAbBz".indexOf(c) >= 0) return;
      if (c == 'x') {
        if (take('{')) {
          int start = index;
          while (index < source.length() && Character.digit(peek(), 16) >= 0) index++;
          if (index == start || index - start > 6 || !take('}')) throw invalid();
          int value = Integer.parseInt(source.substring(start, index - 1), 16);
          if (value > 0x10ffff || (value >= 0xd800 && value <= 0xdfff)) throw invalid();
        } else
          for (int i = 0; i < 2; i++) {
            if (index == source.length() || Character.digit(source.charAt(index++), 16) < 0)
              throw invalid();
          }
        return;
      }
      if (c <= 127 && !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) return;
      throw invalid();
    }

    private void scalar(char c) {
      if (Character.isHighSurrogate(c)) {
        if (index == source.length() || !Character.isLowSurrogate(source.charAt(index++)))
          throw invalid();
      } else if (Character.isLowSurrogate(c)) throw invalid();
    }

    private int number() {
      int start = index;
      while (index < source.length() && peek() >= '0' && peek() <= '9') index++;
      if (start == index || index - start > 4) throw invalid();
      return Integer.parseInt(source.substring(start, index));
    }

    private void node() {
      if (++nodes > REPEAT.patternNodes())
        throw rejected(
            "PATTERN_COMPLEXITY",
            "Patterns allow at most " + REPEAT.patternNodes() + " syntax nodes.");
    }

    private int bounded(long cost) {
      if (cost > BUDGET)
        throw rejected(
            "PATTERN_COMPLEXITY",
            "Pattern expansion exceeds the " + BUDGET + "-instruction budget before compilation.");
      return (int) cost;
    }

    private char peek() {
      return source.charAt(index);
    }

    private boolean take(char c) {
      if (index < source.length() && peek() == c) {
        index++;
        return true;
      }
      return false;
    }

    private RepeatRejected invalid() {
      return rejected(
          "INVALID_REPEAT_PATTERN",
          "Use literals, Unicode character sets/ranges, . ^ $, alternatives, groups, regular"
              + " quantifiers, and basic escapes. Inline flags, lookaround, backreferences,"
              + " possessive quantifiers and extensions are unsupported. Check the syntax near"
              + " UTF-16 position "
              + index
              + ".");
    }
  }
}
