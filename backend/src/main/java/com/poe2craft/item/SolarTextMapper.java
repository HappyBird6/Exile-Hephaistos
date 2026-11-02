package com.poe2craft.item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict catalog-backed clipboard mapping. Ambiguity or unsupported evidence blocks the whole item.
 */
public final class SolarTextMapper {
  private static final Pattern RANGE =
      Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)\\s*[\u2014\u2013-]\\s*(-?\\d+(?:\\.\\d+)?)\\)");
  private final List<ItemCatalog> catalogs;

  public SolarTextMapper(ItemCatalog catalog) {
    this(List.of(catalog));
  }

  public SolarTextMapper(List<ItemCatalog> catalogs) {
    this.catalogs = List.copyOf(catalogs);
  }

  public Result map(ItemModels.Item item) {
    String name = item.displayBase() == null ? item.displayName() : item.displayBase();
    var candidates = catalogs.stream().filter(c -> c.base().name().equals(name)).toList();
    if (candidates.size() != 1)
      return new Result(
          false, null, List.of(new Issue(0, "An exact, unambiguous registered base is required.")));
    return map(item, candidates.getFirst());
  }

  private Result map(ItemModels.Item item, ItemCatalog catalog) {
    var issues = new ArrayList<Issue>();
    String expectedClass = BaseRegistry.itemClass(catalog.base().id());
    if (expectedClass == null
        || !(expectedClass.equals(item.itemClass())
            || (expectedClass.equals("Amulet") && item.itemClass().equals("Amulets"))
            || (expectedClass.equals("Amulets") && item.itemClass().equals("Amulet"))))
      issues.add(new Issue(0, "Item class does not match the registered base."));
    if (item.itemLevel() == null || item.itemLevel() < 1 || item.itemLevel() > 100)
      issues.add(new Issue(0, "Item level 1 to 100 is required."));
    if (!Set.of(ItemModels.Rarity.NORMAL, ItemModels.Rarity.MAGIC, ItemModels.Rarity.RARE)
        .contains(item.rarity())) issues.add(new Issue(0, "Unsupported rarity."));
    var attachedMetadata = new HashSet<Integer>();
    for (var modifier : item.modifiers())
      if (modifier.metadata() != null
          && modifier.affix() != null
          && modifier.affixName() != null
          && modifier.tier() != null
          && modifier
              .metadata()
              .raw()
              .matches("^\\{ (?:(?:Fractured|Desecrated) )?(?:Prefix|Suffix) Modifier \".*"))
        attachedMetadata.add(modifier.metadata().number());
    for (var line : item.unparsedLines())
      if (!attachedMetadata.contains(line.number()))
        issues.add(
            new Issue(line.number(), "Unparsed line retained; crafting mapping is blocked."));
    var desecrated = new ArrayList<ItemState.DesecratedSlot>();
    for (var line : item.flags()) {
      if (line.raw().equals("Desecrated Prefix") || line.raw().equals("Desecrated Suffix"))
        desecrated.add(
            new ItemState.DesecratedSlot(
                "clipboard-" + line.number(),
                line.raw().endsWith("Prefix")
                    ? ModifierDefinition.AffixType.PREFIX
                    : ModifierDefinition.AffixType.SUFFIX,
                null));
      else issues.add(new Issue(line.number(), "Special item conditions are not supported."));
    }
    Integer equipmentQuality = null;
    Integer sockets = null;
    var seenProperties = new HashSet<String>();
    for (var property : item.properties()) {
      if (!seenProperties.add(property.key())) {
        issues.add(new Issue(property.source().number(), "Duplicate item property."));
        continue;
      }
      if (property.key().equals("Item Level")) continue;
      if (!BaseRegistry.catalystQuality(catalog.base().id())
          && BaseRegistry.policy(catalog.base().id()).qualityLimit()
          && !BasicJewel.supportedCrafting(catalog.base().id())
          && property.key().equals("Quality")
          && property.value().matches("\\+?[0-9]{1,3}%(?: \\(augmented\\))?")) {
        equipmentQuality =
            Integer.parseInt(property.value().replaceFirst("^\\+", "").split("%", 2)[0]);
        if (equipmentQuality > 100)
          issues.add(new Issue(property.source().number(), "Invalid quality amount."));
      } else if (property.key().equals("Sockets")
          && property.value().trim().matches("S(?: S)*")
          && BaseRegistry.socketExecutionMaximum(catalog.base().id()) != null) {
        sockets = property.value().trim().split(" ").length;
        if (sockets > BaseRegistry.socketExecutionMaximum(catalog.base().id()))
          issues.add(
              new Issue(property.source().number(), "Socket count exceeds reviewed capacity."));
      } else
        issues.add(
            new Issue(
                property.source().number(),
                "This item property has no verified crafting-state representation."));
    }
    var implicits = new ArrayList<ModifierInstance>();
    var explicits = new ArrayList<ModifierInstance>();
    var groups = new LinkedHashMap<Integer, List<ItemModels.Modifier>>();
    for (var line : item.modifiers()) {
      int identity = line.metadata() == null ? line.source().number() : line.metadata().number();
      groups.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(line);
    }
    for (var group : groups.values()) {
      var modifier = group.getFirst();
      if (group.stream().anyMatch(line -> line.type() != modifier.type())) {
        issues.add(
            new Issue(
                modifier.source().number(), "Conflicting modifier layers in one metadata group."));
        continue;
      }
      String text =
          group.stream()
              .map(ItemModels.Modifier::text)
              .map(this::normalize)
              .collect(java.util.stream.Collectors.joining(""));
      if (modifier.type() != ItemModels.ModifierType.IMPLICIT
          && modifier.type() != ItemModels.ModifierType.EXPLICIT
          && modifier.type() != ItemModels.ModifierType.FRACTURED
          && modifier.type() != ItemModels.ModifierType.DESECRATED) {
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
            || (modifier.affixName() != null && !modifier.affixName().equals(definition.name()))
            || (modifier.tier() != null && modifier.tier() != definition.tier())
            || (modifier.affix() != null
                && !modifier.affix().equals(definition.affixType().name()))) continue;
        var match = match(definition, text);
        if (match != null)
          matches.add(
              new ModifierInstance(
                  match.modifierId(),
                  match.values(),
                  modifier.type() == ItemModels.ModifierType.FRACTURED));
      }
      if (matches.size() != 1)
        issues.add(
            new Issue(
                modifier.source().number(),
                matches.isEmpty()
                    ? "No exact catalog match and source-unit conversion was verified."
                    : "Multiple catalog tiers match this value; provide verified affix/tier metadata."));
      else {
        var matched = matches.getFirst();
        (layer == ModifierDefinition.Layer.IMPLICIT ? implicits : explicits).add(matched);
        if (modifier.type() == ItemModels.ModifierType.DESECRATED)
          desecrated.add(
              new ItemState.DesecratedSlot(
                  "clipboard-" + modifier.source().number(),
                  catalog.find(matched.modifierId()).orElseThrow().affixType(),
                  matched.modifierId()));
      }
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
                Set.of(),
                sockets)
            .withDesecrated(desecrated)
            .withEquipmentQuality(equipmentQuality);
    for (var violation : new ItemStateValidator(catalog).validate(state))
      issues.add(new Issue(0, violation.message()));
    return new Result(issues.isEmpty(), issues.isEmpty() ? state : null, issues);
  }

  private ModifierInstance match(ModifierDefinition definition, String text) {
    String source = normalize(definition.text()), valueText = normalize(text);
    var values = new LinkedHashMap<String, Long>();
    for (var stat : definition.stats())
      if (stat.min() == stat.max()) values.put(stat.id(), stat.min());
    var range = RANGE.matcher(source);
    var bindings = new ArrayList<ModifierDefinition.StatRange>();
    var used = new HashSet<String>();
    var regex = new StringBuilder("^");
    int end = 0;
    while (range.find()) {
      var low = new BigDecimal(range.group(1));
      var high = new BigDecimal(range.group(2));
      var candidates =
          definition.stats().stream()
              .filter(
                  stat ->
                      low.compareTo(BigDecimal.valueOf(stat.min())) == 0
                          && high.compareTo(BigDecimal.valueOf(stat.max())) == 0)
              .toList();
      // Never associate sorted stat IDs with textual positions or infer unit conversions.
      if (candidates.size() != 1 || !used.add(candidates.getFirst().id())) return null;
      var stat = candidates.getFirst();
      bindings.add(stat);
      regex
          .append(Pattern.quote(source.substring(end, range.start())))
          .append("(-?\\d+(?:\\.\\d+)?)")
          .append("(?:\\(")
          .append(Pattern.quote(range.group(1)))
          .append("\\s*[\\u2014\\u2013-]\\s*")
          .append(Pattern.quote(range.group(2)))
          .append("\\))?");
      end = range.end();
    }
    regex.append(Pattern.quote(source.substring(end))).append("$");
    var actual = Pattern.compile(regex.toString()).matcher(valueText);
    if (!actual.matches()) return null;
    try {
      for (int i = 0; i < bindings.size(); i++) {
        var stat = bindings.get(i);
        long raw = new BigDecimal(actual.group(i + 1)).longValueExact();
        if (raw < stat.min() || raw > stat.max()) return null;
        values.put(stat.id(), raw);
      }
      if (values.size() != definition.stats().size()) return null;
      return new ModifierInstance(definition.id(), values);
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
