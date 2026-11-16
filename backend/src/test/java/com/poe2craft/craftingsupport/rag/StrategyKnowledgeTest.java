package com.poe2craft.craftingsupport.rag;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.rag.application.*;
import com.poe2craft.craftingsupport.rag.infrastructure.BundledStrategyKnowledge;
import java.util.*;
import org.junit.jupiter.api.Test;

class StrategyKnowledgeTest {
  final StrategyKnowledge knowledge = BundledStrategyKnowledge.load(new ObjectMapper());

  @Test
  void sourceChangesRequireReviewOfExtractedStrategyDocuments() throws Exception {
    var root = java.nio.file.Path.of("..");
    var manifest =
        new ObjectMapper().readTree(root.resolve("docs/crafting-support-rag/corpus.json").toFile());
    for (var source : manifest.path("sources")) {
      var path = source.path("path").asText();
      assertThat(StrategyKnowledge.hash(java.nio.file.Files.readString(root.resolve(path))))
          .as("Review strategy documents after changing %s", path)
          .isEqualTo(source.path("sha256").asText());
    }
  }

  @Test
  void packagedCorpusValidatesHashesAndExcludesHistoricalNotes() {
    var all = knowledge.select(StrategyKnowledge.Mode.ALL, "", 3);
    assertThat(all.documents()).hasSize(8).allMatch(d -> !d.id().contains("reference"));
    assertThat(all.documents()).noneMatch(d -> d.text().contains("46개 통과"));
  }

  @Test
  void retrievalIncludesMandatoryAndTransitiveDocumentsInManifestOrder() {
    var retrieved = knowledge.select(StrategyKnowledge.Mode.RETRIEVED, "훼손 드러내기 DESECRATE", 1);
    assertThat(retrieved.hits()).containsExactly("support.desecration");
    assertThat(retrieved.documents().stream().map(StrategyKnowledge.Document::id))
        .containsExactly(
            "support.common-policy",
            "support.goal-progress",
            "support.first-affix-chaos",
            "support.dummy-replacement",
            "support.desecration");
  }

  @Test
  void noLexicalHitDoesNotPretendAllStrategiesWereRetrieved() {
    assertThat(knowledge.select(StrategyKnowledge.Mode.RETRIEVED, "unfindablezzzz", 2).documents())
        .hasSize(2);
  }

  @Test
  void invalidHashMissingDependencyAndCycleAreRejected() {
    assertThatThrownBy(() -> new StrategyKnowledge("v", "p", List.of(doc("a", List.of(), "bad"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new StrategyKnowledge(
                    "v",
                    "p",
                    List.of(doc("a", List.of("missing"), StrategyKnowledge.hash("text")))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new StrategyKnowledge(
                    "v", "p", List.of(doc("a", List.of("a"), StrategyKnowledge.hash("text")))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private StrategyKnowledge.Document doc(String id, List<String> requires, String hash) {
    return new StrategyKnowledge.Document(id, "text", List.of(), requires, true, hash);
  }
}
