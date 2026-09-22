package com.poe2craft.item.api;

/*
 * item/api: 다른 모듈에 공개할 불변 아이템 상태 계약을 둔다.
 * ItemState: 제작 전후의 아이템 상태와 snapshot 식별자를 표현한다.
 * 구현 시 순수 Java record를 사용하고 컬렉션도 방어 복사한다. persistence entity와 분리한다.
 */
