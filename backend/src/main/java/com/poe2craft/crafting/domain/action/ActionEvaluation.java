package com.poe2craft.crafting.domain.action;

/*
 * ActionEvaluation: 계산 가능한 결과 분포, 규칙상 적용 불가, 데이터 또는 구현 부족을 구분한다.
 * 적용 불가와 사용 불가에는 reasonCode를 남기고 누락 데이터를 0%나 빈 성공 결과로 바꾸지 않는다.
 * 평가 결과는 불변으로 유지하며 목표 충족 여부와 화폐 효과 자체를 혼합하지 않는다.
 */
