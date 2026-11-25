package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    var lastOnly = new java.util.ArrayList<>(options);
    lastOnly.set(1, new RouteCards.Option(2, Map.of(fixture.strength, List.of(common))));
    assertThat(moves.choices(empty, new RouteCards.Card(lastOnly, 0), Map.of()))
        .isNotEmpty()
        .anyMatch(c -> c.target() == 1 && c.purpose() == RecipeMoves.Purpose.BONE)
        .noneMatch(c -> c.purpose() == RecipeMoves.Purpose.LOW_EXALT);
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
