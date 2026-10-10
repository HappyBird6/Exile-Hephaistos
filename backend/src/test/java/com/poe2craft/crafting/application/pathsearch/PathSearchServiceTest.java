package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.infrastructure.RulesetManifestLoader;
import com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class PathSearchServiceTest {
  final ObjectMapper json = new ObjectMapper();
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final String identity = RulesetManifestLoader.load().identity();
  final GoalCatalogIndex index =
      new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load(), identity);
  final GoalFilterService goals = new GoalFilterService(index);
  final Queue<Runnable> queue = new ArrayDeque<>();
  final Executor executor = queue::add;

  PathSearchService service(int budget) {
    return new PathSearchService(
        catalog, goals, identity, json, Clock.systemUTC(), executor, budget);
  }

  Create request(PathSearchService service) throws Exception {
    var fixture =
        json.readTree(Path.of("../contracts/crafting-paths-v1/solar-source-fixture.json").toFile());
    var item = json.treeToValue(fixture.get("startItem"), ItemState.class);
    assertThat(fixture.get("rulesetIdentity").asText()).isEqualTo(identity);
    // The source fixture pins the ordinary snapshot; loadDefault adds reviewed special sources.
    // This explicit test binding constructs a NEW current item, never migrates a submitted one.
    assertThat(catalog.compatibleSnapshotIds()).contains(item.snapshotId());
    assertThat(new ItemStateValidator(catalog).validate(item))
        .extracting(ItemStateValidator.Violation::code)
        .containsExactly(ItemStateValidator.Code.SNAPSHOT_MISMATCH);
    var ordinary =
        json.readTree(Path.of("src/main/resources/catalog/solar-amulet/catalog.json").toFile());
    for (var definition : ordinary.get("modifiers")) {
      var expected = json.treeToValue(definition, ModifierDefinition.class);
      assertThat(catalog.find(expected.id()).orElseThrow()).isEqualTo(expected);
    }
    item =
        new ItemState(
            catalog.metadata().snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            item.explicits(),
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality());
    var goal = (ObjectNode) fixture.get("goalTemplate").deepCopy();
    goal.put("catalogVersion", index.version());
    return new Create(
        1,
        "request-1",
        new BasicCurrencyState(item, service.provenance()),
        json.treeToValue(goal, GoalFilter.class),
        List.of(),
        List.of("500", "0", "1", "2", "100", "300"));
  }

  Snapshot finish(PathSearchService service, Snapshot snapshot) {
    while (!queue.isEmpty()) {
      queue.remove().run();
      snapshot = service.get("00000000-0000-0000-0000-000000000001", snapshot.jobId());
      if (snapshot.resumable())
        snapshot =
            service.mutate(
                "00000000-0000-0000-0000-000000000001",
                snapshot.jobId(),
                new Mutation(1, "RESUME", UUID.randomUUID().toString(), snapshot.revision()));
    }
    return snapshot;
  }

  @Test
  void solarOracleAndRealContinuationMatchWithoutLosingPhaseOrMass() throws Exception {
    long started = System.nanoTime();
    try (var uninterrupted = service(50000);
        var interrupted = service(777)) {
      var request = request(uninterrupted);
      var controller =
          new com.poe2craft.crafting.presentation.pathsearch.PathSearchController(
              uninterrupted, json);
      var http = new org.springframework.mock.web.MockHttpServletRequest();
      http.setCookies(
          new jakarta.servlet.http.Cookie(
              "crafting_path_client", "00000000-0000-0000-0000-000000000001"));
      var accepted =
          controller.create(
              json.writeValueAsString(request),
              http,
              new org.springframework.mock.web.MockHttpServletResponse());
      assertThat(accepted.getStatusCode().value()).isEqualTo(202);
      var result = finish(uninterrupted, accepted.getBody());
      assertThat(
              controller.get(
                  result.jobId(), http, new org.springframework.mock.web.MockHttpServletResponse()))
          .isEqualTo(result);
      assertThat(result.status()).isEqualTo("COMPLETED");
      assertThat(result.recommendations()).hasSize(6);
      var resumed =
          interrupted.create("00000000-0000-0000-0000-000000000001", request(interrupted));
      queue.remove().run();
      resumed = interrupted.get("00000000-0000-0000-0000-000000000001", resumed.jobId());
      assertThat(resumed.status()).isEqualTo("PAUSED");
      var frozen = resumed;
      var command = new Mutation(1, "CANCEL", "cancel-1", resumed.revision());
      var cancelled =
          interrupted.mutate("00000000-0000-0000-0000-000000000001", resumed.jobId(), command);
      assertThat(cancelled.status()).isEqualTo("CANCELLED");
      assertThat(
              interrupted.mutate("00000000-0000-0000-0000-000000000001", resumed.jobId(), command))
          .isEqualTo(cancelled);
      resumed =
          interrupted.mutate(
              "00000000-0000-0000-0000-000000000001",
              resumed.jobId(),
              new Mutation(1, "RESUME", "resume-1", cancelled.revision()));
      resumed = finish(interrupted, resumed);
      assertThat(resumed.recommendations()).isEqualTo(result.recommendations());
      assertThat(resumed.rankings()).isEqualTo(result.rankings());
      assertThat(frozen.status()).isEqualTo("PAUSED");
      var chaos = result.recommendations().getFirst();
      var p = Fraction.of(650, 21107);
      for (var point : chaos.points()) {
        int n = Integer.parseInt(point.attempts());
        var survival =
            new Fraction(
                java.math.BigInteger.valueOf(20457).pow(n),
                java.math.BigInteger.valueOf(21107).pow(n));
        assertThat(point.active().fraction()).isEqualTo(survival);
        assertThat(point.lower().fraction())
            .isEqualTo(
                com.poe2craft.crafting.domain.pathsearch.RenewalFirstHit.complement(survival));
      }
      assertThat(chaos.points().get(1).lower().fraction()).isEqualTo(p);
      assertThat(result.rankings()).allMatch(r -> r.status().equals("CERTIFIED_WITHIN_CANDIDATES"));
      for (var r : result.recommendations())
        for (var point : r.points())
          assertThat(
                  point
                      .lower()
                      .fraction()
                      .add(point.active().fraction())
                      .add(point.dead().fraction())
                      .add(point.unresolved().fraction()))
              .isEqualTo(Fraction.ONE);
      var pages = new ArrayList<GraphPage>();
      var page = result.graph();
      do {
        pages.add(page);
        if (page.nextCursor() == null) break;
        page =
            uninterrupted.graph(
                "00000000-0000-0000-0000-000000000001",
                result.jobId(),
                result.revision(),
                page.nextCursor());
      } while (true);
      assertThat(pages).hasSizeGreaterThan(1);
      assertThat(pages.getFirst().expansions()).isEmpty();
      assertThat(pages.getLast().expansions()).isNotEmpty();
      assertThat(
              uninterrupted.graph(
                  "00000000-0000-0000-0000-000000000001",
                  result.jobId(),
                  result.revision(),
                  result.graph().nextCursor()))
          .isEqualTo(pages.get(1));
      var failure =
          result.graph().executions().stream()
              .filter(
                  e ->
                      e.policyId().equals("solar-policy-1")
                          && !e.stateId().equals(request.start().canonicalKey()))
              .findFirst()
              .orElseThrow();
      var recovery =
          uninterrupted.recover(
              "00000000-0000-0000-0000-000000000001",
              result.jobId(),
              new Recover(
                  1,
                  "recovery-1",
                  result.revision(),
                  failure.id(),
                  request.start().canonicalKey(),
                  List.of("0", "1", "2")));
      assertThat(recovery.recovery().conditional()).isTrue();
      assertThat(recovery.recovery().includedInMain()).isFalse();
      assertThat(uninterrupted.get("00000000-0000-0000-0000-000000000001", result.jobId()))
          .isEqualTo(result);
      // Cancel before the recovery worker starts; the old queued generation cannot publish.
      var stopped =
          uninterrupted.mutate(
              "00000000-0000-0000-0000-000000000001",
              recovery.jobId(),
              new Mutation(1, "CANCEL", "cancel-recovery", recovery.revision()));
      queue.remove().run();
      assertThat(uninterrupted.get("00000000-0000-0000-0000-000000000001", recovery.jobId()))
          .isEqualTo(stopped);
      Files.createDirectories(Path.of("build/path-search"));
      json.writeValue(Path.of("build/path-search/solar-snapshot.json").toFile(), result);
      json.writeValue(Path.of("build/path-search/solar-pages.json").toFile(), pages);
      System.out.println(
          "PATH_SEARCH_ORACLE elapsedMs="
              + (System.nanoTime() - started) / 1_000_000
              + " nodes="
              + result.graph().nodes().size()
              + " executions="
              + result.graph().executions().size()
              + " pages="
              + pages.size());
    }
  }

  @Test
  void idempotencyIsolationConflictsAndExpiredRevisions() throws Exception {
    try (var service = service(1)) {
      var request = request(service);
      var first = service.create("00000000-0000-0000-0000-000000000001", request);
      assertThat(service.create("00000000-0000-0000-0000-000000000001", request)).isEqualTo(first);
      assertThat(service.create("someone-else", request).jobId()).isNotEqualTo(first.jobId());
      assertThatThrownBy(() -> service.get("someone-else", first.jobId()))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("JOB_EXPIRED"));
      var altered =
          new Create(
              1,
              request.clientRequestId(),
              request.start(),
              request.goal(),
              List.of(),
              List.of("4"));
      assertThatThrownBy(() -> service.create("00000000-0000-0000-0000-000000000001", altered))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("REQUEST_ID_REUSED"));
      assertThatThrownBy(
              () ->
                  service.mutate(
                      "00000000-0000-0000-0000-000000000001",
                      first.jobId(),
                      new Mutation(1, "CANCEL", "bad-revision", 99L)))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("REVISION_CONFLICT"));
      assertThatThrownBy(
              () -> service.graph("00000000-0000-0000-0000-000000000001", first.jobId(), 99, null))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("REVISION_EXPIRED"));
      assertThatThrownBy(
              () ->
                  service.graph(
                      "00000000-0000-0000-0000-000000000001",
                      first.jobId(),
                      first.revision(),
                      "forged"))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("INVALID_CURSOR"));
      var cancelled =
          service.mutate(
              "00000000-0000-0000-0000-000000000001",
              first.jobId(),
              new Mutation(1, "CANCEL", "same-command", first.revision()));
      assertThatThrownBy(
              () ->
                  service.mutate(
                      "00000000-0000-0000-0000-000000000001",
                      first.jobId(),
                      new Mutation(1, "RESUME", "same-command", cancelled.revision())))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("COMMAND_ID_REUSED"));
    }
  }
}
