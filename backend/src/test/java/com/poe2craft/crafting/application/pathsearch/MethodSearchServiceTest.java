package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class MethodSearchServiceTest {
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
