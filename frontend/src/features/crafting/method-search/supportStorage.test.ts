import { expect, it, vi } from 'vitest'
import {
  readSupport,
  saveSupport,
  validRun,
  hasImplicitGoal,
} from './supportStorage'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { routeThrough } from './routeSelection'
it('rejects corrupt, expired, oversized and wrong-version local state', () => {
  const key = 'hephaistos.support.v1:run'
  const data = {
    request: 'r',
    job: '11111111-1111-4111-8111-111111111111',
    selected: 0,
    cycles: 100,
    route: [],
  }
  expect(saveSupport('run', data)).toBe(true)
  expect(readSupport('run', validRun)).toEqual(data)
  for (const raw of [
    '{',
    'x'.repeat(131073),
    JSON.stringify({ version: 2, savedAt: Date.now(), data }),
    JSON.stringify({ version: 1, savedAt: 0, data }),
  ]) {
    localStorage.setItem(key, raw)
    expect(readSupport('run', validRun)).toBeNull()
  }
  const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new DOMException('Quota', 'QuotaExceededError')
  })
  expect(saveSupport('run', data)).toBe(false)
  spy.mockRestore()
})
it('retains a chosen branch and replaces an alternative without inventing merge history', () => {
  const a = { method: 'a', source: 0, target: 1, kind: 'CHECKPOINT' },
    b = { method: 'b', source: 1, target: 3, kind: 'GOAL' },
    other = { method: 'other', source: 0, target: 2, kind: 'CHECKPOINT' }
  expect(routeThrough([a], b)).toEqual([a, b])
  expect(routeThrough([a, b], other)).toEqual([other])
  expect(routeThrough([], b)).toEqual([b])
})
it('recognizes saved implicit objectives without changing original rows', () => {
  const goal = requestFixture().goal
  goal.groups[0]!.entries[0]!.statId =
    'hephaistos:v1:implicit:base_spirit_from_equipment'
  const before = JSON.stringify(goal)
  expect(hasImplicitGoal(goal)).toBe(true)
  expect(JSON.stringify(goal)).toBe(before)
})
