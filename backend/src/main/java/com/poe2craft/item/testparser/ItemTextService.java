package com.poe2craft.item.testparser;

import com.poe2craft.item.testparser.ItemTextModels.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Clipboard display parser adapted from PoB ParseRaw; see docs/item-text-parsing.md. */
@org.springframework.stereotype.Service
public final class ItemTextService {
  public static final int MAX_TEXT_BYTES = 16 * 1024;
  private static final Pattern SEPARATOR = Pattern.compile("-{8,}");
  private static final Pattern MARKER =
      Pattern.compile("\\s*\\((implicit|enchant|rune|desecrated|fractured|crafted|mutated)\\)$");
  private static final Pattern ADVANCED =
      Pattern.compile("^\\{ (.*(?:Modifier|Enhancement).*?) }$");
  private static final Pattern AFFIX_NAME = Pattern.compile("Modifier \\\"([^\\\"]*)\\\"");
  private static final Pattern TIER = Pattern.compile("\\(Tier: ([0-9]+)\\)");
  private static final Set<String> FLAGS =
      Set.of(
          "Corrupted",
          "Twice Corrupted",
          "Unidentified",
          "Mirrored",
          "Sanctified",
          "Desecrated Prefix",
          "Desecrated Suffix",
          "Split");
  private static final Set<String> PROPERTY_KEYS =
      Set.of(
          "Chance to Block",
          "Reload Time",
          "Critical Hit Range",
          "Fire Damage",
          "Cold Damage",
          "Lightning Damage",
          "Weapon Range",
          "Quality",
          "Physical Damage",
          "Elemental Damage",
          "Chaos Damage",
          "Critical Hit Chance",
          "Attacks per Second",
          "Armour",
          "Evasion Rating",
          "Evasion",
          "Energy Shield",
          "Ward",
          "Runic Ward",
          "Block chance",
          "Block Chance",
          "Sockets",
          "Spirit",
          "Stack Size",
          "Charm Slots",
          "Rune",
          "Radius",
          "Limited to",
          "Talisman Tier");
  private static final Map<String, Rarity> RARITIES =
      Map.of(
          "Normal",
          Rarity.NORMAL,
          "Magic",
          Rarity.MAGIC,
          "Rare",
          Rarity.RARE,
          "Unique",
          Rarity.UNIQUE);

  public TokenizedText tokenizeText(String text) {
    if (text == null || text.isBlank())
      throw new IllegalArgumentException("Item text must not be empty");
    if (text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES)
      throw new IllegalArgumentException("Item text exceeds 16 KiB UTF-8");
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
      if (kind == LineKind.SEPARATOR) section++;
    }
    return new TokenizedText(text, tokens);
  }

  /** Returns reviewable display data, not catalog-validated ItemState or calculated stats. */
  public ParsedItemText parseText(String text) {
    TokenizedText tokenized = tokenizeText(text);
    List<TextLine> content =
        tokenized.lines().stream().filter(line -> line.kind() == LineKind.CONTENT).toList();
    if (content.size() < 2 || content.getFirst().section() != 0)
      throw new IllegalArgumentException("Item headers and name are missing");
    int header = 0;
    String itemClass = "";
    if (key(content.getFirst().raw()).equals("Item Class")) {
      itemClass = valueAfterColon(content.getFirst().raw());
      if (itemClass.isEmpty()) throw new IllegalArgumentException("Item class must not be empty");
      header++;
    }
    TextLine rarityLine = content.get(header);
    if (rarityLine.section() != 0 || !key(rarityLine.raw()).equals("Rarity"))
      throw new IllegalArgumentException("Expected English Rarity header");
    String rarityText = valueAfterColon(rarityLine.raw());
    if (rarityText.isEmpty()) throw new IllegalArgumentException("Rarity must not be empty");
    List<Warning> warnings = new ArrayList<>();
    Rarity rarity = RARITIES.getOrDefault(rarityText, Rarity.UNKNOWN);
    if (rarity == Rarity.UNKNOWN)
      warnings.add(new Warning("UNSUPPORTED_RARITY", rarityLine.number()));
    if (itemClass.isEmpty()) warnings.add(new Warning("MISSING_ITEM_CLASS", 0));
    List<TextLine> names = new ArrayList<>(),
        marked = new ArrayList<>(),
        unparsed = new ArrayList<>(),
        flags = new ArrayList<>();
    List<RawField> properties = new ArrayList<>(), requirements = new ArrayList<>();
    List<ParsedModifier> modifiers = new ArrayList<>();
    Integer itemLevel = null;
    int levelCount = 0, levelSection = -1, requirementsSection = -1, previousSection = -1;
    boolean explicitFinished = false, explicitSection = false, reminder = false;
    TextLine metadata = null;
    for (TextLine line : content.subList(header + 1, content.size())) {
      String value = line.raw().strip(),
          key = key(value),
          fieldValue = key.isEmpty() ? "" : valueAfterColon(value);
      if (line.section() != previousSection) {
        if (explicitSection) explicitFinished = true;
        explicitSection = false;
        metadata = null;
        reminder = false;
        previousSection = line.section();
      }
      if (line.section() == 0) {
        if (Set.of("Item Class", "Rarity").contains(key))
          throw new IllegalArgumentException("Duplicate item header");
        names.add(line);
      } else if (key.equals("Item Level")) {
        levelCount++;
        levelSection = line.section();
        properties.add(new RawField(key, fieldValue, line));
        itemLevel = unsignedInteger(fieldValue);
        if (itemLevel == null) warnings.add(new Warning("INVALID_ITEM_LEVEL", line.number()));
      } else if (Set.of("Requires", "Requirements", "Requires Level", "Requires Class", "LevelReq")
              .contains(key)
          || value.startsWith("Requires ")) {
        requirements.add(
            new RawField(
                key.isEmpty() ? "Requires" : key,
                key.isEmpty() ? value.substring(9) : fieldValue,
                line));
        requirementsSection = fieldValue.isEmpty() && !key.isEmpty() ? line.section() : -1;
      } else if (line.section() == requirementsSection
          && Set.of("Level", "Str", "Dex", "Int", "Strength", "Dexterity", "Intelligence")
              .contains(key)) {
        requirements.add(new RawField(key, fieldValue, line));
      } else if (PROPERTY_KEYS.contains(key) || key.matches("Quality \\([A-Za-z ]+ Modifiers\\)")) {
        properties.add(new RawField(key, fieldValue, line));
      } else if (FLAGS.contains(value)) {
        flags.add(line);
        metadata = null;
      } else if (ADVANCED.matcher(value).matches()) {
        metadata = line;
        // Preserve even orphaned metadata in evidence; it never becomes a modifier itself.
        unparsed.add(line);
      } else if (value.startsWith("{") || value.startsWith("~") || value.matches("(?i)-?B/O .*")) {
        metadata = null;
        unparsed.add(line);
      } else if (reminder || value.matches("^\\([A-Za-z].*")) {
        reminder = !value.endsWith(")");
        unparsed.add(line);
      } else {
        var marker = MARKER.matcher(value);
        ModifierKind kind = null;
        String display = value;
        if (marker.find()) {
          kind = ModifierKind.valueOf(marker.group(1).toUpperCase(java.util.Locale.ROOT));
          display = value.substring(0, marker.start()).strip();
          marked.add(line);
        } else if (metadata != null) {
          kind = metadataKind(metadata.raw());
        } else if (!explicitFinished
            && line.section() > levelSection
            && levelSection >= 0
            && !value.startsWith("Grants Skill:")
            && key.isEmpty()) {
          // PoB's first post-level modifier section; no semantic/affix identity is inferred.
          kind = ModifierKind.EXPLICIT;
        }
        if (kind == null) {
          unparsed.add(line);
        } else {
          if (kind == ModifierKind.EXPLICIT
              || kind == ModifierKind.FRACTURED
              || kind == ModifierKind.CRAFTED
              || kind == ModifierKind.DESECRATED
              || kind == ModifierKind.MUTATED) explicitSection = true;
          String affix =
              metadata == null
                  ? null
                  : metadata.raw().matches(".*\\bPrefix\\b.*")
                      ? "PREFIX"
                      : metadata.raw().matches(".*\\bSuffix\\b.*") ? "SUFFIX" : null;
          Integer tier = metadata == null ? null : unsignedInteger(match(TIER, metadata.raw()));
          String affixName = metadata == null ? null : match(AFFIX_NAME, metadata.raw());
          modifiers.add(new ParsedModifier(display, kind, line, metadata, affix, tier, affixName));
        }
      }
    }
    if (names.isEmpty()) throw new IllegalArgumentException("Item name is missing");
    if (levelCount > 1) {
      itemLevel = null;
      warnings.add(new Warning("DUPLICATE_ITEM_LEVEL", 0));
    }
    if (levelCount == 0) warnings.add(new Warning("MISSING_ITEM_LEVEL", 0));
    String displayName = names.getFirst().raw().strip(), displayBase = null;
    if ((rarity == Rarity.RARE || rarity == Rarity.UNIQUE) && names.size() == 2)
      displayBase = names.get(1).raw().strip();
    else if (rarity == Rarity.NORMAL && names.size() == 1) displayBase = displayName;
    else warnings.add(new Warning("UNRESOLVED_BASE", names.getFirst().number()));
    if (!unparsed.isEmpty())
      warnings.add(new Warning("UNPARSED_LINES", unparsed.getFirst().number()));
    warnings.add(new Warning("CATALOG_VALIDATION_REQUIRED", 0));
    return new ParsedItemText(
        tokenized,
        "en",
        itemClass,
        rarity,
        rarityText,
        names,
        displayName,
        displayBase,
        itemLevel,
        properties,
        requirements,
        marked,
        modifiers,
        flags,
        unparsed,
        warnings);
  }

  private static ModifierKind metadataKind(String text) {
    String description =
        text.substring(0, text.indexOf("Modifier") >= 0 ? text.indexOf("Modifier") : text.length());
    for (ModifierKind kind :
        List.of(
            ModifierKind.FRACTURED,
            ModifierKind.CRAFTED,
            ModifierKind.DESECRATED,
            ModifierKind.MUTATED,
            ModifierKind.IMPLICIT,
            ModifierKind.RUNE,
            ModifierKind.ENCHANT)) {
      if (description.toUpperCase(java.util.Locale.ROOT).contains(kind.name())) return kind;
    }
    return description.contains("Enhancement")
        ? ModifierKind.ENCHANT
        : description.contains("Vaal Unique") ? ModifierKind.MUTATED : ModifierKind.EXPLICIT;
  }

  private static String match(Pattern pattern, String text) {
    var matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static Integer unsignedInteger(String value) {
    if (value == null || !value.matches("[0-9]+")) return null;
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static String key(String text) {
    int colon = text.indexOf(':');
    return colon < 0 ? "" : text.substring(0, colon).strip();
  }

  private static String valueAfterColon(String text) {
    return text.substring(text.indexOf(':') + 1).strip();
  }
}
