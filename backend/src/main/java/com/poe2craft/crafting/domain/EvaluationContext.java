package com.poe2craft.crafting.domain;

/*
 * EvaluationContext: 하나의 고정된 snapshot에서 준비한 계산 자료와 버전을 담는 불변 내부 데이터다.
 * Engine에 필요한 자료를 제공하며 repository, 외부 서비스, 지연 조회 callback을 담지 않는다.
 * 컬렉션도 불변으로 유지하고 같은 입력과 버전에서 동일한 계산이 가능하도록 한다.
 */
