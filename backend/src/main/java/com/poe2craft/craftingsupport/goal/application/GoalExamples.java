package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.craftingsupport.goal.application.GoalExampleBounds.Band;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
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
 * 슬롯과 충돌 규칙에 맞는 목표 관련 옵션 구성을 열거한 뒤, 각 티어에 실제 목표를 만족하는 아이템이 있는지 검사한다. catalog·capacity는 베이스와 슬롯 검증,
 * goal·evaluate는 기존 목표 판정, sources·rolls·bounds는 획득 자격과 수치 탐색에 쓰인다.
 */
final class GoalExamples {
  private final ItemCatalog catalog;
  private final GoalFilter goal;
  private final Map<String, List<ModifierAcquisition>> sources;
  private final Function<ItemState, Evaluation> evaluate;
  private final GoalExampleRolls rolls;
  private final GoalExampleBounds bounds;
  private final GoalExampleCapacity capacity;

  /** 단일 티어의 정의, 현재 희귀도에서 허용된 획득처, 목표 수치 구간, 실제 굴림 순회다. */
  private record Tier(
      ModifierDefinition definition,
      List<ModifierAcquisition> origins,
      Map<String, Band> range,
      Iterable<ModifierInstance> rolls) {}

  /** 하나의 옵션 슬롯에서 선택 가능한 티어들과 그 전체 수치 구간이다. */
  private record Slot(List<Tier> tiers, Map<String, Band> range) {
    /** 티어 간 같은 접두·접미 및 충돌 계열을 참조할 대표 정의다. */
    ModifierDefinition definition() {
      return tiers.getFirst().definition();
    }
  }

  /** 고정 옵션을 반영한 시작 상태와 해당 상태의 수치 기여다. */
  private record Seed(ItemState item, Map<String, Band> range) {}

  /** 같은 구성의 검증된 실제 상태와 대표 옵션에 대응하는 적격 정의 ID들이다. */
  record Matches(List<ItemState> variants, Map<String, List<String>> optionTiers) {}

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
    this.goal = goal;
    this.sources = sources;
    this.evaluate = evaluate;
    rolls = new GoalExampleRolls(goal, stats, simulator);
    bounds = new GoalExampleBounds(goal, stats);
    capacity = new GoalExampleCapacity(catalog, sources);
  }

  /** 목표 레벨·희귀도 안에서 관련 옵션만 추가한다. 빈 결과는 탐색한 범위에서 예시가 없다는 의미다. */
  List<Matches> find(GoalFilter.Context context) {
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
    var available =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.requiredItemLevel() <= context.itemLevel()
                        && !sources.getOrDefault(d.id(), List.of()).isEmpty())
            .sorted(
                Comparator.comparingInt(ModifierDefinition::tier)
                    .thenComparing(ModifierDefinition::id))
            .toList();
    var direct = available.stream().filter(d -> GoalExampleRolls.relevant(d, targets)).toList();
    var providers =
        available.stream()
            .filter(ModifierMagnitudes::provider)
            .filter(
                p ->
                    direct.stream()
                        .anyMatch(d -> bounds.numeric(bounds.project(d, null)) && affects(p, d)))
            .toList();
    for (var provider : providers)
      for (var stat : provider.stats())
        targets.putIfAbsent(GoalCatalogIndex.id("EXPLICIT", stat.id()), Set.of());
    var definitions =
        available.stream().filter(d -> direct.contains(d) || providers.contains(d)).toList();
    var lower = providers.stream().map(p -> extreme(p, false)).toList();
    var upper = providers.stream().map(p -> extreme(p, true)).toList();
    var tierGroups = tierGroups(definitions);
    var cards = new ArrayList<Matches>();
    for (var rarity : goal.general().rarities()) {
      if (rarity == ItemState.Rarity.UNIQUE) continue;
      var seeds = seeds(context, rarity, quality, targets);
      if (seeds.isEmpty()) continue;
      var grouped = new LinkedHashMap<String, List<Tier>>();
      for (var d : definitions) {
        var origins =
            sources.get(d.id()).stream().filter(source -> source.supportsRarity(rarity)).toList();
        if (origins.isEmpty()) continue;
        if (origins.getFirst().kind() == Kind.NORMAL) origins = List.of(origins.getFirst());
        var range = scaled(d, null, lower, upper);
        var domain = domain(d, range, targets);
        if (!domain.iterator().hasNext()) continue;
        grouped
            .computeIfAbsent(tierGroups.getOrDefault(d.id(), d.id()), ignored -> new ArrayList<>())
            .add(new Tier(d, origins, range, domain));
      }
      var slots =
          grouped.values().stream()
              .map(tiers -> new Slot(tiers, hull(tiers.stream().map(Tier::range).toList())))
              .toList();
      cards.addAll(findShapes(seeds, slots));
    }
    return cards;
  }

  /** 실제 수치가 영향을 주는 티어는 모든 굴림, 존재만 중요한 티어는 검증 가능한 첫 굴림을 사용한다. */
  private Iterable<ModifierInstance> domain(
      ModifierDefinition definition,
      Map<String, Band> projection,
      Map<String, Set<BigDecimal>> targets) {
    var all = rolls.all(definition, targets);
    if (ModifierMagnitudes.provider(definition) || bounds.numeric(projection)) return all;
    var iterator = all.iterator();
    return iterator.hasNext() ? List.of(iterator.next()) : List.of();
  }

  /** 선택 가능한 배율 옵션의 하한 또는 상한이다. 미선택은 0%다. */
  private static ModifierInstance extreme(ModifierDefinition d, boolean upper) {
    var values = new HashMap<String, Long>();
    for (var stat : d.stats())
      values.put(stat.id(), upper ? Math.max(0, stat.max()) : Math.min(0, stat.min()));
    return new ModifierInstance(d.id(), values);
  }

  /** 공용 장비 배율 규칙으로 이 제공자가 대상 수치를 바꿀 수 있는지 판정한다. */
  private static boolean affects(ModifierDefinition p, ModifierDefinition d) {
    return ModifierMagnitudes.factor(List.of(extreme(p, true)), d).compareTo(BigDecimal.ONE) != 0
        || ModifierMagnitudes.factor(List.of(extreme(p, false)), d).compareTo(BigDecimal.ONE) != 0;
  }

  /** 배율 제공자들의 양 끝으로 목표 수치의 가능한 구간을 감싼다. */
  private Map<String, Band> scaled(
      ModifierDefinition d,
      Map<String, Long> values,
      List<ModifierInstance> lower,
      List<ModifierInstance> upper) {
    var ranges = bounds.project(d, values);
    if (ranges.isEmpty()) return ranges;
    var low = ModifierMagnitudes.factor(lower, d);
    var high = ModifierMagnitudes.factor(upper, d);
    ranges.replaceAll((id, band) -> band.times(low).hull(band.times(high)));
    return ranges;
  }

  /** 같은 슬롯을 대신하는 티어·고정 옵션 굴림의 최소·최대를 합친다. 이들은 같은 원천 수치를 갖는다. */
  private static Map<String, Band> hull(List<Map<String, Band>> ranges) {
    var total = new HashMap<String, Band>();
    for (var range : ranges) range.forEach((id, band) -> total.merge(id, band, Band::hull));
    return total;
  }

  /** 설정된 품질·희귀도와 베이스 고정 옵션의 실제 굴림으로 시작 상태를 만든다. */
  private List<Seed> seeds(
      GoalFilter.Context context,
      ItemState.Rarity rarity,
      Integer quality,
      Map<String, Set<BigDecimal>> targets) {
    var definition =
        catalog.base().hasImplicit()
            ? catalog.modifiers().get(catalog.base().implicitModifierId())
            : null;
    if (definition != null && !sources.containsKey(definition.id())) return List.of();
    var seeds = new ArrayList<Seed>();
    Iterable<ModifierInstance> domain =
        definition == null
            ? Collections.singletonList(null)
            : domain(definition, bounds.project(definition, null), targets);
    for (var implicit : domain) {
      var item =
          new ItemState(
              context.snapshotId(),
              context.baseItemId(),
              context.itemLevel(),
              rarity,
              implicit == null ? List.of() : List.of(implicit),
              List.of(),
              Set.of(),
              null,
              quality == null || quality == 0
                  ? null
                  : new CatalystQuality(CatalystQuality.Type.FLESH, quality));
      seeds.add(
          new Seed(
              item, implicit == null ? Map.of() : bounds.project(definition, implicit.values())));
    }
    return seeds;
  }

  /** 각 옵션 구성은 오름차순 인덱스 한 경로에서만 나온다. 남은 슬롯에서 더하거나 생략할 수 있는 구간을 미리 합산한다. */
  private List<Matches> findShapes(List<Seed> seeds, List<Slot> slots) {
    var cards = new ArrayList<Matches>();
    var seedRange = hull(seeds.stream().map(Seed::range).toList());
    var limits = capacity.limits(seeds.getFirst().item());
    var optional = new ArrayList<Map<String, Band>>();
    optional.add(Map.of());
    for (int i = slots.size() - 1; i >= 0; i--) {
      var choice = new HashMap<String, Band>();
      slots.get(i).range().forEach((id, band) -> choice.put(id, band.includingZero()));
      optional.addFirst(GoalExampleBounds.sum(choice, optional.getFirst()));
    }
    visit(
        seeds,
        slots,
        optional,
        limits,
        0,
        new ArrayList<>(),
        new HashSet<>(),
        seedRange,
        0,
        0,
        cards);
    return cards;
  }

  /** 선택·미선택을 포함해도 불가능한 하위 트리만 잘라낸다. 목표에 이미 맞는 구성도 남은 슬롯으로 계속 확장한다. */
  private void visit(
      List<Seed> seeds,
      List<Slot> slots,
      List<Map<String, Band>> optional,
      GoalExampleCapacity.Limits limits,
      int start,
      List<Slot> shape,
      Set<String> conflicts,
      Map<String, Band> range,
      int prefixes,
      int suffixes,
      List<Matches> cards) {
    if (!bounds
        .blocked(GoalExampleBounds.sum(range, optional.get(start)), range.keySet())
        .isEmpty()) return;
    if (bounds.blocked(range).isEmpty() && mechanicsPossible(limits, shape)) {
      var witnesses = witnesses(seeds, shape);
      for (var group : bySupport(shape, witnesses))
        cards.add(new Matches(group, optionTiers(shape, group)));
    }
    for (int i = start; i < slots.size(); i++) {
      var slot = slots.get(i);
      boolean prefix = slot.definition().affixType() == ModifierDefinition.AffixType.PREFIX;
      if (prefix ? prefixes >= limits.prefixes() : suffixes >= limits.suffixes()) continue;
      if (!Collections.disjoint(conflicts, slot.definition().familyIds())) continue;
      shape.add(slot);
      conflicts.addAll(slot.definition().familyIds());
      visit(
          seeds,
          slots,
          optional,
          limits,
          i + 1,
          shape,
          conflicts,
          GoalExampleBounds.sum(range, slot.range()),
          prefixes + (prefix ? 1 : 0),
          suffixes + (prefix ? 0 : 1),
          cards);
      conflicts.removeAll(slot.definition().familyIds());
      shape.removeLast();
    }
  }

  /** 한도 확장에 필요한 별도 보조 옵션이 다르면 완성 아이템의 실제 옵션 구성이 다르므로 카드를 나눈다. */
  private static Collection<List<ItemState>> bySupport(
      List<Slot> shape, List<ItemState> witnesses) {
    if (witnesses.isEmpty()) return List.of();
    var ids = new HashSet<String>();
    for (var slot : shape) for (var tier : slot.tiers()) ids.add(tier.definition().id());
    var groups = new LinkedHashMap<List<String>, List<ItemState>>();
    for (var item : witnesses) {
      var extras =
          item.explicits().stream()
              .map(ModifierInstance::modifierId)
              .filter(id -> !ids.contains(id))
              .sorted()
              .toList();
      groups.computeIfAbsent(extras, ignored -> new ArrayList<>()).add(item);
    }
    return groups.values();
  }

  /** 각 대표 옵션을 탐색한 슬롯에 직접 연결한다. 베이스 고정 옵션과 별도로 필요한 보조 옵션도 표시한다. */
  private static Map<String, List<String>> optionTiers(
      List<Slot> shape, List<ItemState> witnesses) {
    var covered = new HashSet<String>();
    for (var item : witnesses) item.explicits().forEach(m -> covered.add(m.modifierId()));
    var byId = new HashMap<String, List<String>>();
    for (var slot : shape) {
      var eligible =
          slot.tiers().stream().map(t -> t.definition().id()).filter(covered::contains).toList();
      for (var id : eligible) byId.put(id, eligible);
    }
    var result = new TreeMap<String, List<String>>();
    var first = witnesses.getFirst();
    for (var layer : List.of(first.implicits(), first.explicits()))
      for (var mod : layer) {
        var id = mod.modifierId();
        result.put(id, byId.getOrDefault(id, List.of(id)));
      }
    return Map.copyOf(result);
  }

  /** 훼손 슬롯과 제작 옵션의 최소 개수를 검토한다. 보너스 상한을 모두 사용해도 불가능한 구성은 굴림을 펼치지 않는다. */
  private boolean mechanicsPossible(GoalExampleCapacity.Limits limits, List<Slot> shape) {
    int desecrated = 0, crafted = 0;
    for (var slot : shape) {
      boolean needsDesecration = true, needsCrafted = true;
      for (var tier : slot.tiers()) {
        var plain = tier.rolls().iterator().next();
        for (var origin : tier.origins()) {
          var kind = origin.kind();
          needsDesecration &= kind == Kind.DESECRATION;
          needsCrafted &=
              (kind != Kind.NORMAL && kind != Kind.AUGMENT && kind != Kind.DESECRATION)
                  || AugmentCraftingRules.crafted(plain, catalog);
        }
      }
      if (needsDesecration) desecrated++;
      if (needsCrafted) crafted++;
    }
    return desecrated <= 1 && crafted <= limits.crafted();
  }

  /** 첫 완성 상태와 아직 증명되지 않은 각 티어에 필요한 실제 상태만 남겨 티어 조합 곱만큼 응답이 커지지 않게 한다. */
  private List<ItemState> witnesses(List<Seed> seeds, List<Slot> shape) {
    var first = witness(seeds, shape);
    if (first == null) return List.of();
    var result = new ArrayList<ItemState>();
    result.add(first);
    var covered = new HashSet<String>();
    first.explicits().forEach(m -> covered.add(m.modifierId()));
    for (var slot : shape)
      for (var tier : slot.tiers()) {
        if (covered.contains(tier.definition().id())) continue;
        var fixed = new ArrayList<>(shape);
        fixed.remove(slot);
        fixed.addFirst(new Slot(List.of(tier), tier.range()));
        var match = witness(seeds, fixed);
        if (match != null) {
          result.add(match);
          match.explicits().forEach(m -> covered.add(m.modifierId()));
        }
      }
    return result;
  }

  /** 각 단계의 남은 수치 범위를 뒤에서부터 더한 뒤, 실제 고정 옵션과 양립하는 티어·굴림 하나를 찾는다. */
  private ItemState witness(List<Seed> seeds, List<Slot> slots) {
    var providers =
        slots.stream().filter(s -> ModifierMagnitudes.provider(s.definition())).toList();
    var ordinary =
        slots.stream().filter(s -> !ModifierMagnitudes.provider(s.definition())).toList();
    for (var seed : seeds) {
      var found = chooseProviders(seed, providers, ordinary, 0);
      if (found != null) return found;
    }
    return null;
  }

  /** 실제 배율 굴림을 먼저 정하고 나머지 슬롯의 정확한 범위를 계산한다. */
  private ItemState chooseProviders(
      Seed seed, List<Slot> providers, List<Slot> ordinary, int index) {
    if (index < providers.size()) {
      for (var tier : providers.get(index).tiers())
        for (var roll : tier.rolls())
          for (var origin : tier.origins()) {
            var item = append(seed.item(), tier.definition(), roll, origin);
            if (item == null) continue;
            var found =
                chooseProviders(new Seed(item, seed.range()), providers, ordinary, index + 1);
            if (found != null) return found;
          }
      return null;
    }
    var effects = seed.item().explicits();
    var values = seed.range();
    for (var mod : effects)
      values =
          GoalExampleBounds.sum(
              values,
              scaled(catalog.modifiers().get(mod.modifierId()), mod.values(), effects, effects));
    var slots = ordinary;
    var remaining = new ArrayList<Map<String, Band>>();
    remaining.add(Map.of());
    for (int i = slots.size() - 1; i >= 0; i--)
      remaining.addFirst(
          GoalExampleBounds.sum(
              hull(
                  slots.get(i).tiers().stream()
                      .map(t -> scaled(t.definition(), null, effects, effects))
                      .toList()),
              remaining.getFirst()));
    if (!bounds.blocked(GoalExampleBounds.sum(values, remaining.getFirst())).isEmpty()) return null;
    return choose(seed.item(), values, slots, remaining, 0, effects);
  }

  /** 티어와 굴림별 남은 목표 구간을 확인한다. 희귀도·계열·슬롯·소켓·실제 목표까지 모두 유효한 첫 완성 상태를 반환한다. */
  private ItemState choose(
      ItemState item,
      Map<String, Band> values,
      List<Slot> slots,
      List<Map<String, Band>> remaining,
      int index,
      List<ModifierInstance> effects) {
    if (index == slots.size()) {
      var complete = capacity.complete(item);
      return complete != null && evaluate.apply(complete).status() == Status.MATCH
          ? complete
          : null;
    }
    var rest = GoalExampleBounds.sum(values, remaining.get(index + 1));
    for (var tier : slots.get(index).tiers()) {
      if (!bounds
          .blocked(GoalExampleBounds.sum(rest, scaled(tier.definition(), null, effects, effects)))
          .isEmpty()) continue;
      for (var roll : tier.rolls()) {
        var contribution = scaled(tier.definition(), roll.values(), effects, effects);
        if (!bounds.blocked(GoalExampleBounds.sum(rest, contribution)).isEmpty()) continue;
        var accumulated = GoalExampleBounds.sum(values, contribution);
        for (var origin : tier.origins()) {
          var updated = append(item, tier.definition(), roll, origin);
          if (updated == null) continue;
          var found = choose(updated, accumulated, slots, remaining, index + 1, effects);
          if (found != null) return found;
        }
      }
    }
    return null;
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

  /** 기폭제 품질로 달라지는 옵션 수치의 목표 판정이 필요한지 확인한다. */
  static boolean needsQualityProjection(GoalFilter goal) {
    return goal.general().quality() != null
        && goal.general().quality() > 0
        && goal.groups().stream().anyMatch(GoalFilter.Group::active);
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
          var socketed = GoalExampleCapacity.install(item, id);
          if (socketed == null) return null;
          installed = socketed.installedAugments();
          sockets = socketed.augmentSockets();
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
      return result;
    } catch (IllegalArgumentException conflictingSockets) {
      // 개별 획득처의 장착 자격이 있어도 여러 증강물의 소켓 그룹 제한이 충돌할 수 있다.
      return null;
    }
  }
}
