package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Context;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;

/**
 * 테스트 작업대의 완성 예시 요청이다. context는 베이스와 레벨, goal은 목표 조건을 담는다. fracture는 유지할 분열 옵션과 수치이며 selectedTier는
 * 함께 성립하는 티어를 조회할 때 선택한 옵션 ID다. startingItem은 이미 축성된 아이템의 변경 불가 상태를 판정할 때 전달한다. 세 필드 모두 생략할 수 있다.
 */
public record PreviewRequest(
    Context context,
    GoalFilter goal,
    ModifierInstance fracture,
    String selectedTier,
    ItemState startingItem) {}
