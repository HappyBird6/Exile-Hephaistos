package com.poe2craft.crafting.domain.target;

/*
 * crafting/domain/target: 제작 결과가 사용자 목표와 보존 조건을 만족하는지 판정한다.
 * TargetEvaluator: 결과 분포를 기준으로 목표 충족 확률과 보존 조건 훼손 확률을 평가한다.
 * 명시된 AND/OR 의미를 따르며 두 확률은 겹칠 수 있다. 임의 메타 점수나 게임 규칙을 만들지 않는다.
 */
