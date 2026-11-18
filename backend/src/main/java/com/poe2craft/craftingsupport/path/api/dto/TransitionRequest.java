package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.StateBucket;

/** state는 현재 상태이며 action은 전이 분포를 계산할 제작 행동이다. */
public record TransitionRequest(StateBucket state, CraftingAction action) {}
