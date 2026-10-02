package com.poe2craft.crafting.domain;

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
  GREATER_ESSENCE_BODY("amulet:prefix:rotund");

  private final CraftingAction baseAction;
  private final int minimumModifierLevel;
  private final String fixedModifierId;

  WorkbenchCurrency(CraftingAction baseAction, int minimumModifierLevel) {
    this.baseAction = baseAction;
    this.minimumModifierLevel = minimumModifierLevel;
    this.fixedModifierId = null;
  }

  WorkbenchCurrency(String fixedModifierId) {
    this.baseAction = null;
    this.minimumModifierLevel = 0;
    this.fixedModifierId = fixedModifierId;
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
