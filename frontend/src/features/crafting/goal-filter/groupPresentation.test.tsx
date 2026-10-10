import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createGoalFilterEditor, emptyGoal, addStat } from './editor'
import { presentedGroupType, orHelp } from './groupPresentation'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
import { GoalFilterPanel } from './GoalFilterPanel'
import { goalFilterMessages } from './i18n'
import { locales } from '../../../shared/i18n/i18n'

describe('OR presentation over the COUNT contract', () => {
  it.each([
    [{ min: 1, max: null }, 'OR'],
    [{ min: 1, max: 1 }, 'COUNT'],
    [{ min: 2, max: null }, 'COUNT'],
    [{ min: 0, max: null }, 'COUNT'],
    [{ min: null, max: null }, 'COUNT'],
  ] as const)(
    'recognizes only unbounded at-least-one: %j',
    (range, expected) => {
      const goal = emptyGoal(fixtureContext, 'v1')
      const group = goal.groups[0]!
      group.type = 'COUNT'
      group.range = { ...range }
      const before = JSON.stringify(goal)
      expect(presentedGroupType(group)).toBe(expected)
      const restored = createGoalFilterEditor(JSON.parse(before))
      expect(presentedGroupType(restored.getState().goal.groups[0]!)).toBe(
        expected,
      )
      expect(JSON.stringify(restored.getState().goal)).toBe(before)
    },
  )
  it('preserves disabled rows and ranges, writes no new operator, and keeps COUNT editing distinct', async () => {
    const catalog = await mockGoalFilterAdapter.catalog(
      fixtureContext,
      new AbortController().signal,
    )
    const goal = emptyGoal(fixtureContext, catalog.catalogVersion)
    const group = goal.groups[0]!
    addStat(group, catalog.stats[0]!)
    addStat(group, catalog.stats[1]!)
    group.entries[0]!.range = { min: 10, max: 20 }
    group.entries[1]!.disabled = true
    const entries = structuredClone(group.entries)
    const editor = createGoalFilterEditor(goal)
    editor.getState().chooseGroupType(group.id, 'OR')
    const wire = editor.getState().goal
    expect(wire.groups[0]).toEqual({
      ...group,
      type: 'COUNT',
      range: { min: 1, max: null },
      entries,
    })
    expect(JSON.stringify(wire)).not.toContain('"OR"')
    expect(JSON.stringify(wire)).not.toContain('presentedTypes')
    expect(
      (
        await mockGoalFilterAdapter.validate(
          fixtureContext,
          wire,
          new AbortController().signal,
        )
      ).valid,
    ).toBe(true)
    editor.getState().chooseGroupType(group.id, 'COUNT')
    expect(
      presentedGroupType(
        editor.getState().goal.groups[0]!,
        editor.getState().presentedTypes[group.id],
      ),
    ).toBe('COUNT')
    expect(editor.getState().goal).toEqual(wire)
    editor.getState().edit((g) => {
      g.groups[0]!.range!.min = 2
    })
    expect(presentedGroupType(editor.getState().goal.groups[0]!)).toBe('COUNT')
    editor.getState().chooseGroupType(group.id, 'OR')
    expect(editor.getState().goal.groups[0]!.range).toEqual({
      min: 1,
      max: null,
    })
    const restored = createGoalFilterEditor(
      JSON.parse(JSON.stringify(editor.getState().goal)),
    )
    expect(presentedGroupType(restored.getState().goal.groups[0]!)).toBe('OR')
  })
  it.each(locales)(
    'offers literal AND/OR in %s and makes general COUNT bounds editable',
    async (language) => {
      const catalog = await mockGoalFilterAdapter.catalog(
        fixtureContext,
        new AbortController().signal,
      )
      const editor = createGoalFilterEditor(
        emptyGoal(fixtureContext, catalog.catalogVersion),
      )
      const client = new QueryClient({
        defaultOptions: { queries: { retry: false } },
      })
      const view = render(
        <QueryClientProvider client={client}>
          <GoalFilterPanel
            adapter={mockGoalFilterAdapter}
            context={fixtureContext}
            editor={editor}
            bases={[]}
            language={language}
            compact
          />
        </QueryClientProvider>,
      )
      const t = goalFilterMessages[language]
      const select = screen.getByRole('combobox', { name: t.group + ' 1' })
      expect(
        within(select).getByRole('option', { name: 'AND' }),
      ).toBeInTheDocument()
      expect(
        within(select).getByRole('option', { name: 'OR' }),
      ).toBeInTheDocument()
      fireEvent.change(select, { target: { value: 'OR' } })
      expect(select).toHaveValue('OR')
      expect(screen.getByText(orHelp[language])).toBeVisible()
      expect(
        screen.queryByRole('group', { name: t.count }),
      ).not.toBeInTheDocument()
      expect(editor.getState().goal.groups[0]).toMatchObject({
        type: 'COUNT',
        range: { min: 1, max: null },
      })
      fireEvent.change(select, { target: { value: 'COUNT' } })
      expect(select).toHaveValue('COUNT')
      const bounds = screen.getByRole('group', { name: t.count })
      fireEvent.change(within(bounds).getByLabelText(t.min), {
        target: { value: '2' },
      })
      expect(editor.getState().goal.groups[0]!.range).toEqual({
        min: 2,
        max: null,
      })
      fireEvent.change(select, { target: { value: 'OR' } })
      expect(editor.getState().goal.groups[0]!.range).toEqual({
        min: 1,
        max: null,
      })
      view.unmount()
      client.clear()
    },
  )
})
