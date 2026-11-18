package com.poe2craft.workbench.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * 훼손 슬롯을 가진 아이템의 제작 절차를 담당한다. 생성자로 받은 simulator는 카탈로그와 확률 계산을, states는 입력 해석을, random은 화폐 실행 난수를
 * 제공한다. importAbyss는 일반 상태를 검증해 훼손 슬롯 상태로 바꾼다. desecrate와 addAbyss는 훼손 슬롯을 만들고 observeAbyss는 관측 후보를
 * 반영한다. eligibleReveal은 선택 가능한 옵션을 조회한다. selectReveal과 chooseAbyss는 확정한 후보를 적용한다. planAbyss는 다음 작업을
 * 계산하고 applyAbyss와 applyAbyssCurrency는 제작을 실행한다. compareAbyss는 다음 추가 옵션의 확률을 비교한다.
 */
public final class AbyssWorkbenchService {
  private final CraftingOperations simulator;
  private final WorkbenchStateReader states;
  private final SecureRandom random;

  public AbyssWorkbenchService(
      CraftingOperations simulator, WorkbenchStateReader states, SecureRandom random) {
    this.simulator = simulator;
    this.states = states;
    this.random = random;
  }

  public AbyssCraftingState importAbyss(JsonNode input) {
    var item = states.concreteState(input, true);
    var catalog = simulator.abyssCatalog(item);
    var validationItem = item;
    if (item.conditions().equals(Set.of(ItemState.Condition.CORRUPTED))
        && !item.desecrated().isEmpty())
      validationItem =
          new ItemState(
                  item.snapshotId(),
                  item.baseItemId(),
                  item.itemLevel(),
                  item.rarity(),
                  item.implicits(),
                  item.explicits(),
                  Set.of(),
                  item.augmentSockets(),
                  item.catalystQuality(),
                  item.desecrated(),
                  item.equipmentQuality(),
                  item.observations())
              .withAugments(item.installedAugments())
              .withQualityInfused(item.qualityInfused());
    if (!new ItemStateValidator(catalog).validate(validationItem).isEmpty())
      throw new IllegalArgumentException("Invalid slot-aware item state");
    return AbyssCraftingState.from(item, catalog);
  }

  public AbyssCraftingState desecrate(
      JsonNode input,
      ModifierDefinition.AffixType side,
      Set<String> activeOmens,
      List<ModifierDefinition.AffixType> observedSides,
      String currency,
      String removedSlotId) {
    var item = states.concreteState(input, true);
    var catalog = simulator.abyssCatalog(item);
    if (activeOmens != null && activeOmens.contains("Omen_of_Putrefaction"))
      return DesecrationCurrency.putrefy(item, currency, observedSides, activeOmens, catalog);
    if (observedSides != null && !observedSides.isEmpty())
      throw new IllegalArgumentException("Unexpected observed slots");
    return DesecrationCurrency.apply(
        item, currency, side, removedSlotId, activeOmens, catalog, random);
  }

  public AbyssCraftingState addAbyss(
      JsonNode input, String slotId, ModifierDefinition.AffixType side) {
    var state = states.abyssState(input);
    return state.addUnrevealed(slotId, side, simulator.abyssCatalog(state.item()));
  }

  public AbyssCraftingState observeAbyss(JsonNode input, String slotId, JsonNode candidates) {
    var state = states.abyssState(input);
    return state.observe(
        slotId, states.abyssCandidates(candidates), simulator.abyssCatalog(state.item()));
  }

  public List<String> eligibleReveal(JsonNode input, String slotId) {
    var state = states.abyssState(input);
    return state.eligibleRevealIds(slotId, simulator.abyssCatalog(state.item()));
  }

  public AbyssCraftingState selectReveal(JsonNode input, String slotId, JsonNode candidateNode)
      throws java.io.IOException {
    var state = states.abyssState(input);
    return state.selectDirect(
        slotId, states.revealCandidate(candidateNode), simulator.abyssCatalog(state.item()));
  }

  public AbyssCraftingState chooseAbyss(JsonNode input, Integer candidateIndex) {
    var state = states.abyssState(input);
    if (candidateIndex == null) throw new IllegalArgumentException("Candidate required");
    return state.choose(candidateIndex, simulator.abyssCatalog(state.item()));
  }

  public AbyssCraftingPlan planAbyss(
      JsonNode input, WorkbenchCurrency action, Set<String> activeOmens) {
    return simulator.abyssPlan(states.abyssState(input), action, activeOmens);
  }

  public AbyssCraftingState applyAbyss(
      JsonNode input, WorkbenchCurrency action, Set<String> activeOmens) {
    return simulator.applyAbyss(states.abyssState(input), action, activeOmens, random);
  }

  public AbyssCurrencyExecutor.Result applyAbyssCurrency(
      JsonNode input, WorkbenchCurrency action, Set<String> activeOmens) {
    return simulator.applyAbyssCurrency(states.abyssState(input), action, activeOmens, random);
  }

  public AbyssProbabilityComparison.Comparison compareAbyss(
      JsonNode input,
      String slotId,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      Set<String> targetIds) {
    var state = states.abyssState(input);
    if (targetIds == null || targetIds.isEmpty())
      throw new IllegalArgumentException("Target modifiers required");
    var omenIds = activeOmens == null ? Set.<String>of() : activeOmens;
    if (omenIds.size() > 1)
      throw new IllegalArgumentException("At most one addition omen supported");
    var omen = omenIds.isEmpty() ? null : WorkbenchOmen.fromId(omenIds.iterator().next());
    var plan = simulator.abyssPlan(state, action, omenIds);
    if (!plan.available()) throw new IllegalArgumentException("Next addition unavailable");
    return new AbyssProbabilityComparison(simulator.abyssCatalog(state.item()))
        .compare(state, slotId, action, omen, targetIds)
        .orElse(null);
  }
}
