package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 실제 희귀 태양 목걸이의 목표 보유 수·빈도와 마지막 공개 방향으로 분열 준비 후보를 검증한다. */
class RecipeFractureMovesTest {
  /** 작업대와 실제 태양 목걸이 상태를 공유한다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  /** 기본·고정 재생이 공유하는 분열 추천 정책이다. */
  private final RecipeMoves moves = new RecipeMoves(fixture.workbench);

  /** 이 카드에서 희소 목표인 정신력과 보유·공개할 실제 티어들이다. */
  private static final String SPIRIT = "amulet:prefix:countess",
      FIRE = "amulet:suffix:of-the-whelpling",
      MANA = "amulet:prefix:azure",
      COLD = "amulet:suffix:of-the-seal";

  /** 이 목표에 허용한 전체 티어의 기존 카드 분류다. */
  private static final RouteCards.Source
      LOW = new RouteCards.Source("B", null, RouteCards.Frequency.L),
      COMMON = new RouteCards.Source("B", null, RouteCards.Frequency.H),
      BONE = new RouteCards.Source("D", "Preserved_Collarbone", null);

  /** 지정한 유일한 허용 티어·출처로 카드 목표를 만든다. */
  private static RouteCards.Option option(int n, String id, RouteCards.Source source) {
    return new RouteCards.Option(n, Map.of(id, List.of(source)));
  }

  /** 정신력·생명력·화염 저항을 보유한 카드에 후속 목표를 추가한다. */
  private RouteCards.Card card(RouteCards.Option... more) {
    var options =
        new ArrayList<>(
            List.of(
                option(1, SPIRIT, LOW), option(2, fixture.life, COMMON), option(3, FIRE, COMMON)));
    options.addAll(List.of(more));
    return new RouteCards.Card(options, 0);
  }

  /** 현재 세 목표와 일치하는 실제 옵션 번호별 티어다. */
  private Map<Integer, String> held() {
    return Map.of(1, SPIRIT, 2, fixture.life, 3, FIRE);
  }

  /** 이미 보유한 유효 목표 셋 중 L이 있어야 하며 비목표나 다른 행과 중복된 같은 옵션은 세지 않는다. */
  @Test
  void requiresThreeDistinctGoalsIncludingAHeldLowAndAnUnfracturedBase() {
    var four = fixture.item(ItemState.Rarity.RARE, SPIRIT, fixture.life, FIRE, COLD);
    var goal = card(option(4, MANA, BONE));
    assertThat(moves.fractureChoices(four, goal, held()))
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.purpose()).isEqualTo(RecipeMoves.Purpose.FRACTURE);
              assertThat(c.move().action()).isEqualTo("FRACTURING");
              assertThat(c.move().omenIds()).isEmpty();
            });
    assertThat(moves.fractureChoices(four, goal, Map.of(1, SPIRIT, 2, fixture.life))).isEmpty();
    // 두 행에 같은 실제 옵션을 대입해도 두 옵션으로 인정하지 않는다.
    var duplicate = card(option(4, fixture.life, COMMON));
    assertThat(
            moves.fractureChoices(
                four, duplicate, Map.of(1, SPIRIT, 2, fixture.life, 4, fixture.life)))
        .isEmpty();
    var commonOnly =
        new RouteCards.Card(
            List.of(
                option(1, SPIRIT, COMMON),
                option(2, fixture.life, COMMON),
                option(3, FIRE, COMMON)),
            0);
    assertThat(moves.fractureChoices(four, commonOnly, held())).isEmpty();
    assertThat(
            moves.fractureChoices(
                four.withConditions(Set.of(ItemState.Condition.CORRUPTED)), goal, held()))
        .isEmpty();
    for (var locked : fixture.workbench.fractureOutcomes(four))
      assertThat(moves.fractureChoices(locked, goal, held())).isEmpty();
  }

  /** 마지막 공개가 가능한 방향만 예약하며 양쪽 모두 후보일 때에는 두 방향을 별도로 보존한다. */
  @Test
  void reservesOnlyEligibleFutureGoalsAndSeparatesBothAvailableDirections() {
    var three = fixture.item(ItemState.Rarity.RARE, SPIRIT, fixture.life, FIRE);
    var suffix = option(4, COLD, BONE);
    String right = DesecrationCurrency.directionOmen(AffixType.SUFFIX),
        left = DesecrationCurrency.directionOmen(AffixType.PREFIX);
    assertThat(moves.fractureChoices(three, card(), held())).isEmpty();
    var reserved = moves.fractureChoices(three, card(suffix), held());
    assertThat(reserved)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.move().action()).isEqualTo("Preserved_Collarbone");
              assertThat(c.move().omenIds()).containsExactly(right);
              assertThat(c.eligibleTiers()).containsExactly(COLD);
            });
    var both = card(suffix, option(5, MANA, BONE));
    assertThat(moves.fractureChoices(three, both, held()))
        .extracting(c -> c.move().omenIds())
        .containsExactlyInAnyOrder(List.of(left), List.of(right));
    var hidden =
        three.withDesecrated(
            List.of(new ItemState.DesecratedSlot("reserved", AffixType.SUFFIX, null)));
    assertThat(moves.fractureChoices(hidden, both, held()))
        .extracting(c -> c.move().action())
        .containsExactly("FRACTURING");
    var revealed =
        fixture
            .item(ItemState.Rarity.RARE, SPIRIT, fixture.life, FIRE, COLD)
            .withDesecrated(
                List.of(new ItemState.DesecratedSlot("revealed", AffixType.SUFFIX, COLD)));
    assertThat(moves.fractureChoices(revealed, both, held())).isEmpty();
  }

  /** 이미 접두 세 칸이 찬 경우 접미의 뼈 예약에는 징조를 소비하지 않는다. */
  @Test
  void usesTheBoneAloneWhenOnlyTheMatchingSideIsOpen() {
    var threePrefixes = fixture.item(ItemState.Rarity.RARE, SPIRIT, fixture.life, MANA);
    var goal =
        new RouteCards.Card(
            List.of(
                option(1, SPIRIT, LOW),
                option(2, fixture.life, COMMON),
                option(3, MANA, COMMON),
                option(4, COLD, BONE)),
            0);
    assertThat(
            moves.fractureChoices(threePrefixes, goal, Map.of(1, SPIRIT, 2, fixture.life, 3, MANA)))
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.move().action()).isEqualTo("Preserved_Collarbone");
              assertThat(c.move().omenIds()).isEmpty();
              assertThat(c.eligibleTiers()).containsExactly(COLD);
            });
  }
}
