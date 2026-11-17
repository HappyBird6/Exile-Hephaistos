package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

// Keep the real HTTP filters; mock only their external Redis dependency.
/**
 * 아이템·상태·Wire·Contract 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code wire}: wire ({@code ObjectMapper}).
 *   <li>{@code crafting}: 제작 ({@code CatalogCraftingService}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code serverSerializedItemsRoundTripThroughOrdinaryAndStrictRepeatReaders()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest(controllers = com.poe2craft.admin.api.EdgeRejectionController.class)
@Import(CatalogConfiguration.class)
class ItemStateWireContractTest {
  @Autowired ObjectMapper wire;
  @Autowired CatalogCraftingService crafting;
  @Autowired ItemCatalog catalog;

  @Test
  void serverSerializedItemsRoundTripThroughOrdinaryAndStrictRepeatReaders() {
    var root = SolarAmulet.initial(catalog);
    var strict = wire.copy().enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    var simulator =
        new com.poe2craft.shared.crafting.domain.WorkbenchSimulator(
            catalog, new com.poe2craft.shared.crafting.domain.CraftingEngine(catalog));
    var rare =
        simulator
            .apply(
                root,
                com.poe2craft.shared.crafting.domain.WorkbenchCurrency.ALCHEMY,
                java.util.Set.of(),
                new java.util.Random(2))
            .state();
    var modifiers = new java.util.ArrayList<>(rare.explicits());
    var first = modifiers.getFirst();
    modifiers.set(0, new ModifierInstance(first.modifierId(), first.values(), true));
    var locked =
        new ItemState(
            rare.snapshotId(),
            rare.baseItemId(),
            rare.itemLevel(),
            rare.rarity(),
            rare.implicits(),
            modifiers,
            java.util.Set.of());
    var sanctified =
        simulator
            .apply(
                locked,
                com.poe2craft.shared.crafting.domain.WorkbenchCurrency.DIVINE,
                java.util.Set.of("Omen_of_Sanctification"),
                new java.util.Random(4))
            .state();
    assertThat(sanctified.sanctification()).isNotNull();
    for (var state : java.util.List.of(root, root.withQualityInfused(true), sanctified)) {
      var payload = wire.valueToTree(state);
      assertThat(new WorkbenchStateReader(crafting, wire).concreteState(payload)).isEqualTo(state);
      assertThat(new WorkbenchStateReader(crafting, strict).concreteState(payload))
          .isEqualTo(state);
    }
    assertThat(wire.valueToTree(root).has("qualityInfused")).isFalse();
    assertThat(ItemStateWireDefaults.ITEM_FLAGS).contains("qualityInfused");
    var invalid = (com.fasterxml.jackson.databind.node.ObjectNode) wire.valueToTree(root);
    invalid.putNull("qualityInfused");
    assertThatThrownBy(() -> new WorkbenchStateReader(crafting, strict).concreteState(invalid))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
