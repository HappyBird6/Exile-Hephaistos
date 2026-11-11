import * as data from 'virtual:crafting-data'
export const jewelDefinitions =
  data.definitions as typeof import('./basicJewelDefinitions.json')
export const manualRevealDefinitions =
  data.reveal as typeof import('./manualRevealDefinitions.json')
export const essenceTargets =
  data.essences as typeof import('./topBaseEssences.json')
export const loadBaseData = data.loadBaseData

export const materialNames = data.materialNames
