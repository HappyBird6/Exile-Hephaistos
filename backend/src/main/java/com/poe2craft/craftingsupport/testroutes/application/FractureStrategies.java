package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Chance;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.ProbabilityBounds;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Route;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Step;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.math.BigDecimal;
import java.util.*;

/**
 * 같은 아이템과 제작 이력에서 분열을 선택한 경우를 비교한다. 분열 결과는 배타적이지만, 같은 결과에서 선택할 수 있는 여러 제작법은 배타적 확률 사건이 아니다. 따라서 각
 * 결과에서 실제 완주가 확인된 제작법 하나만 골라 합산한다.
 */
public final class FractureStrategies {
  /** 그룹 식별자와 0부터 세는 분열 단계, 시작부터의 전략·무분열 완주 확률, 해당 분열 결과를 관측한 뒤의 완주 확률이다. */
  public record Recommendation(
      String decisionId,
      int stepIndex,
      Chance successChance,
      Chance baselineChance,
      Chance conditionalChance) {}

  /** 같은 실제 행동·관측을 식별한다. 대표 아이템의 굴림이나 경로에 붙인 표시 확률로 무분열 대안을 제외하지 않는다. */
  private record Observation(
      String action,
      Set<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      Integer socket,
      boolean dummy,
      boolean repeatUntilHit,
      Integer maxUses,
      Integer repeatTarget) {
    /** 준비·목표·재시도 선택은 동일하게 유지하고 추정값만 생략한다. */
    static Observation of(Step s) {
      return new Observation(
          s.action(),
          Set.copyOf(s.omens()),
          s.acquired(),
          s.lost(),
          s.socketIndex(),
          s.dummy(),
          s.repeatUntilHit(),
          s.maxUses(),
          s.repeatTarget());
    }
  }

  /** 동일 카드에서 분열하기 전까지 실제로 선택하고 관측한 제작 이력이다. */
  private record History(int card, List<Observation> prefix) {
    /** 주어진 완주 경로에서 결정 직전까지의 이력을 읽는다. */
    static History at(Route route, int depth) {
      return new History(
          route.cardIndex(),
          route.steps().subList(0, depth).stream().map(Observation::of).toList());
    }
  }

  /** 분열 전략은 같은 제작 이력으로 같은 실제 아이템에 도달한 결과끼리만 합산한다. */
  private record Decision(History history, ItemState item) {}

  /** 확인한 완주 질량의 하한·상한이다. 출발점 이후의 조건부 확률로 바꿀 때만 나눈다. */
  private record Mass(double lower, double upper) {
    /** 확률이나 구간이 없으면 가능한 전체 구간을 사용한다. */
    static Mass of(Double probability, ProbabilityBounds bounds) {
      if (probability != null) return new Mass(probability, probability);
      return bounds == null ? new Mass(0, 1) : new Mass(bounds.lower(), bounds.upper());
    }

    /** 시작부터 완주에 이른 질량이며, 분열한 경로에는 실제 추첨 확률이 이미 곱해져 있다. */
    static Mass of(Route route) {
      return of(route.probability(), route.probabilityBounds());
    }

    /** 구조가 확인된 분열 결과에 도달한 누적 질량이다. */
    static Mass of(Chance chance) {
      return chance == null ? new Mass(0, 1) : of(chance.probability(), chance.probabilityBounds());
    }

    /** 같은 결정의 여러 무분열 경로에서 얻을 수 있는 최선의 완주율 경계다. */
    Mass best(Mass other) {
      return new Mass(Math.max(lower, other.lower), Math.max(upper, other.upper));
    }

    /** 표시 가능한 정확값 또는 구간으로 반환한다. */
    Chance wire() {
      return lower == upper
          ? new Chance(lower, null)
          : new Chance(null, new ProbabilityBounds(lower, upper));
    }

    /** 관측한 특정 결과를 분모로 한다. 분모의 하한이 0이면 조건부 상한은 1이다. */
    Chance given(Mass reached) {
      if (lower == upper && reached.lower == reached.upper) {
        return new Chance(Math.min(1, lower / reached.upper), null);
      }
      var lo = Math.max(0, Math.nextDown(lower / reached.upper));
      var hi = reached.lower == 0 ? 1 : Math.min(1, Math.nextUp(upper / reached.lower));
      return new Mass(Math.min(1, lo), hi).wire();
    }
  }

  /** 객체에 계산 상태를 저장하지 않는다. */
  private FractureStrategies() {}

  /** 일반 경로를 유지하고, 모든 추첨 결과가 완주 가능하며 무분열보다 엄격하게 유리한 전략의 결과별 경로만 남긴다. */
  static List<Route> recommend(
      ItemState start, List<Route> routes, List<ItemState> states, boolean complete) {
    var ordinary = new ArrayList<Route>();
    var groups = new LinkedHashMap<Decision, List<Route>>();
    var depths = new TreeSet<Integer>();
    for (var route : routes) {
      int i = 0;
      while (i < route.steps().size() && !isFracture(route.steps().get(i))) i++;
      if (i == route.steps().size()) ordinary.add(route);
      else if (complete) {
        var before = i == 0 ? start : states.get(route.stateIndices().get(i - 1));
        groups
            .computeIfAbsent(
                new Decision(History.at(route, i), before), unused -> new ArrayList<>())
            .add(route);
        depths.add(i);
      }
    }
    if (groups.isEmpty()) return List.copyOf(ordinary);
    var originalBest = new HashMap<Integer, Mass>();
    var comparable = new HashSet<History>();
    var histories = new HashSet<History>();
    groups.keySet().forEach(decision -> histories.add(decision.history));
    for (var route : ordinary) {
      // 같은 원본에서 뼈 등 준비를 처음부터 생략하는 대안도 분열보다 유리할 수 있다.
      originalBest.merge(route.cardIndex(), Mass.of(route), Mass::best);
      for (int depth : depths.headSet(route.steps().size(), true)) {
        var history = History.at(route, depth);
        if (histories.contains(history)) comparable.add(history);
      }
    }
    var selected = new IdentityHashMap<Route, Recommendation>();
    int number = 0;
    for (var entry : groups.entrySet()) {
      var decision = entry.getKey();
      if (!comparable.contains(decision.history)) continue;
      choose(
          "fracture-" + ++number,
          decision.history.prefix.size(),
          decision.item,
          entry.getValue(),
          originalBest.get(decision.history.card),
          selected);
    }
    var output = new ArrayList<>(ordinary);
    for (var route : routes) {
      var recommendation = selected.remove(route);
      if (recommendation != null) output.add(route.withFractureStrategy(recommendation));
    }
    return List.copyOf(output);
  }

  /** 특정 결정점의 가능한 모든 확정 결과마다 완주 확률의 하한이 가장 높은 경로 하나를 고른다. */
  private static void choose(
      String id,
      int depth,
      ItemState before,
      List<Route> candidates,
      Mass baseline,
      Map<Route, Recommendation> selected) {
    var eligible = new LinkedHashSet<String>();
    before.explicits().stream().map(ModifierInstance::modifierId).forEach(eligible::add);
    if (baseline.upper >= 1 || eligible.isEmpty() || eligible.size() != before.explicits().size())
      return;
    var branches = new LinkedHashMap<String, Route>();
    for (var route : candidates) {
      var step = route.steps().get(depth);
      String hit = step.fracturedModifierId();
      var mass = Mass.of(route);
      var reached = Mass.of(step.reachChance());
      var chance = step.chance();
      if (!eligible.contains(hit)
          || chance == null
          || chance.probability() == null
          || chance.probability() != 1.0 / eligible.size()
          || !(mass.lower > 0)
          || step.reachChance() == null
          || !(reached.upper > 0)
          || mass.lower > Math.nextUp(reached.upper)) continue;
      branches.merge(
          hit,
          route,
          (a, b) -> {
            double left = Mass.of(a).lower, right = Mass.of(b).lower;
            if (left != right) return left > right ? a : b;
            return a.steps().size() <= b.steps().size() ? a : b;
          });
    }
    if (!branches.keySet().equals(eligible)) return;
    var lower = BigDecimal.ZERO;
    var upper = BigDecimal.ZERO;
    var guaranteed = BigDecimal.ZERO;
    Mass commonReach = null;
    for (var branch : branches.values()) {
      var mass = Mass.of(branch);
      var reached = Mass.of(branch.steps().get(depth).reachChance());
      // 균등 추첨의 각 결과까지 도달한 질량은 같다. 서로 다른 시작 질량에서 얻은 후속 제작법을 섞지 않는다.
      if (commonReach != null && !commonReach.equals(reached)) return;
      commonReach = reached;
      lower = lower.add(BigDecimal.valueOf(mass.lower));
      upper = upper.add(BigDecimal.valueOf(mass.upper));
      // 이미 double로 반올림한 질량을 다시 합산한다. 비교에서 한 ULP씩 바깥으로 열어 표현 오차를 개선으로 오인하지 않는다.
      guaranteed = guaranteed.add(new BigDecimal(Math.nextDown(mass.lower)));
    }
    if (guaranteed.compareTo(new BigDecimal(Math.nextUp(baseline.upper))) <= 0) return;
    var combined =
        lower.compareTo(upper) == 0
            ? new Chance(Math.min(1, lower.doubleValue()), null)
            : new Mass(
                    Math.max(0, Math.nextDown(lower.doubleValue())),
                    Math.min(1, Math.nextUp(upper.doubleValue())))
                .wire();
    for (var branch : branches.values()) {
      var reached = Mass.of(branch.steps().get(depth).reachChance());
      selected.put(
          branch,
          new Recommendation(id, depth, combined, baseline.wire(), Mass.of(branch).given(reached)));
    }
  }

  /** 작업대의 분열 화폐를 식별한다. */
  private static boolean isFracture(Step step) {
    return WorkbenchCurrency.FRACTURING.name().equals(step.action());
  }
}
