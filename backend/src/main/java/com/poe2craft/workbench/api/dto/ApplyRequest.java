package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import java.util.Set;

/** state에 action으로 지정한 제작 화폐를 적용한다. activeOmens는 함께 적용할 징조다. */
public record ApplyRequest(JsonNode state, WorkbenchCurrency action, Set<String> activeOmens) {}
