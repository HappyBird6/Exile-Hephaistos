package com.poe2craft.shared.item;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 기존 태양 목걸이 카탈로그와 초기 아이템을 제공하는 호환 진입점이다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Amulets/FourAmulet9"} ({@code String}).
 *   <li>{@code IMPLICIT_ID}: 식별자; 초기값 {@code "solar-amulet:implicit:spirit"} ({@code String}).
 *   <li>{@code SPIRIT_STAT}: 수치; 초기값 {@code "base_spirit_from_equipment"} ({@code String}).
 *   <li>{@code DEFAULT_ITEM_LEVEL}: 레벨; 초기값 {@code 82} ({@code int}).
 *   <li>{@code DEFAULT_SPIRIT}: 정신력; 초기값 {@code 15} ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SolarAmulet()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code initial(catalog)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code catalog}(카탈로그). 반환: {@code
 *       ItemState}.
 *   <li>{@code initial(catalog, itemLevel, spirit)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code catalog}(카탈로그),
 *       {@code itemLevel}(아이템·레벨), {@code spirit}(정신력). 반환: {@code ItemState}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 */
public final class SolarAmulet {
  public static final String BASE_ID = "Metadata/Items/Amulets/FourAmulet9";
  public static final String IMPLICIT_ID = "solar-amulet:implicit:spirit";
  public static final String SPIRIT_STAT = "base_spirit_from_equipment";
  public static final int DEFAULT_ITEM_LEVEL = 82;
  public static final int DEFAULT_SPIRIT = 15;

  private SolarAmulet() {}

  /** The fixed initial Spirit roll is a preset choice, not a randomly rolled result. */
  public static ItemState initial(ItemCatalog catalog) {
    return initial(catalog, DEFAULT_ITEM_LEVEL, DEFAULT_SPIRIT);
  }

  public static ItemState initial(ItemCatalog catalog, int itemLevel, int spirit) {
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            BASE_ID,
            itemLevel,
            ItemState.Rarity.NORMAL,
            List.of(new ModifierInstance(IMPLICIT_ID, Map.of(SPIRIT_STAT, (long) spirit))),
            List.of(),
            Set.of());
    var errors = new ItemStateValidator(catalog).validate(state);
    if (!errors.isEmpty())
      throw new IllegalArgumentException("Invalid Solar Amulet preset: " + errors);
    return state;
  }
}
