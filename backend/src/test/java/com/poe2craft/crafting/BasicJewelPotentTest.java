package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class BasicJewelPotentTest {
  ItemCatalog catalog(String base) {
    return base.equals("sapphire")
        ? ItemCatalogLoader.loadSapphire()
        : ItemCatalogLoader.loadBasicJewel(base);
  }

  ModifierInstance minimum(ItemCatalog c, String id) {
    var values = new HashMap<String, Long>();
    c.find(id).orElseThrow().stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(id, values);
  }

  ItemState item(ItemCatalog c, List<ModifierInstance> mods) {
    return new ItemState(
        c.metadata().snapshotId(),
        c.base().id(),
        82,
        ItemState.Rarity.RARE,
        List.of(),
        mods,
        Set.of(),
        null,
        new CatalystQuality(CatalystQuality.Type.CARAPACE, 20));
  }

  List<ModifierInstance> ordinary(ItemCatalog c, int p, int s) {
    var mods = new ArrayList<ModifierInstance>();
    var families = new HashSet<String>();
    for (var d : c.modifiers().values())
      if (d.weight() > 0
          && Collections.disjoint(families, d.familyIds())
          && (d.affixType() == ModifierDefinition.AffixType.PREFIX ? p > 0 : s > 0)) {
        mods.add(minimum(c, d.id()));
        families.addAll(d.familyIds());
        if (d.affixType() == ModifierDefinition.AffixType.PREFIX) p--;
        else s--;
      }
    assertThat(p + s).isZero();
    return mods;
  }

  @Test
  void exactBasePoolsAndApplicabilityAndAllLiquidPositivePaths() {
    var counts = Map.of("ruby", 50, "emerald", 74, "diamond", 160, "sapphire", 58);
    for (var base : counts.keySet()) {
      var c = catalog(base);
      var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
      assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0))
          .hasSize(counts.get(base));
      var full = item(c, ordinary(c, 2, 2));
      for (var a : WorkbenchCurrency.values())
        if (a.isLiquid()) {
          boolean sourced =
              c.modifiers().values().stream()
                  .anyMatch(
                      d ->
                          d.tags().contains("crafted")
                              && d.sourceUrl().equals(a.replacementSource()));
          for (int seed = 0; seed < 20; seed++) {
            var r =
                sim.apply(full, a, Set.of("Omen_of_Sinistral_Crystallisation"), new Random(seed));
            assertThat(r.applied()).as(base + " " + a).isEqualTo(sourced);
            assertThat(new ItemStateValidator(c).validate(r.state())).isEmpty();
            assertThat(r.consumedOmens()).isEmpty();
            if (sourced) {
              assertThat(r.state().explicits()).hasSize(4);
              assertThat(r.assumptions())
                  .extracting(WorkbenchSimulator.Assumption::id)
                  .contains("uniform-liquid-outcomes-v1");
              var again = sim.apply(r.state(), a, Set.of(), new Random(seed));
              assertThat(again.applied()).isFalse();
              assertThat(again.state()).isEqualTo(r.state());
            } else assertThat(r.state()).isEqualTo(full);
          }
        }
      assertThat(
              sim.apply(full, WorkbenchCurrency.ESSENCE_HYSTERIA, Set.of(), new Random()).applied())
          .isFalse();
    }
  }

  @Test
  void contemplatesBothDirectionsAndPreservesOverflowAfterRemovalWhileRestrictingInsertion() {
    for (var base : List.of("ruby", "emerald", "diamond", "sapphire"))
      for (var side : ModifierDefinition.AffixType.values()) {
        if (side == ModifierDefinition.AffixType.NONE) continue;
        var c = catalog(base);
        var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
        String code =
            side == ModifierDefinition.AffixType.PREFIX
                ? "CraftedJewelAdditionalPrefixAllowed"
                : "CraftedJewelAdditionalSuffixAllowed";
        var mods =
            ordinary(
                c,
                side == ModifierDefinition.AffixType.PREFIX ? 3 : 1,
                side == ModifierDefinition.AffixType.SUFFIX ? 3 : 1);
        mods.add(minimum(c, base + ":crafted:" + code));
        var expanded = item(c, mods);
        assertThat(new ItemStateValidator(c).validate(expanded)).isEmpty();
        var slots = new ItemStateValidator(c).slots(expanded);
        assertThat(
                side == ModifierDefinition.AffixType.PREFIX
                    ? slots.maxPrefixes()
                    : slots.maxSuffixes())
            .isEqualTo(3);
        var overflow =
            item(c, mods.stream().filter(m -> !m.modifierId().contains(":crafted:")).toList());
        assertThat(new ItemStateValidator(c).validate(overflow)).isEmpty();
        assertThat(sim.pool(overflow, WorkbenchCurrency.EXALTED, null))
            .noneMatch(d -> d.affixType() == side);
        var refused = sim.apply(overflow, WorkbenchCurrency.EXALTED, Set.of(), new Random(1));
        assertThat(refused.applied()).isFalse();
        assertThat(refused.state()).isEqualTo(overflow);
      }
  }

  @Test
  void ferocityProjectsFromOriginalOnceWithQualityAndLeavesMetaAndConditionalEffectsUnscaled() {
    for (var base : List.of("ruby", "emerald", "sapphire", "diamond"))
      for (var side :
          List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX)) {
        var c = catalog(base);
        var d =
            c.modifiers().values().stream()
                .filter(
                    r -> r.weight() > 0 && r.affixType() == side && r.tags().contains("defences"))
                .findFirst()
                .orElse(null);
        if (d == null) continue;
        var source = minimum(c, d.id());
        String code =
            side == ModifierDefinition.AffixType.PREFIX
                ? "CraftedJewelPrefixEffect"
                : "CraftedJewelSuffixEffect";
        var crafted =
            new ModifierInstance(base + ":crafted:" + code, Map.of("display_source_value", 50L));
        var state = item(c, List.of(source, crafted));
        var display = CatalystQualityDisplay.describe(state, c);
        var projected =
            display.stream().filter(p -> p.modifierId().equals(d.id())).findFirst().orElseThrow();
        assertThat(projected.originalValues()).isEqualTo(source.values());
        assertThat(projected.displayedValues().get(d.stats().getFirst().id()))
            .isEqualTo(
                QualityRoundingPolicy.roundRatio(
                    java.math.BigInteger.valueOf(
                        source.values().get(d.stats().getFirst().id()) * 120 * 150),
                    java.math.BigInteger.valueOf(10000)));
        assertThat(
                display.stream()
                    .filter(p -> p.modifierId().equals(crafted.modifierId()))
                    .findFirst()
                    .orElseThrow()
                    .displayedValues())
            .isEqualTo(crafted.values());
      }
    var c = catalog("sapphire");
    var condition =
        minimum(c, "sapphire:crafted:CraftedJewelExposureOnHitWhileRubyEmeraldSocketed");
    assertThat(CatalystQualityDisplay.describe(item(c, List.of(condition)), c).getFirst().status())
        .isEqualTo("UNSCALABLE");
  }
}
