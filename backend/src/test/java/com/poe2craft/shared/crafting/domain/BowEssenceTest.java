package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 활·에센스 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code essences()}: essences 값을 조회·구성한다. 반환: {@code Stream<WorkbenchCurrency>}.
 *   <li>{@code state(level, rarity, mods)}: 현재 상태 표현을 조회한다. 입력: {@code level}(레벨), {@code
 *       rarity}(희귀도), {@code mods}(옵션목록). 반환: {@code ItemState}.
 *   <li>{@code fixedTargetHasProvenLevelFamilySlotAndProbability(action)}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 입력: {@code action}(행동).
 *   <li>{@code ordinaryPoolIsCompleteAndDefaultSolarIsUnchanged()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code existingOrdinaryActionsRetainFracturedValuesAndDifferentBasesRejectBowEssences()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code tamperedCatalogSourcesAreRejected()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외:
 *       {@code Exception}.
 * </ul>
 */
class BowEssenceTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadBow();
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          BowEssenceTargets.VERIFIED);

  static Stream<WorkbenchCurrency> essences() {
    return BowEssenceTargets.VERIFIED.keySet().stream();
  }

  private ItemState state(int level, ItemState.Rarity rarity, List<ModifierInstance> mods) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        BowEssenceTargets.BASE_ID,
        level,
        rarity,
        List.of(),
        mods,
        Set.of());
  }

  @ParameterizedTest
  @MethodSource("essences")
  void fixedTargetHasProvenLevelFamilySlotAndProbability(WorkbenchCurrency action) {
    var target = catalog.find(BowEssenceTargets.VERIFIED.get(action).getFirst()).orElseThrow();
    var before = state(target.requiredItemLevel(), ItemState.Rarity.MAGIC, List.of());
    var applied = simulator.apply(before, action, Set.of("Omen_of_the_Blessed"), new Random(17));
    assertThat(applied.applied()).isTrue();
    assertThat(applied.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
    assertThat(applied.state().explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactly(target.id());
    assertThat(applied.events()).hasSize(1);
    assertThat(applied.events().getFirst().selectionProbability()).isEqualTo(1);
    assertThat(applied.consumedOmens()).isEmpty();
    assertThat(applied.remainingOmens()).containsExactly("Omen_of_the_Blessed");
    assertThat(new ItemStateValidator(catalog).validate(applied.state())).isEmpty();
    assertThat(
            simulator
                .apply(
                    state(target.requiredItemLevel() - 1, ItemState.Rarity.MAGIC, List.of()),
                    action,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
    var overlap =
        simulator.apply(
            state(82, ItemState.Rarity.MAGIC, applied.state().explicits()),
            action,
            Set.of(),
            new Random(17));
    assertThat(overlap.applied()).isFalse();
    assertThat(overlap.state().explicits()).isEqualTo(applied.state().explicits());
    assertThat(
            simulator
                .apply(
                    state(82, ItemState.Rarity.NORMAL, List.of()), action, Set.of(), new Random(17))
                .applied())
        .isFalse();
  }

  @Test
  void ordinaryPoolIsCompleteAndDefaultSolarIsUnchanged() {
    assertThat(catalog.modifiers()).hasSize(146);
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0).count())
        .isEqualTo(140);
    assertThat(catalog.base().hasImplicit()).isFalse();
    assertThat(catalog.metadata().prefixCount()).isEqualTo(75);
    assertThat(catalog.metadata().suffixCount()).isEqualTo(71);
    assertThat(catalog.metadata().prefixWeight()).isEqualTo(44755);
    assertThat(catalog.metadata().suffixWeight()).isEqualTo(52277);
    assertThat(catalog.modifiers().values().stream().filter(d -> d.stats().size() > 1).count())
        .isEqualTo(50);
    assertThat(catalog.find("crude-bow:prefix:focused").orElseThrow().stats())
        .extracting(ModifierDefinition.StatRange::id)
        .containsExactly("local_accuracy_rating");
    assertThat(catalog.find("crude-bow:suffix:of-skill").orElseThrow().stats())
        .extracting(ModifierDefinition.StatRange::id)
        .containsExactly("local_attack_speed_+%");
    assertThat(ItemCatalogLoader.loadDefault().modifiers()).hasSize(218);
  }

  @Test
  void existingOrdinaryActionsRetainFracturedValuesAndDifferentBasesRejectBowEssences() {
    var rare =
        simulator.apply(
            state(82, ItemState.Rarity.NORMAL, List.of()),
            WorkbenchCurrency.ALCHEMY,
            Set.of(),
            new Random(17));
    assertThat(rare.applied()).isTrue();
    var locked =
        simulator
            .apply(rare.state(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(17))
            .state();
    var instance =
        locked.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    for (var action :
        List.of(
            WorkbenchCurrency.DIVINE,
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.EXALTED)) {
      var result = simulator.apply(locked, action, Set.of(), new Random(19));
      assertThat(result.state().explicits()).contains(instance);
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
    }
    assertThat(simulator.actions(state(82, ItemState.Rarity.NORMAL, List.of()), Set.of()))
        .hasSize(82)
        .extracting(WorkbenchSimulator.Availability::action)
        .containsOnlyOnce(
            WorkbenchCurrency.LESSER_ESSENCE_INFINITE,
            WorkbenchCurrency.ESSENCE_INFINITE,
            WorkbenchCurrency.GREATER_ESSENCE_INFINITE);
    var solar = ItemCatalogLoader.loadDefault();
    var old = new WorkbenchSimulator(solar, new CraftingEngine(solar));
    assertThat(old.actions(SolarAmulet.initial(solar, 82, 15), Set.of())).hasSize(81);
    assertThat(
            old.apply(
                    SolarAmulet.initial(solar, 82, 15),
                    WorkbenchCurrency.ESSENCE_ABRASION,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
  }

  @Test
  void tamperedCatalogSourcesAreRejected() throws Exception {
    try (var data = getClass().getResourceAsStream("/catalog/crude-bow/catalog.json");
        var details = getClass().getResourceAsStream("/catalog/crude-bow/details.raw.json")) {
      assertThatThrownBy(
              () ->
                  ItemCatalogLoader.load(
                      data, new java.io.ByteArrayInputStream(new byte[0]), details))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("checksum");
    }
  }
}
