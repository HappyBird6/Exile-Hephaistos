package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 운영 작업대와 같은 카탈로그·목표 서비스로 비저항 옵션의 수치와 실제 티어 조합을 검증한다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NonResistanceGoalPreviewTest {
  /** 실제 베이스·재료를 조회하고 기폭제를 적용하는 작업대다. */
  private final CatalogCraftingService workbench;

  /** 완성 카드 생성 및 각 티어 예시의 목표 재평가에 사용한다. */
  private final GoalFilterService service;

  /** 전체 장비를 운영 설정으로 한 번 등록한다. */
  NonResistanceGoalPreviewTest() {
    var cfg = new CatalogConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    service = new CraftingSupportConfiguration().goalFilterService(workbench);
  }

  /** 실제 시작 아이템의 스냅샷과 레벨을 사용한다. */
  private Context context(String base, int level) {
    var item = workbench.initial(base, level).state();
    return new Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
  }

  /** 해당 명시 옵션이 지원하는 표시 단위로 행을 구성한다. */
  private Entry row(GoalCatalog catalog, String stat, Range range, BigDecimal weight) {
    var id = GoalCatalogIndex.id("explicit", stat);
    var spec =
        catalog.stats().stream().filter(s -> s.statId().equals(id)).findFirst().orElseThrow();
    assertThat(spec.support().evaluation()).isEqualTo(Capability.SUPPORTED);
    return new Entry(stat, id, spec.unit(), range, weight, false);
  }

  /** 양 끝을 같은 값으로 설정한 정확한 표시 수치다. */
  private Range exact(BigDecimal value) {
    return new Range(value, value);
  }

  /** 이 베이스의 희귀 장비를 대상으로 품질 미지정 조건을 구성한다. */
  private GoalFilter goal(GoalCatalog catalog, Group group) {
    return new GoalFilter(
        1,
        catalog.catalogVersion(),
        new General(
            catalog.context().baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
        List.of(group));
  }

  /** 실제 분열 굴림과 재료 적용 결과가 기준 수치를 증명하고 카드에도 같은 품질·옵션이 남는지 검증한다. */
  @ParameterizedTest
  @CsvSource({
    "solar,amulet:prefix:athlete-s,,149,", "solar,amulet:prefix:athlete-s,CATALYST_FLESH,179,",
    "solar,amulet:suffix:of-the-titan,,33,",
        "solar,amulet:suffix:of-the-titan,CATALYST_ADAPTIVE,40,",
    "solar,amulet:suffix:of-immortality,,33,",
        "solar,amulet:suffix:of-immortality,CATALYST_FLESH,39.6,",
    "solar,amulet:prefix:incandescent,,89,",
        "solar,amulet:prefix:incandescent,CATALYST_CARAPACE,107,",
    "ring,iron-ring:prefix:zaffre,,179,", "ring,iron-ring:prefix:zaffre,CATALYST_NEURAL,215,",
    "ring,iron-ring:prefix:adroit,,233,", "ring,iron-ring:prefix:adroit,CATALYST_CARAPACE,280,",
    "ring,iron-ring:prefix:flaring,,32,19",
        "ring,iron-ring:prefix:flaring,CATALYST_UUL_NETOL,38,23",
    "stocky,stocky-mitts:prefix:cremating,,45,29",
        "stocky,stocky-mitts:suffix:of-the-vampire,,9.9,",
    "bow,crude-bow:suffix:of-the-sniper,,4,", "wand,attuned-wand:suffix:of-finesse,,35,",
    "wand,attuned-wand:prefix:lich-s,,49,45", "body,rusted-cuirass:prefix:queen-s,,61,",
    "helmet,rusted-greathelm:prefix:hoarder-s,,19,"
  })
  void frozenRollMatchesWorkbench(
      String base, String id, WorkbenchCurrency currency, BigDecimal first, BigDecimal second) {
    var start = workbench.initial(base, 82).state();
    var context = new Context(start.snapshotId(), start.baseItemId(), 82);
    var catalog = service.catalog(context);
    var simulator = workbench.simulatorForBase(start.baseItemId());
    var d = catalog.sourceModifiers().get(id);
    var values = new HashMap<String, Long>();
    var targets = second == null ? List.of(first) : List.of(first, second);
    assertThat(d.stats()).hasSize(targets.size());
    var rows = new ArrayList<Entry>();
    for (int i = 0; i < targets.size(); i++) {
      var stat = d.stats().get(i);
      values.put(stat.id(), stat.max());
      rows.add(row(catalog, stat.id(), exact(targets.get(i)), null));
    }
    var frozen = new ModifierInstance(id, values, true);
    var item =
        new ItemState(
            start.snapshotId(),
            start.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            start.implicits(),
            List.of(frozen),
            Set.of());
    if (currency != null) {
      var applied = simulator.apply(item, currency, Set.of(), new Random(1));
      assertThat(applied.applied()).isTrue();
      item = applied.state();
    }
    var expectedQuality = item.catalystQuality();
    var goal = goal(catalog, new Group("both", Type.AND, false, null, rows));
    assertThat(service.evaluate(item, goal).status()).isEqualTo(Status.MATCH);
    var variants =
        service.preview(context, goal, workbench, frozen, id).cards().stream()
            .flatMap(c -> c.tierVariants().stream())
            .toList();
    assertThat(variants)
        .anySatisfy(
            v -> {
              assertThat(v.catalystQuality()).isEqualTo(expectedQuality);
              assertThat(v.explicits()).containsExactly(frozen);
            })
        .allSatisfy(v -> assertThat(service.evaluate(v, goal).status()).isEqualTo(Status.MATCH));
    var impossible =
        goal(
            catalog,
            new Group(
                "above",
                Type.AND,
                false,
                null,
                List.of(
                    row(
                        catalog,
                        d.stats().getFirst().id(),
                        exact(BigDecimal.valueOf(100000)),
                        null))));
    assertThat(service.preview(context, impossible, workbench, frozen, id).cards()).isEmpty();
  }

  /** 실제 품질 50%인 같은 목걸이로 입증한 두 행의 개수와 가중합을 카드 탐색과 대조한다. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      value = Type.class,
      names = {"COUNT", "WEIGHTED_V1", "WEIGHTED_V2"})
  void compoundGoalsRequireOneCatalyst(Type type) {
    var start = workbench.initial("solar", 82).state();
    var context = context("solar", 82);
    var catalog = service.catalog(context);
    String life = "base_maximum_life",
        regen = "base_life_regeneration_rate_per_minute",
        strength = "additional_strength";
    var item =
        new ItemState(
            start.snapshotId(),
            start.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            start.implicits(),
            List.of(
                new ModifierInstance("amulet:prefix:athlete-s", Map.of(life, 149L)),
                new ModifierInstance("amulet:suffix:of-immortality", Map.of(regen, 1980L)),
                new ModifierInstance("amulet:suffix:of-the-titan", Map.of(strength, 33L)),
                new ModifierInstance(
                        QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))
                    .asCrafted()),
            Set.of());
    var simulator = workbench.simulatorForBase(start.baseItemId());
    var controls = new ArrayList<ItemState>();
    for (var currency :
        List.of(WorkbenchCurrency.CATALYST_FLESH, WorkbenchCurrency.CATALYST_ADAPTIVE)) {
      var typed = simulator.apply(item, currency, Set.of(), new Random(1));
      assertThat(typed.applied()).isTrue();
      var infused =
          simulator.apply(
              typed.state(), WorkbenchCurrency.VAAL_CATALYSING_INFUSER, Set.of(), new Random(1));
      assertThat(infused.applied()).isTrue();
      controls.add(infused.state());
    }
    boolean count = type == Type.COUNT;
    var unbounded = new Range(null, null);
    var lifeRow =
        row(
            catalog,
            life,
            count ? exact(BigDecimal.valueOf(224)) : unbounded,
            count ? null : BigDecimal.TWO);
    var regenRow =
        row(
            catalog,
            regen,
            count ? exact(new BigDecimal("49.5")) : unbounded,
            count ? null : BigDecimal.ONE);
    var strengthRow =
        row(
            catalog,
            strength,
            count ? exact(BigDecimal.valueOf(50)) : unbounded,
            count ? null : BigDecimal.ONE);
    // 생명력과 재생은 같은 기폭제로 함께 오른다. 힘까지 넣으면 서로 다른 기폭제의 최댓값을 합칠 수 없다.
    var shared =
        goal(
            catalog,
            new Group(
                "shared",
                type,
                false,
                exact(count ? BigDecimal.TWO : new BigDecimal("497.5")),
                List.of(lifeRow, regenRow)));
    var boundary =
        goal(
            catalog,
            new Group(
                "separate",
                type,
                false,
                exact(BigDecimal.valueOf(count ? 1 : 481)),
                List.of(lifeRow, strengthRow)));
    var over =
        goal(
            catalog,
            new Group(
                "over",
                type,
                false,
                new Range(BigDecimal.valueOf(count ? 2 : 482), null),
                List.of(lifeRow, strengthRow)));
    for (var goal : List.of(shared, boundary, over)) {
      var accepted =
          controls.stream()
              .filter(i -> service.evaluate(i, goal).status() == Status.MATCH)
              .map(i -> i.catalystQuality().type())
              .toList();
      if (goal == over) assertThat(accepted).isEmpty();
      else if (count && goal == boundary)
        assertThat(accepted)
            .containsExactly(CatalystQuality.Type.FLESH, CatalystQuality.Type.ADAPTIVE);
      else assertThat(accepted).containsExactly(CatalystQuality.Type.FLESH);
      var cards = service.preview(context, goal, workbench).cards();
      var variants = cards.stream().flatMap(c -> c.tierVariants().stream()).toList();
      assertThat(variants)
          .allSatisfy(v -> assertThat(service.evaluate(v, goal).status()).isEqualTo(Status.MATCH));
      assertThat(variants.stream().map(i -> i.catalystQuality().type()).distinct().toList())
          .as("%s / %s", type, goal.groups().getFirst().id())
          .containsExactlyInAnyOrderElementsOf(accepted);
    }
  }

  /** 실제 교체 화폐로 전용 옵션을 만든 뒤 일반 옵션을 엑잘로 추가한 카드와 획득처를 검증한다. */
  @ParameterizedTest
  @CsvSource({
    "PERFECT_ESSENCE_ENHANCEMENT,ESSENCE,amulet:prefix:essence-global-defences,global_armour_evasion_energy_shield_+%",
    "RUNIC_ALLOY,ALLOY,amulet:prefix:alloy-maximum-runic-ward,maximum_ward_+%"
  })
  void materialExclusiveAndOrdinaryAffix(
      WorkbenchCurrency currency, ModifierAcquisition.Kind kind, String specialId, String stat) {
    var start = workbench.initial("solar", 82).state();
    var context = context("solar", 82);
    var catalog = service.catalog(context);
    var simulator = workbench.simulatorForBase(start.baseItemId());
    String ordinaryId = "amulet:prefix:hale", life = "base_maximum_life";
    var item =
        new ItemState(
            start.snapshotId(),
            start.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            start.implicits(),
            List.of(new ModifierInstance(ordinaryId, Map.of(life, 17L))),
            Set.of());
    var replaced = simulator.apply(item, currency, Set.of(), new Random(1));
    assertThat(replaced.applied()).as(replaced.reason()).isTrue();
    var result =
        simulator.applyCustomExalt(
            replaced.state(), WorkbenchCurrency.EXALTED, Set.of(), new Random(2), ordinaryId);
    assertThat(result.applied()).isTrue();
    var rows = new ArrayList<Entry>();
    for (var mod : result.state().explicits()) {
      String raw = mod.modifierId().equals(specialId) ? stat : life;
      rows.add(row(catalog, raw, exact(BigDecimal.valueOf(mod.values().get(raw))), null));
    }
    var group = new Group("both", Type.AND, false, null, rows);
    var goal = goal(catalog, group);
    assertThat(service.evaluate(result.state(), goal).status()).isEqualTo(Status.MATCH);
    var cards = service.preview(context, goal, workbench).cards();
    assertThat(cards)
        .anySatisfy(
            card -> {
              assertThat(card.sources().get(specialId))
                  .containsExactly(new ModifierAcquisition(kind, currency.name()));
              assertThat(card.sources().get(ordinaryId))
                  .contains(new ModifierAcquisition(ModifierAcquisition.Kind.NORMAL, null));
              assertThat(card.tierVariants())
                  .anySatisfy(
                      v -> {
                        assertThat(v.catalystQuality()).isNull();
                        assertThat(v.explicits())
                            .containsExactlyInAnyOrderElementsOf(result.state().explicits());
                      });
            });
    assertThat(cards)
        .allSatisfy(
            c ->
                c.tierVariants()
                    .forEach(
                        v ->
                            assertThat(service.evaluate(v, goal).status())
                                .isEqualTo(Status.MATCH)));
    var magic =
        new GoalFilter(
            1,
            catalog.catalogVersion(),
            new General(
                context.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.MAGIC)),
            List.of(group));
    assertThat(service.preview(context, magic, workbench).cards()).isEmpty();
    var low = context("solar", catalog.sourceModifiers().get(specialId).requiredItemLevel() - 1);
    assertThat(simulator.modifierSources(low.itemLevel()))
        .containsKey(ordinaryId)
        .doesNotContainKey(specialId);
    assertThat(service.preview(low, goal, workbench).cards()).isEmpty();
  }

  /** 두 전용 옵션 각각에 대해 생명력 35 이상을 실제로 굴릴 수 있는 일반 티어 집합과 카드의 티어 집합을 대조한다. */
  @ParameterizedTest
  @CsvSource({
    "amulet:prefix:essence-global-defences,global_armour_evasion_energy_shield_+%",
    "amulet:prefix:alloy-maximum-runic-ward,maximum_ward_+%"
  })
  void specialAffixKeepsEveryEligibleOrdinaryLifeTier(String specialId, String stat) {
    var context = context("solar", 82);
    var catalog = service.catalog(context);
    String life = "base_maximum_life";
    var floor = BigDecimal.valueOf(35);
    var goal =
        goal(
            catalog,
            new Group(
                "tiers",
                Type.AND,
                false,
                null,
                List.of(
                    row(catalog, stat, new Range(null, null), null),
                    row(catalog, life, new Range(floor, null), null))));
    var expected =
        catalog.sourceModifiers().values().stream()
            .filter(
                d ->
                    d.weight() > 0
                        && d.requiredItemLevel() <= context.itemLevel()
                        && d.stats().size() == 1)
            .filter(
                d ->
                    d.stats().getFirst().id().equals(life)
                        && d.stats().getFirst().max() >= floor.longValueExact())
            .map(ModifierDefinition::id)
            .toList();
    assertThat(expected).hasSizeGreaterThan(1);
    var variants =
        service.preview(context, goal, workbench).cards().stream()
            .flatMap(c -> c.tierVariants().stream())
            .filter(v -> v.catalystQuality() == null && v.explicits().size() == 2)
            .filter(v -> v.explicits().stream().anyMatch(m -> m.modifierId().equals(specialId)))
            .toList();
    var found = new HashSet<String>();
    for (var variant : variants) {
      assertThat(service.evaluate(variant, goal).status()).isEqualTo(Status.MATCH);
      for (var mod : variant.explicits())
        if (!mod.modifierId().equals(specialId)) found.add(mod.modifierId());
    }
    assertThat(found).containsExactlyInAnyOrderElementsOf(expected);
  }

  /** 제작 옵션 한도가 하나인 태양의 목걸이에는 두 전용 접두를 생명력과 동시에 붙일 수 없다. */
  @Test
  void solarAmuletCannotKeepBothExclusivePrefixesWithLife() {
    var context = context("solar", 82);
    var catalog = service.catalog(context);
    var goal =
        goal(
            catalog,
            new Group(
                "three",
                Type.AND,
                false,
                null,
                List.of(
                    row(catalog, "base_maximum_life", exact(BigDecimal.valueOf(17)), null),
                    row(
                        catalog,
                        "global_armour_evasion_energy_shield_+%",
                        new Range(null, null),
                        null),
                    row(catalog, "maximum_ward_+%", new Range(null, null), null))));
    assertThat(AugmentCraftingRules.capacityAugments(context.baseItemId())).isEmpty();
    assertThat(service.preview(context, goal, workbench).cards()).isEmpty();
  }

  /** 마법봉은 추가 제작 슬롯으로 전용 에센스·합금과 일반 접미를 유지한다. 같은 계열의 분열 마나는 합금과 충돌한다. */
  @Test
  void wandSupportsMixedMaterialsWithAnExtraCraftedSlotButRejectsFamilyConflict() {
    var start = workbench.initial("wand", 82).state();
    var context = context("wand", 82);
    var catalog = service.catalog(context);
    var sim = workbench.simulatorForBase(start.baseItemId());
    String alloy = "equipment:prefix:alloy-spell-level-mana-hybrid1",
        essence = "attuned-wand:suffix:essence-spell-skill-level",
        normal = "attuned-wand:suffix:of-finesse",
        mana = "base_maximum_mana",
        speed = "base_cast_speed_+%",
        skill = "spell_skill_gem_level_+";
    var initial =
        new ItemState(
            start.snapshotId(),
            start.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            start.implicits(),
            List.of(),
            Set.of(),
            1);
    var socketed = SocketAugments.install(initial, 0, "Astrids_Creativity");
    var random = new Random(17);
    String filler = "attuned-wand:suffix:of-the-pupil";
    var carrier =
        sim.applyCustomExalt(socketed, WorkbenchCurrency.EXALTED, Set.of(), random, filler);
    var first = sim.apply(carrier.state(), WorkbenchCurrency.CELESTIAL_ALLOY, Set.of(), random);
    assertThat(first.applied()).as(first.reason()).isTrue();
    carrier =
        sim.applyCustomExalt(first.state(), WorkbenchCurrency.EXALTED, Set.of(), random, filler);
    var second =
        sim.apply(
            carrier.state(),
            WorkbenchCurrency.PERFECT_ESSENCE_SORCERY,
            Set.of(WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            random);
    assertThat(second.applied()).as(second.reason()).isTrue();
    var result =
        sim.applyCustomExalt(second.state(), WorkbenchCurrency.EXALTED, Set.of(), random, normal);
    assertThat(result.applied()).isTrue();
    var mods = result.state().explicits();
    assertThat(mods)
        .extracting(ModifierInstance::modifierId)
        .containsExactlyInAnyOrder(alloy, essence, normal);
    var rows = new ArrayList<Entry>();
    for (var stat : List.of(mana, skill, speed)) {
      long total = mods.stream().mapToLong(m -> m.values().getOrDefault(stat, 0L)).sum();
      rows.add(row(catalog, stat, exact(BigDecimal.valueOf(total)), null));
    }
    var goal = goal(catalog, new Group("mixed", Type.AND, false, null, rows));
    assertThat(service.evaluate(result.state(), goal).status()).isEqualTo(Status.MATCH);
    var cards = service.preview(context, goal, workbench, null, alloy).cards();
    assertThat(cards)
        .anySatisfy(
            card -> {
              assertThat(card.sources().get(alloy))
                  .containsExactly(
                      new ModifierAcquisition(ModifierAcquisition.Kind.ALLOY, "CELESTIAL_ALLOY"));
              assertThat(card.sources().get(essence))
                  .containsExactly(
                      new ModifierAcquisition(
                          ModifierAcquisition.Kind.ESSENCE, "PERFECT_ESSENCE_SORCERY"));
              assertThat(card.sources().get(normal))
                  .contains(new ModifierAcquisition(ModifierAcquisition.Kind.NORMAL, null));
              assertThat(card.tierVariants())
                  .anySatisfy(
                      v -> {
                        assertThat(v.explicits()).containsExactlyInAnyOrderElementsOf(mods);
                        assertThat(v.installedAugments())
                            .extracting(ItemState.InstalledAugment::augmentId)
                            .contains("Astrids_Creativity");
                      });
            });
    assertThat(cards)
        .allSatisfy(
            c ->
                c.tierVariants()
                    .forEach(
                        v ->
                            assertThat(service.evaluate(v, goal).status())
                                .isEqualTo(Status.MATCH)));
    String conflict = "attuned-wand:prefix:beryl";
    var def = catalog.sourceModifiers().get(conflict);
    assertThat(def.familyIds())
        .containsAnyElementsOf(catalog.sourceModifiers().get(alloy).familyIds());
    var frozen = new ModifierInstance(conflict, Map.of(mana, def.stats().getFirst().min()), true);
    var sameFamilyGoal =
        goal(
            catalog,
            new Group(
                "mana",
                Type.AND,
                false,
                null,
                List.of(row(catalog, mana, new Range(null, null), null))));
    assertThat(service.preview(context, sameFamilyGoal, workbench, null, alloy).cards())
        .isNotEmpty();
    assertThat(service.preview(context, sameFamilyGoal, workbench, frozen, alloy).cards())
        .isEmpty();
  }

  /** 레벨 24 장갑의 세 접두 티어에 대해 결합 굴림에서 얻은 14쌍과 나머지 112쌍을 모두 대조한다. */
  @Test
  void unfrozenGloveFireDamageMatchesEveryReachablePair() {
    var context = context("stocky", 24);
    var catalog = service.catalog(context);
    var pairs =
        Map.of(
            "heated", List.of(List.of(1, 3), List.of(2, 3)),
            "smouldering",
                List.of(
                    List.of(3, 6),
                    List.of(3, 7),
                    List.of(4, 7),
                    List.of(4, 8),
                    List.of(5, 8),
                    List.of(5, 9)),
            "smoking",
                List.of(
                    List.of(6, 10),
                    List.of(6, 11),
                    List.of(7, 11),
                    List.of(7, 12),
                    List.of(8, 12),
                    List.of(8, 13)));
    for (int min = 1; min <= 9; min++)
      for (int max = 1; max <= 14; max++) {
        var pair = List.of(min, max);
        var expected =
            pairs.entrySet().stream()
                .filter(e -> e.getValue().contains(pair))
                .map(e -> "stocky-mitts:prefix:" + e.getKey())
                .toList();
        var goal =
            goal(
                catalog,
                new Group(
                    "damage",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        row(
                            catalog,
                            "attack_minimum_added_fire_damage",
                            exact(BigDecimal.valueOf(min)),
                            null),
                        row(
                            catalog,
                            "attack_maximum_added_fire_damage",
                            exact(BigDecimal.valueOf(max)),
                            null))));
        var variants =
            service.preview(context, goal, workbench).cards().stream()
                .flatMap(c -> c.tierVariants().stream())
                .toList();
        assertThat(variants)
            .as("damage %s", pair)
            .allSatisfy(
                v -> {
                  assertThat(v.explicits()).hasSize(1);
                  assertThat(service.evaluate(v, goal).status()).isEqualTo(Status.MATCH);
                });
        assertThat(variants)
            .as("tiers for %s", pair)
            .extracting(v -> v.explicits().getFirst().modifierId())
            .containsExactlyInAnyOrderElementsOf(expected);
      }
  }
}
