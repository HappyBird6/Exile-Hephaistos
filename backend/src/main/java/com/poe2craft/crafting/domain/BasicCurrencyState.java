package com.poe2craft.crafting.domain;

import com.poe2craft.item.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Lossless state plus provenance; no goal-dependent projection or legacy relabeling. */
public record BasicCurrencyState(ItemState item, Provenance provenance) {
  public BasicCurrencyState {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(provenance, "provenance");
  }

  public record Provenance(
      String rulesetIdentity,
      String catalogDigest,
      String modelVersion,
      String ruleVersion,
      String ledgerVersion,
      String weightPolicy,
      String sourceUrl,
      String retrievedAt,
      String rawSha256,
      String detailsSha256) {
    public Provenance {
      for (String value :
          List.of(
              rulesetIdentity,
              catalogDigest,
              modelVersion,
              ruleVersion,
              ledgerVersion,
              weightPolicy,
              sourceUrl,
              retrievedAt,
              rawSha256,
              detailsSha256))
        if (value.isBlank()) throw new IllegalArgumentException("Explicit provenance is required");
      if (!catalogDigest.matches("[0-9a-f]{64}")
          || !rawSha256.matches("[0-9a-f]{64}")
          || !detailsSha256.matches("[0-9a-f]{64}"))
        throw new IllegalArgumentException("Invalid catalog provenance digest");
    }
  }

  /**
   * Digest includes actual catalog contents and ordered source arrays, not just claimed metadata.
   */
  static String catalogDigest(ItemCatalog catalog) {
    return digest(
        out -> {
          string(out, "basic-catalog-key-v1");
          var m = catalog.metadata();
          strings(
              out,
              List.of(
                  m.snapshotId(),
                  m.retrievedAt(),
                  m.sourceUrl(),
                  m.weightPolicy(),
                  m.rawSha256(),
                  m.detailsSha256()));
          out.writeInt(m.prefixCount());
          out.writeInt(m.suffixCount());
          out.writeLong(m.prefixWeight());
          out.writeLong(m.suffixWeight());
          var b = catalog.base();
          strings(out, List.of(b.id(), b.name(), b.sourceUrl(), b.implicitModifierId()));
          out.writeInt(b.magicPrefixes());
          out.writeInt(b.magicSuffixes());
          out.writeInt(b.rarePrefixes());
          out.writeInt(b.rareSuffixes());
          strings(out, catalog.compatibleSnapshotIds());
          out.writeInt(catalog.modifiers().size());
          for (var d : catalog.modifiers().values()) {
            strings(
                out,
                List.of(
                    d.id(),
                    d.name(),
                    d.layer().name(),
                    d.affixType() == null ? "" : d.affixType().name()));
            strings(out, d.familyIds().stream().sorted().toList());
            out.writeInt(d.requiredItemLevel());
            out.writeLong(d.weight());
            out.writeInt(d.tier());
            string(out, d.text());
            out.writeInt(d.stats().size());
            for (var s : d.stats()) {
              string(out, s.id());
              out.writeLong(s.min());
              out.writeLong(s.max());
            }
            strings(out, d.tags().stream().sorted().toList());
            string(out, d.sourceUrl());
          }
        });
  }

  /**
   * Length-prefixed binary fields avoid separator/toString collisions and retain unknown sockets.
   */
  public String canonicalKey() {
    return digest(
        out -> {
          string(out, "basic-full-state-key-v1");
          strings(
              out,
              List.of(
                  provenance.rulesetIdentity(),
                  provenance.catalogDigest(),
                  provenance.modelVersion(),
                  provenance.ruleVersion(),
                  provenance.ledgerVersion(),
                  provenance.weightPolicy(),
                  provenance.sourceUrl(),
                  provenance.retrievedAt(),
                  provenance.rawSha256(),
                  provenance.detailsSha256()));
          strings(out, List.of(item.snapshotId(), item.baseItemId(), item.rarity().name()));
          out.writeInt(item.itemLevel());
          modifiers(out, item.implicits());
          modifiers(out, item.explicits());
          strings(out, item.conditions().stream().map(Enum::name).sorted().toList());
          out.writeBoolean(item.augmentSockets() != null);
          if (item.augmentSockets() != null) out.writeInt(item.augmentSockets());
          out.writeBoolean(item.catalystQuality() != null);
          if (item.catalystQuality() != null) {
            string(out, item.catalystQuality().type().name());
            out.writeInt(item.catalystQuality().amount());
          }
        });
  }

  private static void modifiers(DataOutputStream out, List<ModifierInstance> modifiers)
      throws IOException {
    out.writeInt(modifiers.size());
    for (var modifier : modifiers) {
      string(out, modifier.modifierId());
      out.writeBoolean(modifier.fractured());
      out.writeInt(modifier.values().size());
      for (var entry : new TreeMap<>(modifier.values()).entrySet()) {
        string(out, entry.getKey());
        out.writeLong(entry.getValue());
      }
    }
  }

  private static void strings(DataOutputStream out, List<String> values) throws IOException {
    out.writeInt(values.size());
    for (var value : values) string(out, value);
  }

  private static void string(DataOutputStream out, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String digest(Encoder encoder) {
    try {
      var bytes = new ByteArrayOutputStream();
      try (var out = new DataOutputStream(bytes)) {
        encoder.write(out);
      }
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Cannot encode transition identity", e);
    }
  }

  private interface Encoder {
    void write(DataOutputStream out) throws IOException;
  }
}
