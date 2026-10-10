package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PathSearchLifecycleTest {
  @Test
  void ttlAndBoundedCapacityDoNotRecomputeOrMixOwners() throws Exception {
    var f = new PathSearchServiceTest();
    var now = new AtomicReference<>(Instant.parse("2026-10-10T00:00:00Z"));
    var clock =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId zone) {
            return this;
          }

          public Instant instant() {
            return now.get();
          }
        };
    var tasks = new ArrayDeque<Runnable>();
    try (var service =
        new PathSearchService(f.catalog, f.goals, f.identity, f.json, clock, tasks::add, 1)) {
      var r = f.request(service);
      var first = service.create("owner", r);
      for (int i = 1; i < 16; i++)
        service.create(
            "owner", new Create(1, "r" + i, r.start(), r.goal(), List.of(), r.observations()));
      assertThatThrownBy(
              () ->
                  service.create(
                      "owner",
                      new Create(1, "overflow", r.start(), r.goal(), List.of(), r.observations())))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("SEARCH_CAPACITY_REACHED"));
      now.set(now.get().plusSeconds(901));
      assertThatThrownBy(() -> service.get("owner", first.jobId()))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("JOB_EXPIRED"));
      while (!tasks.isEmpty()) tasks.remove().run();
      assertThat(service.create("owner", r).jobId()).isNotEqualTo(first.jobId());
    }
  }

  @Test
  void cancellationRacesWithActualExecutorAndInvalidatesQueuedGeneration() throws Exception {
    var f = new PathSearchServiceTest();
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var finished = new CountDownLatch(1);
    var worker = Executors.newSingleThreadExecutor();
    try (var service =
        new PathSearchService(
            f.catalog,
            f.goals,
            f.identity,
            f.json,
            Clock.systemUTC(),
            task ->
                worker.execute(
                    () -> {
                      started.countDown();
                      try {
                        release.await();
                        task.run();
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      } finally {
                        finished.countDown();
                      }
                    }),
            50000)) {
      var accepted = service.create("owner", f.request(service));
      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      long start = System.nanoTime();
      var cancelled =
          service.mutate(
              "owner", accepted.jobId(), new Mutation(1, "CANCEL", "race", accepted.revision()));
      long cancelMicros = (System.nanoTime() - start) / 1000;
      release.countDown();
      assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(service.get("owner", accepted.jobId())).isEqualTo(cancelled);
      assertThat(cancelled.resumable()).isTrue();
      System.out.println("PATH_SEARCH_CANCEL acknowledgementMicros=" + cancelMicros);
    } finally {
      release.countDown();
      worker.shutdownNow();
    }
  }
}
