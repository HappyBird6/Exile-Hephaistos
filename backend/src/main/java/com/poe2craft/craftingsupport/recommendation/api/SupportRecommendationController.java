package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.application.SupportRecommendationService;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

/**
 * 지원되는 유한 제작 순서에 대한 추천 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code recommendations}: 추천목록 ({@code SupportRecommendationService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SupportRecommendationController(recommendations)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       recommendations}(추천목록).
 *   <li>{@code recommend(request)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code request}(요청). 반환: {@code
 *       SupportRecommendationService.Report}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Request}: {@code SupportRecommendationController}에서 사용하는 요청의 불변 값이다.
 * </ul>
 */
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

  /**
   * {@code SupportRecommendationController}에서 사용하는 요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code goal}: 목표 ({@code SupportGoals.Goal}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code limits}: 한도목록 ({@code SupportRecommendationService.Limits}).
   * </ul>
   */
  public record Request(
      StateBucket state,
      SupportGoals.Goal goal,
      Set<String> activeOmens,
      SupportRecommendationService.Limits limits) {}
}
