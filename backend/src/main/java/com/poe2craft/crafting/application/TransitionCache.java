package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.CraftingAction;
import com.poe2craft.crafting.domain.CraftingEngine;
import com.poe2craft.crafting.domain.StateBucket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded LRU by both entry count and retained outcome count; computation stays outside the lock.
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

  private record Key(String rules, StateBucket state, CraftingAction action) {}

  public record Stats(long hits, long misses, int entries, int retainedOutcomes) {}
}
