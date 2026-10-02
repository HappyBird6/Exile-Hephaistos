package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class SupportRecommendationsTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final Map<String, AdditionPoolCache.Data> saved = new HashMap<>();
  final AdditionPoolStore store =
      new AdditionPoolStore() {
        public Optional<AdditionPoolCache.Data> find(String namespace, String key) {
          return Optional.ofNullable(saved.get(namespace + key));
        }

        public void save(String namespace, String key, AdditionPoolCache.Data value) {
          saved.put(namespace + key, value);
        }
      };

  AdditionPoolCache cache() {
    return new AdditionPoolCache(
        catalog, store, WorkbenchSimulator.RULE_VERSION, "ledger-v1", 2048);
  }

  StateBucket root() {
    return StateBucket.from(SolarAmulet.initial(catalog));
  }

  SupportGoals.Goal life(int tier) {
    return new SupportGoals.Goal(
        List.of(new SupportGoals.Condition("IncreasedLife", tier)), List.of(), 0);
  }

  String id(String family, int tier) {
    return catalog.modifiers().values().stream()
        .filter(d -> d.familyIds().contains(family) && d.tier() == tier)
        .findFirst()
        .orElseThrow()
        .id();
  }

  final SupportRecommendations.Limits generous =
      new SupportRecommendations.Limits(200000, 5000000, 10000);

  @Test
  void multiAdditionOmenCannotProduceAnIncorrectSingleAdditionRecommendation() {
    var service = new SupportRecommendations(catalog, cache());
    var omens = Set.of(WorkbenchOmen.GREATER_EXALTATION.id());
    assertThatThrownBy(() -> service.recommend(root(), life(2), omens, generous))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite addition model");
    assertThatThrownBy(
            () ->
                service.evaluate(
                    root(), life(2), omens, List.of(WorkbenchCurrency.EXALTED), generous))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite addition model");
  }

  @Test
  void snapshotAndSourceDigestChangesInvalidateTheNamespace() {
    var m = catalog.metadata();
    var updated =
        new ItemCatalog.Metadata(
            "synthetic-next-snapshot",
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            "a".repeat(64),
            m.detailsSha256(),
            m.prefixCount(),
            m.suffixCount(),
            m.prefixWeight(),
            m.suffixWeight());
    var other =
        new ItemCatalog(updated, catalog.base(), new ArrayList<>(catalog.modifiers().values()));
    var changed =
        new AdditionPoolCache(other, store, WorkbenchSimulator.RULE_VERSION, "ledger-v1", 2);
    assertThat(changed.namespace()).isNotEqualTo(cache().namespace());
  }

  @Test
  void firstHitAbsorbsAndMatchesIndependentExactEnumeration() {
    var service = new SupportRecommendations(catalog, cache());
    var result =
        service.evaluate(
            root(),
            life(9),
            Set.of(),
            List.of(WorkbenchCurrency.TRANSMUTATION, WorkbenchCurrency.REGAL),
            generous);
    var exact = new AdditionTransitions(catalog);
    double expected = 0;
    for (var first :
        exact.transition(root(), WorkbenchCurrency.TRANSMUTATION, Set.of()).outcomes()) {
      if (first.state().modifierIds().contains(id("IncreasedLife", 9))
          || new SupportGoals(catalog).assess(first.state(), life(9)).achieved())
        expected += first.probability();
      else
        for (var second :
            exact.transition(first.state(), WorkbenchCurrency.REGAL, Set.of()).outcomes())
          if (new SupportGoals(catalog).assess(second.state(), life(9)).achieved())
            expected += first.probability() * second.probability();
    }
    assertThat(result.successLower()).isCloseTo(expected, within(1e-12));
    assertThat(
            result.steps().stream()
                .mapToDouble(SupportRecommendations.Step::firstHitProbability)
                .sum())
        .isCloseTo(result.successLower(), within(1e-12));
    assertThat(result.failureProbability() + result.successLower()).isCloseTo(1, within(1e-12));
    assertThat(result.complete()).isTrue();
  }

  @Test
  void tierQualificationIsNotLostWhenReusingPoolsAcrossGoalsAndExistingTiers() {
    var cache = cache();
    var service = new SupportRecommendations(catalog, cache);
    var low = root().with(ItemState.Rarity.RARE, List.of(id("IncreasedLife", 9)));
    var high = root().with(ItemState.Rarity.RARE, List.of(id("IncreasedLife", 1)));
    var first = cache.get(low, WorkbenchCurrency.EXALTED, Set.of());
    var stats = cache.stats();
    assertThat(cache.get(high, WorkbenchCurrency.EXALTED, Set.of())).isEqualTo(first);
    assertThat(cache.stats().memoryHits()).isEqualTo(stats.memoryHits() + 1);
    assertThat(
            service
                .evaluate(high, life(2), Set.of(), List.of(WorkbenchCurrency.EXALTED), generous)
                .successLower())
        .isEqualTo(1);
    assertThat(
            service
                .evaluate(low, life(2), Set.of(), List.of(WorkbenchCurrency.EXALTED), generous)
                .successLower())
        .isZero();
    assertThat(low.modifierIds()).containsExactly(id("IncreasedLife", 9));
    assertThat(high.modifierIds()).containsExactly(id("IncreasedLife", 1));
  }

  @Test
  void persistentReuseAndNamespaceVersionInvalidationAreSeparateFromGoals() {
    var first = cache();
    var data = first.get(root(), WorkbenchCurrency.PERFECT_TRANSMUTATION, Set.of());
    var restarted = cache();
    assertThat(restarted.get(root(), WorkbenchCurrency.PERFECT_TRANSMUTATION, Set.of()))
        .isEqualTo(data);
    assertThat(restarted.stats().persistedHits()).isEqualTo(1);
    var changedRule = new AdditionPoolCache(catalog, store, "next-rule", "ledger-v1", 2);
    var changedLedger =
        new AdditionPoolCache(catalog, store, WorkbenchSimulator.RULE_VERSION, "next-ledger", 2);
    assertThat(changedRule.namespace()).isNotEqualTo(first.namespace());
    assertThat(changedLedger.namespace()).isNotEqualTo(first.namespace());
    changedRule.get(root(), WorkbenchCurrency.PERFECT_TRANSMUTATION, Set.of());
    assertThat(changedRule.stats().computedPools()).isEqualTo(1);
  }

  @Test
  void itemLevelActionAndOmenContextsDoNotShareWrongPools() {
    var cache = cache();
    var rare = root().with(ItemState.Rarity.RARE, List.of());
    var ordinary = cache.get(rare, WorkbenchCurrency.EXALTED, Set.of());
    var perfect = cache.get(rare, WorkbenchCurrency.PERFECT_EXALTED, Set.of());
    var sin =
        cache.get(rare, WorkbenchCurrency.EXALTED, Set.of(WorkbenchOmen.SINISTRAL_EXALTATION.id()));
    var low =
        cache.get(
            StateBucket.from(SolarAmulet.initial(catalog, 30, 15))
                .with(ItemState.Rarity.RARE, List.of()),
            WorkbenchCurrency.EXALTED,
            Set.of());
    assertThat(ordinary.channels()).hasSizeGreaterThan(perfect.channels().size());
    assertThat(sin.channels())
        .allMatch(
            c ->
                catalog.find(c.modifierId()).orElseThrow().affixType()
                    == ModifierDefinition.AffixType.PREFIX);
    assertThat(low.channels()).hasSizeLessThan(ordinary.channels().size());
    assertThat(cache.stats().computedPools()).isEqualTo(4);
  }

  @Test
  void budgetStopsWithUnresolvedMassRatherThanAFalseFailure() {
    var service = new SupportRecommendations(catalog, cache());
    var result =
        service.evaluate(
            root(),
            life(2),
            Set.of(),
            List.of(WorkbenchCurrency.TRANSMUTATION, WorkbenchCurrency.REGAL),
            new SupportRecommendations.Limits(1, 5000000, 10000));
    assertThat(result.complete()).isFalse();
    assertThat(result.unresolvedProbability()).isPositive();
    assertThat(result.successUpper()).isGreaterThan(result.successLower());
    assertThat(result.successLower() + result.failureProbability() + result.unresolvedProbability())
        .isCloseTo(1, within(1e-12));
  }

  @Test
  void partialRankingReportsUnevaluatedSequencesAndCannotBeCertified() {
    var service = new SupportRecommendations(catalog, cache());
    var report =
        service.recommend(
            root(), life(2), Set.of(), new SupportRecommendations.Limits(1, 5000000, 10000));
    assertThat(report.totalSequences()).isEqualTo(1458);
    assertThat(report.comparedSequences()).isEqualTo(1);
    assertThat(report.complete()).isFalse();
    assertThat(report.rankingCertified()).isFalse();
    assertThat(report.comparisons().getFirst().unresolvedProbability()).isPositive();
  }

  @Test
  void requiredAndCandidateThresholdProbabilityMatchesExhaustiveTwoStepEnumeration() {
    var goal =
        new SupportGoals.Goal(
            List.of(new SupportGoals.Condition("IncreasedLife", 2)),
            List.of(
                new SupportGoals.Condition("BaseSpirit", 2),
                new SupportGoals.Condition("Strength", 1)),
            1);
    var service = new SupportRecommendations(catalog, cache());
    var exact = new AdditionTransitions(catalog);
    var matcher = new SupportGoals(catalog);
    double expected = 0;
    for (var first :
        exact.transition(root(), WorkbenchCurrency.PERFECT_TRANSMUTATION, Set.of()).outcomes())
      for (var second :
          exact.transition(first.state(), WorkbenchCurrency.PERFECT_REGAL, Set.of()).outcomes())
        if (matcher.assess(second.state(), goal).achieved())
          expected += first.probability() * second.probability();
    var result =
        service.evaluate(
            root(),
            goal,
            Set.of(),
            List.of(WorkbenchCurrency.PERFECT_TRANSMUTATION, WorkbenchCurrency.PERFECT_REGAL),
            generous);
    assertThat(result.successLower()).isPositive().isCloseTo(expected, within(1e-12));
    assertThat(result.steps().getFirst().firstHitProbability()).isZero();
  }

  @Test
  void comparesThreeRealOneSlotSequencesAndAlreadyAchievedNeedsNone() {
    var ids =
        List.of(
            id("BaseSpirit", 1),
            id("SpellDamage", 1),
            id("IncreasedCastSpeed", 1),
            id("Strength", 1),
            id("ItemFoundRarityIncrease", 1));
    var five = root().with(ItemState.Rarity.RARE, ids);
    var service = new SupportRecommendations(catalog, cache());
    var result = service.recommend(five, life(2), Set.of(), generous);
    assertThat(result.complete()).isTrue();
    assertThat(result.totalSequences()).isEqualTo(3);
    assertThat(result.comparisons()).hasSize(3);
    assertThat(result.comparisons())
        .allMatch(c -> c.sequence().size() == 1 && c.successLower() > 0 && c.complete());
    var achieved =
        five.with(
            ItemState.Rarity.RARE,
            new ArrayList<>() {
              {
                addAll(ids);
                add(id("IncreasedLife", 1));
              }
            });
    assertThat(service.recommend(achieved, life(2), Set.of(), generous).assessment().achieved())
        .isTrue();
    assertThat(service.recommend(achieved, life(2), Set.of(), generous).comparisons()).isEmpty();
  }
}
