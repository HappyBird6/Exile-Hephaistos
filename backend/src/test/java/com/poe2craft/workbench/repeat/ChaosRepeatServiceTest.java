package com.poe2craft.workbench.repeat;

import static com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import static com.poe2craft.workbench.repeat.RepeatTestWorkbench.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChaosRepeatServiceTest {
  private ChaosRepeatService service() {
    return new ChaosRepeatService(WORKBENCH, DISPLAY, "test-ruleset");
  }

  @Test
  void cancellationPreservesOnlyCompletedUsesForConcreteAndAbyssStates() {
    for (State state :
        List.of(new Concrete(rare()), new Abyss(abyss(true)), new Abyss(abyss(false)))) {
      var checks = new AtomicInteger();
      var result =
          service()
              .run(
                  input(state, WorkbenchCurrency.CHAOS, 1000, Set.of(), "__never_matches__", "en"),
                  new Random(42),
                  () -> checks.incrementAndGet() > 3);
      assertThat(result.actualUses()).isEqualTo(3);
      assertThat(result.termination()).isEqualTo("ERROR");
      assertThat(result.failure().stage()).isEqualTo("STOP_SIGNAL");
      assertThat(result.lastAppliedWitness()).isNotNull();
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = WorkbenchCurrency.class,
      names = {"CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS"})
  void concreteAndBothAbyssShapesEqualExistingManualExecutorsWithSameSeed(
      WorkbenchCurrency action) {
    var omens = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.BLESSED.id());
    for (int seed : List.of(1, 42, 104729)) {
      var initial = rare();
      var current = initial;
      var random = new Random(seed);
      var remaining = omens;
      var consumed = new HashMap<String, Integer>();
      WorkbenchSimulator.Result last = null;
      for (int i = 0; i < 13; i++) {
        last = WORKBENCH.apply(current, action, remaining, random);
        assertThat(last.applied()).isTrue();
        current = last.state();
        remaining = Set.copyOf(last.remainingOmens());
        last.consumedOmens().forEach(id -> consumed.merge(id, 1, Integer::sum));
      }
      var actual =
          service()
              .run(
                  input(new Concrete(initial), action, 13, omens, "__never_matches__", "en"),
                  new Random(seed));
      assertThat(actual.finalState()).isEqualTo(new Concrete(current));
      assertThat(actual.actualUses()).isEqualTo(13);
      assertThat(actual.termination()).isEqualTo("LIMIT");
      assertThat(actual.lastAppliedWitness()).isEqualTo(ConcreteWitness.from(last));
      assertThat(actual.consumedOmens()).isEqualTo(consumed);
      assertThat(actual.remainingOmens()).isEqualTo(remaining);
      assertThat(initial).isEqualTo(rare());
      for (boolean hidden : List.of(false, true)) {
        var start = abyss(hidden);
        var state = start;
        random = new Random(seed);
        remaining = omens;
        consumed.clear();
        AbyssCurrencyExecutor.Result step = null;
        for (int i = 0; i < 13; i++) {
          step = WORKBENCH.applyAbyssCurrency(state, action, remaining, random);
          assertThat(step.applied()).isTrue();
          state = step.state();
          remaining = Set.copyOf(step.remainingOmens());
          step.consumedOmens().forEach(id -> consumed.merge(id, 1, Integer::sum));
        }
        actual =
            service()
                .run(
                    input(new Abyss(start), action, 13, omens, "__never_matches__", "en"),
                    new Random(seed));
        assertThat(actual.finalState()).isEqualTo(new Abyss(state));
        assertThat(actual.actualUses()).isEqualTo(13);
        assertThat(actual.lastAppliedWitness()).isEqualTo(AbyssWitness.from(step));
        assertThat(actual.consumedOmens()).isEqualTo(consumed);
        assertThat(actual.remainingOmens()).isEqualTo(remaining);
        assertThat(start).isEqualTo(abyss(hidden));
      }
    }
  }

  @Test
  void firstAppliedMatchAndExactThousandLimitAreSupportedInBothBranches() {
    for (State start : List.of(new Concrete(rare()), new Abyss(abyss(true)))) {
      var first =
          service()
              .run(input(start, WorkbenchCurrency.CHAOS, 100, Set.of(), ".*", "en"), new Random(1));
      assertThat(first.actualUses()).isEqualTo(1);
      assertThat(first.termination()).isEqualTo("MATCH");
      var max =
          service()
              .run(
                  input(start, WorkbenchCurrency.CHAOS, 1000, Set.of(), "__never_matches__", "en"),
                  new Random(42));
      assertThat(max.actualUses()).isEqualTo(1000);
      assertThat(max.termination()).isEqualTo("LIMIT");
      assertThat(max.requestBinding().maximumUses()).isEqualTo(1000);
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = WorkbenchCurrency.class,
      names = {"CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS"})
  void sixExplicitFracturesAndUnchangedMetadataSurviveBothBranches(WorkbenchCurrency action) {
    var start = rare();
    for (int i = 0; i < 2; i++)
      start = WORKBENCH.apply(start, WorkbenchCurrency.EXALTED, Set.of(), new Random(i)).state();
    start = WORKBENCH.apply(start, WorkbenchCurrency.FRACTURING, Set.of(), new Random(3)).state();
    var locked =
        start.explicits().stream().filter(ModifierInstance::fractured).findFirst().orElseThrow();
    for (State before :
        List.of(
            new Concrete(start),
            new Abyss(AbyssCraftingState.from(start, WORKBENCH.abyssCatalog(start))))) {
      var result =
          service()
              .run(input(before, action, 17, Set.of(), "__never_matches__", "en"), new Random(42));
      assertThat(result.actualUses()).isEqualTo(17);
      assertThat(result.finalState().displayItem().explicits()).hasSize(6).contains(locked);
      assertThat(result.finalState().displayItem().implicits()).isEqualTo(start.implicits());
      assertThat(result.finalState().displayItem().augmentSockets())
          .isEqualTo(start.augmentSockets());
      assertThat(result.finalState().displayItem().itemLevel()).isEqualTo(start.itemLevel());
    }
  }

  @Test
  void offeredHiddenSlotStopsWithZeroUsesWithoutInventingAResult() {
    var start = abyss(true);
    var catalog = WORKBENCH.abyssCatalog(start.item());
    var slot = start.slots().stream().filter(s -> s.modifier() == null).findFirst().orElseThrow();
    var candidates =
        start.eligibleRevealIds(slot.id(), catalog).stream()
            .limit(3)
            .map(
                id -> {
                  var definition = catalog.find(id).orElseThrow();
                  var values = new HashMap<String, Long>();
                  definition.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(id, values);
                })
            .toList();
    var offered = start.observe(slot.id(), candidates, catalog);
    var result =
        service()
            .run(
                input(new Abyss(offered), WorkbenchCurrency.CHAOS, 5, Set.of(), ".*", "en"),
                new Random(1));
    assertThat(result.finalState()).isEqualTo(new Abyss(offered));
    assertThat(result.actualUses()).isZero();
    assertThat(result.lastAppliedWitness()).isNull();
    assertThat(result.termination()).isEqualTo("INAPPLICABLE");
    assertThat(result.consumedOmens()).isEmpty();
  }

  @Test
  void rejectedFinalAttemptAndApplyExceptionsKeepActualLastSuccessfulEvidence() {
    for (boolean throwsFailure : List.of(false, true)) {
      var count = new AtomicInteger();
      var service =
          new ChaosRepeatService(
              WORKBENCH,
              DISPLAY,
              "test-ruleset",
              (s, a, o, r) -> {
                if (count.incrementAndGet() == 2) {
                  if (throwsFailure) throw new IllegalStateException("private engine trace");
                  return new WorkbenchSimulator.Result(
                      "rule",
                      "ledger",
                      s.snapshotId(),
                      s,
                      a,
                      false,
                      "Blocked",
                      List.of(),
                      List.of(),
                      List.of(),
                      List.copyOf(o),
                      null);
                }
                return WORKBENCH.apply(s, a, o, r);
              },
              (s, a, o, r) -> {
                if (count.incrementAndGet() == 2) {
                  if (throwsFailure) throw new IllegalStateException("private engine trace");
                  return new AbyssCurrencyExecutor.Result(
                      s, a, false, "Blocked", List.of(), List.copyOf(o));
                }
                return WORKBENCH.applyAbyssCurrency(s, a, o, r);
              });
      var omens = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.BLESSED.id());
      for (State start : List.of(new Concrete(rare()), new Abyss(abyss(true)))) {
        count.set(0);
        var expected =
            service()
                .run(
                    input(start, WorkbenchCurrency.CHAOS, 1, omens, "__never_matches__", "en"),
                    new Random(42));
        var actual =
            service.run(
                input(start, WorkbenchCurrency.CHAOS, 5, omens, "__never_matches__", "en"),
                new Random(42));
        assertThat(actual.finalState()).isEqualTo(expected.finalState());
        assertThat(actual.actualUses()).isEqualTo(1);
        assertThat(actual.consumedOmens()).isEqualTo(expected.consumedOmens());
        assertThat(actual.remainingOmens()).isEqualTo(expected.remainingOmens());
        assertThat(actual.lastAppliedWitness()).isEqualTo(expected.lastAppliedWitness());
        assertThat(actual.termination()).isEqualTo(throwsFailure ? "ERROR" : "INAPPLICABLE");
        assertThat(actual.toString()).doesNotContain("private engine trace");
      }
    }
  }

  @Test
  void matcherFailureKeepsJustCompletedCanonicalStateInBothBranches() {
    for (State start : List.of(new Concrete(rare()), new Abyss(abyss(true)))) {
      var formats = new AtomicInteger();
      var faulty =
          mock(
              ChaosDisplayResources.class,
              invocation -> {
                if (formats.incrementAndGet() == 2)
                  throw new IllegalStateException("private formatter trace");
                return invocation.getMethod().invoke(DISPLAY, invocation.getArguments());
              });
      var actual =
          new ChaosRepeatService(WORKBENCH, faulty, "test-ruleset")
              .run(input(start, WorkbenchCurrency.CHAOS, 5, Set.of(), ".*", "en"), new Random(42));
      var expected =
          service()
              .run(input(start, WorkbenchCurrency.CHAOS, 1, Set.of(), ".*", "en"), new Random(42));
      assertThat(actual.finalState()).isEqualTo(expected.finalState());
      assertThat(actual.actualUses()).isEqualTo(1);
      assertThat(actual.lastAppliedWitness()).isEqualTo(expected.lastAppliedWitness());
      assertThat(actual.failure()).isEqualTo(new Failure("REPEAT_EXECUTION_FAILED", "MATCHER"));
    }
  }

  @Test
  void admissionHasNoQueueAndPermitIsReleasedAfterCompletionOrPreflightFailure() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var controlled =
        new ChaosRepeatService(
            WORKBENCH,
            DISPLAY,
            "test-ruleset",
            (s, a, o, r) -> {
              entered.countDown();
              try {
                if (!release.await(15, TimeUnit.SECONDS))
                  throw new IllegalStateException("Test synchronization failed");
              } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException();
              }
              return WORKBENCH.apply(s, a, o, r);
            },
            WORKBENCH::applyAbyssCurrency);
    var input = input(new Concrete(rare()), WorkbenchCurrency.CHAOS, 1, Set.of(), ".*", "en");
    try (var executor = Executors.newSingleThreadExecutor()) {
      var running = executor.submit(() -> controlled.run(input, new Random(1)));
      try {
        assertThat(entered.await(15, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> controlled.run(input, new Random(1)))
            .isInstanceOf(RepeatRejected.class)
            .satisfies(error -> assertThat(((RepeatRejected) error).status()).isEqualTo(429));
      } finally {
        release.countDown();
      }
      assertThat(running.get(15, TimeUnit.SECONDS).actualUses()).isEqualTo(1);
    }
    assertThatThrownBy(
            () ->
                controlled.run(
                    input(new Concrete(rare()), WorkbenchCurrency.CHAOS, 0, Set.of(), ".*", "en"),
                    new Random(1)))
        .isInstanceOf(RepeatRejected.class);
    assertThat(controlled.run(input, new Random(1)).actualUses()).isEqualTo(1);
  }

  @Test
  void preflightRejectsInvalidStatePolicyAndRegexWithoutApplying() {
    var calls = new AtomicInteger();
    var guarded =
        new ChaosRepeatService(
            WORKBENCH,
            DISPLAY,
            "test-ruleset",
            (s, a, o, r) -> {
              calls.incrementAndGet();
              return WORKBENCH.apply(s, a, o, r);
            },
            WORKBENCH::applyAbyssCurrency);
    var start = rare();
    for (var bad :
        List.of(
            input(new Concrete(start), WorkbenchCurrency.CHAOS, 0, Set.of(), "a", "en"),
            input(new Concrete(start), WorkbenchCurrency.CHAOS, 1001, Set.of(), "a", "en"),
            input(new Concrete(start), WorkbenchCurrency.EXALTED, 1, Set.of(), "a", "en"),
            input(new Concrete(start), WorkbenchCurrency.CHAOS, 1, Set.of("unknown"), "a", "en"),
            input(new Concrete(start), WorkbenchCurrency.CHAOS, 1, Set.of(), "(?i)a", "en"),
            input(new Concrete(start), WorkbenchCurrency.CHAOS, 1, Set.of(), "a", "unsupported")))
      assertThatThrownBy(() -> guarded.run(bad, new Random(1))).isInstanceOf(RepeatRejected.class);
    var sanctified =
        new ItemState(
            start.snapshotId(),
            start.baseItemId(),
            82,
            start.rarity(),
            start.implicits(),
            start.explicits(),
            Set.of(ItemState.Condition.SANCTIFIED));
    assertThatThrownBy(
            () ->
                guarded.run(
                    input(
                        new Concrete(sanctified), WorkbenchCurrency.CHAOS, 1, Set.of(), "a", "en"),
                    new Random(1)))
        .isInstanceOf(RepeatRejected.class);
    assertThat(calls).hasValue(0);
  }

  @Test
  void wireHasOnlyFinalStateAndStateFreeFlatWitness() throws Exception {
    for (State start : List.of(new Concrete(rare()), new Abyss(abyss(true)))) {
      var actual =
          service()
              .run(
                  input(start, WorkbenchCurrency.CHAOS, 2, Set.of(), "__never_matches__", "en"),
                  new Random(42));
      var value = JSON.valueToTree(actual);
      assertThat(value.path("lastAppliedWitness").has("state")).isFalse();
      assertThat(value.path("lastAppliedWitness").has("item")).isFalse();
      assertThat(value.path("lastAppliedWitness").has("abyss")).isFalse();
      assertThat(value.path("requestBinding").has("state")).isFalse();
      assertThat(value.has("frames")).isFalse();
      assertThat(value.has("before")).isFalse();
      assertThat(value.path("finalState").size()).isEqualTo(2);
      if (start instanceof Abyss) {
        assertThat(value.path("lastAppliedWitness").has("events")).isFalse();
        assertThat(value.path("lastAppliedWitness").has("assumptions")).isFalse();
      }
    }
  }
}
