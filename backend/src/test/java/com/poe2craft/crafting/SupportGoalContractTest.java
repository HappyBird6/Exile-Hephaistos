package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Independent set-based oracle for the existing Solar goal contract. */
class SupportGoalContractTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final SupportGoals goals = new SupportGoals(catalog);
  private final StateBucket root = StateBucket.from(SolarAmulet.initial(catalog, 100, 15));

  @Test
  void everyEligibleFamilyVariantAndThresholdAgreesWithDistinctFamilyOracle() {
    for (var family : goals.families()) {
      var thresholds = new TreeSet<Integer>();
      family.tiers().forEach(t -> thresholds.add(t.tier()));
      for (var tier : family.tiers()) {
        var state = root.with(ItemState.Rarity.RARE, List.of(tier.modifierId()));
        for (int threshold : thresholds) {
          var condition = new SupportGoals.Condition(family.id(), threshold);
          verify(state, new SupportGoals.Goal(List.of(condition), List.of(), 0));
          verify(state, new SupportGoals.Goal(List.of(), List.of(condition), 1));
        }
      }
    }
  }

  @Test
  void candidateQuorumAndRequiredConditionsAreIndependentAcrossTierCombinations() {
    var life = family("IncreasedLife");
    var spirit = family("BaseSpirit");
    var cast = family("IncreasedCastSpeed");
    for (var l : life.tiers()) {
      for (var s : spirit.tiers()) {
        for (var c : cast.tiers()) {
          var ids = List.of(l.modifierId(), s.modifierId(), c.modifierId());
          var state = root.with(ItemState.Rarity.RARE, ids);
          for (int quorum = 0; quorum <= 2; quorum++) {
            var goal =
                new SupportGoals.Goal(
                    List.of(new SupportGoals.Condition(life.id(), 2)),
                    List.of(
                        new SupportGoals.Condition(spirit.id(), 2),
                        new SupportGoals.Condition(cast.id(), 1)),
                    quorum);
            verify(state, goal);
          }
        }
      }
    }
  }

  @Test
  void multipleTiersOfOneCandidateFamilyCannotSupplyTwoVotes() {
    var duplicated =
        new SupportGoals.Goal(
            List.of(),
            List.of(
                new SupportGoals.Condition("IncreasedLife", 1),
                new SupportGoals.Condition("IncreasedLife", 2)),
            2);
    var id = family("IncreasedLife").tiers().getFirst().modifierId();
    var result = goals.assess(root.with(ItemState.Rarity.RARE, List.of(id)), duplicated);
    assertThat(result.valid()).isFalse();
    assertThat(result.achieved()).isFalse();
    assertThat(result.status()).isEqualTo("INVALID_GOAL");
  }

  @Test
  void aStateContainingTwoTiersOfOneFamilyIsRejectedBeforeCounting() {
    var tiers = family("IncreasedLife").tiers();
    var state =
        root.with(
            ItemState.Rarity.RARE,
            List.of(tiers.getFirst().modifierId(), tiers.getLast().modifierId()));
    var goal =
        new SupportGoals.Goal(
            List.of(), List.of(new SupportGoals.Condition("IncreasedLife", 2)), 1);
    assertThatIllegalArgumentException().isThrownBy(() -> goals.assess(state, goal));
  }

  private SupportGoals.Family family(String id) {
    return goals.families().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
  }

  private void verify(StateBucket state, SupportGoals.Goal goal) {
    var qualifiedRequired = qualifiedFamilies(state, goal.required());
    var qualifiedCandidates = qualifiedFamilies(state, goal.candidates());
    var assessment = goals.assess(state, goal);
    String context = state.modifierIds() + " / " + goal;
    assertThat(assessment.valid()).as(context).isTrue();
    assertThat(assessment.requiredMatched()).as(context).isEqualTo(qualifiedRequired.size());
    assertThat(assessment.candidatesMatched()).as(context).isEqualTo(qualifiedCandidates.size());
    assertThat(assessment.achieved())
        .as(context)
        .isEqualTo(
            qualifiedRequired.size() == goal.required().size()
                && qualifiedCandidates.size() >= goal.candidateCount());
  }

  private Set<String> qualifiedFamilies(
      StateBucket state, List<SupportGoals.Condition> conditions) {
    var qualified = new HashSet<String>();
    for (var condition : conditions) {
      for (var id : state.modifierIds()) {
        var modifier = catalog.find(id).orElseThrow();
        if (modifier.familyIds().contains(condition.family())
            && modifier.tier() <= condition.minimumTier()) {
          qualified.add(condition.family());
        }
      }
    }
    return qualified;
  }
}
