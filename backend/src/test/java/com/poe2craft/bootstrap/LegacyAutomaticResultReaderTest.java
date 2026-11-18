package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol;
import com.poe2craft.craftingsupport.search.infrastructure.LocalAutomaticJobRepository;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.item.ItemState;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * 구버전 자동 탐색 결과를 변경하거나 재개하지 않고 조회한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code open()}: 조회할 자원 또는 작업 범위를 연다. 반환: {@code AutomaticSearchService}.
 *   <li>{@code open(productionCatalog)}: 조회할 자원 또는 작업 범위를 연다. 입력: {@code
 *       productionCatalog}(production·카탈로그). 반환: {@code AutomaticSearchService}.
 *   <li>{@code fixture(name)}: 검증사례 처리에서 {@code json.readTree}, {@code node.put}을(를) 사용한다. 입력:
 *       {@code name}(이름). 반환: {@code ObjectNode}. 사용하는 예외: {@code Exception}.
 *   <li>{@code save(node)}: 현재 값을 저장 경계에 기록한다. 입력: {@code node}(노드). 반환: {@code String}. 사용하는 예외:
 *       {@code Exception}.
 *   <li>{@code bytes(id)}: 바이트수 처리에서 {@code Files.readAllBytes}, {@code directory.resolve}을(를)
 *       사용한다. 입력: {@code id}(식별자). 반환: {@code byte[]}. 사용하는 예외: {@code Exception}.
 *   <li>{@code previousSixAffixCatalogAndV12CursorRemainReadableWithoutMutation()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code previousAmuletV5CatalogAndV13CursorRemainReadableWithoutMutation()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code readsActualV8CompletedResultAndOriginalRequestWithoutChangingAnyBytes()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code inProgressV8IsReadOnlyPartialAndNeverPretendsToCompleteOrResume()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code historicalV9ObservationsRemainReadOnlyAcrossThePolicyChange()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code actualV10V3QualityAndChaosObservationsRemainUnchangedAndReadOnly()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code corruptedVersionReferencesAndMassAreRejectedWithoutChangingFiles()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code newV9StillGetsCancelsResumesAndCompletesNormally()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class LegacyAutomaticResultReaderTest {
  final ObjectMapper json = new ObjectMapper();
  @TempDir Path directory;

  AutomaticSearchService open() {
    return open(false);
  }

  AutomaticSearchService open(boolean productionCatalog) {
    var goals =
        productionCatalog
            ? new CraftingSupportConfiguration()
                .goalFilterService(GoalDirectedPlannerScenariosTest.workbench)
            : GoalDirectedPlannerScenariosTest.goals;
    var legacy =
        new MethodSearchService(
            GoalDirectedPlannerScenariosTest.workbench.reviewedCatalogs(),
            goals,
            "legacy-read-fixture",
            json);
    var service =
        new CraftingSupportConfiguration()
            .automaticSearchService(
                GoalDirectedPlannerScenariosTest.workbench,
                goals,
                legacy,
                json,
                directory.toString());
    legacy.close();
    return service;
  }

  /** 현재 작업을 생성할 때만 현재 카탈로그의 출처를 사용한다. 보관된 원본 문서는 그대로 검증한다. */
  BasicCurrencyState currentStart(ItemState item) {
    try (var service =
        new MethodSearchService(
            GoalDirectedPlannerScenariosTest.workbench.reviewedCatalogs(),
            GoalDirectedPlannerScenariosTest.goals,
            "legacy-read-fixture",
            json)) {
      var catalog =
          GoalDirectedPlannerScenariosTest.workbench.reviewedCatalogs().stream()
              .filter(c -> c.base().id().equals(item.baseItemId()))
              .findFirst()
              .orElseThrow();
      var currentItem =
          new ItemState(
              catalog.metadata().snapshotId(),
              item.baseItemId(),
              item.itemLevel(),
              item.rarity(),
              item.implicits(),
              item.explicits(),
              item.conditions());
      assertThat(new com.poe2craft.shared.item.ItemStateValidator(catalog).validate(currentItem))
          .isEmpty();
      return new BasicCurrencyState(currentItem, service.provenance(item.baseItemId()));
    }
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
  void previousSixAffixCatalogAndV12CursorRemainReadableWithoutMutation() throws Exception {
    var workbench = GoalDirectedPlannerScenariosTest.workbench;
    var goals = new CraftingSupportConfiguration().goalFilterService(workbench);
    var requestNode = (ObjectNode) fixture("complete").path("request").deepCopy();
    var item = json.treeToValue(requestNode.path("start").path("item"), ItemState.class);
    var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    ((ObjectNode) requestNode.path("goal"))
        .put("catalogVersion", goals.catalog(context).catalogVersion());
    requestNode.set("start", json.valueToTree(currentStart(item)));
    requestNode.put("clientRequestId", "previous-six-affix");
    String id;
    try (var service = open(true)) {
      id =
          service
              .create(
                  "legacy-owner", json.treeToValue(requestNode, MethodSearchProtocol.Create.class))
              .id();
    }
    var node = (ObjectNode) json.readTree(bytes(id));
    var oldIndex =
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(workbench),
            GoalDefinitionsLoader.load(),
            "reviewed-equipment-20261028-v3-d8fc1817187354306f0db443768486e8e15ea3f4e4904447300104750421bb7a");
    ((ObjectNode) node.path("request").path("goal")).put("catalogVersion", oldIndex.version());
    var graph = (ObjectNode) node.path("graph");
    graph.put("version", "goal-directed-milestone-graph-v12-joint-attribute-laws");
    graph.put("objective", json.writeValueAsString(node.path("request").path("goal")));
    ((ObjectNode) graph.path("identity"))
        .put("policyVersion", "approved-187-2026-10-28-six-affix-v5");
    for (var row : graph.path("nodes"))
      for (var member : row.path("members")) ((ObjectNode) member).remove("initialStage");
    save(node);
    var before = bytes(id);
    try (var service = open(true)) {
      var view = service.get("legacy-owner", id);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      assertThatThrownBy(
              () ->
                  service.control(
                      "legacy-owner",
                      id,
                      new AutomaticSearchService.Control(
                          "resume", view.revision(), view.generation(), "RESUME")))
          .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(bytes(id)).containsExactly(before);
  }

  @Test
  void previousAmuletV5CatalogAndV13CursorRemainReadableWithoutMutation() throws Exception {
    var workbench = GoalDirectedPlannerScenariosTest.workbench;
    var goals = new CraftingSupportConfiguration().goalFilterService(workbench);
    var requestNode = (ObjectNode) fixture("complete").path("request").deepCopy();
    var item = json.treeToValue(requestNode.path("start").path("item"), ItemState.class);
    var context = new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel());
    ((ObjectNode) requestNode.path("goal"))
        .put("catalogVersion", goals.catalog(context).catalogVersion());
    requestNode.set("start", json.valueToTree(currentStart(item)));
    requestNode.put("clientRequestId", "previous-six-affix");
    String id;
    try (var service = open(true)) {
      id =
          service
              .create(
                  "legacy-owner", json.treeToValue(requestNode, MethodSearchProtocol.Create.class))
              .id();
    }
    var node = (ObjectNode) json.readTree(bytes(id));
    var oldIndex =
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(workbench),
            GoalDefinitionsLoader.loadHistoricalV5(),
            "reviewed-equipment-20261028-v4-81ceaf5378ae88a6b874c0b8c04d729d35bd0f87a40c7b59a93ae16df03a6736");
    ((ObjectNode) node.path("request").path("goal")).put("catalogVersion", oldIndex.version());
    var graph = (ObjectNode) node.path("graph");
    graph.put("version", "goal-directed-milestone-graph-v13-initial-chaos-stage");
    graph.put("objective", json.writeValueAsString(node.path("request").path("goal")));
    ((ObjectNode) graph.path("identity"))
        .put("policyVersion", "approved-187-2026-10-28-first-affix-v6");
    for (var row : graph.path("nodes"))
      for (var member : row.path("members")) ((ObjectNode) member).remove("initialStage");
    save(node);
    var before = bytes(id);
    try (var service = open(true)) {
      var view = service.get("legacy-owner", id);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      assertThatThrownBy(
              () ->
                  service.control(
                      "legacy-owner",
                      id,
                      new AutomaticSearchService.Control(
                          "resume", view.revision(), view.generation(), "RESUME")))
          .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(bytes(id)).containsExactly(before);
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
  void historicalV9ObservationsRemainReadOnlyAcrossThePolicyChange() throws Exception {
    var node = fixture("complete");
    ((ObjectNode) node.path("graph"))
        .put("version", "goal-directed-milestone-graph-v9-terminal-classes");
    var id = save(node);
    var before = bytes(id);
    try (var service = open()) {
      var view = service.get("legacy-owner", id);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      assertThat(view.complete()).isTrue();
      assertThat(view.nodes()).hasSize(node.path("graph").path("nodes").size());
      assertThat(view.methods()).isNotEmpty();
      assertThatThrownBy(
              () ->
                  service.control(
                      "legacy-owner",
                      id,
                      new AutomaticSearchService.Control(
                          "resume", view.revision(), view.generation(), "RESUME")))
          .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(bytes(id)).containsExactly(before);
  }

  @Test
  void actualV10V3QualityAndChaosObservationsRemainUnchangedAndReadOnly() throws Exception {
    for (var name : List.of("quality30", "chaos")) {
      var node =
          (ObjectNode)
              json.readTree(getClass().getResourceAsStream("/legacy-v10/" + name + "-disk.json"));
      var id = save(node);
      var before = bytes(id);
      var expected =
          json.readValue(
              getClass().getResourceAsStream("/legacy-v10/" + name + "-view.json"),
              AutomaticSearchService.View.class);
      try (var service = open(true)) {
        var view = service.get("legacy-owner", id);
        assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
        assertThat(view.complete()).isEqualTo(expected.complete());
        assertThat(view.canResume()).isFalse();
        assertThat(view.nodes()).isEqualTo(expected.nodes());
        assertThat(view.methods())
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected.methods());
        assertThat(json.writeValueAsString(service.original("legacy-owner", id)))
            .isEqualTo(json.writeValueAsString(node.path("request")));
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
            old.version(),
            "new-v9",
            currentStart(old.start().item()),
            new GoalFilter(
                old.goal().version(),
                GoalDirectedPlannerScenariosTest.goals
                    .catalog(
                        new GoalFilter.Context(
                            old.start().item().snapshotId(),
                            old.start().item().baseItemId(),
                            old.start().item().itemLevel()))
                    .catalogVersion(),
                old.goal().general(),
                old.goal().groups()),
            old.activeOmens());
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
