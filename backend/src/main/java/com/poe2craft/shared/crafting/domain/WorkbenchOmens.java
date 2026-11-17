package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.*;
import java.util.*;

/**
 * 징조 ID 검증과 화폐 trigger 매칭을 일반·훼손 제작에서 공유한다. 상태와 캐시는 가지지 않는다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchOmens()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code parseOmens(ids)}: 징조 ID와 조합을 검증해 실행 정의로 바꾼다. 입력: {@code ids}(식별자목록). 반환: {@code
 *       List<WorkbenchOmen>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code matching(action, omens)}: 현재 행동의 발동 조건에 맞는 값을 찾는다. 입력: {@code action}(행동), {@code
 *       omens}(징조목록). 반환: {@code List<WorkbenchOmen>}.
 * </ul>
 */
final class WorkbenchOmens {
  private WorkbenchOmens() {}

  static List<WorkbenchOmen> parseOmens(Set<String> ids) {
    if (ids == null) return List.of();
    if (ids.size() > WorkbenchOmen.values().length)
      throw new IllegalArgumentException("Too many active omens");
    return ids.stream().sorted().map(WorkbenchOmen::fromId).toList();
  }

  static List<WorkbenchOmen> matching(WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    return omens.stream()
        .filter(
            o ->
                o.trigger() == action
                    || (o.trigger() == WorkbenchCurrency.ESSENCE_HYSTERIA
                        && !action.isLiquid()
                        && !action.replacementEssenceModifiers().isEmpty())
                    || (action.baseAction() != null
                        && o.trigger().baseAction() == action.baseAction()))
        .toList();
  }
}
