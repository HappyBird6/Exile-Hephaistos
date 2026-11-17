package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 복사한 룬 효과의 수치를 계산하되 구조적 장착 한도는 증폭하지 않는다.
 *
 * <p>기존 계약: Shared source-span arithmetic for copied Rune effects; structural limits never amplify.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code NUMBER}: 숫자 ({@code Pattern}).
 *   <li>{@code TEMPLATES}: 템플릿목록 ({@code JsonNode}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SocketAugmentEffects()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code JsonNode}. 사용하는 예외: {@code
 *       ExceptionInInitializerError}.
 *   <li>{@code visible(text)}: visible 처리에서 {@code text.replaceAll}을(를) 사용한다. 입력: {@code
 *       text}(텍스트). 반환: {@code String}.
 *   <li>{@code multiplier(state)}: 배수의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태). 반환: {@code
 *       double}.
 *   <li>{@code multiplier(state, catalog)}: 배수 처리에서 {@code state.implicits}, {@code
 *       state.explicits}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그). 반환: {@code
 *       double}.
 *   <li>{@code runeMultiplier(installed, multiplier)}: 룬·배수 처리에서 {@code installed.stream}, {@code
 *       AugmentCraftingRules.effect}을(를) 사용한다. 입력: {@code installed}(장착된), {@code multiplier}(배수).
 *       반환: {@code double}.
 *   <li>{@code structural(id)}: 구조적 처리에서 {@code AugmentCraftingRules.effect}, {@code
 *       effect.craftedBonus}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code render(base, id, installed, multiplier, bonded)}: 계산 결과를 외부 응답 형태로 만든다. 입력: {@code
 *       base}(베이스), {@code id}(식별자), {@code installed}(장착된), {@code multiplier}(배수), {@code
 *       bonded}(bonded). 반환: {@code List<String>}.
 *   <li>{@code observed(text, ratio)}: 관측한 처리에서 {@code NUMBER.matcher}, {@code Long.toString}을(를)
 *       사용한다. 입력: {@code text}(텍스트), {@code ratio}(비율). 반환: {@code String}.
 * </ul>
 */
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
