package com.poe2craft.item.testparser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poe2craft.item.ItemModels;
import com.poe2craft.item.ItemModels.LineKind;
import com.poe2craft.item.ItemModels.Rarity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ItemTextServiceTest {
  private final ItemTextService service = new ItemTextService();

  @Test
  void upstreamAdvancedClipboardFixturePreservesLegacyRollsAndUnresolvedGrantSkill()
      throws Exception {
    try (var stream = getClass().getResourceAsStream("/item-text/pob-issue-2117.txt")) {
      assertThat(stream).isNotNull();
      String text = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      var result = service.parseText(text);
      assertThat(result.displayName()).isEqualTo("Prism Guardian");
      assertThat(result.modifiers()).hasSize(6);
      assertThat(result.modifiers().get(0).type()).isEqualTo(ItemModels.ModifierType.RUNE);
      assertThat(result.modifiers().get(2).text())
          .isEqualTo("144(150-200)% increased Armour and Energy Shield");
      assertThat(result.modifiers().get(5).text())
          .isEqualTo("+1 to Maximum Spirit per 25(50) Maximum Life");
      assertThat(result.unparsedLines())
          .extracting(ItemModels.TextLine::raw)
          .contains("Grants Skill: Raise Shield", "When blood is paid, the weak think twice.");
      assertThat(result.text().originalText()).isEqualTo(text);
    }
  }

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
        .extracting(ItemModels.RawField::value)
        .contains("+20% (augmented)", "11-22", "S S", "42");
    assertThat(result.requirements())
        .extracting(ItemModels.RawField::value)
        .containsExactly("Level 10, 20 Dex, 30 Int");
    assertThat(result.markedModifiers())
        .extracting(ItemModels.TextLine::raw)
        .containsExactly("+7 to Synthetic Value (rune)");
    assertThat(result.unparsedLines())
        .extracting(ItemModels.TextLine::raw)
        .containsExactly(
            "Grants Skill: Level 3 Synthetic Skill",
            "{ Unique Modifier — Synthetic metadata }",
            "Synthetic flavour text.");
    assertThat(result.flags()).extracting(ItemModels.TextLine::raw).containsExactly("Corrupted");
    assertThat(result.warnings())
        .extracting(ItemModels.Warning::code)
        .contains("UNPARSED_LINES", "CATALOG_VALIDATION_REQUIRED");
  }

  @Test
  void rejectsKoreanUntilASeparateTranslationLayerIsProvided() {
    assertThatThrownBy(() -> service.parseText("아이템 종류: 테스트\n아이템 희귀도: 일반\n테스트"))
        .isInstanceOf(IllegalArgumentException.class);
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
        .isEqualTo(new ItemModels.TextLine(4, 1, "  body  ", LineKind.CONTENT));
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
        .extracting(ItemModels.Warning::code)
        .contains("INVALID_ITEM_LEVEL");
    assertThat(result.properties()).extracting(ItemModels.RawField::value).contains(level);
  }

  @Test
  void duplicateLevelDoesNotChooseOneValue() {
    var result = service.parseText(ENGLISH + "\nItem Level: 43");
    assertThat(result.itemLevel()).isNull();
    assertThat(result.warnings())
        .extracting(ItemModels.Warning::code)
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
        .extracting(ItemModels.TextLine::raw)
        .contains("아이템 희귀도: +30% (augmented)");
  }

  @Test
  void neverInfersBaseFromMagicName() {
    var result =
        service.parseText(
            "Item Class: X\nRarity: Magic\nSynthetic of Foo\n--------\nItem Level: 1");
    assertThat(result.displayBase()).isNull();
    assertThat(result.warnings()).extracting(ItemModels.Warning::code).contains("UNRESOLVED_BASE");
  }

  @Test
  void unknownRarityAndMissingLevelRemainUnknown() {
    var result = service.parseText("Item Class: X\nRarity: Future\nSynthetic\n--------\nUnknown");
    assertThat(result.rarity()).isEqualTo(Rarity.UNKNOWN);
    assertThat(result.itemLevel()).isNull();
    assertThat(result.warnings())
        .extracting(ItemModels.Warning::code)
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
    assertThatThrownBy(() -> result.modifiers().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.unparsedLines().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.flags().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.warnings().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void advancedBlocksInheritMetadataAcrossLinesButNeverAcrossSeparators() {
    var result =
        service.parseText(
            """
        Item Class: Synthetic Amulets
        Rarity: Rare
        Synthetic Name
        Synthetic Base
        --------
        Quality (Caster Modifiers): +47% (augmented)
        --------
        Requirements:
        Level: 60
        Int: 12
        --------
        Item Level: 82
        --------
        +13 to Synthetic Spirit (implicit)
        --------
        { Fractured Prefix Modifier "Synthetic" (Tier: 1) — Mana }
        8(7-8)% increased Synthetic Mana
        +10(10) to Synthetic Value
        { Suffix Modifier "of Test" (Tier: 3) — Critical }
        25(25-29)% increased Synthetic Critical
        --------
        Synthetic flavour.
        Corrupted
        ~b/o 888 mirror
        """);
    assertThat(result.modifiers()).hasSize(4);
    var first = result.modifiers().get(1);
    assertThat(first.type()).isEqualTo(ItemModels.ModifierType.FRACTURED);
    assertThat(first.affix()).isEqualTo("PREFIX");
    assertThat(first.affixName()).isEqualTo("Synthetic");
    assertThat(first.tier()).isEqualTo(1);
    assertThat(first.text()).isEqualTo("8(7-8)% increased Synthetic Mana");
    assertThat(result.modifiers().get(2).metadata()).isEqualTo(first.metadata());
    assertThat(result.modifiers().get(3).tier()).isEqualTo(3);
    assertThat(result.properties())
        .extracting(ItemModels.RawField::key)
        .contains("Quality (Caster Modifiers)");
    assertThat(result.requirements()).hasSize(3);
    assertThat(result.unparsedLines())
        .extracting(ItemModels.TextLine::raw)
        .contains("Synthetic flavour.", "~b/o 888 mirror");
  }

  @Test
  void parsesRarityFirstAndRequiresWithoutColonAndExactFlags() {
    var result =
        service.parseText(
            "Rarity: Normal\nSynthetic Base\n--------\nRequires Level 10\n--------\nItem Level: 42\n--------\nMirrored\nTwice Corrupted\nSanctified");
    assertThat(result.itemClass()).isEmpty();
    assertThat(result.requirements().getFirst().value()).isEqualTo("Level 10");
    assertThat(result.flags()).hasSize(3);
    assertThat(result.warnings())
        .extracting(ItemModels.Warning::code)
        .contains("MISSING_ITEM_CLASS");
  }

  @Test
  void preservesUnknownGrantSkillAndReminderWithoutGuessingCatalogSemantics() {
    var result =
        service.parseText(
            "Rarity: Rare\nSynthetic\nSynthetic Base\n--------\nItem Level: 42\n--------\nSynthetic rune (rune)\n--------\nGrants Skill: Level 3 Synthetic Skill\n--------\n{ Unique Modifier — Test }\nSynthetic value +10\n(Reminder spanning\nmultiple lines)\n--------\nFlavour");
    assertThat(result.modifiers()).hasSize(2);
    assertThat(result.unparsedLines())
        .extracting(ItemModels.TextLine::raw)
        .contains(
            "Grants Skill: Level 3 Synthetic Skill",
            "(Reminder spanning",
            "multiple lines)",
            "Flavour");
  }

  @Test
  void malformedOrOrphanMetadataAndOversizedTierRemainSafeAndDoNotLeak() {
    var result =
        service.parseText(
            "Rarity: Rare\nSynthetic\nBase\n--------\nItem Level: 42\n--------\n{ Prefix Modifier \"Huge\" (Tier: 9999999999999999) }\nSynthetic +1\n{ malformed\nSynthetic +2\n--------\nSynthetic flavour\n{ Suffix Modifier \"Orphan\" (Tier: 2) }\n--------\nSynthetic footer");
    assertThat(result.modifiers()).hasSize(2);
    assertThat(result.modifiers().getFirst().tier()).isNull();
    assertThat(result.modifiers().get(1).metadata()).isNull();
    assertThat(result.modifiers().get(1).affix()).isNull();
    assertThat(result.unparsedLines())
        .extracting(ItemModels.TextLine::raw)
        .contains(
            "{ malformed",
            "Synthetic flavour",
            "{ Suffix Modifier \"Orphan\" (Tier: 2) }",
            "Synthetic footer");
  }
}
