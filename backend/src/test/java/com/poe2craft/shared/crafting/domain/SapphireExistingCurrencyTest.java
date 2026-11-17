package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SapphireJewel;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 사파이어·기존·화폐 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
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
 *   <li>{@code item(rarity)}: 아이템 처리에서 {@code catalog.metadata}을(를) 사용한다. 입력: {@code rarity}(희귀도).
 *       반환: {@code ItemState}.
 *   <li>{@code annulmentRemovesReviewedSuffixAndPreservesQualityAndUnrelatedOmen()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code divineRerollsOriginalSourceRangeRatherThanCatalystProjection()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code sideAndBlessedRestrictionsRefuseWithoutMutationOrConsumption()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class SapphireExistingCurrencyTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadSapphire();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemState item(ItemState.Rarity rarity) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        SapphireJewel.BASE_ID,
        82,
        rarity,
        List.of(),
        List.of(
            new ModifierInstance(
                SapphireJewel.CAST_SPEED_ID, Map.of("display_cast_speed_percent", 3L))),
        Set.of(),
        null,
        new CatalystQuality(CatalystQuality.Type.SIBILANT, 20));
  }

  @Test
  void annulmentRemovesReviewedSuffixAndPreservesQualityAndUnrelatedOmen() {
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var before = item(rarity);
      var result =
          simulator.apply(
              before,
              WorkbenchCurrency.ANNULMENT,
              Set.of("Omen_of_Sinistral_Exaltation"),
              new Random(1));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().explicits()).isEmpty();
      assertThat(result.state().rarity()).isEqualTo(rarity);
      assertThat(result.state().catalystQuality()).isEqualTo(before.catalystQuality());
      assertThat(result.events())
          .singleElement()
          .satisfies(
              event -> {
                assertThat(event.kind()).isEqualTo("REMOVE");
                assertThat(event.modifierId()).isEqualTo(SapphireJewel.CAST_SPEED_ID);
                assertThat(event.selectionProbability()).isEqualTo(1);
              });
      assertThat(result.assumptions())
          .extracting(WorkbenchSimulator.Assumption::id)
          .containsExactly("uniform-removal-v1");
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly("Omen_of_Sinistral_Exaltation");
      var repeat =
          simulator.apply(result.state(), WorkbenchCurrency.ANNULMENT, Set.of(), new Random(2));
      assertThat(repeat.applied()).isFalse();
      assertThat(repeat.state()).isEqualTo(result.state());
    }
  }

  @Test
  void divineRerollsOriginalSourceRangeRatherThanCatalystProjection() {
    var seen = new HashSet<Long>();
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var before = item(rarity);
      for (int seed = 0; seed < 100; seed++) {
        var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), new Random(seed));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().catalystQuality()).isEqualTo(before.catalystQuality());
        assertThat(result.state().rarity()).isEqualTo(rarity);
        var affix = result.state().explicits().getFirst();
        assertThat(affix.modifierId()).isEqualTo(SapphireJewel.CAST_SPEED_ID);
        assertThat(affix.values().get("display_cast_speed_percent")).isBetween(2L, 4L);
        seen.add(affix.values().get("display_cast_speed_percent"));
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        assertThat(result.events())
            .extracting(WorkbenchSimulator.Event::kind)
            .containsExactly("REROLL_EXPLICIT");
        assertThat(result.assumptions())
            .singleElement()
            .satisfies(
                assumption -> {
                  assertThat(assumption.n()).isEqualTo(3);
                });
      }
    }
    assertThat(seen).containsExactlyInAnyOrder(2L, 3L, 4L);
  }

  @Test
  void sideAndBlessedRestrictionsRefuseWithoutMutationOrConsumption() {
    var before = item(ItemState.Rarity.RARE);
    for (var entry :
        Map.of(
                WorkbenchCurrency.ANNULMENT, "Omen_of_Sinistral_Annulment",
                WorkbenchCurrency.DIVINE, "Omen_of_the_Blessed")
            .entrySet()) {
      var result = simulator.apply(before, entry.getKey(), Set.of(entry.getValue()), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly(entry.getValue());
    }
    var removed =
        simulator.apply(
            before,
            WorkbenchCurrency.ANNULMENT,
            Set.of("Omen_of_Dextral_Annulment"),
            new Random(1));
    assertThat(removed.applied()).isTrue();
    assertThat(removed.consumedOmens()).containsExactly("Omen_of_Dextral_Annulment");
  }
}
