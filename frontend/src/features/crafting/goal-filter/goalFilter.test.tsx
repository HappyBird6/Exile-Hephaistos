import { describe, expect, it } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import fixture from '../../../../../contracts/support-goal-filter-v1/fixtures.json'
import {
  createGoalFilterEditor,
  emptyGoal,
  changeGroupType,
  addStat,
} from './editor'
import { mockGoalFilterAdapter, fixtureContext } from './mock'
import { GoalFilterPanel } from './GoalFilterPanel'
import { groupTypes } from './types'
import type { Catalog, GoalFilter, GoalFilterAdapter } from './types'
describe('goal filter contract editor', () => {
  it('preserves rows across all six operations with type-specific range and weights', async () => {
    const catalog = await mockGoalFilterAdapter.catalog(
      fixtureContext,
      new AbortController().signal,
    )
    const goal = emptyGoal(fixtureContext, catalog.catalogVersion)
    const group = goal.groups[0]!
    addStat(group, catalog.stats[0]!)
    addStat(group, catalog.stats[0]!)
    expect(group.entries).toHaveLength(1)
    for (const type of groupTypes) {
      changeGroupType(group, type)
      expect(group.entries[0]!.statId).toBe(catalog.stats[0]!.statId)
      expect(group.range === null).toBe(['AND', 'NOT', 'IF'].includes(type))
      expect(group.entries[0]!.weight).toBe(
        type.startsWith('WEIGHTED') ? 1 : null,
      )
    }
    changeGroupType(group, 'WEIGHTED_V1')
    group.entries[0]!.weight = -2
    changeGroupType(group, 'WEIGHTED_V2')
    expect(group.entries[0]!.weight).toBe(-2)
  })
  it('uses fixture wire shape and rejects reversed ranges, duplicates, empty groups and stale catalog', async () => {
    const goal = structuredClone(
      fixture.apiExamples.validateRequest.goal,
    ) as GoalFilter
    const signal = new AbortController().signal
    expect(
      await mockGoalFilterAdapter.validate(fixtureContext, goal, signal),
    ).toEqual(fixture.apiExamples.validateResponse)
    goal.groups[0]!.entries[0]!.range = { min: 20, max: 10 }
    expect(
      (
        await mockGoalFilterAdapter.validate(fixtureContext, goal, signal)
      ).issues.some((i) => i.code === 'INVALID_RANGE'),
    ).toBe(true)
    goal.groups[0]!.entries[0]!.disabled = true
    expect(
      (
        await mockGoalFilterAdapter.validate(fixtureContext, goal, signal)
      ).issues.some((i) => i.code === 'EMPTY_GROUP'),
    ).toBe(true)
    goal.catalogVersion = 'old'
    expect(
      (
        await mockGoalFilterAdapter.validate(fixtureContext, goal, signal)
      ).issues.some((i) => i.code === 'CATALOG_VERSION_MISMATCH'),
    ).toBe(true)
  })
  it('preserves goals on base changes and keeps collapsed state out of AST', () => {
    const editor = createGoalFilterEditor(emptyGoal(fixtureContext, 'v1'))
    editor.getState().collapse(editor.getState().goal.groups[0]!.id)
    editor.getState().edit((g) => {
      g.general.baseItemId = 'unsupported'
    })
    expect(editor.getState().goal.groups).toHaveLength(1)
    expect(JSON.stringify(editor.getState().goal)).not.toContain('collapsed')
  })
  it('supports category search, repeated edits, delete focus and unsupported-base diagnostics', async () => {
    const editor = createGoalFilterEditor(
      emptyGoal(fixtureContext, fixture.catalogVersion),
    )
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    render(
      <QueryClientProvider client={client}>
        <GoalFilterPanel
          editor={editor}
          adapter={mockGoalFilterAdapter}
          context={fixtureContext}
          language="en"
          bases={[
            { id: 'fixture-base', label: 'Fixture' },
            { id: 'unsupported', label: 'Unsupported' },
          ]}
          recommendation={{
            ...fixture.apiExamples.recommendUnsupportedResponse,
            version: 1,
            probability: {
              ...fixture.apiExamples.recommendUnsupportedResponse.probability,
              status: 'UNSUPPORTED',
            },
          }}
        />
      </QueryClientProvider>,
    )
    fireEvent.focus(screen.getByLabelText('Search stats'))
    const add = await screen.findByRole('button', {
      name: 'Add total cold resistance',
    })
    fireEvent.change(screen.getByLabelText('Category'), {
      target: { value: 'PSEUDO' },
    })
    fireEvent.click(add)
    expect(screen.getByLabelText('Search stats')).toHaveFocus()
    fireEvent.click(
      screen.getByRole('button', { name: 'Delete total cold resistance' }),
    )
    expect(screen.getByLabelText('Search stats')).toHaveFocus()
    fireEvent.focus(screen.getByLabelText('Search stats'))
    fireEvent.click(
      screen.getByRole('button', { name: 'Add total cold resistance' }),
    )
    fireEvent.change(screen.getByLabelText('Equipment base'), {
      target: { value: 'unsupported' },
    })
    await waitFor(() =>
      expect(screen.getByText(/UNSUPPORTED_BASE/)).toBeInTheDocument(),
    )
    expect(editor.getState().goal.groups[0]!.entries).toHaveLength(1)
    expect(
      screen.getByText('Numeric probability unsupported'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/0%/)).not.toBeInTheDocument()
    client.clear()
  })
})

describe('goal filter async and numeric editing', () => {
  it('opens search on demand, preserves disabled and collapsed rows, and restores focus after deletion', async () => {
    const editor = createGoalFilterEditor(
      emptyGoal(fixtureContext, fixture.catalogVersion),
    )
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const view = render(
      <QueryClientProvider client={client}>
        <GoalFilterPanel
          editor={editor}
          adapter={mockGoalFilterAdapter}
          context={fixtureContext}
          language="en"
          bases={[]}
        />
      </QueryClientProvider>,
    )
    const search = screen.getByRole('combobox', { name: 'Search stats' })
    expect(search).toHaveAttribute('aria-expanded', 'false')
    fireEvent.focus(search)
    const add = await screen.findByRole('button', {
      name: 'Add total cold resistance',
    })
    fireEvent.keyDown(search, { key: 'ArrowDown' })
    expect(search).toHaveFocus()
    expect(
      document.getElementById(search.getAttribute('aria-activedescendant')!),
    ).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(add, { key: 'Escape' })
    expect(search).toHaveFocus()
    expect(search).toHaveAttribute('aria-expanded', 'false')
    fireEvent.keyDown(search, { key: 'Enter' })
    fireEvent.click(
      screen.getByRole('button', { name: 'Add total cold resistance' }),
    )
    expect(search).toHaveAttribute('aria-expanded', 'false')
    expect(search).toHaveValue('')
    fireEvent.click(screen.getAllByRole('checkbox', { name: 'Active' })[1]!)
    expect(editor.getState().goal.groups[0]!.entries[0]!.disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Collapse' }))
    expect(
      screen.queryByRole('button', { name: 'Delete total cold resistance' }),
    ).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Expand' }))
    expect(editor.getState().goal.groups[0]!.entries[0]!.disabled).toBe(true)
    fireEvent.click(
      screen.getByRole('button', { name: 'Delete total cold resistance' }),
    )
    expect(search).toHaveFocus()
    fireEvent.click(screen.getByRole('button', { name: 'Delete Group 1' }))
    expect(screen.getByRole('button', { name: /Add group/ })).toHaveFocus()
    expect(editor.getState().goal.groups).toHaveLength(0)
    fireEvent.click(screen.getByRole('button', { name: /Add group/ }))
    expect(editor.getState().goal.groups).toHaveLength(1)
    view.unmount()
    client.clear()
  })
  it('discards an old catalog response and aborts its context when base changes', async () => {
    const editor = createGoalFilterEditor(
      emptyGoal(fixtureContext, fixture.catalogVersion),
    )
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    let completeOld: (value: Catalog) => void = () => {
      throw new Error('Missing old request')
    }
    let oldSignal: AbortSignal | undefined
    const oldCatalog = await mockGoalFilterAdapter.catalog(
      fixtureContext,
      new AbortController().signal,
    )
    const adapter: GoalFilterAdapter = {
      ...mockGoalFilterAdapter,
      id: 'delayed',
      catalog(context, signal) {
        if (context.baseItemId === 'fixture-base') {
          oldSignal = signal
          return new Promise<Catalog>((resolve) => {
            completeOld = resolve
          })
        }
        return mockGoalFilterAdapter.catalog(context, signal)
      },
    }
    const view = render(
      <QueryClientProvider client={client}>
        <GoalFilterPanel
          editor={editor}
          adapter={adapter}
          context={fixtureContext}
          language="en"
          bases={[
            { id: 'fixture-base', label: 'Fixture' },
            { id: 'new', label: 'New' },
          ]}
        />
      </QueryClientProvider>,
    )
    await waitFor(() => expect(oldSignal).toBeDefined())
    fireEvent.change(screen.getByLabelText('Equipment base'), {
      target: { value: 'new' },
    })
    fireEvent.focus(screen.getByRole('combobox', { name: 'Search stats' }))
    await screen.findByText('No eligible stats.')
    await act(async () => completeOld(oldCatalog))
    expect(oldSignal!.aborted).toBe(true)
    expect(
      screen.queryByRole('button', { name: 'Add cold resistance' }),
    ).not.toBeInTheDocument()
    view.unmount()
    client.clear()
  })
  it('preserves intermediate numeric edits and rejects invalid input without clamping', async () => {
    const editor = createGoalFilterEditor(
      emptyGoal(fixtureContext, fixture.catalogVersion),
    )
    const client = new QueryClient()
    const view = render(
      <QueryClientProvider client={client}>
        <GoalFilterPanel
          editor={editor}
          adapter={mockGoalFilterAdapter}
          context={fixtureContext}
          language="en"
          bases={[]}
        />
      </QueryClientProvider>,
    )
    const min = screen.getByLabelText('Min')
    fireEvent.change(min, { target: { value: '-' } })
    expect(min).toHaveValue('-')
    expect(min).toHaveAttribute('aria-invalid', 'true')
    expect(editor.getState().goal.general.itemLevel.min).toBeNull()
    fireEvent.change(min, { target: { value: '-2' } })
    expect(editor.getState().goal.general.itemLevel.min).toBe(-2)
    fireEvent.change(min, { target: { value: '' } })
    expect(editor.getState().goal.general.itemLevel.min).toBeNull()
    view.unmount()
    client.clear()
  })
})

describe('shared fixture validation cases', () => {
  for (const testCase of fixture.validationCases) {
    it(testCase.id, async () => {
      const goal = structuredClone(
        fixture.apiExamples.validateRequest.goal,
      ) as GoalFilter
      const mutation = testCase.mutation as Record<string, unknown>
      const group = goal.groups[0]!
      const entry = group.entries[0]!
      if (mutation.range) entry.range = mutation.range as typeof entry.range
      if (mutation.unit) entry.unit = mutation.unit as string
      if (mutation.statId) entry.statId = mutation.statId as string
      if (mutation.version) Object.assign(goal, { version: mutation.version })
      if (mutation.duplicateEntry)
        group.entries.push({ ...entry, id: 'duplicate' })
      if (mutation.disableAllEntries)
        group.entries.forEach((row) => {
          row.disabled = true
        })
      if (mutation.disableAllGroups)
        goal.groups.forEach((item) => {
          item.disabled = true
        })
      if (mutation.type) {
        changeGroupType(group, mutation.type as typeof group.type)
        group.range = mutation.range as typeof group.range
      }
      if (mutation.catalogVersion)
        goal.catalogVersion = mutation.catalogVersion as string
      const result = await mockGoalFilterAdapter.validate(
        fixtureContext,
        goal,
        new AbortController().signal,
      )
      expect(result.issues.map((issue) => issue.code)).toContain(
        testCase.expectedCode,
      )
    })
  }
})
