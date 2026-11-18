package com.poe2craft.community.api.dto;

/** 게시물 수정 요청이다. title과 body는 새 제목과 본문이며 revision은 수정 기준 판본이다. */
public record TextEdit(String title, String body, long revision) {}
