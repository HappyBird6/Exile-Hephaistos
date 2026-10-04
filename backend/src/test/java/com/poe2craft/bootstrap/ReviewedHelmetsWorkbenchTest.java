package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedHelmetsWorkbenchTest {
  @ParameterizedTest
  @ValueSource(strings = {"freebooter", "gladiatorial", "grinning"})
  void completePoolsSharedEvasionCraftingAndClassSpecificEssences(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var solar = ItemCatalogLoader.loadDefault();
    var service =
        new CraftingConfiguration()
            .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
    int ordinary =
        switch (key) {
          case "freebooter" -> 137;
          case "gladiatorial" -> 147;
          default -> 135;
        };
    assertThat(c.modifiers()).hasSize(ordinary + 1);
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0))
        .hasSize(ordinary)
        .allSatisfy(d -> assertThat(d.weight()).isEqualTo(1));
    assertThat(c.modifiers().values())
        .anySatisfy(
            d -> {
              assertThat(d.id()).startsWith("polished-bracers:suffix:");
              assertThat(d.familyIds()).contains("EvasionAppliesToDeflection");
            });
    var initial = service.initial(key, 82);
    assertThat(initial.state().implicits()).isEmpty();
    assertThat(initial.augmentSockets()).isNull();
    assertThat(initial.qualityLimit().maximumQuality()).isEqualTo(20);
    var state =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    var normal = state;
    for (var action :
        List.of(
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.EXALTED,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.CHAOS)) {
      var result = service.apply(state, action, Set.of(), new Random(77));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      for (var event : result.events())
        if (event.kind().equals("ADD")) {
          var assumption =
              result.assumptions().stream()
                  .filter(
                      a ->
                          a.id().equals("helmets-uniform-candidates-v1")
                              && a.candidates().contains(event.modifierId()))
                  .findFirst()
                  .orElseThrow();
          assertThat(event.selectionProbability()).isCloseTo(1.0 / assumption.n(), within(1e-12));
        }
      state = result.state();
    }
    assertThat(service.apply(normal, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(77)).applied())
        .isTrue();
    var companion =
        c.modifiers().values().stream()
            .filter(
                d ->
                    d.weight() > 0
                        && d.requiredItemLevel() == 1
                        && d.familyIds().contains("IncreasedLife"))
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    companion.stats().forEach(s -> values.put(s.id(), s.min()));
    var rare =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(new ModifierInstance(companion.id(), values)),
            Set.of());
    var ice =
        service.apply(
            rare,
            WorkbenchCurrency.PERFECT_ESSENCE_THAWING,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            new Random(77));
    assertThat(ice.applied()).isTrue();
    assertThat(ice.events().getLast().modifierId())
        .isEqualTo("rusted-greathelm:suffix:essence-cold-damage-recouped-as-life");
    assertThat(ice.consumedOmens()).containsExactly(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id());
    var conflict =
        service.apply(
            rare,
            WorkbenchCurrency.PERFECT_ESSENCE_THAWING,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random(77));
    assertThat(conflict.applied()).isFalse();
    assertThat(conflict.state()).isEqualTo(rare);
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_ESSENCE_GROUNDING,
            WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE,
            WorkbenchCurrency.PERFECT_ESSENCE_BODY,
            WorkbenchCurrency.ESSENCE_HORROR,
            WorkbenchCurrency.ARTIFICER,
            WorkbenchCurrency.RUNIC_ALLOY,
            WorkbenchCurrency.CATALYST_FLESH)) {
      var refused = service.apply(rare, action, Set.of(), new Random(77));
      assertThat(refused.applied()).as(action.name()).isFalse();
      assertThat(refused.state()).isEqualTo(rare);
      assertThat(refused.events()).isEmpty();
    }
    var old = ItemCatalogLoader.loadHelmet();
    assertThat(
            new ItemStateValidator(c)
                .validate(
                    new ItemState(
                        old.metadata().snapshotId(),
                        old.base().id(),
                        82,
                        ItemState.Rarity.NORMAL,
                        List.of(),
                        List.of(),
                        Set.of())))
        .isNotEmpty();
    assertThat(service.initial("imperial", 82).state().baseItemId())
        .isEqualTo("Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame");
  }
}
