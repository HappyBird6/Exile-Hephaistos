package com.poe2craft.crafting.domain.pathsearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AlmostSureReachabilityTest {
  @Test
  void matchesIndependentEnumerationOfEveryPolicyOnSmallDecisionGraphs() {
    var random = new Random(812743);
    for (int fixture = 0; fixture < 200; fixture++) {
      var graph = new LinkedHashMap<Integer, State<Integer>>();
      graph.put(3, new State<>(true, true, List.of()));
      for (int state = 0; state < 3; state++) {
        var actions = new ArrayList<Choice<Integer>>();
        for (int action = 0; action < 2; action++) {
          var outcomes = new LinkedHashMap<Integer, Fraction>();
          for (int branch = 0; branch < 2; branch++)
            outcomes.merge(random.nextInt(4), Fraction.of(1, 2), Fraction::add);
          actions.add(new Choice<>("a" + action, outcomes, Fraction.ZERO));
        }
        graph.put(state, new State<>(false, true, actions));
      }
      var expected = new HashSet<Integer>();
      expected.add(3);
      // Enumerate every deterministic policy; Floyd closure detects all reachable bottom classes.
      for (int policy = 0; policy < 8; policy++) {
        var paths = new boolean[4][4];
        for (int state = 0; state < 4; state++) paths[state][state] = true;
        for (int state = 0; state < 3; state++)
          for (var target :
              graph.get(state).choices().get((policy >> state) & 1).outcomes().keySet())
            paths[state][target] = true;
        for (int k = 0; k < 4; k++)
          for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) paths[i][j] |= paths[i][k] && paths[k][j];
        for (int state = 0; state < 3; state++) {
          boolean succeeds = true;
          for (int reached = 0; reached < 4; reached++)
            if (paths[state][reached] && !paths[reached][3]) succeeds = false;
          if (succeeds) expected.add(state);
        }
      }
      var cursor = new AlmostSureReachability<>(graph);
      Optional<AlmostSureReachability.Solution<Integer>> result = Optional.empty();
      for (int step = 0; step < 100 && result.isEmpty(); step++) result = cursor.step();
      assertThat(result).as("fixture %s", fixture).isPresent();
      assertThat(result.orElseThrow().winning()).isEqualTo(expected);
      // Verify the returned joint policy, not only existence of some successful policy per state.
      var witnessed = new boolean[4][4];
      for (int state = 0; state < 4; state++) witnessed[state][state] = true;
      for (var entry : result.orElseThrow().policy().entrySet()) {
        var selected =
            graph.get(entry.getKey()).choices().stream()
                .filter(action -> action.methodId().equals(entry.getValue()))
                .findFirst()
                .orElseThrow();
        selected.outcomes().keySet().forEach(target -> witnessed[entry.getKey()][target] = true);
      }
      for (int k = 0; k < 4; k++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) witnessed[i][j] |= witnessed[i][k] && witnessed[k][j];
      for (int state : expected)
        for (int reached = 0; reached < 4; reached++)
          if (witnessed[state][reached]) assertThat(witnessed[reached][3]).isTrue();
    }
  }
}
