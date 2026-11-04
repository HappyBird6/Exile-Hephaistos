package com.poe2craft.crafting.application.chaosrepeat;

import static com.poe2craft.crafting.application.chaosrepeat.ChaosRepeatRunner.FailureStage.*;
import static com.poe2craft.crafting.application.chaosrepeat.ChaosRepeatRunner.Termination.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poe2craft.crafting.application.chaosrepeat.ChaosRepeatRunner.Outcome;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChaosRepeatRunnerTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  private final ChaosRepeatRunner runner = new ChaosRepeatRunner(simulator::apply);

  private ItemState rare(int level) {
    return simulator
        .apply(
            SolarAmulet.initial(catalog, level, 15),
            WorkbenchCurrency.ALCHEMY,
            Set.of(),
            new Random(4))
        .state();
  }

  @ParameterizedTest
  @EnumSource(
      value = WorkbenchCurrency.class,
      names = {"CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS"})
  void equalsManualUsesWithSameSeedAtDifferentLevelsAndWithOneShotOmens(WorkbenchCurrency action) {
    for (int level : List.of(70, 82)) {
      for (long seed : List.of(1L, 42L, 104729L)) {
        var start = rare(level);
        for (var omens :
            List.of(
                Set.<String>of(),
                Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.BLESSED.id()))) {
          var state = start;
          var remaining = omens;
          var consumed = new HashMap<String, Integer>();
          var random = new Random(seed);
          WorkbenchSimulator.Result last = null;
          for (int i = 0; i < 17; i++) {
            last = simulator.apply(state, action, remaining, random);
            assertThat(last.applied()).isTrue();
            state = last.state();
            remaining = Set.copyOf(last.remainingOmens());
            last.consumedOmens().forEach(id -> consumed.merge(id, 1, Integer::sum));
          }
          var actual = runner.run(start, action, omens, 17, new Random(seed), s -> false);
          assertThat(actual)
              .isEqualTo(new Outcome(state, 17, consumed, remaining, last, LIMIT, null));
          assertThat(start).isEqualTo(rare(level));
        }
      }
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = WorkbenchCurrency.class,
      names = {"CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS"})
  void fullSixExplicitItemsKeepFractureAndExistingMetadataAcrossManualUses(
      WorkbenchCurrency action) {
    var start = rare(82);
    for (int i = 0; i < 2; i++)
      start = simulator.apply(start, WorkbenchCurrency.EXALTED, Set.of(), new Random(i)).state();
    start = simulator.apply(start, WorkbenchCurrency.FRACTURING, Set.of(), new Random(2)).state();
    var original = start;
    var locked =
        start.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    var inspected = new AtomicInteger();
    var checkedRunner =
        new ChaosRepeatRunner(
            (state, currency, omens, random) -> {
              var result = simulator.apply(state, currency, omens, random);
              assertThat(result.applied()).isTrue();
              assertThat(result.state().explicits()).hasSize(6).contains(locked);
              assertThat(result.state().implicits()).isEqualTo(original.implicits());
              assertThat(result.state().itemLevel()).isEqualTo(original.itemLevel());
              assertThat(result.state().augmentSockets()).isEqualTo(original.augmentSockets());
              assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
              inspected.incrementAndGet();
              return result;
            });
    var actual = checkedRunner.run(start, action, Set.of(), 101, new Random(42), s -> false);
    var reference = start;
    var random = new Random(42);
    for (int i = 0; i < 101; i++)
      reference = simulator.apply(reference, action, Set.of(), random).state();
    assertThat(actual.termination()).isEqualTo(LIMIT);
    assertThat(actual.completedUses()).isEqualTo(101);
    assertThat(inspected).hasValue(101);
    assertThat(actual.state()).isEqualTo(reference);
    assertThat(start).isEqualTo(original);
    assertThat(start.explicits()).hasSize(6).contains(locked);
  }

  @Test
  void passesDesecratedSlotsThroughExistingApplyWithoutRebuildingTheItem() {
    var root = SolarAmulet.initial(catalog, 82, 15);
    var definition =
        catalog.modifiers().values().stream()
            .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight() > 0)
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    var slot = new ItemState.DesecratedSlot("slot-1", ModifierDefinition.AffixType.SUFFIX, null);
    var start =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(new ModifierInstance(definition.id(), values)),
            Set.of(),
            root.augmentSockets(),
            root.catalystQuality(),
            List.of(slot));
    var expected = simulator.apply(start, WorkbenchCurrency.CHAOS, Set.of(), new Random(42));
    var actual =
        runner.run(start, WorkbenchCurrency.CHAOS, Set.of(), 10, new Random(42), s -> true);
    assertThat(actual.termination()).isEqualTo(MATCH);
    assertThat(actual.state()).isEqualTo(expected.state());
    assertThat(actual.state().desecrated()).containsExactly(slot);
    assertThat(start.desecrated()).containsExactly(slot);
    assertThat(start.explicits()).containsExactly(new ModifierInstance(definition.id(), values));
  }

  @Test
  void initialMatchIsNotInspectedUntilAfterTheFirstUseAndMatchWinsAtTheLimit() {
    var calls = new AtomicInteger();
    var matcherCalls = new AtomicInteger();
    var start = rare(82);
    var fixed = unchanged(start);
    var actual =
        new ChaosRepeatRunner(
                (s, a, o, r) -> {
                  calls.incrementAndGet();
                  return fixed;
                })
            .run(
                start,
                WorkbenchCurrency.CHAOS,
                Set.of(),
                1,
                new Random(1),
                s -> {
                  assertThat(calls).hasValue(1);
                  matcherCalls.incrementAndGet();
                  return true;
                });
    assertThat(actual.completedUses()).isEqualTo(1);
    assertThat(actual.termination()).isEqualTo(MATCH);
    assertThat(actual.lastResult()).isSameAs(fixed);
    assertThat(matcherCalls).hasValue(1);
  }

  @Test
  void matchingStopsWithoutAnotherUse() {
    var matches = new AtomicInteger();
    var applies = new AtomicInteger();
    var checked =
        new ChaosRepeatRunner(
            (s, a, o, r) -> {
              applies.incrementAndGet();
              return simulator.apply(s, a, o, r);
            });
    var actual =
        checked.run(
            rare(82),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            10,
            new Random(7),
            s -> matches.incrementAndGet() == 3);
    assertThat(actual.termination()).isEqualTo(MATCH);
    assertThat(actual.completedUses()).isEqualTo(3);
    assertThat(applies).hasValue(3);
    assertThat(matches).hasValue(3);
  }

  @Test
  void unchangedAppliedStateCountsAndNoDefault500CapOrAutomaticSearchCapIsImposed() {
    var start = rare(82);
    var fixed = unchanged(start);
    var calls = new AtomicInteger();
    var actual =
        new ChaosRepeatRunner(
                (s, a, o, r) -> {
                  calls.incrementAndGet();
                  return fixed;
                })
            .run(start, WorkbenchCurrency.CHAOS, Set.of(), 501, new Random(1), s -> false);
    assertThat(actual.completedUses()).isEqualTo(501);
    assertThat(calls).hasValue(501);
    assertThat(actual.state()).isSameAs(start);
    assertThat(actual.lastResult()).isSameAs(fixed);
    assertThat(actual.termination()).isEqualTo(LIMIT);
  }

  @Test
  void unavailableFirstUseCountsZeroAndNeverCallsMatcherOrConsumesOmens() {
    var start = SolarAmulet.initial(catalog);
    var omens = Set.of(WorkbenchOmen.WHITTLING.id());
    var actual =
        runner.run(
            start,
            WorkbenchCurrency.CHAOS,
            omens,
            10,
            new Random(1),
            s -> {
              throw new AssertionError("Unavailable result must not be matched");
            });
    assertThat(actual.termination()).isEqualTo(INAPPLICABLE);
    assertThat(actual.completedUses()).isZero();
    assertThat(actual.state()).isEqualTo(start);
    assertThat(actual.consumedOmens()).isEmpty();
    assertThat(actual.remainingOmens()).isEqualTo(omens);
    assertThat(actual.lastResult().applied()).isFalse();
  }

  @Test
  void laterUnavailableResultPreservesCompletedUsesAndFinalAttempt() {
    var start = rare(82);
    var rejected =
        simulator.apply(
            SolarAmulet.initial(catalog), WorkbenchCurrency.CHAOS, Set.of(), new Random(1));
    var calls = new AtomicInteger();
    var actual =
        new ChaosRepeatRunner(
                (s, a, o, r) -> calls.incrementAndGet() == 1 ? unchanged(start) : rejected)
            .run(start, WorkbenchCurrency.CHAOS, Set.of(), 10, new Random(1), s -> false);
    assertThat(actual.termination()).isEqualTo(INAPPLICABLE);
    assertThat(actual.completedUses()).isEqualTo(1);
    assertThat(actual.state()).isSameAs(start);
    assertThat(actual.lastResult()).isSameAs(rejected);
    assertThat(calls).hasValue(2);
  }

  @Test
  void onlyInitialMatchingOmensAreConsumedOnceAndUnrelatedOmensSurvive() {
    var consumed = WorkbenchOmen.SINISTRAL_ERASURE.id();
    var unrelated = WorkbenchOmen.BLESSED.id();
    var omens = new HashSet<>(Set.of(consumed, unrelated));
    var calls = new AtomicInteger();
    var checked =
        new ChaosRepeatRunner(
            (s, a, o, r) -> {
              assertThat(o).contains(unrelated);
              if (calls.incrementAndGet() > 1) assertThat(o).doesNotContain(consumed);
              return simulator.apply(s, a, o, r);
            });
    var actual =
        checked.run(rare(82), WorkbenchCurrency.CHAOS, omens, 7, new Random(42), s -> false);
    assertThat(actual.completedUses()).isEqualTo(7);
    assertThat(actual.consumedOmens()).containsExactlyEntriesOf(Map.of(consumed, 1));
    assertThat(actual.remainingOmens()).containsExactly(unrelated);
    assertThat(actual.lastResult().consumedOmens()).isEmpty();
    assertThat(actual.lastResult().events()).hasSize(2);
    assertThat(omens).containsExactlyInAnyOrder(consumed, unrelated);
    assertThatThrownBy(() -> actual.consumedOmens().put("other", 1))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> actual.remainingOmens().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void stopBeforeFirstUseHasNoSingleUseResult() {
    var start = rare(82);
    var actual =
        new ChaosRepeatRunner(
                (s, a, o, r) -> {
                  throw new AssertionError("Stop must be checked before apply");
                })
            .run(
                start, WorkbenchCurrency.CHAOS, Set.of(), 5, new Random(1), s -> false, () -> true);
    assertThat(actual)
        .isEqualTo(new Outcome(start, 0, Map.of(), Set.of(), null, STOP_REQUESTED, null));
  }

  @Test
  void stopBetweenUsesPreservesTheCompletedStateAndOmenConsumption() {
    var polls = new AtomicInteger();
    var start = rare(82);
    var omens = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.BLESSED.id());
    var expected = runner.run(start, WorkbenchCurrency.CHAOS, omens, 2, new Random(42), s -> false);
    var actual =
        runner.run(
            start,
            WorkbenchCurrency.CHAOS,
            omens,
            5,
            new Random(42),
            s -> false,
            () -> polls.incrementAndGet() > 2);
    assertSameCompletedFacts(actual, expected);
    assertThat(actual.termination()).isEqualTo(STOP_REQUESTED);
    assertThat(actual.failureStage()).isNull();
  }

  @Test
  void applyExceptionPreservesPriorCompletedFactsWithoutRetainingTheException() {
    var start = rare(82);
    var omens = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id());
    var expected = runner.run(start, WorkbenchCurrency.CHAOS, omens, 2, new Random(42), s -> false);
    var calls = new AtomicInteger();
    var actual =
        new ChaosRepeatRunner(
                (s, a, o, r) -> {
                  if (calls.incrementAndGet() == 3)
                    throw new IllegalArgumentException("private rule detail");
                  return simulator.apply(s, a, o, r);
                })
            .run(start, WorkbenchCurrency.CHAOS, omens, 5, new Random(42), s -> false);
    assertSameCompletedFacts(actual, expected);
    assertThat(actual.termination()).isEqualTo(ERROR);
    assertThat(actual.failureStage()).isEqualTo(APPLY);
    assertThat(actual.toString()).doesNotContain("private rule detail", "IllegalArgumentException");
  }

  @Test
  void matcherExceptionIncludesTheJustCompletedUseAndConsumedOmen() {
    var start = rare(82);
    var omens = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id());
    var expected = runner.run(start, WorkbenchCurrency.CHAOS, omens, 1, new Random(42), s -> false);
    var actual =
        runner.run(
            start,
            WorkbenchCurrency.CHAOS,
            omens,
            5,
            new Random(42),
            s -> {
              throw new IllegalStateException("private matcher detail");
            });
    assertSameCompletedFacts(actual, expected);
    assertThat(actual.termination()).isEqualTo(ERROR);
    assertThat(actual.failureStage()).isEqualTo(MATCHER);
    assertThat(actual.toString()).doesNotContain("private matcher detail");
  }

  @Test
  void stopSignalExceptionAlsoPreservesCompletedFacts() {
    var start = rare(82);
    var polls = new AtomicInteger();
    var expected =
        runner.run(start, WorkbenchCurrency.CHAOS, Set.of(), 2, new Random(42), s -> false);
    var actual =
        runner.run(
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            5,
            new Random(42),
            s -> false,
            () -> {
              if (polls.incrementAndGet() == 3) throw new IllegalStateException("stop failed");
              return false;
            });
    assertSameCompletedFacts(actual, expected);
    assertThat(actual.termination()).isEqualTo(ERROR);
    assertThat(actual.failureStage()).isEqualTo(STOP_SIGNAL);
  }

  @Test
  void existingRuleExceptionBeforeAnyUseHasZeroCompletedFacts() {
    var start = rare(82);
    var actual =
        runner.run(
            start, WorkbenchCurrency.CHAOS, Set.of("unknown-omen"), 5, new Random(1), s -> false);
    assertThat(actual.termination()).isEqualTo(ERROR);
    assertThat(actual.failureStage()).isEqualTo(APPLY);
    assertThat(actual.state()).isSameAs(start);
    assertThat(actual.completedUses()).isZero();
    assertThat(actual.lastResult()).isNull();
    assertThat(actual.consumedOmens()).isEmpty();
    assertThat(actual.remainingOmens()).containsExactly("unknown-omen");
  }

  @Test
  void rejectsUnsupportedActionsNonpositiveLimitsAndMissingInputsBeforeApply() {
    var start = rare(82);
    var noApply =
        new ChaosRepeatRunner(
            (s, a, o, r) -> {
              throw new AssertionError("Invalid input");
            });
    for (var action : WorkbenchCurrency.values()) {
      if (Set.of(
              WorkbenchCurrency.CHAOS,
              WorkbenchCurrency.GREATER_CHAOS,
              WorkbenchCurrency.PERFECT_CHAOS)
          .contains(action)) continue;
      assertThatIllegalArgumentException()
          .isThrownBy(() -> noApply.run(start, action, Set.of(), 1, new Random(1), s -> false));
    }
    for (int max : List.of(0, -1, Integer.MIN_VALUE))
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  noApply.run(
                      start, WorkbenchCurrency.CHAOS, Set.of(), max, new Random(1), s -> false));
    for (int missing = 0; missing < 6; missing++) {
      final int index = missing;
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  noApply.run(
                      index == 0 ? null : start,
                      index == 1 ? null : WorkbenchCurrency.CHAOS,
                      index == 2 ? null : Set.of(),
                      1,
                      index == 3 ? null : new Random(1),
                      index == 4 ? null : (Predicate<ItemState>) s -> false,
                      index == 5 ? null : (BooleanSupplier) () -> false));
    }
    assertThatNullPointerException().isThrownBy(() -> new ChaosRepeatRunner(null));
  }

  @Test
  void outcomeContainsNoIntermediateStateEventOrAssumptionCollectionsOrThrowable() {
    // Only bounded omen bookkeeping may be a collection; no use history or Throwable may escape.
    for (var component : Outcome.class.getRecordComponents()) {
      assertThat(Throwable.class.isAssignableFrom(component.getType())).isFalse();
      if (Collection.class.isAssignableFrom(component.getType())
          || Map.class.isAssignableFrom(component.getType()))
        assertThat(component.getGenericType().getTypeName())
            .isIn(
                "java.util.Map<java.lang.String, java.lang.Integer>",
                "java.util.Set<java.lang.String>");
    }
  }

  private WorkbenchSimulator.Result unchanged(ItemState state) {
    return new WorkbenchSimulator.Result(
        simulator.ruleVersion(),
        simulator.ledgerVersion(),
        state.snapshotId(),
        state,
        WorkbenchCurrency.CHAOS,
        true,
        "",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        QualityLimitRules.describe(state, catalog));
  }

  private static void assertSameCompletedFacts(Outcome actual, Outcome expected) {
    assertThat(actual.state()).isEqualTo(expected.state());
    assertThat(actual.completedUses()).isEqualTo(expected.completedUses());
    assertThat(actual.consumedOmens()).isEqualTo(expected.consumedOmens());
    assertThat(actual.remainingOmens()).isEqualTo(expected.remainingOmens());
    assertThat(actual.lastResult()).isEqualTo(expected.lastResult());
  }
}
