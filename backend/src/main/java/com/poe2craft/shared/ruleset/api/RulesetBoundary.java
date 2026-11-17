package com.poe2craft.shared.ruleset.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 아이템 상태 밖에서 HTTP 규칙 식별자를 확인하고 구버전 입력을 구분한다.
 *
 * <p>기존 계약: Transport identity is deliberately outside ItemState and never defaults for older
 * clients.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code HEADER}: 헤더; 초기값 {@code "X-Crafting-Ruleset"} ({@code String}).
 *   <li>{@code IDENTITY}: 정체성 ({@code String}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RulesetBoundary(json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON).
 *   <li>{@code identity()}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 반환: {@code String}.
 *   <li>{@code doFilterInternal(request, response, chain)}: 요청을 검사하고 다음 필터 호출 및 종료 처리를 수행한다. 입력:
 *       {@code request}(요청), {@code response}(응답), {@code chain}(필터연쇄). 사용하는 예외: {@code
 *       ServletException}, {@code IOException}.
 * </ul>
 */
@Component
public final class RulesetBoundary extends OncePerRequestFilter {
  public static final String HEADER = "X-Crafting-Ruleset";
  private static final String IDENTITY = RulesetManifestLoader.load().identity();
  private final ObjectMapper json;

  public RulesetBoundary(ObjectMapper json) {
    this.json = json;
  }

  public static String identity() {
    return IDENTITY;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getRequestURI().startsWith("/api/v1/crafting/")
        || request.getRequestURI().startsWith("/api/v2/crafting/")) {
      response.setHeader(HEADER, IDENTITY);
      if ("POST".equals(request.getMethod())) {
        String identity = request.getHeader(HEADER);
        boolean missing = identity == null || identity.isBlank();
        if (missing || !IDENTITY.equals(identity)) {
          var problem =
              ProblemDetail.forStatusAndDetail(
                  HttpStatus.UNPROCESSABLE_ENTITY,
                  missing
                      ? "Ruleset identity is required. Saved records remain readable; start a new craft using the current catalog."
                      : "This craft belongs to another ruleset. Its saved record is preserved and cannot be executed by the current engine.");
          problem.setTitle("Ruleset identity rejected");
          problem.setProperty(
              "code", missing ? "RULESET_IDENTITY_REQUIRED" : "RULESET_IDENTITY_MISMATCH");
          response.setStatus(422);
          response.setContentType("application/problem+json");
          // Preserve the existing endpoints; path-search v1 has a fixed four-member problem shape.
          Object payload =
              request.getRequestURI().startsWith("/api/v1/crafting/path-searches")
                  ? java.util.Map.of(
                      "type",
                      "about:blank",
                      "title",
                      problem.getTitle(),
                      "status",
                      422,
                      "code",
                      missing ? "RULESET_IDENTITY_REQUIRED" : "RULESET_IDENTITY_MISMATCH")
                  : problem;
          json.writeValue(response.getOutputStream(), payload);
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }
}
