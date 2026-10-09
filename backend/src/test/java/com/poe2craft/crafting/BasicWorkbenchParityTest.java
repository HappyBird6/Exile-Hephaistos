package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Captured against 891999d before sharing the no-omen basic currency plan. */
class BasicWorkbenchParityTest {
  @Test
  void seededBasicResultsKeepStatesEventsAssumptionsAndRandomDrawOrder() throws Exception {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var normal = SolarAmulet.initial(catalog);
    var filled =
        simulator.apply(normal, WorkbenchCurrency.ALCHEMY, Set.of(), new Random(11)).state();
    var magic = simulator.apply(normal, CraftingAction.TRANSMUTATION, new Random(17)).state();
    var inputs = List.of(normal, magic, filled);
    var text = new StringBuilder();
    for (var input : inputs) {
      for (var action : WorkbenchCurrency.values()) {
        if (action.baseAction() == null) continue;
        for (int seed = 0; seed < 12; seed++) {
          var result = simulator.apply(input, action, Set.of(), new Random(seed));
          text.append(action)
              .append('|')
              .append(result.applied())
              .append('|')
              .append(result.reason())
              .append('|')
              .append(result.state())
              .append('|');
          for (var event : result.events()) {
            text.append(event.kind())
                .append(':')
                .append(event.modifierId())
                .append(':')
                .append(new TreeMap<>(event.values()))
                .append(':')
                .append(event.selectionProbability())
                .append('|');
          }
          text.append(result.assumptions()).append('\n');
        }
      }
    }
    String digest =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(text.toString().getBytes(StandardCharsets.UTF_8)));
    assertThat(digest)
        .isEqualTo("616d3ea4f16d4f44627d59389da2a221eea28bdf1d9cf809678bde5b24a3b776");
  }
}
