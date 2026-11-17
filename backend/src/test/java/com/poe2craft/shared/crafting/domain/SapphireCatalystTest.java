package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SapphireJewel;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 사파이어·기폭제 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code sim}: sim ({@code WorkbenchSimulator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code item(rarity, affixes)}: 아이템 처리에서 {@code catalog.metadata}을(를) 사용한다. 입력: {@code
 *       rarity}(희귀도), {@code affixes}(affixes). 반환: {@code ItemState}.
 *   <li>{@code allThirteenRefinedTypesApplyRepeatReplaceAndPreserveOriginalRolls()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code emptyJewelHasNoInventedMatchingAffixAndOrdinaryCatalystsRemainUnavailable()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code uniqueSpecialAndAboveCapStatesAreNotSupported()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
class SapphireCatalystTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadSapphire();
  final WorkbenchSimulator sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemState item(ItemState.Rarity rarity, List<ModifierInstance> affixes) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        SapphireJewel.BASE_ID,
        82,
        rarity,
        List.of(),
        affixes,
        Set.of());
  }

  @Test
  void allThirteenRefinedTypesApplyRepeatReplaceAndPreserveOriginalRolls() {
    var suffix =
        new ModifierInstance(SapphireJewel.CAST_SPEED_ID, Map.of("display_cast_speed_percent", 3L));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var before = item(rarity, List.of(suffix));
      for (var type : CatalystQuality.Type.values()) {
        var action = WorkbenchCurrency.valueOf("REFINED_CATALYST_" + type);
        var applied =
            sim.apply(before, action, Set.of("Omen_of_Sinistral_Exaltation"), new Random(1));
        assertThat(applied.applied()).isTrue();
        assertThat(applied.state().catalystQuality()).isEqualTo(new CatalystQuality(type, 20));
        assertThat(applied.state().explicits()).isEqualTo(before.explicits());
        assertThat(applied.consumedOmens()).isEmpty();
        assertThat(applied.events()).isEmpty();
        assertThat(sim.apply(applied.state(), action, Set.of(), new Random(1)).state())
            .isEqualTo(applied.state());
        var projection = CatalystQualityDisplay.describe(applied.state(), catalog).getFirst();
        boolean matches =
            type == CatalystQuality.Type.SIBILANT || type == CatalystQuality.Type.SKITTERING;
        assertThat(projection.status()).isEqualTo(matches ? "SCALED_INTEGER" : "NO_MATCH");
        assertThat(projection.originalValues()).isEqualTo(suffix.values());
        assertThat(projection.displayedValues())
            .containsEntry("display_cast_speed_percent", matches ? 4L : 3L);
        before = applied.state();
      }
    }
  }

  @Test
  void emptyJewelHasNoInventedMatchingAffixAndOrdinaryCatalystsRemainUnavailable() {
    var root = item(ItemState.Rarity.MAGIC, List.of());
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(58);
    for (var a : WorkbenchCurrency.values()) {
      if (a.catalystType() == null) continue;
      var result = sim.apply(root, a, Set.of(), new Random(1));
      assertThat(result.applied()).isEqualTo(a.refinedCatalyst());
      assertThat(result.state().explicits()).isEmpty();
    }
    assertThat(CatalystQualityDisplay.describe(root, catalog)).isEmpty();
    var ring = ItemCatalogLoader.loadRing();
    var ringRoot =
        new ItemState(
            ring.metadata().snapshotId(),
            ring.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(
                new ModifierInstance(
                    ring.base().implicitModifierId(),
                    Map.of(
                        "attack_minimum_added_physical_damage",
                        1L,
                        "attack_maximum_added_physical_damage",
                        4L))),
            List.of(),
            Set.of());
    var ringSim = new WorkbenchSimulator(ring, new CraftingEngine(ring));
    for (var type : CatalystQuality.Type.values())
      assertThat(
              ringSim
                  .apply(
                      ringRoot,
                      WorkbenchCurrency.valueOf("REFINED_CATALYST_" + type),
                      Set.of(),
                      new Random(1))
                  .applied())
          .isFalse();
  }

  @Test
  void uniqueSpecialAndAboveCapStatesAreNotSupported() {
    assertThat(new ItemStateValidator(catalog).validate(item(ItemState.Rarity.NORMAL, List.of())))
        .isEmpty();
    for (var rarity : List.of(ItemState.Rarity.UNIQUE))
      assertThat(new ItemStateValidator(catalog).validate(item(rarity, List.of()))).isNotEmpty();
    var root = item(ItemState.Rarity.MAGIC, List.of());
    var overcap =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            List.of(),
            List.of(),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.FLESH, 21));
    assertThatThrownBy(
            () ->
                sim.apply(
                    overcap, WorkbenchCurrency.REFINED_CATALYST_FLESH, Set.of(), new Random(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(QualityLimitRules.describe(root, catalog).maximumQuality()).isEqualTo(20);
  }
}
