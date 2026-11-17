package com.poe2craft.workbench.api;

import static com.poe2craft.workbench.application.SessionCompatibilityService.rejected;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.workbench.application.ModifierReferenceService;
import com.poe2craft.workbench.application.SessionCompatibilityService;
import com.poe2craft.workbench.application.SessionCompatibilityService.Compatibility;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import org.springframework.web.bind.annotation.*;

/**
 * 작업대의 후보 옵션·세션 호환성 등 조회용 API를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code sessions}: 세션목록 ({@code SessionCompatibilityService}).
 *   <li>{@code references}: 참조목록 ({@code ModifierReferenceService}).
 *   <li>{@code reader}: 읽기변환기 ({@code WorkbenchStateReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchReferenceController(crafting, references, json)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code crafting}(제작), {@code references}(참조목록), {@code json}(JSON).
 *   <li>{@code pools(request)}: 후보풀목록 처리에서 {@code references.describe}, {@code
 *       reader.concreteState}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code
 *       ModifierReferenceService.Reference}.
 *   <li>{@code compatibility(request)}: 호환성 처리에서 {@code request.state}, {@code input.isObject}을(를)
 *       사용한다. 입력: {@code request}(요청). 반환: {@code Compatibility}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code PoolRequest}: {@code WorkbenchReferenceController}에서 사용하는 후보풀·요청의 불변 값이다.
 *   <li>{@code SessionRequest}: {@code WorkbenchReferenceController}에서 사용하는 세션·요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchReferenceController {
  private final SessionCompatibilityService sessions;
  private final ModifierReferenceService references;
  private final WorkbenchStateReader reader;

  public WorkbenchReferenceController(
      CraftingOperations crafting, ModifierReferenceService references, ObjectMapper json) {
    this.sessions = new SessionCompatibilityService(crafting);
    this.references = references;
    this.reader = new WorkbenchStateReader(crafting, json);
  }

  /**
   * {@code WorkbenchReferenceController}에서 사용하는 후보풀·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code dynamic}: dynamic ({@code boolean}).
   * </ul>
   */
  public record PoolRequest(JsonNode state, boolean dynamic) {}

  @PostMapping("/modifier-pools")
  public ModifierReferenceService.Reference pools(@RequestBody PoolRequest request) {
    return references.describe(reader.concreteState(request.state(), true), request.dynamic());
  }

  /**
   * {@code WorkbenchReferenceController}에서 사용하는 세션·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code JsonNode}).
   *   <li>{@code compatibilityId}: 호환성·식별자 ({@code String}).
   * </ul>
   */
  public record SessionRequest(JsonNode state, JsonNode abyss, String compatibilityId) {}

  /** Revalidates an exact checkpoint, never rewrites a saved film or changes rolls. */
  @PostMapping("/session-compatibility")
  public Compatibility compatibility(@RequestBody SessionRequest request) {
    var input = request.state();
    if (input == null || !input.isObject()) return rejected("INVALID_STATE");
    var catalog = sessions.catalog(input.path("baseItemId").asText()).orElse(null);
    if (catalog == null) return rejected("UNSUPPORTED_BASE");
    var rebased = (ObjectNode) input.deepCopy();
    rebased.put("snapshotId", catalog.metadata().snapshotId());
    try {
      reader.concreteState(input, true);
      var state = reader.concreteState(rebased, true);
      AbyssCraftingState abyss = null;
      if (request.abyss() != null && !request.abyss().isNull()) {
        if (!request.abyss().path("item").path("snapshotId").equals(input.path("snapshotId")))
          return rejected("INCONSISTENT_ABYSS");
        var savedAbyss = request.abyss().deepCopy();
        if (!(savedAbyss.path("item") instanceof ObjectNode item)) return rejected("INVALID_STATE");
        item.put("snapshotId", catalog.metadata().snapshotId());
        abyss = reader.abyssState(savedAbyss);
      }
      return sessions.check(state, abyss, request.compatibilityId());
    } catch (IllegalArgumentException invalid) {
      return rejected("INVALID_STATE");
    }
  }
}
