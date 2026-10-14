package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class CertifiedMapProtocolTest {
  @Test
  void sharedDisplayFixturePreservesDiamondMassAlternativesAndAdaptiveRecommendations()
      throws Exception {
    var json = new ObjectMapper();
    var view =
        json.readValue(
            Path.of("../contracts/crafting-methods-v2/certified-diamond.json").toFile(),
            CertifiedMapProtocol.View.class);
    var exact =
        json.readTree(Path.of("../contracts/crafting-methods-v2/synthetic-diamond.json").toFile());
    assertThat(view.search().version()).isEqualTo(CertifiedMapProtocol.VERSION);
    assertThat(view.graph().revision()).isEqualTo(view.search().revision());
    assertThat(view.graph().methods()).hasSize(6);
    var graph = new LinkedHashMap<String, CertifiedOutcomeStrategy.State<String>>();
    for (var bucket : view.graph().buckets()) {
      var choices = new ArrayList<CertifiedOutcomeStrategy.Choice<String>>();
      for (var method : view.graph().methods()) {
        if (!method.sourceBucketId().equals(bucket.id())) continue;
        var law = new LinkedHashMap<String, CertifiedInterval>();
        for (var exit : method.points().getFirst().exits()) {
          assertThat(exit.probability().exact()).isTrue();
          var probability = fraction(exit.probability().lower());
          law.merge(
              exit.bucketId(),
              CertifiedInterval.enclose(probability, 64),
              (a, b) -> a.add(b).probability());
        }
        choices.add(
            new CertifiedOutcomeStrategy.Choice<>(method.id(), law, CertifiedInterval.zero(64)));
        var original =
            exact.path("graph").path("methods").findParents("id").stream()
                .filter(m -> m.path("id").asText().equals(method.id()))
                .findFirst()
                .orElseThrow();
        assertThat(method.points()).hasSize(original.path("points").size());
        for (int i = 0; i < method.points().size(); i++) {
          var point = method.points().get(i);
          assertThat(point.cycles())
              .isEqualTo(original.path("points").get(i).path("cycles").asInt());
          var mass = fraction(point.unresolved().lower());
          for (int j = 0; j < point.exits().size(); j++) {
            var value = point.exits().get(j);
            var p = original.path("points").get(i).path("exits").get(j).path("probability");
            assertThat(value.probability().lower().numerator())
                .isEqualTo(p.path("numerator").asText());
            assertThat(value.probability().lower().denominator())
                .isEqualTo(p.path("denominator").asText());
            mass = mass.add(fraction(value.probability().lower()));
          }
          assertThat(mass).isEqualTo(Fraction.ONE);
        }
      }
      graph.put(
          bucket.id(),
          new CertifiedOutcomeStrategy.State<>(bucket.goal().equals("MATCH"), false, choices));
    }
    var solver = new CertifiedOutcomeStrategy<>(graph, 64);
    for (int i = 0; i < 10; i++) solver.refine();
    assertThat(view.search().localRecommendations().getFirst().methodIds())
        .containsExactly("direct");
    assertThat(solver.result("root").methodIds()).containsExactly("branch");
    assertThat(solver.result("A").methodIds()).containsExactly("finish-A");
    assertThat(solver.result("B").methodIds()).containsExactly("finish-B");
    assertThat(view.search().strategies().getFirst().methodIds())
        .isEqualTo(solver.result("root").methodIds());
  }

  private static Fraction fraction(PathSearchProtocol.Probability value) {
    return new Fraction(new BigInteger(value.numerator()), new BigInteger(value.denominator()));
  }
}
