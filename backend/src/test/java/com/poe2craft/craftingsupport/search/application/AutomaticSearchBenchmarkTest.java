package com.poe2craft.craftingsupport.search.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Warm JVM, identical request and repeated reads. No timing assertions. */
@EnabledIfEnvironmentVariable(named = "AUTOMATIC_BENCHMARK", matches = "true")
class AutomaticSearchBenchmarkTest extends AutomaticSearchServiceTest {
  @Test
  void benchmark() {
    for (int run = 0; run < 4; run++) {
      var repository = new Repository();
      try (var service = service(repository)) {
        var view = service.create("owner", request("benchmark"));
        view =
            service.control(
                "owner",
                view.id(),
                new AutomaticSearchService.Control(
                    "advance", view.revision(), view.generation(), "ADVANCE"));
        long start = System.nanoTime();
        for (int i = 0; i < 30; i++) service.get("owner", view.id());
        System.out.println(
            "BENCH run="
                + run
                + " readsNanos="
                + (System.nanoTime() - start)
                + " "
                + service.diagnostics("owner", view.id()));
      }
    }
  }
}
