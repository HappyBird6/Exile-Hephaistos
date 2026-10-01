package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class SupportGoalsTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final SupportGoals goals = new SupportGoals(catalog);

  private StateBucket state(int level, String family, int tier) {
    var root = StateBucket.from(SolarAmulet.initial(catalog, level, 15));
    if (family == null) return root;
    var d =
        catalog.modifiers().values().stream()
            .filter(m -> m.familyIds().contains(family) && m.tier() == tier)
            .findFirst()
            .orElseThrow();
    return root.with(ItemState.Rarity.RARE, List.of(d.id()));
  }

  private SupportGoals.Condition c(String family, int tier) {
    return new SupportGoals.Condition(family, tier);
  }

  @Test
  void tierTwoOrBetterMeansTierOneAndTwoButNotThree() {
    var goal = new SupportGoals.Goal(List.of(c("IncreasedLife", 2)), List.of(), 0);
    assertThat(goals.assess(state(82, "IncreasedLife", 1), goal).achieved()).isTrue();
    assertThat(goals.assess(state(82, "IncreasedLife", 2), goal).achieved()).isTrue();
    var low = goals.assess(state(82, "IncreasedLife", 3), goal);
    assertThat(low.achieved()).isFalse();
    assertThat(low.status()).isEqualTo("IMPOSSIBLE");
  }

  @Test
  void requiredAndCandidatesUseDistinctFamiliesAndTheirOwnThresholds() {
    var goal =
        new SupportGoals.Goal(
            List.of(c("IncreasedLife", 2)),
            List.of(c("BaseSpirit", 2), c("IncreasedCastSpeed", 1)),
            1);
    var root = state(82, "IncreasedLife", 2);
    var spirit =
        catalog.modifiers().values().stream()
            .filter(d -> d.familyIds().contains("BaseSpirit") && d.tier() == 2)
            .findFirst()
            .orElseThrow();
    var ids = new ArrayList<>(root.modifierIds());
    ids.add(spirit.id());
    var r = goals.assess(root.with(ItemState.Rarity.RARE, ids), goal);
    assertThat(r.achieved()).isTrue();
    assertThat(r.requiredMatched()).isEqualTo(1);
    assertThat(r.candidatesMatched()).isEqualTo(1);
    assertThat(goals.assess(state(82, "BaseSpirit", 2), goal).achieved()).isFalse();
  }

  @Test
  void duplicatesAcrossRequiredAndCandidateSetsAreInvalidRatherThanCountedTwice() {
    var r =
        goals.assess(
            state(82, null, 0),
            new SupportGoals.Goal(
                List.of(c("IncreasedLife", 2)), List.of(c("IncreasedLife", 3)), 1));
    assertThat(r.status()).isEqualTo("INVALID_GOAL");
    assertThat(r.issues()).anyMatch(s -> s.contains("once"));
  }

  @Test
  void nonexistentTiersAndInvalidNAndEmptyGoalsAreRejected() {
    var state = state(82, null, 0);
    assertThat(
            goals
                .assess(state, new SupportGoals.Goal(List.of(c("IncreasedLife", 90)), List.of(), 0))
                .valid())
        .isFalse();
    assertThat(
            goals
                .assess(state, new SupportGoals.Goal(List.of(), List.of(c("IncreasedLife", 2)), 2))
                .valid())
        .isFalse();
    assertThat(goals.assess(state, new SupportGoals.Goal(List.of(), List.of(), 0)).valid())
        .isFalse();
  }

  @Test
  void itemLevelAndAffixCapacityCanMakeAWellFormedGoalImpossible() {
    var goal = new SupportGoals.Goal(List.of(c("IncreasedLife", 1)), List.of(), 0);
    assertThat(goals.assess(state(50, null, 0), goal).status()).isEqualTo("IMPOSSIBLE");
    var prefixes =
        goals.families().stream()
            .filter(f -> f.affix() == ModifierDefinition.AffixType.PREFIX)
            .limit(4)
            .map(f -> c(f.id(), f.tiers().getLast().tier()))
            .toList();
    assertThat(
            goals
                .assess(state(82, null, 0), new SupportGoals.Goal(prefixes, List.of(), 0))
                .status())
        .isEqualTo("IMPOSSIBLE");
  }

  @Test
  void validNormalRootIsReadyAndMalformedStatesAreNotTreatedAsImpossibleGoals() {
    assertThat(
            goals
                .assess(
                    state(82, null, 0),
                    new SupportGoals.Goal(List.of(c("IncreasedLife", 2)), List.of(), 0))
                .status())
        .isEqualTo("READY");
    var invalid = state(82, null, 0).with(ItemState.Rarity.NORMAL, List.of("unknown"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> goals.assess(invalid, new SupportGoals.Goal(List.of(), List.of(), 0)));
  }
}
