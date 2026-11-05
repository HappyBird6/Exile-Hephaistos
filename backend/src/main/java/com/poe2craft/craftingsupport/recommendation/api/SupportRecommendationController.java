package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.application.SupportRecommendationService;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportRecommendationController {
  private final SupportRecommendationService recommendations;

  public SupportRecommendationController(SupportRecommendationService recommendations) {
    this.recommendations = recommendations;
  }

  @PostMapping("/recommend")
  public SupportRecommendationService.Report recommend(@RequestBody Request request) {
    return recommendations.recommend(
        request.state(), request.goal(), request.activeOmens(), request.limits());
  }

  public record Request(
      StateBucket state,
      SupportGoals.Goal goal,
      Set<String> activeOmens,
      SupportRecommendationService.Limits limits) {}
}
