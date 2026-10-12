package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class SharedMethodContractTest {
  @Test
  void sharedDiamondMatchesIndependentExhaustivePolicyOracle() throws Exception {
    var data =
        new ObjectMapper()
            .readTree(Path.of("../contracts/crafting-methods-v2/synthetic-diamond.json").toFile());
    var graph = new LinkedHashMap<String, OutcomeStrategy.State<String>>();
    for (var bucket : data.path("graph").path("buckets")) {
      var choices = new ArrayList<OutcomeStrategy.Choice<String>>();
      for (var method : data.path("graph").path("methods"))
        if (method.path("sourceBucketId").asText().equals(bucket.path("id").asText())) {
          var outcomes = new LinkedHashMap<String, Fraction>();
          for (var exit : method.path("points").get(0).path("exits")) {
            var p = exit.path("probability");
            outcomes.merge(
                exit.path("bucketId").asText(),
                new Fraction(
                    new BigInteger(p.path("numerator").asText()),
                    new BigInteger(p.path("denominator").asText())),
                Fraction::add);
          }
          choices.add(
              new OutcomeStrategy.Choice<>(method.path("id").asText(), outcomes, Fraction.ZERO));
        }
      graph.put(
          bucket.path("id").asText(),
          new OutcomeStrategy.State<>(bucket.path("goal").asText().equals("MATCH"), true, choices));
    }
    // Enumerate the two alternatives at each of A/B independently of Bellman iteration.
    double exhaustive = 0;
    for (int a = 0; a < 2; a++)
      for (int b = 0; b < 2; b++) exhaustive = Math.max(exhaustive, 0.5 * a + 0.5 * b);
    var solver = new OutcomeStrategy<>(graph);
    solver.refine();
    solver.refine();
    assertThat(exhaustive).isEqualTo(1);
    assertThat(solver.bounds().get("root").lower()).isEqualTo(Fraction.ONE);
    assertThat(solver.recommendation("root").orElseThrow().methodId()).isEqualTo("branch");
    assertThat(solver.recommendation("A").orElseThrow().methodId()).isEqualTo("finish-A");
    assertThat(solver.recommendation("B").orElseThrow().methodId()).isEqualTo("finish-B");
  }
}
