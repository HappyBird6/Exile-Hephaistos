package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Set;

/** Shared strict item-property boundary; legacy callers retain their existing mapper semantics. */
public final class WorkbenchStateReader {
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
            "observations",
            "sanctification",
            "desecrated",
            "modifierIds");
    if (input == null || !input.isObject())
      throw new IllegalArgumentException("Concrete item state required");
    input
        .fieldNames()
        .forEachRemaining(
            name -> {
              if (!fields.contains(name)) throw new WorkbenchController.UnsupportedItemProperties();
            });
    var installed = input.get("installedAugments");
    if (installed != null && !installed.isNull()) {
      if (!installed.isArray() || installed.size() > 3)
        throw new WorkbenchController.UnsupportedItemProperties();
      for (var a : installed)
        if (!a.isObject()
            || a.size() != 2
            || !a.path("slotIndex").isIntegralNumber()
            || !a.path("slotIndex").canConvertToInt()
            || !a.path("augmentId").isTextual())
          throw new WorkbenchController.UnsupportedItemProperties();
    }
    var desecrated = input.get("desecrated");
    // Common automatic states use the slot-aware simulator; this editor is concrete-only.
    if (desecrated != null) {
      if (!desecrated.isArray() || (!allowDesecrated && !desecrated.isEmpty()))
        throw new WorkbenchController.UnsupportedItemProperties();
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
          throw new WorkbenchController.UnsupportedItemProperties();
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
          || !observations.path("grantedSkills").isArray())
        throw new WorkbenchController.UnsupportedItemProperties();
      var count = observations.get("socketCount");
      if (!count.isNull() && (!count.isIntegralNumber() || !count.canConvertToInt()))
        throw new WorkbenchController.UnsupportedItemProperties();
      for (var effect : observations.path("augmentEffects"))
        if (!effect.isTextual()) throw new WorkbenchController.UnsupportedItemProperties();
      for (var skill : observations.path("grantedSkills"))
        if (!skill.isObject()
            || skill.size() != 2
            || !skill.path("name").isTextual()
            || !skill.has("level")
            || (!skill.get("level").isNull()
                && (!skill.get("level").isIntegralNumber()
                    || !skill.get("level").canConvertToInt())))
          throw new WorkbenchController.UnsupportedItemProperties();
    }
    var sanctification = input.get("sanctification");
    if (sanctification != null && !sanctification.isNull()) {
      if (!sanctification.isObject()
          || sanctification.size() != 3
          || !sanctification.path("policy").isTextual())
        throw new WorkbenchController.UnsupportedItemProperties();
      for (String layer : List.of("implicits", "explicits")) {
        var factors = sanctification.path(layer);
        if (!factors.isObject()) throw new WorkbenchController.UnsupportedItemProperties();
        for (var modifier : factors) {
          if (!modifier.isObject()) throw new WorkbenchController.UnsupportedItemProperties();
          for (var factor : modifier) {
            if (!factor.isIntegralNumber()
                || !factor.canConvertToInt()
                || factor.intValue() < 78
                || factor.intValue() > 122)
              throw new WorkbenchController.UnsupportedItemProperties();
          }
        }
      }
    }
    var equipmentQuality = input.get("equipmentQuality");
    if (equipmentQuality != null
        && !equipmentQuality.isNull()
        && (!equipmentQuality.isIntegralNumber() || !equipmentQuality.canConvertToInt()))
      throw new WorkbenchController.UnsupportedItemProperties();
    var sockets = input.get("augmentSockets");
    if (sockets != null
        && !sockets.isNull()
        && (!sockets.isIntegralNumber() || !sockets.canConvertToInt()))
      throw new IllegalArgumentException("Integral socket count required");
    var modifierFields = Set.of("modifierId", "values", "fractured", "crafted");
    var quality = input.get("catalystQuality");
    if (quality != null && !quality.isNull()) {
      if (!quality.isObject()
          || quality.size() != 2
          || !quality.has("type")
          || !quality.has("amount")
          || !quality.get("type").isTextual()
          || !quality.get("amount").isIntegralNumber()
          || !quality.get("amount").canConvertToInt())
        throw new WorkbenchController.UnsupportedItemProperties();
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
                  if (!modifierFields.contains(name))
                    throw new WorkbenchController.UnsupportedItemProperties();
                });
        for (String flag : List.of("fractured", "crafted"))
          if (instance.has(flag) && !instance.get(flag).isBoolean())
            throw new WorkbenchController.UnsupportedItemProperties();
        var values = instance.get("values");
        if (values == null || !values.isObject())
          throw new WorkbenchController.UnsupportedItemProperties();
        values
            .elements()
            .forEachRemaining(
                value -> {
                  if (!value.isIntegralNumber() || !value.canConvertToLong())
                    throw new WorkbenchController.UnsupportedItemProperties();
                });
      }
    }
    // modifierIds is legacy derived bucket metadata, never an authoritative item property.
    return json.convertValue(withModifierDefaults(input), ItemState.class);
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
              .readValue(withModifierDefaults(input).traverse(json));
      var result = (AbyssCraftingState) state;
      result.validate(simulator.abyssCatalog(result.item()));
      return result;
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid Abyss state");
    }
  }

  /**
   * Only omitted legacy modifier flags get defaults; explicit nulls and wrong types stay invalid.
   */
  private JsonNode withModifierDefaults(JsonNode input) {
    var copy = input.deepCopy();
    addModifierDefaults(copy);
    return copy;
  }

  private void addModifierDefaults(JsonNode node) {
    if (node.isObject() && node.path("modifierId").isTextual() && node.path("values").isObject()) {
      var modifier = (com.fasterxml.jackson.databind.node.ObjectNode) node;
      for (String flag : List.of("fractured", "crafted"))
        if (!modifier.has(flag)) modifier.put(flag, false);
    }
    if (node.isContainerNode()) node.elements().forEachRemaining(this::addModifierDefaults);
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
