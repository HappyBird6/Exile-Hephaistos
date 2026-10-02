package com.poe2craft.crafting.domain;

import java.util.List;

/** Workbench rules are independent of the legacy six-action bucket explorer. */
public enum WorkbenchCurrency {
  TRANSMUTATION(CraftingAction.TRANSMUTATION, 0),
  GREATER_TRANSMUTATION(CraftingAction.TRANSMUTATION, 44),
  PERFECT_TRANSMUTATION(CraftingAction.TRANSMUTATION, 70),
  AUGMENTATION(CraftingAction.AUGMENTATION, 0),
  GREATER_AUGMENTATION(CraftingAction.AUGMENTATION, 44),
  PERFECT_AUGMENTATION(CraftingAction.AUGMENTATION, 70),
  REGAL(CraftingAction.REGAL, 0),
  GREATER_REGAL(CraftingAction.REGAL, 35),
  PERFECT_REGAL(CraftingAction.REGAL, 50),
  EXALTED(CraftingAction.EXALTED, 0),
  GREATER_EXALTED(CraftingAction.EXALTED, 35),
  PERFECT_EXALTED(CraftingAction.EXALTED, 50),
  ANNULMENT(CraftingAction.ANNULMENT, 0),
  CHAOS(CraftingAction.CHAOS, 0),
  GREATER_CHAOS(CraftingAction.CHAOS, 35),
  PERFECT_CHAOS(CraftingAction.CHAOS, 50),
  DIVINE(null, 0),
  ALCHEMY(null, 0),
  FRACTURING(null, 0),
  LESSER_ESSENCE_BODY("amulet:prefix:healthy"),
  ESSENCE_BODY("amulet:prefix:robust"),
  GREATER_ESSENCE_BODY("amulet:prefix:rotund"),
  LESSER_ESSENCE_MIND("amulet:prefix:azure"),
  ESSENCE_MIND("amulet:prefix:opalescent"),
  GREATER_ESSENCE_MIND("amulet:prefix:gentian"),
  LESSER_ESSENCE_RUIN("amulet:suffix:of-the-lost"),
  ESSENCE_RUIN("amulet:suffix:of-banishment"),
  GREATER_ESSENCE_RUIN("amulet:suffix:of-expulsion"),
  LESSER_ESSENCE_INSULATION("amulet:suffix:of-the-salamander"),
  ESSENCE_INSULATION("amulet:suffix:of-the-kiln"),
  GREATER_ESSENCE_INSULATION("amulet:suffix:of-the-volcano"),
  LESSER_ESSENCE_THAWING("amulet:suffix:of-the-penguin"),
  ESSENCE_THAWING("amulet:suffix:of-the-yeti"),
  GREATER_ESSENCE_THAWING("amulet:suffix:of-the-polar-bear"),
  LESSER_ESSENCE_GROUNDING("amulet:suffix:of-the-squall"),
  ESSENCE_GROUNDING("amulet:suffix:of-the-thunderhead"),
  GREATER_ESSENCE_GROUNDING("amulet:suffix:of-the-maelstrom"),
  LESSER_ESSENCE_OPULENCE("amulet:suffix:of-plunder"),
  ESSENCE_OPULENCE("amulet:suffix:of-raiding"),
  GREATER_ESSENCE_OPULENCE("amulet:suffix:of-archaeology"),
  LESSER_ESSENCE_INFINITE(
      "https://poe2db.tw/us/Lesser_Essence_of_the_Infinite",
      "amulet:suffix:of-the-wrestler",
      "amulet:suffix:of-the-lynx",
      "amulet:suffix:of-the-student"),
  ESSENCE_INFINITE(
      "https://poe2db.tw/us/Essence_of_the_Infinite",
      "amulet:suffix:of-the-lion",
      "amulet:suffix:of-the-falcon",
      "amulet:suffix:of-the-augur"),
  GREATER_ESSENCE_INFINITE(
      "https://poe2db.tw/us/Greater_Essence_of_the_Infinite",
      "amulet:suffix:of-the-goliath",
      "amulet:suffix:of-the-leopard",
      "amulet:suffix:of-the-sage");

  private final CraftingAction baseAction;
  private final int minimumModifierLevel;
  private final String fixedModifierId;
  private final List<String> essenceModifierIds;
  private final String essenceChoiceSource;

  WorkbenchCurrency(CraftingAction baseAction, int minimumModifierLevel) {
    this.baseAction = baseAction;
    this.minimumModifierLevel = minimumModifierLevel;
    this.fixedModifierId = null;
    this.essenceModifierIds = List.of();
    this.essenceChoiceSource = null;
  }

  WorkbenchCurrency(String fixedModifierId) {
    this.baseAction = null;
    this.minimumModifierLevel = 0;
    this.fixedModifierId = fixedModifierId;
    this.essenceModifierIds = List.of(fixedModifierId);
    this.essenceChoiceSource = null;
  }

  WorkbenchCurrency(String sourceUrl, String... choiceIds) {
    this.baseAction = null;
    this.minimumModifierLevel = 0;
    this.fixedModifierId = null;
    this.essenceModifierIds = List.of(choiceIds);
    this.essenceChoiceSource = sourceUrl;
  }

  public List<String> essenceModifierIds() {
    return essenceModifierIds;
  }

  public String essenceChoiceSource() {
    return essenceChoiceSource;
  }

  public String fixedModifierId() {
    return fixedModifierId;
  }

  public CraftingAction baseAction() {
    return baseAction;
  }

  public int minimumModifierLevel() {
    return minimumModifierLevel;
  }
}
