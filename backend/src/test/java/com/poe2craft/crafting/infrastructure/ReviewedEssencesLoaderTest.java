package com.poe2craft.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import org.junit.jupiter.api.Test;

class ReviewedEssencesLoaderTest {
  private final ObjectMapper mapper = new ObjectMapper();

  private ObjectNode data() throws IOException {
    return (ObjectNode)
        mapper.readTree(getClass().getResourceAsStream("/catalog/top-base-essences.json"));
  }

  private Object read(JsonNode data) throws IOException {
    return ReviewedEssencesLoader.read(new ByteArrayInputStream(mapper.writeValueAsBytes(data)));
  }

  @Test
  void allTargetIdsAndTheirOrderRemainExact() throws Exception {
    assertEquals(data(), mapper.valueToTree(ReviewedEssencesLoader.load()));
  }

  @Test
  void wrongShapesCoercedIdsUnknownFieldsAndMissingBasesAreRejected() throws Exception {
    for (JsonNode invalid :
        java.util.List.of(mapper.createArrayNode(), mapper.getNodeFactory().textNode("bad")))
      assertThrows(IOException.class, () -> read(invalid));
    var wrong = data();
    ((ObjectNode) wrong.elements().next()).set("fixed", mapper.createArrayNode());
    assertThrows(IOException.class, () -> read(wrong));
    var scalar = data();
    var targets = (ObjectNode) scalar.elements().next().get("fixed");
    targets.set("ESSENCE_HASTE", mapper.createArrayNode().add(42));
    assertThrows(IOException.class, () -> read(scalar));
    var missing = data();
    missing.remove(missing.fieldNames().next());
    assertThrows(IllegalArgumentException.class, () -> read(missing));
    var typo = data();
    ((ObjectNode) typo.elements().next()).put("fixxed", true);
    assertThrows(IOException.class, () -> read(typo));
    assertThrows(
        IOException.class,
        () ->
            ReviewedEssencesLoader.read(
                new ByteArrayInputStream(
                    "{\"same\":{},\"same\":{}}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }
}
