package com.poe2craft.craftingsupport.rag.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.admin.api.AdminAccessFilter;
import com.poe2craft.craftingsupport.rag.application.RagComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RagComparisonControllerTest {
  final RagComparisonService service = mock(RagComparisonService.class);
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final String path = "/api/support-test/rag";

  @Test
  void statusAndComparisonAreAccessibleWithoutAdminPasswordOrAuthorization() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new RagComparisonController(service, json))
            .addFilters(new AdminAccessFilter(""))
            .build();
    when(service.availability())
        .thenReturn(new RagComparisonService.Availability(false, false, "model", "corpus"));
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    var fixture = new com.poe2craft.craftingsupport.rag.RagFixture();
    when(service.compare(any()))
        .thenThrow(
            new com.poe2craft.craftingsupport.rag.application.RagFailure(503, "RAG_DISABLED"));
    mvc.perform(
            post(path + "/compare")
                .contentType("application/json")
                .content(json.writeValueAsString(fixture.input())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("RAG_DISABLED"));
    verify(service).compare(any());
  }

  @Test
  void malformedUnknownFieldsAndNonBooleanStageNeverReachComparison() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new RagComparisonController(service, json))
            .addFilters(new AdminAccessFilter("test"))
            .build();
    for (var body :
        java.util.List.of(
            "{}",
            "{\"request\":null,\"initialStage\":\"true\",\"abstractQuality\":0,\"mode\":\"ALL\",\"locale\":\"ko\"}",
            "{\"request\":null,\"initialStage\":true,\"abstractQuality\":0,\"mode\":\"ALL\",\"locale\":\"ko\",\"extra\":1}"))
      mvc.perform(post(path + "/compare").contentType("application/json").content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("RAG_INVALID_INPUT"));
    verifyNoInteractions(service);
  }
}
