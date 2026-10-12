package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * A disjoint product cell of the declared independent single-stat roll model, not an averaged entry
 * law.
 */
public record DeferredItem(BasicCurrencyState representative, Map<String, Band> rolls) {
  public static final String VERSION = "deferred-independent-single-stat-v1";

  public record Band(String stat, long min, long max) {
    public Band {
      if (stat == null || stat.isBlank() || min >= max)
        throw new IllegalArgumentException("Non-singleton roll band required");
    }

    public BigInteger size() {
      return BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
    }
  }

  public DeferredItem(BasicCurrencyState state) {
    this(state, Map.of());
  }

  public DeferredItem {
    Objects.requireNonNull(representative);
    rolls = Map.copyOf(rolls);
    for (var entry : rolls.entrySet()) {
      var modifier =
          representative.item().explicits().stream()
              .filter(m -> m.modifierId().equals(entry.getKey()))
              .findFirst()
              .orElseThrow();
      if (modifier.values().size() != 1
          || !Objects.equals(
              modifier.values().get(entry.getValue().stat()), entry.getValue().min()))
        throw new IllegalArgumentException("Roll band must match its representative");
    }
  }

  public ItemState item() {
    return representative.item();
  }

  public BasicCurrencyState.Provenance provenance() {
    return representative.provenance();
  }

  public BigInteger cardinality() {
    return rolls.values().stream().map(Band::size).reduce(BigInteger.ONE, BigInteger::multiply);
  }

  /** Random access into a cell permits revision-pinned pages without materializing its product. */
  public BasicCurrencyState concrete(BigInteger offset) {
    if (offset.signum() < 0 || offset.compareTo(cardinality()) >= 0)
      throw new IllegalArgumentException("Cell offset out of range");
    var values = new HashMap<String, Long>();
    for (var entry : new TreeMap<>(rolls).entrySet()) {
      var divided = offset.divideAndRemainder(entry.getValue().size());
      values.put(
          entry.getKey(),
          BigInteger.valueOf(entry.getValue().min()).add(divided[1]).longValueExact());
      offset = divided[0];
    }
    var explicit =
        item().explicits().stream()
            .map(
                m ->
                    rolls.containsKey(m.modifierId())
                        ? new ModifierInstance(
                            m.modifierId(),
                            Map.of(rolls.get(m.modifierId()).stat(), values.get(m.modifierId())),
                            m.fractured())
                        : m)
            .toList();
    return new BasicCurrencyState(
        new ItemState(
            item().snapshotId(),
            item().baseItemId(),
            item().itemLevel(),
            item().rarity(),
            item().implicits(),
            explicit,
            item().conditions(),
            item().augmentSockets(),
            item().catalystQuality()),
        provenance());
  }

  public String canonicalKey() {
    if (rolls.isEmpty()) return representative.canonicalKey();
    try {
      var text =
          "deferred-roll-cell-v1:" + representative.canonicalKey() + ":" + new TreeMap<>(rolls);
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
