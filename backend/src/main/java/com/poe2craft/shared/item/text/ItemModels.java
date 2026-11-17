package com.poe2craft.shared.item.text;

import java.util.List;

/**
 * 아이템 텍스트 분석의 표시용 결과·경고·메타데이터 값을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemModels()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Item}: {@code ItemModels}에서 사용하는 아이템의 불변 값이다.
 *   <li>{@code Rarity}: {@code ItemModels}의 희귀도 구분값을 정의한다.
 *   <li>{@code LineKind}: {@code ItemModels}의 행·종류 구분값을 정의한다.
 *   <li>{@code TokenizedText}: {@code ItemModels}에서 사용하는 Tokenized·텍스트의 불변 값이다.
 *   <li>{@code RawField}: value는 단위, augmented 표기, 범위 등을 포함한 원문 문자열이다.
 *   <li>{@code Warning}: lineNumber 0은 문서 전체에 대한 경고다.
 *   <li>{@code TextLine}: {@code ItemModels}에서 사용하는 텍스트·행의 불변 값이다.
 *   <li>{@code ModifierType}: {@code ItemModels}의 옵션·타입 구분값을 정의한다.
 *   <li>{@code Modifier}: {@code ItemModels}에서 사용하는 옵션의 불변 값이다.
 * </ul>
 */
public final class ItemModels {
  private ItemModels() {}

  /**
   * {@code ItemModels}에서 사용하는 아이템의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code TokenizedText}).
   *   <li>{@code locale}: 언어코드 ({@code String}).
   *   <li>{@code itemClass}: 아이템·동치류 ({@code String}).
   *   <li>{@code rarity}: 현재 아이템 희귀도 ({@code Rarity}).
   *   <li>{@code rarityText}: 희귀도·텍스트 ({@code String}).
   *   <li>{@code nameLines}: 이름·행목록 ({@code List<TextLine>}).
   *   <li>{@code displayName}: 표시·이름 ({@code String}).
   *   <li>{@code displayBase}: 표시·베이스 ({@code String}).
   *   <li>{@code itemLevel}: 옵션 생성 제한에 사용하는 아이템 레벨 ({@code Integer}).
   *   <li>{@code properties}: 속성 ({@code List<RawField>}).
   *   <li>{@code requirements}: requirements ({@code List<RawField>}).
   *   <li>{@code markedModifiers}: marked·옵션목록 ({@code List<TextLine>}).
   *   <li>{@code modifiers}: 옵션목록 ({@code List<Modifier>}).
   *   <li>{@code flags}: 상태플래그 ({@code List<TextLine>}).
   *   <li>{@code unparsedLines}: unparsed·행목록 ({@code List<TextLine>}).
   *   <li>{@code warnings}: 경고목록 ({@code List<Warning>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Item(text, locale, itemClass, rarity, rarityText, nameLines, displayName,
   *       displayBase, itemLevel, properties, requirements, markedModifiers, modifiers, flags,
   *       unparsedLines, warnings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code text}(텍스트), {@code
   *       locale}(언어코드), {@code itemClass}(아이템·동치류), {@code rarity}(희귀도), {@code
   *       rarityText}(희귀도·텍스트), {@code nameLines}(이름·행목록), {@code displayName}(표시·이름), {@code
   *       displayBase}(표시·베이스), {@code itemLevel}(아이템·레벨), {@code properties}(속성), {@code
   *       requirements}(requirements), {@code markedModifiers}(marked·옵션목록), {@code
   *       modifiers}(옵션목록), {@code flags}(상태플래그), {@code unparsedLines}(unparsed·행목록), {@code
   *       warnings}(경고목록).
   * </ul>
   */
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

  /**
   * {@code ItemModels}의 희귀도 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code NORMAL}: 일반 구분값.
   *   <li>{@code MAGIC}: 마법 구분값.
   *   <li>{@code RARE}: 희귀 구분값.
   *   <li>{@code UNIQUE}: 고유 구분값.
   *   <li>{@code UNKNOWN}: 미확정 구분값.
   * </ul>
   */
  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE,
    UNKNOWN
  }

  /**
   * {@code ItemModels}의 행·종류 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code CONTENT}: 내용 구분값.
   *   <li>{@code SEPARATOR}: SEPARATOR 구분값.
   *   <li>{@code BLANK}: BLANK 구분값.
   * </ul>
   */
  public enum LineKind {
    CONTENT,
    SEPARATOR,
    BLANK
  }

  /**
   * {@code ItemModels}에서 사용하는 Tokenized·텍스트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code originalText}: 원본·텍스트 ({@code String}).
   *   <li>{@code lines}: 행목록 ({@code List<TextLine>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code TokenizedText(originalText, lines)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       originalText}(원본·텍스트), {@code lines}(행목록).
   * </ul>
   */
  public record TokenizedText(String originalText, List<TextLine> lines) {
    public TokenizedText {
      lines = List.copyOf(lines);
    }
  }

  /**
   * value는 단위, augmented 표기, 범위 등을 포함한 원문 문자열이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code String}).
   *   <li>{@code value}: 값 ({@code String}).
   *   <li>{@code source}: 출처 ({@code TextLine}).
   * </ul>
   */
  public record RawField(String key, String value, TextLine source) {}

  /**
   * lineNumber 0은 문서 전체에 대한 경고다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code String}).
   *   <li>{@code lineNumber}: 행·숫자 ({@code int}).
   * </ul>
   */
  public record Warning(String code, int lineNumber) {}

  /**
   * {@code ItemModels}에서 사용하는 텍스트·행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code number}: 숫자 ({@code int}).
   *   <li>{@code section}: section ({@code int}).
   *   <li>{@code raw}: 원문 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code LineKind}).
   * </ul>
   */
  public record TextLine(int number, int section, String raw, LineKind kind) {}

  /**
   * {@code ItemModels}의 옵션·타입 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code IMPLICIT}: 암시옵션 구분값.
   *   <li>{@code ENCHANT}: ENCHANT 구분값.
   *   <li>{@code RUNE}: 룬 구분값.
   *   <li>{@code DESECRATED}: 훼손 구분값.
   *   <li>{@code FRACTURED}: 분열고정 구분값.
   *   <li>{@code CRAFTED}: 제작옵션 구분값.
   *   <li>{@code MUTATED}: MUTATED 구분값.
   *   <li>{@code EXPLICIT}: 명시옵션 구분값.
   * </ul>
   */
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

  /**
   * {@code ItemModels}에서 사용하는 옵션의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   *   <li>{@code type}: 타입 ({@code ModifierType}).
   *   <li>{@code source}: 출처 ({@code TextLine}).
   *   <li>{@code metadata}: 메타데이터 ({@code TextLine}).
   *   <li>{@code affix}: 접두접미옵션 ({@code String}).
   *   <li>{@code tier}: 정의된 옵션 티어 ({@code Integer}).
   *   <li>{@code affixName}: 접두접미옵션·이름 ({@code String}).
   * </ul>
   */
  public record Modifier(
      String text,
      ModifierType type,
      TextLine source,
      TextLine metadata,
      String affix,
      Integer tier,
      String affixName) {}
}
