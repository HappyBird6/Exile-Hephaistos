package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class OmenCompositionTest {
  final Set<String> pair =
      Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.WHITTLING.id());
  final ItemCatalog catalog = catalog();
  final WorkbenchSimulator sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  ItemCatalog catalog() {
    var source = ItemCatalogLoader.loadDefault();
    var defs = new ArrayList<ModifierDefinition>();
    defs.add(source.find(source.base().implicitModifierId()).orElseThrow());
    for (var id : List.of("p20", "p20tie", "p40", "s1", "p50", "s50")) {
      var level = id.equals("p20tie") ? 20 : Integer.parseInt(id.substring(1));
      defs.add(
          new ModifierDefinition(
              id,
              id,
              ModifierDefinition.Layer.EXPLICIT,
              id.startsWith("p")
                  ? ModifierDefinition.AffixType.PREFIX
                  : ModifierDefinition.AffixType.SUFFIX,
              Set.of(id),
              level,
              10,
              1,
              id,
              List.of(new ModifierDefinition.StatRange(id, 1, 1)),
              Set.of("life"),
              "https://example.test/" + id));
    }
    var m = source.metadata();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            m.snapshotId(),
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            4,
            2,
            40,
            20),
        source.base(),
        defs);
  }

  ItemState state(String... ids) {
    var root = SolarAmulet.initial(catalog);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        82,
        ItemState.Rarity.RARE,
        root.implicits(),
        Arrays.stream(ids).map(id -> new ModifierInstance(id, Map.of(id, 1L))).toList(),
        Set.of());
  }

  Random noDraw() {
    return new Random() {
      @Override
      public int nextInt(int bound) {
        throw new AssertionError("Refusal drew randomness");
      }

      @Override
      public long nextLong(long bound) {
        throw new AssertionError("Refusal drew randomness");
      }
    };
  }

  @Test
  void prefixFilterPrecedesLevelAndTiesAreUniformForEveryChaosGrade() {
    var before = state("p20", "p20tie", "p40", "s1");
    for (var currency :
        List.of(
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.GREATER_CHAOS,
            WorkbenchCurrency.PERFECT_CHAOS)) {
      for (int index = 0; index < 2; index++) {
        final int chosen = index;
        var active = new HashSet<>(pair);
        active.add(WorkbenchOmen.BLESSED.id());
        var result =
            sim.apply(
                before,
                currency,
                active,
                new Random() {
                  @Override
                  public int nextInt(int bound) {
                    assertThat(bound).isEqualTo(2);
                    return chosen;
                  }

                  @Override
                  public long nextLong(long bound) {
                    return 0;
                  }
                });
        assertThat(result.applied()).isTrue();
        assertThat(result.events().getFirst().modifierId())
            .isEqualTo(index == 0 ? "p20" : "p20tie");
        assertThat(result.events().getFirst().selectionProbability()).isEqualTo(0.5);
        assertThat(result.assumptions().getFirst().candidates()).containsExactly("p20", "p20tie");
        assertThat(result.state().explicits())
            .contains(before.explicits().get(2), before.explicits().get(3));
        assertThat(result.consumedOmens()).containsExactlyInAnyOrderElementsOf(pair);
        assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      }
    }
  }

  @Test
  void absentPrefixUnknownPairsAndFractureRefuseBeforeAnyDraw() {
    var before = state("s1");
    assertAtomic(before, pair);
    assertAtomic(
        state("p20", "s1"),
        Set.of(
            WorkbenchOmen.DEXTRAL_ERASURE.id(),
            WorkbenchOmen.SINISTRAL_ERASURE.id(),
            WorkbenchOmen.WHITTLING.id()));
    var rare = state("p20", "s1");
    var locked = new ArrayList<>(rare.explicits());
    locked.set(1, new ModifierInstance("s1", Map.of("s1", 1L), true));
    assertAtomic(
        new ItemState(
            rare.snapshotId(),
            rare.baseItemId(),
            82,
            rare.rarity(),
            rare.implicits(),
            locked,
            Set.of()),
        pair);
  }

  void assertAtomic(ItemState before, Set<String> active) {
    var result = sim.apply(before, WorkbenchCurrency.PERFECT_CHAOS, active, noDraw());
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.events()).isEmpty();
    assertThat(result.assumptions()).isEmpty();
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyInAnyOrderElementsOf(active);
  }

  @Test
  void auditedPrefixDoubleRemovalPreservesSuffixAndRefusesOneEligiblePrefix() {
    var active =
        Set.of(
            WorkbenchOmen.SINISTRAL_ANNULMENT.id(),
            WorkbenchOmen.GREATER_ANNULMENT.id(),
            WorkbenchOmen.BLESSED.id());
    var before = state("p20", "p20tie", "s1");
    var result = sim.apply(before, WorkbenchCurrency.ANNULMENT, active, new Random(3));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).containsExactly(before.explicits().get(2));
    assertThat(result.events())
        .extracting(WorkbenchSimulator.Event::selectionProbability)
        .containsExactly(0.5, 1.0);
    assertThat(result.consumedOmens())
        .containsExactlyInAnyOrder(
            WorkbenchOmen.SINISTRAL_ANNULMENT.id(), WorkbenchOmen.GREATER_ANNULMENT.id());
    assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    var one = state("p20", "s1");
    var refused = sim.apply(one, WorkbenchCurrency.ANNULMENT, active, noDraw());
    assertThat(refused.applied()).isFalse();
    assertThat(refused.state()).isEqualTo(one);
    assertThat(refused.consumedOmens()).isEmpty();
  }

  @Test
  void suffixWhittlingUsesTheSymmetricSideRule() {
    var before = state("p20", "s1", "s50");
    var active = Set.of(WorkbenchOmen.DEXTRAL_ERASURE.id(), WorkbenchOmen.WHITTLING.id());
    for (var action :
        List.of(
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.GREATER_CHAOS,
            WorkbenchCurrency.PERFECT_CHAOS)) {
      var result = sim.apply(before, action, active, new Random(2));
      assertThat(result.applied()).isTrue();
      assertThat(result.events().getFirst().modifierId()).isEqualTo("s1");
      assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1.0);
      assertThat(result.consumedOmens()).containsExactlyInAnyOrderElementsOf(active);
    }
    var doubleIds =
        Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT.id(), WorkbenchOmen.GREATER_ANNULMENT.id());
    var result = sim.apply(before, WorkbenchCurrency.ANNULMENT, doubleIds, new Random(4));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).containsExactly(before.explicits().getFirst());
    assertThat(result.consumedOmens()).containsExactlyInAnyOrderElementsOf(doubleIds);
  }

  @Test
  void tieredSideAndSingleHomogenisingUseFilteredPublishedWeightPools() {
    for (var omen :
        List.of(
            WorkbenchOmen.SINISTRAL_EXALTATION,
            WorkbenchOmen.DEXTRAL_EXALTATION,
            WorkbenchOmen.HOMOGENISING_EXALTATION,
            WorkbenchOmen.SINISTRAL_CORONATION,
            WorkbenchOmen.DEXTRAL_CORONATION,
            WorkbenchOmen.HOMOGENISING_CORONATION)) {
      for (var currency : WorkbenchCurrency.values()) {
        if (currency.baseAction() != omen.trigger().baseAction()
            || currency.minimumModifierLevel() == 0) continue;
        var rare = state("s1");
        var before =
            new ItemState(
                rare.snapshotId(),
                rare.baseItemId(),
                82,
                omen.trigger() == WorkbenchCurrency.REGAL
                    ? ItemState.Rarity.MAGIC
                    : ItemState.Rarity.RARE,
                rare.implicits(),
                rare.explicits(),
                Set.of());
        var result = sim.apply(before, currency, Set.of(omen.id()), new Random(7));
        assertThat(result.applied()).isTrue();
        var upgraded =
            new ItemState(
                before.snapshotId(),
                before.baseItemId(),
                82,
                ItemState.Rarity.RARE,
                before.implicits(),
                before.explicits(),
                Set.of());
        var pool = sim.pool(upgraded, currency, omen);
        var event = result.events().getFirst();
        assertThat(pool).extracting(ModifierDefinition::id).contains(event.modifierId());
        assertThat(event.selectionProbability())
            .isEqualTo(
                (double) catalog.find(event.modifierId()).orElseThrow().weight()
                    / pool.stream().mapToLong(ModifierDefinition::weight).sum());
        assertThat(result.consumedOmens()).containsExactly(omen.id());
      }
    }
  }
}
