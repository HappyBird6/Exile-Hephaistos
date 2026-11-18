package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.List;

/** state는 시작 상태이며 plan은 순서대로 평가할 행동이다. maxNodes, maxEdges, maxMillis는 노드 수·간선 수·실행 시간 한도다. */
public record ExploreRequest(
    StateBucket state, List<CraftingAction> plan, int maxNodes, int maxEdges, int maxMillis) {}
