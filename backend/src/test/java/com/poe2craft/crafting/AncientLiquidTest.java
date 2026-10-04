package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class AncientLiquidTest {
  static final List<String> BASES =
      List.of("time-lost-ruby", "time-lost-emerald", "time-lost-sapphire", "time-lost-diamond");

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
        List.of(minimum(c, c.base().implicitModifierId())),
        mods,
        Set.of());
  }

  List<ModifierInstance> ordinary(ItemCatalog c, int p, int s, Set<String> excluded) {
    var mods = new ArrayList<ModifierInstance>();
    var families = new HashSet<>(excluded);
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
  void exactPoolsAllPositiveLiquidsAndCategoryRejections() {
    var counts =
        Map.of(
            "time-lost-ruby",
            53,
            "time-lost-emerald",
            77,
            "time-lost-sapphire",
            60,
            "time-lost-diamond",
            160);
    for (var base : BASES) {
      var c = ItemCatalogLoader.loadBasicJewel(base);
      var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
      assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0))
          .hasSize(counts.get(base));
      var seen = new HashSet<WorkbenchCurrency>();
      for (var action : WorkbenchCurrency.values())
        if (action.isLiquid()) {
          var targets =
              c.modifiers().values().stream()
                  .filter(
                      d ->
                          d.tags().contains("crafted")
                              && d.sourceUrl().equals(action.replacementSource()))
                  .toList();
          for (var before :
              List.of(item(c, ordinary(c, 2, 2, Set.of())), item(c, ordinary(c, 1, 0, Set.of())))) {
            var r =
                sim.apply(
                    before, action, Set.of("Omen_of_Sinistral_Crystallisation"), new Random(23));
            assertThat(r.applied()).as(base + " " + action).isEqualTo(!targets.isEmpty());
            assertThat(r.consumedOmens()).isEmpty();
            assertThat(new ItemStateValidator(c).validate(r.state())).isEmpty();
            assertThat(r.state().implicits()).isEqualTo(before.implicits());
            if (r.applied()) {
              seen.add(action);
              assertThat(r.state().explicits()).hasSize(before.explicits().size());
              assertThat(r.assumptions())
                  .extracting(WorkbenchSimulator.Assumption::id)
                  .contains("uniform-liquid-outcomes-v1");
              var again = sim.apply(r.state(), action, Set.of(), new Random());
              assertThat(again.applied()).isFalse();
              assertThat(again.state()).isEqualTo(r.state());
            } else {
              assertThat(r.state()).isEqualTo(before);
              assertThat(r.events()).isEmpty();
            }
          }
        }
      assertThat(seen).hasSize(base.endsWith("diamond") ? 3 : 13);
      for (var a :
          List.of(
              WorkbenchCurrency.REFINED_CATALYST_FLESH,
              WorkbenchCurrency.CATALYST_FLESH,
              WorkbenchCurrency.ESSENCE_HYSTERIA,
              WorkbenchCurrency.FRACTURING,
              WorkbenchCurrency.ARTIFICER))
        assertThat(
                sim.apply(item(c, ordinary(c, 2, 2, Set.of())), a, Set.of(), new Random())
                    .applied())
            .isFalse();
    }
  }

  @Test
  void ordinaryCurrenciesAndRareOnlyLiquidFailures() {
    for (var base : BASES) {
      var c = ItemCatalogLoader.loadBasicJewel(base);
      var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
      var full = item(c, ordinary(c, 2, 2, Set.of()));
      for (var a : WorkbenchCurrency.values())
        if (a.baseAction() != null
            || a == WorkbenchCurrency.ALCHEMY
            || a == WorkbenchCurrency.DIVINE) {
          if (a == WorkbenchCurrency.ANNULMENT
              || a == WorkbenchCurrency.DIVINE
              || a.name().endsWith("CHAOS"))
            assertThat(sim.apply(full, a, Set.of(), new Random(7)).applied())
                .as(base + " " + a)
                .isTrue();
          else {
            var rarity =
                a.name().endsWith("TRANSMUTATION") || a == WorkbenchCurrency.ALCHEMY
                    ? ItemState.Rarity.NORMAL
                    : a.name().endsWith("EXALTED") ? ItemState.Rarity.RARE : ItemState.Rarity.MAGIC;
            var mods =
                rarity == ItemState.Rarity.NORMAL
                    ? List.<ModifierInstance>of()
                    : ordinary(c, 0, 1, Set.of());
            var before =
                new ItemState(
                    full.snapshotId(),
                    full.baseItemId(),
                    82,
                    rarity,
                    full.implicits(),
                    mods,
                    Set.of());
            assertThat(sim.apply(before, a, Set.of(), new Random(9)).applied())
                .as(base + " " + a)
                .isTrue();
          }
        }
      for (var rarity :
          List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
        var before =
            new ItemState(
                full.snapshotId(),
                full.baseItemId(),
                82,
                rarity,
                full.implicits(),
                List.of(),
                Set.of());
        var r =
            sim.apply(
                before, WorkbenchCurrency.ANCIENT_POTENT_LIQUID_MELANCHOLY, Set.of(), new Random());
        assertThat(r.applied()).isFalse();
        assertThat(r.state()).isEqualTo(before);
      }
    }
  }

  @Test
  void ancientPotentSemanticsAreNotBasicFerocityAndRadiusFamilyMustBeRemoved() {
    for (var base : BASES) {
      var c = ItemCatalogLoader.loadBasicJewel(base);
      var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
      var full = item(c, ordinary(c, 2, 2, Set.of()));
      var ferocity =
          sim.apply(
              full, WorkbenchCurrency.ANCIENT_POTENT_LIQUID_FEROCITY, Set.of(), new Random(3));
      assertThat(ferocity.applied()).isTrue();
      var added = c.find(ferocity.events().get(1).modifierId()).orElseThrow();
      assertThat(added.text()).contains("Notable Passive Skills in Radius", "Resistance");
      assertThat(BasicJewel.effect(ferocity.state(), ModifierDefinition.AffixType.PREFIX)).isZero();
      assertThat(BasicJewel.effect(ferocity.state(), ModifierDefinition.AffixType.SUFFIX)).isZero();
      var radius =
          c.modifiers().values().stream()
              .filter(d -> d.weight() > 0 && d.familyIds().contains("JewelRadiusLargerRadius"))
              .findFirst()
              .orElseThrow();
      var mods = new ArrayList<>(ordinary(c, 1, 2, radius.familyIds()));
      mods.add(minimum(c, radius.id()));
      var before = item(c, mods);
      var r =
          sim.apply(
              before, WorkbenchCurrency.ANCIENT_POTENT_LIQUID_MELANCHOLY, Set.of(), new Random(6));
      assertThat(r.applied()).isTrue();
      assertThat(r.events().getFirst().modifierId()).isEqualTo(radius.id());
      assertThat(r.events().get(1).values()).containsEntry("local_jewel_effect_base_radius", 500L);
      assertThat(
              r.assumptions().stream()
                  .filter(a -> a.id().equals("uniform-removal-v1"))
                  .map(WorkbenchSimulator.Assumption::n))
          .containsOnly(1L);
      assertThat(r.state().implicits()).isEqualTo(before.implicits());
    }
  }

  @Test
  void bothContemptDirectionsOverflowAndReapplyRemainReachable() {
    for (var base : BASES) {
      var c = ItemCatalogLoader.loadBasicJewel(base);
      var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
      var directions = new HashSet<String>();
      for (int seed = 0; seed < 100 && directions.size() < 2; seed++) {
        var r =
            sim.apply(
                item(c, ordinary(c, 2, 2, Set.of())),
                WorkbenchCurrency.ANCIENT_POTENT_LIQUID_CONTEMPT,
                Set.of(),
                new Random(seed * 104729L));
        assertThat(r.applied()).isTrue();
        directions.add(r.events().get(1).modifierId());
      }
      assertThat(directions).hasSize(2);
      for (var id : directions) {
        var crafted = c.find(id).orElseThrow();
        boolean expandP = id.endsWith("PrefixAllowed");
        var mods =
            new ArrayList<>(ordinary(c, expandP ? 3 : 1, expandP ? 1 : 3, crafted.familyIds()));
        mods.add(minimum(c, id));
        var expanded = item(c, mods);
        assertThat(new ItemStateValidator(c).validate(expanded)).isEmpty();
        var overflow = item(c, mods.stream().filter(m -> !m.modifierId().equals(id)).toList());
        assertThat(new ItemStateValidator(c).validate(overflow)).isEmpty();
        assertThat(sim.apply(overflow, WorkbenchCurrency.EXALTED, Set.of(), new Random()).applied())
            .isFalse();
        assertThat(sim.apply(overflow, WorkbenchCurrency.DIVINE, Set.of(), new Random()).applied())
            .isTrue();
        assertThat(
                sim.apply(
                        overflow,
                        WorkbenchCurrency.ANCIENT_POTENT_LIQUID_CONTEMPT,
                        Set.of(),
                        new Random(5))
                    .applied())
            .isTrue();
      }
    }
  }
}
