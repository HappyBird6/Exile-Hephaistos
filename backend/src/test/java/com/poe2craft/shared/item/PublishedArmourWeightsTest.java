package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;
import org.junit.jupiter.api.Test;

class PublishedArmourWeightsTest {
  private static final Set<String> RESTORED_POOLS =
      Set.of(
          "adherent-cuffs",
          "ancestral-tiara",
          "austere-garb",
          "blacksteel-gauntlets",
          "blacksteel-sabatons",
          "conjurer-mantle",
          "corsair-coat",
          "cryptic-crown",
          "daggerfoot-shoes",
          "dastard-armour",
          "death-mail",
          "death-mantle",
          "drakeskin-boots",
          "faithful-leggings",
          "falconers-jacket",
          "feathered-raiment",
          "flowing-raiment",
          "freebooter-cap",
          "gladiatorial-helm",
          "grinning-mask",
          "mail-coat",
          "massive-mitts",
          "polished-bracers",
          "rambler-jacket",
          "sacramental-robe",
          "seastorm-mantle",
          "sekhema-sandals",
          "shrouded-mail-cold",
          "shrouded-mail-fire",
          "shrouded-mail-lightning",
          "sirenscale-gloves",
          "sleek-jacket",
          "slipstrike-vest",
          "tasalian-greaves",
          "thane-mail",
          "vile-robe",
          "war-wraps",
          "wolfskin-mantle",
          "wyrmscale-coat");

  @Test
  void allThirtyNineRestoredPoolsMatchPreservedSourceWeights() throws Exception {
    int checked = 0;
    var mapper = new ObjectMapper();
    for (var base : BaseRegistry.topBases().values()) {
      if (!RESTORED_POOLS.contains(base.pool())) continue;
      var catalog = ItemCatalogLoader.loadTopBase(base.key());
      assertThat(catalog.metadata().weightPolicy()).isEqualTo("POE2DB_AS_PUBLISHED");
      try (var input = SealedResources.open("/catalog/" + base.pool() + "/base.raw.json")) {
        var source = mapper.readTree(input);
        for (var d : catalog.modifiers().values()) {
          if (d.weight() <= 0 || d.layer() != ModifierDefinition.Layer.EXPLICIT) continue;
          var weights = new HashSet<Long>();
          for (var row : source) {
            if (row.path("Name").asText().equals(d.name())
                && row.path("Level").asInt() == d.requiredItemLevel()
                && row.path("ModGenerationTypeID").asInt()
                    == (d.affixType() == ModifierDefinition.AffixType.PREFIX ? 1 : 2))
              weights.add(row.path("DropChance").asLong());
          }
          assertThat(weights).as(base.key() + ":" + d.id()).containsExactly((long) d.weight());
        }
      }
      assertThat(
              catalog.modifiers().values().stream()
                  .filter(d -> d.weight() > 0)
                  .map(ModifierDefinition::weight)
                  .distinct()
                  .count())
          .isGreaterThan(1);
      checked++;
    }
    assertThat(checked).isEqualTo(39);
  }
}
