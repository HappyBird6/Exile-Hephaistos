package com.poe2craft.shared.item;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 텍스트 매핑 결과의 실제 옵션과 예약·공개 훼손 슬롯을 함께 검증한다.
 *
 * <p>기존 계약: Clipboard mapping validates observed data and reserved/revealed slots without losing
 * either.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemTextStateValidation()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code validate(state, catalog)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code state}(상태), {@code
 *       catalog}(카탈로그). 반환: {@code List<String>}.
 * </ul>
 */
final class ItemTextStateValidation {
  private ItemTextStateValidation() {}

  static List<String> validate(ItemState state, ItemCatalog catalog) {
    var projection =
        !state.desecrated().isEmpty()
                && state.conditions().equals(Set.of(ItemState.Condition.CORRUPTED))
            ? state.withConditions(Set.of())
            : state;
    var issues =
        new ArrayList<>(
            new ItemStateValidator(catalog)
                .validate(projection).stream().map(ItemStateValidator.Violation::message).toList());
    if (issues.isEmpty() && !state.desecrated().isEmpty()) {
      try {
        // Unresolved augment observations remain readable and are retained on the mapped item.
        // Their validity is checked above; the slot engine only needs the concrete slot state.
        AbyssCraftingState.from(state.withObservations(null), catalog);
      } catch (IllegalArgumentException invalid) {
        issues.add("Invalid desecrated slot, reveal pool or item state.");
      }
    }
    return List.copyOf(issues);
  }
}
