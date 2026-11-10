package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.*;
import org.junit.jupiter.api.Test;

class CurrentItemImportTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final ItemTextService parser = new ItemTextService();

  private String item(String body) {
    return "Item Class: Amulets\nRarity: Rare\nImported Item\nSolar Amulet\n--------\nItem Level: 100\n--------\n+15 to Spirit (implicit)\n--------\n"
        + body;
  }

  private SolarTextMapper.Result map(String source) {
    return new SolarTextMapper(catalog).map(parser.parseText(source));
  }

  @Test
  void ordinaryCatalystTextUsesItsKnownPercentageAndContinuesCrafting() {
    var result =
        map(
            item("{ Prefix Modifier \"Healthy\" (Tier: 8) }\n+26 to maximum Life")
                .replace(
                    "Item Level: 100",
                    "Item Level: 100\nQuality (Life Modifiers): +20% (augmented)"));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_maximum_life", 22L);
    var displayed =
        CatalystQualityDisplay.describe(result.state(), catalog).stream()
            .filter(p -> p.modifierId().equals("amulet:prefix:healthy"))
            .findFirst()
            .orElseThrow();
    assertThat(displayed.displayedValues()).containsEntry("base_maximum_life", 26L);
    var after =
        new WorkbenchSimulator(catalog, new CraftingEngine(catalog))
            .apply(result.state(), WorkbenchCurrency.ANNULMENT, Set.of(), new Random(2));
    assertThat(after.applied()).isTrue();
    assertThat(after.state().catalystQuality()).isEqualTo(result.state().catalystQuality());
  }

  @Test
  void roundedRegenerationAndCatalystScalingNeedNoExactOriginalRoll() {
    for (boolean quality : List.of(false, true)) {
      String source =
          item(
              "{ Suffix Modifier \"of Convalescence\" (Tier: "
                  + catalog.find("amulet:suffix:of-convalescence").orElseThrow().tier()
                  + ") }\n"
                  + (quality ? "15.8" : "13.2")
                  + " Life Regeneration per second");
      if (quality)
        source =
            source.replace("Item Level: 100", "Item Level: 100\nQuality (Life Modifiers): +20%");
      var result = map(source);
      assertThat(result.issues()).isEmpty();
      assertThat(result.state().explicits().getFirst().values())
          .containsEntry("base_life_regeneration_rate_per_minute", quality ? 790L : 792L);
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      if (quality)
        assertThat(
                CatalystQualityDisplay.describe(result.state(), catalog)
                    .getLast()
                    .displayedValues())
            .containsEntry("base_life_regeneration_rate_per_minute", 948L);
    }
    assertThat(
            map(item("+999999 to maximum Life")
                    .replace("Item Level: 100", "Item Level: 100\nQuality (Life Modifiers): +20%"))
                .mapped())
        .isFalse();
  }

  @Test
  void sanctifiedImportKeepsCurrentValuesOutsideOriginalBoundsAndIsPermanentlyLocked() {
    String source =
        item("{ Prefix Modifier \"Healthy\" (Tier: 8) }\n+35 to maximum Life\n--------\nSanctified")
            .replace("+15 to Spirit", "+18 to Spirit");
    var result = map(source);
    assertThat(result.issues()).isEmpty();
    var state = result.state();
    assertThat(state.observedSanctified()).isTrue();
    assertThat(state.sanctification()).isNull();
    assertThat(state.explicits().getFirst().values()).containsEntry("base_maximum_life", 35L);
    assertThat(state.implicits().getFirst().values())
        .containsEntry("base_spirit_from_equipment", 18L);
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    assertThat(simulator.actions(state, Set.of()))
        .allSatisfy(action -> assertThat(action.available()).isFalse());
    var unchanged = simulator.apply(state, WorkbenchCurrency.DIVINE, Set.of(), new Random(2));
    assertThat(unchanged.applied()).isFalse();
    assertThat(unchanged.state()).isEqualTo(state);
    assertThat(
            ItemTextEditor.merge(
                    state, map(source.replace("Item Level: 100", "Item Level: 99")), catalog)
                .mapped())
        .isFalse();
    assertThat(new ItemStateValidator(catalog).validate(state.withConditions(Set.of())))
        .isNotEmpty();
  }

  @Test
  void importedHiddenDesecrationDefaultsToTheOrdinaryPoolWithoutRequiringItsHistory() {
    var result = map(item("Desecrated Prefix"));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().desecrated())
        .singleElement()
        .satisfies(
            slot -> {
              assertThat(slot.modifierId()).isNull();
              assertThat(slot.minimumModifierLevel()).isNull();
              assertThat(slot.revealPool()).isNull();
            });
    var abyss = AbyssCraftingState.from(result.state(), catalog);
    assertThat(abyss.eligibleRevealIds(abyss.slots().getFirst().id(), catalog)).isNotEmpty();
  }

  @Test
  void aRevealedOtherworldlyEffectNeedsNoPrivateOriginMetadata() {
    var result =
        map(
            item(
                "{ Desecrated Prefix Modifier \"Tul's\" }\nGain 15% of Physical Damage as Extra Cold Damage (desecrated)"));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().explicits().getFirst().values()).containsValue(15L);
    assertThat(result.state().desecrated())
        .singleElement()
        .satisfies(
            slot -> {
              assertThat(slot.revealPool()).isEqualTo("ALTERED");
              assertThat(slot.minimumModifierLevel()).isNull();
            });
  }
}
