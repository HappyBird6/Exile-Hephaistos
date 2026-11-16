package com.poe2craft.craftingsupport.rag;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.rag.application.RagComparisonService;
import com.poe2craft.craftingsupport.search.application.*;
import org.junit.jupiter.api.Test;

class AutomaticSearchInputReaderTest {
  final RagFixture f = new RagFixture();
  final AutomaticSearchInputReader reader = new AutomaticSearchInputReader(f.json);

  @Test
  void canonicalAutomaticAndRagInputsRetainIdenticalStatesAndGoalDefaults() throws Exception {
    var input = f.input();
    var tree = (ObjectNode) f.json.valueToTree(input);
    ((ObjectNode) tree.path("request").path("goal").path("general")).remove("quality");
    var read = reader.read(f.json.writeValueAsString(tree), RagComparisonService.Input.class);
    var original =
        reader.read(
            f.json.writeValueAsString(new AutomaticSearchService.Input(input.request(), false)),
            AutomaticSearchService.Input.class);
    assertThat(read.request().start()).isEqualTo(original.request().start());
    assertThat(read.request().goal()).isEqualTo(original.request().goal());
  }

  @Test
  void unsupportedStateIsRejectedByBothPathsBeforeAnyModelCall() throws Exception {
    var input = f.input();
    for (var wrapper :
        java.util.List.<com.fasterxml.jackson.databind.JsonNode>of(
            f.json.valueToTree(input),
            f.json.valueToTree(new AutomaticSearchService.Input(input.request(), false)))) {
      ((ObjectNode) wrapper.path("request").path("start").path("item")).put("equipmentQuality", 20);
      var body = f.json.writeValueAsString(wrapper);
      var type =
          wrapper.has("mode")
              ? RagComparisonService.Input.class
              : AutomaticSearchService.Input.class;
      assertThatThrownBy(() -> reader.read(body, type))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
  }
}
