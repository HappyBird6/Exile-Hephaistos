package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.CraftingAction;
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
      int budget) {
    this.simulator = simulator;
    this.transitions = transitions;
    this.action = action;
    this.omens = omens;
    this.targets = targets;
    this.protectedIds = protectedIds;
    this.budget = budget;
    held = new HashSet<>();
    for (var modifier : start.explicits()) {
      if (targets.contains(modifier.modifierId())) held.add(modifier.modifierId());
    }
  }

  /** 호출자가 반복으로 지정한 한 단계만 계산한다. 작업대 반복 한도를 넘는 입력은 거절한다. */
  static Result calculate(
      WorkbenchSimulator simulator,
      RecipeTransitions transitions,
      ItemState start,
      WorkbenchCurrency action,
      Set<WorkbenchOmen> omens,
      Set<String> targetIds,
      Set<String> protectedIds,
      int maxUses,
      int budget) {
    if (action.baseAction() != CraftingAction.CHAOS
        || maxUses < 1
        || maxUses > PublicContracts.REPEAT.maximumUses()
        || budget < 0) throw new IllegalArgumentException("Invalid bounded Chaos step");
    return new BoundedChaosRepeat(
            simulator, transitions, start, action, omens, targetIds, protectedIds, budget)
        .run(start, maxUses);
  }

  /** 첫 시도 이후 실제 미적중 상태만 전파한다. 사용 불가·보호 실패·제한 도달 질량도 초기 분모에 남는다. */
  private Result run(ItemState start, int maxUses) {
    var first = attempt(start);
    var firstHits = law(first.hits(), first.complete());
    if (!first.complete()) {
      var exact = samePoolWithoutMissRolls(start, maxUses);
      return exact != null ? exact : unknown(firstHits, first.hits());
    }
    if (first.unavailable() || maxUses == 1 || first.misses().isEmpty())
      return new Result(
          firstHits, firstHits, first.unavailable() ? Fraction.ZERO : Fraction.ONE, work);
    var renewal = verifiedRetryMass(start, first);
    if (renewal != null) return renewalResult(first.hits(), firstHits, renewal, maxUses);
    var hits = new LinkedHashMap<>(first.hits());
    var frontier = first.misses();
    var uses = Fraction.ONE;
    for (int used = 1; used < maxUses && !frontier.isEmpty(); used++) {
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
  private Result samePoolWithoutMissRolls(ItemState start, int maxUses) {
    if (work >= budget
        || !omens.isEmpty()
        || !start.desecrated().isEmpty()
        || BasicJewel.supportedCrafting(start.baseItemId())
        || !OneOptionPreparation.ready(start)) return null;
    var mutable = start.explicits().stream().filter(m -> !m.fractured()).findFirst().orElseThrow();
    if (targets.contains(mutable.modifierId())
        || !start.explicits().stream()
            .filter(ModifierInstance::fractured)
            .map(ModifierInstance::modifierId)
            .toList()
            .containsAll(protectedIds)) return null;
    var plan = simulator.forItem(start).recipePlan(start, action, omens);
    if (plan.status() != AutomaticCraftingPlan.Status.READY || plan.branches().size() != 1)
      return null;
    var redraw = plan.branches().getFirst();
    if (redraw.candidates().isEmpty()
        || redraw.remaining().explicits().stream().anyMatch(m -> !m.fractured())) return null;
    var terminal =
        transitions.successors(start, action.name(), omens, targets, budget - work, false);
    work += Math.min(budget - work, Math.max(1, terminal.outcomes().size()));
    if (!terminal.complete()) return null;
    // 한 번에 정확히 하나만 새로 붙는다. 종료 이후 후속 행동은 적중한 실제 상태의 분포를 받는다.
    var hits = new LinkedHashMap<Hit, Fraction>();
    var chance = Fraction.ZERO;
    for (var outcome : terminal.outcomes()) {
      if (outcome.probability() == null) return null;
      hits.merge(new Hit(outcome.state(), Set.of()), outcome.probability(), Fraction::add);
      chance = chance.add(outcome.probability());
    }
    for (var candidate : redraw.candidates())
      if (!targets.contains(candidate.id())) removable.add(candidate.id());
    return renewalResult(hits, law(hits, true), chance.complement(), maxUses);
  }

  /** 재시도 가능한 질량이 매번 같으면 유한 등비합으로 최초 적중 분포와 중단까지의 평균 사용량을 구한다. */
  private Result renewalResult(
      Map<Hit, Fraction> hits, RecipeTransitions.Law first, Fraction retry, int maxUses) {
    var mean =
        retry.equals(Fraction.ONE)
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
    if (mutable.size() != 1 || targets.contains(mutable.getFirst().modifierId())) return null;
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

  /** 검증할 수 없는 질량 대신 실제 도달 사례만 보존한다. 기대 사용량 역시 미확정이다. */
  private Result unknown(RecipeTransitions.Law firstHits, Map<Hit, Fraction> knownHits) {
    var witnesses = new LinkedHashMap<Hit, Fraction>(knownHits);
    witnesses.replaceAll((hit, mass) -> null);
    return new Result(law(witnesses, false), firstHits, null, work);
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
