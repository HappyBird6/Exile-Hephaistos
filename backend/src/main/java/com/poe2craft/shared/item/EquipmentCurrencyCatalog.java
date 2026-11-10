package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Source-reviewed equipment outcomes, shared by all bases; ordinary spawn weights stay unchanged.
 */
public final class EquipmentCurrencyCatalog {
  private EquipmentCurrencyCatalog() {}

  public record Document(
      List<ModifierDefinition> definitions, Map<String, Map<String, List<String>>> targets) {}

  private static final Document DATA;

  static {
    try (var input = SealedResources.open("/catalog/equipment-currencies.json")) {
      DATA = new ObjectMapper().readValue(input, Document.class);
      var definitions = new HashMap<String, ModifierDefinition>();
      for (var d : DATA.definitions()) {
        if (d.weight() != 0
            || d.layer() != ModifierDefinition.Layer.EXPLICIT
            || definitions.put(d.id(), d) != null)
          throw new IllegalArgumentException("Invalid equipment currency definition");
      }
      DATA.targets()
          .forEach(
              (base, actions) ->
                  actions.forEach(
                      (action, ids) -> {
                        if (ids.isEmpty()
                            || new HashSet<>(ids).size() != ids.size()
                            || !definitions.keySet().containsAll(ids))
                          throw new IllegalArgumentException("Invalid equipment currency targets");
                      }));
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static Map<String, List<String>> targets(String base) {
    return DATA.targets().getOrDefault(base, Map.of());
  }

  public static ItemCatalog enrich(ItemCatalog catalog) {
    var ids = new LinkedHashSet<String>();
    targets(catalog.base().id()).values().forEach(ids::addAll);
    ids.removeAll(catalog.modifiers().keySet());
    if (ids.isEmpty()) return AugmentCraftingRules.enrich(catalog);
    var definitions = new ArrayList<>(catalog.modifiers().values());
    DATA.definitions().stream().filter(d -> ids.contains(d.id())).forEach(definitions::add);
    var old = catalog.metadata();
    String digest;
    try {
      digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest((old.snapshotId() + ids).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    var metadata =
        new ItemCatalog.Metadata(
            "equipment-currency-" + digest,
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            (int)
                definitions.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count(),
            (int)
                definitions.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                    .count(),
            old.prefixWeight(),
            old.suffixWeight());
    var compatible = new ArrayList<>(catalog.compatibleSnapshotIds());
    compatible.add(old.snapshotId());
    return AugmentCraftingRules.enrich(
        new ItemCatalog(metadata, catalog.base(), definitions, compatible));
  }
}
