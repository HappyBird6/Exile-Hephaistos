package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class EquipmentCurrencyCoverageTest {
  private CatalogCraftingService service() {
    WorkbenchDefinitionsLoader.initialize();
    var solar = ItemCatalogLoader.loadDefault();
    return new CatalogConfiguration()
        .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  private ModifierInstance minimum(ModifierDefinition d) {
    var values = new TreeMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }

  @Test
  void everySourcedEquipmentOutcomeIsExecutableWithoutChangingOrdinaryWeights() {
    var service = service();
    int checked = 0;
    for (var catalog : service.reviewedCatalogs()) {
      var targets = EquipmentCurrencyCatalog.targets(catalog.base().id());
      if (targets.isEmpty()) continue;
      var implicit =
          catalog.base().hasImplicit()
              ? List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow()))
              : List.<ModifierInstance>of();
      var ordinary =
          catalog.modifiers().values().stream()
              .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight() > 0)
              .findFirst()
              .orElseThrow();
      var state =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              100,
              ItemState.Rarity.RARE,
              implicit,
              List.of(minimum(ordinary)),
              Set.of());
      var simulator = service.simulator(state);
      for (var target : targets.entrySet()) {
        var action = WorkbenchCurrency.valueOf(target.getKey());
        assertThat(simulator.baseMaterialActions())
            .as(catalog.base().name() + " " + action)
            .contains(action);
        WorkbenchSimulator.Result result;
        try {
          result = simulator.apply(state, action, Set.of(), new Random(371));
        } catch (IllegalArgumentException error) {
          throw new AssertionError(
              catalog.base().name()
                  + " "
                  + action
                  + " "
                  + new ItemStateValidator(catalog).validate(state),
              error);
        }
        assertThat(result.applied())
            .as(catalog.base().name() + " " + action + ": " + result.reason())
            .isTrue();
        assertThat(result.events()).hasSize(2);
        assertThat(result.events().getFirst().modifierId()).isEqualTo(ordinary.id());
        assertThat(result.events().getLast().modifierId()).isIn(target.getValue());
        assertThat(result.state().implicits()).isEqualTo(state.implicits());
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        target
            .getValue()
            .forEach(id -> assertThat(catalog.find(id).orElseThrow().weight()).isZero());
        checked++;
      }
    }
    assertThat(checked).isGreaterThan(600);
  }

  @Test
  void lightRemovesOnlyDesecratedKnownOrHiddenSlotsAndConsumesOnlyItself() {
    var service = service();
    var catalog =
        ManualRevealCatalog.enrich(
            service.reviewedCatalogs().stream()
                .filter(c -> c.base().name().equals("Solar Amulet"))
                .findFirst()
                .orElseThrow());
    var original =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow())),
            List.of(minimum(catalog.find("amulet:prefix:hale").orElseThrow())),
            Set.of());
    var hidden =
        DesecrationCurrency.apply(
            original,
            "Preserved_Collarbone",
            ModifierDefinition.AffixType.SUFFIX,
            null,
            Set.of(),
            catalog);
    var slot = hidden.slots().getLast();
    var revealed =
        hidden.selectDirect(
            slot.id(),
            minimum(
                catalog
                    .find(hidden.eligibleRevealIds(slot.id(), catalog).getFirst())
                    .orElseThrow()),
            catalog);
    var executor = new AbyssCurrencyExecutor(catalog, service.simulator(original));
    var omens = Set.of("Omen_of_Light", "Omen_of_the_Blessed");
    for (var before : List.of(hidden, revealed)) {
      var result = executor.apply(before, WorkbenchCurrency.ANNULMENT, omens, new Random(4));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().storedItem().explicits()).isEqualTo(original.explicits());
      assertThat(result.state().slots()).noneMatch(AbyssCraftingState.Slot::desecrated);
      assertThat(result.consumedOmens()).containsExactly("Omen_of_Light");
      assertThat(result.remainingOmens()).containsExactly("Omen_of_the_Blessed");
    }
    var unresolved =
        new AbyssCraftingState(
            hidden
                .item()
                .withObservations(
                    new ItemObservations(
                        null,
                        List.of("Can have 1 additional Crafted Modifiers (rune)"),
                        List.of())),
            hidden.slots(),
            hidden.offer());
    assertThatThrownBy(
            () -> executor.apply(unresolved, WorkbenchCurrency.ANNULMENT, omens, new Random(4)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Augment");
    var none = AbyssCraftingState.from(original, catalog);
    var blocked = executor.apply(none, WorkbenchCurrency.ANNULMENT, omens, new Random(4));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(none);
    assertThat(blocked.consumedOmens()).isEmpty();
    assertThat(
            executor
                .apply(
                    hidden,
                    WorkbenchCurrency.ANNULMENT,
                    Set.of("Omen_of_Light", "Omen_of_Greater_Annulment"),
                    new Random(4))
                .applied())
        .isFalse();
  }
}
