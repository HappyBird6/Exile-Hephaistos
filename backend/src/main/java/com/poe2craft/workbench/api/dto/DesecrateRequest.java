package com.poe2craft.workbench.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.List;
import java.util.Set;

/**
 * state에 currency로 훼손을 적용한다. side와 removedSlotId는 대상 옵션 측과 제거 슬롯이며 activeOmens와 observedSides는 활성
 * 징조와 관측한 접두·접미 구분이다.
 */
public record DesecrateRequest(
    JsonNode state,
    String currency,
    ModifierDefinition.AffixType side,
    String removedSlotId,
    Set<String> activeOmens,
    List<ModifierDefinition.AffixType> observedSides) {}
