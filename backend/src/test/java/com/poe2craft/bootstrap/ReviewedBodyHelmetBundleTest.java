package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedBodyHelmetBundleTest {
  @ParameterizedTest
  @ValueSource(
      strings = {"slipstrike", "death-mail", "sleek", "vile", "wolfskin", "ancestral", "cryptic"})
  void completeSourcePoolsCraftingClassGatesAndLowItemLevel(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    boolean body = ReviewedBodies.BASES.containsKey(key);
    int ordinary =
        switch (key) {
          case "slipstrike" -> 144;
          case "death-mail" -> 155;
          case "sleek", "wolfskin" -> 152;
          case "vile" -> 141;
          case "ancestral" -> 125;
          case "cryptic" -> 135;
          default -> throw new IllegalArgumentException(key);
        };
    assertThat(c.modifiers()).hasSize(ordinary + (body ? 5 : 3));
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0))
        .hasSize(ordinary)
        .allSatisfy(d -> assertThat(d.weight()).isEqualTo(1));
    var solar = ItemCatalogLoader.loadDefault();
    var service =
        new CraftingConfiguration()
            .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
    var initial = service.initial(key, 82);
    assertThat(initial.state().baseItemId()).isEqualTo(c.base().id());
    assertThat(initial.state().implicits()).isEmpty();
    assertThat(initial.augmentSockets()).isNull();
    assertThat(initial.qualityLimit().maximumQuality()).isEqualTo(20);
    var start =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    var state = start;
    for (var action :
        List.of(
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.EXALTED,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.CHAOS)) {
      var result = service.apply(state, action, Set.of(), new Random(77));
      assertThat(result.applied()).as(key + ":" + action).isTrue();
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      for (var event : result.events())
        if (event.kind().equals("ADD")) {
          var assumption =
              result.assumptions().stream()
                  .filter(
                      a ->
                          a.id()
                                  .equals(
                                      body
                                          ? "body-uniform-candidates-v1"
                                          : "helmets-uniform-candidates-v1")
                              && a.candidates().contains(event.modifierId()))
                  .findFirst()
                  .orElseThrow();
          assertThat(event.selectionProbability()).isCloseTo(1.0 / assumption.n(), within(1e-12));
        }
      state = result.state();
    }
    assertThat(service.apply(start, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(77)).applied())
        .isTrue();
    var companions =
        c.modifiers().values().stream()
            .filter(
                d ->
                    d.weight() > 0
                        && d.requiredItemLevel() == 1
                        && (d.familyIds().contains("IncreasedLife")
                            || d.familyIds().contains("FireResistance")))
            .map(
                d -> {
                  var values = new HashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(d.id(), values);
                })
            .toList();
    assertThat(companions).hasSize(2);
    var rare =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            companions,
            Set.of());
    var actions =
        body
            ? List.of(
                WorkbenchCurrency.PERFECT_ESSENCE_BODY,
                WorkbenchCurrency.PERFECT_ESSENCE_RUIN,
                WorkbenchCurrency.PERFECT_ESSENCE_SEEKING)
            : List.of(WorkbenchCurrency.PERFECT_ESSENCE_THAWING);
    for (var action : actions) {
      var result = service.apply(rare, action, Set.of(), new Random(77));
      assertThat(result.applied()).as(key + ":" + action).isTrue();
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      var low =
          new ItemState(
              c.metadata().snapshotId(),
              c.base().id(),
              71,
              rare.rarity(),
              List.of(),
              List.of(),
              Set.of());
      assertThat(service.apply(low, action, Set.of(), new Random(77)).applied()).isFalse();
    }
    for (var action :
        List.of(
            body
                ? WorkbenchCurrency.PERFECT_ESSENCE_THAWING
                : WorkbenchCurrency.PERFECT_ESSENCE_BODY,
            WorkbenchCurrency.PERFECT_ESSENCE_GROUNDING,
            WorkbenchCurrency.ESSENCE_HORROR,
            WorkbenchCurrency.ARTIFICER)) {
      var refused = service.apply(rare, action, Set.of(), new Random(77));
      assertThat(refused.applied()).as(key + ":" + action).isFalse();
      assertThat(refused.state()).isEqualTo(rare);
      assertThat(refused.events()).isEmpty();
    }
    for (var action :
        List.of(WorkbenchCurrency.ESSENCE_HYSTERIA, WorkbenchCurrency.ESSENCE_ABYSS)) {
      var result =
          service.apply(
              rare, action, Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()), new Random(77));
      assertThat(result.applied()).as(key + ":" + action + ":" + result.reason()).isTrue();
      assertThat(result.consumedOmens())
          .containsExactly(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id());
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      if (action == WorkbenchCurrency.ESSENCE_ABYSS)
        assertThat(result.events().getLast().selectionProbability()).isEqualTo(0.5);
      if (action == WorkbenchCurrency.ESSENCE_HYSTERIA) {
        var below =
            new ItemState(
                c.metadata().snapshotId(),
                c.base().id(),
                body ? 62 : 4,
                ItemState.Rarity.RARE,
                List.of(),
                companions,
                Set.of());
        assertThat(service.apply(below, action, Set.of(), new Random(77)).applied()).isFalse();
      }
      var refused =
          service.apply(
              rare,
              action,
              Set.of(
                  WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                  WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
              new Random(77));
      assertThat(refused.applied()).isFalse();
      assertThat(refused.state()).isEqualTo(rare);
      assertThat(refused.events()).isEmpty();
    }
    var low =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            1,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    var rolled = service.apply(low, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(77));
    assertThat(rolled.applied()).isTrue();
    assertThat(rolled.state().explicits())
        .allSatisfy(
            m -> assertThat(c.modifiers().get(m.modifierId()).requiredItemLevel()).isEqualTo(1));
    var foreign =
        new ItemState(
            solar.metadata().snapshotId(),
            solar.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(c).validate(foreign)).isNotEmpty();
  }
}
