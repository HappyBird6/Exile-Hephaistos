import { fireEvent, render, screen } from '@testing-library/react'
import { OperationCards } from './OperationCards'
import { setLocale } from '../../../shared/i18n/i18n'
import { expect, it } from 'vitest'
import { operationGroups, operationKind } from './operationGrouping'
import type { AutomaticArrow } from './automaticApi'
const method = (
  currency: string,
  omens: string[] = [],
  source = 0,
): AutomaticArrow => ({
  id: currency + omens.join('-') + source,
  source,
  initial: [],
  cycle: [{ currency, omens, purpose: 'GOAL_ACQUISITION' }],
  executionMode: 'SINGLE_PASS',
  points: [],
  reasons: [],
  finished: true,
})
it.each(['EXALTED', 'CHAOS'])(
  'groups %s omen variants without changing method identity or probability data',
  (currency) => {
    const original = [
      method(currency),
      method(currency, ['DEXTRAL_ERASURE']),
      method('GREATER_' + currency),
    ]
    const groups = operationGroups(original)
    expect(groups).toHaveLength(1)
    expect(groups[0]!.variants).toEqual(original)
    expect(groups[0]!.variants[1]).toBe(original[1])
  },
)
it('keeps different states, purposes and preparation sequences separate', () => {
  const first = method('CHAOS'),
    second = method('CHAOS', [], 1),
    prepared = {
      ...method('CHAOS'),
      id: 'prepared',
      initial: [
        {
          currency: 'EXALTED',
          omens: [],
          purpose: 'TEMPORARY_AFFIX_FOR_REMOVAL',
        },
      ],
    }
  const different = {
    ...method('CHAOS'),
    id: 'other-purpose',
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'HIGH_TIER_PREPARATION' }],
  }
  expect(
    operationGroups([first, second, prepared, different, method('REGAL')]),
  ).toHaveLength(5)
  expect(operationKind(first)).toBe('replace')
  expect(operationKind(prepared)).toBe('prepare')
  expect(operationKind(method('EXALTED'))).toBe('add')
})

const probability = (numerator: string, denominator = '100') => ({
  lower: { numerator, denominator },
  upper: { numerator, denominator },
  arithmeticWidth: { numerator: '0', denominator: '1' },
  exact: true,
  confirmedPercent: null,
})
it('defaults composite Transmutation-Regal variants to the certified whole-method goal probability', () => {
  const ordinary = {
    ...method('REGAL'),
    initial: [
      { currency: 'TRANSMUTATION', omens: [], purpose: 'RARITY_UPGRADE' },
    ],
    points: [
      {
        cycles: 1,
        complete: true,
        initialUses: {},
        cycleUses: {},
        outcomes: [
          { kind: 'GOAL', target: 1, probability: probability('20') },
          { kind: 'LOSS', target: null, probability: probability('80') },
        ],
      },
    ],
  }
  const omen = {
    ...ordinary,
    id: 'regal-omen',
    cycle: [
      {
        currency: 'REGAL',
        omens: ['DEXTRAL_CORONATION'],
        purpose: 'GOAL_ACQUISITION',
      },
    ],
    points: [
      {
        ...ordinary.points[0]!,
        outcomes: [
          { kind: 'GOAL', target: 2, probability: probability('30') },
          { kind: 'LOSS', target: null, probability: probability('70') },
        ],
      },
    ],
  }
  const groups = operationGroups([ordinary, omen], 100)
  expect(groups).toHaveLength(1)
  expect(groups[0]!.variants).toEqual([omen, ordinary])
  expect(groups[0]!.variants[0]!.initial).toEqual(ordinary.initial)
  setLocale('en')
  const mounted = render(
    <OperationCards
      methods={[ordinary, omen]}
      cycles={100}
      action={(a) => a.currency + a.omens.join('+')}
      render={(m) => <output data-testid="chosen-regal">{m.id}</output>}
    />,
  )
  expect(screen.getByTestId('chosen-regal')).toHaveTextContent(omen.id)
  fireEvent.click(
    mounted.container.querySelector('button[aria-pressed="false"]')!,
  )
  expect(screen.getByTestId('chosen-regal')).toHaveTextContent(ordinary.id)
  mounted.unmount()
  expect(ordinary.points[0]!.outcomes[0]!.probability.lower.numerator).toBe(
    '20',
  )
  expect(
    operationGroups([
      ordinary,
      { ...omen, points: [{ ...omen.points[0]!, complete: false }] },
    ])[0]!.variants[0],
  ).toBe(ordinary)
})
it('does not rank overlapping certified intervals using rounded percentages', () => {
  const first = {
    ...method('REGAL'),
    points: [
      {
        cycles: 1,
        complete: true,
        initialUses: {},
        cycleUses: {},
        outcomes: [
          {
            kind: 'GOAL',
            target: 1,
            probability: {
              ...probability('10'),
              upper: { numerator: '30', denominator: '100' },
            },
          },
        ],
      },
    ],
  }
  const second = {
    ...first,
    id: 'other',
    cycle: [
      {
        currency: 'REGAL',
        omens: ['DEXTRAL_CORONATION'],
        purpose: 'GOAL_ACQUISITION',
      },
    ],
    points: [
      {
        ...first.points[0]!,
        outcomes: [{ kind: 'GOAL', target: 2, probability: probability('20') }],
      },
    ],
  }
  expect(operationGroups([first, second])[0]!.variants).toEqual([first, second])
})

it.each(['REGAL', 'EXALTED'])(
  'prefers plain %s for identical conditional laws even if the omen was certified first',
  (currency) => {
    const plain = {
      ...method(currency),
      id: 'plain',
      points: [
        {
          cycles: 1,
          complete: true,
          initialUses: {},
          cycleUses: {},
          outcomes: [
            { target: 1, kind: 'GOAL', probability: probability('100') },
          ],
        },
      ],
    }
    const omen = {
      ...plain,
      id: 'omen',
      cycle: [
        {
          currency,
          omens: ['SINISTRAL_CORONATION'],
          purpose: 'GOAL_ACQUISITION',
        },
      ],
    }
    const recommendation = {
      node: 0,
      cycles: 100,
      local: ['omen'],
      strategy: probability('100'),
      strategyStatus: 'ACTION_CERTIFIED',
      strategyMethods: ['omen'],
    }
    expect(
      operationGroups([omen, plain], 100, [recommendation])[0]!.variants.map(
        (m) => m.id,
      ),
    ).toEqual(['plain', 'omen'])
  },
)
it('keeps a certified whole-route action ahead of a larger immediate Exalted chance', () => {
  const plain = {
    ...method('EXALTED'),
    id: 'plain',
    points: [
      {
        cycles: 1,
        complete: true,
        initialUses: {},
        cycleUses: {},
        outcomes: [{ target: 1, kind: 'GOAL', probability: probability('10') }],
      },
    ],
  }
  const omen = {
    ...plain,
    id: 'omen',
    cycle: [
      {
        currency: 'EXALTED',
        omens: ['SINISTRAL_EXALTATION'],
        purpose: 'GOAL_ACQUISITION',
      },
    ],
    points: [
      {
        ...plain.points[0]!,
        outcomes: [{ target: 2, kind: 'GOAL', probability: probability('20') }],
      },
    ],
  }
  const recommendation = {
    node: 0,
    cycles: 100,
    local: ['omen'],
    strategy: probability('50'),
    strategyStatus: 'ACTION_CERTIFIED',
    strategyMethods: ['plain'],
  }
  expect(
    operationGroups([omen, plain], 100, [recommendation])[0]!.variants[0]!.id,
  ).toBe('plain')
})
