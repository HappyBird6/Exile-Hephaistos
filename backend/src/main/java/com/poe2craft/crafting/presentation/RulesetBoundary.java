package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.infrastructure.RulesetManifestLoader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Transport identity is deliberately outside ItemState and never defaults for older clients. */
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
    if (request.getRequestURI().startsWith("/api/v1/crafting/")) {
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
