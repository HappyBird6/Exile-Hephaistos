package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.item.ItemState;

/** item에서 goal까지 제작 경로를 찾는다. qualityCatalyst는 품질 40에 사용할 일반 기폭제의 선택적 재료 ID다. */
public record TestRouteRequest(ItemState item, GoalFilter goal, String qualityCatalyst) {}
