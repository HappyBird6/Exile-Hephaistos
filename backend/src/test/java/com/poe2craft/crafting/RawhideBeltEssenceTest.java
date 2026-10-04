package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RawhideBeltEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadBelt();
  final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          Map.of());

  static Stream<WorkbenchCurrency> actions() {
    return perfect();
  }

  static Stream<WorkbenchCurrency> perfect() {
    return Stream.of(WorkbenchCurrency.PERFECT_ESSENCE_INSULATION);
  }

  ItemState state(int level, ItemState.Rarity rarity, String... ids) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        BeltEssenceTargets.BASE_ID,
        level,
        rarity,
        List.of(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var d = catalog.find(id).orElseThrow();
                  var values = new LinkedHashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(id, values);
                })
            .toList(),
        Set.of());
  }

  @ParameterizedTest
  @MethodSource("actions")
  void exactTargetsLevelsAtomicRefusalsAndNumericSource(WorkbenchCurrency action) {
    boolean replacement = !action.replacementModifiers().isEmpty();
    var id =
        replacement
            ? action.replacementModifiers().getFirst()
            : action.replacementModifiers().getFirst();
    var target = catalog.find(id).orElseThrow();
    var rarity = replacement ? ItemState.Rarity.RARE : ItemState.Rarity.MAGIC;
    var before = state(target.requiredItemLevel(), rarity, "rawhide-belt:prefix:hale");
    var r = simulator.apply(before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(17));
    assertThat(r.applied()).isTrue();
    assertThat(r.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
    assertThat(r.events().getLast().modifierId()).isEqualTo(id);
    assertThat(r.events().getLast().selectionProbability()).isEqualTo(1);
    assertThat(new ItemStateValidator(catalog).validate(r.state())).isEmpty();
    assertThat(r.consumedOmens()).isEmpty();
    assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    if (!replacement) assertThat(r.state().explicits()).containsAll(before.explicits());
    var range = target.stats().getFirst();
    if (range.min() != range.max())
      assertThat(r.assumptions())
          .anySatisfy(
              a -> {
                assertThat(a.id()).isEqualTo("assumed-source-integer-roll-v1");
                assertThat(a.sourceUrl()).isEqualTo(target.sourceUrl());
                assertThat(a.reason()).contains("UNVERIFIED");
              });
    else
      assertThat(r.assumptions())
          .noneSatisfy(a -> assertThat(a.id()).isEqualTo("assumed-source-integer-roll-v1"));
    var low = state(target.requiredItemLevel() - 1, rarity, "rawhide-belt:prefix:hale");
    var denied = simulator.apply(low, action, Set.of(), new Random(17));
    assertThat(denied.applied()).isFalse();
    assertThat(denied.state()).isEqualTo(low);
    assertThat(
            simulator
                .apply(state(82, ItemState.Rarity.NORMAL), action, Set.of(), new Random(17))
                .applied())
        .isFalse();
    var overlap =
        replacement ? state(82, rarity, id, "rawhide-belt:prefix:hale") : state(82, rarity, id);
    var conflict = simulator.apply(overlap, action, Set.of(), new Random(17));
    assertThat(conflict.applied()).isFalse();
    assertThat(conflict.state()).isEqualTo(overlap);
  }

  @ParameterizedTest
  @MethodSource("perfect")
  void crystallisationRemovalSidesLocksAndPairConflict(WorkbenchCurrency action) {
    var before =
        state(
            82,
            ItemState.Rarity.RARE,
            "rawhide-belt:prefix:hale",
            "rawhide-belt:suffix:of-the-brute");
    var seen = new HashSet<String>();
    for (int i = 0; i < 40; i++) {
      var r = simulator.apply(before, action, Set.of(), new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
      seen.add(r.events().getFirst().modifierId());
    }
    assertThat(seen).hasSize(2);
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var r =
          simulator.apply(
              before, action, Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random(17));
      assertThat(r.applied()).isTrue();
      assertThat(catalog.find(r.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      assertThat(r.consumedOmens()).containsExactly(omen.id());
      assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
    var locked =
        new ModifierInstance(
            before.explicits().getFirst().modifierId(),
            before.explicits().getFirst().values(),
            true);
    var state =
        new ItemState(
            before.snapshotId(),
            before.baseItemId(),
            82,
            before.rarity(),
            List.of(),
            List.of(locked, before.explicits().getLast()),
            Set.of());
    assertThat(simulator.apply(state, action, Set.of(), new Random(17)).state().explicits())
        .contains(locked);
    var refused =
        simulator.apply(
            before,
            action,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random(17));
    assertThat(refused.applied()).isFalse();
    assertThat(refused.state()).isEqualTo(before);
    assertThat(refused.consumedOmens()).isEmpty();
  }

  @Test
  void completeWeightedPoolNoSpecialSpawnAndNoImplicitLottery() {
    assertThat(catalog.modifiers()).hasSize(136);
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(135);
    assertThat(catalog.metadata().prefixCount()).isEqualTo(53);
    assertThat(catalog.metadata().suffixCount()).isEqualTo(83);
    assertThat(catalog.metadata().prefixWeight()).isEqualTo(49600);
    assertThat(catalog.metadata().suffixWeight()).isEqualTo(70700);
    assertThat(catalog.base().hasImplicit()).isFalse();
    assertThat(catalog.modifiers().values().stream().filter(d -> d.stats().size() > 1)).hasSize(7);
    for (int i = 0; i < 60; i++) {
      var r =
          simulator.apply(
              state(82, ItemState.Rarity.NORMAL),
              WorkbenchCurrency.ALCHEMY,
              Set.of(),
              new Random(i));
      assertThat(r.applied()).isTrue();
      assertThat(r.state().explicits())
          .allSatisfy(
              m -> assertThat(catalog.find(m.modifierId()).orElseThrow().weight()).isPositive());
      assertThat(new ItemStateValidator(catalog).validate(r.state())).isEmpty();
    }
    assertThat(simulator.actions(state(82, ItemState.Rarity.NORMAL), Set.of())).hasSize(19);
    assertThat(
            simulator
                .apply(
                    state(82, ItemState.Rarity.NORMAL),
                    WorkbenchCurrency.ARTIFICER,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
  }

  @Test
  void independentFifthBaseAndPreviousCatalogsRemainIntact() {
    var solar = ItemCatalogLoader.loadDefault();
    var service =
        new WorkbenchService(
            solar,
            new WorkbenchSimulator(solar, new CraftingEngine(solar)),
            StockyHysteriaTest.stocky(),
            ItemCatalogLoader.loadBow(),
            ItemCatalogLoader.loadWand(),
            ItemCatalogLoader.loadBody(),
            ItemCatalogLoader.loadSceptre(),
            catalog);
    assertThat(service.initial("belt", 82).state().baseItemId())
        .isEqualTo(BeltEssenceTargets.BASE_ID);
    assertThat(service.initial("belt", 82).augmentSockets()).isNull();
    assertThat(service.initial("belt", 82).qualityLimit()).isNull();
    assertThat(service.initial("belt", 82).ruleVersion())
        .isEqualTo(
            "belt-workbench-perfect-essence-v1-homogenising-legacy-v1-legacy-five-v1-omen-composition-v1");
    assertThat(service.initial("bow", 82).modifiers()).hasSize(146);
    assertThat(service.initial("wand", 82).modifiers()).hasSize(187);
    assertThat(service.initial("stocky", 82).modifiers()).hasSize(194);
    assertThat(service.initial("belt", 82).metadata().sourceUrl())
        .isEqualTo("https://poe2db.tw/us/Belts");
    assertThat(service.initial("solar", 82).modifiers()).hasSize(218);
    assertThat(
            new WorkbenchSimulator(solar, new CraftingEngine(solar))
                .apply(
                    SolarAmulet.initial(solar, 82, 15),
                    WorkbenchCurrency.PERFECT_ESSENCE_INSULATION,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
  }

  @Test
  void divineIsAtomicUnsupportedAndQualityStaysUnknown() {
    var before =
        state(
            82,
            ItemState.Rarity.RARE,
            "rawhide-belt:prefix:hale",
            "rawhide-belt:suffix:of-the-brute");
    var omens = Set.of(WorkbenchOmen.BLESSED.id());
    var r = simulator.apply(before, WorkbenchCurrency.DIVINE, omens, new Random(1));
    assertThat(r.applied()).isFalse();
    assertThat(r.state()).isEqualTo(before);
    assertThat(r.events()).isEmpty();
    assertThat(r.consumedOmens()).isEmpty();
    assertThat(r.remainingOmens()).containsExactlyElementsOf(omens);
    assertThat(QualityLimitRules.describe(before, catalog)).isNull();
    assertThat(simulator.actions(before, Set.of()))
        .noneMatch(a -> a.action() == WorkbenchCurrency.DIVINE);
  }
}
