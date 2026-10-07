import { afterEach, describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ConnectedGoalFilter } from './ConnectedGoalFilter'
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
    headers: { 'Content-Type': 'application/json' },
  })
afterEach(() => vi.unstubAllGlobals())
function setup(actual: ConcreteItem | null = item, onLegacyChange = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <div onChange={onLegacyChange}>
      <QueryClientProvider client={client}>
        <ConnectedGoalFilter
          item={actual}
          context={item}
          language="en"
          activeOmens={[]}
          maxMillis={2000}
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
  it('keeps numeric editing from canceling the legacy Support requests', async () => {
    mockFetch()
    const legacyChange = vi.fn()
    setup(item, legacyChange)
    const search = await screen.findByRole('searchbox', {
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
  it('preserves actual values, quality and AbortSignal in both wire requests', async () => {
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
      item,
      goal,
      ['Omen_of_Sinistral_Exaltation'],
      2000,
      signal,
    )
    for (const [, init] of mock.mock.calls) {
      expect(JSON.parse(String(init?.body)).item).toEqual(item)
      expect(init?.signal).toBe(signal)
      expect(JSON.parse(String(init?.body)).item).not.toHaveProperty(
        'modifierIds',
      )
    }
  })
  it('gates missing actual rolls without sending evaluation requests', async () => {
    const mock = mockFetch()
    setup(null)
    expect(
      await screen.findByText(/Actual rolls are unavailable/),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Evaluate current item' }),
    ).toBeDisabled()
    expect(
      mock.mock.calls.every(([url]) => !String(url).endsWith('/evaluate')),
    ).toBe(true)
  })
  it('shows unsupported numeric probability and removes stale results on unfinished editing', async () => {
    mockFetch()
    setup()
    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Cold total' }),
    )
    const button = screen.getByRole('button', { name: 'Evaluate current item' })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    expect(
      await screen.findByText('Current item evaluation: MATCH'),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Numeric probability: UNSUPPORTED/),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/Total sequences unknown: null/),
    ).toBeInTheDocument()
    const mins = screen.getAllByLabelText('Min')
    fireEvent.change(mins.at(-1)!, { target: { value: '-' } })
    expect(
      screen.queryByText('Current item evaluation: MATCH'),
    ).not.toBeInTheDocument()
    expect(button).toBeDisabled()
  })
  it('shows declared-model partial bounds and uncertified ranking', async () => {
    mockFetch(false, {
      ...recommendation,
      probability: {
        status: 'PARTIAL',
        reasonCode: 'BUDGET_EXHAUSTED',
        modelVersion: 'solar-numeric-addition-v1',
        ledgerVersion: 'uniform-integer-roll-v1',
      },
      comparisons: [
        {
          sequence: ['EXALTED'],
          successLower: 0.125,
          successUpper: 0.5,
          failureProbability: 0.5,
          unresolvedProbability: 0.375,
          complete: false,
        },
      ],
      comparedSequences: 1,
      totalSequences: 4,
    })
    setup({ ...item, catalystQuality: null })
    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Cold total' }),
    )
    const button = screen.getByRole('button', { name: 'Evaluate current item' })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    expect(
      await screen.findByText(/Declared model probability/),
    ).toBeInTheDocument()
    expect(screen.getByText(/Ranking unresolved/)).toBeInTheDocument()
    expect(
      screen.getByText(
        /success 12.5000% to 50.0000%; failure 50.0000%; unresolved 37.5000%/,
      ),
    ).toBeInTheDocument()
  })
  it('keeps inputs and permits retry after service failure', async () => {
    mockFetch(true)
    setup()
    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Cold total' }),
    )
    const button = screen.getByRole('button', { name: 'Evaluate current item' })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    expect(await screen.findByText(/Evaluation failed/)).toBeInTheDocument()
    expect(button).toBeEnabled()
    expect(screen.getByText('Cold total (percent)')).toBeInTheDocument()
  })
})
