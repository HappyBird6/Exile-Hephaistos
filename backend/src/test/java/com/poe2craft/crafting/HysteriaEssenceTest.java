package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class HysteriaEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_HYSTERIA;
  final String target = "amulet:suffix:of-suturing";

  ItemState rare(int level, String locked, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.RARE,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()), id.equals(locked));
                })
            .toList(),
        root.conditions());
  }

  @Test
  void removesOnlyUnlockedInstancesAndPreservesEveryOtherRollWithExactEvidence() {
    var definition = catalog.find(target).orElseThrow();
    assertThat(definition.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
    assertThat(definition.stats().getFirst().min()).isEqualTo(19);
    assertThat(definition.stats().getFirst().max()).isEqualTo(21);
    for (var before :
        List.of(
            rare(68, "", "amulet:prefix:hale", "amulet:suffix:of-the-brute"),
            rare(
                82,
                "amulet:prefix:hale",
                "amulet:prefix:hale",
                "amulet:prefix:adept-s",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin"),
            rare(
                82,
                "amulet:prefix:hale",
                "amulet:prefix:hale",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander"))) {
      var eligible = before.explicits().stream().filter(m -> !m.fractured()).toList();
      var reached = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result =
            simulator.apply(
                before,
                action,
                Set.of(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).hasSize(before.explicits().size());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.events()).hasSize(2);
        var removed = result.events().getFirst();
        assertThat(removed.kind()).isEqualTo("REMOVE");
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / eligible.size());
        assertThat(eligible.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        var added = result.events().getLast();
        assertThat(added.kind()).isEqualTo("ADD");
        assertThat(added.modifierId()).isEqualTo(target);
        assertThat(added.selectionProbability()).isEqualTo(1);
        assertThat(result.assumptions().getFirst().id()).isEqualTo("uniform-removal-v1");
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                eligible.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.assumptions().getLast().n()).isEqualTo(3);
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(result.remainingOmens())
            .containsExactlyInAnyOrder(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id());
        reached.add(removed.modifierId());
      }
      assertThat(reached)
          .containsExactlyInAnyOrderElementsOf(
              eligible.stream().map(ModifierInstance::modifierId).toList());
    }
  }

  @Test
  void blocksAnyUnverifiedRemovalBranchWithoutFilteringOrMutatingTheItem() {
    var magic =
        simulator.apply(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1)).state();
    for (var before :
        List.of(
            root,
            magic,
            rare(82, ""),
            rare(67, "", "amulet:prefix:hale"),
            rare(82, target, target),
            rare(
                82,
                "",
                "amulet:prefix:hale",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander"),
            rare(82, "", "amulet:prefix:hale", target))) {
      var result = simulator.apply(before, action, Set.of(), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.events()).isEmpty();
      assertThat(result.assumptions()).isEmpty();
    }
  }

  @Test
  void canReplaceTheSoleUnlockedRecoupInstanceWithoutCreatingADuplicateFamily() {
    var result = simulator.apply(rare(82, "", target), action, Set.of(), new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).hasSize(1);
    assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
    assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
    assertThat(result.events().getLast().modifierId()).isEqualTo(target);
  }
}
