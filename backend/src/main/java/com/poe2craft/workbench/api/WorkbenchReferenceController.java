package com.poe2craft.workbench.api;

import static com.poe2craft.workbench.application.SessionCompatibilityService.rejected;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.workbench.application.ModifierReferenceService;
import com.poe2craft.workbench.application.SessionCompatibilityService;
import com.poe2craft.workbench.application.SessionCompatibilityService.Compatibility;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchReferenceController {
  private final SessionCompatibilityService sessions;
  private final ModifierReferenceService references;
  private final WorkbenchStateReader reader;

  public WorkbenchReferenceController(
      CraftingOperations crafting, ModifierReferenceService references, ObjectMapper json) {
    this.sessions = new SessionCompatibilityService(crafting);
    this.references = references;
    this.reader = new WorkbenchStateReader(crafting, json);
  }

  public record PoolRequest(JsonNode state, boolean dynamic) {}

  @PostMapping("/modifier-pools")
  public ModifierReferenceService.Reference pools(@RequestBody PoolRequest request) {
    return references.describe(reader.concreteState(request.state(), true), request.dynamic());
  }

  public record SessionRequest(JsonNode state, JsonNode abyss, String compatibilityId) {}

  /** Revalidates an exact checkpoint, never rewrites a saved film or changes rolls. */
  @PostMapping("/session-compatibility")
  public Compatibility compatibility(@RequestBody SessionRequest request) {
    var input = request.state();
    if (input == null || !input.isObject()) return rejected("INVALID_STATE");
    var catalog = sessions.catalog(input.path("baseItemId").asText()).orElse(null);
    if (catalog == null) return rejected("UNSUPPORTED_BASE");
    var rebased = (ObjectNode) input.deepCopy();
    rebased.put("snapshotId", catalog.metadata().snapshotId());
    try {
      reader.concreteState(input, true);
      var state = reader.concreteState(rebased, true);
      AbyssCraftingState abyss = null;
      if (request.abyss() != null && !request.abyss().isNull()) {
        if (!request.abyss().path("item").path("snapshotId").equals(input.path("snapshotId")))
          return rejected("INCONSISTENT_ABYSS");
        var savedAbyss = request.abyss().deepCopy();
        if (!(savedAbyss.path("item") instanceof ObjectNode item)) return rejected("INVALID_STATE");
        item.put("snapshotId", catalog.metadata().snapshotId());
        abyss = reader.abyssState(savedAbyss);
      }
      return sessions.check(state, abyss, request.compatibilityId());
    } catch (IllegalArgumentException invalid) {
      return rejected("INVALID_STATE");
    }
  }
}
