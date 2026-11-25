package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SocketAugments;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 작업대의 실제 목걸이·장갑 카탈로그로 카드의 분열 고정, 여러 획득처, 제작 한도와 룬 장착을 검증한다. */
class RouteCardsTest {
  /** 일반 최대 생명력의 원본 수치 ID다. */
  private static final String LIFE = "base_maximum_life";

  /** 장갑 교체형 에센스 두 개의 서로 다른 수치 ID다. */
  private static final String RECOUP = "lightning_damage_taken_goes_to_life_over_4_seconds_%";

  private static final String GOLD = "gold_+%_from_enemies";

  /** 작업대 서비스와 동일하게 베이스별 재료 대상과 추가 카탈로그를 결합한다. */
  private WorkbenchSimulator simulator(ItemCatalog original) {
    var catalog = EquipmentCurrencyCatalog.enrich(original);
    var data = WorkbenchDefinitions.active().targets(catalog.base().id());
    return new WorkbenchSimulator(
        catalog, new CraftingEngine(catalog), Set.of(), data.fixed(), data.replacements());
  }

  /** 지정된 원본 명시 수치가 각각 존재해야 하는 단순 목표 카드를 생성한다. */
  private RouteCards.Result cards(WorkbenchSimulator simulator, ItemState start, String... rawIds) {
    return cards(simulator, start, ItemState.Rarity.RARE, 0.01, 0.1, rawIds);
  }

  /** 희귀도와 H/M/L 경곗값을 바꿔 카탈로그를 만드는 테스트용 진입점이다. */
  private RouteCards.Result cards(
      WorkbenchSimulator simulator,
      ItemState start,
      ItemState.Rarity rarity,
      double low,
      double high,
      String... rawIds) {
    return cards(simulator, start, rarity, low, high, new GoalFilter.Range(null, null), rawIds);
  }

  /** 직접 수치의 하한·상한으로 각 원본 티어의 허용 범위를 검증한다. */
  private RouteCards.Result cards(
      WorkbenchSimulator simulator,
      ItemState start,
      ItemState.Rarity rarity,
      double low,
      double high,
      GoalFilter.Range rowRange,
      String... rawIds) {
    var context = new GoalFilter.Context(start.snapshotId(), start.baseItemId(), start.itemLevel());
    var stats =
        Arrays.stream(rawIds)
            .map(
                id ->
                    new GoalCatalog.Stat(
                        id,
                        id,
                        "flat",
                        GoalCatalog.Kind.EXPLICIT,
                        null,
                        true,
                        null,
                        List.of(id),
                        List.of(),
                        List.of()))
            .toList();
    var catalog = new GoalCatalog(1, "test", context, List.of(), stats, List.of());
    var any = new GoalFilter.Range(null, null);
    var entries =
        Arrays.stream(rawIds)
            .map(id -> new GoalFilter.Entry(id, id, "flat", rowRange, null, false))
            .toList();
    var goal =
        new GoalFilter(
            1,
            "test",
            new GoalFilter.General(start.baseItemId(), any, List.of(rarity)),
            List.of(new GoalFilter.Group("all", GoalFilter.Type.AND, false, null, entries)));
    return RouteCards.build(simulator, start, goal, catalog, low, high);
  }

  /** 아스트리드의 룬과 미장착 강도 제공자는 단일 직접 행의 범위 밖 티어를 획득으로 만들지 않는다. */
  @Test
  void directRangesWithUnrelatedRuneKeepExactlyTheOverlappingTiers() {
    var simulator = simulator(ItemCatalogLoader.loadStocky());
    var catalog = simulator.catalog();
    var empty =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            1);
    var installed = SocketAugments.install(empty, 0, "Astrids_Creativity");
    var stat = "base_fire_damage_resistance_%";
    for (var start : List.of(empty, installed)) {
      var all =
          cards(simulator, start, stat).cards().stream()
              .filter(c -> c.options().size() == 1)
              .flatMap(c -> c.options().getFirst().tiers().keySet().stream())
              .collect(java.util.stream.Collectors.toSet());
      var bounds =
          new GoalFilter.Range(java.math.BigDecimal.valueOf(17), java.math.BigDecimal.valueOf(28));
      var expected =
          all.stream()
              .filter(
                  id ->
                      catalog.find(id).orElseThrow().stats().stream()
                          .anyMatch(r -> r.id().equals(stat) && r.max() >= 17 && r.min() <= 28))
              .toList();
      assertThat(expected.size()).isGreaterThan(1).isLessThan(all.size());
      var bounded = cards(simulator, start, ItemState.Rarity.RARE, 0.01, 0.1, bounds, stat);
      assertThat(
              bounded.cards().stream()
                  .filter(c -> c.options().size() == 1)
                  .flatMap(c -> c.options().getFirst().tiers().keySet().stream())
                  .distinct())
          .containsExactlyInAnyOrderElementsOf(expected);
      var current = simulator.forItem(start);
      var pool = current.pool(start, WorkbenchCurrency.EXALTED, null);
      var total = pool.stream().mapToLong(ModifierDefinition::weight).sum();
      var hit =
          pool.stream()
              .filter(d -> expected.contains(d.id()))
              .mapToLong(ModifierDefinition::weight)
              .sum();
      var law =
          new RecipeTransitions(current, true)
              .successors(start, "EXALTED", Set.of(), Set.copyOf(expected), 2000, false);
      assertThat(law.complete()).isTrue();
      assertThat(
              law.outcomes().stream()
                  .map(RecipeTransitions.Outcome::probability)
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.of(hit, total));
    }
  }

  /** 합금의 실제 저항 강도가 붙은 반지에서는 원래 티어 상한보다 큰 표시값도 후보로 남긴다. */
  @Test
  void existingResistanceMagnitudePreservesTheScaledTier() {
    var simulator = simulator(ItemCatalogLoader.loadRing());
    var catalog = simulator.catalog();
    var providerId = "equipment:prefix:alloy-effect-of-resistance-mods1";
    var stat = "base_fire_damage_resistance_%";
    var tier =
        catalog.modifiers().values().stream()
            .filter(d -> d.weight() > 0 && d.tags().contains("resistance") && d.stats().size() == 1)
            .filter(d -> d.stats().getFirst().id().equals(stat) && d.stats().getFirst().max() > 0)
            .min(java.util.Comparator.comparingInt(ModifierDefinition::requiredItemLevel))
            .orElseThrow();
    var boost = catalog.find(providerId).orElseThrow().stats().getFirst();
    var maximum = java.math.BigDecimal.valueOf(tier.stats().getFirst().max());
    var requested = maximum.multiply(java.math.BigDecimal.valueOf(100L + boost.max(), 2));
    var range = new GoalFilter.Range(requested, requested);
    var empty =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of());
    var provider = new ModifierInstance(providerId, Map.of(boost.id(), boost.max())).asCrafted();
    var scaled =
        new ItemState(
            empty.snapshotId(),
            empty.baseItemId(),
            82,
            empty.rarity(),
            List.of(),
            List.of(provider),
            Set.of());
    assertThat(requested).isGreaterThan(maximum);
    for (var start : List.of(empty, scaled)) {
      var bounded = cards(simulator, start, ItemState.Rarity.RARE, 0.01, 0.1, range, stat);
      assertThat(
              bounded.cards().stream()
                  .flatMap(c -> c.options().stream())
                  .anyMatch(o -> o.tiers().containsKey(tier.id())))
          .isEqualTo(start == scaled);
    }
  }

  /** 보유 중인 분열 티어만 허용하고 다른 목표만으로 구성된 카드는 거른다. */
  @Test
  void fracturePinsTheActualTierAndMustBelongToTheCard() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = simulator(catalog);
    var root = SolarAmulet.initial(catalog, 82, 15);
    var fracture = new ModifierInstance("amulet:prefix:rotund", Map.of(LIFE, 90L), true);
    var start =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(fracture),
            Set.of());
    var result = cards(simulator, start, LIFE);
    assertThat(result.complete()).isTrue();
    assertThat(result.cards())
        .isNotEmpty()
        .allMatch(
            c ->
                c.options().stream()
                    .anyMatch(o -> o.tiers().keySet().equals(Set.of(fracture.modifierId()))));
    assertThat(cards(simulator, start, "base_chaos_damage_resistance_%").cards()).isEmpty();
  }

  /** 생명력의 일반 티어에는 일반 풀, 일반 에센스, 공개의 획득처가 함께 남는다. */
  @Test
  void onlyWeightedSourcesReceiveFrequency() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = simulator(catalog);
    var result = cards(simulator, SolarAmulet.initial(catalog, 82, 15), LIFE);
    var sources =
        result.cards().stream()
            .flatMap(c -> c.options().stream())
            .flatMap(o -> o.tiers().values().stream())
            .toList();
    assertThat(sources)
        .anyMatch(
            tier ->
                tier.stream()
                    .map(RouteCards.Source::label)
                    .toList()
                    .containsAll(List.of("B", "E", "D")));
    assertThat(sources)
        .allMatch(
            tier ->
                tier.stream()
                    .allMatch(
                        s ->
                            (s.label().equals("B") || s.label().equals("A"))
                                == (s.frequency() != null)));
    // 수치가 없는 생명력은 일반 접두 아홉 티어(9,000/72,200)가 모두 적격이다.
    assertThat(sources.stream().flatMap(List::stream).filter(s -> s.label().equals("B")))
        .isNotEmpty()
        .allMatch(s -> s.frequency() == RouteCards.Frequency.H);
  }

  /** 마법 목걸이에서 하위 에센스는 수동으로 사용할 수 있고 제작지원의 후보·고정 재생은 일반 등급부터 허용한다. */
  @Test
  void lesserEssenceIsManualOnlyWhileOrdinaryAndGreaterRemainReplayable() {
    var simulator = simulator(ItemCatalogLoader.loadDefault());
    var start =
        RecipeStateGroups.withAffixes(
            SolarAmulet.initial(simulator.catalog(), 82, 15),
            ItemState.Rarity.MAGIC,
            List.of(
                new ModifierInstance(
                    "amulet:suffix:of-the-seal", Map.of("base_cold_damage_resistance_%", 6L))));
    var lesser = WorkbenchCurrency.LESSER_ESSENCE_BODY;
    var card = cards(simulator, start, LIFE).cards().getFirst();
    var option = card.options().getFirst();
    var lesserTier = simulator.sourceMaterialTargets(82).get(lesser).getFirst();
    assertThat(
            simulator.apply(start, lesser, Set.of(), new java.util.Random(1)).state().explicits())
        .extracting(ModifierInstance::modifierId)
        .contains(lesserTier);
    assertThat(option.tiers().get(lesserTier)).extracting(RouteCards.Source::label).contains("B");
    assertThat(option.tiers().values().stream().flatMap(List::stream))
        .noneMatch(source -> lesser.name().equals(source.id()));

    for (var action :
        List.of(lesser, WorkbenchCurrency.ESSENCE_BODY, WorkbenchCurrency.GREATER_ESSENCE_BODY)) {
      var step =
          new RecipeRoutePlanner.Step(
              action.name(), List.of(), List.of(option.number()), List.of(), null);
      var replay =
          RecipeRoutePlanner.evaluateFixed(
              simulator,
              start,
              card,
              state -> state.explicits().stream().anyMatch(m -> m.values().containsKey(LIFE)),
              2000,
              true,
              null,
              List.of(step));
      assertThat(replay.complete()).isTrue();
      if (action == lesser) assertThat(replay.routes()).isEmpty();
      else
        assertThat(replay.routes())
            .singleElement()
            .satisfies(r -> assertThat(r.probability()).isOne());
    }
  }

  /** 정신력 43 이상은 두 티어의 합이 M이고 47 이상은 한 티어만 적격이라 L이다. 실제로 보유한 티어도 목표 빈도로 보호한다. */
  @Test
  void spiritFrequencyUsesEveryEligibleTierEvenWhenAHigherTierIsAlreadyHeld() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = simulator(catalog);
    var initial = SolarAmulet.initial(catalog, 82, 15);
    String spirit = "base_spirit_from_equipment";
    String t1 = "amulet:prefix:countess";
    String t2 = "amulet:prefix:marchioness";
    var owned = new ModifierInstance(t1, Map.of(spirit, 47L));
    var fracture = new ModifierInstance(t1, owned.values(), true);
    var empty = RecipeStateGroups.withAffixes(initial, ItemState.Rarity.RARE, List.of());
    for (var mods : List.of(List.<ModifierInstance>of(), List.of(owned), List.of(fracture))) {
      var start = RecipeStateGroups.withAffixes(empty, empty.rarity(), mods);
      for (int lower : List.of(43, 47)) {
        var bound = new GoalFilter.Range(java.math.BigDecimal.valueOf(lower), null);
        var result = cards(simulator, start, ItemState.Rarity.RARE, 0.01, 0.1, bound, spirit);
        var option =
            result.cards().stream()
                .flatMap(card -> card.options().stream())
                .filter(o -> o.tiers().containsKey(t1))
                .findFirst()
                .orElseThrow();
        var eligible = lower == 47 || mods.contains(fracture) ? Set.of(t1) : Set.of(t1, t2);
        assertThat(option.tiers().keySet()).isEqualTo(eligible);
        var expected = lower == 43 ? RouteCards.Frequency.M : RouteCards.Frequency.L;
        assertThat(option.tiers().values())
            .allSatisfy(
                sources ->
                    assertThat(sources.stream().filter(s -> s.label().equals("B")))
                        .singleElement()
                        .extracting(RouteCards.Source::frequency)
                        .isEqualTo(expected));
      }
    }
  }

  /** 장갑의 0홈에서 장인의 오브를 사용할 수 있으며 이미 장착한 룬은 다시 요구하지 않는다. */
  @Test
  void twoCraftedOptionsRequireAnActualAstridAndOpeningAZeroSocketItemIsAllowed() {
    var simulator = simulator(ItemCatalogLoader.loadStocky());
    var item = simulator.catalog();
    var empty =
        new ItemState(
            item.metadata().snapshotId(),
            item.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            0);
    assertThat(cards(simulator, empty, RECOUP, GOLD).cards())
        .isNotEmpty()
        .allMatch(c -> c.requiredAstrids() == 1);
    var open =
        new ItemState(
            empty.snapshotId(),
            empty.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            1);
    var installed = SocketAugments.install(open, 0, "Astrids_Creativity");
    assertThat(cards(simulator, installed, RECOUP, GOLD).cards())
        .isNotEmpty()
        .allMatch(c -> c.requiredAstrids() == 0);
    var amulet = ItemCatalogLoader.loadDefault();
    assertThat(
            cards(
                    simulator(amulet),
                    SolarAmulet.initial(amulet, 82, 15),
                    "global_armour_evasion_energy_shield_+%",
                    "strength_+%")
                .cards())
        .isEmpty();
  }

  /** 증강물 풀로만 열리는 티어에는 실제 장착할 증강물 ID와 출현 빈도를 함께 기록한다. */
  @Test
  void augmentOnlyTierNamesItsSocketSource() {
    var simulator = simulator(ItemCatalogLoader.loadStocky());
    var item = simulator.catalog();
    var empty =
        new ItemState(
            item.metadata().snapshotId(),
            item.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            0);
    var id = "augment:marksman:MarksmanInfluenceProjectileDamage1";
    var sources =
        cards(simulator, empty, "projectile_damage_+%").cards().stream()
            .flatMap(c -> c.options().stream())
            .filter(o -> o.tiers().containsKey(id))
            .flatMap(o -> o.tiers().get(id).stream())
            .toList();
    assertThat(sources)
        .anyMatch(
            s -> s.label().equals("A") && s.id().equals("Kolrs_Hunt") && s.frequency() != null);
    var open =
        new ItemState(
            empty.snapshotId(),
            empty.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            1);
    var enabled = SocketAugments.install(open, 0, "Kolrs_Hunt");
    var context = simulator.forItem(enabled).catalog();
    var side = context.find(id).orElseThrow().affixType();
    var state = DesecrationCurrency.apply(enabled, "Preserved_Rib", side, null, Set.of(), context);
    assertThat(state.eligibleRevealIds(state.slots().getLast().id(), context)).contains(id);
    assertThat(sources)
        .anyMatch(
            s -> s.label().equals("D") && s.id().equals("Preserved_Rib") && s.frequency() == null);
  }

  /** 분열 옵션을 포함해 같은 쪽 한도를 넘는 카드는 희귀도별로 사전에 제외한다. */
  @Test
  void prefilterCountsFractureTowardsTheRaritySideLimit() {
    var fixture = new BasicCurrencyFixture();
    var defs = new java.util.ArrayList<ModifierDefinition>();
    defs.add(fixture.implicit);
    defs.add(fixture.b);
    for (var raw : List.of("p1", "p2", "p3", "p4"))
      defs.add(fixture.definition(raw, ModifierDefinition.AffixType.PREFIX, 1, 2, 1));
    var catalog = fixture.catalog(defs);
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var held = new ModifierInstance("p1", Map.of("p1", 1L), true);
    var item = fixture.item(ItemState.Rarity.MAGIC, List.of(held));
    assertThat(cards(simulator, item, ItemState.Rarity.MAGIC, 0.01, 0.1, "p1", "p2").cards())
        .isEmpty();
    assertThat(cards(simulator, item, "p1", "p2", "p3").cards()).isNotEmpty();
    assertThat(cards(simulator, item, "p1", "p2", "p3", "p4").cards()).isEmpty();
  }

  /** 두 전용 풀이 열릴 때 실제 재선택한 작업대의 분모로 두 번째 증강물 티어 빈도를 분류한다. */
  @Test
  void twoDistinctPoolAugmentsAreResolvedFromTheBaseForEachStep() {
    var solar = ItemCatalogLoader.loadDefault();
    var service =
        new CatalogCraftingService(solar, simulator(solar), ItemCatalogLoader.loadStocky());
    var item = ItemCatalogLoader.loadStocky();
    var empty =
        new ItemState(
            item.metadata().snapshotId(),
            item.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(),
            Set.of(),
            2);
    var marksman = SocketAugments.install(empty, 0, "Kolrs_Hunt");
    var both = SocketAugments.install(marksman, 1, "Katlas_Gloom");
    var decay = SocketAugments.install(empty, 1, "Katlas_Gloom");
    var source = service.baseSimulator(marksman);
    var id = "augment:decay:DecayInfluenceIgniteMagnitude1";
    var definition = source.catalog().find(id).orElseThrow();
    assertThat(source.forItem(marksman).catalog().find(id).orElseThrow().weight()).isZero();
    assertThat(source.forItem(both).catalog().find(id).orElseThrow().weight()).isPositive();
    // 각각의 실제 sampler에서 분모를 읽는다. 두 값 중간의 경계를 쓰면 잘못된 분모는 등급이 바뀐다.
    var ids = List.of(id, "augment:decay:DecayInfluenceIgniteMagnitude2");
    var chanceBoth = chance(source.forItem(both), ids, empty.itemLevel());
    var chanceDecay = chance(source.forItem(decay), ids, empty.itemLevel());
    assertThat(chanceBoth).isLessThan(chanceDecay);
    var midpoint = (chanceBoth + chanceDecay) / 2;
    var test =
        cards(
            source,
            marksman,
            ItemState.Rarity.RARE,
            chanceBoth * 0.75,
            midpoint,
            definition.stats().getFirst().id());
    var labels =
        test.cards().stream()
            .flatMap(c -> c.options().stream())
            .flatMap(o -> o.tiers().getOrDefault(id, List.of()).stream())
            .toList();
    assertThat(labels.stream().filter(s -> s.label().equals("A")))
        .isNotEmpty()
        .allMatch(s -> s.id().equals("Katlas_Gloom") && s.frequency() == RouteCards.Frequency.M);
  }

  /** 적용된 카탈로그의 같은 쪽 전체 풀에서 목표가 허용하는 티어 가중치를 직접 합산한다. */
  private double chance(WorkbenchSimulator simulator, List<String> ids, int level) {
    var side = simulator.catalog().find(ids.getFirst()).orElseThrow().affixType();
    long total =
        simulator.catalog().modifiers().values().stream()
            .filter(d -> d.affixType() == side && d.requiredItemLevel() <= level)
            .mapToLong(ModifierDefinition::weight)
            .sum();
    long hit =
        ids.stream()
            .map(id -> simulator.catalog().find(id).orElseThrow())
            .mapToLong(ModifierDefinition::weight)
            .sum();
    return (double) hit / total;
  }
}
