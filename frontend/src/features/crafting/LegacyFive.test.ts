import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
const defs: Record<string, Definition> = { ...initialFixture.modifiers }
for (const id of ['p2', 'p3']) defs[id] = { ...defs.p!, id, familyIds: [id] }
const root = concreteInitial(initialFixture)
const mods = ['p', 'p2', 'p3', 's'].map((id) => ({
  modifierId: id,
  values: Object.fromEntries(defs[id]!.stats!.map((s) => [s.id, s.min])),
}))
function result(
  state: ConcreteItem,
  action: WorkbenchAction,
  id: string,
  next: ConcreteItem,
  events: AppliedItem['events'],
): AppliedItem {
  return {
    ruleVersion: 'legacy-five-v1',
    ledgerVersion: 'test-model',
    snapshotId: state.snapshotId,
    state: next,
    action,
    applied: true,
    reason: '',
    events,
    assumptions: [],
    consumedOmens: [id],
    remainingOmens: [],
  }
}
async function apply(
  state: ConcreteItem,
  r: AppliedItem,
  ids = r.consumedOmens,
) {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(r)))
  return applyCurrency(state, r.action, defs, new AbortController().signal, ids)
}

it('accepts maximum-prefix Alchemy and refuses a consumed wrong direction', async () => {
  const id = 'Omen_of_Sinistral_Alchemy'
  const r = result(
    root,
    'ALCHEMY',
    id,
    { ...root, rarity: 'RARE', explicits: mods },
    mods.map((m) => ({ ...m, kind: 'ADD', selectionProbability: 1 })),
  )
  expect((await apply(root, r)).state.explicits).toHaveLength(4)
  await expect(
    apply(root, { ...r, consumedOmens: ['Omen_of_Dextral_Alchemy'] }),
  ).rejects.toThrow('Your item is unchanged')
})

it('accepts prefix Coronation retaining prior rolls and rejects suffix response', async () => {
  const id = 'Omen_of_Sinistral_Coronation'
  const magic = { ...root, rarity: 'MAGIC' as const, explicits: [mods[3]!] }
  const r = result(
    magic,
    'REGAL',
    id,
    { ...magic, rarity: 'RARE', explicits: [mods[0]!, mods[3]!] },
    [{ ...mods[0]!, kind: 'ADD', selectionProbability: 1 }],
  )
  expect((await apply(magic, r)).state.explicits).toHaveLength(2)
  await expect(
    apply(magic, { ...r, consumedOmens: ['Omen_of_Dextral_Coronation'] }),
  ).rejects.toThrow('Your item is unchanged')
})

it('accepts two distinct removals preserving fracture and rejects duplicate removal', async () => {
  const id = 'Omen_of_Greater_Annulment'
  const locked = { ...mods[0]!, fractured: true }
  const rare = {
    ...root,
    rarity: 'RARE' as const,
    explicits: [locked, ...mods.slice(1)],
  }
  const events: AppliedItem['events'] = [mods[1]!, mods[2]!].map((m, i) => ({
    kind: 'REMOVE',
    modifierId: m.modifierId,
    values: {},
    selectionProbability: 1 / (3 - i),
  }))
  const r = result(
    rare,
    'ANNULMENT',
    id,
    { ...rare, explicits: [locked, mods[3]!] },
    events,
  )
  expect((await apply(rare, r)).state.explicits[0]).toEqual(locked)
  await expect(
    apply(rare, { ...r, events: [events[0]!, events[0]!] }),
  ).rejects.toThrow('Your item is unchanged')
})
