package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Evaluation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition.Kind;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * 요청 한 건에서 목표에 가까운 조합을 확장하여 검증 가능한 서로 다른 예시를 찾는다. catalog·validator는 베이스 제약, goal·evaluate는 기존 목표
 * 판정, sources·rolls는 획득 가능 여부와 구체 수치 선택에 사용한다. FRONTIER_LIMIT와 TRIALS_PER_LAYER는 여러 슬롯을 탐색할 때 다음
 * 깊이로 확장할 부분 상태 수와 판정 횟수의 상한이다. 완성 카드 수는 제한하지 않는다.
 */
final class GoalExamples {
  private static final int FRONTIER_LIMIT = 12;
  private static final int TRIALS_PER_LAYER = 4096;
  private final ItemCatalog catalog;
  private final ItemStateValidator validator;
  private final GoalFilter goal;
  private final Map<String, List<ModifierAcquisition>> sources;
  private final Function<ItemState, Evaluation> evaluate;
  private final GoalExampleRolls rolls;

  /** 탐색 중 보관하는 아이템, 실제 목표 판정, 다음 깊이의 탐색 우선순위다. */
  private record Candidate(ItemState item, Evaluation evaluation, double priority) {}

  /** 획득 분류, 계층, 접두·접미, 충돌 계열, 태그, 순서 있는 수치가 모두 같은 옵션의 티어만 묶는다. */
  private record TierFamily(
      Kind kind,
      ModifierDefinition.Layer layer,
      ModifierDefinition.AffixType affix,
      Set<String> families,
      Set<String> tags,
      List<String> stats) {}

  /** 번들 데이터는 참조로 공유하고 목표와 한정된 탐색 상태는 요청별로 관리한다. */
  GoalExamples(
      WorkbenchSimulator simulator,
      GoalFilter goal,
      Map<String, GoalCatalog.Stat> stats,
      Map<String, List<ModifierAcquisition>> sources,
      Function<ItemState, Evaluation> evaluate) {
    catalog = simulator.catalog();
    validator = new ItemStateValidator(catalog);
    this.goal = goal;
    this.sources = sources;
    this.evaluate = evaluate;
    rolls = new GoalExampleRolls(goal, stats, simulator);
  }

  /** 목표 레벨·희귀도 안에서 관련 옵션만 추가한다. 빈 결과는 탐색한 범위에서 예시가 없다는 의미다. */
  List<List<ItemState>> find(GoalFilter.Context context) {
    if (!goal.general().itemLevel().contains(BigDecimal.valueOf(context.itemLevel())))
      return List.of();
    var quality = goal.general().quality();
    if (quality != null
        && quality > 0
        && (!CatalystQuality.supportedBase(context.baseItemId())
            || needsQualityProjection(goal)
            || (quality > 20 && !sources.containsKey(QualityLimitRules.BREACH_ID))))
      return List.of();
    var targets = rolls.targets(null);
    var definitions =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.requiredItemLevel() <= context.itemLevel()
                        && !sources.getOrDefault(d.id(), List.of()).isEmpty()
                        && GoalExampleRolls.relevant(d, targets))
            .sorted(
                Comparator.comparingInt(ModifierDefinition::tier)
                    .thenComparing(ModifierDefinition::id))
            .toList();
    var tierGroups = tierGroups(definitions);
    // 같은 티어 조합에 여러 수치 예시가 있어도 실제로 통과한 상태 하나만 보관한다.
    var cards = new LinkedHashMap<List<?>, ItemState>();
    for (var rarity : goal.general().rarities()) {
      if (rarity == ItemState.Rarity.UNIQUE) continue;
      var frontier = new ArrayList<Candidate>();
      for (var implicit : implicitSets(targets)) {
        var seed =
            new ItemState(
                context.snapshotId(),
                context.baseItemId(),
                context.itemLevel(),
                rarity,
                implicit,
                List.of(),
                Set.of(),
                null,
                quality == null || quality == 0
                    ? null
                    : new CatalystQuality(CatalystQuality.Type.FLESH, quality));
        if (!validator.validateForGeneration(seed).isEmpty()) continue;
        var first = candidate(seed);
        if (!save(first, cards)) frontier.add(first);
      }
      while (!frontier.isEmpty() && !definitions.isEmpty()) {
        var next = new LinkedHashMap<List<?>, Candidate>();
        int perParent = TRIALS_PER_LAYER / frontier.size();
        for (var parent : frontier) {
          // 첫 슬롯은 모든 티어를 판정한다. 더 깊은 조합은 부분 상태별 예산을 나눠 사용한다.
          expand(
              parent,
              definitions,
              parent.item().explicits().isEmpty() ? Integer.MAX_VALUE : perParent,
              cards,
              next);
        }
        frontier = nextFrontier(next.values(), tierGroups);
      }
    }
    var grouped = new LinkedHashMap<List<?>, List<ItemState>>();
    for (var item : cards.values())
      grouped.computeIfAbsent(shape(item, tierGroups), key -> new ArrayList<>()).add(item);
    return grouped.values().stream().map(List::copyOf).toList();
  }

  /** 같은 구조에서 처음 만난 정의 ID를 티어 공통 키로 사용한다. 같은 티어 번호의 정의라도 실제 상태에는 개별 ID가 보존된다. */
  private Map<String, String> tierGroups(List<ModifierDefinition> definitions) {
    var families = new HashMap<TierFamily, String>();
    var groups = new HashMap<String, String>();
    for (var d : definitions) {
      // 주얼·훼손 등의 표시 전용 수치는 다른 효과에도 같은 ID를 사용하므로 옵션의 정체성이 될 수 없다.
      if (d.tier() < 1 || d.stats().stream().anyMatch(stat -> stat.id().startsWith("display_")))
        continue;
      // 일반 옵션에 에센스 등의 추가 출처가 있어도 이 정의의 획득 분류는 일반이다.
      var key =
          new TierFamily(
              sources.get(d.id()).getFirst().kind(),
              d.layer(),
              d.affixType(),
              d.familyIds(),
              d.tags(),
              d.stats().stream().map(ModifierDefinition.StatRange::id).toList());
      groups.put(d.id(), families.computeIfAbsent(key, ignored -> d.id()));
    }
    return groups;
  }

  /** 서로 다른 옵션 구성을 우선 확장하고 남는 탐색 자리는 높은 점수의 다른 티어가 사용한다. */
  private static ArrayList<Candidate> nextFrontier(
      Collection<Candidate> candidates, Map<String, String> tierGroups) {
    var ranked =
        candidates.stream()
            .sorted(Comparator.comparingDouble(Candidate::priority).reversed())
            .toList();
    var seen = new HashSet<List<?>>();
    var selected = new LinkedHashSet<Candidate>();
    for (var candidate : ranked) {
      if (seen.add(shape(candidate.item(), tierGroups))) selected.add(candidate);
      if (selected.size() == FRONTIER_LIMIT) break;
    }
    for (var candidate : ranked) {
      if (selected.size() == FRONTIER_LIMIT) break;
      selected.add(candidate);
    }
    return new ArrayList<>(selected);
  }

  /** 기폭제 품질로 달라지는 옵션 수치의 목표 판정이 필요한지 확인한다. */
  static boolean needsQualityProjection(GoalFilter goal) {
    return goal.general().quality() != null
        && goal.general().quality() > 0
        && goal.groups().stream().anyMatch(GoalFilter.Group::active);
  }

  /** 고정 옵션은 해당 베이스의 카탈로그에서 구성하고, 관련 목표가 있으면 허용된 수치 변형도 검사한다. */
  private List<List<ModifierInstance>> implicitSets(Map<String, Set<BigDecimal>> targets) {
    if (!catalog.base().hasImplicit()) return List.of(List.of());
    var definition = catalog.modifiers().get(catalog.base().implicitModifierId());
    if (!sources.containsKey(definition.id())) return List.of();
    var variants = rolls.variants(definition, targets);
    return variants.stream()
        .limit(GoalExampleRolls.relevant(definition, targets) ? FRONTIER_LIMIT : 1)
        .map(List::of)
        .toList();
  }

  /** 현재 상태의 충족도와 부족한 수치에 맞춰 각 적격 옵션의 대표 굴림을 최대 budget개 판정한다. */
  private void expand(
      Candidate parent,
      List<ModifierDefinition> definitions,
      int budget,
      Map<List<?>, ItemState> cards,
      Map<List<?>, Candidate> next) {
    var item = parent.item();
    var targets = rolls.targets(parent.evaluation());
    var families = new HashSet<String>();
    item.explicits()
        .forEach(mod -> families.addAll(catalog.modifiers().get(mod.modifierId()).familyIds()));
    var slots = validator.slots(item);
    for (var definition : definitions) {
      if (!Collections.disjoint(families, definition.familyIds())) continue;
      if (definition.affixType() == ModifierDefinition.AffixType.PREFIX
          ? slots.remainingPrefixes() <= 0
          : slots.remainingSuffixes() <= 0) continue;
      var origin =
          sources.get(definition.id()).stream()
              .filter(s -> s.supportsRarity(item.rarity()))
              .findFirst();
      if (origin.isEmpty()) continue;
      for (var roll : rolls.variants(definition, targets)) {
        if (--budget < 0) return;
        var state = append(item, definition, roll, origin.get());
        if (state == null || !validator.validateForGeneration(state).isEmpty()) continue;
        var result = candidate(state);
        if (!save(result, cards)) {
          var key = shape(state, Map.of());
          var prior = next.get(key);
          if (prior == null || prior.priority() < result.priority()) next.put(key, result);
        }
      }
    }
  }

  /** 같은 옵션의 일반 획득을 우선한다. 전용 옵션에는 제작 표시·훼손 슬롯·필요한 소켓 증강물을 실제 상태로 남긴다. */
  private ItemState append(
      ItemState item,
      ModifierDefinition definition,
      ModifierInstance roll,
      ModifierAcquisition origin) {
    var explicits = new ArrayList<>(item.explicits());
    var desecrated = new ArrayList<>(item.desecrated());
    var installed = item.installedAugments();
    Integer sockets = item.augmentSockets();
    if (origin.kind() != Kind.NORMAL) {
      if (origin.kind() == Kind.AUGMENT) {
        var id = origin.id();
        if (installed.stream().noneMatch(a -> a.augmentId().equals(id))) {
          var maximum = BaseRegistry.socketExecutionMaximum(item.baseItemId());
          if (maximum == null || installed.size() >= maximum) return null;
          var next = new ArrayList<>(installed);
          next.add(new ItemState.InstalledAugment(installed.size(), id));
          installed = next;
          sockets = next.size();
        }
      } else if (origin.kind() == Kind.DESECRATION) {
        // 일반 훼손은 기존 훼손 슬롯이 없는 장비에만 적용할 수 있다.
        if (!desecrated.isEmpty()) return null;
        desecrated.add(
            new ItemState.DesecratedSlot(
                "preview-" + definition.id(),
                definition.affixType(),
                definition.id(),
                DesecrationCurrency.minimumModifierLevel(origin.id()),
                DesecrationCurrency.revealPool(origin.id())));
      } else roll = roll.asCrafted();
    }
    explicits.add(roll);
    try {
      var result =
          new ItemState(
                  item.snapshotId(),
                  item.baseItemId(),
                  item.itemLevel(),
                  item.rarity(),
                  item.implicits(),
                  explicits,
                  Set.of(),
                  sockets,
                  item.catalystQuality(),
                  desecrated)
              .withAugments(installed);
      if (explicits.stream().filter(m -> AugmentCraftingRules.crafted(m, catalog)).count()
          > AugmentCraftingRules.craftedLimit(result)) return null;
      return result;
    } catch (IllegalArgumentException conflictingSockets) {
      // 개별 획득처의 장착 자격이 있어도 여러 증강물의 소켓 그룹 제한이 충돌할 수 있다.
      return null;
    }
  }

  /** 이미 만족한 그룹·행, 부분 합계의 경계까지 남은 거리를 순서 선정에 사용한다. 성공 여부는 평가 결과로만 정한다. */
  private Candidate candidate(ItemState item) {
    var evaluation = evaluate.apply(item);
    double score = 0;
    int g = 0;
    for (var group : goal.groups()) {
      if (!group.active()) continue;
      var result = evaluation.groups().get(g++);
      if (result.status() == Status.MATCH) score += 100;
      if (result.score() != null) score += proximity(result.score(), group.range());
      int i = 0;
      for (var entry : group.entries()) {
        if (entry.disabled()) continue;
        var row = result.entries().get(i++);
        double benefit = row.status() == Status.MATCH ? 2 : proximity(row.value(), entry.range());
        score += group.type() == GoalFilter.Type.NOT ? -benefit : benefit;
      }
    }
    return new Candidate(item, evaluation, score);
  }

  /** 관측값과 수치 경계가 가까울수록 큰 탐색 점수를 준다. 관측값이 없으면 부분 달성으로 계산하지 않는다. */
  private static double proximity(BigDecimal value, GoalFilter.Range range) {
    if (value == null || range == null) return 0;
    var distance = BigDecimal.ZERO;
    if (range.min() != null) distance = distance.max(range.min().subtract(value));
    if (range.max() != null) distance = distance.max(value.subtract(range.max()));
    return 1 / (1 + distance.doubleValue());
  }

  /** 단일 베이스의 희귀도와 옵션 ID 집합을 비교한다. 티어 그룹을 전달하면 구체 티어와 부여 순서가 달라도 같은 구성이다. */
  private static List<?> shape(ItemState state, Map<String, String> tierGroups) {
    return List.of(
        state.rarity(),
        state.explicits().stream()
            .map(ModifierInstance::modifierId)
            .map(id -> tierGroups.getOrDefault(id, id))
            .sorted()
            .toList());
  }

  /** 목표를 충족한 구체 티어 조합을 보관하고, 이 상태에서 추가 옵션 탐색이 필요한지 알려준다. */
  private static boolean save(Candidate candidate, Map<List<?>, ItemState> cards) {
    if (candidate.evaluation().status() != Status.MATCH) return false;
    cards.putIfAbsent(shape(candidate.item(), Map.of()), candidate.item());
    return true;
  }
}
