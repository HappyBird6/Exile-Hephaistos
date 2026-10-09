package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import java.util.*;

/** Bounded full-state cache; partial distributions and caller budgets are never hidden by a hit. */
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

  private record Key(BasicCurrencyState state, WorkbenchCurrency action) {}

  public synchronized Stats stats() {
    return new Stats(hits, misses, entries.size(), retainedOutcomes);
  }

  public record Stats(long hits, long misses, int entries, long retainedOutcomes) {}
}
