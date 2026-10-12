package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.ModifierDefinition;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrbitCellTest {
  @Test
  void jointAssignmentsExcludeRepeatedFamiliesAndRestoreUnequalRollCardinalities() {
    var families = new ArrayList<OrbitCell.Family>();
    for (int i = 1; i <= 3; i++) {
      var low =
          new TierCell(
              List.of(new TierCell.Variant("f" + i + "-low", "stat" + i, 1, i, Fraction.ONE)));
      var high =
          new TierCell(
              List.of(new TierCell.Variant("f" + i + "-high", "stat" + i, 10, 10, Fraction.ONE)));
      families.add(new OrbitCell.Family(Set.of("f" + i), Map.of(1, low, 2, high)));
    }
    var profile =
        new OrbitCell.Profile(
            "fixture", ModifierDefinition.AffixType.SUFFIX, families, Map.of(1, 1L, 2, 3L));
    var cell = new OrbitCell(profile, Map.of(1, 1, 2, 1));
    assertThat(cell.assignments()).hasSize(6);
    assertThat(cell.cardinality()).isEqualTo(BigInteger.valueOf(12));
    var seen = new HashSet<Object>();
    var familyMass = new HashMap<String, Fraction>();
    var total = Fraction.ZERO;
    for (var offset = BigInteger.ZERO;
        offset.compareTo(cell.cardinality()) < 0;
        offset = offset.add(BigInteger.ONE)) {
      var value = cell.concrete(offset);
      assertThat(value.modifiers().stream().map(m -> m.modifierId().substring(0, 2)).distinct())
          .hasSize(2);
      assertThat(seen.add(value.modifiers())).isTrue();
      var low =
          value.modifiers().stream()
              .filter(m -> m.modifierId().endsWith("-low"))
              .findFirst()
              .orElseThrow();
      int family = Integer.parseInt(low.modifierId().substring(1, 2));
      assertThat(value.probability()).isEqualTo(Fraction.of(1, 6L * family));
      familyMass.merge(low.modifierId(), value.probability(), Fraction::add);
      total = total.add(value.probability());
    }
    assertThat(total).isEqualTo(Fraction.ONE);
    assertThat(familyMass.values()).containsOnly(Fraction.of(1, 3));
  }
}
