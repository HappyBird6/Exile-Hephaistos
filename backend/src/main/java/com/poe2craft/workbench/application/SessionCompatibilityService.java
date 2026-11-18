package com.poe2craft.workbench.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import java.util.List;
import java.util.Optional;

/**
 * 새 체크포인트의 호환성을 검증한다. 저장 이력과 원래 규칙 영수증은 클라이언트가 보존한다.
 *
 * <p>기존 계약: Validates a new checkpoint. Saved films and original ruleset receipts remain
 * client-owned.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SessionCompatibilityService(crafting)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       crafting}(제작).
 *   <li>{@code catalog(baseItemId)}: 카탈로그 처리에서 {@code crafting.reviewedCatalogs}, {@code
 *       c.base}을(를) 사용한다. 입력: {@code baseItemId}(베이스·아이템·식별자). 반환: {@code Optional<ItemCatalog>}.
 *   <li>{@code check(state, abyss, previousKey)}: check 처리에서 {@code crafting.abyssCatalog}, {@code
 *       abyss.storedItem}을(를) 사용한다. 입력: {@code state}(상태), {@code abyss}(훼손), {@code
 *       previousKey}(이전·키). 반환: {@code Compatibility}.
 *   <li>{@code rejected(reason)}: 거절된의 입력에 따른 분기·결과를 처리한다. 입력: {@code reason}(이유). 반환: {@code
 *       Compatibility}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Compatibility}: {@code SessionCompatibilityService}에서 사용하는 호환성의 불변 값이다.
 * </ul>
 */
public final class SessionCompatibilityService {
  private final CraftingOperations crafting;

  public SessionCompatibilityService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  /**
   * {@code SessionCompatibilityService}에서 사용하는 호환성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code compatible}: 호환되는 ({@code boolean}).
   *   <li>{@code sameRules}: 동일·규칙목록 ({@code boolean}).
   *   <li>{@code compatibilityId}: 호환성·식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code ItemState}).
   *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code AbyssCraftingState}).
   *   <li>{@code reasons}: 이유목록 ({@code List<String>}).
   * </ul>
   */
  public record Compatibility(
      boolean compatible,
      boolean sameRules,
      String compatibilityId,
      ItemState state,
      AbyssCraftingState abyss,
      List<String> reasons) {}

  public Optional<ItemCatalog> catalog(String baseItemId) {
    return crafting.reviewedCatalogs().stream()
        .filter(c -> c.base().id().equals(baseItemId))
        .findFirst();
  }

  public Compatibility check(ItemState state, AbyssCraftingState abyss, String previousKey) {
    var catalog = crafting.abyssCatalog(state);
    if (abyss != null && !abyss.storedItem().equals(state)) return rejected("INCONSISTENT_ABYSS");
    if (abyss == null && !state.desecrated().isEmpty())
      abyss = AbyssCraftingState.from(state, catalog);
    if (abyss != null) abyss.validate(catalog);
    else {
      var violations = new ItemStateValidator(catalog).validate(state);
      if (!violations.isEmpty())
        return new Compatibility(
            false,
            false,
            null,
            null,
            null,
            violations.stream().map(v -> v.code().toString()).distinct().toList());
    }
    var key = crafting.simulator(state).compatibilityId();
    return new Compatibility(true, key.equals(previousKey), key, state, abyss, List.of());
  }

  public static Compatibility rejected(String reason) {
    return new Compatibility(false, false, null, null, null, List.of(reason));
  }

  /** 저장 상태의 스냅샷을 현재 카탈로그에 맞춰 재검증하고 이어하기 가능 여부를 반환한다. */
  public Compatibility check(
      JsonNode input, JsonNode abyssInput, String previousKey, WorkbenchStateReader reader) {

    if (input == null || !input.isObject()) return rejected("INVALID_STATE");
    var catalog = catalog(input.path("baseItemId").asText()).orElse(null);
    if (catalog == null) return rejected("UNSUPPORTED_BASE");
    var rebased = (ObjectNode) input.deepCopy();
    rebased.put("snapshotId", catalog.metadata().snapshotId());
    try {
      reader.concreteState(input, true);
      var state = reader.concreteState(rebased, true);
      AbyssCraftingState abyss = null;
      if (abyssInput != null && !abyssInput.isNull()) {
        if (!abyssInput.path("item").path("snapshotId").equals(input.path("snapshotId")))
          return rejected("INCONSISTENT_ABYSS");
        var savedAbyss = abyssInput.deepCopy();
        if (!(savedAbyss.path("item") instanceof ObjectNode item)) return rejected("INVALID_STATE");
        item.put("snapshotId", catalog.metadata().snapshotId());
        abyss = reader.abyssState(savedAbyss);
      }
      return check(state, abyss, previousKey);
    } catch (IllegalArgumentException invalid) {
      return rejected("INVALID_STATE");
    }
  }
}
