package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AugmentClipboardRulesTest {
  private final String wand = "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand2";

  @Test
  void reconstructsRuneEffectProviderBeforeInvertingItsBoostedCompanion() {
    var result =
        SocketAugmentText.resolve(
            wand,
            2,
            List.of(
                "75% increased effect of Socketed Runes (rune)",
                "Gain 21% of Damage as Extra Fire Damage (rune)"));
    assertThat(result.unidentified()).isEmpty();
    assertThat(result.installed())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactlyInAnyOrder("Legacy_of_Runeseekers_Call", "Perfect_Desert_Rune");
    var scaled =
        SocketAugmentText.resolve(
            wand,
            2,
            List.of(
                "120% increased effect of Socketed Runes (rune)",
                "Gain 34% of Damage as Extra Fire Damage (rune)"),
            1.6);
    assertThat(scaled.unidentified()).isEmpty();
    assertThat(scaled.installed())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactlyInAnyOrder("Legacy_of_Runeseekers_Call", "Perfect_Desert_Rune");
  }

  @Test
  void foxRecoversItsIdentityAndConsumesItsAutomaticallyActiveBondedLineOnce() {
    var base = "Metadata/Items/Armours/BodyArmours/FourBodyStr3Endgame";
    var result =
        SocketAugmentText.resolve(
            base,
            2,
            List.of(
                "Idols socketed in this item gain the benefits of their Bonded modifiers (rune)",
                "+5% to Quality of all Skills (rune)"));
    assertThat(result.unidentified()).isEmpty();
    assertThat(result.installed())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactly("Fox_Idol");
  }

  @Test
  void capacityBonusesDoNotAmplifyButSeparateBondedNumbersDo() {
    var base = "Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace8Endgame";
    var installed = List.of(new ItemState.InstalledAugment(0, "Legacy_of_Serles_Grit"));
    assertThat(SocketAugmentEffects.render(base, "Legacy_of_Serles_Grit", installed, 1.6, true))
        .contains(
            "Maximum Quality is 40%",
            "Skills which Empower an Attack have 32% chance to not count that Attack");
  }

  @Test
  void rageDelayConvertsSourceMillisecondsAndAcceptsGameSingularAndPluralSeconds()
      throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var input =
        com.poe2craft.shared.ruleset.SealedResources.open("/catalog/augment-crafting.json")) {
      var definition =
          json.readValue(input, AugmentCraftingRules.Document.class).modifiers().stream()
              .filter(d -> d.id().endsWith(":BerserkInfluenceRageLossDelay1"))
              .findFirst()
              .orElseThrow();
      for (long value : List.of(1000L, 1500L, 2000L)) {
        var seconds = value == 1000 ? "1 second" : (value == 1500 ? "1.5 seconds" : "2 seconds");
        var matched =
            ModifierClipboardText.match(
                definition,
                "Inherent Rage loss starts " + seconds + " later",
                java.math.BigDecimal.ONE,
                false,
                false);
        assertThat(matched).isNotNull();
        assertThat(matched.values()).containsEntry("rage_loss_delay_ms_+", value);
      }
    }
  }
}
