package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** 저장한 제작 세션의 호환성을 확인한다. state와 abyss는 일반·훼손 상태이며 compatibilityId는 저장 당시 호환성 식별자다. */
public record SessionRequest(JsonNode state, JsonNode abyss, String compatibilityId) {}
