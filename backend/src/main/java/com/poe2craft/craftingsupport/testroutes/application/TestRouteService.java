package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import java.util.List;

/** 테스트 탭의 카드 사전 검사와 경로 검증을 현재 작업대·목표 카탈로그에 연결한다. */
public final class TestRouteService {
  /** number는 목표 번호이고 modifierIds는 해당 번호로 인정되는 티어별 ID다. */
  public record Option(int number, List<String> modifierIds) {
    /** 같은 카드에서 티어가 항상 같은 순서로 표시되게 한다. */
    private Option(RouteCards.Option option) {
      this(option.number(), option.tiers().keySet().stream().sorted().toList());
    }
  }

  /** 경로의 cardIndex로 참조하는 옵션 번호표다. */
  public record Card(List<Option> options) {
    /** 내부 카드에서 화면이 참조할 번호표를 만든다. */
    private Card(RouteCards.Card card) {
      this(card.options().stream().map(Option::new).toList());
    }
  }

  /** 검증한 카드와 경로를 돌려준다. complete는 카드 생성과 경로 평가가 모두 끝났을 때 참이다. */
  public record View(
      List<Card> cards,
      List<RecipeRoutePlanner.Route> routes,
      boolean complete,
      List<ItemState> states) {}

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

  /** 시작 아이템과 목표를 검증하고, 만들 수 있는 카드별로 실제 획득 순서를 계산한다. */
  public View calculate(ItemState item, GoalFilter goal) {
    // compile은 시작 아이템·목표·catalogVersion을 검증한 후 같은 필터 평가기를 재사용한다.
    var evaluate = goals.compile(item, goal);
    var simulator = workbench.baseSimulator(item);
    var violations = new ItemStateValidator(simulator.forItem(item).catalog()).validate(item);
    if (!violations.isEmpty()) {
      throw new GoalFilterService.InvalidGoal(
          violations.stream()
              .map(v -> GoalFilter.Issue.error(v.code().name(), "/item", v.message()))
              .toList());
    }
    var initial = evaluate.apply(item);
    var minimumQuality = goal.general().quality();
    boolean usesCatalyst =
        item.catalystQuality() != null || (minimumQuality != null && minimumQuality > 0);
    if (initial == Status.UNKNOWN
        || initial == Status.UNSUPPORTED
        || (usesCatalyst && goal.groups().stream().anyMatch(g -> !g.disabled()))) {
      // 활성 옵션 조건에서는 기폭제가 바꾼 표시값을 원래 목표 평가기가 검증할 수 없다.
      return new View(List.of(), List.of(), false, List.of());
    }
    var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var generated =
        RouteCards.build(
            simulator,
            item,
            goal,
            goals.catalog(context),
            settings.lowBelow(),
            settings.highFrom());
    boolean complete = generated.complete() && coveredByAcquisition(goal);
    if (generated.cards().isEmpty()) {
      return new View(List.of(), List.of(), complete, List.of());
    }
    var result =
        RecipeRoutePlanner.calculate(
            simulator,
            item,
            generated.cards(),
            state -> evaluate.apply(state) == Status.MATCH,
            settings.outcomeBudget(),
            presenceOnly(goal),
            minimumQuality);
    var dictionary = generated.cards().stream().map(Card::new).toList();
    boolean alreadySatisfied = result.routes().stream().anyMatch(route -> route.steps().isEmpty());
    return new View(
        dictionary,
        result.routes(),
        alreadySatisfied || (complete && result.complete()),
        result.states());
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

  /** 가중 점수와 행별 수치 범위가 없을 때만 같은 옵션의 서로 다른 굴림을 하나로 합산한다. */
  private static boolean presenceOnly(GoalFilter goal) {
    for (var group : goal.groups()) {
      if (group.disabled()) continue;
      if (group.type() == GoalFilter.Type.WEIGHTED_V1
          || group.type() == GoalFilter.Type.WEIGHTED_V2) return false;
      for (var entry : group.entries()) {
        if (!entry.disabled() && (entry.range().min() != null || entry.range().max() != null))
          return false;
      }
    }
    return true;
  }
}
