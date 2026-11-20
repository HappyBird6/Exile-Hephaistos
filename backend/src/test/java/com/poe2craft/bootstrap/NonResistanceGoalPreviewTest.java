package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
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
