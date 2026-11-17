package com.poe2craft.craftingsupport.path.application;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import java.util.*;

/**
 * 완전한 아이템 상태를 키로 완료된 전이 분포를 제한된 크기로 재사용한다.
 *
 * <p>기존 계약: Bounded full-state cache; partial distributions and caller budgets are never hidden by
 * a hit.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code transitions}: 전이목록 ({@code BasicCurrencyTransitions}).
 *   <li>{@code maxEntries}: 최대값·항목목록 ({@code int}).
 *   <li>{@code maxRetainedOutcomes}: 최대값·보유·결과목록 ({@code long}).
 *   <li>{@code entries}: 항목목록 ({@code Map<Key, BasicCurrencyTransitions.Result>}).
 *   <li>{@code retainedOutcomes}: 보유·결과목록 ({@code long}).
 *   <li>{@code hits}: 도달목록 ({@code long}).
 *   <li>{@code misses}: 캐시실패수 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicTransitionCache(transitions, maxEntries, maxRetainedOutcomes)}: 전달받은 값으로 구성원을
 *       초기화한다. 입력: {@code transitions}(전이목록), {@code maxEntries}(최대값·항목목록), {@code
 *       maxRetainedOutcomes}(최대값·보유·결과목록). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code get(state, action, activeOmens, maxElementaryOutcomes)}: 키 또는 위치에 해당하는 값을 조회한다. 입력:
 *       {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code
 *       maxElementaryOutcomes}(최대값·기본·결과목록). 반환: {@code BasicCurrencyTransitions.Result}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code stats()}: 수치목록 값을 조회·구성한다. 반환: {@code Stats}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code BasicTransitionCache}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Stats}: {@code BasicTransitionCache}에서 사용하는 수치목록의 불변 값이다.
 * </ul>
 */
public final class BasicTransitionCache {
  private final BasicCurrencyTransitions transitions;
  private final int maxEntries;
  private final long maxRetainedOutcomes;
  private final Map<Key, BasicCurrencyTransitions.Result> entries =
      new LinkedHashMap<>(16, .75f, true);
  private long retainedOutcomes, hits, misses;

  public BasicTransitionCache(
      BasicCurrencyTransitions transitions, int maxEntries, long maxRetainedOutcomes) {
    this.transitions = Objects.requireNonNull(transitions);
    if (maxEntries < 1 || maxRetainedOutcomes < 1)
      throw new IllegalArgumentException("Positive cache capacities required");
    this.maxEntries = maxEntries;
    this.maxRetainedOutcomes = maxRetainedOutcomes;
  }

  public BasicCurrencyTransitions.Result get(
      BasicCurrencyState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      long maxElementaryOutcomes) {
    if (state == null || action == null || activeOmens == null || maxElementaryOutcomes < 1)
      throw new IllegalArgumentException("Explicit transition request required");
    if (!state.provenance().equals(transitions.provenance()))
      throw new IllegalArgumentException("Transition provenance mismatch");
    if (!activeOmens.isEmpty())
      return transitions.expand(state, action, activeOmens, maxElementaryOutcomes);
    // Record equality retains the complete item and provenance; hashes are never the only check.
    var key = new Key(state, action);
    synchronized (this) {
      var cached = entries.get(key);
      if (cached != null && cached.elementaryOutcomes() <= maxElementaryOutcomes) {
        hits++;
        return cached;
      }
      misses++;
    }
    var result = transitions.expand(state, action, activeOmens, maxElementaryOutcomes);
    synchronized (this) {
      if (result.status() == BasicCurrencyTransitions.Status.COMPLETE
          && !entries.containsKey(key)
          && result.outcomes().size() <= maxRetainedOutcomes) {
        entries.put(key, result);
        retainedOutcomes += result.outcomes().size();
        while (entries.size() > maxEntries || retainedOutcomes > maxRetainedOutcomes) {
          var iterator = entries.entrySet().iterator();
          retainedOutcomes -= iterator.next().getValue().outcomes().size();
          iterator.remove();
        }
      }
    }
    return result;
  }

  /**
   * {@code BasicTransitionCache}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code BasicCurrencyState}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   * </ul>
   */
  private record Key(BasicCurrencyState state, WorkbenchCurrency action) {}

  public synchronized Stats stats() {
    return new Stats(hits, misses, entries.size(), retainedOutcomes);
  }

  /**
   * {@code BasicTransitionCache}에서 사용하는 수치목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code hits}: 도달목록 ({@code long}).
   *   <li>{@code misses}: 캐시실패수 ({@code long}).
   *   <li>{@code entries}: 항목목록 ({@code int}).
   *   <li>{@code retainedOutcomes}: 보유·결과목록 ({@code long}).
   * </ul>
   */
  public record Stats(long hits, long misses, int entries, long retainedOutcomes) {}
}
