package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
  static List<RecipeMoves.Choice> choices(
      ItemState item, int target, WorkbenchSimulator simulator, Set<String> protectedIds) {
    if (item.rarity() != ItemState.Rarity.RARE) return RecipeMoves.rarityPreparation(target, item);
    long count = mutableSlots(item);
    if (count == 0)
      return List.of(
          new RecipeMoves.Choice(
              target, new RecipeMoves.Move(WorkbenchCurrency.EXALTED.name()), true, null));
    if (count == 1) return List.of();
    var choices = new ArrayList<RecipeMoves.Choice>();
    choices.add(
        new RecipeMoves.Choice(
            target, new RecipeMoves.Move(WorkbenchCurrency.ANNULMENT.name()), true, null));
    // 어느 쪽을 제거해도 남은 하나를 카오스로 교체한다면 유료 징조에 이점이 없다.
    if (protectedIds.isEmpty()
        && item.desecrated().isEmpty()
        && !BasicJewel.supportedCrafting(item.baseItemId())) return choices;
    var workbench = simulator.forItem(item);
    var slots = new ArrayList<Removal>();
    for (var mod : item.explicits()) {
      if (mod.fractured()) continue;
      var side = workbench.catalog().find(mod.modifierId()).orElseThrow().affixType();
      boolean locked =
          BasicJewel.supportedCrafting(item.baseItemId())
              && !canRemainForChaos(item, mod, workbench);
      slots.add(new Removal(side, protectedIds.contains(mod.modifierId()) ? -1 : locked ? 1 : 0));
    }
    for (var slot : item.desecrated()) {
      if (slot.modifierId() == null) slots.add(new Removal(slot.side(), 1));
    }
    int first = slots.stream().mapToInt(Removal::priority).max().orElseThrow();
    long preferred = slots.stream().filter(s -> s.priority() == first).count();
    if (preferred == slots.size()) return choices;
    for (var omen : List.of(WorkbenchOmen.SINISTRAL_ANNULMENT, WorkbenchOmen.DEXTRAL_ANNULMENT)) {
      var side = slots.stream().filter(s -> s.side() == omen.affix()).toList();
      long wanted = side.stream().filter(s -> s.priority() == first).count();
      if (wanted * slots.size() > preferred * side.size())
        choices.add(
            new RecipeMoves.Choice(
                target, new RecipeMoves.Move(WorkbenchCurrency.ANNULMENT, omen), true, null));
    }
    return choices;
  }

  /** 먼저 제거할 미공개·잠금 옵션, 일반 옵션, 보호 옵션을 현재 방향별 실제 점유 수에 포함한다. */
  private record Removal(AffixType side, int priority) {}

  /** 주얼에서 해당 하나를 남겼을 때 분열과 소켓을 보존한 채 실제 무징조 카오스를 쓸 수 있는지 확인한다. */
  private static boolean canRemainForChaos(
      ItemState item, ModifierInstance survivor, WorkbenchSimulator simulator) {
    var mods = item.explicits().stream().filter(m -> m.fractured() || m.equals(survivor)).toList();
    var base =
        item.withDesecrated(
            item.desecrated().stream()
                .filter(
                    s ->
                        s.modifierId() != null
                            && mods.stream().anyMatch(m -> s.modifierId().equals(m.modifierId())))
                .toList());
    var remaining = RecipeStateGroups.withAffixes(base, ItemState.Rarity.RARE, mods);
    return simulator.availability(remaining, WorkbenchCurrency.CHAOS, Set.of()).available();
  }

  /** 분열은 제거할 수 없다. 공개된 훼손은 이미 명시 옵션 목록에 있고 미공개 훼손만 별도의 점유 칸이다. */
  private static long mutableSlots(ItemState item) {
    return item.explicits().stream().filter(mod -> !mod.fractured()).count()
        + item.desecrated().stream().filter(slot -> slot.modifierId() == null).count();
  }
}
