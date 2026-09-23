package com.poe2craft.item;
import java.util.List;
public final class ItemModels {
  private ItemModels() {}
  public record Item(
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
      List<Modifier> modifiers,
      List<TextLine> flags,
      List<TextLine> unparsedLines,
      List<Warning> warnings) {
    public Item {
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
  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE,
    UNKNOWN
  }
  public enum LineKind {
    CONTENT,
    SEPARATOR,
    BLANK
  }
  public record TokenizedText(String originalText, List<TextLine> lines) {
    public TokenizedText {
      lines = List.copyOf(lines);
    }
  }
  
  /** value는 단위, augmented 표기, 범위 등을 포함한 원문 문자열이다. */
  public record RawField(String key, String value, TextLine source) {}

  /** lineNumber 0은 문서 전체에 대한 경고다. */
  public record Warning(String code, int lineNumber) {}
  public record TextLine(int number, int section, String raw, LineKind kind) {}
  public enum ModifierType {
    IMPLICIT,
    ENCHANT,
    RUNE,
    DESECRATED,
    FRACTURED,
    CRAFTED,
    MUTATED,
    EXPLICIT
  }
    public record Modifier(
      String text,
      ModifierType Type,
      TextLine source,
      TextLine metadata,
      String affix,
      Integer tier,
      String affixName) {}
}
