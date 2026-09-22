package com.poe2craft;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.bootstrap.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AdminSessionController.class,
    properties = {"app.admin.username=operator", "app.admin.password=SyntheticStrong!12345"})
@Import({SecurityConfiguration.class, AdminCredentials.class})
class AdminSecurityTest {
  @Autowired MockMvc mvc;

  @Test
  void loginRequiresCsrfRotatesSessionAndLogoutClearsAuthentication() throws Exception {
    mvc.perform(get("/api/v1/admin/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(true))
        .andExpect(jsonPath("$.csrfToken").isString());
    mvc.perform(
            post("/api/v1/admin/login")
                .param("username", "operator")
                .param("password", "SyntheticStrong!12345"))
        .andExpect(status().isForbidden());
    var result =
        mvc.perform(
                post("/api/v1/admin/login")
                    .with(csrf())
                    .param("username", "operator")
                    .param("password", "SyntheticStrong!12345"))
            .andExpect(status().isNoContent())
            .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    mvc.perform(get("/api/v1/admin/session").session(session))
        .andExpect(jsonPath("$.authenticated").value(true));
    mvc.perform(get("/unknown").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/admin/logout").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/admin/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/admin/session")).andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void wrongLoginReturnsGenericProblem() throws Exception {
    mvc.perform(
            post("/api/v1/admin/login")
                .with(csrf())
                .param("username", "operator")
                .param("password", "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    mvc.perform(get("/api/v1/admin/crawl-settings")).andExpect(status().isUnauthorized());
  }
}
