package com.poe2craft.bootstrap.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 동시 계산량을 제한하고 허용량을 넘긴 작업은 대기열에 쌓지 않고 거절한다.
 *
 * <p>기존 계약: Reject excess work before body parsing/engine execution; never queue unbounded
 * computation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code ordinary}: 일반 ({@code Semaphore}).
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code PERMIT}: PERMIT ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkloadAdmission(settings, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code settings}(운영설정),
 *       {@code json}(JSON).
 *   <li>{@code addInterceptors(registry)}: Interceptors을(를) 추가한다. 입력: {@code registry}(등록목록).
 *   <li>{@code reject(response, status, code)}: 허용되지 않는 요청을 거절한다. 입력: {@code response}(응답), {@code
 *       status}(상태), {@code code}(코드). 사용하는 예외: {@code java.io.IOException}.
 * </ul>
 */
@Configuration
@Import(com.poe2craft.configuration.BackendConfiguration.class)
public class WorkloadAdmission implements WebMvcConfigurer {
  private final Semaphore ordinary;
  private final BackendSettings settings;
  private final ObjectMapper json;
  private static final String PERMIT = WorkloadAdmission.class.getName();

  public WorkloadAdmission(BackendSettings settings, ObjectMapper json) {
    this.settings = settings;
    this.json = json;
    ordinary = new Semaphore(settings.workbench().requestConcurrency());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler)
                  throws Exception {
                if (!settings.workbench().supportEnabled()
                    && handler instanceof HandlerMethod method
                    && method
                        .getBeanType()
                        .getPackageName()
                        .startsWith("com.poe2craft.craftingsupport.")) {
                  reject(response, 404, "FEATURE_UNAVAILABLE");
                  return false;
                }
                // Repeat batches have a separate, smaller budget; cancellation must not wait for
                // it.
                if (handler instanceof HandlerMethod method
                    && com.poe2craft.workbench.api.ChaosRepeatController.class.isAssignableFrom(
                        method.getBeanType())) return true;
                if (!ordinary.tryAcquire()) {
                  reject(response, 429, "WORKBENCH_BUSY");
                  return false;
                }
                request.setAttribute(PERMIT, Boolean.TRUE);
                return true;
              }

              @Override
              public void afterCompletion(
                  HttpServletRequest request,
                  HttpServletResponse response,
                  Object handler,
                  Exception error) {
                if (Boolean.TRUE.equals(request.getAttribute(PERMIT))) {
                  request.removeAttribute(PERMIT);
                  ordinary.release();
                }
              }
            })
        .addPathPatterns("/api/v1/crafting/**", "/api/v2/crafting/**");
  }

  private void reject(HttpServletResponse response, int status, String code)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setHeader("Cache-Control", "no-store");
    if (status == 429) response.setHeader("Retry-After", "1");
    json.writeValue(
        response.getOutputStream(),
        Map.of(
            "type",
            "about:blank",
            "title",
            status == 429 ? "Too many requests" : "Not found",
            "status",
            status,
            "code",
            code));
  }
}
