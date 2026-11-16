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
  void acceptsOmittedOptionalBooleanFlagsFromTheBrowser() throws Exception {
    var body = (ObjectNode) f.json.valueToTree(f.input());
    var item = (ObjectNode) body.path("request").path("start").path("item");
    item.remove("qualityInfused");
    item.path("implicits").forEach(m -> ((ObjectNode) m).remove("crafted"));
    assertThat(
            reader
                .read(f.json.writeValueAsString(body), RagComparisonService.Input.class)
                .request()
                .start()
                .item())
        .isEqualTo(f.input().request().start().item());
  }

  @Test
  void explicitInvalidBooleanFlagsAreRejectedByBothPaths() throws Exception {
    for (var wrapper :
        java.util.List.<com.fasterxml.jackson.databind.JsonNode>of(
            f.json.valueToTree(f.input()),
            f.json.valueToTree(new AutomaticSearchService.Input(f.input().request(), false)))) {
      for (var invalid : java.util.List.of("null", "\"false\"", "0")) {
        var body = wrapper.deepCopy();
        ((ObjectNode) body.path("request").path("start").path("item"))
            .set("qualityInfused", f.json.readTree(invalid));
        var type =
            body.has("mode")
                ? RagComparisonService.Input.class
                : AutomaticSearchService.Input.class;
        assertThatThrownBy(() -> reader.read(f.json.writeValueAsString(body), type))
            .isInstanceOf(PathSearchProtocol.Rejected.class);
      }
    }
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
