package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewedBootsTest {
  @ParameterizedTest
  @ValueSource(
      strings = {"tasalian", "drakeskin", "sekhema", "blacksteel-boots", "faithful", "daggerfoot"})
  void completeBootsPoolsPreserveMovementRangesFamiliesAndAtomicEssenceRules(String key)
      throws Exception {
    var c = ItemCatalogLoader.loadTopBase(key);
    assertThat(c.base().id()).isEqualTo(ReviewedBoots.BASES.get(key));
    assertThat(c.base().hasImplicit()).isFalse();
    var movement =
        c.modifiers().values().stream()
            .filter(d -> d.familyIds().contains("MovementVelocity"))
            .sorted(Comparator.comparingInt(ModifierDefinition::requiredItemLevel))
            .toList();
    assertThat(movement).hasSize(6);
    assertThat(movement)
        .extracting(ModifierDefinition::requiredItemLevel)
        .containsExactly(1, 16, 33, 46, 65, 82);
    assertThat(movement)
        .extracting(d -> d.stats().getFirst().min())
        .containsExactly(10L, 15L, 20L, 25L, 30L, 35L);
    assertThat(movement).allMatch(d -> d.stats().getFirst().min() == d.stats().getFirst().max());
    var mapper = new ObjectMapper();
    var data =
        mapper.readTree(getClass().getResourceAsStream("/catalog/top-base-essences.json")).get(key);
    var fixed = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    var replacements = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    for (var pair : List.of(Map.entry("fixed", fixed), Map.entry("replacements", replacements)))
      data.get(pair.getKey())
          .fields()
          .forEachRemaining(
              e -> {
                var ids = new ArrayList<String>();
                e.getValue().forEach(v -> ids.add(v.asText()));
                pair.getValue().put(WorkbenchCurrency.valueOf(e.getKey()), ids);
              });
    var simulator =
        new WorkbenchSimulator(
            c,
            new CraftingEngine(c),
            c.modifiers().values().stream()
                .filter(d -> d.stats().size() > 1)
                .map(ModifierDefinition::id)
                .collect(Collectors.toSet()),
            fixed,
            replacements);
    var root =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(QualityLimitRules.describe(root, c).maximumQuality()).isEqualTo(20);
    var movementInstance =
        new ModifierInstance(
            movement.getFirst().id(), Map.of(movement.getFirst().stats().getFirst().id(), 10L));
    var magic =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.MAGIC,
            List.of(),
            List.of(movementInstance),
            Set.of());
    assertThat(simulator.pool(magic, WorkbenchCurrency.AUGMENTATION, null))
        .noneMatch(d -> d.familyIds().contains("MovementVelocity"));
    var life =
        c.modifiers().values().stream()
            .filter(d -> d.familyIds().contains("IncreasedLife") && d.requiredItemLevel() == 1)
            .findFirst()
            .orElseThrow();
    var lifeInstance =
        new ModifierInstance(
            life.id(), Map.of(life.stats().getFirst().id(), life.stats().getFirst().min()));
    var fire =
        c.modifiers().values().stream()
            .filter(d -> d.familyIds().contains("FireResistance") && d.requiredItemLevel() == 1)
            .findFirst()
            .orElseThrow();
    var fireInstance =
        new ModifierInstance(
            fire.id(), Map.of(fire.stats().getFirst().id(), fire.stats().getFirst().min()));
    var rare =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(lifeInstance, fireInstance),
            Set.of());
    var result =
        simulator.apply(
            rare,
            WorkbenchCurrency.ESSENCE_HYSTERIA,
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
            new Random(7));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).contains(fireInstance);
    assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
    assertThat(replacements.get(WorkbenchCurrency.ESSENCE_HYSTERIA))
        .containsExactly(movement.get(4).id());
    var conflicting =
        Set.of(
            WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
            WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id());
    var refused =
        simulator.apply(rare, WorkbenchCurrency.ESSENCE_ABYSS, conflicting, new Random(7));
    assertThat(refused.applied()).isFalse();
    assertThat(refused.state()).isEqualTo(rare);
    assertThat(refused.consumedOmens()).isEmpty();
    assertThat(
            simulator
                .apply(rare, WorkbenchCurrency.PERFECT_ESSENCE_HASTE, Set.of(), new Random(7))
                .applied())
        .isFalse();
  }
}
