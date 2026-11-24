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
import com.poe2craft.shared.item.ItemStateValidator;
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
  /** probability는 해당 단계 시작 상태 분포에서의 적중 확률이며, 미확정은 null이다. */
  public record Chance(@JsonInclude(JsonInclude.Include.ALWAYS) Double probability) {}

  /** action과 omens는 제작 선택, acquired·lost는 목표 번호다. 순수 준비에는 chance가 없고 반복 Divine에는 예상 사용량을 담는다. */
  public record Step(
      String action,
      List<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer socketIndex,
      @JsonInclude(JsonInclude.Include.NON_NULL) Chance chance,
      @JsonInclude(JsonInclude.Include.NON_NULL) Double expectedUses,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer maxUses) {
    /** 고정 경로 입력은 확률을 받지 않고 실제 작업대 분포에서 다시 계산한다. */
    public Step(
        String action,
        List<String> omens,
        List<Integer> acquired,
        List<Integer> lost,
        Integer socketIndex) {
      this(action, omens, acquired, lost, socketIndex, null, null, null);
    }

    /** 같은 제작을 택한 상태 간에 공통인 행동과 관측 결과를 표현한다. */
    private Step(Move move, List<Integer> acquired, List<Integer> lost) {
      this(
          move.action(),
          move.omens().stream().map(WorkbenchOmen::id).sorted().toList(),
          acquired,
          lost,
          move.socketIndex());
    }

    /** 행동 전이를 묶은 후 해당 경로 분포의 확률과 기대 사용량을 붙인다. */
    private Step measured(Chance hit, Double uses, Integer maximum) {
      return new Step(action, omens, acquired, lost, socketIndex, hit, uses, maximum);
    }

    /** 클라이언트에서 돌아온 행동·징조와 실제 홈 위치만 재생 키에 포함한다. */
    private Move move() {
      return new Move(
          action,
          omens.stream().map(WorkbenchOmen::fromId).collect(java.util.stream.Collectors.toSet()),
          socketIndex);
    }
  }

  /**
   * order는 재획득을 포함한 순서, probability는 순수 준비 미적중을 조건으로 한 완주 확률, stateIndices는 실제 완주 분기의 단계별 상태 번호다.
   */
  public record Route(
      int cardIndex,
      List<Integer> order,
      List<Step> steps,
      Double probability,
      List<Integer> stateIndices) {}

  /** states는 반환 경로가 참조하는 상태만 담는다. complete가 거짓이면 미확정 결과 또는 예산 밖 경로가 남았다. */
  public record Result(List<Route> routes, boolean complete, List<ItemState> states) {}

  /** state와 parent는 같은 실제 분기의 연속 상태다. 시작 상태만 parent가 없다. */
  private record Witness(ItemState state, Witness parent) {
    /** 시작 이후의 상태만 순서대로 공통 응답 사전에 등록하여 0 기반 번호로 반환한다. */
    List<Integer> indices(Map<ItemState, Integer> dictionary) {
      var frames = new ArrayDeque<ItemState>();
      for (var frame = this; frame.parent != null; frame = frame.parent)
        frames.addFirst(frame.state);
      return frames.stream()
          .map(s -> dictionary.computeIfAbsent(s, ignored -> dictionary.size()))
          .toList();
    }
  }

  /** 같은 아이템에 도달한 모든 분기의 probability와, 그중 실제로 성립한 witness 한 개다. */
  private record Mass(Fraction probability, Witness witness) {}

  /** 같은 경로의 서로 다른 실제 아이템별 질량과, 그 경로에 빠진 분기가 있는지를 보존한다. */
  private static final class Distribution {
    final Map<ItemState, Mass> masses = new LinkedHashMap<>();
    boolean complete = true;
    // 검증에는 원래 질량을 유지하고, 표시 확률에서만 미적중 준비 사건에 조건을 건다.
    Fraction preparationFactor = Fraction.ONE;
    Fraction parentLiveMass;
    Double divineUses;
    Double divineWithinLimit;
    final PreparationMass incoming;

    /** incoming은 같은 행동에 속한 모든 관측 분기가 공유한다. 시작 분포에는 이전 행동이 없다. */
    Distribution(PreparationMass incoming) {
      this.incoming = incoming;
    }

    /** 미확정 질량은 null로 유지한다. 서로 배타적인 같은 상태 결과만 합친다. */
    void add(ItemState item, Fraction probability, Witness parent) {
      masses.compute(
          item,
          (key, existing) ->
              existing == null
                  ? new Mass(probability, new Witness(key, parent))
                  : new Mass(sum(existing.probability, probability), existing.witness));
    }

    /** 같은 관측 단계에 들어오는 모든 아이템의 원래 확률 질량이다. */
    Fraction total() {
      Fraction weight = Fraction.ZERO;
      for (var mass : masses.values()) weight = sum(weight, mass.probability);
      return weight;
    }
  }

  /** 같은 행동이 가능한 질량과, 그중 목표 없이 승급하거나 임시 칸을 채운 모든 결과의 질량이다. */
  private static final class PreparationMass {
    Fraction eligible = Fraction.ZERO;
    Fraction missed = Fraction.ZERO;
    boolean dummy;
    boolean complete = true;
  }

  private final RecipeTransitions transitions;
  private final RecipeMoves moves;
  private final WorkbenchSimulator simulator;
  private final ItemStateValidator slots;
  private final RouteCards.Card card;
  private final Predicate<ItemState> matchesGoal;
  private final boolean presenceOnly;
  private final Integer minimumQuality;
  private final Set<String> targets = new TreeSet<>();
  private final Set<String> lowIds = new HashSet<>();
  private final Map<Integer, Set<String>> allowed = new TreeMap<>();
  private final int cardIndex;
  private final List<Route> routes = new ArrayList<>();
  private final Map<ItemState, Integer> stateIndices;
  private final List<Step> fixedSteps;
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
      Integer minimumQuality,
      Map<ItemState, Integer> stateIndices,
      List<Step> fixedSteps) {
    this.stateIndices = stateIndices;
    this.fixedSteps = fixedSteps;
    this.presenceOnly = presenceOnly;
    this.minimumQuality = minimumQuality;
    this.simulator = simulator;
    slots = new ItemStateValidator(simulator.catalog());
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
          return new Result(
              List.of(new Route(i, List.of(), List.of(), 1.0, List.of())), true, List.of());
    }
    var routes = new ArrayList<Route>();
    var stateIndices = new LinkedHashMap<ItemState, Integer>();
    boolean complete = true;
    int budget = outcomeBudget;
    for (int i = 0; i < cards.size(); i++) {
      int share = budget / (cards.size() - i);
      var search =
          new RecipeRoutePlanner(
              simulator,
              i,
              cards.get(i),
              matchesGoal,
              share,
              presenceOnly,
              minimumQuality,
              stateIndices,
              null);
      var root = new Distribution(null);
      root.add(start, Fraction.ONE, null);
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
    return new Result(List.copyOf(routes), complete, List.copyOf(stateIndices.keySet()));
  }

  /** 한 카드에서 지정된 행동·징조·옵션 변화만 순서대로 재생한다. 완주 실패와 예산·미확정 법칙에 따른 미완료를 구분한다. */
  public static Result evaluateFixed(
      WorkbenchSimulator simulator,
      ItemState start,
      RouteCards.Card card,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget,
      boolean presenceOnly,
      Integer minimumQuality,
      List<Step> selectedSteps) {
    if (outcomeBudget < 1)
      throw new IllegalArgumentException("Positive route outcome budget required");
    var states = new LinkedHashMap<ItemState, Integer>();
    var planner =
        new RecipeRoutePlanner(
            simulator,
            0,
            card,
            matchesGoal,
            outcomeBudget,
            presenceOnly,
            minimumQuality,
            states,
            List.copyOf(selectedSteps));
    var initial = new Distribution(null);
    initial.add(start, Fraction.ONE, null);
    planner.explore(List.of(), initial);
    return new Result(List.copyOf(planner.routes), planner.complete, List.copyOf(states.keySet()));
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
    Witness representative = null;
    Fraction success = Fraction.ZERO;
    Fraction live = Fraction.ZERO;
    var children = new LinkedHashMap<Step, Distribution>();
    var preparations = new HashMap<Move, PreparationMass>();
    var divine = new DivineRepeatEstimate();
    boolean atFixedEnd = fixedSteps != null && path.size() == fixedSteps.size();
    var required = fixedSteps == null || atFixedEnd ? null : fixedSteps.get(path.size());
    var selected = required == null ? null : required.move();
    var selectedTargets = new HashSet<String>();
    if (required != null) {
      for (int number : required.acquired())
        selectedTargets.addAll(allowed.getOrDefault(number, Set.of()));
    }
    for (var entry : current.masses.entrySet()) {
      var item = entry.getKey();
      var before = held(item);
      if (before.size() == allowed.size() && matchesGoal.test(item)) {
        var mass = entry.getValue();
        if (representative == null) representative = mass.witness;
        success = sum(success, mass.probability);
        continue;
      }
      if (atFixedEnd) continue;
      live = sum(live, entry.getValue().probability);
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
        choices.addAll(RecipeMoves.rarityPreparation(0, item));
        if (!presenceOnly
            && !item.explicits().isEmpty()
            && (path.isEmpty() || !path.getLast().action().equals(WorkbenchCurrency.DIVINE.name())))
          choices.add(new Choice(0, new Move(WorkbenchCurrency.DIVINE.name()), true, null));
      }
      skippedStates |=
          !extend(
              item,
              before,
              protectedIds,
              choices,
              entry.getValue(),
              current.complete,
              children,
              preparations,
              divine,
              selected,
              selectedTargets);
    }
    if (representative != null && (fixedSteps == null || atFixedEnd)) {
      var completed = path;
      Double routeProbability =
          current.complete ? decimal(product(success, current.preparationFactor)) : null;
      if (!path.isEmpty() && path.getLast().action().equals(WorkbenchCurrency.DIVINE.name())) {
        var steps = new ArrayList<>(path);
        var hit = current.complete ? ratio(success, current.parentLiveMass) : null;
        var reached =
            current.complete
                ? decimal(product(current.parentLiveMass, current.preparationFactor))
                : null;
        routeProbability =
            reached == null || current.divineWithinLimit == null
                ? null
                : reached * current.divineWithinLimit;
        steps.set(
            steps.size() - 1,
            path.getLast()
                .measured(
                    new Chance(decimal(hit)), current.divineUses, DivineRepeatEstimate.MAX_USES));
        completed = List.copyOf(steps);
      }
      var order = path.stream().flatMap(s -> s.acquired().stream()).toList();
      routes.add(
          new Route(
              cardIndex, order, completed, routeProbability, representative.indices(stateIndices)));
    }
    // 유한 예산이어도 긴 반복 분기만 우선하지 않도록 직접 획득한 후보를 준비 후보보다 먼저 본다.
    var ordered = new ArrayList<>(children.entrySet());
    ordered.sort(Comparator.comparing(e -> e.getKey().acquired().isEmpty()));
    for (var child : ordered) {
      var step = child.getKey();
      if (required != null
          && (!step.acquired().equals(required.acquired()) || !step.lost().equals(required.lost())))
        continue;
      var nextStates = child.getValue();
      var preparation = nextStates.incoming;
      nextStates.complete &= !skippedStates && preparation.complete;
      nextStates.preparationFactor = current.preparationFactor;
      nextStates.parentLiveMass = live;
      if (step.acquired().isEmpty() && step.lost().isEmpty() && preparation.dummy) {
        // 실제 후속 제작이 가능한 더미만 남았더라도 모든 미적중 더미를 분모에 유지한다.
        nextStates.preparationFactor =
            product(current.preparationFactor, ratio(preparation.eligible, preparation.missed));
      }
      if (!step.acquired().isEmpty()
          || !step.lost().isEmpty()
          || step.action().equals(WorkbenchCurrency.ANNULMENT.name())) {
        var hit = nextStates.complete ? ratio(nextStates.total(), live) : null;
        step = step.measured(new Chance(decimal(hit)), null, null);
      } else if (step.action().equals(WorkbenchCurrency.DIVINE.name())) {
        nextStates.divineUses = divine.expectedUses(live, nextStates.complete);
        nextStates.divineWithinLimit = divine.successWithinLimit(live, nextStates.complete);
        step = step.measured(null, 1.0, null);
      }
      var next = new ArrayList<>(path);
      next.add(step);
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
      Mass prior,
      boolean parentComplete,
      Map<Step, Distribution> children,
      Map<Move, PreparationMass> preparations,
      DivineRepeatEstimate divine,
      Move selected,
      Set<String> selectedTargets) {
    var byMove = new LinkedHashMap<Move, List<Choice>>();
    for (var c : choices) byMove.computeIfAbsent(c.move(), ignored -> new ArrayList<>()).add(c);
    if (selected != null) byMove = forReplay(byMove, selected, !protectedIds.isEmpty());
    var laws = new LinkedHashMap<Move, Law>();
    var safeTargets = new HashSet<Integer>();
    boolean allConsidered = true;
    for (var entry : byMove.entrySet()) {
      var move = entry.getKey();
      if (selected != null
          && !move.equals(selected)
          && Collections.disjoint(laws.get(selected).removableModifierIds(), protectedIds)) break;
      if (remaining == 0) {
        complete = false;
        allConsidered = false;
        break;
      }
      var intents = entry.getValue();
      boolean prepares = intents.stream().anyMatch(Choice::preparation);
      var action =
          move.socketIndex() == null ? move.action() : move.action() + "@" + move.socketIndex();
      // 고정된 적중 단계는 해당 번호의 모든 티어만 열거한다. 전이 법칙은 여전히 전체 작업대 후보를 분모로 쓴다.
      boolean replayHit = move.equals(selected) && !selectedTargets.isEmpty();
      var law =
          transitions.successors(
              item,
              action,
              move.omens(),
              replayHit ? selectedTargets : targets,
              remaining,
              prepares && !replayHit);
      var preparation = preparations.computeIfAbsent(move, k -> new PreparationMass());
      preparation.complete &= law.complete();
      if (move.action().equals(WorkbenchCurrency.DIVINE.name()))
        divine.add(prior.probability, law, matchesGoal);
      if (prepares && !replayHit)
        countDummyMisses(preparation, item, before, prior.probability, law);
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
      if (selected != null && !selected.equals(move)) continue;
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
        var step = new Step(move, acquired, List.copyOf(lost));
        var child =
            children.computeIfAbsent(step, ignored -> new Distribution(preparations.get(move)));
        child.complete &= parentComplete && law.complete();
        child.add(
            outcome.state(), product(prior.probability, outcome.probability()), prior.witness);
      }
    }
    return allConsidered;
  }

  /** 기본 등급 경로에서 합법이었던 한 행동만 선택 등급으로 교체한다. 위험한 제거의 L 보호 여부를 검증할 대안만 함께 둔다. */
  private static LinkedHashMap<Move, List<Choice>> forReplay(
      Map<Move, List<Choice>> choices, Move selected, boolean protectsLow) {
    var selectedBase = RecipeMoves.baseMove(selected);
    var intents = choices.get(selectedBase);
    var replay = new LinkedHashMap<Move, List<Choice>>();
    if (intents == null) return replay;
    replay.put(selected, intents);
    if (!protectsLow) return replay;
    for (var entry : choices.entrySet()) {
      var move = entry.getKey();
      if (move.equals(selectedBase)) continue;
      var alternatives =
          entry.getValue().stream()
              .filter(c -> intents.stream().anyMatch(i -> i.target() == c.target()))
              .toList();
      if (alternatives.isEmpty()) continue;
      if (move.action().equals(selectedBase.action()))
        move = new Move(selected.action(), move.omens(), move.socketIndex());
      replay.put(move, alternatives);
    }
    return replay;
  }

  /** 보호·후속 제작 필터보다 먼저, 제거 없는 승급·옵션 추가에서 새 목표가 붙지 않은 질량을 계산한다. */
  private void countDummyMisses(
      PreparationMass prep, ItemState item, Map<Integer, String> held, Fraction prior, Law law) {
    if (law.outcomes().isEmpty() || !law.removableModifierIds().isEmpty()) return;
    var first = law.outcomes().getFirst().state();
    if (first.rarity() == item.rarity() && first.explicits().size() <= item.explicits().size())
      return;
    prep.dummy = true;
    prep.eligible = sum(prep.eligible, prior);
    for (var outcome : law.outcomes()) {
      if (outcome.state().explicits().stream()
          .noneMatch(
              m -> targets.contains(m.modifierId()) && !held.containsValue(m.modifierId()))) {
        prep.missed = sum(prep.missed, product(prior, outcome.probability()));
      }
    }
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
    // 엑잘이 마지막 명시 칸에 목표 대신 더미를 붙이면 이 분기는 실패다. 미공개 훼손 칸도 점유에 포함한다.
    if (RecipeMoves.baseMove(choice.move()).action().equals(WorkbenchCurrency.EXALTED.name())
        && outcome.state().explicits().stream()
            .noneMatch(
                m -> targets.contains(m.modifierId()) && !before.containsValue(m.modifierId()))) {
      var capacity = slots.slots(outcome.state());
      if (capacity.remainingPrefixes() == 0 && capacity.remainingSuffixes() == 0) return false;
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

  /** 원래 질량이나 준비 조건부 배수에 미확정 값이 있으면 결과 역시 미확정이다. */
  private static Fraction product(Fraction a, Fraction b) {
    return a == null || b == null ? null : a.multiply(b);
  }

  /** 앞 단계에서 살아 있는 상태에 대한 조건부 비율이다. 분모가 확인되지 않으면 미확정이다. */
  private static Fraction ratio(Fraction value, Fraction denominator) {
    return value == null || denominator == null || denominator.equals(Fraction.ZERO)
        ? null
        : value.divide(denominator);
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
