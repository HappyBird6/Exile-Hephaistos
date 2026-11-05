package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import java.util.*;
import java.util.random.RandomGenerator;

/** Common catalog and simulation boundary; consumers do not choose catalog implementations. */
public interface CraftingOperations extends CraftingCatalogs {
  Initial initial(String base, int level);

  Initial initial(String base, int level, boolean manualReveal);

  ItemCatalog abyssCatalog(ItemState state);

  WorkbenchSimulator simulator(ItemState state);

  AbyssCraftingPlan abyssPlan(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens);

  AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random);

  AbyssCurrencyExecutor.Result applyAbyssCurrency(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random);

  WorkbenchSimulator.Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> omens, RandomGenerator random);

  List<WorkbenchSimulator.Availability> actions(ItemState state, Set<String> omens);

  QualityDisplay qualityDisplay(ItemState state);

  public record QualityDisplay(
      String ruleVersion,
      ItemState state,
      QualityLimitRules.Limit qualityLimit,
      List<CatalystQualityDisplay.Projection> modifiers) {}

  public record Initial(
      String ruleVersion,
      ItemCatalog.Metadata metadata,
      String id,
      StateBucket state,
      Map<String, ModifierDefinition> modifiers,
      List<CraftingEngine.Availability> actions,
      List<String> compatibleSnapshotIds,
      QualityLimitRules.Limit qualityLimit,
      Integer augmentSockets,
      ItemCatalog.BaseItem baseRules,
      Integer equipmentQuality) {}
}
