package com.poe2craft.craftingsupport.testroutes.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poe2craft.craftingsupport.testroutes.application.RecipeMoves.Choice;
import com.poe2craft.craftingsupport.testroutes.application.RecipeMoves.Move;
import com.poe2craft.craftingsupport.testroutes.application.RecipeMoves.Purpose;
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
  /** 미확정 확률이 포함되는 폐구간이다. lower·upper는 0 이상 1 이하다. */
  public record ProbabilityBounds(double lower, double upper) {}

  /** probability는 해당 단계 시작 상태 분포에서의 적중 확률이며, 미확정은 null이다. */
  public record Chance(
      @JsonInclude(JsonInclude.Include.ALWAYS) Double probability,
      @JsonInclude(JsonInclude.Include.NON_NULL) ProbabilityBounds probabilityBounds) {
    /** 관측 분모와 분자의 경계가 일치하면 정확한 숫자 하나만 돌려준다. */
    static Chance measured(Fraction exact, RecipeProbabilityRange range) {
      Fraction resolved = exact != null ? exact : range.exact();
      return new Chance(decimal(resolved), resolved == null ? range.wire() : null);
    }
  }

  /** 해당 화폐를 한 번 사용했을 때의 목표 적중, 같은 옵션을 다시 노릴 수 있는 미적중, 실패다. */
  public record OmenOutcomes(Chance success, Chance recoverable, Chance failure) {
    /** 세 결과 중 하나라도 범위이면 이 경로의 단계 분류는 미완전하다. */
    private boolean complete() {
      return success.probability() != null
          && recoverable.probability() != null
          && failure.probability() != null;
    }
  }

  /** action과 omens는 제작 선택, acquired·lost는 목표 번호다. 순수 준비에는 chance가 없고 반복 Divine에는 예상 사용량을 담는다. */
  public record Step(
      String action,
      List<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer socketIndex,
      @JsonInclude(JsonInclude.Include.NON_NULL) Chance chance,
      @JsonInclude(JsonInclude.Include.NON_NULL) Double expectedUses,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer maxUses,
      @JsonInclude(JsonInclude.Include.NON_NULL) String originalAction,
      @JsonInclude(JsonInclude.Include.NON_NULL) OmenOutcomes omenOutcomes) {
    /** 측정된 서버 응답에는 등급 비교용 입력의 원본 화폐를 포함하지 않는다. */
    public Step(
        String action,
        List<String> omens,
        List<Integer> acquired,
        List<Integer> lost,
        Integer socketIndex,
        Chance chance,
        Double expectedUses,
        Integer maxUses) {
      this(action, omens, acquired, lost, socketIndex, chance, expectedUses, maxUses, null, null);
    }

    /** 화폐 등급을 바꿔도 일반 추첨과 희소 티어를 노린 원래 추첨을 구별한다. */
    public Step(
        String action,
        List<String> omens,
        List<Integer> acquired,
        List<Integer> lost,
        Integer socketIndex,
        Integer maxUses,
        String originalAction) {
      this(action, omens, acquired, lost, socketIndex, null, null, maxUses, originalAction, null);
    }

    /** 고정 경로 입력은 확률을 받지 않고 실제 작업대 분포에서 다시 계산한다. */
    public Step(
        String action,
        List<String> omens,
        List<Integer> acquired,
        List<Integer> lost,
        Integer socketIndex) {
      this(action, omens, acquired, lost, socketIndex, null, null, null);
    }

    /** 고정 재생 시에는 이전 계산값 대신 같은 베이스에서 허용한 최대 시도 횟수만 받는다. */
    public Step(
        String action,
        List<String> omens,
        List<Integer> acquired,
        List<Integer> lost,
        Integer socketIndex,
        Integer maxUses) {
      this(action, omens, acquired, lost, socketIndex, null, null, maxUses);
    }

    /** 누적 확률을 갖는 카오스 시도와 동일 화폐의 단회 사용을 구별한다. */
    private boolean boundedChaos() {
      return maxUses != null
          && RecipeMoves.baseMove(move()).action().equals(WorkbenchCurrency.CHAOS.name());
    }

    /** 선택한 반복 한도와 원본 화폐가 실제로 비교 가능한 제작인지 확인한다. */
    public boolean validGradeSelection() {
      if (action == null || action.isBlank()) return false;
      var base = RecipeMoves.baseMove(new Move(action));
      if (maxUses != null
          && (maxUses != DivineRepeatEstimate.MAX_USES
              || !Set.of("CHAOS", "DIVINE").contains(base.action())
              || base.action().equals("CHAOS") && !omens.isEmpty())) return false;
      return originalAction == null
          || (!originalAction.isBlank()
              && !originalAction.equals(action)
              && base.equals(RecipeMoves.baseMove(new Move(originalAction))));
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

    /** 표시용 3분류를 추가한다. 다음 단계나 전체 경로의 성공 확률은 별도로 유지한다. */
    private Step withOmenOutcomes(OmenOutcomes results) {
      return new Step(
          action,
          omens,
          acquired,
          lost,
          socketIndex,
          results.success(),
          expectedUses,
          maxUses,
          null,
          results);
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
      List<Integer> stateIndices,
      @JsonInclude(JsonInclude.Include.NON_NULL) ProbabilityBounds probabilityBounds) {
    /** 시작부터 이미 달성한 경로 등 정확한 확률에는 추가 경계가 없다. */
    public Route(
        int cardIndex,
        List<Integer> order,
        List<Step> steps,
        Double probability,
        List<Integer> stateIndices) {
      this(cardIndex, order, steps, probability, stateIndices, null);
    }
  }

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
  private record Mass(RevealTierMass weight, Witness witness) {}

  /** 같은 경로의 서로 다른 실제 아이템별 질량과, 그 경로에 빠진 분기가 있는지를 보존한다. */
  private static final class Distribution {
    final Map<ItemState, Mass> masses = new LinkedHashMap<>();
    boolean complete = true;
    // 검증에는 원래 질량을 유지하고, 표시 확률에서만 미적중 준비 사건에 조건을 건다.
    Fraction preparationFactor = Fraction.ONE;
    // rawCap은 이 관측 경로에 올 수 있는 무조건부 최대 질량, displayedCap은 준비를 조건화한 이전 단계의 상한이다.
    Fraction rawCap = Fraction.ONE;
    Fraction displayedCap = Fraction.ONE;
    Fraction factorLow = Fraction.ONE;
    Fraction factorHigh = Fraction.ONE;
    RecipeProbabilityRange parentLiveBounds;
    Fraction parentDisplayedCap;
    Fraction parentLiveMass;
    Double divineUses;
    Double divineWithinLimit;
    final PreparationMass incoming;

    /** incoming은 같은 행동에 속한 모든 관측 분기가 공유한다. 시작 분포에는 이전 행동이 없다. */
    Distribution(PreparationMass incoming) {
      this.incoming = incoming;
    }

    /** 미확정 질량은 null로 유지한다. 서로 배타적인 같은 상태 결과만 합친다. */
    void add(ItemState item, RevealTierMass weight, Witness parent, RecipeStateGroups groups) {
      masses.compute(
          groups.key(item),
          (key, existing) ->
              existing == null
                  ? new Mass(weight, new Witness(item, parent))
                  : new Mass(existing.weight.plus(weight), existing.witness));
    }

    /** 같은 관측 단계에 들어오는 모든 아이템의 원래 확률 질량이다. */
    RevealTierMass totalWeight() {
      var total = RevealTierMass.known(Fraction.ZERO);
      for (var mass : masses.values()) total = total.plus(mass.weight);
      return total;
    }
  }

  /** 훼손에서는 목표 번호마다 독립적으로 선택할 수 있다. 일반 추첨은 한 행동의 모든 목표가 같은 분모를 사용한다. */
  private record Trial(Move move, int target) {}

  /** 같은 행동이 가능한 질량과, 그중 목표 없이 승급하거나 임시 칸을 채운 모든 결과의 질량이다. */
  private static final class PreparationMass {
    // covered는 완전한 부모의 질량과 일부만 열거한 부모의 확인된 결과 질량이다.
    RevealTierMass covered = RevealTierMass.known(Fraction.ZERO);
    RevealTierMass firstCovered = RevealTierMass.known(Fraction.ZERO);
    final Map<Step, RevealTierMass> firstWeights = new HashMap<>();
    RevealTierMass missedProof = RevealTierMass.known(Fraction.ZERO);
    Fraction eligible = Fraction.ZERO;
    Fraction missed = Fraction.ZERO;
    // 첫 시도 확률은 실제 시작 분포를 따르고 비용 기준이 되는 목표 티어는 별도로 보존한다.
    final Set<String> chaosTargetTiers = new HashSet<>();
    boolean firstKnown = true;
    boolean dummy;
    boolean complete = true;
  }

  private final RecipeTransitions transitions;
  private final RecipeMoves moves;
  private final RecipeStateGroups stateGroups;
  private final WorkbenchSimulator simulator;
  private final ItemStateValidator slots;
  private final RouteCards.Card card;
  private final Predicate<ItemState> matchesGoal;
  private final boolean presenceOnly;
  private final Integer minimumQuality;
  private final Set<String> targets = new TreeSet<>();
  private final Set<String> lowIds = new HashSet<>();

  /** 같은 카드 안에서 이미 구한 목표 번호별 빈도와 장착 후 추첨 풀을 열어 주는 증강물이다. */
  private final Map<Integer, RouteCards.Frequency> priorities = new HashMap<>();

  private final Map<Integer, Set<String>> augmentIds = new HashMap<>();
  private final Map<Integer, Set<String>> allowed = new TreeMap<>();
  private final int cardIndex;
  private final List<Route> routes = new ArrayList<>();
  private final Map<ItemState, Integer> stateIndices;
  private final List<Step> fixedSteps;

  /** 첫 카오스 방식을 선택한 탐색만 희귀 한 옵션 준비를 수행한다. */
  private final boolean seekFirstChaos;

  /** 하나의 요청 안에서 모든 카드와 고정 재생 분기가 공유하는 1회 옵션 분류 예산이다. */
  private static final class OmenBudget {
    int left;

    OmenBudget(int total) {
      left = total;
    }
  }

  /** 이 단계에 도달해 선택한 절사 조합을 시도할 수 있던 한 실제 부모 상태다. */
  private record OmenParent(
      Mass prior, Map<Integer, String> held, Set<String> protectedIds, List<Choice> intents) {}

  private final OmenBudget omenBudget;
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
      List<Step> fixedSteps,
      boolean seekFirstChaos,
      Set<String> numericSourceStatIds,
      OmenBudget omenBudget) {
    this.omenBudget = omenBudget;
    this.seekFirstChaos = seekFirstChaos;
    this.stateIndices = stateIndices;
    this.fixedSteps = fixedSteps;
    this.presenceOnly = presenceOnly;
    this.minimumQuality = minimumQuality;
    this.simulator = simulator;
    slots = new ItemStateValidator(simulator.catalog());
    transitions = new RecipeTransitions(simulator, presenceOnly, numericSourceStatIds);
    moves = new RecipeMoves(simulator);
    this.card = card;
    this.cardIndex = cardIndex;
    this.matchesGoal = matchesGoal;
    remaining = outcomeBudget;
    for (var opt : card.options()) {
      allowed.put(opt.number(), opt.tiers().keySet());
      var augments = new HashSet<String>();
      for (var sources : opt.tiers().values())
        for (var source : sources) {
          if (source.label().equals("A")) augments.add(source.id());
          if (source.frequency() != null)
            priorities.merge(
                opt.number(), source.frequency(), (a, b) -> a.compareTo(b) < 0 ? a : b);
        }
      augmentIds.put(opt.number(), augments);
      targets.addAll(opt.tiers().keySet());
      opt.tiers()
          .forEach(
              (id, sources) -> {
                if (sources.stream().anyMatch(s -> s.frequency() == RouteCards.Frequency.L))
                  lowIds.add(id);
              });
    }
    stateGroups =
        new RecipeStateGroups(
            simulator.catalog(), card, targets, presenceOnly, numericSourceStatIds);
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
    return calculate(
        simulator, start, cards, matchesGoal, outcomeBudget, presenceOnly, minimumQuality, null);
  }

  /** 활성 수치 목표의 원본 stat을 전달할 수 있으면 다른 더미 옵션의 굴림을 합산한다. */
  public static Result calculate(
      WorkbenchSimulator simulator,
      ItemState start,
      List<RouteCards.Card> cards,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget,
      boolean presenceOnly,
      Integer minimumQuality,
      Set<String> numericSourceStatIds) {
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
    var omenBudget = new OmenBudget(outcomeBudget);
    for (int i = 0; i < cards.size(); i++) {
      int share = budget / (cards.size() - i);
      var card = cards.get(i);
      boolean canRepeat = canSeekChaos(card, start);
      int reserve = canRepeat ? share / 3 : 0;
      for (boolean repeat : (canRepeat ? List.of(false, true) : List.of(false))) {
        // 일반 획득과 시작 카오스에 각각 기회를 준다. 일반 탐색의 잔여 예산은 반복에 쓸 수 있다.
        int allocation = repeat ? share : share - reserve;
        var search =
            new RecipeRoutePlanner(
                simulator,
                i,
                card,
                matchesGoal,
                allocation,
                presenceOnly,
                minimumQuality,
                stateIndices,
                null,
                repeat,
                numericSourceStatIds,
                omenBudget);
        var root = new Distribution(null);
        root.add(start, RevealTierMass.known(Fraction.ONE), null, search.stateGroups);
        search.explore(List.of(), root);
        routes.addAll(search.routes);
        complete &= search.complete;
        int used = allocation - search.remaining;
        budget -= used;
        share -= used;
      }
    }
    routes.sort(
        Comparator.comparing(Route::probability, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                (Route r) -> r.probabilityBounds() == null ? 0 : r.probabilityBounds().lower(),
                Comparator.reverseOrder())
            .thenComparingInt(r -> r.steps().size())
            .thenComparingInt(Route::cardIndex)
            .thenComparing(r -> r.steps().toString()));
    return new Result(List.copyOf(routes), complete, List.copyOf(stateIndices.keySet()));
  }

  /** 시작부터 지켜야 하는 비분열 L이 없고 새로 뽑을 일반·증강 목표가 있을 때에만 한 옵션 카오스를 준비한다. */
  private static boolean canSeekChaos(RouteCards.Card card, ItemState start) {
    var held =
        start.explicits().stream()
            .map(m -> m.modifierId())
            .collect(java.util.stream.Collectors.toSet());
    var mutable =
        start.explicits().stream()
            .filter(m -> !m.fractured())
            .map(m -> m.modifierId())
            .collect(java.util.stream.Collectors.toSet());
    boolean missing = false;
    for (var option : card.options()) {
      if (option.tiers().entrySet().stream()
          .anyMatch(
              t ->
                  mutable.contains(t.getKey())
                      && t.getValue().stream()
                          .anyMatch(s -> s.frequency() == RouteCards.Frequency.L))) return false;
      if (Collections.disjoint(option.tiers().keySet(), held)
          && option.tiers().values().stream()
              .flatMap(List::stream)
              .anyMatch(s -> s.label().equals("B") || s.label().equals("A"))) missing = true;
    }
    return missing;
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
    return evaluateFixed(
        simulator,
        start,
        card,
        matchesGoal,
        outcomeBudget,
        presenceOnly,
        minimumQuality,
        selectedSteps,
        null);
  }

  /** 고정 재생도 같은 목표 행에서 요구하는 수치만 세어 탐색과 같은 질량을 계산한다. */
  public static Result evaluateFixed(
      WorkbenchSimulator simulator,
      ItemState start,
      RouteCards.Card card,
      Predicate<ItemState> matchesGoal,
      int outcomeBudget,
      boolean presenceOnly,
      Integer minimumQuality,
      List<Step> selectedSteps,
      Set<String> numericSourceStatIds) {
    if (outcomeBudget < 1)
      throw new IllegalArgumentException("Positive route outcome budget required");
    if (selectedSteps.stream()
        .anyMatch(
            s ->
                RecipeMoves.baseMove(s.move()).action().equals(WorkbenchCurrency.CHAOS.name())
                    && s.omens().isEmpty() != s.boundedChaos())) {
      return new Result(List.of(), true, List.of());
    }
    var states = new LinkedHashMap<ItemState, Integer>();
    boolean explicitRepeat = selectedSteps.stream().anyMatch(Step::boundedChaos);
    if (explicitRepeat && !canSeekChaos(card, start)) return new Result(List.of(), true, List.of());
    boolean mayHitDuringSetup =
        !explicitRepeat
            && canSeekChaos(card, start)
            && !selectedSteps.isEmpty()
            && OneOptionPreparation.choices(start, 0).stream()
                .anyMatch(
                    c -> RecipeMoves.baseMove(selectedSteps.getFirst().move()).equals(c.move()));
    int available = outcomeBudget;
    var omenBudget = new OmenBudget(outcomeBudget);
    boolean verified = true;
    for (boolean seek : (mayHitDuringSetup ? List.of(false, true) : List.of(explicitRepeat))) {
      var planner =
          new RecipeRoutePlanner(
              simulator,
              0,
              card,
              matchesGoal,
              available,
              presenceOnly,
              minimumQuality,
              states,
              List.copyOf(selectedSteps),
              seek,
              numericSourceStatIds,
              omenBudget);
      var initial = new Distribution(null);
      initial.add(start, RevealTierMass.known(Fraction.ONE), null, planner.stateGroups);
      planner.explore(List.of(), initial);
      verified &= planner.complete;
      if (!planner.routes.isEmpty())
        return new Result(
            List.copyOf(planner.routes), planner.complete, List.copyOf(states.keySet()));
      available = planner.remaining;
    }
    return new Result(List.of(), verified, List.of());
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
    Map<Integer, String> representativeTiers = Map.of();
    var successfulMass = RevealTierMass.known(Fraction.ZERO);
    var liveMass = RevealTierMass.known(Fraction.ZERO);
    var children = new LinkedHashMap<Step, Distribution>();
    var preparations = new HashMap<Trial, PreparationMass>();
    var omenParents = new LinkedHashMap<Move, List<OmenParent>>();
    var divine = new DivineRepeatEstimate();
    boolean atFixedEnd = fixedSteps != null && path.size() == fixedSteps.size();
    var required = fixedSteps == null || atFixedEnd ? null : fixedSteps.get(path.size());
    var selected = required == null ? null : required.move();
    boolean hadChaos = path.stream().anyMatch(Step::boundedChaos);
    var selectedTargets = new HashSet<String>();
    if (required != null) {
      for (int number : required.acquired())
        selectedTargets.addAll(allowed.getOrDefault(number, Set.of()));
    }
    for (var entry : current.masses.entrySet()) {
      var item = entry.getValue().witness.state();
      var before = held(item);
      if (before.size() == allowed.size() && matchesGoal.test(item)) {
        var mass = entry.getValue();
        if (seekFirstChaos
            && !hadChaos
            && item.explicits().stream()
                .noneMatch(m -> !m.fractured() && lowIds.contains(m.modifierId()))) continue;
        if (representative == null || earlierAcquiredTier(path, before, representativeTiers)) {
          representative = mass.witness;
          representativeTiers = before;
        }
        successfulMass = successfulMass.plus(mass.weight);
        continue;
      }
      liveMass = liveMass.plus(entry.getValue().weight);
      if (atFixedEnd) continue;
      if (remaining == 0) {
        complete = false;
        skippedStates = true;
        continue;
      }
      var protectedIds = new HashSet<>(before.values());
      protectedIds.retainAll(lowIds);
      boolean seeking =
          seekFirstChaos
              && !hadChaos
              && item.explicits().stream()
                  .noneMatch(m -> !m.fractured() && lowIds.contains(m.modifierId()));
      boolean repeat = seeking && OneOptionPreparation.ready(item);
      var choices = new ArrayList<>(moves.choices(item, card, before));
      if (seeking) {
        // 증강물 풀에서만 얻는 목표도 같은 시작 아이템에 홈을 열고 장착한 뒤 반복할 수 있다.
        var installations =
            choices.stream()
                .filter(
                    c -> {
                      var ids = augmentIds.getOrDefault(c.target(), Set.of());
                      return c.preparation()
                          && !ids.isEmpty()
                          && (ids.contains(c.move().action())
                              || c.move().action().equals(WorkbenchCurrency.ARTIFICER.name()));
                    })
                .toList();
        if (!repeat) {
          choices.clear();
          for (var opt : card.options()) {
            if (!before.containsKey(opt.number()))
              OneOptionPreparation.choices(item, opt.number()).stream()
                  .map(Choice::asLowPreparation)
                  .forEach(choices::add);
          }
        } else {
          choices.removeIf(
              c ->
                  !c.move().action().equals(WorkbenchCurrency.CHAOS.name())
                      || !c.move().omens().isEmpty());
          boolean wantsLow = choices.stream().anyMatch(c -> c.purpose() == Purpose.LOW_CHAOS);
          if (wantsLow) choices.removeIf(c -> c.purpose() != Purpose.LOW_CHAOS);
          else {
            var best =
                choices.stream()
                    .map(c -> priorities.get(c.target()))
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            choices.removeIf(c -> priorities.get(c.target()) != best);
          }
        }
        choices.addAll(installations);
      } else
        choices.removeIf(
            c ->
                c.purpose() == Purpose.LOW_CHAOS
                    || (c.move().omens().isEmpty()
                        && RecipeMoves.baseMove(c.move())
                            .action()
                            .equals(WorkbenchCurrency.CHAOS.name())));
      if (required != null
          && required.boundedChaos()
              != (repeat
                  && RecipeMoves.baseMove(selected)
                      .action()
                      .equals(WorkbenchCurrency.CHAOS.name()))) continue;
      if (!seeking
          && minimumQuality != null
          && minimumQuality
              > (item.catalystQuality() == null ? 0 : item.catalystQuality().amount())) {
        for (var currency : WorkbenchCurrency.values()) {
          if (currency.catalystType() != null)
            choices.add(new Choice(0, new Move(currency.name()), true, null));
        }
      }
      if (!seeking && before.size() == allowed.size()) {
        // 한 번 재굴림한 뒤에도 미달이면 이 실행열은 실패다. 무한 재시도의 성공률을 가정하지 않는다.
        choices.addAll(RecipeMoves.rarityPreparation(0, item));
        if (!presenceOnly
            && !item.explicits().isEmpty()
            && (required != null
                || item.rarity() != ItemState.Rarity.MAGIC
                || !matchesGoal.test(
                    RecipeStateGroups.withAffixes(item, ItemState.Rarity.RARE, item.explicits())))
            && (path.isEmpty() || !path.getLast().action().equals(WorkbenchCurrency.DIVINE.name())))
          choices.add(new Choice(0, new Move(WorkbenchCurrency.DIVINE.name()), true, null));
      }
      extend(
          item,
          before,
          protectedIds,
          choices,
          entry.getValue(),
          current.complete,
          children,
          preparations,
          omenParents,
          divine,
          selected,
          selectedTargets,
          repeat,
          required == null ? null : required.originalAction());
    }
    var live = liveMass.exact();
    var liveBounds =
        RecipeProbabilityRange.event(
            liveMass, current.complete, current.rawCap, successfulMass.minimum());
    var successRange =
        RecipeProbabilityRange.event(
            successfulMass, current.complete, current.rawCap, liveMass.minimum());
    var success = successfulMass.exact();
    if (representative != null && (fixedSteps == null || atFixedEnd)) {
      var completed = path;
      var range =
          successRange.displayed(current.factorLow, current.factorHigh, current.displayedCap);
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
        // 한 번 적중한 질량은 반복 완료의 하한이다. 이후 재시도의 상한은 첫 시도에 들어온 모든 질량이다.
        range = new RecipeProbabilityRange(range.lower(), current.parentDisplayedCap);
        steps.set(
            steps.size() - 1,
            path.getLast()
                .measured(
                    Chance.measured(hit, successRange.given(current.parentLiveBounds)),
                    current.divineUses,
                    DivineRepeatEstimate.MAX_USES));
        completed = List.copyOf(steps);
      }
      var order = path.stream().flatMap(s -> s.acquired().stream()).toList();
      if (routeProbability == null) routeProbability = decimal(range.exact());
      if (routeProbability == null
          || completed.stream()
              .anyMatch(s -> s.omenOutcomes() != null && !s.omenOutcomes().complete()))
        complete = false;
      routes.add(
          new Route(
              cardIndex,
              order,
              completed,
              routeProbability,
              representative.indices(stateIndices),
              routeProbability == null ? range.wire() : null));
    }
    // 유한 예산이어도 긴 반복 분기만 우선하지 않도록 직접 획득한 후보를 준비 후보보다 먼저 본다.
    var omenChances =
        measureWhittling(
            children, omenParents, required, liveMass, current.complete && !skippedStates);
    var ordered = new ArrayList<>(children.entrySet());
    ordered.sort(Comparator.comparing(e -> e.getKey().acquired().isEmpty()));
    for (var child : ordered) {
      var step = child.getKey();
      if (required != null
          && (!step.acquired().equals(required.acquired())
              || !step.lost().equals(required.lost())
              || (required.maxUses() != null
                  && !step.action().equals(WorkbenchCurrency.DIVINE.name())
                  && !Objects.equals(step.maxUses(), required.maxUses())))) continue;
      var nextStates = child.getValue();
      var preparation = nextStates.incoming;
      nextStates.complete &= !skippedStates && preparation.complete;
      nextStates.preparationFactor = current.preparationFactor;
      nextStates.parentLiveMass = live;
      nextStates.parentLiveBounds = liveBounds;
      nextStates.parentDisplayedCap =
          liveBounds.displayed(current.factorLow, current.factorHigh, current.displayedCap).upper();
      nextStates.factorLow = current.factorLow;
      nextStates.factorHigh = current.factorHigh;
      var weight = nextStates.totalWeight();
      var eventBounds = measuredEvent(weight, preparation.covered, nextStates.complete, liveBounds);
      nextStates.rawCap = eventBounds.upper();
      if (step.acquired().isEmpty() && step.lost().isEmpty() && preparation.dummy) {
        // 실제 후속 제작이 가능한 더미만 남았더라도 모든 미적중 더미를 분모에 유지한다.
        nextStates.preparationFactor =
            product(current.preparationFactor, ratio(preparation.eligible, preparation.missed));
        // 부분 열거에서 eligible/missed 자체는 확정 배수가 아니다. 실제 미적중은 적격 질량의 부분집합이다.
        var knownFactor =
            nextStates.complete ? ratio(preparation.eligible, preparation.missed) : null;
        if (knownFactor != null) {
          nextStates.factorLow = product(current.factorLow, knownFactor);
          nextStates.factorHigh = product(current.factorHigh, knownFactor);
        } else {
          var high = ratio(liveBounds.upper(), preparation.missedProof.minimum());
          nextStates.factorHigh = product(current.factorHigh, high);
        }
      }
      nextStates.displayedCap =
          eventBounds
              .displayed(nextStates.factorLow, nextStates.factorHigh, nextStates.parentDisplayedCap)
              .upper();
      if (step.boundedChaos()) {
        boolean firstKnown = current.complete && !skippedStates && preparation.firstKnown;
        var firstWeight =
            preparation.firstWeights.getOrDefault(step, RevealTierMass.known(Fraction.ZERO));
        var first = firstKnown ? ratio(firstWeight.exact(), live) : null;
        // 손실만 관측되면 원래 노린 목표 중 하나, 여러 번호를 획득하면 그 번호들을 함께 충족하는 티어를 기준으로 한다.
        var tiers = new HashSet<>(preparation.chaosTargetTiers);
        for (int number : step.acquired()) tiers.retainAll(allowed.get(number));
        var expected =
            BoundedChaosRepeat.emptyBaseExpectedUses(
                simulator,
                nextStates.masses.values().iterator().next().witness.state(),
                WorkbenchCurrency.valueOf(step.action()),
                tiers,
                step.maxUses());
        var firstBounds =
            measuredEvent(firstWeight, preparation.firstCovered, firstKnown, liveBounds)
                .given(liveBounds);
        step = step.measured(Chance.measured(first, firstBounds), expected, step.maxUses());
      } else if (!step.acquired().isEmpty()
          || !step.lost().isEmpty()
          || step.action().equals("REVEAL")
          || step.action().equals(WorkbenchCurrency.ANNULMENT.name())) {
        var hit = nextStates.complete ? ratio(weight.exact(), live) : null;
        step =
            step.measured(
                Chance.measured(hit, eventBounds.given(liveBounds)), null, step.maxUses());
      } else if (step.action().equals(WorkbenchCurrency.DIVINE.name())) {
        nextStates.divineUses = divine.expectedUses(live, nextStates.complete);
        nextStates.divineWithinLimit = divine.successWithinLimit(live, nextStates.complete);
        step = step.measured(null, 1.0, null);
      }
      var assessment = omenChances.get(child.getKey());
      if (assessment != null) {
        step =
            step.withOmenOutcomes(
                assessment.measured(
                    step.chance(),
                    liveBounds,
                    live,
                    current.complete && !skippedStates && preparation.complete,
                    weight));
      }
      var next = new ArrayList<>(path);
      next.add(step);
      int progress = held(child.getValue().masses.keySet().iterator().next()).size();
      pending.add(new Pending(List.copyOf(next), child.getValue(), progress, nextSequence++));
    }
  }

  /** 실제 반환 후보가 생긴 절사 조합만 한 번 더 확인한다. 서로 다른 목표 후보는 같은 1회 분포를 공유한다. */
  private Map<Step, WhittlingChances> measureWhittling(
      Map<Step, Distribution> children,
      Map<Move, List<OmenParent>> parents,
      Step required,
      RevealTierMass live,
      boolean allParents) {
    var result = new HashMap<Step, WhittlingChances>();
    if (parents.isEmpty()) return result;
    for (var group : parents.entrySet()) {
      var move = group.getKey();
      var interests = new LinkedHashMap<Step, Set<String>>();
      for (var step : children.keySet()) {
        if (!step.move().equals(move)
            || required != null
                && (!required.acquired().equals(step.acquired())
                    || !required.lost().equals(step.lost()))) continue;
        var ids = new HashSet<>(allowed.get(step.acquired().getFirst()));
        for (int n : step.acquired()) ids.retainAll(allowed.get(n));
        interests.put(step, ids);
        result.put(step, new WhittlingChances());
      }
      if (interests.isEmpty()) continue;
      var covered = RevealTierMass.known(Fraction.ZERO);
      for (var parent : group.getValue()) {
        covered = covered.plus(parent.prior.weight);
        if (parent.intents.isEmpty()) {
          interests
              .keySet()
              .forEach(
                  s -> result.get(s).failure = result.get(s).failure.plus(parent.prior.weight));
          continue;
        }
        if (omenBudget.left == 0) {
          interests.keySet().forEach(s -> result.get(s).complete = false);
          continue;
        }
        var item = parent.prior.witness.state;
        var heldGoals = Set.copyOf(parent.held.values());
        var raw = WhittlingOutcomeLaw.calculate(simulator, item, move, omenBudget.left);
        omenBudget.left -= Math.max(1, raw.outcomes().size());
        if (!raw.complete()) interests.keySet().forEach(s -> result.get(s).complete = false);
        if (raw.complete() && raw.outcomes().isEmpty()) {
          interests
              .keySet()
              .forEach(
                  s -> result.get(s).failure = result.get(s).failure.plus(parent.prior.weight));
          continue;
        }
        for (var outcome : raw.outcomes()) {
          var mass = transitionMass(parent.prior.weight, outcome);
          var observed =
              observed(move, item, parent.held, parent.protectedIds, parent.intents, outcome, null);
          String added = WhittlingOutcomeLaw.addedId(item, outcome);
          for (var interest : interests.entrySet()) {
            var counts = result.get(interest.getKey());
            if (interest.getKey().equals(observed)) counts.success = counts.success.plus(mass);
            else
              switch (WhittlingRecovery.classifyMiss(
                  simulator,
                  WorkbenchCurrency.valueOf(move.action()),
                  move.omens(),
                  outcome.state(),
                  added,
                  outcome.removedModifierIds(),
                  heldGoals,
                  interest.getValue())) {
                case RECOVERABLE -> counts.recoverable = counts.recoverable.plus(mass);
                case FAILURE -> counts.failure = counts.failure.plus(mass);
                case UNKNOWN -> counts.complete = false;
              }
          }
        }
      }
      // 완전히 처리한 부모 중 해당 조합이 제시되지 않은 상태는 이 경로에 진입할 수 없다.
      var total = live.exact();
      var offered = covered.exact();
      for (var step : interests.keySet()) {
        var counts = result.get(step);
        if (allParents
            && children.get(step).incoming.complete
            && offered != null
            && total != null) {
          counts.failure =
              counts.failure.plus(
                  RevealTierMass.known(RecipeProbabilityRange.subtract(total, offered)));
        } else counts.complete = false;
      }
    }
    return result;
  }

  /** 전체 분모에서 완전히 검사한 질량과 적중 최대값의 차이는 이 사건에 속할 수 없는 질량이다. */
  private static RecipeProbabilityRange measuredEvent(
      RevealTierMass hits, RevealTierMass covered, boolean complete, RecipeProbabilityRange live) {
    var max = hits.maximum();
    var failure =
        complete || max == null
            ? Fraction.ZERO
            : RecipeProbabilityRange.subtract(
                RecipeProbabilityRange.bound(covered.minimum(), false),
                RecipeProbabilityRange.bound(max, true));
    return RecipeProbabilityRange.event(hits, complete, live.upper(), failure);
  }

  /** 완주한 분기의 실제 획득 순서대로 더 낮은 레벨부터 나오는 티어를 대표 필름에 택한다. */
  private boolean earlierAcquiredTier(
      List<Step> path, Map<Integer, String> candidate, Map<Integer, String> previous) {
    for (var step : path) {
      for (int number : step.acquired()) {
        var nextId = candidate.get(number);
        var oldId = previous.get(number);
        if (nextId.equals(oldId)) continue;
        var next = simulator.catalog().find(nextId).orElseThrow();
        var old = simulator.catalog().find(oldId).orElseThrow();
        if (next.requiredItemLevel() != old.requiredItemLevel())
          return next.requiredItemLevel() < old.requiredItemLevel();
        if (next.tier() > 0 && old.tier() > 0 && next.tier() != old.tier())
          return next.tier() > old.tier();
      }
    }
    return false;
  }

  /** 현재 상태에서 같은 행동을 한 번만 전개하고, 같은 목표를 지킬 수 있는 대안이 있으면 위험 선택을 뺀다. */
  private void extend(
      ItemState item,
      Map<Integer, String> before,
      Set<String> protectedIds,
      List<Choice> choices,
      Mass prior,
      boolean parentComplete,
      Map<Step, Distribution> children,
      Map<Trial, PreparationMass> preparations,
      Map<Move, List<OmenParent>> omenParents,
      DivineRepeatEstimate divine,
      Move selected,
      Set<String> selectedTargets,
      boolean repeat,
      String originalAction) {
    var byMove = new LinkedHashMap<Move, List<Choice>>();
    for (var c : choices) byMove.computeIfAbsent(c.move(), ignored -> new ArrayList<>()).add(c);
    if (selected != null)
      byMove = forReplay(byMove, selected, !protectedIds.isEmpty(), originalAction);
    var trials = new LinkedHashMap<Trial, List<Choice>>();
    for (var entry : byMove.entrySet())
      for (var intent : entry.getValue()) {
        boolean desecration =
            intent.purpose() == Purpose.DESECRATION || intent.purpose() == Purpose.LOW_REVEAL;
        if (desecration
            && selected != null
            && selected.equals(entry.getKey())
            && !selectedTargets.isEmpty()
            && Collections.disjoint(intent.eligibleTiers(), selectedTargets)) continue;
        var trial = new Trial(entry.getKey(), desecration ? intent.target() : 0);
        trials.computeIfAbsent(trial, k -> new ArrayList<>()).add(intent);
      }
    var laws = new LinkedHashMap<Trial, Law>();
    var firstLaws = new HashMap<Trial, Law>();
    var safeTargets = new HashSet<Integer>();
    boolean allConsidered = true;
    for (var entry : trials.entrySet()) {
      var trial = entry.getKey();
      var move = trial.move();
      if (selected != null
          && !move.equals(selected)
          && laws.entrySet().stream()
              .filter(l -> l.getKey().move().equals(selected))
              .noneMatch(
                  l -> !Collections.disjoint(l.getValue().removableModifierIds(), protectedIds)))
        break;
      if (remaining == 0) {
        complete = false;
        allConsidered = false;
        var skipped = preparations.computeIfAbsent(trial, k -> new PreparationMass());
        skipped.complete = false;
        skipped.firstKnown = false;
        continue;
      }
      var intents = entry.getValue();
      boolean prepares = intents.stream().anyMatch(Choice::preparation);
      var action =
          move.socketIndex() == null ? move.action() : move.action() + "@" + move.socketIndex();
      // 고정된 적중 단계는 해당 번호의 모든 티어만 열거한다. 전이 법칙은 여전히 전체 작업대 후보를 분모로 쓴다.
      boolean replayHit = move.equals(selected) && !selectedTargets.isEmpty();
      var preparation = preparations.computeIfAbsent(trial, k -> new PreparationMass());
      Law law;
      if (repeat && RecipeMoves.baseMove(move).action().equals(WorkbenchCurrency.CHAOS.name())) {
        // 가장 우선인 목표 번호 중 하나가 붙으면 멈춘다. 같은 번호의 모든 허용 티어를 포함한다.
        // 시작 M/H의 제거는 관측 결과 lost에 남긴다. 새 목표 전에 기존 M/H가 빠져도 반복은 이어진다.
        var interested = new HashSet<String>();
        for (var intent : intents)
          interested.addAll(
              intent.purpose() == Purpose.LOW_CHAOS
                  ? intent.eligibleTiers()
                  : allowed.get(intent.target()));
        preparation.chaosTargetTiers.addAll(interested);
        interested.addAll(protectedIds);
        var bounded =
            BoundedChaosRepeat.calculate(
                simulator,
                transitions,
                item,
                WorkbenchCurrency.valueOf(move.action()),
                move.omens(),
                interested,
                protectedIds,
                DivineRepeatEstimate.MAX_USES,
                remaining);
        law = bounded.hits();
        firstLaws.put(trial, bounded.firstHits());
        preparation.firstKnown &= bounded.firstHits().complete();
        remaining -= Math.max(1, bounded.work());
      } else {
        var interested = replayHit ? selectedTargets : targets;
        if (intents.stream()
            .anyMatch(
                c ->
                    c.purpose() == Purpose.LOW_BONE
                        || c.purpose() == Purpose.LOW_REVEAL
                        || c.purpose() == Purpose.DESECRATION)) {
          interested = new HashSet<>();
          for (var intent : intents) interested.addAll(intent.eligibleTiers());
        }
        if (intents.stream().anyMatch(c -> c.purpose() == Purpose.LOW_BONE))
          law = transitions.prepareDesecration(item, action, move.omens(), interested, remaining);
        else
          law =
              transitions.successors(
                  item, action, move.omens(), interested, remaining, prepares && !replayHit);
        remaining -= Math.max(1, law.outcomes().size());
      }
      preparation.complete &= law.complete();
      if (move.action().equals(WorkbenchCurrency.DIVINE.name()))
        divine.add(prior.weight.exact(), law, matchesGoal);
      if (prepares && !replayHit) countDummyMisses(preparation, item, before, prior.weight, law);

      complete &= law.complete();
      laws.put(trial, law);
      if (!protectedIds.isEmpty()
          && Collections.disjoint(law.removableModifierIds(), protectedIds)) {
        for (Choice c : intents) {
          if (law.outcomes().stream().anyMatch(o -> reaches(c, before, o, protectedIds)))
            safeTargets.add(c.target());
        }
      }
    }
    for (var entry : laws.entrySet()) {
      var trial = entry.getKey();
      var move = trial.move();
      if (selected != null && !selected.equals(move)) continue;
      var law = entry.getValue();
      boolean safe = Collections.disjoint(law.removableModifierIds(), protectedIds);
      if (!safe && !allConsidered) {
        preparations.get(trial).complete = false;
        preparations.get(trial).firstKnown = false;
        continue;
      }
      var intents =
          trials.get(trial).stream()
              .filter(c -> safe || !safeTargets.contains(c.target()))
              .toList();
      // 확인되지 않은 안전 대안이 있으면 불가피한 L 위험이라고 단정할 수 없다.
      if (!safe
          && laws.entrySet().stream()
              .anyMatch(
                  other ->
                      !other.getValue().complete()
                          && Collections.disjoint(
                              other.getValue().removableModifierIds(), protectedIds)
                          && trials.get(other.getKey()).stream()
                              .anyMatch(
                                  candidate ->
                                      intents.stream()
                                          .anyMatch(
                                              intent -> intent.target() == candidate.target())))) {
        preparations.get(trial).complete = false;
        preparations.get(trial).firstKnown = false;
        continue;
      }
      if (move.omens().contains(WorkbenchOmen.WHITTLING))
        omenParents
            .computeIfAbsent(move, k -> new ArrayList<>())
            .add(new OmenParent(prior, before, protectedIds, intents));
      boolean bounded = firstLaws.containsKey(trial);
      Integer maxUses = bounded ? DivineRepeatEstimate.MAX_USES : null;
      var preparation = preparations.get(trial);
      if (law.complete()) preparation.covered = preparation.covered.plus(prior.weight);
      if (bounded) {
        var firstLaw = firstLaws.get(trial);
        if (firstLaw.complete())
          preparation.firstCovered = preparation.firstCovered.plus(prior.weight);
        for (var outcome : firstLaw.outcomes()) {
          var firstWeight = transitionMass(prior.weight, outcome);
          if (!firstLaw.complete())
            preparation.firstCovered = preparation.firstCovered.plus(firstWeight);
          var step = observed(move, item, before, protectedIds, intents, outcome, maxUses);
          if (step != null) preparation.firstWeights.merge(step, firstWeight, RevealTierMass::plus);
        }
      }
      for (var outcome : law.outcomes()) {
        var weight = transitionMass(prior.weight, outcome);
        if (!law.complete()) preparation.covered = preparation.covered.plus(weight);
        var step = observed(move, item, before, protectedIds, intents, outcome, maxUses);
        if (step == null) continue;
        var child =
            children.computeIfAbsent(step, ignored -> new Distribution(preparations.get(trial)));
        child.complete &= parentComplete && law.complete();

        child.add(outcome.state(), weight, prior.witness, stateGroups);
      }
    }
  }

  /** 같은 원래 분모를 유지하며 확인된 일반 확률 또는 특정 훼손 티어의 조건부 질량을 곱한다. */
  private static RevealTierMass transitionMass(RevealTierMass parent, Outcome outcome) {
    var reveal = outcome.reveal();
    return reveal == null
        ? parent.times(outcome.probability())
        : parent.select(reveal.group(), reveal.tier(), reveal.conditional());
  }

  /** 같은 상태를 만들었어도 보유 옵션이 제거된 뒤 다시 붙었다면 획득 및 제거에 포함한다. */
  private Step observed(
      Move move,
      ItemState item,
      Map<Integer, String> before,
      Set<String> protectedIds,
      List<Choice> intents,
      Outcome outcome,
      Integer maxUses) {
    if ((outcome.state().equals(item)
            && !move.action().equals(WorkbenchCurrency.DIVINE.name())
            && Collections.disjoint(outcome.removedModifierIds(), before.values()))
        || !Collections.disjoint(outcome.removedModifierIds(), protectedIds)) return null;
    if (intents.stream().noneMatch(c -> reaches(c, before, outcome, protectedIds))) return null;
    var after = held(outcome.state());
    var lost =
        before.entrySet().stream()
            .filter(
                e ->
                    !after.containsKey(e.getKey())
                        || outcome.removedModifierIds().contains(e.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    var acquired =
        after.keySet().stream().filter(n -> !before.containsKey(n) || lost.contains(n)).toList();
    if (move.omens().contains(WorkbenchOmen.WHITTLING) && acquired.isEmpty()) return null;
    return new Step(move, acquired, lost).measured(null, null, maxUses);
  }

  /** 기본 등급 경로에서 합법이었던 한 행동만 선택 등급으로 교체한다. 위험한 제거의 L 보호 여부를 검증할 대안만 함께 둔다. */
  private static LinkedHashMap<Move, List<Choice>> forReplay(
      Map<Move, List<Choice>> choices, Move selected, boolean protectsLow, String originalAction) {
    var selectedBase = RecipeMoves.baseMove(selected);
    var original =
        originalAction == null
            ? null
            : new Move(originalAction, selected.omens(), selected.socketIndex());
    if (original == null) {
      if (choices.containsKey(selected)) original = selected;
      else if (choices.containsKey(selectedBase)) original = selectedBase;
      else
        original =
            choices.keySet().stream()
                .filter(m -> RecipeMoves.baseMove(m).equals(selectedBase))
                .findFirst()
                .orElse(null);
    }
    var intents = choices.getOrDefault(original, List.of());
    var replay = new LinkedHashMap<Move, List<Choice>>();
    if (intents.isEmpty()) return replay;
    replay.put(selected, intents);
    if (!protectsLow) return replay;
    for (var entry : choices.entrySet()) {
      var move = entry.getKey();
      if (RecipeMoves.baseMove(move).equals(selectedBase)) continue;
      var alternatives =
          entry.getValue().stream()
              .filter(c -> intents.stream().anyMatch(i -> i.target() == c.target()))
              .toList();
      if (alternatives.isEmpty()) continue;
      if (RecipeMoves.baseMove(move).action().equals(selectedBase.action()))
        move = new Move(selected.action(), move.omens(), move.socketIndex());
      replay.computeIfAbsent(move, k -> new ArrayList<>()).addAll(alternatives);
    }
    return replay;
  }

  /** 보호·후속 제작 필터보다 먼저, 제거 없는 승급·옵션 추가에서 새 목표가 붙지 않은 질량을 계산한다. */
  private void countDummyMisses(
      PreparationMass prep,
      ItemState item,
      Map<Integer, String> held,
      RevealTierMass prior,
      Law law) {
    if (law.outcomes().isEmpty() || !law.removableModifierIds().isEmpty()) return;
    var first = law.outcomes().getFirst().state();
    if (first.rarity() == item.rarity() && first.explicits().size() <= item.explicits().size())
      return;
    prep.dummy = true;
    prep.eligible = sum(prep.eligible, prior.exact());
    for (var outcome : law.outcomes()) {
      if (outcome.state().explicits().stream()
          .noneMatch(
              m -> targets.contains(m.modifierId()) && !held.containsValue(m.modifierId()))) {
        prep.missed = sum(prep.missed, product(prior.exact(), outcome.probability()));
        prep.missedProof = prep.missedProof.plus(transitionMass(prior, outcome));
      }
    }
  }

  /** 준비 후 지정한 교체가 실제로 가능하고 L을 잃지 않는지 작업대의 완전한 제거 후보와 대조한다. */
  private boolean reaches(
      Choice choice, Map<Integer, String> before, Outcome outcome, Set<String> protectedIds) {
    // 동률 시도는 허용하되 실제로 보유 목표를 지운 절사는 재부착해도 실패다. 안전한 대안 검사에도 같은 조건을 쓴다.
    if (choice.move().omens().contains(WorkbenchOmen.WHITTLING)
        && !Collections.disjoint(outcome.removedModifierIds(), before.values())) return false;
    if (!RecipeMoves.permits(
        choice, before, held(outcome.state()), outcome.removedModifierIds(), lowIds)) return false;
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
