package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrbitCellMixtureTest {
  OrbitCell.Profile profile() {
    var families = new ArrayList<OrbitCell.Family>();
    for (int i = 1; i <= 3; i++)
      families.add(
          new OrbitCell.Family(
              Set.of("f" + i),
              Map.of(
                  1,
                      new TierCell(
                          List.of(
                              new TierCell.Variant(
                                  "f" + i + "-low", "stat" + i, 1, i, Fraction.ONE))),
                  2,
                      new TierCell(
                          List.of(
                              new TierCell.Variant(
                                  "f" + i + "-high", "stat" + i, 10, 10, Fraction.ONE))))));
    return new OrbitCell.Profile(
        "fixture", ModifierDefinition.AffixType.SUFFIX, families, Map.of(1, 1L, 2, 3L));
  }

  Map<List<ModifierInstance>, Fraction> restore(OrbitCell cell) {
    var result = new HashMap<List<ModifierInstance>, Fraction>();
    for (var n = BigInteger.ZERO; n.compareTo(cell.cardinality()) < 0; n = n.add(BigInteger.ONE)) {
      var concrete = cell.concrete(n);
      assertThat(concrete.modifiers().stream().map(m -> m.modifierId().substring(0, 2)).distinct())
          .hasSize(2);
      assertThat(result.put(concrete.modifiers(), concrete.probability())).isNull();
    }
    return result;
  }

  @Test
  void categoryMixtureKeepsJointFamilyExclusionAndExactUnequalRollWeights() {
    var profile = profile();
    var low = new OrbitCell(profile, Map.of(1, 2));
    var varied = new OrbitCell(profile, Map.of(1, 1, 2, 1));
    var mixed = OrbitCell.mix(Map.of(low, Fraction.of(1, 3), varied, Fraction.of(2, 3)));
    assertThat(mixed.occupancy()).isEqualTo(2);
    assertThat(mixed.cardinality()).isEqualTo(low.cardinality().add(varied.cardinality()));
    var expected = new HashMap<List<ModifierInstance>, Fraction>();
    restore(low).forEach((state, p) -> expected.put(state, p.multiply(Fraction.of(1, 3))));
    restore(varied).forEach((state, p) -> expected.put(state, p.multiply(Fraction.of(2, 3))));
    assertThat(restore(mixed)).isEqualTo(expected);
    assertThat(restore(mixed).values().stream().reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    assertThatThrownBy(mixed::counts).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(mixed::assignments).isInstanceOf(IllegalStateException.class);
    var nested = OrbitCell.mix(Map.of(mixed, Fraction.of(1, 2), low, Fraction.of(1, 2)));
    assertThat(nested.basis())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(low, Fraction.of(2, 3), varied, Fraction.of(1, 3)));
    assertThat(OrbitCell.mix(Map.of(mixed, Fraction.ONE))).isEqualTo(mixed);
    assertThatThrownBy(
            () ->
                OrbitCell.mix(
                    Map.of(
                        low,
                        Fraction.of(1, 2),
                        new OrbitCell(profile, Map.of(1, 1)),
                        Fraction.of(1, 2))))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
