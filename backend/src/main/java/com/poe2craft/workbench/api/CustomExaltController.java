package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.workbench.application.CustomExaltService;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/workbench/custom-exalt")
public final class CustomExaltController {
  private final CustomExaltService service;
  private final WorkbenchStateReader reader;
  private final SecureRandom random = new SecureRandom();

  public CustomExaltController(CraftingOperations crafting, ObjectMapper json) {
    service = new CustomExaltService(crafting);
    reader = new WorkbenchStateReader(crafting, json);
  }

  public record Request(
      JsonNode state,
      JsonNode abyss,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      String modifierId) {}

  @PostMapping("/choices")
  public List<WorkbenchSimulator.ExaltChoice> choices(@RequestBody Request request) {
    return service.choices(
        reader.concreteState(request.state(), true),
        request.abyss() == null || request.abyss().isNull()
            ? null
            : reader.abyssState(request.abyss()),
        request.action(),
        request.activeOmens());
  }

  @PostMapping("/apply")
  public CustomExaltService.Applied apply(@RequestBody Request request) {
    return service.apply(
        reader.concreteState(request.state(), true),
        request.abyss() == null || request.abyss().isNull()
            ? null
            : reader.abyssState(request.abyss()),
        request.action(),
        request.activeOmens(),
        request.modifierId(),
        random);
  }
}
