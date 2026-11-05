package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.AdditionRules;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/** Uses the actual shared pool rule, including a family's highest-tier fallback below a cutoff. */
final class BasicCandidateEligibility {
  private final Map<String, Integer> masks;

  BasicCandidateEligibility(ItemCatalog catalog, ItemState root) {
    var actions = BasicCurrencyTransitions.reachableActions(root);
    var rules = new AdditionRules(catalog);
    var empty = StateBucket.from(root).with(ItemState.Rarity.RARE, List.of());
    var result = new HashMap<String, Integer>();
    for (int i = 0; i < actions.size(); i++) {
      int bit = 1 << i;
      for (var candidate : rules.pool(empty, actions.get(i), null))
        result.merge(candidate.id(), bit, (a, b) -> a | b);
    }
    masks = Map.copyOf(result);
  }

  int category(String modifier) {
    return masks.getOrDefault(modifier, 0);
  }
}
