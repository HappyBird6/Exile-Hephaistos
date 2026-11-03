package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

class AbyssContinuousCurrencyTest {
  final HysteriaEssenceTest f = new HysteriaEssenceTest();
  final ItemCatalog catalog = ManualRevealCatalog.enrich(f.catalog);
  final AbyssCurrencyExecutor executor = new AbyssCurrencyExecutor(catalog, f.simulator);

  AbyssCraftingState hidden() {
    return DesecrationCurrency.apply(
        f.rare(82, "amulet:prefix:hale", "amulet:prefix:hale"),
        "Ancient_Collarbone",
        AffixType.SUFFIX,
        null,
        Set.of(),
        catalog);
  }

  @Test
  void automaticSideUsesBothAvailableSidesButNeverRevealsAModifier() {
    var seen = new HashSet<AffixType>();
    var item = f.rare(82, "");
    for (int seed = 0; seed < 40; seed++) {
      var next =
          DesecrationCurrency.apply(
              item,
              "Ancient_Collarbone",
              null,
              null,
              Set.of(),
              catalog,
              new Random(seed * 104729L));
      var slot = next.slots().getLast();
      seen.add(slot.side());
      assertThat(slot.modifier()).isNull();
      assertThat(slot.minimumModifierLevel()).isEqualTo(40);
      assertThat(next.offer()).isNull();
    }
    assertThat(seen).containsExactlyInAnyOrder(AffixType.PREFIX, AffixType.SUFFIX);
    assertThat(item.desecrated()).isEmpty();
  }

  @Test
  void onlyOpenSideIsChosenAndNecromancyCannotExceedItsCapacity() {
    var item = f.rare(82, "", "amulet:prefix:hale", "amulet:prefix:azure", "amulet:prefix:adept-s");
    for (int seed = 0; seed < 8; seed++) {
      var next =
          DesecrationCurrency.apply(
              item, "Preserved_Collarbone", null, null, Set.of(), catalog, new Random(seed));
      assertThat(next.slots().getLast().side()).isEqualTo(AffixType.SUFFIX);
    }
    assertThatThrownBy(
            () ->
                DesecrationCurrency.apply(
                    item,
                    "Preserved_Collarbone",
                    null,
                    null,
                    Set.of("Omen_of_Sinistral_Necromancy"),
                    catalog,
                    new Random(1)))
        .isInstanceOf(IllegalArgumentException.class);
    var forced =
        DesecrationCurrency.apply(
            f.rare(82, ""),
            "Preserved_Collarbone",
            null,
            null,
            Set.of("Omen_of_Dextral_Necromancy"),
            catalog,
            new Random(1));
    assertThat(forced.slots().getLast().side()).isEqualTo(AffixType.SUFFIX);
  }

  @Test
  void hiddenSlotSurvivesAdditionAndCatalystWhileRemovalProtectsFractures() {
    var state = hidden();
    var slot = state.slots().getLast();
    var add = executor.apply(state, WorkbenchCurrency.EXALTED, Set.of(), new Random(1));
    assertThat(add.applied()).isTrue();
    assertThat(add.state().slots()).contains(slot);
    var catalyst = executor.apply(state, WorkbenchCurrency.CATALYST_FLESH, Set.of(), new Random(1));
    assertThat(catalyst.applied()).isTrue();
    assertThat(catalyst.state().slots()).containsExactlyInAnyOrderElementsOf(state.slots());
    assertThat(catalyst.state().item().catalystQuality()).isNotNull();
    assertThat(catalyst.state().storedItem().desecrated())
        .isEqualTo(state.storedItem().desecrated());
    var afterQuality =
        executor.apply(catalyst.state(), WorkbenchCurrency.EXALTED, Set.of(), new Random(1));
    assertThat(afterQuality.applied()).isTrue();
    assertThat(afterQuality.state().item().catalystQuality())
        .isEqualTo(catalyst.state().item().catalystQuality());
    assertThat(afterQuality.state().slots()).contains(slot);
    var remove =
        executor.apply(
            state,
            WorkbenchCurrency.ANNULMENT,
            Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT.id()),
            new Random(1));
    assertThat(remove.applied()).isTrue();
    assertThat(remove.state().slots()).doesNotContain(slot);
    assertThat(remove.state().knownItem().explicits()).containsAll(state.knownItem().explicits());
    assertThat(remove.consumedOmens()).containsExactly(WorkbenchOmen.DEXTRAL_ANNULMENT.id());
    assertThat(state.slots()).contains(slot);
  }

  @Test
  void rejectedCurrencyPreservesCompleteOfferAndOmens() {
    var state = hidden();
    var slot = state.slots().getLast();
    var definition =
        catalog.find(state.eligibleRevealIds(slot.id(), catalog).getFirst()).orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    var option = new ModifierInstance(definition.id(), values);
    var offered = state.observe(slot.id(), List.of(option, option, option), catalog);
    var omens = Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT.id());
    var result = executor.apply(offered, WorkbenchCurrency.ANNULMENT, omens, new Random(1));
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isSameAs(offered);
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyElementsOf(omens);
    assertThat(offered.offer()).isNotNull();
  }

  @Test
  void chaosCountsTheHiddenSlotAsARemovalAndKeepsTheFracturedModifier() {
    var state = hidden();
    var result =
        executor.apply(
            state,
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.DEXTRAL_ERASURE.id()),
            new Random(19));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().slots()).hasSize(state.slots().size());
    assertThat(result.state().slots()).noneMatch(s -> s.modifier() == null);
    assertThat(result.state().knownItem().explicits()).containsAll(state.knownItem().explicits());
    assertThat(result.consumedOmens()).containsExactly(WorkbenchOmen.DEXTRAL_ERASURE.id());
    assertThat(state.slots().getLast().modifier()).isNull();
  }

  @Test
  void sanctifiedSlotsRejectEveryAbyssMutationBeforeAnySelection() {
    var original = hidden();
    var i = original.item();
    var item =
        new ItemState(
            i.snapshotId(),
            i.baseItemId(),
            i.itemLevel(),
            i.rarity(),
            i.implicits(),
            i.explicits(),
            Set.of(ItemState.Condition.SANCTIFIED));
    var state = new AbyssCraftingState(item, original.slots(), original.offer());
    String id = state.slots().getLast().id();
    assertThatThrownBy(() -> state.addUnrevealed("new", AffixType.PREFIX, catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanctified");
    assertThatThrownBy(() -> state.remove(id, null, catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanctified");
    assertThatThrownBy(() -> state.selectDirect(id, null, catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanctified");
    assertThatThrownBy(() -> state.observe(id, List.of(), catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanctified");
    assertThatThrownBy(() -> state.choose(0, catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Sanctified");
    assertThat(state.slots()).isEqualTo(original.slots());
  }

  @Test
  void removingARevealedModifierAndRollingTheSameIdDoesNotRetainDesecration() {
    var original = f.rare(82, "amulet:prefix:hale", "amulet:prefix:hale");
    var hidden =
        DesecrationCurrency.apply(
            original, "Preserved_Collarbone", AffixType.SUFFIX, null, Set.of(), catalog);
    var d = catalog.find("amulet:suffix:of-the-brute").orElseThrow();
    var stat = d.stats().getFirst();
    var revealed =
        hidden.selectDirect(
            hidden.slots().getLast().id(),
            new ModifierInstance(d.id(), Map.of(stat.id(), stat.min())),
            catalog);
    var pool = f.simulator.pool(original, WorkbenchCurrency.CHAOS, null);
    long start = 0;
    for (var candidate : pool) {
      if (candidate.id().equals(d.id())) break;
      start += candidate.weight();
    }
    final long draw = start;
    final long total = pool.stream().mapToLong(ModifierDefinition::weight).sum();
    var random =
        new Random(1) {
          @Override
          public int nextInt(int bound) {
            return 0;
          }

          @Override
          public long nextLong(long bound) {
            return bound == total ? draw : 0;
          }
        };
    var result = executor.apply(revealed, WorkbenchCurrency.CHAOS, Set.of(), random);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().knownItem().explicits()).anyMatch(m -> m.modifierId().equals(d.id()));
    assertThat(result.state().storedItem().desecrated()).isEmpty();
    assertThat(revealed.storedItem().desecrated()).hasSize(1);
  }

  @Test
  void integratedSanctificationMetadataSurvivesRestoreAndRejectedCraft() {
    var source = hidden().storedItem();
    var implicitFactors = new HashMap<String, Map<String, Integer>>();
    var explicitFactors = new HashMap<String, Map<String, Integer>>();
    for (var modifier : source.implicits()) {
      var factors = new HashMap<String, Integer>();
      modifier.values().keySet().forEach(id -> factors.put(id, 100));
      implicitFactors.put(modifier.modifierId(), factors);
    }
    for (var modifier : source.explicits()) {
      var factors = new HashMap<String, Integer>();
      modifier.values().keySet().forEach(id -> factors.put(id, 100));
      explicitFactors.put(modifier.modifierId(), factors);
    }
    var sanctified =
        source.withSanctification(
            new ItemState.Sanctification(
                "independent-stat-percent-v1", implicitFactors, explicitFactors));
    var restored = AbyssCraftingState.from(sanctified, catalog);
    assertThat(restored.storedItem()).isEqualTo(sanctified);
    assertThat(restored.item().sanctification()).isEqualTo(sanctified.sanctification());
    var omens = Set.of("Omen_of_Sanctification");
    var result = executor.apply(restored, WorkbenchCurrency.DIVINE, omens, new Random(1));
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isSameAs(restored);
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyElementsOf(omens);
  }
}
