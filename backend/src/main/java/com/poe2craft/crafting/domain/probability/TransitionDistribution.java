package com.poe2craft.crafting.domain.probability;

/*
 * TransitionDistribution: 가능한 최종 ItemState와 각각의 확률을 불변으로 표현한다.
 * 조건부 경로 확률을 곱하고 같은 최종 상태에 도달하는 경로의 확률은 합산한다.
 * 음수와 분포 합을 검증하고 순서를 결정적으로 유지한다. 부분 계산을 완전한 분포로 표시하지 않는다.
 */
