package com.poe2craft.crafting.domain.action;

/*
 * crafting/domain/action: 화폐 효과의 적용 가능성과 상태 전이를 계산하는 Handler를 둔다.
 * CurrencyActionHandler: 검증된 규칙에 따라 적용 조건, 후보 풀, 가능한 결과의 분포를 계산한다.
 * 입력 ItemState를 수정하지 않는다. 순차 효과는 중간 상태별로 평가하며 목표 판정은 분리한다.
 */
