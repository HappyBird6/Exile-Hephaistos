package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import java.util.Objects;

/**
 * 구체 수치와 명시 슬롯 정체성을 함께 보존하는 현재 아이템 표현이다.
 *
 * <p>기존 계약: One current item with two views: concrete values and preserved explicit-slot
 * identities.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code item}: 아이템 ({@code ItemState}).
 *   <li>{@code slots}: 슬롯목록 ({@code AbyssCraftingState}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code resolve(item, slots, catalog)}: 해결 처리에서 {@code AbyssCraftingState.from}, {@code
 *       resolved.validate}을(를) 사용한다. 입력: {@code item}(아이템), {@code slots}(슬롯목록), {@code
 *       catalog}(카탈로그). 반환: {@code CraftingItemState}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public record CraftingItemState(ItemState item, AbyssCraftingState slots) {
  public static CraftingItemState resolve(
      ItemState item, AbyssCraftingState slots, ItemCatalog catalog) {
    Objects.requireNonNull(item);
    var resolved = slots == null ? AbyssCraftingState.from(item, catalog) : slots;
    resolved.validate(catalog);
    if (!resolved.storedItem().equals(item))
      throw new IllegalArgumentException(
          "Concrete item and explicit slots describe different states");
    return new CraftingItemState(item, resolved);
  }
}
