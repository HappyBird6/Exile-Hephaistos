package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.search.application.*;
import com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy;
import com.poe2craft.craftingsupport.search.infrastructure.LocalAutomaticJobRepository;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** Real catalog, isolated disk persistence and JSON encoding; no timing assertions. */
@EnabledIfEnvironmentVariable(named = "AUTOMATIC_BENCHMARK", matches = "true")
class AutomaticRequestLatencyTest {
  @TempDir Path directory;

  @Test
  void firstRecommendationAndWholeRequestLifecycle() throws Exception {
    var f = new GoalDirectedPlannerScenariosTest();
    var root = f.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = f.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    var json = new ObjectMapper();
    for (int run = 0; run < 4; run++) {
      var request =
          new MethodSearchProtocol.Create(
              2,
              "latency-" + run,
              new BasicCurrencyState(
                  root,
                  new BasicCurrencyTransitions(
                          GoalDirectedPlannerScenariosTest.catalog, "latency-fixture")
                      .provenance()),
              goal,
              List.of());
      try (var service =
          new AutomaticSearchService(
              json,
              new LocalAutomaticJobRepository(directory.resolve("run-" + run)),
              GoalDirectedPlannerScenariosTest.policy,
              ignored ->
                  new AutomaticSearchService.Runtime(
                      f.kernel(root, goal), f.evaluation(root, goal), goal.toString()),
              Clock.systemUTC())) {
        long start = System.nanoTime();
        var view =
            service.create(
                "latency",
                new AutomaticSearchService.Input(
                    request,
                    false,
                    0,
                    new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, 10000, 50000)));
        long bytes = json.writeValueAsBytes(view).length;
        long created = System.nanoTime(), first = -1, firstPositive = -1;
        long advanceNanos = 0, queryNanos = 0;
        int advances = 0;
        for (int step = 0; step < 1000 && view.status().equals("READY"); step++) {
          long before = System.nanoTime();
          view =
              service.control(
                  "latency",
                  view.id(),
                  new AutomaticSearchService.Control(
                      "advance-" + step, view.revision(), view.generation(), "ADVANCE"));
          bytes += json.writeValueAsBytes(view).length;
          long returned = System.nanoTime();
          advanceNanos += returned - before;
          advances++;
          if (first < 0 && !view.candidates().isEmpty()) first = returned - start;
          if (firstPositive < 0
              && view.candidates().stream()
                  .anyMatch(c -> !c.success().lower().numerator().equals("0")))
            firstPositive = returned - start;
        }
        for (int query = 0; query < 30; query++) {
          long before = System.nanoTime();
          bytes += json.writeValueAsBytes(service.get("latency", view.id())).length;
          queryNanos += System.nanoTime() - before;
        }
        long total = System.nanoTime() - start;
        var row = new LinkedHashMap<String, Object>();
        row.put("run", run);
        row.put("createNanos", created - start);
        row.put("firstCandidateNanos", first);
        row.put("firstPositiveNanos", firstPositive);
        row.put("advanceNanos", advanceNanos);
        row.put("queryNanos", queryNanos);
        row.put("totalNanos", total);
        row.put("advances", advances);
        row.put("queries", 30);
        row.put("status", view.status());
        row.put("operations", view.operations());
        row.put("jsonBytes", bytes);
        row.put("diagnostics", service.diagnostics("latency", view.id()));
        System.out.println("LIFECYCLE " + json.writeValueAsString(row));
      }
    }
  }
}
