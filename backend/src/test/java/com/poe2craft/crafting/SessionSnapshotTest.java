package com.poe2craft.crafting;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.SessionSnapshots;
import com.poe2craft.crafting.infrastructure.LocalSessionSnapshots;
import com.poe2craft.crafting.presentation.SessionSnapshotController;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionSnapshotTest {
  @TempDir Path directory;
  final ObjectMapper json = new ObjectMapper();
  final String document =
      """
      {"format":"exile-session","version":1,"exportedAt":"now","film":{"id":"original","createdAt":"then","sourceDocument":"untouched\\r\\ntext","future":{"field":true},"frames":[{"action":null,"state":{"baseItemId":"unknown","implicits":[],"explicits":[],"conditions":[]}}]}}
      """;

  @Test
  void immutableBytesSurviveRestartAndIdsCannotEscapeDirectory() throws Exception {
    var store = new LocalSessionSnapshots(directory);
    String id = store.create(document.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertTrue(id.matches("[a-f0-9]{48}"));
    assertArrayEquals(
        document.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        new LocalSessionSnapshots(directory).read(id));
    assertThrows(NoSuchFileException.class, () -> store.read("../private"));
    assertNotEquals(id, store.create(document.getBytes()));
  }

  @Test
  void validatesBoundariesWithoutExecutingHistoricalData() throws Exception {
    SessionSnapshots.validate(json.readTree(document));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SessionSnapshots.validate(
                json.readTree(document.replace("\"version\":1", "\"version\":2"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SessionSnapshots.validate(
                json.readTree(document.replace("\"future\"", "\"__proto__\""))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LocalSessionSnapshots(directory).create(new byte[SessionSnapshots.MAX_BYTES + 1]));
  }

  @Test
  void publicReadUsesOpaqueIdAndReturnsProblemsWithoutInternalDetails() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new SessionSnapshotController(json, directory.toString()))
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
