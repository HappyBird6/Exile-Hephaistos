import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { Definition } from '../craftingApi'
import { localizedModifierText } from '../localizedModifiers'
import templates from '../../../shared/i18n/modifierTemplates.json'
import { StartModifierPicker } from './StartModifierPicker'
import { groupStartModifiers } from './modifierGroups'
import { modifierPickerMessages } from './modifierPickerMessages'

const life = initialFixture.modifiers.p!
const strength = initialFixture.modifiers.s!
const lowerLife = {
  ...life,
  id: 'life-low',
  tier: 2,
  text: '+(1—5) to maximum Life',
  stats: [{ id: 'life', min: 1, max: 5 }],
}

describe('starting modifier groups', () => {
  it('groups tiers by identity and keeps different effects, affixes and families separate', () => {
    const groups = groupStartModifiers([
      lowerLife,
      life,
      {
        ...life,
        id: 'different-stat',
        stats: [{ id: 'other-life', min: 10, max: 20 }],
      },
      { ...life, id: 'different-family', familyIds: ['OtherLife'] },
      { ...life, id: 'suffix', affixType: 'SUFFIX' },
      { ...life, id: 'unclassified-a', familyIds: [] },
      { ...life, id: 'unclassified-b', familyIds: [] },
    ])
    expect(groups).toHaveLength(6)
    expect(groups[0]!.tiers.map((m) => m.id)).toEqual(['p', 'life-low'])
    expect(
      groupStartModifiers([{ ...life, text: 'translated label' }])[0]!.id,
    ).toBe(groups[0]!.id)
  })
  it('supports open-list typeahead, arrows, Escape and separate tier selection', async () => {
    const add = vi.fn()
    render(
      <StartModifierPicker
        definitions={[life, lowerLife, strength]}
        onAdd={add}
      />,
    )
    const input = screen.getByRole('combobox', { name: 'Modifier group' })
    const tier = screen.getByRole('combobox', {
      name: 'Starting modifier tier',
    })
    expect(tier).toBeDisabled()
    fireEvent.click(input)
    expect(
      within(screen.getByRole('listbox')).getAllByRole('option'),
    ).toHaveLength(2)
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    expect(
      document.getElementById(input.getAttribute('aria-activedescendant')!),
    ).toHaveTextContent('Strength')
    fireEvent.keyDown(input, { key: 'ArrowUp' })
    fireEvent.change(input, { target: { value: 'LIFE' } })
    expect(
      within(screen.getByRole('listbox')).getAllByRole('option'),
    ).toHaveLength(1)
    fireEvent.keyDown(input, { key: 'Escape' })
    expect(input).toHaveAttribute('aria-expanded', 'false')
    expect(tier).toBeDisabled()
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'Enter' })
    await act(async () => {})
    expect(tier).toHaveFocus()
    expect(add).not.toHaveBeenCalled()
    expect(within(tier).getAllByRole('option')).toHaveLength(3)
    fireEvent.change(tier, { target: { value: 'life-low' } })
    expect(add).toHaveBeenCalledExactlyOnceWith('life-low')
    expect(input).toHaveFocus()
    expect(tier).toBeDisabled()
  })
  it('preserves a chosen group on Escape, closes on blur and ignores IME Enter', () => {
    const add = vi.fn()
    render(<StartModifierPicker definitions={[life, strength]} onAdd={add} />)
    const input = screen.getByRole('combobox', { name: 'Modifier group' })
    fireEvent.click(input)
    fireEvent.keyDown(input, { key: 'Enter', isComposing: true })
    expect(input).toHaveAttribute('aria-expanded', 'true')
    fireEvent.click(screen.getByRole('option', { name: /maximum Life/ }))
    fireEvent.click(input)
    fireEvent.change(input, { target: { value: 'Strength' } })
    fireEvent.keyDown(input, { key: 'Escape' })
    expect((input as HTMLInputElement).value).toContain('Life')
    fireEvent.click(input)
    fireEvent.blur(input)
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(add).not.toHaveBeenCalled()
  })
  it('rejects stale groups after eligibility changes and handles empty searches', () => {
    const add = vi.fn()
    const { rerender } = render(
      <StartModifierPicker definitions={[life]} onAdd={add} />,
    )
    const input = screen.getByRole('combobox', { name: 'Modifier group' })
    fireEvent.change(input, { target: { value: 'not-a-modifier' } })
    expect(screen.getByRole('status')).toHaveTextContent(
      'No matching modifiers.',
    )
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(add).not.toHaveBeenCalled()
    fireEvent.change(input, { target: { value: 'Life' } })
    fireEvent.keyDown(input, { key: 'Enter' })
    rerender(<StartModifierPicker definitions={[strength]} onAdd={add} />)
    expect(
      screen.getByRole('combobox', { name: 'Starting modifier tier' }),
    ).toBeDisabled()
    rerender(<StartModifierPicker definitions={[]} onAdd={add} />)
    expect(input).toBeDisabled()
    expect(screen.getByRole('status')).toHaveTextContent(
      'No more modifiers can be added.',
    )
  })
  it.each(locales)(
    'provides accessible labels and localized search in %s',
    (locale) => {
      setLocale(locale)
      const binding = templates.definitions['amulet:prefix:athlete-s']
      const definition: Definition = {
        ...life,
        id: 'amulet:prefix:athlete-s',
        text: binding.englishText,
        stats: binding.stats,
      }
      const add = vi.fn()
      render(<StartModifierPicker definitions={[definition]} onAdd={add} />)
      const t = modifierPickerMessages[locale]
      const input = screen.getByRole('combobox', { name: t.group })
      const text = localizedModifierText(definition, undefined, locale)
      fireEvent.change(input, { target: { value: text } })
      expect(
        within(screen.getByRole('listbox')).getByRole('option'),
      ).toHaveTextContent(text)
      fireEvent.keyDown(input, { key: 'Enter' })
      fireEvent.change(screen.getByRole('combobox', { name: t.tier }), {
        target: { value: definition.id },
      })
      expect(add).toHaveBeenCalledExactlyOnceWith(definition.id)
    },
  )
})
