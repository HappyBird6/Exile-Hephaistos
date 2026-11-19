package com.poe2craft.craftingsupport.goal.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.api.dto.EvaluateRequest;
import com.poe2craft.craftingsupport.goal.api.dto.RecommendRequest;
import com.poe2craft.craftingsupport.goal.api.dto.ValidateRequest;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Context;
import com.poe2craft.craftingsupport.goal.domain.GoalValidator;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import org.springframework.web.bind.annotation.*;

/**
 * reader가 목표 필터 요청을 검증하고 service가 게임 규칙을 적용한다. catalog는 베이스별 목표 후보, validate는 조건 유효성, evaluate는 달성
 * 여부, recommend는 다음 제작 행동을 반환한다. preview는 workbench의 베이스별 재료 정책을 service에 전달해 목표 완성 예시를 반환한다. 생성자는
 * 서비스와 작업대, 엄격한 요청 판독에 사용할 JSON 설정을 받는다.
 */
@RestController
@RequestMapping("/api/v1/crafting/support/goal-filters")
public final class GoalFilterController {
  private final GoalFilterService service;
  private final GoalRequestReader reader;

  /** 베이스별 재료 규칙이 등록된 작업대 조회 서비스. */
  private final CraftingOperations workbench;

  public GoalFilterController(
      GoalFilterService service, ObjectMapper json, CraftingOperations workbench) {
    this.service = service;
    this.workbench = workbench;
    reader = new GoalRequestReader(json);
  }

  @GetMapping("/catalog")
  public GoalCatalog catalog(
      @RequestParam String snapshotId,
      @RequestParam String baseItemId,
      @RequestParam Integer itemLevel) {
    return service.catalog(new Context(snapshotId, baseItemId, itemLevel));
  }

  @PostMapping("/validate")
  public GoalValidator.Validation validate(@RequestBody JsonNode body) {
    var request = reader.decode(body, ValidateRequest.class);
    return service.validate(request.context(), request.goal());
  }

  @PostMapping("/evaluate")
  public GoalEvaluator.Evaluation evaluate(@RequestBody JsonNode body) {
    var request = reader.decode(body, EvaluateRequest.class);
    return service.evaluate(request.item(), request.goal());
  }

  /** 테스트 작업대 입력의 분열 명시 옵션은 선택 사항이며 원래 수치를 유지한다. */
  private record PreviewRequest(
      Context context,
      com.poe2craft.craftingsupport.goal.domain.GoalFilter goal,
      com.poe2craft.shared.item.ModifierInstance fracture) {}

  /** 목표와 문맥을 엄격히 읽고 확인된 아이템 예시 및 옵션별 획득처를 반환한다. */
  @PostMapping("/preview")
  public GoalFilterService.Preview preview(@RequestBody JsonNode body) {
    var request = reader.decode(body, PreviewRequest.class);
    return service.preview(request.context(), request.goal(), workbench, request.fracture());
  }

  @PostMapping("/recommend")
  public GoalFilterService.Recommendation recommend(@RequestBody JsonNode body) {
    var request = reader.decode(body, RecommendRequest.class);
    return service.recommend(
        request.item(), request.goal(), request.activeOmens(), request.limits());
  }
}
