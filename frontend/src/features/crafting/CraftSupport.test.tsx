import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { fixtureFetch, jsonResponse } from '../../shared/test/craftingFixtures'
import type { Bucket } from './craftingApi'

function openFamilyComparison() {
  const advanced = screen.getByText('Advanced family / tier comparison')
  if (!advanced.closest('details')?.open) fireEvent.click(advanced)
}

let failRecommendations = false
let partialRecommendations = false
const families = [
  {
    id: 'Life',
    affix: 'PREFIX',
    effectExamples: ['+Life'],
    tiers: [
      { tier: 1, requiredItemLevel: 1, exampleText: '+Life', modifierId: 'p' },
    ],
  },
  {
    id: 'Strength',
    affix: 'SUFFIX',
    effectExamples: ['+Strength'],
    tiers: [
      {
        tier: 1,
        requiredItemLevel: 1,
        exampleText: '+Strength',
        modifierId: 's',
      },
    ],
  },
  {
    id: 'IncreaseSocketedGemLevel',
    affix: 'SUFFIX',
    effectExamples: [
      '+2 to Level of all Melee Skills',
      '+2 to Level of all Spell Skills',
    ],
    tiers: [
      {
        tier: 1,
        requiredItemLevel: 1,
        exampleText: '+2 to Level of all Melee Skills',
        modifierId: 'melee',
      },
      {
        tier: 1,
        requiredItemLevel: 1,
        exampleText: '+2 to Level of all Spell Skills',
        modifierId: 'spell',
      },
      {
        tier: 2,
        requiredItemLevel: 1,
        exampleText: '+1 to Level of all Melee Skills',
        modifierId: 'melee-low',
      },
    ],
  },
]
beforeEach(() => {
  failRecommendations = false
  partialRecommendations = false
  useItemDraft.getState().setBase()
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.includes('/support/families')) return jsonResponse(families)
      if (url.includes('/support/recommend')) {
        if (failRecommendations) return jsonResponse({}, 503)
        const partial = partialRecommendations
        return jsonResponse({
          rulesetIdentity: 'fixture-ruleset',
          ruleVersion: 'fixture-rule',
          ledgerVersion: 'fixture-ledger',
          transitionNamespace: 'fixture-cache',
          assessment: {
            status: 'READY',
            valid: true,
            achieved: false,
            feasible: true,
            requiredMatched: 0,
            candidatesMatched: 0,
            matches: { Life: false },
            issues: [],
          },
          complete: !partial,
          rankingCertified: !partial,
          comparedSequences: 3,
          totalSequences: partial ? 1458 : 3,
          expandedStates: 3,
          expandedEdges: 9,
          elapsedMillis: 1.2,
          cache: { memoryHits: 3, persistedHits: 0, computedPools: 0 },
          comparisons: [
            'TRANSMUTATION',
            'GREATER_TRANSMUTATION',
            'PERFECT_TRANSMUTATION',
          ].map((action) => ({
            sequence: [action],
            successLower: 0.1,
            successUpper: partial ? 0.8 : 0.1,
            failureProbability: partial ? 0.2 : 0.9,
            unresolvedProbability: partial ? 0.7 : 0,
            complete: !partial,
            steps: [
              {
                step: 1,
                action,
                firstHitProbability: 0.1,
                blockedProbability: 0,
                continuingOrUnresolvedProbability: partial ? 0.7 : 0.9,
                blockedReasons: [],
              },
            ],
          })),
        })
      }
      if (url.includes('/support/assess')) {
        const body = JSON.parse(String(init?.body)) as { state: Bucket }
        const achieved = body.state.modifierIds.includes('p')
        return jsonResponse({
          status: achieved ? 'ACHIEVED' : 'READY',
          valid: true,
          achieved,
          feasible: true,
          requiredMatched: achieved ? 1 : 0,
          candidatesMatched: 0,
          matches: { Life: achieved },
          issues: [],
        })
      }
      return fixtureFetch(input, init)
    }),
  )
})
afterEach(() => vi.unstubAllGlobals())
function show() {
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CraftingPage />
    </QueryClientProvider>,
  )
}
describe('Independent Craft Support goal input', () => {
  it('deduplicates goal tiers while naming every effect variant and retaining manual alternatives', async () => {
    show()
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    await screen.findAllByRole('option', {
      name: 'IncreaseSocketedGemLevel · suffix',
    })
    fireEvent.change(screen.getByLabelText('Add required family'), {
      target: { value: 'IncreaseSocketedGemLevel' },
    })
    const tiers = screen.getByLabelText(
      'required IncreaseSocketedGemLevel minimum tier',
    )
    expect(within(tiers).getAllByRole('option')).toHaveLength(2)
    expect(
      within(tiers).getAllByRole('option', { name: 'T1 or better (T1)' }),
    ).toHaveLength(1)
    expect(screen.getByText('Any effect in this family counts.')).toBeVisible()
    expect(screen.getByText('+2 to Level of all Spell Skills')).toBeVisible()
    fireEvent.change(screen.getByLabelText('Support input source'), {
      target: { value: 'manual' },
    })
    const manual = screen.getByLabelText('Add manual modifier')
    expect(
      within(manual).getAllByRole('option', { name: /Melee Skills/ }),
    ).toHaveLength(2)
    expect(
      within(manual).getByRole('option', { name: /Spell Skills/ }),
    ).toBeInTheDocument()
  })
  it('shows actual-service comparison fields and chosen guide, then clears the old route for recovery input', async () => {
    show()
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    await waitFor(() =>
      expect(
        screen.getAllByRole('option', { name: 'Life · prefix' })[0],
      ).toBeInTheDocument(),
    )
    fireEvent.change(screen.getByLabelText('Add required family'), {
      target: { value: 'Life' },
    })
    fireEvent.change(screen.getByLabelText('Candidate N'), {
      target: { value: '0' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Compare currency sequences' }),
    )
    await screen.findByRole('heading', {
      name: 'Calculated sequence comparison',
    })
    expect(
      screen.getByText('Calculation details and sources').closest('details'),
    ).not.toHaveAttribute('open')
    fireEvent.click(screen.getByRole('button', { name: 'Choose sequence 1' }))
    expect(
      screen.getByRole('heading', {
        name: 'Chosen sequence — stop as soon as the goal is met',
      }),
    ).toBeVisible()
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Enter recovered state as a new root',
      }),
    )
    expect(
      screen.queryByRole('heading', { name: 'Calculated sequence comparison' }),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText('Support input source')).toHaveValue('manual')
    expect(
      screen.getByText(/previous comparison results were cleared/),
    ).toBeVisible()
  })
  it('distinguishes partial bounds and a service failure from a zero success result', async () => {
    partialRecommendations = true
    show()
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    await waitFor(() =>
      expect(
        screen.getAllByRole('option', { name: 'Life · prefix' })[0],
      ).toBeInTheDocument(),
    )
    fireEvent.change(screen.getByLabelText('Add required family'), {
      target: { value: 'Life' },
    })
    fireEvent.change(screen.getByLabelText('Candidate N'), {
      target: { value: '0' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Compare currency sequences' }),
    )
    await screen.findByRole('heading', {
      name: 'Partial comparison — ranking is not final',
    })
    expect(screen.getByText(/1455 unexamined sequences/)).toBeVisible()
    failRecommendations = true
    fireEvent.click(
      screen.getByRole('button', { name: 'Compare currency sequences' }),
    )
    await screen.findByText(
      'The recommendation service failed. Your inputs are preserved; please retry.',
    )
    expect(
      screen.queryByRole('heading', {
        name: 'Partial comparison — ranking is not final',
      }),
    ).not.toBeInTheDocument()
  })
  it('builds a distinct family goal and keeps the Support state while using Workbench', async () => {
    show()
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    await waitFor(() =>
      expect(
        screen.getAllByRole('option', { name: 'Life · prefix' })[0]!,
      ).toBeInTheDocument(),
    )
    fireEvent.change(screen.getByLabelText('Add required family'), {
      target: { value: 'Life' },
    })
    fireEvent.change(screen.getByLabelText('Candidate N'), {
      target: { value: '0' },
    })
    expect(
      screen.getByRole('option', { name: 'T1 or better (T1)' }),
    ).toBeInTheDocument()
    expect(
      screen
        .getByLabelText('Add candidates family')
        .querySelector('[value="Life"]'),
    ).toBeNull()
    fireEvent.click(
      screen.getByRole('button', { name: 'Check starting state and goal' }),
    )
    await screen.findByRole('heading', {
      name: 'Goal ready for sequence comparison',
    })
    expect(
      screen.getByText(
        'No sequence probabilities calculated yet. Choose Compare currency sequences.',
      ),
    ).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Crafting Workbench' }))
    fireEvent.click(
      screen.getByRole('button', { name: 'Orb of Transmutation' }),
    )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    expect(
      screen.getByText(
        'Solar Amulet · Level 82 · normal · 0 explicit modifiers',
      ),
    ).toBeVisible()
    expect(
      screen.getByRole('heading', {
        name: 'Goal ready for sequence comparison',
      }),
    ).toBeVisible()
  })
  it('recognizes an achieved manual tier root without fabricating a currency recommendation', async () => {
    show()
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    openFamilyComparison()
    await waitFor(() =>
      expect(
        screen.getAllByRole('option', { name: 'Life · prefix' })[0]!,
      ).toBeInTheDocument(),
    )
    fireEvent.change(screen.getByLabelText('Support input source'), {
      target: { value: 'manual' },
    })
    fireEvent.change(screen.getByLabelText('Add manual modifier'), {
      target: { value: 'p' },
    })
    fireEvent.change(screen.getByLabelText('Add required family'), {
      target: { value: 'Life' },
    })
    fireEvent.change(screen.getByLabelText('Candidate N'), {
      target: { value: '0' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Check starting state and goal' }),
    )
    await screen.findByRole('heading', { name: 'Goal already achieved' })
    expect(
      screen.getByText(/First-hit probability at step 0 is 100%/),
    ).toBeVisible()
  })
  it('uses keyboard navigation across all three services', async () => {
    show()
    const bench = screen.getByRole('tab', { name: 'Crafting Workbench' })
    bench.focus()
    fireEvent.keyDown(bench, { key: 'ArrowRight' })
    expect(screen.getByRole('tab', { name: 'Craft Support' })).toHaveFocus()
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Craft Support' }), {
      key: 'ArrowRight',
    })
    expect(screen.getByRole('tab', { name: 'State explorer' })).toHaveFocus()
  })
})
