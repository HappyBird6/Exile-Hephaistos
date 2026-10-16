package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Method transport for approved currency/omen executions, with primitive first-hit semantics. */
public final class AutomaticMethodExecution implements AutoCloseable {
  public static final String VERSION = "automatic-method-execution-v1";

  public enum PreparationSuccess {
    UNDECIDED,
    STOP_ON_FIRST_HIT,
    REQUIRE_REPLACEMENT
  }

  public record State(ItemState item, boolean temporaryPreparation) {}

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
        if (action.purpose() != AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL)
          continue;
        if (i + 1 == sequence.size()
            || sequence.get(i + 1).currency().replacementModifiers().isEmpty())
          throw new IllegalArgumentException(
              "Temporary addition requires a following replacement step");
      }
    }
  }

  private CertifiedMethodRunner<State, AutomaticCraftingAction> runner;
  private final ItemState root;
  private final Template template;
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
    if (primitiveOutcomeBudget < 1)
      throw new IllegalArgumentException("Positive primitive budget required");
    this.root = root;
    this.template = template;
    this.kernel = kernel;
    this.classify = classify;
    this.precision = precision;
    this.primitiveOutcomeBudget = primitiveOutcomeBudget;
    this.preparationSuccess = Objects.requireNonNull(preparationSuccess);
    runner =
        new CertifiedMethodRunner<>(
            new State(root, false),
            compiledTemplate(),
            this::transition,
            this::classifyState,
            precision);
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
    var law = kernel.expand(state.item(), action, primitiveOutcomeBudget);
    // The runner has not removed this pending state or charged material yet.
    if (law.reason().equals("OUTCOME_BUDGET")) throw new PrimitiveBudgetReached();
    assumptions.addAll(law.assumptions());
    if (law.status() == AutomaticCraftingKernel.Status.UNAVAILABLE)
      return new Kernel<>(List.of(), Fraction.ZERO, true);
    if (!law.reason().isEmpty()) unresolvedReasons.add(law.reason());
    var outcomes =
        law.outcomes().stream()
            .map(
                o ->
                    new Outcome<>(
                        new State(
                            o.item(),
                            action.purpose()
                                == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
                        o.probability()))
            .toList();
    return new Kernel<>(outcomes, law.unresolved().add(law.boundary()), false);
  }

  private Optional<Exit> classifyState(State state) {
    var exit = classify.apply(state.item());
    if (state.temporaryPreparation() && exit.orElse(null) == Exit.GOAL) {
      if (preparationSuccess == PreparationSuccess.UNDECIDED) {
        unresolvedReasons.add("PREPARATION_FIRST_HIT_POLICY_UNDECIDED");
        return Optional.of(Exit.UNRESOLVED);
      }
      if (preparationSuccess == PreparationSuccess.REQUIRE_REPLACEMENT) return Optional.empty();
    }
    // Preparation is not a completed method checkpoint.
    return state.temporaryPreparation() && exit.orElse(null) == Exit.CHECKPOINT
        ? Optional.empty()
        : exit;
  }

  public record Checkpoint(
      String version,
      String objectiveIdentity,
      AutomaticCraftingKernel.Identity identity,
      ItemState root,
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
            saved.preparationSuccess());
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

  public long retainedStates() {
    return runner.retainedStates();
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
