package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.item.ModifierDefinition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 완성 예시 탐색의 보수적 가지치기다. groups는 활성 거래소 조건, terms는 각 목표 행에 기여하는 실제 계층·수치·단위다. 구간 안의 모든 값이 가능하다고 가정하고,
 * 이 가정으로도 실패할 때만 가지를 제거한다. 최종 일치는 실제 아이템 판정기가 결정한다.
 */
final class GoalExampleBounds {
  private final List<GoalFilter.Group> groups;
  private final Map<String, List<Term>> terms = new LinkedHashMap<>();
  private final Set<String> numericRows = new HashSet<>();

  /** 하나의 원천 수치가 어느 계층에서 어떤 계수·단위로 목표 표시값에 더해지는지 나타낸다. */
  private record Term(String layer, String raw, BigDecimal factor, BigDecimal divisor) {}

  /** 존재하는 수치의 가능한 최소·최대다. 수치 자체가 없을 때는 행을 맵에서 생략한다. */
  record Band(BigDecimal low, BigDecimal high) {
    private static final Band ZERO = new Band(BigDecimal.ZERO, BigDecimal.ZERO);

    /** 한 아이템에 함께 적용되는 두 기여를 더한다. */
    Band plus(Band other) {
      return new Band(low.add(other.low), high.add(other.high));
    }

    /** 서로 대체할 수 있는 티어 또는 점수의 전체 범위를 감싼다. */
    Band hull(Band other) {
      return new Band(low.min(other.low), high.max(other.high));
    }

    /** 이 슬롯을 선택하지 않는 값 0까지 포함한다. */
    Band includingZero() {
      return hull(ZERO);
    }

    /** 음수 가중치에서도 경계를 오름차순으로 유지한다. */
    Band times(BigDecimal factor) {
      return factor.signum() < 0
          ? new Band(high.multiply(factor), low.multiply(factor))
          : new Band(low.multiply(factor), high.multiply(factor));
    }

    /** 목표 범위와 교차할 가능성이 있는지 판정한다. */
    boolean intersects(GoalFilter.Range range) {
      return (range.min() == null || high.compareTo(range.min()) >= 0)
          && (range.max() == null || low.compareTo(range.max()) <= 0);
    }

    /** 가능한 값이 모두 행의 범위 안에 속하는지 판정한다. */
    boolean inside(GoalFilter.Range range) {
      return range.contains(low) && range.contains(high);
    }
  }

  /** 직접·의사 수치 모두 기존 목표 카탈로그의 계층, 원천, 계수, 단위를 사용한다. */
  GoalExampleBounds(GoalFilter goal, Map<String, GoalCatalog.Stat> stats) {
    groups = goal.groups().stream().filter(GoalFilter.Group::active).toList();
    for (var group : groups)
      for (var row : group.entries()) {
        if (row.disabled()) continue;
        if (row.range().min() != null
            || row.range().max() != null
            || group.weighted()
                && (group.range().min() != null || group.range().max() != null)
                && group.scoreWeight(row).signum() != 0) numericRows.add(row.statId());
        if (terms.containsKey(row.statId())) continue;
        var stat = stats.get(row.statId());
        var sources =
            stat.kind() == GoalCatalog.Kind.PSEUDO
                ? stat.contributions()
                : List.of(new GoalCatalog.Contribution(stat.statId(), BigDecimal.ONE));
        var list = new ArrayList<Term>();
        for (var source : sources)
          GoalCatalogIndex.mapping(stats.get(source.statId()))
              .ifPresent(
                  m ->
                      list.add(
                          new Term(
                              m.layer(),
                              m.sourceStatId(),
                              source.coefficient().multiply(BigDecimal.valueOf(m.numerator())),
                              BigDecimal.valueOf(m.denominator()))));
        terms.put(row.statId(), list);
      }
  }

  /** 범위·가중합이 보는 수치가 없으면 해당 티어의 모든 실제 굴림은 존재 여부 판정에서 동치다. */
  boolean numeric(Map<String, Band> contribution) {
    return !Collections.disjoint(numericRows, contribution.keySet());
  }

  /** 한 티어의 전체 범위 또는 주어진 실제 굴림을 목표 행 단위로 투영한다. 나눗셈은 바깥 방향으로만 반올림한다. */
  Map<String, Band> project(ModifierDefinition definition, Map<String, Long> values) {
    var result = new HashMap<String, Band>();
    for (var entry : terms.entrySet())
      for (var term : entry.getValue()) {
        if (!definition.layer().name().equals(term.layer())) continue;
        for (var stat : definition.stats())
          if (stat.id().equals(term.raw())) {
            var raw =
                values == null
                    ? new Band(BigDecimal.valueOf(stat.min()), BigDecimal.valueOf(stat.max()))
                    : new Band(
                        BigDecimal.valueOf(values.get(stat.id())),
                        BigDecimal.valueOf(values.get(stat.id())));
            var amount = raw.times(term.factor());
            if (term.divisor().compareTo(BigDecimal.ONE) != 0)
              amount =
                  new Band(
                      amount.low().divide(term.divisor(), 24, RoundingMode.FLOOR),
                      amount.high().divide(term.divisor(), 24, RoundingMode.CEILING));
            result.merge(entry.getKey(), amount, Band::plus);
          }
      }
    return result;
  }

  /** 서로 다른 슬롯이 함께 기여할 수 있는 범위를 더한다. */
  static Map<String, Band> sum(Map<String, Band> left, Map<String, Band> right) {
    var total = new HashMap<>(left);
    right.forEach((id, band) -> total.merge(id, band, Band::plus));
    return total;
  }

  /** 실패가 확정된 조건을 바꿀 수 있는 행을 반환한다. 빈 집합은 실제 굴림을 검사할 가치가 있다는 뜻이다. */
  Set<String> blocked(Map<String, Band> values) {
    return blocked(values, values.keySet());
  }

  /** 앞으로 붙일 수도 있는 옵션까지 포함한 바깥 구간이다. 아직 붙지 않은 행은 불일치하거나 점수 0일 수도 있다. */
  Set<String> blocked(Map<String, Band> values, Set<String> guaranteed) {
    for (var group : groups) {
      int certain = 0, possible = 0, present = 0, active = 0;
      var score = Band.ZERO;
      for (var row : group.entries()) {
        if (row.disabled()) continue;
        active++;
        var value = values.get(row.statId());
        boolean can = value != null && value.intersects(row.range());
        boolean certainPresence = guaranteed.contains(row.statId());
        boolean must = certainPresence && value.inside(row.range());
        if (can) possible++;
        if (must) certain++;
        if (value != null) present++;
        switch (group.type()) {
          case AND -> {
            if (!can) return Set.of(row.statId());
          }
          case NOT -> {
            if (must) return Set.of(row.statId());
          }
          case IF, WEIGHTED_V1 -> {
            if (certainPresence && !can) return Set.of(row.statId());
          }
          default -> {}
        }
        if (group.weighted() && can) {
          // v2에서는 유효 구간 밖의 굴림이 0점이 될 수 있다.
          var contribution =
              new Band(
                      row.range().min() == null ? value.low() : value.low().max(row.range().min()),
                      row.range().max() == null
                          ? value.high()
                          : value.high().min(row.range().max()))
                  .times(group.scoreWeight(row));
          score =
              score.plus(
                  !certainPresence || group.type() == GoalFilter.Type.WEIGHTED_V2 && !must
                      ? contribution.hull(Band.ZERO)
                      : contribution);
        }
      }
      if (group.type() == GoalFilter.Type.COUNT
              && !new Band(BigDecimal.valueOf(certain), BigDecimal.valueOf(possible))
                  .intersects(group.range().forCount(active))
          || group.weighted()
              && (!score.intersects(group.range())
                  || present == 0 && group.type() == GoalFilter.Type.WEIGHTED_V2)) {
        var rows = new HashSet<String>();
        for (var row : group.entries()) if (!row.disabled()) rows.add(row.statId());
        return rows;
      }
    }
    return Set.of();
  }
}
