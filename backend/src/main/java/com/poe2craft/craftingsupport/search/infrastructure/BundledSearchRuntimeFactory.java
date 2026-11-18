package com.poe2craft.craftingsupport.search.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.application.*;
import com.poe2craft.craftingsupport.search.application.AutomaticKernelFactory;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;

/**
 * 공유 카탈로그와 현재·과거 목표 정의로 탐색 실행 환경을 만든다.
 *
 * <p>기존 계약: Loads current and frozen goal definitions; search jobs only depend on
 * AutomaticRuntimeFactory.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code policy}: 자동 제작에 허용한 화폐와 조건.
 *   <li>{@code workbench}: 작업대 ({@code CraftingOperations}).
 *   <li>{@code goals}: 목표목록 ({@code GoalFilterService}).
 *   <li>{@code legacy}: 구버전 ({@code MethodSearchService}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code historicalGoals}: 과거·목표목록 ({@code java.util.Map<String, GoalFilterService>}).
 *   <li>{@code factory}: 생성기 ({@code java.util.function.BiFunction<GoalFilterService,
 *       MethodSearchProtocol.Create, AutomaticSearchService.Runtime>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BundledSearchRuntimeFactory(workbench, goals, legacy, json, policy)}: 전달받은 값으로 구성원을
 *       초기화한다. 입력: {@code workbench}(작업대), {@code goals}(목표목록), {@code legacy}(구버전), {@code
 *       json}(JSON), {@code policy}(정책). 사용하는 예외: {@code PathSearchProtocol.Rejected}, {@code
 *       IllegalArgumentException}.
 *   <li>{@code create(request)}: 새 작업 또는 값을 생성한다. 입력: {@code request}(요청). 반환: {@code
 *       AutomaticSearchService.Runtime}.
 *   <li>{@code historical(request)}: 과거 정의의 읽기 전용 실행 환경을 조회한다. 입력: {@code request}(요청). 반환: {@code
 *       AutomaticSearchService.Runtime}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public final class BundledSearchRuntimeFactory implements AutomaticRuntimeFactory {
  private final AutomaticCurrencyPolicy policy;
  private final CraftingOperations workbench;
  private final GoalFilterService goals;
  private final MethodSearchService legacy;
  private final ObjectMapper json;
  private final java.util.Map<String, GoalFilterService> historicalGoals =
      new java.util.LinkedHashMap<>(4, 0.75f, true);
  private final java.util.function.BiFunction<
          GoalFilterService, MethodSearchProtocol.Create, AutomaticSearchService.Runtime>
      factory;

  public BundledSearchRuntimeFactory(
      CraftingOperations workbench,
      GoalFilterService goals,
      MethodSearchService legacy,
      ObjectMapper json,
      AutomaticCurrencyPolicy policy) {
    this.policy = policy;
    this.workbench = workbench;
    this.goals = goals;
    this.legacy = legacy;
    this.json = json;
    this.factory =
        (goalService, request) -> {
          if (request == null
              || !Integer.valueOf(2).equals(request.version())
              || request.start() == null
              || request.goal() == null
              || request.activeOmens() == null
              || !request.activeOmens().isEmpty())
            throw new PathSearchProtocol.Rejected(422, "INVALID_REQUEST");
          var item = request.start().item();
          if (!legacy.provenance(item.baseItemId()).equals(request.start().provenance()))
            throw new PathSearchProtocol.Rejected(422, "RULESET_IDENTITY_MISMATCH");
          var catalog =
              workbench.reviewedCatalogs().stream()
                  .filter(c -> c.base().id().equals(item.baseItemId()))
                  .findFirst()
                  .orElseThrow();
          if (!new ItemStateValidator(catalog).validate(item).isEmpty()
              || item.explicits().size() > AutomaticCurrencyPolicy.MAXIMUM_EXPLORED_EXPLICITS)
            throw new PathSearchProtocol.Rejected(422, "INVALID_ITEM");
          var general = request.goal().general();
          goalService.validate(
              new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel()),
              request.goal());
          try {
            return new AutomaticSearchService.Runtime(
                new AutomaticKernelFactory(workbench)
                    .create(item, policy)
                    .withRollPartition(
                        goalService.rollPartition(item, request.goal()),
                        json.writeValueAsString(request.goal()))
                    .withQualityGoal(general.quality() == null ? 0 : general.quality()),
                progress(goalService, item, request.goal()),
                json.writeValueAsString(request.goal()),
                goalService.minimumExplicitModifiers(item, request.goal()));
          } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Cannot preserve objective", e);
          }
        };
  }

  @Override
  public AutomaticSearchService.Runtime create(MethodSearchProtocol.Create request) {
    return factory.apply(goals, request);
  }

  @Override
  public synchronized AutomaticSearchService.Runtime historical(
      MethodSearchProtocol.Create request) {

    var item = request.start().item();
    if (ArchivedSearchCatalog.supports(item.snapshotId(), request.goal().catalogVersion()))
      return archivedRuntime(request);
    var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var goal = request.goal();
    var goalService = goals;
    if (!goals.catalog(context).catalogVersion().equals(goal.catalogVersion())) {
      goalService = historicalGoals.get(goal.catalogVersion());
      if (goalService == null) {
        // Frozen definitions reproduce the exact historical catalog, never relabel its AST.
        var catalogs = BundledGoalCatalogs.load(workbench);
        goalService = null;
        for (var membership :
            java.util.List.of(catalogs, BundledGoalCatalogs.loadHistoricalV5(workbench))) {
          for (var definitions :
              java.util.List.of(
                  GoalDefinitionsLoader.load(),
                  GoalDefinitionsLoader.loadHistoricalV3(),
                  GoalDefinitionsLoader.loadHistoricalV4(),
                  GoalDefinitionsLoader.loadHistoricalV5())) {
            for (var identity :
                java.util.Arrays.asList(
                    request.start().provenance().rulesetIdentity(),
                    (String) null,
                    "reviewed-equipment-20261028-v5-a830beabb0506da58c864d19bcf428a72c3ecdb8f9fa42fe1e491e1e7c0b02b1",
                    "reviewed-equipment-20261028-v4-81ceaf5378ae88a6b874c0b8c04d729d35bd0f87a40c7b59a93ae16df03a6736",
                    "reviewed-equipment-20261028-v3-d8fc1817187354306f0db443768486e8e15ea3f4e4904447300104750421bb7a",
                    "reviewed-equipment-20261009-v2-c303fc1ea724ddf2ccca61f7c9512e6c9fdcd997cb9d4348fe5a5f8766f07d5a",
                    "reviewed-equipment-20261009-v2-68910c93fedbba8263972e2c97605e96271d6cf73fa6186088d3c9b76903cf1a",
                    "reviewed-equipment-20261009-v2-debb3f07052d54419a4732f49d9febf78508e23cd69a489698c5e81be80597f7",
                    "reviewed-equipment-20261009-v2-6346262eb5667dafc37a65907bb03f16469d2ce976559de50a4b6cb1a7a2d727")) {
              var index = new GoalCatalogIndex(membership, definitions, identity);
              if (index.version().equals(goal.catalogVersion())) {
                goalService = new GoalFilterService(index);
                break;
              }
            }
            if (goalService != null) break;
          }
          if (goalService != null) break;
        }
        if (goalService == null)
          throw new IllegalArgumentException("Historical goal catalog changed");
        if (historicalGoals.size() >= 4)
          historicalGoals.remove(historicalGoals.keySet().iterator().next());
        historicalGoals.put(goal.catalogVersion(), goalService);
      }
    }
    var runtime =
        factory.apply(
            goalService,
            new MethodSearchProtocol.Create(
                request.version(),
                request.clientRequestId(),
                new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
                goal,
                request.activeOmens()));
    try {
      return new AutomaticSearchService.Runtime(
          runtime.kernel(),
          runtime.evaluate(),
          json.writeValueAsString(request.goal()),
          runtime.minimumExplicitModifiers());
    } catch (java.io.IOException error) {
      throw new IllegalArgumentException("Cannot read historical objective", error);
    }
  }

  /** 당시 정의에서 옵션과 굴림 구간을 읽는다. 실행·재개에는 사용하지 않으며 표시용 목표만 로컬 인덱스의 판본에 연결한다. */
  private AutomaticSearchService.Runtime archivedRuntime(MethodSearchProtocol.Create request) {
    var goal = request.goal();
    var item = request.start().item();
    var catalog = ArchivedSearchCatalog.catalog();
    try {
      var key = goal.catalogVersion();
      var service = historicalGoals.get(key);
      if (service == null) {
        var definitions =
            GoalDefinitionsLoader.read(
                new java.io.ByteArrayInputStream(
                    json.writeValueAsBytes(ArchivedSearchCatalog.goalDefinitions(key))));
        service =
            new GoalFilterService(new GoalCatalogIndex(java.util.List.of(catalog), definitions));
        if (historicalGoals.size() >= 4)
          historicalGoals.remove(historicalGoals.keySet().iterator().next());
        historicalGoals.put(key, service);
      }
      var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
      var local =
          new GoalFilter(
              goal.version(),
              service.catalog(context).catalogVersion(),
              goal.general(),
              goal.groups());
      var kernel =
          new AutomaticCraftingKernel(
                  policy,
                  catalog,
                  new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
                  "amulets")
              .withRollPartition(service.rollPartition(item, local), json.writeValueAsString(goal))
              .withQualityGoal(goal.general().quality() == null ? 0 : goal.general().quality());
      return new AutomaticSearchService.Runtime(
          kernel, progress(service, item, local), json.writeValueAsString(goal));
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Cannot read historical objective", invalid);
    }
  }

  /** 일반 목표와 옵션 목표를 당시 아이템 수준과 희귀도에 맞춰 평가한다. */
  private static java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> progress(
      GoalFilterService goalService, ItemState item, GoalFilter goal) {
    var general = goal.general();
    var affixGoal =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            new GoalFilter.General(general.baseItemId(), general.itemLevel(), general.rarities()),
            goal.groups());
    java.util.function.Function<ItemState, GoalEvaluator.Evaluation> evaluate =
        affixGoal.groups().stream().anyMatch(g -> !g.disabled())
            ? goalService.compileEvaluation(item, affixGoal)
            : state ->
                new GoalEvaluator.Evaluation(
                    1,
                    affixGoal.catalogVersion(),
                    affixGoal.general().rarities().contains(state.rarity())
                            && affixGoal
                                .general()
                                .itemLevel()
                                .contains(java.math.BigDecimal.valueOf(state.itemLevel()))
                        ? GoalFilter.Status.MATCH
                        : GoalFilter.Status.NO_MATCH,
                    GoalFilter.Status.MATCH,
                    java.util.List.of(),
                    java.util.List.of());
    return state -> {
      var result = evaluate.apply(state);
      return new AutomaticMethodGraph.Progress(
          result.status(),
          result.groups().stream()
              .flatMap(group -> group.entries().stream())
              .map(entry -> entry.status())
              .toList());
    };
  }
}
