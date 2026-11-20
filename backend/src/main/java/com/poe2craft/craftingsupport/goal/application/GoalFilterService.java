package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.craftingsupport.goal.domain.ExplicitGoalScope;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.domain.GoalValidator;
import com.poe2craft.craftingsupport.goal.domain.ItemStatProjection;
import com.poe2craft.craftingsupport.goal.domain.NumericAdditionKernel;
import com.poe2craft.craftingsupport.goal.domain.NumericAdditionSearch;
import com.poe2craft.craftingsupport.search.domain.RollPartition;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 아이템 수치 관측과 목표 조건 검증·평가 및 유한 추가 탐색을 조정한다. 테스트 작업대의 preview는 작업대와 공유하는 베이스의 획득처를 구하고 examples로 목표에
 * 맞는 상태를 찾는다. Preview는 표시용 정의와 ExampleCard 목록을, 각 ExampleCard는 완성 상태와 옵션별 획득처를 보관한다.
 *
 * <p>기존 계약: Coordinates numeric observations and the explicitly declared finite addition model.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code index}: 색인 ({@code GoalCatalogIndex}).
 *   <li>{@code validator}: 상태·입력 검증기 ({@code GoalValidator}).
 *   <li>{@code evaluator}: 평가기 ({@code GoalEvaluator}).
 *   <li>{@code projection}: 투영 ({@code ItemStatProjection}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalFilterService(index)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code index}(색인).
 *   <li>{@code minimumExplicitModifiers(item, goal)}: 최소값·명시옵션·옵션목록 처리에서 {@code
 *       ExplicitGoalScope.minimumModifiers}, {@code index.catalog}을(를) 사용한다. 입력: {@code item}(아이템),
 *       {@code goal}(목표). 반환: {@code int}.
 *   <li>{@code catalog(context)}: 카탈로그 처리에서 {@code index.catalog}을(를) 사용한다. 입력: {@code
 *       context}(문맥). 반환: {@code GoalCatalog}.
 *   <li>{@code rollPartition(item, goal)}: 수치굴림·분할 처리에서 {@code item.snapshotId}, {@code
 *       item.baseItemId}을(를) 사용한다. 입력: {@code item}(아이템), {@code goal}(목표). 반환: {@code
 *       RollPartition}.
 *   <li>{@code validate(context, goal)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code context}(문맥), {@code
 *       goal}(목표). 반환: {@code GoalValidator.Validation}. 사용하는 예외: {@code InvalidGoal}.
 *   <li>{@code evaluate(item, goal)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code item}(아이템), {@code
 *       goal}(목표). 반환: {@code GoalEvaluator.Evaluation}. 사용하는 예외: {@code InvalidGoal}.
 *   <li>{@code compile(item, goal)}: 입력 조건을 재사용할 평가·실행 표현으로 바꾼다. 입력: {@code item}(아이템), {@code
 *       goal}(목표). 반환: {@code java.util.function.Function<ItemState, Status>}.
 *   <li>{@code compileEvaluation(item, goal)}: 컴파일·평가 처리에서 {@code initial.status}, {@code
 *       item.snapshotId}을(를) 사용한다. 입력: {@code item}(아이템), {@code goal}(목표). 반환: {@code
 *       java.util.function.Function<ItemState, GoalEvaluator.Evaluation>}.
 *   <li>{@code requiredStats(goal, known)}: 필수·수치목록 처리에서 {@code goal.groups}, {@code
 *       group.disabled}을(를) 사용한다. 입력: {@code goal}(목표), {@code known}(알려진). 반환: {@code
 *       java.util.Map<String, GoalCatalog.Stat>}.
 *   <li>{@code recommend(item, goal, activeOmens, limits)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code
 *       item}(아이템), {@code goal}(목표), {@code activeOmens}(활성·징조목록), {@code limits}(한도목록). 반환:
 *       {@code Recommendation}. 사용하는 예외: {@code InvalidGoal}.
 *   <li>{@code requireContext(context)}: 문맥을(를) 필수 조건을 확인한다. 입력: {@code context}(문맥). 사용하는 예외:
 *       {@code InvalidGoal}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Limits}: {@code GoalFilterService}에서 사용하는 한도목록의 불변 값이다.
 *   <li>{@code Probability}: {@code GoalFilterService}에서 사용하는 확률의 불변 값이다.
 *   <li>{@code Recommendation}: {@code GoalFilterService}에서 사용하는 추천의 불변 값이다.
 *   <li>{@code InvalidGoal}: {@code GoalFilterService} 내부의 유효하지않은·목표 처리를 맡는다.
 * </ul>
 */
public final class GoalFilterService {
  private final GoalCatalogIndex index;
  private final GoalValidator validator = new GoalValidator();
  private final GoalEvaluator evaluator = new GoalEvaluator();
  private final ItemStatProjection projection = new ItemStatProjection();

  public GoalFilterService(GoalCatalogIndex index) {
    this.index = index;
  }

  public int minimumExplicitModifiers(ItemState item, GoalFilter goal) {
    return ExplicitGoalScope.minimumModifiers(
        goal, index.catalog(new Context(item.snapshotId(), item.baseItemId(), item.itemLevel())));
  }

  public GoalCatalog catalog(Context context) {
    requireContext(context);
    return index.catalog(context);
  }

  /** 문맥·목표를 기존 규칙으로 검증하고 같은 스냅샷의 획득처와 굴림 모델로 완성 예시를 구성한다. 잘못된 목표는 InvalidGoal로 거절한다. */
  public List<ItemState> examples(
      Context context,
      GoalFilter goal,
      WorkbenchSimulator simulator,
      Map<String, List<ModifierAcquisition>> sources) {
    return exampleGroups(context, goal, simulator, sources, null, null).stream()
        .flatMap(group -> group.variants().stream())
        .toList();
  }

  /** 아이템 상태와 획득 가능성을 검증한 뒤 같은 옵션 구성의 티어 조합을 한 그룹으로 반환한다. */
  private List<GoalExamples.Matches> exampleGroups(
      Context context,
      GoalFilter goal,
      WorkbenchSimulator simulator,
      Map<String, List<ModifierAcquisition>> sources,
      ModifierInstance fracture,
      String selectedTier) {
    validate(context, goal);
    var catalog = index.itemCatalog(context).orElse(null);
    if (catalog == null
        || !catalog.metadata().snapshotId().equals(simulator.catalog().metadata().snapshotId())
        || !catalog.base().id().equals(simulator.catalog().base().id())) return List.of();
    var known = requiredStats(goal, index.knownStats(context));
    // 수동 공개를 포함하는 작업대 카탈로그로 상태를 검증한 뒤 기존 목표 판정을 그대로 사용한다.
    // 단독 품질은 최소 조건으로, 복합 목표는 작업대에서 도달한 품질을 고정해 같은 기폭제로 모든 행을 판정한다.
    var general = goal.general();
    var optionGoal =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            new General(general.baseItemId(), general.itemLevel(), general.rarities()),
            goal.groups());
    return new GoalExamples(
            simulator,
            goal,
            known,
            sources,
            compiledEvaluation(optionGoal, known, simulator.catalog()))
        .find(context, fracture, selectedTier);
  }

  /**
   * item은 대표 상태, tierVariants는 각 적격 티어를 증명하는 실제 아이템, sources는 티어별 획득처, optionTiers는 대표 옵션별 적격 정의
   * ID다.
   */
  public record ExampleCard(
      ItemState item,
      List<ItemState> tierVariants,
      Map<String, List<ModifierAcquisition>> sources,
      Map<String, List<String>> optionTiers,
      List<Occupancy> occupancy,
      Integer minimumQuality,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
          Map<String, Map<String, BigDecimal>> projectedValues) {}

  /** 동일 목표 카드의 잡옵 접두·접미 수와 남은 접두·접미 칸 수다. */
  public record Occupancy(int prefixes, int suffixes, int emptyPrefixes, int emptySuffixes) {}

  /** cards는 완성 예시, modifiers는 표시 정의, resolvedCountRanges는 판정에 쓰인 그룹별 개수 경계, reasonCode는 미지원 사유다. */
  public record Preview(
      List<ExampleCard> cards,
      Map<String, ModifierDefinition> modifiers,
      Map<String, Range> resolvedCountRanges,
      String reasonCode) {}

  /** 요청의 베이스와 레벨로 획득처를 조회하고 예시 카드에 실제로 등장한 옵션만 응답에 포함한다. */
  public Preview preview(Context context, GoalFilter goal, CraftingOperations workbench) {
    return preview(context, goal, workbench, null);
  }

  /** 시작 장비의 분열 옵션이 있으면 모든 완성 예시에 그대로 보존한다. */
  public Preview preview(
      Context context, GoalFilter goal, CraftingOperations workbench, ModifierInstance fracture) {
    return preview(context, goal, workbench, fracture, null);
  }

  /** 특정 티어를 고정한 상태에서도 나머지 옵션의 적격 티어를 다시 검증한다. */
  public Preview preview(
      Context context,
      GoalFilter goal,
      CraftingOperations workbench,
      ModifierInstance fracture,
      String selectedTier) {
    return preview(context, goal, workbench, fracture, selectedTier, null);
  }

  /** 축성된 시작 아이템은 현재의 수치·품질·옵션만 평가한다. 기존 요청은 분열 옵션만 고정해 탐색한다. */
  public Preview preview(
      Context context,
      GoalFilter goal,
      CraftingOperations workbench,
      ModifierInstance fracture,
      String selectedTier,
      ItemState startingItem) {
    requireContext(context);
    if (startingItem != null) {
      var actualFracture =
          startingItem.explicits().stream()
              .filter(ModifierInstance::fractured)
              .findFirst()
              .orElse(null);
      if (!context.equals(
              new Context(
                  startingItem.snapshotId(), startingItem.baseItemId(), startingItem.itemLevel()))
          || (fracture != null && !fracture.equals(actualFracture)))
        throw new InvalidGoal(
            List.of(
                Issue.error(
                    "INVALID_ITEM",
                    "/startingItem",
                    "Starting item does not match the preview context or fracture.")));
      fracture = actualFracture;
    }
    var simulator = workbench.simulatorForBase(context.baseItemId());
    var sources = simulator.modifierSources(context.itemLevel());
    var definitions = new java.util.TreeMap<String, ModifierDefinition>();
    var cards = new ArrayList<ExampleCard>();
    var groups =
        startingItem != null && startingItem.conditions().contains(ItemState.Condition.SANCTIFIED)
            ? existingItemGroup(startingItem, goal, selectedTier)
            : exampleGroups(context, goal, simulator, sources, fracture, selectedTier);
    for (var group : groups) {
      var variants = group.variants();
      var used = new java.util.TreeMap<String, List<ModifierAcquisition>>();
      var projectedValues = new java.util.TreeMap<String, Map<String, BigDecimal>>();
      for (var item : variants) {
        for (var layer : List.of(item.implicits(), item.explicits())) {
          for (var modifier : layer) {
            var id = modifier.modifierId();
            if (used.containsKey(id)) continue;
            used.put(
                id,
                sources.getOrDefault(id, List.of()).stream()
                    .filter(source -> source.supportsRarity(item.rarity()))
                    .toList());
            var definition = simulator.catalog().find(id).orElseThrow();
            definitions.put(id, definition);
            if (ItemStatProjection.usesQualityProjection(item)
                || item.conditions().contains(ItemState.Condition.SANCTIFIED))
              projectedValues.put(id, ItemStatProjection.qualityValues(item, definition, modifier));
          }
        }
      }
      cards.add(
          new ExampleCard(
              variants.getFirst(),
              variants,
              Map.copyOf(used),
              group.optionTiers(),
              group.occupancy(),
              goal.general().quality(),
              Map.copyOf(projectedValues)));
    }
    var countRanges = new java.util.LinkedHashMap<String, Range>();
    for (var group : goal.groups()) {
      if (group.type() == Type.COUNT && group.active()) {
        int rows = (int) group.entries().stream().filter(entry -> !entry.disabled()).count();
        countRanges.put(group.id(), group.range().forCount(rows));
      }
    }
    return new Preview(List.copyOf(cards), Map.copyOf(definitions), Map.copyOf(countRanges), null);
  }

  /** 변경 불가 상태가 목표에 맞으면 기존 옵션과 각 단일 티어를 그대로 가진 한 카드만 반환한다. */
  private List<GoalExamples.Matches> existingItemGroup(
      ItemState item, GoalFilter goal, String selectedTier) {
    if (evaluate(item, goal).status() != Status.MATCH) return List.of();
    var tiers = new java.util.TreeMap<String, List<String>>();
    for (var layer : List.of(item.implicits(), item.explicits()))
      for (var mod : layer) tiers.put(mod.modifierId(), List.of(mod.modifierId()));
    if (selectedTier != null && !tiers.containsKey(selectedTier)) return List.of();
    return List.of(new GoalExamples.Matches(List.of(item), Map.copyOf(tiers), List.of()));
  }

  public RollPartition rollPartition(ItemState item, GoalFilter goal) {
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var catalog = index.itemCatalog(context);
    return catalog
        .map(c -> RollPartition.compile(c, index.knownStats(context), goal, item))
        .orElseGet(RollPartition::exact);
  }

  public GoalValidator.Validation validate(Context context, GoalFilter goal) {
    requireContext(context);
    var result =
        validator.validate(context, goal, index.catalog(context), index.knownStats(context));
    if (!result.valid()) throw new InvalidGoal(result.issues());
    var catalog = index.catalog(context);
    boolean numeric =
        catalog.groupTypes().stream().allMatch(g -> g.probability() == Capability.SUPPORTED)
            && goal.groups().stream()
                .filter(g -> !g.disabled())
                .flatMap(g -> g.entries().stream())
                .filter(e -> !e.disabled())
                .allMatch(
                    e ->
                        index.knownStats(context).get(e.statId()).support().probability()
                            == Capability.SUPPORTED);
    return new GoalValidator.Validation(
        result.version(),
        result.valid(),
        result.issues(),
        new GoalValidator.Capabilities(
            result.capabilities().evaluation(),
            numeric ? Capability.SUPPORTED : Capability.UNSUPPORTED));
  }

  public GoalEvaluator.Evaluation evaluate(ItemState item, GoalFilter goal) {
    if (item == null)
      throw new InvalidGoal(
          List.of(Issue.error("INVALID_ITEM", "/item", "ItemState is required.")));
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var validated = validate(context, goal);
    var issues = new ArrayList<>(validated.issues());
    var itemCatalog = index.itemCatalog(context).orElse(null);
    boolean supported = itemCatalog != null;
    if (itemCatalog != null) {
      var violations = new ItemStateValidator(itemCatalog).validate(item);
      for (var violation : violations) {
        if (violation.code() == ItemStateValidator.Code.UNSUPPORTED_STATE) {
          supported = false;
          issues.add(
              Issue.warning(
                  "UNSUPPORTED_ITEM_EFFECT",
                  "/item",
                  "Special conditions or quality effects need a reviewed numeric projection."));
        } else issues.add(Issue.error(violation.code().name(), "/item", violation.message()));
      }
    }
    if (issues.stream().anyMatch(i -> i.severity() == Severity.ERROR))
      throw new InvalidGoal(issues);
    var observations = projection.project(item, index.knownStats(context), supported, itemCatalog);
    for (int i = 0; i < goal.groups().size(); i++) {
      var group = goal.groups().get(i);
      if (group.disabled()) continue;
      for (int j = 0; j < group.entries().size(); j++) {
        var entry = group.entries().get(j);
        if (entry.disabled()) continue;
        var observation = observations.get(entry.statId());
        if (observation != null && observation.support() != Capability.SUPPORTED)
          issues.add(
              Issue.warning(
                  observation.support() == Capability.UNSUPPORTED
                      ? "UNSUPPORTED_STAT_VALUE"
                      : "UNKNOWN_STAT_VALUE",
                  "/goal/groups/" + i + "/entries/" + j,
                  "The stat value cannot be established from this item."));
      }
    }
    var general = goal.general();
    var generalStatus =
        general.itemLevel().contains(BigDecimal.valueOf(item.itemLevel()))
                && general.rarities().contains(item.rarity())
                && (general.quality() == null
                    || general.quality()
                        <= (item.catalystQuality() == null ? 0 : item.catalystQuality().amount()))
            ? Status.MATCH
            : Status.NO_MATCH;
    var result = evaluator.evaluate(goal, generalStatus, observations, issues);
    if (supported) return result;
    return new GoalEvaluator.Evaluation(
        result.version(),
        result.catalogVersion(),
        GoalEvaluator.and(List.of(result.status(), Status.UNSUPPORTED)),
        result.generalStatus(),
        result.groups(),
        result.issues());
  }

  /** Validates once, then projects every concrete outcome through the same numeric semantics. */
  public java.util.function.Function<ItemState, Status> compile(ItemState item, GoalFilter goal) {
    var compiled = compileEvaluation(item, goal);
    return state -> compiled.apply(state).status();
  }

  /** Keeps every entry result for checkpoint classification without dropping the original AST. */
  public java.util.function.Function<ItemState, GoalEvaluator.Evaluation> compileEvaluation(
      ItemState item, GoalFilter goal) {
    var initial = evaluate(item, goal);
    if (initial.status() == Status.UNKNOWN || initial.status() == Status.UNSUPPORTED)
      return state -> evaluate(state, goal);
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var known = requiredStats(goal, index.knownStats(context));
    return compiledEvaluation(goal, known, index.itemCatalog(context).orElseThrow());
  }

  /** 검증된 아이템의 일반 조건과 목표 수치만 판정하는 요청 범위 함수를 만든다. */
  private java.util.function.Function<ItemState, GoalEvaluator.Evaluation> compiledEvaluation(
      GoalFilter goal,
      Map<String, GoalCatalog.Stat> known,
      com.poe2craft.shared.item.ItemCatalog catalog) {
    return state ->
        evaluator.evaluate(
            goal,
            goal.general().itemLevel().contains(BigDecimal.valueOf(state.itemLevel()))
                    && goal.general().rarities().contains(state.rarity())
                    && (goal.general().quality() == null
                        || goal.general().quality()
                            <= (state.catalystQuality() == null
                                ? 0
                                : state.catalystQuality().amount()))
                ? Status.MATCH
                : Status.NO_MATCH,
            projection.project(state, known, true, catalog),
            List.of());
  }

  /** A dependency closure, not a reduction of the submitted goal AST or its numeric values. */
  private static java.util.Map<String, GoalCatalog.Stat> requiredStats(
      GoalFilter goal, java.util.Map<String, GoalCatalog.Stat> known) {
    var pending = new java.util.ArrayDeque<String>();
    for (var group : goal.groups())
      if (!group.disabled())
        for (var entry : group.entries()) if (!entry.disabled()) pending.add(entry.statId());
    var result = new java.util.LinkedHashMap<String, GoalCatalog.Stat>();
    while (!pending.isEmpty()) {
      var id = pending.remove();
      var stat = known.get(id);
      if (stat == null || result.putIfAbsent(id, stat) != null) continue;
      for (var contribution : stat.contributions()) pending.add(contribution.statId());
    }
    return java.util.Map.copyOf(result);
  }

  /**
   * {@code GoalFilterService}에서 사용하는 한도목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxStates}: 최대값·상태목록 ({@code Integer}).
   *   <li>{@code maxEdges}: 최대값·간선목록 ({@code Integer}).
   *   <li>{@code maxMillis}: 최대값·밀리초 ({@code Integer}).
   * </ul>
   */
  public record Limits(Integer maxStates, Integer maxEdges, Integer maxMillis) {}

  /**
   * {@code GoalFilterService}에서 사용하는 확률의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reasonCode}: 이유·코드 ({@code String}).
   *   <li>{@code modelVersion}: 모형·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code interpretation}: 해석 ({@code String}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Probability(status, reasonCode, modelVersion, ledgerVersion)}: 기본 정책을 보충해 공통 생성자로
   *       위임한다. 입력: {@code status}(상태), {@code reasonCode}(이유·코드), {@code modelVersion}(모형·버전),
   *       {@code ledgerVersion}(규칙장부·버전).
   * </ul>
   */
  public record Probability(
      String status,
      String reasonCode,
      String modelVersion,
      String ledgerVersion,
      String interpretation,
      List<String> assumptions) {
    public Probability(
        String status, String reasonCode, String modelVersion, String ledgerVersion) {
      this(
          status,
          reasonCode,
          modelVersion,
          ledgerVersion,
          modelVersion == null ? null : "DECLARED_MODEL_NOT_VERIFIED_GAME_PROBABILITY",
          modelVersion == null
              ? List.of()
              : List.of(
                  "Existing AdditionRules eligibility and catalog weight / total eligible weight.",
                  "Single-stat min..max integers are equiprobable under uniform-integer-roll-v1.",
                  "All candidates remain in the denominator, including unrelated modifiers.",
                  "Fixed starting rolls; first success is absorbing; finite open-loop addition sequences.",
                  "No quality, omens, multi-stat ratio ticks, removal, recovery, or repeat policies."));
    }
  }

  /**
   * {@code GoalFilterService}에서 사용하는 추천의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code int}).
   *   <li>{@code catalogVersion}: 카탈로그·버전 ({@code String}).
   *   <li>{@code evaluation}: 평가 ({@code Status}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   *   <li>{@code comparisons}: comparisons ({@code List<NumericAdditionSearch.Comparison>}).
   *   <li>{@code rankingCertified}: 순위·확률구간 ({@code boolean}).
   *   <li>{@code comparedSequences}: compared·순서목록 ({@code int}).
   *   <li>{@code totalSequences}: 합계·순서목록 ({@code Integer}).
   * </ul>
   */
  public record Recommendation(
      int version,
      String catalogVersion,
      Status evaluation,
      Probability probability,
      List<NumericAdditionSearch.Comparison> comparisons,
      boolean rankingCertified,
      int comparedSequences,
      Integer totalSequences) {}

  public Recommendation recommend(
      ItemState item, GoalFilter goal, Set<String> activeOmens, Limits limits) {
    if (activeOmens == null)
      throw new InvalidGoal(
          List.of(Issue.error("INVALID_OMENS", "/activeOmens", "An omen set is required.")));
    if (limits == null
        || limits.maxStates() == null
        || limits.maxEdges() == null
        || limits.maxMillis() == null
        || limits.maxStates() < 1
        || limits.maxEdges() < 1
        || limits.maxMillis() < 1)
      throw new InvalidGoal(
          List.of(
              Issue.error(
                  "INVALID_LIMITS", "/limits", "Positive exploration limits are required.")));
    for (var omen : activeOmens) {
      try {
        WorkbenchOmen.fromId(omen);
      } catch (IllegalArgumentException error) {
        throw new InvalidGoal(
            List.of(
                Issue.error("UNKNOWN_OMEN", "/activeOmens", "Provide known Workbench omen IDs.")));
      }
    }
    var evaluation = evaluate(item, goal);
    var context = new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    var catalog = index.itemCatalog(context).orElse(null);
    String blocked =
        item.catalystQuality() != null
            ? "QUALITY_PROJECTION_NOT_IMPLEMENTED"
            : !item.conditions().isEmpty()
                    || item.rarity() == ItemState.Rarity.UNIQUE
                    || item.explicits().stream().anyMatch(ModifierInstance::fractured)
                ? "UNSUPPORTED_ITEM_EFFECT"
                : !activeOmens.isEmpty()
                    ? "OMEN_NUMERIC_MODEL_NOT_IMPLEMENTED"
                    : !NumericAdditionKernel.supports(catalog)
                        ? "NUMERIC_BASE_NOT_IMPLEMENTED"
                        : validate(context, goal).capabilities().probability()
                                != Capability.SUPPORTED
                            ? "UNIT_OR_EFFECT_NOT_REVIEWED"
                            : null;
    if (blocked == null) {
      var known = index.knownStats(context);
      var general = goal.general();
      var search =
          new NumericAdditionSearch()
              .compare(
                  catalog,
                  item,
                  state ->
                      evaluator
                          .evaluate(
                              goal,
                              general.itemLevel().contains(BigDecimal.valueOf(state.itemLevel()))
                                      && general.rarities().contains(state.rarity())
                                  ? Status.MATCH
                                  : Status.NO_MATCH,
                              projection.project(state, known, true, catalog),
                              List.of())
                          .status(),
                  new NumericAdditionSearch.Budget(
                      limits.maxStates(), limits.maxEdges(), limits.maxMillis()));
      return new Recommendation(
          1,
          goal.catalogVersion(),
          evaluation.status(),
          new Probability(
              search.complete() ? "COMPLETE" : "PARTIAL",
              search.complete() ? null : "BUDGET_EXHAUSTED",
              NumericAdditionKernel.MODEL,
              NumericAdditionKernel.LEDGER),
          search.comparisons(),
          search.complete(),
          search.comparisons().size(),
          search.totalSequences());
    }
    return new Recommendation(
        1,
        goal.catalogVersion(),
        evaluation.status(),
        new Probability("UNSUPPORTED", blocked, null, null),
        List.of(),
        false,
        0,
        null);
  }

  private static void requireContext(Context context) {
    var issues = GoalValidator.contextIssues(context);
    if (!issues.isEmpty()) throw new InvalidGoal(issues);
  }

  /**
   * {@code GoalFilterService} 내부의 유효하지않은·목표 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code issues}: 문제목록 ({@code List<Issue>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code InvalidGoal(issues)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code issues}(문제목록).
   *   <li>{@code issues()}: {@code issues} 값을 조회한다. 반환: {@code List<Issue>}.
   *   <li>{@code staleCatalog()}: stale·카탈로그 값을 조회·구성한다. 반환: {@code boolean}.
   * </ul>
   */
  public static final class InvalidGoal extends IllegalArgumentException {
    private final List<Issue> issues;

    public InvalidGoal(List<Issue> issues) {
      super("Invalid goal filter request");
      this.issues = List.copyOf(issues);
    }

    public List<Issue> issues() {
      return issues;
    }

    public boolean staleCatalog() {
      return issues.stream().anyMatch(i -> i.code().equals("CATALOG_VERSION_MISMATCH"));
    }
  }
}
