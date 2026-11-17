package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.StockyCraftingFixture;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.text.ItemTextService;
import org.junit.jupiter.api.Test;

/**
 * 텍스트·편집·장착홈 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code retainsExactInstalledIdentityAndRejectsRemovingOrChangingProtectedLines()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class TextEditSocketTest {
  @Test
  void retainsExactInstalledIdentityAndRejectsRemovingOrChangingProtectedLines() {
    var f = new StockyCraftingFixture();
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
