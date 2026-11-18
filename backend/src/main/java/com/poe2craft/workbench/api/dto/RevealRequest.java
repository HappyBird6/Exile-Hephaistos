package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** state의 slotId에 대해 candidate로 지정한 공개 후보를 선택한다. */
public record RevealRequest(JsonNode state, String slotId, JsonNode candidate) {}
