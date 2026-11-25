package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** 시작의 희소 목표를 우선하고, 완벽 엑잘의 실제 적중·하위 등급 재생과 일반 목표를 구분한다. */
class RecipeLowAcquisitionPolicyTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final RecipeMoves moves = new RecipeMoves(fixture.workbench);
  private final String low = "amulet:prefix:virile";
  private final RouteCards.Source rare = new RouteCards.Source("B", null, RouteCards.Frequency.L);
  private final RouteCards.Source common = new RouteCards.Source("B", null, RouteCards.Frequency.H);

  /** 첫 L의 허용 티어는 실제로 얻은 경우에만 적중하며, 같은 카드의 H 목표가 먼저 일반 엑잘을 열지 않는다. */
  @Test
  void missingLowHasItsOwnPerfectExaltOnlyForTheActualEligibleTier() {
    var start = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:adept-s");
    var lowOption = new RouteCards.Option(1, Map.of(low, List.of(rare)));
    var highOption = new RouteCards.Option(2, Map.of(fixture.strength, List.of(common)));
    var c = new RouteCards.Card(List.of(lowOption, highOption), 0);
    var choices = moves.choices(start, c, Map.of());
    assertThat(choices)
        .extracting(RecipeMoves.Choice::purpose)
        .containsExactlyInAnyOrder(RecipeMoves.Purpose.LOW_CHAOS, RecipeMoves.Purpose.LOW_EXALT);
    var perfect =
        choices.stream()
            .filter(m -> m.purpose() == RecipeMoves.Purpose.LOW_EXALT)
            .findFirst()
            .orElseThrow();
    assertThat(perfect.move().action()).isEqualTo("PERFECT_EXALTED");
    assertThat(perfect.eligibleTiers()).containsExactly(low);
    assertThat(
            RecipeMoves.permits(
                perfect, Map.of(), Map.of(2, fixture.strength), Set.of(), Set.of(low)))
        .isFalse();
    assertThat(RecipeMoves.permits(perfect, Map.of(), Map.of(1, low), Set.of(), Set.of(low)))
        .isTrue();
    assertThat(
            fixture
                .law
                .successors(start, "PERFECT_EXALTED", Set.of(), Set.of(low), 64, false)
                .outcomes())
        .isNotEmpty();
    assertThat(
            fixture
                .law
                .successors(start, "PERFECT_EXALTED", Set.of(), Set.of(fixture.life), 64, false)
                .outcomes())
        .isEmpty();
  }

  /** 허용 범위가 L과 H 양쪽에 걸쳐도 기본 엑잘로 우연히 L을 얻은 경우만 별도 예외로 제한한다. */
  @Test
  void mixedTierGoalsAllowTheOrdinaryTierAndSeparateThePerfectGamble() {
    var option =
        new RouteCards.Option(1, Map.of(low, List.of(rare), fixture.life, List.of(common)));
    var start = fixture.item(ItemState.Rarity.RARE);
    var choices = moves.choices(start, new RouteCards.Card(List.of(option), 0), Map.of());
    var basic =
        choices.stream()
            .filter(c -> c.move().action().equals("EXALTED") && c.move().omens().isEmpty())
            .findFirst()
            .orElseThrow();
    assertThat(RecipeMoves.permits(basic, Map.of(), Map.of(1, low), Set.of(), Set.of(low)))
        .isFalse();
    assertThat(RecipeMoves.permits(basic, Map.of(), Map.of(1, fixture.life), Set.of(), Set.of(low)))
        .isTrue();
    assertThat(choices.stream().filter(c -> c.purpose() == RecipeMoves.Purpose.LOW_EXALT))
        .singleElement()
        .satisfies(c -> assertThat(c.eligibleTiers()).containsExactly(low));
    var ordinaryOnly =
        moves.choices(
            start,
            new RouteCards.Card(
                List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(common)))), 0),
            Map.of());
    assertThat(ordinaryOnly).noneMatch(c -> c.move().action().equals("PERFECT_EXALTED"));
    assertThat(
            RecipeMoves.permits(
                basic.asLowPreparation(), Map.of(), Map.of(1, low), Set.of(), Set.of(low)))
        .isTrue();
  }

  /** 훼손 가능한 L이어도 빈 시작부터 다른 다섯 목표를 먼저 채우지 않는다. 이미 보유한 L은 추가 L 연기를 허용한다. */
  @Test
  void reservesTheLastDesecrationSlotOnlyAfterALowIsHeldOrFiveAreReady() {
    var rareWithReveal = List.of(rare, new RouteCards.Source("D", "Preserved_Collarbone", null));
    var options =
        List.of(
            new RouteCards.Option(1, Map.of(low, rareWithReveal)),
            new RouteCards.Option(2, Map.of(fixture.strength, List.of(rare))),
            new RouteCards.Option(3, Map.of("amulet:prefix:adept-s", List.of(common))),
            new RouteCards.Option(4, Map.of("amulet:suffix:of-the-seal", List.of(common))),
            new RouteCards.Option(5, Map.of("amulet:prefix:azure", List.of(common))),
            new RouteCards.Option(6, Map.of("amulet:suffix:of-the-whelpling", List.of(common))));
    var card = new RouteCards.Card(options, 0);
    var empty = fixture.item(ItemState.Rarity.RARE);
    var initial = moves.choices(empty, card, Map.of());
    assertThat(initial).isNotEmpty().noneMatch(c -> c.target() > 2);
    var oneLow = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    assertThat(moves.choices(oneLow, card, Map.of(2, fixture.strength)))
        .anyMatch(c -> c.target() == 3 && c.move().action().equals("EXALTED"));
    var lastOnly = new ArrayList<>(options);
    lastOnly.set(1, new RouteCards.Option(2, Map.of(fixture.strength, List.of(common))));
    assertThat(moves.choices(empty, new RouteCards.Card(lastOnly, 0), Map.of()))
        .isNotEmpty()
        .anyMatch(c -> c.target() == 1 && c.purpose() == RecipeMoves.Purpose.BONE)
        .noneMatch(c -> c.purpose() == RecipeMoves.Purpose.LOW_EXALT);
  }

  /** 정신력 L이 있는 태양 목걸이에서 다른 L인 +3 근접은 곧바로 공개 준비하거나 반대쪽 한 칸만 닫아 확정한다. */
  @Test
  void onlyOppositeSidePreparationCanImproveTheSecondLowReveal() {
    String spirit = "amulet:prefix:countess", melee = "amulet:suffix:of-battle";
    var initial = fixture.item(ItemState.Rarity.RARE, spirit, "amulet:prefix:adept-s");
    var sources = new ArrayList<>(List.of(rare));
    var actual =
        DesecrationCurrency.sources(
            initial.baseItemId(),
            initial.itemLevel(),
            fixture.catalog.find(melee).orElseThrow(),
            true);
    assertThat(actual)
        .contains("Preserved_Collarbone", "Ancient_Collarbone")
        .doesNotContain("Gnawed_Collarbone");
    for (var bone : actual) sources.add(new RouteCards.Source("D", bone, null));
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(spirit, List.of(rare))),
                new RouteCards.Option(2, Map.of(melee, sources))),
            0);
    var choices = moves.choices(initial, card, Map.of(1, spirit));
    assertThat(choices).anyMatch(c -> c.purpose() == RecipeMoves.Purpose.BONE);
    var directional =
        choices.stream()
            .filter(c -> c.purpose() == RecipeMoves.Purpose.LOW_SLOT_PREPARATION)
            .toList();
    assertThat(directional)
        .allSatisfy(
            c -> assertThat(c.move().omens()).containsExactly(WorkbenchOmen.SINISTRAL_EXALTATION));
    assertThat(directional)
        .extracting(c -> c.followup().action())
        .containsExactlyInAnyOrderElementsOf(actual);
    var fill =
        new RecipeRoutePlanner.Step(
            "EXALTED",
            List.of(WorkbenchOmen.SINISTRAL_EXALTATION.id()),
            List.of(),
            List.of(),
            null);
    var bone =
        new RecipeRoutePlanner.Step("Preserved_Collarbone", List.of(), List.of(), List.of(), null);
    var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(2), List.of(), null);
    for (var steps : List.of(List.of(bone, reveal), List.of(fill, bone, reveal))) {
      var result =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              initial,
              card,
              s -> s.explicits().stream().anyMatch(m -> m.modifierId().equals(melee)),
              50000,
              true,
              null,
              steps);
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              r -> {
                int index = steps.size() - 2;
                assertThat(r.steps().get(index).chance().probability())
                    .isEqualTo(index == 0 ? 0.5 : 1.0);
                assertThat(r.steps().getLast().chance().probability()).isEqualTo(1.0);
                var hidden =
                    result.states().get(r.stateIndices().get(index)).desecrated().getFirst();
                assertThat(hidden.modifierId()).isNull();
                assertThat(hidden.side()).isEqualTo(AffixType.SUFFIX);
                assertThat(
                        result
                            .states()
                            .get(r.stateIndices().getLast())
                            .desecrated()
                            .getFirst()
                            .modifierId())
                    .isEqualTo(melee);
                if (index > 0) {
                  var closed = result.states().get(r.stateIndices().getFirst());
                  assertThat(moves.choices(closed, card, Map.of(1, spirit)))
                      .noneMatch(c -> c.purpose() == RecipeMoves.Purpose.LOW_SLOT_PREPARATION);
                }
              });
    }
  }

  /** 철제 반지의 실제 가중치에서 생명력은 H, 카오스 저항은 M이다. 어려운 접미에 훼손을 남기고 생명력을 먼저 얻는다. */
  @Test
  void reservesTheIronRingRevealForTheHarderRemainingExaltTarget() {
    var catalog = ItemCatalogLoader.loadRing();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var moves = new RecipeMoves(simulator);
    var implicit = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
    var bare =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(minimum(implicit)),
            List.of(),
            Set.of());
    var first = family(catalog, "IncreasedLife", 1, RouteCards.Frequency.H);
    var second = family(catalog, "ChaosResistance", 2, RouteCards.Frequency.M);
    var card = new RouteCards.Card(List.of(first, second), 0);
    var candidates = moves.choices(bare, card, Map.of());
    assertThat(candidates.stream().filter(c -> c.purpose() == RecipeMoves.Purpose.BONE))
        .isNotEmpty()
        .allMatch(c -> c.target() == 2);
    assertThat(candidates)
        .anyMatch(c -> c.target() == 1 && c.move().action().equals("EXALTED"))
        .noneMatch(c -> c.target() == 2 && c.move().action().equals("EXALTED"));
    // 둘 다 M인 경우에도 같은 풀에서 합산 가중치 1,500의 카오스 저항이 8,000의 냉기 저항보다 어렵다.
    var cold = family(catalog, "ColdResistance", 1, RouteCards.Frequency.M);
    var sameFrequency =
        moves.choices(bare, new RouteCards.Card(List.of(cold, second), 0), Map.of());
    assertThat(sameFrequency.stream().filter(c -> c.purpose() == RecipeMoves.Purpose.BONE))
        .isNotEmpty()
        .allMatch(c -> c.target() == 2);
    var single = moves.choices(bare, new RouteCards.Card(List.of(first), 0), Map.of());
    assertThat(single)
        .anyMatch(c -> c.purpose() == RecipeMoves.Purpose.BONE)
        .noneMatch(c -> !c.preparation() && c.move().action().equals("EXALTED"));
    var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(2), List.of(), null);
    var bone =
        new RecipeRoutePlanner.Step("Preserved_Collarbone", List.of(), List.of(), List.of(), null);
    var add = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            simulator,
            bare,
            card,
            state ->
                state.explicits().stream().anyMatch(m -> first.tiers().containsKey(m.modifierId()))
                    && state.explicits().stream()
                        .anyMatch(m -> second.tiers().containsKey(m.modifierId())),
            20000,
            true,
            null,
            List.of(bone, add, reveal));
    assertThat(fixed.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().getFirst().chance().probability()).isEqualTo(0.5);
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1.0);
              assertThat(route.probability())
                  .isEqualTo(0.5 * route.steps().get(1).chance().probability());
              var pending = fixed.states().get(route.stateIndices().get(1)).desecrated().getFirst();
              assertThat(pending.modifierId()).isNull();
              assertThat(pending.side()).isEqualTo(AffixType.SUFFIX);
            });
  }

  /** 골라 둔 계열의 실제 베이스 티어마다 공통 B·실제 뼈 출처를 만들며 출처에 따른 누적 빈도도 확인한다. */
  private RouteCards.Option family(
      ItemCatalog catalog, String family, int number, RouteCards.Frequency expected) {
    var tiers = new LinkedHashMap<String, List<RouteCards.Source>>();
    var familyMods =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.familyIds().contains(family) && m.weight() > 0 && m.requiredItemLevel() <= 82)
            .toList();
    long all =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.affixType() == familyMods.getFirst().affixType()
                        && m.weight() > 0
                        && m.requiredItemLevel() <= 82)
            .mapToLong(m -> m.weight())
            .sum();
    double hit = (double) familyMods.stream().mapToLong(m -> m.weight()).sum() / all;
    if (expected == RouteCards.Frequency.H) assertThat(hit).isGreaterThanOrEqualTo(.1);
    else assertThat(hit).isGreaterThanOrEqualTo(.01).isLessThan(.1);
    for (var tier : familyMods) {
      assertThat(DesecrationCurrency.sources(catalog.base().id(), 82, tier, true))
          .contains("Preserved_Collarbone");
      tiers.put(
          tier.id(),
          List.of(
              new RouteCards.Source("B", null, expected),
              new RouteCards.Source("D", "Preserved_Collarbone", null)));
    }
    return new RouteCards.Option(number, tiers);
  }

  /** 화염 X가 막은 상위 화염은 절사가 X를 없애면서 획득한다. 이후 뼈가 남은 냉기를 공개한다. */
  @Test
  void whittlingUpgradesBlockedFireAndLeavesTheIronRingRevealForCold() {
    var catalog = ItemCatalogLoader.loadRing();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    String blocker = "iron-ring:suffix:of-the-whelpling";
    var start =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow())),
            List.of(
                minimum(catalog.find(blocker).orElseThrow()),
                minimum(catalog.find("iron-ring:prefix:rotund").orElseThrow())),
            Set.of());
    var sources =
        List.of(
            new RouteCards.Source("B", null, RouteCards.Frequency.M),
            new RouteCards.Source("D", "Preserved_Collarbone", null));
    var fire =
        new RouteCards.Option(
            1,
            Map.of("iron-ring:suffix:of-magma", sources, "iron-ring:suffix:of-tzteosh", sources));
    var cold =
        new RouteCards.Option(
            2,
            Map.of("iron-ring:suffix:of-haast", sources, "iron-ring:suffix:of-the-ice", sources));
    var card = new RouteCards.Card(List.of(fire, cold), 0);
    var omens = Set.of(WorkbenchOmen.WHITTLING);
    var law = simulator.recipePlan(start, WorkbenchCurrency.CHAOS, omens);
    assertThat(law.branches())
        .singleElement()
        .satisfies(
            b -> {
              assertThat(b.remaining().explicits()).noneMatch(m -> m.modifierId().equals(blocker));
              assertThat(
                      b.candidates().stream()
                          .filter(m -> fire.tiers().containsKey(m.id()))
                          .mapToLong(m -> m.weight())
                          .sum())
                  .isEqualTo(2000);
            });
    var chaos =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
    var bone =
        new RecipeRoutePlanner.Step("Preserved_Collarbone", List.of(), List.of(), List.of(), null);
    var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(2), List.of(), null);
    var expected = List.of("CHAOS", "Preserved_Collarbone", "REVEAL");
    Predicate<ItemState> both =
        item ->
            item.explicits().stream().anyMatch(m -> fire.tiers().containsKey(m.modifierId()))
                && item.explicits().stream()
                    .anyMatch(m -> cold.tiers().containsKey(m.modifierId()));
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, card, both, 20000, true, null, List.of(chaos, bone, reveal));
    var search =
        RecipeRoutePlanner.calculate(simulator, start, List.of(card), both, 80000, true, null);
    for (var result : List.of(fixed, search)) {
      assertThat(result.routes())
          .anySatisfy(
              route -> {
                assertThat(route.steps())
                    .extracting(RecipeRoutePlanner.Step::action)
                    .containsExactlyElementsOf(expected);
                assertThat(route.steps().getFirst().omens())
                    .containsExactly(WorkbenchOmen.WHITTLING.id());
                assertThat(route.steps().getFirst().acquired()).containsExactly(1);
                assertThat(route.steps().getLast().acquired()).containsExactly(2);
                assertThat(route.steps().getLast().chance().probability()).isEqualTo(1);
                var changed = result.states().get(route.stateIndices().getFirst()).explicits();
                assertThat(changed)
                    .noneMatch(m -> m.modifierId().equals(blocker))
                    .anyMatch(m -> fire.tiers().containsKey(m.modifierId()));
              });
    }
  }

  /** 실제 카탈로그의 최솟값으로 테스트 시작 옵션을 만든다. */
  private ModifierInstance minimum(ModifierDefinition tier) {
    var values = new HashMap<String, Long>();
    tier.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(tier.id(), values);
  }

  /** 여러 티어의 뼈 예약 결과는 슬롯별 실제 단일 티어 검사들의 합집합과 같아야 한다. */
  @Test
  void fullBoardReservationSharesBranchesWithoutLosingProtectedRemovalOrTierChecks() {
    var five = new RecipeLightTransitionTest().fiveModifiers();
    var mods = new ArrayList<>(five.explicits());
    mods.add(minimum(fixture.catalog.find(fixture.life).orElseThrow()));
    var full = fixture.workbench.finishBasicCurrency(five, mods);
    var targets = Set.of(low, fixture.strength, fixture.defences);
    var bone = "Preserved_Collarbone";
    for (var protectedIds : List.of(Set.<String>of(), Set.of(fixture.life)))
      for (boolean defer : List.of(false, true)) {
        var individual = new HashSet<String>();
        for (String target : targets) {
          var law =
              fixture.law.prepareDesecration(
                  full, bone, Set.of(), Set.of(target), Integer.MAX_VALUE, defer);
          if (law.outcomes().stream()
              .anyMatch(o -> Collections.disjoint(o.removedModifierIds(), protectedIds)))
            individual.add(target);
        }
        var batched =
            RecipeTransitions.reservableBoneTargets(
                full, bone, fixture.catalog, targets, protectedIds, defer);
        assertThat(batched)
            .isEqualTo(individual)
            .contains(fixture.strength)
            .doesNotContain(fixture.defences);
        if (!defer && protectedIds.contains(fixture.life)) assertThat(batched).doesNotContain(low);
        if (!defer && protectedIds.isEmpty()) assertThat(batched).contains(low);
      }
  }

  /** 작업대에서 기존 타락 슬롯은 공개할 수 있고 타락 후 새 뼈는 쓸 수 없다. 탐색과 고정 재생 모두 같은 자격을 쓴다. */
  @Test
  void corruptedItemsMayRevealAnExistingSlotButCannotOpenANewOne() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var bone = "Preserved_Collarbone";
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1, Map.of(fixture.life, List.of(new RouteCards.Source("D", bone, null))))),
            0);
    var corrupted = Set.of(ItemState.Condition.CORRUPTED);
    var pending =
        DesecrationCurrency.apply(start, bone, AffixType.PREFIX, null, Set.of(), fixture.catalog)
            .storedItem()
            .withConditions(corrupted);
    var actual = AbyssCraftingState.from(pending, fixture.catalog);
    var slot = actual.slots().getLast();
    assertThat(actual.eligibleRevealIds(slot.id(), fixture.catalog, Set.of(fixture.life)))
        .containsExactly(fixture.life);
    assertThat(moves.choices(start.withConditions(corrupted), card, Map.of())).isEmpty();
    var reach =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            pending,
            List.of(card),
            item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            100,
            true,
            null);
    var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(1), List.of(), null);
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            pending,
            card,
            item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            100,
            true,
            null,
            List.of(reveal));
    for (var result : List.of(reach, fixed)) {
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.probability()).isEqualTo(1);
                assertThat(r.steps())
                    .singleElement()
                    .satisfies(s -> assertThat(s.action()).isEqualTo("REVEAL"));
                var end = result.states().get(r.stateIndices().getFirst());
                assertThat(end.conditions()).isEqualTo(corrupted);
                assertThat(end.desecrated().getFirst().modifierId()).isEqualTo(fixture.life);
              });
    }
  }

  /** 같은 뼈의 두 목표를 별도 선택으로 유지하고, 기존 접두 공개 슬롯에는 실제 가능한 접두 티어만 둔다. */
  @Test
  void eachDesecrationChoiceKeepsItsTargetTiersAndExistingSlot() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var bone = new RouteCards.Source("D", "Preserved_Collarbone", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(bone), low, List.of(bone))),
                new RouteCards.Option(2, Map.of(fixture.strength, List.of(bone)))),
            0);
    var choices = moves.choices(start, card, Map.of());
    assertThat(choices)
        .hasSize(2)
        .allMatch(
            c -> c.purpose() == RecipeMoves.Purpose.BONE && c.move().action().equals(bone.id()));
    assertThat(choices)
        .anyMatch(c -> c.target() == 1 && c.eligibleTiers().equals(Set.of(fixture.life, low)))
        .anyMatch(c -> c.target() == 2 && c.eligibleTiers().equals(Set.of(fixture.strength)));
    var pending =
        DesecrationCurrency.apply(
            start, bone.id(), AffixType.PREFIX, null, Set.of(), fixture.catalog);
    assertThat(moves.choices(pending.storedItem(), card, Map.of())).isEmpty();
    var revealChoices =
        moves.choices(
            pending.storedItem(),
            new RouteCards.Card(List.of(card.options().getFirst()), 0),
            Map.of());
    assertThat(revealChoices)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.target()).isEqualTo(1);
              assertThat(c.move().action()).isEqualTo("REVEAL");
              assertThat(c.eligibleTiers()).containsExactlyInAnyOrder(fixture.life, low);
            });
    var selected =
        pending
            .selectDirect(
                pending.slots().getLast().id(),
                fixture.item(ItemState.Rarity.RARE, fixture.life).explicits().getFirst(),
                fixture.catalog)
            .storedItem();
    assertThat(moves.choices(selected, card, Map.of(1, fixture.life))).isEmpty();
  }
}
