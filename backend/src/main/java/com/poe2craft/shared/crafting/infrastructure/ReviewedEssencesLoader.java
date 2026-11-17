package com.poe2craft.shared.crafting.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.*;
import java.util.*;

/**
 * 검토된 상위 베이스의 에센스 대상 목록을 읽는다. 개별 옵션 적합성은 시뮬레이터가 검증한다.
 *
 * <p>private 생성자는 생성을 막는다. load는 Loaded.TARGETS를 공유하고 loadBundled는 봉인 문서를 한 번 연다. read(stream)는 독립
 * 입력 검사용이며 BaseRegistry와 베이스 키 집합이 일치하는지 확인한다. 누락·불일치는 IllegalArgumentException, read의 I/O는
 * IOException, loadBundled의 I/O는 IllegalStateException이다. 반환 map·중첩 후보 목록은 불변이다. Targets는
 * fixed(추가)와 replacements(교체)를 가진다. Loaded는 지연 초기화 홀더다.
 */
public final class ReviewedEssencesLoader {
  private ReviewedEssencesLoader() {}

  /**
   * fixed는 추가형, replacements는 교체형의 화폐별 옵션 ID 목록이다. 생성자는 copy를 통해 map과 목록을 방어 복사한다. copy는 null map과
   * null/빈 ID를 거절하고 EnumMap으로 순서를 고정한다.
   */
  public record Targets(
      Map<WorkbenchCurrency, List<String>> fixed,
      Map<WorkbenchCurrency, List<String>> replacements) {
    public Targets {
      fixed = copy(fixed);
      replacements = copy(replacements);
    }

    private static Map<WorkbenchCurrency, List<String>> copy(
        Map<WorkbenchCurrency, List<String>> source) {
      if (source == null) throw new IllegalArgumentException("Missing reviewed essence target map");
      var result = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
      source.forEach(
          (action, ids) -> {
            if (ids == null || ids.stream().anyMatch(id -> id == null || id.isBlank()))
              throw new IllegalArgumentException("Missing reviewed essence target ID");
            result.put(action, List.copyOf(ids));
          });
      return Collections.unmodifiableMap(result);
    }
  }

  /** TARGETS는 loadBundled가 처음 읽은 검증 완료 목록이다. 요청별로 재파싱하지 않는다. */
  private static final class Loaded {
    private static final Map<String, Targets> TARGETS = loadBundled();
  }

  public static Map<String, Targets> load() {
    return Loaded.TARGETS;
  }

  private static Map<String, Targets> loadBundled() {
    try (var stream = SealedResources.open("/catalog/top-base-essences.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed essence manifest", e);
    }
  }

  public static Map<String, Targets> read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing reviewed essence manifest");
    var mapper = BundledJson.mapper();
    Map<String, Targets> manifest =
        mapper.readValue(stream, new TypeReference<Map<String, Targets>>() {});
    var expected = new HashSet<String>();
    BaseRegistry.topBases()
        .forEach(
            (key, base) -> {
              if (base.policy().legacyCatalog() == null) expected.add(key);
            });
    if (manifest == null
        || !expected.equals(manifest.keySet())
        || manifest.values().stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Reviewed essence base scope differs");
    return Collections.unmodifiableMap(new LinkedHashMap<>(manifest));
  }
}
