package com.poe2craft.community.api.dto;

/** 삭제 요청의 password는 권한 확인에 쓰고 revision은 편집 충돌을 확인할 판본이다. */
public record Deletion(String password, long revision) {}
