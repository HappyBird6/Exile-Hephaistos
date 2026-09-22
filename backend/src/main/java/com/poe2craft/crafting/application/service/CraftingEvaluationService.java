package com.poe2craft.crafting.application.service;

/*
 * crafting/application/service: 데이터 준비와 제작 평가 유스케이스의 실행 순서를 조율한다.
 * CraftingEvaluationService: 요청마다 snapshot 하나를 고정하고 공개 catalog API를 조회한다.
 * 조회 자료를 불변 EvaluationContext로 변환한 후 순수 Engine을 호출한다. 게임 계산은 맡지 않는다.
 */
