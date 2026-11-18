package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** 가능한 행동을 조회한다. state는 현재 아이템 상태이며 activeOmens는 활성 징조 목록이다. */
public record ActionRequest(JsonNode state, Set<String> activeOmens) {}
