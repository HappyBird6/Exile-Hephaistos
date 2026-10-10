package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.RenewalFirstHit;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PathSearchContractTest {
  @Test
  void sharedWireFixturesRoundTripAndExactOracleAreConsumed() throws Exception {
    var json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    var fixture =
        json.readTree(Path.of("../contracts/crafting-paths-v1/synthetic-fixtures.json").toFile());
    var types =
        Map.of(
            "createRequest",
            Create.class,
            "jobSnapshot",
            Snapshot.class,
            "graphPage",
            GraphPage.class,
            "mutationRequest",
            Mutation.class,
            "recoveryRequest",
            Recover.class,
            "problem",
            com.poe2craft.crafting.presentation.pathsearch.PathSearchErrors.Problem.class);
    var roundTrips = json.createArrayNode();
    for (var example : fixture.get("examples")) {
      if (example.has("expectedInvalid")) continue;
      var type = types.get(example.get("definition").asText());
      assertThat(type).isNotNull();
      var output =
          json.readTree(json.writeValueAsBytes(json.treeToValue(example.get("value"), type)));
      assertThat(output).isEqualTo(example.get("value"));
      roundTrips.add(example.deepCopy());
    }
    for (var point : fixture.path("oracle").path("observations")) {
      long n = Long.parseLong(point.get("attempts").asText());
      var result = RenewalFirstHit.at(Fraction.of(1, 2), Fraction.ZERO, 1, false, false, n, 65536);
      assertThat(Probability.of(result.hit()))
          .isEqualTo(json.treeToValue(point.get("lower"), Probability.class));
    }
    var ranked =
        fixture.get("examples").findValues("value").stream()
            .filter(v -> v.has("rankings") && v.path("recommendations").size() > 1)
            .toList();
    assertThat(ranked).isNotEmpty();
    for (var value : ranked) {
      var snapshot = json.treeToValue(value, Snapshot.class);
      var rankings =
          PathSearchCalculation.rankings(
              snapshot.recommendations(),
              snapshot.rankings().stream().map(Ranking::attempts).toList());
      for (int i = 0; i < rankings.size(); i++)
        assertThat(rankings.get(i).entries()).isEqualTo(snapshot.rankings().get(i).entries());
    }
    Files.createDirectories(Path.of("build/path-search"));
    json.writeValue(Path.of("build/path-search/fixture-roundtrips.json").toFile(), roundTrips);
  }
}
