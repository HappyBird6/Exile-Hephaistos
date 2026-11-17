package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import org.junit.jupiter.api.Test;

/**
 * 베이스·제작재료·생성적격 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code CatalogCraftingService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BaseMaterialEligibilityTest()}: 전달받은 값으로 구성원을 초기화한다.
 *   <li>{@code listsReviewedMaterialsForEveryBaseWithoutItemLevelOrRarityGating()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code keepsSupportedMaterialsVisibleBeforeTheirCurrentExecutionConditionsAreMet()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class BaseMaterialEligibilityTest {
  private final CatalogCraftingService service;

  BaseMaterialEligibilityTest() {
    var solar = ItemCatalogLoader.loadDefault();
    service =
        new CatalogConfiguration()
            .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  @Test
  void listsReviewedMaterialsForEveryBaseWithoutItemLevelOrRarityGating() {
    for (String key : BaseRegistry.registeredKeys()) {
      var low = service.initial(key, 1);
      var high = service.initial(key, 100);
      assertThat(low.baseMaterialActions())
          .as(key)
          .isEqualTo(high.baseMaterialActions())
          .doesNotHaveDuplicates();
      String id = low.state().baseItemId();
      for (var action : WorkbenchCurrency.values()) {
        if (action.catalystType() != null) {
          assertThat(low.baseMaterialActions().contains(action))
              .as(key + ":" + action)
              .isEqualTo(
                  action.refinedCatalyst()
                      ? BaseRegistry.refinedCatalyst(id)
                      : BaseRegistry.ordinaryCatalyst(id));
        }
      }
      if (!BasicJewel.supportedCrafting(id))
        assertThat(low.baseMaterialActions()).noneMatch(WorkbenchCurrency::isLiquid);
      else
        assertThat(low.baseMaterialActions())
            .noneMatch(a -> a.name().contains("ESSENCE_") || a.isAlloy());
    }
  }

  @Test
  void keepsSupportedMaterialsVisibleBeforeTheirCurrentExecutionConditionsAreMet() {
    var solar = service.initial("solar", 82);
    assertThat(solar.state().rarity()).isEqualTo(ItemState.Rarity.NORMAL);
    assertThat(solar.baseMaterialActions())
        .contains(WorkbenchCurrency.GREATER_ESSENCE_BODY, WorkbenchCurrency.CATALYST_FLESH)
        .doesNotContain(
            WorkbenchCurrency.PERFECT_ESSENCE_BODY, WorkbenchCurrency.REFINED_CATALYST_FLESH);
    assertThat(service.initial("ruby", 82).baseMaterialActions())
        .contains(WorkbenchCurrency.REFINED_CATALYST_FLESH, WorkbenchCurrency.DILUTED_LIQUID_IRE)
        .doesNotContain(WorkbenchCurrency.CATALYST_FLESH, WorkbenchCurrency.GREATER_ESSENCE_BODY);
  }
}
