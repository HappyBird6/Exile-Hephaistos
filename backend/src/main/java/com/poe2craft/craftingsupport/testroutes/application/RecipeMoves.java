package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
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
    DIRECT_UPGRADE,
    LOW_PREPARATION,
    LOW_CHAOS,
    LOW_EXALT,
    LOW_SLOT_PREPARATION,
    BONE,
    LOW_REVEAL,
    DESECRATION
  }

  /** eligibleTiers는 해당 경로에서 얻을 수 있는 목표 티어다. followup은 더미로 보호할 교체 행동 또는 방향을 확정할 뼈다. */
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
    for (var entry : after.entrySet()) {
      var id = entry.getValue();
      if (!lowIds.contains(id) || (id.equals(before.get(entry.getKey())) && !removed.contains(id)))
        continue;
      if (choice.purpose() == Purpose.LOW_PREPARATION
          || choice.purpose() == Purpose.LOW_SLOT_PREPARATION) continue;
      boolean sameChaosHit =
          choice.purpose() == Purpose.LOW_CHAOS && id.equals(after.get(choice.target()));
      if ((!sameChaosHit && choice.target() != entry.getKey())
          || !choice.eligibleTiers().contains(id)) return false;
    }
    if (choice.purpose() == Purpose.LOW_CHAOS
        || choice.purpose() == Purpose.LOW_EXALT
        || choice.purpose() == Purpose.LOW_REVEAL
        || choice.purpose() == Purpose.DESECRATION) {
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
    return choices(item, card, held, false);
  }

  /** 고정 경로의 엑잘은 새 추천의 훼손 선호를 적용하기 전 기본·증강 추첨 후보로도 검증한다. */
  List<Choice> choices(
      ItemState item, RouteCards.Card card, Map<Integer, String> held, boolean replayExalt) {
    var result = new LinkedHashSet<Choice>();
    var slots = validator.slots(item);
    var pendingReveal = pendingReveal(item, card, false);
    var protectedIds = new HashSet<String>();
    for (var option : card.options()) {
      var tier = held.get(option.number());
      if (tier != null && low(option.tiers().get(tier))) protectedIds.add(tier);
    }
    var mandatory =
        card.options().stream()
            .filter(o -> !held.containsKey(o.number()))
            .filter(
                o -> !o.tiers().isEmpty() && o.tiers().values().stream().allMatch(RecipeMoves::low))
            .toList();
    var reserved = preferredReveals(item, card, held, pendingReveal, protectedIds);
    boolean lowFirst = mandatory.stream().anyMatch(o -> !reserved.containsKey(o.number()));
    for (var opt : card.options()) {
      int target = opt.number();
      if (held.containsKey(target)) continue;
      if (lowFirst && !mandatory.contains(opt)) continue;
      var revealTiers = reserved.get(target);
      // 동률인 여러 목표에는 다른 목표를 먼저 얻는 분기가 필요하다. 하나만 남으면 그 목표의 추첨을 훼손으로 대체한다.
      boolean useReveal = revealTiers != null && reserved.size() == 1;
      boolean preferRevealDraw = useReveal && !replayExalt;
      lowMoves(result, item, opt, slots, pendingReveal);
      if (revealTiers == null && item.rarity() == ItemState.Rarity.RARE)
        result.removeIf(
            c ->
                c.target() == target
                    && (c.purpose() == Purpose.BONE || c.purpose() == Purpose.LOW_REVEAL));
      if (useReveal) {
        result.removeIf(c -> c.target() == target && c.purpose() == Purpose.LOW_EXALT);
      }
      var bones = new LinkedHashMap<String, Set<String>>();
      for (var tier : opt.tiers().entrySet()) {
        if (low(tier.getValue())) continue;
        var definition = simulator.catalog().find(tier.getKey());
        if (definition.isEmpty()) continue;
        var side = definition.get().affixType();
        for (var source : tier.getValue()) {
          switch (source.label()) {
            case "B" ->
                baseMoves(result, target, item, side, slots, !reserved.isEmpty(), preferRevealDraw);
            case "A" -> {
              if (item.installedAugments().stream()
                  .anyMatch(a -> source.id().equals(a.augmentId()))) {
                baseMoves(result, target, item, side, slots, !reserved.isEmpty(), preferRevealDraw);
              } else install(result, target, item, source.id());
            }
            case "E" -> {
              if (item.rarity() == ItemState.Rarity.NORMAL)
                result.addAll(rarityPreparation(target, item));
              else add(result, target, new Move(source.id()), false);
            }
            case "D" -> bones.computeIfAbsent(source.id(), k -> new TreeSet<>()).add(tier.getKey());
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
                    result,
                    item,
                    target,
                    WorkbenchCurrency.valueOf(source.id()),
                    side,
                    slots,
                    held.values(),
                    protectedIds);
            }
            default -> throw new IllegalArgumentException("Unknown acquisition source");
          }
        }
      }
      if (!reserved.isEmpty()) addWhittlingForBlockedDraw(result, opt, item, held);
      if (item.rarity() != ItemState.Rarity.RARE || revealTiers != null)
        desecrationChoices(result, item, target, bones, pendingReveal, false);
      if (useReveal
          && held.size() + 1 == card.options().size()
          && opt.tiers().values().stream()
              .flatMap(List::stream)
              .anyMatch(src -> src.frequency() != null))
        fillForReveal(result, item, opt, slots, revealTiers);
    }
    // 공개는 마지막 옵션을 얻는 행동이다. 뼈가 먼저 부여한 미공개 슬롯은 그때까지 실제 작업대 상태에 남는다.
    if (held.size() + 1 < card.options().size())
      result.removeIf(c -> c.move().action().equals("REVEAL"));
    return List.copyOf(result);
  }

  /** 같은 계열의 비목표 X가 막은 추첨이면, 절사로 바로 그 X를 제거하고 목표를 얻어 다른 옵션에 훼손을 남길 수 있다. */
  private void addWhittlingForBlockedDraw(
      Set<Choice> result, RouteCards.Option option, ItemState item, Map<Integer, String> held) {
    var workbench = simulator.forItem(item);
    var catalog = workbench.catalog();
    var families = new HashSet<String>();
    for (var tier : option.tiers().entrySet()) {
      boolean ordinary =
          tier.getValue().stream()
              .anyMatch(
                  source ->
                      source.frequency() != null
                          && source.frequency() != RouteCards.Frequency.L
                          && (source.label().equals("B")
                              || source.label().equals("A")
                                  && item.installedAugments().stream()
                                      .anyMatch(a -> a.augmentId().equals(source.id()))));
      if (ordinary) families.addAll(catalog.find(tier.getKey()).orElseThrow().familyIds());
    }
    if (families.isEmpty()) return;
    var blockers = new HashSet<String>();
    for (var mod : item.explicits())
      if (!mod.fractured()
          && !held.containsValue(mod.modifierId())
          && !Collections.disjoint(
              families, catalog.find(mod.modifierId()).orElseThrow().familyIds()))
        blockers.add(mod.modifierId());
    if (blockers.isEmpty()) return;
    for (var omens : WhittlingRecovery.OMEN_COMBINATIONS) {
      if (workbench.chaosRemovalCandidates(item, WorkbenchCurrency.CHAOS, omens).stream()
          .anyMatch(m -> blockers.contains(m.modifierId())))
        add(result, option.number(), new Move(WorkbenchCurrency.CHAOS.name(), omens, null), false);
    }
  }

  /** 현재 미보유 목표에서 실제 공개할 수 있는 옵션을 골라, 다른 방법으로 가장 얻기 어려운 목표에 마지막 공개를 배정한다. */
  private Map<Integer, Set<String>> preferredReveals(
      ItemState item,
      RouteCards.Card card,
      Map<Integer, String> held,
      Set<String> pendingReveal,
      Set<String> protectedIds) {
    if (item.rarity() != ItemState.Rarity.RARE
        || (item.desecrated().isEmpty() && !item.conditions().isEmpty())) return Map.of();
    Map<Integer, Set<String>> preferred = new HashMap<>();
    var tied = new ArrayList<RouteCards.Option>();
    int priority = Integer.MAX_VALUE;
    Set<String> reservable = null;
    var slots = validator.slots(item);
    boolean defer = held.size() + 1 < card.options().size();
    Map<String, Set<String>> fullBoardTiers = null;
    if (item.desecrated().isEmpty() && slots.remainingPrefixes() + slots.remainingSuffixes() == 0) {
      fullBoardTiers = new HashMap<>();
      for (var option : card.options()) {
        if (held.containsKey(option.number())) continue;
        for (var tier : option.tiers().entrySet())
          for (var source : tier.getValue())
            if (source.label().equals("D"))
              fullBoardTiers.computeIfAbsent(source.id(), k -> new HashSet<>()).add(tier.getKey());
      }
      var catalog = simulator.forItem(item).catalog();
      fullBoardTiers.replaceAll(
          (bone, targets) ->
              RecipeTransitions.reservableBoneTargets(
                  item, bone, catalog, targets, protectedIds, defer));
    }
    // 카드에 C만 있는 목표와 이미 확보한 목표에는 훼손 후보 검사를 수행할 필요가 없다.
    for (var option : card.options()) {
      if (held.containsKey(option.number())) continue;
      var sources = option.tiers().values().stream().flatMap(List::stream).toList();
      if (sources.stream().noneMatch(s -> s.label().equals("D"))) continue;
      boolean craftable = hasCraftAlternative(item, option, sources, protectedIds);
      var frequency =
          sources.stream()
              .map(RouteCards.Source::frequency)
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .orElse(null);
      // 희귀 단계에서는 승급 에센스를 쓸 수 없다. D 전용부터, L/M/H, 제작 대안이 있는 목표 순이다.
      int rank =
          craftable
              ? RouteCards.Frequency.values().length + 1
              : frequency == null ? 0 : frequency.ordinal() + 1;
      if (rank > priority) continue;
      var tiers = revealTiers(option, item, pendingReveal, fullBoardTiers, protectedIds, defer);
      if (tiers.isEmpty()
          && defer
          && item.desecrated().stream().anyMatch(slot -> slot.modifierId() == null)) {
        // 남은 제작이 기존 일반 계열을 제거할 수 있다. 분열 충돌과 실제 마지막 공개는 우회하지 않는다.
        if (reservable == null) reservable = pendingReveal(item, card, true);
        tiers = revealTiers(option, item, reservable, fullBoardTiers, protectedIds, true);
      }
      if (tiers.isEmpty()) continue;
      if (rank < priority) {
        preferred.clear();
        tied.clear();
        priority = rank;
      }
      preferred.put(option.number(), tiers);
      tied.add(option);
    }
    if (tied.size() < 2 || priority == 0) return preferred;
    // 같은 빈도 분류 안에서만 실제 장착·슬롯·차단 계열이 반영된 엑잘 풀의 허용 티어 가중치 합으로 비교한다.
    var pool = simulator.forItem(item).pool(item, WorkbenchCurrency.EXALTED, null);
    var weights = new HashMap<Integer, Long>();
    for (var option : tied) {
      long weight =
          pool.stream()
              .filter(m -> option.tiers().containsKey(m.id()))
              .mapToLong(m -> m.weight())
              .sum();
      weights.put(option.number(), weight);
    }
    long least = Collections.min(weights.values());
    preferred.keySet().removeIf(number -> weights.get(number) != least);
    return preferred;
  }

  /** 희귀에서 쓸 수 있는 확정 제작이 실제로 원하는 티어를 붙이고 보호 옵션을 유지하는 분기가 있는지 확인한다. */
  private boolean hasCraftAlternative(
      ItemState item,
      RouteCards.Option option,
      List<RouteCards.Source> sources,
      Set<String> protectedIds) {
    for (var source : sources) {
      if (!source.label().equals("C")) continue;
      var plan =
          simulator
              .forItem(item)
              .recipePlan(item, WorkbenchCurrency.valueOf(source.id()), Set.of());
      if (plan.branches().stream()
          .anyMatch(
              branch ->
                  branch.candidates().stream()
                          .anyMatch(candidate -> option.tiers().containsKey(candidate.id()))
                      && branch.remaining().explicits().stream()
                          .map(m -> m.modifierId())
                          .toList()
                          .containsAll(protectedIds))) return true;
    }
    return false;
  }

  /** 적격 재료·티어·빈칸이나 이미 열린 슬롯을 검사한다. 풀 보드의 뼈는 실제 교체로 해당 슬롯이 열려야 한다. */
  private Set<String> revealTiers(
      RouteCards.Option option,
      ItemState item,
      Set<String> pending,
      Map<String, Set<String>> fullBoardTiers,
      Set<String> protectedIds,
      boolean defer) {
    var ids = new LinkedHashSet<String>();
    if (!item.desecrated().isEmpty()) {
      for (var entry : option.tiers().entrySet())
        if (pending.contains(entry.getKey())
            && entry.getValue().stream().anyMatch(s -> s.label().equals("D")))
          ids.add(entry.getKey());
      return ids;
    }
    var workbench = simulator.forItem(item);
    for (var entry : option.tiers().entrySet()) {
      if (entry.getValue().stream().noneMatch(s -> s.label().equals("D"))) continue;
      if (fullBoardTiers != null) {
        if (entry.getValue().stream()
            .anyMatch(
                source ->
                    source.label().equals("D")
                        && fullBoardTiers.get(source.id()).contains(entry.getKey())))
          ids.add(entry.getKey());
        continue;
      }
      var d = workbench.catalog().find(entry.getKey()).orElse(null);
      if (d == null || !canReserve(item, d.id(), protectedIds, defer)) continue;
      // 장착으로 활성화된 증강물 옵션은 현재 카탈로그에서 실제 추첨 가중치를 갖는다.
      var bones =
          DesecrationCurrency.sources(item.baseItemId(), item.itemLevel(), d, d.weight() > 0);
      if (entry.getValue().stream()
          .anyMatch(source -> source.label().equals("D") && bones.contains(source.id())))
        ids.add(d.id());
    }
    return ids;
  }

  /** 뼈를 쓰기 전의 목표 쪽 한 칸과 배타 계열을 보존한다. 재료·공개 자격은 실제 슬롯에서 다시 검증한다. */
  boolean canReserve(ItemState item, String tier) {
    return canReserve(item, tier, Set.of(), false);
  }

  /** 나중에 공개할 때에는 후속 제작이 제거할 수 있는 일반 옵션의 계열 충돌을 임시로 허용한다. */
  private boolean canReserve(ItemState item, String tier, Set<String> protectedIds, boolean defer) {
    var catalog = simulator.forItem(item).catalog();
    var definition = catalog.find(tier).orElse(null);
    return definition != null
        && definition.requiredItemLevel() <= item.itemLevel()
        && free(validator.slots(item), definition.affixType()) > 0
        && item.explicits().stream()
            .filter(m -> !defer || m.fractured() || protectedIds.contains(m.modifierId()))
            .map(m -> catalog.find(m.modifierId()).orElseThrow())
            .noneMatch(m -> !Collections.disjoint(m.familyIds(), definition.familyIds()));
  }

  /** 같은 뼈가 실제로 양쪽을 열 수 있을 때에만 반대쪽 칸을 닫아 목표 방향을 확정하는 준비를 제안한다. */
  private void fillForReveal(
      Set<Choice> result,
      ItemState item,
      RouteCards.Option option,
      ItemStateValidator.AffixSlots slots,
      Set<String> tiers) {
    if (!item.desecrated().isEmpty()
        || slots.remainingPrefixes() <= 0
        || slots.remainingSuffixes() <= 0) return;
    var catalog = simulator.forItem(item).catalog();
    var eligibleByBone = new HashMap<String, Set<String>>();
    for (String tier : tiers) {
      var side = catalog.find(tier).orElseThrow().affixType();
      var omen =
          side == AffixType.PREFIX
              ? WorkbenchOmen.DEXTRAL_EXALTATION
              : WorkbenchOmen.SINISTRAL_EXALTATION;
      for (var source : option.tiers().get(tier)) {
        if (!source.label().equals("D")
            || !eligibleByBone
                .computeIfAbsent(
                    source.id(),
                    bone -> RecipeTransitions.twoSidedRevealTargets(item, bone, catalog, tiers))
                .contains(tier)) continue;
        result.add(
            new Choice(
                option.number(),
                new Move(WorkbenchCurrency.EXALTED, omen),
                true,
                new Move(source.id()),
                Purpose.LOW_SLOT_PREPARATION,
                Set.of(tier)));
      }
    }
  }

  /** 각 L 출처의 실제 소재·티어를 묶어 한 번의 등급 추첨 또는 공개 사건으로 등록한다. */
  private void lowMoves(
      Set<Choice> result,
      ItemState item,
      RouteCards.Option option,
      ItemStateValidator.AffixSlots slots,
      Set<String> pendingReveal) {
    var chaosTiers = new TreeSet<String>();
    var exaltTiers = new TreeSet<String>();
    var boneTiers = new LinkedHashMap<String, Set<String>>();
    int target = option.number();
    for (var tier : option.tiers().entrySet()) {
      if (!low(tier.getValue())) continue;
      var definition = simulator.catalog().find(tier.getKey());
      if (definition.isEmpty()) continue;
      for (var source : tier.getValue()) {
        if (source.label().equals("D"))
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
    desecrationChoices(result, item, target, boneTiers, pendingReveal, true);
  }

  /** B/A 추첨에 희소도가 있는 티어에 한해서 L 보호·첫 획득 규칙을 적용한다. */
  private static boolean low(List<RouteCards.Source> sources) {
    return sources.stream().anyMatch(s -> s.frequency() == RouteCards.Frequency.L);
  }

  /** 이미 열린 미공개 슬롯에서 실제 선택 가능한 티어를 베이스·방향·전용 공개 풀로 검증한다. */
  private Set<String> pendingReveal(ItemState item, RouteCards.Card card, boolean deferSelection) {
    if (item.desecrated().stream().noneMatch(s -> s.modifierId() == null)) return Set.of();
    var catalog = simulator.forItem(item).catalog();
    var abyss = AbyssCraftingState.from(item, catalog);
    var targets = new HashSet<String>();
    for (var option : card.options()) targets.addAll(option.tiers().keySet());
    var eligible = new TreeSet<String>();
    for (var slot : item.desecrated()) {
      if (slot.modifierId() == null)
        eligible.addAll(
            deferSelection
                ? RecipeTransitions.reservableRevealIds(abyss, slot.id(), catalog, targets)
                : abyss.eligibleRevealIds(slot.id(), catalog, targets));
    }
    return eligible;
  }

  /** 빈칸에 쓰는 재료와 기존 슬롯의 공개 중 실제 선택만 해당 목표·재료별 티어로 묶는다. */
  private static void desecrationChoices(
      Set<Choice> result,
      ItemState item,
      int target,
      Map<String, Set<String>> bones,
      Set<String> pendingReveal,
      boolean low) {
    if (bones.isEmpty()) return;
    if (item.rarity() != ItemState.Rarity.RARE) {
      if (!low) result.addAll(rarityPreparation(target, item));
      return;
    }
    if (item.desecrated().isEmpty()) {
      bones.forEach(
          (id, tiers) ->
              result.add(
                  new Choice(target, new Move(id), true, null, Purpose.BONE, Set.copyOf(tiers))));
      return;
    }
    if (pendingReveal.isEmpty()) return;
    var eligible = new TreeSet<String>();
    bones.values().forEach(eligible::addAll);
    eligible.retainAll(pendingReveal);
    if (!eligible.isEmpty())
      result.add(
          new Choice(
              target,
              new Move("REVEAL"),
              false,
              null,
              low ? Purpose.LOW_REVEAL : Purpose.DESECRATION,
              Set.copyOf(eligible)));
  }

  /** 베이스 풀을 실제로 추첨할 수 있는 화폐와 목표 쪽 징조, 자리가 차면 제거 후보를 추가한다. */
  private void baseMoves(
      Set<Choice> result,
      int target,
      ItemState item,
      AffixType side,
      ItemStateValidator.AffixSlots slots,
      boolean reservingReveal,
      boolean useReveal) {
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
      // 마지막 공개 목표의 엑잘 추첨은 대체하지만, 최초 희귀 한 칸의 무징조 카오스 전략은 계속 독립적으로 비교한다.
      if (useReveal && currency == WorkbenchCurrency.EXALTED) continue;
      if (currency == WorkbenchCurrency.CHAOS && reservingReveal && free(slots, side) > 0) {
        if (OneOptionPreparation.ready(item))
          addBaseDraw(result, target, new Move(currency.name()), false);
        continue;
      }
      if (currency != WorkbenchCurrency.CHAOS || OneOptionPreparation.ready(item))
        addBaseDraw(result, target, new Move(currency.name()), upgrades);
      if (currency == WorkbenchCurrency.CHAOS) {
        // 절사의 징조로 가장 낮은 등장 레벨을 제거한다. 좌우 말소를 함께 쓰면 해당 쪽에서 고른다.
        for (var omens : WhittlingRecovery.OMEN_COMBINATIONS)
          add(result, target, new Move(currency.name(), omens, null), false);
        continue;
      }
      for (var omen : WorkbenchOmen.values()) {
        if (omen.trigger() != null
            && omen.trigger().baseAction() == currency.baseAction()
            && side == omen.affix()) {
          addBaseDraw(result, target, new Move(currency, omen), upgrades);
        }
      }
    }
    if (!useReveal && item.rarity() == ItemState.Rarity.RARE && free(slots, side) == 0) {
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

  /** 기본 풀의 목표를 직접 노리는 진화·제왕은 승급 미적중을 허용해도 의도한 적중이다. */
  private static void addBaseDraw(Set<Choice> result, int target, Move move, boolean upgrades) {
    result.add(
        new Choice(
            target,
            move,
            upgrades,
            null,
            upgrades ? Purpose.DIRECT_UPGRADE : Purpose.ORDINARY,
            Set.of()));
  }

  /** 기존 옵션을 보호하는 제거 징조와, 같은 쪽에 제거할 더미를 먼저 추가하는 방법을 살핀다. */
  private void replacement(
      Set<Choice> result,
      ItemState item,
      int target,
      WorkbenchCurrency currency,
      AffixType targetSide,
      ItemStateValidator.AffixSlots slots,
      Collection<String> heldGoals,
      Set<String> protectedIds) {
    add(result, target, new Move(currency.name()), false);
    if (currency.isAlloy() || currency.isLiquid()) {
      if (item.explicits().stream().allMatch(m -> m.fractured()))
        add(result, target, new Move(WorkbenchCurrency.EXALTED.name()), true);
      return;
    }
    var mutableSides = EnumSet.noneOf(AffixType.class);
    var protectedSides = EnumSet.noneOf(AffixType.class);
    var expendableSides = EnumSet.noneOf(AffixType.class);
    for (var mod : item.explicits()) {
      if (mod.fractured()) continue;
      var side = simulator.catalog().find(mod.modifierId()).orElseThrow().affixType();
      mutableSides.add(side);
      if (protectedIds.contains(mod.modifierId())) protectedSides.add(side);
      if (!heldGoals.contains(mod.modifierId())) expendableSides.add(side);
    }
    boolean roomForTarget = free(slots, targetSide) > 0;
    var safeSides =
        roomForTarget ? EnumSet.of(AffixType.PREFIX, AffixType.SUFFIX) : EnumSet.of(targetSide);
    safeSides.removeAll(protectedSides);
    boolean safeReplacement = !Collections.disjoint(mutableSides, safeSides);
    // L을 지키는 직접 교체가 있어도 남은 제거 후보가 전부 목표라면 한 번의 더미로 보존 분기를 연다.
    boolean canKeepAll = !Collections.disjoint(expendableSides, safeSides);
    for (var removal :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var next = new Move(currency, removal);
      add(result, target, next, false);
      var side = removal.affix();
      var otherSide = side == AffixType.PREFIX ? AffixType.SUFFIX : AffixType.PREFIX;
      boolean canPrepareSafeSide = safeSides.contains(otherSide) && free(slots, otherSide) > 0;
      if (canKeepAll
          || (protectedSides.contains(side) && (safeReplacement || canPrepareSafeSide))
          || expendableSides.contains(side)
          || free(slots, side) <= 0
          || (side != targetSide && !roomForTarget)) continue;
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
