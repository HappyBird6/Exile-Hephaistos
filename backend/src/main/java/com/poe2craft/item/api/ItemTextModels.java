package com.poe2craft.item.api;

import java.util.List;

/** 사용자 확인용 텍스트 초안 계약. 검증된 catalog 또는 계산용 ItemState가 아니다. */
public final class ItemTextModels {
  private ItemTextModels() {}

  public enum LineKind {
    CONTENT,
    SEPARATOR,
    BLANK
  }

  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE,
    UNKNOWN
  }

  public record TextLine(int number, int section, String raw, LineKind kind) {}

  public record TokenizedText(String originalText, List<TextLine> lines) {
    public TokenizedText {
      lines = List.copyOf(lines);
    }
  }

  /** value는 단위, augmented 표기, 범위 등을 포함한 원문 문자열이다. */
  public record RawField(String key, String value, TextLine source) {}

  /** lineNumber 0은 문서 전체에 대한 경고다. */
  public record Warning(String code, int lineNumber) {}

  public record ParsedItemText(
      TokenizedText text,
      String locale,
      String itemClass,
      Rarity rarity,
      String rarityText,
      List<TextLine> nameLines,
      String displayName,
      String displayBase,
      Integer itemLevel,
      List<RawField> properties,
      List<RawField> requirements,
      List<TextLine> markedModifiers,
      List<TextLine> flags,
      List<TextLine> unparsedLines,
      List<Warning> warnings) {
    public ParsedItemText {
      nameLines = List.copyOf(nameLines);
      properties = List.copyOf(properties);
      requirements = List.copyOf(requirements);
      markedModifiers = List.copyOf(markedModifiers);
      flags = List.copyOf(flags);
      unparsedLines = List.copyOf(unparsedLines);
      warnings = List.copyOf(warnings);
    }
  }
}
