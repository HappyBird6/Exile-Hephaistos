import { topBase, topBaseKey } from './topBases'
import reviewedEssenceTargets from './topBaseEssences.json'
import { supportsConcreteStateShape } from './workbenchStateShape'
import displayBindings from '../../shared/i18n/modifierTemplates.json'
import liquidTargets from './basicJewelLiquidTargets.json'
import {
  isBasicJewel,
  workbenchJewelBases,
  isWorkbenchJewel,
  reviewedBasicJewel,
  jewelCapacity,
} from './basicJewel'
import { qualityCapChangeMatches } from './qualityCapChangePolicy'
import { whittlingCandidates } from './omenRemovalCandidates'
import {
  catalystBase,
  catalystTypes,
  verifiedCatalystQuality,
} from './catalystQuality'
import type { CatalystQuality } from './catalystQuality'
import { maximumQuality, qualityLimitMatches } from './qualityLimit'
import type { QualityLimit } from './qualityLimit'
import { currencyActions, actionNames } from './craftingApi'
import type { Action, Bucket, Definition, Initial } from './craftingApi'

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
  | 'ARTIFICER'
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
const bowFixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
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
const fixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
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
const stockyFixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
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
const choiceEssenceModifiers: Partial<
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
const wandFixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
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
const sceptreFixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
  LESSER_ESSENCE_COMMAND: 'rattling-sceptre:prefix:agitative',
  ESSENCE_COMMAND: 'rattling-sceptre:prefix:provocative',
  GREATER_ESSENCE_COMMAND: 'rattling-sceptre:prefix:motivating',
}
const replacementEssenceModifiers: Partial<
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
export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {
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

export interface ConcreteItem extends Omit<Bucket, 'modifierIds'> {
  catalystQuality?: CatalystQuality | null | undefined
  augmentSockets?: number | null | undefined
  explicits: {
    modifierId: string
    values: Record<string, number>
    fractured?: boolean
  }[]
}
export interface MappingResult {
  mapped: boolean
  state: ConcreteItem | null
  issues: { lineNumber: number; message: string }[]
}
export async function mapSolarText(
  text: string,
  signal: AbortSignal,
  definitions: Record<string, Definition>,
): Promise<MappingResult> {
  const response = await fetch('/api/v1/crafting/workbench/map-text', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
  if (!response.ok)
    throw new Error(
      'Could not validate this item for crafting. The displayed text is preserved.',
    )
  const v = (await response.json()) as MappingResult
  if (
    typeof v?.mapped !== 'boolean' ||
    !Array.isArray(v.issues) ||
    !v.issues.every(
      (i) => Number.isInteger(i.lineNumber) && typeof i.message === 'string',
    ) ||
    (v.mapped
      ? !v.state || !Array.isArray(v.state.explicits) || v.issues.length !== 0
      : v.state !== null)
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  if (
    v.state &&
    (!supportsConcreteStateShape(v.state) ||
      typeof v.state.snapshotId !== 'string' ||
      typeof v.state.baseItemId !== 'string' ||
      !Number.isInteger(v.state.itemLevel) ||
      v.state.itemLevel < 1 ||
      v.state.itemLevel > 100 ||
      !['NORMAL', 'MAGIC', 'RARE'].includes(v.state.rarity) ||
      !Array.isArray(v.state.implicits) ||
      v.state.implicits.length !== 1 ||
      !Array.isArray(v.state.conditions) ||
      v.state.conditions.length !== 0 ||
      v.state.explicits.length > 6 ||
      ![...v.state.implicits, ...v.state.explicits].every(
        (m) =>
          m &&
          definitions[m.modifierId]?.stats &&
          typeof m.values === 'object' &&
          m.values !== null &&
          Object.keys(m.values).length ===
            definitions[m.modifierId]!.stats!.length &&
          definitions[m.modifierId]!.stats!.every(
            (s) =>
              Number.isSafeInteger(m.values[s.id]) &&
              m.values[s.id]! >= s.min &&
              m.values[s.id]! <= s.max,
          ),
      ))
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  return v
}
export interface RollAssumption {
  id: string
  candidateUnit: string
  n: number
  candidates: string[]
  min: number | null
  max: number | null
  sourceUrl: string
  reason: string
  ratioTick?: number | null
}
export interface AppliedItem {
  qualityLimit?: QualityLimit | null
  ruleVersion: string
  ledgerVersion: string
  snapshotId: string
  state: ConcreteItem
  action: WorkbenchAction
  applied: boolean
  reason: string
  events: {
    kind: 'ADD' | 'REMOVE' | 'REROLL_IMPLICIT' | 'REROLL_EXPLICIT' | 'FRACTURE'
    modifierId: string
    values: Record<string, number>
    selectionProbability: number
  }[]
  consumedOmens: string[]
  remainingOmens: string[]
  assumptions: RollAssumption[]
}

export function coupledModelsMatch(
  result: Pick<AppliedItem, 'events' | 'assumptions' | 'state'>,
  definitions: Record<string, Definition>,
): boolean {
  const rolled = result.events.filter(
    (event) =>
      ['ADD', 'REROLL_IMPLICIT', 'REROLL_EXPLICIT'].includes(event.kind) &&
      (definitions[event.modifierId]?.stats?.length ?? 0) > 1,
  )
  const models = result.assumptions.filter(
    (a) => a.id === 'user-coupled-ratio-half-up-v1',
  )
  if (models.length !== rolled.length) return false
  const bound = new Set<string>()
  return models.every((a) => {
    const id = a.candidates[0]
    const tick = a.ratioTick
    if (
      !id ||
      a.candidates.length !== 1 ||
      bound.has(id) ||
      a.n !== 10001 ||
      a.min !== 0 ||
      a.max !== 10000 ||
      !Number.isSafeInteger(tick) ||
      tick == null ||
      tick < 0 ||
      tick > 10000
    )
      return false
    bound.add(id)
    const event = rolled.find((e) => e.modifierId === id)
    const instance = [
      ...result.state.implicits,
      ...result.state.explicits,
    ].find((m) => m.modifierId === id)
    const stats = definitions[id]?.stats
    if (
      !event ||
      !instance ||
      !stats ||
      stats.length < 2 ||
      Object.keys(event.values).length !== stats.length
    )
      return false
    return stats.every((s) => {
      if (
        !Number.isSafeInteger(s.min) ||
        !Number.isSafeInteger(s.max) ||
        s.min > s.max
      )
        return false
      const numerator =
        BigInt(s.min) * BigInt(10000 - tick) + BigInt(s.max) * BigInt(tick)
      const sign = numerator < 0n ? -1n : 1n
      const absolute = numerator < 0n ? -numerator : numerator
      const expected = Number(sign * ((absolute + 5000n) / 10000n))
      return (
        Number.isSafeInteger(expected) &&
        event.values[s.id] === expected &&
        instance.values[s.id] === expected
      )
    })
  })
}
export function concreteInitial(initial: Initial): ConcreteItem {
  return {
    snapshotId: initial.state.snapshotId,
    baseItemId: initial.state.baseItemId,
    itemLevel: initial.state.itemLevel,
    rarity: initial.state.rarity,
    implicits: initial.state.implicits,
    conditions: initial.state.conditions,
    explicits: [],
    ...(initial.augmentSockets === undefined
      ? {}
      : { augmentSockets: initial.augmentSockets }),
  }
}
export async function applyCurrency(
  state: ConcreteItem,
  action: WorkbenchAction,
  definitions: Record<string, Definition>,
  signal: AbortSignal,
  activeOmens: string[] = [],
): Promise<AppliedItem> {
  if (!supportsConcreteStateShape(state))
    throw new Error(
      'This item contains unsupported properties. Its state has not been changed.',
    )

  const response = await fetch('/api/v1/crafting/workbench/apply', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ state, action, activeOmens }),
  })
  if (!response.ok)
    throw new Error(
      'Could not apply currency. Your item is unchanged. Please retry.',
    )
  const v = (await response.json()) as AppliedItem
  const next = v?.state
  const baseAction = baseWorkbenchAction(action)
  const sameValues = (
    a: Record<string, number> | undefined,
    b: Record<string, number> | undefined,
  ) =>
    Boolean(a && b) &&
    Object.keys(a!).length === Object.keys(b!).length &&
    Object.entries(a!).every(
      ([key, value]) => Object.hasOwn(b!, key) && b![key] === value,
    )
  const fixedTarget =
    state.baseItemId ===
    'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
      ? sceptreFixedEssenceModifiers[action]
      : state.baseItemId ===
          'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
        ? wandFixedEssenceModifiers[action]
        : state.baseItemId ===
            'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
          ? bowFixedEssenceModifiers[action]
          : state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
            ? (stockyFixedEssenceModifiers[action] ??
              fixedEssenceModifiers[action])
            : fixedEssenceModifiers[action]
  const reviewedKey = topBaseKey(state.baseItemId)
  const reviewedTargets =
    reviewedKey &&
    ['gloves', 'helmets', 'body', 'boots', 'bows'].includes(
      topBase(reviewedKey)?.family ?? '',
    )
      ? (
          reviewedEssenceTargets as Record<
            string,
            {
              fixed: Record<string, string[]>
              replacements: Record<string, string[]>
            }
          >
        )[reviewedKey]
      : undefined
  const essenceCandidates = reviewedTargets
    ? (reviewedTargets.fixed[action] ?? [])
    : fixedTarget
      ? [fixedTarget]
      : (choiceEssenceModifiers[action] ?? [])
  const stockyReplacementTargets: Partial<
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
  const baseName = Object.keys(workbenchJewelBases).find(
    (b) =>
      (workbenchJewelBases as Record<string, string>)[b] === state.baseItemId,
  )
  const replacementTargets = action.includes('LIQUID_')
    ? baseName
      ? ((liquidTargets as Record<string, Record<string, string[]>>)[
          baseName
        ]?.[action] ?? [])
      : []
    : reviewedTargets
      ? (reviewedTargets.replacements[action] ??
        ([
          'PERFECT_ESSENCE_GROUNDING',
          'PERFECT_ESSENCE_OPULENCE',
          'ESSENCE_ABYSS',
        ].includes(action)
          ? stockyReplacementTargets[action]
          : []) ??
        [])
      : ((state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
          ? (stockyReplacementTargets[action] ??
            replacementEssenceModifiers[action])
          : replacementEssenceModifiers[action]) ?? [])
  const sameModifiers = (
    a: ConcreteItem['explicits'],
    b: ConcreteItem['explicits'],
  ) =>
    Array.isArray(a) &&
    Array.isArray(b) &&
    a.length === b.length &&
    a.every(
      (m, i) =>
        m.modifierId === b[i]?.modifierId &&
        Boolean(m.fractured) === Boolean(b[i]?.fractured) &&
        sameValues(m.values, b[i]?.values),
    )
  const validValues = (values: Record<string, number>) =>
    values &&
    typeof values === 'object' &&
    Object.values(values).every(Number.isSafeInteger)
  if (
    !next ||
    !supportsConcreteStateShape(next) ||
    !verifiedCatalystQuality(next, definitions) ||
    (isWorkbenchJewel(next.baseItemId) && !reviewedBasicJewel(next)) ||
    (v.applied &&
      action.includes('LIQUID_') &&
      (!isWorkbenchJewel(state.baseItemId) ||
        state.explicits.some((m) =>
          definitions[m.modifierId]?.tags?.includes('crafted'),
        ))) ||
    (catalystActionType(action) === null &&
      !qualityCapChangeMatches(state, v, definitions)) ||
    (state.baseItemId === 'Metadata/Items/Rings/FourRing1' &&
      (!sameModifiers(next.implicits, state.implicits) ||
        next.implicits.length !== 1 ||
        next.implicits[0]?.modifierId !==
          'iron-ring:implicit:added-physical-damage-to-attacks' ||
        Object.keys(next.implicits[0].values).length !== 2 ||
        next.implicits[0].values.attack_minimum_added_physical_damage !== 1 ||
        next.implicits[0].values.attack_maximum_added_physical_damage !== 4 ||
        Boolean(next.implicits[0].fractured))) ||
    (v.qualityLimit !== undefined &&
      (v.qualityLimit === null
        ? ![
            'Metadata/Items/Jewels/JewelRadiusStr',
            'Metadata/Items/Jewels/JewelRadiusDex',
            'Metadata/Items/Jewels/JewelRadiusInt',
            'Metadata/Items/Jewels/JewelRadiusDiamond',
            'Metadata/Items/Belts/FourBelt1',
            'Metadata/Items/Rings/FourRing1',
          ].includes(next.baseItemId) ||
          maximumQuality(next, definitions) !== null
        : !qualityLimitMatches(v.qualityLimit, next, definitions))) ||
    (v.applied &&
      [
        'LESSER_ESSENCE_COMMAND',
        'ESSENCE_COMMAND',
        'GREATER_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_COMMAND',
      ].includes(action) &&
      state.baseItemId !==
        'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1') ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_INSULATION' &&
      state.baseItemId !== 'Metadata/Items/Belts/FourBelt1') ||
    (v.applied &&
      action === 'DIVINE' &&
      state.baseItemId === 'Metadata/Items/Belts/FourBelt1') ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_MIND' &&
      state.baseItemId !== 'Metadata/Items/Rings/FourRing1') ||
    (v.applied &&
      action === 'PERFECT_ESSENCE_THAWING' &&
      state.baseItemId !== 'Metadata/Items/Armours/Helmets/FourHelmetStr1' &&
      !['helmet', 'helmets'].includes(
        topBase(reviewedKey ?? '')?.family ?? '',
      )) ||
    (v.applied && action === 'PRISMATIC_ALLOY' && state.itemLevel < 45) ||
    (v.applied &&
      [
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
      ].includes(action) &&
      state.baseItemId !== 'Metadata/Items/Armours/BodyArmours/FourBodyStr1' &&
      topBase(reviewedKey ?? '')?.family !== 'body') ||
    (v.applied &&
      ['EXPANSIVE_ALLOY', 'ADAPTIVE_ALLOY', 'SOVEREIGN_ALLOY'].includes(
        action,
      ) &&
      state.itemLevel < 25) ||
    (v.applied &&
      ['CYCLONIC_ALLOY', 'MYSTIC_ALLOY', 'SWIFT_ALLOY'].includes(action) &&
      state.itemLevel < 45) ||
    (v.applied &&
      [
        'PERFECT_ESSENCE_MIND',
        'PERFECT_ESSENCE_THAWING',
        'PERFECT_ESSENCE_INSULATION',
        'PERFECT_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
        'PERFECT_ESSENCE_SORCERY',
        'PERFECT_ESSENCE_ALACRITY',
        'PERFECT_ESSENCE_ABRASION',
        'PERFECT_ESSENCE_FLAMES',
        'PERFECT_ESSENCE_ICE',
        'PERFECT_ESSENCE_ELECTRICITY',
        'PERFECT_ESSENCE_BATTLE',
        'PERFECT_ESSENCE_HASTE',
        'PERFECT_ESSENCE_GROUNDING',
        'PERFECT_ESSENCE_OPULENCE',
      ].includes(action) &&
      state.itemLevel < 72) ||
    (v.applied &&
      [
        'ESSENCE_HORROR',
        'EXPANSIVE_ALLOY',
        'CYCLONIC_ALLOY',
        'MYSTIC_ALLOY',
        'ADAPTIVE_ALLOY',
        'SWIFT_ALLOY',
        'SOVEREIGN_ALLOY',
        'PRISMATIC_ALLOY',
        'PERFECT_ESSENCE_GROUNDING',
        'PERFECT_ESSENCE_OPULENCE',
      ].includes(action) &&
      state.baseItemId !== 'Metadata/Items/Armours/Gloves/FourGlovesStr1' &&
      state.baseItemId !==
        'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1' &&
      topBase(reviewedKey ?? '')?.family !== 'bows') ||
    v.action !== action ||
    typeof v.applied !== 'boolean' ||
    typeof v.reason !== 'string' ||
    typeof v.ruleVersion !== 'string' ||
    typeof v.ledgerVersion !== 'string' ||
    v.snapshotId !== state.snapshotId ||
    next.snapshotId !== state.snapshotId ||
    next.baseItemId !== state.baseItemId ||
    next.itemLevel !== state.itemLevel ||
    !['NORMAL', 'MAGIC', 'RARE'].includes(next.rarity) ||
    (action !== 'DIVINE' && !sameModifiers(next.implicits, state.implicits)) ||
    !Array.isArray(next.implicits) ||
    next.implicits.length !== state.implicits.length ||
    next.implicits.some((m) => m.fractured) ||
    next.implicits[0]?.modifierId !== state.implicits[0]?.modifierId ||
    !next.implicits.every((m) =>
      definitions[m.modifierId]?.stats?.every(
        (r) =>
          Number.isSafeInteger(m.values[r.id]) &&
          m.values[r.id]! >= r.min &&
          m.values[r.id]! <= r.max,
      ),
    ) ||
    !Array.isArray(next.conditions) ||
    next.conditions.length !== 0 ||
    !Array.isArray(next.explicits) ||
    next.explicits.length > 6 ||
    !next.explicits.every(
      (m) =>
        definitions[m.modifierId] &&
        validValues(m.values) &&
        (m.fractured === undefined || typeof m.fractured === 'boolean') &&
        definitions[m.modifierId]!.stats?.length ===
          Object.keys(m.values).length &&
        definitions[m.modifierId]!.stats?.every(
          (s) =>
            Number.isSafeInteger(m.values[s.id]) &&
            m.values[s.id]! >= s.min &&
            m.values[s.id]! <= s.max,
        ),
    ) ||
    !Array.isArray(v.events) ||
    !v.events.every(
      (e) =>
        [
          'ADD',
          'REMOVE',
          'REROLL_IMPLICIT',
          'REROLL_EXPLICIT',
          'FRACTURE',
        ].includes(e.kind) &&
        definitions[e.modifierId] &&
        validValues(e.values) &&
        Number.isFinite(e.selectionProbability) &&
        e.selectionProbability > 0 &&
        e.selectionProbability <= 1,
    ) ||
    !Array.isArray(v.consumedOmens) ||
    !Array.isArray(v.remainingOmens) ||
    [...v.consumedOmens, ...v.remainingOmens].length !== activeOmens.length ||
    new Set([...v.consumedOmens, ...v.remainingOmens]).size !==
      activeOmens.length ||
    ![...v.consumedOmens, ...v.remainingOmens].every((id) =>
      activeOmens.includes(id),
    ) ||
    (!v.applied && v.consumedOmens.length !== 0) ||
    !Array.isArray(v.assumptions) ||
    !v.assumptions.every(
      (a) =>
        typeof a.id === 'string' &&
        typeof a.candidateUnit === 'string' &&
        Number.isSafeInteger(a.n) &&
        a.n > 0 &&
        Array.isArray(a.candidates) &&
        a.candidates.every((c) => typeof c === 'string') &&
        typeof a.reason === 'string' &&
        typeof a.sourceUrl === 'string',
    )
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const catalystType = catalystActionType(action)
  if (
    catalystType &&
    (!sameModifiers(next.explicits, state.explicits) ||
      next.rarity !== state.rarity ||
      v.events.length !== 0 ||
      v.assumptions.length !== 0 ||
      v.consumedOmens.length !== 0 ||
      (v.applied
        ? action.startsWith('REFINED_') !== isBasicJewel(state.baseItemId) ||
          !catalystBase(state.baseItemId) ||
          next.catalystQuality?.type !== catalystType ||
          next.catalystQuality?.amount !==
            Math.max(
              state.catalystQuality?.amount ?? 0,
              maximumQuality(state, definitions) ?? 0,
            )
        : next.catalystQuality?.type !== state.catalystQuality?.type ||
          next.catalystQuality?.amount !== state.catalystQuality?.amount))
  )
    throw new Error(
      'Could not verify the catalyst policy. Your item is unchanged.',
    )
  if (!coupledModelsMatch(v, definitions))
    throw new Error(
      'Could not verify the coupled roll model. Your item is unchanged. Please retry.',
    )
  const oldSockets = state.augmentSockets ?? null
  const newSockets = next.augmentSockets ?? null
  if (
    (action === 'ARTIFICER' &&
      v.applied &&
      (state.baseItemId !== 'Metadata/Items/Armours/Gloves/FourGlovesStr1' ||
        oldSockets !== 0 ||
        newSockets !== 1 ||
        next.rarity !== state.rarity ||
        !sameModifiers(next.explicits, state.explicits) ||
        !sameModifiers(next.implicits, state.implicits) ||
        v.events.length !== 0 ||
        v.assumptions.length !== 0 ||
        v.consumedOmens.length !== 0)) ||
    ((action !== 'ARTIFICER' || !v.applied) && newSockets !== oldSockets)
  )
    throw new Error(
      'Could not verify the Augment Sockets. Your item is unchanged. Please retry.',
    )
  const families = new Set<string>()
  let prefixes = 0
  let suffixes = 0
  for (const instance of next.explicits) {
    const d = definitions[instance.modifierId]!
    if (d.familyIds.some((id) => families.has(id)) || d.affixType === 'NONE')
      throw new Error(
        'Could not verify the applied item. Your item is unchanged. Please retry.',
      )
    d.familyIds.forEach((id) => families.add(id))
    if (d.affixType === 'PREFIX') prefixes++
    else suffixes++
  }
  const capacity =
    next.rarity === 'NORMAL'
      ? 0
      : next.rarity === 'MAGIC'
        ? 1
        : isWorkbenchJewel(next.baseItemId)
          ? 2
          : 3
  const expectedRarity =
    baseAction === 'TRANSMUTATION'
      ? 'MAGIC'
      : baseAction === 'REGAL' ||
          action === 'ALCHEMY' ||
          essenceCandidates.length > 0
        ? 'RARE'
        : state.rarity
  const expectedCount =
    action === 'ALCHEMY'
      ? 4
      : state.explicits.length +
        (baseAction === 'ANNULMENT'
          ? activeOmens.includes('Omen_of_Greater_Annulment')
            ? -2
            : -1
          : baseAction === 'CHAOS' ||
              action === 'DIVINE' ||
              action === 'ARTIFICER' ||
              action === 'FRACTURING' ||
              catalystActionType(action) !== null ||
              replacementTargets.length > 0
            ? 0
            : baseAction === 'EXALTED' &&
                activeOmens.includes('Omen_of_Greater_Exaltation')
              ? 2
              : 1)
  if (
    (isWorkbenchJewel(next.baseItemId)
      ? !reviewedBasicJewel(next)
      : prefixes > capacity || suffixes > capacity) ||
    (v.applied &&
      (next.rarity !== expectedRarity ||
        next.explicits.length !== expectedCount)) ||
    (!v.applied &&
      (!sameModifiers(next.explicits, state.explicits) ||
        next.rarity !== state.rarity ||
        !sameModifiers(next.implicits, state.implicits) ||
        v.events.length !== 0 ||
        v.assumptions.length !== 0))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  if (
    action === 'DIVINE' &&
    (next.explicits.map((m) => m.modifierId).join('|') !==
      state.explicits.map((m) => m.modifierId).join('|') ||
      (activeOmens.includes('Omen_of_the_Blessed') &&
        !sameModifiers(next.explicits, state.explicits)))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const locks = next.explicits.filter((m) => m.fractured)
  if (v.applied && replacementTargets.length > 0) {
    const matching = workbenchOmens.filter(
      (o) =>
        ![
          'RUNIC_ALLOY',
          'ADAPTIVE_ALLOY',
          'SWIFT_ALLOY',
          'SOVEREIGN_ALLOY',
          'PRISMATIC_ALLOY',
          'EXPANSIVE_ALLOY',
          'CYCLONIC_ALLOY',
          'MYSTIC_ALLOY',
        ].includes(action) &&
        !action.includes('LIQUID_') &&
        o.trigger === 'ESSENCE_HYSTERIA' &&
        activeOmens.includes(o.id),
    )
    const side =
      matching[0]?.id === 'Omen_of_Sinistral_Crystallisation'
        ? 'PREFIX'
        : matching[0]?.id === 'Omen_of_Dextral_Crystallisation'
          ? 'SUFFIX'
          : null
    const candidates = state.explicits.filter(
      (m) =>
        !m.fractured &&
        (!side || definitions[m.modifierId]?.affixType === side) &&
        (!action.includes('LIQUID_') ||
          replacementTargets.some((id) => {
            const target = definitions[id]
            if (!target) return false
            const rest = state.explicits
              .filter((old) => old.modifierId !== m.modifierId)
              .map((old) => definitions[old.modifierId]!)
            return (
              rest.filter((d) => d.affixType === target.affixType).length <
                jewelCapacity(state, target.affixType) &&
              rest.every(
                (d) => !d.familyIds.some((f) => target.familyIds.includes(f)),
              )
            )
          })),
    )
    const removed = v.events[0]
    const validTargets = action.includes('LIQUID_')
      ? replacementTargets.filter((id) => {
          const target = definitions[id]!
          const restState = {
            ...state,
            explicits: state.explicits.filter(
              (m) => m.modifierId !== removed?.modifierId,
            ),
          }
          const rest = restState.explicits.map(
            (m) => definitions[m.modifierId]!,
          )
          return (
            rest.filter((d) => d.affixType === target.affixType).length <
              jewelCapacity(restState, target.affixType) &&
            rest.every(
              (d) => !d.familyIds.some((f) => target.familyIds.includes(f)),
            )
          )
        })
      : replacementTargets
    const added = v.events[1]
    const old = candidates.find((m) => m.modifierId === removed?.modifierId)
    const target = next.explicits.find(
      (m) => m.modifierId === added?.modifierId,
    )
    const preserved = state.explicits.filter(
      (m) => m.modifierId !== old?.modifierId,
    )
    if (
      state.rarity !== 'RARE' ||
      v.events.length !== 2 ||
      removed?.kind !== 'REMOVE' ||
      !old ||
      removed.selectionProbability !== 1 / candidates.length ||
      Object.keys(removed.values).length !== 0 ||
      added?.kind !== 'ADD' ||
      !validTargets.includes(added.modifierId) ||
      added.selectionProbability !== 1 / validTargets.length ||
      !target ||
      !sameValues(added.values, target.values) ||
      matching.length > 1 ||
      v.consumedOmens.length !== matching.length ||
      !matching.every((o) => v.consumedOmens.includes(o.id)) ||
      !sameModifiers(
        preserved,
        next.explicits.filter((m) =>
          preserved.some((p) => p.modifierId === m.modifierId),
        ),
      ) ||
      (validTargets.length > 1 &&
        !v.assumptions.some(
          (a) =>
            a.id ===
              (action.includes('LIQUID_')
                ? 'uniform-liquid-outcomes-v1'
                : 'uniform-essence-choice-v1') &&
            a.n === validTargets.length &&
            a.candidates.length === validTargets.length &&
            new Set(a.candidates).size === validTargets.length &&
            a.candidates.every((id) => validTargets.includes(id)),
        )) ||
      !v.assumptions.some(
        (a) =>
          a.id === 'uniform-removal-v1' &&
          a.n === candidates.length &&
          a.candidates.length === candidates.length &&
          new Set(a.candidates).size === candidates.length &&
          a.candidates.every((id) =>
            candidates.some((m) => m.modifierId === id),
          ),
      )
    )
      throw new Error(
        'Could not verify the essence replacement. Your item is unchanged. Please retry.',
      )
    if (
      [
        'PERFECT_ESSENCE_MIND',
        'PERFECT_ESSENCE_THAWING',
        'PERFECT_ESSENCE_INSULATION',
        'PERFECT_ESSENCE_COMMAND',
        'PERFECT_ESSENCE_BODY',
        'PERFECT_ESSENCE_RUIN',
        'PERFECT_ESSENCE_SEEKING',
        'PERFECT_ESSENCE_SORCERY',
        'PERFECT_ESSENCE_ALACRITY',
        'PERFECT_ESSENCE_ABRASION',
        'PERFECT_ESSENCE_FLAMES',
        'PERFECT_ESSENCE_ICE',
        'PERFECT_ESSENCE_ELECTRICITY',
        'PERFECT_ESSENCE_BATTLE',
        'PERFECT_ESSENCE_HASTE',
        'PERFECT_ESSENCE_GROUNDING',
        'PERFECT_ESSENCE_OPULENCE',
        'EXPANSIVE_ALLOY',
        'CYCLONIC_ALLOY',
        'MYSTIC_ALLOY',
        'ADAPTIVE_ALLOY',
        'SWIFT_ALLOY',
        'SOVEREIGN_ALLOY',
        'PRISMATIC_ALLOY',
      ].includes(action)
    ) {
      const range = definitions[added!.modifierId]?.stats?.[0]
      const expectedSource =
        'https://poe2db.tw/us/hover?s=Data%5CMods%2F' +
        (action === 'PERFECT_ESSENCE_MIND'
          ? 'EssenceIncreasedManaPercent1'
          : action === 'PERFECT_ESSENCE_THAWING'
            ? 'EssenceColdRecoupLife1'
            : action === 'PERFECT_ESSENCE_INSULATION'
              ? 'EssenceFireRecoupLife1'
              : action === 'PERFECT_ESSENCE_COMMAND'
                ? 'EssenceAuraEffect1'
                : action === 'PERFECT_ESSENCE_BODY'
                  ? 'EssenceIncreasedLifePercent1'
                  : action === 'PERFECT_ESSENCE_RUIN'
                    ? 'EssencePhysicalDamageTakenAsChaos1'
                    : action === 'PERFECT_ESSENCE_SEEKING'
                      ? 'EssenceReducedCriticalDamageAgainstYou1'
                      : action === 'PERFECT_ESSENCE_SORCERY'
                        ? 'EssenceSpellSkillLevel1H1'
                        : action === 'PERFECT_ESSENCE_ALACRITY'
                          ? 'EssenceManaCostReduction'
                          : action === 'PERFECT_ESSENCE_ABRASION'
                            ? 'EssenceDamageasExtraPhysical1'
                            : action === 'PERFECT_ESSENCE_FLAMES'
                              ? 'EssenceDamageasExtraFire1'
                              : action === 'PERFECT_ESSENCE_ICE'
                                ? 'EssenceDamageasExtraCold1'
                                : action === 'PERFECT_ESSENCE_ELECTRICITY'
                                  ? 'EssenceDamageasExtraLightning1'
                                  : action === 'PERFECT_ESSENCE_BATTLE'
                                    ? 'EssenceAttackSkillLevel1H1'
                                    : action === 'PERFECT_ESSENCE_HASTE'
                                      ? 'EssenceOnslaughtonKill1'
                                      : action === 'PERFECT_ESSENCE_GROUNDING'
                                        ? 'EssenceLightningRecoupLife1'
                                        : action === 'PERFECT_ESSENCE_OPULENCE'
                                          ? 'EssenceGoldDropped1'
                                          : action === 'ADAPTIVE_ALLOY'
                                            ? 'AlloyAttackSpeedIfMissingWardRecently1'
                                            : action === 'SWIFT_ALLOY'
                                              ? 'AlloyCastSpeedGloves1'
                                              : action === 'SOVEREIGN_ALLOY'
                                                ? 'AlloyLocalWardIncreasePercent1'
                                                : action === 'EXPANSIVE_ALLOY'
                                                  ? 'AlloyRemnantPickupRange1'
                                                  : action === 'CYCLONIC_ALLOY'
                                                    ? 'AlloyDamagingAilmentDuration1'
                                                    : action === 'MYSTIC_ALLOY'
                                                      ? 'AlloyAttackAreaOfEffect1'
                                                      : 'AlloyElementalPenetration1')
      const models = v.assumptions.filter(
        (a) => a.id === 'assumed-source-integer-roll-v1',
      )
      if (
        !range ||
        models.length !== (range.min === range.max ? 0 : 1) ||
        !models.every(
          (a) =>
            a.candidateUnit === range.id &&
            a.min === range.min &&
            a.max === range.max &&
            a.n === range.max - range.min + 1 &&
            a.candidates.length === 0 &&
            a.sourceUrl === expectedSource &&
            a.reason.includes('UNVERIFIED'),
        )
      ) {
        throw new Error(
          'Could not verify the numeric model. Your item is unchanged. Please retry.',
        )
      }
    }
  }
  if (
    v.applied &&
    essenceCandidates.length > 0 &&
    (state.rarity !== 'MAGIC' ||
      v.events.length !== 1 ||
      v.events[0]?.kind !== 'ADD' ||
      !essenceCandidates.includes(v.events[0]?.modifierId ?? '') ||
      v.events[0]?.selectionProbability !== 1 / essenceCandidates.length ||
      (essenceCandidates.length > 1 &&
        !v.assumptions.some(
          (a) =>
            a.id === 'uniform-essence-choice-v1' &&
            a.n === essenceCandidates.length &&
            a.candidates.length === essenceCandidates.length &&
            new Set(a.candidates).size === essenceCandidates.length &&
            a.candidates.every((id) => essenceCandidates.includes(id)),
        )) ||
      v.consumedOmens.length !== 0 ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      !sameValues(
        v.events[0]?.values,
        next.explicits.find((m) => m.modifierId === v.events[0]?.modifierId)
          ?.values,
      ))
  )
    throw new Error(
      'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
    )
  if (v.applied && sceptreFixedEssenceModifiers[action]) {
    const target = definitions[sceptreFixedEssenceModifiers[action]!]
    if (
      !target ||
      state.itemLevel <
        {
          LESSER_ESSENCE_COMMAND: 8,
          ESSENCE_COMMAND: 33,
          GREATER_ESSENCE_COMMAND: 60,
        }[
          action as
            | 'LESSER_ESSENCE_COMMAND'
            | 'ESSENCE_COMMAND'
            | 'GREATER_ESSENCE_COMMAND'
        ] ||
      next.rarity !== 'RARE'
    )
      throw new Error(
        'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
      )
    const code = {
      LESSER_ESSENCE_COMMAND: 'NearbyAlliesAllDamage2',
      ESSENCE_COMMAND: 'NearbyAlliesAllDamage4',
      GREATER_ESSENCE_COMMAND: 'NearbyAlliesAllDamage6',
    }[
      action as
        'LESSER_ESSENCE_COMMAND' | 'ESSENCE_COMMAND' | 'GREATER_ESSENCE_COMMAND'
    ]
    const range = target.stats?.[0]
    const models = v.assumptions.filter(
      (a) => a.id === 'assumed-source-integer-roll-v1',
    )
    if (
      !range ||
      target.stats?.length !== 1 ||
      models.length !== 1 ||
      !models.every(
        (a) =>
          a.candidateUnit === range.id &&
          a.min === range.min &&
          a.max === range.max &&
          a.n === range.max - range.min + 1 &&
          a.candidates.length === 0 &&
          a.sourceUrl ===
            'https://poe2db.tw/us/hover?s=Data%5CMods%2F' + code &&
          a.reason.includes('UNVERIFIED'),
      )
    )
      throw new Error(
        'Could not verify the numeric model. Your item is unchanged. Please retry.',
      )
  }
  if (
    v.applied &&
    baseAction === 'EXALTED' &&
    activeOmens.includes('Omen_of_Greater_Exaltation') &&
    (state.rarity !== 'RARE' ||
      state.explicits.length > (isWorkbenchJewel(state.baseItemId) ? 2 : 4) ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      v.events.length !== 2 ||
      new Set(v.events.map((event) => event.modifierId)).size !== 2 ||
      v.events.some(
        (event) =>
          event.kind !== 'ADD' ||
          state.explicits.some((old) => old.modifierId === event.modifierId) ||
          !sameValues(
            event.values,
            next.explicits.find((m) => m.modifierId === event.modifierId)
              ?.values,
          ),
      ) ||
      !v.consumedOmens.includes('Omen_of_Greater_Exaltation'))
  )
    throw new Error(
      'Could not verify both added modifiers. Your item is unchanged. Please retry.',
    )
  if (
    v.applied &&
    baseAction === 'CHAOS' &&
    activeOmens.includes('Omen_of_Whittling')
  ) {
    const candidates = whittlingCandidates(state, definitions, activeOmens)
    const matching = workbenchOmens.filter(
      (o) => o.trigger === 'CHAOS' && activeOmens.includes(o.id),
    )
    const removed = v.events.filter((e) => e.kind === 'REMOVE')
    const added = v.events.filter((e) => e.kind === 'ADD')
    const removal = removed[0]
    const model = v.assumptions.filter((a) => a.id === 'uniform-removal-v1')
    if (
      candidates.length === 0 ||
      matching.length > 2 ||
      (matching.length === 2 &&
        !compatibleOmenPair(matching[0]!.id, matching[1]!.id)) ||
      v.consumedOmens.length !== matching.length ||
      !matching.every((o) => v.consumedOmens.includes(o.id)) ||
      v.events.length !== 2 ||
      removed.length !== 1 ||
      added.length !== 1 ||
      !removal ||
      !candidates.includes(removal.modifierId) ||
      Object.keys(removal.values).length !== 0 ||
      removal.selectionProbability !== 1 / candidates.length ||
      model.length !== 1 ||
      model[0]!.n !== candidates.length ||
      model[0]!.candidates.length !== candidates.length ||
      new Set(model[0]!.candidates).size !== candidates.length ||
      !model[0]!.candidates.every((id) => candidates.includes(id)) ||
      !sameModifiers(
        state.explicits.filter((m) => m.modifierId !== removal.modifierId),
        next.explicits.filter((m) => m.modifierId !== added[0]!.modifierId),
      ) ||
      !sameValues(
        added[0]!.values,
        next.explicits.find((m) => m.modifierId === added[0]!.modifierId)
          ?.values,
      )
    )
      throw new Error(
        'Could not verify the Whittling removal candidates. Your item is unchanged. Please retry.',
      )
  }
  const legacyFive = legacyFiveIds.filter(
    (id) =>
      activeOmens.includes(id) &&
      (id.endsWith('Alchemy')
        ? action === 'ALCHEMY'
        : id.endsWith('Coronation')
          ? baseAction === 'REGAL'
          : baseAction === 'ANNULMENT'),
  )
  if (v.applied && legacyFive.length > 0) {
    const id = legacyFive[0]!
    const trigger = id.endsWith('Alchemy')
      ? 'ALCHEMY'
      : id.endsWith('Coronation')
        ? 'REGAL'
        : 'ANNULMENT'
    const matches = workbenchOmens.filter(
      (o) => o.trigger === trigger && activeOmens.includes(o.id),
    )
    const side = id.includes('Sinistral') ? 'PREFIX' : 'SUFFIX'
    const added = v.events.filter((e) => e.kind === 'ADD')
    const removed = v.events.filter((e) => e.kind === 'REMOVE')
    let valid =
      baseAction === trigger &&
      (matches.length === 1 ||
        (trigger === 'ANNULMENT' &&
          matches.length === 2 &&
          matches.some(
            (o) =>
              o.id === 'Omen_of_Sinistral_Annulment' ||
              o.id === 'Omen_of_Dextral_Annulment',
          ))) &&
      v.consumedOmens.length === matches.length &&
      matches.every((o) => v.consumedOmens.includes(o.id))
    if (trigger === 'ALCHEMY') {
      valid &&=
        !state.explicits.some((m) => m.fractured) &&
        added.length === 4 &&
        removed.length === state.explicits.length &&
        next.explicits.filter(
          (m) => definitions[m.modifierId]?.affixType === side,
        ).length === (isWorkbenchJewel(state.baseItemId) ? 2 : 3)
    } else if (trigger === 'REGAL') {
      valid &&=
        added.length === 1 &&
        removed.length === 0 &&
        definitions[added[0]!.modifierId]?.affixType === side &&
        sameModifiers(
          state.explicits,
          next.explicits.filter((m) =>
            state.explicits.some((old) => old.modifierId === m.modifierId),
          ),
        )
    } else {
      const candidates = state.explicits.filter(
        (m) =>
          !m.fractured &&
          (matches.length === 1 ||
            definitions[m.modifierId]?.affixType ===
              (matches.some((o) => o.id === 'Omen_of_Sinistral_Annulment')
                ? 'PREFIX'
                : 'SUFFIX')),
      )
      valid &&=
        candidates.length >= 2 &&
        added.length === 0 &&
        removed.length === 2 &&
        new Set(removed.map((e) => e.modifierId)).size === 2 &&
        removed.every(
          (e, i) =>
            candidates.some((m) => m.modifierId === e.modifierId) &&
            Object.keys(e.values).length === 0 &&
            e.selectionProbability === 1 / (candidates.length - i),
        ) &&
        sameModifiers(
          state.explicits.filter(
            (m) => !removed.some((e) => e.modifierId === m.modifierId),
          ),
          next.explicits,
        )
    }
    if (!valid)
      throw new Error(
        'Could not verify the legacy omen result. Your item is unchanged. Please retry.',
      )
  }
  const homogenising = legacyHomogenisingIds.find(
    (id) =>
      activeOmens.includes(id) &&
      baseAction === (id.endsWith('Coronation') ? 'REGAL' : 'EXALTED'),
  )
  if (v.applied && homogenising) {
    const matching = workbenchOmens.filter(
      (o) => o.trigger === baseAction && activeOmens.includes(o.id),
    )
    const originalTags = new Set(
      [...state.implicits, ...state.explicits].flatMap(
        (m) => definitions[m.modifierId]?.tags ?? [],
      ),
    )
    let intermediate = [...state.explicits]
    const valid =
      matching.every(
        (o) => o.id === homogenising || compatibleOmenPair(o.id, homogenising),
      ) &&
      v.consumedOmens.length === matching.length &&
      matching.every((o) => v.consumedOmens.includes(o.id)) &&
      v.events.length === expectedCount - state.explicits.length &&
      sameModifiers(
        state.explicits,
        next.explicits.filter((m) =>
          state.explicits.some((old) => old.modifierId === m.modifierId),
        ),
      ) &&
      v.events.every((event) => {
        const existingFamilies = new Set(
          intermediate.flatMap((m) => definitions[m.modifierId]!.familyIds),
        )
        let pool = Object.values(definitions).filter(
          (d) =>
            d.layer === 'EXPLICIT' &&
            (d.weight ?? 0) > 0 &&
            // requiredItemLevel is preserved by the initial API definition contract.
            d.requiredItemLevel != null &&
            d.requiredItemLevel <= state.itemLevel &&
            !d.familyIds.some((family) => existingFamilies.has(family)) &&
            intermediate.filter(
              (m) => definitions[m.modifierId]!.affixType === d.affixType,
            ).length <
              (isWorkbenchJewel(state.baseItemId)
                ? jewelCapacity(
                    { ...state, explicits: intermediate },
                    d.affixType,
                  )
                : 3) &&
            (originalTags.size === 0 ||
              d.tags?.some((tag) => originalTags.has(tag))),
        )
        const minimum = action.startsWith('GREATER_')
          ? 35
          : action.startsWith('PERFECT_')
            ? 50
            : 0
        if (minimum > 0) {
          const highest = new Map<string, number>()
          for (const d of pool) {
            if (d.familyIds.length !== 1) return false
            const group = `${d.affixType}:${d.familyIds[0]}`
            highest.set(
              group,
              Math.max(highest.get(group) ?? 0, d.requiredItemLevel!),
            )
          }
          pool = pool.filter(
            (d) =>
              d.requiredItemLevel! >= minimum ||
              d.requiredItemLevel ===
                highest.get(`${d.affixType}:${d.familyIds[0]}`),
          )
        }
        const chosen = pool.find((d) => d.id === event.modifierId)
        const added = next.explicits.find(
          (m) => m.modifierId === event.modifierId,
        )
        if (
          !chosen ||
          !added ||
          event.kind !== 'ADD' ||
          !sameValues(event.values, added.values) ||
          event.selectionProbability !==
            chosen.weight! / pool.reduce((sum, d) => sum + d.weight!, 0)
        )
          return false
        intermediate = [...intermediate, added]
        return true
      })
    if (!valid)
      throw new Error(
        'Could not verify the legacy tag-restricted additions. Your item is unchanged. Please retry.',
      )
  }
  const previousLocks = state.explicits.filter((m) => m.fractured)
  if (
    locks.length > 1 ||
    (locks.length && next.rarity !== 'RARE') ||
    previousLocks.some(
      (locked) =>
        !next.explicits.some(
          (m) =>
            m.modifierId === locked.modifierId &&
            m.fractured &&
            sameValues(m.values, locked.values),
        ),
    ) ||
    (v.applied &&
      action === 'FRACTURING' &&
      (locks.length !== 1 ||
        previousLocks.length !== 0 ||
        state.rarity !== 'RARE' ||
        state.explicits.length < 4 ||
        next.explicits.some(
          (m, i) =>
            m.modifierId !== state.explicits[i]?.modifierId ||
            !sameValues(m.values, state.explicits[i]?.values),
        ) ||
        v.events.length !== 1 ||
        v.events[0]?.kind !== 'FRACTURE' ||
        v.events[0]?.modifierId !== locks[0]?.modifierId ||
        v.events[0]?.selectionProbability !== 1 / state.explicits.length)) ||
    (action !== 'FRACTURING' && locks.length !== previousLocks.length)
  )
    throw new Error(
      'Could not verify the Fractured modifier. Your item is unchanged. Please retry.',
    )
  return v
}

export function rolledText(
  definition: Definition,
  values: Record<string, number>,
): string {
  if (/^(ruby|emerald|sapphire|diamond):/.test(definition.id)) {
    const binding = (
      displayBindings.definitions as Record<
        string,
        { englishText: string; values: string[]; template: string }
      >
    )[definition.id]
    if (
      binding &&
      binding.englishText === definition.text &&
      definition.stats &&
      !definition.tags?.includes('unscalable')
    ) {
      let text = definition.text
      for (const [i, n] of binding.values.entries()) {
        const stat = definition.stats[i]
        if (stat && Number.isSafeInteger(values[stat.id])) {
          const replacement = n.startsWith('+')
            ? '+' + values[stat.id]
            : String(values[stat.id])
          text = text.replace(n, replacement)
        }
      }
      return text
    }
  }
  if (
    definition.stats &&
    definition.stats.length > 1 &&
    Object.keys(values).length === definition.stats.length &&
    definition.stats.every((s) => s.min === s.max && values[s.id] === s.min)
  )
    return definition.text
  const stat = definition.stats?.length === 1 ? definition.stats[0] : undefined
  const range = /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013?-]\s*(-?\d+(?:\.\d+)?)\)/
  const match = definition.text.match(range)
  if (
    stat &&
    match &&
    Number(match[1]) === stat.min &&
    Number(match[2]) === stat.max
  )
    return definition.text.replace(range, String(values[stat.id]))
  if (stat && stat.min === stat.max) return definition.text
  return `${definition.name}: ${Object.entries(values)
    .map(
      ([id, value]) => `${id.replaceAll('_', ' ')} = ${value} (source units)`,
    )
    .join(', ')}`
}
