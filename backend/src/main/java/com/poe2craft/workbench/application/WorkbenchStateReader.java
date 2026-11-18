package com.poe2craft.workbench.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;
import java.util.Set;

/**
 * 아이템 JSON을 제작 상태로 읽는다. json은 필드와 수치를 변환하고 simulator는 카탈로그로 아이템을 검증한다. concreteState는 일반 상태와 허용된 훼손
 * 속성을, abyssState와 optionalAbyssState는 필수·선택 훼손 상태를 읽는다. validateAbyssNumbers는 정수 범위를 검사한다.
 * abyssCandidates와 revealCandidate는 관측 후보 세 개와 공개 후보 한 개를 읽는다. UnsupportedItemProperties는 지원하지 않는
 * 속성을 구분하는 예외다.
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

  public List<ModifierInstance> abyssCandidates(JsonNode input) {
    if (input == null || !input.isArray() || input.size() != 3)
      throw new IllegalArgumentException("Three observed candidates required");
    validateAbyssNumbers(input);
    try {
      return json.readerFor(
              new com.fasterxml.jackson.core.type.TypeReference<List<ModifierInstance>>() {})
          .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .readValue(input.traverse(json));
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid observed candidates");
    }
  }

  public ModifierInstance revealCandidate(JsonNode candidate) throws java.io.IOException {
    if (candidate == null) throw new IllegalArgumentException("Candidate required");
    validateAbyssNumbers(candidate);
    return json.readerFor(ModifierInstance.class)
        .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .readValue(candidate.traverse(json));
  }

  /** 선택적 훼손 상태를 읽고 값이 없으면 null을 돌려준다. */
  public AbyssCraftingState optionalAbyssState(JsonNode input) {
    return input == null || input.isNull() ? null : abyssState(input);
  }
}
