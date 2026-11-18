package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import java.util.Set;

/** state와 abyss는 일반·훼손 상태다. action은 제작 화폐, activeOmens는 활성 징조, modifierId는 선택한 옵션이다. */
public record CustomExaltRequest(
    JsonNode state,
    JsonNode abyss,
    WorkbenchCurrency action,
    Set<String> activeOmens,
    String modifierId) {}
