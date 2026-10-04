package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedGlovesWorkbenchTest {
  private WorkbenchService service() {
    var solar = ItemCatalogLoader.loadDefault();
    return new CraftingConfiguration()
        .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  private ItemState normal(ItemCatalog c, int level) {
    return new ItemState(
        c.metadata().snapshotId(),
        c.base().id(),
        level,
        ItemState.Rarity.NORMAL,
        List.of(),
        List.of(),
        Set.of());
  }

  private ModifierInstance instance(ModifierDefinition d) {
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }

  @ParameterizedTest
  @ValueSource(strings = {"massive", "sirenscale", "adherent"})
  void fullPoolUniformCraftingAndLegacyIsolation(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var service = service();
    var initial = service.initial(key, 82);
    int ordinary =
        switch (key) {
          case "massive" -> 182;
          case "sirenscale" -> 178;
          default -> 188;
        };
    assertThat(c.modifiers()).hasSize(ordinary + 4);
    assertThat(c.modifiers().values().stream().filter(d -> d.weight() > 0))
        .hasSize(ordinary)
        .allSatisfy(d -> assertThat(d.weight()).isEqualTo(1));
    assertThat(initial.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(initial.state().implicits()).isEmpty();
    assertThat(initial.augmentSockets()).isNull();
    assertThat(c.compatibleSnapshotIds()).isEmpty();
    var state = normal(c, 82);
    var first = service.apply(state, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(77));
    long eligible =
        c.modifiers().values().stream()
            .filter(d -> d.weight() > 0 && d.requiredItemLevel() <= 82)
            .count();
    assertThat(first.applied()).isTrue();
    assertThat(first.assumptions())
        .anySatisfy(a -> assertThat(a.id()).isEqualTo("gloves-uniform-candidates-v1"));
    assertThat(first.events().getFirst().selectionProbability())
        .isCloseTo(1.0 / eligible, within(1e-12));
    state = first.state();
    for (var action :
        List.of(
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.EXALTED,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.CHAOS)) {
      var result = service.apply(state, action, Set.of(), new Random(77));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      state = result.state();
    }
    assertThat(service.qualityDisplay(state).qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(service.initial("stocky", 82).state().baseItemId())
        .isEqualTo(WorkbenchService.STOCKY_BASE_ID);
    var old = service.initial("stocky", 82).state();
    var oldState =
        new ItemState(
            old.snapshotId(),
            old.baseItemId(),
            82,
            ItemState.Rarity.NORMAL,
            old.implicits(),
            List.of(),
            old.conditions());
    assertThat(new ItemStateValidator(c).validate(oldState)).isNotEmpty();
    var low =
        service.apply(normal(c, 1), WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(77));
    assertThat(low.applied()).isTrue();
    assertThat(low.state().explicits())
        .allSatisfy(
            m -> assertThat(c.find(m.modifierId()).orElseThrow().requiredItemLevel()).isEqualTo(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"massive", "sirenscale", "adherent"})
  void sourceEssencesClassRefusalFracturesAndLowLevel(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var service = service();
    var suffix =
        c.modifiers().values().stream()
            .filter(
                d ->
                    d.familyIds().contains("FireResistance")
                        && d.weight() > 0
                        && d.requiredItemLevel() == 1)
            .findFirst()
            .orElseThrow();
    var magic =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.MAGIC,
            List.of(),
            List.of(instance(suffix)),
            Set.of());
    for (var action :
        List.of(
            WorkbenchCurrency.LESSER_ESSENCE_BODY,
            WorkbenchCurrency.ESSENCE_BODY,
            WorkbenchCurrency.GREATER_ESSENCE_BODY,
            WorkbenchCurrency.LESSER_ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.GREATER_ESSENCE_ENHANCEMENT,
            WorkbenchCurrency.LESSER_ESSENCE_INFINITE)) {
      var result = service.apply(magic, action, Set.of(), new Random(77));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      if (action == WorkbenchCurrency.LESSER_ESSENCE_ENHANCEMENT) {
        var target = c.find(result.events().getLast().modifierId()).orElseThrow();
        assertThat(target.familyIds()).contains("DefencesPercent");
        assertThat(target.text()).contains(key.equals("massive") ? "Armour" : "Energy Shield");
      }
    }
    var lowMagic =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            15,
            ItemState.Rarity.MAGIC,
            List.of(),
            List.of(instance(suffix)),
            Set.of());
    var low =
        service.apply(
            lowMagic, WorkbenchCurrency.LESSER_ESSENCE_ENHANCEMENT, Set.of(), new Random(77));
    assertThat(low.applied()).isFalse();
    assertThat(low.state()).isEqualTo(lowMagic);
    var life =
        c.modifiers().values().stream()
            .filter(
                d ->
                    d.familyIds().contains("IncreasedLife")
                        && d.weight() > 0
                        && d.requiredItemLevel() == 1)
            .findFirst()
            .orElseThrow();
    var rare =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(instance(life), instance(suffix)),
            Set.of());
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_ESSENCE_GROUNDING,
            WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE,
            WorkbenchCurrency.ESSENCE_ABYSS)) {
      var result =
          service.apply(
              rare, action, Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()), new Random(77));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(result.consumedOmens())
          .containsExactly(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id());
      var targets =
          action == WorkbenchCurrency.ESSENCE_ABYSS
              ? List.of(
                  "stocky-mitts:prefix:essence-abyssal-mark",
                  "stocky-mitts:suffix:essence-abyssal-mark")
              : action.replacementModifiers();
      assertThat(result.events().getLast().modifierId()).isIn(targets);
      assertThat(result.events().getLast().selectionProbability()).isEqualTo(1.0 / targets.size());
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
    }
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_ESSENCE_ICE,
            WorkbenchCurrency.PERFECT_ESSENCE_BODY,
            WorkbenchCurrency.ARTIFICER,
            WorkbenchCurrency.ESSENCE_HORROR,
            WorkbenchCurrency.RUNIC_ALLOY,
            WorkbenchCurrency.CATALYST_FLESH)) {
      var refused = service.apply(rare, action, Set.of(), new Random(77));
      assertThat(refused.applied()).as(action.name()).isFalse();
      assertThat(refused.state()).isEqualTo(rare);
      assertThat(refused.events()).isEmpty();
      assertThat(refused.consumedOmens()).isEmpty();
    }
    var alchemy = service.apply(normal(c, 82), WorkbenchCurrency.ALCHEMY, Set.of(), new Random(77));
    var fractured =
        service.apply(alchemy.state(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(77));
    assertThat(fractured.applied()).isTrue();
    var locked =
        fractured.state().explicits().stream()
            .filter(ModifierInstance::fractured)
            .findFirst()
            .orElseThrow();
    var chaos = service.apply(fractured.state(), WorkbenchCurrency.CHAOS, Set.of(), new Random(77));
    assertThat(chaos.applied()).isTrue();
    assertThat(chaos.state().explicits()).contains(locked);
  }
}
