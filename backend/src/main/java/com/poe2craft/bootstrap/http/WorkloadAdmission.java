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

/** Reject excess work before body parsing/engine execution; never queue unbounded computation. */
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
                if (request.getRequestURI().contains("/workbench/chaos-repeat")) return true;
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
