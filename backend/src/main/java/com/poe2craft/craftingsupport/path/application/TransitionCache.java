package com.poe2craft.craftingsupport.path.application;

import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 항목 수와 보유 결과 수를 함께 제한하는 전이 LRU 캐시다.
 *
 * <p>기존 계약: Bounded LRU by both entry count and retained outcome count; computation stays outside
 * the lock.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code engine}: 계산 실행기 ({@code CraftingEngine}).
 *   <li>{@code maxEntries}: 최대값·항목목록 ({@code int}).
 *   <li>{@code maxOutcomes}: 최대값·결과목록 ({@code int}).
 *   <li>{@code entries}: 항목목록 ({@code Map<Key, CraftingEngine.TransitionResult>}).
 *   <li>{@code retainedOutcomes}: 보유·결과목록 ({@code int}).
 *   <li>{@code hits}: 도달목록 ({@code long}).
 *   <li>{@code misses}: 캐시실패수 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TransitionCache(engine, maxEntries, maxOutcomes)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       engine}(계산엔진), {@code maxEntries}(최대값·항목목록), {@code maxOutcomes}(최대값·결과목록). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code get(state, action)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code state}(상태), {@code
 *       action}(행동). 반환: {@code CraftingEngine.TransitionResult}.
 *   <li>{@code stats()}: 수치목록 값을 조회·구성한다. 반환: {@code Stats}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code TransitionCache}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Stats}: {@code TransitionCache}에서 사용하는 수치목록의 불변 값이다.
 * </ul>
 */
public final class TransitionCache {
  private final CraftingEngine engine;
  private final int maxEntries, maxOutcomes;
  private final Map<Key, CraftingEngine.TransitionResult> entries =
      new LinkedHashMap<>(16, .75f, true);
  private int retainedOutcomes;
  private long hits, misses;

  public TransitionCache(CraftingEngine engine, int maxEntries, int maxOutcomes) {
    if (maxEntries < 1 || maxOutcomes < 1)
      throw new IllegalArgumentException("Invalid cache capacity");
    this.engine = engine;
    this.maxEntries = maxEntries;
    this.maxOutcomes = maxOutcomes;
  }

  public CraftingEngine.TransitionResult get(StateBucket state, CraftingAction action) {
    var key = new Key(CraftingEngine.RULE_VERSION, state, action);
    synchronized (this) {
      var cached = entries.get(key);
      if (cached != null) {
        hits++;
        return cached;
      }
      misses++;
    }
    var result = engine.transition(state, action);
    synchronized (this) {
      if (!entries.containsKey(key) && result.outcomes().size() <= maxOutcomes) {
        entries.put(key, result);
        retainedOutcomes += result.outcomes().size();
        while (entries.size() > maxEntries || retainedOutcomes > maxOutcomes) {
          var iterator = entries.entrySet().iterator();
          retainedOutcomes -= iterator.next().getValue().outcomes().size();
          iterator.remove();
        }
      }
    }
    return result;
  }

  public synchronized Stats stats() {
    return new Stats(hits, misses, entries.size(), retainedOutcomes);
  }

  /**
   * {@code TransitionCache}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rules}: 규칙목록 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code CraftingAction}).
   * </ul>
   */
  private record Key(String rules, StateBucket state, CraftingAction action) {}

  /**
   * {@code TransitionCache}에서 사용하는 수치목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code hits}: 도달목록 ({@code long}).
   *   <li>{@code misses}: 캐시실패수 ({@code long}).
   *   <li>{@code entries}: 항목목록 ({@code int}).
   *   <li>{@code retainedOutcomes}: 보유·결과목록 ({@code int}).
   * </ul>
   */
  public record Stats(long hits, long misses, int entries, int retainedOutcomes) {}
}
