package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner;
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** 실제 태양의 목걸이 카탈로그와 테스트 탭 서비스에서 희귀 목표 셋을 달성하는 제작 기록을 검증한다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SolarRareTestRoutesTest {
  /** 정신력과 근접 스킬 목표는 각각 Countess, of Battle 티어만 허용한다. */
  private static final String COUNTESS = "amulet:prefix:countess";

  private static final String BATTLE = "amulet:suffix:of-battle";
  private static final String GLOBAL = "amulet:prefix:essence-global-defences";

  /** 서비스, 고정 정신력 +15 시작 상태, 제작 세션에 입력하는 세 행의 목표다. */
  private final TestRouteService service;

  private final ItemCatalog catalog;
  private final ItemState solar;
  private final GoalFilter goal;

  /** 운영 카탈로그와 탐색 예산으로 서비스를 구성하고 명시 정신력·근접 스킬·전체 방어 행을 묶는다. */
  SolarRareTestRoutesTest() {
    var init = new CatalogConfiguration();
    catalog = init.itemCatalog(init.workbenchDefinitions());
    var operations =
        init.workbenchService(
            catalog, init.workbenchSimulator(catalog, init.craftingEngine(catalog)));
    var goals = new CraftingSupportConfiguration().goalFilterService(operations);
    service = new TestRouteService(operations, goals, BackendSettings.defaults().testRoutes());
    solar = SolarAmulet.initial(catalog, 82, 15);
    var context = new GoalFilter.Context(solar.snapshotId(), solar.baseItemId(), solar.itemLevel());
    var entries =
        List.of(
            entry(SolarAmulet.SPIRIT_STAT, "flat", 47),
            entry("global_armour_evasion_energy_shield_+%", "percent", null),
            entry("melee_skill_gem_level_+", "flat", 3));
    goal =
        new GoalFilter(
            1,
            goals.catalog(context).catalogVersion(),
            new GoalFilter.General(
                solar.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(new GoalFilter.Group("three", GoalFilter.Type.AND, false, null, entries)));
  }

  /** 실제 표시용 명시 행에 선택적 최솟값을 적용한다. */
  private static GoalFilter.Entry entry(String statId, String unit, Integer min) {
    return new GoalFilter.Entry(
        statId,
        GoalCatalogIndex.id("explicit", statId),
        unit,
        new GoalFilter.Range(min == null ? null : BigDecimal.valueOf(min), null),
        null,
        false);
  }

  /** 명시 옵션만 바꾼 희귀 시작 상태를 만든다. */
  private ItemState start(ModifierInstance... mods) {
    return new ItemState(
        solar.snapshotId(),
        solar.baseItemId(),
        solar.itemLevel(),
        ItemState.Rarity.RARE,
        solar.implicits(),
        List.of(mods),
        Set.of());
  }

  /** 목걸이의 별세계 각 목표를 독립적으로 선택하고, 일반 뼈와 공유 표시 키로 오인하지 않는지 검증한다. */
  @Test
  void eachOtherworldlyGoalUsesOnlyAlteredCollarboneAndItsOwnReveal() {
    var effects =
        com.poe2craft.shared.item.ManualRevealCatalog.enrichOtherworldly(catalog)
            .modifiers()
            .values()
            .stream()
            .filter(d -> d.tags().contains("otherworldly"))
            .toList();
    assertThat(effects).hasSize(4);
    for (var effect : effects) {
      var range = effect.stats().getFirst();
      boolean presence = range.id().equals("display_reveal_present");
      var row =
          entry(
              effect.id(),
              presence ? "presence" : "roll",
              presence ? null : Math.toIntExact(range.min()));
      var request =
          new GoalFilter(
              1,
              goal.catalogVersion(),
              goal.general(),
              List.of(
                  new GoalFilter.Group(
                      "otherworldly", GoalFilter.Type.AND, false, null, List.of(row))));
      var view = service.calculate(start(), request);
      assertThat(view.routes())
          .as(effect.id())
          .isNotEmpty()
          .allSatisfy(
              route -> {
                assertThat(route.steps())
                    .extracting(RecipeRoutePlanner.Step::action)
                    .containsExactly("Altered_Collarbone", "REVEAL");
                assertThat(
                        view.states()
                            .get(route.stateIndices().getLast())
                            .desecrated()
                            .getFirst()
                            .modifierId())
                    .isEqualTo(effect.id());
                assertThat(route.steps().getLast().chance().probability()).isEqualTo(1.0);
              });
      if (!presence && range.max() > range.min()) {
        var high = entry(effect.id(), "roll", Math.toIntExact(range.max()));
        var bounded =
            new GoalFilter(
                1,
                goal.catalogVersion(),
                goal.general(),
                List.of(
                    new GoalFilter.Group(
                        "bounded", GoalFilter.Type.AND, false, null, List.of(high))));
        assertThat(service.calculate(start(), bounded).routes())
            .isNotEmpty()
            .allSatisfy(
                route ->
                    assertThat(route.steps())
                        .extracting(RecipeRoutePlanner.Step::action)
                        .containsExactly("Altered_Collarbone", "REVEAL", "DIVINE"));
      }
    }
  }

  /** 일반 시작 5목표에서 보호용 접미는 임의의 한 칸으로 합치고, 실제 목표 적중과 재계산을 구분한다. */
  @Test
  void sacrificialSuffixIsCertainAndIsConsumedByTheNextEssence() {
    var rows = new ArrayList<>(goal.groups().getFirst().entries());
    rows.set(2, entry("projectile_skill_gem_level_+", "flat", 3));
    rows.add(entry("base_cold_damage_resistance_%", "percent", null));
    rows.add(entry("physical_damage_reduction_rating_+%", "percent", null));
    var five =
        new GoalFilter(
            1,
            goal.catalogVersion(),
            goal.general(),
            List.of(new GoalFilter.Group("five", GoalFilter.Type.AND, false, null, rows)));
    var view = service.calculate(solar, five);
    boolean seen = false;
    for (var route : view.routes()) {
      for (int i = 0; i + 1 < route.steps().size(); i++) {
        var add = route.steps().get(i);
        var use = route.steps().get(i + 1);
        if (!add.action().equals("EXALTED")
            || add.omens().isEmpty()
            || !use.action().equals("PERFECT_ESSENCE_ENHANCEMENT")
            || use.omens().isEmpty()) continue;
        assertThat(java.util.Collections.disjoint(add.acquired(), use.lost())).isTrue();
        if (!add.dummy()) continue;
        assertThat(add.chance().probability()).isEqualTo(1);
        assertThat(add.acquired()).isEmpty();
        var before = i == 0 ? solar : view.states().get(route.stateIndices().get(i - 1));
        var after = view.states().get(route.stateIndices().get(i));
        var slot =
            after.explicits().stream()
                .filter(m -> !before.explicits().contains(m))
                .findFirst()
                .orElseThrow();
        assertThat(view.states().get(route.stateIndices().get(i + 1)).explicits())
            .doesNotContain(slot);
        seen = true;
      }
    }
    assertThat(seen).isTrue();
  }

  /** 명시 정신력 +47을 계속 보존한다. 여유 공간에서 준비한 뼈는 마지막 목표까지 미공개로 둘 수 있다. */
  @Test
  void keepsUnfracturedSpiritAndRevealsMeleeThree() {
    var spirit = new ModifierInstance(COUNTESS, Map.of(SolarAmulet.SPIRIT_STAT, 47L));
    var item = start(spirit);
    var view = service.calculate(item, goal);
    assertThat(view.routes()).isNotEmpty();
    assertThat(view.diagnostics().stage()).isEqualTo("ROUTES_FOUND");
    var trace = view.diagnostics().search();
    assertThat(trace.queuedPaths).isPositive();
    assertThat(trace.actions.get("REVEAL").accepted).isPositive();
    assertThat(view.routes().stream().flatMap(r -> r.steps().stream()))
        .noneMatch(s -> s.action().equals("PERFECT_EXALTED") || isRepeatedChaos(s));
    assertThat(view.routes())
        .anySatisfy(
            r -> {
              assertFinalReveal(item, view, r);
              int bone = boneIndex(view, r);
              assertThat(bone).isZero();
              assertThat(r.steps().subList(bone + 1, r.steps().size()))
                  .anyMatch(step -> step.action().equals("PERFECT_ESSENCE_ENHANCEMENT"));
              assertThat(r.stateIndices())
                  .allSatisfy(
                      index -> assertThat(view.states().get(index).explicits()).contains(spirit));
            });
  }

  /** 일반 베이스에서 진화 적중과 미적중 후 소멸·카오스를 모두 운영 예산으로 완주한다. */
  @Test
  void normalStartFinishesViaBothTheMissedUpgradeChaosRouteAndTheLuckySpiritHit() {
    var entries =
        List.of(
            entry(SolarAmulet.SPIRIT_STAT, "flat", 47),
            entry("global_armour_evasion_energy_shield_+%", "percent", null),
            entry("base_cold_damage_resistance_%", "percent", null));
    var cold =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(new GoalFilter.Group("cold", GoalFilter.Type.AND, false, null, entries)));
    var view = service.calculate(solar, cold);
    var normal = view.routes().stream().filter(r -> !r.lucky()).toList();
    assertThat(normal)
        .anySatisfy(
            r -> {
              var steps = r.steps();
              assertThat(steps.subList(0, 4))
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("TRANSMUTATION", "REGAL", "ANNULMENT", "CHAOS");
              assertThat(steps.subList(0, 3))
                  .allMatch(s -> s.acquired().isEmpty() && s.omens().isEmpty());
              assertThat(steps.get(3).repeatUntilHit()).isTrue();
              assertThat(steps.get(3).chance().probability()).isEqualTo(1d);
              var afterRegal = view.states().get(r.stateIndices().get(1));
              var afterAnnul = view.states().get(r.stateIndices().get(2));
              var afterChaos = view.states().get(r.stateIndices().get(3));
              assertThat(afterRegal.explicits()).hasSize(2).containsAll(afterAnnul.explicits());
              assertThat(afterAnnul.explicits()).hasSize(1);
              assertThat(afterChaos.explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactly(COUNTESS);
              var finalMods = view.states().get(r.stateIndices().getLast()).explicits();
              assertThat(finalMods)
                  .anyMatch(m -> m.modifierId().equals(COUNTESS))
                  .anyMatch(m -> m.modifierId().equals(GLOBAL))
                  .anyMatch(m -> m.values().containsKey("base_cold_damage_resistance_%"));
            });
    assertThat(view.routes().stream().filter(RecipeRoutePlanner.Route::lucky))
        .anyMatch(
            r ->
                view.states().get(r.stateIndices().getFirst()).explicits().stream()
                    .anyMatch(m -> m.modifierId().equals(COUNTESS)));
    var selected = normal.getFirst();
    var replay = service.calculate(solar, cold, selected.cardIndex(), selected.steps());
    assertThat(replay.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.lucky()).isFalse();
              assertThat(r.steps()).isEqualTo(selected.steps());
              assertThat(r.probability()).isEqualTo(selected.probability());
            });
  }

  /** 희귀 빈 아이템에서도 첫 희소 옵션을 먼저 확보하고 다른 희소 옵션을 훼손으로 마무리할 수 있다. */
  @Test
  void emptyRareReachesThreeGoalsThroughDesecration() {
    var item = start();
    // 화면에서 꺼 둔 방어도와 냉기 저항 행도 실제 HTTP 입력에는 남아 있다.
    var entries = new ArrayList<>(goal.groups().getFirst().entries());
    int position = 2;
    for (String stat :
        List.of("physical_damage_reduction_rating_+%", "base_cold_damage_resistance_%")) {
      var row = entry(stat, "percent", null);
      entries.add(
          position++,
          new GoalFilter.Entry(row.id(), row.statId(), row.unit(), row.range(), null, true));
    }
    var filter =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(new GoalFilter.Group("three", GoalFilter.Type.AND, false, null, entries)));
    var view = service.calculate(item, filter);
    assertThat(view.cards())
        .singleElement()
        .satisfies(
            card ->
                assertThat(card.options())
                    .extracting(TestRouteService.Option::modifierIds)
                    .containsExactlyInAnyOrder(
                        List.of(COUNTESS), List.of(GLOBAL), List.of(BATTLE)));
    for (var route : view.routes()) {
      assertNoFillerBeforeLastBone(view, route);
      var first =
          route.steps().stream()
              .filter(step -> !step.acquired().isEmpty())
              .findFirst()
              .orElseThrow();
      assertThat(first.acquired())
          .allSatisfy(
              number ->
                  assertThat(view.cards().get(route.cardIndex()).options())
                      .anyMatch(
                          option ->
                              option.number() == number
                                  && option.modifierIds().stream()
                                      .anyMatch(id -> id.equals(COUNTESS) || id.equals(BATTLE))));
    }
    for (var firstTier : List.of(COUNTESS, BATTLE)) {
      var repeated =
          view.routes().stream()
              .filter(route -> !route.lucky())
              .filter(
                  route -> {
                    var card = view.cards().get(route.cardIndex());
                    int target =
                        card.options().stream()
                            .filter(o -> o.modifierIds().contains(firstTier))
                            .findFirst()
                            .orElseThrow()
                            .number();
                    return route.steps().stream()
                        .filter(s -> !s.acquired().isEmpty())
                        .findFirst()
                        .filter(SolarRareTestRoutesTest::isRepeatedChaos)
                        .filter(
                            s ->
                                Integer.valueOf(target).equals(s.repeatTarget())
                                    && s.acquired().contains(target))
                        .isPresent();
                  })
              .toList();
      assertThat(repeated)
          .as("first repeated Chaos target %s", firstTier)
          .anySatisfy(
              route -> {
                assertFinalReveal(item, view, route);
                int bone = boneIndex(view, route);
                assertThat(route.steps().get(bone - 1).action())
                    .isEqualTo("PERFECT_ESSENCE_ENHANCEMENT");
                var end = view.states().get(route.stateIndices().getLast());
                assertThat(end.desecrated().getFirst().modifierId())
                    .isEqualTo(firstTier.equals(COUNTESS) ? BATTLE : COUNTESS);
              });
    }
  }

  /** 마지막 공개 직전에 훼손 방향을 정하려고 더미 엑잘을 붙이지 않는다. */
  private void assertNoFillerBeforeLastBone(
      TestRouteService.View view, RecipeRoutePlanner.Route route) {
    int bone = boneIndex(view, route);
    if (bone < 2 || !route.steps().getLast().action().equals("REVEAL")) return;
    var step = route.steps().get(bone - 1);
    if (!step.action().equals("EXALTED") || !step.acquired().isEmpty() || !step.lost().isEmpty())
      return;
    var revealed =
        view.states().get(route.stateIndices().getLast()).desecrated().getFirst().modifierId();
    var before = view.states().get(route.stateIndices().get(bone - 2));
    assertThat(
            before.explicits().stream()
                .map(ModifierInstance::modifierId)
                .toList()
                .containsAll(List.of(GLOBAL, revealed.equals(BATTLE) ? COUNTESS : BATTLE)))
        .as("두 목표를 확보했으면 곧바로 뼈로 세 번째를 준비한다")
        .isFalse();
  }

  /** 빈 희귀와 정신력 분열 희귀에서 수치 제한 없는 명시 방어도%까지 실제로 보유한 네 목표 제작 기록을 반환한다. */
  @Test
  void emptyOrFracturedRareCanFinishGlobalDefencesSpiritMeleeAndArmour() {
    String armour = "physical_damage_reduction_rating_+%";
    var entries = new ArrayList<>(goal.groups().getFirst().entries());
    entries.add(entry(armour, "percent", null));
    var four =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(new GoalFilter.Group("four", GoalFilter.Type.AND, false, null, entries)));
    var fracture = new ModifierInstance(COUNTESS, Map.of(SolarAmulet.SPIRIT_STAT, 47L), true);
    for (var item : List.of(start(), start(fracture))) {
      var view = service.calculate(item, four);
      if (item.explicits().contains(fracture))
        assertThat(view.routes()).extracting(RecipeRoutePlanner.Route::lucky).contains(true, false);
      assertThat(view.routes())
          .as("start %s", item.explicits())
          .anySatisfy(
              route -> {
                assertFinalReveal(item, view, route);
                var end = view.states().get(route.stateIndices().getLast());
                assertThat(end.explicits()).anyMatch(m -> m.values().containsKey(armour));
                if (!item.explicits().isEmpty())
                  assertThat(route.stateIndices())
                      .allSatisfy(
                          index ->
                              assertThat(view.states().get(index).explicits()).contains(fracture));
              });
    }
  }

  /** 분열 정신력은 교체 불가이므로 일반 접미 하나를 그대로 첫 카오스 반복에 쓸 수 있다. */
  @Test
  void firstChaosCanReplaceOneMutableAffixBesideFracturedSpirit() {
    var fracture = new ModifierInstance(COUNTESS, Map.of(SolarAmulet.SPIRIT_STAT, 47L), true);
    var dummy =
        new ModifierInstance("amulet:suffix:of-the-brute", Map.of("additional_strength", 5L));
    var view = service.calculate(start(fracture, dummy), goal);
    assertThat(view.routes())
        .anySatisfy(
            route -> {
              var first = route.steps().getFirst();
              assertThat(isRepeatedChaos(first)).isTrue();
              assertThat(first.maxUses()).isNull();
              assertThat(first.chance().probability()).isEqualTo(1d);
              assertThat(first.expectedUses()).isGreaterThan(1d);
              var hit = view.states().get(route.stateIndices().getFirst());
              assertThat(hit.explicits())
                  .contains(fracture)
                  .anyMatch(m -> m.modifierId().equals(BATTLE));
              assertThat(route.stateIndices())
                  .allSatisfy(
                      index -> assertThat(view.states().get(index).explicits()).contains(fracture));
              assertFinalGoals(view, route);
            });
  }

  /** 등급 I 무징조 적중까지 반복과 절사 징조의 단회를 구분한다. */
  private static boolean isRepeatedChaos(RecipeRoutePlanner.Step step) {
    return step.action().equals("CHAOS") && step.omens().isEmpty() && step.repeatUntilHit();
  }

  /** 동일 경로에서 실제 미공개 슬롯을 처음 만든 뼈 단계를 식별한다. */
  private static int boneIndex(TestRouteService.View view, RecipeRoutePlanner.Route route) {
    for (int i = 0; i < route.stateIndices().size(); i++) {
      var observed = view.states().get(route.stateIndices().get(i)).desecrated();
      if (observed.stream().anyMatch(s -> s.modifierId() == null)) return i;
    }
    return -1;
  }

  /** 뼈 사용과 마지막 목표 공개의 두 실상태를 확인한다. 그 사이 수행한 다른 제작에서도 미공개 슬롯이 유지된다. */
  private static void assertFinalReveal(
      ItemState start, TestRouteService.View view, RecipeRoutePlanner.Route route) {
    var actions = route.steps().stream().map(RecipeRoutePlanner.Step::action).toList();
    int reveal = actions.indexOf("REVEAL");
    int bone = boneIndex(view, route);
    assertThat(bone).isGreaterThanOrEqualTo(0).isLessThan(reveal);
    var beforeBone = bone == 0 ? start : view.states().get(route.stateIndices().get(bone - 1));
    var afterBone = view.states().get(route.stateIndices().get(bone));
    var afterReveal = view.states().get(route.stateIndices().get(reveal));
    assertThat(beforeBone.desecrated()).isEmpty();
    var hidden = afterBone.desecrated().getFirst();
    assertThat(hidden.modifierId()).isNull();
    for (int i = bone; i < reveal; i++)
      assertThat(view.states().get(route.stateIndices().get(i)).desecrated())
          .containsExactly(hidden);
    var revealed = afterReveal.desecrated().getFirst();
    assertThat(revealed.id()).isEqualTo(hidden.id());
    assertThat(route.steps().get(reveal).acquired())
        .singleElement()
        .satisfies(
            number ->
                assertThat(view.cards().get(route.cardIndex()).options())
                    .anyMatch(
                        option ->
                            option.number() == number
                                && option.modifierIds().contains(revealed.modifierId())));
    assertThat(actions.subList(reveal + 1, actions.size())).allMatch(a -> a.equals("DIVINE"));
    assertThat(route.steps().get(reveal).chance().probability()).isEqualTo(1d);
    assertFinalGoals(view, route);
  }

  /** 최종 환산 수치에서 베이스 고정 정신력을 제외하고, 획득한 방어 옵션은 실제 제작 플래그도 확인한다. */
  private static void assertFinalGoals(TestRouteService.View view, RecipeRoutePlanner.Route route) {
    var end = view.states().get(route.stateIndices().getLast());
    assertThat(end.explicits())
        .anyMatch(
            m -> m.modifierId().equals(COUNTESS) && m.values().get(SolarAmulet.SPIRIT_STAT) >= 47)
        .anyMatch(
            m -> m.modifierId().equals(BATTLE) && m.values().get("melee_skill_gem_level_+") == 3)
        .anyMatch(m -> m.modifierId().equals(GLOBAL) && m.crafted());
  }
}
