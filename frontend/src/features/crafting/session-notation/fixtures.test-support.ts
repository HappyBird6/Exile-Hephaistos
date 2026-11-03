import {
  fixtureFetch,
  initialFixture,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import { emptyFilms, recordCraft, startFilm } from '../workbenchHistory'
import type { Films } from '../workbenchHistory'

export const root = concreteInitial(initialFixture)
export const identity = initialFixture.rulesetIdentity

export function start(): Films {
  return startFilm(emptyFilms(), root, 'session', identity, 'original\r\ntext')
}

export async function craft(state: ConcreteItem = root): Promise<AppliedItem> {
  const response = await fixtureFetch('/api/v1/crafting/workbench/apply', {
    body: JSON.stringify({ state, action: 'TRANSMUTATION' }),
  })
  return (await response.json()) as AppliedItem
}

export async function twoSteps() {
  const first = await craft()
  // The shared HTTP fixture implements only Transmutation. This explicit saved
  // success fixture adds its known suffix; it is not an inferred production roll.
  const second: AppliedItem = {
    ...first,
    action: 'AUGMENTATION',
    state: {
      ...first.state,
      explicits: [
        ...first.state.explicits,
        { modifierId: 's', values: { strength: 6 } },
      ],
    },
    events: [
      {
        kind: 'ADD',
        modifierId: 's',
        values: { strength: 6 },
        selectionProbability: 1,
      },
    ],
    assumptions: [],
  }
  const history = recordCraft(
    recordCraft(start(), root, first, 'unused'),
    first.state,
    second,
    'unused',
  )
  return { history, first, second }
}

export function freeze<T>(value: T): T {
  if (value && typeof value === 'object') {
    Object.freeze(value)
    Object.values(value).forEach(freeze)
  }
  return value
}
