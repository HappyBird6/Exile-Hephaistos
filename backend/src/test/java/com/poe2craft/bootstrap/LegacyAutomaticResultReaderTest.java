package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.infrastructure.LocalAutomaticJobRepository;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class LegacyAutomaticResultReaderTest {
  final ObjectMapper json = new ObjectMapper();
  @TempDir Path directory;

  AutomaticSearchService open() {
    var legacy =
        new MethodSearchService(
            GoalDirectedPlannerScenariosTest.workbench.reviewedCatalogs(),
            GoalDirectedPlannerScenariosTest.goals,
            "legacy-read-fixture",
            json);
    var service =
        new SupportConfiguration()
            .automaticSearchService(
                GoalDirectedPlannerScenariosTest.workbench,
                GoalDirectedPlannerScenariosTest.goals,
                legacy,
                json,
                directory.toString());
    legacy.close();
    return service;
  }

  ObjectNode fixture(String name) throws Exception {
    try (var stream = getClass().getResourceAsStream("/legacy-v8/" + name + ".json")) {
      var node = (ObjectNode) json.readTree(Objects.requireNonNull(stream));
      node.put("expires", Long.MAX_VALUE);
      return node;
    }
  }

  String save(ObjectNode node) throws Exception {
    var repo = new LocalAutomaticJobRepository(directory.resolve("automatic-v1"));
    var id = node.path("id").asText();
    repo.save(id, json.writeValueAsBytes(node));
    return id;
  }

  byte[] bytes(String id) throws Exception {
    return Files.readAllBytes(directory.resolve("automatic-v1").resolve(id + ".json"));
  }

  @Test
  void readsActualV8CompletedResultAndOriginalRequestWithoutChangingAnyBytes() throws Exception {
    var saved = fixture("complete");
    var id = save(saved);
    var before = bytes(id);
    try (var service = open()) {
      var actual = service.get("legacy-owner", id);
      var old =
          json.readValue(
              getClass().getResourceAsStream("/legacy-v8/view.json"),
              AutomaticSearchService.View.class);
      assertThat(actual.complete()).isTrue();
      assertThat(actual.canResume()).isFalse();
      assertThat(actual.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(actual.nodes()).isEqualTo(old.nodes());
      assertThat(actual.methods())
          .usingRecursiveComparison()
          .ignoringCollectionOrder()
          .isEqualTo(old.methods());
      assertThat(actual.recommendations()).isEmpty();
      var request = service.original("legacy-owner", id);
      assertThat(json.writeValueAsString(request))
          .isEqualTo(json.writeValueAsString(saved.path("request")));
      assertThat(service.create("legacy-owner", request)).isEqualTo(actual);
      assertThat(json.valueToTree(actual).has("resources")).isFalse();
      var output = Path.of("build/legacy-fixtures");
      Files.createDirectories(output);
      json.writeValue(output.resolve("complete-view.json").toFile(), actual);
      json.writeValue(output.resolve("request.json").toFile(), request);
      assertThatThrownBy(
              () ->
                  service.control(
                      "legacy-owner",
                      id,
                      new AutomaticSearchService.Control(
                          "resume", actual.revision(), actual.generation(), "RESUME")))
          .isInstanceOf(PathSearchProtocol.Rejected.class)
          .hasMessage("RECALCULATION_REQUIRED");
      assertThatThrownBy(() -> service.get("another-owner", id)).hasMessage("JOB_NOT_FOUND");
    }
    assertThat(bytes(id)).containsExactly(before);
  }

  @Test
  void inProgressV8IsReadOnlyPartialAndNeverPretendsToCompleteOrResume() throws Exception {
    var id = save(fixture("ready"));
    var before = bytes(id);
    try (var service = open()) {
      var view = service.get("legacy-owner", id);
      assertThat(view.status()).isEqualTo("PARTIAL");
      assertThat(view.complete()).isFalse();
      assertThat(view.canResume()).isFalse();
      assertThat(view.nodes()).hasSize(1);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      for (var operation : List.of("ADVANCE", "RESUME", "CANCEL"))
        assertThatThrownBy(
                () ->
                    service.control(
                        "legacy-owner",
                        id,
                        new AutomaticSearchService.Control(
                            operation, view.revision(), view.generation(), operation)))
            .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(bytes(id)).containsExactly(before);
  }

  @Test
  void corruptedVersionReferencesAndMassAreRejectedWithoutChangingFiles() throws Exception {
    for (int kind = 0; kind < 5; kind++) {
      var node = fixture("complete");
      var graph = (ObjectNode) node.path("graph");
      if (kind == 0) graph.put("version", "unrecognized-v10");
      if (kind == 1) ((ObjectNode) graph.path("executions").get(0)).put("source", 99999);
      if (kind == 2)
        ((ObjectNode) graph.path("executions").get(0).path("execution"))
            .put("version", "invalid-execution");
      if (kind == 3)
        ((ObjectNode) graph.path("executions").get(0).path("points").get(0)).putArray("outcomes");
      if (kind == 4) graph.put("operations", "123");
      var id = save(node);
      var before = bytes(id);
      try (var service = open()) {
        assertThatThrownBy(() -> service.get("legacy-owner", id))
            .isInstanceOf(PathSearchProtocol.Rejected.class)
            .hasMessage("RECALCULATION_REQUIRED");
      }
      assertThat(bytes(id)).containsExactly(before);
    }
  }

  @Test
  void newV9StillGetsCancelsResumesAndCompletesNormally() throws Exception {
    var old =
        json.treeToValue(fixture("complete").path("request"), MethodSearchProtocol.Create.class);
    var request =
        new MethodSearchProtocol.Create(
            old.version(), "new-v9", old.start(), old.goal(), old.activeOmens());
    try (var service = open()) {
      var view = service.create("new-owner", request);
      view =
          service.control(
              "new-owner",
              view.id(),
              new AutomaticSearchService.Control(
                  "cancel", view.revision(), view.generation(), "CANCEL"));
      view =
          service.control(
              "new-owner",
              view.id(),
              new AutomaticSearchService.Control(
                  "resume", view.revision(), view.generation(), "RESUME"));
      for (int i = 0; i < 100 && view.status().equals("READY"); i++)
        view =
            service.control(
                "new-owner",
                view.id(),
                new AutomaticSearchService.Control(
                    "advance-" + i, view.revision(), view.generation(), "ADVANCE"));
      assertThat(view.complete()).isTrue();
      assertThat(service.get("new-owner", view.id()).reason()).isNotEqualTo("LEGACY_READ_ONLY");
    }
  }
}
