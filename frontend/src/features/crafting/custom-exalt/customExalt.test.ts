import { documentFor, decodeDocument } from '../session-history/transfer'
import { expect, it } from 'vitest'
import { currencyRank, chanceText, customExaltMessages } from './messages'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import type { Film } from '../workbenchHistory'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { locales } from '../../../shared/i18n/i18n'
it('shows a custom selection and its natural chance while keeping the real currency ID for costs', () => {
  const root = concreteInitial(initialFixture),
    film = {
      id: 'custom',
      createdAt: '2026-11-10',
      frames: [
        { state: root, action: null },
        {
          state: {
            ...root,
            explicits: [{ modifierId: 'chosen', values: { life: 1 } }],
          },
          action: 'PERFECT_EXALTED',
          customExalt: { modifierId: 'chosen', probability: 0.025 },
          manual: {
            action: 'PERFECT_EXALTED',
            rulesetIdentity: 'test',
            consumedOmens: [],
          },
        },
      ],
    } as Film
  const steps = extractSessionNotation(film, 1).steps
  expect(steps).toHaveLength(1)
  expect(steps[0]!.currencyId).toBe('Perfect_Exalted_Orb')
  expect(steps[0]!.customExalt).toEqual({
    modifierId: 'chosen',
    probability: 0.025,
  })
  const restored = decodeDocument(JSON.stringify(documentFor(film))).film
  expect(extractSessionNotation(restored, 1).steps[0]!.customExalt).toEqual(
    steps[0]!.customExalt,
  )
  film.frames[1]!.customExalt!.probability = NaN
  expect(extractSessionNotation(film, 1).steps[0]!.customExalt).toBeUndefined()
})
it('distinguishes only tiered crafting currencies and formats the custom controls in every site locale', () => {
  expect(
    ['EXALTED', 'GREATER_EXALTED', 'PERFECT_EXALTED'].map(currencyRank),
  ).toEqual(['I', 'II', 'III'])
  expect(currencyRank('ESSENCE_MIND')).toBeUndefined()
  for (const locale of locales) {
    expect(customExaltMessages[locale].title).toBeTruthy()
    expect(chanceText(0.0234, locale)).toMatch(/2/)
  }
})
