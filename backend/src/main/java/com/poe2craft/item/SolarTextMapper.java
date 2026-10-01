package com.poe2craft.item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict single-stat clipboard mapping. Ambiguity or unsupported evidence blocks the whole item.
 */
public final class SolarTextMapper {
  private static final Pattern RANGE =
      Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)\\s*[\u2014\u2013-]\\s*(-?\\d+(?:\\.\\d+)?)\\)");
  private final ItemCatalog catalog;

  public SolarTextMapper(ItemCatalog catalog) {
    this.catalog = catalog;
  }

  public Result map(ItemModels.Item item) {
    var issues = new ArrayList<Issue>();
    String base = item.displayBase() == null ? item.displayName() : item.displayBase();
    if (!"Solar Amulet".equals(base))
      issues.add(new Issue(0, "Only an exact Solar Amulet base can be mapped."));
    if (!Set.of("Amulet", "Amulets").contains(item.itemClass()))
      issues.add(new Issue(0, "Unsupported item class."));
    if (item.itemLevel() == null || item.itemLevel() < 1 || item.itemLevel() > 100)
      issues.add(new Issue(0, "Item level 1 to 100 is required."));
    if (!Set.of(ItemModels.Rarity.NORMAL, ItemModels.Rarity.MAGIC, ItemModels.Rarity.RARE)
        .contains(item.rarity())) issues.add(new Issue(0, "Unsupported rarity."));
    for (var line : item.unparsedLines())
      issues.add(new Issue(line.number(), "Unparsed line retained; crafting mapping is blocked."));
    for (var line : item.flags())
      issues.add(new Issue(line.number(), "Special item conditions are not supported."));
    for (var property : item.properties())
      if (!property.key().equals("Item Level"))
        issues.add(
            new Issue(
                property.source().number(),
                "This item property has no verified crafting-state representation."));
    var implicits = new ArrayList<ModifierInstance>();
    var explicits = new ArrayList<ModifierInstance>();
    for (var modifier : item.modifiers()) {
      if (modifier.type() != ItemModels.ModifierType.IMPLICIT
          && modifier.type() != ItemModels.ModifierType.EXPLICIT) {
        issues.add(new Issue(modifier.source().number(), "Unsupported modifier layer."));
        continue;
      }
      var layer =
          modifier.type() == ItemModels.ModifierType.IMPLICIT
              ? ModifierDefinition.Layer.IMPLICIT
              : ModifierDefinition.Layer.EXPLICIT;
      var matches = new ArrayList<ModifierInstance>();
      for (var definition : catalog.modifiers().values()) {
        if (definition.layer() != layer
            || (modifier.tier() != null && modifier.tier() != definition.tier())
            || (modifier.affix() != null
                && !modifier.affix().equals(definition.affixType().name()))) continue;
        var match = match(definition, modifier.text());
        if (match != null) matches.add(match);
      }
      if (matches.size() != 1)
        issues.add(
            new Issue(
                modifier.source().number(),
                matches.isEmpty()
                    ? "No exact catalog match and source-unit conversion was verified."
                    : "Multiple catalog tiers match this value; provide verified affix/tier metadata."));
      else
        (layer == ModifierDefinition.Layer.IMPLICIT ? implicits : explicits)
            .add(matches.getFirst());
    }
    if (!issues.isEmpty()) return new Result(false, null, issues);
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            item.itemLevel(),
            ItemState.Rarity.valueOf(item.rarity().name()),
            implicits,
            explicits,
            Set.of());
    for (var violation : new ItemStateValidator(catalog).validate(state))
      issues.add(new Issue(0, violation.message()));
    return new Result(issues.isEmpty(), issues.isEmpty() ? state : null, issues);
  }

  private ModifierInstance match(ModifierDefinition definition, String text) {
    if (definition.stats().size() != 1) return null;
    String source = normalize(definition.text()), valueText = normalize(text);
    var stat = definition.stats().getFirst();
    var range = RANGE.matcher(source);
    if (!range.find())
      return stat.min() == stat.max() && source.equals(valueText)
          ? new ModifierInstance(definition.id(), Map.of(stat.id(), stat.min()))
          : null;
    int start = range.start(), end = range.end();
    var pattern =
        Pattern.compile(
            "^"
                + Pattern.quote(source.substring(0, start))
                + "(-?\\d+(?:\\.\\d+)?)"
                + Pattern.quote(source.substring(end))
                + "$");
    var actual = pattern.matcher(valueText);
    if (!actual.matches()) return null;
    try {
      var low = new BigDecimal(range.group(1));
      var high = new BigDecimal(range.group(2));
      // Endpoint ratios do not establish display rounding/quantization. Only identical units are
      // safe.
      if (low.compareTo(BigDecimal.valueOf(stat.min())) != 0
          || high.compareTo(BigDecimal.valueOf(stat.max())) != 0) return null;
      long raw = new BigDecimal(actual.group(1)).longValueExact();
      if (raw < stat.min() || raw > stat.max()) return null;
      return new ModifierInstance(definition.id(), Map.of(stat.id(), raw));
    } catch (ArithmeticException ex) {
      return null;
    }
  }

  private String normalize(String text) {
    return text.trim().replaceAll("\\s+", " ");
  }

  public record Issue(int lineNumber, String message) {}

  public record Result(boolean mapped, ItemState state, List<Issue> issues) {
    public Result {
      issues = List.copyOf(issues);
    }
  }
}
