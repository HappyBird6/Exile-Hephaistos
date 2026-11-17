package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph.Progress;
import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 자동 그래프의 상태에 수동 훼손 선택 경로를 연결한다.
 *
 * <p>기존 계약: Manual-choice routes attached to an exact automatic graph node, with no reveal
 * distribution.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, Progress>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssStrategyPlanner(catalog, simulator, evaluate)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code simulator}(제작시뮬레이터), {@code evaluate}(평가).
 *   <li>{@code recommend(item, inventory)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code item}(아이템), {@code
 *       inventory}(등록자료). 반환: {@code List<Strategy>}.
 *   <li>{@code safe(plan, before)}: 안전범위 처리에서 {@code plan.branches}, {@code evaluate.apply}을(를)
 *       사용한다. 입력: {@code plan}(계획), {@code before}(변경전). 반환: {@code int}.
 *   <li>{@code matched(progress)}: 일치한 처리에서 {@code progress.entries}을(를) 사용한다. 입력: {@code
 *       progress}(진행상황). 반환: {@code long}.
 *   <li>{@code preserves(before, after)}: preserves 처리에서 {@code before.entries}, {@code
 *       after.entries}을(를) 사용한다. 입력: {@code before}(변경전), {@code after}(변경후). 반환: {@code boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Kind}: {@code AbyssStrategyPlanner}의 종류 구분값을 정의한다.
 *   <li>{@code Protection}: {@code AbyssStrategyPlanner}에서 사용하는 보호정책의 불변 값이다.
 *   <li>{@code Strategy}: {@code AbyssStrategyPlanner}에서 사용하는 전략의 불변 값이다.
 * </ul>
 */
public final class AbyssStrategyPlanner {
  /**
   * {@code AbyssStrategyPlanner}의 종류 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code COMPLETE_SIDE}: 접두접미 구분값.
   *   <li>{@code BLOCK_HIGH_WEIGHT}: 가중치 구분값.
   *   <li>{@code SHARE_REMOVAL_RISK}: RISK 구분값.
   * </ul>
   */
  public enum Kind {
    COMPLETE_SIDE,
    BLOCK_HIGH_WEIGHT,
    SHARE_REMOVAL_RISK
  }

  /**
   * {@code AbyssStrategyPlanner}에서 사용하는 보호정책의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code safeBefore}: 안전범위·변경전 ({@code int}).
   *   <li>{@code totalBefore}: 합계·변경전 ({@code int}).
   *   <li>{@code safeAfter}: 안전범위·변경후 ({@code int}).
   *   <li>{@code totalAfter}: 합계·변경후 ({@code int}).
   * </ul>
   */
  public record Protection(int safeBefore, int totalBefore, int safeAfter, int totalAfter) {}

  /**
   * {@code AbyssStrategyPlanner}에서 사용하는 전략의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code Kind}).
   *   <li>{@code prepared}: 준비된 ({@code AbyssCraftingState}).
   *   <li>{@code nextCurrency}: 다음·화폐 ({@code WorkbenchCurrency}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code targetModifierIds}: 대상·옵션·식별자목록 ({@code Set<String>}).
   *   <li>{@code modifierSelection}: 옵션·선택 ({@code AbyssProbabilityComparison.Comparison}).
   *   <li>{@code removalProtection}: 제거·보호정책 ({@code Protection}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Strategy(kind, prepared, nextCurrency, activeOmens, targetModifierIds,
   *       modifierSelection, removalProtection)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code kind}(종류), {@code
   *       prepared}(준비된), {@code nextCurrency}(다음·화폐), {@code activeOmens}(활성·징조목록), {@code
   *       targetModifierIds}(대상·옵션·식별자목록), {@code modifierSelection}(옵션·선택), {@code
   *       removalProtection}(제거·보호정책).
   * </ul>
   */
  public record Strategy(
      Kind kind,
      AbyssCraftingState prepared,
      WorkbenchCurrency nextCurrency,
      Set<String> activeOmens,
      Set<String> targetModifierIds,
      AbyssProbabilityComparison.Comparison modifierSelection,
      Protection removalProtection) {
    public Strategy {
      activeOmens = Set.copyOf(activeOmens);
      targetModifierIds = Set.copyOf(targetModifierIds);
    }
  }

  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final Function<ItemState, Progress> evaluate;

  public AbyssStrategyPlanner(
      ItemCatalog catalog, WorkbenchSimulator simulator, Function<ItemState, Progress> evaluate) {
    this.catalog = catalog;
    this.simulator = simulator;
    this.evaluate = evaluate;
  }

  public List<Strategy> recommend(ItemState item, List<AutomaticCraftingAction> inventory) {
    if (item.rarity() != ItemState.Rarity.RARE
        || item.explicits().size() >= 6
        || item.catalystQuality() != null
        || BasicJewel.supportedCrafting(item.baseItemId())) return List.of();
    var source = AbyssCraftingState.from(item, catalog);
    var before = evaluate.apply(item);
    if (before.goal() == Status.MATCH
        || before.goal() == Status.UNSUPPORTED
        || before.goal() == Status.UNKNOWN) return List.of();
    var useful =
        source.removalCandidates(null).stream()
            .filter(
                s ->
                    !preserves(
                        before, evaluate.apply(source.remove(s.id(), null, catalog).knownItem())))
            .map(AbyssCraftingState.Slot::id)
            .collect(Collectors.toSet());
    // Fractured useful affixes still count for side completion, but never for removal risk.
    for (var slot : source.slots()) {
      if (slot.modifier() != null && slot.modifier().fractured()) {
        var without =
            new AbyssCraftingState(
                source.item(),
                source.slots().stream().filter(s -> !s.id().equals(slot.id())).toList(),
                null);
        if (!preserves(before, evaluate.apply(without.knownItem()))) useful.add(slot.id());
      }
    }
    var result = new ArrayList<Strategy>();
    boolean exalted =
        inventory.stream()
            .anyMatch(a -> a.currency() == WorkbenchCurrency.EXALTED && a.omens().isEmpty());
    var additions = new AbyssProbabilityComparison(catalog);
    for (var side : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      String id = "abyss-preparation-" + side.name().toLowerCase(Locale.ROOT);
      AbyssCraftingState prepared;
      try {
        prepared = source.addUnrevealed(id, side, catalog);
      } catch (IllegalArgumentException full) {
        continue;
      }
      if (exalted) {
        var pool = additions.pool(prepared, WorkbenchCurrency.EXALTED, null);
        if (source.count(side) == 2
            && source.slots().stream()
                    .filter(s -> s.side() == side && useful.contains(s.id()))
                    .count()
                == 2
            && !pool.isEmpty()) {
          result.add(
              new Strategy(
                  Kind.COMPLETE_SIDE,
                  prepared,
                  WorkbenchCurrency.EXALTED,
                  Set.of(),
                  Set.of(),
                  null,
                  null));
        }
        var targetIds = new TreeSet<String>();
        for (var candidate : pool) {
          var values =
              candidate.stats().stream()
                  .collect(
                      Collectors.toMap(
                          ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::max));
          var probe =
              prepared.append("goal-probe", new ModifierInstance(candidate.id(), values), catalog);
          var after = evaluate.apply(probe.knownItem());
          if (preserves(before, after) && matched(after) > matched(before))
            targetIds.add(candidate.id());
        }
        if (!targetIds.isEmpty()) {
          var comparison =
              additions.compare(prepared, id, WorkbenchCurrency.EXALTED, null, targetIds);
          if (comparison.isPresent()
              && comparison.get().afterBlocking().percent() > comparison.get().baseline().percent())
            result.add(
                new Strategy(
                    Kind.BLOCK_HIGH_WEIGHT,
                    prepared,
                    WorkbenchCurrency.EXALTED,
                    Set.of(),
                    targetIds,
                    comparison.get(),
                    null));
        }
      }
      if (useful.isEmpty()) continue;
      var seen = new HashSet<String>();
      for (var action : inventory) {
        if (action.currency().baseAction() != null
            || action.currency() == WorkbenchCurrency.ESSENCE_ABYSS
            || !seen.add(action.currency() + ":" + action.omenIds())) continue;
        var baseline = simulator.abyssPlan(source, action.currency(), action.omenIds());
        var next = simulator.abyssPlan(prepared, action.currency(), action.omenIds());
        if (!baseline.available() || !next.available()) continue;
        int safeBefore = safe(baseline, before), safeAfter = safe(next, before);
        if ((long) safeAfter * baseline.branches().size()
            <= (long) safeBefore * next.branches().size()) continue;
        result.add(
            new Strategy(
                Kind.SHARE_REMOVAL_RISK,
                prepared,
                action.currency(),
                action.omenIds(),
                Set.of(),
                null,
                new Protection(
                    safeBefore, baseline.branches().size(), safeAfter, next.branches().size())));
      }
    }
    return List.copyOf(result);
  }

  private int safe(AbyssCraftingPlan plan, Progress before) {
    return (int)
        plan.branches().stream()
            .filter(b -> preserves(before, evaluate.apply(b.remaining().knownItem())))
            .count();
  }

  private static long matched(Progress progress) {
    return progress.entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private static boolean preserves(Progress before, Progress after) {
    if (before.entries().size() != after.entries().size()) return false;
    for (int i = 0; i < before.entries().size(); i++)
      if (before.entries().get(i) == Status.MATCH && after.entries().get(i) != Status.MATCH)
        return false;
    return true;
  }
}
