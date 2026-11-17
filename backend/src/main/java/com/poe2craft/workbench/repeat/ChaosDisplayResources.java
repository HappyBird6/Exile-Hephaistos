package com.poe2craft.workbench.repeat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierTextResources;
import com.poe2craft.workbench.display.ChaosOptionDisplay;
import com.poe2craft.workbench.display.DisplayLocale;
import com.poe2craft.workbench.display.DisplayTemplates;
import java.io.IOException;

/**
 * 빌드 시 복사한 프런트 표시 자료의 해시를 검증해 반복 제작에 제공한다.
 *
 * <p>기존 계약: Exact build-time frontend input, pinned independently of the immutable game ruleset
 * manifest.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code TEMPLATE_SHA256}: 256 ({@code String}).
 *   <li>{@code bindings}: 결합목록 ({@code JsonNode}).
 *   <li>{@code display}: 표시 ({@code ChaosOptionDisplay}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosDisplayResources(json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON). 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code format(state, catalog, locale)}: 지정 언어와 표시 규칙에 맞는 문자열을 만든다. 입력: {@code state}(상태),
 *       {@code catalog}(카탈로그), {@code locale}(언어코드). 반환: {@code ChaosOptionDisplay.Result}.
 *   <li>{@code sameStats(stats, definition)}: 동일·수치목록 처리에서 {@code stats.isArray}, {@code
 *       stats.size}을(를) 사용한다. 입력: {@code stats}(수치목록), {@code definition}(정의). 반환: {@code boolean}.
 *   <li>{@code unsupported()}: 미지원 조건을 나타내는 결과를 만든다. 반환: {@code RepeatRejected}.
 * </ul>
 */
public final class ChaosDisplayResources {
  public static final String TEMPLATE_SHA256 = ModifierTextResources.TEMPLATE_SHA256;
  private final JsonNode bindings;
  private final ChaosOptionDisplay display;

  public ChaosDisplayResources(ObjectMapper json) {
    try (var stream = new java.io.ByteArrayInputStream(ModifierTextResources.read())) {
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
