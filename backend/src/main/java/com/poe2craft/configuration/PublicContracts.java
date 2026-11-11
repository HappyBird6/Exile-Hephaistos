package com.poe2craft.configuration;

import com.fasterxml.jackson.databind.*;
import org.springframework.core.io.ClassPathResource;

/** Public transport contracts, distinct from environment overrides and sealed game rules. */
public final class PublicContracts {
  private PublicContracts() {}

  private static JsonNode read(String name) {
    try (var input = new ClassPathResource("contracts/" + name + ".json").getInputStream()) {
      return new ObjectMapper().readTree(input);
    } catch (java.io.IOException failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  public static final java.util.Set<String> LOCALE_CODES = loadLocales();

  private static java.util.Set<String> loadLocales() {
    var data = read("locales");
    var codes = new java.util.TreeSet<String>();
    data.path("locales").fieldNames().forEachRemaining(codes::add);
    if (codes.isEmpty() || !codes.contains(data.path("defaultLocale").asText()))
      throw new IllegalStateException("Invalid locale contract");
    return java.util.Set.copyOf(codes);
  }

  public static final Repeat REPEAT = loadRepeat();

  public record Repeat(
      int defaultUses,
      int minimumUses,
      int maximumUses,
      int maximumBodyBytes,
      int patternLength,
      int patternDepth,
      int patternNodes,
      int patternProgram) {}

  private static Repeat loadRepeat() {
    var data = read("chaos-repeat");
    var value =
        new Repeat(
            positive(data, "defaultUses"),
            positive(data, "minimumUses"),
            positive(data, "maximumUses"),
            positive(data, "maximumBodyBytes"),
            positive(data, "patternLength"),
            positive(data, "patternDepth"),
            positive(data, "patternNodes"),
            positive(data, "patternProgram"));
    if (value.minimumUses() > value.defaultUses() || value.defaultUses() > value.maximumUses())
      throw new IllegalStateException("Invalid repeat limits");
    return value;
  }

  private static int positive(JsonNode data, String key) {
    var value = data.path(key);
    if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1)
      throw new IllegalStateException("Invalid public contract: " + key);
    return value.intValue();
  }
}
