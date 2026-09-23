package com.poe2craft.bootstrap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
  @Bean
  UserDetailsService adminUsers(AdminCredentials credentials) {
    var users = new InMemoryUserDetailsManager();
    if (credentials.configured())
      users.createUser(
          User.withUsername(credentials.username())
              .password(
                  PasswordEncoderFactories.createDelegatingPasswordEncoder()
                      .encode(credentials.password()))
              .roles("ADMIN")
              .build());
    return users;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AdminCredentials credentials)
      throws Exception {
    var parseRequest =
        new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
            "/api/v1/items/parse", "POST");
    // 읽기 전용 파싱은 세션을 사용하거나 상태를 변경하지 않는다. 관리자 CSRF는 유지한다.
    http.csrf(csrf -> csrf.ignoringRequestMatchers(parseRequest));
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(parseRequest)
                    .permitAll()
                    .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/session")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .access(
                        (authentication, context) ->
                            new org.springframework.security.authorization.AuthorizationDecision(
                                credentials.configured()
                                    && authentication.get().getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))))
                    .anyRequest()
                    .denyAll())
        .requestCache(cache -> cache.disable())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, error) ->
                            problem(
                                response,
                                request.getRequestURI().startsWith("/api/v1/admin/") ? 401 : 403,
                                "AUTH_REQUIRED"))
                    .accessDeniedHandler(
                        (request, response, error) -> problem(response, 403, "ACCESS_DENIED")));
    if (credentials.configured()) {
      http.formLogin(
              login ->
                  login
                      .loginPage("/api/v1/admin/session")
                      .loginProcessingUrl("/api/v1/admin/login")
                      .successHandler((request, response, auth) -> response.setStatus(204))
                      .failureHandler(
                          (request, response, error) ->
                              problem(response, 401, "INVALID_CREDENTIALS")))
          .logout(
              logout ->
                  logout
                      .logoutUrl("/api/v1/admin/logout")
                      .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
          .addFilterBefore(new LoginThrottle(), UsernamePasswordAuthenticationFilter.class);
    }
    return http.build();
  }

  static void problem(HttpServletResponse response, int status, String code) throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"instance\":\"\",\"type\":\"about:blank\",\"title\":\"Request rejected\",\"status\":"
                + status
                + ",\"detail\":\""
                + code
                + "\",\"code\":\""
                + code
                + "\",\"traceId\":\""
                + UUID.randomUUID()
                + "\"}");
  }

  static class LoginThrottle extends OncePerRequestFilter {
    private long window = System.nanoTime();
    private int attempts;

    private synchronized boolean allowed() {
      long now = System.nanoTime();
      if (now - window >= 60_000_000_000L) {
        window = now;
        attempts = 0;
      }
      return ++attempts <= 10;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      if (request.getMethod().equals("POST")
          && request.getServletPath().equals("/api/v1/admin/login")
          && !allowed()) {
        response.setHeader("Retry-After", "60");
        problem(response, 429, "LOGIN_RATE_LIMITED");
        return;
      }
      chain.doFilter(request, response);
    }
  }
}
