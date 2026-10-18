package com.poe2craft.crafting;

import static com.poe2craft.crafting.domain.AutomaticCurrencyPolicy.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticCurrencyPolicyTest {
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();

  @Test
  void approvedRosterIncludesEveryCurrencyAndOmenWithoutClaimingExecutionSupport() {
    assertThat(policy.document().entries()).hasSize(187);
    var counts = new EnumMap<Decision, Long>(Decision.class);
    for (var entry : policy.document().entries()) counts.merge(entry.decision(), 1L, Long::sum);
    assertThat(counts)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                Decision.USE,
                33L,
                Decision.CONDITIONAL,
                18L,
                Decision.EXCLUDED,
                110L,
                Decision.DEFERRED,
                26L));
    assertThat(policy.candidates()).hasSize(51);
    assertThat(policy.candidates())
        .extracting(Entry::id)
        .contains(
            "ESSENCE_ABYSS",
            "ESSENCE_HORROR",
            "OMEN:SINISTRAL_CRYSTALLISATION",
            "OMEN:DEXTRAL_CRYSTALLISATION");
    for (var currency : WorkbenchCurrency.values())
      assertThat(policy.document().entries()).anyMatch(e -> e.id().equals(currency.name()));
    for (var omen : WorkbenchOmen.values())
      assertThat(policy.document().entries()).anyMatch(e -> e.id().equals("OMEN:" + omen.name()));
  }

  @Test
  void temporaryAdditionsRequirePurposeAndMatchingOmenCurrency() {
    var ordinary = new Context("amulets", Purpose.ORDINARY, WorkbenchCurrency.EXALTED);
    var exalt =
        new Context("amulets", Purpose.TEMPORARY_AFFIX_FOR_REMOVAL, WorkbenchCurrency.EXALTED);
    var regal =
        new Context("amulets", Purpose.TEMPORARY_AFFIX_FOR_REMOVAL, WorkbenchCurrency.REGAL);
    for (var id : List.of("REGAL", "EXALTED")) {
      assertThat(policy.evaluate(id, ordinary)).isEqualTo(Verdict.CONDITION_NOT_MET);
      assertThat(policy.evaluate(id, exalt)).isEqualTo(Verdict.PERMITTED);
    }
    assertThat(policy.evaluate("OMEN:SINISTRAL_CORONATION", exalt))
        .isEqualTo(Verdict.CONDITION_NOT_MET);
    assertThat(policy.evaluate("OMEN:SINISTRAL_CORONATION", regal)).isEqualTo(Verdict.PERMITTED);
    assertThat(policy.evaluate("OMEN:DEXTRAL_EXALTATION", regal))
        .isEqualTo(Verdict.CONDITION_NOT_MET);
    assertThat(policy.evaluate("OMEN:DEXTRAL_EXALTATION", exalt)).isEqualTo(Verdict.PERMITTED);
    assertThat(policy.evaluate("ANNULMENT", exalt)).isEqualTo(Verdict.EXCLUDED);
    assertThat(policy.evaluate("OMEN:HOMOGENISING_EXALTATION", exalt)).isEqualTo(Verdict.EXCLUDED);
  }

  @Test
  void restrictionsUseAllApprovedPartsAndRejectUnreviewedParts() {
    var allowed =
        Map.of(
            "ESSENCE_HYSTERIA", Set.of("gloves", "boots", "quivers"),
            "PRISMATIC_ALLOY", Set.of("gloves"),
            "SWIFT_ALLOY", Set.of("gloves", "rings", "shields", "bucklers", "foci"),
            "SOVEREIGN_ALLOY", Set.of("rings", "amulets", "belts"),
            "EXPANSIVE_ALLOY", Set.of("boots"),
            "CYCLONIC_ALLOY", Set.of("boots", "gloves", "helmet", "helmets"),
            "MYSTIC_ALLOY", Set.of("boots", "gloves", "helmet", "helmets", "quivers"));
    for (var entry : allowed.entrySet())
      for (var family :
          List.of(
              "gloves",
              "boots",
              "quivers",
              "rings",
              "shields",
              "bucklers",
              "foci",
              "amulets",
              "belts",
              "helmet",
              "helmets",
              "body",
              "wands",
              "unknown"))
        assertThat(policy.evaluate(entry.getKey(), new Context(family, Purpose.ORDINARY, null)))
            .as("%s on %s", entry.getKey(), family)
            .isEqualTo(
                entry.getValue().contains(family) ? Verdict.PERMITTED : Verdict.CONDITION_NOT_MET);
  }

  @Test
  void qualityExceptionDoesNotEnableCatalystsAndJewelDeferralIsNotExclusion() {
    var context = new Context("amulets", Purpose.ORDINARY, null);
    assertThat(policy.evaluate("ESSENCE_BREACH", context)).isEqualTo(Verdict.PERMITTED);
    for (var entry : policy.document().entries()) {
      if (entry.number() >= 109 && entry.number() <= 134)
        assertThat(policy.evaluate(entry.id(), context)).isEqualTo(Verdict.EXCLUDED);
      if (entry.number() >= 135 && entry.number() <= 160)
        assertThat(policy.evaluate(entry.id(), context)).isEqualTo(Verdict.DEFERRED);
    }
    assertThat(policy.evaluate("NEW_CURRENCY", context)).isEqualTo(Verdict.UNREVIEWED);
    assertThat(policy.withinExplorationBoundary(4)).isTrue();
    assertThat(policy.withinExplorationBoundary(5)).isFalse();
    assertThatThrownBy(() -> policy.withinExplorationBoundary(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
