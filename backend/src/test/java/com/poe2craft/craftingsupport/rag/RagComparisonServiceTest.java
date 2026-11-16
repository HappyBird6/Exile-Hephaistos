package com.poe2craft.craftingsupport.rag;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.rag.application.*;
import com.poe2craft.craftingsupport.search.domain.StrategyEvidence;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RagComparisonServiceTest {
  final RagFixture f = new RagFixture();

  StrategyModel model(
      java.util.function.Function<StrategyEvidence.Facts, StrategyModel.Result> answer) {
    return new StrategyModel() {
      public boolean configured() {
        return true;
      }

      public Result propose(
          StrategyKnowledge.Selection docs,
          StrategyEvidence.Facts facts,
          com.poe2craft.craftingsupport.goal.domain.GoalFilter goal,
          String locale) {
        return answer.apply(facts);
      }
    };
  }

  @Test
  void exactProposalsUseActualEngineAndRepeatedInputDoesNotCallProvider() {
    var calls = new AtomicInteger();
    var service =
        f.service(
            model(
                facts -> {
                  calls.incrementAndGet();
                  return f.answer(facts);
                }),
            Map.of());
    var first = service.compare(f.input());
    assertThat(first.candidateMatch()).isTrue();
    assertThat(first.executionMatch()).isTrue();
    assertThat(first.executions()).isNotEmpty().allMatch(e -> e.status().equals("MATCH"));
    assertThat(first.executions().getFirst().reference().initialUses()).isNotNull();
    assertThat(service.compare(f.input()).cached()).isTrue();
    assertThat(calls).hasValue(1);
  }

  @Test
  void orderingAndMissingCandidatesRemainVisibleWithoutFallback() {
    var service =
        f.service(
            model(
                facts -> {
                  var answer = f.answer(facts);
                  var choices = new ArrayList<>(answer.choices());
                  Collections.reverse(choices);
                  return new StrategyModel.Result("COMPLETE", choices, 1, 1);
                }),
            Map.of());
    var report = service.compare(f.input());
    assertThat(report.reference()).hasSizeGreaterThan(1);
    assertThat(report.candidateMatch()).isFalse();
    assertThat(report.orderMatches()).isFalse();
    assertThat(report.missing()).isEmpty();
    assertThat(report.extra()).isEmpty();
    var missing =
        f.service(model(facts -> new StrategyModel.Result("COMPLETE", List.of(), 1, 1)), Map.of())
            .compare(f.input());
    assertThat(missing.strategies()).isEmpty();
    assertThat(missing.missing()).hasSize(missing.reference().size());
  }

  @Test
  void fabricatedActionsCitationsDuplicatesAndStoppingAreNotSilentlyRepaired() {
    for (String invalid : List.of("action", "citation", "stop", "duplicate")) {
      var service =
          f.service(
              model(
                  facts -> {
                    var row = f.answer(facts).choices().getFirst();
                    var proposal =
                        new StrategyModel.Proposal(
                            row.initial(),
                            invalid.equals("action") ? List.of("unknown") : row.cycle(),
                            invalid.equals("citation")
                                ? List.of("support.reference.abyss-strategies")
                                : row.documents(),
                            invalid.equals("stop") ? "UNDECIDED" : row.preparationSuccess(),
                            "test");
                    return new StrategyModel.Result(
                        "COMPLETE",
                        invalid.equals("duplicate")
                            ? List.of(proposal, proposal)
                            : List.of(proposal),
                        1,
                        1);
                  }),
              Map.of());
      var report = service.compare(f.input());
      assertThat(report.validationErrors()).isNotEmpty();
      assertThat(report.candidateMatch()).isFalse();
      assertThat(report.executionMatch()).isFalse();
    }
  }

  @Test
  void insufficientEvidenceAndResourceLimitsAreNeverMatches() {
    var report =
        f.service(
                model(facts -> new StrategyModel.Result("INSUFFICIENT_EVIDENCE", List.of(), 1, 1)),
                Map.of())
            .compare(f.input());
    assertThat(report.candidateMatch()).isFalse();
    assertThat(report.strategies()).isEmpty();
    var bounded =
        f.service(model(f::answer), Map.of("executionSteps", 1, "primitiveOutcomes", 1))
            .compare(f.input());
    assertThat(bounded.candidateMatch()).isTrue();
    assertThat(bounded.executionMatch()).isFalse();
    assertThatThrownBy(
            () ->
                f.service(
                        model(
                            facts -> {
                              throw new AssertionError("Must not call model");
                            }),
                        Map.of("factOperations", 1))
                    .compare(f.input()))
        .isInstanceOf(RagFailure.class)
        .hasMessage("RAG_FACT_BUDGET");
  }

  @Test
  void invalidOptionalCorpusDisablesOnlyTheExperiment() {
    var service =
        new RagComparisonService(
            f.runtimes,
            f.policy,
            null,
            model(f::answer),
            f.settings(Map.of()),
            f.json,
            java.time.Clock.systemUTC());
    assertThat(service.availability().configured()).isFalse();
    assertThatThrownBy(() -> service.compare(f.input())).hasMessage("RAG_CORPUS_INVALID");
  }

  @Test
  void featureOffAndMissingKeyDoNotRunEngineOrProvider() {
    assertThatThrownBy(
            () ->
                f.service(
                        model(
                            facts -> {
                              throw new AssertionError();
                            }),
                        Map.of("enabled", false))
                    .compare(f.input()))
        .hasMessage("RAG_DISABLED");
    var noKey =
        new StrategyModel() {
          public boolean configured() {
            return false;
          }

          public Result propose(
              StrategyKnowledge.Selection d,
              StrategyEvidence.Facts f,
              com.poe2craft.craftingsupport.goal.domain.GoalFilter goal,
              String l) {
            throw new AssertionError();
          }
        };
    assertThatThrownBy(() -> f.service(noKey, Map.of()).compare(f.input()))
        .hasMessage("RAG_KEY_MISSING");
  }

  @Test
  void concurrentRequestIsRejectedAndFailureReleasesAdmission() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var service =
        f.service(
            model(
                facts -> {
                  entered.countDown();
                  try {
                    assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                  } catch (InterruptedException e) {
                    throw new AssertionError(e);
                  }
                  throw new RagFailure(502, "RAG_PROVIDER_REJECTED");
                }),
            Map.of());
    try (var worker = Executors.newSingleThreadExecutor()) {
      var pending = worker.submit(() -> service.compare(f.input()));
      assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> service.compare(f.input())).hasMessage("RAG_BUSY");
      release.countDown();
      assertThatThrownBy(pending::get).hasCauseInstanceOf(RagFailure.class);
      assertThatThrownBy(() -> service.compare(f.input())).hasMessage("RAG_PROVIDER_REJECTED");
    }
  }

  @Test
  void metadataAndExcludedModelArgumentsNeverContainTheReferenceAnswer() throws Exception {
    var tree = f.json.valueToTree(f.facts());
    assertThat(tree.has("choices")).isFalse();
    assertThat(tree.has("reference")).isFalse();
    assertThat(f.expected()).isNotEmpty();
    assertThat(f.facts().preparations()).isNotEmpty();
  }
}
