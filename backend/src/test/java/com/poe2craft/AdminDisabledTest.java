package com.poe2craft;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.bootstrap.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AdminSessionController.class,
    properties = {"app.admin.username=", "app.admin.password="})
@Import({SecurityConfiguration.class, AdminCredentials.class})
class AdminDisabledTest {
  @Autowired MockMvc mvc;

  @Test
  void absentAccountDisablesAllAdminActions() throws Exception {
    mvc.perform(get("/api/v1/admin/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(false));
    mvc.perform(post("/api/v1/admin/login").with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/admin/crawl-settings").with(user("synthetic").roles("ADMIN")))
        .andExpect(status().isForbidden());
  }
}
