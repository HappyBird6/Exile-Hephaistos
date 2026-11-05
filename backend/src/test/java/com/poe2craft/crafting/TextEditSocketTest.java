package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.*;
import com.poe2craft.item.testparser.ItemTextService;
import org.junit.jupiter.api.Test;

class TextEditSocketTest {
  @Test
  void retainsExactInstalledIdentityAndRejectsRemovingOrChangingProtectedLines() {
    var f = new StockyHysteriaTest();
    var before =
        SocketAugments.install(
            new ItemState(
                f.root.snapshotId(),
                f.root.baseItemId(),
                82,
                ItemState.Rarity.NORMAL,
                f.root.implicits(),
                java.util.List.of(),
                java.util.Set.of(),
                2),
            0,
            "Perfect_Desert_Rune");
    var text =
        "Item Class: Gloves\nRarity: Normal\nStocky Mitts\n--------\nItem Level: 81\nSockets: S S\nRune: Slot 1: Perfect Desert Rune";
    var parser = new ItemTextService();
    var mapper = new SolarTextMapper(f.catalog);
    var result = ItemTextEditor.merge(before, mapper.map(parser.parseText(text)), f.catalog);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().installedAugments()).isEqualTo(before.installedAugments());
    assertThat(result.state().augmentSockets()).isEqualTo(2);
    assertThat(result.state().itemLevel()).isEqualTo(81);
    assertThat(
            ItemTextEditor.merge(
                    before,
                    mapper.map(
                        parser.parseText(
                            text.replace("Perfect Desert Rune", "Perfect Storm Rune"))),
                    f.catalog)
                .mapped())
        .isFalse();
    assertThat(
            ItemTextEditor.merge(
                    before,
                    mapper.map(
                        parser.parseText(
                            text.replace("Sockets: S S\nRune: Slot 1: Perfect Desert Rune", ""))),
                    f.catalog)
                .mapped())
        .isFalse();
  }
}
