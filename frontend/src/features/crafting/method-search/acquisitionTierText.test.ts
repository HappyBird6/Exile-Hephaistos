import { expect, it } from 'vitest'
import type { Definition } from '../craftingApi'
import { acquisitionTierText } from './acquisitionTierText'

const tier = (id: string, min: number, max: number): Definition => ({
  id,
  name: 'Life',
  text: `+(${min}—${max}) to maximum Life`,
  affixType: 'PREFIX',
  familyIds: ['life'],
  tier: 1,
  stats: [{ id: 'life', min, max }],
})
const defs = {
  early: tier('early', 10, 19),
  boundary: tier('boundary', 95, 105),
  better: tier('better', 106, 120),
}

it('shows the floor of the boundary tier regardless of the actual roll or the ID order', () => {
  expect(acquisitionTierText(['better', 'boundary'], defs, 'en', {})).toBe(
    '+95 to maximum Life',
  )
  expect(
    acquisitionTierText(['better', 'early', 'boundary'], defs, 'en', {}),
  ).toBe('+10 to maximum Life')
})

it('shows the ceiling for a maximum and both tier edges for a two-sided range', () => {
  expect(
    acquisitionTierText(['early', 'boundary'], defs, 'en', {
      hasUpperBound: true,
    }),
  ).toBe('+105 to maximum Life')
  expect(
    acquisitionTierText(['boundary', 'better'], defs, 'en', {
      hasLowerBound: true,
      hasUpperBound: true,
    }),
  ).toBe('+95 to maximum Life – +120 to maximum Life')
})
