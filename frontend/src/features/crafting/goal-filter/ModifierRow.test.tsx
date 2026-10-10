import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Definition } from '../craftingApi'
import { ModifierRow } from './ModifierRow'
import { changeModifierTier } from './modifierGroups'
import { setupMessages } from './setupMessages'
import { locales } from '../../../shared/i18n/i18n'
import { groupTypes } from './types'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const css = readFileSync(
  resolve('src/features/crafting/goal-filter/craft-start.css'),
  'utf8',
)
const pickerCss = readFileSync(
  resolve('src/features/crafting/goal-filter/picker.css'),
  'utf8',
)

const definition: Definition = {
  id: 'physical',
  name: 'Physical',
  text: 'Adds (1–3) to (4–8) Physical Damage',
  tier: 2,
  affixType: 'PREFIX',
  familyIds: ['physical'],
  stats: [
    { id: 'min-damage', min: 1, max: 3 },
    { id: 'max-damage', min: 4, max: 8 },
  ],
}
const modifier = {
  modifierId: 'physical',
  values: { 'min-damage': 2, 'max-damage': 7 },
}
describe('compact starting modifier rows', () => {
  it('keeps name, tier, distinct meaningful multi-stat inputs and delete together', () => {
    const change = vi.fn(),
      remove = vi.fn(),
      issue = vi.fn()
    render(
      <ModifierRow
        definition={definition}
        modifier={modifier}
        tiers={[definition]}
        eligible={new Set(['physical'])}
        index={0}
        onChange={change}
        onIssue={issue}
        onRemove={remove}
      />,
    )
    const row = screen.getByRole('group')
    expect(row.children).toHaveLength(4)
    const inputs = within(row).getAllByRole('spinbutton')
    expect(inputs[0]).toHaveAccessibleName(
      'Adds [Value] to (4–8) Physical Damage',
    )
    expect(inputs[1]).toHaveAccessibleName(
      'Adds (1–3) to [Value] Physical Damage',
    )
    fireEvent.change(inputs[1]!, { target: { value: '6' } })
    expect(change).toHaveBeenLastCalledWith({
      ...modifier,
      values: { 'min-damage': 2, 'max-damage': 6 },
    })
    fireEvent.change(inputs[1]!, { target: { value: '9' } })
    expect(issue).toHaveBeenCalledWith('Roll must be an integer from 4 to 8.')
    fireEvent.click(within(row).getByRole('button'))
    expect(remove).toHaveBeenCalledOnce()
    expect(row).not.toHaveTextContent('Value 1.1')
  })
  it('preserves rolls within the new range and clamps each stat independently, by ID rather than order', () => {
    const next = {
      ...definition,
      id: 'higher',
      stats: [
        { id: 'max-damage', min: 5, max: 9 },
        { id: 'min-damage', min: 3, max: 6 },
      ],
    }
    const result = changeModifierTier(modifier, next)
    expect(result).toEqual({
      modifierId: 'higher',
      values: { 'min-damage': 3, 'max-damage': 7 },
    })
    expect(
      changeModifierTier(
        { ...modifier, values: { 'min-damage': 3, 'max-damage': 12 } },
        definition,
      ).values,
    ).toEqual({ 'min-damage': 3, 'max-damage': 8 })
  })
  it.each(locales)('uses exact supported operator tokens in %s', (locale) => {
    for (const type of groupTypes)
      expect(setupMessages[locale].groups[type]).toBe(type)
    expect(Object.keys(setupMessages[locale].groups)).not.toContain('OR')
  })
  it('reserves initial space, permits narrow row wrapping and shares fixed input dimensions (CSS contract, not browser geometry)', () => {
    expect(css).toContain('min-height: 580px')
    expect(css).toContain('min-height: 180px')
    expect(css).toMatch(
      /\.craft-start \.craft-start-rolls\s*\{[^}]*flex-wrap: wrap/s,
    )
    expect(pickerCss.replace(/\s+/g, ' ')).toContain(
      '.craft-start.craft-start .craft-picker-label .craft-picker-input.craft-picker-input,',
    )
    expect(pickerCss).toContain(
      '.goal-filter .craft-picker-label .craft-picker-input',
    )
    expect(pickerCss).toContain('height: 32px')
  })
})
