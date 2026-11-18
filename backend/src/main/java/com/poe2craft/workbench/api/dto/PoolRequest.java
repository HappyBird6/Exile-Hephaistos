package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** state는 옵션 목록을 조회할 아이템이고 dynamic은 현재 상태에 따른 후보 계산 여부다. */
public record PoolRequest(JsonNode state, boolean dynamic) {}
