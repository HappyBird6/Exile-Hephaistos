import { expect, it } from 'vitest'
import { observationsShape, sameObservations } from './itemObservations'
it('preserves unknown identity without inventing Augment slots and rejects malformed observations', () => {
  const value = {
    socketCount: 4,
    augmentEffects: ['20% increased Armour (rune)'],
    grantedSkills: [{ name: 'Malice', level: 17 }],
  }
  expect(observationsShape(value)).toBe(true)
  expect(sameObservations(value, JSON.parse(JSON.stringify(value)))).toBe(true)
  expect(sameObservations(value, null)).toBe(false)
  expect(sameObservations(value, { ...value, augmentEffects: [] })).toBe(false)
  for (const bad of [
    { ...value, socketCount: 1.5 },
    { ...value, unknown: true },
    { ...value, grantedSkills: [{ name: 'Malice', level: 0 }] },
    { ...value, augmentEffects: [42] },
    {
      ...value,
      grantedSkills: [...value.grantedSkills, ...value.grantedSkills],
    },
  ])
    expect(observationsShape(bad)).toBe(false)
})
