package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 제작 목표의 현재 달성 여부와 지원 범위를 평가한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code goals}: 목표목록 ({@code SupportGoals}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SupportAssessmentController(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그).
 *   <li>{@code families()}: 옵션계열목록 값을 조회·구성한다. 반환: {@code List<SupportGoals.Family>}.
 *   <li>{@code assess(request)}: assess 처리에서 {@code goals.assess}, {@code request.state}을(를) 사용한다.
 *       입력: {@code request}(요청). 반환: {@code SupportGoals.Assessment}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Request}: {@code SupportAssessmentController}에서 사용하는 요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportAssessmentController {
  private final SupportGoals goals;

  public SupportAssessmentController(ItemCatalog catalog) {
    goals = new SupportGoals(catalog);
  }

  @GetMapping("/families")
  public List<SupportGoals.Family> families() {
    return goals.families();
  }

  @PostMapping("/assess")
  public SupportGoals.Assessment assess(@RequestBody Request request) {
    return goals.assess(request.state(), request.goal());
  }

  /**
   * {@code SupportAssessmentController}에서 사용하는 요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code goal}: 목표 ({@code SupportGoals.Goal}).
   * </ul>
   */
  public record Request(StateBucket state, SupportGoals.Goal goal) {}
}
