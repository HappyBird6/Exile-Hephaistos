package com.poe2craft.account.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** 문서 저장 요청이다. version은 편집 기준 판본이며 content는 저장할 JSON이다. */
public record Save(long version, JsonNode content) {}
