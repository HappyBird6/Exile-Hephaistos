package com.poe2craft.craftingsupport.goal.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.api.dto.EvaluateRequest;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner;
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

  /** item·goal로 카드를 복원하고, cardIndex의 기존 단계 순서를 선택한 화폐 등급으로만 재생한다. */
  private record GradeRequest(
      ItemState item, GoalFilter goal, Integer cardIndex, List<GradeStep> steps) {}

  /** action은 선택한 등급의 화폐, omens·socketIndex는 재료 적용 조건, acquired·lost는 지켜야 할 목표 번호 기록이다. */
  private record GradeStep(
      String action,
      List<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      Integer socketIndex) {
    /** 응답에서 받은 확률을 입력으로 사용하지 않고 서버가 검증할 단계만 만든다. */
    private RecipeRoutePlanner.Step step() {
      if (action == null
          || action.isBlank()
          || omens == null
          || omens.stream().anyMatch(Objects::isNull)
          || !validNumbers(acquired)
          || !validNumbers(lost)
          || (socketIndex != null && socketIndex < 0)) throw new MalformedPayload();
      return new RecipeRoutePlanner.Step(action, omens, acquired, lost, socketIndex);
    }

    /** 목표 번호는 카드에 붙인 1부터 6 사이의 번호이고 한 단계에서 중복되지 않는다. */
    private static boolean validNumbers(List<Integer> numbers) {
      return numbers != null
          && numbers.size() <= 6
          && numbers.stream().allMatch(n -> n != null && n >= 1 && n <= 6)
          && new HashSet<>(numbers).size() == numbers.size();
    }
  }

  /** 기본 경로 전체를 재탐색하지 않고 요청한 단계와 등급의 조합만 작업대 규칙으로 검증한다. */
  @PostMapping(path = "/grade", consumes = "application/json")
  public TestRouteService.View grade(@RequestBody JsonNode body) {
    ItemStateWireDefaults.normalizeOwned(body);
    var input = reader.decode(body, GradeRequest.class);
    if (input.item() == null
        || input.goal() == null
        || input.cardIndex() == null
        || input.cardIndex() < 0
        || input.steps() == null
        || input.steps().stream().anyMatch(Objects::isNull)) throw new MalformedPayload();
    return routes.calculate(
        input.item(),
        input.goal(),
        input.cardIndex(),
        input.steps().stream().map(GradeStep::step).toList());
  }
}
