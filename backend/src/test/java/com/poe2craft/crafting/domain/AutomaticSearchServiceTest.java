package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticSearchServiceTest {
  static final class Repository implements AutomaticJobRepository {
    final Map<String, byte[]> documents = new LinkedHashMap<>();
    boolean fail;
    boolean budget;

    public List<String> ids() {
      return List.copyOf(documents.keySet());
    }

    public byte[] read(String id) {
      return documents.get(id).clone();
    }

    public void save(String id, byte[] data) {
      if (fail) throw new IllegalStateException("storage failure");
      if (budget) {
        budget = false;
        throw new AutomaticJobRepository.BudgetExceeded();
      }
      documents.put(id, data.clone());
    }

    public void delete(String id) {
      documents.remove(id);
    }
  }

  final BasicCurrencyTransitionsTest tiny = new BasicCurrencyTransitionsTest();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          policy,
          tiny.catalog,
          new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
          "amulets");

  AutomaticSearchService service(Repository repository) {
    return service(repository, Clock.systemUTC());
  }

  AutomaticSearchService service(Repository repository, Clock clock) {
    return new AutomaticSearchService(
        new ObjectMapper(),
        repository,
        policy,
        request ->
            new AutomaticSearchService.Runtime(
                kernel,
                item ->
                    new AutomaticMethodGraph.Progress(
                        GoalFilter.Status.NO_MATCH, List.of(GoalFilter.Status.NO_MATCH)),
                request.goal().toString()),
        clock);
  }

  MethodSearchProtocol.Create request(String id) {
    return new MethodSearchProtocol.Create(
        2,
        id,
        new BasicCurrencyState(
            tiny.item(ItemState.Rarity.RARE, List.of()), tiny.kernel.provenance()),
        new GoalFilter(
            1,
            "test",
            new GoalFilter.General(
                tiny.catalog.base().id(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group(
                    "g",
                    GoalFilter.Type.AND,
                    false,
                    null,
                    List.of(
                        new GoalFilter.Entry(
                            "e",
                            "unknown-kept",
                            "flat",
                            new GoalFilter.Range(null, null),
                            null,
                            false))))),
        List.of());
  }

  @Test
  void activeCapacityExpiresAfterFixedTtl() throws Exception {
    var repository = new Repository();
    var json = new ObjectMapper();
    var now = Instant.parse("2026-10-19T00:00:00Z");
    try (var service = service(repository, Clock.fixed(now, ZoneOffset.UTC))) {
      for (int i = 0; i < 16; i++) service.create("owner", request("ttl-" + i));
    }
    var before = new LinkedHashMap<>(repository.documents);
    try (var service = service(repository, Clock.fixed(now.plusSeconds(86399), ZoneOffset.UTC))) {
      assertThatThrownBy(() -> service.create("owner", request("before-expiry")))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              e -> {
                assertThat(e.code()).isEqualTo("JOB_CAPACITY");
                assertThat(e.retryAfterSeconds()).isNull();
              });
      assertThat(repository.documents).containsExactlyEntriesOf(before);
    }
    try (var service = service(repository, Clock.fixed(now.plusSeconds(86401), ZoneOffset.UTC))) {
      var created = service.create("owner", request("after-expiry"));
      assertThat(repository.ids()).containsExactly(created.id());
      assertThat(json.readTree(repository.read(created.id())).get("expires").asLong())
          .isEqualTo(now.plusSeconds(172801).toEpochMilli());
    }
  }

  @Test
  void capacityRejectsWithoutDeletingJobsAndAllowsIdempotentRead() {
    var repository = new Repository();
    try (var service = service(repository)) {
      for (int i = 0; i < 16; i++) service.create("owner", request("capacity-" + i));
      var before = new LinkedHashMap<>(repository.documents);
      assertThatThrownBy(() -> service.create("owner", request("extra")))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              e -> {
                assertThat(e.status()).isEqualTo(429);
                assertThat(e.code()).isEqualTo("JOB_CAPACITY");
                assertThat(e.retryAfterSeconds()).isNull();
                var response =
                    new com.poe2craft.crafting.presentation.pathsearch.PathSearchErrors()
                        .rejected(e);
                assertThat(response.getHeaders().getFirst("Retry-After")).isNull();
                assertThat(response.getBody().code()).isEqualTo("JOB_CAPACITY");
              });
      assertThat(repository.documents).containsExactlyEntriesOf(before);
      assertThat(service.create("owner", request("capacity-0")).id())
          .isEqualTo(repository.ids().getFirst());
    }
  }

  @Test
  void completedArchivesRemainReadableWhileNewSearchCompletes() throws Exception {
    var repository = new Repository();
    var json = new ObjectMapper();
    var views = new ArrayList<AutomaticSearchService.View>();
    try (var service =
        new AutomaticSearchService(
            json,
            repository,
            policy,
            request ->
                new AutomaticSearchService.Runtime(
                    kernel,
                    item ->
                        new AutomaticMethodGraph.Progress(
                            GoalFilter.Status.MATCH, List.of(GoalFilter.Status.MATCH)),
                    request.goal().toString()),
            Clock.systemUTC())) {
      for (int i = 0; i < 16; i++) {
        var created = service.create("owner", request("complete-" + i));
        var done =
            service.control(
                "owner",
                created.id(),
                new AutomaticSearchService.Control("finish", 1, 1, "ADVANCE"));
        assertThat(done.status()).isEqualTo("COMPLETE");
        views.add(done);
      }
      var before = new LinkedHashMap<>(repository.documents);
      var created = service.create("owner", request("new"));
      var done =
          service.control(
              "owner", created.id(), new AutomaticSearchService.Control("finish", 1, 1, "ADVANCE"));
      assertThat(done.status()).isEqualTo("COMPLETE");
      assertThat(repository.ids()).hasSize(17);
      for (var old : views) {
        assertThat(service.get("owner", old.id())).isEqualTo(old);
        assertThat(repository.read(old.id())).containsExactly(before.get(old.id()));
      }
      assertThat(service.create("owner", request("complete-0"))).isEqualTo(views.getFirst());
    }
  }

  @Test
  void concurrentAdmissionIdempotencyAndResumeRespectActiveLimit() throws Exception {
    var repository = new Repository();
    try (var service = service(repository);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      for (int i = 0; i < 15; i++) service.create("owner", request("active-" + i));
      var gate = new java.util.concurrent.CountDownLatch(1);
      var futures = new ArrayList<java.util.concurrent.Future<AutomaticSearchService.View>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            executor.submit(
                () -> {
                  gate.await();
                  return service.create("owner", request("same-request"));
                }));
      gate.countDown();
      var same = futures.getFirst().get();
      for (var future : futures) assertThat(future.get()).isEqualTo(same);
      assertThat(repository.ids()).hasSize(16);
      var cancelled =
          service.control(
              "owner",
              same.id(),
              new AutomaticSearchService.Control(
                  "cancel", same.revision(), same.generation(), "CANCEL"));
      var replacement = service.create("owner", request("replacement"));
      var before = repository.read(same.id());
      var resume =
          new AutomaticSearchService.Control(
              "resume", cancelled.revision(), cancelled.generation(), "RESUME");
      assertThatThrownBy(() -> service.control("owner", same.id(), resume))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              error -> assertThat(error.code()).isEqualTo("JOB_CAPACITY"));
      assertThat(repository.read(same.id())).containsExactly(before);
      service.control(
          "owner", replacement.id(), new AutomaticSearchService.Control("cancel", 1, 1, "CANCEL"));
      var resumed = service.control("owner", same.id(), resume);
      assertThat(resumed.status()).isEqualTo("READY");
      assertThat(service.control("owner", same.id(), resume)).isEqualTo(resumed);
      assertThatThrownBy(() -> service.create("owner", request("overflow")))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      var stale =
          new AutomaticSearchService.Control(
              "late-cancel", cancelled.revision(), cancelled.generation(), "CANCEL");
      assertThatThrownBy(() -> service.control("owner", same.id(), stale))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              error -> assertThat(error.code()).isEqualTo("STALE_REVISION"));
    }
  }

  @Test
  void pausedAdmissionIsReacquiredAndStorageFailureDoesNotConsumeActiveSlot() throws Exception {
    var repository = new Repository();
    var json = new ObjectMapper();
    String pausedId;
    try (var service = service(repository)) {
      var paused = service.create("owner", request("paused"));
      pausedId = paused.id();
    }
    var document =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(repository.read(pausedId));
    document.put("status", "PAUSED");
    document.put("reason", "RESOURCE_BUDGET");
    repository.save(pausedId, json.writeValueAsBytes(document));
    try (var service = service(repository)) {
      for (int i = 0; i < 16; i++) service.create("owner", request("active-" + i));
      assertThat(service.get("owner", pausedId).status()).isEqualTo("PAUSED");
      assertThatThrownBy(
              () ->
                  service.control(
                      "owner",
                      pausedId,
                      new AutomaticSearchService.Control("resume", 1, 1, "RESUME")))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              error -> assertThat(error.code()).isEqualTo("JOB_CAPACITY"));
    }
    var empty = new Repository();
    try (var service = service(empty)) {
      empty.budget = true;
      assertThatThrownBy(() -> service.create("owner", request("storage-full")))
          .isInstanceOfSatisfying(
              PathSearchProtocol.Rejected.class,
              error -> assertThat(error.code()).isEqualTo("JOB_STORAGE_CAPACITY"));
      assertThat(empty.ids()).isEmpty();
      assertThat(service.create("owner", request("retry")).status()).isEqualTo("READY");
    }
  }

  @Test
  void competingCreatesAndCancelResumeRaceAreSerialized() throws Exception {
    var repository = new Repository();
    try (var service = service(repository);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      for (int i = 0; i < 15; i++) service.create("owner", request("ready-" + i));
      var gate = new java.util.concurrent.CountDownLatch(1);
      var futures = new ArrayList<java.util.concurrent.Future<Object>>();
      for (int i = 0; i < 4; i++) {
        var key = "competing-" + i;
        futures.add(
            executor.submit(
                () -> {
                  gate.await();
                  try {
                    return service.create("owner", request(key));
                  } catch (PathSearchProtocol.Rejected error) {
                    return error.code();
                  }
                }));
      }
      gate.countDown();
      var outcomes = new ArrayList<Object>();
      for (var future : futures) outcomes.add(future.get());
      assertThat(outcomes.stream().filter(v -> v instanceof AutomaticSearchService.View))
          .hasSize(1);
      assertThat(outcomes.stream().filter(v -> v.equals("JOB_CAPACITY"))).hasSize(3);
      var winner =
          (AutomaticSearchService.View)
              outcomes.stream()
                  .filter(v -> v instanceof AutomaticSearchService.View)
                  .findFirst()
                  .orElseThrow();
      var cancelled =
          service.control(
              "owner",
              winner.id(),
              new AutomaticSearchService.Control("cancel-first", 1, 1, "CANCEL"));
      var race = new java.util.concurrent.CountDownLatch(1);
      var controls = new ArrayList<java.util.concurrent.Future<Object>>();
      for (var operation : List.of("CANCEL", "RESUME"))
        controls.add(
            executor.submit(
                () -> {
                  race.await();
                  try {
                    return service.control(
                        "owner",
                        winner.id(),
                        new AutomaticSearchService.Control(
                            operation, cancelled.revision(), cancelled.generation(), operation));
                  } catch (PathSearchProtocol.Rejected error) {
                    return error.code();
                  }
                }));
      race.countDown();
      var results = new ArrayList<Object>();
      for (var future : controls) results.add(future.get());
      assertThat(results.stream().filter(v -> v instanceof AutomaticSearchService.View)).hasSize(1);
      assertThat(results.stream().filter(v -> v.equals("STALE_REVISION"))).hasSize(1);
      assertThat(repository.ids()).hasSize(16);
    }
  }

  @Test
  void restartCommandsCancellationAndOriginalUnknownGoalsArePreserved() {
    var repository = new Repository();
    var request = request("original");
    String id;
    AutomaticSearchService.View saved;
    try (var service = service(repository)) {
      var created = service.create("owner", request);
      id = created.id();
      assertThat(service.create("owner", request)).isEqualTo(created);
      var advance =
          new AutomaticSearchService.Control(
              "advance", created.revision(), created.generation(), "ADVANCE");
      saved = service.control("owner", id, advance);
      assertThat(service.control("owner", id, advance)).isEqualTo(saved);
      assertThatThrownBy(
              () ->
                  service.control(
                      "owner", id, new AutomaticSearchService.Control("stale", 1, 1, "ADVANCE")))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      assertThatThrownBy(() -> service.get("other", id))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
    try (var service = service(repository)) {
      assertThat(service.get("owner", id)).isEqualTo(saved);
      assertThat(service.original("owner", id)).isEqualTo(request);
      var cancelled =
          service.control(
              "owner",
              id,
              new AutomaticSearchService.Control(
                  "cancel", saved.revision(), saved.generation(), "CANCEL"));
      assertThat(cancelled.status()).isEqualTo("CANCELLED");
      assertThat(cancelled.generation()).isGreaterThan(saved.generation());
      assertThatThrownBy(
              () ->
                  service.control(
                      "owner",
                      id,
                      new AutomaticSearchService.Control(
                          "late", cancelled.revision(), saved.generation(), "ADVANCE")))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
      var edited = service.create("owner", request("edited"));
      assertThat(edited.id()).isNotEqualTo(id);
      assertThat(edited.operations()).isZero();
    }
  }

  @Test
  void storageBudgetPausesAtLastDurableCursorAndRetainsQualityContext() {
    var repository = new Repository();
    String id;
    try (var service = service(repository)) {
      var original =
          service.create("owner", new AutomaticSearchService.Input(request("budget"), true));
      id = original.id();
      repository.budget = true;
      var paused =
          service.control(
              "owner", id, new AutomaticSearchService.Control("advance", 1, 1, "ADVANCE"));
      assertThat(paused.status()).isEqualTo("PAUSED");
      assertThat(paused.reason()).isEqualTo("STORAGE_BUDGET");
      assertThat(paused.operations()).isEqualTo(original.operations());
      assertThat(paused.complete()).isFalse();
    }
    try (var service = service(repository)) {
      assertThat(service.originalInput("owner", id).abstractQuality40()).isTrue();
      assertThat(service.get("owner", id).nodes().getFirst().quality40()).isTrue();
    }
  }

  @Test
  void failedPublicationRollsBackTheMutation() {
    var repository = new Repository();
    try (var service = service(repository)) {
      var original = service.create("owner", request("rollback"));
      repository.fail = true;
      assertThatThrownBy(
              () ->
                  service.control(
                      "owner",
                      original.id(),
                      new AutomaticSearchService.Control("cancel", 1, 1, "CANCEL")))
          .isInstanceOf(IllegalStateException.class);
      repository.fail = false;
      assertThat(service.get("owner", original.id())).isEqualTo(original);
    }
  }
}
