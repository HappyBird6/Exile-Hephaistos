package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class MethodSearchServiceTest {
  @Test
  void uniformBaseUsesItsOwnCatalogProvenanceAndAcceptsMethodSearch() throws Exception {
    var catalog = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadSapphire();
    var identity = com.poe2craft.crafting.infrastructure.RulesetManifestLoader.load().identity();
    var index =
        new com.poe2craft.crafting.domain.goalfilter.GoalCatalogIndex(
            java.util.List.of(catalog),
            com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load(),
            identity);
    var goals = new com.poe2craft.crafting.application.goalfilter.GoalFilterService(index);
    var queued = new java.util.ArrayDeque<Runnable>();
    try (var service =
        new MethodSearchService(
            catalog,
            goals,
            identity,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            Clock.systemUTC(),
            queued::add,
            64)) {
      var item =
          new com.poe2craft.item.ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              82,
              com.poe2craft.item.ItemState.Rarity.MAGIC,
              java.util.List.of(),
              java.util.List.of(),
              java.util.Set.of());
      var stat =
          index
              .knownStats(
                  new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context(
                      item.snapshotId(), item.baseItemId(), item.itemLevel()))
              .values()
              .stream()
              .filter(
                  s ->
                      s.kind()
                          == com.poe2craft.crafting.domain.goalfilter.GoalCatalog.Kind.EXPLICIT)
              .findFirst()
              .orElseThrow();
      var goal =
          new com.poe2craft.crafting.domain.goalfilter.GoalFilter(
              1,
              index.version(),
              new com.poe2craft.crafting.domain.goalfilter.GoalFilter.General(
                  catalog.base().id(),
                  new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(null, null),
                  java.util.List.of(com.poe2craft.item.ItemState.Rarity.RARE)),
              java.util.List.of(
                  new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group(
                      "target",
                      com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.AND,
                      false,
                      null,
                      java.util.List.of(
                          new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Entry(
                              "entry",
                              stat.statId(),
                              stat.unit(),
                              new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(
                                  java.math.BigDecimal.ONE, null),
                              null,
                              false)))));
      var request =
          new Create(
              2,
              "uniform-base",
              new com.poe2craft.crafting.domain.BasicCurrencyState(
                  item, service.provenance(catalog.base().id())),
              goal,
              java.util.List.of());
      var created = service.create("owner", request);
      try (var registry =
          new MethodSearchService(
              java.util.List.of(
                  com.poe2craft.item.infrastructure.ItemCatalogLoader.loadDefault(), catalog),
              goals,
              identity,
              new com.fasterxml.jackson.databind.ObjectMapper(),
              Clock.systemUTC(),
              ignored -> {},
              1)) {
        var registered = registry.create("owner", request);
        assertThat(registered.transition()).isEqualTo(request.start().provenance());
        var mvc =
            org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                    new com.poe2craft.crafting.presentation.pathsearch.MethodSearchController(
                        registry, new com.fasterxml.jackson.databind.ObjectMapper()))
                .build();
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v2/crafting/method-searches/provenance")
                    .param("baseItemId", catalog.base().id()))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.modelVersion")
                    .value("reviewed-basic-transition-v2"));
        registry.mutate(
            "owner",
            registered.jobId(),
            new Mutation(2, "stop", registered.revision(), "CANCEL", null));
      }
      queued.remove().run();
      assertThat(service.get("owner", created.jobId()).status()).isNotEqualTo("FAILED");
      assertThat(service.provenance(catalog.base().id()).weightPolicy())
          .isEqualTo("USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS");
      assertThatThrownBy(() -> service.provenance("missing-base"))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
  }

  @Test
  void cancellationGenerationIdempotencyAndResumePreservePartialWork() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var old = fixture.service(1);
        var service =
            new MethodSearchService(
                fixture.catalog,
                fixture.goals,
                fixture.identity,
                fixture.json,
                Clock.systemUTC(),
                fixture.executor,
                64)) {
      var input = fixture.request(old);
      var request = new Create(2, "v2-request", input.start(), input.goal(), input.activeOmens());
      var created = service.create("owner", request);
      assertThat(created.version()).isEqualTo(2);
      assertThat(service.create("owner", request)).isEqualTo(created);
      assertThat(fixture.queue).hasSize(1);
      var command = new Mutation(2, "cancel", created.revision(), "CANCEL", null);
      var cancelled = service.mutate("owner", created.jobId(), command);
      fixture.queue.remove().run();
      assertThat(service.get("owner", created.jobId())).isEqualTo(cancelled);
      assertThat(service.mutate("owner", created.jobId(), command)).isEqualTo(cancelled);
      assertThat(cancelled.generation()).isGreaterThan(created.generation());
      assertThatThrownBy(
              () ->
                  service.mutate(
                      "owner",
                      created.jobId(),
                      new Mutation(2, "stale", created.revision(), "RESUME", null)))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      service.mutate(
          "owner",
          created.jobId(),
          new Mutation(2, "resume", cancelled.revision(), "RESUME", null));
      fixture.queue.remove().run();
      var paused = service.get("owner", created.jobId());
      assertThat(paused.status()).isEqualTo("PAUSED");
      assertThat(paused.reason()).isEqualTo("COMPUTE_BUDGET");
      assertThat(paused.enumerationComplete()).isFalse();
      assertThat(paused.operations()).isPositive();
      assertThat(paused.graph().methods()).isNotEmpty();
      assertThatThrownBy(() -> service.get("other-owner", created.jobId()))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      assertThatThrownBy(
              () -> service.graph("owner", created.jobId(), paused.revision(), "invented"))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
  }
}
