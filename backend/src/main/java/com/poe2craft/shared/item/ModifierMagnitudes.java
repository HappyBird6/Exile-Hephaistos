package com.poe2craft.shared.item;

import java.math.BigDecimal;
import java.util.*;

/** Source-tagged explicit effects; original provider rolls select a single nonrecursive pass. */
public final class ModifierMagnitudes {
  private ModifierMagnitudes() {}

  public record Rule(String stat, List<String> tags, String description, List<String> consumers) {}

  public static boolean provider(ModifierDefinition definition) {
    return definition.stats().stream()
        .anyMatch(
            s ->
                AugmentCraftingRules.magnitudeRules().stream()
                    .anyMatch(r -> r.stat().equals(s.id())));
  }

  public static BigDecimal factor(List<ModifierInstance> providers, ModifierDefinition definition) {
    if (definition.layer() != ModifierDefinition.Layer.EXPLICIT
        || definition.tags().contains("unscalable")
        || definition.text().lines().anyMatch(l -> l.startsWith("Grants Skill:")))
      return BigDecimal.ONE;
    long amount = 0;
    for (var rule : AugmentCraftingRules.magnitudeRules())
      if (definition.tags().containsAll(rule.tags()))
        for (var modifier : providers) amount += modifier.values().getOrDefault(rule.stat(), 0L);
    return BigDecimal.ONE.add(BigDecimal.valueOf(amount, 2));
  }

  public static BigDecimal factor(
      ItemState state, ModifierDefinition definition, boolean implicit) {
    if (implicit || state.observedSanctified()) return BigDecimal.ONE;
    var raw = factor(state.explicits(), definition);
    if (state.sanctification() == null || raw.compareTo(BigDecimal.ONE) == 0) return raw;
    BigDecimal amount = BigDecimal.ZERO;
    for (var rule : AugmentCraftingRules.magnitudeRules())
      if (definition.tags().containsAll(rule.tags()))
        for (var provider : state.explicits())
          if (provider.values().containsKey(rule.stat()))
            amount = amount.add(QualitySanctification.value(state, provider, rule.stat(), false));
    return BigDecimal.ONE.add(amount.movePointLeft(2));
  }

  public static BigDecimal value(
      ItemState state,
      ModifierDefinition definition,
      ModifierInstance modifier,
      String stat,
      boolean implicit) {
    return QualitySanctification.value(state, modifier, stat, implicit)
        .multiply(factor(state, definition, implicit));
  }

  public static boolean rawMetadata(com.poe2craft.shared.item.text.ItemModels.TextLine metadata) {
    return metadata != null
        && java.util.regex.Pattern.compile("(?:—|-)\\s+[0-9]+(?:\\.[0-9]+)?% Increased\\s*}")
            .matcher(metadata.raw())
            .find();
  }

  public static BigDecimal clipboardFactor(
      ModifierDefinition definition,
      List<ModifierInstance> providers,
      com.poe2craft.shared.item.text.ItemModels.TextLine metadata,
      boolean finalValues) {
    return finalValues || rawMetadata(metadata) ? BigDecimal.ONE : factor(providers, definition);
  }

  /** Provider percentages may amplify their own displayed value, but never recursively. */
  static ModifierInstance matchProvider(
      ModifierDefinition definition, String text, List<ModifierInstance> providers) {
    var range = definition.stats().getFirst();
    if (definition.stats().size() != 1 || range.max() - range.min() > 100) return null;
    var numbers = java.util.regex.Pattern.compile("[+-]?\\d+(?:\\.\\d+)?").matcher(text);
    if (!numbers.find()) return null;
    var observed = new BigDecimal(numbers.group());
    ModifierInstance best = null;
    BigDecimal bestError = null;
    for (long value = range.min(); value <= range.max(); value++) {
      var candidate = new ModifierInstance(definition.id(), Map.of(range.id(), value));
      var context = new ArrayList<>(providers);
      context.add(candidate);
      var scale = factor(context, definition);
      var matched = ModifierClipboardText.match(definition, text, scale, true, false);
      if (matched == null || matched.values().get(range.id()) != value) continue;
      var error = BigDecimal.valueOf(value).multiply(scale).subtract(observed).abs();
      if (bestError == null || error.compareTo(bestError) < 0) {
        best = matched;
        bestError = error;
      }
    }
    return best;
  }
}
