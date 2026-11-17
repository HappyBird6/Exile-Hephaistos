package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 편집한 수치 원문을 적용하면서 복사 텍스트에 없는 기존 상태를 보존한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code SolarTextMapper}).
 *   <li>{@code parser}: 파서 ({@code ItemTextService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code text(extra)}: 텍스트의 입력에 따른 분기·결과를 처리한다. 입력: {@code extra}(extra). 반환: {@code String}.
 *   <li>{@code editsRawRollsAndPreservesObservations()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unresolvedTextDoesNotProduceAReplacement()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code observedSocketsAreNotConvertedToCraftingCapacity()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
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
