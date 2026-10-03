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
            "modifierIds");
    if (input == null || !input.isObject())
      throw new IllegalArgumentException("Concrete item state required");
    input
        .fieldNames()
        .forEachRemaining(
            name -> {
              if (!fields.contains(name)) throw new UnsupportedItemProperties();
            });
    var sockets = input.get("augmentSockets");
    if (sockets != null
        && !sockets.isNull()
        && (!sockets.isIntegralNumber() || !sockets.canConvertToInt()))
      throw new IllegalArgumentException("Integral socket count required");
    var modifierFields = Set.of("modifierId", "values", "fractured");
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
      }
    }
    // modifierIds is legacy derived bucket metadata, never an authoritative item property.
    return json.convertValue(input, ItemState.class);
  }

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return mapper.map(parser.parseText(request.text()));
  }

  public record TextRequest(String text) {}
}
