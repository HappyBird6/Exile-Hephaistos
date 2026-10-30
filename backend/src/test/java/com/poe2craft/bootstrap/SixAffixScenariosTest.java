package com.poe2craft.bootstrap;

import static com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.item.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

@Timeout(90)
class SixAffixScenariosTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();
  @TempDir Path directory;
  static final List<String> STATS =
      List.of(
          LIFE,
          "base_maximum_mana",
          "base_maximum_energy_shield",
          CHAOS,
          COLD,
          "base_fire_damage_resistance_%");

  ItemState root(int count) {
    var modifiers =
        STATS.subList(0, count).stream()
            .map(
                stat -> {
                  var definition =
                      catalog.modifiers().values().stream()
                          .filter(
                              m -> m.stats().size() == 1 && m.stats().getFirst().id().equals(stat))
                          .findFirst()
                          .orElseThrow();
                  return new ModifierInstance(
                      definition.id(), Map.of(stat, definition.stats().getFirst().min()));
                })
            .toList();
    return f.item(ItemState.Rarity.RARE, 82, modifiers);
  }

  @Test
  void productionAcceptsFiveAndSixStartsAndRejectsSevenOrInvalidRarity() throws Exception {
    var json = new ObjectMapper();
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                  new com.poe2craft.crafting.presentation.pathsearch.AutomaticSearchController(
                      service, json))
              .setControllerAdvice(
                  new com.poe2craft.crafting.presentation.pathsearch.PathSearchErrors())
              .build();
      var owner = "11111111-1111-4111-8111-111111111111";
      for (int count : List.of(5, 6)) {
        var item = root(count);
        var requirements = new LinkedHashMap<String, Long>();
        STATS.subList(0, count).forEach(s -> requirements.put(s, 1L));
        var goal = f.goal(item, requirements);
        assertThat(goals.minimumExplicitModifiers(item, goal)).isEqualTo(count);
        var request =
            new MethodSearchProtocol.Create(
                2,
                "start-" + count,
                new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
                goal,
                List.of());
        var payload = json.valueToTree(new AutomaticSearchService.Input(request, false));
        if (count == 5)
          ((com.fasterxml.jackson.databind.node.ObjectNode)
                  payload.path("request").path("start").path("item"))
              .remove(
                  "desecrated"); // Existing clients and stored scenarios omit the optional field.
        var response =
            mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                            "/api/v2/crafting/method-searches/automatic")
                        .cookie(new jakarta.servlet.http.Cookie("crafting_method_client", owner))
                        .contentType("application/json")
                        .content(json.writeValueAsString(payload)))
                .andExpect(
                    org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isAccepted())
                .andReturn()
                .getResponse();
        var view =
            json.readValue(response.getContentAsByteArray(), AutomaticSearchService.View.class);
        assertThat(view.nodes().getFirst().item().explicits()).hasSize(count);
        assertThat(service.get(owner, view.id()).nodes()).isEqualTo(view.nodes());
      }
      var valid = root(6);
      var seven = new ArrayList<>(valid.explicits());
      seven.add(f.life());
      var fourPrefixes = new ArrayList<>(root(3).explicits());
      fourPrefixes.add(
          new ModifierInstance("amulet:prefix:adept-s", Map.of("spell_damage_+%", 8L)));
      for (var invalid :
          List.of(
              f.item(ItemState.Rarity.RARE, 82, seven),
              f.item(ItemState.Rarity.RARE, 82, fourPrefixes),
              f.item(ItemState.Rarity.MAGIC, 82, valid.explicits()),
              f.item(ItemState.Rarity.RARE, 82, List.of(f.life(), f.life())))) {
        var request =
            new MethodSearchProtocol.Create(
                2,
                UUID.randomUUID().toString(),
                new BasicCurrencyState(invalid, legacy.provenance(invalid.baseItemId())),
                f.goal(valid, Map.of(LIFE, 1L)),
                List.of());
        assertThatThrownBy(() -> service.create("six", request)).hasMessage("INVALID_ITEM");
      }
    }
  }

  @Test
  void priorFourAffixPolicyAndGoalCatalogRemainReadOnlyWithoutChangingSavedBytes()
      throws Exception {
    var json = new ObjectMapper();
    var previous =
        "reviewed-equipment-20261009-v2-c303fc1ea724ddf2ccca61f7c9512e6c9fdcd997cb9d4348fe5a5f8766f07d5a";
    var oldCatalog =
        new com.poe2craft.crafting.domain.goalfilter.GoalCatalogIndex(
            com.poe2craft.crafting.infrastructure.goalfilter.BundledGoalCatalogs.load(workbench),
            com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load(),
            previous);
    var productionGoals = new SupportConfiguration().goalFilterService(workbench);
    String id;
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var item = root(4);
      var request =
          new MethodSearchProtocol.Create(
              2,
              "legacy-four",
              new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
              f.goal(item, Map.of(LIFE, 1L)),
              List.of());
      id = service.create("legacy", request).id();
    }
    var path = directory.resolve("automatic-v1").resolve(id + ".json");
    var disk = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(path.toFile());
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("graph").path("identity"))
        .put("policyVersion", "approved-187-2026-10-21-rarity-v4");
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("request").path("goal"))
        .put("catalogVersion", oldCatalog.version());
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("graph"))
        .put("objective", json.writeValueAsString(disk.path("request").path("goal")));
    json.writeValue(path.toFile(), disk);
    var before = java.nio.file.Files.readAllBytes(path);
    try (var legacy =
            new MethodSearchService(
                workbench.reviewedCatalogs(), productionGoals, "six-affix", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(
                    workbench, productionGoals, legacy, json, directory.toString())) {
      var view = service.get("legacy", id);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      for (var operation : List.of("ADVANCE", "RESUME", "CANCEL"))
        assertThatThrownBy(
                () ->
                    service.control(
                        "legacy",
                        id,
                        new AutomaticSearchService.Control(
                            operation, view.revision(), view.generation(), operation)))
            .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(java.nio.file.Files.readAllBytes(path)).containsExactly(before);
  }

  @Test
  void fiveAndSixGoalsFromFourAffixesKeepPartialWorkWithoutFinishingExalt() {
    for (int count : List.of(5, 6)) {
      var item = root(4);
      var requirements = new LinkedHashMap<String, Long>();
      STATS.subList(0, count).forEach(s -> requirements.put(s, 1L));
      var goal = f.goal(item, requirements);
      long started = System.nanoTime();
      try (var graph = f.graph(item, goal)) {
        f.exhaust(graph);
        System.out.println(
            "SIX_AFFIX|goal="
                + count
                + "|complete="
                + graph.complete()
                + "|hasGoal="
                + graph.hasGoal()
                + "|resources="
                + graph.resources()
                + "|ms="
                + (System.nanoTime() - started) / 1000000);
        assertThat(graph.hasGoal()).isFalse();
        assertThat(graph.nodes().size()).isGreaterThan(1);
        assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 6);
        assertThat(graph.nodes())
            .allMatch(n -> new ItemStateValidator(catalog).validate(n.item()).isEmpty());
        if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
      }
    }
  }

  @Test
  void sixGoalCancellationResumeAndReloadPreserveResults() throws Exception {
    var json = new ObjectMapper();
    var item = root(4);
    var requirements = new LinkedHashMap<String, Long>();
    STATS.forEach(stat -> requirements.put(stat, 1L));
    var goal = f.goal(item, requirements);
    AutomaticSearchService.View result;
    MethodSearchProtocol.Create request;
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      request =
          new MethodSearchProtocol.Create(
              2,
              "six-resume",
              new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
              goal,
              List.of());
      var view = service.create("resume", request);
      view =
          service.control(
              "resume",
              view.id(),
              new AutomaticSearchService.Control(
                  "cancel", view.revision(), view.generation(), "CANCEL"));
      assertThat(view.status()).isEqualTo("CANCELLED");
      view =
          service.control(
              "resume",
              view.id(),
              new AutomaticSearchService.Control(
                  "resume", view.revision(), view.generation(), "RESUME"));
      long maxMillis = 0;
      for (int i = 0; i < 100 && view.status().equals("READY"); i++) {
        long started = System.nanoTime();
        view =
            service.control(
                "resume",
                view.id(),
                new AutomaticSearchService.Control(
                    "advance-" + i, view.revision(), view.generation(), "ADVANCE"));
        maxMillis = Math.max(maxMillis, (System.nanoTime() - started) / 1000000);
      }
      assertThat(view.nodes().size()).isGreaterThan(1);
      assertThat(view.nodes())
          .noneMatch(
              n ->
                  n.progress().goal()
                      == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH);
      assertThat(view.nodes()).allMatch(n -> n.item().explicitSlotCount() <= 6);
      result = view;
      System.out.println(
          "SIX_API|status="
              + view.status()
              + "|maxAdvanceMs="
              + maxMillis
              + "|resources="
              + view.resources());
    }
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new SupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var restored = service.get("resume", result.id());
      assertThat(restored.nodes()).isEqualTo(result.nodes());
      assertThat(restored.methods()).isEqualTo(result.methods());
    }
    var output = Path.of("build/six-affix-fixtures");
    java.nio.file.Files.createDirectories(output);
    json.writeValue(output.resolve("six-goal-request.json").toFile(), request);
    json.writeValue(output.resolve("six-goal-view.json").toFile(), result);
  }

  @Test
  void sixGoalsFromMagicLifeStayBoundedAndOrDoesNotRequireEveryEntry() {
    var item = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var requirements = new LinkedHashMap<String, Long>();
    STATS.forEach(stat -> requirements.put(stat, 1L));
    var goal = f.goal(item, requirements);
    long started = System.nanoTime();
    try (var graph = f.graph(item, goal)) {
      f.exhaust(graph);
      System.out.println(
          "SIX_FROM_ONE|complete="
              + graph.complete()
              + "|hasGoal="
              + graph.hasGoal()
              + "|resources="
              + graph.resources()
              + "|ms="
              + (System.nanoTime() - started) / 1000000);
      assertThat(graph.finished() || graph.resourceLimited()).isTrue();
      assertThat(graph.nodes())
          .allMatch(n -> new ItemStateValidator(catalog).validate(n.item()).isEmpty());
      if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
    }
    var group = goal.groups().getFirst();
    var alternative =
        new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(
                new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group(
                    group.id(),
                    com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.COUNT,
                    false,
                    new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(
                        java.math.BigDecimal.ONE, null),
                    group.entries())));
    assertThat(goals.minimumExplicitModifiers(item, alternative)).isZero();
    assertThat(f.evaluation(item, alternative).apply(root(5)).goal())
        .isEqualTo(com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH);
  }
}
