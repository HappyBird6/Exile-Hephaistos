package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticSessionPolicyTest {
  final AutomaticSearchServiceTest fixture = new AutomaticSearchServiceTest();

  @Test
  void readsReuseEvaluationAndAdvancingInvalidatesProvisionalLaws() {
    var repository = new AutomaticSearchServiceTest.Repository();
    try (var service = fixture.service(repository)) {
      var view = service.create("owner", fixture.request("cache"));
      var before = service.diagnostics("owner", view.id());
      for (int i = 0; i < 4; i++) assertThat(service.get("owner", view.id())).isEqualTo(view);
      var after = service.diagnostics("owner", view.id());
      assertThat(after.graph().evaluationBuilds()).isEqualTo(before.graph().evaluationBuilds());
      assertThat(after.graph().refinementIterations())
          .isEqualTo(before.graph().refinementIterations());
      var command =
          new AutomaticSearchService.Control(
              "advance", view.revision(), view.generation(), "ADVANCE");
      view = service.control("owner", view.id(), command);
      assertThat(service.diagnostics("owner", view.id()).graph().evaluationBuilds())
          .isGreaterThan(after.graph().evaluationBuilds());
      var advanced = service.diagnostics("owner", view.id());
      assertThat(service.control("owner", view.id(), command)).isEqualTo(view);
      assertThat(service.diagnostics("owner", view.id()).graph().refinementIterations())
          .isEqualTo(advanced.graph().refinementIterations());
    }
  }

  @Test
  void policyRoundTripsAndRequestIdCannotBeReusedWithDifferentSettings() throws Exception {
    var repository = new AutomaticSearchServiceTest.Repository();
    var policy = new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, 10000, 50000);
    var request = fixture.request("policy");
    AutomaticSearchService.View saved;
    try (var service = fixture.service(repository)) {
      saved = service.create("owner", new AutomaticSearchService.Input(request, false, 0, policy));
      assertThat(saved.rareOutcomePolicy()).isEqualTo(policy);
      assertThatThrownBy(() -> service.create("owner", request)).hasMessage("REQUEST_ID_REUSED");
      saved =
          service.control(
              "owner",
              saved.id(),
              new AutomaticSearchService.Control(
                  "go", saved.revision(), saved.generation(), "ADVANCE"));
    }
    try (var service = fixture.service(repository)) {
      var restored = service.get("owner", saved.id());
      assertThat(restored.rareOutcomePolicy()).isEqualTo(policy);
      assertThat(restored.methods()).isEqualTo(saved.methods());
      assertThat(restored.candidates()).isEqualTo(saved.candidates());
      assertThat(service.originalInput("owner", saved.id()).rareOutcomePolicy()).isEqualTo(policy);
    }
  }

  @Test
  void prePolicyCheckpointReadsWithDisabledDefaults() throws Exception {
    var repository = new AutomaticSearchServiceTest.Repository();
    String id;
    try (var service = fixture.service(repository)) {
      id = service.create("owner", fixture.request("old")).id();
    }
    var json = new ObjectMapper();
    var document = (ObjectNode) json.readTree(repository.read(id));
    var graph = (ObjectNode) document.get("graph");
    graph.put("version", "goal-directed-milestone-graph-v17-strict-exalted");
    graph.remove("rareOutcomePolicy");
    repository.save(id, json.writeValueAsBytes(document));
    try (var service = fixture.service(repository)) {
      assertThat(service.get("owner", id).rareOutcomePolicy())
          .isEqualTo(RareOutcomePolicy.disabled());
    }
  }
}
