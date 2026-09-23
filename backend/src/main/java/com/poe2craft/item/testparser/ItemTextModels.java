package com.poe2craft.item.testparser;

import java.util.List;

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

  public enum ModifierKind {
    IMPLICIT,
    ENCHANT,
    RUNE,
    DESECRATED,
    FRACTURED,
    CRAFTED,
    MUTATED,
    EXPLICIT
  }

  /** Display metadata from the clipboard, never a resolved catalog modifier. */
  public record ParsedModifier(
      String text,
      ModifierKind kind,
      TextLine source,
      TextLine metadata,
      String affix,
      Integer tier,
      String affixName) {}

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
      List<ParsedModifier> modifiers,
      List<TextLine> flags,
      List<TextLine> unparsedLines,
      List<Warning> warnings) {
    public ParsedItemText {
      nameLines = List.copyOf(nameLines);
      properties = List.copyOf(properties);
      requirements = List.copyOf(requirements);
      markedModifiers = List.copyOf(markedModifiers);
      modifiers = List.copyOf(modifiers);
      flags = List.copyOf(flags);
      unparsedLines = List.copyOf(unparsedLines);
      warnings = List.copyOf(warnings);
    }
  }
}
