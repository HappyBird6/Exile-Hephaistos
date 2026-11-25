package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions.MaterialMode;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.ModifierMagnitudes;
import com.poe2craft.shared.item.SocketAugments;
import java.math.BigDecimal;
import java.util.*;

/** 시작 베이스에서 목표 행에 기여하는 명시 옵션을 계열·티어별로 모아 획득 순서를 검증할 카드를 만든다. */
public final class RouteCards {
  /** 추첨 풀에서만 사용하는 등급. 추첨이 없는 출처의 frequency는 null이다. */
  public enum Frequency {
    L,
    M,
    H
  }

  /** label은 B/E/C/D/A, id는 작업대 재료 식별자다. frequency는 해당 출처에서 목표 허용 티어 중 하나를 얻는 빈도다. */
  public record Source(String label, String id, Frequency frequency) {}

  /** 카드 내 번호·허용 티어·획득처와 목표에 입력한 수치 경계다. 상한만 지정하면 획득 시 티어 최댓값을 사용한다. */
  public record Option(
      int number, Map<String, List<Source>> tiers, boolean hasLowerBound, boolean hasUpperBound) {
    /** 수치 범위 없는 목표는 획득한 티어의 최저값에서 시작한다. */
    public Option(int number, Map<String, List<Source>> tiers) {
      this(number, tiers, false, false);
    }
  }

  /** options는 동시에 유지할 옵션이고 requiredAstrids는 추가로 장착해야 할 제작 한도 증강물 수다. */
  public record Card(List<Option> options, int requiredAstrids) {}

  /** complete가 거짓이면 시간·개수 제한으로 확인하지 못한 카드가 남는다. */
  public record Result(List<Card> cards, boolean complete) {}

  /** 선택 시 같은 접두·접미 칸, 같은 계열 충돌, 같은 능력치 조합을 갖는 티어들을 묶는다. */
  private record Family(
      ModifierDefinition.AffixType side, Set<String> families, List<String> stats) {}

  /** 같은 배타 계열과 스탯을 가진 티어 목록. key는 계열 충돌과 복합 스탯 기여를 판정한다. */
  private record TierGroup(Family key, List<ModifierDefinition> tiers) {}

  /** 시작 상태와 보유 옵션, 베이스의 정적 카탈로그는 해당 요청 안에서만 사용한다. */
  private final ItemState start;

  private final ItemCatalog itemCatalog;

  /** 품질·신성화가 별도 표시값 투영을 요구하는 상태인지와 이 베이스의 강도 제공 옵션을 보관한다. */
  private final boolean checkTierRanges;

  private final List<ModifierDefinition> magnitudeProviders;
  private final Map<String, Set<String>> augmentPools;

  /** 이 카드에서 공존할 수 있는 강도 보정의 하한·상한을 통상의 작업대 계산에 전달한다. */
  private record Magnitudes(List<ModifierInstance> lower, List<ModifierInstance> upper) {}

  /** 목표의 활성 그룹·수치 사전과 그 그룹에 기여하는 원본 수치 ID를 보관한다. */
  private final Map<String, GoalCatalog.Stat> stats = new HashMap<>();

  private final List<GoalFilter.Group> groups;
  private final Map<String, ModifierInstance> held = new HashMap<>();

  /** 후보 계열과 티어별 획득처는 한 번 생성해 모든 카드에서 공유한다. */
  private final List<TierGroup> families;

  private final Map<String, List<Source>> sources;
  private final Set<String> relevant = new HashSet<>();

  /** 실제 옵션에 기여하는 원본 stat 중 하한 또는 상한이 명시된 집합이다. */
  private final Set<String> lowerBoundStats = new HashSet<>();

  private final Set<String> upperBoundStats = new HashSet<>();

  /** 카드의 목표 범위를 적용하기 전 B/A 추첨 출처와 접두·접미별 전체 적격 가중치다. */
  private final Map<Source, Map<AffixType, Long>> poolWeights = new HashMap<>();

  /** B/A 추첨에서 L은 lowProbability 미만, H는 highProbability 이상이다. */
  private final double lowProbability;

  private final double highProbability;

  /** 시작 제작 옵션 한도와 실제로 늘릴 수 있는 한도다. */
  private final int maxPrefixes;

  private final int maxSuffixes;
  private final boolean jewelOverflow;
  private final int craftedLimit;
  private final int additionalAstrids;

  /** 검증된 카드와 요청별 탐색 진행 상태다. */
  private final List<Card> cards = new ArrayList<>();

  // 응답 생성의 작업 예산이다. 도달한 경우 가능성이 0이라고 단정하지 않고 complete=false로 응답한다.
  private static final int MAX_CARD_CHECKS = 20000;
  private static final int MAX_CARDS = 256;
  private int checked;
  private boolean complete = true;

  /** 입력 카탈로그의 동일 계열·레벨에서 후보 티어와 출처를 찾고, 분열과 제작 한도를 먼저 대조한다. */
  public static Result build(
      WorkbenchSimulator simulator,
      ItemState start,
      GoalFilter goal,
      GoalCatalog catalog,
      double lowProbability,
      double highProbability) {
    if (!(lowProbability >= 0 && lowProbability < highProbability && highProbability <= 1))
      throw new IllegalArgumentException("Invalid route frequency thresholds");
    if (!goal.general().baseItemId().equals(start.baseItemId())
        || !goal.general().itemLevel().contains(BigDecimal.valueOf(start.itemLevel()))
        || goal.general().rarities().stream()
            .noneMatch(
                r -> r != ItemState.Rarity.UNIQUE && r.ordinal() >= start.rarity().ordinal()))
      return new Result(List.of(), true);
    var builder = new RouteCards(simulator, start, goal, catalog, lowProbability, highProbability);
    builder.choose(0, new ArrayList<>(), new HashSet<>(), 0);
    return new Result(List.copyOf(builder.cards), builder.complete);
  }

  /** 활성 수치의 명시 계열을 수집하고 베이스 출처, 제작 한도, 범위 보정 조건을 초기화한다. */
  private RouteCards(
      WorkbenchSimulator simulator,
      ItemState start,
      GoalFilter goal,
      GoalCatalog catalog,
      double lowProbability,
      double highProbability) {
    this.start = start;
    this.lowProbability = lowProbability;
    this.highProbability = highProbability;
    groups = goal.groups().stream().filter(g -> !g.disabled()).toList();
    catalog.stats().forEach(s -> stats.put(s.statId(), s));
    for (var group : groups)
      for (var entry : group.entries())
        if (!entry.disabled())
          addStats(entry.statId(), entry.range().min() != null, entry.range().max() != null);
    for (var mod : start.explicits()) held.put(mod.modifierId(), mod);
    itemCatalog = simulator.catalog();
    checkTierRanges =
        start.catalystQuality() == null
            && start.sanctification() == null
            && !start.observedSanctified()
            && (goal.general().quality() == null || goal.general().quality() == 0)
            && groups.stream()
                .flatMap(g -> g.entries().stream())
                .anyMatch(
                    e -> !e.disabled() && (e.range().min() != null || e.range().max() != null));
    augmentPools = AugmentCraftingRules.poolSources(start.baseItemId());
    magnitudeProviders =
        checkTierRanges
            ? itemCatalog.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
                .filter(d -> held.containsKey(d.id()) || d.requiredItemLevel() <= start.itemLevel())
                .filter(ModifierMagnitudes::provider)
                .toList()
            : List.of();
    sources = sourceMap(simulator);
    var byFamily = new LinkedHashMap<Family, List<ModifierDefinition>>();
    itemCatalog.modifiers().values().stream()
        .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
        .filter(d -> sources.containsKey(d.id()) || held.containsKey(d.id()))
        .filter(d -> d.stats().stream().anyMatch(s -> relevant.contains(s.id())))
        .sorted(
            Comparator.comparing(ModifierDefinition::affixType)
                .thenComparing(ModifierDefinition::id))
        .forEach(
            d -> {
              var key =
                  new Family(
                      d.affixType(),
                      d.familyIds(),
                      d.stats().stream().map(ModifierDefinition.StatRange::id).sorted().toList());
              byFamily.computeIfAbsent(key, ignored -> new ArrayList<>()).add(d);
            });
    families =
        byFamily.entrySet().stream()
            .map(e -> new TierGroup(e.getKey(), List.copyOf(e.getValue())))
            .toList();
    craftedLimit = AugmentCraftingRules.craftedLimit(start);
    additionalAstrids = additionalInstallations(start, simulator, "Astrids_Creativity");
    var rare = goal.general().rarities().contains(ItemState.Rarity.RARE);
    var magic = goal.general().rarities().contains(ItemState.Rarity.MAGIC);
    var base = itemCatalog.base();
    maxPrefixes = rare ? base.rarePrefixes() : magic ? base.magicPrefixes() : 0;
    int suffixLimit = rare ? base.rareSuffixes() : magic ? base.magicSuffixes() : 0;
    maxSuffixes = rare || magic ? suffixLimit + AugmentCraftingRules.suffixBonus(start) : 0;
    jewelOverflow = rare && BasicJewel.supportedCrafting(start.baseItemId());
  }

  /** 작업대에서 홈을 열고 룬을 설치해 남은 홈·최대 홈·장착 제한을 모두 적용한다. */
  private static int additionalInstallations(
      ItemState start, WorkbenchSimulator simulator, String augment) {
    var maximum = BaseRegistry.socketExecutionMaximum(start.baseItemId());
    if (maximum == null
        || start.augmentSockets() == null
        || !SocketAugments.supports(start.baseItemId(), augment)) return 0;
    var model = start;
    int count = 0;
    for (int slot = 0; slot < maximum; slot++) {
      int index = slot;
      if (start.installedAugments().stream()
          .anyMatch(
              a ->
                  a.slotIndex() == index
                      && (a.augmentId().equals(augment)
                          || AugmentCraftingRules.socketBound(a.augmentId())))) continue;
      try {
        if (slot >= model.augmentSockets()) {
          var opened = simulator.apply(model, WorkbenchCurrency.ARTIFICER, Set.of(), new Random(0));
          if (!opened.applied()) break;
          model = opened.state();
        }
        model = SocketAugments.install(model, slot, augment);
        count++;
      } catch (IllegalArgumentException unavailable) {
        continue;
      }
    }
    return count;
  }

  /** 파생 스탯은 원래 필터에 등록된 기여 스탯들로 펼친다. 암묵 옵션은 새 획득 단계에서 제외한다. */
  private void addStats(String id, boolean lowerBound, boolean upperBound) {
    var stat = stats.get(id);
    if (stat == null) return;
    if (stat.kind() == GoalCatalog.Kind.EXPLICIT) {
      relevant.addAll(stat.sourceStatIds());
      if (lowerBound) lowerBoundStats.addAll(stat.sourceStatIds());
      if (upperBound) upperBoundStats.addAll(stat.sourceStatIds());
    } else if (stat.kind() == GoalCatalog.Kind.PSEUDO)
      stat.contributions().forEach(c -> addStats(c.statId(), lowerBound, upperBound));
  }

  /** 같은 쪽의 전체 추첨 풀을 보관하고 목표 범위를 적용하기 전 각 티어의 획득처를 구한다. */
  private Map<String, List<Source>> sourceMap(WorkbenchSimulator simulator) {
    var catalog = simulator.catalog();
    var base = start.baseItemId();
    var level = start.itemLevel();
    var pool =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.requiredItemLevel() <= level)
            .toList();
    var materialOptions = new HashMap<String, List<Source>>();
    simulator
        .sourceMaterialTargets(level)
        .forEach(
            (action, targets) -> {
              // 하위 에센스는 수동 작업대에서만 쓴다. 같은 일반 옵션의 다른 획득처는 유지한다.
              if (action.name().startsWith("LESSER_ESSENCE_")) return;
              var label = action.materialPolicy().mode() == MaterialMode.ADD ? "E" : "C";
              for (var target : targets)
                materialOptions
                    .computeIfAbsent(target, k -> new ArrayList<>())
                    .add(new Source(label, action.name(), null));
            });
    var availablePools = new LinkedHashMap<String, Set<String>>();
    var activeIds = new HashSet<String>();
    for (var augment : augmentPools.entrySet()) {
      boolean installed =
          start.installedAugments().stream().anyMatch(a -> a.augmentId().equals(augment.getKey()));
      if (installed) activeIds.addAll(augment.getValue());
      if (installed || additionalInstallations(start, simulator, augment.getKey()) > 0)
        availablePools.put(augment.getKey(), augment.getValue());
    }
    var baseSource = new Source("B", null, null);
    poolWeights.put(baseSource, weights(pool, activeIds));
    availablePools.forEach(
        (augment, ids) -> {
          var enabled = new HashSet<>(activeIds);
          enabled.addAll(ids);
          poolWeights.put(new Source("A", augment, null), weights(pool, enabled));
        });
    var result = new HashMap<String, List<Source>>();
    for (var d : pool) {
      if (d.stats().stream().noneMatch(s -> relevant.contains(s.id()))) continue;
      var list = new ArrayList<Source>();
      if (d.weight() > 0 && !d.tags().contains("augment_pool")) list.add(baseSource);
      list.addAll(materialOptions.getOrDefault(d.id(), List.of()));
      boolean inPool =
          d.weight() > 0 || availablePools.values().stream().anyMatch(ids -> ids.contains(d.id()));
      for (var bone : DesecrationCurrency.sources(base, level, d, inPool))
        list.add(new Source("D", bone, null));
      for (var augment : availablePools.entrySet()) {
        if (augment.getValue().contains(d.id())) list.add(new Source("A", augment.getKey(), null));
      }
      if (!list.isEmpty()) result.put(d.id(), List.copyOf(list));
    }
    return result;
  }

  /** 이미 열린 전용 풀과 후보 증강물의 전용 풀을 합집합으로 한 번만 세어 총가중치를 구한다. */
  private static Map<AffixType, Long> weights(
      List<ModifierDefinition> pool, Set<String> enabledAugmentIds) {
    var weights = new EnumMap<AffixType, Long>(AffixType.class);
    for (var d : pool) {
      long weight = d.weight();
      if (d.tags().contains("augment_pool"))
        weight =
            enabledAugmentIds.contains(d.id())
                ? AugmentCraftingRules.poolWeightPolicy().weightPerTier()
                : 0;
      weights.merge(d.affixType(), weight, Long::sum);
    }
    return weights;
  }

  /** 주어진 획득 가중치를 해당 출처·방향의 전체 가중치로 나눠 빈도를 판정한다. */
  private Frequency frequency(Source source, AffixType side, long weight) {
    double chance = (double) weight / poolWeights.get(source).get(side);
    return chance < lowProbability
        ? Frequency.L
        : chance >= highProbability ? Frequency.H : Frequency.M;
  }

  /** 최대 6옵션, 계열 충돌 없는 조합을 방문한다. 같은 접두와 티어를 반복 조합하지 않는다. */
  private void choose(int next, List<TierGroup> selected, Set<String> used, int prefixes) {
    if (++checked > MAX_CARD_CHECKS || cards.size() >= MAX_CARDS) {
      complete = false;
      return;
    }
    consider(selected);
    if (selected.size() == 6) return;
    for (int i = next; i < families.size() && complete; i++) {
      var f = families.get(i);
      int nextPrefixes = prefixes + (f.key().side() == AffixType.PREFIX ? 1 : 0);
      int nextSuffixes = selected.size() + 1 - nextPrefixes;
      boolean fits =
          jewelOverflow
              ? BasicJewel.existingCapacity(nextPrefixes, nextSuffixes, maxPrefixes, maxSuffixes)
              : nextPrefixes <= maxPrefixes && nextSuffixes <= maxSuffixes;
      if (!fits || !Collections.disjoint(f.key().families(), used)) continue;
      selected.add(f);
      used.addAll(f.key().families());
      choose(i + 1, selected, used, nextPrefixes);
      selected.removeLast();
      used.removeAll(f.key().families());
    }
  }

  /** 필요한 행의 존재 가능성, 실물 분열 티어와 수치, 제작 옵션 최소 점유를 보수적으로 판정한다. */
  private void consider(List<TierGroup> selected) {
    for (var fracture : held.values()) {
      if (fracture.fractured()
          && selected.stream()
              .noneMatch(
                  f -> f.tiers().stream().anyMatch(d -> d.id().equals(fracture.modifierId()))))
        return;
    }
    var rows = new HashSet<String>();
    selected.forEach(f -> rows.addAll(f.key().stats()));
    for (var group : groups) {
      var active = group.entries().stream().filter(e -> !e.disabled()).toList();
      long present = active.stream().filter(e -> canSupply(e.statId(), rows)).count();
      if (group.type() == GoalFilter.Type.AND && present < active.size()) return;
      if (group.type() == GoalFilter.Type.COUNT
          && group.range().min() != null
          && group.range().min().compareTo(BigDecimal.valueOf(present)) > 0) return;
    }
    int requiredCrafted = 0;
    var options = new ArrayList<Option>();
    var magnitudes = magnitudes(selected);
    for (var f : selected) {
      var tiers = eligibleTiers(f, selected, magnitudes);
      if (tiers.isEmpty()) return;
      if (tiers.entrySet().stream().allMatch(t -> requiresCrafted(t.getKey(), t.getValue())))
        requiredCrafted++;
      options.add(
          new Option(
              options.size() + 1,
              Collections.unmodifiableMap(tiers),
              !Collections.disjoint(f.key().stats(), lowerBoundStats),
              !Collections.disjoint(f.key().stats(), upperBoundStats)));
    }
    int extra = Math.max(0, requiredCrafted - craftedLimit);
    if (extra <= additionalAstrids) cards.add(new Card(List.copyOf(options), extra));
  }

  /** 분열 티어와 수치 범위에 맞으며 시작 상태부터 도달할 수 있는 티어만 남긴다. */
  private Map<String, List<Source>> eligibleTiers(
      TierGroup family, List<TierGroup> selected, Magnitudes magnitudes) {
    var tiers = new TreeMap<String, List<Source>>();
    var goalWeights = new HashMap<Source, Long>();
    var fractured =
        family.tiers().stream()
            .map(ModifierDefinition::id)
            .map(held::get)
            .filter(Objects::nonNull)
            .filter(ModifierInstance::fractured)
            .findFirst();
    for (var d : family.tiers()) {
      var choices = sources.getOrDefault(d.id(), List.of());
      if ((!held.containsKey(d.id()) && choices.stream().noneMatch(this::usableFromStart))
          || !matchesRanges(d, selected, magnitudes)) continue;
      // 분열은 실제 보유 티어만 카드에 남긴다. 빈도는 같은 목표를 충족할 수 있는 티어 전체로 판단한다.
      if (fractured.isEmpty() || fractured.get().modifierId().equals(d.id()))
        tiers.put(d.id(), choices);
      for (var source : choices) {
        if (!poolWeights.containsKey(source)) continue;
        long weight =
            source.label().equals("B")
                ? d.weight()
                : AugmentCraftingRules.poolWeightPolicy().weightPerTier();
        goalWeights.merge(source, weight, Long::sum);
      }
    }
    // 수치 범위가 허용하는 티어만 합한다. 한 증강물의 추첨과 다른 증강물의 추첨은 별개다.
    var classified = new HashMap<Source, Source>();
    goalWeights.forEach(
        (s, weight) ->
            classified.put(
                s, new Source(s.label(), s.id(), frequency(s, family.key().side(), weight))));
    tiers.replaceAll(
        (id, choices) -> choices.stream().map(s -> classified.getOrDefault(s, s)).toList());
    return tiers;
  }

  /** 희귀에서 마법으로 되돌리는 단계가 없으므로 승급형 에센스는 희귀 시작에서 제외한다. */
  private boolean usableFromStart(Source source) {
    return !source.label().equals("E") || start.rarity() != ItemState.Rarity.RARE;
  }

  /** 이미 보유한 일반 옵션과 다른 획득처를 모두 대조해 이 티어의 제작 칸 점유가 강제되는지 확인한다. */
  private boolean requiresCrafted(String tier, List<Source> choices) {
    var existing = held.get(tier);
    if (existing != null) {
      boolean crafted = AugmentCraftingRules.crafted(existing, itemCatalog);
      if (!crafted || existing.fractured()) return crafted;
    }
    return choices.stream().filter(this::usableFromStart).allMatch(s -> s.label().equals("C"));
  }

  /** 보유 옵션, 선택한 목표 또는 그 목표의 추첨 풀에서 실제로 붙을 수 있는 명시 강도만 반영한다. */
  private Magnitudes magnitudes(List<TierGroup> selected) {
    if (magnitudeProviders.isEmpty()) return new Magnitudes(List.of(), List.of());
    var reachable = new HashSet<>(held.keySet());
    var pools = new HashSet<String>();
    start.installedAugments().forEach(a -> pools.add(a.augmentId()));
    for (var group : selected) {
      for (var tier : group.tiers()) {
        reachable.add(tier.id());
        for (var source : sources.getOrDefault(tier.id(), List.of()))
          if (source.label().equals("A")) pools.add(source.id());
      }
    }
    pools.forEach(id -> reachable.addAll(augmentPools.getOrDefault(id, Set.of())));
    var lower = new ArrayList<ModifierInstance>();
    var upper = new ArrayList<ModifierInstance>();
    for (var d : magnitudeProviders) {
      if (!reachable.contains(d.id()) && (d.weight() == 0 || d.tags().contains("augment_pool")))
        continue;
      var existing = held.get(d.id());
      if (existing != null && existing.fractured()) {
        lower.add(existing);
        upper.add(existing);
      } else {
        // 제거·재획득 가능성을 포함한다. 서로 배타적인 제공자가 있어도 가능한 티어를 먼저 버리지 않는다.
        var lows = new HashMap<String, Long>();
        var highs = new HashMap<String, Long>();
        for (var stat : d.stats()) {
          lows.put(stat.id(), Math.min(0L, stat.min()));
          highs.put(stat.id(), Math.max(0L, stat.max()));
        }
        lower.add(new ModifierInstance(d.id(), lows));
        upper.add(new ModifierInstance(d.id(), highs));
      }
    }
    return new Magnitudes(lower, upper);
  }

  /** 단일 기여 옵션인 AND 행의 수치 범위와 양립하는 티어만 남긴다. 복합·합산은 2차 평가에 맡긴다. */
  private boolean matchesRanges(
      ModifierDefinition d, List<TierGroup> selected, Magnitudes magnitudes) {
    if (!checkTierRanges) return true;
    for (var group : groups) {
      if (group.type() != GoalFilter.Type.AND) continue;
      for (var e : group.entries()) {
        if (e.disabled() || e.range().min() == null && e.range().max() == null) continue;
        var s = stats.get(e.statId());
        if (s == null || s.kind() != GoalCatalog.Kind.EXPLICIT || s.sourceStatIds().size() != 1)
          continue;
        var raw = s.sourceStatIds().getFirst();
        if (selected.stream().filter(f -> f.key().stats().contains(raw)).count() != 1) continue;
        var range = d.stats().stream().filter(r -> r.id().equals(raw)).findFirst().orElse(null);
        if (range == null) continue;
        var fracture = held.get(d.id());
        long min = range.min(), max = range.max();
        if (fracture != null && fracture.fractured()) min = max = fracture.values().get(raw);
        var lo = ModifierMagnitudes.factor(magnitudes.lower(), d);
        var hi = ModifierMagnitudes.factor(magnitudes.upper(), d);
        var minRaw = BigDecimal.valueOf(min);
        var maxRaw = BigDecimal.valueOf(max);
        var lowValue = minRaw.multiply(lo).min(minRaw.multiply(hi));
        var highValue = maxRaw.multiply(hi).max(maxRaw.multiply(lo));
        var divisor = BigDecimal.valueOf(s.sourceDivisor() == null ? 1 : s.sourceDivisor());
        if (e.range().min() != null && e.range().min().multiply(divisor).compareTo(highValue) > 0
            || e.range().max() != null && e.range().max().multiply(divisor).compareTo(lowValue) < 0)
          return false;
      }
    }
    return true;
  }

  /** 선택한 계열과 시작 암묵 옵션이 하나의 직접·파생 목표 수치에 기여할 수 있는지 판정한다. */
  private boolean canSupply(String statId, Set<String> chosen) {
    var stat = stats.get(statId);
    if (stat == null) return false;
    return switch (stat.kind()) {
      case EXPLICIT -> stat.sourceStatIds().stream().anyMatch(chosen::contains);
      case IMPLICIT ->
          start.implicits().stream()
              .anyMatch(m -> stat.sourceStatIds().stream().anyMatch(m.values()::containsKey));
      case PSEUDO -> stat.contributions().stream().anyMatch(c -> canSupply(c.statId(), chosen));
    };
  }
}
