package com.poe2craft.workbench.repeat;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Batch values deliberately separate from the single-apply response. No intermediate state field.
 */
public final class ChaosRepeatProtocol {
  private ChaosRepeatProtocol() {}

  public sealed interface State permits Concrete, Abyss {
    ItemState displayItem();
  }

  public record Concrete(String kind, ItemState item) implements State {
    public Concrete(ItemState item) {
      this("CONCRETE", item);
    }

    public ItemState displayItem() {
      return item;
    }
  }

  public record Abyss(String kind, AbyssCraftingState abyss) implements State {
    public Abyss(AbyssCraftingState abyss) {
      this("ABYSS", abyss);
    }

    public ItemState displayItem() {
      return abyss.storedItem();
    }
  }

  public record Input(
      String requestId,
      String requestSha256,
      State state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      String pattern,
      String locale,
      int maximumUses) {
    public Input {
      activeOmens = Set.copyOf(activeOmens);
    }
  }

  public record Binding(
      String requestId,
      String requestSha256,
      WorkbenchCurrency action,
      String locale,
      int maximumUses,
      String matcherVersion) {}

  public record Rejection(String code, String reason) {}

  public record Failure(String code, String stage) {}

  public sealed interface Witness permits ConcreteWitness, AbyssWitness {}

  public record ConcreteWitness(
      String kind,
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<WorkbenchSimulator.Event> events,
      List<WorkbenchSimulator.Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens,
      QualityLimitRules.Limit qualityLimit)
      implements Witness {
    public static ConcreteWitness from(WorkbenchSimulator.Result result) {
      return result == null
          ? null
          : new ConcreteWitness(
              "CONCRETE",
              result.ruleVersion(),
              result.ledgerVersion(),
              result.snapshotId(),
              result.action(),
              result.applied(),
              result.reason(),
              result.events(),
              result.assumptions(),
              result.consumedOmens(),
              result.remainingOmens(),
              result.qualityLimit());
    }
  }

  public record AbyssWitness(
      String kind,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<String> consumedOmens,
      List<String> remainingOmens)
      implements Witness {
    public static AbyssWitness from(AbyssCurrencyExecutor.Result result) {
      return new AbyssWitness(
          "ABYSS",
          result.action(),
          result.applied(),
          result.reason(),
          result.consumedOmens(),
          result.remainingOmens());
    }
  }

  public record Outcome(
      int schemaVersion,
      String kind,
      String rulesetIdentity,
      Binding requestBinding,
      State finalState,
      int actualUses,
      Map<String, Integer> consumedOmens,
      Set<String> remainingOmens,
      Witness lastAppliedWitness,
      String termination,
      Rejection lastRejection,
      Failure failure) {
    public Outcome {
      consumedOmens = Map.copyOf(consumedOmens);
      remainingOmens = Set.copyOf(remainingOmens);
    }
  }
}
