package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * No·암시옵션·베이스 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code solar}: 태양목걸이 ({@code ItemCatalog}).
 *   <li>{@code definitions}: 정의목록 ({@code List<ModifierDefinition>}).
 *   <li>{@code base}: 베이스 ({@code ItemCatalog.BaseItem}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code fixture(defs)}: 검증사례 처리에서 {@code ItemCatalog.Metadata}, {@code base.sourceUrl}을(를)
 *       사용한다. 입력: {@code defs}(defs). 반환: {@code ItemCatalog}.
 *   <li>{@code state(implicits)}: 현재 상태 표현을 조회한다. 입력: {@code implicits}(암시옵션목록). 반환: {@code
 *       ItemState}.
 *   <li>{@code sourcedNoImplicitBaseAcceptsAnEmptyImplicitListAndRejectsInventedSpirit()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code noImplicitCatalogRejectsOrphanImplicitDefinitionsAndWhitespaceIdentity()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code solarStillRequiresExactlyItsOriginalSpiritAndRejectsTheOtherBase()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class NoImplicitBaseTest {
  final ItemCatalog solar = ItemCatalogLoader.loadDefault();
  // Contract fixture only: this is not a complete Stocky Mitts modifier pool and is never
  // activated.
  final List<ModifierDefinition> definitions =
      List.of(
          solar.find("amulet:prefix:hale").orElseThrow(),
          solar.find("amulet:suffix:of-the-brute").orElseThrow());
  final ItemCatalog.BaseItem base =
      new ItemCatalog.BaseItem(
          "Metadata/Items/Armours/Gloves/FourGlovesStr1",
          "Stocky Mitts",
          "https://poe2db.tw/us/Stocky_Mitts",
          "",
          1,
          1,
          3,
          3);

  ItemCatalog fixture(List<ModifierDefinition> defs) {
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            "no-implicit-contract-fixture",
            "2026-10-02",
            base.sourceUrl(),
            "CONTRACT_FIXTURE_NOT_GAME_POOL",
            "a".repeat(64),
            "b".repeat(64),
            1,
            1,
            1000,
            1000),
        base,
        defs);
  }

  ItemState state(List<ModifierInstance> implicits) {
    return new ItemState(
        "no-implicit-contract-fixture",
        base.id(),
        82,
        ItemState.Rarity.NORMAL,
        implicits,
        List.of(),
        Set.of());
  }

  @Test
  void sourcedNoImplicitBaseAcceptsAnEmptyImplicitListAndRejectsInventedSpirit() {
    var validator = new ItemStateValidator(fixture(definitions));
    assertThat(base.hasImplicit()).isFalse();
    assertThat(validator.validate(state(List.of()))).isEmpty();
    assertThat(validator.validate(state(SolarAmulet.initial(solar).implicits())))
        .extracting(ItemStateValidator.Violation::code)
        .contains(
            ItemStateValidator.Code.INVALID_IMPLICIT, ItemStateValidator.Code.UNKNOWN_MODIFIER);
  }

  @Test
  void noImplicitCatalogRejectsOrphanImplicitDefinitionsAndWhitespaceIdentity() {
    var orphan = new ArrayList<>(definitions);
    orphan.add(solar.find(SolarAmulet.IMPLICIT_ID).orElseThrow());
    assertThatThrownBy(() -> fixture(orphan)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ItemCatalog.BaseItem(base.id(), base.name(), base.sourceUrl(), " ", 1, 1, 3, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ItemCatalog.BaseItem(
                    base.id(), base.name(), base.sourceUrl(), null, 1, 1, 3, 3))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void solarStillRequiresExactlyItsOriginalSpiritAndRejectsTheOtherBase() {
    var root = SolarAmulet.initial(solar);
    var validator = new ItemStateValidator(solar);
    assertThat(solar.base().hasImplicit()).isTrue();
    assertThat(validator.validate(root)).isEmpty();
    var missing =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            List.of(),
            List.of(),
            Set.of());
    assertThat(validator.validate(missing))
        .extracting(ItemStateValidator.Violation::code)
        .containsExactly(ItemStateValidator.Code.INVALID_IMPLICIT);
    assertThat(validator.validate(state(List.of())))
        .extracting(ItemStateValidator.Violation::code)
        .contains(
            ItemStateValidator.Code.UNSUPPORTED_BASE,
            ItemStateValidator.Code.SNAPSHOT_MISMATCH,
            ItemStateValidator.Code.INVALID_IMPLICIT);
  }
}
