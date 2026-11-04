import { describe, expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  initialFixture,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import type { AppliedItem } from '../workbenchApi'
import {
  createChaosRepeatReceipt,
  isChaosRepeatMetadata,
  readChaosRepeatReceipt,
} from './chaosRepeatReceipt'
import type {
  ChaosRepeatReceipt,
  ChaosRepeatReceiptInput,
} from './chaosRepeatReceipt'

async function input(): Promise<ChaosRepeatReceiptInput> {
  const before = concreteInitial(initialFixture)
  const witness = (await (
    await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({ state: before, action: 'TRANSMUTATION' }),
    })
  ).json()) as AppliedItem
  // Synthetic completed-batch data, not a simulated multi-roll transition.
  return {
    before,
    action: 'CHAOS',
    uses: 3,
    finalWitness: { ...witness, action: 'CHAOS' },
    consumedOmens: [],
  }
}

describe('dedicated repeat receipt boundary', () => {
  it.each(['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'])(
    'accepts positive safe counts for %s without a request limit',
    (action) => {
      for (const uses of [1, 501, Number.MAX_SAFE_INTEGER])
        expect(isChaosRepeatMetadata({ version: 1, action, uses })).toBe(true)
    },
  )

  it.each([
    undefined,
    null,
    {},
    { action: 'CHAOS', uses: 1 },
    { version: 2, action: 'CHAOS', uses: 1 },
    { version: 1, action: 'EXALTED', uses: 1 },
    ...[undefined, 0, -1, 1.5, NaN, Infinity, 2 ** 53, '3'].map((uses) => ({
      version: 1,
      action: 'CHAOS',
      uses,
    })),
  ])('rejects malformed metadata: %j', (metadata) => {
    expect(isChaosRepeatMetadata(metadata)).toBe(false)
  })

  it('requires dedicated validation and preserves detached validated inputs', async () => {
    const value = await input()
    const validate = vi.fn(() => true)
    const receipt = createChaosRepeatReceipt(value, validate)!
    expect(validate).toHaveBeenCalledOnce()
    expect(validate.mock.calls[0]).toEqual([value])
    value.uses = 999
    value.finalWitness.events.length = 0
    const saved = readChaosRepeatReceipt(receipt)!
    expect(saved.uses).toBe(3)
    expect(saved.finalWitness.events).toHaveLength(1)
    saved.uses = 888
    expect(readChaosRepeatReceipt(receipt)?.uses).toBe(3)
    expect(readChaosRepeatReceipt({} as ChaosRepeatReceipt)).toBeUndefined()
    expect(readChaosRepeatReceipt(null)).toBeUndefined()
  })

  it('does not mint receipts for rejection, zero uses, action conflicts or unconfirmed omens', async () => {
    const value = await input()
    expect(createChaosRepeatReceipt(value, () => false)).toBeNull()
    expect(
      createChaosRepeatReceipt(value, () => {
        throw new Error('invalid batch response')
      }),
    ).toBeNull()
    const invalid = [
      { ...value, uses: 0 },
      { ...value, uses: 1.5 },
      { ...value, action: 'GREATER_CHAOS' as const },
      { ...value, finalWitness: { ...value.finalWitness, applied: false } },
      { ...value, consumedOmens: ['unknown-omen'] },
      { ...value, consumedOmens: Array<string>(1) },
      { ...value, finalWitness: { ...value.finalWitness, ruleVersion: '' } },
      {
        ...value,
        finalWitness: {
          ...value.finalWitness,
          consumedOmens: ['Omen_of_Whittling'],
        },
      },
    ]
    for (const bad of invalid) {
      const validate = vi.fn(() => true)
      expect(createChaosRepeatReceipt(bad, validate)).toBeNull()
      expect(validate).not.toHaveBeenCalled()
    }
    for (const key of [
      'before',
      'finalWitness',
      'action',
      'uses',
      'consumedOmens',
    ] as const) {
      const missing = structuredClone(value) as Partial<ChaosRepeatReceiptInput>
      delete missing[key]
      expect(
        createChaosRepeatReceipt(
          missing as ChaosRepeatReceiptInput,
          () => true,
        ),
      ).toBeNull()
    }
  })
})
