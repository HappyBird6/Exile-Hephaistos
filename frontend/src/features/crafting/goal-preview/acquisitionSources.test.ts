import { expect, it } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import {
  materialActions,
  materialNames,
  materialProfiles,
} from '../catalogData'
import { workbenchCurrencyActions } from '../currencyDefinitions'
import {
  acquisitionSourceLabel,
  acquisitionSourceLabels,
} from './acquisitionSources'

it('keeps the English source headings and names in a non-English UI', async () => {
  await setLocale('ko')
  expect(
    acquisitionSourceLabels([
      { kind: 'IMPLICIT' },
      { kind: 'NORMAL' },
      { kind: 'ESSENCE', id: 'GREATER_ESSENCE_BODY' },
      { kind: 'ESSENCE', id: 'PERFECT_ESSENCE_BODY' },
      { kind: 'ESSENCE', id: 'ESSENCE_ABYSS' },
      { kind: 'ALLOY', id: 'THE_RUNEBINDERS_ALLOY' },
      { kind: 'ALLOY', id: 'THE_RUNEFATHERS_ALLOY' },
      { kind: 'LIQUID', id: 'LIQUID_ENVY' },
      { kind: 'DESECRATION', id: 'Ancient_Rib' },
      { kind: 'DESECRATION', id: 'Preserved_Rib' },
      { kind: 'AUGMENT', id: 'Kolrs_Hunt' },
    ]),
  ).toEqual([
    'Implicit',
    'base',
    'Essence (Greater Essence of the Body)',
    'Perfect Essence / Alloy (Perfect Essence of the Body)',
    'Perfect Essence / Alloy (Essence of the Abyss)',
    "Perfect Essence / Alloy (The Runebinder's Alloy)",
    "Perfect Essence / Alloy (The Runefather's Alloy)",
    'Liquid Emotions (Liquid Envy)',
    'Desecrated Modifiers',
    "Kolr's Hunt",
  ])
})

it('identifies every registered Essence, Alloy and Liquid by its English item name', () => {
  const itemIds = new Map<string, string>(
    Object.entries(workbenchCurrencyActions).map(([id, action]) => [
      action,
      id,
    ]),
  )
  for (const [action, profile] of Object.entries(materialActions)) {
    const kind = materialProfiles[profile]?.kind
    if (kind !== 'ESSENCE' && kind !== 'ALLOY' && kind !== 'LIQUID') continue
    const name = materialNames[itemIds.get(action) ?? '']
    expect(name, action).toBeTruthy()
    expect(acquisitionSourceLabel({ kind, id: action }), action).toContain(
      `(${name})`,
    )
  }
})

it('rejects unknown and mismatched materials instead of printing an action ID', () => {
  expect(
    acquisitionSourceLabel({ kind: 'ALLOY', id: 'ESSENCE_ABYSS' }),
  ).toBeUndefined()
  expect(
    acquisitionSourceLabel({ kind: 'AUGMENT', id: 'Desert_Rune' }),
  ).toBeUndefined()
  expect(() =>
    acquisitionSourceLabels([{ kind: 'ALLOY', id: 'MISSING' }]),
  ).toThrow()
})
