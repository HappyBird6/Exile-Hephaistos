package com.poe2craft.bootstrap;

import static com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.infrastructure.LocalAutomaticJobRepository;
import com.poe2craft.item.ItemState;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TerminalContinuationApiTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();
  @TempDir Path directory;

  AutomaticSearchService.Control command(AutomaticSearchService.View view, String operation) {
    return new AutomaticSearchService.Control(
        UUID.randomUUID().toString(), view.revision(), view.generation(), operation);
  }

  @Test
  void cancelsResumesAndReloadsSharedConcreteOutcomesWithMeasuredRequestLatency() throws Exception {
    var root = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var goal =
        f.goal(root, Map.of(LIFE, 1L, CHAOS, 1L, COLD, 1L, "base_fire_damage_resistance_%", 1L));
    var request =
        new MethodSearchProtocol.Create(
            2,
            UUID.randomUUID().toString(),
            new BasicCurrencyState(
                root, new BasicCurrencyTransitions(catalog, "terminal-regression").provenance()),
            goal,
            List.of());
    var json = new ObjectMapper();
    var repository = new LocalAutomaticJobRepository(directory);
    java.util.function.Function<MethodSearchProtocol.Create, AutomaticSearchService.Runtime>
        runtime =
            r ->
                new AutomaticSearchService.Runtime(
                    f.kernel(root, goal), f.evaluation(root, goal), goal.toString());
    AutomaticSearchService.View finalView;
    long maxAdvance = 0, cancelMillis;
    int advances = 0, resumes = 0;
    try (var service =
            new AutomaticSearchService(json, repository, policy, runtime, Clock.systemUTC());
        var executor = Executors.newSingleThreadExecutor()) {
      var initial = service.create("terminal-api", request);
      var entered = new CountDownLatch(1);
      var pending =
          executor.submit(
              () -> {
                entered.countDown();
                return service.control("terminal-api", initial.id(), command(initial, "ADVANCE"));
              });
      entered.await();
      Thread.sleep(10);
      long cancelStarted = System.nanoTime();
      var current = service.get("terminal-api", initial.id());
      current = service.control("terminal-api", current.id(), command(current, "CANCEL"));
      cancelMillis = (System.nanoTime() - cancelStarted) / 1000000;
      pending.get();
      assertThat(current.status()).isEqualTo("CANCELLED");
      current = service.control("terminal-api", current.id(), command(current, "RESUME"));
      for (int i = 0; i < 1000; i++) {
        String operation =
            current.status().equals("READY")
                ? "ADVANCE"
                : current.status().equals("PAUSED") && current.canResume() ? "RESUME" : null;
        if (operation == null) break;
        long started = System.nanoTime();
        current = service.control("terminal-api", current.id(), command(current, operation));
        if (operation.equals("ADVANCE")) {
          advances++;
          maxAdvance = Math.max(maxAdvance, (System.nanoTime() - started) / 1000000);
        } else resumes++;
      }
      assertThat(current.status()).isEqualTo("COMPLETE");
      assertThat(current.resources().methodExecutions()).isLessThan(current.methods().size());
      finalView = current;
    }
    try (var reloaded =
        new AutomaticSearchService(json, repository, policy, runtime, Clock.systemUTC())) {
      var actual = reloaded.get("terminal-api", finalView.id());
      assertThat(actual.nodes()).isEqualTo(finalView.nodes());
      assertThat(actual.methods()).hasSameSizeAs(finalView.methods());
      for (int i = 0; i < actual.methods().size(); i++)
        assertThat(actual.methods().get(i))
            .as("restored method " + i)
            .isEqualTo(finalView.methods().get(i));
      assertThat(actual.complete()).isTrue();
    }
    var output = Path.of("build/terminal-fixtures");
    java.nio.file.Files.createDirectories(output);
    json.writeValue(output.resolve("four-goal-api.json").toFile(), finalView);
    json.writeValue(output.resolve("four-goal-request.json").toFile(), request);
    System.out.println(
        "TERMINAL_API|advances="
            + advances
            + "|resumes="
            + resumes
            + "|maxAdvanceMs="
            + maxAdvance
            + "|cancelMs="
            + cancelMillis
            + "|resources="
            + finalView.resources());
  }
}
