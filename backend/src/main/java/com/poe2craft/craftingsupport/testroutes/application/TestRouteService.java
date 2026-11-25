package com.poe2craft.craftingsupport.testroutes.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** 테스트 탭의 카드 사전 검사와 경로 검증을 현재 작업대·목표 카탈로그에 연결한다. */
public final class TestRouteService {
  /** number는 목표 번호, modifierIds는 허용한 티어, hasLowerBound와 hasUpperBound는 이 옵션에 적용되는 목표 경계다. */
  public record Option(
      int number,
      List<String> modifierIds,
      @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean hasLowerBound,
      @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean hasUpperBound) {}

  /** 경로의 cardIndex로 참조하는 옵션 번호표다. */
  public record Card(List<Option> options) {}

  /** 검증한 카드와 경로를 돌려준다. complete는 카드 생성과 경로 평가가 모두 끝났을 때 참이다. */
  public record View(
      List<Card> cards,
      List<RecipeRoutePlanner.Route> routes,
      boolean complete,
      List<ItemState> states,
      Diagnostics diagnostics) {}

  /** stage는 마지막 계산 단계, outcomeBudget은 적용된 예산, search는 실제 전이 평가 결과다. */
  public record Diagnostics(
      String stage, int outcomeBudget, RecipeRoutePlanner.SearchTrace search) {}

  private final CraftingOperations workbench;
  private final GoalFilterService goals;
  private final BackendSettings.TestRoutes settings;

  /** workbench와 goals를 사용해 입력을 검증하고, settings의 등장 빈도와 계산 예산을 카드·경로 계산에 전달한다. */
  public TestRouteService(
      CraftingOperations workbench, GoalFilterService goals, BackendSettings.TestRoutes settings) {
    this.workbench = workbench;
    this.goals = goals;
    this.settings = settings;
  }

  /** 기본 등급 화폐로 도달할 수 있는 경로를 찾는다. */
  public View calculate(ItemState item, GoalFilter goal) {
    return calculate(item, goal, null, List.of());
  }

  /** cardIndex가 있으면 그 카드의 지정된 제작 단계만 사용해 선택한 화폐 등급으로 재검증한다. */
  public View calculate(
      ItemState item, GoalFilter goal, Integer cardIndex, List<RecipeRoutePlanner.Step> steps) {
    return calculate(item, goal, cardIndex, steps, null);
  }

  /** 품질 40의 기폭제 선택을 새 경로·고정 재생에 같이 적용하며, 기존 경로는 저장된 그룹을 재생할 수 있다. */
  public View calculate(
      ItemState item,
      GoalFilter goal,
      Integer cardIndex,
      List<RecipeRoutePlanner.Step> steps,
      WorkbenchCurrency qualityCatalyst) {
    // compile은 시작 아이템·목표·catalogVersion을 검증한 후 같은 필터 평가기를 재사용한다.
    var evaluate = goals.compileTestRoute(item, goal);
    var minimumQuality = goal.general().quality();
    if (cardIndex == null) {
      for (int i = 0; i < goal.groups().size(); i++) {
        var group = goal.groups().get(i);
        if (!group.disabled() && group.type() != GoalFilter.Type.AND)
          throw new GoalFilterService.InvalidGoal(
              List.of(
                  GoalFilter.Issue.error(
                      "AND_GROUP_REQUIRED",
                      "/goal/groups/" + i + "/type",
                      "Use AND for active target groups.")));
      }
    }
    if (Integer.valueOf(40).equals(minimumQuality)) {
      if (!BaseRegistry.maximumQualityBreach(item.baseItemId()))
        throw new GoalFilterService.InvalidGoal(
            List.of(
                GoalFilter.Issue.error(
                    "UNSUPPORTED_QUALITY_BASE",
                    "/goal/general/quality",
                    "Quality 40 requires an amulet or ring.")));
    } else if (qualityCatalyst != null) {
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error(
                  "QUALITY_40_REQUIRED",
                  "/qualityCatalyst",
                  "A selected catalyst requires the quality 40 goal.")));
    }
    var simulator = workbench.baseSimulator(item);
    var violations = new ItemStateValidator(simulator.forItem(item).catalog()).validate(item);
    if (!violations.isEmpty()) {
      throw new GoalFilterService.InvalidGoal(
          violations.stream()
              .map(v -> GoalFilter.Issue.error(v.code().name(), "/item", v.message()))
              .toList());
    }
    var initial = evaluate.apply(item);
    if (initial == Status.UNKNOWN || initial == Status.UNSUPPORTED) {
      return new View(
          List.of(),
          List.of(),
          false,
          List.of(),
          new Diagnostics(initial.name(), settings.outcomeBudget(), null));
    }
    var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var goalCatalog = goals.catalog(context);
    var generated =
        RouteCards.build(
            simulator, item, goal, goalCatalog, settings.lowBelow(), settings.highFrom());
    boolean complete = generated.complete() && coveredByAcquisition(goal);
    if (generated.cards().isEmpty()) {
      return new View(
          List.of(),
          List.of(),
          complete,
          List.of(),
          new Diagnostics("NO_ELIGIBLE_CARDS", settings.outcomeBudget(), null));
    }
    var cards = generated.cards();
    if (cardIndex != null && (cardIndex < 0 || cardIndex >= cards.size()))
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error("INVALID_CARD_INDEX", "/cardIndex", "Unknown target card.")));
    Predicate<ItemState> matchesGoal = state -> evaluate.apply(state) == Status.MATCH;
    var numerical = numericSourceStats(goal, goalCatalog);
    RecipeRoutePlanner.Result result;
    if (cardIndex == null) {
      result =
          RecipeRoutePlanner.calculate(
              simulator,
              item,
              cards,
              matchesGoal,
              settings.outcomeBudget(),
              numerical != null && numerical.isEmpty(),
              minimumQuality,
              numerical,
              qualityCatalyst);
    } else {
      var card = cards.get(cardIndex);
      cards = List.of(card);
      result =
          RecipeRoutePlanner.evaluateFixed(
              simulator,
              item,
              card,
              matchesGoal,
              settings.outcomeBudget(),
              numerical != null && numerical.isEmpty(),
              minimumQuality,
              steps,
              numerical,
              qualityCatalyst);
      // 이 응답은 한 가지 지정된 경로에 한정되므로 다른 후보의 미탐색 여부는 영향을 주지 않는다.
      complete = true;
    }
    var dictionary =
        cards.stream()
            .map(
                card ->
                    new Card(
                        card.options().stream()
                            .map(
                                option ->
                                    new Option(
                                        option.number(),
                                        option.tiers().keySet().stream().sorted().toList(),
                                        option.hasLowerBound(),
                                        option.hasUpperBound()))
                            .toList()))
            .toList();
    boolean alreadySatisfied = result.routes().stream().anyMatch(route -> route.steps().isEmpty());
    return new View(
        dictionary,
        result.routes(),
        alreadySatisfied || (complete && result.complete()),
        result.states(),
        new Diagnostics(
            result.routes().isEmpty() ? "NO_VERIFIED_ROUTES" : "ROUTES_FOUND",
            settings.outcomeBudget(),
            result.trace()));
  }

  /** 직접 수치 조건만 있으면 그 원본 ID를 넘긴다. 다른 점수·파생 규칙은 모든 수치를 보존한다. */
  private static Set<String> numericSourceStats(GoalFilter goal, GoalCatalog catalog) {
    if (goal.groups().stream()
        .anyMatch(
            g ->
                !g.disabled()
                    && (g.type() == GoalFilter.Type.WEIGHTED_V1
                        || g.type() == GoalFilter.Type.WEIGHTED_V2))) return null;
    var rows =
        goal.groups().stream()
            .filter(g -> !g.disabled())
            .flatMap(g -> g.entries().stream())
            .filter(e -> !e.disabled() && (e.range().min() != null || e.range().max() != null))
            .map(GoalFilter.Entry::statId)
            .collect(Collectors.toSet());
    var result = new java.util.HashSet<String>();
    for (var row : rows) {
      var stat = catalog.stats().stream().filter(s -> s.statId().equals(row)).findFirst();
      if (stat.isEmpty()
          || (stat.get().kind() != GoalCatalog.Kind.EXPLICIT
              && stat.get().kind() != GoalCatalog.Kind.IMPLICIT)
          || stat.get().sourceStatIds().isEmpty()) return null;
      result.addAll(stat.get().sourceStatIds());
    }
    return Set.copyOf(result);
  }

  /** 단순 제거로도 달성할 수 있는 부정·조건부·가중합·상한 목표는 획득 순서 탐색만으로 완전성을 보장할 수 없다. */
  private static boolean coveredByAcquisition(GoalFilter goal) {
    for (var group : goal.groups()) {
      if (group.disabled()) continue;
      if (group.type() != GoalFilter.Type.AND && group.type() != GoalFilter.Type.COUNT)
        return false;
      if (group.type() == GoalFilter.Type.COUNT && group.range().max() != null) return false;
      if (group.entries().stream()
          .anyMatch(entry -> !entry.disabled() && entry.range().max() != null)) {
        return false;
      }
    }
    return true;
  }
}
