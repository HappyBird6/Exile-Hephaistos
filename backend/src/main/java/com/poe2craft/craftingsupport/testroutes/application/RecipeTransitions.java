package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 작업대의 상태·제거·추첨 규칙으로 한 단계의 실제 결과를 열거한다. simulator는 시작 베이스의 검증된 작업대이며 successors는 매 단계의 소켓 효과를 반영한다.
 * 결과 수 제한으로 열거하지 못한 질량을 성공 후보에 재배분하지 않는다.
 */
public final class RecipeTransitions {
  private final WorkbenchSimulator simulator;
  private final boolean presenceOnly;

  /** 활성 목표가 실제 수치로 평가하는 원본 stat ID다. 의존성을 모르면 기존처럼 모든 수치를 열거한다. */
  private final Set<String> numericSourceStatIds;

  /** simulator에 설정된 수치·교체 정책을 공유한다. */
  public RecipeTransitions(WorkbenchSimulator simulator) {
    this(simulator, false);
  }

  /** presenceOnly가 참이면 목표·다음 행동이 수치와 무관한 굴림을 합산한다. */
  public RecipeTransitions(WorkbenchSimulator simulator, boolean presenceOnly) {
    this(simulator, presenceOnly, null);
  }

  /** 수치 목표·제작 자격에 쓰이지 않는 옵션은 동일한 획득 결과로 합친다. null이면 수치 목표의 전체 분포를 유지한다. */
  public RecipeTransitions(
      WorkbenchSimulator simulator, boolean presenceOnly, Set<String> numericSourceStatIds) {
    this.simulator = Objects.requireNonNull(simulator);
    this.presenceOnly = presenceOnly;
    this.numericSourceStatIds =
        numericSourceStatIds == null ? null : Set.copyOf(numericSourceStatIds);
  }

  /** 같은 미공개 슬롯과 목표 번호에 속하는 허용 티어 집합이다. */
  record RevealGroup(ItemState item, Set<String> tiers) {}

  /** 실제 티어를 조건으로 뼈 분기와 수치 굴림에 해당하는 질량이다. 티어 간 비율은 부여하지 않는다. */
  record RevealRoll(RevealGroup group, String tier, Fraction conditional) {}

  /**
   * state는 실제 결과, probability는 확정한 분기 질량, removedModifierIds는 제거 이력이다. 티어 분포를 모르면 reveal에 조건부 질량을
   * 보관한다.
   */
  public record Outcome(
      ItemState state, Fraction probability, Set<String> removedModifierIds, RevealRoll reveal) {
    public Outcome(ItemState state, Fraction probability, Set<String> removedModifierIds) {
      this(state, probability, removedModifierIds, null);
    }
  }

  /** outcomes는 열거된 분기, complete는 질량을 모두 확인했는지, removableModifierIds는 제한 전의 모든 제거 가능 대상이다. */
  public record Law(List<Outcome> outcomes, boolean complete, Set<String> removableModifierIds) {
    private static Law empty(boolean complete) {
      return new Law(List.of(), complete, Set.of());
    }

    private static Law deterministic(ItemState next) {
      return new Law(List.of(new Outcome(next, Fraction.ONE, Set.of())), true, Set.of());
    }
  }

  /** 제거 직후의 후보·추첨 분모와 수치를 적용하는 작업대 상태 복원 함수를 공유한다. */
  private record Draw(
      List<ModifierDefinition> options,
      boolean uniform,
      Fraction probability,
      Set<String> removed,
      Function<ModifierInstance, ItemState> finish,
      RevealGroup revealGroup) {
    Draw(
        List<ModifierDefinition> options,
        boolean uniform,
        Fraction probability,
        Set<String> removed,
        Function<ModifierInstance, ItemState> finish) {
      this(options, uniform, probability, removed, finish, null);
    }
  }

  /**
   * action은 Workbench 화폐 key, 증강물·훼손 재료 ID 또는 기존 슬롯의 REVEAL이다. targetModifierIds는 결과 열거 순서와 조건부 공개
   * 목표만 정한다. 기본 추첨 분모는 작업대의 전체 후보 풀이다.
   */
  public Law successors(
      ItemState item,
      String action,
      Set<WorkbenchOmen> omens,
      Set<String> targetModifierIds,
      int remainingOutcomes) {
    return successors(item, action, omens, targetModifierIds, remainingOutcomes, true);
  }

  /** includeMisses가 false이면 목표 외 결과의 수치 열거를 생략하되 가중치와 제거 분모는 유지한다. */
  public Law successors(
      ItemState item,
      String action,
      Set<WorkbenchOmen> omens,
      Set<String> targetModifierIds,
      int remainingOutcomes,
      boolean includeMisses) {
    if (remainingOutcomes < 1) return Law.empty(false);
    var workbench = simulator.forItem(item);
    var catalog = workbench.catalog();
    WorkbenchCurrency currency;
    try {
      currency = WorkbenchCurrency.valueOf(action);
    } catch (IllegalArgumentException material) {
      int separator = action.lastIndexOf('@');
      String augment = separator < 0 ? action : action.substring(0, separator);
      if (SocketAugments.supports(item.baseItemId(), augment)) {
        if (!omens.isEmpty()) return Law.empty(true);
        if (separator < 0) return install(item, augment);
        try {
          return installAt(item, augment, Integer.parseInt(action.substring(separator + 1)));
        } catch (NumberFormatException invalidSlot) {
          return Law.empty(true);
        }
      }
      return reveal(item, action, omens, targetModifierIds, workbench, remainingOutcomes);
    }
    var abyss = item.desecrated().isEmpty() ? null : AbyssCraftingState.from(item, catalog);
    var known = abyss == null ? item : abyss.knownItem();
    if (omens.contains(WorkbenchOmen.LIGHT)) return Law.empty(true);
    boolean hidden = item.desecrated().stream().anyMatch(s -> s.modifierId() == null);
    if (currency == WorkbenchCurrency.ARTIFICER
        || currency.catalystType() != null
        || QualityCurrencyRules.isQuality(currency)
        || QualityInfuserRules.supports(currency)) {
      if (!omens.isEmpty()) return Law.empty(false);
      var used =
          workbench.apply(
              known,
              currency,
              Set.of(),
              () -> {
                throw new IllegalStateException("Unexpected random preparation");
              });
      if (!used.applied()) return Law.empty(true);
      return Law.deterministic(
          abyss == null
              ? used.state()
              : abyss.withKnownItem(used.state(), Set.of(), catalog).storedItem());
    }

    if (currency == WorkbenchCurrency.DIVINE) {
      if (!omens.isEmpty()) return Law.empty(false);
      if (!workbench.availability(known, currency, omens).available()) return Law.empty(true);
      var results = new ArrayList<Outcome>();
      boolean complete =
          reroll(
              workbench,
              known,
              abyss,
              0,
              new ArrayList<>(known.explicits()),
              Fraction.ONE,
              remainingOutcomes,
              results);
      return new Law(List.copyOf(results), complete, Set.of());
    }

    boolean crafted = currency.materialPolicy().mode() == WorkbenchDefinitions.MaterialMode.REPLACE;
    var draws = new ArrayList<Draw>();
    if (hidden) {
      var plan = workbench.abyssPlan(abyss, currency, omenIds(omens));
      if (!plan.available()) {
        boolean unknown =
            plan.reason().contains("UNSUPPORTED")
                || plan.reason().equals("UNREVEALED_MODIFIER_LEVEL_UNKNOWN");
        return Law.empty(!unknown);
      }
      for (var branch : plan.branches()) {
        var remaining = branch.remaining();
        String id = branch.removedSlotId() == null ? freeSlot(remaining) : branch.removedSlotId();
        var removed = removed(known, remaining.knownItem());
        draws.add(
            new Draw(
                branch.candidates(),
                branch.uniformSelection(),
                branchMass(branch.probabilityDenominator(), plan.branches().size()),
                removed,
                m ->
                    m == null
                        ? remaining.storedItem()
                        : remaining.append(id, m, catalog).storedItem()));
      }
    } else {
      var plan = workbench.recipePlan(known, currency, omens);
      if (plan.status() != AutomaticCraftingPlan.Status.READY)
        return Law.empty(
            plan.status() == AutomaticCraftingPlan.Status.UNAVAILABLE
                || (crafted && noEligibleEssenceRemoval(known, currency, omens, workbench)));
      for (var branch : plan.branches()) {
        var remaining = branch.remaining();
        var removed = removed(known, remaining);
        draws.add(
            new Draw(
                branch.candidates(),
                plan.uniformCandidates(),
                branchMass(branch.probabilityDenominator(), plan.branches().size()),
                removed,
                m -> {
                  var next = remaining;
                  if (m != null) {
                    var mods = new ArrayList<>(remaining.explicits());
                    mods.add(m);
                    next = workbench.finishBasicCurrency(remaining, mods);
                  }
                  return abyss == null
                      ? next
                      : abyss.withKnownItem(next, removed, catalog).storedItem();
                }));
      }
    }
    return enumerate(
        workbench, draws, crafted, targetModifierIds, remainingOutcomes, true, includeMisses);
  }

  /** 교체형 에센스의 결정화 방향에 제거할 옵션이 없거나, 그 제거로는 모든 적격 결과의 자리가 막혀 있으면 확정 실패다. */
  private static boolean noEligibleEssenceRemoval(
      ItemState item,
      WorkbenchCurrency currency,
      Set<WorkbenchOmen> omens,
      WorkbenchSimulator workbench) {
    if (currency.materialPolicy().kind() != WorkbenchDefinitions.MaterialKind.ESSENCE
        || omens.size() != 1) return false;
    var omen = omens.iterator().next();
    if (omen != WorkbenchOmen.SINISTRAL_CRYSTALLISATION
        && omen != WorkbenchOmen.DEXTRAL_CRYSTALLISATION) return false;
    var side = omen.affix();
    var catalog = workbench.catalog();
    if (AbyssCraftingState.from(item, catalog).removalCandidates(side).isEmpty()) return true;
    var slots = new ItemStateValidator(catalog).slots(item);
    boolean otherSideFull =
        side == ModifierDefinition.AffixType.PREFIX
            ? slots.remainingSuffixes() == 0
            : slots.remainingPrefixes() == 0;
    if (!otherSideFull) return false;
    var targets =
        workbench.sourceMaterialTargets(item.itemLevel()).getOrDefault(currency, List.of());
    return !targets.isEmpty()
        && targets.stream().allMatch(id -> catalog.find(id).orElseThrow().affixType() != side);
  }

  /** 실제 설치 검증을 거친 첫 빈 홈만 사용한다. 이미 설치된 증강물을 덮어쓰지 않는다. */
  private static Law install(ItemState item, String material) {
    if (item.augmentSockets() == null) return Law.empty(true);
    var occupied =
        item.installedAugments().stream()
            .map(ItemState.InstalledAugment::slotIndex)
            .collect(Collectors.toSet());
    for (int slot = 0; slot < item.augmentSockets(); slot++) {
      if (occupied.contains(slot)) continue;
      return installAt(item, material, slot);
    }
    return Law.empty(true);
  }

  /** 지정한 홈에만 장착한다. 같은 재료의 재설치나 잠긴 기존 증강물은 후보가 되지 않는다. */
  private static Law installAt(ItemState item, String material, int slot) {
    if (item.augmentSockets() == null || slot < 0 || slot >= item.augmentSockets())
      return Law.empty(true);
    if (item.installedAugments().stream()
        .anyMatch(a -> a.slotIndex() == slot && a.augmentId().equals(material)))
      return Law.empty(true);
    try {
      return Law.deterministic(SocketAugments.install(item, slot, material));
    } catch (IllegalArgumentException invalid) {
      return Law.empty(true);
    }
  }

  /** 다섯 명시 옵션을 유지하며 마지막 빈 자리에 뼈만 적용한다. 공개의 목표 선택은 다음 전이에서 검증한다. */
  public Law prepareDesecration(
      ItemState item,
      String material,
      Set<WorkbenchOmen> omens,
      Set<String> targetIds,
      int remainingOutcomes) {
    if (remainingOutcomes < 1) return Law.empty(false);
    if (item.rarity() != ItemState.Rarity.RARE
        || !item.desecrated().isEmpty()
        || item.explicits().size() != 5) return Law.empty(true);
    var catalog = simulator.forItem(item).catalog();
    var slots = new ItemStateValidator(catalog).slots(item);
    if (slots.remainingPrefixes() + slots.remainingSuffixes() != 1) return Law.empty(true);
    for (var prepared : boneStates(item, material, omens, catalog)) {
      var state = prepared.state();
      var last = state.slots().getLast();
      if (!Collections.disjoint(state.eligibleRevealIds(last.id(), catalog), targetIds))
        return Law.deterministic(state.storedItem());
    }
    return Law.empty(true);
  }

  /** 제거·방향을 이미 결정한 실제 미공개 상태와 해당 작업대 분기 확률이다. */
  private record BoneState(AbyssCraftingState state, Fraction probability) {}

  /** 재료 적용으로 열 수 있는 미공개 상태를 작업대에서 검사한다. 가득 찬 경우에는 제거할 슬롯별로 검사한다. */
  private static List<BoneState> boneStates(
      ItemState item, String material, Set<WorkbenchOmen> omens, ItemCatalog catalog) {
    var removalsWithSides = new ArrayList<List<AbyssCraftingState>>();
    if (item.desecrated().isEmpty()) {
      var slots = new ItemStateValidator(catalog).slots(item);
      var before = AbyssCraftingState.from(item, catalog);
      boolean full = before.slots().size() == slots.maxPrefixes() + slots.maxSuffixes();
      var removals =
          full
              ? before.removalCandidates(null).stream().map(AbyssCraftingState.Slot::id).toList()
              : List.of("");
      for (String removed : removals) {
        var sides = new ArrayList<AbyssCraftingState>();
        for (var side :
            List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX)) {
          try {
            var rolled =
                DesecrationCurrency.apply(item, material, side, removed, omenIds(omens), catalog);
            // 가상 분기끼리 같은 상태를 비교할 수 있게 작업대의 임의 신규 슬롯 ID만 정규화한다.
            var slotsWithStableIds =
                rolled.slots().stream()
                    .map(
                        s ->
                            s.desecrated()
                                ? new AbyssCraftingState.Slot(
                                    "recipe-desecrated",
                                    s.side(),
                                    s.modifier(),
                                    true,
                                    s.minimumModifierLevel(),
                                    s.revealPool())
                                : s)
                    .toList();
            sides.add(new AbyssCraftingState(rolled.item(), slotsWithStableIds, rolled.offer()));
          } catch (IllegalArgumentException unavailable) {
            // 이 제거·방향 조합에서만 베이스·분열·레벨·방향·슬롯 자격이 맞지 않았다.
          }
        }
        if (!sides.isEmpty()) removalsWithSides.add(sides);
      }
    }
    var prepared = new ArrayList<BoneState>();
    for (var sides : removalsWithSides) {
      var mass = Fraction.of(1, (long) removalsWithSides.size() * sides.size());
      for (var state : sides) prepared.add(new BoneState(state, mass));
    }
    return prepared;
  }

  /** 현재 조건에서 제거 가능한 슬롯과 방향을 분리해 훼손 재료를 적용하고, 목표가 공개되었을 때의 선택을 열거한다. */
  private Law reveal(
      ItemState item,
      String material,
      Set<WorkbenchOmen> omens,
      Set<String> targets,
      WorkbenchSimulator workbench,
      int limit) {
    var catalog = workbench.catalog();
    var before = AbyssCraftingState.from(item, catalog);
    boolean existing = "REVEAL".equals(material);
    if (omens.contains(WorkbenchOmen.LIGHT) || (existing && !omens.isEmpty()))
      return Law.empty(true);
    var prepared =
        existing
            ? List.of(new BoneState(before, Fraction.ONE))
            : boneStates(item, material, omens, catalog);
    var draws = new ArrayList<Draw>();
    var removable = new HashSet<String>();
    for (var bone : prepared) {
      var state = bone.state();
      var removed = removed(before.knownItem(), state.knownItem());
      removable.addAll(removed);
      // 수동 공개에서 같은 티어를 여러 슬롯에서 고를 수 있어도 한 번만 획득한다.
      var slotForTier = new TreeMap<String, String>();
      for (var slot : state.slots()) {
        if (!slot.desecrated() || slot.modifier() != null) continue;
        for (var id : state.eligibleRevealIds(slot.id(), catalog))
          if (targets.contains(id)) slotForTier.putIfAbsent(id, slot.id());
      }
      var group =
          slotForTier.size() <= 1
              ? null
              : new RevealGroup(state.storedItem(), Set.copyOf(slotForTier.keySet()));
      slotForTier.forEach(
          (id, slot) ->
              draws.add(
                  new Draw(
                      List.of(catalog.find(id).orElseThrow()),
                      true,
                      bone.probability(),
                      removed,
                      m -> state.selectDirect(slot, m, catalog).storedItem(),
                      group)));
    }
    var law = enumerate(workbench, draws, false, targets, limit, true, false);
    return new Law(law.outcomes(), law.complete(), Set.copyOf(removable));
  }

  /** 원래 분모로 모든 후보의 수치 질량을 계산한다. 예산은 목표 수치를 먼저 열거하는 순서에만 쓰인다. */
  private Law enumerate(
      WorkbenchSimulator workbench,
      List<Draw> draws,
      boolean crafted,
      Set<String> targets,
      int limit,
      boolean complete,
      boolean includeMisses) {
    var outcomes = new ArrayList<Outcome>();
    var removable =
        draws.stream().flatMap(d -> d.removed().stream()).collect(Collectors.toUnmodifiableSet());
    for (boolean priority : includeMisses ? List.of(true, false) : List.of(true)) {
      for (var draw : draws) {
        if (draw.options().isEmpty()) {
          if (priority) {
            if (outcomes.size() >= limit) return new Law(List.copyOf(outcomes), false, removable);
            outcomes.add(
                new Outcome(draw.finish().apply(null), draw.probability(), draw.removed()));
          }
          continue;
        }
        long total =
            draw.uniform()
                ? draw.options().size()
                : draw.options().stream().mapToLong(ModifierDefinition::weight).sum();
        if (total == 0) {
          complete = false;
          continue;
        }
        for (var candidate : draw.options()) {
          if (targets.contains(candidate.id()) != priority) continue;
          if (outcomes.size() >= limit) return new Law(List.copyOf(outcomes), false, removable);
          var baseMass =
              draw.probability() == null
                  ? null
                  : draw.probability()
                      .multiply(Fraction.of(draw.uniform() ? 1 : candidate.weight(), total));
          if (!rolls(
              workbench,
              candidate,
              (roll, probability) -> {
                if (outcomes.size() >= limit) return false;
                var value = crafted ? roll.asCrafted() : roll;
                var mass = baseMass == null ? null : baseMass.multiply(probability);
                var choice =
                    draw.revealGroup() == null
                        ? null
                        : new RevealRoll(draw.revealGroup(), candidate.id(), mass);
                outcomes.add(
                    new Outcome(
                        draw.finish().apply(value),
                        choice == null ? mass : null,
                        draw.removed(),
                        choice));
                return true;
              })) complete = false;
        }
      }
    }
    return new Law(List.copyOf(outcomes), complete, removable);
  }

  /** 일반 Divine의 모든 변경 가능 명시 수치를 함께 굴린다. unchanged·분열 옵션은 원래 인스턴스를 보존한다. */
  private boolean reroll(
      WorkbenchSimulator workbench,
      ItemState item,
      AbyssCraftingState abyss,
      int index,
      List<ModifierInstance> mods,
      Fraction mass,
      int limit,
      List<Outcome> results) {
    if (results.size() >= limit) return false;
    if (index == mods.size()) {
      var next = workbench.finishBasicCurrency(item, mods);
      results.add(
          new Outcome(
              abyss == null
                  ? next
                  : abyss.withKnownItem(next, Set.of(), workbench.catalog()).storedItem(),
              mass,
              Set.of()));
      return true;
    }
    var old = mods.get(index);
    var definition = workbench.catalog().find(old.modifierId()).orElseThrow();
    if (old.fractured() || definition.stats().stream().noneMatch(r -> r.max() > r.min()))
      return reroll(workbench, item, abyss, index + 1, mods, mass, limit, results);
    return rolls(
        workbench,
        definition,
        (rolled, p) -> {
          mods.set(index, old.crafted() ? rolled.asCrafted() : rolled);
          boolean more =
              reroll(workbench, item, abyss, index + 1, mods, mass.multiply(p), limit, results);
          mods.set(index, old);
          return more;
        });
  }

  /** 단일 수치는 균등 정수, 복합 수치는 작업대에서 검토한 동일 결합 분포를 소비한다. false는 중단·미지원이다. */
  private boolean rolls(
      WorkbenchSimulator workbench,
      ModifierDefinition definition,
      BiPredicate<ModifierInstance, Fraction> accept) {
    boolean ignoresValues =
        presenceOnly
            || (numericSourceStatIds != null
                && definition.stats().stream().noneMatch(s -> numericSourceStatIds.contains(s.id()))
                && !ModifierMagnitudes.provider(definition));
    if (definition.stats().size() == 1) {
      var stat = definition.stats().getFirst();
      if (ignoresValues) {
        var minimum = new ModifierInstance(definition.id(), Map.of(stat.id(), stat.min()));
        if (scalableRoll(minimum, workbench)) return accept.test(minimum, Fraction.ONE);
      }
      var mass = Fraction.of(1, Math.addExact(Math.subtractExact(stat.max(), stat.min()), 1));
      for (long value = stat.min(); ; value++) {
        if (!accept.test(new ModifierInstance(definition.id(), Map.of(stat.id(), value)), mass))
          return false;
        if (value == stat.max()) return true;
      }
    }
    var cells = workbench.automaticJointRolls(definition);
    if (cells.isEmpty() || cells.get().isEmpty()) return false;
    if (ignoresValues) {
      // 결합 굴림은 검토된 분포에서 실제 가능한 한 셀을 예시로 남긴다.
      var witness = cells.get().getFirst().modifier();
      if (scalableRoll(witness, workbench)) return accept.test(witness, Fraction.ONE);
    }
    for (var cell : cells.get())
      if (!accept.test(
          cell.modifier(), Fraction.of(cell.ticks(), CoupledStatRollModel.RESOLUTION + 1)))
        return false;
    return true;
  }

  /** 품질 한도나 허용 슬롯 수 등 제작 자격에 쓰이는 수치가 하나라도 있으면 전체 굴림을 검증한다. */
  private static boolean scalableRoll(ModifierInstance witness, WorkbenchSimulator workbench) {
    return QualitySanctification.targets(witness, workbench.catalog())
        .containsAll(witness.values().keySet());
  }

  /** 유한 분기가 제공한 정확한 분모를 사용한다. 0인 일반 분기는 모두 같은 확률이다. */
  private static Fraction branchMass(long denominator, int count) {
    return Fraction.of(1, denominator == 0 ? count : denominator);
  }

  /** 제거 직후 상태와 비교해 같은 옵션을 즉시 다시 획득한 경우에도 제거를 기록한다. */
  private static Set<String> removed(ItemState before, ItemState remaining) {
    var lost = new ArrayList<>(before.explicits());
    remaining.explicits().forEach(lost::remove);
    return lost.stream().map(ModifierInstance::modifierId).collect(Collectors.toUnmodifiableSet());
  }

  /** 기존 슬롯 식별자와 충돌하지 않는 새 슬롯 ID를 선택한다. */
  private static String freeSlot(AbyssCraftingState state) {
    var ids = state.slots().stream().map(AbyssCraftingState.Slot::id).collect(Collectors.toSet());
    int n = 0;
    while (ids.contains("added-" + n)) n++;
    return "added-" + n;
  }

  /** Workbench가 사용한 징조 식별자로 변환한다. */
  private static Set<String> omenIds(Set<WorkbenchOmen> omens) {
    return omens.stream().map(WorkbenchOmen::id).collect(Collectors.toSet());
  }
}
