package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.workbench.api.dto.PoolRequest;
import com.poe2craft.workbench.api.dto.SessionRequest;
import com.poe2craft.workbench.application.ModifierReferenceService;
import com.poe2craft.workbench.application.SessionCompatibilityService;
import com.poe2craft.workbench.application.SessionCompatibilityService.Compatibility;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import org.springframework.web.bind.annotation.*;

/**
 * 제작 참조 자료와 저장 세션 호환성을 조회한다. reader는 아이템 상태를 읽으며 pools는 references로 옵션 후보를 조회한다. compatibility는
 * sessions로 저장 상태를 현재 카탈로그에 대조한다.
 */
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

  @PostMapping("/modifier-pools")
  public ModifierReferenceService.Reference pools(@RequestBody PoolRequest request) {
    return references.describe(reader.concreteState(request.state(), true), request.dynamic());
  }

  /** Revalidates an exact checkpoint, never rewrites a saved film or changes rolls. */
  @PostMapping("/session-compatibility")
  public Compatibility compatibility(@RequestBody SessionRequest request) {
    return sessions.check(request.state(), request.abyss(), request.compatibilityId(), reader);
  }
}
