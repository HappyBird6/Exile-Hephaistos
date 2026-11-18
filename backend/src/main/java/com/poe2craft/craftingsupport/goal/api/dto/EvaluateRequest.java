package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.item.ItemState;

/** 목표 달성 여부를 평가한다. item은 평가할 아이템 상태이고 goal은 적용할 조건이다. */
public record EvaluateRequest(ItemState item, GoalFilter goal) {}
