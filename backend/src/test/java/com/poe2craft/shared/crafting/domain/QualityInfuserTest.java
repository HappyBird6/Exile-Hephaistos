package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 품질·Infuser 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code item(catalog, jewellery, quality)}: 아이템 처리에서 {@code catalog.find}, {@code
 *       catalog.base}을(를) 사용한다. 입력: {@code catalog}(카탈로그), {@code jewellery}(jewellery), {@code
 *       quality}(품질). 반환: {@code ItemState}.
 *   <li>{@code fourInfusersAddTenWithoutCorruptingAndCannotBeReusedEvenAfterCrafting()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code jewelleryFortyReachesFiftyAndSanctificationRoundsBeforeQuality()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class QualityInfuserTest {
  ItemState item(ItemCatalog catalog, boolean jewellery, int quality) {
    var implicit = catalog.find(catalog.base().implicitModifierId());
    var implicits =
        implicit
            .map(
                d -> {
                  var values = new HashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return List.of(new ModifierInstance(d.id(), values));
                })
            .orElse(List.of());
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        100,
        ItemState.Rarity.RARE,
        implicits,
        List.of(),
        Set.of(),
        null,
        jewellery ? new CatalystQuality(CatalystQuality.Type.NECROTIC, quality) : null,
        List.of(),
        jewellery ? null : quality);
  }

  @Test
  void fourInfusersAddTenWithoutCorruptingAndCannotBeReusedEvenAfterCrafting() {
    var catalogs =
        List.of(
            ItemCatalogLoader.loadBody(),
            ItemCatalogLoader.loadBow(),
            ItemCatalogLoader.loadWand(),
            ItemCatalogLoader.loadDefault());
    var actions =
        List.of(
            WorkbenchCurrency.VAAL_ARMOURERS_INFUSER,
            WorkbenchCurrency.VAAL_BLACKSMITHS_INFUSER,
            WorkbenchCurrency.VAAL_ARCANISTS_INFUSER,
            WorkbenchCurrency.VAAL_CATALYSING_INFUSER);
    for (int i = 0; i < actions.size(); i++) {
      var catalog = catalogs.get(i);
      var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
      for (int quality : List.of(0, 7, 20, 25)) {
        var before = item(catalog, i == 3, quality);
        var result =
            sim.apply(before, actions.get(i), Set.of(WorkbenchOmen.BLESSED.id()), new Random(1));
        assertThat(result.applied()).as(result.reason()).isTrue();
        assertThat(result.state().conditions()).isEmpty();
        assertThat(result.state().qualityInfused()).isTrue();
        assertThat(QualityInfuserRules.current(result.state(), actions.get(i)))
            .isEqualTo(Math.min(quality + 10, 30));
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(sim.apply(result.state(), actions.get(i), Set.of(), new Random(1)).applied())
            .isFalse();
        var choice =
            sim.exaltChoices(result.state(), WorkbenchCurrency.EXALTED, Set.of()).stream()
                .filter(c -> catalog.find(c.modifierId()).orElseThrow().stats().size() == 1)
                .findFirst()
                .orElseThrow();
        var exalt =
            sim.applyCustomExalt(
                result.state(),
                WorkbenchCurrency.EXALTED,
                Set.of(),
                new Random(1),
                choice.modifierId());
        assertThat(exalt.applied()).isTrue();
        assertThat(exalt.state().qualityInfused()).isTrue();
        assertThat(sim.apply(exalt.state(), actions.get(i), Set.of(), new Random(1)).applied())
            .isFalse();
      }
    }
  }

  @Test
  void jewelleryFortyReachesFiftyAndSanctificationRoundsBeforeQuality() {
    var catalog = ItemCatalogLoader.loadDefault();
    var base = item(catalog, true, 40);
    var mods =
        List.of(
            new ModifierInstance(
                QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L)),
            new ModifierInstance(
                "amulet:suffix:of-the-overseer", Map.of("minion_skill_gem_level_+", 3L)));
    var before =
        new ItemState(
            base.snapshotId(),
            base.baseItemId(),
            100,
            base.rarity(),
            base.implicits(),
            mods,
            Set.of(),
            null,
            base.catalystQuality());
    var sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var result =
        sim.apply(before, WorkbenchCurrency.VAAL_CATALYSING_INFUSER, Set.of(), new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().catalystQuality().amount()).isEqualTo(50);
    var sanctified =
        sim.apply(
                result.state(),
                WorkbenchCurrency.DIVINE,
                Set.of(WorkbenchOmen.SANCTIFICATION.id()),
                new Random() {
                  @Override
                  public int nextInt(int bound) {
                    return bound - 1;
                  }

                  @Override
                  public int nextInt(int origin, int bound) {
                    return bound - 1;
                  }

                  @Override
                  public long nextLong(long bound) {
                    return bound - 1;
                  }
                })
            .state();
    assertThat(sanctified.qualityInfused()).isTrue();
    var projection =
        CatalystQualityDisplay.describe(sanctified, catalog).stream()
            .filter(p -> p.modifierId().equals("amulet:suffix:of-the-overseer"))
            .findFirst()
            .orElseThrow();
    assertThat(projection.displayedValues()).containsEntry("minion_skill_gem_level_+", 6L);
    assertThat(projection.originalValues()).containsEntry("minion_skill_gem_level_+", 3L);
  }
}
