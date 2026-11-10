package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * Inverts known English stat spans, accepting game display rounding without inferring hidden
 * history.
 */
final class ModifierClipboardText {
  private ModifierClipboardText() {}

  private static final JsonNode DATA = load();
  private static final Pattern TOKEN = Pattern.compile("\\{v(\\d+)\\}");
  private static final Pattern RANGE =
      Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)\\s*[—–-]\\s*(-?\\d+(?:\\.\\d+)?)\\)");

  private static JsonNode load() {
    try (var input = new java.io.ByteArrayInputStream(ModifierTextResources.read())) {
      return new ObjectMapper().readTree(input);
    } catch (java.io.IOException invalid) {
      throw new IllegalStateException("Invalid shared modifier text bindings", invalid);
    }
  }

  static boolean hasVerifiedValues(ModifierDefinition definition) {
    var binding = DATA.path("definitions").path(definition.id());
    if (!binding.path("englishText").asText().equals(definition.text())
        || !binding.path("valueStats").isArray()
        || binding.path("stats").size() != definition.stats().size()) return false;
    for (int i = 0; i < definition.stats().size(); i++) {
      var source = binding.path("stats").get(i);
      var stat = definition.stats().get(i);
      if (!source.path("id").asText().equals(stat.id())
          || source.path("min").asLong() != stat.min()
          || source.path("max").asLong() != stat.max()) return false;
    }
    return true;
  }

  static ModifierInstance match(ModifierDefinition definition, String text) {
    return match(definition, text, BigDecimal.ONE, true, false);
  }

  static ModifierInstance match(
      ModifierDefinition definition,
      String text,
      BigDecimal scale,
      boolean rounded,
      boolean finalValues) {
    var binding = DATA.path("definitions").path(definition.id());
    if (!binding.path("englishText").asText().equals(definition.text())
        || binding.path("stats").size() != definition.stats().size()) return null;
    for (int i = 0; i < definition.stats().size(); i++) {
      var source = binding.path("stats").get(i);
      var stat = definition.stats().get(i);
      if (!source.path("id").asText().equals(stat.id())
          || source.path("min").asLong() != stat.min()
          || source.path("max").asLong() != stat.max()) return null;
    }
    var converters = binding.path("valueStats");
    boolean jewel =
        definition.id().matches("^(ruby|emerald|sapphire|diamond):.*")
            && !definition.tags().contains("unscalable");
    if (!converters.isArray() && !jewel) return null;
    String template =
        DATA.path("templates")
            .path("en")
            .path(binding.path("template").asText())
            .path("template")
            .asText();
    if (template.isBlank()) return null;
    template = normalize(visibleText(template));
    var tokens = TOKEN.matcher(template);
    var indices = new ArrayList<Integer>();
    var regex = new StringBuilder("^");
    int end = 0;
    while (tokens.find()) {
      int index = Integer.parseInt(tokens.group(1));
      indices.add(index);
      regex
          .append(Pattern.quote(template.substring(end, tokens.start())))
          .append("([+-]?\\d+(?:\\.\\d+)?)");
      var bounds = RANGE.matcher(binding.path("values").path(index).asText());
      if (bounds.find())
        regex
            .append("(?:\\(")
            .append(Pattern.quote(bounds.group(1)))
            .append("\\s*[—–-]\\s*")
            .append(Pattern.quote(bounds.group(2)))
            .append("\\))?");
      end = tokens.end();
    }
    regex.append(Pattern.quote(template.substring(end))).append("$");
    var actual = Pattern.compile(regex.toString()).matcher(normalize(text));
    if (!actual.matches()) return null;
    var values = new LinkedHashMap<String, Long>();
    for (var stat : definition.stats())
      if (stat.min() == stat.max()) values.put(stat.id(), stat.min());
    var observed = new java.util.HashSet<String>();
    try {
      for (int i = 0; i < indices.size(); i++) {
        int index = indices.get(i);
        String id =
            converters.isArray()
                ? converters.path(index).path("id").asText()
                : definition.stats().get(index).id();
        var divisor =
            converters.isArray()
                ? converters.path(index).path("divisor").decimalValue()
                : BigDecimal.ONE;
        if (divisor == null || divisor.signum() == 0) return null;
        var amount = new BigDecimal(actual.group(i + 1));
        var stat =
            definition.stats().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null);
        if (stat == null) return null;
        Long candidate =
            sourceRoll(
                amount,
                divisor,
                scale,
                finalValues ? Long.MIN_VALUE : stat.min(),
                finalValues ? Long.MAX_VALUE : stat.max(),
                rounded,
                finalValues);
        if (candidate == null) return null;
        if (finalValues && observed.add(id)) values.put(id, candidate);
        else {
          var previous = values.putIfAbsent(id, candidate);
          if (previous != null && !previous.equals(candidate)) return null;
        }
      }
      return values.size() == definition.stats().size()
          ? new ModifierInstance(definition.id(), values)
          : null;
    } catch (ArithmeticException | IndexOutOfBoundsException invalid) {
      return null;
    }
  }

  /** Nearest reachable roll; rounding is permitted only for a known unit or catalyst conversion. */
  static Long sourceRoll(
      BigDecimal amount,
      BigDecimal divisor,
      BigDecimal scale,
      long minimum,
      long maximum,
      boolean rounded,
      boolean finalValues) {
    try {
      var raw = amount.multiply(divisor).divide(scale, 20, RoundingMode.HALF_UP);
      long nearest = raw.setScale(0, RoundingMode.HALF_UP).longValueExact();
      long candidate = Math.clamp(nearest, minimum, maximum);
      if (raw.subtract(BigDecimal.valueOf(candidate)).abs().compareTo(new BigDecimal("0.000000001"))
          <= 0) return candidate;
      if (finalValues) return candidate;
      if (!rounded
          || (divisor.abs().compareTo(BigDecimal.ONE) == 0 && scale.compareTo(BigDecimal.ONE) == 0))
        return null;
      var display =
          BigDecimal.valueOf(candidate).multiply(scale).divide(divisor, 20, RoundingMode.HALF_UP);
      // Game copy may round or truncate. One unit of its displayed precision bounds the error.
      var quantum = BigDecimal.ONE.scaleByPowerOfTen(-Math.max(0, amount.scale()));
      return display.subtract(amount).abs().compareTo(quantum) <= 0 ? candidate : null;
    } catch (ArithmeticException invalid) {
      return null;
    }
  }

  static String visibleText(String text) {
    return text.lines()
        .filter(
            line ->
                !line.matches(
                    "^local (?:weapon implicit hidden added .* damage|display grants spear throw skill) \\[(?:-?\\d+|\\{v\\d+\\})\\]$"))
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private static String normalize(String text) {
    return text.trim().replaceAll("\\s+", " ").replaceAll("\\bsecond\\b", "seconds");
  }
}
