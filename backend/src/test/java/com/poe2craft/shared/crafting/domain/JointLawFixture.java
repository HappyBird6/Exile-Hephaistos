package com.poe2craft.shared.crafting.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.RollPartition;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.math.BigDecimal;
import java.util.*;

/**
 * 결합·분포·검증사례 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Independently enumerable weighted-goal catalog; no production inventory is changed.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code policy}: 정책 ({@code AutomaticCurrencyPolicy}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code root}: 시작점 ({@code ItemState}).
 *   <li>{@code goal}: 목표 ({@code GoalFilter}).
 *   <li>{@code kernel}: 전이커널 ({@code AutomaticCraftingKernel}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code JointLawFixture()}: 전달받은 값으로 구성원을 초기화한다.
 *   <li>{@code value(item, stat)}: 값 처리에서 {@code item.explicits}, {@code m.values}을(를) 사용한다. 입력:
 *       {@code item}(아이템), {@code stat}(수치). 반환: {@code long}.
 *   <li>{@code evaluate(item)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code item}(아이템). 반환: {@code
 *       AutomaticMethodGraph.Progress}.
 *   <li>{@code graph()}: 그래프 값을 조회·구성한다. 반환: {@code AutomaticMethodGraph}.
 *   <li>{@code exhaust(graph)}: exhaust 처리에서 {@code graph.finished}, {@code
 *       graph.resourceLimited}을(를) 사용한다. 입력: {@code graph}(그래프).
 * </ul>
 */
public final class JointLawFixture {
  public final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  public final ItemCatalog catalog;
  public final ItemState root;
  public final GoalFilter goal;
  public final AutomaticCraftingKernel kernel;

  public JointLawFixture() {
    var tiny = new BasicCurrencyFixture();
    var definitions = new ArrayList<ModifierDefinition>();
    definitions.add(tiny.implicit);
    for (var stat : List.of("a", "b", "c"))
      for (int value : stat.equals("c") ? List.of(1) : List.of(1, 4)) {
        definitions.add(
            new ModifierDefinition(
                stat + value,
                stat + value,
                ModifierDefinition.Layer.EXPLICIT,
                stat.equals("b")
                    ? ModifierDefinition.AffixType.SUFFIX
                    : ModifierDefinition.AffixType.PREFIX,
                Set.of(stat),
                1,
                1,
                value,
                stat,
                List.of(new ModifierDefinition.StatRange(stat, value, value)),
                Set.of(),
                "https://example.test/" + stat + value));
      }
    catalog = tiny.catalog(definitions);
    root = tiny.item(ItemState.Rarity.NORMAL, List.of());
    var entries =
        List.of("a", "b").stream()
            .map(stat -> new GoalFilter.Entry(stat, stat, "flat", null, BigDecimal.ONE, false))
            .toList();
    goal =
        new GoalFilter(
            1,
            "joint-test",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group(
                    "sum",
                    GoalFilter.Type.WEIGHTED_V1,
                    false,
                    new GoalFilter.Range(BigDecimal.valueOf(5), null),
                    entries),
                new GoalFilter.Group(
                    "third",
                    GoalFilter.Type.AND,
                    false,
                    null,
                    List.of(
                        new GoalFilter.Entry(
                            "c",
                            "c",
                            "flat",
                            new GoalFilter.Range(BigDecimal.ONE, null),
                            null,
                            false)))));
    var known = new HashMap<String, GoalCatalog.Stat>();
    for (var stat : List.of("a", "b", "c"))
      known.put(
          stat,
          new GoalCatalog.Stat(
              stat,
              stat,
              "flat",
              GoalCatalog.Kind.EXPLICIT,
              new GoalCatalog.Support(
                  GoalFilter.Capability.SUPPORTED, GoalFilter.Capability.SUPPORTED, null),
              true,
              null,
              List.of(stat),
              List.of(),
              List.of()));
    kernel =
        new AutomaticCraftingKernel(
                policy,
                catalog,
                new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
                "amulets")
            .withRollPartition(RollPartition.compile(catalog, known, goal, root), goal.toString());
  }

  public long value(ItemState item, String stat) {
    return item.explicits().stream().mapToLong(m -> m.values().getOrDefault(stat, 0L)).sum();
  }

  public AutomaticMethodGraph.Progress evaluate(ItemState item) {
    boolean sum = value(item, "a") + value(item, "b") >= 5, third = value(item, "c") >= 1;
    return new AutomaticMethodGraph.Progress(
        sum && third && item.rarity() == ItemState.Rarity.RARE
            ? GoalFilter.Status.MATCH
            : GoalFilter.Status.NO_MATCH,
        List.of(
            sum ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
            third ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH));
  }

  public AutomaticMethodGraph graph() {
    return new AutomaticMethodGraph(
        root,
        policy,
        kernel,
        this::evaluate,
        CertifiedInterval.Precision.standard(),
        64,
        128,
        20000,
        5000);
  }

  public void exhaust(AutomaticMethodGraph graph) {
    for (int steps = 0; !graph.finished() && !graph.resourceLimited() && steps < 100000; steps++) {
      graph.step();
      if (steps % 100 == 0) graph.publishProgress();
    }
    graph.publishProgress();
  }
}
