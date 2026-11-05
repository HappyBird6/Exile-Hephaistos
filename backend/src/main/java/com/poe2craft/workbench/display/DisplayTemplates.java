package com.poe2craft.workbench.display;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.ModifierDefinition;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, caller-supplied modifierTemplates.json. No filesystem, classpath or network loader.
 */
public final class DisplayTemplates {
  record ValueStat(String id, double divisor) {}

  record Binding(
      String englishText,
      List<ModifierDefinition.StatRange> stats,
      List<String> values,
      String template,
      List<ValueStat> valueStats) {}

  record Translation(String template, String name) {}

  private final Map<String, Binding> bindings;
  private final Map<DisplayLocale, Map<String, Translation>> templates;
  private final String sha256;

  private DisplayTemplates(
      Map<String, Binding> bindings,
      Map<DisplayLocale, Map<String, Translation>> templates,
      String sha256) {
    this.bindings = Map.copyOf(bindings);
    this.templates = Map.copyOf(templates);
    this.sha256 = sha256;
  }

  public String sha256() {
    return sha256;
  }

  /**
   * The integrator must supply bytes from the single original asset and its trusted manifest hash.
   */
  public static DisplayTemplates parse(byte[] bytes, String expectedSha256, ObjectMapper mapper) {
    try {
      String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!actual.equals(expectedSha256))
        throw new IllegalArgumentException("Display template hash mismatch");
      JsonNode root = mapper.readTree(bytes);
      var definitions = object(root.get("definitions"));
      Map<String, Binding> bindings = new LinkedHashMap<>();
      definitions
          .fields()
          .forEachRemaining(
              entry -> {
                JsonNode row = object(entry.getValue());
                List<ModifierDefinition.StatRange> stats = new ArrayList<>();
                for (var stat : array(row.get("stats"))) {
                  stats.add(
                      new ModifierDefinition.StatRange(
                          string(stat.get("id")),
                          integer(stat.get("min")),
                          integer(stat.get("max"))));
                }
                List<String> values = new ArrayList<>();
                for (var value : array(row.get("values"))) values.add(string(value));
                List<ValueStat> valueStats = null;
                if (row.has("valueStats")) {
                  valueStats = new ArrayList<>();
                  for (var stat : array(row.get("valueStats"))) {
                    var divisor = stat.get("divisor");
                    if (divisor == null
                        || !divisor.isNumber()
                        || !Double.isFinite(divisor.doubleValue())
                        || divisor.doubleValue() == 0)
                      throw new IllegalArgumentException("Invalid display divisor");
                    valueStats.add(new ValueStat(string(stat.get("id")), divisor.doubleValue()));
                  }
                  valueStats = List.copyOf(valueStats);
                }
                bindings.put(
                    entry.getKey(),
                    new Binding(
                        string(row.get("englishText")),
                        List.copyOf(stats),
                        List.copyOf(values),
                        string(row.get("template")),
                        valueStats));
              });
      Map<DisplayLocale, Map<String, Translation>> templates = new EnumMap<>(DisplayLocale.class);
      JsonNode locales = object(root.get("templates"));
      for (var locale : DisplayLocale.values()) {
        Map<String, Translation> translated = new LinkedHashMap<>();
        object(locales.get(locale.code()))
            .fields()
            .forEachRemaining(
                entry ->
                    translated.put(
                        entry.getKey(),
                        new Translation(
                            string(entry.getValue().get("template")),
                            string(entry.getValue().get("name")))));
        templates.put(locale, Map.copyOf(translated));
      }
      return new DisplayTemplates(bindings, templates, actual);
    } catch (IOException | NoSuchAlgorithmException invalid) {
      throw new IllegalArgumentException("Invalid display templates", invalid);
    }
  }

  Binding binding(ModifierDefinition definition) {
    Binding binding = bindings.get(definition.id());
    return binding != null
            && binding.englishText().equals(definition.text())
            && binding.stats().equals(definition.stats())
        ? binding
        : null;
  }

  Binding englishBinding(ModifierDefinition definition) {
    Binding binding = bindings.get(definition.id());
    return binding != null && binding.englishText().equals(definition.text()) ? binding : null;
  }

  boolean usesEnglishRevealFallback(
      ModifierDefinition definition, Binding binding, DisplayLocale locale) {
    return locale != DisplayLocale.EN
        && definition.id().startsWith("reveal:")
        && !templates.get(locale).containsKey(binding.template())
        && templates.get(DisplayLocale.EN).containsKey(binding.template());
  }

  Translation translation(ModifierDefinition definition, Binding binding, DisplayLocale locale) {
    var translation = templates.get(locale).get(binding.template());
    return translation == null && definition.id().startsWith("reveal:")
        ? templates.get(DisplayLocale.EN).get(binding.template())
        : translation;
  }

  private static JsonNode object(JsonNode node) {
    if (node == null || !node.isObject())
      throw new IllegalArgumentException("Display object required");
    return node;
  }

  private static JsonNode array(JsonNode node) {
    if (node == null || !node.isArray())
      throw new IllegalArgumentException("Display array required");
    return node;
  }

  private static String string(JsonNode node) {
    if (node == null || !node.isTextual())
      throw new IllegalArgumentException("Display string required");
    return node.textValue();
  }

  private static long integer(JsonNode node) {
    if (node == null || !node.isIntegralNumber() || !node.canConvertToLong())
      throw new IllegalArgumentException("Display integer required");
    return node.longValue();
  }
}
