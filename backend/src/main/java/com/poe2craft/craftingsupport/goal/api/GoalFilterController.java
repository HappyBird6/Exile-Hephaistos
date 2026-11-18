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
import org.springframework.web.bind.annotation.*;

/**
 * reader가 목표 필터 요청을 검증하고 service가 게임 규칙을 적용한다. catalog는 베이스별 목표 후보, validate는 조건 유효성, evaluate는 달성
 * 여부, recommend는 다음 제작 행동을 반환한다.
 */
@RestController
@RequestMapping("/api/v1/crafting/support/goal-filters")
public final class GoalFilterController {
  private final GoalFilterService service;
  private final GoalRequestReader reader;

  public GoalFilterController(GoalFilterService service, ObjectMapper json) {
    this.service = service;
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

  @PostMapping("/recommend")
  public GoalFilterService.Recommendation recommend(@RequestBody JsonNode body) {
    var request = reader.decode(body, RecommendRequest.class);
    return service.recommend(
        request.item(), request.goal(), request.activeOmens(), request.limits());
  }
}
