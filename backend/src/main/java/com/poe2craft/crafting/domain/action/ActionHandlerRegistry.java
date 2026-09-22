package com.poe2craft.crafting.domain.action;

/*
 * ActionHandlerRegistry: 규칙의 handler 식별자와 버전에 대응하는 구현을 찾는다.
 * 등록 중복은 구성 오류로 처리하고 알 수 없는 Handler를 정상 결과로 대체하지 않는다.
 * Spring 의존 없이 구성된 Handler를 전달받는다. snapshot 발행 시 지원 검사도 별도로 필요하다.
 */
