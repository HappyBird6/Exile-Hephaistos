package com.poe2craft.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // No product API or login provider is implemented yet. CSRF remains enabled.
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/liveness", "/actuator/health/readiness")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }
}
