package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;
import java.util.regex.Pattern;

/** Shared source-span arithmetic for copied Rune effects; structural limits never amplify. */
public final class SocketAugmentEffects {
  private SocketAugmentEffects() {}

  private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
  private static final JsonNode TEMPLATES = load();

  private static JsonNode load() {
    try (var input = SealedResources.open("/catalog/augment-effect-templates.json")) {
      return new ObjectMapper().readTree(input);
    } catch (java.io.IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static String visible(String text) {
    return text.replaceAll(
            ",?\\s*(?:local maximum mods allowed \\+|display additional socketable mods for idols) \\[[^\\]]+\\]",
            "")
        .trim();
  }

  public static double multiplier(ItemState state) {
    return multiplier(state, null);
  }

  public static double multiplier(ItemState state, ItemCatalog catalog) {
    double sum = 0;
    for (boolean implicit : List.of(true, false))
      for (var modifier : implicit ? state.implicits() : state.explicits()) {
        if (modifier.values().containsKey("local_socketed_items_effect_+%"))
          sum +=
              (catalog == null
                      ? QualitySanctification.value(
                          state, modifier, "local_socketed_items_effect_+%", implicit)
                      : ModifierMagnitudes.value(
                          state,
                          catalog.find(modifier.modifierId()).orElseThrow(),
                          modifier,
                          "local_socketed_items_effect_+%",
                          implicit))
                  .doubleValue();
      }
    return Math.max(0, 1 + sum / 100);
  }

  public static double runeMultiplier(
      List<ItemState.InstalledAugment> installed, double multiplier) {
    return multiplier
        + installed.stream()
                .mapToLong(
                    a ->
                        Math.round(
                            AugmentCraftingRules.effect(a.augmentId()).runeEffectPercent()
                                * multiplier))
                .sum()
            / 100.0;
  }

  public static boolean structural(String id) {
    var effect = AugmentCraftingRules.effect(id);
    return effect.craftedBonus() > 0
        || effect.suffixBonus() > 0
        || effect.maximumQuality() > 0
        || effect.pool() != null
        || effect.marker() != null;
  }

  public static List<String> render(
      String base,
      String id,
      List<ItemState.InstalledAugment> installed,
      double multiplier,
      boolean bonded) {
    double amount = multiplier;
    if (SocketAugments.kind(id).equals("Rune"))
      amount +=
          installed.stream()
                  .filter(a -> !a.augmentId().equals(id))
                  .mapToLong(
                      a ->
                          Math.round(
                              AugmentCraftingRules.effect(a.augmentId()).runeEffectPercent()
                                  * multiplier))
                  .sum()
              / 100.0;
    var result = new ArrayList<String>();
    var groups = bonded ? List.of("ordinary", "bonded") : List.of("ordinary");
    for (var group : groups)
      for (var row : TEMPLATES.path(id).path(group)) {
        boolean matches = false;
        for (var itemClass : row.path("classes"))
          if (itemClass.asText().equals(BaseRegistry.itemClass(base))) matches = true;
        if (!matches) continue;
        final double effectAmount = group.equals("ordinary") && structural(id) ? 1 : amount;
        for (var line : row.path("locales").path("en")) {
          String text = line.path("template").asText();
          for (int i = 0; i < line.path("values").size(); i++) {
            String value = line.path("values").get(i).asText();
            if (effectAmount != 1) {
              final double scale = effectAmount;
              value =
                  NUMBER
                      .matcher(value)
                      .replaceAll(
                          m -> Long.toString(Math.round(Double.parseDouble(m.group()) * scale)));
            }
            text = text.replace("{v" + i + "}", value);
          }
          visible(text).lines().filter(s -> !s.isBlank()).forEach(result::add);
        }
      }
    return List.copyOf(result);
  }

  public static String observed(String text, double ratio) {
    if (Math.abs(ratio - 1) < 1e-12) return text;
    return NUMBER
        .matcher(text)
        .replaceAll(m -> Long.toString(Math.round(Double.parseDouble(m.group()) * ratio)));
  }
}
