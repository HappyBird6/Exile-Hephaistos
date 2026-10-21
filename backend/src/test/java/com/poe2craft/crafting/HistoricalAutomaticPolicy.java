package com.poe2craft.crafting;

import com.poe2craft.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import java.util.Set;

/** Explicit pre-deferral fixtures keep previously implemented omen laws under regression tests. */
public final class HistoricalAutomaticPolicy {
  private HistoricalAutomaticPolicy() {}

  public static AutomaticCurrencyPolicy beforeDeferrals() {
    var current = AutomaticCurrencyPolicyLoader.load().document();
    var deferred = Set.of("OMEN:SINISTRAL_ERASURE", "OMEN:DEXTRAL_ERASURE", "OMEN:WHITTLING");
    var entries =
        current.entries().stream()
            .map(
                e ->
                    deferred.contains(e.id())
                        ? new AutomaticCurrencyPolicy.Entry(
                            e.number(),
                            e.id(),
                            e.nameKo(),
                            e.sourceUrl(),
                            e.effectKo(),
                            AutomaticCurrencyPolicy.Decision.CONDITIONAL,
                            "Historical engine fixture",
                            AutomaticCurrencyPolicy.Condition.TEMPORARY_REMOVAL)
                        : e)
            .toList();
    return new AutomaticCurrencyPolicy(
        new AutomaticCurrencyPolicy.Document(
            current.schemaVersion(),
            current.policyVersion(),
            current.sourceUrl(),
            current.sourceModifiedAt(),
            current.maximumExploredExplicits(),
            entries));
  }
}
