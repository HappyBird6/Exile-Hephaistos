import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { boneNames, isBone } from '../desecrationCurrency'
import { excludedWorkbenchBoneIds, isExcludedWorkbenchBone } from './policy'
import { UnrevealedRune } from './UnrevealedRune'
import { abyssWorkbenchMessages } from './messages'

afterEach(() => {
  cleanup()
  setLocale('en')
})

it('excludes exactly the three Gnawed bones from new selection while retaining historical IDs', () => {
  expect(excludedWorkbenchBoneIds).toEqual([
    'Gnawed_Rib',
    'Gnawed_Jawbone',
    'Gnawed_Collarbone',
  ])
  const visible = Object.keys(boneNames).filter(
    (id) => !isExcludedWorkbenchBone(id),
  )
  expect(visible).toHaveLength(8)
  expect(visible).toContain('Preserved_Cranium')
  expect(visible).toContain('Altered_Collarbone')
  for (const id of excludedWorkbenchBoneIds) expect(isBone(id)).toBe(true)
  expect(isExcludedWorkbenchBone('Omen_of_Putrefaction')).toBe(false)
})

it.each(
  Object.keys(
    abyssWorkbenchMessages,
  ) as (keyof typeof abyssWorkbenchMessages)[],
)(
  'renders the user-provided rune with an accessible %s name and no visible placeholder text',
  (locale) => {
    setLocale(locale)
    const { container } = render(<UnrevealedRune />)
    expect(
      screen.getByRole('img', { name: abyssWorkbenchMessages[locale].hidden }),
    ).toHaveClass('abyss-unrevealed-rune')
    expect(container.textContent).toBe('')
  },
)
