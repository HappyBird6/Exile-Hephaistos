package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.item.ItemState;
import java.util.Set;

/** item의 현재 상태와 goal 조건을 바탕으로 제작 행동을 추천한다. activeOmens는 활성 징조이며 limits는 계산 한도다. */
public record RecommendRequest(
    ItemState item, GoalFilter goal, Set<String> activeOmens, GoalFilterService.Limits limits) {}
