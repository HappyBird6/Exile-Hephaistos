package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JewelFiveAffixTest {
  ModifierInstance minimum(ItemCatalog c, String id) {
    var values = new HashMap<String, Long>();
    c.find(id).orElseThrow().stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(id, values);
  }

  List<ModifierInstance> ordinary(ItemCatalog c, int p, int s) {
    return new AncientLiquidTest().ordinary(c, p, s, Set.of());
  }

  ItemState item(ItemCatalog c, List<ModifierInstance> mods) {
    return new ItemState(
        c.metadata().snapshotId(),
        c.base().id(),
        82,
        ItemState.Rarity.RARE,
        BasicJewel.timeLost(c.base().id())
            ? List.of(minimum(c, c.base().implicitModifierId()))
            : List.of(),
        mods,
        Set.of());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ruby",
        "emerald",
        "sapphire",
        "diamond",
        "time-lost-ruby",
        "time-lost-emerald",
        "time-lost-sapphire",
        "time-lost-diamond"
      })
  void fillsOnlyTheOpenSideWithoutPersistingAnExpansion(String base) {
    var c =
        base.equals("sapphire")
            ? ItemCatalogLoader.loadSapphire()
            : ItemCatalogLoader.loadBasicJewel(base);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var overflowSide : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      var start =
          item(
              c,
              ordinary(
                  c,
                  overflowSide == AffixType.PREFIX ? 3 : 1,
                  overflowSide == AffixType.SUFFIX ? 3 : 1));
      assertThat(new ItemStateValidator(c).validate(start)).isEmpty();
      var accepted = sim.apply(start, WorkbenchCurrency.EXALTED, Set.of(), new Random(11));
      assertThat(accepted.applied()).as(base + " " + overflowSide).isTrue();
      assertThat(accepted.state().explicits()).hasSize(5).containsAll(start.explicits());
      var slots = new ItemStateValidator(c).slots(accepted.state());
      assertThat(slots.maxPrefixes()).isEqualTo(2);
      assertThat(slots.maxSuffixes()).isEqualTo(2);
      assertThat(accepted.ruleVersion()).contains("five-affix");
      var blocked = sim.apply(accepted.state(), WorkbenchCurrency.EXALTED, Set.of(), new Random());
      assertThat(blocked.applied()).isFalse();
      assertThat(blocked.state()).isEqualTo(accepted.state());
      assertThat(blocked.consumedOmens()).isEmpty();
      assertThat(new ItemStateValidator(c).validate(item(c, ordinary(c, 3, 3)))).isNotEmpty();
      var bucket =
          new StateBucket(
              accepted.state().snapshotId(),
              c.base().id(),
              82,
              ItemState.Rarity.RARE,
              accepted.state().implicits(),
              accepted.state().explicits().stream().map(ModifierInstance::modifierId).toList(),
              Set.of());
      assertThatCode(() -> new CraftingEngine(c).validate(bucket)).doesNotThrowAnyException();
      assertThat(new ModifierPoolResolver(c).resolve(bucket).candidates()).isEmpty();
    }
  }

  @Test
  void craniumCapLossHiddenExaltRevealAndKnownCraftsPreserveTheThirdSuffix() {
    var c = ManualRevealCatalog.enrich(ItemCatalogLoader.loadSapphire());
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var executor = new AbyssCurrencyExecutor(c, sim);
    var mods = new ArrayList<ModifierInstance>();
    String expansion = "sapphire:crafted:CraftedJewelAdditionalSuffixAllowed";
    mods.add(minimum(c, expansion));
    mods.addAll(ordinary(c, 1, 2));
    var expanded = item(c, mods);
    var hidden =
        DesecrationCurrency.apply(
            expanded, "Preserved_Cranium", null, null, Set.of(), c, new Random(1));
    var hiddenSlot =
        hidden.slots().stream().filter(s -> s.modifier() == null).findFirst().orElseThrow();
    assertThat(hiddenSlot.side()).isEqualTo(AffixType.SUFFIX);
    var fixedFirst =
        new Random() {
          @Override
          public int nextInt(int bound) {
            return 0;
          }

          @Override
          public long nextLong(long bound) {
            return 0;
          }
        };
    var annulled =
        executor.apply(
            hidden, WorkbenchCurrency.ANNULMENT, Set.of("Omen_of_Sinistral_Annulment"), fixedFirst);
    assertThat(annulled.applied()).isTrue();
    assertThat(annulled.state().knownItem().explicits())
        .noneMatch(m -> m.modifierId().equals(expansion));
    assertThat(annulled.state().count(AffixType.SUFFIX)).isEqualTo(3);
    var filled =
        executor.apply(annulled.state(), WorkbenchCurrency.EXALTED, Set.of(), new Random(2));
    assertThat(filled.applied()).isTrue();
    assertThat(filled.state().slots()).hasSize(5).contains(hiddenSlot);
    assertThat(filled.state().count(AffixType.PREFIX)).isEqualTo(2);
    var eligible = filled.state().eligibleRevealIds(hiddenSlot.id(), c);
    assertThat(eligible).isNotEmpty();
    var revealed = filled.state().selectDirect(hiddenSlot.id(), minimum(c, eligible.getFirst()), c);
    assertThat(revealed.slots()).hasSize(5);
    assertThat(
            revealed.slots().stream()
                .filter(s -> s.id().equals(hiddenSlot.id()))
                .findFirst()
                .orElseThrow()
                .desecrated())
        .isTrue();
    var suffixes = revealed.slots().stream().filter(s -> s.side() == AffixType.SUFFIX).toList();
    var bareChaos = executor.apply(revealed, WorkbenchCurrency.CHAOS, Set.of(), new Random());
    assertThat(bareChaos.applied()).isTrue();
    assertThat(bareChaos.state().slots()).hasSize(5);
    assertThat(bareChaos.state().slots().stream().filter(s -> s.side() == AffixType.SUFFIX))
        .containsExactlyInAnyOrderElementsOf(suffixes);
    assertThat(bareChaos.consumedOmens()).isEmpty();
    var prefixChaos =
        executor.apply(
            revealed, WorkbenchCurrency.CHAOS, Set.of("Omen_of_Sinistral_Erasure"), new Random(4));
    assertThat(prefixChaos.applied()).isTrue();
    assertThat(prefixChaos.state().slots().stream().filter(s -> s.side() == AffixType.SUFFIX))
        .containsExactlyInAnyOrderElementsOf(suffixes);
    var ferocity =
        executor.apply(
            prefixChaos.state(), WorkbenchCurrency.POTENT_LIQUID_FEROCITY, Set.of(), new Random(3));
    assertThat(ferocity.applied()).isTrue();
    assertThat(ferocity.state().slots()).hasSize(5);
    assertThat(ferocity.state().slots().stream().filter(s -> s.side() == AffixType.SUFFIX))
        .containsExactlyInAnyOrderElementsOf(suffixes);
    var divine =
        executor.apply(ferocity.state(), WorkbenchCurrency.DIVINE, Set.of(), new Random(7));
    assertThat(divine.applied()).isTrue();
    assertThat(divine.state().slots().stream().map(AbyssCraftingState.Slot::id))
        .containsExactlyElementsOf(
            ferocity.state().slots().stream().map(AbyssCraftingState.Slot::id).toList());
  }
}
