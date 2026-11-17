package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedLawStore;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 작업 소유의 불변 분포 페이지를 파일에 두고 작은 캐시만 메모리에 유지한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code hotCacheSpillsLazilyAndRepeatedReadsDoNotAppendDuplicatePayloads()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code completeLawRowsEndpointsAndDeduplicationSurviveSpillAtEveryPrecision()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code unavailableOrTruncatedBackingFailsExplicitlyWithoutInventingProbability()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class TemporaryLawPagesTest {
  @TempDir Path directory;

  @Test
  void hotCacheSpillsLazilyAndRepeatedReadsDoNotAppendDuplicatePayloads() throws Exception {
    var keep = Files.writeString(directory.resolve("unrelated.txt"), "keep");
    var backing = new TemporaryLawPages(directory, 512);
    var payloads = new ArrayList<CertifiedLawStore.PackedPages.Payload>();
    var expected = new ArrayList<byte[]>();
    var random = new Random(44391);
    for (int i = 0; i < 40; i++) {
      var bytes = new byte[128];
      random.nextBytes(bytes);
      expected.add(bytes.clone());
      payloads.add(backing.store(bytes));
      assertThat(backing.residentBytes()).isLessThanOrEqualTo(512);
      if (i < 4) assertThat(backing.diskBytes()).isZero();
    }
    for (int repeat = 0; repeat < 4; repeat++)
      for (int i = 0; i < payloads.size(); i++) {
        assertThat(payloads.get(i).read()).isEqualTo(expected.get(i));
        assertThat(backing.residentBytes()).isLessThanOrEqualTo(512);
      }
    assertThat(backing.diskBytes()).isEqualTo(40 * 128);
    backing.close();
    backing.close();
    assertThat(backing.residentBytes()).isZero();
    assertThat(backing.diskBytes()).isZero();
    assertThatThrownBy(() -> payloads.getFirst().read()).isInstanceOf(IllegalStateException.class);
    try (var files = Files.list(directory)) {
      assertThat(files.toList()).containsExactly(keep);
    }
  }

  @Test
  void completeLawRowsEndpointsAndDeduplicationSurviveSpillAtEveryPrecision() throws Exception {
    for (int bits : new int[] {16, 64, 256}) {
      var backing = new TemporaryLawPages(directory, 1024);
      try (var store = new CertifiedLawStore(bits, backing)) {
        var source = new LinkedHashMap<Integer, CertifiedInterval>();
        for (int i = 8191; i >= 0; i--)
          source.put(i * 113, new CertifiedInterval(bits, BigInteger.ONE, BigInteger.TWO));
        source.put(Integer.MAX_VALUE, CertifiedInterval.zero(bits));
        var first = store.freeze(source);
        long bytes = store.packedBytes();
        var second = store.freeze(new TreeMap<>(source));
        assertThat(store.packedBytes()).isEqualTo(bytes);
        assertThat(store.metrics().reusedPages()).isEqualTo(33);
        assertThat(store.metrics().residentPackedBytes()).isLessThanOrEqualTo(1024);
        assertThat(store.metrics().diskBytes()).isPositive();
        for (int offset = 0; offset < first.size(); offset += 47)
          assertThat(CertifiedLawStore.page(first, offset, 47))
              .isEqualTo(new TreeMap<>(source).entrySet().stream().skip(offset).limit(47).toList());
        assertThat(first).isEqualTo(source);
        assertThat(second).isEqualTo(source);
        assertThat(CertifiedLawStore.total(first, bits))
            .isEqualTo(CertifiedLawStore.total(source, bits));
        assertThat(store.metrics().materializedRows()).isLessThanOrEqualTo(1024);
        source.put(0, CertifiedInterval.zero(bits));
        var changed = store.freeze(source);
        assertThat(changed).isEqualTo(source);
        assertThat(first.get(0))
            .isEqualTo(new CertifiedInterval(bits, BigInteger.ONE, BigInteger.TWO));
        assertThat(store.metrics().uniquePages()).isEqualTo(34);
      }
      try (var files = Files.list(directory)) {
        assertThat(files.toList()).isEmpty();
      }
    }
  }

  @Test
  void unavailableOrTruncatedBackingFailsExplicitlyWithoutInventingProbability() throws Exception {
    var blocked = Files.writeString(directory.resolve("file-not-directory"), "keep");
    try (var backing = new TemporaryLawPages(blocked, 0)) {
      assertThatThrownBy(() -> backing.store(new byte[] {1, 2}))
          .isInstanceOf(UncheckedIOException.class);
    }
    try (var backing = new TemporaryLawPages(directory, 0)) {
      var payload = backing.store(new byte[] {1, 2, 3});
      Path path;
      try (var files = Files.list(directory)) {
        path = files.filter(p -> p.toString().endsWith(".pages")).findFirst().orElseThrow();
      }
      try (var file = FileChannel.open(path, StandardOpenOption.WRITE)) {
        file.truncate(1);
      }
      assertThatThrownBy(payload::read).isInstanceOf(UncheckedIOException.class);
    }
    assertThat(Files.readString(blocked)).isEqualTo("keep");
  }
}
