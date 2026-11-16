package com.poe2craft.craftingsupport.rag.application;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.search.application.*;
import com.poe2craft.craftingsupport.search.domain.*;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/** Explicit, opt-in test comparison. No production search jobs or private libraries are changed. */
public final class RagComparisonService {
  public static final String VERSION = "rag-root-comparison-v1";

  public record Input(
      MethodSearchProtocol.Create request,
      boolean initialStage,
      int abstractQuality,
      StrategyKnowledge.Mode mode,
      String locale) {}

  public record Availability(
      boolean enabled, boolean configured, String model, String corpusVersion) {}

  public record Candidate(
      List<AutomaticCraftingAction> initial,
      List<AutomaticCraftingAction> cycle,
      List<String> documents,
      String explanation,
      String preparationSuccess) {}

  public record Execution(
      String status,
      long cycles,
      Map<String, CertifiedInterval> exits,
      CertifiedInterval active,
      Map<String, CertifiedInterval> initialUses,
      Map<String, CertifiedInterval> cycleUses,
      String outcomeDigest) {}

  public record Comparison(
      int referenceIndex,
      int proposedIndex,
      String status,
      Execution reference,
      Execution proposed) {}

  public record Report(
      String version,
      String model,
      String corpusVersion,
      String corpusDigest,
      AutomaticCraftingKernel.Identity engine,
      StrategyKnowledge.Mode mode,
      List<String> documents,
      List<String> retrievalHits,
      String proposalStatus,
      List<Candidate> strategies,
      List<GoalDirectedPlanner.Choice> reference,
      List<Integer> missing,
      List<Integer> extra,
      boolean orderMatches,
      List<String> validationErrors,
      List<Comparison> executions,
      boolean candidateMatch,
      boolean executionMatch,
      long inputTokens,
      long outputTokens,
      boolean cached,
      long elapsedMillis) {
    Report asCached() {
      return new Report(
          version,
          model,
          corpusVersion,
          corpusDigest,
          engine,
          mode,
          documents,
          retrievalHits,
          proposalStatus,
          strategies,
          reference,
          missing,
          extra,
          orderMatches,
          validationErrors,
          executions,
          candidateMatch,
          executionMatch,
          inputTokens,
          outputTokens,
          true,
          0);
    }
  }

  private record Cached(Instant until, Report report) {}

  private record Run(Execution view, Object observation) {}

  private final AutomaticRuntimeFactory runtimes;
  private final AutomaticCurrencyPolicy policy;
  private final StrategyKnowledge knowledge;
  private final StrategyModel model;
  private final BackendSettings.SupportRag settings;
  private final ObjectMapper json;
  private final Clock clock;
  private final Semaphore admission = new Semaphore(1);
  private final Map<String, Cached> cache = new LinkedHashMap<>();

  public RagComparisonService(
      AutomaticRuntimeFactory runtimes,
      AutomaticCurrencyPolicy policy,
      StrategyKnowledge knowledge,
      StrategyModel model,
      BackendSettings.SupportRag settings,
      ObjectMapper json,
      Clock clock) {
    this.runtimes = runtimes;
    this.policy = policy;
    this.knowledge = knowledge;
    this.model = model;
    this.settings = settings;
    this.json = json;
    this.clock = clock;
  }

  public Availability availability() {
    return new Availability(
        settings.enabled(),
        model.configured() && knowledge != null,
        settings.model(),
        knowledge == null ? "unavailable" : knowledge.version());
  }

  public Report compare(Input input) {
    validate(input);
    if (!model.configured()) throw new RagFailure(503, "RAG_KEY_MISSING");
    if (!admission.tryAcquire()) throw new RagFailure(429, "RAG_BUSY");
    long began = System.nanoTime();
    try {
      var runtime = runtimes.create(input.request());
      var kernel = runtime.kernel();
      if (!knowledge.policyVersion().equals(kernel.identity().policyVersion()))
        throw new RagFailure(409, "RAG_CORPUS_POLICY_MISMATCH");
      var root = input.request().start().item();
      var key =
          StrategyKnowledge.hash(
              canonical(
                  Map.of(
                      "version",
                      VERSION,
                      "model",
                      settings.model(),
                      "corpus",
                      knowledge.digest(),
                      "engine",
                      kernel.identity(),
                      "start",
                      root,
                      "goal",
                      input.request().goal(),
                      "initial",
                      input.initialStage(),
                      "quality",
                      input.abstractQuality(),
                      "mode",
                      input.mode(),
                      "locale",
                      input.locale())));
      cache.entrySet().removeIf(e -> !e.getValue().until().isAfter(clock.instant()));
      var saved = cache.get(key);
      if (saved != null) return saved.report().asCached();
      var context = context(input, runtime);
      var facts = context.facts();
      var selection = context.selection();
      var evaluate = context.evaluate();
      var inventory = context.inventory();
      long deadline = System.nanoTime() + settings.computeTimeout().toNanos();
      // The reference answer is never included in facts, retrieval query or the provider prompt.
      var reference =
          facts.context().nodeComplete()
              ? List.<GoalDirectedPlanner.Choice>of()
              : new GoalDirectedPlanner(kernel, evaluate, inventory)
                  .choices(root, input.initialStage());
      checkDeadline(deadline);
      var answer = model.propose(selection, facts, input.request().goal(), input.locale());
      deadline = System.nanoTime() + settings.computeTimeout().toNanos();
      var decoded = decode(context, answer);
      var errors = decoded.errors();
      var proposed = decoded.choices();
      var strategies = decoded.strategies();
      var selectedIds = selection.documents().stream().map(StrategyKnowledge.Document::id).toList();
      var missing = new ArrayList<Integer>();
      var extra = new ArrayList<Integer>();
      for (int i = 0; i < reference.size(); i++)
        if (!proposed.contains(reference.get(i))) missing.add(i);
      for (int i = 0; i < proposed.size(); i++)
        if (!reference.contains(proposed.get(i))) extra.add(i);
      boolean order = reference.equals(proposed);
      boolean candidateMatch = answer.status().equals("COMPLETE") && errors.isEmpty() && order;
      var comparisons = new ArrayList<Comparison>();
      try (var graph =
          new AutomaticMethodGraph(
              root,
              policy,
              kernel,
              runtime.evaluate(),
              CertifiedInterval.Precision.standard(),
              1,
              1,
              settings.primitiveOutcomes(),
              settings.primitiveOutcomes(),
              input.abstractQuality() == 40,
              input.abstractQuality())) {
        for (int i = 0; i < reference.size(); i++) {
          int index = proposed.indexOf(reference.get(i));
          if (index < 0) continue;
          if (System.nanoTime() > deadline) {
            comparisons.add(new Comparison(i, index, "NOT_RUN_BUDGET", null, null));
            continue;
          }
          var expected = execute(graph, reference.get(i), deadline);
          var actual = execute(graph, proposed.get(index), deadline);
          boolean complete =
              expected.view().status().equals("COMPLETE")
                  && actual.view().status().equals("COMPLETE");
          comparisons.add(
              new Comparison(
                  i,
                  index,
                  !complete
                      ? "INCONCLUSIVE"
                      : expected.observation().equals(actual.observation()) ? "MATCH" : "DIFFERENT",
                  expected.view(),
                  actual.view()));
        }
      }
      var report =
          new Report(
              VERSION,
              settings.model(),
              knowledge.version(),
              knowledge.digest(),
              kernel.identity(),
              input.mode(),
              selectedIds,
              selection.hits(),
              answer.status(),
              List.copyOf(strategies),
              reference,
              List.copyOf(missing),
              List.copyOf(extra),
              order,
              List.copyOf(errors),
              List.copyOf(comparisons),
              candidateMatch,
              candidateMatch
                  && comparisons.size() == reference.size()
                  && comparisons.stream().allMatch(c -> c.status().equals("MATCH")),
              answer.inputTokens(),
              answer.outputTokens(),
              false,
              (System.nanoTime() - began) / 1_000_000);
      if (cache.size() >= settings.cacheEntries()) cache.remove(cache.keySet().iterator().next());
      cache.put(key, new Cached(clock.instant().plus(settings.cacheTtl()), report));
      return report;
    } catch (StrategyEvidence.LimitReached error) {
      throw new RagFailure(422, "RAG_FACT_BUDGET");
    } finally {
      admission.release();
    }
  }

  public record PromptPreview(String promptId, StrategyPrompt prompt) {}

  public record GenerateInput(String promptId) {}

  public record StrategyResponse(String status, List<Candidate> strategies) {}

  private record Context(
      Input input,
      AutomaticSearchService.Runtime runtime,
      StrategyEvidence.Facts facts,
      StrategyKnowledge.Selection selection,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      List<AutomaticCraftingAction> inventory) {}

  private record Decoded(
      List<GoalDirectedPlanner.Choice> choices, List<Candidate> strategies, List<String> errors) {}

  private record Prepared(Instant until, Context context, StrategyResponse response) {}

  // Prompt IDs refer only to bounded, server-built evidence; callers cannot supply arbitrary
  // prompts.
  private final Map<String, Prepared> prepared = new LinkedHashMap<>();

  public PromptPreview prepare(Input input) {
    validate(input);
    if (!admission.tryAcquire()) throw new RagFailure(429, "RAG_BUSY");
    try {
      var context = context(input, runtimes.create(input.request()));
      var prompt =
          StrategyPrompt.build(
              json, context.selection(), context.facts(), input.request().goal(), input.locale());
      if ((prompt.system() + prompt.user()).getBytes(java.nio.charset.StandardCharsets.UTF_8).length
          > settings.maxPromptBytes()) throw new RagFailure(422, "RAG_CONTEXT_LIMIT");
      prepared.entrySet().removeIf(e -> !e.getValue().until().isAfter(clock.instant()));
      if (prepared.size() >= settings.cacheEntries())
        prepared.remove(prepared.keySet().iterator().next());
      var id = UUID.randomUUID().toString();
      prepared.put(id, new Prepared(clock.instant().plus(settings.cacheTtl()), context, null));
      return new PromptPreview(id, prompt);
    } catch (StrategyEvidence.LimitReached error) {
      throw new RagFailure(422, "RAG_FACT_BUDGET");
    } finally {
      admission.release();
    }
  }

  public StrategyResponse generate(GenerateInput input) {
    if (!settings.enabled()) throw new RagFailure(503, "RAG_DISABLED");
    if (!model.configured()) throw new RagFailure(503, "RAG_KEY_MISSING");
    if (input == null || input.promptId() == null) throw new RagFailure(422, "RAG_INVALID_INPUT");
    if (!admission.tryAcquire()) throw new RagFailure(429, "RAG_BUSY");
    try {
      var saved = prepared.get(input.promptId());
      if (saved == null || !saved.until().isAfter(clock.instant())) {
        prepared.remove(input.promptId());
        throw new RagFailure(410, "RAG_PROMPT_EXPIRED");
      }
      if (saved.response() != null) return saved.response();
      var context = saved.context();
      var answer =
          model.propose(
              context.selection(),
              context.facts(),
              context.input().request().goal(),
              context.input().locale());
      var decoded = decode(context, answer);
      if (!decoded.errors().isEmpty()) throw new RagFailure(502, "RAG_INVALID_OUTPUT");
      var response = new StrategyResponse(answer.status(), List.copyOf(decoded.strategies()));
      prepared.put(input.promptId(), new Prepared(saved.until(), context, response));
      return response;
    } finally {
      admission.release();
    }
  }

  private void validate(Input input) {
    if (!settings.enabled()) throw new RagFailure(503, "RAG_DISABLED");
    if (knowledge == null) throw new RagFailure(503, "RAG_CORPUS_INVALID");
    if (input == null
        || input.request() == null
        || input.mode() == null
        || input.abstractQuality() < 0
        || input.abstractQuality() > 40
        || !com.poe2craft.configuration.PublicContracts.LOCALE_CODES.contains(
            input.locale() == null ? "" : input.locale()))
      throw new RagFailure(422, "RAG_INVALID_INPUT");
  }

  private Context context(Input input, AutomaticSearchService.Runtime runtime) {
    var kernel = runtime.kernel();
    if (!knowledge.policyVersion().equals(kernel.identity().policyVersion()))
      throw new RagFailure(409, "RAG_CORPUS_POLICY_MISMATCH");
    var root = input.request().start().item();
    long deadline = System.nanoTime() + settings.computeTimeout().toNanos();
    var inventory = AutomaticCraftingAction.candidates(policy);
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item ->
            AutomaticMethodGraph.qualityProgress(
                kernel, item, input.abstractQuality(), runtime.evaluate().apply(item));
    var facts =
        new StrategyEvidence(kernel, evaluate, inventory, settings.factOperations(), deadline)
            .build(root, input.initialStage(), input.abstractQuality(), inventory);
    var query =
        root.rarity()
            + " "
            + kernel.identity().family()
            + " "
            + facts.actions().stream()
                .filter(a -> a.status().equals("READY"))
                .map(a -> a.action().toString())
                .reduce("", (a, b) -> a + " " + b)
            + (facts.context().qualityGoal() > 0 ? " QUALITY_CHECKPOINT 품질 " : "")
            + (root.explicits().stream().anyMatch(m -> m.fractured()) ? " 분열 " : "")
            + (!root.desecrated().isEmpty() ? " DESECRATE 훼손 " : "");
    var selection = knowledge.select(input.mode(), query, settings.retrievalCount());
    checkDeadline(deadline);
    return new Context(input, runtime, facts, selection, evaluate, inventory);
  }

  private Decoded decode(Context context, StrategyModel.Result answer) {
    var facts = context.facts();
    var selection = context.selection();
    var kernel = context.runtime().kernel();
    var root = context.input().request().start().item();
    var evaluate = context.evaluate();
    var errors = new ArrayList<String>();
    var proposed = new ArrayList<GoalDirectedPlanner.Choice>();
    var strategies = new ArrayList<Candidate>();
    var actions = new HashMap<String, AutomaticCraftingAction>();
    facts.actions().forEach(a -> actions.put(a.id(), a.action()));
    var selectedIds = selection.documents().stream().map(StrategyKnowledge.Document::id).toList();
    if (!Set.of("COMPLETE", "INSUFFICIENT_EVIDENCE").contains(answer.status())
        || answer.choices() == null
        || answer.choices().size() > settings.maxChoices()
        || (answer.status().equals("INSUFFICIENT_EVIDENCE") && !answer.choices().isEmpty()))
      throw new RagFailure(502, "RAG_INVALID_OUTPUT");
    for (int index = 0; index < answer.choices().size(); index++) {
      var row = answer.choices().get(index);
      try {
        if (row == null
            || row.initial() == null
            || row.cycle() == null
            || row.documents() == null
            || row.initial().size() > 1
            || row.cycle().size() != 1
            || row.documents().isEmpty()
            || !selectedIds.containsAll(row.documents())
            || row.explanation() == null
            || row.explanation().length() > 1200) throw new IllegalArgumentException();
        var initial =
            row.initial().stream().map(id -> Objects.requireNonNull(actions.get(id))).toList();
        var cycle =
            row.cycle().stream().map(id -> Objects.requireNonNull(actions.get(id))).toList();
        String stopping =
            initial.stream().anyMatch(a -> a.currency() == WorkbenchCurrency.EXALTED)
                    && cycle.getFirst().currency() != WorkbenchCurrency.CHAOS
                ? "REQUIRE_REPLACEMENT"
                : "STOP_ON_FIRST_HIT";
        if (!stopping.equals(row.preparationSuccess())) throw new IllegalArgumentException();
        new AutomaticMethodExecution.Template("validation", initial, cycle);
        if (initial.isEmpty()) {
          var action = cycle.getFirst();
          boolean quality =
              action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT
                  || action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT;
          if (quality
              ? kernel.expand(root, action, 1).status() != AutomaticCraftingKernel.Status.COMPLETE
              : kernel.plan(root, action).status() != AutomaticCraftingPlan.Status.READY)
            throw new IllegalArgumentException();
          if (!root.desecrated().isEmpty())
            cycle = List.of(kernel.withSelectionComparison(root, action, evaluate));
        } else if (facts.preparations().stream()
            .noneMatch(
                p ->
                    p.initial().equals(row.initial().getFirst())
                        && p.cycle().equals(row.cycle().getFirst())
                        && p.ready())) throw new IllegalArgumentException();
        var choice = new GoalDirectedPlanner.Choice(initial, cycle);
        if (proposed.contains(choice)) throw new IllegalArgumentException();
        proposed.add(choice);
        strategies.add(
            new Candidate(
                initial, cycle, List.copyOf(row.documents()), row.explanation(), stopping));
      } catch (IllegalArgumentException | NullPointerException error) {
        errors.add("INVALID_CHOICE_" + index);
      }
    }
    return new Decoded(List.copyOf(proposed), List.copyOf(strategies), List.copyOf(errors));
  }

  private Run execute(
      AutomaticMethodGraph graph, GoalDirectedPlanner.Choice choice, long deadline) {
    try (var execution = graph.executionAtRoot(choice)) {
      int steps = 0;
      while (!(execution.atBoundary()
              && (execution.absorbed()
                  || execution.cycles() >= AutomaticMethodGraph.CURRENT_OBSERVATION_HORIZON))
          && !execution.resourceLimited()
          && steps++ < settings.executionSteps()
          && System.nanoTime() <= deadline) {
        if (execution.atBoundary()) execution.beginCycle();
        execution.step(64);
      }
      var point = execution.provisional(AutomaticMethodGraph.CURRENT_OBSERVATION_HORIZON);
      boolean complete =
          execution.atBoundary()
              && (execution.absorbed()
                  || execution.cycles() >= AutomaticMethodGraph.CURRENT_OBSERVATION_HORIZON);
      var exits = new TreeMap<String, CertifiedInterval>();
      point
          .exits()
          .forEach(
              (kind, law) ->
                  exits.put(
                      kind.name(),
                      law.values().stream()
                          .reduce(CertifiedInterval.zero(64), CertifiedInterval::add)));
      var states = new ArrayList<String>();
      point
          .exits()
          .forEach(
              (kind, law) ->
                  law.forEach((state, mass) -> states.add(canonical(List.of(kind, state, mass)))));
      point
          .active()
          .forEach((state, mass) -> states.add(canonical(List.of("ACTIVE", state, mass))));
      Collections.sort(states);
      return new Run(
          new Execution(
              complete ? "COMPLETE" : "INCONCLUSIVE",
              point.cycles(),
              exits,
              point.active().values().stream()
                  .reduce(CertifiedInterval.zero(64), CertifiedInterval::add),
              AutomaticMethodExecution.materialUses(point.initialUses()),
              AutomaticMethodExecution.materialUses(point.cycleUses()),
              StrategyKnowledge.hash(canonical(states))),
          point);
    }
  }

  private static void checkDeadline(long deadline) {
    if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
      throw new StrategyEvidence.LimitReached();
  }

  private String canonical(Object value) {
    try {
      return json.writeValueAsString(sorted(json.valueToTree(value)));
    } catch (java.io.IOException error) {
      throw new IllegalArgumentException("Cannot encode comparison");
    }
  }

  private JsonNode sorted(JsonNode node) {
    if (node.isObject()) {
      var result = json.createObjectNode();
      var fields = new TreeSet<String>();
      node.fieldNames().forEachRemaining(fields::add);
      fields.forEach(name -> result.set(name, sorted(node.get(name))));
      return result;
    }
    if (node.isArray()) {
      var result = json.createArrayNode();
      node.forEach(n -> result.add(sorted(n)));
      return result;
    }
    return node;
  }
}
