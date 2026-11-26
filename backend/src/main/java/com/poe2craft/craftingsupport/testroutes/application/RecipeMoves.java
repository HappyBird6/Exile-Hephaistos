package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;

/** 카드의 허용 출처와 현재 작업대 상태에서 다음 화폐·징조 또는 준비 행동을 만든다. Choice의 target은 시도 목적이고 실제 획득 번호는 전이 뒤에 결정한다. */
final class RecipeMoves {
  /** action은 실제 화폐 또는 재료 식별자이며 omens는 같은 사용에 소비하는 징조다. */
  record Move(String action, Set<WorkbenchOmen> omens, Integer socketIndex, String boneOmen) {
    Move(String action, Set<WorkbenchOmen> omens, Integer socketIndex) {
      this(action, omens, socketIndex, null);
    }

    /** 방향 징조만 제거하여 비교할 화폐 조합을 만든다. 절사 등 다른 징조는 유지한다. */
    Move withoutDirection() {
      if (omens.stream().noneMatch(o -> o.affix() != null)) return this;
      var simpler = EnumSet.copyOf(omens);
      simpler.removeIf(o -> o.affix() != null);
      return new Move(action, Set.copyOf(simpler), socketIndex, boneOmen);
    }

    /** 작업대의 뼈 전용 징조와 일반 화폐 징조를 동일한 기록 형식으로 반환한다. */
    List<String> omenIds() {
      return boneOmen == null
          ? omens.stream().map(WorkbenchOmen::id).sorted().toList()
          : List.of(boneOmen);
    }

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
    /** 균열 접두를 붙일 자리를 마련할 때 실제로 지정한 제작 옵션이 제거된 분기다. */
    QUALITY_CLEAR,
    /** 품질 작업을 마친 뒤 임시 상한을 제거한다. 절사 카오스는 같은 사용으로 지정한 목표도 얻어야 한다. */
    QUALITY_CAP_REMOVAL,
    LOW_CHAOS,
    LOW_EXALT,
    BONE,
    LOW_REVEAL,
    DESECRATION
  }

  /** eligibleTiers는 해당 경로에서 얻을 수 있는 목표 티어다. followup은 더미를 제거할 에센스다. */
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

    /** 같은 준비 목적·티어·후속 행동을 가진 다른 징조 조합과 비교한다. */
    Choice withMove(Move other) {
      return new Choice(target, other, preparation, followup, purpose, eligibleTiers);
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
      if (choice.purpose() == Purpose.LOW_PREPARATION) continue;
      boolean sameChaosHit =
          choice.purpose() == Purpose.LOW_CHAOS && id.equals(after.get(choice.target()));
      if ((!sameChaosHit && choice.target() != entry.getKey())
          || !choice.eligibleTiers().contains(id)) return false;
    }
    if (choice.purpose() == Purpose.LOW_CHAOS
        || choice.purpose() == Purpose.LOW_EXALT
        || choice.purpose() == Purpose.LOW_REVEAL
        || choice.purpose() == Purpose.DESECRATION
        || (choice.purpose() == Purpose.QUALITY_CAP_REMOVAL && choice.target() > 0)) {
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

  /** 저장된 무징조 뼈 경로는 당시 선택한 방향 추첨 확률로 재생한다. */
  private final boolean replay;

  /** 새 경로 추천에는 목표 방향을 지정하는 뼈를 사용한다. */
  RecipeMoves(WorkbenchSimulator simulator) {
    this(simulator, false);
  }

  /** simulator의 베이스 카탈로그로 슬롯과 재료 자격을 확인한다. */
  RecipeMoves(WorkbenchSimulator simulator, boolean replay) {
    this.replay = replay;
    this.simulator = simulator;
    validator = new ItemStateValidator(simulator.catalog());
  }

  /** 목표를 아직 보유하지 않은 상태에서 사용할 직접 획득 및 필수 준비 후보를 만든다. */
  List<Choice> choices(ItemState item, RouteCards.Card card, Map<Integer, String> held) {
    return choices(item, card, held, false, false);
  }

  /** 고정 경로의 엑잘은 새 추천의 훼손 선호를 적용하기 전 기본·증강 추첨 후보로도 검증한다. */
  List<Choice> choices(
      ItemState item,
      RouteCards.Card card,
      Map<Integer, String> held,
      boolean replayExalt,
      boolean reserveQualitySlot) {
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
            case "D", "O" ->
                bones.computeIfAbsent(source.id(), k -> new TreeSet<>()).add(tier.getKey());
            case "C" -> {
              long availableCrafts =
                  AugmentCraftingRules.craftedLimit(item)
                      - item.explicits().stream()
                          .filter(m -> AugmentCraftingRules.crafted(m, simulator.catalog()))
                          .count();
              // 임시 품질 접두와 공존할 자리가 없으면 다른 제작 목표는 품질 작업 뒤에 얻는다.
              if (reserveQualitySlot
                  && availableCrafts <= 1
                  && !tier.getKey().equals(QualityLimitRules.BREACH_ID)) continue;
              if (item.rarity() != ItemState.Rarity.RARE) {
                result.addAll(rarityPreparation(target, item));
              } else if (availableCrafts <= 0) {
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
      boolean ordinary = whittlingTier(tier.getValue(), item, false);
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

  /** 절사로 노릴 수 있는 일반·이미 장착한 증강물의 목표 티어인지, 목표 수치의 합산 빈도로 판정한다. */
  private static boolean whittlingTier(
      List<RouteCards.Source> sources, ItemState item, boolean allowLow) {
    return sources.stream()
        .anyMatch(
            source ->
                source.frequency() != null
                    && (allowLow || source.frequency() != RouteCards.Frequency.L)
                    && (source.label().equals("B")
                        || source.label().equals("A")
                            && item.installedAugments().stream()
                                .anyMatch(a -> a.augmentId().equals(source.id()))));
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
            if (source.reveal())
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
      if (sources.stream().noneMatch(RouteCards.Source::reveal)) continue;
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
            && entry.getValue().stream().anyMatch(RouteCards.Source::reveal))
          ids.add(entry.getKey());
      return ids;
    }
    var workbench = simulator.forItem(item);
    for (var entry : option.tiers().entrySet()) {
      if (entry.getValue().stream().noneMatch(RouteCards.Source::reveal)) continue;
      if (fullBoardTiers != null) {
        if (entry.getValue().stream()
            .anyMatch(
                source ->
                    source.reveal() && fullBoardTiers.get(source.id()).contains(entry.getKey())))
          ids.add(entry.getKey());
        continue;
      }
      var d = workbench.catalog().find(entry.getKey()).orElse(null);
      if (d == null || !canReserve(item, d.id(), protectedIds, defer)) continue;
      // 장착으로 활성화된 증강물 옵션은 현재 카탈로그에서 실제 추첨 가중치를 갖는다.
      var bones =
          DesecrationCurrency.sources(item.baseItemId(), item.itemLevel(), d, d.weight() > 0);
      if (entry.getValue().stream()
          .anyMatch(source -> source.reveal() && bones.contains(source.id()))) ids.add(d.id());
    }
    return ids;
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
        if (source.reveal())
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
  private void desecrationChoices(
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
      var slots = validator.slots(item);
      boolean prefixOpen = slots.remainingPrefixes() > 0;
      boolean suffixOpen = slots.remainingSuffixes() > 0;
      for (var entry : bones.entrySet()) {
        String id = entry.getKey();
        var tiers = Set.copyOf(entry.getValue());
        // 한쪽만 비어 있으면 뼈 자체가 이미 방향을 정한다.
        if (prefixOpen != suffixOpen
            || !RecipeTransitions.bothBoneSides(item, id, simulator.forItem(item).catalog())) {
          addBone(result, target, new Move(id), tiers);
          continue;
        }
        var bySide = new EnumMap<AffixType, Set<String>>(AffixType.class);
        for (var tier : tiers) {
          var side = simulator.catalog().find(tier).orElseThrow().affixType();
          bySide.computeIfAbsent(side, ignored -> new TreeSet<>()).add(tier);
        }
        // 두 방향 모두 목표 티어면 무징조도 유효하다. 단일 방향 목표는 해당 징조로 확정한다.
        if (replay || bySide.size() > 1) addBone(result, target, new Move(id), tiers);
        bySide.forEach(
            (side, matching) ->
                addBone(
                    result,
                    target,
                    new Move(id, Set.of(), null, DesecrationCurrency.directionOmen(side)),
                    Set.copyOf(matching)));
      }
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

  /** 무징조 또는 방향을 지정한 뼈를 동일한 작업대 전이로 검증한다. */
  private static void addBone(Set<Choice> result, int target, Move move, Set<String> tiers) {
    result.add(new Choice(target, move, true, null, Purpose.BONE, tiers));
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

  /** 특정 기폭제를 정하지 않고 상한 40에서 작업을 마쳤다는 제작지원 단계다. */
  static final String QUALITY_WORK = "QUALITY_40";

  /** 균열을 쓸 수 있는 베이스에 실제 최대 품질 접두가 있고 작업대가 그 상한을 검증한 경우다. */
  static boolean qualityCapReady(ItemState item, ItemCatalog catalog) {
    if (!BaseRegistry.maximumQualityBreach(item.baseItemId())
        || !item.conditions().isEmpty()
        || item.explicits().stream()
            .noneMatch(m -> m.modifierId().equals(QualityLimitRules.BREACH_ID))) return false;
    var limit = QualityLimitRules.describe(item, catalog);
    return limit != null && limit.maximumQuality() >= 40;
  }

  /** 품질 작업이 끝난 임시 상한을 소멸로 지우거나, 그 자리를 절사 카오스로 실제 추첨 가능한 목표로 교체한다. */
  List<Choice> qualityCleanup(
      ItemState item,
      RouteCards.Card card,
      Map<Integer, String> held,
      boolean firstChaosAvailable,
      boolean directCapRemoval) {
    String cap = QualityLimitRules.BREACH_ID;
    var workbench = simulator.forItem(item);
    if (!qualityCapReady(item, workbench.catalog()) || held.containsValue(cap)) return List.of();
    var slots = AbyssCraftingState.from(item, workbench.catalog());
    var candidates = slots.removalCandidates(null);
    var capSlot =
        candidates.stream()
            .filter(s -> s.modifier() != null && s.modifier().modifierId().equals(cap))
            .findFirst();
    if (capSlot.isEmpty()) return List.of();
    var side = capSlot.get().side();
    var result = new ArrayList<Choice>();
    result.add(
        new Choice(
            0,
            new Move(WorkbenchCurrency.ANNULMENT.name()),
            true,
            null,
            Purpose.QUALITY_CAP_REMOVAL,
            Set.of()));
    if (directCapRemoval || candidates.stream().anyMatch(s -> s.side() != side)) {
      var omen =
          side == AffixType.PREFIX
              ? WorkbenchOmen.SINISTRAL_ANNULMENT
              : WorkbenchOmen.DEXTRAL_ANNULMENT;
      result.add(
          new Choice(
              0,
              new Move(WorkbenchCurrency.ANNULMENT, omen),
              true,
              null,
              Purpose.QUALITY_CAP_REMOVAL,
              Set.of()));
    }
    // 미공개 훼손 슬롯은 절사 제거에 필요한 등장 레벨을 알 수 없다. 소멸에서는 실제 한 칸으로 센다.
    if (held.size() == card.options().size()
        || item.desecrated().stream().anyMatch(s -> s.modifierId() == null)) return result;
    var known = item.desecrated().isEmpty() ? item : slots.knownItem();
    var lowFallbacks = new HashMap<Integer, Boolean>();
    for (var omens : WhittlingRecovery.OMEN_COMBINATIONS) {
      if (workbench.chaosRemovalCandidates(known, WorkbenchCurrency.CHAOS, omens).stream()
          .noneMatch(m -> m.modifierId().equals(cap))) continue;
      var pool = new HashSet<String>();
      for (var branch : workbench.recipePlan(known, WorkbenchCurrency.CHAOS, omens).branches()) {
        if (branch.remaining().explicits().stream().anyMatch(m -> m.modifierId().equals(cap)))
          continue;
        for (var candidate : branch.candidates())
          if (candidate.weight() > 0) pool.add(candidate.id());
      }
      for (var option : card.options()) {
        if (held.containsKey(option.number())) continue;
        var eligible = new HashSet<String>();
        for (var tier : option.tiers().entrySet()) {
          if (!pool.contains(tier.getKey()) || !whittlingTier(tier.getValue(), item, true))
            continue;
          if (whittlingTier(tier.getValue(), item, false)
              || lowFallbacks.computeIfAbsent(
                  option.number(),
                  unused ->
                      !hasEarlierLowRoute(item, known, option, card, held, firstChaosAvailable)))
            eligible.add(tier.getKey());
        }
        if (!eligible.isEmpty())
          result.add(
              new Choice(
                  option.number(),
                  new Move(WorkbenchCurrency.CHAOS.name(), omens, null),
                  false,
                  null,
                  Purpose.QUALITY_CAP_REMOVAL,
                  Set.copyOf(eligible)));
      }
    }
    return result;
  }

  /** 같은 L 목표가 1옵션 카오스, 실제 완벽 엑잘 추첨 또는 아직 쓸 수 있는 훼손으로 가능하면 상한 절사를 미룬다. */
  private boolean hasEarlierLowRoute(
      ItemState item,
      ItemState known,
      RouteCards.Option goal,
      RouteCards.Card card,
      Map<Integer, String> held,
      boolean firstChaosAvailable) {
    var workbench = simulator.forItem(known);
    var drawTiers = new HashSet<String>();
    goal.tiers()
        .forEach(
            (id, sources) -> {
              if (whittlingTier(sources, item, true)) drawTiers.add(id);
            });
    if (firstChaosAvailable) {
      var empty =
          RecipeStateGroups.withAffixes(
              known,
              ItemState.Rarity.RARE,
              known.explicits().stream().filter(m -> m.fractured()).toList());
      if (workbench.pool(empty, WorkbenchCurrency.CHAOS, null).stream()
          .anyMatch(d -> d.weight() > 0 && drawTiers.contains(d.id()))) return true;
    }
    if (workbench.recipePlan(known, WorkbenchCurrency.PERFECT_EXALTED, Set.of()).branches().stream()
        .flatMap(branch -> branch.candidates().stream())
        .anyMatch(d -> d.weight() > 0 && drawTiers.contains(d.id()))) return true;
    // 이 시점에 남은 훼손 슬롯이 있으면 이미 공개한 슬롯이다. 미공개일 때는 절사 전체가 불가능하다.
    if (!item.desecrated().isEmpty()) return false;
    var bones = new HashMap<String, Set<String>>();
    goal.tiers()
        .forEach(
            (id, sources) -> {
              for (var source : sources)
                if (source.reveal())
                  bones.computeIfAbsent(source.id(), key -> new HashSet<>()).add(id);
            });
    if (bones.isEmpty()) return false;
    var protectedIds = new HashSet<String>();
    // 임시 상한을 지우며 여는 뼈는 이 경로에서 허용한 제거 방법이 아니다.
    protectedIds.add(QualityLimitRules.BREACH_ID);
    for (var option : card.options()) {
      var id = held.get(option.number());
      if (id != null && low(option.tiers().get(id))) protectedIds.add(id);
    }
    return bones.entrySet().stream()
        .anyMatch(
            bone ->
                !RecipeTransitions.reservableBoneTargets(
                        item,
                        bone.getKey(),
                        workbench.catalog(),
                        bone.getValue(),
                        protectedIds,
                        false)
                    .isEmpty());
  }

  /** 실제 베이스·상한·제작 한도에서 균열 준비, 접두 획득, 품질 작업 중 다음 단계만 제공한다. */
  List<Choice> qualityPreparation(
      ItemState item,
      RouteCards.Card card,
      Map<Integer, String> held,
      Set<String> protectedIds,
      WorkbenchCurrency catalyst) {
    if (!BaseRegistry.maximumQualityBreach(item.baseItemId()) || !item.conditions().isEmpty())
      return List.of();
    var workbench = simulator.forItem(item);
    if (qualityCapReady(item, workbench.catalog()))
      return List.of(
          new Choice(0, new Move(catalyst == null ? QUALITY_WORK : catalyst.name()), true, null));
    if (item.rarity() != ItemState.Rarity.RARE) return rarityPreparation(0, item);
    var result = new LinkedHashSet<Choice>();
    var crafted =
        item.explicits().stream()
            .filter(m -> AugmentCraftingRules.crafted(m, workbench.catalog()))
            .toList();
    if (crafted.size() >= AugmentCraftingRules.craftedLimit(item)) {
      // 두 제작 옵션이 함께 허용되는 기반이면 기존 목표를 유지하도록 한도를 먼저 높일 수 있다.
      install(result, 0, item, "Astrids_Creativity");
      for (var m : crafted) {
        if (m.fractured() || protectedIds.contains(m.modifierId())) continue;
        var side = workbench.catalog().find(m.modifierId()).orElseThrow().affixType();
        for (var move :
            List.of(
                new Move(WorkbenchCurrency.ANNULMENT.name()),
                new Move(
                    WorkbenchCurrency.ANNULMENT,
                    side == AffixType.PREFIX
                        ? WorkbenchOmen.SINISTRAL_ANNULMENT
                        : WorkbenchOmen.DEXTRAL_ANNULMENT)))
          result.add(
              new Choice(0, move, true, null, Purpose.QUALITY_CLEAR, Set.of(m.modifierId())));
      }
    } else {
      int capTarget =
          card.options().stream()
              .filter(o -> o.tiers().containsKey(QualityLimitRules.BREACH_ID))
              .mapToInt(RouteCards.Option::number)
              .findFirst()
              .orElse(0);
      replacement(
          result,
          item,
          capTarget,
          WorkbenchCurrency.ESSENCE_BREACH,
          AffixType.PREFIX,
          validator.slots(item),
          held.values(),
          protectedIds);
    }
    return List.copyOf(result);
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
    // 제거할 수 있는 속성이 없으면 새로 붙인 한 칸만 에센스가 교체한다.
    if (mutableSides.isEmpty() && item.desecrated().isEmpty() && roomForTarget) {
      result.add(
          new Choice(
              target, new Move(WorkbenchCurrency.EXALTED.name()), true, new Move(currency.name())));
      return;
    }
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
