package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** state의 slotIndex 위치에 augmentId로 지정한 증강물을 장착한다. */
public record AugmentRequest(JsonNode state, JsonNode slotIndex, String augmentId) {}
