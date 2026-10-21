package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.infrastructure.LocalAutomaticJobRepository;
import com.poe2craft.item.ItemState;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class EmptyRareApiRegressionTest {
  @TempDir Path directory;

  @Test
  @Timeout(120)
  void completedApiRetainsPreparationRetryProbabilityAndOnlyConcreteGoalTargets() throws Exception {
    var f = new GoalDirectedPlannerScenariosTest();
    var root = f.item(ItemState.Rarity.RARE, 82, List.of());
    var goal = f.goal(root, Map.of("minion_skill_gem_level_+", 3L));
    var request =
        new MethodSearchProtocol.Create(
            2,
            UUID.randomUUID().toString(),
            new BasicCurrencyState(
                root,
                new BasicCurrencyTransitions(GoalDirectedPlannerScenariosTest.catalog, "regression")
                    .provenance()),
            goal,
            List.of());
    var json = new ObjectMapper();
    var kernel = f.kernel(root, goal);
    var evaluation = f.evaluation(root, goal);
    long started = System.nanoTime();
    try (var service =
        new AutomaticSearchService(
            json,
            new LocalAutomaticJobRepository(directory),
            GoalDirectedPlannerScenariosTest.policy,
            r -> new AutomaticSearchService.Runtime(kernel, evaluation, goal.toString()),
            Clock.systemUTC())) {
      var view = service.create("synthetic-visual", request);
      for (int i = 0; i < 500 && view.status().equals("READY"); i++)
        view =
            service.control(
                "synthetic-visual",
                view.id(),
                new AutomaticSearchService.Control(
                    UUID.randomUUID().toString(), view.revision(), view.generation(), "ADVANCE"));
      assertThat(view.status()).isEqualTo("COMPLETE");
      assertThat(view.complete()).isTrue();
      var prepared =
          view.methods().stream()
              .filter(
                  m ->
                      !m.initial().isEmpty()
                          && m.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS
                          && m.cycle().getFirst().omens().isEmpty())
              .toList();
      // v4 intentionally offers only plain Exalted when it prepares a Chaos roll.
      assertThat(prepared).hasSize(1).allMatch(m -> m.executionMode().equals("REPEATABLE"));
      assertThat(prepared.getFirst().initial())
          .singleElement()
          .satisfies(
              a -> {
                assertThat(a.currency()).isEqualTo(WorkbenchCurrency.EXALTED);
                assertThat(a.omens()).isEmpty();
              });
      assertThat(prepared)
          .allSatisfy(
              m -> {
                var point =
                    m.points().stream().filter(p -> p.cycles() == 100).findFirst().orElseThrow();
                assertThat(point.complete()).isTrue();
                assertThat(point.outcomes())
                    .anyMatch(o -> o.kind().equals("GOAL") && o.target() != null);
                assertThat(point.outcomes())
                    .anyMatch(o -> o.kind().equals("RETRY") && o.target() == null);
                assertThat(point.initialUses()).containsKey("EXALTED");
                assertThat(point.cycleUses()).containsKey("CHAOS");
              });
      var output = Path.of("build/empty-fixtures");
      Files.createDirectories(output);
      json.writeValue(output.resolve("empty-rare-api.json").toFile(), view);
      json.writeValue(output.resolve("empty-rare-request.json").toFile(), request);
      System.out.println(
          "EMPTY_API status="
              + view.status()
              + " complete="
              + view.complete()
              + " nodes="
              + view.nodes().size()
              + " methods="
              + view.methods().size()
              + " operations="
              + view.operations()
              + " retained="
              + view.retainedStates()
              + " elapsedMillis="
              + (System.nanoTime() - started) / 1_000_000);
    }
  }
}
