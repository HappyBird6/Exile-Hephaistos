package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedLawStoreTest {
  @Test
  void everyEndpointAndClassSurvivesLazyPagingAtEveryPrecision() {
    for (int bits : new int[] {16, 17, 32, 64, 65, 128, 256}) {
      try (var store = new CertifiedLawStore(bits)) {
        var source = new LinkedHashMap<Integer, CertifiedInterval>();
        for (int i = 0; i < 2500; i++)
          source.put(
              i * 19,
              new CertifiedInterval(
                  bits, BigInteger.valueOf(i % 7), BigInteger.valueOf(i % 7 + 1)));
        source.put(Integer.MAX_VALUE, CertifiedInterval.zero(bits));
        var frozen = store.freeze(source);
        assertThat(store.metrics().materializedRows()).isZero();
        assertThat(frozen).isEqualTo(source);
        assertThat(new LinkedHashMap<>(frozen)).isEqualTo(source);
        assertThat(frozen.get(-1)).isNull();
        assertThat(frozen.get(1)).isNull();
        assertThat(frozen.get("0")).isNull();
        assertThat(store.metrics().materializedRows()).isLessThanOrEqualTo(1024);
        assertThat(CertifiedLawStore.total(frozen, bits))
            .isEqualTo(CertifiedLawStore.total(source, bits));
        assertThat(CertifiedLawStore.total(frozen.values(), bits))
            .isEqualTo(CertifiedLawStore.total(source, bits));
        assertThatThrownBy(() -> frozen.put(0, CertifiedInterval.one(bits)))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(
                () -> frozen.entrySet().iterator().next().setValue(CertifiedInterval.one(bits)))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(store.freeze(frozen)).isSameAs(frozen);
        var one = store.freeze(Map.of(7, CertifiedInterval.one(bits)));
        assertThat(one.get(7)).isEqualTo(CertifiedInterval.one(bits));
      }
    }
  }

  @Test
  void completePageEqualitySharesStorageAndCloseInvalidatesReaders() {
    var store = new CertifiedLawStore(64);
    var source = new HashMap<Integer, CertifiedInterval>();
    for (int i = 0; i < 1024; i++)
      source.put(
          i,
          CertifiedInterval.enclose(
              new com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction(
                  BigInteger.ONE, BigInteger.valueOf(2048)),
              64));
    var first = store.freeze(source);
    long bytes = store.packedBytes();
    var second = store.freeze(new TreeMap<>(source));
    assertThat(store.packedBytes()).isEqualTo(bytes);
    assertThat(store.metrics().reusedPages()).isEqualTo(4);
    source.put(1000, CertifiedInterval.zero(64));
    var changed = store.freeze(source);
    assertThat(store.metrics().uniquePages()).isEqualTo(5);
    assertThat(changed.get(1000)).isNotEqualTo(first.get(1000));
    assertThat(second).isEqualTo(first);
    var reader = first.entrySet().iterator();
    store.close();
    assertThat(store.packedBytes()).isZero();
    assertThat(store.metrics().materializedRows()).isZero();
    assertThatThrownBy(reader::hasNext).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> first.get(0)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> store.freeze(Map.of())).isInstanceOf(IllegalStateException.class);
  }
}
