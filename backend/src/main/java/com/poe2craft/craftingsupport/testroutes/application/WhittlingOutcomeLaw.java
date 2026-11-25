package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Set;

/** 절사 카오스 한 번의 제거 대상과 새 옵션별 질량을 실제 작업대의 후보와 가중치로 구한다. */
final class WhittlingOutcomeLaw {
  /** 각 요청의 작업대와 아이템 상태만 사용한다. */
  private WhittlingOutcomeLaw() {}

  /** 제작 자격을 바꾸는 수치는 열거하고 나머지는 실제 가능한 대표 굴림에 합산한다. 예산을 넘으면 확인한 분기만 원래 분모의 질량으로 돌려준다. */
  static RecipeTransitions.Law calculate(
      WorkbenchSimulator simulator, ItemState item, RecipeMoves.Move move, int budget) {
    if (!RecipeMoves.baseMove(move).action().equals("CHAOS")
        || !move.omens().contains(WorkbenchOmen.WHITTLING))
      throw new IllegalArgumentException("Expected a Chaos Orb with an Omen of Whittling");
    if (item.desecrated().stream().anyMatch(s -> s.modifierId() == null))
      return new RecipeTransitions.Law(List.of(), false, Set.of());
    return new RecipeTransitions(simulator, true)
        .successors(item, move.action(), move.omens(), Set.of(), budget);
  }

  /** ItemState가 명시 옵션을 정렬하므로 시작 상태와 실제 제거 이력을 대조해 새로 획득한 ID를 구한다. */
  static String addedId(ItemState before, RecipeTransitions.Outcome outcome) {
    for (var modifier : outcome.state().explicits()) {
      String id = modifier.modifierId();
      if (outcome.removedModifierIds().contains(id)
          || before.explicits().stream().noneMatch(old -> old.modifierId().equals(id))) return id;
    }
    throw new IllegalArgumentException("Chaos replacement has no new modifier");
  }
}
