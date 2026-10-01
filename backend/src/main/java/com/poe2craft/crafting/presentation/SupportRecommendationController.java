package com.poe2craft.crafting.presentation;

import com.poe2craft.crafting.application.SupportRecommendations;
import com.poe2craft.crafting.domain.*;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportRecommendationController {
  private final SupportRecommendations recommendations;

  public SupportRecommendationController(SupportRecommendations recommendations) {
    this.recommendations = recommendations;
  }

  @PostMapping("/recommend")
  public SupportRecommendations.Report recommend(@RequestBody Request request) {
    return recommendations.recommend(
        request.state(), request.goal(), request.activeOmens(), request.limits());
  }

  public record Request(
      StateBucket state,
      SupportGoals.Goal goal,
      Set<String> activeOmens,
      SupportRecommendations.Limits limits) {}
}
