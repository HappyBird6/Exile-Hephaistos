package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import java.util.List;

/** 같은 시작 아이템을 교체할 명시 옵션 하나의 희귀 아이템으로 만드는 다음 한 단계만 고른다. 분열은 그대로 남는다. */
final class OneOptionPreparation {
  /** 상태를 갖지 않으며 팀의 후보 생성과 고정 경로 재검증이 같은 조건을 쓴다. */
  private OneOptionPreparation() {}

  /** 희귀이고 공개된 비분열 옵션 단 하나를 교체할 수 있어야 일반 카오스 반복을 시작할 수 있다. */
  static boolean ready(ItemState item) {
    return item.rarity() == ItemState.Rarity.RARE
        && mutableSlots(item) == 1
        && item.desecrated().stream().noneMatch(slot -> slot.modifierId() == null);
  }

  /** 희귀까지 승급하고 두 칸 이상이면 소멸로 한 칸씩 줄인다. 각 징조의 실제 자격과 L 보존은 작업대 전이를 따른다. */
  static List<RecipeMoves.Choice> choices(ItemState item, int target) {
    if (item.rarity() != ItemState.Rarity.RARE) return RecipeMoves.rarityPreparation(target, item);
    long count = mutableSlots(item);
    if (count == 0)
      return List.of(
          new RecipeMoves.Choice(
              target, new RecipeMoves.Move(WorkbenchCurrency.EXALTED.name()), true, null));
    if (count == 1) return List.of();
    return List.of(
        new RecipeMoves.Choice(
            target, new RecipeMoves.Move(WorkbenchCurrency.ANNULMENT.name()), true, null),
        new RecipeMoves.Choice(
            target,
            new RecipeMoves.Move(WorkbenchCurrency.ANNULMENT, WorkbenchOmen.SINISTRAL_ANNULMENT),
            true,
            null),
        new RecipeMoves.Choice(
            target,
            new RecipeMoves.Move(WorkbenchCurrency.ANNULMENT, WorkbenchOmen.DEXTRAL_ANNULMENT),
            true,
            null));
  }

  /** 분열은 제거할 수 없다. 공개된 훼손은 이미 명시 옵션 목록에 있고 미공개 훼손만 별도의 점유 칸이다. */
  private static long mutableSlots(ItemState item) {
    return item.explicits().stream().filter(mod -> !mod.fractured()).count()
        + item.desecrated().stream().filter(slot -> slot.modifierId() == null).count();
  }
}
