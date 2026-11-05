package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemTextEditorTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final SolarTextMapper mapper = new SolarTextMapper(catalog);
  private final ItemTextService parser = new ItemTextService();

  private String text(String extra) {
    return "Item Class: Amulets\nRarity: Rare\nTest Item\nSolar Amulet\n--------\nItem Level: 82\n--------\n+15 to Spirit (implicit)\n--------\n"
        + extra;
  }

  @Test
  void editsRawRollsAndPreservesObservations() {
    var before =
        mapper
            .map(parser.parseText(text("+17 to maximum Life")))
            .state()
            .withObservations(new ItemObservations(null, List.of("Unresolved effect"), List.of()));
    var result =
        ItemTextEditor.merge(
            before, mapper.map(parser.parseText(text("+18 to maximum Life"))), catalog);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().observations()).isEqualTo(before.observations());
    assertThat(result.state().explicits()).isNotEqualTo(before.explicits());
  }

  @Test
  void unresolvedTextDoesNotProduceAReplacement() {
    var before = mapper.map(parser.parseText(text("+17 to maximum Life"))).state();
    var result =
        ItemTextEditor.merge(
            before, mapper.map(parser.parseText(text("Unknown future effect"))), catalog);
    assertThat(result.mapped()).isFalse();
    assertThat(result.state()).isNull();
    assertThat(before.explicits()).hasSize(1);
  }

  @Test
  void observedSocketsAreNotConvertedToCraftingCapacity() {
    var before = mapper.map(parser.parseText(text("+17 to maximum Life"))).state();
    var mapped =
        mapper.map(
            parser.parseText(
                text("+18 to maximum Life")
                    .replace("Item Level: 82", "Sockets: S S\nItem Level: 82")));
    assertThat(ItemTextEditor.merge(before, mapped, catalog).mapped()).isFalse();
  }
}
