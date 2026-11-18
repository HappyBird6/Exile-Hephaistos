package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** state는 편집 기준 아이템이며 text는 변경 사항을 읽을 아이템 원문이다. */
public record EditTextRequest(JsonNode state, String text) {}
