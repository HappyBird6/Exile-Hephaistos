package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/** Inverts only source-correlated English stat spans; never translated or rounded UI labels. */
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

  static ModifierInstance match(ModifierDefinition definition, String text) {
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
        // A one-decimal per-second observation may correspond to several per-minute rolls.
        if (divisor.abs().compareTo(BigDecimal.valueOf(60)) == 0 && amount.scale() < 2) return null;
        var raw = amount.multiply(divisor);
        long candidate = raw.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (raw.subtract(BigDecimal.valueOf(candidate))
                    .abs()
                    .compareTo(new BigDecimal("0.000000001"))
                > 0
            && BigDecimal.valueOf(candidate)
                    .divide(divisor, amount.scale(), RoundingMode.HALF_UP)
                    .compareTo(amount)
                != 0) return null;
        var stat =
            definition.stats().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null);
        if (stat == null || candidate < stat.min() || candidate > stat.max()) return null;
        var previous = values.putIfAbsent(id, candidate);
        if (previous != null && previous != candidate) return null;
      }
      return values.size() == definition.stats().size()
          ? new ModifierInstance(definition.id(), values)
          : null;
    } catch (ArithmeticException | IndexOutOfBoundsException invalid) {
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
    return text.trim().replaceAll("\\s+", " ");
  }
}
