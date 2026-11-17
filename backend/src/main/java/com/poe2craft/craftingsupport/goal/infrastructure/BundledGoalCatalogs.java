package com.poe2craft.craftingsupport.goal.infrastructure;

import com.poe2craft.shared.crafting.application.CraftingCatalogs;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.util.List;

/**
 * 작업대가 공유하는 검토된 카탈로그로 목표 평가용 입력을 구성한다.
 *
 * <p>기존 계약: Reuses Workbench's publicly exposed reviewed definitions without crossing item
 * internals.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BundledGoalCatalogs()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code readBases(stream)}: 베이스목록을(를) 읽는다. 입력: {@code stream}(스트림). 반환: {@code List<Base>}.
 *       사용하는 예외: {@code IllegalArgumentException}, {@code IOException}.
 *   <li>{@code load(workbench)}: 검증된 자료를 로딩한다. 입력: {@code workbench}(작업대). 반환: {@code
 *       List<ItemCatalog>}.
 *   <li>{@code loadHistoricalV5(workbench)}: 과거·5을(를) 읽어 검증한다. 입력: {@code workbench}(작업대). 반환:
 *       {@code List<ItemCatalog>}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Base}: {@code BundledGoalCatalogs}에서 사용하는 베이스의 불변 값이다.
 *   <li>{@code Registry}: {@code BundledGoalCatalogs}에서 사용하는 등록목록의 불변 값이다.
 *   <li>{@code HistoricalInventory}: {@code BundledGoalCatalogs} 내부의 과거·등록자료 처리를 맡는다.
 * </ul>
 */
public final class BundledGoalCatalogs {
  private BundledGoalCatalogs() {}

  /**
   * {@code BundledGoalCatalogs}에서 사용하는 베이스의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code String}).
   *   <li>{@code pool}: 후보풀 ({@code String}).
   * </ul>
   */
  public record Base(String key, String pool) {}

  /**
   * {@code BundledGoalCatalogs}에서 사용하는 등록목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code bases}: 베이스목록 ({@code List<Base>}).
   * </ul>
   */
  public record Registry(int schemaVersion, List<Base> bases) {}

  public static List<Base> readBases(java.io.InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal base registry");
    var mapper = BundledJson.mapper();
    var registry = mapper.readValue(stream, Registry.class);
    if (registry.schemaVersion() != 1 || registry.bases() == null || registry.bases().isEmpty())
      throw new IllegalArgumentException("Invalid goal base registry version or entries");
    var keys = new java.util.HashSet<String>();
    var pools = new java.util.HashSet<String>();
    for (var base : registry.bases())
      if (base == null
          || base.key() == null
          || base.pool() == null
          || !base.key().matches("[a-z0-9-]+")
          || !base.pool().matches("[a-z0-9-]+")
          || !keys.add(base.key())
          || !pools.add(base.pool()))
        throw new IllegalArgumentException("Invalid or duplicate goal base identity");
    return registry.bases().stream().sorted(java.util.Comparator.comparing(Base::key)).toList();
  }

  /** Shares the complete starting-item inventory; registration requires no second goal list. */
  public static List<ItemCatalog> load(CraftingCatalogs workbench) {
    return workbench.reviewedCatalogs();
  }

  /** Frozen membership reproduces saved catalog signatures before the base-picker expansion. */
  public static List<ItemCatalog> loadHistoricalV5(CraftingCatalogs workbench) {
    var ids = HistoricalInventory.IDS;
    var catalogs = load(workbench).stream().filter(c -> ids.contains(c.base().id())).toList();
    if (catalogs.size() != ids.size())
      throw new IllegalStateException("Historical base inventory changed");
    return catalogs;
  }

  /**
   * {@code BundledGoalCatalogs} 내부의 과거·등록자료 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code IDS}: 식별자목록 ({@code java.util.Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code java.util.Set<String>}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   */
  private static final class HistoricalInventory {
    private static final java.util.Set<String> IDS = load();

    private static java.util.Set<String> load() {
      try (var input =
          SealedResources.open("/crafting/goalfilter/base-inventory-v5-readonly.json")) {
        return java.util.Set.copyOf(
            BundledJson.mapper()
                .readValue(
                    input,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Set<String>>() {}));
      } catch (IOException error) {
        throw new IllegalStateException("Cannot read historical base inventory", error);
      }
    }
  }
}
