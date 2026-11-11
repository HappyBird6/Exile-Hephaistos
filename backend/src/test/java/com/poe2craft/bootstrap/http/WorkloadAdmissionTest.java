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

class WorkloadAdmissionTest {
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
                1, defaults.repeatTimeBudget(), defaults.repeatReceiptTtl(), 1, 1, false));
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
