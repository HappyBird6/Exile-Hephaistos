package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class MethodSearchLifecycleTest {
  private Create request(PathSearchServiceTest fixture) throws Exception {
    try (var old = fixture.service(1)) {
      var input = fixture.request(old);
      return new Create(2, "lifecycle", input.start(), input.goal(), input.activeOmens());
    }
  }

  private MethodSearchService service(PathSearchServiceTest fixture, Clock clock) {
    return new MethodSearchService(
        fixture.catalog,
        fixture.goals,
        fixture.identity,
        fixture.json,
        clock,
        fixture.executor,
        64);
  }

  private void rejected(String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(
            PathSearchProtocol.Rejected.class, e -> assertThat(e.code()).isEqualTo(code));
  }

  @Test
  void staleQueuedGenerationCannotPublishAfterCancelAndResume() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var service = service(fixture, Clock.systemUTC())) {
      var created = service.create("owner", request(fixture));
      var cancel = new Mutation(2, "cancel", created.revision(), "CANCEL", null);
      var cancelled = service.mutate("owner", created.jobId(), cancel);
      var resumed =
          service.mutate(
              "owner",
              created.jobId(),
              new Mutation(2, "resume", cancelled.revision(), "RESUME", null));
      assertThat(fixture.queue).hasSize(2);
      fixture.queue.remove().run();
      assertThat(service.get("owner", created.jobId())).isEqualTo(resumed);
      fixture.queue.remove().run();
      var paused = service.get("owner", created.jobId());
      assertThat(paused.operations()).isEqualTo(64);
      assertThat(paused.status()).isEqualTo("PAUSED");
      assertThat(service.mutate("owner", created.jobId(), cancel)).isEqualTo(cancelled);
      assertThat(service.get("owner", created.jobId())).isEqualTo(paused);
      rejected(
          "COMMAND_ID_REUSED",
          () ->
              service.mutate(
                  "owner",
                  created.jobId(),
                  new Mutation(2, "cancel", paused.revision(), "CANCEL", null)));
      rejected(
          "REVISION_CONFLICT",
          () ->
              service.mutate(
                  "owner",
                  created.jobId(),
                  new Mutation(2, "stale", resumed.revision(), "RESUME", null)));
      assertThat(service.get("owner", created.jobId())).isEqualTo(paused);
    }
  }

  @Test
  void reconnectReadsCurrentRevisionAndRejectsExpiredPagesAndCrossRevisionCursors()
      throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var service = service(fixture, Clock.systemUTC())) {
      var created = service.create("owner", request(fixture));
      fixture.queue.remove().run();
      var first = service.get("owner", created.jobId());
      var page = service.graph("owner", created.jobId(), first.revision(), null);
      assertThat(page.nextCursor()).isNotNull();
      var second =
          service.mutate(
              "owner",
              created.jobId(),
              new Mutation(2, "resume-0", first.revision(), "RESUME", null));
      assertThat(service.graph("owner", created.jobId(), first.revision(), null)).isEqualTo(page);
      rejected(
          "INVALID_CURSOR",
          () -> service.graph("owner", created.jobId(), second.revision(), page.nextCursor()));
      fixture.queue.remove().run();
      for (int i = 1; i <= 3; i++) {
        var current = service.get("owner", created.jobId());
        service.mutate(
            "owner",
            created.jobId(),
            new Mutation(2, "resume-" + i, current.revision(), "RESUME", null));
        fixture.queue.remove().run();
      }
      rejected(
          "REVISION_EXPIRED",
          () -> service.graph("owner", created.jobId(), first.revision(), null));
      var current = service.get("owner", created.jobId());
      assertThat(current.operations()).isGreaterThan(first.operations());
      assertThat(service.graph("owner", created.jobId(), current.revision(), null).revision())
          .isEqualTo(current.revision());
    }
  }

  @Test
  void expiredQueuedJobCannotPublishIntoANewJobWithTheSameRequestId() throws Exception {
    var fixture = new PathSearchServiceTest();
    var instant =
        new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-10-13T00:00:00Z"));
    var clock =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
          }

          public Instant instant() {
            return instant.get();
          }
        };
    try (var service = service(fixture, clock)) {
      var request = request(fixture);
      var old = service.create("owner", request);
      instant.updateAndGet(i -> i.plusSeconds(1200));
      rejected("JOB_EXPIRED", () -> service.get("owner", old.jobId()));
      var fresh = service.create("owner", request);
      assertThat(fresh.jobId()).isNotEqualTo(old.jobId());
      fixture.queue.remove().run();
      assertThat(service.get("owner", fresh.jobId())).isEqualTo(fresh);
      fixture.queue.remove().run();
      assertThat(service.get("owner", fresh.jobId()).operations()).isEqualTo(64);
    }
  }

  @Test
  void shrinkingResumeBudgetIsARequestErrorWithoutChangingTheJob() throws Exception {
    var fixture = new PathSearchServiceTest();
    try (var service =
        new MethodSearchService(
            fixture.catalog,
            fixture.goals,
            fixture.identity,
            fixture.json,
            Clock.systemUTC(),
            action -> {
              throw new RejectedExecutionException();
            },
            64)) {
      var created = service.create("owner", request(fixture));
      var paused = service.get("owner", created.jobId());
      assertThat(paused.reason()).isEqualTo("WORKER_CAPACITY");
      var increased =
          service.mutate(
              "owner",
              created.jobId(),
              new Mutation(2, "increase", paused.revision(), "RESUME", 200000));
      rejected(
          "INVALID_BUDGET",
          () ->
              service.mutate(
                  "owner",
                  created.jobId(),
                  new Mutation(2, "shrink", increased.revision(), "RESUME", 100000)));
      assertThat(service.get("owner", created.jobId())).isEqualTo(increased);
      assertThat(increased.operations()).isZero();
    }
  }
}
