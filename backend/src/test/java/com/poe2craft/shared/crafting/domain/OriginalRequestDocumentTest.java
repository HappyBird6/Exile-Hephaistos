package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import java.util.List;
import org.junit.jupiter.api.Test;

class OriginalRequestDocumentTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  @Test
  void historicalDocumentsRetainTheirExactJsonShapeAndFieldOrder() throws Exception {
    for (var path :
        List.of(
            "/legacy-v8/complete.json",
            "/legacy-v10/quality30-disk.json",
            "/legacy-v10/chaos-disk.json")) {
      var source = json.readTree(getClass().getResourceAsStream(path)).path("request");
      var typed = json.treeToValue(source, MethodSearchProtocol.Create.class);
      var restored = typed.withOriginalDocument(source);
      assertThat(restored).isEqualTo(typed);
      assertThat(json.writeValueAsString(restored)).isEqualTo(json.writeValueAsString(source));
    }
  }

  @Test
  void retainedSourceIsImmutableAndNotPartOfExecutionIdentity() throws Exception {
    var request = new MethodSearchProtocol.Create(2, "original", null, null, List.of());
    var source = (ObjectNode) json.valueToTree(request);
    var retained = request.withOriginalDocument(source);
    source.put("clientRequestId", "changed");
    assertThat(retained).isEqualTo(request).hasSameHashCodeAs(request);
    var exported = (ObjectNode) retained.serializedDocument();
    exported.put("clientRequestId", "changed-again");
    assertThat(json.valueToTree(retained).path("clientRequestId").asText()).isEqualTo("original");
  }

  @Test
  void currentRequestShapeAndUnknownFieldRejectionRemainUnchanged() throws Exception {
    var request = new MethodSearchProtocol.Create(2, "current", null, null, List.of());
    var document = json.valueToTree(request);
    assertThat(document.size()).isEqualTo(5);
    assertThat(document.has("start")).isTrue();
    assertThat(document.get("start").isNull()).isTrue();
    assertThat(json.treeToValue(document, MethodSearchProtocol.Create.class)).isEqualTo(request);
    ((ObjectNode) document).put("originalDocument", "untrusted");
    assertThatThrownBy(() -> json.treeToValue(document, MethodSearchProtocol.Create.class))
        .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
  }
}
