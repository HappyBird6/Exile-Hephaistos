package com.poe2craft.workbench.history.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.workbench.history.application.SessionSnapshotService;
import com.poe2craft.workbench.history.application.SessionSnapshotValidator;
import com.poe2craft.workbench.history.infrastructure.FileSessionSnapshotRepository;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionSnapshotTest {
  @Test
  void configuredStorageQuotasKeepExistingDocumentsReadable() throws Exception {
    byte[] bytes = document.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var oneDocument =
        new FileSessionSnapshotRepository(
            directory.resolve("count"), new BackendSettings.SessionHistory(1, 100000000));
    var id = oneDocument.create(bytes);
    assertThrows(java.io.IOException.class, () -> oneDocument.create(bytes));
    assertArrayEquals(bytes, oneDocument.read(id));
    var byteQuota =
        new FileSessionSnapshotRepository(
            directory.resolve("bytes"),
            new BackendSettings.SessionHistory(10, bytes.length * 2L - 1));
    var otherId = byteQuota.create(bytes);
    assertThrows(java.io.IOException.class, () -> byteQuota.create(bytes));
    assertArrayEquals(bytes, byteQuota.read(otherId));
  }

  @TempDir Path directory;
  final ObjectMapper json = new ObjectMapper();
  final String document =
      """
      {"format":"exile-session","version":1,"exportedAt":"now","film":{"id":"original","createdAt":"then","sourceDocument":"untouched\\r\\ntext","future":{"field":true},"frames":[{"action":null,"state":{"baseItemId":"unknown","implicits":[],"explicits":[],"conditions":[]}}]}}
      """;

  @Test
  void immutableBytesSurviveRestartAndIdsCannotEscapeDirectory() throws Exception {
    var store = new FileSessionSnapshotRepository(directory);
    String id = store.create(document.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertTrue(id.matches("[a-f0-9]{48}"));
    assertArrayEquals(
        document.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        new FileSessionSnapshotRepository(directory).read(id));
    assertThrows(NoSuchFileException.class, () -> store.read("../private"));
    assertNotEquals(id, store.create(document.getBytes()));
  }

  @Test
  void validatesBoundariesWithoutExecutingHistoricalData() throws Exception {
    SessionSnapshotValidator.validate(json.readTree(document));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SessionSnapshotValidator.validate(
                json.readTree(document.replace("\"version\":1", "\"version\":2"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SessionSnapshotValidator.validate(
                json.readTree(document.replace("\"future\"", "\"__proto__\""))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FileSessionSnapshotRepository(directory)
                .create(new byte[SessionSnapshotValidator.MAX_BYTES + 1]));
  }

  @Test
  void publicReadUsesOpaqueIdAndReturnsProblemsWithoutInternalDetails() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new SessionSnapshotController(
                    new SessionSnapshotService(json, new FileSessionSnapshotRepository(directory))))
            .setControllerAdvice(new SessionSnapshotController.Errors())
            .build();
    String response =
        mvc.perform(
                post("/api/v1/session-snapshots").contentType("application/json").content(document))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(response).path("id").asText();
    mvc.perform(get("/api/v1/session-snapshots/" + id))
        .andExpect(status().isOk())
        .andExpect(content().string(document));
    mvc.perform(get("/api/v1/session-snapshots/unknown")).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/session-snapshots").contentType("application/json").content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
