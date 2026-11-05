package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.api.MethodSearchController;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol.*;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class MethodSearchServiceTest {
  @Test
  void uniformBaseUsesItsOwnCatalogProvenanceAndAcceptsMethodSearch() throws Exception {
    var catalog = ItemCatalogLoader.loadSapphire();
    var identity = RulesetManifestLoader.load().identity();
    var index =
        new GoalCatalogIndex(java.util.List.of(catalog), GoalDefinitionsLoader.load(), identity);
    var goals = new GoalFilterService(index);
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
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              82,
              ItemState.Rarity.MAGIC,
              java.util.List.of(),
              java.util.List.of(),
              java.util.Set.of());
      var stat =
          index
              .knownStats(
                  new GoalFilter.Context(item.snapshotId(), item.baseItemId(), item.itemLevel()))
              .values()
              .stream()
              .filter(s -> s.kind() == GoalCatalog.Kind.EXPLICIT)
              .findFirst()
              .orElseThrow();
      var goal =
          new GoalFilter(
              1,
              index.version(),
              new GoalFilter.General(
                  catalog.base().id(),
                  new GoalFilter.Range(null, null),
                  java.util.List.of(ItemState.Rarity.RARE)),
              java.util.List.of(
                  new GoalFilter.Group(
                      "target",
                      GoalFilter.Type.AND,
                      false,
                      null,
                      java.util.List.of(
                          new GoalFilter.Entry(
                              "entry",
                              stat.statId(),
                              stat.unit(),
                              new GoalFilter.Range(java.math.BigDecimal.ONE, null),
                              null,
                              false)))));
      var request =
          new Create(
              2,
              "uniform-base",
              new BasicCurrencyState(item, service.provenance(catalog.base().id())),
              goal,
              java.util.List.of());
      var created = service.create("owner", request);
      try (var registry =
          new MethodSearchService(
              java.util.List.of(ItemCatalogLoader.loadDefault(), catalog),
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
                    new MethodSearchController(
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
