package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Method transport for approved currency/omen executions, with primitive first-hit semantics. */
public final class AutomaticMethodExecution implements AutoCloseable {
  public static final String VERSION = "automatic-method-execution-v4-prepared-renewal";

  public enum PreparationSuccess {
    UNDECIDED,
    STOP_ON_FIRST_HIT,
    REQUIRE_REPLACEMENT
  }

  public record State(ItemState item, boolean temporaryPreparation, boolean quality40) {
    public State(ItemState item, boolean temporaryPreparation) {
      this(item, temporaryPreparation, false);
    }
  }

  public record Template(
      String id, List<AutomaticCraftingAction> initial, List<AutomaticCraftingAction> cycle) {
    public Template {
      Objects.requireNonNull(id);
      initial = List.copyOf(initial);
      cycle = List.copyOf(cycle);
      if (id.isBlank() || cycle.isEmpty())
        throw new IllegalArgumentException("Method cycle required");
      var sequence = new ArrayList<>(initial);
      sequence.addAll(cycle);
      for (int i = 0; i < sequence.size(); i++) {
        var action = sequence.get(i);
        if (action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION) {
          if (action.currency() != WorkbenchCurrency.CHAOS
              || !action.omens().isEmpty()
              || i + 1 == sequence.size()
              || sequence.get(i + 1).currency() != WorkbenchCurrency.CHAOS
              || !sequence.get(i + 1).omens().contains(WorkbenchOmen.WHITTLING))
            throw new IllegalArgumentException(
                "Tier preparation requires a planned Whittling acquisition");
        }
        if (action.purpose() != AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL)
          continue;
        if (i + 1 == sequence.size()
            || (sequence.get(i + 1).currency().replacementModifiers().isEmpty()
                && sequence.get(i + 1).purpose()
                    != AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION))
          throw new IllegalArgumentException(
              "Temporary addition requires a following replacement step");
      }
    }
  }

  private CertifiedMethodRunner<State, AutomaticCraftingAction> runner;
  // The runner synchronously classifies its root during construction. A post-preparation
  // return to that item is a failure; the untouched initial root is not.
  private boolean initializingRoot = true;
  private final ItemState root;
  private final boolean initialQuality40;
  private final Template template;
  private final ItemState preparedRetryAnchor;
  // This memo only avoids repeated plan comparisons; evicting it never changes probability mass.
  private final Map<ItemState, ItemState> preparedRepresentatives =
      new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemState, ItemState> eldest) {
          return size() > 64;
        }
      };
  private State preparedTransitionState;
  private AutomaticCraftingAction preparedTransitionAction;
  private Kernel<State> preparedTransition;
  private final AutomaticCraftingKernel kernel;
  private final Function<ItemState, Optional<Exit>> classify;
  private final CertifiedInterval.Precision precision;
  private long primitiveOutcomeBudget;
  private boolean primitiveLimited;

  private static final class PrimitiveBudgetReached extends RuntimeException {}

  private final PreparationSuccess preparationSuccess;
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
  private final Set<String> unresolvedReasons = new LinkedHashSet<>();

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        PreparationSuccess.UNDECIDED);
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess) {
    this(
        root,
        template,
        kernel,
        classify,
        precision,
        primitiveOutcomeBudget,
        preparationSuccess,
        false);
  }

  public AutomaticMethodExecution(
      ItemState root,
      Template template,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      boolean initialQuality40) {
    if (primitiveOutcomeBudget < 1)
      throw new IllegalArgumentException("Positive primitive budget required");
    this.root = root;
    this.initialQuality40 = initialQuality40;
    this.template = template;
    this.kernel = kernel;
    this.classify = classify;
    this.precision = precision;
    this.primitiveOutcomeBudget = primitiveOutcomeBudget;
    this.preparationSuccess = Objects.requireNonNull(preparationSuccess);
    preparedRetryAnchor = preparedRetryCandidate();
    runner =
        new CertifiedMethodRunner<>(
            new State(root, false, initialQuality40),
            compiledTemplate(),
            this::transition,
            this::classifyState,
            precision);
    initializingRoot = false;
    if (preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT)
      runner.compactTerminalFailures(new State(root, false, initialQuality40));
  }

  private ExactMethodRunner.Template<State, AutomaticCraftingAction> compiledTemplate() {
    var initial =
        template.initial().stream()
            .<Function<State, AutomaticCraftingAction>>map(a -> s -> a)
            .toList();
    var cycle =
        template.cycle().stream()
            .<Function<State, AutomaticCraftingAction>>map(a -> s -> a)
            .toList();
    return new ExactMethodRunner.Template<>(template.id(), initial, cycle);
  }

  private Kernel<State> transition(State state, AutomaticCraftingAction action) {
    if (state.equals(preparedTransitionState) && action.equals(preparedTransitionAction))
      return preparedTransition;
    if (template.initial().stream()
            .anyMatch(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)
        && action.currency() == WorkbenchCurrency.CHAOS
        && action.omens().contains(WorkbenchOmen.WHITTLING)) {
      var plan = kernel.plan(state.item(), action);
      // Stop before charging the follower if preparation did not protect any reviewed target.
      if (plan.status() == AutomaticCraftingPlan.Status.READY
          && !plan.branches().isEmpty()
          && plan.branches().stream()
              .allMatch(b -> classify.apply(b.remaining()).orElse(null) == Exit.LOSS))
        return new Kernel<>(List.of(), Fraction.ZERO, true);
    }
    var law = kernel.expand(state.item(), action, primitiveOutcomeBudget);
    // The runner has not removed this pending state or charged material yet.
    if (law.reason().equals("OUTCOME_BUDGET")) throw new PrimitiveBudgetReached();
    assumptions.addAll(law.assumptions());
    if (law.status() == AutomaticCraftingKernel.Status.UNAVAILABLE)
      return new Kernel<>(List.of(), Fraction.ZERO, true);
    if (!law.reason().isEmpty()) unresolvedReasons.add(law.reason());
    var outcomes = new LinkedHashMap<State, Fraction>();
    var terminalRepresentatives = new EnumMap<Exit, State>(Exit.class);
    var unresolved = law.unresolved().add(law.boundary());
    for (var outcome : law.outcomes()) {
      if (!state.quality40()
          && AutomaticCraftingKernel.hasQualityCap(state.item())
          && !AutomaticCraftingKernel.hasQualityCap(outcome.item())) {
        unresolved = unresolved.add(outcome.probability());
        unresolvedReasons.add("QUALITY_CAP_REMOVAL_BEFORE_40");
      } else {
        // One non-fractured affix is completely removed by each Chaos attempt. The next
        // eligible pool is therefore identical, independent of the failed modifier/tier.
        // Do not apply this quotient to multi-affix, quality-changing, or prepared cycles.
        boolean stationaryRetry =
            action.currency() == WorkbenchCurrency.CHAOS
                && action.omens().stream().allMatch(o -> o == WorkbenchOmen.WHITTLING)
                && action.purpose() == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION
                && template.initial().isEmpty()
                && template.cycle().size() == 1
                && root.explicits().size() == 1
                && state.item().explicits().size() == 1
                && outcome.item().explicits().size() == 1
                && !root.explicits().getFirst().fractured()
                && root.catalystQuality() == null
                && outcome.item().catalystQuality() == null
                && !AutomaticCraftingKernel.hasQualityCap(root)
                && !AutomaticCraftingKernel.hasQualityCap(outcome.item())
                && classifyState(new State(outcome.item(), false, state.quality40())).orElse(null)
                    == Exit.LOSS;
        var next =
            new State(
                stationaryRetry ? root : preparedRepresentative(outcome.item()),
                action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
                    || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION,
                state.quality40()
                    || action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT);
        if (preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT
            && preparedRetryAnchor != null) {
          var terminal = classifyState(next).orElse(null);
          if (terminal == Exit.LOSS || terminal == Exit.DEAD) {
            var representative = terminalRepresentatives.putIfAbsent(terminal, next);
            if (representative != null) next = representative;
          }
        }
        outcomes.merge(next, outcome.probability(), Fraction::add);
      }
    }
    var result =
        new Kernel<State>(
            outcomes.entrySet().stream().map(e -> new Outcome<>(e.getKey(), e.getValue())).toList(),
            unresolved,
            false);
    // A proven renewal state's complete transition law is identical on every cycle.
    // Retain only this one folded law; never cache an OUTCOME_BUDGET result.
    if (state.item().equals(preparedRetryAnchor) && action.equals(template.cycle().getFirst())) {
      preparedTransitionState = state;
      preparedTransitionAction = action;
      preparedTransition = result;
    }
    return result;
  }

  private AutomaticCraftingPlan retryPlan(ItemState item) {
    if (template.initial().size() != 1
        || !root.explicits().isEmpty()
        || template.cycle().size() != 1
        || template.cycle().getFirst().currency() != WorkbenchCurrency.CHAOS
        || item.explicits().size() != 1
        || item.explicits().getFirst().fractured()
        || item.catalystQuality() != null
        || AutomaticCraftingKernel.hasQualityCap(item)) return null;
    var plan = kernel.plan(item, template.cycle().getFirst());
    return plan.status() == AutomaticCraftingPlan.Status.READY
            && plan.branches().size() == 1
            && plan.branches().getFirst().remaining().explicits().isEmpty()
        ? plan
        : null;
  }

  private ItemState preparedRetryCandidate() {
    if (template.initial().size() != 1 || !root.explicits().isEmpty()) return null;
    return kernel.representatives(kernel.plan(root, template.initial().getFirst())).stream()
        .filter(item -> classify.apply(item).isEmpty() && retryPlan(item) != null)
        .findFirst()
        .orElse(null);
  }

  private ItemState preparedRepresentative(ItemState item) {
    if (preparedRetryAnchor == null || !classify.apply(item).isEmpty()) return item;
    return preparedRepresentatives.computeIfAbsent(
        item,
        value -> {
          var plan = retryPlan(value);
          return plan != null && plan.equals(retryPlan(preparedRetryAnchor))
              ? preparedRetryAnchor
              : value;
        });
  }

  private Optional<Exit> classifyState(State state) {
    var exit = classify.apply(state.item());
    if (!state.quality40()
        && AutomaticCraftingKernel.hasQualityCap(state.item())
        && state.temporaryPreparation()) return Optional.empty();
    if (!state.quality40() && AutomaticCraftingKernel.hasQualityCap(state.item()))
      return state.item().equals(root) ? Optional.empty() : Optional.of(Exit.CHECKPOINT);
    if (state.quality40() && !initialQuality40 && exit.isEmpty())
      return Optional.of(Exit.CHECKPOINT);
    if (state.temporaryPreparation() && exit.orElse(null) == Exit.GOAL) {
      if (preparationSuccess == PreparationSuccess.UNDECIDED) {
        unresolvedReasons.add("PREPARATION_FIRST_HIT_POLICY_UNDECIDED");
        return Optional.of(Exit.UNRESOLVED);
      }
      if (preparationSuccess == PreparationSuccess.REQUIRE_REPLACEMENT) return Optional.empty();
    }
    // An eligible one-affix Chaos retry continues without another preparation charge.
    // Only equal complete removal plans are quotiented; distinct pools remain distinct.
    if (exit.isEmpty() && preparedRetryAnchor != null && retryPlan(state.item()) != null)
      return Optional.empty();
    // A prepared state with an unavailable follower cannot consume that follower.
    // Absorb it now so thousands of failed numeric rolls do not become pending jobs.
    if (exit.isEmpty()
        && preparedRetryAnchor != null
        && state.temporaryPreparation()
        && kernel.plan(state.item(), template.cycle().getFirst()).status()
            == AutomaticCraftingPlan.Status.UNAVAILABLE) return Optional.of(Exit.DEAD);
    // Preparation is not a completed method checkpoint.
    if (state.temporaryPreparation() && exit.orElse(null) == Exit.CHECKPOINT)
      return Optional.empty();
    if (!initializingRoot
        && preparationSuccess == PreparationSuccess.STOP_ON_FIRST_HIT
        && !state.temporaryPreparation()
        && exit.isEmpty()
        && (!state.item().equals(root)
            || template.initial().stream()
                .anyMatch(a -> a.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)))
      return Optional.of(Exit.LOSS);
    return exit;
  }

  public record Checkpoint(
      String version,
      String objectiveIdentity,
      AutomaticCraftingKernel.Identity identity,
      ItemState root,
      boolean initialQuality40,
      Template template,
      CertifiedInterval.Precision precision,
      long primitiveOutcomeBudget,
      PreparationSuccess preparationSuccess,
      CertifiedMethodRunner.CursorHeader<State> header,
      List<CertifiedMethodRunner.CursorRow<State, AutomaticCraftingAction>> rows,
      List<WorkbenchSimulator.Assumption> assumptions,
      Set<String> unresolvedReasons) {
    public Checkpoint {
      if (objectiveIdentity == null || objectiveIdentity.isBlank())
        throw new IllegalArgumentException("Canonical objective identity required");
      rows = List.copyOf(rows);
      assumptions = List.copyOf(assumptions);
      unresolvedReasons = Set.copyOf(unresolvedReasons);
    }

    public Checkpoint withIdentity(AutomaticCraftingKernel.Identity shared) {
      return new Checkpoint(
          version,
          objectiveIdentity,
          shared,
          root,
          initialQuality40,
          template,
          precision,
          primitiveOutcomeBudget,
          preparationSuccess,
          header,
          rows,
          assumptions,
          unresolvedReasons);
    }
  }

  public Checkpoint checkpoint(String objectiveIdentity) {
    try (var lease = runner.leaseCursor()) {
      var rows = new ArrayList<CertifiedMethodRunner.CursorRow<State, AutomaticCraftingAction>>();
      lease.rows().forEachRemaining(rows::add);
      return new Checkpoint(
          VERSION,
          objectiveIdentity,
          kernel.identity(),
          root,
          initialQuality40,
          template,
          precision,
          primitiveOutcomeBudget,
          preparationSuccess,
          lease.header(),
          rows,
          assumptions(),
          unresolvedReasons());
    }
  }

  public static AutomaticMethodExecution restore(
      Checkpoint saved,
      AutomaticCraftingKernel kernel,
      String objectiveIdentity,
      Function<ItemState, Optional<Exit>> classify) {
    if (!VERSION.equals(saved.version())
        || !kernel.identity().equals(saved.identity())
        || !saved.objectiveIdentity().equals(objectiveIdentity))
      throw new IllegalArgumentException(
          "Method execution requires explicit recalculation for changed models");
    var execution =
        new AutomaticMethodExecution(
            saved.root(),
            saved.template(),
            kernel,
            classify,
            saved.precision(),
            saved.primitiveOutcomeBudget(),
            saved.preparationSuccess(),
            saved.initialQuality40());
    execution.runner.closeWorkingMaps();
    try (var restore =
        CertifiedMethodRunner.restoreCursor(
            saved.header(),
            execution.compiledTemplate(),
            execution::transition,
            execution::classifyState,
            saved.precision(),
            LinkedHashMap::new)) {
      saved.rows().forEach(restore::accept);
      execution.runner = restore.finish();
      if (saved.preparationSuccess() == PreparationSuccess.STOP_ON_FIRST_HIT)
        execution.runner.compactTerminalFailures(
            new State(saved.root(), false, saved.initialQuality40()));
    }
    execution.assumptions.addAll(saved.assumptions());
    execution.unresolvedReasons.addAll(saved.unresolvedReasons());
    return execution;
  }

  public boolean atBoundary() {
    return runner.atBoundary();
  }

  public boolean absorbed() {
    return runner.absorbed();
  }

  public long cycles() {
    return runner.cycles();
  }

  public Set<ItemState> retryRepresentatives() {
    return preparedRetryAnchor == null ? Set.of() : Set.of(preparedRetryAnchor);
  }

  public long retainedStates() {
    return runner.retainedStates()
        + runner.retention().flightOutcomes()
        + preparedRepresentatives.size()
        + (preparedTransition == null ? 0 : preparedTransition.outcomes().size());
  }

  public boolean hasPrimitiveInFlight() {
    return runner.retention().flightOutcomes() > 0;
  }

  public void beginCycle() {
    runner.beginCycle();
  }

  public void step(int outcomes) {
    if (primitiveLimited) return;
    try {
      runner.step(outcomes);
    } catch (PrimitiveBudgetReached ex) {
      primitiveLimited = true;
    }
  }

  public boolean resourceLimited() {
    return primitiveLimited;
  }

  public void increasePrimitiveBudget(long budget) {
    if (budget < primitiveOutcomeBudget)
      throw new IllegalArgumentException("Resource budget cannot shrink");
    primitiveOutcomeBudget = budget;
    primitiveLimited = false;
  }

  public CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> observation() {
    return runner.observation();
  }

  public CertifiedMethodRunner.Observation<State, AutomaticCraftingAction> provisional(
      int horizon) {
    if (runner.atBoundary() && runner.absorbed())
      return runner.observation().atLaterAbsorbedBoundary(horizon);
    if (runner.atBoundary() && runner.cycles() == horizon) return runner.observation();
    var point = runner.provisional(horizon, new State(root, false, initialQuality40));
    return new CertifiedMethodRunner.Observation<>(
        horizon,
        point.exits(),
        point.active(),
        usageBounds(point.initialUses(), template.initial(), 1),
        usageBounds(point.cycleUses(), template.cycle(), horizon),
        point.mass());
  }

  private Map<AutomaticCraftingAction, CertifiedInterval> usageBounds(
      Map<AutomaticCraftingAction, CertifiedInterval> spent,
      List<AutomaticCraftingAction> actions,
      int repetitions) {
    var counts = new LinkedHashMap<AutomaticCraftingAction, Long>();
    actions.forEach(a -> counts.merge(a, (long) repetitions, Long::sum));
    var result = new LinkedHashMap<AutomaticCraftingAction, CertifiedInterval>();
    counts.forEach(
        (action, count) ->
            result.put(
                action,
                new CertifiedInterval(
                    precision.fractionalBits(),
                    spent
                        .getOrDefault(action, CertifiedInterval.zero(precision.fractionalBits()))
                        .lowerUnits(),
                    java.math.BigInteger.valueOf(count).shiftLeft(precision.fractionalBits()))));
    return result;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  public Set<String> unresolvedReasons() {
    return Set.copyOf(unresolvedReasons);
  }

  /**
   * Expand execution costs into actual currencies and consumed omens; never count two as one use.
   */
  public static Map<String, CertifiedInterval> materialUses(
      Map<AutomaticCraftingAction, CertifiedInterval> uses) {
    var result = new LinkedHashMap<String, CertifiedInterval>();
    uses.forEach(
        (action, probability) -> {
          if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
            result.merge("WORK:QUALITY_40", probability, CertifiedInterval::add);
            return;
          }
          result.merge(action.currency().name(), probability, CertifiedInterval::add);
          action
              .omens()
              .forEach(
                  omen -> result.merge("OMEN:" + omen.name(), probability, CertifiedInterval::add));
        });
    return Collections.unmodifiableMap(result);
  }

  @Override
  public void close() {
    runner.closeWorkingMaps();
  }
}
