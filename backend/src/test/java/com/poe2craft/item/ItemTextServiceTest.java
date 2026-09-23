package com.poe2craft.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poe2craft.item.ItemTextService.LineKind;
import com.poe2craft.item.ItemTextService.Rarity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ItemTextServiceTest {
  private final ItemTextService service = new ItemTextService();
  // Synthetic names/stats only: examples test clipboard grammar, not actual game rules.
  private static final String ENGLISH =
      """
      Item Class: Synthetic Staff
      Rarity: Rare
      Synthetic Dawn
      Synthetic Base
      --------
      Quality: +20% (augmented)
      Physical Damage: 11-22
      --------
      Requires: Level 10, 20 Dex, 30 Int
      --------
      Sockets: S S
      --------
      Item Level: 42
      --------
      +7 to Synthetic Value (rune)
      --------
      12% increased Synthetic Power
      Grants Skill: Level 3 Synthetic Skill
      { Unique Modifier — Synthetic metadata }
      Continued unknown line
      --------
      Synthetic flavour text.
      Corrupted
      """;

  @Test
  void parsesEnglishWithoutGuessingCatalogIdsOrDiscardingUnknownLines() {
    var result = service.parseText(ENGLISH);
    assertThat(result.text().originalText()).isEqualTo(ENGLISH);
    assertThat(result.locale()).isEqualTo("en");
    assertThat(result.itemClass()).isEqualTo("Synthetic Staff");
    assertThat(result.rarity()).isEqualTo(Rarity.RARE);
    assertThat(result.displayName()).isEqualTo("Synthetic Dawn");
    assertThat(result.displayBase()).isEqualTo("Synthetic Base");
    assertThat(result.itemLevel()).isEqualTo(42);
    assertThat(result.properties())
        .extracting(ItemTextService.RawField::value)
        .contains("+20% (augmented)", "11-22", "S S", "42");
    assertThat(result.requirements())
        .extracting(ItemTextService.RawField::value)
        .containsExactly("Level 10, 20 Dex, 30 Int");
    assertThat(result.markedModifiers())
        .extracting(ItemTextService.TextLine::raw)
        .containsExactly("+7 to Synthetic Value (rune)");
    assertThat(result.unparsedLines())
        .extracting(ItemTextService.TextLine::raw)
        .containsExactly(
            "12% increased Synthetic Power",
            "Grants Skill: Level 3 Synthetic Skill",
            "{ Unique Modifier — Synthetic metadata }",
            "Continued unknown line",
            "Synthetic flavour text.");
    assertThat(result.flags())
        .extracting(ItemTextService.TextLine::raw)
        .containsExactly("Corrupted");
    assertThat(result.warnings())
        .extracting(ItemTextService.Warning::code)
        .contains("UNPARSED_LINES", "CATALOG_VALIDATION_REQUIRED");
  }

  @Test
  void parsesKoreanHeadersAndMultilineRequirementsAndPreservesMetadata() {
    var result =
        service.parseText(
            """
        아이템 종류: 테스트 지팡이
        아이템 희귀도: 희귀
        가상 새벽
        가상 베이스
        --------
        요구사항:
        레벨: 10
        지능: 20
        --------
        아이템 레벨: 42
        --------
        { 접두어 속성 부여 "가상" (등급: 1) — 출현 }
        가상 능력치 +7
        알 수 없는 속성: 보존
        타락
        """);
    assertThat(result.locale()).isEqualTo("ko");
    assertThat(result.rarity()).isEqualTo(Rarity.RARE);
    assertThat(result.displayBase()).isEqualTo("가상 베이스");
    assertThat(result.itemLevel()).isEqualTo(42);
    assertThat(result.requirements())
        .extracting(ItemTextService.RawField::value)
        .containsExactly("", "10", "20");
    assertThat(result.unparsedLines()).hasSize(3);
    assertThat(result.flags()).extracting(ItemTextService.TextLine::raw).containsExactly("타락");
  }

  @Test
  void parsesSingleLineKoreanRequirements() {
    var result =
        service.parseText(
            """
        아이템 종류: 테스트
        아이템 희귀도: 고유
        가상 이름
        가상 베이스
        --------
        요구 사항: 레벨 64
        --------
        아이템 레벨: 81
        """);
    assertThat(result.requirements())
        .extracting(ItemTextService.RawField::value)
        .containsExactly("레벨 64");
    assertThat(result.rarity()).isEqualTo(Rarity.UNIQUE);
  }

  @Test
  void tokenizerPreservesBomOriginalWhitespaceBlankLinesAndSections() {
    String original = "\uFEFF Item Class: X\r\n\r\n--------\r\n  body  \r\n";
    var result = service.tokenizeText(original);
    assertThat(result.originalText()).isEqualTo(original);
    assertThat(result.lines()).hasSize(5);
    assertThat(result.lines().get(1).kind()).isEqualTo(LineKind.BLANK);
    assertThat(result.lines().get(2).kind()).isEqualTo(LineKind.SEPARATOR);
    assertThat(result.lines().get(3))
        .isEqualTo(new ItemTextService.TextLine(4, 1, "  body  ", LineKind.CONTENT));
    assertThat(service.parseText("\uFEFF" + ENGLISH.replace("\n", "\r\n")).itemLevel())
        .isEqualTo(42);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t", "\uFEFF"})
  void rejectsEmptyInput(String input) {
    assertThatThrownBy(() -> service.parseText(input)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void limitsUtf8BytesRatherThanCharacters() {
    assertThat(service.tokenizeText("x".repeat(16 * 1024)).lines()).hasSize(1);
    assertThatThrownBy(() -> service.tokenizeText("x".repeat(16 * 1024 + 1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.tokenizeText("가".repeat(6000)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "-1", "42 (augmented)", "2147483648", "abc"})
  void malformedLevelIsUnavailableAndRawFieldIsPreserved(String level) {
    var result = service.parseText(ENGLISH.replace("Item Level: 42", "Item Level: " + level));
    assertThat(result.itemLevel()).isNull();
    assertThat(result.warnings())
        .extracting(ItemTextService.Warning::code)
        .contains("INVALID_ITEM_LEVEL");
    assertThat(result.properties()).extracting(ItemTextService.RawField::value).contains(level);
  }

  @Test
  void duplicateLevelDoesNotChooseOneValue() {
    var result = service.parseText(ENGLISH + "\nItem Level: 43");
    assertThat(result.itemLevel()).isNull();
    assertThat(result.warnings())
        .extracting(ItemTextService.Warning::code)
        .contains("DUPLICATE_ITEM_LEVEL");
  }

  @Test
  void duplicateHeaderIsRejected() {
    assertThatThrownBy(
            () ->
                service.parseText(
                    ENGLISH.replace("Synthetic Dawn", "Rarity: Magic\nSynthetic Dawn")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not an item",
        "Item Class: X\nRarity: Rare\n--------\nItem Level: 1",
        "Item Class: X\nRare\nName",
        "Item Class: \nRarity: Rare\nName",
        "Objet: X\nRarete: Rare\nName"
      })
  void rejectsMissingHeadersNameAndUnsupportedLanguage(String input) {
    assertThatThrownBy(() -> service.parseText(input)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void preservesBodyRarityPropertyWithoutConfusingItWithHeader() {
    var result = service.parseText(ENGLISH + "\n아이템 희귀도: +30% (augmented)");
    assertThat(result.unparsedLines())
        .extracting(ItemTextService.TextLine::raw)
        .contains("아이템 희귀도: +30% (augmented)");
  }

  @Test
  void neverInfersBaseFromMagicName() {
    var result =
        service.parseText(
            "Item Class: X\nRarity: Magic\nSynthetic of Foo\n--------\nItem Level: 1");
    assertThat(result.displayBase()).isNull();
    assertThat(result.warnings())
        .extracting(ItemTextService.Warning::code)
        .contains("UNRESOLVED_BASE");
  }

  @Test
  void unknownRarityAndMissingLevelRemainUnknown() {
    var result = service.parseText("Item Class: X\nRarity: Future\nSynthetic\n--------\nUnknown");
    assertThat(result.rarity()).isEqualTo(Rarity.UNKNOWN);
    assertThat(result.itemLevel()).isNull();
    assertThat(result.warnings())
        .extracting(ItemTextService.Warning::code)
        .contains("UNSUPPORTED_RARITY", "MISSING_ITEM_LEVEL");
  }

  @Test
  void returnedCollectionsAreImmutable() {
    var result = service.parseText(ENGLISH);
    assertThatThrownBy(() -> result.text().lines().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.nameLines().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.properties().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.requirements().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.markedModifiers().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.unparsedLines().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.flags().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.warnings().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
