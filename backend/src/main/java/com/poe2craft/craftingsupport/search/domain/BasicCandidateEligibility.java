package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.AdditionRules;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/**
 * 공유 추가 규칙을 사용해 티어 하한과 최고 티어 대체 후보를 판정한다.
 *
 * <p>기존 계약: Uses the actual shared pool rule, including a family's highest-tier fallback below a
 * cutoff.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code masks}: masks ({@code Map<String, Integer>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicCandidateEligibility(catalog, root)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code root}(시작점).
 *   <li>{@code category(modifier)}: 분류 처리에서 {@code masks.getOrDefault}을(를) 사용한다. 입력: {@code
 *       modifier}(옵션). 반환: {@code int}.
 * </ul>
 */
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
