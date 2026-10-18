import { GoalFilterPanel } from './GoalFilterPanel'
import {
  readSupport,
  saveSupport,
  validGoal,
} from '../method-search/supportStorage'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  act,
  render,
  renderHook,
  screen,
  waitFor,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useStore } from 'zustand'
import { compatibilityIssues, rememberContract } from './catalogCompatibility'
import {
  useCatalogRecovery,
  useRefreshConflictedCatalog,
} from './useCatalogRecovery'
import { createGoalFilterEditor } from './editor'
import { GoalFilterApiError } from './api'
import type { Catalog, GoalFilter, GoalFilterAdapter } from './types'

const context = { snapshotId: 'snapshot', baseItemId: 'solar', itemLevel: 82 }
const statId = 'hephaistos:v1:explicit:base_maximum_life'
function catalog(version = 'new'): Catalog {
  return {
    version: 1,
    catalogVersion: version,
    context,
    groupTypes: ['AND', 'COUNT'].map((type) => ({
      type: type as 'AND' | 'COUNT',
      evaluation: 'SUPPORTED',
      probability: 'SUPPORTED',
      reasonCode: null,
    })),
    stats: [
      {
        statId,
        label: 'Maximum Life',
        unit: 'flat',
        kind: 'EXPLICIT',
        support: {
          evaluation: 'SUPPORTED',
          probability: 'SUPPORTED',
          reasonCode: null,
        },
        eligible: true,
        eligibilityReason: null,
        sourceStatIds: ['base_maximum_life'],
        contributions: [],
        sourceUrls: [],
      },
    ],
    issues: [],
  }
}
function goal(version = 'old'): GoalFilter {
  return {
    version: 1,
    catalogVersion: version,
    general: {
      baseItemId: 'solar',
      itemLevel: { min: 82, max: 100 },
      rarities: ['RARE'],
    },
    groups: [
      {
        id: 'g',
        type: 'COUNT',
        range: { min: 1, max: null },
        disabled: false,
        entries: [
          {
            id: 'e',
            statId,
            unit: 'flat',
            range: { min: 120, max: 140 },
            weight: null,
            disabled: false,
          },
        ],
      },
    ],
  }
}
const success = {
  version: 1 as const,
  valid: true,
  issues: [],
  capabilities: {
    evaluation: 'SUPPORTED' as const,
    probability: 'SUPPORTED' as const,
  },
}
function setup(
  initial = goal(),
  initialCatalog = catalog(),
  validate = vi.fn(async () => success),
) {
  const editor = createGoalFilterEditor(initial)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  const adapter: GoalFilterAdapter = {
    id: 'test',
    catalog: async () => initialCatalog,
    validate,
  }
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
  const hook = renderHook(
    ({ current }) => {
      const value = useStore(editor, (s) => s.goal)
      return useCatalogRecovery(adapter, context, value, current, editor, true)
    },
    { initialProps: { current: initialCatalog }, wrapper },
  )
  return { editor, validate, ...hook }
}
beforeEach(() => localStorage.clear())
describe('catalog recovery preserves the goal', () => {
  it('validates a legacy direct goal before committing only the version', async () => {
    const original = goal()
    const h = setup(original)
    await waitFor(() =>
      expect(h.editor.getState().goal.catalogVersion).toBe('new'),
    )
    expect(h.editor.getState().goal).toEqual({
      ...original,
      catalogVersion: 'new',
    })
    expect(h.validate).toHaveBeenCalledTimes(1)
    h.rerender({ current: catalog() })
    expect(h.validate).toHaveBeenCalledTimes(1)
  })
  it('does not migrate an already current goal', () => {
    const h = setup(goal('new'))
    expect(h.validate).not.toHaveBeenCalled()
    expect(h.editor.getState().revision).toBe(0)
  })
  it.each(['missing', 'unit', 'operation', 'meaning', 'legacy-pseudo'])(
    'preserves incompatible %s entries',
    (kind) => {
      const g = goal()
      const c = catalog()
      if (kind === 'missing') c.stats = []
      if (kind === 'unit') c.stats[0]!.unit = 'percent'
      if (kind === 'operation') c.groupTypes = []
      if (kind === 'meaning') {
        rememberContract(g, catalog('old'))
        c.stats[0]!.sourceStatIds = ['other']
      }
      if (kind === 'legacy-pseudo') c.stats[0]!.kind = 'PSEUDO'
      const h = setup(g, c)
      expect(h.result.current.issues.length).toBeGreaterThan(0)
      expect(h.validate).not.toHaveBeenCalled()
      expect(h.editor.getState().goal).toEqual(g)
    },
  )
  it('preserves disabled rows and validates them too', () => {
    const g = goal()
    g.groups[0]!.entries[0]!.disabled = true
    const c = catalog()
    c.stats = []
    expect(compatibilityIssues(g, c)).toHaveLength(1)
  })
  it('accepts a pseudo only with an unchanged saved semantic definition', () => {
    const g = goal()
    const c = catalog('old')
    c.stats[0]!.kind = 'PSEUDO'
    c.stats[0]!.contributions = [{ statId: 'life', coefficient: 1 }]
    rememberContract(g, c)
    expect(compatibilityIssues(g, { ...c, catalogVersion: 'new' })).toEqual([])
    c.stats[0]!.contributions[0]!.coefficient = 2
    expect(
      compatibilityIssues(g, { ...c, catalogVersion: 'new' })[0]?.code,
    ).toBe('RECOVERY_MEANING')
  })
  it('handles a catalog change while mounted and keeps subsequent refreshes stable', async () => {
    const original = goal('old')
    const h = setup(original, catalog('old'))
    h.rerender({ current: catalog('new') })
    await waitFor(() =>
      expect(h.editor.getState().goal.catalogVersion).toBe('new'),
    )
    h.unmount()
    const remounted = setup(h.editor.getState().goal, catalog('new'))
    expect(remounted.editor.getState().goal).toEqual({
      ...original,
      catalogVersion: 'new',
    })
    expect(remounted.validate).not.toHaveBeenCalled()
  })
  it('does not commit a rejected condition or loop on rejection', async () => {
    const validate = vi.fn(async () => {
      throw new GoalFilterApiError(422, [
        {
          code: 'INVALID_RANGE',
          path: '/goal/groups/0/entries/0',
          message: 'range',
          severity: 'ERROR',
        },
      ])
    })
    const h = setup(goal(), catalog(), validate)
    await waitFor(() => expect(h.result.current.failed).toBe(true))
    h.rerender({ current: catalog() })
    expect(h.editor.getState().goal).toEqual(goal())
    expect(validate).toHaveBeenCalledTimes(1)
  })
  it('never commits an obsolete validation response over a later edit', async () => {
    let resolve!: (value: typeof success) => void
    const validate = vi.fn(
      () =>
        new Promise<typeof success>((r) => {
          resolve = r
        }),
    )
    const h = setup(goal(), catalog(), validate)
    await waitFor(() => expect(validate).toHaveBeenCalledTimes(1))
    const first = resolve
    act(() =>
      h.editor.getState().edit((g) => {
        g.groups[0]!.entries[0]!.range.min = 130
      }),
    )
    await act(async () => first(success))
    expect(h.editor.getState().goal.catalogVersion).toBe('old')
    expect(h.editor.getState().goal.groups[0]!.entries[0]!.range.min).toBe(130)
  })
  it('only refreshes a specific catalog conflict once and leaves revision conflicts alone', () => {
    const refetch = vi.fn()
    const error = new GoalFilterApiError(409, [
      {
        code: 'CATALOG_VERSION_MISMATCH',
        path: '/goal/catalogVersion',
        message: 'stale',
        severity: 'ERROR',
      },
    ])
    const h = renderHook(
      ({ error }) => useRefreshConflictedCatalog(error, refetch),
      { initialProps: { error } },
    )
    expect(refetch).toHaveBeenCalledTimes(1)
    h.rerender({ error })
    h.rerender({
      error: new GoalFilterApiError(409, [
        {
          code: 'REVISION_CONFLICT',
          path: '/revision',
          message: 'stale',
          severity: 'ERROR',
        },
      ]),
    })
    expect(refetch).toHaveBeenCalledTimes(1)
  })
})

describe('recovery user interface and persistence', () => {
  it('shows the exact incompatible option without deleting the saved goal or note', async () => {
    const original = goal()
    saveSupport('goal', original)
    localStorage.setItem('unrelated-note', 'keep this note')
    const editor = createGoalFilterEditor(readSupport('goal', validGoal)!)
    const current = catalog()
    current.stats = []
    const adapter: GoalFilterAdapter = {
      id: 'panel',
      catalog: async () => current,
      validate: vi.fn(async () => success),
    }
    render(
      <QueryClientProvider client={new QueryClient()}>
        <GoalFilterPanel
          adapter={adapter}
          context={context}
          editor={editor}
          bases={[{ id: 'solar', label: 'Solar' }]}
          language="en"
          compact
        />
      </QueryClientProvider>,
    )
    expect(
      await screen.findByText(/This option is absent from the current catalog/),
    ).toHaveTextContent(statId)
    expect(editor.getState().goal).toEqual(original)
    expect(readSupport('goal', validGoal)).toEqual(original)
    expect(localStorage.getItem('unrelated-note')).toBe('keep this note')
  })
  it('retains an upgraded goal and contract across storage restore and remount', async () => {
    saveSupport('goal', goal())
    const h = setup(readSupport('goal', validGoal)!)
    await waitFor(() =>
      expect(h.editor.getState().goal.catalogVersion).toBe('new'),
    )
    saveSupport('goal', h.editor.getState().goal)
    h.unmount()
    const saved = readSupport('goal', validGoal)!
    const next = setup(saved)
    expect(next.editor.getState().goal).toEqual({
      ...goal(),
      catalogVersion: 'new',
    })
    expect(next.validate).not.toHaveBeenCalled()
  })
})
