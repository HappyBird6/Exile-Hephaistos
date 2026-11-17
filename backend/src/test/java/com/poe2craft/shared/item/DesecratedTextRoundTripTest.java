package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.item.text.ItemTextService;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 훼손·텍스트·반올림·Trip 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code SolarTextMapper}).
 *   <li>{@code parser}: 파서 ({@code ItemTextService}).
 *   <li>{@code header}: 헤더 ({@code String}).
 *   <li>{@code revealed}: revealed ({@code String}).
 *   <li>{@code hidden}: 미공개; 초기값 {@code "Desecrated Prefix (Minimum Modifier Level: 40)"} ({@code
 *       String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code mapsExclusiveRevealAndHiddenSlotWithConstraintsAndCorruption()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code levelEditRetainsOriginalSlotIdentityAndConstraints()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code retainsAllSupportedJewelleryPoolsOnUnrevealedSlots()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code blocksInvalidPoolsLevelsAndExclusiveEffectsWithoutDesecration()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class DesecratedTextRoundTripTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadTopBase("soldier");
  private final SolarTextMapper mapper = new SolarTextMapper(catalog);
  private final ItemTextService parser = new ItemTextService();
  private final String header =
      "Item Class: Body Armours\nRarity: Rare\nWorkbench Item\nSoldier Cuirass\n--------\nItem Level: 82\n--------\n";
  private final String revealed =
      "{ Desecrated Suffix Modifier \"of Ulaman\" (Tier: 0) (Minimum Modifier Level: 40) }\n+15% to Lightning and Chaos Resistances (desecrated)";
  private final String hidden = "Desecrated Prefix (Minimum Modifier Level: 40)";

  @Test
  void mapsExclusiveRevealAndHiddenSlotWithConstraintsAndCorruption() {
    var source = header + revealed + "\n--------\n" + hidden + "\n--------\nCorrupted";
    var result = mapper.map(parser.parseText(source));
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().conditions()).isEqualTo(Set.of(ItemState.Condition.CORRUPTED));
    assertThat(result.state().explicits()).hasSize(1);
    assertThat(result.state().explicits().getFirst().modifierId()).startsWith("reveal:");
    assertThat(result.state().desecrated()).hasSize(2);
    var known =
        result.state().desecrated().stream()
            .filter(s -> s.modifierId() != null)
            .findFirst()
            .orElseThrow();
    assertThat(known.revealPool()).isNull();
    assertThat(known.minimumModifierLevel()).isEqualTo(40);
    var unknown =
        result.state().desecrated().stream()
            .filter(s -> s.modifierId() == null)
            .findFirst()
            .orElseThrow();
    assertThat(unknown.minimumModifierLevel()).isEqualTo(40);
    assertThat(unknown.revealPool()).isNull();
    assertThat(parser.parseText(source).text().originalText()).isEqualTo(source);
  }

  @Test
  void levelEditRetainsOriginalSlotIdentityAndConstraints() {
    var source = header + revealed + "\n--------\n" + hidden + "\nCorrupted";
    var before = mapper.map(parser.parseText(source)).state();
    assertThat(before).isNotNull();
    var mapped = mapper.map(parser.parseText(source.replace("Item Level: 82", "Item Level: 81")));
    var result = ItemTextEditor.merge(before, mapped, catalog);
    assertThat(result.issues()).isEmpty();
    assertThat(result.state().itemLevel()).isEqualTo(81);
    assertThat(result.state().desecrated()).isEqualTo(before.desecrated());
    assertThat(result.state().conditions()).isEqualTo(before.conditions());
  }

  @Test
  void retainsAllSupportedJewelleryPoolsOnUnrevealedSlots() {
    var solar = new SolarTextMapper(ItemCatalogLoader.loadDefault());
    for (var pool : java.util.List.of("ALTERED", "AMANAMU", "KURGAL", "ULAMAN")) {
      var source =
          "Item Class: Amulets\nRarity: Rare\nWorkbench Item\nSolar Amulet\n--------\nItem Level: 82\n--------\n+15 to Spirit (implicit)\n--------\nDesecrated Prefix (Minimum Modifier Level: 40) (Reveal Pool: "
              + pool
              + ")";
      var result = solar.map(parser.parseText(source));
      assertThat(result.issues()).as(pool).isEmpty();
      assertThat(result.state().desecrated())
          .singleElement()
          .satisfies(
              slot -> {
                assertThat(slot.modifierId()).isNull();
                assertThat(slot.minimumModifierLevel()).isEqualTo(40);
                assertThat(slot.revealPool()).isEqualTo(pool);
              });
    }
  }

  @Test
  void blocksInvalidPoolsLevelsAndExclusiveEffectsWithoutDesecration() {
    for (var source :
        java.util.List.of(
            header + revealed.replace("(Minimum Modifier Level: 40)", "(Reveal Pool: AMANAMU)"),
            header + revealed.replace("(Minimum Modifier Level: 40)", "(Reveal Pool: UNKNOWN)"),
            header
                + revealed.replace(
                    "(Minimum Modifier Level: 40)",
                    "(Minimum Modifier Level: 40) (Minimum Modifier Level: 40)"),
            header + hidden.replace("40", "39"),
            header + hidden + " (Reveal Pool: UNKNOWN)",
            header + revealed.replace("Desecrated Suffix", "Suffix").replace(" (desecrated)", ""),
            header + "Corrupted")) {
      var result = mapper.map(parser.parseText(source));
      assertThat(result.mapped()).as(source).isFalse();
      assertThat(result.state()).isNull();
      assertThat(result.issues()).isNotEmpty();
    }
  }
}
