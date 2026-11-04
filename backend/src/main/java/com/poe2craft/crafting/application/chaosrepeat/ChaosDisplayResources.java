package com.poe2craft.crafting.application.chaosrepeat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.chaosdisplay.*;
import com.poe2craft.item.*;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;

/** Exact build-time frontend input, pinned independently of the immutable game ruleset manifest. */
public final class ChaosDisplayResources {
  public static final String TEMPLATE_SHA256 =
      "a36cc5aaea06170a685ca2d8a322affb82e65a8c880732e7a9b63fd55ce09665";
  private final JsonNode bindings;
  private final ChaosOptionDisplay display;

  public ChaosDisplayResources(ObjectMapper json) {
    try (var stream =
        new ClassPathResource("chaosrepeat/modifierTemplates.json").getInputStream()) {
      var bytes = stream.readAllBytes();
      var templates = DisplayTemplates.parse(bytes, TEMPLATE_SHA256, json);
      bindings = json.readTree(bytes).path("definitions");
      display = new ChaosOptionDisplay(templates);
    } catch (IOException unavailable) {
      throw new IllegalStateException("Chaos display resources unavailable");
    }
  }

  public ChaosOptionDisplay.Result format(
      ItemState state, ItemCatalog catalog, DisplayLocale locale) {
    for (var modifier : state.explicits()) {
      var definition = catalog.find(modifier.modifierId()).orElseThrow(RepeatRejected::input);
      var binding = bindings.get(definition.id());
      if (binding != null
          && (!binding.path("englishText").asText().equals(definition.text())
              || !sameStats(binding.path("stats"), definition))) throw unsupported();
    }
    var result = display.format(state, catalog, locale);
    if (!result.supported()) throw unsupported();
    return result;
  }

  private static boolean sameStats(JsonNode stats, ModifierDefinition definition) {
    if (!stats.isArray() || stats.size() != definition.stats().size()) return false;
    for (int i = 0; i < stats.size(); i++) {
      var actual = stats.get(i);
      var expected = definition.stats().get(i);
      if (!actual.path("id").isTextual()
          || !actual.path("id").textValue().equals(expected.id())
          || !actual.path("min").isIntegralNumber()
          || !actual.path("min").canConvertToLong()
          || actual.path("min").longValue() != expected.min()
          || !actual.path("max").isIntegralNumber()
          || !actual.path("max").canConvertToLong()
          || actual.path("max").longValue() != expected.max()) return false;
    }
    return true;
  }

  private static RepeatRejected unsupported() {
    return new RepeatRejected(
        422,
        "UNSUPPORTED_REPEAT_DISPLAY",
        "This item has no verified option-only display for repeat matching. Original item values"
            + " are preserved.");
  }
}
