package com.poe2craft.craftingsupport.recommendation.api.dto;

import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;

/** state는 평가할 제작 상태이며 goal은 달성 여부를 확인할 목표다. */
public record SupportAssessmentRequest(StateBucket state, SupportGoals.Goal goal) {}
