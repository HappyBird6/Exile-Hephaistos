package com.poe2craft.crafting.application.chaosrepeat;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

/** Bounded regular-language grammar checked before the linear-time engine compiles a pattern. */
public final class ChaosRepeatPattern {
  public static final String VERSION = "chaos-repeat-re2j-1.8-find-ci-v1";
  private static final int BUDGET = 4096;
  private final Pattern pattern;

  private ChaosRepeatPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public static ChaosRepeatPattern compile(String source) {
    if (source == null || source.isEmpty() || source.length() > 512)
      throw rejected(
          "PATTERN_LENGTH",
          "Use a nonempty pattern of at most 512 UTF-16 code units. Input is not trimmed.");
    var grammar = new Grammar(source);
    grammar.alternation(0);
    if (grammar.index != source.length()) throw grammar.invalid();
    try {
      var compiled = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
      if (compiled.programSize() > BUDGET)
        throw rejected(
            "PATTERN_COMPLEXITY", "The compiled pattern exceeds the 4096-instruction budget.");
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
        if (depth >= 16)
          throw rejected("PATTERN_DEPTH", "Patterns allow at most 16 nested groups.");
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
      if (++nodes > 512)
        throw rejected("PATTERN_COMPLEXITY", "Patterns allow at most 512 syntax nodes.");
    }

    private int bounded(long cost) {
      if (cost > BUDGET)
        throw rejected(
            "PATTERN_COMPLEXITY",
            "Pattern expansion exceeds the 4096-instruction budget before compilation.");
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
