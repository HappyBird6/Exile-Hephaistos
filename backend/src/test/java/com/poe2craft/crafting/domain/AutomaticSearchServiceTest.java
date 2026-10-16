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
        Clock.systemUTC());
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
