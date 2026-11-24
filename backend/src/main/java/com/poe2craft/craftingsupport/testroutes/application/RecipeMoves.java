package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;

/** 카드의 허용 출처와 현재 작업대 상태에서 다음 화폐·징조 또는 준비 행동을 만든다. Choice의 target은 시도 목적이고 실제 획득 번호는 전이 뒤에 결정한다. */
final class RecipeMoves {
  /** action은 실제 화폐 또는 재료 식별자이며 omens는 같은 사용에 소비하는 징조다. */
  record Move(String action, Set<WorkbenchOmen> omens, Integer socketIndex) {
    Move(String action) {
      this(action, Set.of(), null);
    }

    Move(WorkbenchCurrency currency, WorkbenchOmen omen) {
      this(currency.name(), Set.of(omen), null);
    }
  }

  /** preparation이면 획득이 없는 중간 상태도 이어간다. followup은 더미가 보호할 교체다. */
  record Choice(int target, Move move, boolean preparation, Move followup) {}

  private final WorkbenchSimulator simulator;
  private final ItemStateValidator validator;

  /** simulator의 베이스 카탈로그로 슬롯과 재료 자격을 확인한다. */
  RecipeMoves(WorkbenchSimulator simulator) {
    this.simulator = simulator;
    validator = new ItemStateValidator(simulator.catalog());
  }

  /** 목표를 아직 보유하지 않은 상태에서 사용할 직접 획득 및 필수 준비 후보를 만든다. */
  List<Choice> choices(ItemState item, RouteCards.Card card, Map<Integer, String> held) {
    var result = new LinkedHashSet<Choice>();
    var slots = validator.slots(item);
    for (var opt : card.options()) {
      int target = opt.number();
      if (held.containsKey(target)) continue;
      for (var tier : opt.tiers().entrySet()) {
        var definition = simulator.catalog().find(tier.getKey());
        if (definition.isEmpty()) continue;
        var side = definition.get().affixType();
        for (var source : tier.getValue()) {
          switch (source.label()) {
            case "B" -> baseMoves(result, target, item, side, slots);
            case "A" -> {
              if (item.installedAugments().stream()
                  .anyMatch(a -> source.id().equals(a.augmentId()))) {
                baseMoves(result, target, item, side, slots);
              } else install(result, target, item, source.id());
            }
            case "E" -> {
              if (item.rarity() == ItemState.Rarity.NORMAL) rarityPreparation(result, target, item);
              else add(result, target, new Move(source.id()), false);
            }
            case "D" -> {
              if (item.rarity() != ItemState.Rarity.RARE) rarityPreparation(result, target, item);
              else if (item.desecrated().stream().anyMatch(s -> s.modifierId() == null))
                add(result, target, new Move("REVEAL"), false);
              else add(result, target, new Move(source.id()), false);
            }
            case "C" -> {
              if (item.rarity() != ItemState.Rarity.RARE) {
                rarityPreparation(result, target, item);
              } else if (item.explicits().stream()
                      .filter(m -> AugmentCraftingRules.crafted(m, simulator.catalog()))
                      .count()
                  >= AugmentCraftingRules.craftedLimit(item)) {
                install(result, target, item, "Astrids_Creativity");
                for (var mod : item.explicits()) {
                  if (mod.fractured()
                      || held.containsValue(mod.modifierId())
                      || !AugmentCraftingRules.crafted(mod, simulator.catalog())) continue;
                  var dropSide =
                      simulator.catalog().find(mod.modifierId()).orElseThrow().affixType();
                  add(result, target, new Move(WorkbenchCurrency.ANNULMENT.name()), true);
                  add(
                      result,
                      target,
                      new Move(
                          WorkbenchCurrency.ANNULMENT,
                          dropSide == AffixType.PREFIX
                              ? WorkbenchOmen.SINISTRAL_ANNULMENT
                              : WorkbenchOmen.DEXTRAL_ANNULMENT),
                      true);
                }
              } else
                replacement(
                    result, item, target, WorkbenchCurrency.valueOf(source.id()), side, slots);
            }
            default -> throw new IllegalArgumentException("Unknown acquisition source");
          }
        }
      }
    }
    return List.copyOf(result);
  }

  /** 베이스 풀을 실제로 추첨할 수 있는 화폐와 목표 쪽 징조, 자리가 차면 제거 후보를 추가한다. */
  private void baseMoves(
      Set<Choice> result,
      int target,
      ItemState item,
      AffixType side,
      ItemStateValidator.AffixSlots slots) {
    var actions =
        switch (item.rarity()) {
          case NORMAL -> Set.of(CraftingAction.TRANSMUTATION);
          case MAGIC -> Set.of(CraftingAction.AUGMENTATION, CraftingAction.REGAL);
          case RARE -> Set.of(CraftingAction.EXALTED, CraftingAction.CHAOS);
          default -> Set.<CraftingAction>of();
        };
    for (var currency : WorkbenchCurrency.values()) {
      if (currency.baseAction() == null || !actions.contains(currency.baseAction())) continue;
      add(result, target, new Move(currency.name()), false);
      for (var omen : WorkbenchOmen.values()) {
        if (currency.baseAction() == CraftingAction.CHAOS) {
          if (omen.trigger() == WorkbenchCurrency.CHAOS
              && (omen.affix() != null || omen == WorkbenchOmen.WHITTLING))
            add(result, target, new Move(currency, omen), false);
        } else if (omen.trigger() != null
            && omen.trigger().baseAction() == currency.baseAction()
            && side == omen.affix()) {
          add(result, target, new Move(currency, omen), false);
        }
      }
    }
    if (item.rarity() == ItemState.Rarity.RARE && free(slots, side) == 0) {
      add(result, target, new Move(WorkbenchCurrency.ANNULMENT.name()), true);
      add(
          result,
          target,
          new Move(
              WorkbenchCurrency.ANNULMENT,
              side == AffixType.PREFIX
                  ? WorkbenchOmen.SINISTRAL_ANNULMENT
                  : WorkbenchOmen.DEXTRAL_ANNULMENT),
          true);
    }
  }

  /** 기존 옵션을 보호하는 제거 징조와, 같은 쪽에 제거할 더미를 먼저 추가하는 방법을 살핀다. */
  private void replacement(
      Set<Choice> result,
      ItemState item,
      int target,
      WorkbenchCurrency currency,
      AffixType targetSide,
      ItemStateValidator.AffixSlots slots) {
    add(result, target, new Move(currency.name()), false);
    if (currency.isAlloy() || currency.isLiquid()) {
      if (item.explicits().stream().allMatch(m -> m.fractured()))
        add(result, target, new Move(WorkbenchCurrency.EXALTED.name()), true);
      return;
    }
    for (var removal :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var next = new Move(currency, removal);
      add(result, target, next, false);
      var side = removal.affix();
      if (free(slots, side) <= 0 || (side != targetSide && free(slots, targetSide) <= 0)) continue;
      if (item.explicits().stream()
          .anyMatch(
              m ->
                  !m.fractured()
                      && simulator.catalog().find(m.modifierId()).orElseThrow().affixType()
                          == side)) continue;
      var omen =
          side == AffixType.PREFIX
              ? WorkbenchOmen.SINISTRAL_EXALTATION
              : WorkbenchOmen.DEXTRAL_EXALTATION;
      result.add(new Choice(target, new Move(WorkbenchCurrency.EXALTED, omen), true, next));
    }
  }

  /** 열 수 있는 홈이 필요하면 장인의 오브를 먼저 사용한다. 장착 가능 여부는 작업대가 판정한다. */
  private static void install(Set<Choice> choices, int target, ItemState item, String augment) {
    if (item.augmentSockets() != null && item.augmentSockets() > item.installedAugments().size()) {
      add(choices, target, new Move(augment), true);
      return;
    }
    add(choices, target, new Move(WorkbenchCurrency.ARTIFICER.name()), true);
    for (var socket : item.installedAugments()) {
      if (!socket.augmentId().equals(augment)
          && !AugmentCraftingRules.socketBound(socket.augmentId()))
        add(choices, target, new Move(augment, Set.of(), socket.slotIndex()), true);
    }
  }

  /** Rare 전용 재료를 쓰기 위해 희귀도를 한 단계 올린다. 이 과정의 우연한 목표 획득도 기록된다. */
  private static void rarityPreparation(Set<Choice> choices, int target, ItemState item) {
    if (item.rarity() == ItemState.Rarity.NORMAL)
      add(choices, target, new Move(WorkbenchCurrency.TRANSMUTATION.name()), true);
    if (item.rarity() == ItemState.Rarity.MAGIC)
      add(choices, target, new Move(WorkbenchCurrency.REGAL.name()), true);
  }

  /** 지정한 쪽의 남은 명시 옵션 자리를 반환한다. */
  private static int free(ItemStateValidator.AffixSlots slots, AffixType side) {
    return side == AffixType.PREFIX ? slots.remainingPrefixes() : slots.remainingSuffixes();
  }

  /** 동일한 행동·준비 목적을 한 번만 등록한다. */
  private static void add(Set<Choice> choices, int target, Move move, boolean preparation) {
    choices.add(new Choice(target, move, preparation, null));
  }
}
