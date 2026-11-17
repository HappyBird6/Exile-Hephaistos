package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.workbench.application.CustomExaltService;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자가 옵션을 선택하는 엑잘티드 제작 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code CustomExaltService}).
 *   <li>{@code reader}: 읽기변환기 ({@code WorkbenchStateReader}).
 *   <li>{@code random}: 호출 순서대로 소비하는 난수 공급자 ({@code SecureRandom}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CustomExaltController(crafting, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       crafting}(제작), {@code json}(JSON).
 *   <li>{@code choices(request)}: 선택목록 처리에서 {@code service.choices}, {@code
 *       reader.concreteState}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code
 *       List<WorkbenchSimulator.ExaltChoice>}.
 *   <li>{@code apply(request)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code request}(요청). 반환: {@code
 *       CustomExaltService.Applied}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Request}: {@code CustomExaltController}에서 사용하는 요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/workbench/custom-exalt")
public final class CustomExaltController {
  private final CustomExaltService service;
  private final WorkbenchStateReader reader;
  private final SecureRandom random = new SecureRandom();

  public CustomExaltController(CraftingOperations crafting, ObjectMapper json) {
    service = new CustomExaltService(crafting);
    reader = new WorkbenchStateReader(crafting, json);
  }

  /**
   * {@code CustomExaltController}에서 사용하는 요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code JsonNode}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   * </ul>
   */
  public record Request(
      JsonNode state,
      JsonNode abyss,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      String modifierId) {}

  @PostMapping("/choices")
  public List<WorkbenchSimulator.ExaltChoice> choices(@RequestBody Request request) {
    return service.choices(
        reader.concreteState(request.state(), true),
        request.abyss() == null || request.abyss().isNull()
            ? null
            : reader.abyssState(request.abyss()),
        request.action(),
        request.activeOmens());
  }

  @PostMapping("/apply")
  public CustomExaltService.Applied apply(@RequestBody Request request) {
    return service.apply(
        reader.concreteState(request.state(), true),
        request.abyss() == null || request.abyss().isNull()
            ? null
            : reader.abyssState(request.abyss()),
        request.action(),
        request.activeOmens(),
        request.modifierId(),
        random);
  }
}
