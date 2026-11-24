package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
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

  /** 재생할 화폐의 상위 등급만 탐색에 쓴 기본 화폐에 대응시키고, 같은 단계의 징조와 장착 위치를 보존한다. */
  static Move baseMove(Move move) {
    String base =
        switch (move.action()) {
          case "GREATER_TRANSMUTATION", "PERFECT_TRANSMUTATION" -> "TRANSMUTATION";
          case "GREATER_AUGMENTATION", "PERFECT_AUGMENTATION" -> "AUGMENTATION";
          case "GREATER_REGAL", "PERFECT_REGAL" -> "REGAL";
          case "GREATER_EXALTED", "PERFECT_EXALTED" -> "EXALTED";
          case "GREATER_CHAOS", "PERFECT_CHAOS" -> "CHAOS";
          default -> move.action();
        };
    return base.equals(move.action()) ? move : new Move(base, move.omens(), move.socketIndex());
  }

  /** L 획득에서 시작 준비, 1옵션 반복, 완벽 엑잘, 마지막 훼손 슬롯의 각 행동을 구분한다. */
  enum Purpose {
    ORDINARY,
    LOW_PREPARATION,
    LOW_CHAOS,
    LOW_EXALT,
    LOW_LIGHT,
    LOW_BONE,
    LOW_REVEAL,
    LOW_REVEAL_MISS
  }

  /** eligibleTiers는 해당 L 예외가 실제로 얻을 수 있는 티어다. followup은 더미가 보호할 교체다. */
  record Choice(
      int target,
      Move move,
      boolean preparation,
      Move followup,
      Purpose purpose,
      Set<String> eligibleTiers) {
    Choice(int target, Move move, boolean preparation, Move followup) {
      this(target, move, preparation, followup, Purpose.ORDINARY, Set.of());
    }

    /** 교체 가능 옵션 하나로 시작하기 위한 준비 중 우연한 획득을 구분한다. */
    Choice asLowPreparation() {
      return new Choice(
          target, move, preparation, followup, Purpose.LOW_PREPARATION, eligibleTiers);
    }
  }

  /** 각 예외가 허용한 실제 L 티어만 적중이다. 다른 목표를 시도한 일반 추첨은 L 경로를 열지 않는다. */
  static boolean permits(
      Choice choice,
      Map<Integer, String> before,
      Map<Integer, String> after,
      Set<String> removed,
      Set<String> lowIds) {
    if (choice.purpose() == Purpose.LOW_REVEAL_MISS) return before.equals(after);
    for (var entry : after.entrySet()) {
      var id = entry.getValue();
      if (!lowIds.contains(id) || (id.equals(before.get(entry.getKey())) && !removed.contains(id)))
        continue;
      if (choice.purpose() == Purpose.LOW_PREPARATION) continue;
      if (choice.target() != entry.getKey() || !choice.eligibleTiers().contains(id)) return false;
    }
    if (choice.purpose() == Purpose.LOW_CHAOS
        || choice.purpose() == Purpose.LOW_EXALT
        || choice.purpose() == Purpose.LOW_REVEAL) {
      var acquired = after.get(choice.target());
      return acquired != null
          && choice.eligibleTiers().contains(acquired)
          && (!before.containsKey(choice.target())
              || removed.contains(before.get(choice.target())));
    }
    return true;
  }

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
    var mandatory =
        card.options().stream()
            .filter(o -> !held.containsKey(o.number()))
            .filter(
                o -> !o.tiers().isEmpty() && o.tiers().values().stream().allMatch(RecipeMoves::low))
            .toList();
    // 미보유 L 한 개는 다른 목표 다섯 개를 완성한 다음 훼손 슬롯에서 얻을 수 있다.
    boolean deferLast =
        mandatory.size() == 1
            && card.options().size() == 6
            && (held.size() == 5
                || card.options().stream()
                    .filter(o -> held.containsKey(o.number()))
                    .anyMatch(o -> low(o.tiers().get(held.get(o.number())))))
            && mandatory.getFirst().tiers().values().stream()
                .flatMap(Collection::stream)
                .anyMatch(s -> s.label().equals("D"));
    boolean lowFirst = !mandatory.isEmpty() && !deferLast;
    for (var opt : card.options()) {
      int target = opt.number();
      if (held.containsKey(target)) continue;
      if (lowFirst && !mandatory.contains(opt)) continue;
      lowMoves(result, item, card, opt, held, slots);
      for (var tier : opt.tiers().entrySet()) {
        if (low(tier.getValue())) continue;
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
              if (item.rarity() == ItemState.Rarity.NORMAL)
                result.addAll(rarityPreparation(target, item));
              else add(result, target, new Move(source.id()), false);
            }
            case "D" -> {
              if (item.rarity() != ItemState.Rarity.RARE)
                result.addAll(rarityPreparation(target, item));
              else if (item.desecrated().stream().anyMatch(s -> s.modifierId() == null))
                add(result, target, new Move("REVEAL"), false);
              else add(result, target, new Move(source.id()), false);
            }
            case "C" -> {
              if (item.rarity() != ItemState.Rarity.RARE) {
                result.addAll(rarityPreparation(target, item));
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

  /** 각 L 출처의 실제 소재·티어를 묶어 한 번의 등급 추첨 또는 공개 사건으로 등록한다. */
  private void lowMoves(
      Set<Choice> result,
      ItemState item,
      RouteCards.Card card,
      RouteCards.Option option,
      Map<Integer, String> held,
      ItemStateValidator.AffixSlots slots) {
    var chaosTiers = new TreeSet<String>();
    var exaltTiers = new TreeSet<String>();
    var boneTiers = new LinkedHashMap<String, Set<String>>();
    int target = option.number();
    for (var tier : option.tiers().entrySet()) {
      if (!low(tier.getValue())) continue;
      var definition = simulator.catalog().find(tier.getKey());
      if (definition.isEmpty()) continue;
      for (var source : tier.getValue()) {
        if (source.label().equals("D") && held.size() == 5)
          boneTiers.computeIfAbsent(source.id(), k -> new TreeSet<>()).add(tier.getKey());
        if (source.frequency() != RouteCards.Frequency.L) continue;
        if (source.label().equals("A")
            && item.installedAugments().stream()
                .noneMatch(a -> source.id().equals(a.augmentId()))) {
          install(result, target, item, source.id());
        } else if (item.rarity() == ItemState.Rarity.RARE) {
          if (OneOptionPreparation.ready(item)) chaosTiers.add(tier.getKey());
          if (free(slots, definition.get().affixType()) > 0) exaltTiers.add(tier.getKey());
        } else result.addAll(rarityPreparation(target, item));
      }
    }
    if (!chaosTiers.isEmpty())
      result.add(
          new Choice(
              target,
              new Move(WorkbenchCurrency.CHAOS.name()),
              false,
              null,
              Purpose.LOW_CHAOS,
              Set.copyOf(chaosTiers)));
    if (!exaltTiers.isEmpty())
      result.add(
          new Choice(
              target,
              new Move(WorkbenchCurrency.PERFECT_EXALTED.name()),
              false,
              null,
              Purpose.LOW_EXALT,
              Set.copyOf(exaltTiers)));
    if (boneTiers.isEmpty() || item.rarity() != ItemState.Rarity.RARE) return;
    if (item.desecrated().isEmpty()) {
      if (item.explicitSlotCount() == 5)
        boneTiers.forEach(
            (id, tiers) ->
                result.add(
                    new Choice(
                        target, new Move(id), true, null, Purpose.LOW_BONE, Set.copyOf(tiers))));
    } else {
      var revealTiers = new TreeSet<String>();
      boneTiers.values().forEach(revealTiers::addAll);
      lowDesecration(result, target, item, held, card, Set.copyOf(revealTiers));
    }
  }

  /** B/A 추첨에 희소도가 있는 티어에 한해서 L 보호·첫 획득 규칙을 적용한다. */
  private static boolean low(List<RouteCards.Source> sources) {
    return sources.stream().anyMatch(s -> s.frequency() == RouteCards.Frequency.L);
  }

  /** 보존할 다섯 옵션을 남기고 마지막 한 슬롯에만 뼈, 공개 또는 빛+소멸을 쓴다. */
  private void lowDesecration(
      Set<Choice> result,
      int target,
      ItemState item,
      Map<Integer, String> held,
      RouteCards.Card card,
      Set<String> tiers) {
    if (item.rarity() != ItemState.Rarity.RARE) return;
    if (item.explicitSlotCount() != 6 || item.desecrated().size() != 1) return;
    var slot = item.desecrated().getFirst();
    if (slot.modifierId() != null && held.containsValue(slot.modifierId())) return;
    if (slot.modifierId() == null) {
      var workbench = simulator.forItem(item);
      var catalog = workbench.catalog();
      var abyss = AbyssCraftingState.from(item, catalog);
      var eligible = abyss.eligibleRevealIds(slot.id(), catalog);
      if (!Collections.disjoint(tiers, eligible)) {
        result.add(new Choice(target, new Move("REVEAL"), false, null, Purpose.LOW_REVEAL, tiers));
        // 결과를 모르는 공개에서도 실제 가능한 실패 한 개만 대표 상태로 보여준다.
        var goalTiers = new HashSet<String>();
        for (var option : card.options()) goalTiers.addAll(option.tiers().keySet());
        eligible.stream()
            .filter(id -> !goalTiers.contains(id))
            .map(id -> catalog.find(id).orElseThrow())
            .filter(d -> d.stats().size() == 1 || workbench.automaticJointRolls(d).isPresent())
            .min(
                Comparator.comparingInt((ModifierDefinition d) -> d.stats().size())
                    .thenComparing(ModifierDefinition::id))
            .ifPresent(
                d ->
                    result.add(
                        new Choice(
                            target,
                            new Move("REVEAL"),
                            true,
                            null,
                            Purpose.LOW_REVEAL_MISS,
                            Set.of(d.id()))));
        return;
      }
    }
    result.add(
        new Choice(
            target,
            new Move(WorkbenchCurrency.ANNULMENT, WorkbenchOmen.LIGHT),
            true,
            null,
            Purpose.LOW_LIGHT,
            tiers));
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
          case NORMAL -> List.of(WorkbenchCurrency.TRANSMUTATION);
          case MAGIC -> List.of(WorkbenchCurrency.AUGMENTATION, WorkbenchCurrency.REGAL);
          case RARE -> List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.CHAOS);
          default -> List.<WorkbenchCurrency>of();
        };
    for (var currency : actions) {
      boolean upgrades =
          currency == WorkbenchCurrency.TRANSMUTATION || currency == WorkbenchCurrency.REGAL;
      add(result, target, new Move(currency.name()), upgrades);
      for (var omen : WorkbenchOmen.values()) {
        if (currency == WorkbenchCurrency.CHAOS) {
          if (omen.trigger() == WorkbenchCurrency.CHAOS
              && (omen.affix() != null || omen == WorkbenchOmen.WHITTLING))
            add(result, target, new Move(currency, omen), false);
        } else if (omen.trigger() != null
            && omen.trigger().baseAction() == currency.baseAction()
            && side == omen.affix()) {
          add(result, target, new Move(currency, omen), upgrades);
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

  /** 시작 희귀도를 한 단계 올리는 기본 화폐의 적중과 승급만 이뤄진 결과를 모두 이어간다. */
  static List<Choice> rarityPreparation(int target, ItemState item) {
    return switch (item.rarity()) {
      case NORMAL ->
          List.of(new Choice(target, new Move(WorkbenchCurrency.TRANSMUTATION.name()), true, null));
      case MAGIC ->
          List.of(new Choice(target, new Move(WorkbenchCurrency.REGAL.name()), true, null));
      default -> List.of();
    };
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
