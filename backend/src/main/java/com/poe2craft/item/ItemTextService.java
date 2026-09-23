package com.poe2craft.item;

import com.poe2craft.item.api.ItemTextModels.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 게임 복사 텍스트를 사용자 확인용 초안으로 읽는다. Catalog ID와 게임 규칙은 확정하지 않는다. */
@org.springframework.stereotype.Service
public final class ItemTextService {
  public static final int MAX_TEXT_BYTES = 16 * 1024;
  private static final Pattern SEPARATOR = Pattern.compile("-{8,}");
  private static final Pattern MARKER =
      Pattern.compile(".*\\((implicit|enchant|rune|desecrated|fractured|crafted)\\)$");
  private static final Set<String> PROPERTY_KEYS =
      Set.of(
          "Quality",
          "Physical Damage",
          "Elemental Damage",
          "Chaos Damage",
          "Critical Hit Chance",
          "Attacks per Second",
          "Armour",
          "Evasion Rating",
          "Energy Shield",
          "Block chance",
          "Block Chance",
          "Sockets",
          "Spirit",
          "Stack Size");
  private static final Map<String, Rarity> RARITIES =
      Map.of(
          "Normal",
          Rarity.NORMAL,
          "Magic",
          Rarity.MAGIC,
          "Rare",
          Rarity.RARE,
          "Unique",
          Rarity.UNIQUE,
          "일반",
          Rarity.NORMAL,
          "마법",
          Rarity.MAGIC,
          "희귀",
          Rarity.RARE,
          "고유",
          Rarity.UNIQUE);

  /** 행 번호는 1부터, section은 0부터 시작한다. 구분선과 빈 행도 보존한다. */
  public TokenizedText tokenizeText(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Item text must not be empty");
    }
    if (text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
      throw new IllegalArgumentException("Item text exceeds 16 KiB UTF-8");
    }
    String normalized = text.startsWith("\uFEFF") ? text.substring(1) : text;
    String[] lines = normalized.split("\\r\\n|\\n|\\r", -1);
    List<TextLine> tokens = new ArrayList<>();
    int section = 0;
    for (int i = 0; i < lines.length; i++) {
      String value = lines[i].strip();
      LineKind kind =
          value.isEmpty()
              ? LineKind.BLANK
              : SEPARATOR.matcher(value).matches() ? LineKind.SEPARATOR : LineKind.CONTENT;
      tokens.add(new TextLine(i + 1, section, lines[i], kind));
      if (kind == LineKind.SEPARATOR) {
        section++;
      }
    }
    return new TokenizedText(text, tokens);
  }

  /** 계산용 ItemState가 아니다. 미해석 행과 경고를 확인한 뒤 catalog에 연결해야 한다. */
  public ParsedItemText parseText(String text) {
    TokenizedText tokenized = tokenizeText(text);
    List<TextLine> content =
        tokenized.lines().stream().filter(line -> line.kind() == LineKind.CONTENT).toList();
    if (content.size() < 3 || content.get(0).section() != 0 || content.get(1).section() != 0) {
      throw new IllegalArgumentException("Item headers and name are missing");
    }
    String classKey = key(content.get(0).raw());
    boolean korean = classKey.equals("아이템 종류");
    String rarityKey = korean ? "아이템 희귀도" : "Rarity";
    if ((!korean && !classKey.equals("Item Class"))
        || !key(content.get(1).raw()).equals(rarityKey)) {
      throw new IllegalArgumentException(
          "Expected English or Korean item class and rarity headers");
    }
    String itemClass = valueAfterColon(content.get(0).raw());
    String rarityText = valueAfterColon(content.get(1).raw());
    if (itemClass.isEmpty() || rarityText.isEmpty()) {
      throw new IllegalArgumentException("Item class and rarity must not be empty");
    }
    List<Warning> warnings = new ArrayList<>();
    Rarity rarity = RARITIES.getOrDefault(rarityText, Rarity.UNKNOWN);
    if (rarity == Rarity.UNKNOWN) {
      warnings.add(new Warning("UNSUPPORTED_RARITY", content.get(1).number()));
    }
    List<TextLine> names = new ArrayList<>();
    List<RawField> properties = new ArrayList<>();
    List<RawField> requirements = new ArrayList<>();
    List<TextLine> markedModifiers = new ArrayList<>();
    List<TextLine> unparsed = new ArrayList<>();
    List<TextLine> flags = new ArrayList<>();
    Integer itemLevel = null;
    int levelCount = 0;
    int requirementsSection = -1;
    for (TextLine line : content.subList(2, content.size())) {
      String value = line.raw().strip();
      String key = key(value);
      String fieldValue = key.isEmpty() ? "" : valueAfterColon(value);
      if (line.section() == 0
          && Set.of("Item Class", "Rarity", "아이템 종류", "아이템 희귀도").contains(key)) {
        throw new IllegalArgumentException("Duplicate item class or rarity header");
      }
      if (line.section() == 0) {
        names.add(line);
      } else if (key.equals("Item Level") || key.equals("아이템 레벨")) {
        levelCount++;
        properties.add(new RawField(key, fieldValue, line));
        try {
          if (!fieldValue.matches("[0-9]+")) {
            throw new NumberFormatException();
          }
          itemLevel = Integer.valueOf(fieldValue);
        } catch (NumberFormatException invalidLevel) {
          itemLevel = null;
          warnings.add(new Warning("INVALID_ITEM_LEVEL", line.number()));
        }
      } else if (Set.of("Requires", "Requirements", "요구사항", "요구 사항").contains(key)) {
        requirements.add(new RawField(key, fieldValue, line));
        requirementsSection = fieldValue.isEmpty() ? line.section() : -1;
      } else if (line.section() == requirementsSection) {
        requirements.add(new RawField(key, key.isEmpty() ? value : fieldValue, line));
      } else if (PROPERTY_KEYS.contains(key)) {
        properties.add(new RawField(key, fieldValue, line));
      } else if (Set.of("Corrupted", "Unidentified", "타락", "미확인").contains(value)) {
        flags.add(line);
      } else if (MARKER.matcher(value).matches()) {
        markedModifiers.add(line);
      } else {
        // 일반 옵션, flavour, advanced metadata는 catalog 없이 구분하지 않는다.
        unparsed.add(line);
      }
    }
    if (names.isEmpty()) {
      throw new IllegalArgumentException("Item name is missing");
    }
    if (levelCount > 1) {
      itemLevel = null;
      warnings.add(new Warning("DUPLICATE_ITEM_LEVEL", 0));
    }
    if (levelCount == 0) {
      warnings.add(new Warning("MISSING_ITEM_LEVEL", 0));
    }
    String displayName = names.getFirst().raw().strip();
    String displayBase = null;
    if ((rarity == Rarity.RARE || rarity == Rarity.UNIQUE) && names.size() == 2) {
      displayBase = names.get(1).raw().strip();
    } else if (rarity == Rarity.NORMAL && names.size() == 1) {
      displayBase = displayName;
    } else {
      warnings.add(new Warning("UNRESOLVED_BASE", names.getFirst().number()));
    }
    if (!unparsed.isEmpty()) {
      warnings.add(new Warning("UNPARSED_LINES", unparsed.getFirst().number()));
    }
    warnings.add(new Warning("CATALOG_VALIDATION_REQUIRED", 0));
    return new ParsedItemText(
        tokenized,
        korean ? "ko" : "en",
        itemClass,
        rarity,
        rarityText,
        names,
        displayName,
        displayBase,
        itemLevel,
        properties,
        requirements,
        markedModifiers,
        flags,
        unparsed,
        warnings);
  }

  private static String key(String text) {
    int colon = text.indexOf(':');
    return colon < 0 ? "" : text.substring(0, colon).strip();
  }

  private static String valueAfterColon(String text) {
    return text.substring(text.indexOf(':') + 1).strip();
  }
}
