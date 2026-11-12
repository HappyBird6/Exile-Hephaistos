package com.poe2craft;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AccountAccessIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4.5-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry p) {
    p.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testmigration");
    p.add("spring.datasource.url", PG::getJdbcUrl);
    p.add("spring.datasource.username", PG::getUsername);
    p.add("spring.datasource.password", PG::getPassword);
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;

  record Client(Cookie cookie, String csrf, String id) {}

  Client client(MvcResult r) throws Exception {
    var data = json.readTree(r.getResponse().getContentAsString());
    String[] cookie = r.getResponse().getHeader("Set-Cookie").split(";", 2)[0].split("=", 2);
    return new Client(
        new Cookie(cookie[0], cookie[1]),
        data.path("csrf").asText(),
        data.path("user").path("id").asText());
  }

  @Test
  void registrationSessionRotationCsrfOwnershipLogoutAndAdminIsolation() throws Exception {
    var guest = client(mvc.perform(get("/api/account/me")).andExpect(status().isOk()).andReturn());
    String username = "user_" + UUID.randomUUID().toString().substring(0, 8),
        password = "temporary-test-123";
    String credentials =
        json.writeValueAsString(java.util.Map.of("username", username, "password", password));
    mvc.perform(
            post("/api/account/register")
                .cookie(guest.cookie())
                .contentType("application/json")
                .content(credentials))
        .andExpect(status().isForbidden());
    var a =
        client(
            mvc.perform(
                    post("/api/account/register")
                        .cookie(guest.cookie())
                        .header("X-CSRF-Token", guest.csrf())
                        .contentType("application/json")
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(a.cookie().getValue()).isNotEqualTo(guest.cookie().getValue());
    assertThat(
            db.queryForObject(
                "SELECT password_hash FROM app_user.password_identity WHERE username=?",
                String.class,
                username))
        .startsWith("$2")
        .isNotEqualTo(password);
    mvc.perform(get("/api/account/library").cookie(guest.cookie()).header("X-Account-Id", a.id()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/account/library")
                .cookie(a.cookie())
                .header("X-Account-Id", UUID.randomUUID()))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/account/library").cookie(a.cookie()).header("X-Account-Id", a.id()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(get("/api/admin/dashboard").cookie(a.cookie())).andExpect(status().isNotFound());
    mvc.perform(
            put("/api/account/library/session/a")
                .header("X-Session-Generation", "0")
                .cookie(a.cookie())
                .header("X-Account-Id", a.id())
                .header("X-CSRF-Token", a.csrf())
                .contentType("application/json")
                .content("{\"version\":0,\"content\":{\"id\":\"a\",\"frames\":[{}]}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(post("/api/account/logout").cookie(a.cookie()).header("X-CSRF-Token", a.csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/account/library").cookie(a.cookie()).header("X-Account-Id", a.id()))
        .andExpect(status().isUnauthorized());
    var guest2 = client(mvc.perform(get("/api/account/me")).andReturn());
    mvc.perform(
            post("/api/account/login")
                .cookie(guest2.cookie())
                .header("X-CSRF-Token", guest2.csrf())
                .contentType("application/json")
                .content(credentials.replace(password, "incorrect-password")))
        .andExpect(status().isUnauthorized());
    var logged =
        client(
            mvc.perform(
                    post("/api/account/login")
                        .cookie(guest2.cookie())
                        .header("X-CSRF-Token", guest2.csrf())
                        .contentType("application/json")
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(logged.id()).isEqualTo(a.id());
  }

  @Test
  void authAttemptsAreBounded() throws Exception {
    var guest = client(mvc.perform(get("/api/account/me")).andReturn());
    for (int i = 0; i < 10; i++)
      mvc.perform(
              post("/api/account/login")
                  .with(
                      r -> {
                        r.setRemoteAddr("192.0.2.44");
                        return r;
                      })
                  .cookie(guest.cookie())
                  .header("X-CSRF-Token", guest.csrf())
                  .contentType("application/json")
                  .content("{\"username\":\"x\",\"password\":\"bad\"}"))
          .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/account/login")
                .with(
                    r -> {
                      r.setRemoteAddr("192.0.2.44");
                      return r;
                    })
                .cookie(guest.cookie())
                .header("X-CSRF-Token", guest.csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isTooManyRequests());
  }
}
