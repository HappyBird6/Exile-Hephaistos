package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.workbench.api.dto.CustomExaltRequest;
import com.poe2craft.workbench.application.CustomExaltService;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/**
 * 선택형 엑잘티드 제작 요청을 받는다. reader는 일반·훼손 상태를 읽는다. choices는 service에서 후보를 조회하고 apply는 선택한 옵션과 난수 공급자
 * random을 서비스에 전달해 제작을 실행한다.
 */
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

  @PostMapping("/choices")
  public List<WorkbenchSimulator.ExaltChoice> choices(@RequestBody CustomExaltRequest request) {
    return service.choices(
        reader.concreteState(request.state(), true),
        reader.optionalAbyssState(request.abyss()),
        request.action(),
        request.activeOmens());
  }

  @PostMapping("/apply")
  public CustomExaltService.Applied apply(@RequestBody CustomExaltRequest request) {
    return service.apply(
        reader.concreteState(request.state(), true),
        reader.optionalAbyssState(request.abyss()),
        request.action(),
        request.activeOmens(),
        request.modifierId(),
        random);
  }
}
