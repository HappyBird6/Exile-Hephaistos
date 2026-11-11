import { currencyActions, actionNames } from './craftingApi'
import type { Action } from './craftingApi'
import { catalystTypes } from './catalystQuality'
import type { CatalystQuality } from './catalystQuality'

export type CatalystAction =
  | `CATALYST_${keyof typeof catalystTypes}`
  | `REFINED_CATALYST_${keyof typeof catalystTypes}`
export const catalystActionType = (action: WorkbenchAction) => {
  const type = action.replace(/^REFINED_/, '').replace(/^CATALYST_/, '')
  return Object.hasOwn(catalystTypes, type)
    ? (type as CatalystQuality['type'])
    : null
}

export type WorkbenchAction =
  | 'ANCIENT_DILUTED_LIQUID_IRE'
  | 'ANCIENT_DILUTED_LIQUID_GUILT'
  | 'ANCIENT_DILUTED_LIQUID_GREED'
  | 'ANCIENT_LIQUID_PARANOIA'
  | 'ANCIENT_LIQUID_ENVY'
  | 'ANCIENT_LIQUID_DISGUST'
  | 'ANCIENT_LIQUID_DESPAIR'
  | 'ANCIENT_CONCENTRATED_LIQUID_FEAR'
  | 'ANCIENT_CONCENTRATED_LIQUID_SUFFERING'
  | 'ANCIENT_CONCENTRATED_LIQUID_ISOLATION'
  | 'ANCIENT_POTENT_LIQUID_MELANCHOLY'
  | 'ANCIENT_POTENT_LIQUID_FEROCITY'
  | 'ANCIENT_POTENT_LIQUID_CONTEMPT'
  | 'POTENT_LIQUID_MELANCHOLY'
  | 'POTENT_LIQUID_FEROCITY'
  | 'POTENT_LIQUID_CONTEMPT'
  | 'DILUTED_LIQUID_IRE'
  | 'DILUTED_LIQUID_GUILT'
  | 'DILUTED_LIQUID_GREED'
  | 'LIQUID_PARANOIA'
  | 'LIQUID_ENVY'
  | 'LIQUID_DISGUST'
  | 'LIQUID_DESPAIR'
  | 'CONCENTRATED_LIQUID_FEAR'
  | 'CONCENTRATED_LIQUID_SUFFERING'
  | 'CONCENTRATED_LIQUID_ISOLATION'
  | CatalystAction
  | 'PERFECT_ESSENCE_MIND'
  | 'PERFECT_ESSENCE_THAWING'
  | 'PERFECT_ESSENCE_INSULATION'
  | 'LESSER_ESSENCE_COMMAND'
  | 'ESSENCE_COMMAND'
  | 'GREATER_ESSENCE_COMMAND'
  | 'PERFECT_ESSENCE_COMMAND'
  | 'PERFECT_ESSENCE_BODY'
  | 'PERFECT_ESSENCE_RUIN'
  | 'PERFECT_ESSENCE_SEEKING'
  | 'LESSER_ESSENCE_ABRASION'
  | 'ESSENCE_ABRASION'
  | 'GREATER_ESSENCE_ABRASION'
  | 'LESSER_ESSENCE_FLAMES'
  | 'ESSENCE_FLAMES'
  | 'GREATER_ESSENCE_FLAMES'
  | 'LESSER_ESSENCE_ICE'
  | 'ESSENCE_ICE'
  | 'GREATER_ESSENCE_ICE'
  | 'LESSER_ESSENCE_ELECTRICITY'
  | 'ESSENCE_ELECTRICITY'
  | 'GREATER_ESSENCE_ELECTRICITY'
  | 'LESSER_ESSENCE_BATTLE'
  | 'ESSENCE_BATTLE'
  | 'LESSER_ESSENCE_HASTE'
  | 'ESSENCE_HASTE'
  | 'GREATER_ESSENCE_HASTE'
  | 'LESSER_ESSENCE_SEEKING'
  | 'ESSENCE_SEEKING'
  | 'GREATER_ESSENCE_SEEKING'
  | 'INSTALL_AUGMENT'
  | 'ARTIFICER'
  | 'ARMOURERS_SCRAP'
  | 'BLACKSMITHS_WHETSTONE'
  | Action
  | `GREATER_${Exclude<Action, 'ANNULMENT'>}`
  | `PERFECT_${Exclude<Action, 'ANNULMENT'>}`
  | 'LESSER_ESSENCE_ENHANCEMENT'
  | 'ESSENCE_ENHANCEMENT'
  | 'GREATER_ESSENCE_ENHANCEMENT'
  | 'GREATER_ESSENCE_BATTLE'
  | 'DIVINE'
  | 'ALCHEMY'
  | 'FRACTURING'
  | 'ESSENCE_HYSTERIA'
  | 'PERFECT_ESSENCE_INFINITE'
  | 'PERFECT_ESSENCE_ENHANCEMENT'
  | 'ESSENCE_ABYSS'
  | 'ESSENCE_HORROR'
  | 'LESSER_ESSENCE_SORCERY'
  | 'ESSENCE_SORCERY'
  | 'GREATER_ESSENCE_SORCERY'
  | 'PERFECT_ESSENCE_SORCERY'
  | 'LESSER_ESSENCE_ALACRITY'
  | 'ESSENCE_ALACRITY'
  | 'GREATER_ESSENCE_ALACRITY'
  | 'PERFECT_ESSENCE_ALACRITY'
  | 'PERFECT_ESSENCE_ABRASION'
  | 'PERFECT_ESSENCE_FLAMES'
  | 'PERFECT_ESSENCE_ICE'
  | 'PERFECT_ESSENCE_ELECTRICITY'
  | 'PERFECT_ESSENCE_BATTLE'
  | 'PERFECT_ESSENCE_HASTE'
  | 'PERFECT_ESSENCE_GROUNDING'
  | 'PERFECT_ESSENCE_OPULENCE'
  | 'ESSENCE_BREACH'
  | 'PROTECTIVE_ALLOY'
  | 'CELESTIAL_ALLOY'
  | 'TRANSCENDENT_ALLOY'
  | 'THE_RUNEBINDERS_ALLOY'
  | 'THE_RUNEFATHERS_ALLOY'
  | 'RUNIC_ALLOY'
  | 'EXPANSIVE_ALLOY'
  | 'CYCLONIC_ALLOY'
  | 'MYSTIC_ALLOY'
  | 'ADAPTIVE_ALLOY'
  | 'SWIFT_ALLOY'
  | 'SOVEREIGN_ALLOY'
  | 'PRISMATIC_ALLOY'
  | 'LESSER_ESSENCE_BODY'
  | 'ESSENCE_BODY'
  | 'GREATER_ESSENCE_BODY'
  | 'LESSER_ESSENCE_MIND'
  | 'ESSENCE_MIND'
  | 'GREATER_ESSENCE_MIND'
  | 'LESSER_ESSENCE_RUIN'
  | 'ESSENCE_RUIN'
  | 'GREATER_ESSENCE_RUIN'
  | 'LESSER_ESSENCE_INFINITE'
  | 'ESSENCE_INFINITE'
  | 'GREATER_ESSENCE_INFINITE'
  | 'LESSER_ESSENCE_INSULATION'
  | 'ESSENCE_INSULATION'
  | 'GREATER_ESSENCE_INSULATION'
  | 'LESSER_ESSENCE_THAWING'
  | 'ESSENCE_THAWING'
  | 'GREATER_ESSENCE_THAWING'
  | 'LESSER_ESSENCE_GROUNDING'
  | 'ESSENCE_GROUNDING'
  | 'GREATER_ESSENCE_GROUNDING'
  | 'LESSER_ESSENCE_OPULENCE'
  | 'ESSENCE_OPULENCE'
  | 'GREATER_ESSENCE_OPULENCE'
export const bowFixedEssenceModifiers: Partial<
  Record<WorkbenchAction, string>
> = {
  LESSER_ESSENCE_ABRASION: 'crude-bow:prefix:burnished',
  ESSENCE_ABRASION: 'crude-bow:prefix:gleaming',
  GREATER_ESSENCE_ABRASION: 'crude-bow:prefix:razor-sharp',
  LESSER_ESSENCE_FLAMES: 'crude-bow:prefix:smouldering',
  ESSENCE_FLAMES: 'crude-bow:prefix:flaming',
  GREATER_ESSENCE_FLAMES: 'crude-bow:prefix:incinerating',
  LESSER_ESSENCE_ICE: 'crude-bow:prefix:chilled',
  ESSENCE_ICE: 'crude-bow:prefix:freezing',
  GREATER_ESSENCE_ICE: 'crude-bow:prefix:glaciated',
  LESSER_ESSENCE_ELECTRICITY: 'crude-bow:prefix:buzzing',
  ESSENCE_ELECTRICITY: 'crude-bow:prefix:sparking',
  GREATER_ESSENCE_ELECTRICITY: 'crude-bow:prefix:shocking',
  LESSER_ESSENCE_BATTLE: 'crude-bow:prefix:focused',
  ESSENCE_BATTLE: 'crude-bow:prefix:consistent',
  GREATER_ESSENCE_BATTLE: 'crude-bow:prefix:hunter-s',
  LESSER_ESSENCE_HASTE: 'crude-bow:suffix:of-ease',
  ESSENCE_HASTE: 'crude-bow:suffix:of-mastery',
  GREATER_ESSENCE_HASTE: 'crude-bow:suffix:of-renown',
  LESSER_ESSENCE_SEEKING: 'crude-bow:suffix:of-havoc',
  ESSENCE_SEEKING: 'crude-bow:suffix:of-disaster',
  GREATER_ESSENCE_SEEKING: 'crude-bow:suffix:of-calamity',
}
export const fixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
  LESSER_ESSENCE_ENHANCEMENT: 'stocky-mitts:prefix:layered',
  ESSENCE_ENHANCEMENT: 'stocky-mitts:prefix:buttressed',
  GREATER_ESSENCE_ENHANCEMENT: 'stocky-mitts:prefix:thickened',
  GREATER_ESSENCE_BATTLE: 'stocky-mitts:prefix:hunter-s',
  LESSER_ESSENCE_BODY: 'amulet:prefix:healthy',
  ESSENCE_BODY: 'amulet:prefix:robust',
  GREATER_ESSENCE_BODY: 'amulet:prefix:rotund',
  LESSER_ESSENCE_MIND: 'amulet:prefix:azure',
  ESSENCE_MIND: 'amulet:prefix:opalescent',
  GREATER_ESSENCE_MIND: 'amulet:prefix:gentian',
  LESSER_ESSENCE_RUIN: 'amulet:suffix:of-the-lost',
  ESSENCE_RUIN: 'amulet:suffix:of-banishment',
  GREATER_ESSENCE_RUIN: 'amulet:suffix:of-expulsion',
  LESSER_ESSENCE_INSULATION: 'amulet:suffix:of-the-salamander',
  ESSENCE_INSULATION: 'amulet:suffix:of-the-kiln',
  GREATER_ESSENCE_INSULATION: 'amulet:suffix:of-the-volcano',
  LESSER_ESSENCE_THAWING: 'amulet:suffix:of-the-penguin',
  ESSENCE_THAWING: 'amulet:suffix:of-the-yeti',
  GREATER_ESSENCE_THAWING: 'amulet:suffix:of-the-polar-bear',
  LESSER_ESSENCE_GROUNDING: 'amulet:suffix:of-the-squall',
  ESSENCE_GROUNDING: 'amulet:suffix:of-the-thunderhead',
  GREATER_ESSENCE_GROUNDING: 'amulet:suffix:of-the-maelstrom',
  LESSER_ESSENCE_OPULENCE: 'amulet:suffix:of-plunder',
  ESSENCE_OPULENCE: 'amulet:suffix:of-raiding',
  GREATER_ESSENCE_OPULENCE: 'amulet:suffix:of-archaeology',
}
export const stockyFixedEssenceModifiers: Partial<
  Record<WorkbenchAction, string>
> = {
  LESSER_ESSENCE_BODY: 'stocky-mitts:prefix:sanguine',
  ESSENCE_BODY: 'stocky-mitts:prefix:robust',
  GREATER_ESSENCE_BODY: 'stocky-mitts:prefix:rotund',
  LESSER_ESSENCE_MIND: 'stocky-mitts:prefix:azure',
  ESSENCE_MIND: 'stocky-mitts:prefix:aqua',
  GREATER_ESSENCE_MIND: 'stocky-mitts:prefix:opalescent',
  LESSER_ESSENCE_RUIN: 'stocky-mitts:suffix:of-the-lost',
  ESSENCE_RUIN: 'stocky-mitts:suffix:of-banishment',
  GREATER_ESSENCE_RUIN: 'stocky-mitts:suffix:of-expulsion',
  LESSER_ESSENCE_INSULATION: 'stocky-mitts:suffix:of-the-salamander',
  ESSENCE_INSULATION: 'stocky-mitts:suffix:of-the-kiln',
  GREATER_ESSENCE_INSULATION: 'stocky-mitts:suffix:of-the-volcano',
  LESSER_ESSENCE_THAWING: 'stocky-mitts:suffix:of-the-penguin',
  ESSENCE_THAWING: 'stocky-mitts:suffix:of-the-yeti',
  GREATER_ESSENCE_THAWING: 'stocky-mitts:suffix:of-the-polar-bear',
  LESSER_ESSENCE_GROUNDING: 'stocky-mitts:suffix:of-the-squall',
  ESSENCE_GROUNDING: 'stocky-mitts:suffix:of-the-thunderhead',
  GREATER_ESSENCE_GROUNDING: 'stocky-mitts:suffix:of-the-maelstrom',
  LESSER_ESSENCE_OPULENCE: 'stocky-mitts:suffix:of-plunder',
  ESSENCE_OPULENCE: 'stocky-mitts:suffix:of-raiding',
  GREATER_ESSENCE_OPULENCE: 'stocky-mitts:suffix:of-archaeology',
}
export const choiceEssenceModifiers: Partial<
  Record<WorkbenchAction, readonly string[]>
> = {
  LESSER_ESSENCE_INFINITE: [
    'amulet:suffix:of-the-wrestler',
    'amulet:suffix:of-the-lynx',
    'amulet:suffix:of-the-student',
  ],
  ESSENCE_INFINITE: [
    'amulet:suffix:of-the-lion',
    'amulet:suffix:of-the-falcon',
    'amulet:suffix:of-the-augur',
  ],
  GREATER_ESSENCE_INFINITE: [
    'amulet:suffix:of-the-goliath',
    'amulet:suffix:of-the-leopard',
    'amulet:suffix:of-the-sage',
  ],
}
export const wandFixedEssenceModifiers: Partial<
  Record<WorkbenchAction, string>
> = {
  LESSER_ESSENCE_SORCERY: 'attuned-wand:prefix:adept-s',
  ESSENCE_SORCERY: 'attuned-wand:prefix:professor-s',
  GREATER_ESSENCE_SORCERY: 'attuned-wand:prefix:incanter-s',
  LESSER_ESSENCE_SEEKING:
    'attuned-wand:suffix:of-havoc:spellcriticalstrikechance2',
  ESSENCE_SEEKING: 'attuned-wand:suffix:of-disaster',
  GREATER_ESSENCE_SEEKING: 'attuned-wand:suffix:of-calamity',
  LESSER_ESSENCE_ALACRITY: 'attuned-wand:suffix:of-nimbleness',
  ESSENCE_ALACRITY: 'attuned-wand:suffix:of-expertise',
  GREATER_ESSENCE_ALACRITY: 'attuned-wand:suffix:of-legerdemain',
}
export const sceptreFixedEssenceModifiers: Partial<
  Record<WorkbenchAction, string>
> = {
  LESSER_ESSENCE_COMMAND: 'rattling-sceptre:prefix:agitative',
  ESSENCE_COMMAND: 'rattling-sceptre:prefix:provocative',
  GREATER_ESSENCE_COMMAND: 'rattling-sceptre:prefix:motivating',
}
export const replacementEssenceModifiers: Partial<
  Record<WorkbenchAction, readonly string[]>
> = {
  DILUTED_LIQUID_IRE: ['sapphire:crafted:JewelEnergyShield'],
  DILUTED_LIQUID_GUILT: ['sapphire:crafted:JewelColdDamage'],
  DILUTED_LIQUID_GREED: ['sapphire:crafted:JewelChaosDamage'],
  LIQUID_PARANOIA: ['sapphire:crafted:JewelCastSpeed'],
  LIQUID_ENVY: ['sapphire:crafted:JewelSpellDamage'],
  LIQUID_DISGUST: ['sapphire:crafted:JewelManaonKill'],
  LIQUID_DESPAIR: ['sapphire:crafted:JewelSpellCriticalChance'],
  CONCENTRATED_LIQUID_FEAR: ['sapphire:crafted:JewelSpellCriticalDamage'],
  CONCENTRATED_LIQUID_SUFFERING: ['sapphire:crafted:JewelAreaofEffect'],
  CONCENTRATED_LIQUID_ISOLATION: [
    'sapphire:crafted:JewelMaximumColdResistance',
  ],
  PERFECT_ESSENCE_MIND: ['iron-ring:prefix:essence-increased-maximum-mana'],
  PERFECT_ESSENCE_THAWING: [
    'rusted-greathelm:suffix:essence-cold-damage-recouped-as-life',
  ],
  PERFECT_ESSENCE_INSULATION: [
    'rawhide-belt:suffix:essence-fire-damage-recouped-as-life',
  ],
  PERFECT_ESSENCE_COMMAND: ['rattling-sceptre:suffix:essence-aura-magnitude'],
  PERFECT_ESSENCE_BODY: ['rusted-cuirass:prefix:essence-maximum-life-percent'],
  PERFECT_ESSENCE_RUIN: [
    'rusted-cuirass:prefix:essence-physical-taken-as-chaos',
  ],
  PERFECT_ESSENCE_SEEKING: [
    'rusted-cuirass:suffix:essence-reduced-incoming-critical-damage',
  ],
  PERFECT_ESSENCE_SORCERY: ['attuned-wand:suffix:essence-spell-skill-level'],
  PERFECT_ESSENCE_ALACRITY: [
    'attuned-wand:suffix:essence-mana-cost-efficiency',
  ],
  PERFECT_ESSENCE_ABRASION: ['crude-bow:prefix:essence-extra-physical-damage'],
  PERFECT_ESSENCE_FLAMES: ['crude-bow:prefix:essence-extra-fire-damage'],
  PERFECT_ESSENCE_ICE: ['crude-bow:prefix:essence-extra-cold-damage'],
  PERFECT_ESSENCE_ELECTRICITY: [
    'crude-bow:prefix:essence-extra-lightning-damage',
  ],
  PERFECT_ESSENCE_BATTLE: ['crude-bow:suffix:essence-attack-skill-level'],
  PERFECT_ESSENCE_HASTE: ['crude-bow:suffix:essence-onslaught-on-kill'],
  ESSENCE_HYSTERIA: ['amulet:suffix:of-suturing'],
  PERFECT_ESSENCE_ENHANCEMENT: ['amulet:prefix:essence-global-defences'],
  ESSENCE_ABYSS: [
    'amulet:prefix:essence-abyssal-mark',
    'amulet:suffix:essence-abyssal-mark',
  ],
  ESSENCE_BREACH: ['amulet:prefix:essence-maximum-quality'],
  RUNIC_ALLOY: ['amulet:prefix:alloy-maximum-runic-ward'],
  PERFECT_ESSENCE_INFINITE: [
    'amulet:suffix:essence-percent-strength',
    'amulet:suffix:essence-percent-dexterity',
    'amulet:suffix:essence-percent-intelligence',
  ],
}
export const stockyReplacementTargets: Partial<
  Record<WorkbenchAction, readonly string[]>
> = {
  ESSENCE_HYSTERIA: ['stocky-mitts:suffix:of-fury'],
  ESSENCE_HORROR: ['stocky-mitts:suffix:essence-socketed-augment-effect'],
  EXPANSIVE_ALLOY: ['stocky-mitts:suffix:alloy-remnant-pickup-range'],
  CYCLONIC_ALLOY: ['stocky-mitts:suffix:alloy-damaging-ailment-duration'],
  MYSTIC_ALLOY: ['stocky-mitts:suffix:alloy-attack-area-of-effect'],
  ADAPTIVE_ALLOY: ['stocky-mitts:suffix:alloy-attack-speed-missing-ward'],
  SWIFT_ALLOY: ['stocky-mitts:suffix:alloy-cast-speed'],
  SOVEREIGN_ALLOY: ['stocky-mitts:prefix:alloy-local-runic-ward'],
  PRISMATIC_ALLOY: ['stocky-mitts:prefix:alloy-elemental-penetration'],
  PERFECT_ESSENCE_GROUNDING: ['stocky-mitts:suffix:essence-lightning-recoup'],
  PERFECT_ESSENCE_OPULENCE: ['stocky-mitts:suffix:essence-gold-quantity'],
  ESSENCE_ABYSS: [
    'stocky-mitts:prefix:essence-abyssal-mark',
    'stocky-mitts:suffix:essence-abyssal-mark',
  ],
}
export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {
  Armourers_Scrap: 'ARMOURERS_SCRAP',
  Blacksmiths_Whetstone: 'BLACKSMITHS_WHETSTONE',
  Ancient_Diluted_Liquid_Ire: 'ANCIENT_DILUTED_LIQUID_IRE',
  Ancient_Diluted_Liquid_Guilt: 'ANCIENT_DILUTED_LIQUID_GUILT',
  Ancient_Diluted_Liquid_Greed: 'ANCIENT_DILUTED_LIQUID_GREED',
  Ancient_Liquid_Paranoia: 'ANCIENT_LIQUID_PARANOIA',
  Ancient_Liquid_Envy: 'ANCIENT_LIQUID_ENVY',
  Ancient_Liquid_Disgust: 'ANCIENT_LIQUID_DISGUST',
  Ancient_Liquid_Despair: 'ANCIENT_LIQUID_DESPAIR',
  Ancient_Concentrated_Liquid_Fear: 'ANCIENT_CONCENTRATED_LIQUID_FEAR',
  Ancient_Concentrated_Liquid_Suffering:
    'ANCIENT_CONCENTRATED_LIQUID_SUFFERING',
  Ancient_Concentrated_Liquid_Isolation:
    'ANCIENT_CONCENTRATED_LIQUID_ISOLATION',
  Ancient_Potent_Liquid_Melancholy: 'ANCIENT_POTENT_LIQUID_MELANCHOLY',
  Ancient_Potent_Liquid_Ferocity: 'ANCIENT_POTENT_LIQUID_FEROCITY',
  Ancient_Potent_Liquid_Contempt: 'ANCIENT_POTENT_LIQUID_CONTEMPT',
  Potent_Liquid_Melancholy: 'POTENT_LIQUID_MELANCHOLY',
  Potent_Liquid_Ferocity: 'POTENT_LIQUID_FEROCITY',
  Potent_Liquid_Contempt: 'POTENT_LIQUID_CONTEMPT',
  Diluted_Liquid_Ire: 'DILUTED_LIQUID_IRE',
  Diluted_Liquid_Guilt: 'DILUTED_LIQUID_GUILT',
  Diluted_Liquid_Greed: 'DILUTED_LIQUID_GREED',
  Liquid_Paranoia: 'LIQUID_PARANOIA',
  Liquid_Envy: 'LIQUID_ENVY',
  Liquid_Disgust: 'LIQUID_DISGUST',
  Liquid_Despair: 'LIQUID_DESPAIR',
  Concentrated_Liquid_Fear: 'CONCENTRATED_LIQUID_FEAR',
  Concentrated_Liquid_Suffering: 'CONCENTRATED_LIQUID_SUFFERING',
  Concentrated_Liquid_Isolation: 'CONCENTRATED_LIQUID_ISOLATION',
  Flesh_Catalyst: 'CATALYST_FLESH',
  Refined_Flesh_Catalyst: 'REFINED_CATALYST_FLESH',
  Neural_Catalyst: 'CATALYST_NEURAL',
  Refined_Neural_Catalyst: 'REFINED_CATALYST_NEURAL',
  Carapace_Catalyst: 'CATALYST_CARAPACE',
  Refined_Carapace_Catalyst: 'REFINED_CATALYST_CARAPACE',
  'Uul-Netols_Catalyst': 'CATALYST_UUL_NETOL',
  'Refined_Uul-Netols_Catalyst': 'REFINED_CATALYST_UUL_NETOL',
  Xophs_Catalyst: 'CATALYST_XOPH',
  Refined_Xophs_Catalyst: 'REFINED_CATALYST_XOPH',
  Tuls_Catalyst: 'CATALYST_TUL',
  Refined_Tuls_Catalyst: 'REFINED_CATALYST_TUL',
  Eshs_Catalyst: 'CATALYST_ESH',
  Refined_Eshs_Catalyst: 'REFINED_CATALYST_ESH',
  Chayulas_Catalyst: 'CATALYST_CHAYULA',
  Refined_Chayulas_Catalyst: 'REFINED_CATALYST_CHAYULA',
  Reaver_Catalyst: 'CATALYST_REAVER',
  Refined_Reaver_Catalyst: 'REFINED_CATALYST_REAVER',
  Sibilant_Catalyst: 'CATALYST_SIBILANT',
  Refined_Sibilant_Catalyst: 'REFINED_CATALYST_SIBILANT',
  Skittering_Catalyst: 'CATALYST_SKITTERING',
  Refined_Skittering_Catalyst: 'REFINED_CATALYST_SKITTERING',
  Adaptive_Catalyst: 'CATALYST_ADAPTIVE',
  Refined_Adaptive_Catalyst: 'REFINED_CATALYST_ADAPTIVE',
  Necrotic_Catalyst: 'CATALYST_NECROTIC',
  Refined_Necrotic_Catalyst: 'REFINED_CATALYST_NECROTIC',
  Lesser_Essence_of_Abrasion: 'LESSER_ESSENCE_ABRASION',
  Essence_of_Abrasion: 'ESSENCE_ABRASION',
  Greater_Essence_of_Abrasion: 'GREATER_ESSENCE_ABRASION',
  Lesser_Essence_of_Flames: 'LESSER_ESSENCE_FLAMES',
  Essence_of_Flames: 'ESSENCE_FLAMES',
  Greater_Essence_of_Flames: 'GREATER_ESSENCE_FLAMES',
  Lesser_Essence_of_Ice: 'LESSER_ESSENCE_ICE',
  Essence_of_Ice: 'ESSENCE_ICE',
  Greater_Essence_of_Ice: 'GREATER_ESSENCE_ICE',
  Lesser_Essence_of_Electricity: 'LESSER_ESSENCE_ELECTRICITY',
  Essence_of_Electricity: 'ESSENCE_ELECTRICITY',
  Greater_Essence_of_Electricity: 'GREATER_ESSENCE_ELECTRICITY',
  Lesser_Essence_of_Battle: 'LESSER_ESSENCE_BATTLE',
  Essence_of_Battle: 'ESSENCE_BATTLE',
  Lesser_Essence_of_Haste: 'LESSER_ESSENCE_HASTE',
  Essence_of_Haste: 'ESSENCE_HASTE',
  Greater_Essence_of_Haste: 'GREATER_ESSENCE_HASTE',
  Lesser_Essence_of_Seeking: 'LESSER_ESSENCE_SEEKING',
  Essence_of_Seeking: 'ESSENCE_SEEKING',
  Greater_Essence_of_Seeking: 'GREATER_ESSENCE_SEEKING',
  Artificers_Orb: 'ARTIFICER',
  ...currencyActions,
  Lesser_Essence_of_Enhancement: 'LESSER_ESSENCE_ENHANCEMENT',
  Essence_of_Enhancement: 'ESSENCE_ENHANCEMENT',
  Greater_Essence_of_Enhancement: 'GREATER_ESSENCE_ENHANCEMENT',
  Greater_Essence_of_Battle: 'GREATER_ESSENCE_BATTLE',
  Divine_Orb: 'DIVINE',
  Orb_of_Alchemy: 'ALCHEMY',
  Fracturing_Orb: 'FRACTURING',
  Essence_of_Hysteria: 'ESSENCE_HYSTERIA',
  Perfect_Essence_of_the_Infinite: 'PERFECT_ESSENCE_INFINITE',
  Perfect_Essence_of_Enhancement: 'PERFECT_ESSENCE_ENHANCEMENT',
  Essence_of_the_Abyss: 'ESSENCE_ABYSS',
  Essence_of_Horror: 'ESSENCE_HORROR',
  Lesser_Essence_of_Sorcery: 'LESSER_ESSENCE_SORCERY',
  Essence_of_Sorcery: 'ESSENCE_SORCERY',
  Greater_Essence_of_Sorcery: 'GREATER_ESSENCE_SORCERY',
  Lesser_Essence_of_Command: 'LESSER_ESSENCE_COMMAND',
  Essence_of_Command: 'ESSENCE_COMMAND',
  Greater_Essence_of_Command: 'GREATER_ESSENCE_COMMAND',
  Perfect_Essence_of_the_Mind: 'PERFECT_ESSENCE_MIND',
  Perfect_Essence_of_Thawing: 'PERFECT_ESSENCE_THAWING',
  Perfect_Essence_of_Insulation: 'PERFECT_ESSENCE_INSULATION',
  Perfect_Essence_of_Command: 'PERFECT_ESSENCE_COMMAND',
  Perfect_Essence_of_the_Body: 'PERFECT_ESSENCE_BODY',
  Perfect_Essence_of_Ruin: 'PERFECT_ESSENCE_RUIN',
  Perfect_Essence_of_Seeking: 'PERFECT_ESSENCE_SEEKING',
  Perfect_Essence_of_Sorcery: 'PERFECT_ESSENCE_SORCERY',
  Lesser_Essence_of_Alacrity: 'LESSER_ESSENCE_ALACRITY',
  Essence_of_Alacrity: 'ESSENCE_ALACRITY',
  Greater_Essence_of_Alacrity: 'GREATER_ESSENCE_ALACRITY',
  Perfect_Essence_of_Alacrity: 'PERFECT_ESSENCE_ALACRITY',
  Perfect_Essence_of_Abrasion: 'PERFECT_ESSENCE_ABRASION',
  Perfect_Essence_of_Flames: 'PERFECT_ESSENCE_FLAMES',
  Perfect_Essence_of_Ice: 'PERFECT_ESSENCE_ICE',
  Perfect_Essence_of_Electricity: 'PERFECT_ESSENCE_ELECTRICITY',
  Perfect_Essence_of_Battle: 'PERFECT_ESSENCE_BATTLE',
  Perfect_Essence_of_Haste: 'PERFECT_ESSENCE_HASTE',
  Perfect_Essence_of_Grounding: 'PERFECT_ESSENCE_GROUNDING',
  Perfect_Essence_of_Opulence: 'PERFECT_ESSENCE_OPULENCE',
  Essence_of_the_Breach: 'ESSENCE_BREACH',
  Protective_Alloy: 'PROTECTIVE_ALLOY',
  Celestial_Alloy: 'CELESTIAL_ALLOY',
  Transcendent_Alloy: 'TRANSCENDENT_ALLOY',
  The_Runebinders_Alloy: 'THE_RUNEBINDERS_ALLOY',
  The_Runefathers_Alloy: 'THE_RUNEFATHERS_ALLOY',
  Runic_Alloy: 'RUNIC_ALLOY',
  Expansive_Alloy: 'EXPANSIVE_ALLOY',
  Cyclonic_Alloy: 'CYCLONIC_ALLOY',
  Mystic_Alloy: 'MYSTIC_ALLOY',
  Adaptive_Alloy: 'ADAPTIVE_ALLOY',
  Swift_Alloy: 'SWIFT_ALLOY',
  Sovereign_Alloy: 'SOVEREIGN_ALLOY',
  Prismatic_Alloy: 'PRISMATIC_ALLOY',
  Lesser_Essence_of_the_Body: 'LESSER_ESSENCE_BODY',
  Essence_of_the_Body: 'ESSENCE_BODY',
  Greater_Essence_of_the_Body: 'GREATER_ESSENCE_BODY',
  Lesser_Essence_of_the_Mind: 'LESSER_ESSENCE_MIND',
  Essence_of_the_Mind: 'ESSENCE_MIND',
  Greater_Essence_of_the_Mind: 'GREATER_ESSENCE_MIND',
  Lesser_Essence_of_Ruin: 'LESSER_ESSENCE_RUIN',
  Essence_of_Ruin: 'ESSENCE_RUIN',
  Greater_Essence_of_Ruin: 'GREATER_ESSENCE_RUIN',
  Lesser_Essence_of_the_Infinite: 'LESSER_ESSENCE_INFINITE',
  Essence_of_the_Infinite: 'ESSENCE_INFINITE',
  Greater_Essence_of_the_Infinite: 'GREATER_ESSENCE_INFINITE',
  Lesser_Essence_of_Insulation: 'LESSER_ESSENCE_INSULATION',
  Essence_of_Insulation: 'ESSENCE_INSULATION',
  Greater_Essence_of_Insulation: 'GREATER_ESSENCE_INSULATION',
  Lesser_Essence_of_Thawing: 'LESSER_ESSENCE_THAWING',
  Essence_of_Thawing: 'ESSENCE_THAWING',
  Greater_Essence_of_Thawing: 'GREATER_ESSENCE_THAWING',
  Lesser_Essence_of_Grounding: 'LESSER_ESSENCE_GROUNDING',
  Essence_of_Grounding: 'ESSENCE_GROUNDING',
  Greater_Essence_of_Grounding: 'GREATER_ESSENCE_GROUNDING',
  Lesser_Essence_of_Opulence: 'LESSER_ESSENCE_OPULENCE',
  Essence_of_Opulence: 'ESSENCE_OPULENCE',
  Greater_Essence_of_Opulence: 'GREATER_ESSENCE_OPULENCE',
}
export const workbenchActionNames: Record<WorkbenchAction, string> = {
  ARMOURERS_SCRAP: "Armourer's Scrap",
  BLACKSMITHS_WHETSTONE: "Blacksmith's Whetstone",
  ANCIENT_DILUTED_LIQUID_IRE: 'Ancient Diluted Liquid Ire',
  ANCIENT_DILUTED_LIQUID_GUILT: 'Ancient Diluted Liquid Guilt',
  ANCIENT_DILUTED_LIQUID_GREED: 'Ancient Diluted Liquid Greed',
  ANCIENT_LIQUID_PARANOIA: 'Ancient Liquid Paranoia',
  ANCIENT_LIQUID_ENVY: 'Ancient Liquid Envy',
  ANCIENT_LIQUID_DISGUST: 'Ancient Liquid Disgust',
  ANCIENT_LIQUID_DESPAIR: 'Ancient Liquid Despair',
  ANCIENT_CONCENTRATED_LIQUID_FEAR: 'Ancient Concentrated Liquid Fear',
  ANCIENT_CONCENTRATED_LIQUID_SUFFERING:
    'Ancient Concentrated Liquid Suffering',
  ANCIENT_CONCENTRATED_LIQUID_ISOLATION:
    'Ancient Concentrated Liquid Isolation',
  ANCIENT_POTENT_LIQUID_MELANCHOLY: 'Ancient Potent Liquid Melancholy',
  ANCIENT_POTENT_LIQUID_FEROCITY: 'Ancient Potent Liquid Ferocity',
  ANCIENT_POTENT_LIQUID_CONTEMPT: 'Ancient Potent Liquid Contempt',
  POTENT_LIQUID_MELANCHOLY: 'Potent Liquid Melancholy',
  POTENT_LIQUID_FEROCITY: 'Potent Liquid Ferocity',
  POTENT_LIQUID_CONTEMPT: 'Potent Liquid Contempt',
  DILUTED_LIQUID_IRE: 'Diluted Liquid Ire',
  DILUTED_LIQUID_GUILT: 'Diluted Liquid Guilt',
  DILUTED_LIQUID_GREED: 'Diluted Liquid Greed',
  LIQUID_PARANOIA: 'Liquid Paranoia',
  LIQUID_ENVY: 'Liquid Envy',
  LIQUID_DISGUST: 'Liquid Disgust',
  LIQUID_DESPAIR: 'Liquid Despair',
  CONCENTRATED_LIQUID_FEAR: 'Concentrated Liquid Fear',
  CONCENTRATED_LIQUID_SUFFERING: 'Concentrated Liquid Suffering',
  CONCENTRATED_LIQUID_ISOLATION: 'Concentrated Liquid Isolation',
  CATALYST_FLESH: 'Flesh Catalyst',
  REFINED_CATALYST_FLESH: 'Refined Flesh Catalyst',
  CATALYST_NEURAL: 'Neural Catalyst',
  REFINED_CATALYST_NEURAL: 'Refined Neural Catalyst',
  CATALYST_CARAPACE: 'Carapace Catalyst',
  REFINED_CATALYST_CARAPACE: 'Refined Carapace Catalyst',
  CATALYST_UUL_NETOL: 'Uul-Netols Catalyst',
  REFINED_CATALYST_UUL_NETOL: 'Refined Uul-Netols Catalyst',
  CATALYST_XOPH: 'Xophs Catalyst',
  REFINED_CATALYST_XOPH: 'Refined Xophs Catalyst',
  CATALYST_TUL: 'Tuls Catalyst',
  REFINED_CATALYST_TUL: 'Refined Tuls Catalyst',
  CATALYST_ESH: 'Eshs Catalyst',
  REFINED_CATALYST_ESH: 'Refined Eshs Catalyst',
  CATALYST_CHAYULA: 'Chayulas Catalyst',
  REFINED_CATALYST_CHAYULA: 'Refined Chayulas Catalyst',
  CATALYST_REAVER: 'Reaver Catalyst',
  REFINED_CATALYST_REAVER: 'Refined Reaver Catalyst',
  CATALYST_SIBILANT: 'Sibilant Catalyst',
  REFINED_CATALYST_SIBILANT: 'Refined Sibilant Catalyst',
  CATALYST_SKITTERING: 'Skittering Catalyst',
  REFINED_CATALYST_SKITTERING: 'Refined Skittering Catalyst',
  CATALYST_ADAPTIVE: 'Adaptive Catalyst',
  REFINED_CATALYST_ADAPTIVE: 'Refined Adaptive Catalyst',
  CATALYST_NECROTIC: 'Necrotic Catalyst',
  REFINED_CATALYST_NECROTIC: 'Refined Necrotic Catalyst',
  PERFECT_ESSENCE_MIND: 'Perfect Essence of the Mind',
  PERFECT_ESSENCE_THAWING: 'Perfect Essence of Thawing',
  PERFECT_ESSENCE_INSULATION: 'Perfect Essence of Insulation',
  LESSER_ESSENCE_COMMAND: 'Lesser Essence of Command',
  ESSENCE_COMMAND: 'Essence of Command',
  GREATER_ESSENCE_COMMAND: 'Greater Essence of Command',
  PERFECT_ESSENCE_COMMAND: 'Perfect Essence of Command',
  PERFECT_ESSENCE_BODY: 'Perfect Essence of the Body',
  PERFECT_ESSENCE_RUIN: 'Perfect Essence of Ruin',
  PERFECT_ESSENCE_SEEKING: 'Perfect Essence of Seeking',
  LESSER_ESSENCE_ABRASION: 'Lesser Essence of Abrasion',
  ESSENCE_ABRASION: 'Essence of Abrasion',
  GREATER_ESSENCE_ABRASION: 'Greater Essence of Abrasion',
  LESSER_ESSENCE_FLAMES: 'Lesser Essence of Flames',
  ESSENCE_FLAMES: 'Essence of Flames',
  GREATER_ESSENCE_FLAMES: 'Greater Essence of Flames',
  LESSER_ESSENCE_ICE: 'Lesser Essence of Ice',
  ESSENCE_ICE: 'Essence of Ice',
  GREATER_ESSENCE_ICE: 'Greater Essence of Ice',
  LESSER_ESSENCE_ELECTRICITY: 'Lesser Essence of Electricity',
  ESSENCE_ELECTRICITY: 'Essence of Electricity',
  GREATER_ESSENCE_ELECTRICITY: 'Greater Essence of Electricity',
  LESSER_ESSENCE_BATTLE: 'Lesser Essence of Battle',
  ESSENCE_BATTLE: 'Essence of Battle',
  LESSER_ESSENCE_HASTE: 'Lesser Essence of Haste',
  ESSENCE_HASTE: 'Essence of Haste',
  GREATER_ESSENCE_HASTE: 'Greater Essence of Haste',
  LESSER_ESSENCE_SEEKING: 'Lesser Essence of Seeking',
  ESSENCE_SEEKING: 'Essence of Seeking',
  GREATER_ESSENCE_SEEKING: 'Greater Essence of Seeking',
  INSTALL_AUGMENT: 'Install Augment',
  ARTIFICER: "Artificer's Orb",
  ...actionNames,
  LESSER_ESSENCE_ENHANCEMENT: 'Lesser Essence of Enhancement',
  ESSENCE_ENHANCEMENT: 'Essence of Enhancement',
  GREATER_ESSENCE_ENHANCEMENT: 'Greater Essence of Enhancement',
  GREATER_ESSENCE_BATTLE: 'Greater Essence of Battle',
  DIVINE: 'Divine Orb',
  ALCHEMY: 'Orb of Alchemy',
  FRACTURING: 'Fracturing Orb',
  ESSENCE_HYSTERIA: 'Essence of Hysteria',
  PERFECT_ESSENCE_INFINITE: 'Perfect Essence of the Infinite',
  PERFECT_ESSENCE_ENHANCEMENT: 'Perfect Essence of Enhancement',
  ESSENCE_ABYSS: 'Essence of the Abyss',
  ESSENCE_HORROR: 'Essence of Horror',
  LESSER_ESSENCE_SORCERY: 'Lesser Essence of Sorcery',
  ESSENCE_SORCERY: 'Essence of Sorcery',
  GREATER_ESSENCE_SORCERY: 'Greater Essence of Sorcery',
  PERFECT_ESSENCE_SORCERY: 'Perfect Essence of Sorcery',
  LESSER_ESSENCE_ALACRITY: 'Lesser Essence of Alacrity',
  ESSENCE_ALACRITY: 'Essence of Alacrity',
  GREATER_ESSENCE_ALACRITY: 'Greater Essence of Alacrity',
  PERFECT_ESSENCE_ALACRITY: 'Perfect Essence of Alacrity',
  PERFECT_ESSENCE_ABRASION: 'Perfect Essence of Abrasion',
  PERFECT_ESSENCE_FLAMES: 'Perfect Essence of Flames',
  PERFECT_ESSENCE_ICE: 'Perfect Essence of Ice',
  PERFECT_ESSENCE_ELECTRICITY: 'Perfect Essence of Electricity',
  PERFECT_ESSENCE_BATTLE: 'Perfect Essence of Battle',
  PERFECT_ESSENCE_HASTE: 'Perfect Essence of Haste',
  PERFECT_ESSENCE_GROUNDING: 'Perfect Essence of Grounding',
  PERFECT_ESSENCE_OPULENCE: 'Perfect Essence of Opulence',
  ESSENCE_BREACH: 'Essence of the Breach',
  PROTECTIVE_ALLOY: 'Protective Alloy',
  CELESTIAL_ALLOY: 'Celestial Alloy',
  TRANSCENDENT_ALLOY: 'Transcendent Alloy',
  THE_RUNEBINDERS_ALLOY: 'The Runebinders Alloy',
  THE_RUNEFATHERS_ALLOY: 'The Runefathers Alloy',
  RUNIC_ALLOY: 'Runic Alloy',
  EXPANSIVE_ALLOY: 'Expansive Alloy',
  CYCLONIC_ALLOY: 'Cyclonic Alloy',
  MYSTIC_ALLOY: 'Mystic Alloy',
  ADAPTIVE_ALLOY: 'Adaptive Alloy',
  SWIFT_ALLOY: 'Swift Alloy',
  SOVEREIGN_ALLOY: 'Sovereign Alloy',
  PRISMATIC_ALLOY: 'Prismatic Alloy',
  LESSER_ESSENCE_BODY: 'Lesser Essence of the Body',
  ESSENCE_BODY: 'Essence of the Body',
  GREATER_ESSENCE_BODY: 'Greater Essence of the Body',
  LESSER_ESSENCE_MIND: 'Lesser Essence of the Mind',
  ESSENCE_MIND: 'Essence of the Mind',
  GREATER_ESSENCE_MIND: 'Greater Essence of the Mind',
  LESSER_ESSENCE_RUIN: 'Lesser Essence of Ruin',
  ESSENCE_RUIN: 'Essence of Ruin',
  GREATER_ESSENCE_RUIN: 'Greater Essence of Ruin',
  LESSER_ESSENCE_INFINITE: 'Lesser Essence of the Infinite',
  ESSENCE_INFINITE: 'Essence of the Infinite',
  GREATER_ESSENCE_INFINITE: 'Greater Essence of the Infinite',
  LESSER_ESSENCE_INSULATION: 'Lesser Essence of Insulation',
  ESSENCE_INSULATION: 'Essence of Insulation',
  GREATER_ESSENCE_INSULATION: 'Greater Essence of Insulation',
  LESSER_ESSENCE_THAWING: 'Lesser Essence of Thawing',
  ESSENCE_THAWING: 'Essence of Thawing',
  GREATER_ESSENCE_THAWING: 'Greater Essence of Thawing',
  LESSER_ESSENCE_GROUNDING: 'Lesser Essence of Grounding',
  ESSENCE_GROUNDING: 'Essence of Grounding',
  GREATER_ESSENCE_GROUNDING: 'Greater Essence of Grounding',
  LESSER_ESSENCE_OPULENCE: 'Lesser Essence of Opulence',
  ESSENCE_OPULENCE: 'Essence of Opulence',
  GREATER_ESSENCE_OPULENCE: 'Greater Essence of Opulence',
} as Record<WorkbenchAction, string>
for (const [id, base] of Object.entries(currencyActions)) {
  if (base === 'ANNULMENT') continue
  for (const tier of ['Greater', 'Perfect'] as const) {
    const action = `${tier.toUpperCase()}_${base}` as WorkbenchAction
    workbenchCurrencyActions[`${tier}_${id}`] = action
    workbenchActionNames[action] = `${tier} ${actionNames[base]}`
  }
}
export const workbenchOmens = [
  { id: 'Omen_of_Light', trigger: 'ANNULMENT' },
  {
    id: 'Omen_of_Sanctification',
    effect: 'Sanctify a Rare item',
    trigger: 'DIVINE',
  },
  {
    id: 'Omen_of_Putrefaction',
    trigger: 'DESECRATE_ALL',
    effect: 'Record all observed desecrated slots and corruption',
  },
  ...['Sovereign', 'Liege', 'Blackblooded'].map((name) => ({
    id: `Omen_of_the_${name}`,
    trigger: 'DESECRATE_LICH',
    effect: 'Restrict the reveal pool',
  })),
  {
    id: 'Omen_of_Sinistral_Necromancy',
    trigger: 'DESECRATE',
    effect: 'Add only a prefix on desecration',
  },
  {
    id: 'Omen_of_Dextral_Necromancy',
    trigger: 'DESECRATE',
    effect: 'Add only a suffix on desecration',
  },
  {
    id: 'Omen_of_Sinistral_Alchemy',
    trigger: 'ALCHEMY',
    effect: 'Legacy: maximum prefixes on ordinary Alchemy',
  },
  {
    id: 'Omen_of_Dextral_Alchemy',
    trigger: 'ALCHEMY',
    effect: 'Legacy: maximum suffixes on ordinary Alchemy',
  },
  {
    id: 'Omen_of_Sinistral_Coronation',
    trigger: 'REGAL',
    effect: 'Legacy: add only a prefix on ordinary Regal',
  },
  {
    id: 'Omen_of_Dextral_Coronation',
    trigger: 'REGAL',
    effect: 'Legacy: add only a suffix on ordinary Regal',
  },
  {
    id: 'Omen_of_Greater_Annulment',
    trigger: 'ANNULMENT',
    effect: 'Legacy: remove two unlocked explicit modifiers',
  },
  {
    id: 'Omen_of_Homogenising_Exaltation',
    trigger: 'EXALTED',
    effect:
      'Legacy: match existing modifier tags; compatible with Greater Exaltation only',
  },
  {
    id: 'Omen_of_Homogenising_Coronation',
    trigger: 'REGAL',
    effect: 'Legacy: match existing modifier tags on ordinary Regal Orb',
  },
  {
    id: 'Omen_of_Sinistral_Crystallisation',
    trigger: 'ESSENCE_HYSTERIA',
    effect:
      'Hysteria / Perfect Infinite / Perfect Enhancement / Breach / Abyss remove only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Crystallisation',
    trigger: 'ESSENCE_HYSTERIA',
    effect:
      'Hysteria / Perfect Infinite / Perfect Enhancement / Breach / Abyss remove only suffixes',
  },
  {
    id: 'Omen_of_Greater_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add two modifiers; requires two free slots in Workbench',
  },
  {
    id: 'Omen_of_Sinistral_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add only suffixes',
  },
  {
    id: 'Omen_of_Sinistral_Annulment',
    trigger: 'ANNULMENT',
    effect: 'Remove only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Annulment',
    trigger: 'ANNULMENT',
    effect: 'Remove only suffixes',
  },
  {
    id: 'Omen_of_Sinistral_Erasure',
    trigger: 'CHAOS',
    effect: 'Remove only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Erasure',
    trigger: 'CHAOS',
    effect: 'Remove only suffixes',
  },
  {
    id: 'Omen_of_Whittling',
    trigger: 'CHAOS',
    effect: 'Remove the lowest modifier level; tied candidates are uniform',
  },
  {
    id: 'Omen_of_the_Blessed',
    trigger: 'DIVINE',
    effect: 'Reroll only implicit values',
  },
] as const
export const legacyHomogenisingIds: readonly string[] = [
  'Omen_of_Homogenising_Exaltation',
  'Omen_of_Homogenising_Coronation',
]
export const legacyFiveIds: readonly string[] = [
  'Omen_of_Sinistral_Alchemy',
  'Omen_of_Dextral_Alchemy',
  'Omen_of_Sinistral_Coronation',
  'Omen_of_Dextral_Coronation',
  'Omen_of_Greater_Annulment',
]
export const legacyOmenIds = [...legacyHomogenisingIds, ...legacyFiveIds]
export function compatibleOmenPair(first: string, second: string): boolean {
  return (
    new Set([first, second]).size === 2 &&
    [
      ['Omen_of_Homogenising_Exaltation', 'Omen_of_Greater_Exaltation'],
      ['Omen_of_Sinistral_Exaltation', 'Omen_of_Greater_Exaltation'],
      ['Omen_of_Dextral_Exaltation', 'Omen_of_Greater_Exaltation'],
      ['Omen_of_Sinistral_Erasure', 'Omen_of_Whittling'],
      ['Omen_of_Dextral_Erasure', 'Omen_of_Whittling'],
      ['Omen_of_Sinistral_Annulment', 'Omen_of_Greater_Annulment'],
      ['Omen_of_Dextral_Annulment', 'Omen_of_Greater_Annulment'],
    ].some((pair) => pair.includes(first) && pair.includes(second))
  )
}
export const baseWorkbenchAction = (action: WorkbenchAction) =>
  action.replace(/^(GREATER|PERFECT)_/, '') as
    Action | 'DIVINE' | 'ALCHEMY' | 'FRACTURING'
