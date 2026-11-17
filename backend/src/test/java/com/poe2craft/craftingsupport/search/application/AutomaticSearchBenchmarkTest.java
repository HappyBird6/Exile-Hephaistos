package com.poe2craft.craftingsupport.search.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 자동·탐색·Benchmark 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Warm JVM, identical request and repeated reads. No timing assertions.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code benchmark()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
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
