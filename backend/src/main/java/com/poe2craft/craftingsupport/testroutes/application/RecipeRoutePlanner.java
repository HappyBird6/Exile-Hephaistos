package com.poe2craft.craftingsupport.testroutes.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poe2craft.craftingsupport.testroutes.application.RecipeMoves.Choice;
import com.poe2craft.craftingsupport.testroutes.application.RecipeMoves.Move;
import com.poe2craft.craftingsupport.testroutes.application.RecipeTransitions.Law;
import com.poe2craft.craftingsupport.testroutes.application.RecipeTransitions.Outcome;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Predicate;

/**
 * 카드별로 작업대 전이를 따라가 실제 목표 획득·손실을 기록한다. 한 경로의 동일한 관측 결과에 이르는 확률은 합치지만 서로 다른 화폐 선택의 확률은 합치지 않는다. 계산
 * 인스턴스는 요청 카드 한 장에 한정된다.
 */
public final class RecipeRoutePlanner {
  /** action과 omens는 한 번의 제작 선택이며 acquired와 lost는 실제 전이의 목표 번호다. */
  public record Step(
      String action,
      List<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer socketIndex) {}

  /** order에는 회복한 목표도 다시 나타난다. probability는 이 관측 경로의 검증된 성공 확률이다. */
  public record Route(int cardIndex, List<Integer> order, List<Step> steps, Double probability) {}

  /** complete가 거짓이면 미확정 결과 또는 계산 예산 밖 경로가 남았다. */
  public record Result(List<Route> routes, boolean complete) {}

  /** 같은 경로의 서로 다른 실제 아이템별 질량과, 그 경로에 빠진 분기가 있는지를 보존한다. */
  private static final class Distribution {
    final Map<ItemState, Fraction> masses = new LinkedHashMap<>();
    boolean complete = true;

    /** 미확정 질량은 null로 유지한다. 서로 배타적인 같은 상태 결과만 합친다. */
    void add(ItemState item, Fraction probability) {
      if (!masses.containsKey(item)) masses.put(item, probability);
      else masses.put(item, sum(masses.get(item), probability));
    }
  }

  private final RecipeTransitions transitions;
  private final RecipeMoves moves;
  private final WorkbenchSimulator simulator;
  private final RouteCards.Card card;
  private final Predicate<ItemState> matchesGoal;
  private final boolean presenceOnly;
  private final Integer minimumQuality;
  private final Set<String> targets = new TreeSet<>();
  private final Set<String> lowIds = new HashSet<>();
  private final Map<Integer, Set<String>> allowed = new TreeMap<>();
  private final int cardIndex;
  private final List<Route> routes = new ArrayList<>();
  private int remaining;
  private boolean complete = true;
  private long nextSequence;

  /** simulator·card·matchesGoal을 같은 시작 상태에 적용하고 outcomeBudget만큼 결과를 열거한다. */
  private RecipeRoutePlanner(
      WorkbenchSimulator simulator,
      int cardIndex,
      RouteCards.Card card,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget,
      boolean presenceOnly,
      Integer minimumQuality) {
    this.presenceOnly = presenceOnly;
    this.minimumQuality = minimumQuality;
    this.simulator = simulator;
    transitions = new RecipeTransitions(simulator, presenceOnly);
    moves = new RecipeMoves(simulator);
    this.card = card;
    this.cardIndex = cardIndex;
    this.matchesGoal = matchesGoal;
    remaining = outcomeBudget;
    for (var opt : card.options()) {
      allowed.put(opt.number(), opt.tiers().keySet());
      targets.addAll(opt.tiers().keySet());
      opt.tiers()
          .forEach(
              (id, sources) -> {
                if (sources.stream().anyMatch(s -> s.frequency() == RouteCards.Frequency.L))
                  lowIds.add(id);
              });
    }
  }

  /** 모든 카드에 계산 기회를 나누고 완주 질량을 큰 순으로, 미확정 확률은 뒤에 둔다. */
  public static Result calculate(
      WorkbenchSimulator simulator,
      ItemState start,
      List<RouteCards.Card> cards,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget) {
    return calculate(simulator, start, cards, matchesGoal, outcomeBudget, false, null);
  }

  /** 원래 목표의 수치 관여 여부와 요구 품질을 사용해 동등한 수치 결과만 함께 센다. */
  public static Result calculate(
      WorkbenchSimulator simulator,
      ItemState start,
      List<RouteCards.Card> cards,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget,
      boolean presenceOnly,
      Integer minimumQuality) {
    if (outcomeBudget < 1)
      throw new IllegalArgumentException("Positive route outcome budget required");
    if (matchesGoal.test(start)) {
      var ids =
          start.explicits().stream()
              .map(m -> m.modifierId())
              .collect(java.util.stream.Collectors.toSet());
      for (int i = 0; i < cards.size(); i++)
        if (cards.get(i).options().stream()
            .allMatch(o -> !Collections.disjoint(ids, o.tiers().keySet())))
          return new Result(List.of(new Route(i, List.of(), List.of(), 1.0)), true);
    }
    var routes = new ArrayList<Route>();
    boolean complete = true;
    int budget = outcomeBudget;
    for (int i = 0; i < cards.size(); i++) {
      int share = budget / (cards.size() - i);
      var search =
          new RecipeRoutePlanner(
              simulator, i, cards.get(i), matchesGoal, share, presenceOnly, minimumQuality);
      var root = new Distribution();
      root.add(start, Fraction.ONE);
      search.explore(List.of(), root);
      routes.addAll(search.routes);
      complete &= search.complete;
      budget -= share - search.remaining;
    }
    routes.sort(
        Comparator.comparing(Route::probability, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparingInt(r -> r.steps().size())
            .thenComparingInt(Route::cardIndex)
            .thenComparing(r -> r.steps().toString()));
    return new Result(List.copyOf(routes), complete);
  }

  /** 같은 관측 경로의 상태 및 검색 우선순위. 보유 목표가 많은 경로부터 검사하고 동점이면 짧은 경로·등록 순서를 따른다. */
  private record Pending(List<Step> path, Distribution current, int heldCount, long sequence) {}

  private void explore(List<Step> initialPath, Distribution initial) {
    var pending =
        new PriorityQueue<>(
            Comparator.comparingInt(Pending::heldCount)
                .reversed()
                .thenComparingInt(p -> p.path().size())
                .thenComparingLong(Pending::sequence));
    pending.add(new Pending(initialPath, initial, 0, nextSequence++));
    while (!pending.isEmpty()) {
      var next = pending.remove();
      expand(next.path(), next.current(), pending);
    }
  }

  /** 동일 관측 경로의 실제 아이템을 함께 전개해, 먼저 달성한 상태에서 즉시 멈춘다. */
  private void expand(List<Step> path, Distribution current, Queue<Pending> pending) {
    boolean skippedStates = false;
    boolean found = false;
    Fraction success = Fraction.ZERO;
    var children = new LinkedHashMap<Step, Distribution>();
    for (var entry : current.masses.entrySet()) {
      var item = entry.getKey();
      var before = held(item);
      if (before.size() == allowed.size() && matchesGoal.test(item)) {
        found = true;
        success = sum(success, entry.getValue());
        continue;
      }
      if (remaining == 0) {
        complete = false;
        skippedStates = true;
        continue;
      }
      var protectedIds = new HashSet<>(before.values());
      protectedIds.retainAll(lowIds);
      var choices = new ArrayList<>(moves.choices(item, card, before));
      if (minimumQuality != null
          && minimumQuality
              > (item.catalystQuality() == null ? 0 : item.catalystQuality().amount())) {
        for (var currency : WorkbenchCurrency.values()) {
          if (currency.catalystType() != null)
            choices.add(new Choice(0, new Move(currency.name()), true, null));
        }
      }
      if (before.size() == allowed.size()) {
        // 한 번 재굴림한 뒤에도 미달이면 이 실행열은 실패다. 무한 재시도의 성공률을 가정하지 않는다.
        if (item.rarity() == ItemState.Rarity.NORMAL)
          choices.add(new Choice(0, new Move(WorkbenchCurrency.TRANSMUTATION.name()), true, null));
        if (item.rarity() == ItemState.Rarity.MAGIC)
          choices.add(new Choice(0, new Move(WorkbenchCurrency.REGAL.name()), true, null));
        if (!presenceOnly
            && !item.explicits().isEmpty()
            && (path.isEmpty() || !path.getLast().action().equals(WorkbenchCurrency.DIVINE.name())))
          choices.add(new Choice(0, new Move(WorkbenchCurrency.DIVINE.name()), true, null));
      }
      skippedStates |=
          !extend(
              item, before, protectedIds, choices, entry.getValue(), current.complete, children);
    }
    if (found) {
      var order = path.stream().flatMap(s -> s.acquired().stream()).toList();
      routes.add(new Route(cardIndex, order, path, current.complete ? decimal(success) : null));
    }
    // 유한 예산이어도 긴 반복 분기만 우선하지 않도록 직접 획득한 후보를 준비 후보보다 먼저 본다.
    var ordered = new ArrayList<>(children.entrySet());
    ordered.sort(Comparator.comparing(e -> e.getKey().acquired().isEmpty()));
    for (var child : ordered) {
      if (skippedStates) child.getValue().complete = false;
      var next = new ArrayList<>(path);
      next.add(child.getKey());
      int progress = held(child.getValue().masses.keySet().iterator().next()).size();
      pending.add(new Pending(List.copyOf(next), child.getValue(), progress, nextSequence++));
    }
  }

  /** 현재 상태에서 같은 행동을 한 번만 전개하고, 같은 목표를 지킬 수 있는 대안이 있으면 위험 선택을 뺀다. */
  private boolean extend(
      ItemState item,
      Map<Integer, String> before,
      Set<String> protectedIds,
      List<Choice> choices,
      Fraction prior,
      boolean parentComplete,
      Map<Step, Distribution> children) {
    var byMove = new LinkedHashMap<Move, List<Choice>>();
    for (var c : choices) byMove.computeIfAbsent(c.move(), ignored -> new ArrayList<>()).add(c);
    var laws = new LinkedHashMap<Move, Law>();
    var safeTargets = new HashSet<Integer>();
    boolean allConsidered = true;
    for (var entry : byMove.entrySet()) {
      if (remaining == 0) {
        complete = false;
        allConsidered = false;
        break;
      }
      var move = entry.getKey();
      var intents = entry.getValue();
      boolean prepares = intents.stream().anyMatch(Choice::preparation);
      var action =
          move.socketIndex() == null ? move.action() : move.action() + "@" + move.socketIndex();
      var law = transitions.successors(item, action, move.omens(), targets, remaining, prepares);
      remaining -= Math.max(1, law.outcomes().size());
      complete &= law.complete();
      laws.put(move, law);
      if (!protectedIds.isEmpty()
          && Collections.disjoint(law.removableModifierIds(), protectedIds)) {
        for (Choice c : intents) {
          if (law.outcomes().stream().anyMatch(o -> reaches(c, before, o, protectedIds)))
            safeTargets.add(c.target());
        }
      }
    }
    for (var entry : laws.entrySet()) {
      var move = entry.getKey();
      var law = entry.getValue();
      boolean safe = Collections.disjoint(law.removableModifierIds(), protectedIds);
      if (!safe && !allConsidered) continue;
      var intents =
          byMove.get(move).stream().filter(c -> safe || !safeTargets.contains(c.target())).toList();
      if (intents.isEmpty()) continue;
      for (var outcome : law.outcomes()) {
        if (outcome.state().equals(item)
            || !Collections.disjoint(outcome.removedModifierIds(), protectedIds)) continue;
        if (intents.stream().noneMatch(c -> reaches(c, before, outcome, protectedIds))) continue;
        var after = held(outcome.state());
        var lost = new ArrayList<Integer>();
        before.forEach(
            (n, id) -> {
              if (!after.containsKey(n) || outcome.removedModifierIds().contains(id)) lost.add(n);
            });
        var acquired =
            after.keySet().stream()
                .filter(n -> !before.containsKey(n) || lost.contains(n))
                .toList();
        var step =
            new Step(
                move.action(),
                move.omens().stream().map(WorkbenchOmen::id).sorted().toList(),
                acquired,
                List.copyOf(lost),
                move.socketIndex());
        var child = children.computeIfAbsent(step, ignored -> new Distribution());
        child.complete &= parentComplete && law.complete();
        child.add(
            outcome.state(),
            prior == null || outcome.probability() == null
                ? null
                : prior.multiply(outcome.probability()));
      }
    }
    return allConsidered;
  }

  /** 준비 후 지정한 교체가 실제로 가능하고 L을 잃지 않는지 작업대의 완전한 제거 후보와 대조한다. */
  private boolean reaches(
      Choice choice, Map<Integer, String> before, Outcome outcome, Set<String> protectedIds) {
    if (!choice.preparation()) {
      return outcome.state().explicits().stream()
          .anyMatch(
              m ->
                  allowed.get(choice.target()).contains(m.modifierId())
                      && (!before.containsKey(choice.target())
                          || outcome.removedModifierIds().contains(before.get(choice.target()))));
    }
    if (choice.followup() == null) return true;
    var workbench = simulator.forItem(outcome.state());
    var followup = choice.followup();
    var plan =
        workbench.recipePlan(
            outcome.state(), WorkbenchCurrency.valueOf(followup.action()), followup.omens());
    return plan.status() == com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan.Status.READY
        && plan.branches().stream()
            .allMatch(
                b ->
                    b.remaining().explicits().stream()
                        .map(m -> m.modifierId())
                        .toList()
                        .containsAll(protectedIds))
        && plan.branches().stream()
            .flatMap(b -> b.candidates().stream())
            .anyMatch(m -> allowed.get(choice.target()).contains(m.id()));
  }

  /** 구체적인 명시 옵션을 카드 번호에 투영한다. 시작 목표의 단순 보유는 획득으로 세지 않는다. */
  private Map<Integer, String> held(ItemState item) {
    var result = new TreeMap<Integer, String>();
    for (var opt : card.options())
      for (var instance : item.explicits())
        if (opt.tiers().containsKey(instance.modifierId()))
          result.put(opt.number(), instance.modifierId());
    return result;
  }

  /** 서로 배타적인 두 분기 질량을 더한다. 자료가 없는 분기는 미확정으로 보존한다. */
  private static Fraction sum(Fraction a, Fraction b) {
    return a == null || b == null ? null : a.add(b);
  }

  /** 정렬·표시할 때만 분수를 실수로 변환한다. */
  private static Double decimal(Fraction value) {
    return value == null
        ? null
        : new BigDecimal(value.numerator())
            .divide(new BigDecimal(value.denominator()), MathContext.DECIMAL128)
            .doubleValue();
  }
}
