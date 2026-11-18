package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Context;

/** 목표 필터 검증 요청이다. context는 카탈로그·베이스·레벨을 지정하고 goal은 검증할 조건이다. */
public record ValidateRequest(Context context, GoalFilter goal) {}
