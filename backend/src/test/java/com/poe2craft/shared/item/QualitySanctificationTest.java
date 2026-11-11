package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.QualityCurrencyRules;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.StockyCraftingFixture;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class QualitySanctificationTest {
  abstract static class StateWire {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract ItemState.Sanctification sanctification();
  }

  @Test
  void matchesSharedBrowserProjectionCases() throws Exception {
    var cases =
        new ObjectMapper()
            .readTree(
                java.nio.file.Path.of("../contracts/sanctification/projection-cases.json")
                    .toFile());
    for (var test : cases) {
      var modifier =
          new ModifierInstance(
              "mod",
              Map.of("value", test.path("source").longValue()),
              test.path("fractured").asBoolean());
      boolean implicit = test.path("implicit").asBoolean();
      Map<String, Map<String, Integer>> factors =
          test.path("factor").isNull()
              ? Map.of()
              : Map.of("mod", Map.of("value", test.path("factor").asInt()));
      var state =
          new ItemState(
                  "test",
                  "test",
                  82,
                  ItemState.Rarity.RARE,
                  implicit ? List.of(modifier) : List.of(),
                  implicit ? List.of() : List.of(modifier),
                  Set.of())
              .withSanctification(
                  new ItemState.Sanctification(
                      test.path("policy").asText(),
                      implicit ? factors : Map.of(),
                      implicit ? Map.of() : factors));
      assertThat(QualitySanctification.value(state, modifier, "value", implicit))
          .as(test.path("id").asText())
          .isEqualByComparingTo(test.path("expected").asText());
    }
  }

  @Test
  void qualityUsesUserSelectedMaximumForEveryRarityAndNeverDropsOtherState() {
    var fixture = new StockyCraftingFixture();
    for (var rarity :
        List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var state =
          new ItemState(
              fixture.root.snapshotId(),
              fixture.root.baseItemId(),
              82,
              rarity,
              List.of(),
              List.of(),
              Set.of(),
              0,
              null,
              List.of(),
              0,
              new ItemObservations(0, List.of(), List.of()));
      var result =
          fixture.simulator.apply(
              state, WorkbenchCurrency.ARMOURERS_SCRAP, Set.of(), new Random(1));
      assertThat(result.applied()).isTrue();
      assertThat(result.state()).isEqualTo(state.withEquipmentQuality(20));
      assertThat(result.qualityLimit())
          .isEqualTo(QualityLimitRules.describe(state, fixture.catalog));
      assertThat(result.assumptions())
          .singleElement()
          .satisfies(
              a -> {
                assertThat(a.id()).isEqualTo(QualityCurrencyRules.VERSION);
                assertThat(a.candidateUnit()).isEqualTo("equipment quality target percentage");
                assertThat(a.n()).isEqualTo(1);
                assertThat(a.min()).isEqualTo(20L);
                assertThat(a.max()).isEqualTo(20L);
                assertThat(a.reason()).contains("User-selected simulator policy");
              });
      assertThat(result.state().observations()).isEqualTo(state.observations());
      assertThat(result.state().augmentSockets()).isZero();
      assertThat(result.state().explicits()).isEqualTo(state.explicits());
      assertThat(
              fixture
                  .simulator
                  .apply(
                      state.withEquipmentQuality(19),
                      WorkbenchCurrency.ARMOURERS_SCRAP,
                      Set.of(),
                      new Random(1))
                  .state()
                  .equipmentQuality())
          .isEqualTo(20);
      for (int quality : List.of(20, 30)) {
        var inherited = state.withEquipmentQuality(quality);
        var blocked =
            fixture.simulator.apply(
                inherited, WorkbenchCurrency.ARMOURERS_SCRAP, Set.of(), new Random(1));
        assertThat(blocked.applied()).isFalse();
        assertThat(blocked.state()).isEqualTo(inherited);
      }
      assertThat(
              fixture
                  .simulator
                  .apply(state, WorkbenchCurrency.BLACKSMITHS_WHETSTONE, Set.of(), new Random(1))
                  .applied())
          .isFalse();
    }
    assertThat(
            fixture
                .simulator
                .apply(fixture.root, WorkbenchCurrency.ARMOURERS_SCRAP, Set.of(), new Random(1))
                .applied())
        .isFalse();
  }

  @Test
  void sanctifiesWithoutRerollingAndPreservesImplicitQualitySlotsObservationsAndLocks()
      throws Exception {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var root = SolarAmulet.initial(catalog);
    var rare = simulator.apply(root, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(2)).state();
    var original = rare.explicits().getFirst();
    var locked = new ModifierInstance(original.modifierId(), original.values(), true);
    var modifiers = new ArrayList<>(rare.explicits());
    modifiers.set(0, locked);
    rare =
        new ItemState(
            rare.snapshotId(),
            rare.baseItemId(),
            rare.itemLevel(),
            rare.rarity(),
            rare.implicits(),
            modifiers,
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.FLESH, 20),
            List.of(
                new ItemState.DesecratedSlot(
                    "unknown",
                    modifiers.stream()
                                .filter(
                                    m ->
                                        catalog.find(m.modifierId()).orElseThrow().affixType()
                                            == ModifierDefinition.AffixType.PREFIX)
                                .count()
                            >= 3
                        ? ModifierDefinition.AffixType.SUFFIX
                        : ModifierDefinition.AffixType.PREFIX,
                    null)),
            null,
            new ItemObservations(null, List.of("Preserved unresolved Augment effect"), List.of()));
    var result =
        simulator.apply(
            rare,
            WorkbenchCurrency.DIVINE,
            Set.of("Omen_of_Sanctification", "Omen_of_Whittling"),
            new Random(4));
    assertThat(result.applied()).isTrue();
    var next = result.state();
    assertThat(next.conditions()).containsExactly(ItemState.Condition.SANCTIFIED);
    assertThat(next.implicits()).isEqualTo(rare.implicits());
    assertThat(next.explicits()).isEqualTo(rare.explicits());
    assertThat(next.desecrated()).isEqualTo(rare.desecrated());
    assertThat(next.catalystQuality()).isEqualTo(rare.catalystQuality());
    assertThat(next.observations()).isEqualTo(rare.observations());
    assertThat(next.sanctification()).isNotNull();
    assertThat(next.sanctification().implicits()).isEmpty();
    assertThat(next.sanctification().policy()).isEqualTo("unfractured-explicit-stat-percent-v3");
    assertThat(QualitySanctification.valid(next, catalog)).isTrue();
    assertThat(next.sanctification().explicits()).doesNotContainKey(locked.modifierId());
    assertThat(next.sanctification().explicits()).isNotEmpty();
    locked
        .values()
        .forEach(
            (stat, value) ->
                assertThat(QualitySanctification.value(next, locked, stat, false))
                    .isEqualByComparingTo(Long.toString(value)));
    // Current responses cannot smuggle a multiplier onto a locked modifier.
    var factors = new TreeMap<>(next.sanctification().explicits());
    var lockedFactors = new TreeMap<String, Integer>();
    QualitySanctification.targets(original, catalog).forEach(stat -> lockedFactors.put(stat, 122));
    assertThat(lockedFactors).isNotEmpty();
    factors.put(locked.modifierId(), lockedFactors);
    var invalid =
        next.withSanctification(
            new ItemState.Sanctification(QualitySanctification.POLICY, Map.of(), factors));
    assertThat(QualitySanctification.valid(invalid, catalog)).isFalse();
    assertThat(new ItemStateValidator(catalog).validate(invalid)).isNotEmpty();
    // Historical maps survive JSON loading, but protected values ignore those old factors.
    var legacy =
        next.withSanctification(
            new ItemState.Sanctification("explicit-stat-percent-v2", Map.of(), factors));
    assertThat(QualitySanctification.valid(legacy, catalog)).isTrue();
    assertThat(
            new ObjectMapper()
                .readValue(new ObjectMapper().writeValueAsString(legacy), ItemState.class))
        .isEqualTo(legacy);
    locked
        .values()
        .forEach(
            (stat, value) ->
                assertThat(QualitySanctification.value(legacy, locked, stat, false))
                    .isEqualByComparingTo(Long.toString(value)));

    assertThat(result.consumedOmens()).containsExactly("Omen_of_Sanctification");
    assertThat(result.remainingOmens()).containsExactly("Omen_of_Whittling");
    assertThat(result.events()).isEmpty();
    assertThat(simulator.actions(next, Set.of()))
        .noneMatch(WorkbenchSimulator.Availability::available);
    assertThat(simulator.apply(next, WorkbenchCurrency.DIVINE, Set.of(), new Random(1)).state())
        .isEqualTo(next);
    var mapper = new ObjectMapper().addMixIn(ItemState.class, StateWire.class);
    assertThat(mapper.readValue(mapper.writeValueAsString(next), ItemState.class)).isEqualTo(next);
    assertThat(mapper.writeValueAsString(root)).doesNotContain("sanctification");
    assertThat(
            next.withObservations(next.observations())
                .withEquipmentQuality(null)
                .withDesecrated(next.desecrated())
                .sanctification())
        .isEqualTo(next.sanctification());
  }

  @Test
  void sanctificationIsNotConsumedForWrongRarityOrConflictingDivineOmens() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var root = SolarAmulet.initial(catalog);
    var refused =
        simulator.apply(
            root, WorkbenchCurrency.DIVINE, Set.of("Omen_of_Sanctification"), new Random(1));
    assertThat(refused.applied()).isFalse();
    assertThat(refused.consumedOmens()).isEmpty();
    var rare = simulator.apply(root, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(2)).state();
    assertThat(
            simulator
                .apply(
                    rare,
                    WorkbenchCurrency.DIVINE,
                    Set.of("Omen_of_Sanctification", "Omen_of_the_Blessed"),
                    new Random(1))
                .applied())
        .isFalse();
    var divine = simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), new Random(1));
    assertThat(divine.state().implicits()).isEqualTo(rare.implicits());
    assertThat(divine.state().sanctification()).isNull();
  }
}
