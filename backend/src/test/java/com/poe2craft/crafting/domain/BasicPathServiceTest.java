package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.BasicPathService;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class BasicPathServiceTest {
  @Test
  void jointCandidateNeverGetsPrunedByRenewalAndActualSolarHasExact500Points() {
    var a = fixture.a;
    var joint =
        new ModifierDefinition(
            a.id(),
            a.name(),
            a.layer(),
            a.affixType(),
            a.familyIds(),
            a.requiredItemLevel(),
            a.weight(),
            a.tier(),
            a.text(),
            List.of(a.stats().getFirst(), new ModifierDefinition.StatRange("joint", 1, 2)),
            a.tags(),
            a.sourceUrl());
    var jointCatalog = fixture.catalog(List.of(fixture.implicit, joint, fixture.b));
    var jointService = new BasicPathService(jointCatalog, fixture.RULESET);
    var state =
        new BasicCurrencyState(
            fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("b", 3))),
            jointService.provenance());
    var unsupported =
        jointService.calculate(
            request(state, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("a"))),
            false,
            () -> false);
    assertThat(unsupported.renewalProof()).isNull();
    assertThat(unsupported.distribution().points().getLast().unresolved()).isEqualTo(Fraction.ONE);
    var catalog = ItemCatalogLoader.loadDefault();
    var real = new BasicPathService(catalog, "test-current");
    var item = SolarAmulet.initial(catalog);
    var eligible =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.layer() == ModifierDefinition.Layer.EXPLICIT
                        && m.weight() > 0
                        && m.requiredItemLevel() <= item.itemLevel()
                        && m.stats().size() == 1)
            .toList();
    var root = eligible.getFirst();
    var stat = root.stats().getFirst();
    var rare =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            ItemState.Rarity.RARE,
            item.implicits(),
            List.of(new ModifierInstance(root.id(), Map.of(stat.id(), stat.min()))),
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality());
    var target = eligible.stream().filter(m -> !m.id().equals(root.id())).findFirst().orElseThrow();
    var result =
        real.calculate(
            request(
                new BasicCurrencyState(rare, real.provenance()),
                WorkbenchCurrency.CHAOS,
                new BasicPathService.Target(null, Set.of(target.id()))),
            false,
            () -> false);
    assertThat(result.renewalProof()).isNotNull();
    assertThat(result.renewalProof().emptyState().item().implicits()).isEqualTo(rare.implicits());
    for (var point : result.distribution().points()) {
      assertThat(point.status()).isEqualTo("COMPLETE");
      assertThat(point.unresolved()).isEqualTo(Fraction.ZERO);
      assertThat(point.lower().add(point.active())).isEqualTo(Fraction.ONE);
    }
  }

  @Test
  void renewalMatchesGeneralEnumerationAndCyclePhaseOrder() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var r = request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("b")));
    var optimized = service.calculate(r, false, () -> false);
    var enumerated =
        new com.poe2craft.crafting.application.FirstHitCalculator<BasicCurrencyState>()
            .calculate(
                start,
                r.observations(),
                state ->
                    state.item().explicits().stream().anyMatch(m -> m.modifierId().equals("b"))
                        ? com.poe2craft.crafting.application.FirstHitCalculator.Match.HIT
                        : com.poe2craft.crafting.application.FirstHitCalculator.Match.MISS,
                (state, step) -> {
                  var k = fixture.kernel.expand(state, WorkbenchCurrency.CHAOS, Set.of(), 1000);
                  return new com.poe2craft.crafting.application.FirstHitCalculator.Kernel<>(
                      k.outcomes().stream()
                          .map(
                              o ->
                                  new com.poe2craft.crafting.application.FirstHitCalculator.Edge<>(
                                      o.state(), o.probability()))
                          .toList(),
                      k.unresolved(),
                      false);
                },
                new com.poe2craft.crafting.application.FirstHitCalculator.Budget(
                    10000, 1000, 65536),
                () -> false);
    assertThat(optimized.renewalProof()).isNotNull();
    assertThat(optimized.distribution().points()).isEqualTo(enumerated.points());
    var cycle =
        new BasicPathService.Request(
            start,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.EXALTED),
                BasicPathService.PolicyMode.REPEAT_CYCLE),
            r.target(),
            Set.of(),
            List.of(1L, 2L, 3L, 4L));
    var phase = service.calculate(cycle, false, () -> false);
    assertThat(phase.renewalProof()).isNull();
    assertThat(
            phase.distribution().points().stream()
                .map(com.poe2craft.crafting.application.FirstHitCalculator.Point::lower))
        .containsExactly(Fraction.ZERO, Fraction.of(3, 4), Fraction.of(3, 4), Fraction.of(15, 16));
  }

  @Test
  void narrowRenewalFallsBackAndLongMaxRetainsUncomputedMass() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var r = request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("b")));
    var huge =
        new BasicPathService.Request(
            start, r.policy(), r.target(), Set.of(), List.of(500L, Long.MAX_VALUE));
    var result = service.calculate(huge, false, () -> false);
    assertThat(result.distribution().points().getFirst().status()).isEqualTo("COMPLETE");
    assertThat(result.distribution().points().getLast().status()).isEqualTo("PARTIAL");
    assertThat(result.distribution().points().getLast().upper()).isEqualTo(Fraction.ONE);
    assertThat(
            result
                .distribution()
                .points()
                .getLast()
                .lower()
                .add(result.distribution().points().getLast().unresolved()))
        .isEqualTo(Fraction.ONE);
    var multi =
        fixture.state(
            fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1), fixture.mod("b", 3))));
    assertThat(fixture.kernel.proveSingleExplicitChaos(multi, "b")).isEmpty();
    var fractured =
        fixture.state(
            fixture.item(
                ItemState.Rarity.RARE, List.of(new ModifierInstance("a", Map.of("a", 1L), true))));
    assertThat(fixture.kernel.proveSingleExplicitChaos(fractured, "b")).isEmpty();
    var other = request(start, WorkbenchCurrency.GREATER_CHAOS, r.target());
    assertThat(service.calculate(other, false, () -> false).renewalProof()).isNull();
    var hit =
        request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("a")));
    assertThat(
            service.calculate(hit, false, () -> false).distribution().points().getFirst().lower())
        .isEqualTo(Fraction.ONE);
  }

  @Test
  void realSolarAdditionPreservesPartialResidualAtAllObservationPoints() {
    var catalog = ItemCatalogLoader.loadDefault();
    var real = new BasicPathService(catalog, "test-current");
    var item = SolarAmulet.initial(catalog);
    var id =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.layer() == ModifierDefinition.Layer.EXPLICIT
                        && m.requiredItemLevel() <= item.itemLevel()
                        && m.weight() > 0)
            .findFirst()
            .orElseThrow()
            .id();
    var result =
        real.calculate(
            request(
                new BasicCurrencyState(item, real.provenance()),
                WorkbenchCurrency.TRANSMUTATION,
                new BasicPathService.Target(null, Set.of(id))),
            false,
            () -> false);
    for (var p : result.distribution().points()) {
      assertThat(p.lower().add(p.active()).add(p.dead()).add(p.unresolved()))
          .isEqualTo(Fraction.ONE);
      assertThat(p.unresolved()).isNotEqualTo(Fraction.ZERO);
    }
  }

  @Test
  void singlePassStopsAndFreshRetryDoesNotReuseCancelledMass() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var repeated =
        request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("b")));
    var once =
        new BasicPathService.Request(
            start,
            new BasicPathService.Policy(
                List.of(WorkbenchCurrency.CHAOS), BasicPathService.PolicyMode.SINGLE_PASS),
            repeated.target(),
            Set.of(),
            repeated.observations());
    var stopped = service.calculate(once, true, () -> false);
    assertThat(stopped.distribution().points().getFirst().dead()).isEqualTo(Fraction.of(1, 4));
    assertThat(stopped.distribution().points().getLast().lower()).isEqualTo(Fraction.of(3, 4));
    var interrupted = service.calculate(repeated, false, () -> true);
    assertThat(interrupted.distribution().points().getLast().unresolved()).isEqualTo(Fraction.ONE);
    var retried = service.calculate(repeated, false, () -> false);
    assertThat(retried.distribution().points().getFirst().lower()).isEqualTo(Fraction.of(3, 4));
    assertThat(retried.distribution().points().getLast().unresolved()).isEqualTo(Fraction.ZERO);
  }

  final BasicCurrencyTransitionsTest fixture = new BasicCurrencyTransitionsTest();
  final BasicPathService service = new BasicPathService(fixture.catalog, fixture.RULESET);

  BasicPathService.Request request(
      BasicCurrencyState start, WorkbenchCurrency action, BasicPathService.Target target) {
    return new BasicPathService.Request(
        start,
        new BasicPathService.Policy(List.of(action), BasicPathService.PolicyMode.REPEAT_CYCLE),
        target,
        Set.of(),
        List.of(1L, 2L, 100L, 300L, 500L));
  }

  @Test
  void chaosSyntheticGeometricDistributionWithoutAssumingConstantProbability() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var result =
        service.calculate(
            request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("b"))),
            false,
            () -> false);
    assertThat(result.distribution().points().getFirst().lower()).isEqualTo(Fraction.of(3, 4));
    assertThat(result.distribution().points().get(1).lower()).isEqualTo(Fraction.of(15, 16));
    var d = java.math.BigInteger.valueOf(4).pow(500);
    assertThat(result.distribution().points().getLast().lower())
        .isEqualTo(new Fraction(d.subtract(java.math.BigInteger.ONE), d));
    assertThat(result.recoveryIncludedInMain()).isFalse();
  }

  @Test
  void conditionalRecoveryRestoresExplicitCheckpoint() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var checkpoint = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of()));
    var result =
        service.calculate(
            request(
                start,
                WorkbenchCurrency.ANNULMENT,
                new BasicPathService.Target(checkpoint, Set.of())),
            true,
            () -> false);
    assertThat(result.purpose()).isEqualTo("CONDITIONAL_RECOVERY");
    assertThat(result.distribution().points().getFirst().lower()).isEqualTo(Fraction.ONE);
    assertThat(result.recoveryIncludedInMain()).isFalse();
  }

  @Test
  void omensAndOldProvenanceNeverBypassSupportBoundary() {
    var start = fixture.state(fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 1))));
    var r = request(start, WorkbenchCurrency.CHAOS, new BasicPathService.Target(null, Set.of("b")));
    var omen =
        new BasicPathService.Request(
            start, r.policy(), r.target(), Set.of("anything"), r.observations());
    assertThat(
            service
                .calculate(omen, false, () -> false)
                .distribution()
                .points()
                .getLast()
                .unresolved())
        .isEqualTo(Fraction.ONE);
    var old =
        new BasicCurrencyState(
            start.item(), new BasicCurrencyTransitions(fixture.catalog, "old-season").provenance());
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                service.calculate(
                    request(old, WorkbenchCurrency.CHAOS, r.target()), false, () -> false));
  }

  @Test
  void realSolarPreservedExistingStateRecovery() {
    var catalog = ItemCatalogLoader.loadDefault();
    var real = new BasicPathService(catalog, "test-current");
    var original = SolarAmulet.initial(catalog);
    var modifier =
        catalog.modifiers().values().stream()
            .filter(
                m ->
                    m.layer() == ModifierDefinition.Layer.EXPLICIT
                        && m.stats().size() == 1
                        && m.requiredItemLevel() <= original.itemLevel())
            .findFirst()
            .orElseThrow();
    var range = modifier.stats().getFirst();
    var rare =
        new ItemState(
            original.snapshotId(),
            original.baseItemId(),
            original.itemLevel(),
            ItemState.Rarity.RARE,
            original.implicits(),
            List.of(new ModifierInstance(modifier.id(), Map.of(range.id(), range.min()))),
            original.conditions(),
            original.augmentSockets(),
            original.catalystQuality());
    var empty =
        new ItemState(
            rare.snapshotId(),
            rare.baseItemId(),
            rare.itemLevel(),
            rare.rarity(),
            rare.implicits(),
            List.of(),
            rare.conditions(),
            rare.augmentSockets(),
            rare.catalystQuality());
    var result =
        real.calculate(
            request(
                new BasicCurrencyState(rare, real.provenance()),
                WorkbenchCurrency.ANNULMENT,
                new BasicPathService.Target(
                    new BasicCurrencyState(empty, real.provenance()), Set.of())),
            true,
            () -> false);
    assertThat(result.distribution().points().getFirst().lower()).isEqualTo(Fraction.ONE);
  }
}
