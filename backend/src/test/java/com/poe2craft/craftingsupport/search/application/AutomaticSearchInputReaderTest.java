package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticSearchInputReaderTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final BasicCurrencyFixture fixture = new BasicCurrencyFixture();
  final AutomaticSearchInputReader reader = new AutomaticSearchInputReader(json);

  AutomaticSearchService.Input input() {
    var root = fixture.item(ItemState.Rarity.RARE, List.of());
    var goal =
        new GoalFilter(
            1,
            "test-goal",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of());
    return new AutomaticSearchService.Input(
        new MethodSearchProtocol.Create(
            2, UUID.randomUUID().toString(), fixture.state(root), goal, List.of()),
        false);
  }

  @Test
  void canonicalInputRetainsStateAndOptionalGoalDefaults() throws Exception {
    var original = input();
    var body = (ObjectNode) json.valueToTree(original);
    ((ObjectNode) body.path("request").path("goal").path("general")).remove("quality");
    assertThat(reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isEqualTo(original);
  }

  @Test
  void acceptsOmittedOptionalBooleanFlagsFromTheBrowser() throws Exception {
    var original = input();
    var body = (ObjectNode) json.valueToTree(original);
    var item = (ObjectNode) body.path("request").path("start").path("item");
    item.remove("qualityInfused");
    item.path("implicits").forEach(m -> ((ObjectNode) m).remove(List.of("crafted", "fractured")));
    assertThat(reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isEqualTo(original);
  }

  @Test
  void explicitInvalidBooleanFlagsRemainRejected() throws Exception {
    for (var invalid : List.of("null", "\"false\"", "0")) {
      var body = (ObjectNode) json.valueToTree(input());
      ((ObjectNode) body.path("request").path("start").path("item"))
          .set("qualityInfused", json.readTree(invalid));
      assertThatThrownBy(
              () -> reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
  }

  @Test
  void unsupportedStateRemainsRejected() throws Exception {
    var body = (ObjectNode) json.valueToTree(input());
    ((ObjectNode) body.path("request").path("start").path("item")).put("equipmentQuality", 20);
    assertThatThrownBy(
            () -> reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isInstanceOf(PathSearchProtocol.Rejected.class);
  }
}
