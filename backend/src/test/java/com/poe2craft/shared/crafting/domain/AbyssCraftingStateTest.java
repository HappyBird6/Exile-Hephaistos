package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemObservations;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 관측된 훼손 슬롯과 공개 후보·수동 선택 상태를 보존한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code fixture}: 검증사례 ({@code HysteriaEssenceTest}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code start(ids)}: 시작값 처리에서 {@code AbyssCraftingState.from}, {@code fixture.rare}을(를)
 *       사용한다. 입력: {@code ids}(식별자목록). 반환: {@code AbyssCraftingState}.
 *   <li>{@code modifier(id)}: 옵션 처리에서 {@code fixture.rare}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code ModifierInstance}.
 *   <li>{@code
 *       observedSocketCountSurvivesSlotChoiceAndUnresolvedAugmentsCannotBeSilentlyCrafted()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unrevealedCountsAsSlotAndRemovalTargetWithoutInventedStats()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code observedChoiceReworksTheSameSlotAtSixWithoutChangingOtherRolls()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code invalidOffersAreRejectedRatherThanSilentlyPruned()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code fracturedOptionsStayOutsideDummyRemovalPool()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
class AbyssCraftingStateTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();
  final ItemCatalog catalog = fixture.catalog;

  AbyssCraftingState start(String... ids) {
    return AbyssCraftingState.from(fixture.rare(82, "", ids), catalog);
  }

  ModifierInstance modifier(String id) {
    return fixture.rare(82, "", id).explicits().getFirst();
  }

  @Test
  void observedSocketCountSurvivesSlotChoiceAndUnresolvedAugmentsCannotBeSilentlyCrafted() {
    var original = fixture.rare(82, "", "amulet:prefix:hale");
    var observations = new ItemObservations(2, List.of(), List.of());
    var before = original.withObservations(observations);
    var state =
        AbyssCraftingState.from(before, catalog).addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var candidate = modifier("amulet:prefix:azure");
    var chosen =
        state
            .observe("abyss", List.of(candidate, candidate, candidate), catalog)
            .choose(0, catalog);
    assertThat(chosen.storedItem().observations()).isEqualTo(observations);
    assertThat(AbyssCraftingState.from(chosen.storedItem(), catalog).storedItem())
        .isEqualTo(chosen.storedItem());
    var unresolved =
        original.withObservations(
            new ItemObservations(2, List.of("Unknown installed effect (rune)"), List.of()));
    assertThatThrownBy(() -> AbyssCraftingState.from(unresolved, catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(unresolved.observations().augmentEffects())
        .containsExactly("Unknown installed effect (rune)");
  }

  @Test
  void unrevealedCountsAsSlotAndRemovalTargetWithoutInventedStats() {
    var before = start("amulet:prefix:hale", "amulet:prefix:adept-s");
    var state = before.addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(state.count(AffixType.PREFIX)).isEqualTo(3);
    assertThat(state.knownItem()).isEqualTo(before.knownItem());
    assertThat(state.removalCandidates(AffixType.PREFIX)).hasSize(3);
    assertThat(state.removalCandidates(AffixType.SUFFIX)).isEmpty();
    assertThat(state.remove("abyss", null, catalog).slots()).isEqualTo(before.slots());
    assertThatThrownBy(() -> state.addUnrevealed("second", AffixType.PREFIX, catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void observedChoiceReworksTheSameSlotAtSixWithoutChangingOtherRolls() {
    var state =
        start(
                "amulet:prefix:hale",
                "amulet:prefix:adept-s",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander")
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var candidate = modifier("amulet:prefix:azure");
    var observed = state.observe("abyss", List.of(candidate, candidate, candidate), catalog);
    var chosen = observed.choose(1, catalog);
    assertThat(chosen.slots()).hasSize(6);
    assertThat(chosen.slots().subList(0, 5)).isEqualTo(state.slots().subList(0, 5));
    assertThat(chosen.slots().getLast().modifier()).isEqualTo(candidate);
    assertThat(chosen.offer()).isNull();
    assertThat(
            chosen
                .observe("abyss", List.of(candidate, candidate, candidate), catalog)
                .choose(0, catalog))
        .isEqualTo(chosen);
  }

  @Test
  void invalidOffersAreRejectedRatherThanSilentlyPruned() {
    var state = start("amulet:prefix:hale").addUnrevealed("abyss", AffixType.PREFIX, catalog);
    var valid = modifier("amulet:prefix:azure");
    var conflict = modifier("amulet:prefix:hale");
    var wrongSide = modifier("amulet:suffix:of-the-brute");
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid, conflict), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("abyss", List.of(valid, valid, wrongSide), catalog))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.choose(0, catalog)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> state.observe("explicit-0", List.of(valid, valid, valid), catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fracturedOptionsStayOutsideDummyRemovalPool() {
    var concrete =
        fixture.rare(82, "amulet:prefix:hale", "amulet:prefix:hale", "amulet:prefix:adept-s");
    var state =
        AbyssCraftingState.from(concrete, catalog)
            .addUnrevealed("abyss", AffixType.PREFIX, catalog);
    assertThat(state.removalCandidates(null)).hasSize(2);
    assertThatThrownBy(() -> state.remove("explicit-1", null, catalog))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
