package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.api.dto.SupportRecommendationRequest;
import com.poe2craft.craftingsupport.recommendation.application.SupportRecommendationService;
import org.springframework.web.bind.annotation.*;

/** 제작 행동 추천 요청을 받는다. recommend는 현재 상태·목표·활성 징조·계산 한도를 recommendations에 전달한다. */
@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportRecommendationController {
  private final SupportRecommendationService recommendations;

  public SupportRecommendationController(SupportRecommendationService recommendations) {
    this.recommendations = recommendations;
  }

  @PostMapping("/recommend")
  public SupportRecommendationService.Report recommend(
      @RequestBody SupportRecommendationRequest request) {
    return recommendations.recommend(
        request.state(), request.goal(), request.activeOmens(), request.limits());
  }
}
