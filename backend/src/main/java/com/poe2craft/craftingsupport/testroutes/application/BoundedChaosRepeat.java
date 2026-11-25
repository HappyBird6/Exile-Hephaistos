package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.QualityCapChangePolicy;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/** 같은 베이스에 지정된 카오스를 반복할 때 첫 목표 옵션 획득·제거의 실제 상태와 소모량을 계산한다. */
final class BoundedChaosRepeat {
  /** hits는 제한 이내의 첫 변화, firstHits는 첫 사용에서의 변화, expectedUses는 실패를 포함한 평균 사용량, work는 사용한 탐색 예산이다. */
  record Result(
      RecipeTransitions.Law hits,
      RecipeTransitions.Law firstHits,
      Fraction expectedUses,
      int work) {}

  /** state와 제거된 목표 ID가 같으면 도달 시도 횟수나 중간의 비목표 옵션에 관계없이 질량을 합산한다. */
  private record Hit(ItemState state, Set<String> removedTargets) {}

  /** 한 번 시도에서의 목표 변화·재시도 상태와 완전성을 보관한다. unavailable이면 시도 비용도 발생하지 않는다. */
  private record Attempt(
      Map<Hit, Fraction> hits,
      Map<ItemState, Fraction> misses,
      boolean complete,
      boolean unavailable) {}

  /** 각 상태의 소켓 효과가 반영된 실제 카오스 계획을 조회한다. */
  private final WorkbenchSimulator simulator;

  /** 수치 열거 방식은 이 경로의 다른 단계와 공유한다. */
  private final RecipeTransitions transitions;

  /** 각 시도의 카오스 등급과 징조는 시작부터 고정된다. */
  private final WorkbenchCurrency action;

  private final Set<WorkbenchOmen> omens;

  /** 모든 목표 티어 중, 이번 행동에서 처음 얻거나 잃은 티어를 판별한다. */
  private final Set<String> targets;

  /** 이 단계에서 제거되는 즉시 실패하는 원래 옵션들의 ID다. */
  private final Set<String> protectedIds;

  /** 반복 시작 시 이미 보유한 목표 티어들이다. */
  private final Set<String> held;

  /** 이 계산 안에서만 각 실제 상태의 1회 전이를 재사용한다. */
  private final Map<ItemState, Attempt> attempts = new HashMap<>();

  /** 어느 시도에서든 제거 가능한 옵션을 수집해 안전한 대안 비교에 사용한다. */
  private final Set<String> removable = new HashSet<>();

  /** 호출자의 남은 계산 예산이며 work는 이 반복에서 이미 쓴 전이 처리 수다. */
  private final int budget;

  private int work;

  /** 실제 시작 아이템에 이미 붙은 목표 티어는 제거되지 않는 한 새 획득으로 세지 않는다. */
  private BoundedChaosRepeat(
      WorkbenchSimulator simulator,
      RecipeTransitions transitions,
      ItemState start,
      WorkbenchCurrency action,
      Set<WorkbenchOmen> omens,
      Set<String> targets,
      Set<String> protectedIds,
      int budget,
      boolean ignoreStartingDummy) {
    this.simulator = simulator;
    this.transitions = transitions;
    this.action = action;
    this.omens = omens;
    this.targets = targets;
    this.protectedIds = protectedIds;
    this.budget = budget;
    held = new HashSet<>();
    for (var modifier : start.explicits()) {
      if ((!ignoreStartingDummy || modifier.fractured()) && targets.contains(modifier.modifierId()))
        held.add(modifier.modifierId());
    }
  }

  /** maxUses가 없으면 무징조 카오스를 목표까지 반복한다. 저장한 유한 반복은 기존 작업대 한도를 따른다. */
  static Result calculate(
      WorkbenchSimulator simulator,
      RecipeTransitions transitions,
      ItemState start,
      WorkbenchCurrency action,
      Set<WorkbenchOmen> omens,
      Set<String> targetIds,
      Set<String> protectedIds,
      Integer maxUses,
      int budget) {
    return calculate(
        simulator,
        transitions,
        start,
        action,
        omens,
        targetIds,
        protectedIds,
        maxUses,
        budget,
        false);
  }

  /** 다음 카오스에서 갈아낼 임시 옵션은 시작부터 보유한 목표에서 뺀다. 그 티어를 다시 뽑으면 실제 신규 획득이다. */
  static Result calculate(
      WorkbenchSimulator simulator,
      RecipeTransitions transitions,
      ItemState start,
      WorkbenchCurrency action,
      Set<WorkbenchOmen> omens,
      Set<String> targetIds,
      Set<String> protectedIds,
      Integer maxUses,
      int budget,
      boolean ignoreStartingDummy) {
    if (action.baseAction() != CraftingAction.CHAOS
        || (maxUses == null
            ? !omens.isEmpty()
            : maxUses < 1 || maxUses > PublicContracts.REPEAT.maximumUses())
        || budget < 0) throw new IllegalArgumentException("Invalid bounded Chaos step");
    return new BoundedChaosRepeat(
            simulator,
            transitions,
            start,
            action,
            omens,
            targetIds,
            protectedIds,
            budget,
            ignoreStartingDummy)
        .run(start, maxUses);
  }

  /**
   * 비용 표시는 명시 옵션이 없는 동일 베이스의 희귀 아이템에서 대상 티어들의 추첨 확률로 추정한다. 화폐 등급과 장착 효과는 작업대 풀을 따르고, 카오스용 제거 징조는 새로
   * 붙는 티어의 분모에 관여하지 않는다. 실제 경로 전이는 원래 아이템에서 별도로 검증한다.
   */
  static Double emptyBaseExpectedUses(
      WorkbenchSimulator simulator,
      ItemState item,
      WorkbenchCurrency action,
      Set<String> targetIds,
      Integer maxUses) {
    if (action.baseAction() != CraftingAction.CHAOS
        || (maxUses != null && (maxUses < 1 || maxUses > PublicContracts.REPEAT.maximumUses())))
      throw new IllegalArgumentException("Invalid bounded Chaos estimate");
    if (targetIds.isEmpty()) return null;
    var base = item.desecrated().isEmpty() ? item : item.withDesecrated(List.of());
    var empty = RecipeStateGroups.withAffixes(base, ItemState.Rarity.RARE, List.of());
    var pool = simulator.forItem(empty).pool(empty, action, null);
    long totalWeight = 0;
    long targetWeight = 0;
    for (var candidate : pool) {
      totalWeight += candidate.weight();
      if (targetIds.contains(candidate.id())) targetWeight += candidate.weight();
    }
    if (totalWeight == 0) return null;
    if (maxUses == null) return targetWeight == 0 ? null : (double) totalWeight / targetWeight;
    double p = (double) targetWeight / totalWeight;
    if (p == 0) return (double) maxUses;
    return Math.min(maxUses, -Math.expm1(maxUses * Math.log1p(-p)) / p);
  }

  /** 첫 시도 이후 실제 미적중 상태만 전파한다. 사용 불가·보호 실패·제한 도달 질량도 초기 분모에 남는다. */
  private Result run(ItemState start, Integer maxUses) {
    var samePool = samePoolWithoutMissRolls(start, maxUses);
    if (samePool != null) return samePool;
    var first = attempt(start);
    var firstHits = law(first.hits(), first.complete());
    if (!first.complete()) return unknown(firstHits, first.hits());
    if (first.unavailable() || Integer.valueOf(1).equals(maxUses) || first.misses().isEmpty())
      return new Result(
          firstHits, firstHits, first.unavailable() ? Fraction.ZERO : Fraction.ONE, work);
    var renewal = verifiedRetryMass(start, first);
    if (renewal != null) return renewalResult(first.hits(), firstHits, renewal, maxUses);
    var hits = new LinkedHashMap<>(first.hits());
    var frontier = first.misses();
    var uses = Fraction.ONE;
    for (int used = 1; (maxUses == null || used < maxUses) && !frontier.isEmpty(); used++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var branch : frontier.entrySet()) {
        var current = attempt(branch.getKey());
        if (current.unavailable()) continue;
        // 캐시에서 꺼낸 상태도 매 차수별 질량 합산 작업에 예산을 사용한다.
        int edges = Math.max(1, current.hits().size() + current.misses().size());
        if (!current.complete() || edges > budget - work) {
          current.hits().keySet().forEach(hit -> hits.putIfAbsent(hit, null));
          return unknown(firstHits, hits);
        }
        work += edges;
        var mass = branch.getValue();
        uses = uses.add(mass);
        accumulate(hits, current.hits(), mass);
        accumulate(next, current.misses(), mass);
      }
      frontier = next;
    }
    return new Result(law(hits, true), firstHits, uses, work);
  }

  /**
   * 무징조 카오스가 유일한 공개 비분열 옵션을 제거하면 매번 동일한 분열·소켓 상태에서 새 티어를 뽑는다. 종료 목표의 숫자 분포는 실제 작업대 법칙으로 검증하고, 다음에
   * 전부 제거될 비목표의 숫자는 적분한다. 제거 대상·자격이 변할 수 있는 주얼이나 훼손, 징조에는 적용하지 않는다.
   */
  private Result samePoolWithoutMissRolls(ItemState start, Integer maxUses) {
    if (work >= budget
        || !omens.isEmpty()
        || !start.desecrated().isEmpty()
        || BasicJewel.supportedCrafting(start.baseItemId())
        || !OneOptionPreparation.ready(start)) return null;
    var mutable = start.explicits().stream().filter(m -> !m.fractured()).findFirst().orElseThrow();
    if (held.contains(mutable.modifierId())
        || !start.explicits().stream()
            .filter(ModifierInstance::fractured)
            .map(ModifierInstance::modifierId)
            .toList()
            .containsAll(protectedIds)) return null;
    var workbench = simulator.forItem(start);
    // 품질 상한 옵션의 제거를 막는 작업대에서는 미적중한 옵션에 따라 다음 사용 가능 여부가 달라진다.
    if (start.catalystQuality() != null
        && workbench.automaticIdentity().qualityPolicy() == QualityCapChangePolicy.REJECT_OVERCAP)
      return null;
    var plan = workbench.recipePlan(start, action, omens);
    if (plan.status() != AutomaticCraftingPlan.Status.READY || plan.branches().size() != 1)
      return null;
    var redraw = plan.branches().getFirst();
    if (redraw.candidates().isEmpty()
        || redraw.remaining().explicits().stream().anyMatch(m -> !m.fractured())) return null;
    var terminal =
        transitions.successors(start, action.name(), omens, targets, budget - work, false);
    work += Math.min(budget - work, Math.max(1, terminal.outcomes().size()));
    removable.addAll(terminal.removableModifierIds());
    // 한 번에 정확히 하나만 새로 붙는다. 종료 이후 후속 행동은 적중한 실제 상태의 분포를 받는다.
    var hits = new LinkedHashMap<Hit, Fraction>();
    boolean complete =
        terminal.complete() && terminal.outcomes().stream().noneMatch(o -> o.probability() == null);
    for (var outcome : terminal.outcomes()) {
      var hit = new Hit(outcome.state(), Set.of());
      if (outcome.probability() == null) hits.putIfAbsent(hit, null);
      else hits.merge(hit, outcome.probability(), Fraction::add);
    }
    var firstHits = law(hits, complete);
    if (maxUses == null || maxUses > 1)
      for (var candidate : redraw.candidates())
        if (!targets.contains(candidate.id())) removable.add(candidate.id());
    if (!complete) return unknown(firstHits, hits);
    var chance = hits.values().stream().reduce(Fraction.ZERO, Fraction::add);
    return renewalResult(hits, firstHits, chance.complement(), maxUses);
  }

  /** 재시도 상태의 분포가 같으면 등비합으로 각 최초 적중의 질량과 중단까지의 평균 사용량을 구한다. */
  private Result renewalResult(
      Map<Hit, Fraction> hits, RecipeTransitions.Law first, Fraction retry, Integer maxUses) {
    if (maxUses == null && retry.equals(Fraction.ONE))
      return new Result(law(Map.of(), true), first, null, work);
    var mean =
        maxUses == null
            ? Fraction.ONE.divide(retry.complement())
            : retry.equals(Fraction.ONE)
                ? Fraction.of(maxUses, 1)
                : retry.pow(maxUses).complement().divide(retry.complement());
    var accumulated = new LinkedHashMap<Hit, Fraction>();
    accumulate(accumulated, hits, mean);
    return new Result(law(accumulated, true), first, mean, work);
  }

  /** 한 비분열 옵션의 모든 재시도에서 제거 직후 상태·후보·가중치가 같을 때에만 유한 등비합을 쓴다. */
  private Fraction verifiedRetryMass(ItemState start, Attempt first) {
    if (!start.desecrated().isEmpty()) return null;
    var mutable = start.explicits().stream().filter(m -> !m.fractured()).toList();
    if (mutable.size() != 1 || held.contains(mutable.getFirst().modifierId())) return null;
    var initial = simulator.forItem(start).recipePlan(start, action, omens);
    if (initial.status() != AutomaticCraftingPlan.Status.READY || initial.branches().size() != 1)
      return null;
    var mass = Fraction.ZERO;
    for (var branch : first.misses().entrySet()) {
      if (work == budget) return null;
      work++;
      var state = branch.getKey();
      var plan = simulator.forItem(state).recipePlan(state, action, omens);
      if (plan.status() == AutomaticCraftingPlan.Status.UNAVAILABLE) continue;
      if (!initial.equals(plan)) return null;
      state.explicits().stream()
          .filter(m -> !m.fractured())
          .map(ModifierInstance::modifierId)
          .forEach(removable::add);
      mass = mass.add(branch.getValue());
    }
    return mass;
  }

  /** 같은 상태의 1회 분포를 재사용하며 모든 분기의 합이 1인 경우에만 미스 질량을 다음 시도로 보낸다. */
  private Attempt attempt(ItemState item) {
    if (!OneOptionPreparation.ready(item)) return new Attempt(Map.of(), Map.of(), true, true);
    var known = attempts.get(item);
    if (known != null) return known;
    var raw = transitions.successors(item, action.name(), omens, targets, budget - work, true);
    work += Math.min(budget - work, Math.max(1, raw.outcomes().size()));
    removable.addAll(raw.removableModifierIds());
    var hits = new LinkedHashMap<Hit, Fraction>();
    var misses = new LinkedHashMap<ItemState, Fraction>();
    boolean complete = raw.complete();
    var total = Fraction.ZERO;
    for (var outcome : raw.outcomes()) {
      var mass = outcome.probability();
      if (mass == null) complete = false;
      else total = total.add(mass);
      if (!Collections.disjoint(outcome.removedModifierIds(), protectedIds)) continue;
      var removed = new HashSet<>(outcome.removedModifierIds());
      removed.retainAll(held);
      boolean acquired =
          outcome.state().explicits().stream()
              .map(ModifierInstance::modifierId)
              .anyMatch(id -> targets.contains(id) && !held.contains(id));
      if (acquired || !removed.isEmpty()) {
        var key = new Hit(outcome.state(), Set.copyOf(removed));
        if (hits.containsKey(key)) {
          var prior = hits.get(key);
          mass = prior == null || mass == null ? null : prior.add(mass);
        }
        hits.put(key, mass);
      } else if (mass != null && !mass.equals(Fraction.ZERO))
        misses.merge(outcome.state(), mass, Fraction::add);
    }
    boolean unavailable = raw.outcomes().isEmpty() && raw.complete();
    var attempt =
        new Attempt(
            hits, misses, complete && (unavailable || total.equals(Fraction.ONE)), unavailable);
    attempts.put(item, attempt);
    return attempt;
  }

  /** 시작 질량을 곱한 각 분기를 같은 실제 상태나 적중 사건에 누적한다. */
  private static <T> void accumulate(
      Map<T, Fraction> combined, Map<T, Fraction> local, Fraction mass) {
    local.forEach((key, value) -> combined.merge(key, mass.multiply(value), Fraction::add));
  }

  /** 이미 관측한 배타적 첫 적중의 질량은 하한이다. 아직 계산하지 않은 추가 적중과 기대 사용량은 미확정이다. */
  private Result unknown(RecipeTransitions.Law firstHits, Map<Hit, Fraction> knownHits) {
    return new Result(law(knownHits, false), firstHits, null, work);
  }

  /** 후속 단계가 사용하는 실제 종착 상태와 옵션 제거 이력을 일반 전이 형식으로 반환한다. */
  private RecipeTransitions.Law law(Map<Hit, Fraction> hits, boolean complete) {
    var outcomes =
        hits.entrySet().stream()
            .map(
                entry ->
                    new RecipeTransitions.Outcome(
                        entry.getKey().state(), entry.getValue(), entry.getKey().removedTargets()))
            .toList();
    return new RecipeTransitions.Law(outcomes, complete, Set.copyOf(removable));
  }
}
