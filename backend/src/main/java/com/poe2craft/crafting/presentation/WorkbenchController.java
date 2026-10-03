package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.JsonNode;
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
  private final SecureRandom random = new SecureRandom();
  private final JsonNode registry = CraftingRegistryLoader.load();

  public WorkbenchController(
      WorkbenchService simulator, SolarTextMapper mapper, ItemTextService parser) {
    this.simulator = simulator;
    this.mapper = mapper;
    this.parser = parser;
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
    return simulator.apply(request.state(), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/actions")
  public List<WorkbenchSimulator.Availability> actions(@RequestBody ActionRequest request) {
    return simulator.actions(request.state(), request.activeOmens());
  }

  public record ApplyRequest(ItemState state, WorkbenchCurrency action, Set<String> activeOmens) {}

  public record ActionRequest(ItemState state, Set<String> activeOmens) {}

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return mapper.map(parser.parseText(request.text()));
  }

  public record TextRequest(String text) {}
}
