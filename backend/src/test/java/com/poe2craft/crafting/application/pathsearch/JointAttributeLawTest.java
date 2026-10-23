package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class JointAttributeLawTest {
  @Test
  void inProgressV12KeepsProvisionalHorizonsAndUsageWithoutExecutingAnotherTransition()
      throws Exception {
    var fixture = new JointLawFixture();
    var json = new ObjectMapper();
    try (var graph = fixture.graph()) {
      for (int step = 0; step < 80 && !graph.finished(); step++) {
        graph.step();
        if (step % 5 != 0) continue;
        graph.publishProgress();
        var objective = json.writeValueAsString(fixture.goal);
        var checkpoint =
            json.readValue(
                json.writeValueAsString(graph.checkpoint(objective))
                    .replace(
                        AutomaticCraftingKernel.VERSION,
                        "automatic-finite-kernel-v6-declared-coupled-rolls"),
                AutomaticMethodGraph.Checkpoint.class);
        var bytes = json.writeValueAsString(checkpoint);
        var disk =
            new AutomaticSearchService.Disk(
                AutomaticSearchService.VERSION,
                "00000000-0000-4000-8000-000000000002",
                "synthetic-owner",
                null,
                1,
                1,
                "PAUSED",
                "RESOURCE_BUDGET",
                Long.MAX_VALUE,
                Map.of(),
                checkpoint);
        var runtime =
            new AutomaticSearchService.Runtime(fixture.kernel, fixture::evaluate, objective);
        var view = SavedAutomaticResultReader.read(disk, runtime);
        assertThat(view.canResume()).isFalse();
        assertThat(view.methods())
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(AutomaticSearchService.arrows(graph.nodes(), graph.methods()));
        assertThat(json.writeValueAsString(checkpoint)).isEqualTo(bytes);
      }
    }
  }

  @Test
  void oldV12JointSourcesAndConditionalMassRemainIdenticalInReadOnlyViews() throws Exception {
    var fixture = new JointLawFixture();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var graph = fixture.graph()) {
      fixture.exhaust(graph);
      var objective = json.writeValueAsString(fixture.goal);
      var encoded =
          json.writeValueAsString(graph.checkpoint(objective))
              .replace(
                  AutomaticCraftingKernel.VERSION, "automatic-scalar-kernel-v5-goal-acquisition");
      var checkpoint = json.readValue(encoded, AutomaticMethodGraph.Checkpoint.class);
      var beforeRead = json.writeValueAsString(checkpoint);
      var disk =
          new AutomaticSearchService.Disk(
              AutomaticSearchService.VERSION,
              "00000000-0000-4000-8000-000000000001",
              "synthetic-owner",
              null,
              1,
              1,
              "COMPLETE",
              "",
              Long.MAX_VALUE,
              Map.of(),
              checkpoint);
      var runtime =
          new AutomaticSearchService.Runtime(fixture.kernel, fixture::evaluate, objective);
      var view = SavedAutomaticResultReader.read(disk, runtime);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      assertThat(view.nodes()).isEqualTo(graph.nodes());
      assertThat(view.methods())
          .usingRecursiveComparison()
          .ignoringCollectionOrder()
          .isEqualTo(AutomaticSearchService.arrows(graph.nodes(), graph.methods()));
      assertThat(json.writeValueAsString(checkpoint)).isEqualTo(beforeRead);
    }
  }

  @Test
  void correlatedWeightedCheckpointIsOneRealStateAndNextTransitionRetainsItsJointSource()
      throws Exception {
    var fixture = new JointLawFixture();
    try (var graph = fixture.graph()) {
      fixture.exhaust(graph);
      assertThat(graph.finished()).isTrue();
      assertThat(graph.complete()).isTrue();
      var nodes = graph.nodes();
      var source =
          nodes.stream()
              .filter(
                  n ->
                      n.item().explicits().size() == 2
                          && fixture.value(n.item(), "a") > 0
                          && fixture.value(n.item(), "b") > 0
                          && n.progress().entries().getFirst() == GoalFilter.Status.MATCH)
              .toList();
      assertThat(source).hasSize(1);
      var joined = source.getFirst();
      assertThat(joined.members()).hasSize(3);
      assertThat(joined.attributes().values())
          .allSatisfy(
              options ->
                  assertThat(
                          options.stream()
                              .map(AutomaticMethodGraph.AttributeOption::min)
                              .collect(java.util.stream.Collectors.toSet()))
                      .containsExactlyInAnyOrder(1L, 4L));
      var arrows = AutomaticSearchService.arrows(nodes, graph.methods());
      var first =
          arrows.stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.initial().size() == 1
                          && m.initial().getFirst().currency() == WorkbenchCurrency.TRANSMUTATION
                          && m.cycle().size() == 1
                          && m.cycle().getFirst().currency() == WorkbenchCurrency.REGAL
                          && m.cycle().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var checkpoint =
          first.points().stream()
              .filter(p -> p.cycles() == 1)
              .findFirst()
              .orElseThrow()
              .outcomes()
              .stream()
              .filter(o -> Objects.equals(o.target(), joined.id()))
              .findFirst()
              .orElseThrow();
      var pairs = new HashSet<List<Long>>();
      for (var component : checkpoint.conditional()) {
        assertThat(component.retainedFromSource()).isEmpty();
        long a = component.local().stream().mapToLong(m -> m.values().getOrDefault("a", 0L)).sum(),
            b = component.local().stream().mapToLong(m -> m.values().getOrDefault("b", 0L)).sum();
        pairs.add(List.of(a, b));
        assertContains(component.probability(), Fraction.of(2, 15));
      }
      assertThat(pairs)
          .containsExactlyInAnyOrder(List.of(1L, 4L), List.of(4L, 1L), List.of(4L, 4L));
      assertThat(pairs).doesNotContain(List.of(1L, 1L));
      assertContains(checkpoint.probability(), Fraction.of(2, 5));
      var next =
          arrows.stream()
              .filter(
                  m ->
                      m.source() == joined.id()
                          && m.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED
                          && m.cycle().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var goal =
          next.points().stream()
              .filter(p -> p.cycles() == 1)
              .findFirst()
              .orElseThrow()
              .outcomes()
              .stream()
              .filter(o -> o.kind().equals("GOAL"))
              .findFirst()
              .orElseThrow();
      assertContains(goal.probability(), Fraction.ONE);
      assertThat(goal.conditional()).hasSize(1);
      var transform = goal.conditional().getFirst();
      assertThat(transform.retainedFromSource()).hasSize(2);
      assertThat(transform.local()).hasSize(1);
      assertThat(transform.local().getFirst().values()).containsEntry("c", 1L);
      var target = nodes.stream().filter(n -> n.id() == goal.target()).findFirst().orElseThrow();
      assertThat(target.joint())
          .anyMatch(o -> o.source() == joined.id() && o.retained().size() == 2);
      // Source a/b coordinates stay paired through the projection, not multiplied
      // into independent marginals (which would invent the impossible (1,1) pair).
      var projectedPairs = new HashSet<List<Long>>();
      for (var member : joined.members()) {
        var retained = new ArrayList<com.poe2craft.item.ModifierInstance>();
        for (var field : transform.retainedFromSource()) {
          var canonical =
              joined.item().explicits().stream()
                  .filter(m -> m.modifierId().equals(field))
                  .findFirst()
                  .orElseThrow();
          retained.add(
              member.item().explicits().stream()
                  .filter(
                      m ->
                          fixture
                              .kernel
                              .attributeKey(m)
                              .equals(fixture.kernel.attributeKey(canonical)))
                  .findFirst()
                  .orElseThrow());
        }
        retained.addAll(transform.local());
        long a = retained.stream().mapToLong(m -> m.values().getOrDefault("a", 0L)).sum(),
            b = retained.stream().mapToLong(m -> m.values().getOrDefault("b", 0L)).sum();
        projectedPairs.add(List.of(a, b));
      }
      assertThat(projectedPairs).isEqualTo(pairs);
      assertThat(projectedPairs).doesNotContain(List.of(1L, 1L));
      var different =
          nodes.stream()
              .filter(
                  n ->
                      n.item().explicits().size() == 2
                          && fixture.value(n.item(), "a") > 0
                          && fixture.value(n.item(), "c") > 0)
              .toList();
      assertThat(different).hasSize(2); // Here the weighted goal makes future success differ.
      for (var input : different) {
        var addition =
            arrows.stream()
                .filter(
                    m ->
                        m.source() == input.id()
                            && m.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED
                            && m.cycle().getFirst().omens().isEmpty())
                .findFirst()
                .orElseThrow();
        var outcome =
            addition.points().stream()
                .filter(p -> p.cycles() == 1)
                .findFirst()
                .orElseThrow()
                .outcomes()
                .stream()
                .filter(o -> o.kind().equals("GOAL"))
                .findFirst()
                .orElseThrow();
        assertContains(
            outcome.probability(),
            fixture.value(input.item(), "a") == 1 ? Fraction.of(1, 2) : Fraction.ONE);
      }
      var json = new ObjectMapper();
      var cursor =
          json.readValue(
              json.writeValueAsBytes(graph.checkpoint(fixture.goal.toString())),
              AutomaticMethodGraph.Checkpoint.class);
      try (var restored =
          AutomaticMethodGraph.restore(
              cursor, fixture.goal.toString(), fixture.policy, fixture.kernel, fixture::evaluate)) {
        assertThat(restored.nodes()).isEqualTo(nodes);
        assertThat(AutomaticSearchService.arrows(restored.nodes(), restored.methods()))
            .isEqualTo(arrows);
      }
      System.out.println(
          "JOINT_WEIGHTED|nodes="
              + nodes.size()
              + "|sourceMembers="
              + joined.members().size()
              + "|pairs="
              + pairs
              + "|checkpoint=2/5|next=1");
    }
  }

  static void assertContains(CertifiedMethodProtocol.Bounds bounds, Fraction exact) {
    var lower = bounds.lower();
    var upper = bounds.upper();
    assertThat(
            new java.math.BigInteger(lower.numerator())
                .multiply(exact.denominator())
                .compareTo(
                    exact.numerator().multiply(new java.math.BigInteger(lower.denominator()))))
        .isLessThanOrEqualTo(0);
    assertThat(
            new java.math.BigInteger(upper.numerator())
                .multiply(exact.denominator())
                .compareTo(
                    exact.numerator().multiply(new java.math.BigInteger(upper.denominator()))))
        .isGreaterThanOrEqualTo(0);
  }
}
