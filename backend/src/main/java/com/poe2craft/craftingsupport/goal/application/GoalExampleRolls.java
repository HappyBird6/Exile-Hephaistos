package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.CoupledStatRollModel;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** 목표 경계에 가까운 실제 굴림을 찾는다. goal과 stats는 요청의 목표와 활성 직접·의사 수치, simulator는 공유 굴림 모델이다. */
final class GoalExampleRolls {
  private final GoalFilter goal;
  private final Map<String, GoalCatalog.Stat> stats;
  private final WorkbenchSimulator simulator;

  /** 요청의 기존 목표 판정 단위와 해당 베이스 시뮬레이터를 보관한다. */
  GoalExampleRolls(
      GoalFilter goal, Map<String, GoalCatalog.Stat> stats, WorkbenchSimulator simulator) {
    this.goal = goal;
    this.stats = stats;
    this.simulator = simulator;
  }

  /** 활성 행과 가중합에서 부족한 양을 각 직접 옵션의 내부 단위로 변환해 탐색할 수치 후보를 반환한다. */
  Map<String, Set<BigDecimal>> targets(GoalEvaluator.Evaluation evaluation) {
    var targets = new LinkedHashMap<String, Set<BigDecimal>>();
    int groupIndex = 0;
    for (var group : goal.groups()) {
      if (group.disabled()) continue;
      var result = evaluation == null ? null : evaluation.groups().get(groupIndex);
      groupIndex++;
      int row = 0;
      for (var entry : group.entries()) {
        if (entry.disabled()) continue;
        var stat = stats.get(entry.statId());
        var current = result == null ? null : result.entries().get(row).value();
        row++;
        if (stat == null) continue;
        var direct =
            stat.kind() == GoalCatalog.Kind.PSEUDO
                ? stat.contributions()
                : List.of(new GoalCatalog.Contribution(stat.statId(), BigDecimal.ONE));
        for (var source : direct) {
          var sourceStat = stats.get(source.statId());
          if (sourceStat == null || GoalCatalogIndex.mapping(sourceStat).isEmpty()) continue;
          var divisor =
              BigDecimal.valueOf(
                  sourceStat.sourceDivisor() == null ? 1 : sourceStat.sourceDivisor());
          var values = targets.computeIfAbsent(source.statId(), key -> new LinkedHashSet<>());
          addBounds(values, entry.range(), current, divisor, source.coefficient());
          if (entry.weight() != null && entry.weight().signum() != 0)
            addBounds(
                values,
                group.range(),
                result == null ? null : result.score(),
                divisor,
                source.coefficient().multiply(entry.weight()));
        }
      }
    }
    return targets;
  }

  /** 범위의 각 경계에서 현재 합계를 빼고 단위·가중치를 되돌린다. 근사치는 후보 선정에만 쓰이고 성공 판정은 기존 판정기가 담당한다. */
  private static void addBounds(
      Set<BigDecimal> values,
      GoalFilter.Range range,
      BigDecimal current,
      BigDecimal divisor,
      BigDecimal factor) {
    if (range == null || factor.signum() == 0) return;
    for (var bound : Arrays.asList(range.min(), range.max()))
      if (bound != null)
        values.add(
            bound
                .subtract(current == null ? BigDecimal.ZERO : current)
                .multiply(divisor)
                .divide(factor, 12, RoundingMode.HALF_UP));
  }

  /** 직접 수치 식별자와 목표의 원천 수치가 겹치는 옵션만 조합 후보로 선택한다. */
  static boolean relevant(ModifierDefinition definition, Map<String, Set<BigDecimal>> targets) {
    return definition.stats().stream()
        .anyMatch(
            stat -> targets.containsKey(GoalCatalogIndex.id(definition.layer().name(), stat.id())));
  }

  /** 단일 수치는 실제 티어 범위 안으로 제한하고, 복합 수치는 Workbench의 동일 결합 굴림에서만 후보를 고른다. */
  List<ModifierInstance> variants(
      ModifierDefinition definition, Map<String, Set<BigDecimal>> targets) {
    List<CoupledStatRollModel.Cell> joint =
        definition.stats().size() > 1
            ? simulator.automaticJointRolls(definition).orElse(List.of())
            : List.of();
    if (definition.stats().size() > 1 && joint.isEmpty()) return List.of();
    var variants = new LinkedHashSet<ModifierInstance>();
    for (var stat : definition.stats()) {
      var values = new LinkedHashSet<Long>();
      for (var target :
          targets.getOrDefault(
              GoalCatalogIndex.id(definition.layer().name(), stat.id()), Set.of())) {
        long clamped =
            target
                .max(BigDecimal.valueOf(stat.min()))
                .min(BigDecimal.valueOf(stat.max()))
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
        values.add(clamped);
        if (clamped < stat.max()) values.add(clamped + 1);
        if (clamped > stat.min()) values.add(clamped - 1);
      }
      values.add(stat.max());
      values.add(stat.min());
      for (long value : values) {
        if (joint.isEmpty())
          variants.add(new ModifierInstance(definition.id(), Map.of(stat.id(), value)));
        else {
          // 동일 비율로 오름차순 생성된 결합 셀에서 경계 양옆의 실제 수치 튜플을 유지한다.
          int lo = 0, hi = joint.size() - 1;
          while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (joint.get(mid).modifier().values().get(stat.id()) < value) lo = mid + 1;
            else hi = mid;
          }
          variants.add(joint.get(lo).modifier());
          if (lo > 0) variants.add(joint.get(lo - 1).modifier());
        }
      }
    }
    return List.copyOf(variants);
  }
}
