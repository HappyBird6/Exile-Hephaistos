package com.poe2craft.craftingsupport.recommendation.api.dto;

import com.poe2craft.craftingsupport.recommendation.application.SupportRecommendationService;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.Set;

/** state와 goal은 현재 상태와 목표다. activeOmens는 적용할 징조이고 limits는 추천 탐색에 허용한 계산 한도다. */
public record SupportRecommendationRequest(
    StateBucket state,
    SupportGoals.Goal goal,
    Set<String> activeOmens,
    SupportRecommendationService.Limits limits) {}
