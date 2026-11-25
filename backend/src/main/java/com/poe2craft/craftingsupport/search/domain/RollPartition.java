package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import java.math.*;
import java.util.*;

/**
 * 작업의 수치 영역을 서로 겹치지 않는 구간으로 나눈다.
 *
 * <p>기존 계약: Job-fixed disjoint intervals; uncertain numeric dependencies retain individual rolls.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "goal-reviewed-positive-sum-partition-v7"} ({@code
 *       String}).
 *   <li>{@code exact}: 정확값 ({@code boolean}).
 *   <li>{@code initial}: 초기값 ({@code ItemState}).
 *   <li>{@code relevant}: 관련된 ({@code Set<String>}).
 *   <li>{@code intervalSafe}: 구간·안전범위 ({@code Set<String>}).
 *   <li>{@code cuts}: 분할점 ({@code Map<String, NavigableSet<BigInteger>>}).
 *   <li>{@code automaticCuts}: 자동·분할점 ({@code Map<String, NavigableSet<BigInteger>>}).
 *   <li>{@code tiers}: 티어목록 ({@code TierPartition}).
 *   <li>{@code orbits}: 교환가능계열목록 ({@code OrbitPartition}).
 *   <li>{@code actions}: 행동목록 ({@code Set<WorkbenchCurrency>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RollPartition(exact, relevant, intervalSafe, cuts)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code exact}(정확값), {@code relevant}(관련된), {@code intervalSafe}(구간·안전범위), {@code
 *       cuts}(분할점).
 *   <li>{@code RollPartition(exact, relevant, intervalSafe, cuts, tiers, orbits, actions)}: 기본 정책을
 *       보충해 공통 생성자로 위임한다. 입력: {@code exact}(정확값), {@code relevant}(관련된), {@code
 *       intervalSafe}(구간·안전범위), {@code cuts}(분할점), {@code tiers}(티어목록), {@code orbits}(교환가능계열목록),
 *       {@code actions}(행동목록).
 *   <li>{@code RollPartition(exact, relevant, intervalSafe, cuts, tiers, orbits, actions,
 *       initial)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code exact}(정확값), {@code relevant}(관련된), {@code
 *       intervalSafe}(구간·안전범위), {@code cuts}(분할점), {@code tiers}(티어목록), {@code orbits}(교환가능계열목록),
 *       {@code actions}(행동목록), {@code initial}(초기값).
 *   <li>{@code RollPartition(exact, relevant, intervalSafe, cuts, tiers, orbits, actions, initial,
 *       automaticCuts)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code exact}(정확값), {@code relevant}(관련된), {@code
 *       intervalSafe}(구간·안전범위), {@code cuts}(분할점), {@code tiers}(티어목록), {@code orbits}(교환가능계열목록),
 *       {@code actions}(행동목록), {@code initial}(초기값), {@code automaticCuts}(자동·분할점).
 *   <li>{@code initial()}: 제작을 시작할 초기 상태를 만든다. 반환: {@code ItemState}.
 *   <li>{@code withInitial(root)}: 초기값을(를) 반영한 값을 구한다. 입력: {@code root}(시작점). 반환: {@code
 *       RollPartition}.
 *   <li>{@code automaticRegion(stat, value)}: 자동·영역 처리에서 {@code relevant.contains}, {@code
 *       intervalSafe.contains}을(를) 사용한다. 입력: {@code stat}(수치), {@code value}(값). 반환: {@code
 *       Object}.
 *   <li>{@code exact()}: 정확값 값을 조회·구성한다. 반환: {@code RollPartition}.
 *   <li>{@code compile(catalog, known, goal, root)}: 입력 조건을 재사용할 평가·실행 표현으로 바꾼다. 입력: {@code
 *       catalog}(카탈로그), {@code known}(알려진), {@code goal}(목표), {@code root}(시작점). 반환: {@code
 *       RollPartition}.
 *   <li>{@code raw(value, mapping, rounding)}: 원문 처리에서 {@code value.multiply}, {@code
 *       BigDecimal.valueOf}을(를) 사용한다. 입력: {@code value}(값), {@code mapping}(매핑), {@code
 *       rounding}(rounding). 반환: {@code BigInteger}.
 *   <li>{@code end(candidate, first)}: end 처리에서 {@code candidate.stats}, {@code
 *       relevant.contains}을(를) 사용한다. 입력: {@code candidate}(후보), {@code first}(최초). 반환: {@code
 *       long}.
 *   <li>{@code tiers()}: {@code tiers} 값을 조회한다. 반환: {@code TierPartition}.
 *   <li>{@code orbits()}: {@code orbits} 값을 조회한다. 반환: {@code OrbitPartition}.
 *   <li>{@code allows(action)}: allows 처리에서 {@code actions.contains}을(를) 사용한다. 입력: {@code
 *       action}(행동). 반환: {@code boolean}.
 *   <li>{@code relevant(stat)}: 관련된 처리에서 {@code relevant.contains}을(를) 사용한다. 입력: {@code stat}(수치).
 *       반환: {@code boolean}.
 *   <li>{@code transitionRegion(definition, modifier)}: 전이·영역 처리에서 {@code definition.stats}, {@code
 *       modifier.values}을(를) 사용한다. 입력: {@code definition}(정의), {@code modifier}(옵션). 반환: {@code
 *       Object}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code tierRegion(stat, first)}: 티어·영역의 입력에 따른 분기·결과를 처리한다. 입력: {@code stat}(수치), {@code
 *       first}(최초). 반환: {@code Object}.
 *   <li>{@code region(stat, first, boundaries)}: 영역 처리에서 {@code relevant.contains}, {@code
 *       boundaries.getOrDefault}을(를) 사용한다. 입력: {@code stat}(수치), {@code first}(최초), {@code
 *       boundaries}(boundaries). 반환: {@code Object}.
 * </ul>
 */
public final class RollPartition {
  public static final String VERSION = "goal-reviewed-positive-sum-partition-v7";
  private final boolean exact;
  private final ItemState initial;
  private final Set<String> relevant, intervalSafe;
  private final Map<String, NavigableSet<BigInteger>> cuts;
  private final Map<String, NavigableSet<BigInteger>> automaticCuts;
  private final TierPartition tiers;
  private final OrbitPartition orbits;
  private final Set<WorkbenchCurrency> actions;

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts) {
    this(
        exact,
        relevant,
        intervalSafe,
        cuts,
        TierPartition.empty(),
        OrbitPartition.empty(),
        Set.copyOf(BasicCurrencyTransitions.supportedActions()));
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions) {
    this(exact, relevant, intervalSafe, cuts, tiers, orbits, actions, null);
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions,
      ItemState initial) {
    this(exact, relevant, intervalSafe, cuts, tiers, orbits, actions, initial, cuts);
  }

  private RollPartition(
      boolean exact,
      Set<String> relevant,
      Set<String> intervalSafe,
      Map<String, NavigableSet<BigInteger>> cuts,
      TierPartition tiers,
      OrbitPartition orbits,
      Set<WorkbenchCurrency> actions,
      ItemState initial,
      Map<String, NavigableSet<BigInteger>> automaticCuts) {
    this.initial = initial;
    this.exact = exact;
    this.relevant = Set.copyOf(relevant);
    this.intervalSafe = Set.copyOf(intervalSafe);
    this.cuts = new HashMap<>();
    this.automaticCuts = new HashMap<>();
    automaticCuts.forEach(
        (key, value) ->
            this.automaticCuts.put(
                key, Collections.unmodifiableNavigableSet(new TreeSet<>(value))));
    this.tiers = tiers;
    this.orbits = orbits;
    this.actions = Set.copyOf(actions);
    cuts.forEach(
        (key, value) ->
            this.cuts.put(key, Collections.unmodifiableNavigableSet(new TreeSet<>(value))));
  }

  public ItemState initial() {
    return initial;
  }

  private RollPartition withInitial(ItemState root) {
    return new RollPartition(
        exact, relevant, intervalSafe, cuts, tiers, orbits, actions, root, automaticCuts);
  }

  public Object automaticRegion(String stat, long value) {
    return exact || (relevant.contains(stat) && !intervalSafe.contains(stat))
        ? List.of(stat, value)
        : region(stat, value, automaticCuts);
  }

  public static RollPartition exact() {
    return new RollPartition(true, Set.of(), Set.of(), Map.of());
  }

  public static RollPartition compile(
      ItemCatalog catalog, Map<String, GoalCatalog.Stat> known, GoalFilter goal, ItemState root) {
    if (root.catalystQuality() != null) return exact().withInitial(root);
    var relevant = new HashSet<String>();
    var pending = new ArrayDeque<String>();
    goal.groups().stream()
        .filter(g -> !g.disabled())
        .flatMap(g -> g.entries().stream())
        .filter(e -> !e.disabled())
        .forEach(e -> pending.add(e.statId()));
    var seen = new HashSet<String>();
    while (!pending.isEmpty()) {
      var id = pending.remove();
      if (!seen.add(id)) continue;
      var stat = known.get(id);
      if (stat == null || stat.support().evaluation() != GoalFilter.Capability.SUPPORTED)
        return exact().withInitial(root);
      // 별세계 효과들은 같은 표시 수치 키를 공유한다. 개별 옵션·수치가 보존되는 정확한 상태로 비교한다.
      if (stat.sourceStatIds().stream()
          .anyMatch(
              idSource ->
                  catalog
                      .find(idSource)
                      .filter(d -> d.tags().contains("otherworldly"))
                      .isPresent())) return exact().withInitial(root);
      relevant.addAll(stat.sourceStatIds());
      stat.contributions().forEach(c -> pending.add(c.statId()));
    }
    var safe = new HashSet<String>();
    var cuts = new HashMap<String, NavigableSet<BigInteger>>();
    var exactSums = new HashSet<String>();
    boolean simple =
        goal.groups().stream()
            .filter(g -> !g.disabled())
            .allMatch(
                g ->
                    g.type() != GoalFilter.Type.WEIGHTED_V1
                        && g.type() != GoalFilter.Type.WEIGHTED_V2
                        && g.entries().stream()
                            .filter(e -> !e.disabled())
                            .allMatch(
                                e -> known.get(e.statId()).kind() == GoalCatalog.Kind.EXPLICIT));
    if (simple)
      for (var group : goal.groups())
        if (!group.disabled())
          for (var entry : group.entries())
            if (!entry.disabled()) {
              var mapping = GoalCatalogIndex.mapping(known.get(entry.statId()));
              if (mapping.isEmpty() || mapping.get().numerator() <= 0)
                return exact().withInitial(root);
              var m = mapping.get();
              var producers =
                  catalog.modifiers().values().stream()
                      .filter(
                          d ->
                              d.layer() == ModifierDefinition.Layer.EXPLICIT
                                  && d.stats().stream()
                                      .anyMatch(s -> s.id().equals(m.sourceStatId())))
                      .toList();
              if (producers.isEmpty()) continue;
              var common = new HashSet<>(producers.getFirst().familyIds());
              producers.forEach(d -> common.retainAll(d.familyIds()));
              if (common.isEmpty()) exactSums.add(m.sourceStatId());
              safe.add(m.sourceStatId());
              var boundaries = cuts.computeIfAbsent(m.sourceStatId(), ignored -> new TreeSet<>());
              if (entry.range() != null && entry.range().min() != null)
                boundaries.add(raw(entry.range().min(), m, RoundingMode.CEILING));
              if (entry.range() != null && entry.range().max() != null)
                boundaries.add(raw(entry.range().max(), m, RoundingMode.FLOOR).add(BigInteger.ONE));
            }
    // 함께 붙을 수 있는 양수 속성은 마지막 목표 경계까지 개별 값을 보존한다.
    // 그 이상에서는 다른 양수 속성을 더하거나 빼도 남아 있는 속성 하나만으로 경계를 넘는다.
    for (var stat : exactSums) {
      var boundaries = cuts.get(stat);
      var ranges =
          catalog.modifiers().values().stream()
              .flatMap(d -> d.stats().stream())
              .filter(r -> r.id().equals(stat))
              .toList();
      if (ranges.stream().anyMatch(r -> r.min() < 0)
          || root.explicits().stream().anyMatch(m -> m.values().getOrDefault(stat, 0L) < 0)
          || root.implicits().stream().anyMatch(m -> m.values().getOrDefault(stat, 0L) < 0)) {
        safe.remove(stat);
        continue;
      }
      if (boundaries.isEmpty()) continue;
      var last = boundaries.last();
      if (last.compareTo(BigInteger.valueOf(2048)) > 0) {
        safe.remove(stat);
        continue;
      }
      for (long value = 0; BigInteger.valueOf(value).compareTo(last) < 0; value++)
        boundaries.add(BigInteger.valueOf(value));
    }
    // A known point must remain disjoint in probability space. Its artificial
    // boundaries do not change the goal or any later transition, however.
    var automaticCuts = new HashMap<String, NavigableSet<BigInteger>>();
    cuts.forEach((stat, values) -> automaticCuts.put(stat, new TreeSet<>(values)));
    // Initial deterministic values must never overlap a generated uniform cell.
    for (var modifier : root.explicits())
      modifier
          .values()
          .forEach(
              (stat, value) -> {
                var boundaries = cuts.computeIfAbsent(stat, ignored -> new TreeSet<>());
                boundaries.add(BigInteger.valueOf(value));
                boundaries.add(BigInteger.valueOf(value).add(BigInteger.ONE));
              });
    var result =
        new RollPartition(
            false,
            relevant,
            safe,
            cuts,
            TierPartition.empty(),
            OrbitPartition.empty(),
            Set.copyOf(BasicCurrencyTransitions.supportedActions()),
            root,
            automaticCuts);
    // The shared minimum-level grouping does not yet support multi-family generation groups.
    if (!root.baseItemId().equals(SolarAmulet.BASE_ID)
        || !simple
        || !safe.containsAll(relevant)
        || catalog.modifiers().values().stream()
            .anyMatch(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.weight() > 0
                        && d.requiredItemLevel() <= root.itemLevel()
                        && d.familyIds().size() != 1)) return result;
    var eligibility = new BasicCandidateEligibility(catalog, root);
    return new RollPartition(
        false,
        relevant,
        safe,
        cuts,
        TierPartition.compile(catalog, root, result, eligibility),
        OrbitPartition.compile(catalog, root, result, eligibility),
        Set.copyOf(BasicCurrencyTransitions.reachableActions(root)),
        root,
        automaticCuts);
  }

  private static BigInteger raw(
      BigDecimal value, GoalCatalog.Mapping mapping, RoundingMode rounding) {
    return value
        .multiply(BigDecimal.valueOf(mapping.denominator()))
        .divide(BigDecimal.valueOf(mapping.numerator()), 0, rounding)
        .toBigIntegerExact();
  }

  public long end(ModifierDefinition candidate, Long first) {
    var range = candidate.stats().getFirst();
    if (exact || relevant.contains(range.id()) && !intervalSafe.contains(range.id())) return first;
    var boundary =
        cuts.getOrDefault(range.id(), Collections.emptyNavigableSet())
            .higher(BigInteger.valueOf(first));
    return boundary == null
        ? range.max()
        : BigInteger.valueOf(range.max()).min(boundary.subtract(BigInteger.ONE)).longValueExact();
  }

  public TierPartition tiers() {
    return tiers;
  }

  public OrbitPartition orbits() {
    return orbits;
  }

  public boolean allows(WorkbenchCurrency action) {
    return actions.contains(action);
  }

  boolean relevant(String stat) {
    return relevant.contains(stat);
  }

  /** Existing tier eligibility is not inspected by the no-omen basic rules. */
  Object transitionRegion(ModifierDefinition definition, ModifierInstance modifier) {
    if (exact || definition.stats().size() != 1 || modifier.values().size() != 1)
      return List.of(modifier.modifierId(), new TreeMap<>(modifier.values()));
    var stat = definition.stats().getFirst().id();
    var value = modifier.values().get(stat);
    if (value == null) throw new IllegalArgumentException("Missing modifier stat");
    if (relevant.contains(stat) && !intervalSafe.contains(stat)) return List.of(stat, value);
    return tierRegion(stat, value);
  }

  Object tierRegion(String stat, long first) {
    return region(stat, first, cuts);
  }

  private Object region(String stat, long first, Map<String, NavigableSet<BigInteger>> boundaries) {
    if (!relevant.contains(stat)) return List.of();
    var floor =
        boundaries
            .getOrDefault(stat, Collections.emptyNavigableSet())
            .floor(BigInteger.valueOf(first));
    return List.of(
        stat, floor == null ? BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE) : floor);
  }
}
