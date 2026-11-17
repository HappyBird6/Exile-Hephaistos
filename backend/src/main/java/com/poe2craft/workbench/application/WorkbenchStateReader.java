package com.poe2craft.workbench.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import java.util.List;
import java.util.Set;

/**
 * 일반·훼손 아이템 입력의 필드와 수치 타입을 엄격히 검증하고 상태로 변환한다.
 *
 * <p>기존 계약: Shared strict item-property boundary; legacy callers retain their existing mapper
 * semantics.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code CraftingOperations}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchStateReader(simulator, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       simulator}(제작시뮬레이터), {@code json}(JSON).
 *   <li>{@code concreteState(input)}: 일반 아이템 JSON을 엄격히 검증해 구체 상태로 읽는다. 입력: {@code input}(입력). 반환:
 *       {@code ItemState}.
 *   <li>{@code concreteState(input, allowDesecrated)}: 일반 아이템 JSON을 엄격히 검증해 구체 상태로 읽는다. 입력: {@code
 *       input}(입력), {@code allowDesecrated}(allow·훼손). 반환: {@code ItemState}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code UnsupportedItemProperties}.
 *   <li>{@code abyssState(input)}: 미공개 슬롯을 포함한 훼손 JSON을 검증해 상태로 읽는다. 입력: {@code input}(입력). 반환:
 *       {@code AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code validateAbyssNumbers(node)}: 훼손·Numbers을(를) 검증한다. 입력: {@code node}(노드). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code UnsupportedItemProperties}: 지원하지 않는 속성·타입을 일반 요청 오류와 구별해 HTTP 경계에 전달한다.
 * </ul>
 */
public final class WorkbenchStateReader {
  /**
   * 지원하지 않는 속성·타입을 일반 요청 오류와 구별해 HTTP 경계에 전달한다.
   *
   * <p>직접 선언한 필드·생성자·메서드는 없다.
   */
  public static final class UnsupportedItemProperties extends IllegalArgumentException {}

  private final CraftingOperations simulator;
  private final ObjectMapper json;

  public WorkbenchStateReader(CraftingOperations simulator, ObjectMapper json) {
    this.simulator = simulator;
    this.json = json;
  }

  public ItemState concreteState(JsonNode input) {
    return concreteState(input, false);
  }

  public ItemState concreteState(JsonNode input, boolean allowDesecrated) {
    var fields =
        new java.util.HashSet<>(
            Set.of(
                "snapshotId",
                "baseItemId",
                "itemLevel",
                "rarity",
                "implicits",
                "explicits",
                "conditions",
                "augmentSockets",
                "installedAugments",
                "catalystQuality",
                "equipmentQuality",
                "qualityInfused",
                "observations",
                "sanctification",
                "desecrated",
                "modifierIds"));
    fields.addAll(ItemStateWireDefaults.ITEM_FLAGS);
    if (input == null || !input.isObject())
      throw new IllegalArgumentException("Concrete item state required");
    input
        .fieldNames()
        .forEachRemaining(
            name -> {
              if (!fields.contains(name)) throw new UnsupportedItemProperties();
            });
    for (String flag : ItemStateWireDefaults.ITEM_FLAGS)
      if (input.has(flag) && !input.get(flag).isBoolean()) throw new UnsupportedItemProperties();
    var installed = input.get("installedAugments");
    if (installed != null && !installed.isNull()) {
      if (!installed.isArray() || installed.size() > 3) throw new UnsupportedItemProperties();
      for (var a : installed)
        if (!a.isObject()
            || a.size() != 2
            || !a.path("slotIndex").isIntegralNumber()
            || !a.path("slotIndex").canConvertToInt()
            || !a.path("augmentId").isTextual()) throw new UnsupportedItemProperties();
    }
    var desecrated = input.get("desecrated");
    // Common automatic states use the slot-aware simulator; this editor is concrete-only.
    if (desecrated != null) {
      if (!desecrated.isArray() || (!allowDesecrated && !desecrated.isEmpty()))
        throw new UnsupportedItemProperties();
      for (var slot : desecrated) {
        if (!slot.isObject()
            || slot.size()
                != 3 + (slot.has("minimumModifierLevel") ? 1 : 0) + (slot.has("revealPool") ? 1 : 0)
            || (slot.has("minimumModifierLevel")
                && !slot.path("minimumModifierLevel").isNull()
                && !(slot.path("minimumModifierLevel").isIntegralNumber()
                    && slot.path("minimumModifierLevel").intValue() == 40))
            || !slot.path("id").isTextual()
            || !slot.path("side").isTextual()
            || !slot.has("modifierId")
            || !(slot.get("modifierId").isNull() || slot.get("modifierId").isTextual()))
          throw new UnsupportedItemProperties();
      }
    }
    var observations = input.get("observations");
    if (observations != null && !observations.isNull()) {
      if (!observations.isObject()
          || (observations.size() != 3 && observations.size() != 4)
          || (observations.size() == 4 && !observations.has("augmentEffectScale"))
          || (observations.hasNonNull("augmentEffectScale")
              && !observations.get("augmentEffectScale").isNumber())
          || !observations.has("socketCount")
          || !observations.path("augmentEffects").isArray()
          || !observations.path("grantedSkills").isArray()) throw new UnsupportedItemProperties();
      var count = observations.get("socketCount");
      if (!count.isNull() && (!count.isIntegralNumber() || !count.canConvertToInt()))
        throw new UnsupportedItemProperties();
      for (var effect : observations.path("augmentEffects"))
        if (!effect.isTextual()) throw new UnsupportedItemProperties();
      for (var skill : observations.path("grantedSkills"))
        if (!skill.isObject()
            || skill.size() != 2
            || !skill.path("name").isTextual()
            || !skill.has("level")
            || (!skill.get("level").isNull()
                && (!skill.get("level").isIntegralNumber()
                    || !skill.get("level").canConvertToInt())))
          throw new UnsupportedItemProperties();
    }
    var sanctification = input.get("sanctification");
    if (sanctification != null && !sanctification.isNull()) {
      if (!sanctification.isObject()
          || sanctification.size() != 3
          || !sanctification.path("policy").isTextual()) throw new UnsupportedItemProperties();
      for (String layer : List.of("implicits", "explicits")) {
        var factors = sanctification.path(layer);
        if (!factors.isObject()) throw new UnsupportedItemProperties();
        for (var modifier : factors) {
          if (!modifier.isObject()) throw new UnsupportedItemProperties();
          for (var factor : modifier) {
            if (!factor.isIntegralNumber()
                || !factor.canConvertToInt()
                || factor.intValue() < 78
                || factor.intValue() > 122) throw new UnsupportedItemProperties();
          }
        }
      }
    }
    var equipmentQuality = input.get("equipmentQuality");
    if (equipmentQuality != null
        && !equipmentQuality.isNull()
        && (!equipmentQuality.isIntegralNumber() || !equipmentQuality.canConvertToInt()))
      throw new UnsupportedItemProperties();
    var sockets = input.get("augmentSockets");
    if (sockets != null
        && !sockets.isNull()
        && (!sockets.isIntegralNumber() || !sockets.canConvertToInt()))
      throw new IllegalArgumentException("Integral socket count required");
    var modifierFields = new java.util.HashSet<>(Set.of("modifierId", "values"));
    modifierFields.addAll(ItemStateWireDefaults.MODIFIER_FLAGS);
    var quality = input.get("catalystQuality");
    if (quality != null && !quality.isNull()) {
      if (!quality.isObject()
          || quality.size() != 2
          || !quality.has("type")
          || !quality.has("amount")
          || !quality.get("type").isTextual()
          || !quality.get("amount").isIntegralNumber()
          || !quality.get("amount").canConvertToInt()) throw new UnsupportedItemProperties();
    }
    for (String layer : List.of("implicits", "explicits")) {
      var modifiers = input.get(layer);
      if (modifiers == null || !modifiers.isArray())
        throw new IllegalArgumentException("Concrete modifiers required");
      for (var instance : modifiers) {
        if (!instance.isObject()) throw new IllegalArgumentException("Concrete modifier required");
        instance
            .fieldNames()
            .forEachRemaining(
                name -> {
                  if (!modifierFields.contains(name)) throw new UnsupportedItemProperties();
                });
        for (String flag : ItemStateWireDefaults.MODIFIER_FLAGS)
          if (instance.has(flag) && !instance.get(flag).isBoolean())
            throw new UnsupportedItemProperties();
        var values = instance.get("values");
        if (values == null || !values.isObject()) throw new UnsupportedItemProperties();
        values
            .elements()
            .forEachRemaining(
                value -> {
                  if (!value.isIntegralNumber() || !value.canConvertToLong())
                    throw new UnsupportedItemProperties();
                });
      }
    }
    // modifierIds is legacy derived bucket metadata, never an authoritative item property.
    return json.convertValue(ItemStateWireDefaults.normalized(input), ItemState.class);
  }

  public AbyssCraftingState abyssState(JsonNode input) {
    try {
      if (input == null || !input.isObject())
        throw new IllegalArgumentException("Abyss state required");
      validateAbyssNumbers(input);
      concreteState(input.get("item"));
      var state =
          json.readerFor(AbyssCraftingState.class)
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
              .readValue(ItemStateWireDefaults.normalized(input).traverse(json));
      var result = (AbyssCraftingState) state;
      result.validate(simulator.abyssCatalog(result.item()));
      return result;
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid Abyss state");
    }
  }

  private void validateAbyssNumbers(JsonNode node) {
    if (node.isObject() && node.has("values")) {
      var values = node.get("values");
      if (!values.isObject()) throw new IllegalArgumentException("Integral stat values required");
      values
          .elements()
          .forEachRemaining(
              value -> {
                if (!value.isIntegralNumber() || !value.canConvertToLong())
                  throw new IllegalArgumentException("Integral stat values required");
              });
    }
    if (node.isContainerNode()) node.elements().forEachRemaining(this::validateAbyssNumbers);
  }
}
