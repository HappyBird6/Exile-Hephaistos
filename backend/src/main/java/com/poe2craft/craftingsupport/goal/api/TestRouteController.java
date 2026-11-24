package com.poe2craft.craftingsupport.goal.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.api.dto.EvaluateRequest;
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import org.springframework.web.bind.annotation.*;

/** 테스트 탭의 아이템·목표 요청을 기존 필터 형식으로 읽고 규칙 기반 결과를 반환한다. */
@RestController
@RequestMapping("/api/v1/crafting/support/goal-filters/test-routes")
public final class TestRouteController {
  private final TestRouteService routes;
  private final GoalRequestReader reader;

  /** routes는 테스트 전용 계산 서비스이고 reader는 수치·필드 타입을 검사한다. */
  public TestRouteController(TestRouteService routes, ObjectMapper json) {
    this.routes = routes;
    reader = new GoalRequestReader(json);
  }

  /** 새 작업을 저장하지 않고 현재 요청의 시작 아이템에서 검증한 경로와 옵션 번호표를 반환한다. */
  @PostMapping(consumes = "application/json")
  public TestRouteService.View calculate(@RequestBody JsonNode body) {
    ItemStateWireDefaults.normalizeOwned(body);
    var input = reader.decode(body, EvaluateRequest.class);
    return routes.calculate(input.item(), input.goal());
  }
}
