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
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
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
  private final ItemStateValidator validator;
  private List<ModifierDefinition> fillers = List.of();
  private ModifierInstance fracture;
  private String selectedTier;

  /** 현재 탐색 사례의 기폭제와 주얼 여부다. 요청의 최소 품질은 goal에 그대로 둔다. */
  private CatalystQuality catalyst;

  /** 현재 사례가 품질 주입을 사용했는지 시드와 완성 상태에 보존한다. */
  private boolean infused;

  /** 기폭제 종류와 무관하게 도달 가능한 품질 수치와 주입 사용 여부다. */
  private record Quality(int amount, boolean infused) {}

  private final boolean jewel;
  private final WorkbenchSimulator simulator;

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
  record Matches(
      List<ItemState> variants,
      Map<String, List<String>> optionTiers,
      List<GoalFilterService.Occupancy> occupancy) {}

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
    this.simulator = simulator;
    catalog = simulator.catalog();
    jewel = BasicJewel.supported(catalog.base().id());
    this.goal = goal;
    this.sources = sources;
    this.evaluate = evaluate;
    rolls = new GoalExampleRolls(goal, stats, simulator);
    bounds = new GoalExampleBounds(goal, stats);
    capacity = new GoalExampleCapacity(catalog, sources);
    validator = new ItemStateValidator(catalog);
  }

  /** 목표 레벨·희귀도 안에서 관련 옵션만 추가한다. 빈 결과는 탐색한 범위에서 예시가 없다는 의미다. */
  List<Matches> find(GoalFilter.Context context, ModifierInstance fracture, String selectedTier) {
    this.selectedTier = selectedTier;
    this.fracture = fracture;
    if (fracture != null
        && (!fracture.fractured()
            || catalog
                .find(fracture.modifierId())
                .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
                .isEmpty()))
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error("INVALID_ITEM", "/fracture", "Invalid fractured modifier.")));
    if (!goal.general().itemLevel().contains(BigDecimal.valueOf(context.itemLevel())))
      return List.of();
    var minimum = goal.general().quality();
    var matches = new ArrayList<Matches>();
    boolean optional = minimum == null || minimum == 0;
    var catalysts =
        simulator.baseMaterialActions().stream()
            .filter(c -> c.catalystType() != null)
            .filter(
                c ->
                    !optional
                        || catalog.modifiers().values().stream()
                            .anyMatch(
                                d ->
                                    !d.tags().contains("unscalable")
                                        && (jewel || c.catalystType().matches(d.tags()))
                                        && bounds.numeric(bounds.project(d, null))))
            .toList();
    for (var rarity : goal.general().rarities()) {
      if (rarity == ItemState.Rarity.UNIQUE) continue;
      if (optional) matches.addAll(findForQuality(context, rarity, null, false));
      if (catalysts.isEmpty()) continue;
      // 품질 자격은 옵션 수치와 독립적이다. 실제 베이스와 분열 옵션으로 가능한 상한을 먼저 검증한다.
      catalyst = null;
      infused = false;
      var seeds = seeds(context, rarity, Map.of());
      if (seeds.isEmpty()) continue;
      var qualities = reachableQualities(seeds.getFirst().item(), catalysts.getFirst());
      if (!optional) qualities.removeIf(q -> q.amount() < minimum);
      if (goal.groups().stream().noneMatch(GoalFilter.Group::active)) {
        if (!qualities.isEmpty()) matches.addAll(findForQuality(context, rarity, null, false));
        continue;
      }
      for (var quality : qualities)
        for (var currency : catalysts)
          matches.addAll(
              findForQuality(
                  context,
                  rarity,
                  new CatalystQuality(currency.catalystType(), quality.amount()),
                  quality.infused()));
    }
    return matches;
  }

  /** 시작 상태와 균열 옵션을 임시로 획득할 수 있는 상태 각각에서 기폭제·주입기 규칙을 적용한다. */
  private Set<Quality> reachableQualities(ItemState seed, WorkbenchCurrency currency) {
    var result = new LinkedHashSet<Quality>();
    if (!validator.validateForGenerationRetainingFracture(seed).isEmpty()) return result;
    addQualities(result, seed, currency);
    var breach = catalog.find(QualityLimitRules.BREACH_ID).orElse(null);
    if (breach == null
        || seed.explicits().stream().anyMatch(m -> m.modifierId().equals(breach.id())))
      return result;
    for (var source : sources.getOrDefault(breach.id(), List.of())) {
      if (!source.supportsRarity(seed.rarity())) continue;
      var withBreach = append(seed, breach, rolls.variants(breach, Map.of()).getFirst(), source);
      if (withBreach == null) continue;
      var valid = capacity.complete(withBreach);
      if (valid != null) {
        addQualities(result, valid, currency);
        break;
      }
    }
    return result;
  }

  /** 작업대가 품질을 보존하므로 높였을 때의 상한을 사용한다. 실제 주입이 가능한 경우만 한 번 더 계산한다. */
  private void addQualities(Set<Quality> result, ItemState item, WorkbenchCurrency currency) {
    // 두 작업 모두 수치가 확정적이며 작업대 API의 난수 인자를 소비하지 않는다.
    var random = java.util.concurrent.ThreadLocalRandom.current();
    var applied = simulator.apply(item, currency, Set.of(), random);
    if (!applied.applied()) return;
    var typed = applied.state();
    result.add(new Quality(typed.catalystQuality().amount(), typed.qualityInfused()));
    var infusion =
        simulator.apply(typed, WorkbenchCurrency.VAAL_CATALYSING_INFUSER, Set.of(), random);
    if (infusion.applied()) {
      var after = infusion.state();
      result.add(new Quality(after.catalystQuality().amount(), after.qualityInfused()));
    }
  }

  /** 한 희귀도와 한 기폭제의 실제 품질을 고정해 각 티어의 증명 사례를 독립적으로 찾는다. */
  private List<Matches> findForQuality(
      GoalFilter.Context context,
      ItemState.Rarity rarity,
      CatalystQuality catalyst,
      boolean infused) {
    this.catalyst = catalyst;
    this.infused = infused;
    var quality = catalyst == null ? goal.general().quality() : Integer.valueOf(catalyst.amount());
    var targets = rolls.targets(null);
    if (quality != null && quality > QualityLimitRules.BASE_CAP)
      targets.putIfAbsent(GoalCatalogIndex.id("EXPLICIT", QualityLimitRules.STAT_ID), Set.of());
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
            .filter(this::provider)
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
    fillers = available.stream().filter(d -> !definitions.contains(d)).toList();
    var lower = providers.stream().map(p -> extreme(p, false)).toList();
    var upper = providers.stream().map(p -> extreme(p, true)).toList();
    var tierGroups = tierGroups(definitions);
    var seeds = seeds(context, rarity, targets);
    if (seeds.isEmpty()) return List.of();
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
    if (selectedTier != null) {
      var group = grouped.get(tierGroups.getOrDefault(selectedTier, selectedTier));
      if (group != null) group.removeIf(t -> !t.definition().id().equals(selectedTier));
      if (group != null && group.isEmpty()) return List.of();
    }
    var slots =
        grouped.values().stream()
            .map(tiers -> new Slot(tiers, hull(tiers.stream().map(Tier::range).toList())))
            .toList();
    return findShapes(seeds, slots);
  }

  /** 실제 수치가 영향을 주는 티어는 모든 굴림, 존재만 중요한 티어는 검증 가능한 첫 굴림을 사용한다. */
  private Iterable<ModifierInstance> domain(
      ModifierDefinition definition,
      Map<String, Band> projection,
      Map<String, Set<BigDecimal>> targets) {
    var all = rolls.all(definition, targets);
    if (provider(definition) || bounds.numeric(projection)) return all;
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
  private boolean affects(ModifierDefinition p, ModifierDefinition d) {
    if (catalyst != null
        && jewel
        && !d.tags().contains("unscalable")
        && BasicJewel.effect(List.of(extreme(p, true)), d.affixType()) != 0) return true;
    return ModifierMagnitudes.factor(List.of(extreme(p, true)), d).compareTo(BigDecimal.ONE) != 0
        || ModifierMagnitudes.factor(List.of(extreme(p, false)), d).compareTo(BigDecimal.ONE) != 0;
  }

  /** 품질 주얼의 접사 효과도 실제 굴림을 먼저 정해야 하는 제공자에 포함한다. */
  private boolean provider(ModifierDefinition d) {
    if (ModifierMagnitudes.provider(d)) return true;
    if (catalyst == null || !jewel) return false;
    var modifiers = List.of(extreme(d, true));
    return BasicJewel.effect(modifiers, ModifierDefinition.AffixType.PREFIX) != 0
        || BasicJewel.effect(modifiers, ModifierDefinition.AffixType.SUFFIX) != 0;
  }

  /** 배율 제공자들의 양 끝으로 목표 수치의 가능한 구간을 감싼다. */
  private Map<String, Band> scaled(
      ModifierDefinition d,
      Map<String, Long> values,
      List<ModifierInstance> lower,
      List<ModifierInstance> upper) {
    var ranges = bounds.project(d, values);
    if (ranges.isEmpty()) return ranges;
    if (catalyst != null) {
      long minEffect = jewel ? BasicJewel.effect(lower, d.affixType()) : 0;
      long maxEffect = jewel ? BasicJewel.effect(upper, d.affixType()) : 0;
      var min = qualityEndpoint(d, values, minEffect, maxEffect, false);
      var max =
          values != null && minEffect == maxEffect
              ? min
              : qualityEndpoint(d, values, minEffect, maxEffect, true);
      ranges = bounds.project(d, min, max);
    }
    var low = ModifierMagnitudes.factor(lower, d);
    var high = ModifierMagnitudes.factor(upper, d);
    ranges.replaceAll((id, band) -> band.times(low).hull(band.times(high)));
    return ranges;
  }

  /** 원천 범위의 한쪽 끝에 품질을 적용한다. 음수 수치도 포함하므로 가능한 주얼 효과의 양 끝을 비교한다. */
  private Map<String, Long> qualityEndpoint(
      ModifierDefinition d,
      Map<String, Long> values,
      long minEffect,
      long maxEffect,
      boolean upper) {
    if (values == null) {
      values = new HashMap<>();
      for (var stat : d.stats()) values.put(stat.id(), upper ? stat.max() : stat.min());
    }
    var roll = new ModifierInstance(d.id(), values);
    var first =
        CatalystQualityDisplay.project(roll, d, catalyst, jewel, minEffect).displayedValues();
    if (minEffect == maxEffect) return first;
    var second =
        CatalystQualityDisplay.project(roll, d, catalyst, jewel, maxEffect).displayedValues();
    var endpoint = new HashMap<>(first);
    endpoint.replaceAll(
        (id, value) -> upper ? Math.max(value, second.get(id)) : Math.min(value, second.get(id)));
    return endpoint;
  }

  /** 같은 슬롯을 대신하는 티어·고정 옵션 굴림의 최소·최대를 합친다. 이들은 같은 원천 수치를 갖는다. */
  private static Map<String, Band> hull(List<Map<String, Band>> ranges) {
    var total = new HashMap<String, Band>();
    for (var range : ranges) range.forEach((id, band) -> total.merge(id, band, Band::hull));
    return total;
  }

  /** 설정된 품질·희귀도와 베이스 고정 옵션의 실제 굴림으로 시작 상태를 만든다. */
  private List<Seed> seeds(
      GoalFilter.Context context, ItemState.Rarity rarity, Map<String, Set<BigDecimal>> targets) {
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
                  fracture == null ? List.of() : List.of(fracture),
                  Set.of(),
                  null,
                  catalyst)
              .withQualityInfused(infused);
      if (fracture != null && !validator.validateForGenerationRetainingFracture(item).isEmpty())
        continue;
      seeds.add(
          new Seed(
              item,
              implicit == null
                  ? Map.of()
                  : scaled(definition, implicit.values(), List.of(), List.of())));
    }
    return seeds;
  }

  /** 각 옵션 구성은 오름차순 인덱스 한 경로에서만 나온다. 남은 슬롯에서 더하거나 생략할 수 있는 구간을 미리 합산한다. */
  private List<Matches> findShapes(List<Seed> seeds, List<Slot> slots) {
    var cards = new ArrayList<Matches>();
    var seedRange = hull(seeds.stream().map(Seed::range).toList());
    var initial = seeds.getFirst().item();
    var limits = capacity.limits(initial);
    var occupied = validator.slots(initial);
    Set<String> conflicts = new HashSet<>();
    if (fracture != null) {
      var d = catalog.modifiers().get(fracture.modifierId());
      conflicts.addAll(d.familyIds());
      // 분열 수치도 이후 선택 가능한 배율 제공자의 영향을 받는다.
      var extrema =
          slots.stream()
              .flatMap(s -> s.tiers().stream())
              .map(Tier::definition)
              .filter(this::provider)
              .toList();
      seedRange =
          GoalExampleBounds.sum(
              seedRange,
              scaled(
                  d,
                  fracture.values(),
                  extrema.stream().map(p -> extreme(p, false)).toList(),
                  extrema.stream().map(p -> extreme(p, true)).toList()));
    }
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
        conflicts,
        seedRange,
        occupied.usedPrefixes(),
        occupied.usedSuffixes(),
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
        if (selectedTier == null
            || group.getFirst().explicits().stream()
                .anyMatch(m -> m.modifierId().equals(selectedTier)))
          cards.add(new Matches(group, optionTiers(shape, group), occupancies(group.getFirst())));
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

  /** 대표 아이템의 빈칸을 목표에 무관한 옵션으로 채울 수 있는 접사 수 조합을 검증한다. */
  private List<GoalFilterService.Occupancy> occupancies(ItemState item) {
    var slots = validator.slots(item);
    var results = new ArrayList<GoalFilterService.Occupancy>();
    for (int p = 0; p <= slots.remainingPrefixes(); p++)
      for (int s = 0; s <= slots.remainingSuffixes(); s++)
        if (canFill(item, 0, p, s))
          results.add(
              new GoalFilterService.Occupancy(
                  p, s, slots.remainingPrefixes() - p, slots.remainingSuffixes() - s));
    return List.copyOf(results);
  }

  /** 목표 밖 옵션의 계열·출처와 슬롯 충돌을 확인해 개수 조합당 실제 예 하나를 찾는다. */
  private boolean canFill(ItemState item, int start, int prefixes, int suffixes) {
    if (prefixes + suffixes == 0) return true;
    var used = new HashSet<String>();
    item.explicits().forEach(m -> used.addAll(catalog.modifiers().get(m.modifierId()).familyIds()));
    for (int i = start; i < fillers.size(); i++) {
      var d = fillers.get(i);
      boolean prefix = d.affixType() == ModifierDefinition.AffixType.PREFIX;
      if ((prefix ? prefixes : suffixes) == 0 || !Collections.disjoint(used, d.familyIds()))
        continue;
      var domain = rolls.variants(d, Map.of());
      if (domain.isEmpty()) continue;
      for (var origin : sources.get(d.id())) {
        if (!origin.supportsRarity(item.rarity())) continue;
        var next = append(item, d, domain.getFirst(), origin);
        if (next != null
            && validator.validateForGenerationRetainingFracture(next).isEmpty()
            && next.explicits().stream()
                    .filter(m -> AugmentCraftingRules.crafted(m, catalog))
                    .count()
                <= AugmentCraftingRules.craftedLimit(next)
            && canFill(next, i + 1, prefixes - (prefix ? 1 : 0), suffixes - (prefix ? 0 : 1)))
          return true;
      }
    }
    return false;
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
    var providers = slots.stream().filter(s -> provider(s.definition())).toList();
    var ordinary = slots.stream().filter(s -> !provider(s.definition())).toList();
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
      if (complete == null) return null;
      return evaluate.apply(complete).status() == Status.MATCH ? complete : null;
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
      return new ItemState(
          item.snapshotId(),
          item.baseItemId(),
          item.itemLevel(),
          item.rarity(),
          item.implicits(),
          explicits,
          item.conditions(),
          sockets,
          item.catalystQuality(),
          desecrated,
          item.equipmentQuality(),
          item.observations(),
          item.sanctification(),
          installed,
          item.qualityInfused());
    } catch (IllegalArgumentException conflictingSockets) {
      // 개별 획득처의 장착 자격이 있어도 여러 증강물의 소켓 그룹 제한이 충돌할 수 있다.
      return null;
    }
  }
}
