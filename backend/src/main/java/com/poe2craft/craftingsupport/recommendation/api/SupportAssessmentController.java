package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.api.dto.SupportAssessmentRequest;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.item.ItemCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 제작 목표 평가 API다. families는 goals에서 목표 종류를 조회하고 assess는 현재 상태가 요청한 목표를 달성했는지 평가한다. */
@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportAssessmentController {
  private final SupportGoals goals;

  public SupportAssessmentController(ItemCatalog catalog) {
    goals = new SupportGoals(catalog);
  }

  @GetMapping("/families")
  public List<SupportGoals.Family> families() {
    return goals.families();
  }

  @PostMapping("/assess")
  public SupportGoals.Assessment assess(@RequestBody SupportAssessmentRequest request) {
    return goals.assess(request.state(), request.goal());
  }
}
