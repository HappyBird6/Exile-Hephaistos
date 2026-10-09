package com.poe2craft.crafting.domain.goalfilter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalFixtureTest {
  private final ObjectMapper json = new ObjectMapper();

  private JsonNode fixture() throws Exception {
    return json.readTree(Path.of("../contracts/support-goal-filter-v1/fixtures.json").toFile());
  }

  @Test
  void allSharedEvaluationFixturesMatch() throws Exception {
    var fixture = fixture();
    for (var test : fixture.get("cases")) {
      var entries = new ArrayList<Entry>();
      for (int i = 0; i < test.get("entries").size(); i++) {
        var key = test.get("entries").get(i).asText();
        var node = (ObjectNode) fixture.get("entryTemplates").get(key).deepCopy();
        if (test.has("weights")) node.set("weight", test.get("weights").get(i));
        if (test.has("disabledEntries"))
          for (var disabled : test.get("disabledEntries"))
            if (disabled.asText().equals(key)) node.put("disabled", true);
        entries.add(json.treeToValue(node, Entry.class));
      }
      var group =
          new Group(
              "g",
              Type.valueOf(test.get("type").asText()),
              false,
              test.has("range") ? json.treeToValue(test.get("range"), Range.class) : null,
              entries);
      var goal = new GoalFilter(1, "goal-filter-fixture-v1", null, List.of(group));
      var observations = new HashMap<String, GoalEvaluator.Observation>();
      for (var stat : fixture.get("stats")) {
        var id = stat.get("statId").asText();
        if (!stat.get("kind").asText().equals("PSEUDO")) {
          var value = test.get("observedStats").get(id);
          observations.put(
              id,
              value == null
                  ? GoalEvaluator.Observation.absent()
                  : value.isNull()
                      ? GoalEvaluator.Observation.unknown()
                      : GoalEvaluator.Observation.present(value.decimalValue()));
        } else {
          var contributions = new ArrayList<GoalCatalog.Contribution>();
          for (var c : stat.get("contributions"))
            contributions.add(
                new GoalCatalog.Contribution(
                    c.get("statId").asText(), c.get("coefficient").decimalValue()));
          observations.put(id, GoalEvaluator.pseudo(contributions, observations));
        }
      }
      var evaluated = new GoalEvaluator().evaluate(goal, Status.MATCH, observations, List.of());
      assertThat(evaluated.status())
          .as(test.get("id").asText())
          .isEqualTo(Status.valueOf(test.get("expected").asText()));
      var result = evaluated.groups().getFirst();
      if (test.has("expectedValue"))
        assertThat(result.entries().getFirst().value())
            .isEqualByComparingTo(test.get("expectedValue").decimalValue());
      if (test.has("expectedScore"))
        assertThat(result.score()).isEqualByComparingTo(test.get("expectedScore").decimalValue());
      if (test.has("expectedCount"))
        assertThat(result.count()).isEqualTo(test.get("expectedCount").intValue());
    }
  }

  @Test
  void allSharedValidationFixturesMatch() throws Exception {
    var fixture = fixture();
    var context =
        json.treeToValue(
            fixture.get("apiExamples").get("validateRequest").get("context"), Context.class);
    var known = new HashMap<String, GoalCatalog.Stat>();
    for (var s : fixture.get("stats")) {
      var stat =
          new GoalCatalog.Stat(
              s.get("statId").asText(),
              "Synthetic",
              "percent",
              GoalCatalog.Kind.valueOf(s.get("kind").asText()),
              new GoalCatalog.Support(Capability.SUPPORTED, Capability.UNSUPPORTED, null),
              true,
              null,
              List.of(),
              List.of(),
              List.of());
      known.put(stat.statId(), stat);
    }
    var catalog =
        new GoalCatalog(
            1,
            "goal-filter-fixture-v1",
            context,
            List.of(),
            List.copyOf(known.values()),
            List.of());
    for (var test : fixture.get("validationCases")) {
      var goal =
          (ObjectNode) fixture.get("apiExamples").get("validateRequest").get("goal").deepCopy();
      var group = (ObjectNode) goal.get("groups").get(0);
      var row = (ObjectNode) group.get("entries").get(0);
      var mutation = test.get("mutation");
      if (mutation.has("version")) goal.set("version", mutation.get("version"));
      if (mutation.has("catalogVersion"))
        goal.set("catalogVersion", mutation.get("catalogVersion"));
      if (mutation.has("type")) {
        group.set("type", mutation.get("type"));
        group.set("range", mutation.get("range"));
      } else if (mutation.has("range")) row.set("range", mutation.get("range"));
      if (mutation.has("unit")) row.set("unit", mutation.get("unit"));
      if (mutation.has("statId")) row.set("statId", mutation.get("statId"));
      if (mutation.has("duplicateEntry")) {
        var copy = row.deepCopy();
        copy.put("id", "duplicate");
        ((com.fasterxml.jackson.databind.node.ArrayNode) group.get("entries")).add(copy);
      }
      if (mutation.has("disableAllEntries")) row.put("disabled", true);
      if (mutation.has("disableAllGroups")) group.put("disabled", true);
      var validated =
          new GoalValidator()
              .validate(context, json.treeToValue(goal, GoalFilter.class), catalog, known);
      assertThat(validated.valid()).as(test.get("id").asText()).isFalse();
      assertThat(validated.issues())
          .extracting(Issue::code)
          .contains(test.get("expectedCode").asText());
    }
  }

  @Test
  void pseudoPresenceAndSupportAreNotNumericZero() {
    var contributions =
        List.of(
            new GoalCatalog.Contribution("a", BigDecimal.ONE),
            new GoalCatalog.Contribution("b", BigDecimal.ONE));
    assertThat(
            GoalEvaluator.pseudo(
                    contributions,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.absent(),
                        "b",
                        GoalEvaluator.Observation.absent()))
                .presence())
        .isEqualTo(Presence.ABSENT);
    assertThat(
            GoalEvaluator.pseudo(
                    contributions,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.present(BigDecimal.ZERO),
                        "b",
                        GoalEvaluator.Observation.absent()))
                .presence())
        .isEqualTo(Presence.PRESENT);
    assertThat(
            GoalEvaluator.pseudo(
                    contributions,
                    Map.of(
                        "a",
                        GoalEvaluator.Observation.present(BigDecimal.TEN),
                        "b",
                        GoalEvaluator.Observation.unsupported()))
                .support())
        .isEqualTo(Capability.UNSUPPORTED);
  }
}
