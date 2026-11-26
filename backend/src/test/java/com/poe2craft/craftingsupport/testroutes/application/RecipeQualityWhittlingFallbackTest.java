package com.poe2craft.craftingsupport.testroutes.application;

import static com.poe2craft.shared.crafting.domain.QualityLimitRules.BREACH_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 태양 목걸이의 정신력 +47 L을 임시 품질 접두 대신 추첨하는 최후 수단을 검증한다. */
class RecipeQualityWhittlingFallbackTest {
  /** 이미 보유하여 보호하는 근접 레벨 +3의 명시 접미다. */
  private static final String MELEE = "amulet:suffix:of-battle";

  /** 새로 획득할 정신력 +47 이상 티어다. */
  private static final String SPIRIT = "amulet:prefix:countess";

  /** 레벨 82 태양 목걸이의 기본 카탈로그와 작업대 전이를 재사용한다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  /** 실제 아이템에 남은 명시 옵션으로 목표 달성을 검증한다. */
  private List<String> ids(ItemState item) {
    return item.explicits().stream().map(ModifierInstance::modifierId).toList();
  }

  /** 해당 희소 티어가 테스트 탭의 보존된 쇄골로 공개 가능한지 원본 카탈로그에서 확인한다. */
  private RouteCards.Option low(int number, String id, ItemState item) {
    var definition = fixture.catalog.find(id).orElseThrow();
    String bone = "Preserved_Collarbone";
    assertThat(DesecrationCurrency.sources(item.baseItemId(), item.itemLevel(), definition, true))
        .contains(bone);
    return new RouteCards.Option(
        number,
        Map.of(
            id,
            List.of(
                new RouteCards.Source("B", null, RouteCards.Frequency.L),
                new RouteCards.Source("D", bone, null))));
  }

  /** 보유한 근접 레벨이 공개된 훼손 접미인 정상 체크포인트를 구성한다. */
  private ItemState revealedMelee(String... mods) {
    var item =
        fixture
            .item(ItemState.Rarity.RARE, mods)
            .withDesecrated(
                List.of(new ItemState.DesecratedSlot("revealed", AffixType.SUFFIX, MELEE)));
    assertThat(new ItemStateValidator(fixture.catalog).validate(item)).isEmpty();
    AbyssCraftingState.from(item, fixture.catalog).validate(fixture.catalog);
    return item;
  }

  /** 품질 작업 후 지정 번호의 정신력 목표를 절사로 획득하는 고정 실행열이다. */
  private List<RecipeRoutePlanner.Step> fixedSteps(int target) {
    return List.of(
        new RecipeRoutePlanner.Step("QUALITY_40", List.of(), List.of(), List.of(), null),
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(target), List.of(), null));
  }

  /** 기존 일반 카오스·완벽 엑잘·뼈가 각각 가능한 시작에서는 절사로 L을 얻는 고정 실행을 거절한다. */
  @Test
  void excludesCapWhittlingWhenOrdinaryLowAcquisitionIsAvailable() {
    excludeWhenAvailable(
        fixture.item(ItemState.Rarity.RARE, BREACH_ID), Map.of(), RecipeMoves.Purpose.LOW_CHAOS);
    excludeWhenAvailable(
        revealedMelee(BREACH_ID, fixture.life, MELEE),
        Map.of(1, MELEE),
        RecipeMoves.Purpose.LOW_EXALT);
    excludeWhenAvailable(
        fixture.item(
            ItemState.Rarity.RARE,
            BREACH_ID,
            fixture.life,
            "amulet:prefix:azure",
            MELEE,
            fixture.strength,
            "amulet:suffix:of-the-yeti"),
        Map.of(1, MELEE),
        RecipeMoves.Purpose.BONE);
  }

  /** 지정한 기존 대안이 실제로 존재하는 카드에서 상한 절사를 강제한 경로만 거절하는지 검사한다. */
  private void excludeWhenAvailable(
      ItemState start, Map<Integer, String> held, RecipeMoves.Purpose alternative) {
    int target = held.size() + 1;
    var spirit = low(target, SPIRIT, start);
    var card =
        new RouteCards.Card(
            held.isEmpty() ? List.of(spirit) : List.of(low(1, MELEE, start), spirit), 0);
    var choices = new RecipeMoves(fixture.workbench).choices(start, card, held);
    assertThat(choices).anyMatch(c -> c.target() == target && c.purpose() == alternative);
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    fixture.workbench,
                    start,
                    card,
                    s -> ids(s).contains(SPIRIT),
                    12000,
                    true,
                    40,
                    fixedSteps(target))
                .routes())
        .isEmpty();
  }

  /** 채워진 접두와 이미 사용한 훼손 슬롯에서 희소 옵션을 보존하며 상한만 정신력으로 바꾸고 같은 실행을 재생한다. */
  @Test
  void searchesAndReplaysTheCapReplacementAfterTheOtherLowMethodsAreBlocked() {
    var start = revealedMelee(BREACH_ID, fixture.life, "amulet:prefix:azure", MELEE);
    var card = new RouteCards.Card(List.of(low(1, MELEE, start), low(2, SPIRIT, start)), 0);
    var existing = new RecipeMoves(fixture.workbench).choices(start, card, Map.of(1, MELEE));
    assertThat(existing)
        .noneMatch(
            c ->
                c.target() == 2
                    && (c.purpose() == RecipeMoves.Purpose.LOW_CHAOS
                        || c.purpose() == RecipeMoves.Purpose.LOW_EXALT
                        || c.purpose() == RecipeMoves.Purpose.BONE
                        || c.purpose() == RecipeMoves.Purpose.LOW_REVEAL));
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            s -> ids(s).containsAll(List.of(MELEE, SPIRIT)),
            12000,
            true,
            40);
    var route =
        found.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 2
                        && r.steps().getFirst().action().equals("QUALITY_40")
                        && r.steps().getLast().action().equals("CHAOS")
                        && r.steps()
                            .getLast()
                            .omens()
                            .equals(List.of(WorkbenchOmen.WHITTLING.id())))
            .findFirst()
            .orElseThrow();
    var replaced = found.states().get(route.stateIndices().getLast());
    assertThat(replaced.explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactlyInAnyOrder(fixture.life, "amulet:prefix:azure", MELEE, SPIRIT);
    assertThat(replaced.desecrated()).isEqualTo(start.desecrated());
    assertThat(route.steps().getLast().acquired()).containsExactly(2);
    assertThat(route.steps().getLast().lost()).isEmpty();
    assertThat(route.steps().getLast().omenOutcomes()).isNotNull();
    var replay =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            s -> ids(s).containsAll(List.of(MELEE, SPIRIT)),
            12000,
            true,
            40,
            fixedSteps(2));
    var fixed = replay.routes().getFirst();
    assertThat(fixed.steps()).isEqualTo(route.steps());
    assertThat(replay.states().get(fixed.stateIndices().getLast())).isEqualTo(replaced);
  }
}
