package com.poe2craft.crafting.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.BasicCurrencyTransitions.Status;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class BasicTransitionCacheTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final BasicCurrencyTransitions kernel = new BasicCurrencyTransitions(catalog, "test-ruleset-one");
  final BasicCurrencyState root =
      new BasicCurrencyState(SolarAmulet.initial(catalog), kernel.provenance());

  @Test
  void boundedCacheSeparatesRulesetsAndNeverReturnsCompleteDataForASmallerBudget() {
    var cache = new BasicTransitionCache(kernel, 1, 100000);
    var complete = cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000);
    assertThat(complete.status()).isEqualTo(Status.COMPLETE);
    assertThat(cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000))
        .isSameAs(complete);
    assertThat(cache.stats().hits()).isEqualTo(1);
    var partial = cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 1);
    assertThat(partial.status()).isEqualTo(Status.PARTIAL);
    assertThat(partial.elementaryOutcomes()).isEqualTo(1);
    var foreignKernel = new BasicCurrencyTransitions(catalog, "test-ruleset-two");
    var foreignRoot = new BasicCurrencyState(root.item(), foreignKernel.provenance());
    assertThat(root.canonicalKey()).isNotEqualTo(foreignRoot.canonicalKey());
    assertThatThrownBy(
            () -> cache.get(foreignRoot, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000))
        .isInstanceOf(IllegalArgumentException.class);
    var other = new BasicTransitionCache(foreignKernel, 1, 100000);
    assertThat(
            other
                .get(foreignRoot, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000)
                .source()
                .provenance())
        .isEqualTo(foreignKernel.provenance());
    assertThat(other.stats().hits()).isZero();
    cache.get(root, WorkbenchCurrency.GREATER_TRANSMUTATION, Set.of(), 100000);
    assertThat(cache.stats().entries()).isEqualTo(1);
    assertThat(cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000))
        .isNotSameAs(complete);
    assertThat(cache.stats().retainedOutcomes()).isLessThanOrEqualTo(100000);
  }

  @Test
  void partialAndOversizedResultsAreNotCachedAndExistingNumericStatesAreNotMerged() {
    var cache = new BasicTransitionCache(kernel, 2, 1);
    cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 1);
    cache.get(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000);
    assertThat(cache.stats().entries()).isZero();
    var full = kernel.expand(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 100000);
    var low = full.outcomes().getFirst().state();
    var def = catalog.find(low.item().explicits().getFirst().modifierId()).orElseThrow();
    var range = def.stats().getFirst();
    assertThat(range.max()).isGreaterThan(range.min());
    var highItem =
        new ItemState(
            low.item().snapshotId(),
            low.item().baseItemId(),
            low.item().itemLevel(),
            low.item().rarity(),
            low.item().implicits(),
            List.of(new ModifierInstance(def.id(), Map.of(range.id(), range.max()))),
            Set.of());
    var high = new BasicCurrencyState(highItem, kernel.provenance());
    var numeric = new BasicTransitionCache(kernel, 10, 100000);
    numeric.get(low, WorkbenchCurrency.AUGMENTATION, Set.of(), 100000);
    numeric.get(high, WorkbenchCurrency.AUGMENTATION, Set.of(), 100000);
    assertThat(numeric.stats().misses()).isEqualTo(2);
    assertThat(numeric.stats().entries()).isEqualTo(2);
    assertThat(numeric.get(low, WorkbenchCurrency.AUGMENTATION, Set.of(), 100000).outcomes())
        .allSatisfy(
            o ->
                assertThat(o.state().item().explicits())
                    .contains(low.item().explicits().getFirst()));
    assertThatThrownBy(() -> numeric.get(low, WorkbenchCurrency.AUGMENTATION, null, 100000))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            numeric
                .get(low, WorkbenchCurrency.AUGMENTATION, Set.of("Omen_of_Whittling"), 100000)
                .status())
        .isEqualTo(Status.UNSUPPORTED);
  }
}
