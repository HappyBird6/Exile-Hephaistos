package com.poe2craft.bootstrap;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSessionController {
  private final AdminCredentials credentials;

  public AdminSessionController(AdminCredentials credentials) {
    this.credentials = credentials;
  }

  @GetMapping("/api/v1/admin/session")
  public Map<String, Object> session(
      Authentication auth, CsrfToken csrf, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return Map.of(
        "configured",
        credentials.configured(),
        "authenticated",
        auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken),
        "csrfToken",
        csrf.getToken(),
        "csrfHeaderName",
        csrf.getHeaderName());
  }
}
