package com.poe2craft.crafting.domain;

import java.util.List;
import java.util.Set;

/** Workbench rules are independent of the legacy six-action bucket explorer. */
public enum WorkbenchCurrency {
  PERFECT_ESSENCE_COMMAND(null, 0),
  LESSER_ESSENCE_COMMAND("rattling-sceptre:prefix:agitative"),
  ESSENCE_COMMAND("rattling-sceptre:prefix:provocative"),
  GREATER_ESSENCE_COMMAND("rattling-sceptre:prefix:motivating"),
  PERFECT_ESSENCE_BODY(null, 0),
  PERFECT_ESSENCE_RUIN(null, 0),
  PERFECT_ESSENCE_SEEKING(null, 0),
  LESSER_ESSENCE_ABRASION("crude-bow:prefix:burnished"),
  ESSENCE_ABRASION("crude-bow:prefix:gleaming"),
  GREATER_ESSENCE_ABRASION("crude-bow:prefix:razor-sharp"),
  LESSER_ESSENCE_FLAMES("crude-bow:prefix:smouldering"),
  ESSENCE_FLAMES("crude-bow:prefix:flaming"),
  GREATER_ESSENCE_FLAMES("crude-bow:prefix:incinerating"),
  LESSER_ESSENCE_ICE("crude-bow:prefix:chilled"),
  ESSENCE_ICE("crude-bow:prefix:freezing"),
  GREATER_ESSENCE_ICE("crude-bow:prefix:glaciated"),
  LESSER_ESSENCE_ELECTRICITY("crude-bow:prefix:buzzing"),
  ESSENCE_ELECTRICITY("crude-bow:prefix:sparking"),
  GREATER_ESSENCE_ELECTRICITY("crude-bow:prefix:shocking"),
  LESSER_ESSENCE_BATTLE("crude-bow:prefix:focused"),
  ESSENCE_BATTLE("crude-bow:prefix:consistent"),
  LESSER_ESSENCE_HASTE("crude-bow:suffix:of-ease"),
  ESSENCE_HASTE("crude-bow:suffix:of-mastery"),
  GREATER_ESSENCE_HASTE("crude-bow:suffix:of-renown"),
  LESSER_ESSENCE_SEEKING("crude-bow:suffix:of-havoc"),
  ESSENCE_SEEKING("crude-bow:suffix:of-disaster"),
  GREATER_ESSENCE_SEEKING("crude-bow:suffix:of-calamity"),
  ARTIFICER(null, 0),
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
  ESSENCE_HYSTERIA(null, 0),
  ESSENCE_HORROR(null, 0),
  PERFECT_ESSENCE_SORCERY(null, 0),
  PERFECT_ESSENCE_ALACRITY(null, 0),
  PERFECT_ESSENCE_ABRASION(null, 0),
  PERFECT_ESSENCE_FLAMES(null, 0),
  PERFECT_ESSENCE_ICE(null, 0),
  PERFECT_ESSENCE_ELECTRICITY(null, 0),
  PERFECT_ESSENCE_BATTLE(null, 0),
  PERFECT_ESSENCE_HASTE(null, 0),
  PERFECT_ESSENCE_GROUNDING(null, 0),
  PERFECT_ESSENCE_OPULENCE(null, 0),
  PERFECT_ESSENCE_INFINITE(null, 0),
  PERFECT_ESSENCE_ENHANCEMENT(null, 0),
  ESSENCE_BREACH(null, 0),
  RUNIC_ALLOY(null, 0),
  PRISMATIC_ALLOY(null, 0),
  ADAPTIVE_ALLOY(null, 0),
  SWIFT_ALLOY(null, 0),
  SOVEREIGN_ALLOY(null, 0),
  EXPANSIVE_ALLOY(null, 0),
  CYCLONIC_ALLOY(null, 0),
  MYSTIC_ALLOY(null, 0),
  ESSENCE_ABYSS(null, 0),
  LESSER_ESSENCE_SORCERY("attuned-wand:prefix:adept-s"),
  ESSENCE_SORCERY("attuned-wand:prefix:professor-s"),
  GREATER_ESSENCE_SORCERY("attuned-wand:prefix:incanter-s"),
  LESSER_ESSENCE_ALACRITY("attuned-wand:suffix:of-nimbleness"),
  ESSENCE_ALACRITY("attuned-wand:suffix:of-expertise"),
  GREATER_ESSENCE_ALACRITY("attuned-wand:suffix:of-legerdemain"),
  LESSER_ESSENCE_ENHANCEMENT("stocky-mitts:prefix:layered"),
  ESSENCE_ENHANCEMENT("stocky-mitts:prefix:buttressed"),
  GREATER_ESSENCE_ENHANCEMENT("stocky-mitts:prefix:thickened"),
  GREATER_ESSENCE_BATTLE("stocky-mitts:prefix:hunter-s"),
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

  public List<String> replacementEssenceModifiers() {
    return isAlloy() ? List.of() : replacementModifiers();
  }

  public boolean isAlloy() {
    return Set.of(
            RUNIC_ALLOY,
            PRISMATIC_ALLOY,
            EXPANSIVE_ALLOY,
            CYCLONIC_ALLOY,
            MYSTIC_ALLOY,
            ADAPTIVE_ALLOY,
            SWIFT_ALLOY,
            SOVEREIGN_ALLOY)
        .contains(this);
  }

  public List<String> replacementModifiers() {
    return switch (this) {
      case PERFECT_ESSENCE_COMMAND -> List.of("rattling-sceptre:suffix:essence-aura-magnitude");
      case PERFECT_ESSENCE_BODY -> List.of("rusted-cuirass:prefix:essence-maximum-life-percent");
      case PERFECT_ESSENCE_RUIN -> List.of("rusted-cuirass:prefix:essence-physical-taken-as-chaos");
      case PERFECT_ESSENCE_SEEKING ->
          List.of("rusted-cuirass:suffix:essence-reduced-incoming-critical-damage");
      case EXPANSIVE_ALLOY -> List.of("stocky-mitts:suffix:alloy-remnant-pickup-range");
      case CYCLONIC_ALLOY -> List.of("stocky-mitts:suffix:alloy-damaging-ailment-duration");
      case MYSTIC_ALLOY -> List.of("stocky-mitts:suffix:alloy-attack-area-of-effect");
      case ADAPTIVE_ALLOY -> List.of("stocky-mitts:suffix:alloy-attack-speed-missing-ward");
      case SWIFT_ALLOY -> List.of("stocky-mitts:suffix:alloy-cast-speed");
      case SOVEREIGN_ALLOY -> List.of("stocky-mitts:prefix:alloy-local-runic-ward");
      case RUNIC_ALLOY -> List.of("amulet:prefix:alloy-maximum-runic-ward");
      case PRISMATIC_ALLOY -> List.of("stocky-mitts:prefix:alloy-elemental-penetration");
      case ESSENCE_HYSTERIA -> List.of("amulet:suffix:of-suturing");
      case ESSENCE_HORROR -> List.of("stocky-mitts:suffix:essence-socketed-augment-effect");
      case PERFECT_ESSENCE_SORCERY -> List.of("attuned-wand:suffix:essence-spell-skill-level");
      case PERFECT_ESSENCE_ALACRITY -> List.of("attuned-wand:suffix:essence-mana-cost-efficiency");
      case PERFECT_ESSENCE_ABRASION -> List.of("crude-bow:prefix:essence-extra-physical-damage");
      case PERFECT_ESSENCE_FLAMES -> List.of("crude-bow:prefix:essence-extra-fire-damage");
      case PERFECT_ESSENCE_ICE -> List.of("crude-bow:prefix:essence-extra-cold-damage");
      case PERFECT_ESSENCE_ELECTRICITY ->
          List.of("crude-bow:prefix:essence-extra-lightning-damage");
      case PERFECT_ESSENCE_BATTLE -> List.of("crude-bow:suffix:essence-attack-skill-level");
      case PERFECT_ESSENCE_HASTE -> List.of("crude-bow:suffix:essence-onslaught-on-kill");
      case PERFECT_ESSENCE_GROUNDING -> List.of("stocky-mitts:suffix:essence-lightning-recoup");
      case PERFECT_ESSENCE_OPULENCE -> List.of("stocky-mitts:suffix:essence-gold-quantity");
      case ESSENCE_ABYSS ->
          List.of("amulet:prefix:essence-abyssal-mark", "amulet:suffix:essence-abyssal-mark");
      case ESSENCE_BREACH -> List.of("amulet:prefix:essence-maximum-quality");
      case PERFECT_ESSENCE_ENHANCEMENT -> List.of("amulet:prefix:essence-global-defences");
      case PERFECT_ESSENCE_INFINITE ->
          List.of(
              "amulet:suffix:essence-percent-strength",
              "amulet:suffix:essence-percent-dexterity",
              "amulet:suffix:essence-percent-intelligence");
      default -> List.of();
    };
  }

  public String replacementEssenceSource() {
    return switch (this) {
      case PERFECT_ESSENCE_COMMAND -> "https://poe2db.tw/us/Perfect_Essence_of_Command";
      case PERFECT_ESSENCE_BODY -> "https://poe2db.tw/us/Perfect_Essence_of_the_Body";
      case PERFECT_ESSENCE_RUIN -> "https://poe2db.tw/us/Perfect_Essence_of_Ruin";
      case PERFECT_ESSENCE_SEEKING -> "https://poe2db.tw/us/Perfect_Essence_of_Seeking";
      case ESSENCE_HYSTERIA -> "https://poe2db.tw/us/Essence_of_Hysteria";
      case ESSENCE_HORROR -> "https://poe2db.tw/us/Essence_of_Horror";
      case PERFECT_ESSENCE_SORCERY -> "https://poe2db.tw/us/Perfect_Essence_of_Sorcery";
      case PERFECT_ESSENCE_ALACRITY -> "https://poe2db.tw/us/Perfect_Essence_of_Alacrity";
      case PERFECT_ESSENCE_ABRASION -> "https://poe2db.tw/us/Perfect_Essence_of_Abrasion";
      case PERFECT_ESSENCE_FLAMES -> "https://poe2db.tw/us/Perfect_Essence_of_Flames";
      case PERFECT_ESSENCE_ICE -> "https://poe2db.tw/us/Perfect_Essence_of_Ice";
      case PERFECT_ESSENCE_ELECTRICITY -> "https://poe2db.tw/us/Perfect_Essence_of_Electricity";
      case PERFECT_ESSENCE_BATTLE -> "https://poe2db.tw/us/Perfect_Essence_of_Battle";
      case PERFECT_ESSENCE_HASTE -> "https://poe2db.tw/us/Perfect_Essence_of_Haste";
      case PERFECT_ESSENCE_GROUNDING -> "https://poe2db.tw/us/Perfect_Essence_of_Grounding";
      case PERFECT_ESSENCE_OPULENCE -> "https://poe2db.tw/us/Perfect_Essence_of_Opulence";
      case ESSENCE_ABYSS -> "https://poe2db.tw/us/Essence_of_the_Abyss";
      case ESSENCE_BREACH -> "https://poe2db.tw/us/Essence_of_the_Breach";
      case PERFECT_ESSENCE_ENHANCEMENT -> "https://poe2db.tw/us/Perfect_Essence_of_Enhancement";
      case PERFECT_ESSENCE_INFINITE -> "https://poe2db.tw/us/Perfect_Essence_of_the_Infinite";
      default -> throw new IllegalArgumentException("Not a supported replacement essence");
    };
  }

  public String replacementSource() {
    return switch (this) {
      case EXPANSIVE_ALLOY -> "https://poe2db.tw/us/Expansive_Alloy";
      case CYCLONIC_ALLOY -> "https://poe2db.tw/us/Cyclonic_Alloy";
      case MYSTIC_ALLOY -> "https://poe2db.tw/us/Mystic_Alloy";
      case ADAPTIVE_ALLOY -> "https://poe2db.tw/us/Adaptive_Alloy";
      case SWIFT_ALLOY -> "https://poe2db.tw/us/Swift_Alloy";
      case SOVEREIGN_ALLOY -> "https://poe2db.tw/us/Sovereign_Alloy";
      case RUNIC_ALLOY -> "https://poe2db.tw/us/Runic_Alloy";
      case PRISMATIC_ALLOY -> "https://poe2db.tw/us/Prismatic_Alloy";
      default -> replacementEssenceSource();
    };
  }
}
