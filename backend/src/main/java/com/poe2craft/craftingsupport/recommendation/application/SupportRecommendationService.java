package com.poe2craft.craftingsupport.recommendation.application;

import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.AdditionRules;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/**
 * 유한 추가 순서의 최초 성공 확률을 계산하고 미확정 질량을 따로 보존한다.
 *
 * <p>기존 계약: Forward first-hit DP for fixed finite-addition sequences; unresolved mass is never
 * failure.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code pools}: 후보풀목록 ({@code AdditionPoolCache}).
 *   <li>{@code goals}: 목표목록 ({@code SupportGoals}).
 *   <li>{@code familyIndex}: 옵션계열·색인 ({@code Map<String, Integer>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SupportRecommendationService(catalog, pools)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code pools}(후보풀목록).
 *   <li>{@code recommend(root, goal, activeOmens, limits)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code
 *       root}(시작점), {@code goal}(목표), {@code activeOmens}(활성·징조목록), {@code limits}(한도목록). 반환:
 *       {@code Report}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code report(assessment, top, complete, certified, compared, total, budget, before,
 *       start)}: report 처리에서 {@code pools.stats}, {@code pools.namespace}을(를) 사용한다. 입력: {@code
 *       assessment}(평가), {@code top}(상위), {@code complete}(완료), {@code certified}(확률구간), {@code
 *       compared}(compared), {@code total}(합계), {@code budget}(계산예산), {@code before}(변경전), {@code
 *       start}(시작값). 반환: {@code Report}.
 *   <li>{@code evaluate(root, goal, omens, sequence, limits)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code
 *       root}(시작점), {@code goal}(목표), {@code omens}(징조목록), {@code sequence}(순서), {@code
 *       limits}(한도목록). 반환: {@code Comparison}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code rejectMultiAddition(omens)}: reject·Multi·추가 처리에서 {@code omens.stream}, {@code
 *       o.trigger}을(를) 사용한다. 입력: {@code omens}(징조목록). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code evaluate(root, goal, omens, sequence, budget)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code
 *       root}(시작점), {@code goal}(목표), {@code omens}(징조목록), {@code sequence}(순서), {@code
 *       budget}(계산예산). 반환: {@code Comparison}.
 *   <li>{@code comparison(sequence, success, failure, unresolved, complete, steps)}: 비교의 입력에 따른
 *       분기·결과를 처리한다. 입력: {@code sequence}(순서), {@code success}(성공), {@code failure}(실패), {@code
 *       unresolved}(미해결), {@code complete}(완료), {@code steps}(단계목록). 반환: {@code Comparison}. 사용하는
 *       예외: {@code IllegalStateException}.
 *   <li>{@code key(state, goal, omens)}: 키 처리에서 {@code state.modifierIds}, {@code catalog.find}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code goal}(목표), {@code omens}(징조목록). 반환: {@code Key}.
 *   <li>{@code thresholds(goal)}: thresholds 처리에서 {@code goal.required}, {@code thresholds.put}을(를)
 *       사용한다. 입력: {@code goal}(목표). 반환: {@code Map<String, Integer>}.
 *   <li>{@code matched(state, goal)}: 일치한 처리에서 {@code state.modifierIds}, {@code catalog.find}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code goal}(목표). 반환: {@code boolean}.
 *   <li>{@code sequences(rarity, count, level, prefix, results)}: 순서목록 처리에서 {@code results.add},
 *       {@code action.minimumModifierLevel}을(를) 사용한다. 입력: {@code rarity}(희귀도), {@code count}(개수),
 *       {@code level}(레벨), {@code prefix}(접두), {@code results}(결과목록).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code SupportRecommendationService}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Mass}: {@code SupportRecommendationService}에서 사용하는 확률질량의 불변 값이다.
 *   <li>{@code Budget}: {@code SupportRecommendationService} 내부의 계산예산 처리를 맡는다.
 *   <li>{@code Limits}: {@code SupportRecommendationService}에서 사용하는 한도목록의 불변 값이다.
 *   <li>{@code Step}: {@code SupportRecommendationService}에서 사용하는 단계의 불변 값이다.
 *   <li>{@code Comparison}: {@code SupportRecommendationService}에서 사용하는 비교의 불변 값이다.
 *   <li>{@code Report}: {@code SupportRecommendationService}에서 사용하는 Report의 불변 값이다.
 * </ul>
 */
public final class SupportRecommendationService {
  private final ItemCatalog catalog;
  private final AdditionPoolCache pools;
  private final SupportGoals goals;
  private final Map<String, Integer> familyIndex = new HashMap<>();

  public SupportRecommendationService(ItemCatalog catalog, AdditionPoolCache pools) {
    this.catalog = catalog;
    this.pools = pools;
    goals = new SupportGoals(catalog);
    int index = 0;
    for (var family : goals.families()) familyIndex.put(family.id(), index++);
  }

  public synchronized Report recommend(
      StateBucket root, SupportGoals.Goal goal, Set<String> activeOmens, Limits limits) {
    if (limits == null
        || limits.maxStates() < 1
        || limits.maxStates() > 200000
        || limits.maxEdges() < 1
        || limits.maxEdges() > 5000000
        || limits.maxMillis() < 10
        || limits.maxMillis() > 10000)
      throw new IllegalArgumentException("Invalid Support computation budget");
    var assessment = goals.assess(root, goal);
    if (!assessment.valid()) throw new IllegalArgumentException("Invalid Support goal");
    var omens = activeOmens == null ? Set.<String>of() : Set.copyOf(activeOmens);
    omens.forEach(WorkbenchOmen::fromId);
    rejectMultiAddition(omens);
    long start = System.nanoTime();
    var before = pools.stats();
    var budget = new Budget(limits, start);
    if (assessment.achieved() || !assessment.feasible())
      return report(assessment, List.of(), true, true, 0, 0, budget, before, start);
    var sequences = new ArrayList<List<WorkbenchCurrency>>();
    sequences(
        root.rarity(), root.modifierIds().size(), root.itemLevel(), new ArrayList<>(), sequences);
    var comparisons = new ArrayList<Comparison>();
    for (var sequence : sequences) {
      if (budget.globallyExhausted()) break;
      budget.localStateLimit =
          budget.states + Math.max(1, limits.maxStates() / Math.max(1, sequences.size()));
      comparisons.add(evaluate(root, goal, omens, sequence, budget));
    }
    comparisons.sort(
        Comparator.comparingDouble(Comparison::successLower)
            .reversed()
            .thenComparing(c -> c.sequence().toString()));
    var top = comparisons.stream().limit(5).toList();
    boolean complete =
        comparisons.size() == sequences.size()
            && comparisons.stream().allMatch(Comparison::complete);
    boolean certified = complete;
    return report(
        assessment,
        top,
        complete,
        certified,
        comparisons.size(),
        sequences.size(),
        budget,
        before,
        start);
  }

  private Report report(
      SupportGoals.Assessment assessment,
      List<Comparison> top,
      boolean complete,
      boolean certified,
      int compared,
      int total,
      Budget budget,
      AdditionPoolCache.Stats before,
      long start) {
    var after = pools.stats();
    return new Report(
        WorkbenchSimulator.RULE_VERSION,
        WorkbenchSimulator.LEDGER_VERSION,
        pools.namespace(),
        assessment,
        top,
        complete,
        certified,
        compared,
        total,
        budget.states,
        budget.edges,
        (System.nanoTime() - start) / 1e6,
        new AdditionPoolCache.Stats(
            after.memoryHits() - before.memoryHits(),
            after.persistedHits() - before.persistedHits(),
            after.computedPools() - before.computedPools()));
  }

  public Comparison evaluate(
      StateBucket root,
      SupportGoals.Goal goal,
      Set<String> omens,
      List<WorkbenchCurrency> sequence,
      Limits limits) {
    rejectMultiAddition(omens);
    var assessment = goals.assess(root, goal);
    if (!assessment.valid()
        || sequence == null
        || sequence.size() > 6
        || sequence.stream().anyMatch(a -> !AdditionRules.isAddition(a)))
      throw new IllegalArgumentException("Invalid finite addition sequence or goal");
    return evaluate(root, goal, omens, sequence, new Budget(limits, System.nanoTime()));
  }

  private void rejectMultiAddition(Set<String> omens) {
    if (omens != null
        && omens.stream()
            .map(WorkbenchOmen::fromId)
            .anyMatch(o -> o.trigger() == WorkbenchCurrency.ESSENCE_HYSTERIA))
      throw new IllegalArgumentException(
          "Crystallisation is outside the finite addition model. Deactivate it for Support.");
    if (omens != null && omens.contains(WorkbenchOmen.GREATER_EXALTATION.id()))
      throw new IllegalArgumentException(
          "Greater Exaltation is outside the finite addition model. Deactivate it for Support.");
  }

  private Comparison evaluate(
      StateBucket root,
      SupportGoals.Goal goal,
      Set<String> omens,
      List<WorkbenchCurrency> sequence,
      Budget budget) {
    if (matched(root, goal))
      return new Comparison(
          sequence, 1, 1, 0, 0, true, List.of(new Step(0, null, 1, 0, 0, List.of())));
    Map<Key, Mass> current = new LinkedHashMap<>();
    current.put(key(root, goal, omens), new Mass(root, omens, 1));
    double success = 0, failure = 0;
    var steps = new ArrayList<Step>();
    for (int step = 0; step < sequence.size(); step++) {
      var action = sequence.get(step);
      Map<Key, Mass> next = new LinkedHashMap<>();
      var sources = new ArrayList<>(current.values());
      double stepSuccess = 0, stepFailure = 0;
      var blockedReasons = new TreeSet<String>();
      for (int index = 0; index < sources.size(); index++) {
        if (budget.exhausted()) {
          double unresolved = next.values().stream().mapToDouble(Mass::probability).sum();
          for (int i = index; i < sources.size(); i++) unresolved += sources.get(i).probability();
          steps.add(
              new Step(
                  step + 1,
                  action,
                  stepSuccess,
                  stepFailure,
                  unresolved,
                  List.copyOf(blockedReasons)));
          return comparison(sequence, success, failure, unresolved, false, steps);
        }
        var source = sources.get(index);
        budget.states++;
        var pool = pools.get(source.state(), action, source.omens());
        if (!pool.available()) {
          failure += source.probability();
          stepFailure += source.probability();
          blockedReasons.add(pool.reason());
          continue;
        }
        // Expand a pool atomically (<=209 channels), then check the budget before the next source.
        budget.edges += pool.channels().size();
        var remaining = Set.copyOf(pool.remainingOmens());
        for (var channel : pool.channels()) {
          var ids = new ArrayList<>(source.state().modifierIds());
          ids.add(channel.modifierId());
          var target = source.state().with(pool.rarity(), ids);
          double probability = source.probability() * channel.probability();
          if (matched(target, goal)) {
            success += probability;
            stepSuccess += probability;
          } else {
            var key = key(target, goal, remaining);
            var prior = next.get(key);
            next.put(
                key,
                new Mass(
                    prior == null ? target : prior.state(),
                    remaining,
                    probability + (prior == null ? 0 : prior.probability())));
          }
        }
      }
      double unresolved = next.values().stream().mapToDouble(Mass::probability).sum();
      steps.add(
          new Step(
              step + 1, action, stepSuccess, stepFailure, unresolved, List.copyOf(blockedReasons)));
      current = next;
      if (current.isEmpty()) break;
    }
    failure += current.values().stream().mapToDouble(Mass::probability).sum();
    return comparison(sequence, success, failure, 0, true, steps);
  }

  private Comparison comparison(
      List<WorkbenchCurrency> sequence,
      double success,
      double failure,
      double unresolved,
      boolean complete,
      List<Step> steps) {
    if (Math.abs(success + failure + unresolved - 1) > 1e-8)
      throw new IllegalStateException("Support probability mass mismatch");
    return new Comparison(
        sequence, success, Math.min(1, success + unresolved), failure, unresolved, complete, steps);
  }

  private Key key(StateBucket state, SupportGoals.Goal goal, Set<String> omens) {
    long occupied = 0, qualified = 0;
    var thresholds = thresholds(goal);
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      var family = d.familyIds().iterator().next();
      long bit = 1L << familyIndex.get(family);
      occupied |= bit;
      if (thresholds.containsKey(family) && d.tier() <= thresholds.get(family)) qualified |= bit;
    }
    return new Key(occupied, qualified, state.rarity(), omens);
  }

  private Map<String, Integer> thresholds(SupportGoals.Goal goal) {
    var thresholds = new HashMap<String, Integer>();
    goal.required().forEach(c -> thresholds.put(c.family(), c.minimumTier()));
    goal.candidates().forEach(c -> thresholds.put(c.family(), c.minimumTier()));
    return thresholds;
  }

  private boolean matched(StateBucket state, SupportGoals.Goal goal) {
    var tiers = new HashMap<String, Integer>();
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      tiers.put(d.familyIds().iterator().next(), d.tier());
    }
    return goal.required().stream()
            .allMatch(c -> tiers.getOrDefault(c.family(), Integer.MAX_VALUE) <= c.minimumTier())
        && goal.candidates().stream()
                .filter(c -> tiers.getOrDefault(c.family(), Integer.MAX_VALUE) <= c.minimumTier())
                .count()
            >= goal.candidateCount();
  }

  private void sequences(
      ItemState.Rarity rarity,
      int count,
      int level,
      List<WorkbenchCurrency> prefix,
      List<List<WorkbenchCurrency>> results) {
    if (count >= 6) {
      results.add(List.copyOf(prefix));
      return;
    }
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_TRANSMUTATION,
            WorkbenchCurrency.GREATER_TRANSMUTATION,
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.PERFECT_AUGMENTATION,
            WorkbenchCurrency.GREATER_AUGMENTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.PERFECT_REGAL,
            WorkbenchCurrency.GREATER_REGAL,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.PERFECT_EXALTED,
            WorkbenchCurrency.GREATER_EXALTED,
            WorkbenchCurrency.EXALTED)) {
      if (level < action.minimumModifierLevel()) continue;
      boolean eligible =
          switch (action.baseAction()) {
            case TRANSMUTATION -> rarity == ItemState.Rarity.NORMAL;
            case AUGMENTATION -> rarity == ItemState.Rarity.MAGIC && count < 2;
            case REGAL -> rarity == ItemState.Rarity.MAGIC;
            case EXALTED -> rarity == ItemState.Rarity.RARE;
            default -> false;
          };
      if (!eligible) continue;
      var nextRarity =
          action.baseAction() == CraftingAction.TRANSMUTATION
              ? ItemState.Rarity.MAGIC
              : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : rarity;
      prefix.add(action);
      sequences(nextRarity, count + 1, level, prefix, results);
      prefix.removeLast();
    }
  }

  /**
   * {@code SupportRecommendationService}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code occupied}: occupied ({@code long}).
   *   <li>{@code qualified}: qualified ({@code long}).
   *   <li>{@code rarity}: 현재 아이템 희귀도 ({@code ItemState.Rarity}).
   *   <li>{@code omens}: 징조목록 ({@code Set<String>}).
   * </ul>
   */
  private record Key(long occupied, long qualified, ItemState.Rarity rarity, Set<String> omens) {}

  /**
   * {@code SupportRecommendationService}에서 사용하는 확률질량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code omens}: 징조목록 ({@code Set<String>}).
   *   <li>{@code probability}: 확률 ({@code double}).
   * </ul>
   */
  private record Mass(StateBucket state, Set<String> omens, double probability) {}

  /**
   * {@code SupportRecommendationService} 내부의 계산예산 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code limits}: 한도목록 ({@code Limits}).
   *   <li>{@code start}: 시작값 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code int}).
   *   <li>{@code edges}: 간선목록 ({@code int}).
   *   <li>{@code localStateLimit}: 로컬·상태·한도 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Budget(limits, start)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code limits}(한도목록), {@code
   *       start}(시작값).
   *   <li>{@code globallyExhausted()}: globally·Exhausted 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code exhausted()}: exhausted 값을 조회·구성한다. 반환: {@code boolean}.
   * </ul>
   */
  private static final class Budget {
    final Limits limits;
    final long start;
    int states, edges;
    int localStateLimit = Integer.MAX_VALUE;

    Budget(Limits limits, long start) {
      this.limits = limits;
      this.start = start;
    }

    boolean globallyExhausted() {
      return states >= limits.maxStates()
          || edges >= limits.maxEdges()
          || System.nanoTime() - start >= limits.maxMillis() * 1000000L
          || Thread.currentThread().isInterrupted();
    }

    boolean exhausted() {
      return states >= localStateLimit || globallyExhausted();
    }
  }

  /**
   * {@code SupportRecommendationService}에서 사용하는 한도목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code maxStates}: 최대값·상태목록 ({@code int}).
   *   <li>{@code maxEdges}: 최대값·간선목록 ({@code int}).
   *   <li>{@code maxMillis}: 최대값·밀리초 ({@code int}).
   * </ul>
   */
  public record Limits(int maxStates, int maxEdges, int maxMillis) {}

  /**
   * {@code SupportRecommendationService}에서 사용하는 단계의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code step}: 단계 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code firstHitProbability}: 최초·목표도달·확률 ({@code double}).
   *   <li>{@code blockedProbability}: 차단·확률 ({@code double}).
   *   <li>{@code continuingOrUnresolvedProbability}: continuing·Or·미해결·확률 ({@code double}).
   *   <li>{@code blockedReasons}: 차단·이유목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Step(step, action, firstHitProbability, blockedProbability,
   *       continuingOrUnresolvedProbability, blockedReasons)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       step}(단계), {@code action}(행동), {@code firstHitProbability}(최초·목표도달·확률), {@code
   *       blockedProbability}(차단·확률), {@code
   *       continuingOrUnresolvedProbability}(continuing·Or·미해결·확률), {@code
   *       blockedReasons}(차단·이유목록).
   * </ul>
   */
  public record Step(
      int step,
      WorkbenchCurrency action,
      double firstHitProbability,
      double blockedProbability,
      double continuingOrUnresolvedProbability,
      List<String> blockedReasons) {
    public Step {
      blockedReasons = List.copyOf(blockedReasons);
    }
  }

  /**
   * {@code SupportRecommendationService}에서 사용하는 비교의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sequence}: 순서 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code successLower}: 성공·하한 ({@code double}).
   *   <li>{@code successUpper}: 성공·상한 ({@code double}).
   *   <li>{@code failureProbability}: 실패·확률 ({@code double}).
   *   <li>{@code unresolvedProbability}: 미해결·확률 ({@code double}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code steps}: 단계목록 ({@code List<Step>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Comparison(sequence, successLower, successUpper, failureProbability,
   *       unresolvedProbability, complete, steps)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code sequence}(순서),
   *       {@code successLower}(성공·하한), {@code successUpper}(성공·상한), {@code
   *       failureProbability}(실패·확률), {@code unresolvedProbability}(미해결·확률), {@code complete}(완료),
   *       {@code steps}(단계목록).
   * </ul>
   */
  public record Comparison(
      List<WorkbenchCurrency> sequence,
      double successLower,
      double successUpper,
      double failureProbability,
      double unresolvedProbability,
      boolean complete,
      List<Step> steps) {
    public Comparison {
      sequence = List.copyOf(sequence);
      steps = List.copyOf(steps);
    }
  }

  /**
   * {@code SupportRecommendationService}에서 사용하는 Report의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code transitionNamespace}: 전이·이름공간 ({@code String}).
   *   <li>{@code assessment}: 평가 ({@code SupportGoals.Assessment}).
   *   <li>{@code comparisons}: comparisons ({@code List<Comparison>}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code rankingCertified}: 순위·확률구간 ({@code boolean}).
   *   <li>{@code comparedSequences}: compared·순서목록 ({@code int}).
   *   <li>{@code totalSequences}: 합계·순서목록 ({@code int}).
   *   <li>{@code expandedStates}: 확장된·상태목록 ({@code int}).
   *   <li>{@code expandedEdges}: 확장된·간선목록 ({@code int}).
   *   <li>{@code elapsedMillis}: elapsed·밀리초 ({@code double}).
   *   <li>{@code cache}: 캐시 ({@code AdditionPoolCache.Stats}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Report(ruleVersion, ledgerVersion, transitionNamespace, assessment, comparisons,
   *       complete, rankingCertified, comparedSequences, totalSequences, expandedStates,
   *       expandedEdges, elapsedMillis, cache)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       ruleVersion}(규칙·버전), {@code ledgerVersion}(규칙장부·버전), {@code
   *       transitionNamespace}(전이·이름공간), {@code assessment}(평가), {@code comparisons}(comparisons),
   *       {@code complete}(완료), {@code rankingCertified}(순위·확률구간), {@code
   *       comparedSequences}(compared·순서목록), {@code totalSequences}(합계·순서목록), {@code
   *       expandedStates}(확장된·상태목록), {@code expandedEdges}(확장된·간선목록), {@code
   *       elapsedMillis}(elapsed·밀리초), {@code cache}(캐시).
   * </ul>
   */
  public record Report(
      String ruleVersion,
      String ledgerVersion,
      String transitionNamespace,
      SupportGoals.Assessment assessment,
      List<Comparison> comparisons,
      boolean complete,
      boolean rankingCertified,
      int comparedSequences,
      int totalSequences,
      int expandedStates,
      int expandedEdges,
      double elapsedMillis,
      AdditionPoolCache.Stats cache) {
    public Report {
      comparisons = List.copyOf(comparisons);
    }
  }
}
