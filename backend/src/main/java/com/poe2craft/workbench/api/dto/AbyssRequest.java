package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.Set;

/**
 * 훼손 작업대 요청이다. state와 slotId는 대상 상태와 슬롯이며 side는 접두·접미 구분이다. candidates와 candidateIndex는 관측한 후보와 선택
 * 위치이며 action, activeOmens, targetIds는 사용할 화폐·징조·목표 옵션이다.
 */
public record AbyssRequest(
    JsonNode state,
    String slotId,
    ModifierDefinition.AffixType side,
    JsonNode candidates,
    Integer candidateIndex,
    WorkbenchCurrency action,
    Set<String> activeOmens,
    Set<String> targetIds) {}
