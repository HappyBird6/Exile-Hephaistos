package com.poe2craft.bootstrap.http;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.recommendation.api.SupportRecommendationController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * 동시 계산량을 제한하고 허용량을 넘긴 작업은 대기열에 쌓지 않고 거절한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code interceptor()}: interceptor 값을 조회·구성한다. 반환: {@code HandlerInterceptor}.
 *   <li>{@code refusesExcessWorkAndReleasesPermitOnFailureWhileAllowingCancellation()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code disabledSupportIsRejectedByServiceOwnershipBeforeExecutingTheHandler()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Registry}: {@code WorkloadAdmissionTest} 내부의 등록목록 처리를 맡는다.
 * </ul>
 */
class WorkloadAdmissionTest {
  /**
   * {@code WorkloadAdmissionTest} 내부의 등록목록 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code interceptor()}: interceptor 값을 조회·구성한다. 반환: {@code Object}.
   * </ul>
   */
  static class Registry extends InterceptorRegistry {
    Object interceptor() {
      return getInterceptors().getFirst();
    }
  }

  HandlerInterceptor interceptor() {
    var settings = mock(BackendSettings.class);
    var defaults = BackendSettings.defaults().workbench();
    when(settings.workbench())
        .thenReturn(
            new BackendSettings.Workbench(
                1, defaults.repeatTimeBudget(), defaults.repeatReceiptTtl(), 1, 1, 2, false));
    var registry = new Registry();
    new WorkloadAdmission(settings, new ObjectMapper()).addInterceptors(registry);
    return ((org.springframework.web.servlet.handler.MappedInterceptor) registry.interceptor())
        .getInterceptor();
  }

  @Test
  void refusesExcessWorkAndReleasesPermitOnFailureWhileAllowingCancellation() throws Exception {
    var gate = interceptor();
    var first = new MockHttpServletRequest("POST", "/api/v1/crafting/workbench/apply");
    assertThat(gate.preHandle(first, new MockHttpServletResponse(), new Object())).isTrue();
    var second = new MockHttpServletRequest("POST", "/api/v1/crafting/workbench/apply");
    var response = new MockHttpServletResponse();
    assertThat(gate.preHandle(second, response, new Object())).isFalse();
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    assertThat(
            gate.preHandle(
                new MockHttpServletRequest(
                    "POST", "/api/v1/crafting/workbench/chaos-repeat/id/cancel"),
                new MockHttpServletResponse(),
                new HandlerMethod(
                    mock(com.poe2craft.workbench.api.ChaosRepeatController.class),
                    com.poe2craft.workbench.api.ChaosRepeatController.class.getMethod(
                        "cancel", String.class))))
        .isTrue();
    // User-controlled matrix text must not grant the repeat-controller admission exemption.
    assertThat(
            gate.preHandle(
                new MockHttpServletRequest(
                    "POST", "/api/v1/crafting/workbench/apply;x=/workbench/chaos-repeat"),
                new MockHttpServletResponse(),
                new Object()))
        .isFalse();
    gate.afterCompletion(
        first, new MockHttpServletResponse(), new Object(), new RuntimeException());
    assertThat(gate.preHandle(second, new MockHttpServletResponse(), new Object())).isTrue();
  }

  @Test
  void disabledSupportIsRejectedByServiceOwnershipBeforeExecutingTheHandler() throws Exception {
    var method =
        java.util.Arrays.stream(SupportRecommendationController.class.getMethods())
            .filter(m -> m.getName().equals("recommend"))
            .findFirst()
            .orElseThrow();
    var response = new MockHttpServletResponse();
    assertThat(
            interceptor()
                .preHandle(
                    new MockHttpServletRequest(),
                    response,
                    new HandlerMethod(mock(SupportRecommendationController.class), method)))
        .isFalse();
    assertThat(response.getStatus()).isEqualTo(404);
  }
}
