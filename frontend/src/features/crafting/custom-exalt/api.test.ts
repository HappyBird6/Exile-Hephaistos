import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { applyCustomExalt } from './api'
import type { CustomExaltContext } from './api'
afterEach(() => vi.unstubAllGlobals())
it('accepts omitted nullable properties and false flags but refuses changes to existing item properties', async () => {
  const context: CustomExaltContext = {
    state: { ...concreteInitial(initialFixture), rarity: 'RARE' },
    action: 'EXALTED',
    activeOmens: [],
  }
  const makeResult = () => ({
    result: {
      state: {
        ...context.state,
        installedAugments: [],
        equipmentQuality: null,
        catalystQuality: null,
        implicits: context.state.implicits.map((m) => ({
          ...m,
          fractured: false,
        })),
        explicits: [
          { modifierId: 'p', values: { life: 15 }, fractured: false },
        ],
      },
      action: 'EXALTED',
      ruleVersion: 'test',
      ledgerVersion: 'test',
      snapshotId: context.state.snapshotId,
      applied: true,
      reason: '',
      events: [],
      assumptions: [],
      consumedOmens: [],
      remainingOmens: [],
    },
    choice: { modifierId: 'p', probability: 0.1 },
    abyss: null,
  })
  let response = makeResult()
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(response)))
  expect(
    (
      await applyCustomExalt(
        context,
        initialFixture,
        new AbortController().signal,
        'p',
      )
    ).choice.probability,
  ).toBe(0.1)
  response = makeResult()
  response.result.state.itemLevel = 1
  await expect(
    applyCustomExalt(
      context,
      initialFixture,
      new AbortController().signal,
      'p',
    ),
  ).rejects.toThrow('Invalid custom Exalted result')
  response = makeResult()
  response.result.state.implicits[0]!.fractured = true
  await expect(
    applyCustomExalt(
      context,
      initialFixture,
      new AbortController().signal,
      'p',
    ),
  ).rejects.toThrow('Invalid custom Exalted result')
})
