package com.poe2craft.craftingsupport.goal.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Context;
import com.poe2craft.craftingsupport.goal.domain.GoalValidator;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

/**
 * 목표 필터의 카탈로그 조회·검증·평가 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code GoalFilterService}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalFilterController(service, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code service}(서비스),
 *       {@code json}(JSON).
 *   <li>{@code catalog(snapshotId, baseItemId, itemLevel)}: 카탈로그 처리에서 {@code service.catalog}을(를)
 *       사용한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code
 *       itemLevel}(아이템·레벨). 반환: {@code GoalCatalog}.
 *   <li>{@code validate(body)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code body}(본문). 반환: {@code
 *       GoalValidator.Validation}.
 *   <li>{@code evaluate(body)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code body}(본문). 반환: {@code
 *       GoalEvaluator.Evaluation}.
 *   <li>{@code recommend(body)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code body}(본문). 반환: {@code
 *       GoalFilterService.Recommendation}.
 *   <li>{@code decode(body, type)}: 저장·전송 표현을 내부 값으로 복원한다. 입력: {@code body}(본문), {@code type}(타입).
 *       반환: {@code T}. 사용하는 예외: {@code MalformedPayload}, {@code GoalFilterService.InvalidGoal}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code MalformedPayload}: {@code GoalFilterController} 내부의 잘못된형식·저장내용 처리를 맡는다.
 *   <li>{@code ValidateRequest}: {@code GoalFilterController}에서 사용하는 Validate·요청의 불변 값이다.
 *   <li>{@code EvaluateRequest}: {@code GoalFilterController}에서 사용하는 평가·요청의 불변 값이다.
 *   <li>{@code RecommendRequest}: {@code GoalFilterController}에서 사용하는 추천·요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/support/goal-filters")
public final class GoalFilterController {
  private final GoalFilterService service;
  private final ObjectMapper json;

  public GoalFilterController(GoalFilterService service, ObjectMapper json) {
    this.service = service;
    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  @GetMapping("/catalog")
  public GoalCatalog catalog(
      @RequestParam String snapshotId,
      @RequestParam String baseItemId,
      @RequestParam Integer itemLevel) {
    return service.catalog(new Context(snapshotId, baseItemId, itemLevel));
  }

  @PostMapping("/validate")
  public GoalValidator.Validation validate(@RequestBody JsonNode body) {
    var request = decode(body, ValidateRequest.class);
    return service.validate(request.context(), request.goal());
  }

  @PostMapping("/evaluate")
  public GoalEvaluator.Evaluation evaluate(@RequestBody JsonNode body) {
    var request = decode(body, EvaluateRequest.class);
    return service.evaluate(request.item(), request.goal());
  }

  @PostMapping("/recommend")
  public GoalFilterService.Recommendation recommend(@RequestBody JsonNode body) {
    var request = decode(body, RecommendRequest.class);
    return service.recommend(
        request.item(), request.goal(), request.activeOmens(), request.limits());
  }

  private <T> T decode(JsonNode body, Class<T> type) {
    if (body == null || !body.isObject()) throw new MalformedPayload();
    try {
      return json.treeToValue(body, type);
    } catch (ValueInstantiationException error) {
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error(
                  "INVALID_ITEM", "/item", "ItemState properties are invalid.")));
    } catch (JsonProcessingException error) {
      throw new MalformedPayload();
    }
  }

  /**
   * {@code GoalFilterController} 내부의 잘못된형식·저장내용 처리를 맡는다.
   *
   * <p>직접 선언한 필드·생성자·메서드는 없다.
   */
  public static final class MalformedPayload extends IllegalArgumentException {}

  /**
   * {@code GoalFilterController}에서 사용하는 Validate·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code context}: 문맥 ({@code Context}).
   *   <li>{@code goal}: 목표 ({@code GoalFilter}).
   * </ul>
   */
  public record ValidateRequest(Context context, GoalFilter goal) {}

  /**
   * {@code GoalFilterController}에서 사용하는 평가·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code goal}: 목표 ({@code GoalFilter}).
   * </ul>
   */
  public record EvaluateRequest(ItemState item, GoalFilter goal) {}

  /**
   * {@code GoalFilterController}에서 사용하는 추천·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code goal}: 목표 ({@code GoalFilter}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code limits}: 한도목록 ({@code GoalFilterService.Limits}).
   * </ul>
   */
  public record RecommendRequest(
      ItemState item, GoalFilter goal, Set<String> activeOmens, GoalFilterService.Limits limits) {}
}
