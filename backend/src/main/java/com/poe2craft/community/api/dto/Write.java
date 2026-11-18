package com.poe2craft.community.api.dto;

import java.util.UUID;

/** 댓글 작성·수정 요청이다. id는 댓글 식별자, body는 내용, revision은 편집 기준 판본이다. */
public record Write(UUID id, String body, long revision) {}
