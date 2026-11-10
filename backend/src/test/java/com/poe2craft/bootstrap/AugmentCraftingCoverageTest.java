package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AugmentCraftingCoverageTest {
  private CatalogCraftingService service() {
    var definitions = WorkbenchDefinitionsLoader.initialize();
    var configuration = new CatalogConfiguration();
    var solar = configuration.itemCatalog(definitions);
    return configuration.workbenchService(
        solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  private boolean available(
      CatalogCraftingService service, ItemState state, WorkbenchCurrency action) {
    return service.simulator(state).actions(state, Set.of()).stream()
        .filter(a -> a.action() == action)
        .findFirst()
        .orElseThrow()
        .available();
  }

  private ModifierInstance minimum(ModifierDefinition d) {
    var values = new TreeMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }

  private ItemState empty(ItemCatalog catalog) {
    return new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            100,
            ItemState.Rarity.RARE,
            catalog.base().hasImplicit()
                ? List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow()))
                : List.of(),
            List.of(),
            Set.of(),
            2)
        .withEquipmentQuality(BaseRegistry.policy(catalog.base().id()).qualityLimit() ? 0 : null);
  }

  @Test
  void sixSourcePoolsActivateOnlyWhileEquippedAndUsePublishedWeights() {
    var service = service();
    var ids =
        List.of(
            "Uhtreds_Sidereus",
            "Kolrs_Hunt",
            "Voranas_Carnage",
            "Thruds_Might",
            "Medveds_Tending",
            "Katlas_Gloom");
    int checked = 0;
    for (var catalog : service.reviewedCatalogs())
      for (var id : ids) {
        if (!SocketAugments.supports(catalog.base().id(), id)) continue;
        var original = empty(catalog);
        var installed = SocketAugments.install(original, 0, id);
        var simulator = service.simulator(installed);
        var pool = simulator.pool(installed, WorkbenchCurrency.EXALTED, null);
        var sourced = pool.stream().filter(d -> d.tags().contains("augment_pool")).toList();
        assertThat(sourced)
            .as(catalog.base().name() + ":" + id)
            .isNotEmpty()
            .allSatisfy(d -> assertThat(d.weight()).isEqualTo(1));
        assertThat(service.simulator(original).pool(original, WorkbenchCurrency.EXALTED, null))
            .noneMatch(d -> d.tags().contains("augment_pool"));
        assertThat(pool)
            .noneMatch(
                d ->
                    d.tags().contains("augment_pool")
                        && !d.tags().contains(AugmentCraftingRules.effect(id).pool() + "_pool"));
        var selected = minimum(sourced.getFirst());
        var retained =
            new ItemState(
                    installed.snapshotId(),
                    installed.baseItemId(),
                    100,
                    ItemState.Rarity.RARE,
                    installed.implicits(),
                    List.of(selected),
                    Set.of(),
                    2)
                .withAugments(installed.installedAugments());
        assertThat(new ItemStateValidator(simulator.catalog()).validate(retained)).isEmpty();
        assertThatThrownBy(() -> SocketAugments.install(retained, 0, "Astrids_Creativity"))
            .isInstanceOf(IllegalArgumentException.class);
        checked++;
      }
    assertThat(checked).isEqualTo(97);
  }

  @Test
  void astridAllowsTwoCraftedOutcomesAndRetainsThemAfterReplacement() {
    var service = service();
    var catalog =
        service.reviewedCatalogs().stream()
            .filter(c -> BaseRegistry.itemClass(c.base().id()).equals("Wands"))
            .findFirst()
            .orElseThrow();
    var original = empty(catalog);
    var installed = SocketAugments.install(original, 0, "Astrids_Creativity");
    var targets = EquipmentCurrencyCatalog.targets(catalog.base().id());
    var target =
        targets.entrySet().stream()
            .filter(e -> e.getKey().contains("ALLOY"))
            .findFirst()
            .orElseThrow();
    var action = WorkbenchCurrency.valueOf(target.getKey());
    var first = minimum(catalog.find(target.getValue().getFirst()).orElseThrow()).asCrafted();
    var ordinary =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.weight() > 0
                        && !d.familyIds().stream()
                            .anyMatch(
                                catalog.find(first.modifierId()).orElseThrow().familyIds()
                                    ::contains))
            .findFirst()
            .orElseThrow();
    var one =
        new ItemState(
            original.snapshotId(),
            original.baseItemId(),
            100,
            ItemState.Rarity.RARE,
            original.implicits(),
            List.of(first, minimum(ordinary)),
            Set.of(),
            2);
    assertThat(available(service, one, action)).isFalse();
    // The second material can target a different guaranteed family; source-incompatible actions
    // still refuse normally.
    var eligible =
        targets.entrySet().stream()
            .filter(
                e ->
                    e.getValue().stream()
                        .allMatch(
                            id ->
                                java.util.Collections.disjoint(
                                    catalog.find(id).orElseThrow().familyIds(),
                                    catalog.find(first.modifierId()).orElseThrow().familyIds())))
            .map(e -> WorkbenchCurrency.valueOf(e.getKey()))
            .filter(a -> available(service, one.withAugments(installed.installedAugments()), a))
            .findFirst()
            .orElseThrow();
    WorkbenchSimulator.Result result = null;
    for (int seed = 0; seed < 100; seed++) {
      var attempt =
          service
              .simulator(one.withAugments(installed.installedAugments()))
              .apply(
                  one.withAugments(installed.installedAugments()),
                  eligible,
                  Set.of(),
                  new SplittableRandom(seed));
      if (attempt.state().explicits().stream()
              .filter(m -> AugmentCraftingRules.crafted(m, catalog))
              .count()
          == 2) {
        result = attempt;
        break;
      }
    }
    assertThat(result).isNotNull();
    var replaced = SocketAugments.install(result.state(), 0, "Perfect_Desert_Rune");
    assertThat(replaced.explicits()).isEqualTo(result.state().explicits());
    assertThat(new ItemStateValidator(catalog).validate(replaced)).isEmpty();
    assertThat(available(service, replaced, eligible)).isFalse();
  }

  @Test
  void serleAddsSuffixCapacityAndBindsItsSocketWhileGritQualityPersists() {
    var service = service();
    var catalog =
        service.reviewedCatalogs().stream()
            .filter(c -> BaseRegistry.itemClass(c.base().id()).equals("One Hand Maces"))
            .findFirst()
            .orElseThrow();
    var original = empty(catalog);
    var grit = SocketAugments.install(original, 0, "Legacy_of_Serles_Grit");
    var improved =
        service
            .simulator(grit)
            .apply(grit, WorkbenchCurrency.BLACKSMITHS_WHETSTONE, Set.of(), new Random(1));
    assertThat(improved.state().equipmentQuality()).isEqualTo(40);
    var swapped = SocketAugments.install(improved.state(), 0, "Astrids_Creativity");
    assertThat(swapped.equipmentQuality()).isEqualTo(40);
    assertThat(QualityCurrencyRules.refusal(swapped, WorkbenchCurrency.BLACKSMITHS_WHETSTONE))
        .isNotEmpty();
    var serle = SocketAugments.install(original, 0, "Serles_Triumph");
    assertThat(service.simulator(serle).catalog().base().rareSuffixes())
        .isEqualTo(catalog.base().rareSuffixes() + 1);
    var current = serle;
    for (int n = 0; n < 7; n++) {
      var result =
          service
              .simulator(current)
              .apply(current, WorkbenchCurrency.EXALTED, Set.of(), new Random(311 + n));
      assertThat(result.applied()).as(result.reason()).isTrue();
      current = result.state();
    }
    assertThat(current.explicits()).hasSize(7);
    assertThat(new ItemStateValidator(service.simulator(current).catalog()).validate(current))
        .isEmpty();
    assertThatThrownBy(() -> SocketAugments.install(serle, 0, "Astrids_Creativity"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
