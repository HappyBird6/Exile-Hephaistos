package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import java.util.List;

/**
 * emptyState는 갱신이 시작되는 상태다. targetModifierId와 probability는 목표 옵션과 성공 확률이며 eligibleModifierIds와
 * proofVersion은 증명에 사용한 후보와 규칙 버전이다.
 */
public record RenewalProof(
    BasicCurrencyState emptyState,
    String targetModifierId,
    Probability probability,
    List<String> eligibleModifierIds,
    String proofVersion) {}
