package com.poe2craft.shared.item;

import com.poe2craft.shared.item.text.ItemModels;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Catalog-backed current-item mapping; known display scaling is inverted to a usable nearest roll.
 */
public final class SolarTextMapper {
  private static final Pattern RANGE =
      Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)\\s*[\u2014\u2013-]\\s*(-?\\d+(?:\\.\\d+)?)\\)");
  private static final Pattern DESECRATION_MINIMUM =
      Pattern.compile("\\(Minimum Modifier Level: ([^)]*)\\)");
  private static final Pattern DESECRATION_POOL = Pattern.compile("\\(Reveal Pool: ([^)]*)\\)");
  private final List<ItemCatalog> catalogs;

  public SolarTextMapper(ItemCatalog catalog) {
    this(List.of(catalog));
  }

  public SolarTextMapper(List<ItemCatalog> catalogs) {
    this.catalogs = catalogs.stream().map(ManualRevealCatalog::enrich).toList();
  }

  public Result map(ItemModels.Item item) {
    String name = item.displayBase() == null ? item.displayName() : item.displayBase();
    var candidates = catalogs.stream().filter(c -> c.base().name().equals(name)).toList();
    if (candidates.size() > 1) {
      var skills =
          item.unparsedLines().stream().filter(l -> l.raw().startsWith("Grants Skill: ")).toList();
      if (!skills.isEmpty())
        candidates =
            candidates.stream()
                .filter(
                    c ->
                        skills.stream()
                            .allMatch(
                                l -> BaseRegistry.recognizesGrantedSkill(c.base().id(), l.raw())))
                .toList();
    }
    if (candidates.size() > 1) {
      var resolved = candidates.stream().map(c -> map(item, c)).filter(Result::mapped).toList();
      if (resolved.size() == 1) return resolved.getFirst();
    }
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
    var skills = new ArrayList<ItemObservations.GrantedSkill>();
    for (var line : item.unparsedLines()) {
      var skill = BaseRegistry.observedGrantedSkill(catalog.base().id(), line.raw());
      if (skill != null) skills.add(skill);
    }
    if (skills.stream().map(ItemObservations.GrantedSkill::name).distinct().count()
        != skills.size())
      issues.add(new Issue(0, "Duplicate or conflicting granted-skill observations."));
    var augmentEffects = new ArrayList<String>();
    var attachedMetadata = new HashSet<Integer>();
    for (var modifier : item.modifiers())
      if (modifier.type() == ItemModels.ModifierType.RUNE && modifier.metadata() != null)
        attachedMetadata.add(modifier.metadata().number());
    for (var modifier : item.modifiers())
      if (modifier.metadata() != null
          && modifier.type() == ItemModels.ModifierType.IMPLICIT
          && modifier.metadata().raw().startsWith("{ Implicit Modifier"))
        attachedMetadata.add(modifier.metadata().number());
    for (var modifier : item.modifiers())
      if (modifier.metadata() != null
          && modifier.affix() != null
          && modifier.affixName() != null
          && modifier
              .metadata()
              .raw()
              .matches(
                  "^\\{ (?:(?:Fractured|Desecrated|Crafted) )*(?:Prefix|Suffix) Modifier \".*"))
        attachedMetadata.add(modifier.metadata().number());
    for (var line : item.unparsedLines())
      if (!attachedMetadata.contains(line.number())
          && !BaseRegistry.recognizesGrantedSkill(catalog.base().id(), line.raw()))
        issues.add(
            new Issue(line.number(), "Unparsed line retained; crafting mapping is blocked."));
    var desecrated = new ArrayList<ItemState.DesecratedSlot>();
    var conditions = new HashSet<ItemState.Condition>();
    for (var line : item.flags()) {
      if (line.raw().startsWith("Desecrated Prefix")
          || line.raw().startsWith("Desecrated Suffix")) {
        var details = desecrationDetails(line.raw(), line.number(), issues);
        desecrated.add(
            new ItemState.DesecratedSlot(
                "clipboard-" + line.number(),
                line.raw().startsWith("Desecrated Prefix")
                    ? ModifierDefinition.AffixType.PREFIX
                    : ModifierDefinition.AffixType.SUFFIX,
                null,
                details.minimumLevel(),
                details.pool()));
      } else if (line.raw().equals("Corrupted")) conditions.add(ItemState.Condition.CORRUPTED);
      else if (line.raw().equals("Sanctified")) conditions.add(ItemState.Condition.SANCTIFIED);
      else issues.add(new Issue(line.number(), "Special item conditions are not supported."));
    }
    Integer equipmentQuality = null;
    CatalystQuality catalystQuality = null;
    Integer sockets = null;
    Integer observedSockets = null;
    var seenProperties = new HashSet<String>();
    for (var property : item.properties()) {
      if (!seenProperties.add(property.key())) {
        issues.add(new Issue(property.source().number(), "Duplicate item property."));
        continue;
      }
      if (property.key().equals("Item Level")) continue;
      // These are clipboard observations, not raw modifier rolls or inputs to currency rules.
      // ItemModels retains their exact lines; the workbench film retains the original document.
      if (Set.of(
                  "Armour",
                  "Evasion Rating",
                  "Evasion",
                  "Energy Shield",
                  "Runic Ward",
                  "Spirit",
                  "Physical Damage",
                  "Elemental Damage",
                  "Chaos Damage",
                  "Fire Damage",
                  "Cold Damage",
                  "Lightning Damage",
                  "Critical Hit Chance",
                  "Attacks per Second",
                  "Chance to Block",
                  "Block Chance",
                  "Block chance",
                  "Reload Time",
                  "Weapon Range")
              .contains(property.key())
          && property
              .value()
              .matches(
                  "[0-9]+(?:\\.[0-9]+)?(?:-[0-9]+(?:\\.[0-9]+)?)?(?:, [0-9]+(?:-[0-9]+)?)*%?(?: \\(augmented\\))?"))
        continue;
      if (BaseRegistry.catalystQuality(catalog.base().id())
          && catalystType(property.key()) != null
          && property.value().matches("\\+?[0-9]{1,3}%(?: \\(augmented\\))?")) {
        int amount = Integer.parseInt(property.value().replaceFirst("^\\+", "").split("%", 2)[0]);
        if (catalystQuality != null || amount > 100)
          issues.add(
              new Issue(property.source().number(), "Conflicting or invalid catalyst quality."));
        else catalystQuality = new CatalystQuality(catalystType(property.key()), amount);
      } else if (!BaseRegistry.catalystQuality(catalog.base().id())
          && BaseRegistry.policy(catalog.base().id()).qualityLimit()
          && !BasicJewel.supportedCrafting(catalog.base().id())
          && property.key().equals("Quality")
          && property.value().matches("\\+?[0-9]{1,3}%(?: \\(augmented\\))?")) {
        equipmentQuality =
            Integer.parseInt(property.value().replaceFirst("^\\+", "").split("%", 2)[0]);
        if (equipmentQuality > 100)
          issues.add(new Issue(property.source().number(), "Invalid quality amount."));
      } else if (property.key().equals("Sockets") && property.value().trim().matches("S(?: S)*")) {
        observedSockets = property.value().trim().split(" ").length;
        // Observed count is retained; crafting capacity still comes from the reviewed base policy.
      } else if (property.key().equals("Rune") && !property.value().isBlank()) {
        augmentEffects.add(property.source().raw());
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
      if (line.type() == ItemModels.ModifierType.RUNE) {
        augmentEffects.add(
            (line.metadata() == null ? "" : line.metadata().raw() + "\n") + line.source().raw());
        continue;
      }
      int identity =
          line.metadata() == null
              ? (line.type() == ItemModels.ModifierType.IMPLICIT
                  ? -1 - line.source().section()
                  : line.source().number())
              : line.metadata().number();
      groups.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(line);
    }
    boolean finalValues = conditions.contains(ItemState.Condition.SANCTIFIED);
    var ordered = new ArrayList<>(groups.values());
    ordered.sort(
        java.util.Comparator.comparingInt(
            g ->
                g.stream()
                        .anyMatch(
                            m ->
                                m.text()
                                    .toLowerCase(java.util.Locale.ROOT)
                                    .contains("modifier magnitudes"))
                    ? 0
                    : 1));
    for (var group : ordered) {
      var modifier = group.getFirst();
      if (modifier.metadata() != null
          && ((modifier.metadata().raw().startsWith("{ Fractured ")
                  && modifier.type() != ItemModels.ModifierType.FRACTURED)
              || (modifier.metadata().raw().startsWith("{ Desecrated ")
                  && modifier.type() != ItemModels.ModifierType.DESECRATED))) {
        issues.add(new Issue(modifier.source().number(), "Conflicting modifier layer markers."));
        continue;
      }
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
              .collect(java.util.stream.Collectors.joining("\n"));
      if (modifier.type() != ItemModels.ModifierType.IMPLICIT
          && modifier.type() != ItemModels.ModifierType.EXPLICIT
          && modifier.type() != ItemModels.ModifierType.FRACTURED
          && modifier.type() != ItemModels.ModifierType.DESECRATED
          && modifier.type() != ItemModels.ModifierType.CRAFTED) {
        issues.add(new Issue(modifier.source().number(), "Unsupported modifier layer."));
        continue;
      }
      var layer =
          modifier.type() == ItemModels.ModifierType.IMPLICIT
              ? ModifierDefinition.Layer.IMPLICIT
              : ModifierDefinition.Layer.EXPLICIT;
      var matches = new ArrayList<ModifierInstance>();
      for (var definition : catalog.modifiers().values()) {
        if ((definition.tags().contains("reveal_only")
                && modifier.type() != ItemModels.ModifierType.DESECRATED)
            || definition.layer() != layer
            || (modifier.affixName() != null && !modifier.affixName().equals(definition.name()))
            || (modifier.tier() != null && modifier.tier() != definition.tier())
            || (modifier.affix() != null
                && !modifier.affix().equals(definition.affixType().name()))) continue;
        var scale =
            clipboardScale(definition, catalystQuality, modifier.metadata())
                .multiply(
                    ModifierMagnitudes.clipboardFactor(
                        definition, explicits, modifier.metadata(), finalValues));
        var match =
            !finalValues
                    && ModifierMagnitudes.provider(definition)
                    && !ModifierMagnitudes.rawMetadata(modifier.metadata())
                ? ModifierMagnitudes.matchProvider(definition, text, explicits)
                : match(definition, text, scale, finalValues);
        if (match != null)
          matches.add(
              new ModifierInstance(
                  match.modifierId(),
                  match.values(),
                  modifier.type() == ItemModels.ModifierType.FRACTURED,
                  modifier.type() == ItemModels.ModifierType.CRAFTED
                      || (modifier.metadata() != null
                          && modifier.metadata().raw().contains("Crafted "))));
      }
      if (matches.size() > 1) {
        var first = catalog.find(matches.getFirst().modifierId()).orElseThrow();
        boolean sameEffect =
            matches.stream()
                .allMatch(
                    candidate -> {
                      var other = catalog.find(candidate.modifierId()).orElseThrow();
                      return first.tags().equals(other.tags())
                          && first.familyIds().equals(other.familyIds())
                          && first.affixType() == other.affixType()
                          && first.layer() == other.layer()
                          && first.stats().stream()
                              .map(ModifierDefinition.StatRange::id)
                              .toList()
                              .equals(
                                  other.stats().stream()
                                      .map(ModifierDefinition.StatRange::id)
                                      .toList());
                    });
        if (sameEffect) {
          var quality = catalystQuality;
          matches.sort(
              java.util.Comparator.comparingInt(
                      (ModifierInstance candidate) -> {
                        var d = catalog.find(candidate.modifierId()).orElseThrow();
                        return ModifierClipboardText.match(
                                    d,
                                    text,
                                    clipboardScale(d, quality, modifier.metadata())
                                        .multiply(
                                            ModifierMagnitudes.clipboardFactor(
                                                d, explicits, modifier.metadata(), finalValues)),
                                    false,
                                    finalValues)
                                != null
                            ? 0
                            : 1;
                      })
                  .thenComparingInt(
                      candidate ->
                          catalog.find(candidate.modifierId()).orElseThrow().weight() > 0 ? 0 : 1)
                  .thenComparing(ModifierInstance::modifierId));
          var canonical = matches.getFirst();
          matches.clear();
          matches.add(canonical);
        }
      }
      if (matches.size() != 1)
        issues.add(
            new Issue(
                modifier.source().number(),
                matches.isEmpty()
                    ? (catalystQuality != null && catalystQuality.amount() > 0
                        ? "No exact catalog match; catalyst-affected values need original unscaled rolls, tier and verified rounding."
                        : "No exact catalog match and source-unit conversion was verified.")
                    : "Multiple catalog tiers match this value; provide verified affix/tier metadata."));
      else {
        var matched = matches.getFirst();
        var definition = catalog.find(matched.modifierId()).orElseThrow();
        (layer == ModifierDefinition.Layer.IMPLICIT ? implicits : explicits).add(matched);
        if (modifier.type() == ItemModels.ModifierType.DESECRATED) {
          var details =
              desecrationDetails(
                  modifier.metadata() == null ? "" : modifier.metadata().raw(),
                  modifier.source().number(),
                  issues);
          desecrated.add(
              new ItemState.DesecratedSlot(
                  "clipboard-" + modifier.source().number(),
                  definition.affixType(),
                  matched.modifierId(),
                  details.minimumLevel(),
                  details.pool() == null && definition.tags().contains("otherworldly")
                      ? "ALTERED"
                      : details.pool()));
        }
      }
    }
    if (implicits.isEmpty() && catalog.base().hasImplicit()) {
      var innate = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
      if (ModifierClipboardText.visibleText(innate.text()).isBlank()
          && innate.stats().stream().allMatch(stat -> stat.min() == stat.max())) {
        var values = new LinkedHashMap<String, Long>();
        innate.stats().forEach(stat -> values.put(stat.id(), stat.min()));
        implicits.add(new ModifierInstance(innate.id(), values));
      }
    }
    if (conditions.contains(ItemState.Condition.CORRUPTED) && desecrated.isEmpty())
      issues.add(new Issue(0, "Special item conditions are not supported."));
    if (!issues.isEmpty()) return new Result(false, null, issues);
    double multiplier = 1;
    for (var modifier : implicits)
      multiplier += modifier.values().getOrDefault("local_socketed_items_effect_+%", 0L) / 100.0;
    for (var modifier : explicits)
      if (modifier.values().containsKey("local_socketed_items_effect_+%"))
        multiplier +=
            modifier.values().get("local_socketed_items_effect_+%")
                * (finalValues
                    ? 1
                    : ModifierMagnitudes.factor(
                            explicits, catalog.find(modifier.modifierId()).orElseThrow())
                        .doubleValue())
                / 100;
    var resolved =
        SocketAugmentText.resolve(catalog.base().id(), observedSockets, augmentEffects, multiplier);
    double observedScale = SocketAugmentEffects.runeMultiplier(resolved.installed(), multiplier);
    Integer maximumSockets = BaseRegistry.socketExecutionMaximum(catalog.base().id());
    if (observedSockets != null && maximumSockets != null && observedSockets <= maximumSockets)
      sockets = observedSockets;
    var unresolvedSocketCount = sockets == null ? observedSockets : null;
    var observations =
        unresolvedSocketCount == null && resolved.unidentified().isEmpty() && skills.isEmpty()
            ? null
            : new ItemObservations(
                unresolvedSocketCount,
                resolved.unidentified(),
                skills,
                !resolved.unidentified().isEmpty() && observedScale != 1 ? observedScale : null);
    var state =
        new ItemState(
                catalog.metadata().snapshotId(),
                catalog.base().id(),
                item.itemLevel(),
                ItemState.Rarity.valueOf(item.rarity().name()),
                implicits,
                explicits,
                conditions,
                sockets,
                catalystQuality)
            .withDesecrated(desecrated)
            .withEquipmentQuality(equipmentQuality)
            .withObservations(observations)
            .withAugments(resolved.installed());
    for (var message : ItemTextStateValidation.validate(state, catalog))
      issues.add(new Issue(0, message));
    return new Result(issues.isEmpty(), issues.isEmpty() ? state : null, issues);
  }

  private record DesecrationDetails(Integer minimumLevel, String pool) {}

  private static DesecrationDetails desecrationDetails(String text, int line, List<Issue> issues) {
    Integer minimum = null;
    String pool = null;
    var levelMatch = DESECRATION_MINIMUM.matcher(text);
    if (levelMatch.find()) {
      if (!levelMatch.group(1).equals("40") || levelMatch.find())
        issues.add(new Issue(line, "Unsupported or duplicate desecration level constraint."));
      else minimum = 40;
    } else if (text.contains("Minimum Modifier Level:"))
      issues.add(new Issue(line, "Invalid desecration level constraint."));
    var poolMatch = DESECRATION_POOL.matcher(text);
    if (poolMatch.find()) {
      var value = poolMatch.group(1);
      if (!Set.of("ALTERED", "AMANAMU", "KURGAL", "ULAMAN").contains(value) || poolMatch.find())
        issues.add(new Issue(line, "Unsupported or duplicate desecration reveal pool."));
      else pool = value;
    } else if (text.contains("Reveal Pool:"))
      issues.add(new Issue(line, "Invalid desecration reveal pool."));
    return new DesecrationDetails(minimum, pool);
  }

  private static CatalystQuality.Type catalystType(String label) {
    return switch (label) {
      case "Quality (Life Modifiers)" -> CatalystQuality.Type.FLESH;
      case "Quality (Mana Modifiers)" -> CatalystQuality.Type.NEURAL;
      case "Quality (Defence Modifiers)", "Quality (Defences Modifiers)" ->
          CatalystQuality.Type.CARAPACE;
      case "Quality (Physical Modifiers)" -> CatalystQuality.Type.UUL_NETOL;
      case "Quality (Fire Modifiers)" -> CatalystQuality.Type.XOPH;
      case "Quality (Cold Modifiers)" -> CatalystQuality.Type.TUL;
      case "Quality (Lightning Modifiers)" -> CatalystQuality.Type.ESH;
      case "Quality (Chaos Modifiers)" -> CatalystQuality.Type.CHAYULA;
      case "Quality (Attack Modifiers)" -> CatalystQuality.Type.REAVER;
      case "Quality (Caster Modifiers)" -> CatalystQuality.Type.SIBILANT;
      case "Quality (Speed Modifiers)" -> CatalystQuality.Type.SKITTERING;
      case "Quality (Attribute Modifiers)" -> CatalystQuality.Type.ADAPTIVE;
      case "Quality (Minion Modifiers)" -> CatalystQuality.Type.NECROTIC;
      default -> null;
    };
  }

  private static BigDecimal clipboardScale(
      ModifierDefinition definition, CatalystQuality quality, ItemModels.TextLine metadata) {
    if (quality == null
        || quality.amount() == 0
        || !quality.type().matches(definition.tags())
        || definition.tags().contains("unscalable")
        || definition.stats().stream().anyMatch(stat -> stat.id().equals("local_maximum_quality_+"))
        || (metadata != null
            && Pattern.compile("(?:—|-)\\s+" + quality.amount() + "% Increased\\s*}")
                .matcher(metadata.raw())
                .find())) return BigDecimal.ONE;
    return BigDecimal.valueOf(100 + quality.amount(), 2);
  }

  private ModifierInstance match(
      ModifierDefinition definition, String text, BigDecimal scale, boolean finalValues) {
    var converted = ModifierClipboardText.match(definition, text, scale, true, finalValues);
    if (converted != null) return converted;
    String source = normalize(ModifierClipboardText.visibleText(definition.text())),
        valueText = normalize(text);
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
    if (scale.compareTo(BigDecimal.ONE) != 0 && bindings.isEmpty()) return null;
    regex.append(Pattern.quote(source.substring(end))).append("$");
    var actual = Pattern.compile(regex.toString()).matcher(valueText);
    if (!actual.matches()) return null;
    try {
      for (int i = 0; i < bindings.size(); i++) {
        var stat = bindings.get(i);
        Long raw =
            ModifierClipboardText.sourceRoll(
                new BigDecimal(actual.group(i + 1)),
                BigDecimal.ONE,
                scale,
                finalValues ? Long.MIN_VALUE : stat.min(),
                finalValues ? Long.MAX_VALUE : stat.max(),
                true,
                finalValues);
        if (raw == null) return null;
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
