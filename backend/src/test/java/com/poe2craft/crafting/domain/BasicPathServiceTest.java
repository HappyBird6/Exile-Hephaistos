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
