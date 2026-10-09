package com.poe2craft.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.ModifierDefinition;
import java.io.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorkbenchDefinitionsLoaderTest {
  private static final ObjectMapper M = new ObjectMapper();

  private byte[] fixture() throws IOException {
    try (var in = getClass().getResourceAsStream("/workbench-definitions-pre-extraction.json")) {
      return in.readAllBytes();
    }
  }

  private ObjectNode data() throws IOException {
    return (ObjectNode) M.readTree(fixture());
  }

  private WorkbenchDefinitions read(JsonNode data) throws IOException {
    return WorkbenchDefinitionsLoader.read(new ByteArrayInputStream(M.writeValueAsBytes(data)));
  }

  @Test
  void everyEnumGetterNullExceptionAndTargetOrderMatchesThePriorRuntimeOracle() throws Exception {
    assertEquals(
        "8d050b94aeb67a475c24eb767a0700f1c6441d169d6d697c290013cfc5b15d93",
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fixture())));
    var expected = WorkbenchDefinitionsLoader.read(new ByteArrayInputStream(fixture()));
    var actual = WorkbenchDefinitionsLoader.initialize();
    assertEquals(expected.document(), actual.document());
    assertEquals(160, actual.document().currencies().size());
    assertEquals(18, actual.document().omens().size());
    for (var row : expected.document().currencies()) {
      var c = row.id();
      assertEquals(row.baseAction(), c.baseAction());
      assertEquals(row.minimumModifierLevel(), c.minimumModifierLevel());
      assertEquals(row.fixedModifierId(), c.fixedModifierId());
      assertEquals(row.essenceModifierIds(), c.essenceModifierIds());
      assertEquals(row.essenceChoiceSource(), c.essenceChoiceSource());
      assertEquals(row.replacementModifiers(), c.replacementModifiers());
      assertEquals(
          c.isAlloy() || c.isLiquid() ? List.of() : row.replacementModifiers(),
          c.replacementEssenceModifiers());
      if (row.replacementEssenceSource() == null)
        assertThrows(IllegalArgumentException.class, c::replacementEssenceSource);
      else assertEquals(row.replacementEssenceSource(), c.replacementEssenceSource());
      if (row.replacementSource() == null)
        assertThrows(IllegalArgumentException.class, c::replacementSource);
      else assertEquals(row.replacementSource(), c.replacementSource());
    }
    for (var row : expected.document().omens()) {
      var o = row.key();
      assertEquals(row.id(), o.id());
      assertEquals(row.trigger(), o.trigger());
      assertEquals(row.affix(), o.affix());
      assertEquals(row.supportsTieredCurrency(), o.supportsTieredCurrency());
      assertSame(o, WorkbenchOmen.fromId(row.id()));
    }
    assertThrows(UnsupportedOperationException.class, () -> actual.document().currencies().clear());
    assertThrows(
        UnsupportedOperationException.class,
        () -> WorkbenchCurrency.ESSENCE_INFINITE.essenceModifierIds().clear());
  }

  @Test
  void missingDuplicateAndReorderedIdentitiesAreRejected() throws Exception {
    for (String field : List.of("currencies", "omens")) {
      var missing = data();
      ((ArrayNode) missing.get(field)).remove(0);
      assertThrows(IllegalArgumentException.class, () -> read(missing));
      var duplicate = data();
      ((ArrayNode) duplicate.get(field)).add(duplicate.get(field).get(0).deepCopy());
      assertThrows(IllegalArgumentException.class, () -> read(duplicate));
      var reordered = data();
      var values = (ArrayNode) reordered.get(field);
      var first = values.remove(0);
      values.add(first);
      assertThrows(IllegalArgumentException.class, () -> read(reordered));
    }
  }

  @Test
  void unknownFieldsCoercionAndUnimplementedApplicabilityAreRejected() throws Exception {
    var unknown = data();
    ((ObjectNode) unknown.get("currencies").get(0)).put("minimumModifierLevell", 0);
    assertThrows(IOException.class, () -> read(unknown));
    var fraction = data();
    fraction.put("schemaVersion", 1.5);
    assertThrows(IOException.class, () -> read(fraction));
    var ordinal = data();
    ((ObjectNode) ordinal.get("currencies").get(0)).put("baseAction", 0);
    assertThrows(IOException.class, () -> read(ordinal));
    var unknownTrigger = data();
    ((ObjectNode) unknownTrigger.get("omens").get(0)).put("trigger", "NOT_IMPLEMENTED");
    assertThrows(IOException.class, () -> read(unknownTrigger));
    var unsupported = data();
    ((ObjectNode) unsupported.get("omens").get(0)).put("supportsTieredCurrency", true);
    assertThrows(IllegalArgumentException.class, () -> read(unsupported));
    var wrongOperation = data();
    ((ObjectNode) wrongOperation.get("currencies").get(0)).put("baseAction", "EXALTED");
    assertThrows(IllegalArgumentException.class, () -> read(wrongOperation));
  }

  @Test
  void unknownTargetsAndWrongTargetLayersFailBeforeBootstrap() throws Exception {
    var known = new HashMap<>(WorkbenchDefinitionsLoader.readTargets());
    var bad = data();
    var row = (ObjectNode) bad.get("currencies").get(56);
    assertEquals("LESSER_ESSENCE_COMMAND", row.get("id").asText());
    row.put("fixedModifierId", "missing:target");
    row.putArray("essenceModifierIds").add("missing:target");
    assertThrows(IllegalArgumentException.class, () -> read(bad).validateTargets(known));
    var ordinary = known.get("rattling-sceptre:prefix:agitative").getFirst();
    var changed = (ObjectNode) M.valueToTree(ordinary);
    changed.put("layer", "IMPLICIT");
    changed.put("affixType", "NONE");
    changed.put("tier", 0);
    changed.put("weight", 0);
    known.put(ordinary.id(), List.of(M.treeToValue(changed, ModifierDefinition.class)));
    assertThrows(IllegalArgumentException.class, () -> read(data()).validateTargets(known));
  }

  @Test
  void dataOnlyLevelEditsAreTypedButCannotReplaceTheActiveBundleInPlace() throws Exception {
    var updated = data();
    for (var c : updated.get("currencies"))
      if (c.get("id").asText().equals("GREATER_TRANSMUTATION"))
        ((ObjectNode) c).put("minimumModifierLevel", 45);
    var definitions = read(updated);
    assertEquals(
        45, definitions.currency(WorkbenchCurrency.GREATER_TRANSMUTATION).minimumModifierLevel());
    assertThrows(IllegalStateException.class, () -> WorkbenchDefinitions.initialize(definitions));
    assertEquals(44, WorkbenchCurrency.GREATER_TRANSMUTATION.minimumModifierLevel());
  }
}
