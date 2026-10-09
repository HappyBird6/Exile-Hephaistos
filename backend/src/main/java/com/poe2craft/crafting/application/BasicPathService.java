package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Explicit fixed policies only. Recovery probabilities are conditional on the supplied failure. */
public final class BasicPathService {
  public enum PolicyMode {
    SINGLE_PASS,
    REPEAT_CYCLE
  }

  public record Policy(List<WorkbenchCurrency> actions, PolicyMode mode) {
    public Policy {
      if (actions == null || actions.isEmpty() || actions.size() > 32 || mode == null)
        throw new IllegalArgumentException("Explicit policy with one to 32 actions required");
      if (actions.stream().anyMatch(Objects::isNull))
        throw new IllegalArgumentException("Non-null policy actions required");
      actions = List.copyOf(actions);
      if (!BasicCurrencyTransitions.supportedActions().containsAll(actions))
        throw new IllegalArgumentException("Policy action is outside the basic transition model");
    }
  }

  /** Exact checkpoint OR presence of explicit modifier IDs; numeric goals are not inferred. */
  public record Target(BasicCurrencyState checkpoint, Set<String> explicitModifierIds) {
    public Target {
      if (explicitModifierIds == null)
        throw new IllegalArgumentException("Explicit target required");
      explicitModifierIds = Set.copyOf(explicitModifierIds);
      if ((checkpoint == null) == explicitModifierIds.isEmpty())
        throw new IllegalArgumentException(
            "Specify checkpoint or nonempty modifier IDs, exclusively");
    }

    boolean matches(BasicCurrencyState state) {
      if (checkpoint != null) return checkpoint.equals(state);
      return state.item().explicits().stream()
          .map(ModifierInstance::modifierId)
          .toList()
          .containsAll(explicitModifierIds);
    }
  }

  public record Request(
      BasicCurrencyState start,
      Policy policy,
      Target target,
      Set<String> activeOmens,
      List<Long> observations) {}

  public record Blocker(
      BasicCurrencyState state, WorkbenchCurrency action, String status, String reason) {}

  public record Limits(
      long evaluations, int frontierStates, int fractionBits, long elementaryOutcomes) {}

  public static final Limits LIMITS = new Limits(10000, 1000, 65536, 1000);

  public record Result(
      String purpose,
      BasicCurrencyState start,
      Policy policy,
      Target target,
      BasicCurrencyState.Provenance provenance,
      String interpretation,
      FirstHitCalculator.Result distribution,
      List<Blocker> blockers,
      boolean blockersTruncated,
      boolean recoveryIncludedInMain,
      BasicCurrencyTransitions.ChaosRenewal renewalProof) {}

  private final ItemCatalog catalog;
  private final BasicCurrencyTransitions transitions;
  private final BasicTransitionCache cache;

  public BasicPathService(ItemCatalog catalog, String identity) {
    this.catalog = catalog;
    transitions = new BasicCurrencyTransitions(catalog, identity);
    cache = new BasicTransitionCache(transitions, 32, 20000);
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public Result calculate(Request request, boolean recovery, BooleanSupplier cancelled) {
    if (request == null
        || request.policy() == null
        || request.target() == null
        || request.activeOmens() == null)
      throw new IllegalArgumentException("Explicit path request required");
    validate(request.start());
    if (request.target().checkpoint() != null) validate(request.target().checkpoint());
    for (var id : request.target().explicitModifierIds()) {
      var definition = catalog.modifiers().get(id);
      if (definition == null || definition.layer() != ModifierDefinition.Layer.EXPLICIT)
        throw new IllegalArgumentException(
            "Target modifier is not in the current explicit catalog");
    }
    FirstHitCalculator.observationPoints(request.observations());
    if (request.policy().mode() == PolicyMode.REPEAT_CYCLE
        && request.policy().actions().equals(List.of(WorkbenchCurrency.CHAOS))
        && request.activeOmens().isEmpty()
        && request.target().checkpoint() == null
        && request.target().explicitModifierIds().size() == 1) {
      var proof =
          transitions.proveSingleExplicitChaos(
              request.start(), request.target().explicitModifierIds().iterator().next());
      if (proof.isPresent()) {
        var renewal = proof.get();
        var distribution =
            ChaosRenewalCalculator.calculate(
                renewal.probability(),
                request.target().matches(request.start()),
                request.observations(),
                LIMITS.fractionBits(),
                cancelled);
        return new Result(
            recovery ? "CONDITIONAL_RECOVERY" : "MAIN_FIRST_HIT",
            request.start(),
            request.policy(),
            request.target(),
            provenance(),
            BasicCurrencyTransitions.INTERPRETATION,
            distribution,
            List.of(),
            false,
            false,
            renewal);
      }
    }
    var blockers = new ArrayList<Blocker>();
    boolean[] truncated = {false};
    var distribution =
        new FirstHitCalculator<BasicCurrencyState>()
            .calculate(
                request.start(),
                request.observations(),
                state ->
                    request.target().matches(state)
                        ? FirstHitCalculator.Match.HIT
                        : FirstHitCalculator.Match.MISS,
                (state, step) -> {
                  var action =
                      request
                          .policy()
                          .actions()
                          .get((int) (step % request.policy().actions().size()));
                  var expanded =
                      cache.get(state, action, request.activeOmens(), LIMITS.elementaryOutcomes());
                  if (expanded.status() != BasicCurrencyTransitions.Status.COMPLETE) {
                    if (blockers.size() < 32)
                      blockers.add(
                          new Blocker(state, action, expanded.status().name(), expanded.reason()));
                    else truncated[0] = true;
                  }
                  if (expanded.status() == BasicCurrencyTransitions.Status.UNAVAILABLE)
                    return new FirstHitCalculator.Kernel<>(List.of(), Fraction.ZERO, true);
                  return new FirstHitCalculator.Kernel<>(
                      expanded.outcomes().stream()
                          .map(
                              outcome ->
                                  new FirstHitCalculator.Edge<>(
                                      outcome.state(), outcome.probability()))
                          .toList(),
                      expanded.unresolved(),
                      false);
                },
                new FirstHitCalculator.Budget(
                    LIMITS.evaluations(), LIMITS.frontierStates(), LIMITS.fractionBits()),
                cancelled,
                step ->
                    request.policy().mode() == PolicyMode.SINGLE_PASS
                        && step >= request.policy().actions().size());
    return new Result(
        recovery ? "CONDITIONAL_RECOVERY" : "MAIN_FIRST_HIT",
        request.start(),
        request.policy(),
        request.target(),
        provenance(),
        BasicCurrencyTransitions.INTERPRETATION,
        distribution,
        List.copyOf(blockers),
        truncated[0],
        false,
        null);
  }

  private void validate(BasicCurrencyState state) {
    if (state == null || !provenance().equals(state.provenance()))
      throw new IllegalArgumentException("Current full-state provenance required");
    if (!new ItemStateValidator(catalog).validate(state.item()).isEmpty())
      throw new IllegalArgumentException("Invalid concrete item state");
  }
}
