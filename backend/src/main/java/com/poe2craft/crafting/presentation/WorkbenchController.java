package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.WorkbenchSimulator;
import com.poe2craft.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.item.ItemState;
import com.poe2craft.item.SolarTextMapper;
import com.poe2craft.item.testparser.ItemTextService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchController {
  private final WorkbenchService simulator;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;
  private final ObjectMapper json;
  private final SecureRandom random = new SecureRandom();
  private final JsonNode registry = CraftingRegistryLoader.load();

  public WorkbenchController(
      WorkbenchService simulator,
      SolarTextMapper mapper,
      ItemTextService parser,
      ObjectMapper json) {
    this.simulator = simulator;
    this.mapper = mapper;
    this.parser = parser;
    this.json = json;
  }

  @GetMapping("/registry")
  public JsonNode registry() {
    return registry.deepCopy();
  }

  @GetMapping("/initial")
  public WorkbenchService.Initial initial(
      @RequestParam(defaultValue = "solar") String base,
      @RequestParam(defaultValue = "82") int itemLevel) {
    return simulator.initial(base, itemLevel);
  }

  @PostMapping("/apply")
  public WorkbenchSimulator.Result apply(@RequestBody ApplyRequest request) {
    return simulator.apply(
        concreteState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/actions")
  public List<WorkbenchSimulator.Availability> actions(@RequestBody ActionRequest request) {
    return simulator.actions(concreteState(request.state()), request.activeOmens());
  }

  public record ApplyRequest(JsonNode state, WorkbenchCurrency action, Set<String> activeOmens) {}

  public record ActionRequest(JsonNode state, Set<String> activeOmens) {}

  @PostMapping("/quality-display")
  public WorkbenchService.QualityDisplay qualityDisplay(@RequestBody ActionRequest request) {
    return simulator.qualityDisplay(concreteState(request.state()));
  }

  public static final class UnsupportedItemProperties extends IllegalArgumentException {}

  /** Unknown item properties must never vanish during Jackson conversion or crafting. */
  private ItemState concreteState(JsonNode input) {
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
            "catalystQuality",
            "desecrated",
            "modifierIds");
    if (input == null || !input.isObject())
      throw new IllegalArgumentException("Concrete item state required");
    input
        .fieldNames()
        .forEachRemaining(
            name -> {
              if (!fields.contains(name)) throw new UnsupportedItemProperties();
            });
    var desecrated = input.get("desecrated");
    // Common automatic states use the slot-aware simulator; this editor is concrete-only.
    if (desecrated != null && (!desecrated.isArray() || !desecrated.isEmpty()))
      throw new UnsupportedItemProperties();
    var sockets = input.get("augmentSockets");
    if (sockets != null
        && !sockets.isNull()
        && (!sockets.isIntegralNumber() || !sockets.canConvertToInt()))
      throw new IllegalArgumentException("Integral socket count required");
    var modifierFields = Set.of("modifierId", "values", "fractured");
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
    return json.convertValue(input, ItemState.class);
  }

  public record AbyssRequest(
      JsonNode state,
      String slotId,
      com.poe2craft.item.ModifierDefinition.AffixType side,
      JsonNode candidates,
      Integer candidateIndex,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      Set<String> targetIds) {}

  private com.poe2craft.crafting.domain.AbyssCraftingState abyssState(JsonNode input) {
    try {
      if (input == null || !input.isObject())
        throw new IllegalArgumentException("Abyss state required");
      validateAbyssNumbers(input);
      concreteState(input.get("item"));
      var state =
          json.readerFor(com.poe2craft.crafting.domain.AbyssCraftingState.class)
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
              .readValue(input.traverse(json));
      var result = (com.poe2craft.crafting.domain.AbyssCraftingState) state;
      result.validate(simulator.abyssCatalog(result.item()));
      return result;
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid Abyss state");
    }
  }

  private List<com.poe2craft.item.ModifierInstance> abyssCandidates(JsonNode input) {
    if (input == null || !input.isArray() || input.size() != 3)
      throw new IllegalArgumentException("Three observed candidates required");
    validateAbyssNumbers(input);
    try {
      return json.readerFor(
              new com.fasterxml.jackson.core.type.TypeReference<
                  List<com.poe2craft.item.ModifierInstance>>() {})
          .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .readValue(input.traverse(json));
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid observed candidates");
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

  @PostMapping("/abyss/validate")
  public com.poe2craft.crafting.domain.AbyssCraftingState validateAbyss(
      @RequestBody AbyssRequest request) {
    return abyssState(request.state());
  }

  @PostMapping("/abyss/import")
  public com.poe2craft.crafting.domain.AbyssCraftingState importAbyss(
      @RequestBody ActionRequest request) {
    var item = concreteState(request.state());
    return com.poe2craft.crafting.domain.AbyssCraftingState.from(
        item, simulator.abyssCatalog(item));
  }

  @PostMapping("/abyss/add")
  public com.poe2craft.crafting.domain.AbyssCraftingState addAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    return state.addUnrevealed(
        request.slotId(), request.side(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/observe")
  public com.poe2craft.crafting.domain.AbyssCraftingState observeAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    return state.observe(
        request.slotId(),
        abyssCandidates(request.candidates()),
        simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/choose")
  public com.poe2craft.crafting.domain.AbyssCraftingState chooseAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.candidateIndex() == null) throw new IllegalArgumentException("Candidate required");
    return state.choose(request.candidateIndex(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/plan")
  public com.poe2craft.crafting.domain.AbyssCraftingPlan planAbyss(
      @RequestBody AbyssRequest request) {
    return simulator.abyssPlan(
        abyssState(request.state()), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/apply")
  public com.poe2craft.crafting.domain.AbyssCraftingState applyAbyss(
      @RequestBody AbyssRequest request) {
    return simulator.applyAbyss(
        abyssState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/abyss/compare")
  public com.poe2craft.crafting.domain.AbyssProbabilityComparison.Comparison compareAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.targetIds() == null || request.targetIds().isEmpty())
      throw new IllegalArgumentException("Target modifiers required");
    var omenIds = request.activeOmens() == null ? Set.<String>of() : request.activeOmens();
    if (omenIds.size() > 1)
      throw new IllegalArgumentException("At most one addition omen supported");
    var omen =
        omenIds.isEmpty()
            ? null
            : com.poe2craft.crafting.domain.WorkbenchOmen.fromId(omenIds.iterator().next());
    var plan = simulator.abyssPlan(state, request.action(), omenIds);
    if (!plan.available()) throw new IllegalArgumentException("Next addition unavailable");
    return new com.poe2craft.crafting.domain.AbyssProbabilityComparison(
            simulator.abyssCatalog(state.item()))
        .compare(state, request.slotId(), request.action(), omen, request.targetIds())
        .orElse(null);
  }

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return mapper.map(parser.parseText(request.text()));
  }

  public record TextRequest(String text) {}
}
