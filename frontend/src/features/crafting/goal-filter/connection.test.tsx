import {
  saveSupport,
  readSupport,
  validGoal,
} from '../method-search/supportStorage'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ConnectedGoalFilter } from './ConnectedGoalFilter'
import type { SearchGoalSnapshot } from './ConnectedGoalFilter'
import { evaluateNumericItem, presentNumericStats } from './numericApi'
import { addStat, emptyGoal } from './editor'
import type { Catalog } from './types'
import type { ConcreteItem } from '../workbenchApi'
const item: ConcreteItem = {
  snapshotId: 'solar',
  baseItemId: 'amulet',
  itemLevel: 82,
  rarity: 'RARE',
  implicits: [],
  explicits: [
    { modifierId: 'cold', values: { cold: 10 } },
    { modifierId: 'all', values: { all: 12 } },
  ],
  conditions: [],
  catalystQuality: { type: 'CHAYULA', amount: 20 },
}
const context = {
  snapshotId: item.snapshotId,
  baseItemId: item.baseItemId,
  itemLevel: item.itemLevel,
}
const catalog = {
  version: 1,
  catalogVersion: 'v1',
  context,
  groupTypes: [],
  issues: [],
  stats: [
    {
      statId: 'cold-total',
      label: 'Cold total',
      unit: 'percent',
      kind: 'PSEUDO',
      support: {
        evaluation: 'SUPPORTED',
        probability: 'UNSUPPORTED',
        reasonCode: null,
      },
      eligible: true,
      eligibilityReason: null,
      sourceStatIds: [],
      contributions: [],
      sourceUrls: [],
    },
  ],
}
const recommendation = {
  version: 1,
  catalogVersion: 'v1',
  evaluation: 'MATCH',
  probability: {
    status: 'UNSUPPORTED',
    reasonCode: 'NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED',
    modelVersion: null,
    ledgerVersion: null,
  },
  comparisons: [],
  rankingCertified: false,
  comparedSequences: 0,
  totalSequences: null,
}
const evaluation = {
  version: 1,
  catalogVersion: 'v1',
  status: 'MATCH',
  generalStatus: 'MATCH',
  groups: [],
  issues: [],
}
const response = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      'X-Crafting-Ruleset': 'fixture-ruleset',
      'Content-Type': 'application/json',
    },
  })
afterEach(() => vi.unstubAllGlobals())
function setup(
  actual: ConcreteItem | null = item,
  onLegacyChange = vi.fn(),
  compact = false,
  onSearchGoalChange = vi.fn<(snapshot: SearchGoalSnapshot) => void>(),
) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <div onChange={onLegacyChange}>
      <QueryClientProvider client={client}>
        <ConnectedGoalFilter
          onSearchGoalChange={onSearchGoalChange}
          rulesetIdentity="fixture-ruleset"
          item={actual}
          context={item}
          language="en"
          activeOmens={[]}
          maxMillis={2000}
          compact={compact}
        />
      </QueryClientProvider>
    </div>,
  )
}
function mockFetch(failure = false, recommended: unknown = recommendation) {
  const mock = vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
    const path = String(url)
    if (path.includes('/catalog')) return response(catalog)
    if (path.endsWith('/validate')) {
      const body = JSON.parse(String(init?.body))
      return response({
        version: 1,
        valid: body.goal.groups.some(
          (g: { entries: unknown[] }) => g.entries.length,
        ),
        issues: [],
        capabilities: { evaluation: 'SUPPORTED', probability: 'UNSUPPORTED' },
      })
    }
    if (path.endsWith('/evaluate'))
      return response(evaluation, failure ? 503 : 200)
    return response(recommended)
  })
  vi.stubGlobal('fetch', mock)
  return mock
}
describe('production goal connection', () => {
  it('publishes the existing numeric goal as a detached ready snapshot', async () => {
    mockFetch()
    const capture = vi.fn<(snapshot: SearchGoalSnapshot) => void>()
    setup(item, vi.fn(), true, capture)
    const search = await screen.findByRole('combobox', { name: 'Search stats' })
    fireEvent.change(search, { target: { value: 'Cold' } })
    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Cold total' }),
    )
    await waitFor(() =>
      expect(capture.mock.lastCall?.[0].goal.groups[0]?.entries).toHaveLength(
        1,
      ),
    )
    await waitFor(() => expect(capture.mock.lastCall?.[0].ready).toBe(true))
    const snapshot = capture.mock.lastCall![0]
    expect(snapshot.ready).toBe(true)
    expect(snapshot.catalogVersion).toBe('v1')
    expect(snapshot.goal.groups[0]!.entries[0]!.statId).toBe('cold-total')
    snapshot.goal.groups[0]!.entries[0]!.statId = 'consumer-mutation'
    expect(
      screen.getByRole('button', { name: 'Delete Cold total' }),
    ).toBeVisible()
    expect(screen.queryByText('consumer-mutation')).not.toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('Min'), {
      target: { value: 'invalid' },
    })
    await waitFor(() => expect(capture.mock.lastCall?.[0].ready).toBe(false))
    fireEvent.change(screen.getByPlaceholderText('Min'), {
      target: { value: '15' },
    })
    await waitFor(() => expect(capture.mock.lastCall?.[0].ready).toBe(true))
    expect(
      capture.mock.lastCall![0].goal.groups[0]!.entries[0]!.range.min,
    ).toBe(15)
  })
  it('removes manual evaluation while retaining numeric editing', async () => {
    const fetch = mockFetch()
    setup(item, vi.fn(), true)
    const search = await screen.findByRole('combobox', {
      name: 'Search stats',
    })
    expect(
      screen.queryByRole('button', { name: /Equipment filters/ }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText('Numeric input settings')).not.toBeInTheDocument()
    fireEvent.change(search, { target: { value: 'Cold' } })
    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Cold total' }),
    )
    expect(
      screen.queryByRole('button', { name: 'Evaluate current item' }),
    ).not.toBeInTheDocument()
    expect(
      fetch.mock.calls.some(([url]) => /evaluate|recommend/.test(String(url))),
    ).toBe(false)
  })
  it('keeps numeric editing from canceling the legacy Support requests', async () => {
    mockFetch()
    const legacyChange = vi.fn()
    setup(item, legacyChange)
    const search = await screen.findByRole('combobox', {
      name: 'Search stats',
    })
    fireEvent.change(search, { target: { value: 'Cold' } })
    expect(legacyChange).not.toHaveBeenCalled()
  })
  it('keeps existing ineligible source stats and layer-specific presence without inventing values', () => {
    const c = structuredClone(catalog) as Catalog
    c.stats = [
      {
        ...c.stats[0]!,
        kind: 'EXPLICIT',
        statId: 'direct-cold',
        sourceStatIds: ['cold'],
        eligible: false,
      },
      {
        ...c.stats[0]!,
        kind: 'IMPLICIT',
        statId: 'implicit-cold',
        sourceStatIds: ['cold'],
        eligible: false,
      },
      {
        ...c.stats[0]!,
        contributions: [{ statId: 'direct-cold', coefficient: 1 }],
      },
    ]
    const present = presentNumericStats(item, c)
    expect([...present]).toEqual(['direct-cold', 'cold-total'])
    const goal = emptyGoal(context, 'v1')
    addStat(goal.groups[0]!, c.stats[0]!, present.has('direct-cold'))
    expect(goal.groups[0]!.entries).toHaveLength(1)
    expect(
      presentNumericStats({ ...item, baseItemId: 'another' }, c).size,
    ).toBe(0)
  })
  it.each(['NORMAL', 'MAGIC', 'RARE'] as const)(
    'preserves %s rarity, actual values, quality and AbortSignal in both wire requests',
    async (rarity) => {
      const actual = { ...item, rarity }
      const mock = mockFetch()
      const signal = new AbortController().signal
      const goal = {
        version: 1 as const,
        catalogVersion: 'v1',
        general: {
          baseItemId: 'amulet',
          itemLevel: { min: null, max: null },
          rarities: ['RARE'],
        },
        groups: [],
      }
      await evaluateNumericItem(
        actual,
        goal,
        ['Omen_of_Sinistral_Exaltation'],
        2000,
        signal,
        'fixture-ruleset',
      )
      for (const [, init] of mock.mock.calls) {
        expect(JSON.parse(String(init?.body)).item).toEqual(actual)
        expect(init?.signal).toBe(signal)
        expect(JSON.parse(String(init?.body)).item).not.toHaveProperty(
          'modifierIds',
        )
      }
    },
  )
  it('gates missing actual rolls without sending evaluation requests', async () => {
    const mock = mockFetch()
    setup(null)
    expect(
      await screen.findByText('Select a starting item to evaluate its values.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Evaluate current item' }),
    ).not.toBeInTheDocument()
    expect(
      mock.mock.calls.every(([url]) => !String(url).endsWith('/evaluate')),
    ).toBe(true)
  })
  it('retains internal numeric evaluation and service failure handling', async () => {
    const goal = emptyGoal(context, 'v1')
    mockFetch()
    const result = await evaluateNumericItem(
      item,
      goal,
      [],
      2000,
      new AbortController().signal,
      'fixture-ruleset',
    )
    expect(result.evaluation.status).toBe('MATCH')
    expect(result.recommendation.probability.status).toBe('UNSUPPORTED')
    mockFetch(true)
    await expect(
      evaluateNumericItem(
        item,
        goal,
        [],
        2000,
        new AbortController().signal,
        'fixture-ruleset',
      ),
    ).rejects.toThrow()
  })
})

it('resets goal defaults immediately without changing the start and restores them after reload', async () => {
  mockFetch()
  const goal = emptyGoal(context, 'v1')
  goal.general.rarities = ['RARE']
  goal.general.itemLevel = { min: 70, max: 90 }
  addStat(goal.groups[0]!, (catalog as Catalog).stats[0]!)
  saveSupport('goal', goal)
  localStorage.setItem('hephaistos.support.v1:start', 'untouched-start')
  const capture = vi.fn<(snapshot: SearchGoalSnapshot) => void>()
  const rendered = setup(item, vi.fn(), true, capture)
  const reset = await screen.findByRole('button', { name: 'Reset goal' })
  await waitFor(() => expect(reset).toBeEnabled())
  fireEvent.click(reset)
  const saved = readSupport('goal', validGoal)!
  expect(saved.general).toEqual({
    baseItemId: context.baseItemId,
    itemLevel: { min: null, max: null },
    rarities: ['RARE'],
  })
  expect(saved.groups).toHaveLength(1)
  expect(saved.groups[0]!.entries).toEqual([])
  expect(localStorage.getItem('hephaistos.support.v1:start')).toBe(
    'untouched-start',
  )
  rendered.unmount()
  setup(item, vi.fn(), true, capture)
  await waitFor(() => expect(capture.mock.lastCall?.[0].goal).toEqual(saved))
})

it('sends the same above-cap goal to validation and automatic search from a restored numeric setting', async () => {
  const fetcher = mockFetch()
  const goal = emptyGoal(context, 'v1')
  goal.general.quality = 30
  addStat(goal.groups[0]!, (catalog as Catalog).stats[0]!)
  saveSupport('goal', goal)
  const capture = vi.fn<(snapshot: SearchGoalSnapshot) => void>()
  setup(item, vi.fn(), true, capture)
  const checkbox = await screen.findByRole('checkbox', {
    name: /Use Essence of the Breach/,
  })
  expect(checkbox).toBeChecked()
  await waitFor(() => expect(capture.mock.lastCall?.[0].ready).toBe(true))
  expect(capture.mock.lastCall?.[0].goal.general.quality).toBe(40)
  const validated = fetcher.mock.calls.filter(([url]) =>
    String(url).endsWith('/validate'),
  )
  expect(
    JSON.parse(String(validated.at(-1)![1]?.body)).goal.general.quality,
  ).toBe(40)
  fireEvent.click(checkbox)
  await waitFor(() =>
    expect(capture.mock.lastCall?.[0].goal.general.quality).toBeNull(),
  )
  expect(readSupport('goal', validGoal)?.groups[0]?.entries).toHaveLength(1)
})
