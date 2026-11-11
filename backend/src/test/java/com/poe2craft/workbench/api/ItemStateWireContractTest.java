package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.item.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

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
    for (var state : java.util.List.of(root, root.withQualityInfused(true))) {
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
