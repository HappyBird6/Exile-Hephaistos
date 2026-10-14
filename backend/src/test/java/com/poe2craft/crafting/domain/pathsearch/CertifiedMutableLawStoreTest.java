package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedMutableLawStoreTest {
  @Test
  void evictionAndRepeatedUpdatesPreserveEveryEndpointAcrossIndependentLaws() {
    for (int bits : List.of(16, 64, 256)) {
      try (var store = new CertifiedMutableLawStore(bits, 2)) {
        var first = store.create();
        var second = store.create();
        var expected = new TreeMap<Integer, CertifiedInterval>();
        var random = new Random(271);
        for (int round = 0; round < 3; round++) {
          for (int i = 0; i < 1600; i++) {
            int key = i * 13;
            var delta =
                CertifiedInterval.enclose(
                    new Fraction(
                        BigInteger.valueOf(random.nextInt(100)), BigInteger.valueOf(10000)),
                    bits);
            expected.merge(key, delta, (a, b) -> a.add(b).probability());
            first.merge(key, delta, (a, b) -> a.add(b).probability());
            second.put(Integer.MAX_VALUE - key, CertifiedInterval.one(bits));
          }
        }
        first.put(Integer.MAX_VALUE, CertifiedInterval.zero(bits));
        expected.put(Integer.MAX_VALUE, CertifiedInterval.zero(bits));
        assertThat(new TreeMap<>(first)).isEqualTo(expected);
        assertThat(store.metrics().decodedRows()).isLessThanOrEqualTo(512);
        assertThat(store.metrics().packedBytes()).isPositive();
        assertThat(store.metrics().evictions()).isPositive();
        assertThat(store.metrics().rows()).isEqualTo(first.size() + second.size());
        var iterator = first.entrySet().iterator();
        assertThat(iterator.hasNext()).isTrue();
        second.values().forEach(value -> assertThat(value).isEqualTo(CertifiedInterval.one(bits)));
        assertThat(iterator.next()).isEqualTo(expected.firstEntry());
        first.close();
        assertThatThrownBy(first::size).isInstanceOf(IllegalStateException.class);
        assertThat(second).hasSize(1600);
        second.clear();
        assertThat(store.metrics().rows()).isZero();
        assertThat(store.metrics().packedBytes()).isZero();
        assertThat(store.metrics().decodedRows()).isZero();
      }
    }
  }

  @Test
  void pagedAbsorptionMatchesOrdinaryRunnerAndCanBeFrozenBeforeRelease() {
    try (var working = new CertifiedMutableLawStore(64, 1);
        var saved = new CertifiedLawStore(64)) {
      var outcomes = new ArrayList<Outcome<Integer>>();
      var mass = new Fraction(BigInteger.ONE, BigInteger.valueOf(2048));
      for (int i = 1; i <= 2048; i++) outcomes.add(new Outcome<>(i, mass));
      Transition<Integer, String> transition =
          (state, action) -> new Kernel<>(outcomes, Fraction.ZERO, false);
      var template = new Template<Integer, String>("absorbing", List.of(), List.of(s -> "add"));
      java.util.function.Function<Integer, Optional<Exit>> classify =
          state -> state == 0 ? Optional.empty() : Optional.of(Exit.GOAL);
      var plain =
          new CertifiedMethodRunner<>(
              0, template, transition, classify, new CertifiedInterval.Precision(64, 2));
      var paged =
          new CertifiedMethodRunner<>(
              0,
              template,
              transition,
              classify,
              new CertifiedInterval.Precision(64, 2),
              working::create);
      plain.beginCycle();
      paged.beginCycle();
      while (!paged.atBoundary()) {
        paged.step(7);
        plain.step(7);
      }
      assertThat(paged.observation()).isEqualTo(plain.observation());
      assertThat(paged.retainedStates()).isZero();
      assertThat(working.metrics().rows()).isEqualTo(2048);
      assertThat(working.metrics().decodedRows()).isLessThanOrEqualTo(256);
      var lease = paged.leaseObservation();
      var frozen = saved.freeze(lease.point().exits().get(Exit.GOAL));
      lease.close();
      working.close();
      assertThat(frozen).hasSize(2048);
      assertThat(CertifiedLawStore.total(frozen, 64)).isEqualTo(CertifiedInterval.one(64));
      assertThat(frozen.values()).allMatch(value -> value.exact() && value.contains(mass));
    }
  }
}
