import { expect, it } from 'vitest'
import { parseChaosRepeatMaximumUses } from './maximumUses'
import {
  chaosRepeatMaximumUsesLimit,
  defaultChaosRepeatMaximumUses,
} from './types'

it('provides the approved default 100 and independent UI cap 1000', () => {
  expect(defaultChaosRepeatMaximumUses).toBe('100')
  expect(chaosRepeatMaximumUsesLimit).toBe(1000)
})

it.each([
  ['1', 1],
  ['100', 100],
  ['1000', 1000],
  ['00501', 501],
] as const)(
  'parses valid decimal count %s without changing the raw string',
  (raw, expected) => {
    expect(parseChaosRepeatMaximumUses(raw)).toBe(expected)
  },
)

it.each([
  '',
  '0',
  '-1',
  '1.5',
  '1001',
  '100.0',
  '1e2',
  ' 1 ',
  'text',
  '0x10',
  'Infinity',
  '9007199254740993',
  '1\n',
])('returns null for %j so the caller can block execution', (raw) => {
  expect(parseChaosRepeatMaximumUses(raw)).toBeNull()
})
