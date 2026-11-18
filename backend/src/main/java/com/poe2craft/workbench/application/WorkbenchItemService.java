package com.poe2craft.workbench.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.SocketAugments;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * 일반 아이템의 제작 요청을 실행한다. 생성자로 전달받은 simulator는 카탈로그와 제작 엔진을 제공하고 states는 저장 상태를 검증하며 random은 제작 난수를
 * 공급한다. installAugment는 슬롯 입력을 확인하고 장착 결과와 품질 한도를 만든다. apply는 화폐와 활성 징조를 실행한다. actions는 현재 가능한 화폐를,
 * qualityDisplay는 표시할 품질 수치를 조회한다.
 */
public final class WorkbenchItemService {
  private final CraftingOperations simulator;
  private final WorkbenchStateReader states;
  private final SecureRandom random;

  public WorkbenchItemService(
      CraftingOperations simulator, WorkbenchStateReader states, SecureRandom random) {
    this.simulator = simulator;
    this.states = states;
    this.random = random;
  }

  public AugmentResult installAugment(JsonNode input, JsonNode slotIndex, String augmentId) {
    if (slotIndex == null || !slotIndex.isIntegralNumber() || !slotIndex.canConvertToInt())
      throw new IllegalArgumentException("Integral socket index required");
    var state = states.concreteState(input, true);
    var catalog = simulator.abyssCatalog(state);
    com.poe2craft.shared.crafting.domain.CraftingItemState.resolve(state, null, catalog);
    var next = SocketAugments.install(state, slotIndex.intValue(), augmentId);
    return new AugmentResult(
        "socket-augments-v1",
        "ordinary-socket-effects-v1",
        next.snapshotId(),
        next,
        "INSTALL_AUGMENT",
        true,
        "",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        QualityLimitRules.describe(next, simulator.abyssCatalog(next)));
  }

  public WorkbenchSimulator.Result apply(
      JsonNode input, WorkbenchCurrency action, Set<String> activeOmens) {
    return simulator.apply(
        states.concreteState(
            input,
            action == WorkbenchCurrency.DIVINE
                && activeOmens != null
                && activeOmens.contains("Omen_of_Sanctification")),
        action,
        activeOmens,
        random);
  }

  public List<WorkbenchSimulator.Availability> actions(JsonNode input, Set<String> activeOmens) {
    return simulator.actions(states.concreteState(input, true), activeOmens);
  }

  public CraftingOperations.QualityDisplay qualityDisplay(JsonNode input) {
    return simulator.qualityDisplay(states.concreteState(input, true));
  }
}
